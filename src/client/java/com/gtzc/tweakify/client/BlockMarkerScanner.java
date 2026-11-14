package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界标记扫描调度：基岩高亮与屏障可见共用一次 33³ 方块遍历。
 *
 * <p>两个功能都开启时，原先每 10 tick 要扫两遍（屏障一遍、基岩一遍）的区域，
 * 现在只在两种扫描同时到期的 tick 合并成一遍，方块读取次数约减少三分之一；
 * 只开一个功能时扫描频率与原来一致。
 *
 * <p>整个体积分帧处理：每 tick 只扫 {@link #LAYERS_PER_TICK} 层 y，
 * 避免一次 3.6 万次方块读取造成的帧时间尖刺。
 */
final class BlockMarkerScanner {
	private static final int SCAN_RADIUS = 16;
	// 屏障每 10 tick（0.5 秒）刷新一次，基岩标记每 20 tick（1 秒）刷新一次。
	private static final int BARRIER_INTERVAL_TICKS = 10;
	private static final int BEDROCK_INTERVAL_TICKS = 20;
	// 每次扫描分帧处理：每 tick 3 层 y（约 3.2k 次方块读取），一次完整扫描约 11 tick。
	private static final int LAYERS_PER_TICK = 3;
	// 单层最多跨几个区块：跨度 2R+1 格，最坏情况下起点落在区块末格，
	// 覆盖的区块数为 floor((跨度+14)/16) + 1（R=16 时为 3）。
	private static final int MAX_CHUNKS_PER_AXIS = (SCAN_RADIUS * 2 + 1 + 14) / 16 + 1;
	// 计数器回到 0 的周期：同时是 10 与 20 的整数倍，避免长时间运行后 int 溢出。
	private static final int COUNTER_RESET_TICKS = 1200;

	private static int tickCounter;
	private static boolean barrierWasEnabled;
	private static boolean bedrockWasEnabled;
	private static ClientLevel lastLevel;
	// 当前进行中的分帧扫描；世界切换或功能关闭时直接丢弃。
	private static ScanJob activeJob;

	private BlockMarkerScanner() {
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
		boolean barrierDue = barrier && (toggled || tickCounter % BARRIER_INTERVAL_TICKS == 0);
		boolean bedrockDue = bedrock && (toggled || tickCounter % BEDROCK_INTERVAL_TICKS == 0);
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

		ScanJob(ClientLevel level, BlockPos center, boolean barrier, boolean bedrock) {
			this.level = level;
			this.centerX = center.getX();
			this.centerY = center.getY();
			this.centerZ = center.getZ();
			this.barrier = barrier;
			this.bedrock = bedrock;
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

		// 单层扫描：先预取本层涉及的区块，再按原来的 x 主序、z 次序逐方块遍历。
		// 遍历顺序与逐方块 getBlockState 完全一致（标记写入顺序不变），
		// 但区块查找从每层 1089 次降到最多 9 次。
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
					// 未加载区块返回空区块，读取结果是空气，与原版 getBlockState 行为一致。
					this.layerChunks[chunkX * chunkCountZ + chunkZ] =
						this.level.getChunk(baseChunkX + chunkX, baseChunkZ + chunkZ);
				}
			}
			for (int x = minX; x <= maxX; x++) {
				int chunkX = (x >> 4) - baseChunkX;
				for (int z = minZ; z <= maxZ; z++) {
					LevelChunk chunk = this.layerChunks[chunkX * chunkCountZ + ((z >> 4) - baseChunkZ)];
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
