package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.config.BlockPlacementMode;
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
public final class BetterBlockPlacementRules {
	private BetterBlockPlacementRules() {
	}

	public static boolean canReplacePlant(BlockState state, BlockPlaceContext context) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.betterBlockPlacement || state == null || context == null) {
			return false;
		}
		// 潜行放行：开启时潜行交回原版，先打掉植物再放置。
		if (context.getPlayer() != null && shouldBypassForSneak(
			config.functions.betterBlockPlacementSneakBypass, context.getPlayer().isShiftKeyDown())) {
			return false;
		}
		// 只处理植物类方块；有碰撞的（如甜浆果丛）保持原样，避免误拆交互方块。
		// 26.3 的 BlockStateBase.isSolid() 已标记为 legacy，改用碰撞形状判断。
		if (!(state.getBlock() instanceof VegetationBlock)
			|| !state.getCollisionShape(context.getLevel(), context.getClickedPos()).isEmpty()) {
			return false;
		}
		// 水草、海带等含水植物不处理，避免在水下把整株植物顶掉。
		if (!state.getFluidState().isEmpty()) {
			return false;
		}
		if (!isCoveredByMode(state, BlockPlacementMode.fromCode(config.functions.betterBlockPlacementMode))) {
			return false;
		}
		ItemStack held = context.getItemInHand();
		if (!(held.getItem() instanceof BlockItem)) {
			return false;
		}
		// 手持同种植物时沿用原版行为（放回同种植物而不是覆盖）。
		return !held.is(state.getBlock().asItem());
	}

	// 潜行放行：开启且玩家正在潜行时，不覆盖植物（保持原版交互）。
	public static boolean shouldBypassForSneak(boolean bypassEnabled, boolean sneaking) {
		return bypassEnabled && sneaking;
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
