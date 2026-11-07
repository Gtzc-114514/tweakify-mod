package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 物品 ID 字符串列表配置：GUI 使用物品注册表做 Tab 补全与有效性校验。
 */
public class ItemIdConfigStringList extends IdConfigStringList {
	public ItemIdConfigStringList(String name, ImmutableList<String> defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public Registry<?> registry() {
		return BuiltInRegistries.ITEM;
	}

	@Override
	public boolean entityIds() {
		return false;
	}
}
