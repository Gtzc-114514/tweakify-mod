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
		String code = stripComments(Files.readString(MIXIN_SOURCE));

		assertTrue(code.matches("(?ms).*^[ \\t]*@Inject\\s*\\([\\s\\S]*?method\\s*=\\s*\"checkTotemDeathProtection\"[\\s\\S]*?\\).*"),
			"checkTotemDeathProtection 注入注解被注释、移除或改名");
		assertTrue(code.matches("(?s).*@Inject\\s*\\([\\s\\S]*?at\\s*=\\s*@At\\(\"HEAD\"\\)[\\s\\S]*?\\).*"),
			"@Inject 注入点必须是 HEAD");
		assertTrue(code.matches("(?s).*@Inject\\s*\\([\\s\\S]*?cancellable\\s*=\\s*true[\\s\\S]*?\\).*"),
			"@Inject 必须保持 cancellable，否则 setReturnValue 无法跳过原方法");
		assertTrue(code.matches("(?s).*!config\\.modified\\.totemFromInventory.*"),
			"背包图腾开关必须取反：关闭时不能触发");
		assertTrue(code.matches("(?s).*[^!]source\\.is\\(DamageTypeTags\\.BYPASSES_INVULNERABILITY\\)\\s*\\)\\s*\\{\\s*return;.*"),
			"绕过无敌的伤害判断必须保持正极性并早退");
		assertTrue(code.matches("(?s).*if\\s*\\(\\s*!\\s*\\(\\s*self\\s+instanceof\\s+Player\\s+\\w+\\s*\\)\\s*\\)\\s*\\{\\s*return;\\s*}.*"),
			"非 Player 必须先早退");
		assertTrue(code.matches("(?s).*InteractionHand\\.values\\(\\).*?getItemInHand\\(\\w+\\)[^}]*DEATH_PROTECTION[^}]*!=\\s*null\\s*\\)\\s*\\{\\s*return;.*"),
			"手部已有图腾时必须保持正极性并交给原版处理（手部优先）");
		assertTrue(code.matches("(?s).*if\\s*\\(\\s*TotemInventory\\.tryUseInventoryTotem\\(\\w+\\)\\s*\\)\\s*\\{\\s*cir\\.setReturnValue\\(true\\);\\s*}.*"),
			"setReturnValue(true) 必须位于命中分支内，不能无条件免死");
	}

	// 剥离整行 // 与块注释，避免“注释里写着关键字”让接线断言假通过。
	private static String stripComments(String source) {
		return source.replaceAll("(?m)^[ \\t]*//.*$", "")
			.replaceAll("(?s)/\\*.*?\\*/", "");
	}
}
