package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.RefreshScheduler;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 世界标记扫描调度：基岩高亮与屏障可见共用一次以两者较大半径为边长的方块遍历
 * （两者半径分别由 barrierVisibilityScanRadius / highlightSingleLayerBedrockScanRadius 控制）。
 *
 * <p>刷新频率：每秒 4 次（5 tick），由 {@link RefreshScheduler} 累计调度，
 * 开关或世界变化时立即补扫一次。
 *
 * <p>整个体积分帧处理：每 tick 推进 {@link #LAYERS_PER_TICK} 层 y，
 * 默认半径 24 时 49 层在 3 tick 内完成一轮，因此每次刷新窗口都能拿到完整结果；
 * 单层最多 49×49 次读取，一次刷新约 11.8 万次位置遍历（section 预筛会跳过大部分读取）。
 *
 * <p>section 预筛：逐层先查区块的 section palette 是否可能包含目标方块，
 * 不含（如纯石头/空气/越界层）则整段跳过，不再逐方块读取。
 */
public final class BlockMarkerScanner {
	// 可见半径上限：两种功能的扫描半径都可配置（8 ~ 32），数组按上限分配，
	// 因此调小半径不会越界，也不会超出容量。
	private static final int MAX_SCAN_RADIUS = 32;
	// 刷新频率：每秒 4 次（5 tick 一次）。
	// 包私有：单测需要把频率数值与需求固定下来。
	static final int REFRESHES_PER_SECOND = 4;
	private static final RefreshScheduler REFRESH = new RefreshScheduler(REFRESHES_PER_SECOND);
	// 分帧层数：默认半径 24 时 49 层 / 22 ≈ 3 tick 完成一轮，与 5 tick 的刷新窗口对齐。
	private static final int LAYERS_PER_TICK = 22;
	// 单层最多跨几个区块：跨度 2R+1 格，最坏情况下起点落在区块末格，
	// 覆盖的区块数为 floor((跨度+14)/16) + 1（R=32 时为 5）。
	private static final int MAX_CHUNKS_PER_AXIS = (MAX_SCAN_RADIUS * 2 + 1 + 14) / 16 + 1;

	// 目标方块谓词：section 的 palette 完全不包含它们时整段跳过读取。
	private static final Predicate<BlockState> IS_BARRIER = state -> state.is(Blocks.BARRIER);
	private static final Predicate<BlockState> IS_BEDROCK = state -> state.is(Blocks.BEDROCK);

	private static boolean barrierWasEnabled;
	private static boolean bedrockWasEnabled;
	private static ClientLevel lastLevel;
	// 当前进行中的分帧扫描；世界切换或功能关闭时直接丢弃。
	private static ScanJob activeJob;

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

	// 按开关组合选出的扫描参数；独立成可测纯函数，避免 createJob 的三目接线被写反。
	record ScanParams(int barrierRadius, int bedrockRadius, int maxBedrockMarkers) {
	}

	static ScanParams scanParams(boolean barrier, boolean bedrock, int barrierRadius,
			int bedrockRadius, int maxBedrockMarkers) {
		return new ScanParams(barrier ? barrierRadius : 0,
			bedrock ? bedrockRadius : 0,
			bedrock ? maxBedrockMarkers : 0);
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

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean available = client.player != null && client.level != null;
		boolean barrier = config.functions.barrierVisibility && available;
		boolean bedrock = config.functions.highlightSingleLayerBedrock && available;

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
			REFRESH.reset();
			return;
		}

		// 分帧作业跟随当前世界与开关状态：中途失效就丢弃，交给下一次周期重扫。
		if (activeJob != null && (activeJob.level != level
			|| (activeJob.barrier && !barrier) || (activeJob.bedrock && !bedrock))) {
			activeJob = null;
		}
		// 每 tick 都推进刷新额度；窗口到期且没有进行中的扫描时才启动新一轮。
		boolean refreshDue = REFRESH.tick();
		if (activeJob == null && (toggled || refreshDue)) {
			activeJob = createJob(level, client.player.blockPosition(), barrier, bedrock);
		}
		if (activeJob != null && activeJob.step()) {
			activeJob.finish();
			activeJob = null;
		}
		if (barrier) {
			BarrierVisibilityState.tickParticles(client);
		}
	}

	// 按当前配置创建扫描任务；ScanJob 本身只依赖 Level，便于真实 LevelChunk GameTest 复用。
	private static ScanJob createJob(ClientLevel level, BlockPos center, boolean barrier, boolean bedrock) {
		ScanParams params = scanParams(barrier, bedrock, barrierScanRadius(), bedrockScanRadius(),
			maxBedrockMarkers());
		return new ScanJob(level, center, barrier, bedrock,
			params.barrierRadius(), params.bedrockRadius(), params.maxBedrockMarkers());
	}

	// 一次分帧扫描：从下到上逐层处理 y，扫描期间世界与开关都不可变。
	// 包私有：GameTest 用真实 ServerLevel/LevelChunk 直接驱动它，验证 section 预筛接线。
	static final class ScanJob {
		private final Level level;
		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final boolean barrier;
		private final boolean bedrock;
		private final List<BlockPos> barriers = new ArrayList<>();
		private final List<BedrockHighlightState.Marker> markers = new ArrayList<>();
		private final int maxBedrockMarkers;
		// 本任务的扫描盒半径（取启用功能的最大半径）与各自功能的过滤半径。
		private final int radius;
		private final int barrierRadius;
		private final int bedrockRadius;
		private int nextOffsetY;
		// 复用同一个可变坐标：整个体积扫描（每 tick 最多 9.3 万格）只在命中时才创建不可变副本，
		// 避免每秒数十万次临时对象分配。可变实例不能存进列表，命中处必须 immutable()。
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		// 相邻位置查询同样复用：基岩密集时每层要查上千次上下邻居。
		private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
		// 本层涉及的区块：按扫描半径算出的上界分配，每个扫描任务复用同一数组。
		private final LevelChunk[] layerChunks = new LevelChunk[MAX_CHUNKS_PER_AXIS * MAX_CHUNKS_PER_AXIS];
		// 本层每个区块的 section 是否可能含目标方块；false 时整段跳过方块读取。
		private final boolean[] chunkMayContain = new boolean[MAX_CHUNKS_PER_AXIS * MAX_CHUNKS_PER_AXIS];
		// 目标方块类型：按开关组合决定 section 预筛只关心哪些方块。
		private final ScanTarget target;
		// 实际逐方块读取次数：GameTest 用它判别 section 预筛是否真的跳过了非目标区块。
		private int blockReads;
		// 实际读取过的 section：仅 GameTest 开启时分配，生产路径不产生集合/键对象。
		private final boolean recordDiagnostics;
		private final Set<SectionKey> sectionsRead;

		// 目标 section 的稳定标识；仅用于 GameTest 行为断言。
		record SectionKey(int chunkX, int chunkZ, int sectionIndex) {
		}

		ScanJob(Level level, BlockPos center, boolean barrier, boolean bedrock,
				int barrierRadius, int bedrockRadius, int maxBedrockMarkers) {
			this(level, center, barrier, bedrock, barrierRadius, bedrockRadius, maxBedrockMarkers, false);
		}

		// GameTest 专用重载：开启诊断集合记录；生产 createJob 使用上面的 7 参构造。
		ScanJob(Level level, BlockPos center, boolean barrier, boolean bedrock,
				int barrierRadius, int bedrockRadius, int maxBedrockMarkers, boolean recordDiagnostics) {
			this.recordDiagnostics = recordDiagnostics;
			this.sectionsRead = recordDiagnostics ? new HashSet<>() : null;
			this.level = level;
			this.centerX = center.getX();
			this.centerY = center.getY();
			this.centerZ = center.getZ();
			this.barrier = barrier;
			this.bedrock = bedrock;
			// 至少有一个功能开启才会创建扫描任务。
			this.target = ScanTarget.of(barrier, bedrock);
			this.maxBedrockMarkers = maxBedrockMarkers;
			this.barrierRadius = barrierRadius;
			this.bedrockRadius = bedrockRadius;
			// 两种功能共用一次扫描：盒子取较大半径，各自再按自身半径过滤。
			this.radius = Math.max(this.barrierRadius, this.bedrockRadius);
			this.nextOffsetY = -this.radius;
		}

		// 处理最多 LAYERS_PER_TICK 层，返回是否已完成。
		boolean step() {
			int layers = 0;
			while (layers < LAYERS_PER_TICK && this.nextOffsetY <= this.radius) {
				scanLayer(this.centerY + this.nextOffsetY);
				this.nextOffsetY++;
				layers++;
			}
			return this.nextOffsetY > this.radius;
		}

		// 单层扫描：先预取本层涉及的区块并预检 section，再按原来的 x 主序、z 次序逐方块遍历。
		// 遍历顺序与逐方块 getBlockState 完全一致（标记写入顺序不变），
		// 但区块查找从每层 4225 次降到最多 25 次，palette 不含目标方块的 section 整段跳过。
		private void scanLayer(int y) {
			int minX = this.centerX - this.radius;
			int maxX = this.centerX + this.radius;
			int minZ = this.centerZ - this.radius;
			int maxZ = this.centerZ + this.radius;
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
					boolean mayContain = BlockMarkerScanner.sectionNeedsScan(chunk, y, this.target);
					this.chunkMayContain[slot] = mayContain;
					if (mayContain && this.recordDiagnostics) {
						this.sectionsRead.add(new SectionKey(baseChunkX + chunkX, baseChunkZ + chunkZ,
							chunk.getSectionIndex(y)));
					}
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
					this.blockReads++;
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
				BarrierVisibilityState.setMarkers(this.barriers, center, maxBarrierMarkers(), this.barrierRadius);
			}
			if (this.bedrock) {
				BedrockHighlightState.setMarkers(this.markers, center, this.maxBedrockMarkers, this.bedrockRadius);
			}
		}

		// 以下四个包私有访问器仅供真实 LevelChunk GameTest 使用。
		int blockReadsForTest() {
			return this.blockReads;
		}

		Set<SectionKey> sectionsReadForTest() {
			return this.sectionsRead == null ? Set.of() : Set.copyOf(this.sectionsRead);
		}

		List<BlockPos> barriersForTest() {
			return List.copyOf(this.barriers);
		}

		List<BedrockHighlightState.Marker> markersForTest() {
			return List.copyOf(this.markers);
		}
	}

	private static int maxBarrierMarkers() {
		TweakifyModConfig config = TweakifyMod.config();
		int configured = config.functions.barrierVisibilityMaxMarkers;
		return Math.max(TweakifyModConfig.MIN_BARRIER_MARKERS,
			Math.min(TweakifyModConfig.MAX_BARRIER_MARKERS, configured));
	}

	private static int maxBedrockMarkers() {
		TweakifyModConfig config = TweakifyMod.config();
		int configured = config.functions.highlightSingleLayerBedrockMaxMarkers;
		return Math.max(TweakifyModConfig.MIN_BEDROCK_MARKERS,
			Math.min(TweakifyModConfig.MAX_BEDROCK_MARKERS, configured));
	}

	// 屏障扫描半径：配置缺失或越界时夹紧到允许范围。
	private static int barrierScanRadius() {
		TweakifyModConfig config = TweakifyMod.config();
		int configured = config.functions.barrierVisibilityScanRadius;
		return Math.max(TweakifyModConfig.MIN_BARRIER_SCAN_RADIUS,
			Math.min(TweakifyModConfig.MAX_BARRIER_SCAN_RADIUS, configured));
	}

	// 基岩扫描半径：配置缺失或越界时夹紧到允许范围。
	private static int bedrockScanRadius() {
		TweakifyModConfig config = TweakifyMod.config();
		int configured = config.functions.highlightSingleLayerBedrockScanRadius;
		return Math.max(TweakifyModConfig.MIN_BEDROCK_SCAN_RADIUS,
			Math.min(TweakifyModConfig.MAX_BEDROCK_SCAN_RADIUS, configured));
	}
}
