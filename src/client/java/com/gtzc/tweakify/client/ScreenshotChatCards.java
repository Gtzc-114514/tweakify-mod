package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 截图聊天卡片：登记缩略图、聊天文本与删除/打开文件夹操作。
 */
public final class ScreenshotChatCards {
	public static final int THUMB_WIDTH = 16;
	public static final int THUMB_HEIGHT = 9;
	private static final String MARKER_PREFIX = "tweakify:screenshot:";
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final int MAX_CARDS = 100;
	private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
	private static final Map<Integer, Card> CARDS_BY_ID = new LinkedHashMap<>();

	private ScreenshotChatCards() {
	}

	private record Card(int id, String fileName, File file, Identifier texture) {
	}

	static void add(File file, int[] thumbnailPixels, int blockX, int blockY, int blockZ) {
		Minecraft client = Minecraft.getInstance();
		NativeImage thumbnail = new NativeImage(
			NativeImage.Format.RGBA, THUMB_WIDTH, THUMB_HEIGHT, false);
		for (int y = 0; y < THUMB_HEIGHT; y++) {
			for (int x = 0; x < THUMB_WIDTH; x++) {
				thumbnail.setPixel(x, y, thumbnailPixels[y * THUMB_WIDTH + x]);
			}
		}
		if (client.gui == null || client.gui.hud == null) {
			thumbnail.close();
			return;
		}
		int id = NEXT_ID.getAndIncrement();
		Identifier textureId = Identifier.fromNamespaceAndPath(
			TweakifyMod.MOD_ID, "screenshots/" + id);
		DynamicTexture texture = new DynamicTexture(
			() -> "tweakify screenshot " + id, thumbnail);
		client.getTextureManager().register(textureId, texture);
		texture.upload();
		Card card = new Card(id, file.getName(), file, textureId);
		CARDS_BY_ID.put(id, card);
		while (CARDS_BY_ID.size() > MAX_CARDS) {
			Card oldest = CARDS_BY_ID.values().iterator().next();
			CARDS_BY_ID.remove(oldest.id());
			client.getTextureManager().release(oldest.texture());
		}

		String coords = blockY == Integer.MIN_VALUE
			? "~ ~ ~" : "(" + blockX + ", " + blockY + ", " + blockZ + ")";
		ClickEvent.OpenFile openFolder = new ClickEvent.OpenFile(file.getParentFile());
		String marker = MARKER_PREFIX + id;
		MutableComponent text = Component.literal("    ")
			.withStyle(style -> style
				.withInsertion(marker)
				.withClickEvent(openFolder))
			.append(Component.translatable("tweakify-mod.chat.screenshot.label")
				.withStyle(style -> style
					.withColor(ChatFormatting.GRAY)
					.withClickEvent(openFolder)))
			.append(Component.literal(card.fileName())
				.withStyle(style -> style
					.withColor(ChatFormatting.WHITE)
					.withUnderlined(true)
					.withClickEvent(openFolder)))
			.append(Component.literal(" "))
			.append(Component.translatable("tweakify-mod.chat.screenshot.delete")
				.withStyle(style -> style
					.withColor(ChatFormatting.RED)
					.withClickEvent(new ClickEvent.RunCommand(
						"/tweakify_screenshot delete " + id))))
			.append(Component.literal(" " + LocalTime.now().format(TIME_FORMAT) + " " + coords)
				.withStyle(ChatFormatting.GRAY));
		client.gui.hud.getChat().addClientSystemMessage(text);
	}

	// 在聊天行的左侧绘制 16×9 缩略图；文本前预留 4 个空格。
	public static void render(GuiGraphicsExtractor graphics, int y, float alpha,
							FormattedCharSequence content) {
		if (alpha < 0.05f) {
			return;
		}
		int cardId = findCardId(content);
		Card card = CARDS_BY_ID.get(cardId);
		if (card == null) {
			return;
		}
		graphics.blit(card.texture(), 0, y - 1, THUMB_WIDTH, y - 1 + THUMB_HEIGHT,
			0.0f, 1.0f, 0.0f, 1.0f);
	}

	static boolean delete(int id) {
		Card card = CARDS_BY_ID.get(id);
		if (card == null) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (card.file().exists() && !card.file().delete()) {
			client.gui.hud.getChat().addClientSystemMessage(Component.translatable(
				"tweakify-mod.chat.screenshot.deleteFailed", card.fileName()));
			return false;
		}
		CARDS_BY_ID.remove(id);
		client.getTextureManager().release(card.texture());
		client.gui.hud.getChat().addClientSystemMessage(Component.translatable(
			"tweakify-mod.chat.screenshot.deleted", card.fileName()));
		return true;
	}

	static void clear() {
		for (Card card : CARDS_BY_ID.values()) {
			Minecraft.getInstance().getTextureManager().release(card.texture());
		}
		CARDS_BY_ID.clear();
	}

	// 用不可见的 insertion 标记定位卡片，避免解析本地化文案。
	private static int findCardId(FormattedCharSequence content) {
		int[] found = {-1};
		content.accept((index, style, codepoint) -> {
			String insertion = style.getInsertion();
			if (insertion != null && insertion.startsWith(MARKER_PREFIX)) {
				try {
					found[0] = Integer.parseInt(insertion.substring(MARKER_PREFIX.length()));
				} catch (NumberFormatException ignored) {
					found[0] = -1;
				}
				return false;
			}
			return true;
		});
		return found[0];
	}
}
