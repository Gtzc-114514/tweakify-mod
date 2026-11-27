package com.gtzc.tweakify.client.state;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附魔冲突解除范围：同类 = 双方 exclusive_set 为同一个标签。
 */
class EnchantmentConflictRulesTest {
	private static final TagKey<Enchantment> DAMAGE = TagKey.create(Registries.ENCHANTMENT,
		Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/damage"));
	private static final TagKey<Enchantment> ARMOR = TagKey.create(Registries.ENCHANTMENT,
		Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/armor"));

	@Test
	void sameTagIsSameGroup() {
		assertTrue(EnchantmentConflictRules.sameExclusiveGroup(Optional.of(DAMAGE), Optional.of(DAMAGE)));
	}

	@Test
	void differentOrMissingTagIsNotSameGroup() {
		assertFalse(EnchantmentConflictRules.sameExclusiveGroup(Optional.of(DAMAGE), Optional.of(ARMOR)));
		assertFalse(EnchantmentConflictRules.sameExclusiveGroup(Optional.empty(), Optional.of(DAMAGE)));
		assertFalse(EnchantmentConflictRules.sameExclusiveGroup(Optional.empty(), Optional.empty()));
	}

	@Test
	void sameGroupOnlyRequiresSameGroup() {
		assertFalse(EnchantmentConflictRules.shouldBypass(false, false, true));
		assertTrue(EnchantmentConflictRules.shouldBypass(true, false, false),
			"关闭仅同类时保持全部放行");
		assertTrue(EnchantmentConflictRules.shouldBypass(true, true, true));
		assertFalse(EnchantmentConflictRules.shouldBypass(true, true, false),
			"仅同类时跨组冲突保留原版判定");
	}
}
