package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

/**
 * 把模组的快捷键配置项注册进 MaLiLib 快捷键系统：
 * addKeysToMap 在按键映射重建时被调用，addHotkeys 在注册时立即填充分类列表。
 */
public class TweakifyModKeybindProvider implements IKeybindProvider {
	private static final String HOTKEY_CATEGORY_KEY = TweakifyMod.MOD_ID + ".hotkeys.category.general";

	@Override
	public void addKeysToMap(IKeybindManager manager) {
		TweakifyModConfigOptions.HOTKEY_OPTIONS.forEach(hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
	}

	@Override
	public void addHotkeys(IKeybindManager manager) {
		manager.addHotkeysForCategory(TweakifyMod.MOD_ID, HOTKEY_CATEGORY_KEY,
			TweakifyModConfigOptions.HOTKEY_OPTIONS);
	}
}
