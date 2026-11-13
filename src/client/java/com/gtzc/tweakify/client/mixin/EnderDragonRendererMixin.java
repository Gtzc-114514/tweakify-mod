package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonRenderer.class)
public class EnderDragonRendererMixin {
	// 末影龙不是 LivingEntityRenderer，自行用 hurtTime 生成红色 overlay（死亡红使用
	// DYING 渲染类型，不受该字段影响），这里在状态提取末尾统一关闭受伤红。
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;"
		+ "Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;F)V",
		at = @At("TAIL"))
	private void tweakify$disableHurtTint(EnderDragon dragon, EnderDragonRenderState state,
										 float partialTick, CallbackInfo ci) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.modified.disableHurtTint) {
			state.hasRedOverlay = false;
		}
	}
}
