package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 连接方块轮廓：把门、床、双箱、双层植物等由多个方块组成的整体合并成一个轮廓，
 * 准星只命中其中一格时也能像基岩版一样描出整体。
 *
 * <p>合并结果仍以命中方块的坐标为原点：相邻方块的形状按相对偏移平移后与当前形状取并集，
 * 渲染层的 pose 已经平移到该方块，因此不需要修改渲染代码。
 * 门是例外：按 2 格视觉高度描边，而不是原版约 1.5 格的碰撞形状。
 */
public final class ConnectedBlockOutlineState {
	// 合并形状缓存：键包含准星方块、邻居状态、世界/玩家身份与门高度配置；
	// 小 LRU 足够（同一时刻只有一个准星目标），不持有世界或玩家引用。
	private static final MergedShapeCache CACHE = new MergedShapeCache();

	private ConnectedBlockOutlineState() {
	}

	// 当前方块是否属于需要合并的“连接方块”。
	public static boolean isSupported(BlockState state) {
		if (state == null) {
			return false;
		}
		return state.getBlock() instanceof DoorBlock
			|| state.getBlock() instanceof AbstractBedBlock
			|| state.getBlock() instanceof ChestBlock
			|| state.getBlock() instanceof DoublePlantBlock;
	}

	// 与当前方块构成整体的相邻位置；不查世界，只依据方块状态。
	public static List<BlockPos> connectedPositions(BlockState state, BlockPos pos) {
		if (!isSupported(state)) {
			return List.of();
		}
		if (state.getBlock() instanceof DoorBlock) {
			return List.of(otherHalf(state.getValue(DoorBlock.HALF), pos));
		}
		if (state.getBlock() instanceof AbstractBedBlock) {
			return List.of(pos.relative(AbstractBedBlock.getConnectedDirection(state)));
		}
		if (state.getBlock() instanceof ChestBlock) {
			return state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
				? List.of()
				: List.of(ChestBlock.getConnectedBlockPos(pos, state));
		}
		if (state.getBlock() instanceof DoublePlantBlock) {
			return List.of(otherHalf(state.getValue(DoublePlantBlock.HALF), pos));
		}
		return List.of();
	}

	// 合并后的轮廓；没有可合并的相邻方块时返回 null，调用方保留原轮廓。
	public static VoxelShape mergedShape(BlockState state, BlockPos pos, BlockGetter level) {
		return mergedShape(state, pos, level, 0);
	}

	// 重载：playerIdentity 由调用方传 System.identityHashCode(player)，避免缓存持有玩家引用。
	// 该身份是预留维度：合并形状只由方块状态决定，不随玩家变化；保留它便于未来按玩家隔离
	// 缓存生命周期，代价是换玩家时多一次构建，但形状结果不变。
	public static VoxelShape mergedShape(BlockState state, BlockPos pos, BlockGetter level,
			int playerIdentity) {
		if (!isSupported(state)) {
			return null;
		}
		BlockPos immutablePos = pos.immutable();
		List<Neighbor> neighbors = neighbors(state, immutablePos, level);
		boolean doorFullHeight = doorFullHeightEnabled();
		CacheKey key = new CacheKey(System.identityHashCode(level), playerIdentity, immutablePos,
			state, neighbors, doorFullHeight);
		return CACHE.get(key, () -> computeMergedShape(state, immutablePos, level, neighbors,
			doorFullHeight));
	}

	// 先读取邻居状态：它既参与缓存键（方块更新时失效），也供重建直接复用。
	private static List<Neighbor> neighbors(BlockState state, BlockPos pos, BlockGetter level) {
		List<BlockPos> positions = connectedPositions(state, pos);
		if (positions.isEmpty()) {
			return List.of();
		}
		List<Neighbor> neighbors = new ArrayList<>(positions.size());
		for (BlockPos other : positions) {
			BlockPos immutable = other.immutable();
			neighbors.add(new Neighbor(immutable, level.getBlockState(immutable)));
		}
		return List.copyOf(neighbors);
	}

	// 无缓存重建：与原实现等价，只是邻居状态由调用方传入。
	private static VoxelShape computeMergedShape(BlockState state, BlockPos pos, BlockGetter level,
			List<Neighbor> neighbors, boolean doorFullHeight) {
		// 门的模型视觉高度是 2 格，不直接使用原版约 1.5 格的碰撞形状。
		if (state.getBlock() instanceof DoorBlock) {
			return mergedDoorShape(state, pos, level, neighbors, doorFullHeight);
		}
		VoxelShape merged = state.getShape(level, pos);
		boolean mergedAny = false;
		for (Neighbor neighbor : neighbors) {
			BlockState otherState = neighbor.state();
			// 必须“互为另一半”：只比方块类型会把相邻但未连接的同类方块也合并进来。
			if (!isCounterpart(state, otherState)) {
				continue;
			}
			VoxelShape otherShape = otherState.getShape(level, neighbor.pos());
			if (otherShape.isEmpty()) {
				continue;
			}
			merged = Shapes.or(merged, otherShape.move(
				neighbor.pos().getX() - pos.getX(),
				neighbor.pos().getY() - pos.getY(),
				neighbor.pos().getZ() - pos.getZ()));
			mergedAny = true;
		}
		return mergedAny ? merged : null;
	}

	private static BlockPos otherHalf(DoubleBlockHalf half, BlockPos pos) {
		return half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
	}

	// 配置缺失时保持 2 格视觉高度（旧行为）。
	private static boolean doorFullHeightEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null || config.modified.connectedBlockOutlineDoorFullHeight;
	}

	// 门：保留原版形状的 x/z 范围，把 y 范围扩到两格（下半 0..2，上半 -1..1）。
	// 子设置关闭时返回 null，门保持原版轮廓。
	private static VoxelShape mergedDoorShape(BlockState state, BlockPos pos, BlockGetter level,
			List<Neighbor> neighbors, boolean doorFullHeight) {
		if (!doorFullHeight) {
			return null;
		}
		if (neighbors.isEmpty() || !isCounterpart(state, neighbors.get(0).state())) {
			return null;
		}
		AABB base = state.getShape(level, pos).bounds();
		double minY = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? 0.0 : -1.0;
		return Shapes.box(base.minX, minY, base.minZ, base.maxX, minY + 2.0, base.maxZ);
	}

	// 两个状态是否互为另一半：方块相同之外，还要求连接属性互补、朝向一致。
	private static boolean isCounterpart(BlockState state, BlockState other) {
		if (other.getBlock() != state.getBlock()) {
			return false;
		}
		if (state.getBlock() instanceof DoorBlock) {
			return other.getValue(DoorBlock.HALF) != state.getValue(DoorBlock.HALF)
				&& other.getValue(DoorBlock.FACING) == state.getValue(DoorBlock.FACING);
		}
		if (state.getBlock() instanceof DoublePlantBlock) {
			return other.getValue(DoublePlantBlock.HALF) != state.getValue(DoublePlantBlock.HALF);
		}
		if (state.getBlock() instanceof AbstractBedBlock) {
			return other.getValue(AbstractBedBlock.PART) != state.getValue(AbstractBedBlock.PART)
				&& other.getValue(AbstractBedBlock.FACING) == state.getValue(AbstractBedBlock.FACING);
		}
		if (state.getBlock() instanceof ChestBlock) {
			ChestType type = state.getValue(ChestBlock.TYPE);
			if (type == ChestType.SINGLE) {
				return false;
			}
			ChestType otherType = other.getValue(ChestBlock.TYPE);
			// LEFT 只能配 RIGHT，RIGHT 只能配 LEFT。
			return otherType != ChestType.SINGLE && otherType != type
				&& other.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING);
		}
		return false;
	}

	// 包私有：测试之间清空缓存，避免跨用例互相影响。
	static void resetCacheForTest() {
		CACHE.clear();
	}

	private record Neighbor(BlockPos pos, BlockState state) {
	}

	// playerIdentity 为预留维度：不参与合并形状结果，只影响缓存命中/生命周期。
	private record CacheKey(int levelIdentity, int playerIdentity, BlockPos pos, BlockState state,
			List<Neighbor> neighbors, boolean doorFullHeight) {
	}

	// 小型 LRU：同一时刻只有一个准星目标，8 条足够覆盖快速切换；不持有世界/玩家引用。
	// 仅客户端渲染线程访问（准星轮廓提取与渲染同线程）：不做同步。
	private static final class MergedShapeCache {
		private static final int MAX_ENTRIES = 8;
		private final Map<CacheKey, CachedShape> entries =
			new LinkedHashMap<>(MAX_ENTRIES + 1, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<CacheKey, CachedShape> eldest) {
					return size() > MAX_ENTRIES;
				}
			};

		VoxelShape get(CacheKey key, Supplier<VoxelShape> builder) {
			CachedShape cached = this.entries.get(key);
			if (cached != null) {
				return cached.shape();
			}
			VoxelShape built = builder.get();
			this.entries.put(key, new CachedShape(built));
			return built;
		}

		void clear() {
			this.entries.clear();
		}

		// null 也是有效结果（没有可合并的邻居），用包装区分“未缓存”。
		private record CachedShape(VoxelShape shape) {
		}
	}
}
