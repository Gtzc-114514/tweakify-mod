package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平滑滚动 mixin 依赖的原版成员必须存在。
 *
 * <p>客户端 mixin 配置是 {@code defaultRequire = 1}：目标方法/字段改名后会在游戏启动或打开界面时
 * 直接抛注入失败，只在游戏里才暴露。这里在单测阶段用反射提前校验一遍签名。
 */
class SmoothScrollMixinTargetsTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void chatComponentTargetsExist() throws Exception {
		assertTrue(hasMethod(ChatComponent.class, "scrollChat", int.class), "scrollChat(int)");
		assertTrue(hasMethod(ChatComponent.class, "resetChatScroll"), "resetChatScroll()");
		assertTrue(hasMethod(ChatComponent.class, "getLinesPerPage"), "getLinesPerPage()");
		assertTrue(hasMethod(ChatComponent.class, "addMessageToDisplayQueue", GuiMessage.class),
			"addMessageToDisplayQueue(GuiMessage)");
		assertTrue(hasMethod(ChatComponent.class, "extractRenderState",
			GuiGraphicsExtractor.class, Font.class, int.class, int.class, int.class,
			ChatComponent.DisplayMode.class, boolean.class), "extractRenderState(7 参数公开重载)");
		assertTrue(hasField(ChatComponent.class, "chatScrollbarPos"), "chatScrollbarPos");
		assertTrue(hasField(ChatComponent.class, "trimmedMessages"), "trimmedMessages");
	}

	@Test
	void creativeScreenTargetsExist() throws Exception {
		assertTrue(hasMethod(CreativeModeInventoryScreen.class, "mouseScrolled",
			double.class, double.class, double.class, double.class), "mouseScrolled(4 个 double)");
		assertTrue(hasMethod(CreativeModeInventoryScreen.class, "extractRenderState",
			GuiGraphicsExtractor.class, int.class, int.class, float.class), "extractRenderState");
		assertTrue(hasField(CreativeModeInventoryScreen.class, "scrollOffs"), "scrollOffs");
		// 滚轮注入点：scrollOffs 的换算入口（super 与 canScroll 检查之后的接缝）。
		assertTrue(hasMethod(CreativeModeInventoryScreen.ItemPickerMenu.class, "subtractInputFromScroll",
			float.class, double.class), "ItemPickerMenu.subtractInputFromScroll(float, double)");
		assertTrue(hasMethod(CreativeModeInventoryScreen.ItemPickerMenu.class, "scrollTo", float.class),
			"ItemPickerMenu.scrollTo(float)");
		assertTrue(hasField(CreativeModeInventoryScreen.ItemPickerMenu.class, "items"), "ItemPickerMenu.items");
	}

	@Test
	void creativeGridTargetsExist() throws Exception {
		// 自绘网格依赖的接缝：容器内容（含 extractLabels 调用点）、槽位绘制、悬停判定与槽位坐标。
		assertTrue(hasMethod(AbstractContainerScreen.class, "extractContents",
			GuiGraphicsExtractor.class, int.class, int.class, float.class), "AbstractContainerScreen.extractContents");
		assertTrue(hasMethod(AbstractContainerScreen.class, "extractLabels",
			GuiGraphicsExtractor.class, int.class, int.class), "AbstractContainerScreen.extractLabels");
		assertTrue(hasMethod(AbstractContainerScreen.class, "extractSlots",
			GuiGraphicsExtractor.class, int.class, int.class), "AbstractContainerScreen.extractSlots");
		assertTrue(hasMethod(AbstractContainerScreen.class, "extractSlot",
			GuiGraphicsExtractor.class, Slot.class, int.class, int.class), "AbstractContainerScreen.extractSlot");
		assertTrue(hasMethod(AbstractContainerScreen.class, "getHoveredSlot", double.class, double.class),
			"AbstractContainerScreen.getHoveredSlot");
		assertTrue(hasField(AbstractContainerScreen.class, "leftPos"), "AbstractContainerScreen.leftPos");
		assertTrue(hasField(AbstractContainerScreen.class, "topPos"), "AbstractContainerScreen.topPos");
		assertTrue(hasField(Slot.class, "x"), "Slot.x");
		assertTrue(hasField(Slot.class, "y"), "Slot.y");
	}

	@Test
	void selectionListTargetsExist() throws Exception {
		assertTrue(hasMethod(AbstractScrollArea.class, "mouseScrolled",
			double.class, double.class, double.class, double.class), "AbstractScrollArea.mouseScrolled");
		assertTrue(hasMethod(AbstractScrollArea.class, "scrollRate"), "AbstractScrollArea.scrollRate()");
		assertTrue(hasMethod(AbstractScrollArea.class, "maxScrollAmount"), "AbstractScrollArea.maxScrollAmount()");
		// 逐帧推进处是把列表强转为 AbstractSelectionList 后调用继承成员（不是 @Shadow），按可调用性校验。
		assertTrue(hasInheritedMethod(AbstractSelectionList.class, "scrollAmount"),
			"AbstractSelectionList.scrollAmount()");
		assertTrue(hasInheritedMethod(AbstractSelectionList.class, "setScrollAmount", double.class),
			"AbstractSelectionList.setScrollAmount(double)");
		assertTrue(hasInheritedMethod(AbstractSelectionList.class, "maxScrollAmount"),
			"AbstractSelectionList.maxScrollAmount()");
		// 滚轮入口通过强转读 visible（继承字段不能 @Shadow）。
		assertTrue(hasField(AbstractWidget.class, "visible"), "AbstractWidget.visible");
	}

	@Test
	void creativePickerMenuScrollTargetsExist() throws Exception {
		// 记录槽位实际行：scrollTo 与它内部的四舍五入换算。
		assertTrue(hasMethod(CreativeModeInventoryScreen.ItemPickerMenu.class, "scrollTo", float.class),
			"ItemPickerMenu.scrollTo(float)");
		assertTrue(hasMethod(CreativeModeInventoryScreen.ItemPickerMenu.class, "getRowIndexForScroll", float.class),
			"ItemPickerMenu.getRowIndexForScroll(float)");
	}

	@Test
	void editBoxTextPositionTargetExists() throws Exception {
		// 亚行平移数字输入框后要重算文字坐标，否则外框动了、数字留在原地。
		assertTrue(hasMethod(EditBox.class, "updateTextPosition"), "EditBox.updateTextPosition()");
	}

	@Test
	void advancementsScrollTargetsExist() throws Exception {
		assertTrue(hasMethod(AdvancementsScreen.class, "mouseScrolled",
			double.class, double.class, double.class, double.class), "AdvancementsScreen.mouseScrolled");
		assertTrue(hasMethod(AdvancementsScreen.class, "extractRenderState",
			GuiGraphicsExtractor.class, int.class, int.class, float.class), "AdvancementsScreen.extractRenderState");
		assertTrue(hasField(AdvancementsScreen.class, "selectedTab"), "AdvancementsScreen.selectedTab");
		// 逐帧缓动写回原版标签的滚动位置，尺寸字段用于计算可滚动范围。
		for (String field : new String[]{"scrollX", "scrollY", "maxX", "maxY"}) {
			assertTrue(hasField(AdvancementTab.class, field), "AdvancementTab." + field);
		}
		assertTrue(hasMethod(AdvancementTab.class, "canScrollHorizontally"), "AdvancementTab.canScrollHorizontally");
		assertTrue(hasMethod(AdvancementTab.class, "canScrollVertically"), "AdvancementTab.canScrollVertically");
	}

	private static boolean hasMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			// getDeclaredMethod 只查本类；这些成员都是目标类自己声明的（mixin 不能 shadow 继承成员）。
			owner.getDeclaredMethod(name, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static boolean hasInheritedMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			owner.getMethod(name, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static boolean hasField(Class<?> owner, String name) {
		try {
			owner.getDeclaredField(name);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}
}
