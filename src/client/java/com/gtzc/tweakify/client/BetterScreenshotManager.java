package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.WindowInvoker;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
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
 * <p>截图回调在渲染线程触发；像素复制与坐标采样后立即把编码和磁盘 IO
 * 移到 IO 线程，聊天卡片/纹理注册再通过 {@link Minecraft#execute} 回渲染线程。
 */
public final class BetterScreenshotManager {
	private static final int FOUR_K_WIDTH = 3840;
	private static final int FOUR_K_HEIGHT = 2160;
	private static final DateTimeFormatter FILE_TIME =
		DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
	private static final HighResScreenshotState HIGH_RES_STATE = new HighResScreenshotState();
	private static int highResOriginalWidth;
	private static int highResOriginalHeight;
	private static int highResOriginalWindowWidth;
	private static int highResOriginalWindowHeight;
	private static int highResOriginalWindowedX;
	private static int highResOriginalWindowedY;
	private static int highResOriginalWindowedWidth;
	private static int highResOriginalWindowedHeight;
	private static boolean highResWasFullscreen;
	private static boolean highResFullscreenPreference;
	private static TweakifyModConfig highResConfig;

	private BetterScreenshotManager() {
	}

	public static void capture(Minecraft client, boolean fourK) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.betterScreenshot) {
			return;
		}
		if (HIGH_RES_STATE.isPending()) {
			if (!fourK) {
				notifyBusy(client);
			}
			return;
		}
		if (fourK) {
			requestHighResolution(client, config);
		} else {
			captureNow(client, false, config);
		}
	}

	private static void captureNow(Minecraft client, boolean fourK,
								 TweakifyModConfig config) {
		try {
			Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), 1,
				image -> save(client, image, fourK, config));
		} catch (RuntimeException error) {
			TweakifyMod.LOGGER.error("Failed to request screenshot capture", error);
		}
	}

	// F9：临时把窗口与主渲染目标一起切到 4K，等待至少两帧后再抓取。
	private static void requestHighResolution(Minecraft client, TweakifyModConfig config) {
		RenderTarget target = client.gameRenderer.mainRenderTarget();
		if (target.width == FOUR_K_WIDTH && target.height == FOUR_K_HEIGHT
			&& client.getWindow().getWidth() == FOUR_K_WIDTH
			&& client.getWindow().getHeight() == FOUR_K_HEIGHT) {
			captureNow(client, true, config);
			return;
		}
		Window window = client.getWindow();
		WindowInvoker invoker = (WindowInvoker) (Object) window;
		highResOriginalWidth = target.width;
		highResOriginalHeight = target.height;
		highResOriginalWindowWidth = window.getScreenWidth();
		highResOriginalWindowHeight = window.getScreenHeight();
		highResOriginalWindowedX = invoker.tweakify$getWindowedX();
		highResOriginalWindowedY = invoker.tweakify$getWindowedY();
		highResOriginalWindowedWidth = invoker.tweakify$getWindowedWidth();
		highResOriginalWindowedHeight = invoker.tweakify$getWindowedHeight();
		highResWasFullscreen = invoker.tweakify$isWindowFullscreen();
		highResFullscreenPreference = client.options.fullscreen().get();
		highResConfig = config;
		HIGH_RES_STATE.beginRequest(100);
		try {
			window.setWindowed(logicalPixelSize(FOUR_K_WIDTH, window),
				logicalPixelSize(FOUR_K_HEIGHT, window));
			client.gameRenderer.resize(FOUR_K_WIDTH, FOUR_K_HEIGHT);
		} catch (RuntimeException error) {
			restoreResolution(client);
			notifyError(client, error.getMessage());
		}
	}

	private static int logicalPixelSize(int pixels, Window window) {
		float density = window.getPixelDensity();
		if (!Float.isFinite(density) || density <= 0.0f) {
			return pixels;
		}
		return Math.max(1, Math.round(pixels / density));
	}

	public static void onHighResolutionFrameRendered() {
		HIGH_RES_STATE.onFrameRendered();
	}

	public static void tick(Minecraft client) {
		if (!HIGH_RES_STATE.isActive()) {
			return;
		}
		RenderTarget target = client.gameRenderer.mainRenderTarget();
		boolean targetReady = target.width == FOUR_K_WIDTH && target.height == FOUR_K_HEIGHT
			&& client.getWindow().getWidth() == FOUR_K_WIDTH
			&& client.getWindow().getHeight() == FOUR_K_HEIGHT;
		switch (HIGH_RES_STATE.tick(targetReady)) {
			case IDLE, WAIT -> {
			}
			case RESTORE -> restoreResolution(client);
			case TIMEOUT -> failHighResolution(client, "4K screenshot timed out");
			case READY -> {
				if (client.gui.screen() != null) {
					failHighResolution(client, "4K screenshot cancelled: screen opened");
					return;
				}
				int attempt = HIGH_RES_STATE.beginCapture();
				if (attempt < 0) {
					return;
				}
				TweakifyModConfig config = highResConfig;
				highResConfig = null;
				try {
					Screenshot.takeScreenshot(target, 1, image -> {
						if (!HIGH_RES_STATE.onCaptureCallback(attempt)) {
							image.close();
							return;
						}
						// 在下一 tick 恢复窗口，避免截图回调所在帧重新分配渲染目标。
						save(client, image, true, config);
					});
				} catch (RuntimeException error) {
					restoreResolution(client);
					notifyError(client, error.getMessage());
				}
			}
		}
	}

	public static void cancelHighResolution(Minecraft client) {
		if (HIGH_RES_STATE.isActive()) {
			restoreResolution(client);
		}
	}

	private static void failHighResolution(Minecraft client, String message) {
		restoreResolution(client);
		notifyError(client, message);
	}

	private static void restoreResolution(Minecraft client) {
		if (!HIGH_RES_STATE.isActive()) {
			return;
		}
		HIGH_RES_STATE.beginRestore();
		Window window = client.getWindow();
		WindowInvoker invoker = (WindowInvoker) (Object) window;
		try {
			try {
				window.setWindowed(highResOriginalWindowWidth, highResOriginalWindowHeight);
			} catch (RuntimeException error) {
				logRestoreError("window size", error);
			}
			if (highResWasFullscreen) {
				try {
					window.setFullscreen(true);
					invoker.tweakify$setMode();
				} catch (RuntimeException error) {
					logRestoreError("fullscreen", error);
				}
			}
			try {
				client.gameRenderer.resize(highResOriginalWidth, highResOriginalHeight);
			} catch (RuntimeException error) {
				logRestoreError("render target", error);
			}
			try {
				invoker.tweakify$setWindowedX(highResOriginalWindowedX);
				invoker.tweakify$setWindowedY(highResOriginalWindowedY);
				invoker.tweakify$setWindowedWidth(highResOriginalWindowedWidth);
				invoker.tweakify$setWindowedHeight(highResOriginalWindowedHeight);
			} catch (RuntimeException error) {
				logRestoreError("windowed geometry", error);
			}
			try {
				if (client.options.fullscreen().get() != highResFullscreenPreference) {
					client.options.fullscreen().set(highResFullscreenPreference);
					client.options.save();
				}
			} catch (RuntimeException error) {
				logRestoreError("fullscreen option", error);
			}
		} finally {
			HIGH_RES_STATE.completeRestore();
			highResConfig = null;
		}
	}

	private static void logRestoreError(String step, RuntimeException error) {
		TweakifyMod.LOGGER.error("Failed to restore {} after 4K screenshot", step, error);
	}

	private static void notifyBusy(Minecraft client) {
		if (client.gui != null && client.gui.hud != null) {
			client.gui.hud.getChat().addClientSystemMessage(
				Component.translatable("tweakify-mod.chat.screenshot.busy"));
		}
	}

	private static void notifyError(Minecraft client, String message) {
		if (client.gui != null && client.gui.hud != null) {
			client.gui.hud.getChat().addClientSystemMessage(
				Component.translatable("tweakify-mod.chat.screenshot.saveFailed", message));
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
			if (fourK && (width != FOUR_K_WIDTH || height != FOUR_K_HEIGHT)) {
				throw new IOException("4K render target unavailable: " + width + "x" + height);
			}
			BufferedImage buffered = toBufferedImage(pixels, width, height);
			write(format, buffered, file);
			File savedFile = file;
			if (config.functions.betterScreenshotChatCard) {
				int[] thumbnail = makeThumbnailPixels(pixels, width, height);
				client.execute(() -> ScreenshotChatCards.add(savedFile, thumbnail,
					blockX, blockY, blockZ));
			}
			TweakifyMod.LOGGER.info("Saved screenshot: {} ({}x{})",
				savedFile.getName(), buffered.getWidth(), buffered.getHeight());
		} catch (Exception error) {
			if (file != null) {
				file.delete();
			}
			TweakifyMod.LOGGER.error("Failed to save screenshot", error);
			client.execute(() -> notifyError(client, error.getMessage()));
		}
	}

	private static BufferedImage toBufferedImage(int[] pixels, int width, int height) {
		BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		buffered.setRGB(0, 0, width, height, pixels, 0, width);
		return buffered;
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
