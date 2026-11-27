package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.CreativeItemGroups;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 物品分组的槽位绘制（行为与贴图对齐 {@code BizCub/inventory-item-groups}）：
 *
 * <ul>
 *   <li>展开的组：组图标槽位画 {@code icon_slot}，成员槽位画 {@code item_slot}（18x18，向左上外扩 1 像素）；</li>
 *   <li>组图标槽位右上角画 {@code plus} / {@code minus}（16x16）表示当前可展开 / 可收起；</li>
 *   <li>开启“显示组内物品”后，图标槽位每 1.5 秒轮播一个组内物品；</li>
 *   <li>悬停在组图标上时，工具提示只有组名，不显示图标物品的信息。</li>
 * </ul>
 *
 * <p>性能：每个回调只做“槽位 → 显示索引”（有缓存）+ 一次数组查表，
 * 不再逐槽遍历组与成员；贴图标识也提升为静态常量，避免每帧构造。
 */
@Mixin(AbstractContainerScreen.class)
public class CreativeModeInventorySlotRenderMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$ITEM_GROUP_SLOT_PROBE = new AtomicBoolean();

	@Unique
	private static final AtomicBoolean TWEAKIFY$ITEM_GROUP_ICON_PROBE = new AtomicBoolean();

	private static final int ICON_SLOT_SIZE = 18;
	private static final int MARKER_SIZE = 16;

	private static final Identifier ICON_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/icon_slot");
	private static final Identifier ITEM_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/item_slot");
	private static final Identifier PLUS_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/plus");
	private static final Identifier MINUS_SPRITE =
		Identifier.fromNamespaceAndPath(TweakifyMod.MOD_ID, "container/minus");

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Shadow
	protected Slot hoveredSlot;

	// 组底板：图标槽位与展开的成员槽位各用一张 18x18 精灵，向左上各扩 1 像素盖住原版边框。
	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void tweakify$renderGroupBackgrounds(GuiGraphicsExtractor graphics, Slot slot,
												 int mouseX, int mouseY, CallbackInfo ci) {
		if (!tweakify$creativeScreen() || !CreativeItemGroups.isCreativeSlot(slot.index)) {
			return;
		}
		int index = this.tweakify$displayIndex(slot);
		if (CreativeItemGroups.isExpandedIconSlot(index)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_SLOT_SPRITE,
				slot.x - 1, slot.y - 1, ICON_SLOT_SIZE, ICON_SLOT_SIZE);
		}
		if (CreativeItemGroups.isMemberSlot(index)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ITEM_SLOT_SPRITE,
				slot.x - 1, slot.y - 1, ICON_SLOT_SIZE, ICON_SLOT_SIZE);
		}
	}

	// 展开/收起标记：画在图标槽位右下角，压在轮播图标之上。
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void tweakify$renderExpandMarker(GuiGraphicsExtractor graphics, Slot slot,
											 int mouseX, int mouseY, CallbackInfo ci) {
		if (!tweakify$creativeScreen() || !CreativeItemGroups.isCreativeSlot(slot.index)) {
			return;
		}
		int index = this.tweakify$displayIndex(slot);
		if (CreativeItemGroups.isExpandedIconSlot(index)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MINUS_SPRITE,
				slot.x, slot.y, MARKER_SIZE, MARKER_SIZE);
		} else if (CreativeItemGroups.isCollapsedIconSlot(index)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PLUS_SPRITE,
				slot.x, slot.y, MARKER_SIZE, MARKER_SIZE);
		}
	}

	// 图标轮播：只在“组图标槽位 + 该槽位确实显示组图标”时替换成组内物品。
	@WrapOperation(method = "extractSlot", require = 0,
		at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack tweakify$rotateGroupIcon(Slot slot, Operation<ItemStack> original) {
		HookProbe.markOnce(TWEAKIFY$ITEM_GROUP_ICON_PROBE, "itemGroupIcon");
		ItemStack real = original.call(slot);
		if (!tweakify$creativeScreen() || !CreativeItemGroups.isCreativeSlot(slot.index)) {
			return real;
		}
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(
			this.tweakify$displayIndex(slot));
		if (group == null || !ItemStack.isSameItem(real, group.icon())) {
			return real;
		}
		ItemStack rotating = CreativeItemGroups.rotatingIcon(group);
		return rotating == null ? real : rotating;
	}

	// 悬停在组图标上时只显示组名（参考实现同样完全替换提示内容）。
	@WrapOperation(method = "extractTooltip", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;"
				+ "getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
	private List<Component> tweakify$groupTooltip(AbstractContainerScreen<?> screen, ItemStack stack,
												  Operation<List<Component>> original) {
		HookProbe.markOnce(TWEAKIFY$ITEM_GROUP_SLOT_PROBE, "itemGroupTooltip");
		if (!tweakify$creativeScreen() || this.hoveredSlot == null
			|| !CreativeItemGroups.isCreativeSlot(this.hoveredSlot.index)) {
			return original.call(screen, stack);
		}
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(
			this.tweakify$displayIndex(this.hoveredSlot));
		if (group == null) {
			return original.call(screen, stack);
		}
		return List.of(group.name());
	}

	// 显示索引：平滑滚动开启时用精确值（menuRow*9 + 槽位号），否则回退到原版启发式。
	// 精确值跟预告行同源，避免槽位与预告行对同一格子给出不同判定而闪烁。
	@Unique
	private int tweakify$displayIndex(Slot slot) {
		int exact = SmoothScrolling.creativeDisplayIndex(slot.index);
		return exact >= 0 ? exact : CreativeItemGroups.displayIndex(this.menu.slots, slot.index);
	}

	// 分组只作用于创造模式物品页：其他容器界面即使残留状态也不会被绘制。
	@Unique
	private boolean tweakify$creativeScreen() {
		return (Object) this instanceof CreativeModeInventoryScreen;
	}
}
