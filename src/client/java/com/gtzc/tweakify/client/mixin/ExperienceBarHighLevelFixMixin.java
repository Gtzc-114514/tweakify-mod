package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ExperienceBarState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-79545：等级过高时经验条消失。
 *
 * <p>等级极高时 {@code getXpNeededForNextLevel()} 溢出为负数，原版用
 * {@code > 0} 判断是否绘制经验条，整条背景与进度条都会被跳过。
 * 这里只改写这一个调用的返回值，不影响经验等级文字或其它经验计算。
 */
@Mixin(ExperienceBar.class)
public abstract class ExperienceBarHighLevelFixMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$EXPERIENCE_BAR_HIGH_LEVEL_PROBE = new AtomicBoolean();

	@ModifyExpressionValue(method = "extractBackground",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;getXpNeededForNextLevel()I"),
		require = 0)
	private int tweakify$keepExperienceBarVisible(int original) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config.fix.experienceBarHighLevelFix;
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$EXPERIENCE_BAR_HIGH_LEVEL_PROBE, "experienceBarHighLevelFix");
		}
		int minLevel = config.fix.experienceBarHighLevelFixMinLevel;
		Minecraft client = Minecraft.getInstance();
		int currentLevel = client.player == null ? 0 : client.player.experienceLevel;
		return ExperienceBarState.xpNeededForNextLevel(enabled, minLevel, currentLevel, original);
	}
}
