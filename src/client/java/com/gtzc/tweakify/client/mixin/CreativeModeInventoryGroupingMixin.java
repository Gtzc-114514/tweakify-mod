package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CreativeItemGroups;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
	private void refreshCurrentTabContents(Collection<ItemStack> items) {
		throw new AssertionError("Mixin shadow not applied");
	}

	@Inject(method = "selectTab", at = @At("HEAD"))
	private void tweakify$beginTab(CreativeModeTab tab, CallbackInfo ci) {
		CreativeItemGroups.beginTab(tab);
		this.tweakify$lastConfigSignature = CreativeItemGroups.configSignature(TweakifyMod.CONFIG);
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
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(
			CreativeItemGroups.displayIndex(picker.slots, slot.index));
		if (group == null) {
			return;
		}
		// 与参考实现一致：点击图标时不拿起物品（光标里的物品同时被清空）。
		picker.setCarried(ItemStack.EMPTY);
		int topRow = Math.max(CreativeItemGroups.displayIndex(picker.slots, 0), 0) / 9;
		CreativeItemGroups.toggle(group, picker.items);
		if (picker instanceof CreativeItemPickerMenuAccessor accessor) {
			picker.scrollTo(accessor.tweakify$getScrollForRowIndex(topRow));
		}
		ci.cancel();
	}

	// 关闭界面时丢弃分组状态：下次打开会按当前标签页重建。
	@Inject(method = "removed", at = @At("TAIL"))
	private void tweakify$clearGroupStateOnClose(CallbackInfo ci) {
		CreativeItemGroups.clear();
	}

	// 配置变化（总开关/排序/默认组/自定义组）时重建当前页，避免必须重开界面。
	// 只重建分类页：快捷栏/背包页的物品来自原版其他数据源，搜索页结果由搜索框维护，
	// 用 getDisplayItems 重填会清空它们的预览内容。类型判断必须在 beginTab 之前：
	// 非分类页既不需要重建，也不应清空分组状态（进入该页时已经清空）。
	@Inject(method = "containerTick", at = @At("TAIL"))
	private void tweakify$refreshOnConfigChange(CallbackInfo ci) {
		String signature = CreativeItemGroups.configSignature(TweakifyMod.CONFIG);
		if (signature.equals(this.tweakify$lastConfigSignature)) {
			return;
		}
		this.tweakify$lastConfigSignature = signature;
		if (selectedTab == null) {
			return;
		}
		if (!CreativeItemGroups.needsContentRefreshOnConfigChange(selectedTab.getType())) {
			// 非分类页不重建列表，但仍要检查自定义组配置：
			// 搜索/背包页引用了非分类页标签页时也要给出一次反馈，而不是静默无效。
			CreativeItemGroups.warnUnsupportedCustomGroups();
			return;
		}
		CreativeItemGroups.beginTab(selectedTab);
		this.refreshCurrentTabContents(selectedTab.getDisplayItems());
	}

	@Unique
	private CreativeModeInventoryScreen.ItemPickerMenu tweakify$picker() {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		return screen.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
	}
}
