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
	// 标签列最小宽度，以及右侧按钮/重置/滚动条合计需要的宽度。
	private static final int MIN_LABEL_WIDTH = 80;
	private static final int MIN_CONTROLS_TOTAL = 230;
	private static final int LABEL_CONTROL_GAP = 10;
	private static final int RIGHT_RESERVE = 40;
	private static final int MIN_CONFIG_WIDTH = 120;

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
		// 4x/自动缩放时屏幕很窄：给右侧按钮、重置键和滚动条留出空间，
		// 避免右侧控件压到滚动条上。
		int maxAllowed = Math.max(MIN_LABEL_WIDTH, this.browserEntryWidth - MIN_CONTROLS_TOTAL);
		return Math.min(maxLength, Math.min(this.configWidth, maxAllowed));
	}

	@Override
	protected WidgetConfigOption createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
													   GuiConfigsBase.ConfigOptionWrapper entry) {
		if (entry.getType() != GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG) {
			return super.createListEntryWidget(x, y, listIndex, isOdd, entry);
		}
		int availableControls = this.browserEntryWidth - this.maxLabelWidth
			- LABEL_CONTROL_GAP - RIGHT_RESERVE;
		int effectiveConfigWidth = Math.max(MIN_CONFIG_WIDTH,
			Math.min(this.configWidth, availableControls));
		return new TweakifyModConfigOptionWidget(x, y, this.browserEntryWidth,
			TweakifyModConfigOptionWidget.ENTRY_HEIGHT, this.maxLabelWidth, effectiveConfigWidth,
			entry, listIndex, this.parent, this);
	}
}
