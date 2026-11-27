package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 经验条与定位栏同时显示（参考 {@code Haage001/locator-heads} 的 alwaysShowXP）：
 * 强制经验条优先，定位栏标记随后叠在它上面。
 */
@Mixin(Hud.class)
public class HudAlwaysShowXpMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "willPrioritizeExperienceInfo", at = @At("RETURN"), cancellable = true)
	private void tweakify$prioritizeExperienceBar(CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.locatorBarHeads
			|| !config.functions.locatorBarAlwaysShowXp) {
			return;
		}
		// 与参考实现一致：乘车时不强制，保持原版跳劈/经验条的优先级。
		if (this.minecraft.level == null || this.minecraft.getCameraEntity() == null
			|| this.minecraft.player == null || this.minecraft.player.isPassenger()) {
			return;
		}
		cir.setReturnValue(true);
	}
}
