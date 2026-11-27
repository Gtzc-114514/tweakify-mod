package com.gtzc.tweakify;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 配置替换的测试接缝：{@link TweakifyMod} 的字段保持私有，测试统一经本类写入，
 * 包括写入 {@code null} 模拟“配置尚未加载”的场景。
 *
 * <p>本类只存在于 test 源集，生产代码无法引用。
 */
public final class TweakifyModTestSupport {
	private TweakifyModTestSupport() {
	}

	public static void installConfig(TweakifyModConfig config) {
		TweakifyMod.setConfigForTest(config);
	}
}
