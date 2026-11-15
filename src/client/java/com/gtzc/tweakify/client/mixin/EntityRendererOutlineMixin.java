package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.EntityHighlightState;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 实体高亮描边颜色：原版在 extractRenderState 里用 getTeamColor() 计算发光描边色。
 * 这里只改写这一个调用点，不影响名字标签等其它使用队伍色的地方。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererOutlineMixin {
	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
	private int tweakify$overrideHighlightColor(int original, Entity entity, EntityRenderState state,
										float partialTick) {
		return EntityHighlightState.outlineColor(entity, original);
	}
}
