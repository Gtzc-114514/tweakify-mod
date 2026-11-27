package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CooldownIndicatorState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-268420：高攻速下切换物品时，准星旁攻击冷却指示器会闪一下。
 *
 * <p>26.3 在 extractCrosshair 中先画指示器背景，再用攻击强度乘 17 作为宽度
 * 画进度精灵；强度为 0 时进度宽度为 0，内部会直接返回，但背景条仍会绘制。
 * 这里只跳过 4 参数 blitSprite 的背景调用（准星、FULL、背景中的第 3 次），
 * 准星与其它指示器元素保持原版。
 */
@Mixin(Hud.class)
public abstract class HudCooldownIndicatorMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$COOLDOWN_INDICATOR_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapWithCondition(method = "extractCrosshair", require = 0,
		at = @At(value = "INVOKE", ordinal = 2,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite("
				+ "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"
				+ "Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean tweakify$skipEmptyCooldownBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
			Identifier sprite, int x, int y, int width, int height) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config != null && config.fix.cooldownIndicatorFix;
		if (!enabled || this.minecraft.player == null) {
			return true;
		}
		HookProbe.markOnce(TWEAKIFY$COOLDOWN_INDICATOR_PROBE, "cooldownIndicatorFix");
		return CooldownIndicatorState.shouldDrawProgress(true,
			config.fix.cooldownIndicatorFixMinProgress,
			this.minecraft.player.getAttackStrengthScale(0.0F));
	}
}
