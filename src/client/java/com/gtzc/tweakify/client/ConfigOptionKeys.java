package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

/**
 * 配置项的公共键与快捷键设置。
 *
 * <p>单独成类而不是放在 {@link TweakifyModConfigOptions}：各标签页分片类在骨架类
 * 初始化期间被加载，此时骨架类的静态常量可能尚未赋值，直接引用会读到 null。
 */
final class ConfigOptionKeys {
	static final String GENERAL_KEY = TweakifyMod.MOD_ID + ".config.general";
	static final String LISTS_KEY = TweakifyMod.MOD_ID + ".config.lists";
	static final String HIGHLIGHT_KEY = TweakifyMod.MOD_ID + ".config.highlight";

	// 开关项的切换快捷键：默认无快捷键（允许为空），允许多余按键便于边走边按。
	static final KeybindSettings TOGGLE_HOTKEY_SETTINGS = KeybindSettings.PRESS_ALLOWEXTRA_EMPTY;

	// 打开配置界面的快捷键：任意界面可用，允许清空。
	static final KeybindSettings OPEN_CONFIG_GUI_SETTINGS =
		KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true, true);

	// 复制物体 ID 的快捷键：需要在容器界面（背包/箱子）里也能触发，因此同样使用 ANY 上下文。
	static final KeybindSettings COPY_ID_HOTKEY_SETTINGS =
		KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true, true);

	private ConfigOptionKeys() {
	}
}
