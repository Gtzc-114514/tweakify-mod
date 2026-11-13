package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.EntityHighlightState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftGlowingMixin {
	// 实体渲染器通过 shouldEntityAppearGlowing 决定描边颜色，
	// 配置命中的实体在这里强制返回 true 即可获得发光描边（含掉落物实体）。
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean tweakify$highlightConfiguredEntities(boolean original, Entity entity) {
		if (EntityHighlightState.shouldGlow(entity)) {
			return true;
		}
		return original;
	}
}
