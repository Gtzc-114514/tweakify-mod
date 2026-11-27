package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NBT 自动补全的上下文解析：组件方括号、NBT 花括号、值位置与选择器排除。
 */
class NbtAutocompleteTest {
	private static final List<String> COMPONENTS = List.of(
		"minecraft:custom_name", "minecraft:lore", "minecraft:enchantments", "minecraft:damage");
	private static final List<String> NBT_KEYS = List.of(
		"CustomName", "CustomNameVisible", "NoAI", "Silent", "ShowArms", "Pose");

	@Test
	void nonCommandTextIsIgnored() {
		assertFalse(NbtAutocomplete.analyze("hello world", 5).isActive());
	}

	@Test
	void componentBracketAfterItemIdIsDetected() {
		String text = "/give @s minecraft:stone[minecraft:cus";
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, text.length());

		assertTrue(analysis.isActive());
		assertEquals(NbtAutocomplete.Context.COMPONENTS, analysis.context());
		assertEquals("minecraft:cus", analysis.prefix());
		assertEquals(List.of("minecraft:custom_name"),
			NbtAutocomplete.filter(COMPONENTS, analysis));
	}

	@Test
	void entitySelectorBracketIsIgnored() {
		String text = "/kill @e[type=";
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, text.length() - 1);

		assertFalse(analysis.isActive());
	}

	@Test
	void compoundBracketSuggestsNbtKeys() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {Sho";
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, text.length());

		assertTrue(analysis.isActive());
		assertEquals(NbtAutocomplete.Context.NBT, analysis.context());
		assertEquals(List.of("ShowArms"), NbtAutocomplete.filter(NBT_KEYS, analysis));
	}

	@Test
	void valuePositionIsIgnored() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {NoAI:";
		assertFalse(NbtAutocomplete.analyze(text, text.length()).isActive());
	}

	@Test
	void quotedValueIsIgnored() {
		String text = "/summon minecraft:pig ~ ~ ~ {CustomName:'Te";
		assertFalse(NbtAutocomplete.analyze(text, text.length()).isActive());
	}

	@Test
	void alreadyUsedKeysAreExcluded() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {NoAI:1b, Silent:1b, }";
		int cursor = text.length() - 1;
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, cursor);

		assertTrue(analysis.isActive());
		assertTrue(analysis.usedKeys().contains("NoAI"));
		assertTrue(analysis.usedKeys().contains("Silent"));
		List<String> suggestions = NbtAutocomplete.filter(NBT_KEYS, analysis);
		assertFalse(suggestions.contains("NoAI"));
		assertFalse(suggestions.contains("Silent"));
		assertTrue(suggestions.contains("CustomName"));
	}

	@Test
	void nestedCompoundKeepsInnerContext() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {ArmorItems:[{id:\"minecraft:stone\",count:1,}";
		int cursor = text.length() - 1;
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, cursor);

		assertTrue(analysis.isActive());
		assertEquals(NbtAutocomplete.Context.NBT, analysis.context());
		assertFalse(NbtAutocomplete.filter(NBT_KEYS, analysis).contains("id"),
			"已写过的 id 不再提示");
	}

	@Test
	void closingBracketEndsCompletion() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {NoAI:1b}";
		assertFalse(NbtAutocomplete.analyze(text, text.length()).isActive());
	}

	@Test
	void prefixMatchIsCaseSensitive() {
		String text = "/summon minecraft:armor_stand ~ ~ ~ {cus";
		NbtAutocomplete.Analysis analysis = NbtAutocomplete.analyze(text, text.length());

		assertTrue(NbtAutocomplete.filter(NBT_KEYS, analysis).isEmpty(),
			"SNBT 键名区分大小写，小写前缀不应匹配 CustomName");
	}
}
