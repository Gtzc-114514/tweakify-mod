package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import net.minecraft.core.Registry;

/**
 * 带注册表语义的 ID 字符串列表配置：GUI 依据 registry() 做 Tab 补全与有效性校验，
 * 依据 entityIds() 选择提示文案。新增列表类型只需再实现一个子类，不必改 GUI 代码。
 */
public abstract class IdConfigStringList extends ConfigStringList {
	protected IdConfigStringList(String name, ImmutableList<String> defaultValue) {
		super(name, defaultValue);
	}

	// 该列表的 ID 所属注册表。
	public abstract Registry<?> registry();

	// 是否为实体 ID 列表（决定补全来源与提示文案）。
	public abstract boolean entityIds();
}
