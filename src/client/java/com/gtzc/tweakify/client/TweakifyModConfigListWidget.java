package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.input.ScanCodes;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置列表控件：为配置条目使用双层标签布局（加高条目 + 第二行英文配置键），
 * 标签列宽度同时适配本地化名称与英文配置键，避免文字挤压按钮区域。
 *
 * <p>滚轮滚动默认是平滑动画（可配置关闭），并支持按住鼠标左键拖动列表：
 * 输入只累加到动画目标位置，每帧收敛后换算成“起始行 + 亚行像素偏移”重新摆放条目，
 * 因此列表能以像素为单位滚动，而不是按整行跳变。拖动只在按下位置没被条目控件（按钮/滑块/
 * 展开按钮/输入框）消费、且移动超过阈值时才开始，不会抢走控件自己的点击与拖动。
 *
 * <p>与分组展开/收起兼容：布局重建只临时改起始 Y 偏移，滚动条值仍是整行索引，
 * 界面层的分组折叠逻辑（按行号保存/恢复滚动位置）不受影响；条目绘制加了裁剪，
 * 半行滚动时不会画到标签栏与页脚上。
 */
public class TweakifyModConfigListWidget extends WidgetListConfigOptions {
	// 标签列最小宽度，以及右侧按钮/重置/收藏星/滚动条合计需要的宽度。
	private static final int MIN_LABEL_WIDTH = 80;
	private static final int MIN_CONTROLS_TOTAL = 246;
	private static final int LABEL_CONTROL_GAP = 10;
	// 行右端固定留给收藏星：16px 星标 + 2px 右边距，再加重置按钮与间隙。
	static final int RIGHT_RESERVE = 56;
	private static final int MIN_CONFIG_WIDTH = 120;

	// 拖动滚动只认鼠标左键；按下时先让条目控件处理，被消费的点击不会起拖。
	private static final int DRAG_SCROLL_BUTTON = ScanCodes.OFFSET_MOUSE_LEFT;

	// 起拖阈值：移动超过该像素才真正开始滚动，避开手抖带来的意外拖动。
	private static final double DRAG_START_THRESHOLD_PX = 3.0;

	// 滚轮一格滚动的行数：整段位移由平滑动画摊到多帧，因此可以比原生一次跳三行更小。
	private static final double SCROLL_ROWS_PER_NOTCH = 2.0;

	// 单帧最大时间步长：卡顿后一次性追上目标会显得突兀，超过 100 毫秒按 100 毫秒推进。
	private static final double MAX_FRAME_SECONDS = 0.1;

	private final SmoothScrollAnimator smoothScroll = new SmoothScrollAnimator();

	// 当前已应用到控件布局的滚动位置；与平滑动画的换算结果不一致时重建控件。
	private int appliedRow;
	private int appliedPixelOffset;
	private long lastFrameMillis = -1L;

	// 左键拖动会话：按下位置未被控件消费时“预备”起拖，移动超过阈值后进入拖动。
	private boolean dragArmed;
	private boolean dragScrolling;
	private double dragTravelY;

	// 条目控件在构造时缓存了高亮开关：开关变化后需要重建才能立即生效。
	private boolean appliedHighlightEnabled;

	public TweakifyModConfigListWidget(int x, int y, int width, int height, int configWidth,
									  float scale, boolean useKeybindSearch, GuiConfigsBase parent) {
		super(x, y, width, height, configWidth, scale, useKeybindSearch, parent);
		this.appliedHighlightEnabled = searchHighlightEnabled();
	}

	// 平滑滚动开关：配置尚未加载时按默认（开启）处理；支持按界面关闭。
	private static boolean smoothScrollEnabled() {
		return SmoothScrolling.configListEnabled();
	}

	// 搜索高亮开关：配置尚未加载时按默认（开启）处理。
	private static boolean searchHighlightEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null || config.general.searchHighlight;
	}

	// 当前搜索过滤串（小写、| 分隔）；高亮关闭或搜索框未展开时返回空串。
	String searchHighlightFilter() {
		if (!searchHighlightEnabled()) {
			return "";
		}
		String filter = this.getFilterText();
		return filter == null ? "" : filter;
	}

	// 本列表的条目等高（全部是加高的配置行）：getConfigs() 只产出 CONFIG 包装，
	// 因此像素与行号换算、refreshScrollRange 里从起始行累加可视数量都成立。
	private int entryHeight() {
		return this.listContents.isEmpty()
			? TweakifyModConfigOptionWidget.ENTRY_HEIGHT
			: this.getBrowserEntryHeightFor(this.listContents.get(0));
	}

	// 可用滚动范围（像素）：滚动条上限按整行计，换算成像素后交给动画夹紧。
	private double maxScrollPixels() {
		return Math.max(0, this.scrollBar.getMaxValue()) * (double) this.entryHeight();
	}

	// 列表整体范围：与 MaLiLib 处理滚轮的区域一致。
	private boolean isInsideList(double mouseX, double mouseY) {
		return mouseX >= this.posX && mouseX <= this.posX + this.browserWidth
			&& mouseY >= this.posY && mouseY <= this.posY + this.browserHeight;
	}

	// 条目列范围：排除搜索框与右侧滚动条，在这两处按下不应该起拖。
	private boolean isInsideEntries(double mouseX, double mouseY) {
		return mouseX >= this.entriesLeft() && mouseX < this.entriesLeft() + this.browserEntryWidth
			&& mouseY >= this.entriesTop() && mouseY < this.posY + this.browserHeight;
	}

	// 条目可视区顶部：搜索框占用 browserEntriesOffsetY 的高度，半行滚动会越到它上面。
	private int entriesTop() {
		return this.browserEntriesStartY + this.browserEntriesOffsetY;
	}

	// 条目可视区高度：从搜索框下沿到底部留白之前，与 MaLiLib 的可用高度一致。
	private int entriesHeight() {
		return this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY;
	}

	// 条目左边界：MaLiLib 的列表布局从 posX + 2 开始摆放条目。
	private int entriesLeft() {
		return this.posX + 2;
	}

	@Override
	public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!smoothScrollEnabled() || !this.isInsideList(mouseX, mouseY)) {
			return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
		// 滚轮一格 = 若干行；轨道板的分数滚动量同样按比例累加。
		this.smoothScroll.addDelta(-verticalAmount * SCROLL_ROWS_PER_NOTCH * this.entryHeight(),
			this.maxScrollPixels());
		return true;
	}

	@Override
	public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick) {
		// 先让条目控件处理：按钮、滑块、展开按钮与输入框消费掉的点击不能起拖，
		// 否则会与控件自身的点击/拖动互相抢事件。
		boolean handled = super.onMouseClicked(click, doubleClick);
		this.dragArmed = !handled
			&& smoothScrollEnabled()
			&& click.input() == DRAG_SCROLL_BUTTON
			&& this.isInsideEntries(click.x(), click.y());
		this.dragScrolling = false;
		this.dragTravelY = 0.0;
		return handled;
	}

	@Override
	public boolean onMouseReleased(MouseButtonEvent click) {
		if (click.input() == DRAG_SCROLL_BUTTON) {
			this.dragArmed = false;
			this.dragScrolling = false;
			this.dragTravelY = 0.0;
		}
		return super.onMouseReleased(click);
	}

	@Override
	public boolean onMouseDragged(MouseButtonEvent click, double dragXAmount, double dragYAmount) {
		if (click.input() != DRAG_SCROLL_BUTTON || (!this.dragArmed && !this.dragScrolling)) {
			// 拖动中换了按键（或本次按下本来就被控件消费）：结束拖动，避免卡在拖动状态。
			this.dragArmed = false;
			this.dragScrolling = false;
			this.dragTravelY = 0.0;
			return super.onMouseDragged(click, dragXAmount, dragYAmount);
		}

		if (this.dragScrolling) {
			// 内容跟随光标：向下拖动时显示更靠前的内容。
			this.smoothScroll.addDelta(-dragYAmount, this.maxScrollPixels());
			return true;
		}

		this.dragTravelY += dragYAmount;
		if (Math.abs(this.dragTravelY) < DRAG_START_THRESHOLD_PX) {
			return super.onMouseDragged(click, dragXAmount, dragYAmount);
		}
		// 跨过阈值后把已经积攒的位移一次性补上，起拖时不会先丢掉几像素。
		double pending = this.dragTravelY;
		this.dragTravelY = 0.0;
		this.dragScrolling = true;
		this.smoothScroll.addDelta(-pending, this.maxScrollPixels());
		return true;
	}

	// 切换标签、搜索过滤变化时 MaLiLib 会把滚动条归零：动画状态一起复位。
	@Override
	public void resetScrollbarPosition() {
		super.resetScrollbarPosition();
		this.smoothScroll.jumpTo(0.0, 0.0);
		this.appliedRow = 0;
		boolean offsetReset = this.appliedPixelOffset != 0;
		this.appliedPixelOffset = 0;
		if (offsetReset) {
			// 控件已按旧偏移摆好，不清零就会与新状态一致而永远不重建。
			this.reCreateListEntryWidgets();
		}
	}

	// 分组折叠/展开后恢复滚动位置：行号 + 亚行像素偏移，只恢复整行会丢掉半行位置。
	void restoreScrollPosition(int row, int pixelOffset) {
		int height = this.entryHeight();
		this.scrollBar.setValue(row);
		this.appliedPixelOffset = Math.max(0, Math.min(pixelOffset, height - 1));
		this.reCreateListEntryWidgets();
		// 列表变短时行号会被夹回合法范围：以实际应用到布局的值为准。
		this.appliedRow = this.scrollBar.getValue();
		this.smoothScroll.jumpTo(this.appliedRow * (double) height + this.appliedPixelOffset,
			this.maxScrollPixels());
	}

	// 当前应用到布局的亚行像素偏移：界面层恢复滚动位置时与行号一起使用。
	int scrollPixelOffset() {
		return this.appliedPixelOffset;
	}

	// 键盘翻页与滚轮的原生分支都是整行跳转：动画状态同步对齐，不保留半行偏移。
	@Override
	protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection) {
		super.offsetSelectionOrScrollbar(amount, changeSelection);
		this.appliedRow = this.scrollBar.getValue();
		this.smoothScroll.jumpTo(this.appliedRow * (double) this.entryHeight(), this.maxScrollPixels());
		if (this.appliedPixelOffset != 0) {
			this.appliedPixelOffset = 0;
			this.reCreateListEntryWidgets();
		}
	}

	// 布局时把起始 Y 临时上移亚行像素偏移，让条目停在半行位置；结束后恢复，
	// 滚动条与搜索框仍按正常偏移绘制。
	@Override
	protected void reCreateListEntryWidgets() {
		int row = this.scrollBar.getValue();
		int savedOffsetY = this.browserEntriesOffsetY;
		if (this.appliedPixelOffset > 0) {
			// 两套搜索框的 offsetY 不同（23 / 17）：不能减成负数，否则起始 Y 会越出列表上沿。
			this.browserEntriesOffsetY = Math.max(0, savedOffsetY - this.appliedPixelOffset);
		}
		try {
			super.reCreateListEntryWidgets();
		} finally {
			this.browserEntriesOffsetY = savedOffsetY;
		}
		// 亚行偏移下可视区会多容纳半行，父类按它算出的滚动上限会少一行：按正常视口重算。
		this.refreshScrollRange(row);
		// 位置已经在这里应用过，避免父类绘制路径再重建一次。
		this.lastScrollbarPosition = this.scrollBar.getValue();
	}

	// 按正常视口（不含亚行偏移）重算可见条目数与滚动上限，并保留给定行号。
	private void refreshScrollRange(int keepRow) {
		// 分组收起/搜索过滤可能让总行数变少：先夹到合法范围再算可见数量，
		// 否则会从越界的行号开始数，得到“可见 0 行”的错误上限。
		int total = this.listContents.size();
		int row = Math.max(0, Math.min(keepRow, Math.max(0, total - 1)));
		int usableHeight = this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY;
		int usedHeight = 0;
		int visible = 0;
		for (int i = row; i < total; i++) {
			int height = this.getBrowserEntryHeightFor(this.listContents.get(i));
			if (usedHeight + height > usableHeight) {
				break;
			}
			usedHeight += height;
			visible++;
		}
		this.maxVisibleBrowserEntries = visible;
		this.scrollBar.setMaxValue(total - visible);
		this.scrollBar.setValue(row);
	}

	// 搜索高亮开关切换后重建条目：标签控件在构造时缓存了高亮开关与过滤串。
	private void refreshHighlightState() {
		boolean enabled = searchHighlightEnabled();
		if (enabled != this.appliedHighlightEnabled) {
			this.appliedHighlightEnabled = enabled;
			this.reCreateListEntryWidgets();
		}
	}

	// 每帧推进平滑滚动：外部直接改滚动条时立即对齐，其余情况按缓动逼近目标。
	private void updateSmoothScroll() {
		long now = Util.getMillis();
		double deltaSeconds = this.lastFrameMillis < 0
			? 0.0
			: Math.min(MAX_FRAME_SECONDS, (now - this.lastFrameMillis) / 1000.0);
		this.lastFrameMillis = now;

		if (!smoothScrollEnabled()) {
			// 关闭时回到原生行为：位置完全由滚动条决定，并清掉残留的半行偏移。
			this.smoothScroll.jumpTo(this.scrollBar.getValue() * (double) this.entryHeight(),
				this.maxScrollPixels());
			if (this.appliedPixelOffset != 0) {
				this.appliedRow = this.scrollBar.getValue();
				this.appliedPixelOffset = 0;
				this.reCreateListEntryWidgets();
			}
			return;
		}

		// 分组展开/收起、滚动记忆恢复、拖动滚动条、键盘翻页都会直接改滚动条值：
		// 此时立即对齐到整行位置，避免旧动画把列表拉回去与新的滚动位置打架。
		if (this.scrollBar.getValue() != this.appliedRow) {
			this.smoothScroll.jumpTo(this.scrollBar.getValue() * (double) this.entryHeight(),
				this.maxScrollPixels());
		}

		this.smoothScroll.setTimeConstant(SmoothScrolling.timeConstantSeconds());
		this.smoothScroll.update(deltaSeconds, this.maxScrollPixels());
		SmoothScrollAnimator.Layout layout = this.smoothScroll.layout(this.entryHeight());
		if (layout.row() != this.appliedRow) {
			// 换行：新条目进入可视区，必须重建控件。
			this.appliedRow = layout.row();
			this.appliedPixelOffset = layout.pixelOffset();
			this.scrollBar.setValue(this.appliedRow);
			this.reCreateListEntryWidgets();
		} else if (layout.pixelOffset() != this.appliedPixelOffset) {
			// 同一行内只是亚行偏移变化：直接平移已有控件。
			// 每帧重建控件会丢失控件内部状态（会重新注册快捷键监听）并造成闪烁。
			int previousOffset = this.appliedPixelOffset;
			this.appliedPixelOffset = layout.pixelOffset();
			this.shiftEntriesBy(previousOffset - this.appliedPixelOffset);
		}
	}

	// 按像素平移可视条目（含子控件）。条目坐标都是绝对值，子控件不会跟随父控件移动，
	// 所以需要逐层平移；遇到非本模组的条目控件时退回重建，避免只动了一部分行。
	private void shiftEntriesBy(int deltaY) {
		if (deltaY == 0) {
			return;
		}
		for (WidgetConfigOption widget : this.listWidgets) {
			if (!(widget instanceof TweakifyModConfigOptionWidget entry)) {
				this.reCreateListEntryWidgets();
				return;
			}
			entry.shiftVertically(deltaY);
		}
	}

	// 与 MaLiLib 0.30.2 的 WidgetListBase#drawContents 保持相同的绘制顺序，差别仅限于：
	// 条目绘制加了裁剪（半行滚动时最上面一行会越出可视区，不裁剪会画到搜索框与页脚上），
	// 以及悬停判定与裁剪区取交集。升级 MaLiLib 时需对照上游重新核对本方法。
	@Override
	public void drawContents(GuiContext gui, int mouseX, int mouseY, float partialTicks) {
		this.refreshHighlightState();
		this.updateSmoothScroll();

		int scrollbarHeight = this.browserHeight - this.browserEntriesOffsetY - 8;
		int totalHeight = 0;
		for (GuiConfigsBase.ConfigOptionWrapper entry : this.listContents) {
			totalHeight += this.getBrowserEntryHeightFor(entry);
		}
		totalHeight = Math.max(totalHeight, scrollbarHeight);

		int scrollBarX = this.posX + this.browserWidth - 9;
		int scrollBarY = this.browserEntriesStartY + this.browserEntriesOffsetY;
		this.scrollBar.render(gui, mouseX, mouseY, partialTicks, scrollBarX, scrollBarY, 8, scrollbarHeight, totalHeight);

		// 拖动滚动条时值会在上面更新：与父类一样在这里重建。
		if (this.scrollBar.getValue() != this.lastScrollbarPosition) {
			this.lastScrollbarPosition = this.scrollBar.getValue();
			this.appliedRow = this.lastScrollbarPosition;
			this.appliedPixelOffset = 0;
			this.smoothScroll.jumpTo(this.appliedRow * (double) this.entryHeight(), this.maxScrollPixels());
			this.reCreateListEntryWidgets();
		}

		WidgetBase hovered = null;
		// 窗口极小时列表高度可能为非正数：不裁剪，交给控件自身处理。
		int clipHeight = this.entriesHeight();
		boolean clipped = clipHeight > 0 && this.browserEntryWidth > 0;
		if (clipped) {
			gui.pushScissor(new ScreenRectangle(this.entriesLeft(), this.entriesTop(),
				this.browserEntryWidth, clipHeight));
		}
		try {
			// 悬停必须与裁剪区一致：亚行偏移下最上面一行会越到搜索框区域，
			// 不取交集会让被裁掉的那一行抢走搜索框的 hoveredWidget。
			boolean mouseInClip = !clipped
				|| (mouseX >= this.entriesLeft() && mouseX < this.entriesLeft() + this.browserEntryWidth
					&& mouseY >= this.entriesTop() && mouseY < this.entriesTop() + clipHeight);
			for (WidgetConfigOption widget : this.listWidgets) {
				GuiConfigsBase.ConfigOptionWrapper entry = widget.getEntry();
				boolean selected = this.allowMultiSelection
					? this.selectedEntries.contains(entry)
					: entry != null && entry.equals(this.getLastSelectedEntry());
				widget.render(gui, mouseX, mouseY, selected);

				if (mouseInClip && widget.isMouseOver(mouseX, mouseY)) {
					hovered = widget;
				}
			}
		} finally {
			if (clipped) {
				gui.popScissor();
			}
		}

		if (this.widgetSearchBar != null) {
			this.widgetSearchBar.render(gui, mouseX, mouseY, false);

			if (hovered == null && this.widgetSearchBar.isMouseOver(mouseX, mouseY)) {
				hovered = this.widgetSearchBar;
			}
		}

		this.hoveredWidget = hovered;
	}

	// 配置条目使用加高布局，为双层标签留出第二行空间。
	@Override
	protected int getBrowserEntryHeightFor(GuiConfigsBase.ConfigOptionWrapper entry) {
		return entry != null && entry.getType() == GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG
			? TweakifyModConfigOptionWidget.ENTRY_HEIGHT
			: super.getBrowserEntryHeightFor(entry);
	}

	// 搜索时使用完整树（忽略分组折叠），否则折叠分组里的子项永远无法被搜到。
	// 只对纯文本搜索生效：快捷键过滤（单独或与文本组合）完全交回 MaLiLib。
	@Override
	protected Collection<GuiConfigsBase.ConfigOptionWrapper> getAllEntries() {
		TweakifyModConfigScreen screen = this.parent instanceof TweakifyModConfigScreen target ? target : null;
		if (screen != null && treeSearchActive()) {
			return GuiConfigsBase.ConfigOptionWrapper.createFor(
				TweakifyModConfigScreen.searchConfigs(screen.currentTab()));
		}
		return super.getAllEntries();
	}

	// 树感知过滤：父项命中时把整组子项一起列出；子项命中时补上父级路径。
	// 先交给 MaLiLib 完成文本/快捷键过滤，再按配置目录补全关系，保留上游的过滤语义；
	// 收藏标签与快捷键过滤路径不参与扩展，结果与上游完全一致。
	@Override
	protected void addFilteredContents(Collection<GuiConfigsBase.ConfigOptionWrapper> entries) {
		super.addFilteredContents(entries);
		TweakifyModConfigScreen screen = this.parent instanceof TweakifyModConfigScreen target ? target : null;
		if (screen == null || !shouldApplyTreeExpansion(screen.currentTab(), treeSearchActive())) {
			return;
		}
		Set<String> matched = new LinkedHashSet<>();
		for (GuiConfigsBase.ConfigOptionWrapper wrapper : this.listContents) {
			IConfigBase config = wrapper.getConfig();
			if (config != null) {
				matched.add(config.getName());
			}
		}
		if (matched.isEmpty()) {
			return;
		}
		Set<String> included = expandMatchedNames(matched);
		this.listContents.clear();
		for (GuiConfigsBase.ConfigOptionWrapper wrapper : entries) {
			IConfigBase config = wrapper.getConfig();
			if (config != null && included.contains(config.getName())) {
				this.listContents.add(wrapper);
			}
		}
		TweakifyModConfigScreen.markSearchVisible(included);
	}

	// 纯文本搜索才启用树感知：快捷键过滤优先，避免给快捷键命中项额外补父子项。
	private boolean treeSearchActive() {
		return shouldUseTreeSearch(this.hasFilter(), hasKeybindFilter(), this.getFilterText());
	}

	private boolean hasKeybindFilter() {
		return this.widgetSearchConfigs != null
			&& !this.widgetSearchConfigs.getKeybind().getKeys().isEmpty();
	}

	// 包私有：供搜索回归测试直接校验快捷键与纯文本的开关条件。
	static boolean shouldUseTreeSearch(boolean hasFilter, boolean hasKeybindFilter, String filterText) {
		return hasFilter && !hasKeybindFilter && filterText != null && !filterText.isBlank();
	}

	// 包私有：收藏标签不参与树扩展，保持只显示收藏项的语义。
	static boolean shouldApplyTreeExpansion(TweakifyModConfigScreen.ConfigTab tab, boolean treeSearch) {
		return treeSearch && tab != TweakifyModConfigScreen.ConfigTab.FAVORITES;
	}

	// 匹配项连同祖先路径与全部后代；包私有：供搜索回归测试直接校验。
	static Set<String> expandMatchedNames(Collection<String> matched) {
		Set<String> included = new LinkedHashSet<>();
		Set<String> expanded = new LinkedHashSet<>();
		for (String name : matched) {
			included.addAll(ConfigCatalog.ancestorsOf(name));
			addDescendants(name, included, expanded);
			included.add(name);
		}
		return included;
	}

	private static void addDescendants(String name, Set<String> included, Set<String> expanded) {
		if (!expanded.add(name)) {
			return;
		}
		for (ConfigCatalog.Entry child : ConfigCatalog.childrenOf(name)) {
			included.add(child.name());
			addDescendants(child.name(), included, expanded);
		}
	}

	// 标签列宽度取本地化名称与英文配置键中较长者，并计入树形缩进，且不超过按钮列起点。
	@Override
	public int getMaxNameLengthWrapped(List<GuiConfigsBase.ConfigOptionWrapper> list) {
		int maxLength = 0;
		for (GuiConfigsBase.ConfigOptionWrapper wrapper : list) {
			if (wrapper.getType() != GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG) {
				continue;
			}
			IConfigBase option = wrapper.getConfig();
			if (option == null) {
				continue;
			}
			TweakifyModConfigScreen.NodeInfo info = TweakifyModConfigScreen.getNodeInfo(option);
			int indent = (info == null ? 0 : info.depth()) * TweakifyModConfigOptionWidget.INDENT_WIDTH
				+ TweakifyModConfigOptionWidget.LABEL_OFFSET;
			maxLength = Math.max(maxLength, this.getStringWidth(option.getConfigGuiDisplayName()) + indent);
			maxLength = Math.max(maxLength, this.getStringWidth(option.getName()) + indent);
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
