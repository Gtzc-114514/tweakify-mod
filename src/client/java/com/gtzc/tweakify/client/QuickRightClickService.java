package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.CreativeModeInventoryScreenSlotWrapperAccessor;
import com.gtzc.tweakify.config.FunctionConfig;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 背包快捷右键（服务端侧）：把背包里的功能方块物品、潜影盒或末影箱直接打开。
 *
 * <p>客户端只发送槽位下标；这里以服务端自己的菜单数据为准，校验槽位属于玩家背包、
 * 光标为空、物品仍在原槽位后才打开对应菜单。潜影盒使用临时容器，关闭界面时把内容写回原物品。
 * 仅单人/局域网主机（安装本模组的一端）有效。
 */
public final class QuickRightClickService {
	// 潜影盒固定 27 格。
	private static final int SHULKER_SLOTS = 27;

	private QuickRightClickService() {
	}

	// 客户端判定并发送请求：普通容器界面与创造模式物品栏共用。
	public static boolean tryOpenFromSlot(Slot slot, int mouseButton, ContainerInput type,
			AbstractContainerMenu menu, boolean sneaking) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.inventoryQuickRightClick) {
			return false;
		}
		if (mouseButton != 1) {
			return false;
		}
		boolean requireSneak = config.functions.quickRightClickRequireSneak;
		// 普通右键是 PICKUP；需要潜行时原版会把 Shift+右键变成 QUICK_MOVE。
		if (type != ContainerInput.PICKUP && !(requireSneak && type == ContainerInput.QUICK_MOVE)) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || (requireSneak && !sneaking) || client.player.isSpectator()) {
			return false;
		}
		// 光标必须为空：光标拿着物品时右键是在放置/交换。
		if (!menu.getCarried().isEmpty()) {
			return false;
		}
		Slot target = unwrapCreativeSlot(slot);
		if (target == null || target.container != client.player.getInventory()) {
			return false;
		}
		if (!isSupported(target.getItem())) {
			return false;
		}
		// 远程服务器没有安装本模组时不发请求，也不取消原版点击。
		if (!ClientPlayNetworking.canSend(QuickRightClickPayload.TYPE)) {
			return false;
		}
		ClientPlayNetworking.send(new QuickRightClickPayload(menu.containerId, target.index, sneaking));
		return true;
	}

	// 创造物品栏的背包槽是 SlotWrapper，下标是创造菜单下标；必须改用真实槽位下标。
	private static Slot unwrapCreativeSlot(Slot slot) {
		if (slot instanceof CreativeModeInventoryScreenSlotWrapperAccessor wrapper) {
			return wrapper.tweakify$target();
		}
		return slot;
	}

	// 客户端判定：避免对不支持的物品发送无效请求。
	public static boolean isSupported(ItemStack stack) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.inventoryQuickRightClick || stack.isEmpty()) {
			return false;
		}
		FunctionConfig functions = config.functions;
		if (functions.quickRightClickShulkerBoxes && isShulkerBox(stack)) {
			return true;
		}
		if (functions.quickRightClickEnderChest && stack.is(Items.ENDER_CHEST)) {
			return true;
		}
		return functions.quickRightClickFunctionalBlocks && FunctionalMenu.of(stack) != null;
	}

	// 服务端入口：由 QuickRightClickPayload 的接收器在服务端线程调用。
	// sneaking 是客户端点击瞬间的状态：包到达前松开 Shift 时仍按点击时判定。
	public static void open(ServerPlayer player, int containerId, int slotIndex, boolean sneaking) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.inventoryQuickRightClick || player.isSpectator()) {
			return;
		}
		FunctionConfig functions = config.functions;
		if (functions.quickRightClickRequireSneak && !sneaking && !player.isShiftKeyDown()) {
			return;
		}
		AbstractContainerMenu menu = player.containerMenu;
		if (menu == null || menu.containerId != containerId || !menu.isValidSlotIndex(slotIndex)) {
			return;
		}
		Slot slot = menu.getSlot(slotIndex);
		// 只处理玩家自己背包里的槽位：避免把容器/合成结果当成“背包物品”。
		if (slot.container != player.getInventory()) {
			return;
		}
		if (!slot.isActive() || !slot.mayPickup(player) || !menu.getCarried().isEmpty()) {
			return;
		}
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) {
			return;
		}
		ResolvedTarget resolved = resolveTarget(stack,
			functions.quickRightClickShulkerBoxes,
			functions.quickRightClickEnderChest,
			functions.quickRightClickFunctionalBlocks);
		Component title = stack.getHoverName();

		// 单一出口：applyResolvedTarget 内部固定“先关旧菜单、再开新菜单”，
		// NONE 时两步都不执行，避免判定失败后留下失效界面。
		applyResolvedTarget(resolved,
			() -> {
				// 先无包关闭旧菜单：openMenu 内部的 closeContainer 会发送 CloseScreen，
				// 客户端会短暂回到游戏并 grabMouse，随后 OpenScreen 的 releaseMouse 会把光标居中。
				// 手动 doCloseContainer 只做服务端收尾（removed/状态转移），不发包，
				// 客户端会直接用 OpenScreen 替换当前界面；openMenu 看到已是 inventoryMenu 也不会再发包。
				if (player.containerMenu != player.inventoryMenu) {
					player.doCloseContainer();
				}
			},
			target -> openResolvedTarget(player, stack, title, target));
	}

	// 单一出口：先判定结果，再关闭旧菜单，最后打开新菜单；
	// NONE 直接返回，不触碰当前界面。测试可直接记录 close/open 的调用顺序。
	static boolean applyResolvedTarget(ResolvedTarget resolved, Runnable closeMenu,
			Consumer<ResolvedTarget> openMenu) {
		if (resolved.target() == QuickOpenTarget.NONE) {
			return false;
		}
		closeMenu.run();
		openMenu.accept(resolved);
		return true;
	}

	private static void openResolvedTarget(ServerPlayer player, ItemStack stack, Component title,
			ResolvedTarget resolved) {
		switch (resolved.target()) {
			case SHULKER -> openShulkerBox(player, stack, title);
			case ENDER_CHEST -> openEnderChest(player, title);
			case FUNCTIONAL -> openFunctionalMenu(player, resolved.menu(), title);
			case NONE -> {
			}
		}
	}

	// 服务端分派判定。必须在关闭旧菜单之前完成，NONE 表示这次请求不应改变当前界面。
	// 功能方块类型随结果一起返回，避免判定与实际打开之间再次解析。
	static ResolvedTarget resolveTarget(ItemStack stack, boolean shulkerEnabled,
			boolean enderEnabled, boolean functionalEnabled) {
		if (stack.isEmpty()) {
			return new ResolvedTarget(QuickOpenTarget.NONE, null);
		}
		if (shulkerEnabled && isShulkerBox(stack)) {
			return new ResolvedTarget(QuickOpenTarget.SHULKER, null);
		}
		if (enderEnabled && stack.is(Items.ENDER_CHEST)) {
			return new ResolvedTarget(QuickOpenTarget.ENDER_CHEST, null);
		}
		if (functionalEnabled) {
			FunctionalMenu menu = FunctionalMenu.of(stack);
			if (menu != null) {
				return new ResolvedTarget(QuickOpenTarget.FUNCTIONAL, menu);
			}
		}
		return new ResolvedTarget(QuickOpenTarget.NONE, null);
	}

	record ResolvedTarget(QuickOpenTarget target, FunctionalMenu menu) {
	}

	enum QuickOpenTarget {
		SHULKER,
		ENDER_CHEST,
		FUNCTIONAL,
		NONE
	}

	private static boolean isShulkerBox(ItemStack stack) {
		Item item = stack.getItem();
		if (!(item instanceof BlockItem blockItem)) {
			return false;
		}
		Block block = blockItem.getBlock();
		return block instanceof ShulkerBoxBlock;
	}

	// 潜影盒：内容复制到临时容器并接管所有权（原物品立即变空盒）。
	// 读取必须放在 provider 内：openMenu 会先关闭旧菜单（可能正好是同一个盒子的便携界面），
	// 等旧菜单写回后再读，避免读到写回前的旧内容而复制物品。
	// 打开期间物品本身为空盒，因此会话中丢弃/清空物品也不会让掉落物带着第二份内容。
	private static void openShulkerBox(ServerPlayer player, ItemStack stack, Component title) {
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
			SimpleContainer container = loadShulkerContents(stack);
			stack.remove(DataComponents.CONTAINER);
			return new PortableShulkerMenu(id, inventory, container, stack);
		}, title));
	}

	private static SimpleContainer loadShulkerContents(ItemStack stack) {
		SimpleContainer container = new SimpleContainer(SHULKER_SLOTS);
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents != null) {
			NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
			contents.copyInto(items);
			for (int i = 0; i < SHULKER_SLOTS; i++) {
				container.setItem(i, items.get(i));
			}
		}
		return container;
	}

	// 关闭界面时把潜影盒内容写回原物品；空盒移除组件，保持与原版空潜影盒一致。
	static void writeBackShulkerBox(ItemStack source, Container container) {
		List<ItemStack> items = new ArrayList<>(SHULKER_SLOTS);
		boolean empty = true;
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			items.add(stack);
			if (!stack.isEmpty()) {
				empty = false;
			}
		}
		if (empty) {
			source.remove(DataComponents.CONTAINER);
		} else {
			source.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		}
	}

	private static void openEnderChest(ServerPlayer player, Component title) {
		player.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
			title));
	}

	private static void openFunctionalMenu(ServerPlayer player, FunctionalMenu type, Component title) {
		if (type == null) {
			return;
		}
		// 工作台/铁砧等只需要可用的容器级别访问：NULL 的校验恒为通过，
		// 与原版的“物品自带界面”一致，不依赖世界中的方块。
		ContainerLevelAccess access = ContainerLevelAccess.NULL;
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> switch (type) {
			case CRAFTING -> new CraftingMenu(id, inventory, access);
			case ANVIL -> new AnvilMenu(id, inventory, access);
			case ENCHANTING -> new PortableEnchantmentMenu(id, inventory,
				ContainerLevelAccess.create(player.level(), player.blockPosition()));
			case SMITHING -> new SmithingMenu(id, inventory, access);
			case LOOM -> new LoomMenu(id, inventory, access);
			case CARTOGRAPHY -> new CartographyTableMenu(id, inventory, access);
			case STONECUTTER -> new StonecutterMenu(id, inventory, access);
			case GRINDSTONE -> new GrindstoneMenu(id, inventory, access);
		}, title));
	}

	// 可打开的功能方块：不依赖世界中方块实体数据的无状态界面。
	enum FunctionalMenu {
		CRAFTING,
		ANVIL,
		ENCHANTING,
		SMITHING,
		LOOM,
		CARTOGRAPHY,
		STONECUTTER,
		GRINDSTONE;

		static FunctionalMenu of(ItemStack stack) {
			if (stack.is(Items.CRAFTING_TABLE)) {
				return CRAFTING;
			}
			if (stack.is(Items.ANVIL) || stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL)) {
				return ANVIL;
			}
			if (stack.is(Items.ENCHANTING_TABLE)) {
				return ENCHANTING;
			}
			if (stack.is(Items.SMITHING_TABLE)) {
				return SMITHING;
			}
			if (stack.is(Items.LOOM)) {
				return LOOM;
			}
			if (stack.is(Items.CARTOGRAPHY_TABLE)) {
				return CARTOGRAPHY;
			}
			if (stack.is(Items.STONECUTTER)) {
				return STONECUTTER;
			}
			if (stack.is(Items.GRINDSTONE)) {
				return GRINDSTONE;
			}
			return null;
		}
	}

	// 潜影盒菜单：关闭时写回物品内容；若物品已不在玩家身上（会话中被丢弃/替换），
	// 把临时容器里剩下的物品掉给玩家，避免静默丢失。
	private static final class PortableShulkerMenu extends ShulkerBoxMenu {
		private final SimpleContainer portableContainer;
		private final ItemStack source;
		private boolean finished;

		private PortableShulkerMenu(int syncId, Inventory playerInventory, SimpleContainer container,
				ItemStack source) {
			super(syncId, playerInventory, container);
			this.portableContainer = container;
			this.source = source;
		}

		@Override
		public void removed(Player player) {
			super.removed(player);
			if (this.finished) {
				return;
			}
			this.finished = true;
			if (isHeldByPlayer(player, this.source)) {
				writeBackShulkerBox(this.source, this.portableContainer);
				return;
			}
			for (ItemStack remainder : this.portableContainer.removeAllItems()) {
				if (!remainder.isEmpty()) {
					player.drop(remainder, false, Prediction.SERVER_ONLY);
				}
			}
		}
	}

	// 用实例身份而不是物品相等性判断：同一格里的新物品不能接管旧物品的内容。
	// 数量为 0 的栈视为已脱离玩家（丢弃时 split 会把原栈数量减到 0），交给掉落兜底。
	private static boolean isHeldByPlayer(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i) == stack) {
				return true;
			}
		}
		AbstractContainerMenu menu = player.containerMenu;
		return menu != null && menu.getCarried() == stack;
	}

	// 附魔台菜单：附魔等级按玩家周围的书架计算，界面有效性不再要求世界里有附魔台方块。
	private static final class PortableEnchantmentMenu extends EnchantmentMenu {
		private PortableEnchantmentMenu(int syncId, Inventory playerInventory,
				ContainerLevelAccess access) {
			super(syncId, playerInventory, access);
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}
	}
}
