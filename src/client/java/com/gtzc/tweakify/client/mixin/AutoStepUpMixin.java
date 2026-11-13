package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 如履平地：开启后把本地玩家的最大跨步高度提升到 1.0（像马一样自动跨上 1 格方块）。
 *
 * <p>26.3 的 maxUpStep 由 Attributes.STEP_HEIGHT 属性决定，没有可写的字段；
 * 这里只在返回值上取最大值（不写入、不压低原版或其他模组的数值），
 * 因此与其他模组和属性同步天然兼容。关闭时完全不介入。
 */
@Mixin(LivingEntity.class)
public abstract class AutoStepUpMixin {
	@ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"), require = 0)
	private float tweakify$autoStepUp(float original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.autoStepUp || !((Object) this instanceof LocalPlayer)) {
			return original;
		}
		return Math.max(original, 1.0f);
	}
}
