package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CreativeItemGroupAccess;
import com.gtzc.tweakify.client.CreativeItemGroups;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把创造模式分类页里的同类物品折叠成一个条目。
 *
 * <p>右键组头展开/收起，组头右上角显示 +/-；搜索页、快捷栏和玩家背包保持
 * 原版。折叠只发生在客户端显示列表上，实际选取物品仍然走原版
 * {@code slotClicked} 与网络同步。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryGroupingMixin implements CreativeItemGroupAccess {
	@Unique
	private static final int TWEAKIFY$CREATIVE_SLOT_COUNT = 45;

	@Unique
	private static final Map<CreativeModeTab, Set<String>> TWEAKIFY$EXPANDED_GROUPS =
		new IdentityHashMap<>();

	@Shadow
	private static CreativeModeTab selectedTab;

	@Shadow
	private void refreshCurrentTabContents(Collection<ItemStack> items) {
		throw new AssertionError("Mixin shadow not applied");
	}

	@Unique
	private Map<Item, CreativeItemGroups.Group> tweakify$groups = Map.of();

	@Unique
	private String tweakify$groupingSignature = "";

	@Inject(method = "selectTab", at = @At("HEAD"))
	private void tweakify$clearGroupsBeforeTabChange(CreativeModeTab tab, CallbackInfo callback) {
		// 旧标签页的组映射不能带到新标签页；展开状态仍按标签页单独保留。
		this.tweakify$groups = Map.of();
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void tweakify$clearGroupStateOnClose(CallbackInfo callback) {
		this.tweakify$groups = Map.of();
		TWEAKIFY$EXPANDED_GROUPS.clear();
	}

	@Redirect(
		method = {"selectTab", "refreshCurrentTabContents"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/NonNullList;addAll(Ljava/util/Collection;)Z"
		)
	)
	private boolean tweakify$addGroupedItems(NonNullList<ItemStack> target,
			Collection<ItemStack> items) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		CreativeModeInventoryScreen.ItemPickerMenu picker =
			(CreativeModeInventoryScreen.ItemPickerMenu) screen.getMenu();
		// selectTab 还会对 menu.slots 调用 addAll(originalSlots)：不是物品列表时必须原样转发。
		if (target != picker.items) {
			return target.addAll(items);
		}
		if (!tweakify$isGroupingActive()) {
			this.tweakify$groups = Map.of();
			return target.addAll(items);
		}
		CreativeItemGroups.Result result = CreativeItemGroups.build(
			tweakify$selectedTabId(), items, tweakify$expandedGroups(), tweakify$sortMode());
		this.tweakify$groups = result.groupByItem();
		return target.addAll(result.displayItems());
	}

	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
	private void tweakify$toggleGroupOnClick(Slot slot, int slotId, int button,
			ContainerInput input, CallbackInfo callback) {
		if ((button != 0 && button != 1) || input != ContainerInput.PICKUP || slot == null
				|| slot.index >= TWEAKIFY$CREATIVE_SLOT_COUNT || !tweakify$isGroupingActive()) {
			return;
		}
		// 手上拿着物品时不拦截点击，避免把手中的物品清掉。
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (!screen.getMenu().getCarried().isEmpty()) {
			return;
		}
		CreativeItemGroups.Group group = this.tweakify$groupOf(slot.getItem());
		if (group == null || !CreativeItemGroups.isRepresentative(slot.getItem(), group)) {
			return;
		}
		String key = group.key();
		Set<String> expanded = tweakify$expandedGroups();
		if (!expanded.remove(key)) {
			expanded.add(key);
		}
		if (selectedTab != null) {
			this.refreshCurrentTabContents(selectedTab.getDisplayItems());
		}
		callback.cancel();
	}

	// 该方法可能被 Jade 等模组用 @WrapMethod 包装，此时 @Inject 的 CallbackInfo 不可取消，
	// 调用 setReturnValue 会抛 CancellationException；改用 MixinExtras 替换返回值。
	@ModifyReturnValue(method = "getTooltipFromContainerItem", at = @At("RETURN"))
	private List<Component> tweakify$appendGroupTooltip(List<Component> original, ItemStack stack) {
		if (original == null) {
			return null;
		}
		CreativeItemGroups.Group group = this.tweakify$groupOf(stack);
		if (group == null || !CreativeItemGroups.isRepresentative(stack, group)
				|| !tweakify$isGroupingActive()) {
			return original;
		}
		List<Component> tooltip = new ArrayList<>(original);
		tooltip.add(Component.translatable(
			tweakify$expandedGroups().contains(group.key())
				? "tweakify-mod.item_group.collapse"
				: "tweakify-mod.item_group.expand",
			group.icon().getHoverName(), group.items().size()));
		return tooltip;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tweakify$drawGroupMarker(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo callback) {
		if (!tweakify$isGroupingActive() || this.tweakify$groups.isEmpty()) {
			return;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		CreativeModeInventoryScreen.ItemPickerMenu picker =
			(CreativeModeInventoryScreen.ItemPickerMenu) screen.getMenu();
		int left = ((AbstractContainerScreenAccessor) (Object) this).tweakify$getLeftPos();
		int top = ((AbstractContainerScreenAccessor) (Object) this).tweakify$getTopPos();
		for (int index = 0; index < TWEAKIFY$CREATIVE_SLOT_COUNT
				&& index < picker.slots.size(); index++) {
			Slot slot = picker.slots.get(index);
			CreativeItemGroups.Group group = this.tweakify$groupOf(slot.getItem());
			if (group == null || !CreativeItemGroups.isRepresentative(slot.getItem(), group)) {
				continue;
			}
			String marker = tweakify$expandedGroups().contains(group.key()) ? "-" : "+";
			// 右下角对齐到 16x16 格子内部，带阴影的 1 像素也不能越出边框。
			graphics.text(screen.getFont(), marker, left + slot.x + 9,
				top + slot.y + 7, 0xFFFFFFFF, true);
		}
	}

	// 黑色槽位背景由 CreativeModeInventorySlotBackgroundMixin 在物品绘制前填充。
	@Override
	public boolean tweakify$isExpandedGroupMember(Slot slot) {
		if (slot == null) {
			return false;
		}
		CreativeItemGroups.Group group = this.tweakify$groupOf(slot.getItem());
		return CreativeItemGroups.shouldDrawExpandedBackground(slot.index, TWEAKIFY$CREATIVE_SLOT_COUNT,
			tweakify$isGroupingActive(), group,
			group != null && tweakify$expandedGroups().contains(group.key()),
			CreativeItemGroups.isRepresentative(slot.getItem(), group));
	}

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void tweakify$refreshOnConfigChange(CallbackInfo callback) {
		String signature = tweakify$isGroupingActive() + "|" + tweakify$sortMode();
		if (!signature.equals(this.tweakify$groupingSignature)) {
			this.tweakify$groupingSignature = signature;
			if (selectedTab != null) {
				this.refreshCurrentTabContents(selectedTab.getDisplayItems());
			}
		}
	}

	@Unique
	private boolean tweakify$isGroupingActive() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.itemGroups
			&& selectedTab != null
			&& selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
	}

	@Unique
	private String tweakify$sortMode() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null ? CreativeItemGroups.SORT_DEFAULT : config.itemGroupsSort;
	}

	@Unique
	private static String tweakify$selectedTabId() {
		if (selectedTab == null) {
			return "";
		}
		Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(selectedTab);
		return id == null ? "" : id.toString();
	}

	@Unique
	private Set<String> tweakify$expandedGroups() {
		if (selectedTab == null) {
			return Set.of();
		}
		return TWEAKIFY$EXPANDED_GROUPS.computeIfAbsent(selectedTab, tab -> new HashSet<>());
	}

	@Unique
	private CreativeItemGroups.Group tweakify$groupOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		return this.tweakify$groups.get(stack.getItem());
	}
}
