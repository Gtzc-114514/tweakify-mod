package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigSlider;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.gui.ConfigOptionListenerResetConfig;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonStringList;
import fi.dy.masa.malilib.gui.interfaces.ISliderCallback;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverComponent;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSlider;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;

/**
 * 双层标签配置条目：左侧第一行显示本地化名称，第二行显示小号灰色英文配置键，
 * 条目整体加高，按钮等控件垂直居中，超宽配置键以省略号截断避免挤压按钮区域。
 */
public class TweakifyModConfigOptionWidget extends WidgetConfigOption {
	// 双层标签条目的统一高度（名称行 + 配置键行），紧凑布局避免条目间空隙过大。
	public static final int ENTRY_HEIGHT = 24;

	// 配置键第二行：不透明灰、缩放 0.75。
	private static final int KEY_COLOR = 0xFF808080;
	private static final float KEY_SCALE = 0.75f;

	// 高度 20 的按钮在条目中的垂直居中下移量。
	private static final int CONTROL_SHIFT = (ENTRY_HEIGHT - 20) / 2;

	// 配置键文字与按钮列之间的像素留白（按缩放后宽度计算）。
	private static final int KEY_MARGIN = 6;

	// 名称行由父类固定在 y + 8，配置键行紧随其后。
	private static final int KEY_LABEL_Y_OFFSET = 16;

	// 树形缩进与展开按钮布局。
	public static final int INDENT_WIDTH = 10;
	public static final int EXPAND_BUTTON_WIDTH = 12;
	public static final int EXPAND_BUTTON_GAP = 4;
	public static final int LABEL_OFFSET = EXPAND_BUTTON_WIDTH + EXPAND_BUTTON_GAP;

	private final IConfigBase config;
	private final int elementWidth;
	private final int labelShift;

	public TweakifyModConfigOptionWidget(int x, int y, int width, int height, int configWidth,
									   int elementWidth, GuiConfigsBase.ConfigOptionWrapper wrapper, int index,
									   IKeybindConfigGui host, WidgetListConfigOptionsBase<?, ?> parent) {
		super(x, y, width, height, configWidth, elementWidth, wrapper, index, host, parent);
		this.config = wrapper.getConfig();
		this.elementWidth = elementWidth;
		this.labelShift = this.computeLabelShift();
		this.shiftNameLabel();
		this.addExpandButton();
		this.centerControls();
		this.extendHoverArea();
		this.addKeyLabel();
		this.replaceStringListButton();
	}

	// 线宽滑块使用自定义回调，把 MaLiLib 默认的四位小数显示改成一位小数。
	@Override
	protected void addConfigSliderEntry(int x, int y, int resetX, int configWidth, int configHeight,
									   IConfigSlider config) {
		if (config != TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH
			|| !(config instanceof IConfigDouble doubleConfig)) {
			super.addConfigSliderEntry(x, y, resetX, configWidth, configHeight, config);
			return;
		}
		ButtonGeneric resetButton = this.createResetButton(resetX, y, config);
		ISliderCallback callback = new BlockOutlineWidthSliderCallback(doubleConfig, resetButton);
		WidgetSlider slider = new WidgetSlider(x, y, configWidth, configHeight, callback);
		ConfigOptionListenerResetConfig listener = new ConfigOptionListenerResetConfig(
			config, null, resetButton, null);
		this.addWidget(slider);
		this.addButton(resetButton, listener);
	}

	// 缩进 + 展开按钮占位：所有行统一左移，保证子项名称与分组名称对齐。
	private int computeLabelShift() {
		TweakifyModConfigScreen.NodeInfo info = TweakifyModConfigScreen.getNodeInfo(this.config);
		int depth = info == null ? 0 : info.depth();
		return depth * INDENT_WIDTH + LABEL_OFFSET;
	}

	// 父类把名称标签加在 this.x，这里按缩进向右平移。
	private void shiftNameLabel() {
		if (this.labelShift <= 0) {
			return;
		}
		for (WidgetBase widget : this.subWidgets) {
			if (widget instanceof WidgetLabel) {
				widget.setX(widget.getX() + this.labelShift);
			}
		}
	}

	// 有子节点的分组在左侧渲染纯文字 [+]/[-] 展开切换（不使用原版按钮框）。
	private void addExpandButton() {
		TweakifyModConfigScreen.NodeInfo info = TweakifyModConfigScreen.getNodeInfo(this.config);
		if (info == null || !info.hasChildren()) {
			return;
		}
		this.addWidget(new ExpandToggleWidget(this.x + info.depth() * INDENT_WIDTH,
			this.y + CONTROL_SHIFT, info.expanded(),
			() -> {
				if (this.host instanceof TweakifyModConfigScreen screen) {
					screen.toggleGroup(this.config.getName());
				}
			}));
	}

	// 将按钮等子控件下移，使它们在加高的条目中垂直居中。
	private void centerControls() {
		for (WidgetBase widget : this.subWidgets) {
			if (widget instanceof ButtonBase) {
				widget.setY(widget.getY() + CONTROL_SHIFT);
			}
		}
	}

	// 悬停提示区域覆盖整行，底部空白处也能看到配置说明。
	private void extendHoverArea() {
		for (WidgetBase widget : this.subWidgets) {
			if (widget instanceof WidgetHoverInfo || widget instanceof WidgetHoverComponent) {
				widget.setHeight(ENTRY_HEIGHT);
			}
		}
	}

	// 把 MaLiLib 默认的字符串列表按钮替换为自定义按钮，使其打开带物品 ID Tab 补全的对话框。
	private void replaceStringListButton() {
		if (!(this.config instanceof IConfigStringList)) {
			return;
		}
		for (int i = 0; i < this.subWidgets.size(); i++) {
			WidgetBase widget = this.subWidgets.get(i);
			if (widget instanceof ConfigButtonStringList) {
				this.subWidgets.set(i, new TweakifyModStringListButton(widget.getX(), widget.getY(),
					widget.getWidth(), widget.getHeight(), (IConfigStringList) this.config,
					this.host, this.host.getDialogHandler()));
			}
		}
	}

	// 在本地化名称下方追加小号灰色英文配置键。
	private void addKeyLabel() {
		if (this.config == null) {
			return;
		}
		int maxUnscaledWidth = (int) ((this.elementWidth - KEY_MARGIN - this.labelShift) / KEY_SCALE);
		String key = truncateToWidth(this.config.getName(), maxUnscaledWidth);
		this.addWidget(new ConfigKeyLabel(this.x + this.labelShift, this.y + KEY_LABEL_Y_OFFSET, key, KEY_COLOR));
	}

	// 超宽时以省略号截断，避免文字压到按钮区域。
	private String truncateToWidth(String text, int maxWidth) {
		if (this.getStringWidth(text) <= maxWidth) {
			return text;
		}
		String shortened = text;
		while (!shortened.isEmpty() && this.getStringWidth(shortened + "…") > maxWidth) {
			shortened = shortened.substring(0, shortened.length() - 1);
		}
		return shortened + "…";
	}

	// 纯文字的展开/折叠切换控件：仅渲染 + 或 - 文本，鼠标悬停高亮，点击切换。
	private static class ExpandToggleWidget extends WidgetBase {
		private final String symbol;
		private final int color;
		private final Runnable onToggle;

		ExpandToggleWidget(int x, int y, boolean expanded, Runnable onToggle) {
			super(x, y, EXPAND_BUTTON_WIDTH, 20);
			this.symbol = expanded ? "-" : "+";
			this.color = expanded ? 0xFFFF5555 : 0xFF55FF55;
			this.onToggle = onToggle;
		}

		@Override
		public void render(GuiContext gui, int mouseX, int mouseY, boolean hovered) {
			gui.text(this.textRenderer, this.symbol, this.x, this.y + 5, hovered ? 0xFFFFFFFF : this.color);
		}

		@Override
		protected boolean onMouseClickedImpl(MouseButtonEvent event, boolean notHovering) {
			this.onToggle.run();
			return true;
		}
	}

	// 小号灰色配置键标签，按 KEY_SCALE 缩放绘制。
	private static class ConfigKeyLabel extends WidgetBase {
		private final String text;
		private final int color;

		ConfigKeyLabel(int x, int y, String text, int color) {
			super(x, y, 0, 9);
			this.text = text;
			this.color = color;
		}

		@Override
		public void render(GuiContext gui, int mouseX, int mouseY, boolean hovered) {
			Matrix3x2fStack pose = gui.pose();
			pose.pushMatrix();
			pose.scale(KEY_SCALE, KEY_SCALE);
			gui.text(this.textRenderer, this.text,
				Math.round(this.x / KEY_SCALE), Math.round(this.y / KEY_SCALE), this.color);
			pose.popMatrix();
		}
	}
}
