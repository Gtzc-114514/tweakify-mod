package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.Locale;

public class TweakifyModClient implements ClientModInitializer, IInitializationHandler {
	private static boolean renderBackendLogged;
	private static boolean remoteServerLogged;

	@Override
	public void onInitializeClient() {
		// 资源包兼容性钩子依赖字符串方法名和私有字段；目标改名时明确报错而不是静默跳过。
		ResourcePackCompatibilitySelfCheck.verify();
		InitializationHandler.getInstance().registerInitializationHandler(this);
		// 各状态机合并到同一个 tick 监听：执行顺序固定，也省掉多监听器的分发开销。
		ClientTickEvents.START_CLIENT_TICK.register(TweakifyModClient::onStartTick);
		// 聊天草稿属于当前世界/服务器会话：断线或切换世界后丢弃，避免跨存档/跨服泄漏。
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			ChatInputMemoryState.beginSession());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			ChatInputMemoryState.endSession());
		// 配置写盘节流后的兜底：tick 结束时提交尚未写盘的修改，并把后台写盘结果取回客户端线程；
		// 命令改值后也在这里把 MaLiLib 选项同步一次（服务端线程 → 客户端线程）。
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TweakifyModConfigOptions.flushPendingSave();
			TweakifyModConfigOptions.pollSaveResults();
			ScrollMemory.tick();
			TweakifyModConfig config = TweakifyMod.CONFIG;
			if (config != null && config.consumeOptionSyncRequest()) {
				TweakifyModConfigOptions.syncFromConfig();
			}
		});

		// 退出前排空后台写盘队列并同步落盘：
		// MaLiLib 只在世界加载完成时保存配置，客户端关闭时不会调用 IConfigHandler.save()。
		// 顺序：先让滚动记忆在同一队列中写完，再让配置写最终快照；
		// 配置超时兜底会关闭写盘线程并同步写，因此必须放在滚动记忆提交之后。
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ScrollMemory.flushOnShutdown();
			TweakifyModConfigOptions.flushOnShutdown();
			AsyncFileWriter.shutdown(2000L);
		});

		// 基岩高亮与攻击范围合并为一次世界叠加层提交，减少管线切换/draw call。
		LevelRenderEvents.COLLECT_SUBMITS.register(WorldOverlayRenderer::render);

		// 不依赖 Mod Menu 也能打开配置界面（Mod Menu 是可选依赖）。
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal("fconfig")
				.executes(ctx -> {
					Minecraft.getInstance().setScreenAndShow(new TweakifyModConfigScreen(null));
					return 1;
				})));

		// 掉落物寿命调长依赖 ItemEntity.tick 里对 discard() 的可选重定向。
		// 它用 require = 0 避免拖垮启动，因此改用行为探针确认：
		// 注入成功时首次 tick 会记录 "Mixin hook active: itemDespawnTick"；
		// 反射检查 handler 方法无效，因为 handler 合并成功并不代表注入成功。
		TweakifyMod.LOGGER.info("Item despawn hook uses a behavior probe; watch for "
			+ "'Mixin hook active: itemDespawnTick' after the first item tick");

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
			+ "thrown spawn eggs) only work in single player or on a LAN host");
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
		TweakifyModConfigOptions.OPEN_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
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
		TweakifyModConfigOptions.COPY_TARGET_ID_HOTKEY.getKeybind().setCallback((action, key) -> {
			if (!TweakifyModConfigOptions.COPY_ITEM_ID.getBooleanValue()) {
				return false;
			}
			CopyIdFeature.copyTargetId();
			return true;
		});
	}
}