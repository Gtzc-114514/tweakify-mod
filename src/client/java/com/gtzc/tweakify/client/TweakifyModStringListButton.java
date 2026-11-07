package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ConfigButtonStringList;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;

/**
 * 字符串列表按钮：以短文本显示列表条目数量（如“12 项”），点击时打开带
 * 物品 ID Tab 补全的 {@link TweakifyModStringListEditGui} 对话框。
 */
public class TweakifyModStringListButton extends ConfigButtonStringList {
	// 按钮短文本的翻译键：显示列表条目数量。
	private static final String ENTRIES_KEY = "tweakify-mod.gui.button.string_list.entries";

	private final IConfigStringList config;
	private final IConfigGui configGui;
	private final IDialogHandler dialogHandler;

	// 缓存的条目数量，仅在列表变化时重新生成文本。
	private int cachedCount = -1;

	public TweakifyModStringListButton(int x, int y, int width, int height, IConfigStringList config,
									  IConfigGui configGui, IDialogHandler dialogHandler) {
		super(x, y, width, height, config, configGui, dialogHandler);
		this.config = config;
		this.configGui = configGui;
		this.dialogHandler = dialogHandler;
	}

	// 按钮只显示列表条目数量，不显示物品 ID 内容。
	@Override
	public void updateDisplayString() {
		if (this.config == null) {
			this.displayString = "";
			return;
		}
		int count = this.config.getStrings().size();
		if (count != this.cachedCount) {
			this.cachedCount = count;
			this.displayString = StringUtils.translate(ENTRIES_KEY, count);
		}
	}

	// 编辑对话框可能改变条目数量，渲染前刷新短文本。
	@Override
	public void render(GuiContext gui, int mouseX, int mouseY, boolean hovered) {
		this.updateDisplayString();
		super.render(gui, mouseX, mouseY, hovered);
	}

	// 复刻父类点击逻辑（含点击音效），但打开自定义对话框；不能调用 super，
	// 否则会打开 MaLiLib 默认的 GuiStringListEdit。
	@Override
	protected boolean onMouseClickedImpl(MouseButtonEvent event, boolean notHovering) {
		this.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		Screen parent = this.dialogHandler != null ? null : GuiUtils.getCurrentScreen();
		TweakifyModStringListEditGui gui = new TweakifyModStringListEditGui(this.config, this.configGui,
			this.dialogHandler, parent);
		if (this.dialogHandler != null) {
			this.dialogHandler.openDialog(gui);
		} else {
			GuiBase.openGui(gui);
		}
		return true;
	}
}
