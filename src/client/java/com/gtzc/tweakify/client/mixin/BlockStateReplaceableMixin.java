package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.BetterBlockPlacementState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 更好的放置覆盖：在方块状态判断“能否被放置替换”时补上花、高草等植物，
 * 让 BlockPlaceContext 把点击位置当作可替换位置，放置时直接顶掉植物。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateReplaceableMixin {
	@ModifyReturnValue(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
		at = @At("RETURN"))
	private boolean tweakify$allowPlantReplacement(boolean original, BlockPlaceContext context) {
		if (BetterBlockPlacementState.canReplacePlant((BlockState) (Object) this, context)) {
			return true;
		}
		return original;
	}
}
