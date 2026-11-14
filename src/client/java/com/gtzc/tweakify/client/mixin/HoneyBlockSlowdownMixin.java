package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HoneyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用蜂蜜块减速：蜂蜜块本身没有覆写速度系数，靠方块属性里的 0.4 让
 * {@code Entity.getBlockSpeedFactor} 降速。
 *
 * <p>这里只对蜂蜜块把系数还原成 1.0，灵魂沙等其它减速方块不受影响。
 */
@Mixin(Block.class)
public class HoneyBlockSlowdownMixin {
	@Inject(method = "getSpeedFactor", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableSlowdown(CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof HoneyBlock)) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.disableHoneyBlockSlowdown) {
			cir.setReturnValue(1.0F);
		}
	}
}
