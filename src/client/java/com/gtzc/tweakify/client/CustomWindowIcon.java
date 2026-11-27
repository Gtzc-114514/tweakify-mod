package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.WindowInvoker;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * 自定义游戏图标：把指定路径的 PNG 应用到窗口图标（Windows 下同时作用于任务栏）。
 *
 * <p>每次客户端 tick 检查一次配置：路径变化或刚开启时重新加载；关闭开关时恢复原版图标。
 * 只接受 16 ~ 64 像素的 PNG；失败时保留当前图标并在日志里说明一次，避免刷屏。
 */
public final class CustomWindowIcon {
	static final int MIN_ICON_SIZE = 16;
	static final int MAX_ICON_SIZE = 64;
	private static final byte[] PNG_SIGNATURE = {
		(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

	// 当前是否已应用自定义图标，以及成功/失败对应的原始配置值与文件修改时间；
	// 修改时间用于在文件被替换后自动重试，同时避免每 tick 重复加载/报错。
	private static boolean customApplied;
	private static String appliedPath;
	private static long appliedModifiedTime = Long.MIN_VALUE;
	private static String failedPath;
	private static long failedModifiedTime = Long.MIN_VALUE;

	private CustomWindowIcon() {
	}

	// 客户端 tick 入口：配置变化在下一 tick 生效。
	static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		if (client == null) {
			return;
		}
		Window window = client.getWindow();
		if (window == null) {
			return;
		}
		if (!config.modified.customWindowIcon) {
			if (customApplied) {
				restoreVanillaIcon(client, window);
			}
			return;
		}
		String rawPath = config.modified.customWindowIconPath;
		if (rawPath == null || rawPath.isBlank()) {
			// 清空路径视为不再指定图标：已应用的自定义图标恢复原版。
			if (customApplied) {
				restoreVanillaIcon(client, window);
			}
			return;
		}
		String trimmed = rawPath.trim();
		if (customApplied && trimmed.equals(appliedPath)
			&& currentModifiedTime(client, trimmed) == appliedModifiedTime) {
			return;
		}
		if (trimmed.equals(failedPath) && currentModifiedTime(client, trimmed) == failedModifiedTime) {
			return;
		}
		if (applyCustomIcon(client, window, trimmed)) {
			customApplied = true;
			appliedPath = trimmed;
			appliedModifiedTime = currentModifiedTime(client, trimmed);
			failedPath = null;
			failedModifiedTime = Long.MIN_VALUE;
		} else {
			failedPath = trimmed;
			failedModifiedTime = currentModifiedTime(client, trimmed);
		}
	}

	// 配置路径对应文件的最后修改时间；路径无效或文件不存在时返回 -1（文件出现后会变化并触发重试）。
	private static long currentModifiedTime(Minecraft client, String rawPath) {
		Path path = resolvePath(client.gameDirectory.toPath(), rawPath);
		if (path == null) {
			return -1L;
		}
		try {
			return Files.isRegularFile(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
		} catch (IOException e) {
			return -1L;
		}
	}

	// 解析配置路径：绝对路径原样使用，相对路径按游戏目录解析。
	static Path resolvePath(Path gameDirectory, String rawPath) {
		if (rawPath == null || rawPath.isBlank() || gameDirectory == null) {
			return null;
		}
		try {
			Path path = Path.of(rawPath.trim());
			return path.isAbsolute() ? path.normalize() : gameDirectory.resolve(path).normalize();
		} catch (InvalidPathException e) {
			return null;
		}
	}

	// PNG 尺寸（宽、高）；不是合法 PNG 时返回 null。只解析签名与 IHDR，足够做尺寸校验。
	static int[] pngSize(byte[] data) {
		if (data == null || data.length < 24) {
			return null;
		}
		for (int i = 0; i < PNG_SIGNATURE.length; i++) {
			if (data[i] != PNG_SIGNATURE[i]) {
				return null;
			}
		}
		if (readInt(data, 8) != 13 || data[12] != 'I' || data[13] != 'H'
			|| data[14] != 'D' || data[15] != 'R') {
			return null;
		}
		int width = readInt(data, 16);
		int height = readInt(data, 20);
		if (width <= 0 || height <= 0) {
			return null;
		}
		return new int[] {width, height};
	}

	// 参考原版图标集：16 / 32 / 48 / 64 像素都在支持范围内。
	static boolean isSupportedSize(int width, int height) {
		return width >= MIN_ICON_SIZE && width <= MAX_ICON_SIZE
			&& height >= MIN_ICON_SIZE && height <= MAX_ICON_SIZE;
	}

	private static boolean applyCustomIcon(Minecraft client, Window window, String rawPath) {
		Path path = resolvePath(client.gameDirectory.toPath(), rawPath);
		if (path == null) {
			TweakifyMod.LOGGER.warn("Ignoring invalid custom window icon path: {}", rawPath);
			return false;
		}
		if (!Files.isRegularFile(path)) {
			TweakifyMod.LOGGER.warn("Custom window icon file does not exist: {}", path);
			return false;
		}
		try {
			byte[] bytes = Files.readAllBytes(path);
			int[] size = pngSize(bytes);
			if (size == null) {
				TweakifyMod.LOGGER.warn("Custom window icon is not a valid PNG: {}", path);
				return false;
			}
			if (!isSupportedSize(size[0], size[1])) {
				TweakifyMod.LOGGER.warn("Custom window icon must be {}~{} pixels, got {}x{}: {}",
					MIN_ICON_SIZE, MAX_ICON_SIZE, size[0], size[1], path);
				return false;
			}
			IoSupplier<InputStream> icon = () -> new ByteArrayInputStream(bytes);
			((WindowInvoker) (Object) window).tweakify$setIcon(List.of(icon));
			TweakifyMod.LOGGER.info("Applied custom window icon {} ({}x{})", path, size[0], size[1]);
			return true;
		} catch (IOException | RuntimeException e) {
			TweakifyMod.LOGGER.warn("Failed to apply custom window icon {}", path, e);
			return false;
		}
	}

	// 关闭功能时恢复原版图标（与 Minecraft 启动时的资源与图标集保持一致）。
	private static void restoreVanillaIcon(Minecraft client, Window window) {
		try {
			PackResources resources = client.getVanillaPackResources().fullResources();
			IconSet iconSet = SharedConstants.getCurrentVersion().stable()
				? IconSet.RELEASE : IconSet.SNAPSHOT;
			window.setIcon(resources, iconSet);
			TweakifyMod.LOGGER.info("Restored the vanilla window icon");
		} catch (IOException | RuntimeException e) {
			TweakifyMod.LOGGER.warn("Failed to restore the vanilla window icon", e);
		} finally {
			customApplied = false;
			appliedPath = null;
			appliedModifiedTime = Long.MIN_VALUE;
			failedPath = null;
			failedModifiedTime = Long.MIN_VALUE;
		}
	}

	// 大端序读取 4 字节整数（PNG 的 IHDR 宽度/高度）。
	private static int readInt(byte[] data, int offset) {
		return (data[offset] & 0xFF) << 24
			| (data[offset + 1] & 0xFF) << 16
			| (data[offset + 2] & 0xFF) << 8
			| (data[offset + 3] & 0xFF);
	}
}
