package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 日志清理：启动时自动清理与配置界面的“立刻清理”按钮共用同一套清理逻辑。
 *
 * <p>只处理 logs 目录下的日期编号归档（见 {@link LogCleanupRules}），
 * latest.log 永远不会被删除；删除失败（文件被占用等）只记录日志，不中断其余文件。
 */
public final class LogCleanupState {
	private LogCleanupState() {
	}

	// 客户端入口调用：总开关与启动清理子开关都开启时执行一次。
	public static void runOnStartup() {
		TweakifyModConfig config = TweakifyMod.config();
		if (!TweakifyMod.isConfigLoaded() || !config.general.logCleanup
			|| !config.general.logCleanupOnStartup) {
			return;
		}
		CleanupResult result = cleanup(logsDirectory(), config.general.logCleanupKeepCount);
		TweakifyMod.LOGGER.info("Log cleanup: scanned {} archived log file(s), deleted {}, kept {}",
			result.scanned(), result.deleted(), result.kept());
	}

	// 按钮入口：不受总开关限制，按当前保留数量立即清理，并以 Toast 反馈结果。
	public static void cleanupNowAndNotify(Minecraft client) {
		CleanupResult result = cleanup(
			logsDirectory(), TweakifyMod.config().general.logCleanupKeepCount);
		TweakifyMod.LOGGER.info("Log cleanup: scanned {} archived log file(s), deleted {}, kept {}",
			result.scanned(), result.deleted(), result.kept());
		if (client.gui == null) {
			return;
		}
		SystemToast.add(client.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
			Component.translatable("tweakify-mod.message.log_cleanup.title"),
			Component.translatable("tweakify-mod.message.log_cleanup.result",
				result.deleted(), result.kept()));
	}

	// 包私有：清理入口，测试可注入临时目录，不依赖 FabricLoader。
	static CleanupResult cleanup(Path logsDirectory, int keepCount) {
		if (logsDirectory == null || !Files.isDirectory(logsDirectory)) {
			return new CleanupResult(0, 0, 0);
		}
		List<Path> archives = new ArrayList<>();
		try (Stream<Path> stream = Files.list(logsDirectory)) {
			stream.filter(Files::isRegularFile)
				.filter(path -> LogCleanupRules.isArchivedLog(path.getFileName().toString()))
				.forEach(archives::add);
		} catch (IOException e) {
			TweakifyMod.LOGGER.warn("Log cleanup: failed to list {}", logsDirectory, e);
			return new CleanupResult(0, 0, 0);
		}
		int scanned = archives.size();
		List<Path> toDelete = LogCleanupRules.selectForDeletion(archives, keepCount);
		int deleted = 0;
		for (Path path : toDelete) {
			try {
				// 只在确实删除成功时计数：文件被其它进程先删掉时不计入，提示数字与结果一致。
				if (Files.deleteIfExists(path)) {
					deleted++;
				}
			} catch (IOException e) {
				TweakifyMod.LOGGER.warn("Log cleanup: failed to delete {}", path, e);
			}
		}
		return new CleanupResult(scanned, deleted, scanned - deleted);
	}

	private static Path logsDirectory() {
		return FabricLoader.getInstance().getGameDir().resolve("logs");
	}

	public record CleanupResult(int scanned, int deleted, int kept) {
	}
}
