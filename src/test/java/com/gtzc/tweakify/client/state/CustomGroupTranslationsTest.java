package com.gtzc.tweakify.client.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 默认物品组的组名翻译键：键格式与参考模组（inventory-item-groups）一致，
 * 已有资源包可直接复用；四种语言都必须存在。
 *
 * <p>与 {@link CreativeItemGroups} 同包，直接读取包私有的默认组定义。
 */
class CustomGroupTranslationsTest {

	@Test
	void defaultGroupNameTranslationsExistInEveryLanguage() throws IOException {
		Set<String> names = new LinkedHashSet<>();
		for (String tabId : List.of(
			"minecraft:building_blocks", "minecraft:colored_blocks", "minecraft:natural_blocks",
			"minecraft:functional_blocks", "minecraft:redstone_blocks",
			"minecraft:tools_and_utilities", "minecraft:combat",
			"minecraft:food_and_drinks", "minecraft:ingredients")) {
			for (CreativeItemGroups.GroupDefinition definition : CreativeItemGroups.definitionsFor(tabId)) {
				names.add(definition.name());
			}
		}
		assertFalse(names.isEmpty(), "default groups must be defined");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String name : names) {
				String key = "group_name.inventory_item_groups." + name;
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	private static Set<String> keysOf(String code) throws IOException {
		return valuesOf(code).keySet();
	}

	private static Map<String, String> valuesOf(String code) throws IOException {
		String path = "/assets/tweakify-mod/lang/" + code + ".json";
		try (InputStream stream = CustomGroupTranslationsTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, "missing resource " + path);
			JsonObject json = JsonParser.parseReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, String> values = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				JsonElement value = entry.getValue();
				values.put(entry.getKey(), value.isJsonPrimitive() ? value.getAsString() : "");
			}
			return values;
		}
	}
}
