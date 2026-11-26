package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聊天签名隐藏的配置门控：配置缺失时按关闭处理，开关变化立即生效。
 */
class ChatSigningHiderTest {

	@Test
	void enabledFollowsConfigAndSurvivesMissingConfig() {
		TweakifyModConfig previous = TweakifyMod.CONFIG;
		TweakifyMod.CONFIG = null;
		try {
			assertFalse(ChatSigningHider.isEnabled(), "配置未加载时应保持原版显示");
		} finally {
			TweakifyMod.CONFIG = previous;
		}

		TweakifyModConfig config = new TweakifyModConfig();
		TweakifyMod.CONFIG = config;
		try {
			assertFalse(ChatSigningHider.isEnabled(), "默认关闭，保持原版权行为");
			config.modified.hideChatSigning = true;
			assertTrue(ChatSigningHider.isEnabled(), "开启后渲染层应立即隐藏签名标记");
		} finally {
			TweakifyMod.CONFIG = previous;
		}
	}
}
