package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 告示牌文本优化：在构造时缓存原始/过滤文本是否存在文字，
 * 供上蜡、染料等服务端判定复用，避免重复遍历文本行。
 */
@Mixin(SignText.class)
public class SignTextOptimizationMixin {
	@Unique
	private boolean tweakify$hasText;

	@Unique
	private boolean tweakify$hasFilteredText;

	@Inject(method = "<init>(Ljava/util/List;Ljava/util/List;Lnet/minecraft/world/item/DyeColor;Z)V",
		at = @At("TAIL"))
	private void tweakify$cacheHasText(List<Component> messages, List<Component> filteredMessages,
									   DyeColor color, boolean glowing, CallbackInfo ci) {
		tweakify$hasText = tweakify$anyText(messages);
		tweakify$hasFilteredText = tweakify$anyText(filteredMessages);
	}

	@Unique
	private static boolean tweakify$anyText(List<Component> lines) {
		if (lines == null) {
			return false;
		}
		for (Component line : lines) {
			if (!line.getString().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Inject(method = "hasMessage", at = @At("HEAD"), cancellable = true)
	private void tweakify$useCachedHasText(boolean filtered, CallbackInfoReturnable<Boolean> cir) {
		if (TweakifyMod.config().optimization.signTextOptimization
			&& TweakifyMod.config().optimization.signTextOptimizationCachedText) {
			cir.setReturnValue(filtered ? tweakify$hasFilteredText : tweakify$hasText);
		}
	}
}
