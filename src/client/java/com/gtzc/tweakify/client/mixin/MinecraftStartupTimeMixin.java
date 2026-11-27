package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.StartupTimeRules;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.management.ManagementFactory;

/**
 * 启动时间（参考 startup-time）：首次加载完成时显示 JVM 启动到当前时刻的耗时。
 *
 * <p>用 JVM uptime 而不是客户端内部计时，能覆盖模组加载与资源加载的全部阶段，
 * 显示的是“启动游戏”的耗时，而不是进入存档的耗时；每次进程只报告一次，
 * 后续资源包重载不会重复弹出。
 */
@Mixin(Minecraft.class)
public class MinecraftStartupTimeMixin {
	@Unique
	private static boolean tweakify$startupTimeReported;

	// 专用通知 id：与资源包加载失败等系统通知分开排队，避免启动耗时被替换或推迟到进入世界后才显示。
	@Unique
	private static final SystemToast.SystemToastId TWEAKIFY$STARTUP_TIME_TOAST =
		new SystemToast.SystemToastId(5000L);

	@Inject(method = "onGameLoadFinished", at = @At("RETURN"))
	private void tweakify$reportStartupTime(GameLoadCookie cookie, CallbackInfo ci) {
		if (tweakify$startupTimeReported) {
			return;
		}
		tweakify$startupTimeReported = true;
		TweakifyModConfig config = TweakifyMod.config();
		if (!TweakifyMod.isConfigLoaded() || !config.general.startupTime) {
			return;
		}
		long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
		String seconds = StartupTimeRules.formatSeconds(uptimeMillis);
		if (config.general.startupTimeLog) {
			TweakifyMod.LOGGER.info("Minecraft startup finished in {} s (JVM uptime)", seconds);
		}
		Minecraft client = (Minecraft) (Object) this;
		// 延迟到下一 tick：主菜单已构建并开始渲染，Toast 不会在初始屏幕切换时被清掉，
		// 也不会拖到进入存档后才出现。
		client.execute(() -> {
			if (client.gui != null) {
				SystemToast.add(client.gui.toastManager(), TWEAKIFY$STARTUP_TIME_TOAST,
					Component.translatable("tweakify-mod.message.startup_time", seconds),
					Component.literal(""));
			}
		});
	}
}
