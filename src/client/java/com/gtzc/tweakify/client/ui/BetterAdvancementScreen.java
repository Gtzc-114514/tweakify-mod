package com.gtzc.tweakify.client.ui;

import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.AdvancementsStateStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 更好的进度界面：参考 {@code DaFuqs/PaginatedAdvancements}（MIT）的自适应窗口。
 *
 * <p>与原版相比：窗口随屏幕放大、顶部标签溢出时自动分页、右侧可固定标签、记住上次
 * 打开的标签，并可关闭悬停时的背景压暗。进度树仍复用原版 {@link AdvancementWidget}，
 * 不引入自定义边框或调试信息，保证与原版数据包完全兼容。
 */
public class BetterAdvancementScreen extends AdvancementsScreen implements ClientAdvancements.Listener {
	private static final int BORDER_PADDING = 32;
	private static final int ADDITIONAL_PADDING_TOP = 20;
	private static final int ELEMENT_WIDTH = 15;
	private static final int TOP_ELEMENT_HEIGHT = 22;
	private static final int BOTTOM_ELEMENT_HEIGHT = 15;
	private static final Identifier WINDOW_TEXTURE =
		Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
	private static final Component TITLE = Component.translatable("gui.advancements");
	private static final Component EMPTY_LABEL = Component.translatable("advancements.empty");
	private static final Component SAD_LABEL = Component.translatable("advancements.sad_label");

	private final ClientAdvancements advancements;
	private final Map<AdvancementHolder, BetterAdvancementTab> tabs = new LinkedHashMap<>();
	private final Map<AdvancementHolder, BetterAdvancementTab> pinnedTabs = new LinkedHashMap<>();
	private @Nullable BetterAdvancementTab selectedTab;
	private boolean dragging;
	private int currentPage;

	public BetterAdvancementScreen(ClientAdvancements advancements) {
		super(advancements);
		this.advancements = advancements;
	}

	@Override
	protected void init() {
		// 不使用原版的固定 252x140 布局与底部按钮；只接管进度数据监听。
		this.advancements.setListener(this);
	}

	@Override
	protected void repositionElements() {
		this.advancements.setListener(this);
		rebuildPinnedTabs();
	}

	@Override
	public void removed() {
		this.advancements.setListener(null);
		ClientPacketListener connection = this.minecraft == null ? null : this.minecraft.getConnection();
		if (connection != null) {
			connection.send(ServerboundSeenAdvancementsPacket.closedScreen());
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(null);
	}

	@Override
	public void onAdvancementsUpdated() {
		Map<AdvancementHolder, BetterAdvancementTab> oldTabs = Map.copyOf(this.tabs);
		AdvancementHolder selectedRoot = this.selectedTab == null ? null : this.selectedTab.getRootNode().holder();
		this.tabs.clear();
		this.pinnedTabs.clear();

		int index = 0;
		for (AdvancementNode root : this.advancements.tree().roots()) {
			BetterAdvancementTab tab = BetterAdvancementTab.create(this.minecraft, this, index, root);
			if (tab != null) {
				BetterAdvancementTab oldTab = oldTabs.get(root.holder());
				if (oldTab != null) {
					tab.copyPosition(oldTab);
				}
				this.tabs.put(root.holder(), tab);
				index++;
			}
		}
		for (AdvancementNode task : this.advancements.tree().tasks()) {
			BetterAdvancementTab tab = this.tabs.get(task.root().holder());
			if (tab != null) {
				tab.addAdvancement(task);
			}
		}
		for (BetterAdvancementTab tab : this.tabs.values()) {
			tab.finishLoading();
		}
		updateProgress();
		rebuildPinnedTabs();

		this.selectedTab = selectedRoot == null ? null : this.tabs.get(selectedRoot);
		if (this.selectedTab == null && BetterAdvancements.rememberTab()) {
			Identifier saved = Identifier.tryParse(AdvancementsStateStorage.lastSelectedTab());
			if (saved != null) {
				for (BetterAdvancementTab tab : this.tabs.values()) {
					if (tab.getId().equals(saved)) {
						this.selectedTab = tab;
						break;
					}
				}
			}
		}
		if (this.selectedTab == null && !this.tabs.isEmpty()) {
			this.selectedTab = this.tabs.values().iterator().next();
		}
		AdvancementHolder newSelectedRoot = this.selectedTab == null ? null : this.selectedTab.getRootNode().holder();
		if (!Objects.equals(selectedRoot, newSelectedRoot)) {
			this.advancements.setSelectedTab(newSelectedRoot, true);
		}
	}

	@Override
	public void onSelectedTabChanged(@Nullable AdvancementHolder advancement) {
		this.selectedTab = advancement == null ? null : this.tabs.get(advancement);
		if (this.selectedTab != null && BetterAdvancements.rememberTab()) {
			AdvancementsStateStorage.setLastSelectedTab(this.selectedTab.getId().toString());
		}
	}

	@Override
	public void onAdvancementsCleared() {
		this.tabs.clear();
		this.pinnedTabs.clear();
		this.selectedTab = null;
		this.currentPage = 0;
	}

	@Override
	public @Nullable AdvancementWidget getAdvancementWidget(AdvancementNode node) {
		BetterAdvancementTab tab = this.tabs.get(node.root().holder());
		return tab == null ? null : tab.getWidget(node.holder());
	}

	private void updateProgress() {
		this.advancements.progress().forEach((holder, progress) -> {
			AdvancementNode node = this.advancements.tree().get(holder);
			if (node == null) {
				return;
			}
			BetterAdvancementTab tab = this.tabs.get(node.root().holder());
			if (tab != null) {
				tab.setProgress(holder, progress);
			}
		});
	}

	private void rebuildPinnedTabs() {
		this.pinnedTabs.clear();
		for (BetterAdvancementTab tab : this.tabs.values()) {
			tab.setPinnedIndex(-1);
		}
		if (!BetterAdvancements.pinTabs() || this.tabs.isEmpty()) {
			return;
		}
		for (String saved : AdvancementsStateStorage.pinnedTabs()) {
			Identifier id = Identifier.tryParse(saved);
			AdvancementHolder holder = id == null ? null : this.advancements.get(id);
			BetterAdvancementTab tab = holder == null ? null : this.tabs.get(holder);
			if (tab != null) {
				tab.setPinnedIndex(this.pinnedTabs.size());
				this.pinnedTabs.put(holder, tab);
			}
		}
	}

	// ---- 几何 ----

	private Geometry geometry() {
		boolean hasPins = BetterAdvancements.pinTabs() && !this.pinnedTabs.isEmpty();
		int reserve = hasPins ? BetterAdvancements.PINNED_TAB_WIDTH : 0;
		int startX = BORDER_PADDING;
		int startY = BORDER_PADDING + ADDITIONAL_PADDING_TOP;
		int endXTitle = Math.max(startX + 200, this.width - BORDER_PADDING);
		int endXWindow = Math.max(startX + 180, this.width - BORDER_PADDING - reserve);
		int endY = Math.max(startY + 120, this.height - BORDER_PADDING);
		return new Geometry(startX, startY, endXTitle, endXWindow, endY);
	}

	private record Geometry(int startX, int startY, int endXTitle, int endXWindow, int endY) {
		int viewLeft() {
			return this.startX + 9;
		}

		int viewTop() {
			return this.startY + 18;
		}

		int viewRight() {
			return this.endXWindow - 9;
		}

		int viewBottom() {
			return this.endY - 9;
		}

		int viewWidth() {
			return this.viewRight() - this.viewLeft();
		}

		int viewHeight() {
			return this.viewBottom() - this.viewTop();
		}
	}

	private boolean paginated(Geometry geometry) {
		return BetterAdvancements.isPaginated(this.tabs.size(), geometry.startX(), geometry.endXTitle());
	}

	private int maxTabsPerPage(Geometry geometry, boolean paginated) {
		return BetterAdvancements.maxTabsPerPage(geometry.startX(), geometry.endXTitle(), paginated);
	}

	// ---- 渲染 ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		Geometry geometry = geometry();
		boolean paginated = paginated(geometry);
		int maxTabs = maxTabsPerPage(geometry, paginated);
		int pageCount = BetterAdvancements.pageCount(this.tabs.size(), maxTabs);
		this.currentPage = Mth.clamp(this.currentPage, 0, Math.max(0, pageCount - 1));

		drawTree(graphics, geometry, mouseX, mouseY);
		drawWindow(graphics, geometry);
		if (this.tabs.size() > 1) {
			if (paginated) {
				drawNavigationButtons(graphics, geometry, mouseX, mouseY);
			}
			drawTabs(graphics, geometry, mouseX, mouseY, paginated, maxTabs);
		}
		drawPinnedTabs(graphics, geometry, mouseX, mouseY);
		drawPinButton(graphics, geometry, mouseX, mouseY);
		// 信息框必须在窗口边框、标签与固定栏都画完之后再画，否则会被裁掉或盖住。
		drawTooltip(graphics, geometry);
	}

	private void drawTree(GuiGraphicsExtractor graphics, Geometry geometry, int mouseX, int mouseY) {
		BetterAdvancementTab tab = this.selectedTab;
		if (tab == null) {
			graphics.fill(geometry.viewLeft(), geometry.viewTop(),
				geometry.viewRight(), geometry.viewBottom(), 0xFF000000);
			int centerX = (geometry.viewLeft() + geometry.viewRight()) / 2;
			int centerY = (geometry.viewTop() + geometry.viewBottom()) / 2;
			graphics.centeredText(this.font, EMPTY_LABEL, centerX, centerY, 0xFFFFFFFF);
			graphics.centeredText(this.font, SAD_LABEL, centerX, centerY + 16, 0xFFFFFFFF);
			return;
		}
		int viewWidth = geometry.viewWidth();
		int viewHeight = geometry.viewHeight();
		graphics.enableScissor(geometry.viewLeft(), geometry.viewTop(),
			geometry.viewRight(), geometry.viewBottom());
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(geometry.viewLeft(), geometry.viewTop());
			tab.render(graphics, viewWidth, viewHeight);
			tab.updateHover(mouseX - geometry.viewLeft(), mouseY - geometry.viewTop(),
				viewWidth, viewHeight);
		} finally {
			graphics.pose().popMatrix();
			graphics.disableScissor();
		}
	}

	// 信息框独立一遍，关闭裁剪并进入新的渲染层；翻转阈值由 tab 内部按实际高度换算。
	private void drawTooltip(GuiGraphicsExtractor graphics, Geometry geometry) {
		BetterAdvancementTab tab = this.selectedTab;
		if (tab == null) {
			return;
		}
		int viewWidth = geometry.viewWidth();
		int viewHeight = geometry.viewHeight();
		graphics.nextStratum();
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(geometry.viewLeft(), geometry.viewTop());
			tab.renderTooltip(graphics, viewWidth, viewHeight, geometry.startX(), this.width);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private void drawWindow(GuiGraphicsExtractor graphics, Geometry geometry) {
		int startX = geometry.startX();
		int startY = geometry.startY();
		int endX = geometry.endXWindow();
		int endY = geometry.endY();
		// 四角
		graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, startX, startY, 0, 0,
			ELEMENT_WIDTH, TOP_ELEMENT_HEIGHT, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, endX - ELEMENT_WIDTH, startY,
			237, 0, ELEMENT_WIDTH, TOP_ELEMENT_HEIGHT, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, startX, endY - BOTTOM_ELEMENT_HEIGHT,
			0, 125, ELEMENT_WIDTH, BOTTOM_ELEMENT_HEIGHT, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, endX - ELEMENT_WIDTH,
			endY - BOTTOM_ELEMENT_HEIGHT, 237, 125, ELEMENT_WIDTH, BOTTOM_ELEMENT_HEIGHT, 256, 256);

		// 左右边框：最多 100 像素一段，避免纹理坐标越界。
		int sideHeight = endY - startY - TOP_ELEMENT_HEIGHT - BOTTOM_ELEMENT_HEIGHT;
		int currentY = startY + TOP_ELEMENT_HEIGHT;
		while (sideHeight > 0) {
			int drawHeight = Math.min(sideHeight, 100);
			graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, startX, currentY,
				0, TOP_ELEMENT_HEIGHT, ELEMENT_WIDTH, drawHeight, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, endX - ELEMENT_WIDTH, currentY,
				237, TOP_ELEMENT_HEIGHT, ELEMENT_WIDTH, drawHeight, 256, 256);
			sideHeight -= drawHeight;
			currentY += drawHeight;
		}

		// 上下边框：最多 220 像素一段。
		int middleWidth = endX - startX - ELEMENT_WIDTH * 2;
		int currentX = startX + ELEMENT_WIDTH;
		while (middleWidth > 0) {
			int drawWidth = Math.min(middleWidth, 220);
			graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, currentX, startY,
				ELEMENT_WIDTH, 0, drawWidth, TOP_ELEMENT_HEIGHT, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, currentX, endY - BOTTOM_ELEMENT_HEIGHT,
				ELEMENT_WIDTH, 125, drawWidth, BOTTOM_ELEMENT_HEIGHT, 256, 256);
			middleWidth -= drawWidth;
			currentX += drawWidth;
		}

		graphics.text(this.font, TITLE, startX + 8, startY + 6, 0xFF404040, false);
	}

	private void drawNavigationButtons(GuiGraphicsExtractor graphics, Geometry geometry, int mouseX, int mouseY) {
		int y = geometry.startY() - BetterAdvancements.TAB_HEIGHT + BetterAdvancements.TAB_ROW_OFFSET;
		drawNavigationButton(graphics, geometry.startX(), y, false,
			isOver(mouseX, mouseY, geometry.startX(), y,
				BetterAdvancements.NAV_BUTTON_SIZE, BetterAdvancements.NAV_BUTTON_SIZE));
		int forwardX = geometry.endXTitle() - BetterAdvancements.NAV_BUTTON_SIZE;
		drawNavigationButton(graphics, forwardX, y, true,
			isOver(mouseX, mouseY, forwardX, y,
				BetterAdvancements.NAV_BUTTON_SIZE, BetterAdvancements.NAV_BUTTON_SIZE));
	}

	private static void drawNavigationButton(GuiGraphicsExtractor graphics, int x, int y, boolean forward,
											 boolean hovered) {
		graphics.fill(x, y, x + BetterAdvancements.NAV_BUTTON_SIZE, y + BetterAdvancements.NAV_BUTTON_SIZE,
			hovered ? 0x66000000 : 0x33000000);
		int color = hovered ? 0xFFFFFFFF : 0xFFD0D0D0;
		for (int row = 0; row < 8; row++) {
			int length = row <= 3 ? row : 7 - row;
			int lineX = forward ? x + 8 + length : x + 14 - length;
			graphics.fill(lineX, y + 8 + row, lineX + 1, y + 9 + row, color);
		}
	}

	private void drawTabs(GuiGraphicsExtractor graphics, Geometry geometry, int mouseX, int mouseY,
						  boolean paginated, int maxTabs) {
		List<BetterAdvancementTab> list = new ArrayList<>(this.tabs.values());
		int visibleCount = 0;
		for (int index = 0; index < list.size(); index++) {
			if (BetterAdvancements.visibleOnPage(index, maxTabs, this.currentPage, paginated)) {
				visibleCount++;
			}
		}
		if (visibleCount <= 0) {
			return;
		}
		int tabY = geometry.startY() - BetterAdvancements.TAB_HEIGHT + BetterAdvancements.TAB_ROW_OFFSET;
		for (int index = 0; index < list.size(); index++) {
			if (!BetterAdvancements.visibleOnPage(index, maxTabs, this.currentPage, paginated)) {
				continue;
			}
			BetterAdvancementTab tab = list.get(index);
			int position = BetterAdvancements.positionOnPage(index, maxTabs, this.currentPage, paginated);
			int tabX = BetterAdvancements.tabX(geometry.startX(), position, paginated);
			boolean selected = tab == this.selectedTab;
			int spriteIndex = BetterAdvancements.tabSpriteIndex(position, paginated, visibleCount);
			AdvancementTabType.ABOVE.extractRenderState(graphics, tabX, tabY, selected, spriteIndex);
			graphics.fakeItem(tab.getIcon(), tabX + 6, tabY + 9);
			if (isOver(mouseX, mouseY, tabX, tabY, BetterAdvancements.TAB_WIDTH,
				BetterAdvancements.TAB_HEIGHT)) {
				graphics.setTooltipForNextFrame(this.font, tab.getTitle(), mouseX, mouseY);
			}
		}
	}

	private void drawPinnedTabs(GuiGraphicsExtractor graphics, Geometry geometry, int mouseX, int mouseY) {
		if (!BetterAdvancements.pinTabs() || this.pinnedTabs.isEmpty()) {
			return;
		}
		int maxPinned = BetterAdvancements.maxPinnedTabs(geometry.startY(), geometry.endY());
		int visiblePinned = Math.min(this.pinnedTabs.size(), maxPinned);
		int index = 0;
		for (BetterAdvancementTab tab : this.pinnedTabs.values()) {
			if (index >= visiblePinned) {
				break;
			}
			// 固定标签向左叠进窗口边框：纹理左侧有约 3 像素透明内边距，
			// 只压住窗口外框内侧，不进入中间的内容区。
			int x = BetterAdvancements.pinnedTabX(geometry.endXWindow());
			int y = BetterAdvancements.pinnedTabY(geometry.startY(), index);
			int spriteIndex = BetterAdvancements.pinnedSpriteIndex(index, visiblePinned);
			AdvancementTabType.RIGHT.extractRenderState(graphics, x, y, tab == this.selectedTab, spriteIndex);
			graphics.fakeItem(tab.getIcon(), x + 6, y + 5);
			if (isOver(mouseX, mouseY, x, y, BetterAdvancements.PINNED_TAB_WIDTH,
				BetterAdvancements.PINNED_TAB_HEIGHT)) {
				graphics.setTooltipForNextFrame(this.font, tab.getTitle(), mouseX, mouseY);
			}
			index++;
		}
	}

	private void drawPinButton(GuiGraphicsExtractor graphics, Geometry geometry, int mouseX, int mouseY) {
		if (!BetterAdvancements.pinTabs() || this.selectedTab == null) {
			return;
		}
		int x = geometry.endXWindow() - 32;
		int y = geometry.startY() + 8;
		boolean hovered = isOver(mouseX, mouseY, x, y,
			BetterAdvancements.PIN_BUTTON_SIZE, BetterAdvancements.PIN_BUTTON_SIZE);
		if (hovered) {
			graphics.fill(x, y, x + BetterAdvancements.PIN_BUTTON_SIZE,
				y + BetterAdvancements.PIN_BUTTON_SIZE, 0x33FFFFFF);
		}
		boolean pinned = AdvancementsStateStorage.isPinned(this.selectedTab.getId().toString());
		int starX = x + (BetterAdvancements.PIN_BUTTON_SIZE - FavoriteStarRenderer.SIZE) / 2;
		int starY = y + (BetterAdvancements.PIN_BUTTON_SIZE - FavoriteStarRenderer.SIZE) / 2;
		FavoriteStarRenderer.drawStar(graphics, starX, starY, pinned, hovered);
	}

	// ---- 输入 ----

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		// 26.3 的 MouseButtonEvent 使用 1 表示左键（原版 AdvancementsScreen 同样判断 == 1）。
		if (click.button() == 1) {
			Geometry geometry = geometry();
			boolean paginated = paginated(geometry);
			int maxTabs = maxTabsPerPage(geometry, paginated);

			if (BetterAdvancements.pinTabs() && this.selectedTab != null
				&& isOver(click.x(), click.y(), geometry.endXWindow() - 32, geometry.startY() + 8,
				BetterAdvancements.PIN_BUTTON_SIZE, BetterAdvancements.PIN_BUTTON_SIZE)) {
				AdvancementsStateStorage.togglePinned(this.selectedTab.getId().toString());
				rebuildPinnedTabs();
				return true;
			}

			if (paginated) {
				int buttonY = geometry.startY() - BetterAdvancements.TAB_HEIGHT + BetterAdvancements.TAB_ROW_OFFSET;
				int forwardX = geometry.endXTitle() - BetterAdvancements.NAV_BUTTON_SIZE;
				if (isOver(click.x(), click.y(), geometry.startX(), buttonY,
					BetterAdvancements.NAV_BUTTON_SIZE, BetterAdvancements.NAV_BUTTON_SIZE)) {
					this.currentPage = (this.currentPage - 1 + BetterAdvancements.pageCount(this.tabs.size(), maxTabs))
						% BetterAdvancements.pageCount(this.tabs.size(), maxTabs);
					return true;
				}
				if (isOver(click.x(), click.y(), forwardX, buttonY,
					BetterAdvancements.NAV_BUTTON_SIZE, BetterAdvancements.NAV_BUTTON_SIZE)) {
					this.currentPage = (this.currentPage + 1)
						% BetterAdvancements.pageCount(this.tabs.size(), maxTabs);
					return true;
				}
			}

			int tabY = geometry.startY() - BetterAdvancements.TAB_HEIGHT + BetterAdvancements.TAB_ROW_OFFSET;
			List<BetterAdvancementTab> list = new ArrayList<>(this.tabs.values());
			for (int index = 0; index < list.size(); index++) {
				if (!BetterAdvancements.visibleOnPage(index, maxTabs, this.currentPage, paginated)) {
					continue;
				}
				int position = BetterAdvancements.positionOnPage(index, maxTabs, this.currentPage, paginated);
				int tabX = BetterAdvancements.tabX(geometry.startX(), position, paginated);
				if (isOver(click.x(), click.y(), tabX, tabY, BetterAdvancements.TAB_WIDTH,
					BetterAdvancements.TAB_HEIGHT)) {
					this.advancements.setSelectedTab(list.get(index).getRootNode().holder(), true);
					return true;
				}
			}

			if (BetterAdvancements.pinTabs() && !this.pinnedTabs.isEmpty()) {
				int maxPinned = BetterAdvancements.maxPinnedTabs(geometry.startY(), geometry.endY());
				int pinnedIndex = 0;
				for (BetterAdvancementTab tab : this.pinnedTabs.values()) {
					if (pinnedIndex >= maxPinned) {
						break;
					}
					int x = BetterAdvancements.pinnedTabX(geometry.endXWindow());
					int y = BetterAdvancements.pinnedTabY(geometry.startY(), pinnedIndex);
					if (isOver(click.x(), click.y(), x, y, BetterAdvancements.PINNED_TAB_WIDTH,
						BetterAdvancements.PINNED_TAB_HEIGHT)) {
						this.advancements.setSelectedTab(tab.getRootNode().holder(), true);
						return true;
					}
					pinnedIndex++;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
		if (click.button() != 1) {
			this.dragging = false;
			return false;
		}
		if (!this.dragging) {
			this.dragging = true;
		} else if (this.selectedTab != null) {
			Geometry geometry = geometry();
			this.selectedTab.move(dragX, dragY, geometry.viewWidth(), geometry.viewHeight());
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		this.dragging = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.selectedTab != null) {
			this.selectedTab.scroll(scrollX * 16.0, scrollY * 16.0);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.minecraft.options.keyAdvancements.matches(event)) {
			onClose();
			this.minecraft.mouseHandler.grabMouse();
			return true;
		}
		if (this.selectedTab != null && event.isCopy() && BetterAdvancements.copyId()) {
			AdvancementWidget hovered = this.selectedTab.getHoveredWidget();
			if (hovered != null) {
				String id = hovered.getAdvancement().id().toString();
				this.minecraft.keyboardHandler.setClipboard(id);
				this.minecraft.gui.hud.setOverlayMessage(
					Component.translatable("tweakify-mod.message.advancementCopied", id), false);
				return true;
			}
		}
		return super.keyPressed(event);
	}

	private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
