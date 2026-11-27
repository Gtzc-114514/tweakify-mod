package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 背包快捷右键的物品分类：功能方块、潜影盒、末影箱按子开关生效，普通物品不触发。
 */
class QuickRightClickServiceTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.ITEM.freeze();
	}

	@AfterEach
	void clearConfig() {
		TweakifyModTestSupport.installConfig(null);
	}

	private static TweakifyModConfig enableAll() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.functions.inventoryQuickRightClick = true;
		TweakifyModTestSupport.installConfig(config);
		return config;
	}

	private static ItemStack stack(Item item) {
		// 单元测试没有完整数据包加载流程，手动绑定空组件表，避免 Holder 的组件尚未绑定。
		BuiltInRegistries.ITEM.getResourceKey(item)
			.flatMap(BuiltInRegistries.ITEM::get)
			.ifPresent(holder -> holder.bindComponents(DataComponentMap.EMPTY));
		return new ItemStack(item);
	}

	@Test
	void recognizesFunctionalBlocks() {
		enableAll();
		assertTrue(QuickRightClickService.isSupported(stack(Items.CRAFTING_TABLE)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.ANVIL)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.DAMAGED_ANVIL)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.ENCHANTING_TABLE)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.SMITHING_TABLE)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.LOOM)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.CARTOGRAPHY_TABLE)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.STONECUTTER)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.GRINDSTONE)));
	}

	@Test
	void recognizesContainers() {
		enableAll();
		assertTrue(QuickRightClickService.isSupported(stack(Items.SHULKER_BOX)));
		assertTrue(QuickRightClickService.isSupported(stack(Items.ENDER_CHEST)));
	}

	@Test
	void ignoresRegularItems() {
		enableAll();
		assertFalse(QuickRightClickService.isSupported(stack(Items.STONE)));
		assertFalse(QuickRightClickService.isSupported(stack(Items.DIAMOND_SWORD)));
		assertFalse(QuickRightClickService.isSupported(ItemStack.EMPTY));
	}

	@Test
	void subOptionsDisableTheirCategories() {
		TweakifyModConfig config = enableAll();
		config.functions.quickRightClickFunctionalBlocks = false;
		config.functions.quickRightClickShulkerBoxes = false;
		config.functions.quickRightClickEnderChest = false;

		assertFalse(QuickRightClickService.isSupported(stack(Items.CRAFTING_TABLE)));
		assertFalse(QuickRightClickService.isSupported(stack(Items.SHULKER_BOX)));
		assertFalse(QuickRightClickService.isSupported(stack(Items.ENDER_CHEST)));
	}

	@Test
	void targetResolutionReturnsNoneForUnsupportedStacks() {
		QuickRightClickService.ResolvedTarget stone = QuickRightClickService.resolveTarget(
			stack(Items.STONE), true, true, true);
		assertEquals(QuickRightClickService.QuickOpenTarget.NONE, stone.target());
		assertNull(stone.menu());

		assertEquals(QuickRightClickService.QuickOpenTarget.NONE,
			QuickRightClickService.resolveTarget(stack(Items.SHULKER_BOX), false, false, false).target());
		assertEquals(QuickRightClickService.QuickOpenTarget.NONE,
			QuickRightClickService.resolveTarget(stack(Items.ENDER_CHEST), true, false, true).target());
	}

	@Test
	void targetResolutionClassifiesSupportedStacks() {
		QuickRightClickService.ResolvedTarget shulker = QuickRightClickService.resolveTarget(
			stack(Items.SHULKER_BOX), true, false, false);
		assertEquals(QuickRightClickService.QuickOpenTarget.SHULKER, shulker.target());
		assertNull(shulker.menu());

		QuickRightClickService.ResolvedTarget ender = QuickRightClickService.resolveTarget(
			stack(Items.ENDER_CHEST), false, true, false);
		assertEquals(QuickRightClickService.QuickOpenTarget.ENDER_CHEST, ender.target());
		assertNull(ender.menu());

		QuickRightClickService.ResolvedTarget crafting = QuickRightClickService.resolveTarget(
			stack(Items.CRAFTING_TABLE), false, false, true);
		assertEquals(QuickRightClickService.QuickOpenTarget.FUNCTIONAL, crafting.target());
		assertEquals(QuickRightClickService.FunctionalMenu.CRAFTING, crafting.menu());
	}

	@Test
	void resolvedTargetClosesBeforeOpening() {
		List<String> events = new ArrayList<>();
		QuickRightClickService.ResolvedTarget resolved = new QuickRightClickService.ResolvedTarget(
			QuickRightClickService.QuickOpenTarget.FUNCTIONAL,
			QuickRightClickService.FunctionalMenu.CRAFTING);

		boolean applied = QuickRightClickService.applyResolvedTarget(resolved,
			() -> events.add("close"),
			target -> events.add("open:" + target.target()));

		assertTrue(applied);
		assertEquals(List.of("close", "open:FUNCTIONAL"), events,
			"必须先关闭旧菜单，再打开新菜单");
	}

	@Test
	void noneTargetDoesNotTouchMenus() {
		List<String> events = new ArrayList<>();
		QuickRightClickService.ResolvedTarget resolved = new QuickRightClickService.ResolvedTarget(
			QuickRightClickService.QuickOpenTarget.NONE, null);

		boolean applied = QuickRightClickService.applyResolvedTarget(resolved,
			() -> events.add("close"),
			target -> events.add("open"));

		assertFalse(applied);
		assertTrue(events.isEmpty(), "判定失败时不应关闭或打开任何菜单");
	}

	@Test
	void disabledRootRejectsEverything() {
		TweakifyModConfig config = enableAll();
		config.functions.inventoryQuickRightClick = false;

		assertFalse(QuickRightClickService.isSupported(stack(Items.CRAFTING_TABLE)));
		assertFalse(QuickRightClickService.isSupported(stack(Items.SHULKER_BOX)));
	}

	@Test
	void shulkerWriteBackPreservesSlotsAndCounts() {
		ItemStack shulker = stack(Items.SHULKER_BOX);
		SimpleContainer container = new SimpleContainer(27);
		// 测试环境把物品组件绑定为空表，setItem 会按缺失的堆叠上限把数量夹到 0；
		// 直接写底层列表，只验证写回逻辑本身。
		ItemStack stones = stack(Items.STONE);
		stones.setCount(5);
		container.getItems().set(2, stones);
		container.getItems().set(10, stack(Items.DIRT));

		assertFalse(container.getItem(2).isEmpty(), "测试容器应已放入石头");
		QuickRightClickService.writeBackShulkerBox(shulker, container);

		ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
		assertNotNull(contents, "非空盒子应写入 CONTAINER 组件");
		assertTrue(contents.size() >= 11, "组件应保留到第 10 格");
		assertEquals(5, templateCount(contents, Items.STONE), "写入后模板数量应保留");
		assertEquals(1, templateCount(contents, Items.DIRT), "第二个物品应写入组件");
	}

	// 测试环境绑定了空组件表，create() 会把数量夹到 1；直接读模板里的数量。
	private static int templateCount(ItemContainerContents contents, Item item) {
		int count = 0;
		for (ItemStackTemplate template : contents.nonEmptyItems()) {
			if (template.item().value() == item) {
				count += template.count();
			}
		}
		return count;
	}

	@Test
	void emptyShulkerWriteBackRemovesComponent() {
		ItemStack shulker = stack(Items.SHULKER_BOX);
		shulker.set(DataComponents.CONTAINER,
			ItemContainerContents.fromItems(List.of(stack(Items.STONE))));

		QuickRightClickService.writeBackShulkerBox(shulker, new SimpleContainer(27));

		assertNull(shulker.get(DataComponents.CONTAINER), "空盒子应移除 CONTAINER 组件");
	}
}
