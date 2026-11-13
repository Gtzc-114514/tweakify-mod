package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用地狱门特效（音效部分）：进入下界传送门时跳过 PORTAL_TRIGGER 环境音。
 * 覆盖层与镜头旋转分别由 HudOverlayMixin、LevelExtractorOverlayMixin 处理。
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerPortalEffectMixin {
	@Redirect(method = "handlePortalTransitionEffect", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"))
	private SoundEngine.PlayResult tweakify$mutePortalSound(SoundManager soundManager, SoundInstance sound) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.disableNetherPortalEffect && config.modified.disableNetherPortalSound) {
			return SoundEngine.PlayResult.NOT_STARTED;
		}
		return soundManager.play(sound);
	}
}
