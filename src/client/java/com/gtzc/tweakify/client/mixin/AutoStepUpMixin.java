package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 如履平地：开启后把本地玩家的最大跨步高度提升到 1.0（像马一样自动跨上 1 格方块）。
 *
 * <p>26.3 的 maxUpStep 由 Attributes.STEP_HEIGHT 属性决定，没有可写的字段；
 * 这里只在返回值上取最大值（不写入、不压低原版或其他模组的数值），
 * 因此与其他模组和属性同步天然兼容。关闭时完全不介入。
 */
@Mixin(LivingEntity.class)
public abstract class AutoStepUpMixin {
	// maxUpStep 每 tick 被每个实体多次调用：用 AtomicBoolean 版本，标记后只剩一次 volatile 读。
	@Unique
	private static final AtomicBoolean TWEAKIFY$STEP_UP_PROBE = new AtomicBoolean();

	@ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"), require = 0)
	private float tweakify$autoStepUp(float original) {
		HookProbe.markOnce(TWEAKIFY$STEP_UP_PROBE, "autoStepUp");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.autoStepUp || !((Object) this instanceof LocalPlayer)) {
			return original;
		}
		return Math.max(original, 1.0f);
	}
}
