package com.gtzc.tweakify.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雕刻书架可视化的纯逻辑：占用位图、槽位数量与居中、附魔行拼接、界面可见性。
 */
class ChiseledBookshelfVisualizerTest {
	// 方块/物品注册表需要在静态初始化前引导一次。
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void signatureSetsOneBitPerOccupiedSlot() {
		assertEquals(0, ChiseledBookshelfVisualizer.signatureOf(new boolean[6]));
		assertEquals(1, ChiseledBookshelfVisualizer.signatureOf(new boolean[]{true, false, false, false, false, false}));
		assertEquals(0b101010, ChiseledBookshelfVisualizer.signatureOf(
			new boolean[]{false, true, false, true, false, true}));
		assertEquals(0b111111, ChiseledBookshelfVisualizer.signatureOf(
			new boolean[]{true, true, true, true, true, true}));
	}

	@Test
	void occupiedSlotsReadsBlockStateProperties() {
		BlockState empty = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
		assertFalse(ChiseledBookshelfVisualizer.signatureOf(ChiseledBookshelfVisualizer.occupiedSlots(empty)) != 0,
			"默认方块状态必须一格都不占用");

		BlockState withBook = empty.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(2), true);
		boolean[] occupied = ChiseledBookshelfVisualizer.occupiedSlots(withBook);
		assertTrue(occupied[2]);
		assertEquals(1 << 2, ChiseledBookshelfVisualizer.signatureOf(occupied));
	}

	@Test
	void panelSlotCountFollowsShowAllSlotsAndHover() {
		assertEquals(6, ChiseledBookshelfVisualizer.panelSlotCount(true, -1));
		assertEquals(6, ChiseledBookshelfVisualizer.panelSlotCount(true, 3));
		assertEquals(1, ChiseledBookshelfVisualizer.panelSlotCount(false, 4));
		assertEquals(0, ChiseledBookshelfVisualizer.panelSlotCount(false, -1));
	}

	@Test
	void slotRowIsCentered() {
		// 6 格：6*18 + 5*2 = 118，居中后左边界为 centerX - 59。
		assertEquals(100 - 59, ChiseledBookshelfVisualizer.slotRowStartX(100, 6));
		// 单格：宽度 18，左边界为 centerX - 9。
		assertEquals(100 - 9, ChiseledBookshelfVisualizer.slotRowStartX(100, 1));
	}

	@Test
	void emptyEnchantmentsAddNoLines() {
		List<Component> lines = new ArrayList<>();
		ChiseledBookshelfVisualizer.appendEnchantmentLines(ItemEnchantments.EMPTY, lines);

		assertTrue(lines.isEmpty(), "空附魔集合不应追加任何行");
	}

	@Test
	void nullScreenKeepsPanelVisible() {
		assertFalse(ChiseledBookshelfVisualizer.hidesPanel(null));
	}
}
