package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用潜行隐藏名字：潜行时名字标签保持完整距离。
 *
 * <p>原版对潜行实体加了 32 格距离限制（{@code isDiscrete} 分支），开启后跳过该分支，
 * 潜行玩家/生物的名字不再因距离消失；队伍可见性等其余规则保持不变。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSneakNameMixin {
	@Redirect(method = "shouldShowName",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;isDiscrete()Z"))
	private boolean tweakify$keepSneakName(LivingEntity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.disableSneakNameHide) {
			return false;
		}
		return entity.isDiscrete();
	}
}
