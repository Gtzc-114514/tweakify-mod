package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.render.PlayerArmorVisibility;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用玩家盔甲渲染：在盔甲层提交阶段整层跳过。
 *
 * <p>只拦截玩家（{@code AvatarRenderState}）的盔甲层；手持物品、披风与鹦鹉等其它层不受影响，
 * 僵尸/骷髅等生物与盔甲架的盔甲保持原版。
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerPlayerArmorMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
		+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
		+ "ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
		at = @At("HEAD"), cancellable = true)
	private void tweakify$hidePlayerArmor(PoseStack pose, SubmitNodeCollector collector, int light,
										  HumanoidRenderState state, float limbAngle, float limbDistance,
										  CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		int selfEntityId = client.player == null ? -1 : client.player.getId();
		if (PlayerArmorVisibility.shouldHideArmor(state, selfEntityId)) {
			ci.cancel();
		}
	}
}
