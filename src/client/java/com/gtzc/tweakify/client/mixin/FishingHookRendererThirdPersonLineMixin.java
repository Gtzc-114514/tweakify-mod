package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.FishingLineState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-4490：潜行时第三人称鱼线没有连接到鱼竿。
 *
 * <p>26.3 的 getPlayerHandPos 在潜行分支使用 -0.1875 的手部高度偏移，
 * 第三人称下鱼线会悬在鱼竿上方。这里只调整该常量，关闭时原样返回。
 * 第一人称路径不使用这个常量，因此不受影响。
 */
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererThirdPersonLineMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$FISHING_LINE_PROBE = new AtomicBoolean();

	@ModifyExpressionValue(method = "getPlayerHandPos",
		at = @At(value = "CONSTANT", args = "floatValue=-0.1875"),
		require = 0)
	private float tweakify$attachFishingLineWhileCrouching(float original) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config.fix.fishingLineThirdPersonFix;
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$FISHING_LINE_PROBE, "fishingLineThirdPersonFix");
		}
		double offset = config.fix.fishingLineThirdPersonFixOffset;
		return FishingLineState.crouchOffset(enabled, offset, original);
	}
}
