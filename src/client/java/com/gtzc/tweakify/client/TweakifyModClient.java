package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.config.FunctionConfigOptions;
import com.gtzc.tweakify.client.config.GeneralConfigOptions;
import com.gtzc.tweakify.client.config.TestingConfigOptions;
import com.gtzc.tweakify.client.config.TweakifyModConfigOptions;
import com.gtzc.tweakify.client.render.AttackRangeState;
import com.gtzc.tweakify.client.render.BetterScreenshotManager;
import com.gtzc.tweakify.client.render.CustomWindowIcon;
import com.gtzc.tweakify.client.render.ExplosionRangeState;
import com.gtzc.tweakify.client.render.LocatorCompass;
import com.gtzc.tweakify.client.render.ScreenshotChatCards;
import com.gtzc.tweakify.client.render.StatusEffectTimerRenderer;
import com.gtzc.tweakify.client.render.WorldOverlayRenderer;
import com.gtzc.tweakify.client.render.BlockMarkerScanner;
import com.gtzc.tweakify.client.state.ChatInputMemoryState;
import com.gtzc.tweakify.client.state.CopyIdFeature;
import com.gtzc.tweakify.client.state.ElytraFireworkState;
import com.gtzc.tweakify.client.state.EnchantmentTooltipDescriptions;
import com.gtzc.tweakify.client.state.FlightDriftState;
import com.gtzc.tweakify.client.state.ForceCrawlState;
import com.gtzc.tweakify.client.state.LockedTradesPayload;
import com.gtzc.tweakify.client.state.LogCleanupState;
import com.gtzc.tweakify.client.state.MerchantMenuAccess;
import com.gtzc.tweakify.client.state.MiningCooldownState;
import com.gtzc.tweakify.client.state.OffhandFallbackState;
import com.gtzc.tweakify.client.state.PiglinArmorSwapState;
import com.gtzc.tweakify.client.state.QuickRightClickPayload;
import com.gtzc.tweakify.client.state.QuickRightClickService;
import com.gtzc.tweakify.client.state.ResourcePackCompatibilitySelfCheck;
import com.gtzc.tweakify.client.state.TickClock;
import com.gtzc.tweakify.client.state.UseCooldownState;
import com.gtzc.tweakify.client.ui.ScrollMemory;
import com.gtzc.tweakify.client.ui.TweakifyModConfigScreen;
import com.gtzc.tweakify.config.AdvancementsStateStorage;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.FavoritesStorage;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import com.gtzc.tweakify.util.HookProbe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.GpuDevice;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TweakifyModClient implements ClientModInitializer, IInitializationHandler {
	private static boolean renderBackendLogged;
	private static boolean remoteServerLogged;

	@Override
	public void onInitializeClient() {
		// 资源包兼容性钩子依赖字符串方法名和私有字段；目标改名时明确报错而不是静默跳过。
		ResourcePackCompatibilitySelfCheck.verify();
		// 日志清理：配置加载完成且允许启动清理时，把 logs 目录的旧归档清到保留数量。
		LogCleanupState.runOnStartup();
		InitializationHandler.getInstance().registerInitializationHandler(this);
		// 各状态机合并到同一个 tick 监听：执行顺序固定，也省掉多监听器的分发开销。
		ClientTickEvents.START_CLIENT_TICK.register(TweakifyModClient::onStartTick);
		// 聊天草稿属于当前世界/服务器会话：断线或切换世界后丢弃，避免跨存档/跨服泄漏。
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			ChatInputMemoryState.beginSession());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			BetterScreenshotManager.cancelHighResolution(client);
			ChatInputMemoryState.endSession();
			// 截图卡片绑定当前会话聊天；断线时释放缩略图纹理，避免累积。
			ScreenshotChatCards.clear();
			// 附魔描述计划缓存键含 RegistryAccess/Component：断线时主动释放。
			EnchantmentTooltipDescriptions.clearCache();
		});
		// 配置写盘节流后的兜底：tick 结束时提交尚未写盘的修改，并把后台写盘结果取回客户端线程；
		// 命令改值后也在这里把 MaLiLib 选项同步一次（服务端线程 → 客户端线程）。
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			BetterScreenshotManager.tick(client);
			TweakifyModConfigOptions.flushPendingSave();
			TweakifyModConfigOptions.pollSaveResults();
			ScrollMemory.tick();
			TweakifyModConfig config = TweakifyMod.config();
			if (config.consumeOptionSyncRequest()) {
				TweakifyModConfigOptions.syncFromConfig();
			}
			// 药水效果计时：在玩家效果 tick 与配置同步之后重建缓存，渲染帧只查表。
			StatusEffectTimerRenderer.tick(client);
			// 自定义窗口图标：配置变化后重新加载，关闭时恢复原版。
			CustomWindowIcon.tick(client);
		});

		// 退出时提交最终状态（不阻塞关闭）：
		// MaLiLib 只在世界加载完成时保存配置，客户端关闭时不会调用 IConfigHandler.save()。
		// 所有快照先提交到共享串行队列，再由 shutdownAsync 的非 daemon 收尾线程在后台排空；
		// 关闭流程不再等待磁盘 IO，慢盘/杀软扫描不会卡住退出界面。
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			long scheduleStartNanos = System.nanoTime();
			BetterScreenshotManager.cancelHighResolution(client);
			ScreenshotChatCards.clear();
			try {
				ScrollMemory.flushOnShutdownAsync();
				FavoritesStorage.flushOnShutdownAsync();
				AdvancementsStateStorage.flushOnShutdownAsync();
				TweakifyModConfigOptions.flushOnShutdownAsync();
			} finally {
				// 30 秒预算：窗口已关闭，延长后台排空只推迟进程退出，不卡界面；
				// 超时后用同步版本兜底，避免慢盘/杀软扫描下丢弃最后一份快照。
				AsyncFileWriter.shutdownAsync(30_000L, TweakifyModClient::flushOnShutdownSynchronously);
			}
			// 这里只记录“提交”耗时；真正的写盘在后台继续，不再阻塞关闭。
			TweakifyMod.LOGGER.info("Shutdown persistence scheduled in {} ms; writes continue in the background",
				(System.nanoTime() - scheduleStartNanos) / 1_000_000L);
		});

		// 基岩高亮与攻击范围合并为一次世界叠加层提交，减少管线切换/draw call。
		LevelRenderEvents.COLLECT_SUBMITS.register(WorldOverlayRenderer::render);

		// 指南针用 Fabric 的 HUD 元素注册（与参考实现同一种注册方式）：
		// addLast 保证排在原版和其它模组的全部 HUD 元素之后，字母永远在最上层。
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "locator_compass"),
			LocatorCompass::renderElement);
		TweakifyMod.LOGGER.info("Locator compass registered via HudElementRegistry.addLast");

		// 不依赖 Mod Menu 也能打开配置界面（Mod Menu 是可选依赖）。
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("fconfig")
				.executes(ctx -> {
					Minecraft.getInstance().setScreenAndShow(new TweakifyModConfigScreen(null));
					return 1;
				}));
			// 聊天卡片的删除按钮通过客户端命令执行，不经过服务端。
			dispatcher.register(ClientCommands.literal("tweakify_screenshot")
				.then(ClientCommands.literal("delete")
					.then(ClientCommands.argument("id", IntegerArgumentType.integer(1))
						.executes(ctx -> ScreenshotChatCards.delete(
							ctx.getArgument("id", Integer.class)) ? 1 : 0))));
		});

		// 掉落物寿命调长依赖 ItemEntity.tick 里对 discard() 的可选重定向。
		// 它用 require = 0 避免拖垮启动，因此改用行为探针确认：
		// 注入成功时首次 tick 会记录 "Mixin hook active: itemDespawnTick"；
		// 反射检查 handler 方法无效，因为 handler 合并成功并不代表注入成功。
		TweakifyMod.LOGGER.info("Item despawn hook uses a behavior probe; watch for "
			+ "'Mixin hook active: itemDespawnTick' after the first item tick");

		// 背包快捷右键：客户端 → 服务端请求打开背包物品；接收器只在集成服务端执行。
		// 远程服务器未安装模组时 canSend 为 false，客户端不会发送也不会拦下原版点击。
		PayloadTypeRegistry.serverboundPlay().register(
			QuickRightClickPayload.TYPE, QuickRightClickPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(QuickRightClickPayload.TYPE, (payload, context) ->
			context.server().execute(() -> QuickRightClickService.open(
				context.player(), payload.containerId(), payload.slot(), payload.sneaking())));

		// 可见交易：注册锁定交易包，并在交易界面打开时把数据合并进 MerchantMenu。
		PayloadTypeRegistry.clientboundPlay().register(LockedTradesPayload.TYPE, LockedTradesPayload.CODEC);
		ClientPlayNetworking.registerGlobalReceiver(LockedTradesPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				Minecraft client = Minecraft.getInstance();
				if (client.gui.screen() instanceof MerchantScreen screen
					&& screen.getMenu() instanceof MerchantMenu menu
					&& menu instanceof MerchantMenuAccess access) {
					access.tweakify$setLockedOffers(payload.offers().orElse(null));
				}
			}));
	}

	// 收尾线程等待超时后的同步兜底：窗口已关闭，宁可多花一点时间也不能丢最后一份数据。
	// 同步版使用更大的提交序号，后台被延迟执行的旧任务会被落盘层的 stale-skip 跳过。
	private static void flushOnShutdownSynchronously() {
		ScrollMemory.flushOnShutdown();
		FavoritesStorage.flushOnShutdown();
		AdvancementsStateStorage.flushOnShutdown();
		TweakifyModConfigOptions.flushOnShutdown();
	}

	// 每 tick 按固定顺序驱动各状态机。
	private static void onStartTick(Minecraft client) {
		// 先推进 tick 序号：状态机写入的标记只在本次 tick 内有效。
		TickClock.advance();
		OffhandFallbackState.tick(client);
		MiningCooldownState.tick(client);
		UseCooldownState.tick(client);
		FlightDriftState.tick(client);
		// 主动爬行：先把按键状态采样成快照，姿态注入在客户端/集成服务端共用。
		ForceCrawlState.tick(client);
		// 基岩高亮与屏障可见共用一次方块扫描。
		BlockMarkerScanner.tick(client);
		ElytraFireworkState.tick(client);
		PiglinArmorSwapState.tick(client);
		AttackRangeState.tick(client);
		ExplosionRangeState.tick(client);
		// 设备就绪后记录一次渲染后端，便于排查 Vulkan/OpenGL 差异。
		logRenderBackendOnce();
		// 远程服务器下服务端类功能不会生效，提示一次避免以为是开关失效。
		logRemoteServerOnce(client);
	}

	// 只在连上远程服务器（非集成服务端）时提示一次：服务端侧注入不在对端运行。
	private static void logRemoteServerOnce(Minecraft client) {
		if (remoteServerLogged || client.level == null || client.player == null
			|| client.getConnection() == null) {
			return;
		}
		if (client.getSingleplayerServer() != null) {
			return;
		}
		remoteServerLogged = true;
		TweakifyMod.LOGGER.warn("Connected to a remote server: server-side features (anvil, shovel snow, item despawn, "
			+ "slime spawn, farmland, pick block, smithing, iron golem minecart, bone meal, active crawling, "
			+ "thrown spawn eggs, cobweb/fluid/slime/honey physics, frost walker jump fix, "
			+ "inventory quick right click) only work in single player or on a LAN host");
	}

	private static void logRenderBackendOnce() {
		if (renderBackendLogged) {
			return;
		}
		GpuDevice device = RenderSystem.tryGetDevice();
		if (device == null) {
			return;
		}
		renderBackendLogged = true;
		DeviceInfo info = device.getDeviceInfo();
		String backend = info.backendName();
		TweakifyMod.LOGGER.info("Render backend: {} ({})", backend, info.name());
		if (backend != null && backend.toLowerCase(Locale.ROOT).contains("vulkan")) {
			TweakifyMod.LOGGER.info("Vulkan backend detected: world overlays share a single custom geometry submit");
		}
	}

	@Override
	public void registerModHandlers() {
		ConfigManager.getInstance().registerConfigHandler(TweakifyMod.MOD_ID, new TweakifyModConfigOptions());
		Registry.CONFIG_SCREEN.registerConfigScreenFactory(
			new ModInfo(TweakifyMod.MOD_ID, "Tweakify Mod", TweakifyModConfigScreen::new));

		// 注册快捷键：必须在 registerModHandlers 中进行，此时 MaLiLib 管理器已就绪。
		InputEventHandler.getKeybindManager().registerKeybindProvider(new TweakifyModKeybindProvider());

		// 打开配置界面快捷键：配置界面已打开时按同键关闭，否则打开。
		GeneralConfigOptions.OPEN_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui.screen() instanceof TweakifyModConfigScreen) {
				client.gui.setScreen(null);
			} else {
				client.setScreenAndShow(new TweakifyModConfigScreen(null));
			}
			return true;
		});

		// 复制物体 ID：开关自身的热键沿用 MaLiLib 默认的“切换开关”，
		// 真正执行复制的是子设置 copyTargetId 的快捷键（界面打开时也能用）。
		FunctionConfigOptions.COPY_TARGET_ID_HOTKEY.getKeybind().setCallback((action, key) -> {
			if (!FunctionConfigOptions.COPY_ITEM_ID.getBooleanValue()) {
				return false;
			}
			CopyIdFeature.copyTargetId();
			return true;
		});

		// 探针报表（测试标签）：把已激活的 Mixin 行为探针打到聊天栏或剪贴板，
		// 排查“功能开了却没反应”时可直接确认钩子是否真的注入。
		TestingConfigOptions.DEBUG_PROBE_REPORT.getKeybind().setCallback((action, key) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui == null) {
				return false;
			}
			List<String> hooks = HookProbe.reportedHooks();
			if (TestingConfigOptions.DEBUG_PROBE_REPORT_COPY.getBooleanValue()) {
				client.keyboardHandler.setClipboard(hooks.isEmpty() ? "" : String.join("\n", hooks));
				client.gui.chatListener().handleSystemMessage(Component.literal(hooks.isEmpty()
					? "[Tweakify] no mixin hook has fired yet (clipboard cleared)"
					: "[Tweakify] copied " + hooks.size() + " active hooks to the clipboard"), false);
				return true;
			}
			if (hooks.isEmpty()) {
				client.gui.chatListener().handleSystemMessage(
					Component.literal("[Tweakify] no mixin hook has fired yet"), false);
				return true;
			}
			client.gui.chatListener().handleSystemMessage(
				Component.literal("[Tweakify] active hooks: " + hooks.size()), false);
			for (String hook : hooks) {
				client.gui.chatListener().handleSystemMessage(
					Component.literal(" - " + hook), false);
			}
			return true;
		});

		// 已启用功能报表（测试标签）：只列根级布尔开关，用于快速核对当前配置组合。
		TestingConfigOptions.DEBUG_ENABLED_FEATURES_REPORT.getKeybind().setCallback((action, key) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui == null) {
				return false;
			}
			List<String> enabled = new ArrayList<>();
			for (ConfigCatalog.Entry entry : ConfigCatalog.commandEntries()) {
				if (entry.isRoot()
					&& TweakifyMod.config().getBoolean(entry.name())) {
					enabled.add(entry.name());
				}
			}
			if (enabled.isEmpty()) {
				client.gui.chatListener().handleSystemMessage(
					Component.literal("[Tweakify] no root toggle is enabled"), false);
				return true;
			}
			// 可选输出到剪贴板：与服务端探针报表的行为保持一致。
			if (TestingConfigOptions.DEBUG_ENABLED_FEATURES_REPORT_COPY.getBooleanValue()) {
				client.keyboardHandler.setClipboard(String.join("\n", enabled));
				client.gui.chatListener().handleSystemMessage(Component.literal(
					"[Tweakify] copied " + enabled.size() + " enabled root toggles to the clipboard"), false);
				return true;
			}
			client.gui.chatListener().handleSystemMessage(
				Component.literal("[Tweakify] enabled root toggles: " + enabled.size()), false);
			for (String name : enabled) {
				client.gui.chatListener().handleSystemMessage(Component.literal(" - " + name), false);
			}
			return true;
		});
	}
}