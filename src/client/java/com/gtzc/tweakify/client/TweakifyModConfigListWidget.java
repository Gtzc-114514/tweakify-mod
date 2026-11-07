package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

import java.util.List;

/**
 * 配置列表控件：为配置条目使用双层标签布局（加高条目 + 第二行英文配置键），
 * 标签列宽度同时适配本地化名称与英文配置键，避免文字挤压按钮区域。
 */
public class TweakifyModConfigListWidget extends WidgetListConfigOptions {
	public TweakifyModConfigListWidget(int x, int y, int width, int height, int configWidth,
									  float scale, boolean useKeybindSearch, GuiConfigsBase parent) {
		super(x, y, width, height, configWidth, scale, useKeybindSearch, parent);
	}

	// 配置条目使用加高布局，为双层标签留出第二行空间。
	@Override
	protected int getBrowserEntryHeightFor(GuiConfigsBase.ConfigOptionWrapper entry) {
		return entry != null && entry.getType() == GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG
			? TweakifyModConfigOptionWidget.ENTRY_HEIGHT
			: super.getBrowserEntryHeightFor(entry);
	}

	// 标签列宽度取本地化名称与英文配置键中较长者，并计入树形缩进，且不超过按钮列起点。
	@Override
	public int getMaxNameLengthWrapped(List<GuiConfigsBase.ConfigOptionWrapper> list) {
		int maxLength = 0;
		for (GuiConfigsBase.ConfigOptionWrapper wrapper : list) {
			if (wrapper.getType() != GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG) {
				continue;
			}
			IConfigBase config = wrapper.getConfig();
			if (config == null) {
				continue;
			}
			TweakifyModConfigScreen.NodeInfo info = TweakifyModConfigScreen.getNodeInfo(config);
			int indent = (info == null ? 0 : info.depth()) * TweakifyModConfigOptionWidget.INDENT_WIDTH
				+ TweakifyModConfigOptionWidget.LABEL_OFFSET;
			maxLength = Math.max(maxLength, this.getStringWidth(config.getConfigGuiDisplayName()) + indent);
			maxLength = Math.max(maxLength, this.getStringWidth(config.getName()) + indent);
		}
		return Math.min(maxLength, this.configWidth);
	}

	@Override
	protected WidgetConfigOption createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
													   GuiConfigsBase.ConfigOptionWrapper entry) {
		if (entry.getType() != GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG) {
			return super.createListEntryWidget(x, y, listIndex, isOdd, entry);
		}
		return new TweakifyModConfigOptionWidget(x, y, this.browserEntryWidth,
			TweakifyModConfigOptionWidget.ENTRY_HEIGHT, this.maxLabelWidth, this.configWidth,
			entry, listIndex, this.parent, this);
	}
}
