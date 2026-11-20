package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物品分组配置转发的接线契约。
 *
 * <p>项目测试基础设施不加载客户端 Mixin，无法直接执行 {@code containerTick} 注入；
 * 这里用源码契约锁住「注入方法名 + 转发接缝 + 刷新调用」三处接线：
 * 转发被删除、条件写反或刷新调用被移除时测试直接失败，替代原先的手动回归。
 *
 * <p>决策逻辑本身（null / 分类页 / 非分类页）由
 * {@link CreativeItemGroupsTest#shouldRefreshAfterConfigChangeMatchesOnConfigChanged()}
 * 覆盖；本测试只负责“Mixin 仍然调用这个接缝”。
 */
class CreativeModeInventoryGroupingMixinContractTest {
	private static final Path MIXIN_SOURCE = Path.of(
		"src/client/java/com/gtzc/tweakify/client/mixin/CreativeModeInventoryGroupingMixin.java");

	@Test
	void configChangeForwardingStaysWired() throws IOException {
		String source = Files.readString(MIXIN_SOURCE);
		assertTrue(source.matches("(?s).*@Inject\\s*\\(\\s*method\\s*=\\s*\"containerTick\".*"),
			"containerTick 注入被移除或改名");
		assertTrue(source.contains("CreativeItemGroups.shouldRefreshAfterConfigChange(selectedTab)"),
			"配置变化转发接缝被移除");
		assertTrue(source.contains("refreshCurrentTabContents(selectedTab.getDisplayItems())"),
			"配置变化后的列表刷新调用被移除");
	}
}
