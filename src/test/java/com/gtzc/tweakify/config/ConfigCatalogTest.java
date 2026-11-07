package com.gtzc.tweakify.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置目录的完整性：名称唯一、父项在前、命令项必须是布尔字段，
 * 且目录与配置字段双向覆盖（漏登记或漏声明都会让界面/命令静默丢项）。
 */
class ConfigCatalogTest {

	@Test
	void entriesAreUniqueAndParentsPrecedeChildren() {
		Set<String> seen = new HashSet<>();
		for (ConfigCatalog.Entry entry : ConfigCatalog.entries()) {
			assertTrue(seen.add(entry.name()), "duplicate entry: " + entry.name());
			if (entry.parent() != null) {
				assertTrue(seen.contains(entry.parent()), "parent must precede " + entry.name());
			}
		}
	}

	@Test
	void blockOutlineWidthUsesSharedFieldNameConstant() {
		ConfigCatalog.Entry entry = ConfigCatalog.entryOf(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		assertNotNull(entry, "catalog must contain the shared block outline width field name");
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, entry.name());
		assertEquals("customBlockOutline", entry.parent());
	}

	@Test
	void everyCommandEntryIsABooleanField() {
		for (ConfigCatalog.Entry entry : ConfigCatalog.commandEntries()) {
			assertTrue(TweakifyModConfig.isBooleanField(entry.name()),
				"command entry must be a boolean config field: " + entry.name());
		}
	}

	@Test
	void everyEntryMapsToAConfigFieldOrHotkeyField() {
		for (ConfigCatalog.Entry entry : ConfigCatalog.entries()) {
			String name = entry.name();
			assertTrue(TweakifyModConfig.hasField(name)
					|| TweakifyModConfig.hasField("hotkey" + capitalize(name)),
				"no config field for entry: " + name);
		}
	}

	// 反向覆盖：除版本号与快捷键字段外，每个配置字段都必须在目录里登记，否则界面会漏项。
	@Test
	void everyConfigFieldIsRegistered() {
		Set<String> names = new HashSet<>(ConfigCatalog.optionNames());
		for (String fieldName : TweakifyModConfig.configFieldNames()) {
			if ("configVersion".equals(fieldName) || fieldName.startsWith("hotkey")) {
				continue;
			}
			assertTrue(names.contains(fieldName), "config field missing from catalog: " + fieldName);
		}
	}

	private static String capitalize(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
