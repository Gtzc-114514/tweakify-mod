package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.LanguageOverrideState;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 所有一参 getOrDefault 调用（MaLiLib 的 StringUtils.translate 与无 fallback 的
 * TranslatableContents）都经过这里；按模组语言设置返回覆盖文本，
 * 未命中时继续调用原版二参方法。
 */
@Mixin(Language.class)
public class LanguageMixin {
	@Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;",
		at = @At("HEAD"), cancellable = true)
	private void tweakify$overrideLanguage(String key, CallbackInfoReturnable<String> cir) {
		String translated = LanguageOverrideState.translate(key);
		if (translated != null) {
			cir.setReturnValue(translated);
		}
	}
}
