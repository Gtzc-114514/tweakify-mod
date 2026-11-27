package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.CreativeItemGroups;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 创造模式物品分组（行为对齐 {@code BizCub/inventory-item-groups}）。
 *
 * <p>与原版列表的衔接方式也照搬参考实现：切页时先按当前页的组定义重建，
 * 再把除图标以外的成员从展示列表里摘掉；点击图标时把成员插回图标之后 / 从同一位置移除，
 * 并用“基准列表 + 偏移”重新计算每个槽位对应的索引。
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryGroupingMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$ITEM_GROUPS_PROBE = new AtomicBoolean();

	// 上次刷新时的配置签名：配置可热切换，变化时重建当前页。
	@Unique
	private String tweakify$lastConfigSignature = "";

	@Shadow
	private static CreativeModeTab selectedTab;

	@Shadow
	private float scrollOffs;

	@Shadow
	private void refreshCurrentTabContents(Collection<ItemStack> items) {
		throw new AssertionError("Mixin shadow not applied");
	}

	@Inject(method = "selectTab", at = @At("HEAD"))
	private void tweakify$beginTab(CreativeModeTab tab, CallbackInfo ci) {
		CreativeItemGroups.beginTab(tab);
		this.tweakify$lastConfigSignature = CreativeItemGroups.configSignature(TweakifyMod.config());
	}

	@Redirect(
		method = {"selectTab", "refreshCurrentTabContents"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/NonNullList;addAll(Ljava/util/Collection;)Z"
		)
	)
	private boolean tweakify$addGroupedItems(NonNullList<ItemStack> target, Collection<ItemStack> items) {
		CreativeModeInventoryScreen.ItemPickerMenu picker = tweakify$picker();
		// selectTab 还会对 menu.slots 调用 addAll：不是物品列表时必须原样转发。
		if (picker == null || target != picker.items) {
			return target.addAll(items);
		}
		return target.addAll(CreativeItemGroups.stripMembers(items));
	}

	// 点击组图标：展开/收起，而不是把组图标拿在手上。
	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$toggleGroupOnClick(Slot slot, int slotId, int button, ContainerInput input,
											 CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$ITEM_GROUPS_PROBE, "itemGroups");
		if (slot == null || input != ContainerInput.PICKUP || (button != 0 && button != 1)) {
			return;
		}
		if (!CreativeItemGroups.isCreativeSlot(slot.index)) {
			return;
		}
		CreativeModeInventoryScreen.ItemPickerMenu picker = tweakify$picker();
		if (picker == null) {
			return;
		}
		// iconGroup 只在“显示索引正好是某个组的图标”时返回非空，已包含原版 selectedTab 判定。
		int exactIndex = SmoothScrolling.creativeDisplayIndex(slot.index);
		int displayIndex = exactIndex >= 0
			? exactIndex
			: CreativeItemGroups.displayIndex(picker.slots, slot.index);
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(displayIndex);
		if (group == null) {
			return;
		}
		// 与参考实现一致：点击图标时不拿起物品（光标里的物品同时被清空）。
		picker.setCarried(ItemStack.EMPTY);
		// 平滑滚动时顶部可能停在半行：恢复整行的同时保留亚行偏移，否则整片物品格会跳一下。
		double fraction = SmoothScrolling.creativeEnabled()
			? this.tweakify$subRowFraction(picker.items.size())
			: 0.0;
		int exactBase = SmoothScrolling.creativeDisplayIndex(0);
		int topRow = exactBase >= 0
			? exactBase / 9
			: Math.max(CreativeItemGroups.displayIndex(picker.slots, 0), 0) / 9;
		CreativeItemGroups.toggle(group, picker.items);
		int newRows = Math.max(0, Mth.positiveCeilDiv(picker.items.size(), 9) - 5);
		float restored = SmoothScrolling.restoredScrollPosition(topRow, fraction, newRows);
		picker.scrollTo(restored);
		// 同步界面层的滚动比例：平滑滚动 mixin 靠它识别“位置已被外部恢复”，
		// 否则下一帧会把刚恢复的位置当成过期状态又动画回去。
		this.scrollOffs = restored;
		ci.cancel();
	}

	// 当前顶部位置的亚行偏移（单位：行）：0 表示正好停在整行。
	@Unique
	private double tweakify$subRowFraction(int itemCount) {
		int rows = Math.max(0, Mth.positiveCeilDiv(itemCount, 9) - 5);
		if (rows <= 0 || !Float.isFinite(this.scrollOffs)) {
			return 0.0;
		}
		double position = (double) this.scrollOffs * rows;
		return Math.max(0.0, Math.min(0.999, position - Math.floor(position)));
	}

	// 关闭界面时丢弃分组状态：下次打开会按当前标签页重建。
	@Inject(method = "removed", at = @At("TAIL"))
	private void tweakify$clearGroupStateOnClose(CallbackInfo ci) {
		CreativeItemGroups.clear();
	}

	// 配置变化（总开关/排序/默认组/自定义组）时重建当前页，避免必须重开界面。
	// 决策（分类页重建 / 非分类页只告警）与空值判断已下沉到
	// CreativeItemGroups.shouldRefreshAfterConfigChange，mixin 只负责签名比对、转发与按返回值刷新。
	@Inject(method = "containerTick", at = @At("TAIL"))
	private void tweakify$refreshOnConfigChange(CallbackInfo ci) {
		String signature = CreativeItemGroups.configSignature(TweakifyMod.config());
		if (signature.equals(this.tweakify$lastConfigSignature)) {
			return;
		}
		this.tweakify$lastConfigSignature = signature;
		if (CreativeItemGroups.shouldRefreshAfterConfigChange(selectedTab)) {
			this.refreshCurrentTabContents(selectedTab.getDisplayItems());
		}
	}

	@Unique
	private CreativeModeInventoryScreen.ItemPickerMenu tweakify$picker() {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		return screen.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
	}
}
