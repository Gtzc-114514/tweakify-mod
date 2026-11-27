package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.CreativeItemGroups;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix3x2fStack;

/**
 * 创造物品栏的自绘物品格：让滚动真正做到逐像素平滑。
 *
 * <p>原版物品格边框是烘焙在面板纹理里的，槽位内容又只能按整行重填；这里在面板之上重画一层
 * 9×5（外加一行“预告”）的槽位底纹，把烘焙底纹遮住，再由平滑滚动把 45 个槽位的 y 整体上移
 * 亚行像素。点击命中直接用 {@code Slot.x/y} 计算，因此槽位移动后 {@code getHoveredSlot}、
 * 取物与提示会自动跟随；被裁掉的那半行不允许命中，其余容器界面完全不受影响。
 *
 * <p>物品分组沿用同一份展示列表（{@code ItemPickerMenu.items}）：底板用自制原版外观的
 * {@code grid_slot}，成员格/组图标格的高亮仍由槽位绘制阶段叠加，预告行与真实行使用完全相同的判定。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class CreativeModeInventoryGridMixin {
	// 几何常量统一放在 SmoothScrolling，命中判定与绘制共用同一份数值。
	private static final int GRID_X = SmoothScrolling.CREATIVE_GRID_X;
	private static final int GRID_Y = SmoothScrolling.CREATIVE_GRID_Y;
	private static final int GRID_WIDTH = SmoothScrolling.CREATIVE_GRID_WIDTH;
	private static final int GRID_HEIGHT = SmoothScrolling.CREATIVE_GRID_HEIGHT;
	private static final int SLOT_SIZE = SmoothScrolling.CREATIVE_SLOT_SIZE;
	private static final int COLUMNS = 9;
	private static final int VISIBLE_ROWS = 5;
	private static final int MARKER_SIZE = 16;

	// 底板必须是“原版外观”的槽位：item_slot 是展开成员的深色高亮，拿它铺满整片
	// 会让所有格子变灰（分组展开后尤其明显）。grid_slot 按原版容器纹理的像素分布自制。
	private static final Identifier GRID_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/grid_slot");
	// 展开后的成员格高亮：与槽位绘制阶段完全同一张贴图。
	private static final Identifier ITEM_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/item_slot");
	private static final Identifier ICON_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/icon_slot");
	private static final Identifier PLUS_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/plus");
	private static final Identifier MINUS_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/minus");

	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@Shadow
	protected Slot hoveredSlot;

	// 槽位底纹 + 预告行：在 extractContents 调用完 extractLabels 之后、槽位高亮/物品之前绘制。
	// 注意不能直接注入 AbstractContainerScreen.extractLabels：创造页重写了该方法且不调 super。
	@Inject(
		method = "extractContents",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
			shift = At.Shift.AFTER
		)
	)
	private void tweakify$drawCreativeGrid(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick,
										   CallbackInfo ci) {
		if (!((Object) this instanceof CreativeModeInventoryScreen) || !SmoothScrolling.creativeGridActive()) {
			return;
		}
		CreativeModeInventoryScreen.ItemPickerMenu picker = this.tweakify$picker();
		if (picker == null) {
			return;
		}
		int shift = SmoothScrolling.creativeShiftPixels();
		int top = GRID_Y - shift;
		this.tweakify$pushGridClip(gui);
		try {
			// 5 行可视 + 1 行预告：即使偏移接近整行也能完整盖住原版烘焙底纹。
			for (int row = 0; row <= VISIBLE_ROWS; row++) {
				for (int column = 0; column < COLUMNS; column++) {
					gui.blitSprite(RenderPipelines.GUI_TEXTURED, GRID_SLOT_SPRITE,
						GRID_X + column * SLOT_SIZE - 1, top + row * SLOT_SIZE - 1,
						SLOT_SIZE, SLOT_SIZE);
				}
			}
			this.tweakify$drawPreviewRow(gui, picker, top + VISIBLE_ROWS * SLOT_SIZE);
		} finally {
			gui.disableScissor();
		}
	}

	// 预告行：下次跨行后进入底部的那一行。没有对应槽位，所以物品、数量与分组标记都在这里画。
	@Unique
	private void tweakify$drawPreviewRow(GuiGraphicsExtractor gui,
										 CreativeModeInventoryScreen.ItemPickerMenu picker, int y) {
		NonNullList<ItemStack> items = picker.items;
		// 槽位实际行与渲染行不一致时（原版刚按四舍五入重填过），
		// 预告行会与第 5 行重复；宁可暂时不画，等下一帧校正。
		if (SmoothScrolling.creativeMenuRow() != SmoothScrolling.creativeAppliedRow()) {
			return;
		}
		int base = (SmoothScrolling.creativeAppliedRow() + VISIBLE_ROWS) * COLUMNS;
		if (base < 0 || base >= items.size()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		for (int column = 0; column < COLUMNS; column++) {
			int index = base + column;
			if (index >= items.size()) {
				break;
			}
			int x = GRID_X + column * SLOT_SIZE;
			boolean expandedIcon = CreativeItemGroups.isExpandedIconSlot(index);
			boolean collapsedIcon = CreativeItemGroups.isCollapsedIconSlot(index);
			// 底纹与标记始终画（空槽位也一样），只有物品与数量要求非空，
			// 与真实行（extractSlot 先画底纹、末尾画标记）保持一致。
			// 这样只滚到半行时成员格也会变灰，而不是等整行滚完才出现。
			if (expandedIcon) {
				gui.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_SLOT_SPRITE,
					x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
			}
			if (CreativeItemGroups.isMemberSlot(index)) {
				gui.blitSprite(RenderPipelines.GUI_TEXTURED, ITEM_SLOT_SPRITE,
					x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
			}
			ItemStack stack = items.get(index);
			if (!stack.isEmpty()) {
				// 与真实行保持一致：展开的组图标同样按轮播物品绘制。
				ItemStack shown = this.tweakify$previewIconStack(index, stack);
				gui.item(shown, x, y);
				gui.itemDecorations(font, shown, x, y);
			}
			if (expandedIcon) {
				gui.blitSprite(RenderPipelines.GUI_TEXTURED, MINUS_SPRITE, x, y, MARKER_SIZE, MARKER_SIZE);
			} else if (collapsedIcon) {
				gui.blitSprite(RenderPipelines.GUI_TEXTURED, PLUS_SPRITE, x, y, MARKER_SIZE, MARKER_SIZE);
			}
		}
	}

	// 预告行的组图标要与真实行同源：只有“确实是组图标”时才替换成轮播物品，
	// 否则跨行的那一瞬间会看到图标跳变。
	@Unique
	private ItemStack tweakify$previewIconStack(int displayIndex, ItemStack stack) {
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(displayIndex);
		if (group == null || !ItemStack.isSameItem(stack, group.icon())) {
			return stack;
		}
		ItemStack rotating = CreativeItemGroups.rotatingIcon(group);
		return rotating == null ? stack : rotating;
	}

	// 物品格槽位的绘制裁到网格矩形，并按亚行偏移上移：上移出来的上半行不会画到标签栏上。
	// 槽位坐标本身保持原版（Slot.y 是 final），移动只发生在绘制与命中判定中。
	@WrapMethod(method = "extractSlot")
	private void tweakify$clipCreativeSlot(GuiGraphicsExtractor gui, Slot slot, int mouseX, int mouseY,
										   Operation<Void> original) {
		int shift = this.tweakify$gridShift(slot);
		if (shift <= 0) {
			original.call(gui, slot, mouseX, mouseY);
			return;
		}
		this.tweakify$pushGridClip(gui);
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(0.0f, -shift);
		try {
			// 鼠标坐标同步抬升，使槽位内部基于坐标的悬停判定与视觉位置一致。
			original.call(gui, slot, mouseX, mouseY + shift);
		} finally {
			pose.popMatrix();
			gui.disableScissor();
		}
	}

	// 悬停高亮：原版按 hoveredSlot 的坐标直接绘制，这里同样补上亚行偏移。
	@WrapMethod(method = "extractSlotHighlightBack")
	private void tweakify$shiftHighlightBack(GuiGraphicsExtractor gui, Operation<Void> original) {
		this.tweakify$drawShifted(gui, original);
	}

	@WrapMethod(method = "extractSlotHighlightFront")
	private void tweakify$shiftHighlightFront(GuiGraphicsExtractor gui, Operation<Void> original) {
		this.tweakify$drawShifted(gui, original);
	}

	@Unique
	private void tweakify$drawShifted(GuiGraphicsExtractor gui, Operation<Void> original) {
		int shift = this.tweakify$gridShift(this.hoveredSlot);
		if (shift <= 0) {
			original.call(gui);
			return;
		}
		// 原版高亮是 24x24（比槽位外扩 4 像素）：第 1 行固定放开这 4 像素，
		// 不随亚行偏移继续上移，避免滚到较深半行时压到面板顶部的标签标题；
		// 其余行仍按网格矩形裁剪。
		this.tweakify$pushClip(gui, SmoothScrolling.hoverHighlightClipTop(this.hoveredSlot.y));
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(0.0f, -shift);
		try {
			original.call(gui);
		} finally {
			pose.popMatrix();
			gui.disableScissor();
		}
	}

	// 点击/悬停判定：只有落在物品格内才把鼠标按亚行偏移抬回槽位坐标系，
	// 背包/快捷栏/盔甲槽用原始坐标（否则会错位到相邻槽位或点不到）。
	@WrapMethod(method = "getHoveredSlot")
	private Slot tweakify$clipCreativeHover(double mouseX, double mouseY, Operation<Slot> original) {
		if (!this.tweakify$isCreativeGridScreen() || !SmoothScrolling.creativeGridActive()
			|| !SmoothScrolling.insideCreativeGrid(mouseX, mouseY, this.leftPos, this.topPos)) {
			return original.call(mouseX, mouseY);
		}
		int shift = SmoothScrolling.creativeShiftPixels();
		Slot slot = original.call(mouseX, mouseY + shift);
		// 被裁剪掉的半行（视觉上在网格外）不能命中。
		return this.tweakify$isCreativeGridSlot(slot) ? slot : null;
	}

	// 该槽位当前应上移的像素：只对创造页物品格生效。
	@Unique
	private int tweakify$gridShift(Slot slot) {
		return this.tweakify$isCreativeGridSlot(slot) ? SmoothScrolling.creativeShiftPixels() : 0;
	}

	@Unique
	private boolean tweakify$isCreativeGridScreen() {
		return (Object) this instanceof CreativeModeInventoryScreen;
	}

	@Unique
	private boolean tweakify$isCreativeGridSlot(Slot slot) {
		return slot != null
			&& this.tweakify$isCreativeGridScreen()
			&& SmoothScrolling.creativeGridActive()
			&& CreativeItemGroups.isCreativeSlot(slot.index);
	}

	// 裁剪矩形用局部坐标：enableScissor 会按当前 pose（已平移到面板原点）变换到屏幕坐标。
	// 调用时必须在额外的平移之前推入，保证裁剪区与网格矩形一致。
	@Unique
	private void tweakify$pushGridClip(GuiGraphicsExtractor gui) {
		this.tweakify$pushClip(gui, GRID_Y);
	}

	// 以自定义上边界推入网格矩形裁剪：网格不包含的部分不会画到标签栏/面板外。
	@Unique
	private void tweakify$pushClip(GuiGraphicsExtractor gui, int top) {
		gui.enableScissor(GRID_X, top, GRID_X + GRID_WIDTH, GRID_Y + GRID_HEIGHT);
	}

	@Unique
	private CreativeModeInventoryScreen.ItemPickerMenu tweakify$picker() {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		return screen.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
	}
}
