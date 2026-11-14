package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用蛛网减速：蜘蛛网的 {@code entityInside} 只做一件事——
 * 调用 {@code Entity.makeStuckInBlock} 把移动速度与跳跃压到极低。
 *
 * <p>整段取消即可解除黏液（包括织网效果加持时的更慢版本），
 * 蜘蛛网的渲染、放置与破坏完全不受影响。
 */
@Mixin(WebBlock.class)
public class CobwebSlowdownMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableCobwebSlowdown(BlockState state, Level level, BlockPos pos, Entity entity,
												InsideBlockEffectApplier applier, boolean bl, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.disableCobwebSlowdown) {
			return;
		}
		// 默认只解除玩家减速：怪物、掉落物仍按原版被蛛网拖住。
		if (entity instanceof Player || config.modified.cobwebSlowdownAllEntities) {
			ci.cancel();
		}
	}
}
