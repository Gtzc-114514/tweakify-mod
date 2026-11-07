package com.gtzc.tweakify.client;

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

import java.util.List;

/**
 * 连接方块轮廓：把门、床、双箱、双层植物等由多个方块组成的整体合并成一个轮廓，
 * 准星只命中其中一格时也能像基岩版一样描出整体。
 *
 * <p>合并结果仍以命中方块的坐标为原点：相邻方块的形状按相对偏移平移后与当前形状取并集，
 * 渲染层的 pose 已经平移到该方块，因此不需要修改渲染代码。
 * 门是例外：按 2 格视觉高度描边，而不是原版约 1.5 格的碰撞形状。
 */
public final class ConnectedBlockOutlineState {
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
		if (!isSupported(state)) {
			return null;
		}
		// 门的模型视觉高度是 2 格，不直接使用原版约 1.5 格的碰撞形状。
		if (state.getBlock() instanceof DoorBlock) {
			return mergedDoorShape(state, pos, level);
		}
		VoxelShape merged = state.getShape(level, pos);
		boolean mergedAny = false;
		for (BlockPos other : connectedPositions(state, pos)) {
			BlockState otherState = level.getBlockState(other);
			// 必须“互为另一半”：只比方块类型会把相邻但未连接的同类方块也合并进来。
			if (!isCounterpart(state, otherState)) {
				continue;
			}
			VoxelShape otherShape = otherState.getShape(level, other);
			if (otherShape.isEmpty()) {
				continue;
			}
			merged = Shapes.or(merged, otherShape.move(
				other.getX() - pos.getX(),
				other.getY() - pos.getY(),
				other.getZ() - pos.getZ()));
			mergedAny = true;
		}
		return mergedAny ? merged : null;
	}

	private static BlockPos otherHalf(DoubleBlockHalf half, BlockPos pos) {
		return half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
	}

	// 门：保留原版形状的 x/z 范围，把 y 范围扩到两格（下半 0..2，上半 -1..1）。
	private static VoxelShape mergedDoorShape(BlockState state, BlockPos pos, BlockGetter level) {
		List<BlockPos> others = connectedPositions(state, pos);
		if (others.isEmpty() || !isCounterpart(state, level.getBlockState(others.get(0)))) {
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
}
