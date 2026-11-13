package com.gtzc.tweakify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 通用分区：界面语言、打开配置界面的快捷键、标签与滚动列表记忆。
 *
 * <p>这些开关不影响游戏行为，只作用于配置界面本身；
 * 语言覆盖与滚动记忆由客户端状态类读取。
 */
public final class GeneralConfig extends ConfigSection {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 已登记字段级校验器的字段名：language 需要回退到支持的语言。
	private static final List<String> SANITIZED_FIELDS = List.of("language");

	// 模组界面语言代码（zh_cn / en_us / ru_ru / fr_fr）；跟随系统语言时忽略。
	public volatile String language = "zh_cn";

	// 跟随系统语言：开启时使用游戏当前语言，忽略 language 设置。
	public volatile boolean followSystemLanguage = true;

	// 滚动列表记忆：关闭界面时记住列表滚动位置，下次打开直接回到该位置。
	public volatile boolean scrollListMemory = true;

	// 跨重启记忆：把位置写入配置文件，下次启动仍生效。
	public volatile boolean scrollListMemoryPersist = false;

	// 子开关：记忆 MaLiLib 配置界面的滚动位置。
	public volatile boolean malilibScrollMemory = true;

	// 子开关：记忆 Mod Menu 模组列表的滚动位置。
	public volatile boolean modMenuScrollMemory = true;

	// 标签记忆：记住上次选择的分类标签，下次打开配置界面直接停在该标签。
	public volatile boolean rememberSelectedTab = false;

	// 打开配置界面的快捷键，默认 G + C。
	public volatile String hotkeyOpenConfigGui = "G,C";

	GeneralConfig() {
		// 启动期校验：新增枚举/数值字段忘记登记校验器时直接失败。
		assertSanitizerCoverage(SANITIZED_FIELDS);
	}

	@Override
	boolean sanitize() {
		return sanitizeLanguage();
	}

	@Override
	List<String> sanitizedFieldNames() {
		return SANITIZED_FIELDS;
	}

	// 修正不合法的语言代码，回退到默认的中文。
	private boolean sanitizeLanguage() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_LANGUAGES, language)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported language in config: {}", language);
		language = "zh_cn";
		return true;
	}
}
