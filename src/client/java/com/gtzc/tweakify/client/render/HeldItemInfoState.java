package com.gtzc.tweakify.client.render;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Set;

/**
 * 构建手持物品信息时的临时上下文：让针对原版 {@code ItemStack} / {@code ItemEnchantments}
 * 的 Mixin 只在本模组构建提示行时改变行为，不影响其它提示框。
 *
 * <p>只在客户端线程使用；字段声明为 volatile 是为了让混入方法在任何调用顺序下都能读到完整值。
 */
public final class HeldItemInfoState {
	// 追加弩的装填物时隐藏原版的“intangible projectile”提示行。
	private static volatile boolean hidingIntangibleProjectile;

	// 附魔过滤：只在追加附魔组件时生效。
	private static volatile boolean filteringEnchantments;
	private static volatile Set<Identifier> filteredEnchantments = Collections.emptySet();
	private static volatile boolean showOnlyFilteredEnchantments;

	private HeldItemInfoState() {
	}

	public static boolean isHidingIntangibleProjectile() {
		return hidingIntangibleProjectile;
	}

	public static void setHidingIntangibleProjectile(boolean hiding) {
		hidingIntangibleProjectile = hiding;
	}

	public static boolean isFilteringEnchantments() {
		return filteringEnchantments;
	}

	public static Set<Identifier> filteredEnchantments() {
		return filteredEnchantments;
	}

	public static boolean showOnlyFilteredEnchantments() {
		return showOnlyFilteredEnchantments;
	}

	public static void setEnchantmentFilter(Set<Identifier> filtered, boolean showOnlyFiltered) {
		filteredEnchantments = filtered == null ? Collections.emptySet() : filtered;
		showOnlyFilteredEnchantments = showOnlyFiltered;
		filteringEnchantments = true;
	}

	public static void clearEnchantmentFilter() {
		filteringEnchantments = false;
		filteredEnchantments = Collections.emptySet();
		showOnlyFilteredEnchantments = false;
	}
}
