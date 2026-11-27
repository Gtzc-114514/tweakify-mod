package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用蜂蜜块黏着：{@code entityInside} 在贴墙下滑时把下落速度夹到很低的固定值，
 * 并周期性地播放下滑粒子，这就是“被蜂蜜粘住”的手感。
 *
 * <p>取消整段逻辑后贴墙会正常下落；减速（速度系数）由单独的开关控制，
 * 两个子设置互不牵连。
 */
@Mixin(HoneyBlock.class)
public class HoneyBlockStickMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableStick(BlockState state, Level level, BlockPos pos, Entity entity,
									   InsideBlockEffectApplier applier, boolean bl, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config != null && config.modified.disableHoneyBlockSlowdown
			&& config.modified.disableHoneyBlockStick) {
			ci.cancel();
		}
	}
}
