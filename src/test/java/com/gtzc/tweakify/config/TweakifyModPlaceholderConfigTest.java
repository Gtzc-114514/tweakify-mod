package com.gtzc.tweakify.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 类初始化占位实例：不得填充依赖注册表的默认物品列表。
 *
 * <p>{@code TweakifyMod} 的静态初始化可能被语言 mixin 在注册表初始化期间回调；
 * 占位实例若扫描注册表或解析物品 ID，会与第三方 mixin（如 ETF）形成初始化循环，
 * 导致启动崩溃（见 v1.2.5.1 修复）。
 */
class TweakifyModPlaceholderConfigTest {

	@Test
	void placeholderInstanceSkipsRegistryBackedDefaultLists() {
		TweakifyModConfig config = TweakifyModConfig.createPlaceholderInstance();

		assertTrue(config.getRestrictedOffhandItems().isEmpty(),
			"占位实例不应扫描注册表填充受限副手列表");
		assertTrue(config.getMainHandUseCooldownItems().isEmpty(),
			"占位实例不应扫描注册表填充主手使用冷却列表");
		assertTrue(config.getOffhandUseCooldownItems().isEmpty(),
			"占位实例不应扫描注册表填充副手使用冷却列表");
	}
}
