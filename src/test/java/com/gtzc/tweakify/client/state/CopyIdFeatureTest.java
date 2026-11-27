package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.client.config.CopyIdFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 复制 ID 的纯格式化：命名空间开关与三种格式的组合。
 * 不构造客户端世界，只覆盖字符串拼装逻辑。
 */
class CopyIdFeatureTest {
	@Test
	void keepsNamespaceByDefault() {
		assertEquals("minecraft:stone", CopyIdFeature.formatTarget(
			"minecraft:stone", "block", 1, CopyIdFormat.PLAIN, true));
	}

	@Test
	void stripsNamespaceWhenDisabled() {
		assertEquals("stone", CopyIdFeature.formatTarget(
			"minecraft:stone", "block", 1, CopyIdFormat.PLAIN, false));
		assertEquals("item:diamond", CopyIdFeature.formatTarget(
			"minecraft:diamond", "item", 1, CopyIdFormat.TYPED, false));
		assertEquals("block:minecraft:stone", CopyIdFeature.formatTarget(
			"minecraft:stone", "block", 1, CopyIdFormat.TYPED, true));
	}

	@Test
	void countedFormatAppliesNamespaceSwitch() {
		assertEquals("64x diamond", CopyIdFeature.formatTarget(
			"minecraft:diamond", "item", 64, CopyIdFormat.COUNTED, false));
		assertEquals("minecraft:diamond", CopyIdFeature.formatTarget(
			"minecraft:diamond", "item", 1, CopyIdFormat.COUNTED, true));
	}

	@Test
	void stripNamespaceHandlesIdsWithoutPrefix() {
		assertEquals("stone", CopyIdFeature.stripNamespace("stone"));
		assertEquals("minecraft:", CopyIdFeature.stripNamespace("minecraft:"));
	}
}
