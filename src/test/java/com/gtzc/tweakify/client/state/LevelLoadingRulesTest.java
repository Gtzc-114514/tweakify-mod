package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 强制关闭加载界面：只有功能开启且未勾选“保留首次加载界面”时才隐藏渲染。
 */
class LevelLoadingRulesTest {

	@Test
	void hideOnlyWhenFeatureEnabledAndFirstScreenNotKept() {
		TweakifyModConfig previous = TweakifyMod.config();
		try {
			TweakifyModTestSupport.installConfig(config(false, false));
			assertFalse(LevelLoadingRules.shouldHide(), "功能关闭时保持原版加载界面");

			TweakifyModTestSupport.installConfig(config(true, false));
			assertTrue(LevelLoadingRules.shouldHide(), "开启且不保留首次界面时隐藏");

			TweakifyModTestSupport.installConfig(config(true, true));
			assertFalse(LevelLoadingRules.shouldHide(), "勾选保留首次界面时不隐藏");

			TweakifyModTestSupport.installConfig(config(false, true));
			assertFalse(LevelLoadingRules.shouldHide(), "功能关闭时保留选项不影响结果");
		} finally {
			TweakifyModTestSupport.installConfig(previous);
		}
	}

	private static TweakifyModConfig config(boolean enabled, boolean keepFirst) {
		TweakifyModConfig config = new TweakifyModConfig();
		config.modified.forceCloseLoadingScreen = enabled;
		config.modified.forceCloseLoadingScreenKeepFirst = keepFirst;
		return config;
	}
}
