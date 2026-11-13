package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.LanguageOverrideState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 所有一参 getOrDefault 调用（MaLiLib 的 StringUtils.translate 与无 fallback 的
 * TranslatableContents）都经过这里；按模组语言设置返回覆盖文本，
 * 未命中时继续使用原版翻译。
 */
@Mixin(Language.class)
public class LanguageMixin {
	// 语言查询是热路径，其他模组也可能包装该方法；用返回值替换避免取消回调。
	@ModifyReturnValue(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"))
	private String tweakify$overrideLanguage(String original, String key) {
		String translated = LanguageOverrideState.translate(key);
		return translated != null ? translated : original;
	}
}
