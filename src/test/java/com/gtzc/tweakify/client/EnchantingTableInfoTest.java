package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附魔台附魔信息的纯逻辑边界：无注册表、空物品与无效花费必须在触碰注册表前返回。
 */
class EnchantingTableInfoTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void emptyInputsReturnNoEnchantments() {
		assertTrue(EnchantingTableInfo.compute(null, ItemStack.EMPTY, 0, 0, 10).isEmpty(),
			"a missing registry access must not be dereferenced");
		assertTrue(EnchantingTableInfo.compute(null, ItemStack.EMPTY, 0, 0, 0).isEmpty(),
			"zero cost must return no enchantments");
	}

	@Test
	void describeOffersReturnsImmutableEmptyListForUnavailableInput() {
		assertTrue(EnchantingTableInfo.describeOffers(null, ItemStack.EMPTY, 0, 0, 10).isEmpty());
	}
}
