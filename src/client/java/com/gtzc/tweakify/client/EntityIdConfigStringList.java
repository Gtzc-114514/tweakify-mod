package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 实体 ID 字符串列表配置：GUI 使用实体注册表做 Tab 补全与有效性校验。
 */
public class EntityIdConfigStringList extends IdConfigStringList {
	public EntityIdConfigStringList(String name, ImmutableList<String> defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public Registry<?> registry() {
		return BuiltInRegistries.ENTITY_TYPE;
	}

	@Override
	public boolean entityIds() {
		return true;
	}
}
