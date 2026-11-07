package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 MaLiLib 配置项与 TweakifyModConfig 字段的对应关系：
 * 名称必须能解析到字段，默认值必须与字段初始化值一致。
 * 两处默认值一旦漂移，“已修改”统计与重置行为就会出错。
 */
class TweakifyModConfigOptionsTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void toggleDefaultsMatchConfigDefaults() throws IllegalAccessException {
		TweakifyModConfig defaults = new TweakifyModConfig();
		for (IConfigBoolean toggle : TweakifyModConfigOptions.TOGGLE_OPTIONS) {
			Field field = fieldOf(toggle.getName());
			assertEquals(field.getBoolean(defaults), toggle.getDefaultBooleanValue(),
				"default value mismatch for " + toggle.getName());
		}
	}

	@Test
	void optionListAndIntegerDefaultsMatchConfigDefaults() throws IllegalAccessException {
		TweakifyModConfig defaults = new TweakifyModConfig();
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			Field field = null;
			if (option instanceof IConfigOptionList optionList) {
				field = fieldOf(option.getName());
				assertEquals((String) field.get(defaults),
					optionList.getDefaultOptionListValue().getStringValue(),
					"default option list value mismatch for " + option.getName());
			} else if (option instanceof IConfigInteger integer) {
				field = fieldOf(option.getName());
				assertEquals(field.getInt(defaults), integer.getDefaultIntegerValue(),
					"default integer value mismatch for " + option.getName());
			} else if (option instanceof IConfigDouble doubleOption) {
				field = fieldOf(option.getName());
				assertEquals(field.getDouble(defaults), doubleOption.getDefaultDoubleValue(),
					"default double value mismatch for " + option.getName());
			}
		}
	}

	@Test
	void everyOptionMapsToConfigField() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			// 纯快捷键项（如 openConfigGui）没有同名的值字段。
			if (option instanceof IConfigBoolean || option instanceof IConfigStringList
				|| option instanceof IConfigOptionList || option instanceof IConfigInteger
				|| option instanceof IConfigDouble) {
				fieldOf(option.getName());
			}
			if (option instanceof IHotkey) {
				assertTrue(TweakifyModConfig.hasField(hotkeyFieldName(option.getName())),
					"missing hotkey field for " + option.getName());
			}
		}
	}

	// 配置界面的统计与快捷键注册都依赖这两份清单，新增开关漏登记时直接失败。
	@Test
	void everyBooleanOptionIsInToggleList() {
		Set<IConfigBoolean> toggles = new HashSet<>(TweakifyModConfigOptions.TOGGLE_OPTIONS);
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (option instanceof IConfigBoolean toggle) {
				assertTrue(toggles.contains(toggle), "missing toggle option: " + option.getName());
			}
		}
	}

	@Test
	void blockOutlineWidthUsesSteppedSliderConfig() {
		assertTrue(TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH instanceof BlockOutlineWidthConfig);
		assertTrue(TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH.shouldUseSlider());
	}

	@Test
	void blockOutlineWidthOptionRangeMatchesConstants() {
		IConfigDouble width = TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH;
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, width.getName());
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, width.getDefaultDoubleValue(), 1.0e-9);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, width.getMinDoubleValue(), 1.0e-9);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, width.getMaxDoubleValue(), 1.0e-9);
	}

	@Test
	void everyHotkeyOptionIsRegistered() {
		Set<IHotkey> hotkeys = new HashSet<>(TweakifyModConfigOptions.HOTKEY_OPTIONS);
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (option instanceof IHotkey hotkey) {
				assertTrue(hotkeys.contains(hotkey), "missing hotkey registration: " + option.getName());
			}
		}
	}

	private static Field fieldOf(String name) {
		try {
			Field field = TweakifyModConfig.class.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException e) {
			throw new AssertionError("Missing TweakifyModConfig field: " + name, e);
		}
	}

	private static String hotkeyFieldName(String optionName) {
		return "hotkey" + Character.toUpperCase(optionName.charAt(0)) + optionName.substring(1);
	}
}
