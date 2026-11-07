package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigBase;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置树节点：每个节点对应一个配置项（开关/快捷键/列表），可附带子节点。
 * 有子节点的节点在配置界面左侧渲染 [+]/[-] 展开按钮。
 */
public final class ConfigGroupNode {
	private final IConfigBase config;
	private final List<ConfigGroupNode> children = new ArrayList<>();
	private boolean expanded;

	public ConfigGroupNode(IConfigBase config) {
		this.config = config;
	}

	// 追加一个子节点并返回它，便于链式构建。
	public ConfigGroupNode child(IConfigBase childConfig) {
		return this.child(new ConfigGroupNode(childConfig));
	}

	public ConfigGroupNode child(ConfigGroupNode child) {
		this.children.add(child);
		return child;
	}

	public IConfigBase config() {
		return this.config;
	}

	public List<ConfigGroupNode> children() {
		return this.children;
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	public boolean isExpanded() {
		return this.expanded;
	}

	public void toggle() {
		this.expanded = !this.expanded;
	}
}
