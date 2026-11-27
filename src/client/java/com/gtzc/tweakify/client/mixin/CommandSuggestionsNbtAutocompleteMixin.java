package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.NbtAutocomplete;
import com.gtzc.tweakify.client.state.NbtAutocompleteKeys;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NBT 自动补全：在 {@code updateCommandInfo} 取到原版补全结果后，
 * 若光标位于组件方括号 / NBT 花括号的键名位置，用组件 ID 或常用 NBT 键名替换空结果。
 *
 * <p>只在原版没有任何补全时接管，避免与命令树自身的候选（物品 ID、选择器选项等）
 * 产生不同区间混排；替换后的区间从当前词起点开始，Tab / 点击插入的行为与原版一致。
 * 注入点为命令补全类模组的常见改写目标：{@code require = 0} + 探针，冲突时降级而不是启动失败。
 */
@Mixin(CommandSuggestions.class)
public class CommandSuggestionsNbtAutocompleteMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$NBT_AUTOCOMPLETE_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	private EditBox input;

	@ModifyExpressionValue(method = "updateCommandInfo",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions("
				+ "Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"),
		require = 0)
	private CompletableFuture<Suggestions> tweakify$appendNbtKeys(CompletableFuture<Suggestions> original) {
		HookProbe.markOnce(TWEAKIFY$NBT_AUTOCOMPLETE_PROBE, "nbtAutocomplete");
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.nbtAutocomplete || this.input == null) {
			return original;
		}
		String text = this.input.getValue();
		int cursor = this.input.getCursorPosition();
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, cursor);
		if (!analysis.isActive()) {
			return original;
		}
		List<String> keys = NbtAutocomplete.filter(
			analysis.context() == NbtAutocomplete.Context.COMPONENTS
				? NbtAutocompleteKeys.componentIds() : NbtAutocompleteKeys.NBT_KEYS,
			analysis);
		if (keys.isEmpty()) {
			return original;
		}
		StringRange range = StringRange.between(analysis.wordStart(), cursor);
		Suggestions extra = new Suggestions(range, buildSuggestions(keys, range));
		return original.thenApply(base -> base != null && !base.isEmpty() ? base : extra);
	}

	private static List<Suggestion> buildSuggestions(List<String> keys, StringRange range) {
		return keys.stream().map(key -> new Suggestion(range, key)).toList();
	}
}
