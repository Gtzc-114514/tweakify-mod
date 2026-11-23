package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用粘液块减速：原版 {@code stepOn} 每 tick 把水平速度乘上
 * {@code 0.4 + |Δy| * 0.2}，这就是“站上去走不动”的原因。
 *
 * <p>取消整段乘算即可，{@code super.stepOn} 是空实现，不影响任何原版行为。
 */
@Mixin(SlimeBlock.class)
public class SlimeBlockSlowdownMixin {
	@Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableSlowdown(Level level, BlockPos pos, BlockState state, Entity entity,
										  CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.disableSlimeBlockSlowdown
			&& (!config.modified.disableSlimeBlockSlowdownPlayersOnly || entity instanceof Player)) {
			ci.cancel();
		}
	}
}
