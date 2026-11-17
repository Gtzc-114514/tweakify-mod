package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.EnchantmentDescriptionState;
import com.gtzc.tweakify.client.EnchantmentTooltipDescriptions;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * 附魔描述：在物品提示中为每个附魔追加一行描述，行为与 Enchantment Descriptions 一致。
 *
 * <p>26.3 的 Enchantment.description() 是附魔名称（不是描述），因此描述统一读取
 * {@code enchantment.<namespace>.<path>.desc} 语言键；没有翻译的附魔直接跳过，
 * 数据包/模组附魔只要提供同名语言键即可显示。
 *
 * <p>只处理 STORED_ENCHANTMENTS（附魔书）或 ENCHANTMENTS（普通附魔物品），
 * 并遵循 hide_tooltip / hidden_components：原版返回空列表或隐藏附魔行时不追加。
 *
 * <p>描述计划按「注册表 + 物品 + 附魔组件 + 组件类型 + 潜行门控 + Language」缓存，
 * 插入位置仍按当前 tooltip 行重新定位，因此其它模组增删行时不会错位。
 */
@Mixin(ItemStack.class)
public class ItemStackEnchantmentTooltipMixin {
	// 提示列表可能被其它模组用 @WrapMethod 包装；返回新列表而不是取消回调，避免 CancellationException。
	@ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"), require = 0)
	private List<Component> tweakify$appendEnchantmentDescriptions(List<Component> original,
			Item.TooltipContext context, Player player, TooltipFlag flag) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.enchantmentDescriptions) {
			return original;
		}
		// 可选的潜行门控：只在按住潜行键时追加，避免提示过长。
		if (!EnchantmentDescriptionState.shouldAppend(true, config.modified.enchantmentDescriptionsSneakOnly,
			player != null && player.isShiftKeyDown())) {
			return original;
		}
		// hide_tooltip 分支返回空列表：不能在这里凭空造出提示。
		if (original == null || original.isEmpty()) {
			return original;
		}
		ItemStack stack = (ItemStack) (Object) this;
		TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
		// 与原版一致：创造模式忽略 hide_tooltip，其它模式遵循它。
		if (!flag.isCreative() && display.hideTooltip()) {
			return original;
		}
		ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
		DataComponentType<ItemEnchantments> type = DataComponents.STORED_ENCHANTMENTS;
		if (enchantments == null || enchantments.isEmpty()) {
			enchantments = stack.getEnchantments();
			type = DataComponents.ENCHANTMENTS;
		}
		if (enchantments == null || enchantments.isEmpty() || !display.shows(type)) {
			return original;
		}
		// 潜行门控只决定是否追加；未开启门控时潜行状态不参与缓存键，避免无谓失效。
		boolean sneaking = config.modified.enchantmentDescriptionsSneakOnly
			&& player != null && player.isShiftKeyDown();
		HolderLookup.Provider registries = context.registries();
		// lambda 只能捕获 effectively final：enchantments 在上面可能被重新赋值。
		ItemEnchantments cachedEnchantments = enchantments;
		EnchantmentTooltipDescriptions.CacheKey key = new EnchantmentTooltipDescriptions.CacheKey(
			registries, stack.getItem(), cachedEnchantments, type, sneaking, Language.getInstance());
		return EnchantmentTooltipDescriptions.appendCached(original, key,
			() -> EnchantmentTooltipDescriptions.buildPlan(registries, cachedEnchantments));
	}
}
