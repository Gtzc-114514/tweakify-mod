package com.gtzc.tweakify;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.gtzc.tweakify.config.TweakifyModConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TweakifyMod implements ModInitializer {
	public static final String MOD_ID = "tweakify-mod";

	// 与模组 ID 同名，日志会带上模组名便于排查。
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 占位默认实例：配置加载完成前（以及加载失败时）的安全兜底，保证 config() 恒非 null。
	// 只做字段初始化与缓存重建，不填充默认物品列表：本类初始化可能被语言 mixin 在
	// 注册表初始化期间回调，扫描注册表会与第三方 mixin（如 ETF）形成初始化循环。
	// 加载完成前按默认值生效（不再依赖 null 回退）；isConfigLoaded() 只用于阻止默认值
	// 在 onInitialize 之前回写磁盘或同步界面，不参与功能判定。
	private static final TweakifyModConfig DEFAULTS = TweakifyModConfig.createPlaceholderInstance();

	// volatile：/tweakify 命令在集成服务端线程执行，客户端线程同时在读配置。
	// 字段私有：读取统一走 config()，写入只发生在 onInitialize 与测试接缝。
	private static volatile TweakifyModConfig CONFIG = DEFAULTS;

	/**
	 * 当前配置；恒非 null，配置加载完成前返回默认实例。
	 */
	public static TweakifyModConfig config() {
		return CONFIG;
	}

	/**
	 * 真实配置是否已加载完成。写盘与选项同步等配置核心逻辑用它区分默认实例，
	 * 避免在 onInitialize 之前把默认值写回磁盘。
	 */
	public static boolean isConfigLoaded() {
		return CONFIG != DEFAULTS;
	}

	// 包私有测试接缝：仅 test 源集的 TweakifyModTestSupport 访问。
	// 传入 null 时重置为默认实例，仍可模拟「配置尚未加载」。
	static void setConfigForTest(TweakifyModConfig config) {
		CONFIG = config != null ? config : DEFAULTS;
	}

	@Override
	public void onInitialize() {
		// 模组加载就绪时执行；此时资源可能尚未初始化，只做配置与命令注册。
		CONFIG = TweakifyModConfig.load();

		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> TweakifyModCommands.register(dispatcher, registryAccess));

		LOGGER.info("Tweakify Mod initialized");
	}
}
