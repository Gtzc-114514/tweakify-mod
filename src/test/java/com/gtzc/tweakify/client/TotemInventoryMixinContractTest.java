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
		assertTrue(source.matches("(?s).*@Inject\\s*\\([^)]*method\\s*=\\s*\"checkTotemDeathProtection\"[^)]*\\).*"),
			"checkTotemDeathProtection 注入被移除或改名");
		assertTrue(source.matches("(?s).*@Inject\\s*\\(.*?cancellable\\s*=\\s*true.*?\\).*"),
			"@Inject 必须保持 cancellable，否则 setReturnValue 无法跳过原方法");
		assertTrue(source.matches("(?s).*!config\\.modified\\.totemFromInventory.*"),
			"背包图腾开关必须取反：关闭时不能触发");
		assertTrue(source.matches("(?s).*BYPASSES_INVULNERABILITY\\)\\)\\s*\\{\\s*return;.*"),
			"绕过无敌的伤害必须先早退");
		assertTrue(source.matches("(?s).*if\\s*\\(\\s*!\\s*\\(\\s*self\\s+instanceof\\s+Player\\s+player\\s*\\)\\s*\\)\\s*\\{\\s*return;\\s*}.*"),
			"非 Player 必须先早退");
		assertTrue(source.matches("(?s).*InteractionHand\\.values\\(\\).*?getItemInHand\\(hand\\)[^}]*DEATH_PROTECTION[^}]*return;.*"),
			"手部已有图腾时必须交给原版处理（手部优先）");
		assertTrue(source.matches("(?s).*if\\s*\\(\\s*TotemInventory\\.tryUseInventoryTotem\\(player\\)\\s*\\)\\s*\\{\\s*cir\\.setReturnValue\\(true\\);\\s*}.*"),
			"setReturnValue(true) 必须位于命中分支内，不能无条件免死");
		assertTrue(source.contains("TotemInventory.tryUseInventoryTotem(player)"),
			"背包图腾触发调用被移除");
	}
}
