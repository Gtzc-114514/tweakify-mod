package com.gtzc.tweakify.client;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * 附魔冲突解除范围：判断两个附魔是否属于同一个互斥组（同一 exclusive_set 标签）。
 * 26.3 没有附魔类别字段，互斥关系由数据包里的 exclusive_set 标签表达，
 * 因此“同类”以标签键相等为准；内联列表（无标签键）视为无法归组。
 */
public final class EnchantmentConflictState {
	private EnchantmentConflictState() {
	}

	public static boolean sameExclusiveGroup(Optional<TagKey<Enchantment>> first,
											 Optional<TagKey<Enchantment>> second) {
		return first.isPresent() && second.isPresent() && first.get().equals(second.get());
	}

	// 仅同类模式下，只有同一互斥组才放行；关闭“仅同类”时保持全部放行。
	public static boolean shouldBypass(boolean enabled, boolean sameGroupOnly, boolean sameExclusiveGroup) {
		return enabled && (!sameGroupOnly || sameExclusiveGroup);
	}
}
