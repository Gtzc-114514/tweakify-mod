package com.gtzc.tweakify.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 附魔台附魔信息：按客户端同步的附魔种子复算每个选项实际会给出的附魔。
 *
 * <p>算法与 {@code EnchantmentMenu.getEnchantmentList} 保持一致：用种子 + 槽位重建随机源，
 * 从 {@code #minecraft:in_enchanting_table} 标签中筛选，附魔书再随机移除一项。
 * 种子由容器数据槽同步到客户端，因此多人服务器上也能显示，不依赖集成服务端。
 */
public final class EnchantingTableInfo {
	private EnchantingTableInfo() {
	}

	// 生成悬停提示的每一行；没有可用结果时返回空列表，由调用方决定是否显示提示。
	public static List<Component> describeOffers(RegistryAccess registryAccess, ItemStack stack,
												 int seed, int slot, int cost) {
		return describeOffers(registryAccess, stack, seed, slot, cost, false, false);
	}

	// 带显示子项的版本：可追加经验消耗行，并在每条附魔后标注最高等级。
	public static List<Component> describeOffers(RegistryAccess registryAccess, ItemStack stack,
												 int seed, int slot, int cost,
												 boolean showCost, boolean showMaxLevel) {
		List<EnchantmentInstance> enchantments = compute(registryAccess, stack, seed, slot, cost);
		if (enchantments.isEmpty()) {
			return List.of();
		}
		List<Component> lines = new ArrayList<>(enchantments.size() + 1);
		for (EnchantmentInstance instance : enchantments) {
			MutableComponent line = Enchantment.getFullname(instance.enchantment(), instance.level()).copy();
			if (showMaxLevel) {
				line.append(Component.literal(" "));
				line.append(Component.translatable("tweakify-mod.enchanting_table_info.max_level",
					instance.enchantment().value().getMaxLevel()).withStyle(ChatFormatting.DARK_GRAY));
			}
			lines.add(line);
		}
		if (showCost && cost > 0) {
			lines.add(Component.translatable("tweakify-mod.enchanting_table_info.cost", cost)
				.withStyle(ChatFormatting.GRAY));
		}
		return List.copyOf(lines);
	}

	// 包私有：与界面解耦，空输入直接在触碰注册表前返回。
	static List<EnchantmentInstance> compute(RegistryAccess registryAccess, ItemStack stack,
											 int seed, int slot, int cost) {
		if (registryAccess == null || stack == null || stack.isEmpty() || cost <= 0) {
			return List.of();
		}
		Optional<HolderSet.Named<Enchantment>> table =
			registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (table.isEmpty()) {
			return List.of();
		}
		RandomSource random = RandomSource.create(seed + slot);
		List<EnchantmentInstance> enchantments =
			EnchantmentHelper.selectEnchantment(random, stack, cost, table.get().stream());
		// 与 EnchantmentMenu 一致：附魔书最多保留一项。
		if (stack.is(Items.BOOK) && enchantments.size() > 1) {
			enchantments.remove(random.nextInt(enchantments.size()));
		}
		return enchantments;
	}
}
