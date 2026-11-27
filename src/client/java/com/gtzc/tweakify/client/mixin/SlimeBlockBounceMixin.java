package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlimeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用粘液块弹跳：26.3 的弹跳由方块弹性系数（{@code bounceRestitution}）驱动，
 * {@code Entity} 落地时按它换算垂直速度。
 *
 * <p>只对粘液块把弹性系数改成 0，其它方块（床、蜂蜜块等）的系数保持原值。
 */
@Mixin(Block.class)
public class SlimeBlockBounceMixin {
	@Inject(method = "getBounceRestitution", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableBounce(CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof SlimeBlock)) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (config != null && config.modified.disableSlimeBlockSlowdown
			&& config.modified.disableSlimeBlockBounce) {
			cir.setReturnValue(0.0F);
		}
	}
}
