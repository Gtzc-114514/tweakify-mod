package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SelfNameVisibility;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第三人称自身名字：原版只显示其他玩家/生物的名字，第三人称下看不到自己。
 * 在名字状态提取末尾补上本地玩家的名字；第一人称保持原版。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererSelfNameMixin {
	// 使用最终的 5 参数版本：LivingEntityRenderer 会覆盖 3 参数版本，
	// 但玩家等生物最终仍会调用这里的 5 参数实现。
	@Inject(
		method = "extractNameTags(Lnet/minecraft/world/entity/Entity;"
			+ "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
		at = @At("TAIL"))
	private void tweakify$showOwnNameInThirdPerson(Entity entity, EntityRenderState state,
													float partialTick, double nameTagDistance,
													double belowNameDistance, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.thirdPersonPlayerName) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player != entity) {
			return;
		}

		boolean showSelfName = SelfNameVisibility.shouldShowSelfName(
			config.thirdPersonPlayerName, true,
			client.options.getCameraType().isFirstPerson(), client.gui.hud.isHidden(),
			entity.isInvisible(), entity.isSpectator());
		if (!showSelfName) {
			// 第一/隐藏 HUD/隐身/旁观都不要把本地玩家名字强行补回来。
			state.nameTag = null;
			state.nameTagAttachment = null;
			return;
		}

		state.nameTag = entity.getDisplayName();
		state.nameTagAttachment = entity.getAttachments()
			.getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
	}
}
