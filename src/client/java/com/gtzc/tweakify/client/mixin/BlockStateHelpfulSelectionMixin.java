package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.HelpfulBlockSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 辅助方块选区：在方块状态返回选取形状后按配置放宽，不改变方块碰撞。
 *
 * <p>只替换返回给准星/交互判定的形状；配置关闭、未手持同类方块或潜行时原样返回。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateHelpfulSelectionMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
			+ "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("RETURN"), cancellable = true)
	private void tweakify$helpfulBlockSelection(BlockGetter level, BlockPos pos,
			CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape original = cir.getReturnValue();
		VoxelShape adjusted = HelpfulBlockSelection.adjust(getBlock(), context, original);
		if (adjusted != original) {
			cir.setReturnValue(adjusted);
		}
	}
}
