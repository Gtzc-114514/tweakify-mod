package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.AdvancementsStateStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 更好的进度界面里的单个标签页。
 *
 * <p>沿用原版 {@link AdvancementTab} 的根组件与父子连接逻辑，但自行维护滚动原点与
 * 边界，使进度树可以铺满自适应大小的窗口；标签图标与固定标签由界面统一绘制。
 * 结构与 {@code DaFuqs/PaginatedAdvancements}（MIT）保持一致，只保留进度树本身，
 * 不引入自定义边框与调试信息。
 */
public final class BetterAdvancementTab extends AdvancementTab {
	private final Minecraft client;
	private final BetterAdvancementScreen screen;
	private final AdvancementNode rootNode;
	private final ItemStack icon;
	private final Component title;
	private final Identifier background;
	private final AdvancementWidget rootWidget;
	private final Map<AdvancementHolder, AdvancementWidget> widgets = new LinkedHashMap<>();

	private int pinnedIndex = -1;
	private double originX;
	private double originY;
	private int minPanX = Integer.MAX_VALUE;
	private int minPanY = Integer.MAX_VALUE;
	private int maxPanX = Integer.MIN_VALUE;
	private int maxPanY = Integer.MIN_VALUE;
	private float fade;
	private boolean centered;
	private int lastViewWidth = -1;
	private int lastViewHeight = -1;
	private @Nullable AdvancementWidget hoveredWidget;

	private BetterAdvancementTab(Minecraft client, BetterAdvancementScreen screen, int index,
								 AdvancementNode rootNode, DisplayInfo display, AdvancementWidget rootWidget,
								 Identifier background) {
		super(client, screen, AdvancementTabType.ABOVE, index, rootWidget, display.icon(),
			display.title(), background);
		this.client = client;
		this.screen = screen;
		this.rootNode = rootNode;
		this.icon = display.icon().create();
		this.title = display.title();
		this.background = background;
		this.rootWidget = rootWidget;
		this.widgets.put(rootNode.holder(), rootWidget);
	}

	public static @Nullable BetterAdvancementTab create(Minecraft client, BetterAdvancementScreen screen,
														int index, AdvancementNode root) {
		DisplayInfo display = root.advancement().display().orElse(null);
		if (display == null) {
			return null;
		}
		AdvancementWidget widget = AdvancementWidget.createWidget(client, root);
		if (widget == null) {
			return null;
		}
		Identifier background = display.background()
			.map(ClientAsset.ResourceTexture::texturePath)
			.orElse(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
		return new BetterAdvancementTab(client, screen, index, root, display, widget, background);
	}

	@Override
	public void copyPosition(AdvancementTab source) {
		if (source instanceof BetterAdvancementTab tab) {
			this.originX = tab.originX;
			this.originY = tab.originY;
			this.centered = tab.centered;
			this.lastViewWidth = tab.lastViewWidth;
			this.lastViewHeight = tab.lastViewHeight;
		}
	}

	public AdvancementNode getRootNode() {
		return this.rootNode;
	}

	public ItemStack getIcon() {
		return this.icon;
	}

	public int getPinnedIndex() {
		return this.pinnedIndex;
	}

	public void setPinnedIndex(int index) {
		this.pinnedIndex = index;
	}

	public @Nullable AdvancementWidget getHoveredWidget() {
		return this.hoveredWidget;
	}

	@Override
	public @Nullable AdvancementWidget getWidget(AdvancementHolder advancement) {
		return this.widgets.get(advancement);
	}

	public void addAdvancement(AdvancementNode node) {
		AdvancementWidget widget = AdvancementWidget.createWidget(this.client, node);
		if (widget != null) {
			this.widgets.put(node.holder(), widget);
		}
	}

	// 所有组件齐了再统一连父子：父节点晚于子节点下发时也不会漏连。
	public void finishLoading() {
		for (AdvancementWidget widget : this.widgets.values()) {
			widget.attachToParent(this);
		}
		this.calculatePan();
	}

	private void calculatePan() {
		this.minPanX = Integer.MAX_VALUE;
		this.minPanY = Integer.MAX_VALUE;
		this.maxPanX = Integer.MIN_VALUE;
		this.maxPanY = Integer.MIN_VALUE;
		for (AdvancementWidget widget : this.widgets.values()) {
			int startX = widget.getX();
			int endX = startX + 28;
			int startY = widget.getY();
			int endY = startY + 27;
			this.minPanX = Math.min(this.minPanX, startX);
			this.maxPanX = Math.max(this.maxPanX, endX);
			this.minPanY = Math.min(this.minPanY, startY);
			this.maxPanY = Math.max(this.maxPanY, endY);
		}
	}

	// 进度树绘制在窗口内部区域；调用方已完成裁剪与坐标平移。
	public void render(GuiGraphicsExtractor graphics, int viewWidth, int viewHeight) {
		clampToView(viewWidth, viewHeight);
		int treeX = Mth.floor(this.originX);
		int treeY = Mth.floor(this.originY);
		int offsetX = Math.floorMod(treeX, 16);
		int offsetY = Math.floorMod(treeY, 16);

		int countX = viewWidth / 16 + 2;
		int countY = viewHeight / 16 + 2;
		for (int tileX = -1; tileX < countX; tileX++) {
			for (int tileY = -1; tileY < countY; tileY++) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.background,
					offsetX + 16 * tileX, offsetY + 16 * tileY, 0.0F, 0.0F, 16, 16, 16, 16);
			}
		}
		this.rootWidget.extractConnectivity(graphics, treeX, treeY, true);
		this.rootWidget.extractConnectivity(graphics, treeX, treeY, false);
		this.rootWidget.extractRenderState(graphics, treeX, treeY);
	}

	// 悬停检测：只更新状态，不绘制。坐标是相对窗口内部左上角的局部坐标。
	public void updateHover(int mouseX, int mouseY, int viewWidth, int viewHeight) {
		boolean hovering = false;
		if (mouseX > 0 && mouseX < viewWidth && mouseY > 0 && mouseY < viewHeight) {
			int treeX = Mth.floor(this.originX);
			int treeY = Mth.floor(this.originY);
			for (AdvancementWidget widget : this.widgets.values()) {
				if (widget.isMouseOver(treeX, treeY, mouseX, mouseY)) {
					hovering = true;
					this.hoveredWidget = widget;
					break;
				}
			}
		}
		if (!hovering) {
			this.hoveredWidget = null;
		}
		// 与参考实现同一组淡入/淡出步进：悬停时 0.02，离开时 0.04。
		this.fade = hovering
			? Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F)
			: Mth.clamp(this.fade - 0.04F, 0.0F, 0.3F);
	}

	// 悬停信息框：必须在关闭 scissor、画完窗口边框之后调用。
	// 原版 extractHover 把 113 固定视图高度写死用于上下翻转；这里传入偏移后的 yo，
	// 同时用 pose 平移补偿绘制位置，使翻转阈值按实际 viewHeight 生效。
	public void renderTooltip(GuiGraphicsExtractor graphics, int viewWidth, int viewHeight,
							 int screenX, int screenWidth) {
		if (!BetterAdvancements.disableFade() && this.fade > 0.0F) {
			graphics.fill(0, 0, viewWidth, viewHeight, Mth.floor(this.fade * 255.0F) << 24);
		}
		if (this.hoveredWidget == null) {
			return;
		}
		int treeX = Mth.floor(this.originX);
		int treeY = Mth.floor(this.originY);
		int yOffset = BetterAdvancements.tooltipYOffset(viewHeight);
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(0.0F, -yOffset);
			this.hoveredWidget.extractHover(graphics, treeX, treeY + yOffset, this.fade,
				screenX, 0, screenWidth);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	public void scroll(double deltaX, double deltaY) {
		scrollTo(this.originX + deltaX, this.originY + deltaY, this.lastViewWidth, this.lastViewHeight);
	}

	// 拖动：与滚轮共用夹紧逻辑。
	public void move(double deltaX, double deltaY, int viewWidth, int viewHeight) {
		scrollTo(this.originX + deltaX, this.originY + deltaY, viewWidth, viewHeight);
	}

	private void scrollTo(double targetX, double targetY, int viewWidth, int viewHeight) {
		if (viewWidth <= 0 || viewHeight <= 0) {
			return;
		}
		this.originX = canScrollHorizontally(viewWidth)
			? Mth.clamp(targetX, viewWidth - this.maxPanX, -this.minPanX)
			: centeredX(viewWidth);
		this.originY = canScrollVertically(viewHeight)
			? Mth.clamp(targetY, viewHeight - this.maxPanY, -this.minPanY)
			: centeredY(viewHeight);
	}

	public boolean canScrollHorizontally(int viewWidth) {
		return this.maxPanX - this.minPanX > viewWidth;
	}

	public boolean canScrollVertically(int viewHeight) {
		return this.maxPanY - this.minPanY > viewHeight;
	}

	private double centeredX(int viewWidth) {
		return viewWidth / 2.0 - (this.maxPanX + this.minPanX) / 2.0;
	}

	private double centeredY(int viewHeight) {
		return viewHeight / 2.0 - (this.maxPanY + this.minPanY) / 2.0;
	}

	// 首次渲染与窗口尺寸变化时重新居中/夹紧，避免窗口拉伸后进度树跑出可视区。
	public void clampToView(int viewWidth, int viewHeight) {
		if (viewWidth <= 0 || viewHeight <= 0) {
			return;
		}
		boolean resized = viewWidth != this.lastViewWidth || viewHeight != this.lastViewHeight;
		if (!this.centered || resized) {
			this.originX = centeredX(viewWidth);
			this.originY = centeredY(viewHeight);
			this.centered = true;
			this.lastViewWidth = viewWidth;
			this.lastViewHeight = viewHeight;
		}
		this.originX = canScrollHorizontally(viewWidth)
			? Mth.clamp(this.originX, viewWidth - this.maxPanX, -this.minPanX)
			: centeredX(viewWidth);
		this.originY = canScrollVertically(viewHeight)
			? Mth.clamp(this.originY, viewHeight - this.maxPanY, -this.minPanY)
			: centeredY(viewHeight);
	}

	public void setProgress(AdvancementHolder holder, AdvancementProgress progress) {
		AdvancementWidget widget = this.widgets.get(holder);
		if (widget != null) {
			widget.setProgress(progress);
		}
	}

	public void setPinIndexFromStorage() {
		this.pinnedIndex = AdvancementsStateStorage.pinIndex(this.rootNode.holder().id().toString());
	}

	public Identifier getId() {
		return this.rootNode.holder().id();
	}

	@Override
	public Component getTitle() {
		return this.title;
	}

	@Override
	public BetterAdvancementScreen getScreen() {
		return this.screen;
	}
}
