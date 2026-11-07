package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * 自定义字符串列表编辑对话框：加宽面板以便显示长物品 ID、在标题右侧显示灰色
 * Tab 补全提示；在聚焦文本框旁绘制命令补全风格的候选菜单（Tab/上下箭头循环、
 * 鼠标点击选择），并在列表下方显示当前输入物品 ID 的有效性（绿色有效/红色无效）。
 * 新增/删除/移动条目使用 MaLiLib 自带的每行右侧图标按钮（绿色 + 号插入空行）；
 * 修改在应用时通过配置值变更回调即时持久化，Esc 或 Enter 直接关闭对话框。
 */
public class TweakifyModStringListEditGui extends GuiStringListEdit {
	private static final String TAB_HINT_KEY = "tweakify-mod.gui.hint.string_list_tab_complete";
	private static final String ID_VALID_KEY = "tweakify-mod.gui.hint.string_list_id_valid";
	private static final String ID_INVALID_KEY = "tweakify-mod.gui.hint.string_list_id_invalid";

	// 实体列表使用对应的实体 ID 提示文案。
	private static final String ENTITY_TAB_HINT_KEY = "tweakify-mod.gui.hint.string_list_tab_complete_entity";
	private static final String ENTITY_ID_VALID_KEY = "tweakify-mod.gui.hint.string_list_id_valid_entity";
	private static final String ENTITY_ID_INVALID_KEY = "tweakify-mod.gui.hint.string_list_id_invalid_entity";

	// SDL scancode：Enter。
	private static final int KEY_ENTER = 40;
	private static final float HINT_SCALE = 0.75f;
	private static final int VALID_COLOR = 0xFF55FF55;
	private static final int INVALID_COLOR = 0xFFFF5555;

	// 候选菜单样式。
	private static final int MENU_ROW_HEIGHT = 10;
	private static final int MENU_PADDING = 2;
	private static final int MENU_BACKGROUND = 0xF0100010;
	private static final int MENU_HIGHLIGHT = 0xFF3377CC;
	private static final int MENU_TEXT_COLOR = 0xFFE0E0E0;
	private static final int MENU_TEXT_HIGHLIGHT = 0xFFFFFFFF;

	// 菜单宽度缓存：候选可能有上千条，不能每帧重新测量整个列表。
	// 候选列表每次打开菜单都会新建，用实例相等即可判断“需要重新测量”。
	private List<String> measuredCandidates = List.of();
	private int measuredMenuWidth;

	// 候选菜单在屏幕上的布局，供绘制与鼠标命中检测共用。
	private record CompletionMenuLayout(int x, int y, int width, int visibleRows) {
		int height() {
			return this.visibleRows * MENU_ROW_HEIGHT;
		}

		boolean contains(int mouseX, int mouseY) {
			return mouseX >= this.x - MENU_PADDING && mouseX <= this.x + this.width + MENU_PADDING
				&& mouseY >= this.y - MENU_PADDING && mouseY <= this.y + this.height() + MENU_PADDING;
		}

		int rowIndexAt(int mouseY) {
			return Math.max(0, Math.min((mouseY - this.y) / MENU_ROW_HEIGHT, this.visibleRows - 1));
		}
	}

	public TweakifyModStringListEditGui(IConfigStringList config, IConfigGui configGui,
									   IDialogHandler dialogHandler, Screen parent) {
		super(config, configGui, dialogHandler, parent);
	}

	// 加宽并加高对话框（原 400 -> 460、原高 -90 -> -60），为长物品 ID
	// 与底部提示区留出空间。
	@Override
	protected void setWidthAndHeight() {
		this.dialogWidth = 460;
		this.dialogHeight = GuiUtils.getScaledWindowHeight() - 60;
	}

	// 列表高度缩短 20，在列表下方留出有效性状态提示行。
	@Override
	protected WidgetListStringListEdit createListWidget(int x, int y) {
		return new TweakifyModStringListWidget(this.dialogLeft + 10, this.dialogTop + 20,
			this.getBrowserWidth(), this.getBrowserHeight() - 20, this.dialogWidth - 100, this);
	}

	// 候选菜单关闭时，Enter 直接关闭对话框（修改在应用时已由配置回调即时保存）。
	// 菜单打开时保持原行为（选中当前候选并关闭菜单）。
	@Override
	public boolean onKeyTyped(KeyEvent event) {
		if (event.key() == KEY_ENTER
			&& !(this.getListWidget() instanceof TweakifyModStringListWidget listWidget
				&& listWidget.isCompletionMenuOpen())) {
			if (this.dialogHandler != null) {
				this.dialogHandler.closeDialog();
			} else {
				this.closeGui(true);
			}
			return true;
		}
		return super.onKeyTyped(event);
	}

	// 在标题右侧追加 Tab 补全的灰色提示。
	@Override
	protected void drawTitle(GuiContext gui, int mouseX, int mouseY, float partialTicks) {
		super.drawTitle(gui, mouseX, mouseY, partialTicks);
		String hint = StringUtils.translate(isEntityList() ? ENTITY_TAB_HINT_KEY : TAB_HINT_KEY);
		int hintX = this.dialogLeft + this.dialogWidth - this.getStringWidth(hint) - 20;
		this.drawStringWithShadow(gui, hint, hintX, this.dialogTop + 6, 0xFFA0A0A0);
	}

	// 绘制候选菜单与列表下方的输入有效性状态。
	@Override
	public void drawContents(GuiContext gui, int mouseX, int mouseY, float partialTicks) {
		super.drawContents(gui, mouseX, mouseY, partialTicks);
		if (this.getListWidget() instanceof TweakifyModStringListWidget listWidget) {
			this.drawCompletionMenu(gui, listWidget);
			this.drawValidityStatus(gui, listWidget);
		}
	}

	// 在聚焦文本框旁绘制候选菜单（下方空间不足时显示在上方）。
	private void drawCompletionMenu(GuiContext gui, TweakifyModStringListWidget listWidget) {
		if (!listWidget.isCompletionMenuOpen()) {
			return;
		}
		CompletionMenuLayout layout = this.computeCompletionMenuLayout(listWidget);
		if (layout == null) {
			return;
		}
		List<String> candidates = listWidget.getActiveCompletions();
		int selectedIndex = listWidget.getCompletionIndex();
		int scrollOffset = listWidget.getCompletionScrollOffset();
		int left = layout.x() - MENU_PADDING;
		int top = layout.y() - MENU_PADDING;
		gui.fill(left, top, layout.x() + layout.width() + MENU_PADDING,
			layout.y() + layout.height() + MENU_PADDING, MENU_BACKGROUND);
		for (int row = 0; row < layout.visibleRows(); row++) {
			int candidateIndex = scrollOffset + row;
			boolean selected = candidateIndex == selectedIndex;
			int rowTop = layout.y() + row * MENU_ROW_HEIGHT;
			if (selected) {
				gui.fill(left, rowTop - 1, layout.x() + layout.width() + MENU_PADDING,
					rowTop + MENU_ROW_HEIGHT - 1, MENU_HIGHLIGHT);
			}
			gui.text(this.font, candidates.get(candidateIndex), layout.x() + 2, rowTop + 1,
				selected ? MENU_TEXT_HIGHLIGHT : MENU_TEXT_COLOR);
		}
	}

	// 计算候选菜单布局：贴聚焦文本框下方，空间不足则移到上方，宽度取最长候选。
	private CompletionMenuLayout computeCompletionMenuLayout(TweakifyModStringListWidget listWidget) {
		TweakifyModStringListWidget.TextFieldBounds field = listWidget.getFocusedTextFieldBounds();
		List<String> candidates = listWidget.getActiveCompletions();
		if (field == null || candidates.isEmpty()) {
			return null;
		}
		int visibleRows = Math.min(candidates.size(), TweakifyModStringListWidget.MAX_VISIBLE_COMPLETIONS);
		int menuWidth = Math.min(Math.max(this.getLongestCandidateWidth(candidates) + 8, 80), this.dialogWidth - 16);
		int x = Math.min(Math.max(field.x(), this.dialogLeft + 4),
			this.dialogLeft + this.dialogWidth - 4 - menuWidth);
		int y = field.y() + field.height() + 2;
		int bottomLimit = this.dialogTop + this.dialogHeight - 4;
		if (y + visibleRows * MENU_ROW_HEIGHT + MENU_PADDING > bottomLimit) {
			y = field.y() - visibleRows * MENU_ROW_HEIGHT - 4;
		}
		return new CompletionMenuLayout(x, y, menuWidth, visibleRows);
	}

	private int getLongestCandidateWidth(List<String> candidates) {
		if (candidates != this.measuredCandidates) {
			this.measuredCandidates = candidates;
			int maxWidth = 0;
			for (String candidate : candidates) {
				maxWidth = Math.max(maxWidth, this.getStringWidth(candidate));
			}
			this.measuredMenuWidth = maxWidth;
		}
		return this.measuredMenuWidth;
	}

	// 鼠标点击候选菜单：命中候选行则选中并关闭菜单，点击其它位置仅关闭菜单。
	@Override
	public boolean onMouseClicked(MouseButtonEvent event, boolean isRealMouse) {
		if (this.getListWidget() instanceof TweakifyModStringListWidget listWidget
			&& listWidget.isCompletionMenuOpen()) {
			CompletionMenuLayout layout = this.computeCompletionMenuLayout(listWidget);
			if (layout != null) {
				int mouseX = (int) event.x();
				int mouseY = (int) event.y();
				if (layout.contains(mouseX, mouseY)) {
					listWidget.applyCompletionAt(layout.rowIndexAt(mouseY));
					return true;
				}
				listWidget.closeCompletionMenu();
			}
		}
		return super.onMouseClicked(event, isRealMouse);
	}

	// 菜单打开时滚动列表会移动文本框位置，先关闭菜单再处理滚动。
	@Override
	public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.getListWidget() instanceof TweakifyModStringListWidget listWidget
			&& listWidget.isCompletionMenuOpen()) {
			listWidget.closeCompletionMenu();
		}
		return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// 在列表下方绘制输入有效性状态（绿/红）。
	private void drawValidityStatus(GuiContext gui, TweakifyModStringListWidget listWidget) {
		TweakifyModStringListWidget.Validity validity = listWidget.getFocusedValidity();
		String status = switch (validity) {
			case VALID -> "✓ " + StringUtils.translate(isEntityList() ? ENTITY_ID_VALID_KEY : ID_VALID_KEY);
			case INVALID -> "✗ " + StringUtils.translate(isEntityList() ? ENTITY_ID_INVALID_KEY : ID_INVALID_KEY);
			case NONE -> null;
		};
		if (status != null) {
			int color = validity == TweakifyModStringListWidget.Validity.VALID ? VALID_COLOR : INVALID_COLOR;
			this.drawScaledText(gui, status, this.dialogLeft + 10,
				this.dialogTop + this.dialogHeight - 12, color);
		}
	}

	// 列表类型决定补全提示与有效性文案（物品 / 实体）。
	private boolean isEntityList() {
		return this.config instanceof IdConfigStringList list && list.entityIds();
	}

	// 以 HINT_SCALE 缩放绘制提示小字，超宽时以省略号截断。
	private void drawScaledText(GuiContext gui, String text, int x, int y, int color) {
		int maxUnscaledWidth = (int) ((this.dialogWidth - 20) / HINT_SCALE);
		if (this.getStringWidth(text) > maxUnscaledWidth) {
			String shortened = text;
			while (!shortened.isEmpty() && this.getStringWidth(shortened + "…") > maxUnscaledWidth) {
				shortened = shortened.substring(0, shortened.length() - 1);
			}
			text = shortened + "…";
		}
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.scale(HINT_SCALE, HINT_SCALE);
		gui.text(this.font, text, Math.round(x / HINT_SCALE), Math.round(y / HINT_SCALE), color);
		pose.popMatrix();
	}
}
