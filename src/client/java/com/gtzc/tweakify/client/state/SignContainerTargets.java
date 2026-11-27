package com.gtzc.tweakify.client.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 穿牌打开容器：准星对着告示牌时，把交互转给告示牌背后的容器。
 *
 * <p>纯逻辑独立在这里，Mixin 只负责发起交互：候选位置按“命中面的反方向优先、
 * 其余方向随后”排序，因此贴在箱子正面的告示牌总能命中背后的箱子，
 * 而立在箱子旁边的告示牌也能在整圈邻位里找到它。
 */
public final class SignContainerTargets {
	// 全部六个方向：命中面的反方向排在最前，其余按枚举顺序补齐。
	private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

	private SignContainerTargets() {
	}

	// 只有“非潜行 + 对准告示牌”才改写交互：潜行时保留告示牌的原版编辑行为。
	// 兼容旧调用：默认开启潜行放行。
	public static boolean shouldOpenContainer(boolean enabled, boolean sign, boolean sneaking) {
		return shouldOpenContainer(enabled, true, sign, sneaking);
	}

	// 潜行放行开关：开启时潜行保留告示牌原版行为；关闭后潜行也会穿牌开容器。
	public static boolean shouldOpenContainer(boolean enabled, boolean sneakBypass, boolean sign, boolean sneaking) {
		return enabled && sign && !(sneakBypass && sneaking);
	}

	public static boolean isSign(BlockState state) {
		return state != null && state.getBlock() instanceof SignBlock;
	}

	// 候选容器位置：先看命中面反方向（告示牌贴在容器上的那一侧），再依次看其余方向。
	public static List<BlockPos> candidatePositions(BlockPos signPos, Direction hitFace) {
		List<BlockPos> positions = new ArrayList<>(6);
		if (hitFace != null) {
			positions.add(signPos.relative(hitFace.getOpposite()));
		}
		for (Direction direction : ALL_DIRECTIONS) {
			BlockPos candidate = signPos.relative(direction);
			if (!positions.contains(candidate)) {
				positions.add(candidate);
			}
		}
		return positions;
	}

	// 在候选位置里找第一个带容器方块实体的方块；找不到时返回 null（保持原版告示牌行为）。
	public static BlockPos findContainer(Level level, BlockPos signPos, Direction hitFace) {
		if (level == null) {
			return null;
		}
		for (BlockPos candidate : candidatePositions(signPos, hitFace)) {
			if (!level.isLoaded(candidate)) {
				continue;
			}
			if (level.getBlockEntity(candidate) instanceof Container) {
				return candidate;
			}
		}
		return null;
	}
}
