package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 附魔描述：在物品提示中为每个附魔追加一行描述，行为与 Enchantment Descriptions 一致。
 *
 * <p>26.3 的 Enchantment.description() 是附魔名称（不是描述），因此描述统一读取
 * {@code enchantment.<namespace>.<path>.desc} 语言键；没有翻译的附魔直接跳过，
 * 数据包/模组附魔只要提供同名语言键即可显示。
 *
 * <p>只处理 STORED_ENCHANTMENTS（附魔书）或 ENCHANTMENTS（普通附魔物品），
 * 并遵循 hide_tooltip / hidden_components：原版返回空列表或隐藏附魔行时不追加。
 */
@Mixin(ItemStack.class)
public class ItemStackEnchantmentTooltipMixin {
	// 描述行与插入位置：先收集、再按位置降序插入，避免前面的插入让后面的索引偏移。
	private record DescriptionInsertion(int position, Component description) {
	}

	// 提示列表可能被其它模组用 @WrapMethod 包装；返回新列表而不是取消回调，避免 CancellationException。
	@ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"), require = 0)
	private List<Component> tweakify$appendEnchantmentDescriptions(List<Component> original,
			Item.TooltipContext context, Player player, TooltipFlag flag) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.enchantmentDescriptions) {
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
		List<Component> lines = new ArrayList<>(original);
		List<DescriptionInsertion> insertions = new ArrayList<>();
		List<Component> trailing = new ArrayList<>();
		// 每个附魔行只分配一次：同名附魔行按出现顺序对应，避免描述堆叠到同一行。
		Set<Integer> usedLines = new HashSet<>();
		// 按原版附魔行顺序（TOOLTIP_ORDER 标签序，其余按哈希序）遍历，重名时归属不会互换。
		HolderLookup.Provider registries = context.registries();
		HolderSet<Enchantment> tooltipOrder = registries.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(EnchantmentTags.TOOLTIP_ORDER);
		List<Holder<Enchantment>> ordered = new ArrayList<>();
		for (Holder<Enchantment> holder : tooltipOrder) {
			if (enchantments.getLevel(holder) > 0) {
				ordered.add(holder);
			}
		}
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (!tooltipOrder.contains(holder)) {
				ordered.add(holder);
			}
		}
		for (Holder<Enchantment> holder : ordered) {
			Identifier id = holder.unwrapKey().map(ResourceKey::identifier).orElse(null);
			if (id == null) {
				continue;
			}
			String key = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
			Component description = Component.translatable(key);
			String text = description.getString();
			// 没有对应语言键（返回键本身）或描述为空时不追加。
			if (text.isBlank() || text.equals(key)) {
				continue;
			}
			Component descLine = Component.literal("  ")
				.append(description)
				.withStyle(ChatFormatting.DARK_GRAY);
			// 从索引 1 开始（第 0 行是物品悬停名称，不能被误当成附魔行）；
			// 附魔块在 lore 之前，从前往后天然避开 lore 行。
			String nameText = Enchantment.getFullname(holder, enchantments.getLevel(holder)).getString();
			int lineIndex = -1;
			for (int i = 1; i < lines.size(); i++) {
				if (!usedLines.contains(i) && lines.get(i).getString().equals(nameText)) {
					lineIndex = i;
					usedLines.add(i);
					break;
				}
			}
			if (lineIndex >= 0) {
				insertions.add(new DescriptionInsertion(lineIndex + 1, descLine));
			} else {
				// 找不到对应附魔行时按遍历顺序退回末尾。
				trailing.add(descLine);
			}
		}
		if (insertions.isEmpty() && trailing.isEmpty()) {
			return original;
		}
		insertions.sort((first, second) -> Integer.compare(second.position(), first.position()));
		for (DescriptionInsertion insertion : insertions) {
			lines.add(insertion.position(), insertion.description());
		}
		lines.addAll(trailing);
		HookProbe.markOnce("enchantmentDescriptions");
		return lines;
	}
}
