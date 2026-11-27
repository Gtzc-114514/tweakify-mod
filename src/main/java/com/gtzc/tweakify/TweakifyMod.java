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

	// volatile：/tweakify 命令在集成服务端线程执行，客户端线程同时在读配置。
	// 字段私有：读取统一走 config()，写入只发生在 onInitialize 与测试接缝。
	private static volatile TweakifyModConfig CONFIG;

	/**
	 * 当前配置。模组初始化完成前可能为 {@code null}，调用方需按原逻辑兜底。
	 */
	public static TweakifyModConfig config() {
		return CONFIG;
	}

	// 包私有测试接缝：仅 test 源集的 TweakifyModTestSupport 访问。
	static void setConfigForTest(TweakifyModConfig config) {
		CONFIG = config;
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
