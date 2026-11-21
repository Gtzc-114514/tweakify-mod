package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 更好的 F2 截图：接管截图保存，支持聊天卡片、F9 4K 输出与多种文件格式。
 *
 * <p>截图回调在渲染线程触发；像素复制与坐标采样后立即把缩放、编码和磁盘 IO
 * 移到 IO 线程，聊天卡片/纹理注册再通过 {@link Minecraft#execute} 回渲染线程。
 */
public final class BetterScreenshotManager {
	private static final int FOUR_K_WIDTH = 3840;
	private static final int FOUR_K_HEIGHT = 2160;
	private static final DateTimeFormatter FILE_TIME =
		DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

	private BetterScreenshotManager() {
	}

	public static void capture(Minecraft client, boolean fourK) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.betterScreenshot) {
			return;
		}
		try {
			Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), 1,
				image -> save(client, image, fourK, config));
		} catch (RuntimeException error) {
			TweakifyMod.LOGGER.error("Failed to request screenshot capture", error);
		}
	}

	// 渲染线程只做像素快照与坐标采样，重活交给 IO 线程。
	private static void save(Minecraft client, NativeImage image, boolean fourK,
							 TweakifyModConfig config) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] pixels = image.getPixels();
		image.close();
		int x = 0;
		int y = Integer.MIN_VALUE;
		int z = 0;
		if (client.player != null) {
			x = client.player.blockPosition().getX();
			y = client.player.blockPosition().getY();
			z = client.player.blockPosition().getZ();
		}
		int blockX = x;
		int blockY = y;
		int blockZ = z;
		Util.ioPool().execute(() -> savePixels(client, pixels, width, height, fourK, config,
			blockX, blockY, blockZ));
	}

	private static void savePixels(Minecraft client, int[] pixels, int width, int height,
								   boolean fourK, TweakifyModConfig config,
								   int blockX, int blockY, int blockZ) {
		File file = null;
		try {
			File directory = new File(client.gameDirectory, Screenshot.SCREENSHOT_DIR);
			if (!directory.exists() && !directory.mkdirs()) {
				throw new IOException("Cannot create screenshot directory: " + directory);
			}
			ScreenshotFormat format = ScreenshotFormat.fromCode(
				config.functions.betterScreenshotFormat);
			String base = (fourK ? "huge_" : "") + LocalDateTime.now().format(FILE_TIME)
				+ coordinateSuffix(blockX, blockY, blockZ);
			file = uniqueFile(directory, base, format.extension());
			BufferedImage buffered = toBufferedImage(pixels, width, height);
			if (fourK) {
				buffered = scaleTo4K(buffered);
			}
			write(format, buffered, file);
			File savedFile = file;
			int[] thumbnail = makeThumbnailPixels(pixels, width, height);
			int[] finalPixels = thumbnail;
			client.execute(() -> ScreenshotChatCards.add(savedFile, finalPixels,
				blockX, blockY, blockZ));
			TweakifyMod.LOGGER.info("Saved screenshot: {} ({}x{})",
				savedFile.getName(), buffered.getWidth(), buffered.getHeight());
		} catch (Exception error) {
			if (file != null) {
				file.delete();
			}
			TweakifyMod.LOGGER.error("Failed to save screenshot", error);
			client.execute(() -> {
				if (client.gui != null && client.gui.hud != null) {
					client.gui.hud.getChat().addClientSystemMessage(
						Component.translatable("tweakify-mod.chat.screenshot.saveFailed",
							error.getMessage()));
				}
			});
		}
	}

	private static BufferedImage toBufferedImage(int[] pixels, int width, int height) {
		BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		buffered.setRGB(0, 0, width, height, pixels, 0, width);
		return buffered;
	}

	private static BufferedImage scaleTo4K(BufferedImage source) {
		if (source.getWidth() == FOUR_K_WIDTH && source.getHeight() == FOUR_K_HEIGHT) {
			return source;
		}
		BufferedImage scaled = new BufferedImage(
			FOUR_K_WIDTH, FOUR_K_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
			// 保持比例并补黑边，避免非 16:9 画面被拉伸。
			double scale = Math.min((double) FOUR_K_WIDTH / source.getWidth(),
				(double) FOUR_K_HEIGHT / source.getHeight());
			int width = (int) Math.round(source.getWidth() * scale);
			int height = (int) Math.round(source.getHeight() * scale);
			graphics.setColor(Color.BLACK);
			graphics.fillRect(0, 0, FOUR_K_WIDTH, FOUR_K_HEIGHT);
			graphics.drawImage(source, (FOUR_K_WIDTH - width) / 2,
				(FOUR_K_HEIGHT - height) / 2, width, height, null);
		} finally {
			graphics.dispose();
		}
		return scaled;
	}

	private static void write(ScreenshotFormat format, BufferedImage image, File file)
		throws IOException {
		switch (format) {
			case PNG -> requireWriter(ImageIO.write(image, "png", file), file);
			case JPG -> requireWriter(ImageIO.write(toRgb(image), "jpg", file), file);
			case BMP -> requireWriter(ImageIO.write(toRgb(image), "bmp", file), file);
			case TGA -> writeTga(image, file);
		}
	}

	private static void requireWriter(boolean written, File file) throws IOException {
		if (!written) {
			file.delete();
			throw new IOException("No ImageIO writer for " + file.getName());
		}
	}

	private static BufferedImage toRgb(BufferedImage source) {
		BufferedImage rgb = new BufferedImage(
			source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgb.createGraphics();
		try {
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return rgb;
	}

	// 32 位无压缩 TGA；图像原点在左上角，ABGR 字节序。
	private static void writeTga(BufferedImage image, File file) throws IOException {
		int width = image.getWidth();
		int height = image.getHeight();
		try (DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(new FileOutputStream(file)))) {
			out.writeByte(0);
			out.writeByte(0);
			out.writeByte(2);
			out.writeShort(0);
			out.writeShort(0);
			out.writeByte(0);
			out.writeShort(0);
			out.writeShort(0);
			writeShortLe(out, width);
			writeShortLe(out, height);
			out.writeByte(32);
			out.writeByte(0x28);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int argb = image.getRGB(x, y);
					out.writeByte(argb & 0xFF);
					out.writeByte(argb >> 8 & 0xFF);
					out.writeByte(argb >> 16 & 0xFF);
					out.writeByte(argb >>> 24);
				}
			}
		}
	}

	private static void writeShortLe(DataOutputStream out, int value) throws IOException {
		out.writeByte(value & 0xFF);
		out.writeByte(value >>> 8 & 0xFF);
	}

	private static int[] makeThumbnailPixels(int[] pixels, int width, int height) {
		int thumbWidth = ScreenshotChatCards.THUMB_WIDTH;
		int thumbHeight = ScreenshotChatCards.THUMB_HEIGHT;
		int[] thumbnail = new int[thumbWidth * thumbHeight];
		for (int y = 0; y < thumbHeight; y++) {
			int sourceY = y * height / thumbHeight;
			for (int x = 0; x < thumbWidth; x++) {
				int sourceX = x * width / thumbWidth;
				thumbnail[y * thumbWidth + x] = pixels[sourceY * width + sourceX];
			}
		}
		return thumbnail;
	}

	// 文件名写入截取时的方块坐标；菜单截图没有玩家坐标时省略。
	private static String coordinateSuffix(int blockX, int blockY, int blockZ) {
		if (blockY == Integer.MIN_VALUE) {
			return "";
		}
		return "_x" + blockX + "_y" + blockY + "_z" + blockZ;
	}

	private static File uniqueFile(File directory, String base, String extension) {
		File file = new File(directory, base + "." + extension);
		int suffix = 1;
		while (file.exists()) {
			file = new File(directory, base + "_" + suffix + "." + extension);
			suffix++;
		}
		return file;
	}
}
