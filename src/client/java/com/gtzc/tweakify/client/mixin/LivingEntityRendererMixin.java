package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.HurtTintState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// 更好的隐身：隐身时连装备、手持物品、披风等附加层一起隐藏（原版只隐藏身体）。
	@ModifyReturnValue(method = "shouldRenderLayers", at = @At("RETURN"))
	private boolean tweakify$hideLayersWhenInvisible(boolean original, LivingEntityRenderState state) {
		if (TweakifyMod.config() != null && TweakifyMod.config().modified.betterInvisibility
			&& TweakifyMod.config().modified.betterInvisibilityLayers && state.isInvisible) {
			return false;
		}
		return original;
	}

	// hasRedOverlay 同时覆盖受伤（hurtTime > 0）与死亡（deathTime > 0），
	// 这里只去掉受伤部分：死亡后的红色 overlay 保持原版。
	@ModifyReturnValue(method = "getOverlayCoords", at = @At("RETURN"))
	private static int tweakify$disableHurtTint(int original, LivingEntityRenderState state,
			float whiteOverlayProgress) {
		if (TweakifyMod.config() == null || !TweakifyMod.config().modified.disableHurtTint
			|| !state.hasRedOverlay || state.deathTime > 0.0f) {
			return original;
		}
		if (TweakifyMod.config().modified.hurtTintPlayersOnly
			&& state.entityType != EntityTypes.PLAYER) {
			return original;
		}
		// 保留白色 overlay 进度（受伤闪白等），仅关闭红色 overlay。
		// 子设置关闭时把白闪进度也归零，即受伤时完全不叠加 overlay。
		return OverlayTexture.pack(HurtTintState.whiteFlashProgress(
			whiteOverlayProgress, TweakifyMod.config().modified.hurtTintKeepWhiteFlash), false);
	}
}
