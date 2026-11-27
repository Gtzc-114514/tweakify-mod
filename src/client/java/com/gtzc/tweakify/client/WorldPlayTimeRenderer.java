package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

/**
 * 存档条目右上角的游玩时长文本：只读 {@link WorldPlayTime} 缓存，
 * 不在渲染帧内做任何文件 IO。
 */
public final class WorldPlayTimeRenderer {
	// 与内容区右边界、顶部各留的间距，避免压到原版文字。
	static final int RIGHT_PAD = 4;
	static final int TOP_PAD = 1;
	// 与原版存档描述文字一致的低对比度灰色。
	static final int TEXT_COLOR = 0xFFA0A0A0;

	private WorldPlayTimeRenderer() {
	}

	public static void draw(GuiGraphicsExtractor gui, WorldSelectionList.WorldListEntry entry) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.savePlayTime) {
			return;
		}
		long ticks = WorldPlayTime.ticksFor(entry.getLevelSummary());
		String text = WorldPlayTime.displayText(ticks, config.functions.savePlayTimeFormat);
		if (text.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		gui.text(font, text, xForRightAligned(entry.getContentRight(), font.width(text)),
			entry.getContentY() + TOP_PAD, TEXT_COLOR, false);
	}

	// 右对齐到内容区右边界并留出 RIGHT_PAD；包私有便于单测。
	static int xForRightAligned(int contentRight, int textWidth) {
		return contentRight - textWidth - RIGHT_PAD;
	}
}
