package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.HeldItemInfoState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 手持物品信息：按配置过滤附魔行。
 * 只在 {@link HeldItemInfoState} 标记生效时拦截（构建手持信息期间），不影响其它提示框。
 */
@Mixin(ItemEnchantments.class)
public abstract class HeldItemInfoEnchantmentsMixin {
	@WrapWithCondition(
		method = "addToTooltip",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
	private boolean tweakify$filterEnchantment(Consumer<Component> consumer, Object text,
			@Local(name = "enchantment") Holder<Enchantment> enchantment) {
		if (!HeldItemInfoState.isFilteringEnchantments()) {
			return true;
		}
		Set<Identifier> filtered = HeldItemInfoState.filteredEnchantments();
		if (filtered.isEmpty()) {
			return true;
		}
		Identifier id = enchantment.unwrapKey().map(ResourceKey::identifier).orElse(null);
		return filtered.contains(id) == HeldItemInfoState.showOnlyFilteredEnchantments();
	}
}
