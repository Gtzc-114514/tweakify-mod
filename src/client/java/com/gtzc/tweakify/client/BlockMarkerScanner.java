package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 世界标记扫描调度：基岩高亮与屏障可见共用一次 33³ 方块遍历。
 *
 * <p>两个功能都开启时，原先每 10 tick 要扫两遍（屏障一遍、基岩一遍）的区域，
 * 现在只在两种扫描同时到期的 tick 合并成一遍，方块读取次数约减少三分之一；
 * 只开一个功能时扫描频率与原来一致。
 *
 * <p>整个体积分帧处理：每 tick 只扫 {@link #LAYERS_PER_TICK} 层 y，
 * 避免一次 3.6 万次方块读取造成的帧时间尖刺。
 *
 * <p>两条额外的降耗措施：
 * <ul>
 *   <li>静止降频：玩家连续 2 秒位移不超过 0.05 格时，屏障/基岩的刷新间隔放宽到 40/80 tick；
 *       重新移动时若没有进行中的扫描，会立即补扫一次，移动时的可见性基本不受影响。</li>
 *   <li>section 预筛：逐层先查区块的 section palette 是否可能包含目标方块，
 *       不含（如纯石头/空气/越界层）则整段跳过，不再逐方块读取。</li>
 * </ul>
 */
final class BlockMarkerScanner {
	private static final int SCAN_RADIUS = 16;
	// 屏障每 10 tick（0.5 秒）刷新一次，基岩标记每 20 tick（1 秒）刷新一次。
	// 包私有：单测需要把间隔数值与需求固定下来。
	static final int BARRIER_INTERVAL_TICKS = 10;
	static final int BEDROCK_INTERVAL_TICKS = 20;
	// 静止时屏障/基岩的刷新间隔（静止判定与阈值见 {@link IdleTracker}）。
	static final int IDLE_BARRIER_INTERVAL_TICKS = 40;
	static final int IDLE_BEDROCK_INTERVAL_TICKS = 80;
	// 每次扫描分帧处理：每 tick 3 层 y（约 3.2k 次方块读取），一次完整扫描约 11 tick。
	private static final int LAYERS_PER_TICK = 3;
	// 单层最多跨几个区块：跨度 2R+1 格，最坏情况下起点落在区块末格，
	// 覆盖的区块数为 floor((跨度+14)/16) + 1（R=16 时为 3）。
	private static final int MAX_CHUNKS_PER_AXIS = (SCAN_RADIUS * 2 + 1 + 14) / 16 + 1;
	// 计数器回到 0 的周期：同时是 10/20/40/80 的整数倍，避免长时间运行后 int 溢出，
	// 也保证静止降频的取模周期不会被重置打断。
	private static final int COUNTER_RESET_TICKS = 1200;

	// 目标方块谓词：section 的 palette 完全不包含它们时整段跳过读取。
	private static final Predicate<BlockState> IS_BARRIER = state -> state.is(Blocks.BARRIER);
	private static final Predicate<BlockState> IS_BEDROCK = state -> state.is(Blocks.BEDROCK);

	private static int tickCounter;
	private static boolean barrierWasEnabled;
	private static boolean bedrockWasEnabled;
	private static ClientLevel lastLevel;
	// 当前进行中的分帧扫描；世界切换或功能关闭时直接丢弃。
	private static ScanJob activeJob;
	// 静止判定状态：连续无位移的 tick 数与上一 tick 的玩家位置。
	private static final IdleTracker IDLE_TRACKER = new IdleTracker();

	private BlockMarkerScanner() {
	}

	/** section 预筛只关心的目标方块组合。 */
	enum ScanTarget {
		BARRIER,
		BEDROCK,
		BOTH;

		// 按开关组合选择目标；两个都关闭时没有扫描目标。
		static ScanTarget of(boolean barrier, boolean bedrock) {
			if (barrier && bedrock) {
				return BOTH;
			}
			if (barrier) {
				return BARRIER;
			}
			return bedrock ? BEDROCK : null;
		}
	}

	// section 是否包含目标方块：纯函数，便于覆盖各种开关组合下的跳过逻辑。
	static boolean sectionNeedsScan(boolean hasBarrier, boolean hasBedrock, ScanTarget target) {
		if (target == null) {
			return false;
		}
		return switch (target) {
			case BARRIER -> hasBarrier;
			case BEDROCK -> hasBedrock;
			case BOTH -> hasBarrier || hasBedrock;
		};
	}

	// 扫描是否到期：功能关闭时永不到期；开关/维度变化或从静止转为移动时立即到期；
	// 否则按当前是否静止选择刷新间隔。
	static boolean isScanDue(boolean enabled, boolean toggled, boolean startedMoving,
							 int tickCounter, boolean idle, int activeInterval, int idleInterval) {
		if (!enabled) {
			return false;
		}
		if (toggled || startedMoving) {
			return true;
		}
		return tickCounter % (idle ? idleInterval : activeInterval) == 0;
	}

	// section 索引是否有效：越界（世界上下边界外）或区块根本没有 section 时无需读取。
	static boolean isValidSectionIndex(int index, int sectionCount) {
		return index >= 0 && index < sectionCount;
	}

	// 该层所在 section 是否需要逐方块读取；越界或缺失 section 一律跳过。
	static boolean sectionNeedsScan(LevelChunk chunk, int y, ScanTarget target) {
		int index = chunk.getSectionIndex(y);
		if (!isValidSectionIndex(index, chunk.getSectionsCount())) {
			return false;
		}
		return sectionNeedsScan(chunk.getSection(index), target);
	}

	// 有 section 时才查 palette；没有 section 视为不含目标方块。
	static boolean sectionNeedsScan(LevelChunkSection section, ScanTarget target) {
		if (section == null) {
			return false;
		}
		return sectionNeedsScan(section.maybeHas(IS_BARRIER), section.maybeHas(IS_BEDROCK), target);
	}

	/** 静止判定状态机：位移阈值与 tick 计数在这里维护，与扫描调度解耦。 */
	static final class IdleTracker {
		// 单 tick 位移超过 0.05 格视为移动（用平方值比较，避免开方）。
		static final double MOVE_EPSILON_SQ = 0.05 * 0.05;
		// 连续 40 tick（2 秒）位移不超过 0.05 格才进入静止状态。
		static final int IDLE_TRIGGER_TICKS = 40;

		private double lastX;
		private double lastY;
		private double lastZ;
		private int idleTicks;
		private boolean idle;
		private boolean initialized;

		// 返回 true 表示从静止转为移动，调用方应立即补扫一次。
		boolean update(double x, double y, double z) {
			if (!this.initialized) {
				reset(x, y, z);
				return false;
			}
			double dx = x - this.lastX;
			double dy = y - this.lastY;
			double dz = z - this.lastZ;
			if (dx * dx + dy * dy + dz * dz > MOVE_EPSILON_SQ) {
				this.lastX = x;
				this.lastY = y;
				this.lastZ = z;
				boolean wasIdle = this.idle;
				this.idleTicks = 0;
				this.idle = false;
				return wasIdle;
			}
			if (this.idleTicks < IDLE_TRIGGER_TICKS) {
				this.idleTicks++;
			}
			this.idle = this.idleTicks >= IDLE_TRIGGER_TICKS;
			return false;
		}

		// 以当前位置为基准重新开始计数（换维度/重进世界）。
		void reset(double x, double y, double z) {
			this.lastX = x;
			this.lastY = y;
			this.lastZ = z;
			this.idleTicks = 0;
			this.idle = false;
			this.initialized = true;
		}

		// 玩家/世界不可用：丢弃位置基准，下次 update 重新初始化。
		void clear() {
			this.idleTicks = 0;
			this.idle = false;
			this.initialized = false;
		}

		boolean isIdle() {
			return this.idle;
		}
	}

	static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean available = client.player != null && client.level != null;
		boolean barrier = config != null && config.functions.barrierVisibility && available;
		boolean bedrock = config != null && config.functions.highlightSingleLayerBedrock && available;

		tickCounter++;
		if (tickCounter >= COUNTER_RESET_TICKS) {
			tickCounter = 0;
		}
		// 切换维度/重进世界时清空旧标记并立即重扫，避免在新维度里看到上个维度的坐标。
		// 退回标题界面（level == null）时也要断开引用：静态字段持有 ClientLevel 会连带留住整个客户端世界。
		ClientLevel level = client.level;
		boolean levelChanged = level != null && level != lastLevel;
		if (level == null) {
			if (lastLevel != null) {
				lastLevel = null;
				activeJob = null;
				BarrierVisibilityState.clear();
				BedrockHighlightState.clear();
			}
		} else if (levelChanged) {
			lastLevel = level;
			activeJob = null;
			BarrierVisibilityState.clear();
			BedrockHighlightState.clear();
		}
		// 静止检测：位移阈值与 tick 计数由 IdleTracker 维护；从静止转为移动时立即补扫。
		boolean startedMoving = false;
		boolean idle = false;
		if (available) {
			double playerX = client.player.getX();
			double playerY = client.player.getY();
			double playerZ = client.player.getZ();
			if (levelChanged) {
				// 换维度/重进世界：以当前位置为基准，避免把传送当成移动。
				IDLE_TRACKER.reset(playerX, playerY, playerZ);
			} else {
				startedMoving = IDLE_TRACKER.update(playerX, playerY, playerZ);
			}
			idle = IDLE_TRACKER.isIdle();
		} else {
			IDLE_TRACKER.clear();
		}
		// 单个功能关闭时立即清空它自己的标记：避免残留几何或继续生成粒子。
		if (barrierWasEnabled && !barrier) {
			BarrierVisibilityState.clear();
		}
		if (bedrockWasEnabled && !bedrock) {
			BedrockHighlightState.clear();
		}
		// 开关变化时立即重扫一次：避免开启后等满一个周期才出现。
		boolean toggled = levelChanged || barrier != barrierWasEnabled || bedrock != bedrockWasEnabled;
		barrierWasEnabled = barrier;
		bedrockWasEnabled = bedrock;

		if (!barrier && !bedrock) {
			activeJob = null;
			tickCounter = 0;
			return;
		}

		// 分帧作业跟随当前世界与开关状态：中途失效就丢弃，交给下一次周期重扫。
		if (activeJob != null && (activeJob.level != level
			|| (activeJob.barrier && !barrier) || (activeJob.bedrock && !bedrock))) {
			activeJob = null;
		}
		boolean barrierDue = isScanDue(barrier, toggled, startedMoving, tickCounter,
			idle, BARRIER_INTERVAL_TICKS, IDLE_BARRIER_INTERVAL_TICKS);
		boolean bedrockDue = isScanDue(bedrock, toggled, startedMoving, tickCounter,
			idle, BEDROCK_INTERVAL_TICKS, IDLE_BEDROCK_INTERVAL_TICKS);
		if (activeJob == null && (barrierDue || bedrockDue)) {
			activeJob = new ScanJob(level, client.player.blockPosition(), barrier, bedrock);
		}
		if (activeJob != null && activeJob.step()) {
			activeJob.finish();
			activeJob = null;
		}
		if (barrier) {
			BarrierVisibilityState.tickParticles(client);
		}
	}

	// 一次分帧扫描：从下到上逐层处理 y，扫描期间世界与开关都不可变。
	private static final class ScanJob {
		private final ClientLevel level;
		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final boolean barrier;
		private final boolean bedrock;
		private final List<BlockPos> barriers = new ArrayList<>();
		private final List<BedrockHighlightState.Marker> markers = new ArrayList<>();
		private final int maxBedrockMarkers;
		private int nextOffsetY = -SCAN_RADIUS;
		// 复用同一个可变坐标：整个体积扫描（每 tick 3.2k 次）只在命中时才创建不可变副本，
		// 避免每秒数万次临时对象分配。可变实例不能存进列表，命中处必须 immutable()。
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		// 相邻位置查询同样复用：基岩密集时每层要查上千次上下邻居。
		private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
		// 本层涉及的区块：按扫描半径算出的上界分配，每个扫描任务复用同一数组。
		private final LevelChunk[] layerChunks = new LevelChunk[MAX_CHUNKS_PER_AXIS * MAX_CHUNKS_PER_AXIS];
		// 本层每个区块的 section 是否可能含目标方块；false 时整段跳过方块读取。
		private final boolean[] chunkMayContain = new boolean[MAX_CHUNKS_PER_AXIS * MAX_CHUNKS_PER_AXIS];
		// 目标方块类型：按开关组合决定 section 预筛只关心哪些方块。
		private final ScanTarget target;

		ScanJob(ClientLevel level, BlockPos center, boolean barrier, boolean bedrock) {
			this.level = level;
			this.centerX = center.getX();
			this.centerY = center.getY();
			this.centerZ = center.getZ();
			this.barrier = barrier;
			this.bedrock = bedrock;
			// 至少有一个功能开启才会创建扫描任务。
			this.target = ScanTarget.of(barrier, bedrock);
			this.maxBedrockMarkers = bedrock ? maxBedrockMarkers() : 0;
		}

		// 处理最多 LAYERS_PER_TICK 层，返回是否已完成。
		boolean step() {
			int layers = 0;
			while (layers < LAYERS_PER_TICK && this.nextOffsetY <= SCAN_RADIUS) {
				scanLayer(this.centerY + this.nextOffsetY);
				this.nextOffsetY++;
				layers++;
			}
			return this.nextOffsetY > SCAN_RADIUS;
		}

		// 单层扫描：先预取本层涉及的区块并预检 section，再按原来的 x 主序、z 次序逐方块遍历。
		// 遍历顺序与逐方块 getBlockState 完全一致（标记写入顺序不变），
		// 但区块查找从每层 1089 次降到最多 9 次，palette 不含目标方块的 section 整段跳过。
		private void scanLayer(int y) {
			int minX = this.centerX - SCAN_RADIUS;
			int maxX = this.centerX + SCAN_RADIUS;
			int minZ = this.centerZ - SCAN_RADIUS;
			int maxZ = this.centerZ + SCAN_RADIUS;
			int baseChunkX = minX >> 4;
			int baseChunkZ = minZ >> 4;
			int chunkCountX = (maxX >> 4) - baseChunkX + 1;
			int chunkCountZ = (maxZ >> 4) - baseChunkZ + 1;
			for (int chunkX = 0; chunkX < chunkCountX; chunkX++) {
				for (int chunkZ = 0; chunkZ < chunkCountZ; chunkZ++) {
					int slot = chunkX * chunkCountZ + chunkZ;
					// 未加载区块返回空区块，读取结果是空气，与原版 getBlockState 行为一致。
					LevelChunk chunk = this.level.getChunk(baseChunkX + chunkX, baseChunkZ + chunkZ);
					this.layerChunks[slot] = chunk;
					this.chunkMayContain[slot] = BlockMarkerScanner.sectionNeedsScan(chunk, y, this.target);
				}
			}
			for (int x = minX; x <= maxX; x++) {
				int chunkX = (x >> 4) - baseChunkX;
				for (int z = minZ; z <= maxZ; z++) {
					int slot = chunkX * chunkCountZ + ((z >> 4) - baseChunkZ);
					if (!this.chunkMayContain[slot]) {
						// 该区块这一层的 section palette 不含目标方块：整段跳过方块读取。
						continue;
					}
					LevelChunk chunk = this.layerChunks[slot];
					this.cursor.set(x, y, z);
					BlockState state = chunk.getBlockState(this.cursor);
					if (this.barrier && BarrierVisibilityState.isBarrier(state)) {
						this.barriers.add(this.cursor.immutable());
					}
					if (this.bedrock) {
						// markerAt 内部会对坐标做 immutable()，传入可变实例是安全的。
						BedrockHighlightState.Marker marker = BedrockHighlightState.markerAt(
							this.level, this.cursor, state, this.neighbor);
						if (marker != null) {
							this.markers.add(marker);
						}
					}
				}
			}
		}

		void finish() {
			BlockPos center = new BlockPos(this.centerX, this.centerY, this.centerZ);
			if (this.barrier) {
				BarrierVisibilityState.setMarkers(this.barriers, center);
			}
			if (this.bedrock) {
				BedrockHighlightState.setMarkers(this.markers, center, this.maxBedrockMarkers);
			}
		}
	}

	private static int maxBedrockMarkers() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		int configured = config == null ? TweakifyModConfig.DEFAULT_BEDROCK_MARKERS
			: config.functions.highlightSingleLayerBedrockMaxMarkers;
		return Math.max(TweakifyModConfig.MIN_BEDROCK_MARKERS,
			Math.min(TweakifyModConfig.MAX_BEDROCK_MARKERS, configured));
	}
}
