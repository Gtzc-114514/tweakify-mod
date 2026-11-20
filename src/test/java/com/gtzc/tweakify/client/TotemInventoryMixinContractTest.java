package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不死图腾背包生效的 Mixin 源码接线契约。
 *
 * <p>普通单测环境不加载客户端 Mixin，这里锁住「注入方法名 + 配置开关 +
 * 背包查找调用 + 伤害来源过滤」四处接线；注入被移除或改名、条件写反时直接失败。
 * 槽位遍历逻辑由 {@link TotemInventoryTest} 覆盖。
 */
class TotemInventoryMixinContractTest {
	private static final Path MIXIN_SOURCE = Path.of(
		"src/client/java/com/gtzc/tweakify/client/mixin/TotemInventoryMixin.java");

	@Test
	void inventoryTotemFallbackStaysWired() throws IOException {
		String source = Files.readString(MIXIN_SOURCE);
		assertTrue(source.matches("(?s).*@Inject\\s*\\(\\s*method\\s*=\\s*\"checkTotemDeathProtection\".*"),
			"checkTotemDeathProtection 注入被移除或改名");
		assertTrue(source.contains("config.modified.totemFromInventory"),
			"背包图腾开关读取被移除");
		assertTrue(source.contains("TotemInventory.tryUseInventoryTotem(player)"),
			"背包图腾触发调用被移除");
		assertTrue(source.contains("BYPASSES_INVULNERABILITY"),
			"绕过无敌的伤害来源过滤被移除");
		assertTrue(source.contains("cir.setReturnValue(true)"),
			"触发后取消原方法的返回值被移除");
	}
}
