package com.gtzc.tweakify;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 配置替换的测试接缝：{@link TweakifyMod} 的字段保持私有，测试统一经本类写入。
 *
 * <p>传入 {@code null} 表示重置为默认实例（模拟「配置尚未加载」）：
 * {@code config()} 恒非 null，但 {@link TweakifyMod#isConfigLoaded()} 会返回 false。
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
