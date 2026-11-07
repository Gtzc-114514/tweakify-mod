package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 更好的放置覆盖：手持方块物品右键时，允许直接替换花、高草等植物。
 *
 * <p>原版只有部分植物（高草、蕨等）标记了 replaceable，花类不在此列。
 * 这里按配置的覆盖范围把植物视为可替换，保持与原版放置流程一致。
 */
public final class BetterBlockPlacementState {
	private BetterBlockPlacementState() {
	}

	public static boolean canReplacePlant(BlockState state, BlockPlaceContext context) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.betterBlockPlacement || state == null || context == null) {
			return false;
		}
		// 只处理植物类方块；有碰撞的（如甜浆果丛）保持原样，避免误拆交互方块。
		if (!(state.getBlock() instanceof VegetationBlock) || state.isSolid()) {
			return false;
		}
		// 水草、海带等水logged 植物不处理，避免在水下把整株植物顶掉。
		if (!state.getFluidState().isEmpty()) {
			return false;
		}
		if (!isCoveredByMode(state, BlockPlacementMode.fromCode(config.betterBlockPlacementMode))) {
			return false;
		}
		ItemStack held = context.getItemInHand();
		if (!(held.getItem() instanceof BlockItem)) {
			return false;
		}
		// 手持同种植物时沿用原版行为（放回同种植物而不是覆盖）。
		return !held.is(state.getBlock().asItem());
	}

	// 覆盖范围：花草（花 + 高草/蕨/灌木）/ 额外包含作物 / 全部无碰撞植物。
	private static boolean isCoveredByMode(BlockState state, BlockPlacementMode mode) {
		boolean flowerOrGrass = state.is(BlockTags.FLOWERS)
			|| state.is(BlockTags.SAPLINGS)
			|| state.is(BlockTags.REPLACEABLE_BY_TREES);
		return switch (mode) {
			case FLOWERS -> flowerOrGrass;
			case CROPS -> flowerOrGrass || state.is(BlockTags.CROPS);
			case ALL -> true;
		};
	}
}
