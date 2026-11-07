package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用耕地踩踏：跳过 fallOn 里把耕地转回泥土的调用，
 * 落地伤害等其它逻辑仍走原版 super.fallOn。
 */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {
	@Redirect(method = "fallOn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;"
			+ "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
			+ "Lnet/minecraft/core/BlockPos;)V"))
	private void tweakify$disableTrampling(FarmlandBlock self, Entity entity, BlockState state,
										   Level level, BlockPos pos) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.disableFarmlandTrampling
			&& (!config.farmlandTramplingPlayerOnly || entity instanceof Player)) {
			return;
		}
		self.turnToBaseBlock(entity, state, level, pos);
	}
}
