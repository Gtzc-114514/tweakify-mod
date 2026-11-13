package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 防猪灵装备切换：附近有存活的猪灵时，把背包里的金制装备换到身上；
 * 猪灵离开、功能关闭或切换模式不再覆盖该部位时换回原装备。
 *
 * <p>原版只要身上有任意一件金制护甲猪灵就不会攻击，因此自动模式在"身上已有金制护甲"
 * （玩家自己穿的或本模组换上的）时不会再换任何部位；否则只从背包里选一件换上
 * （有哪个用哪个），而不是把所有金装备都穿上。
 *
 * <p>所有背包操作都走原版容器点击流程（PICKUP 点击序列）发送给服务器，
 * 因此单人/局域网与多人服务器都可用。每次替换都保存背包槽位与两件物品的快照；
 * 玩家手动改动过对应槽位后记录被清理并抑制到猪灵离开，不会再与玩家对抗。
 */
public final class PiglinArmorSwapState {
	// 猪灵探测半径（格）：与原版猪灵发现玩家的距离一致。
	static final double DETECTION_RANGE = 16.0;
	// 实体扫描间隔与每次装备操作的冷却，避免每 tick 遍历实体或连续发包。
	static final int SCAN_INTERVAL_TICKS = 5;
	static final int ACTION_COOLDOWN_TICKS = 4;
	// 猪灵连续多次扫描都不在才认定为离开，避免边缘徘徊时反复撤销玩家抑制。
	static final int MISSED_SCANS_TO_LEAVE = 2;
	// 重定位原装备连续失败多少 tick 后才放弃记录（约 3 秒）：
	// 玩家临时把原装备放进合成格/副手/箱子或丢地时不应立刻放弃恢复。
	static final int RELOCATE_GIVE_UP_TICKS = 60;

	// 需要保护的四件护甲，按背包界面从上到下处理（自动模式按此顺序选第一件）。
	static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	// 替换记录：背包槽位（玩家背包菜单）、换下来的原装备、换上去的金装备，
	// 以及首次重定位失败的游戏 tick（-1 表示当前没有失效）。
	private record SwapRecord(int menuSlot, ItemStack original, ItemStack gold, int firstMissTick) {
	}

	private static final Map<EquipmentSlot, SwapRecord> SWAPPED = new EnumMap<>(EquipmentSlot.class);
	// 玩家手动改动过的部位：本轮猪灵在场期间不再自动替换，猪灵离开后恢复。
	private static final Set<EquipmentSlot> SUPPRESSED = EnumSet.noneOf(EquipmentSlot.class);

	private static ClientLevel trackedLevel;
	private static LocalPlayer trackedPlayer;
	private static boolean piglinNearby;
	private static int scanCooldown;
	private static int actionCooldown;
	private static int missedScans;

	private PiglinArmorSwapState() {
	}

	static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			resetTracking();
			return;
		}
		if (client.level != trackedLevel || player != trackedPlayer) {
			// 切换世界或重新进入时，旧的槽位记录与抑制标记已经失效。
			SWAPPED.clear();
			SUPPRESSED.clear();
			trackedLevel = client.level;
			trackedPlayer = player;
			piglinNearby = false;
			scanCooldown = 0;
			actionCooldown = 0;
			missedScans = 0;
		}

		boolean enabled = config != null && config.functions.piglinGoldArmorSwap;
		if (!enabled && SWAPPED.isEmpty()) {
			SUPPRESSED.clear();
			return;
		}
		if (client.gameMode == null) {
			return;
		}
		// 打开其他容器时槽位编号不属于玩家背包，等关闭后再操作。
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		// 光标上拿着物品时点击会互相干扰，等玩家放下再操作。
		if (!player.containerMenu.getCarried().isEmpty()) {
			return;
		}
		if (actionCooldown > 0) {
			actionCooldown--;
			return;
		}

		GameType gameMode = client.gameMode.getPlayerMode();
		if (gameMode == GameType.SPECTATOR || !player.isAlive() || player.isDeadOrDying()) {
			// 旁观/死亡/濒死时服务端会丢弃点击并全量重发，保留记录等回到可操作状态。
			return;
		}

		pruneInvalidRecords(player, player.containerMenu);

		if (!enabled) {
			// 功能关闭后仍穿着替换过的金装备：按记录换回原装备。
			if (tryRestoreOne(client, player, null)) {
				actionCooldown = ACTION_COOLDOWN_TICKS;
			}
			return;
		}

		if (--scanCooldown <= 0) {
			scanCooldown = SCAN_INTERVAL_TICKS;
			if (hasPiglinsNearby(client.level, player)) {
				piglinNearby = true;
				missedScans = 0;
			} else if (++missedScans >= MISSED_SCANS_TO_LEAVE) {
				if (piglinNearby) {
					// 猪灵已离开：解除玩家抑制，下一次猪灵靠近时重新按规则替换。
					SUPPRESSED.clear();
				}
				piglinNearby = false;
			}
		}

		PiglinArmorSwapMode mode = PiglinArmorSwapMode.fromCode(config.functions.piglinGoldArmorSwapMode);
		boolean survivalLike = gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE;
		if (survivalLike && piglinNearby) {
			if (tryEquipOne(client, player, mode)) {
				actionCooldown = ACTION_COOLDOWN_TICKS;
			} else if (tryRestoreOne(client, player, mode)) {
				// 切换模式后不再覆盖的部位换回原装备。
				actionCooldown = ACTION_COOLDOWN_TICKS;
			}
		} else if (tryRestoreOne(client, player, null)) {
			actionCooldown = ACTION_COOLDOWN_TICKS;
		}
	}

	// 附近是否存在存活的成年猪灵（幼崽不会攻击玩家）。
	static boolean hasPiglinsNearby(ClientLevel level, Player player) {
		AABB area = player.getBoundingBox().inflate(DETECTION_RANGE);
		for (Piglin piglin : level.getEntitiesOfClass(Piglin.class, area, Piglin::isAlive)) {
			if (!piglin.isBaby()) {
				return true;
			}
		}
		return false;
	}

	// 身上是否已有任意一件金制护甲（玩家自己穿的或本模组换上的）。
	// 原版猪灵只要看到任意一件金制护甲就会中立，因此有它就不再替换。
	static boolean hasAnyGoldenArmor(Player player) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (isGoldenArmorForSlot(player.getItemBySlot(slot), slot)) {
				return true;
			}
		}
		return false;
	}

	// 该物品是否是指定护甲部位的金制装备。
	static boolean isGoldenArmorForSlot(ItemStack stack, EquipmentSlot slot) {
		return !stack.isEmpty() && stack.getItem() == goldItemFor(slot);
	}

	// 护甲部位在玩家背包菜单里的槽位：头盔 5、胸甲 6、护腿 7、靴子 8。
	static int armorMenuSlot(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> InventoryMenu.ARMOR_SLOT_START;
			case CHEST -> InventoryMenu.ARMOR_SLOT_START + 1;
			case LEGS -> InventoryMenu.ARMOR_SLOT_START + 2;
			case FEET -> InventoryMenu.ARMOR_SLOT_START + 3;
			default -> -1;
		};
	}

	// 背包索引（0-8 快捷栏、9-35 主背包）在玩家背包菜单里的槽位号。
	static int inventoryMenuSlot(int inventoryIndex) {
		return inventoryIndex < Inventory.SELECTION_SIZE
			? InventoryMenu.USE_ROW_SLOT_START + inventoryIndex
			: inventoryIndex;
	}

	// 对应护甲部位的金制装备。
	static Item goldItemFor(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> Items.GOLDEN_HELMET;
			case CHEST -> Items.GOLDEN_CHESTPLATE;
			case LEGS -> Items.GOLDEN_LEGGINGS;
			case FEET -> Items.GOLDEN_BOOTS;
			default -> Items.AIR;
		};
	}

	// 在背包里找第一件对应部位的金制装备，找不到返回 -1。
	private static int findGoldArmor(Inventory inventory, EquipmentSlot slot) {
		Item gold = goldItemFor(slot);
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			if (inventory.getItem(i).getItem() == gold) {
				return i;
			}
		}
		return -1;
	}

	// 替换一件：自动模式在“身上已有任意金制护甲”时不新增，否则按部位顺序选第一件可用装备换上；
	// 单部位模式只处理指定部位（该部位已是金装备则不重复替换）。
	private static boolean tryEquipOne(Minecraft client, LocalPlayer player, PiglinArmorSwapMode mode) {
		if (mode == PiglinArmorSwapMode.AUTO
			&& (hasAnyGoldenArmor(player) || !SUPPRESSED.isEmpty())) {
			// 原版只需一件金制护甲；自动模式下已经有（或玩家刚摘掉被抑制）就不再替换。
			return false;
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (!mode.allows(slot) || SUPPRESSED.contains(slot) || SWAPPED.containsKey(slot)) {
				continue;
			}
			if (isGoldenArmorForSlot(player.getItemBySlot(slot), slot)) {
				// 该部位已经穿着金装备，不需要替换。
				continue;
			}
			int inventoryIndex = findGoldArmor(player.getInventory(), slot);
			if (inventoryIndex < 0) {
				continue;
			}
			int menuSlot = inventoryMenuSlot(inventoryIndex);
			ItemStack original = player.getItemBySlot(slot).copy();
			ItemStack gold = player.getInventory().getItem(inventoryIndex).copy();
			clickSlot(client, player, armorMenuSlot(slot));
			clickSlot(client, player, menuSlot);
			clickSlot(client, player, armorMenuSlot(slot));
			// 后置校验：本地预测未达成（槽位拒绝点击等）时把光标物品放回来源槽，不写记录。
			if (!isGoldenArmorForSlot(player.getItemBySlot(slot), slot)
				|| !player.containerMenu.getCarried().isEmpty()) {
				if (!player.containerMenu.getCarried().isEmpty()) {
					clickSlot(client, player, menuSlot);
				}
				return false;
			}
			SWAPPED.put(slot, new SwapRecord(menuSlot, original, gold, -1));
			TweakifyMod.LOGGER.info("Piglin armor swap: {} equipped from inventory slot {}", slot, inventoryIndex);
			return true;
		}
		return false;
	}

	// 尝试换回一件原装备：mode 为 null 时恢复全部，否则只恢复该模式不再覆盖的部位。
	// 本地预测确认恢复生效后立即删除记录，避免下一 tick 被 pruneInvalidRecords 误判为玩家改动。
	private static boolean tryRestoreOne(Minecraft client, LocalPlayer player, PiglinArmorSwapMode mode) {
		AbstractContainerMenu menu = player.containerMenu;
		Iterator<Map.Entry<EquipmentSlot, SwapRecord>> iterator = SWAPPED.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<EquipmentSlot, SwapRecord> entry = iterator.next();
			EquipmentSlot slot = entry.getKey();
			if (mode != null && mode.allows(slot)) {
				continue;
			}
			SwapRecord record = entry.getValue();
			if (!isRecordValid(player, menu, slot, record)) {
				// 玩家手动动过装备或背包槽：交给 pruneInvalidRecords 统一清理。
				continue;
			}
			clickSlot(client, player, armorMenuSlot(slot));
			clickSlot(client, player, record.menuSlot());
			clickSlot(client, player, armorMenuSlot(slot));
			if (!player.containerMenu.getCarried().isEmpty()) {
				// 序列中断：光标上应是原装备，再点一次护甲槽完成恢复；仍失败则保留记录重试。
				clickSlot(client, player, armorMenuSlot(slot));
			}
			if (!isGoldenArmorForSlot(player.getItemBySlot(slot), slot)) {
				// 本地预测已生效：身上不再是该金装备，恢复完成。
				iterator.remove();
				TweakifyMod.LOGGER.info("Piglin armor restore: {} put back from menu slot {}",
					slot, record.menuSlot());
				return true;
			}
			// 预测未生效（含光标仍残留）：保留记录，下一 tick 重试。
			return false;
		}
		return false;
	}

	// 清掉已经失效的记录：被玩家换掉的部位在本轮猪灵在场期间不再自动替换；
	// 背包槽被移动/整理过时先尝试重新定位原装备，避免已穿上的金装备永远换不回来。
	private static void pruneInvalidRecords(LocalPlayer player, AbstractContainerMenu menu) {
		Iterator<Map.Entry<EquipmentSlot, SwapRecord>> iterator = SWAPPED.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<EquipmentSlot, SwapRecord> entry = iterator.next();
			EquipmentSlot slot = entry.getKey();
			SwapRecord record = entry.getValue();
			if (isRecordValid(player, menu, slot, record)) {
				if (record.firstMissTick() != -1) {
					// 记录重新变有效（原装备放回了原位）：重新开始计算宽限。
					entry.setValue(new SwapRecord(record.menuSlot(), record.original(), record.gold(), -1));
				}
				continue;
			}
			if (!isGoldenArmorForSlot(player.getItemBySlot(slot), slot)) {
				// 身上已不是记录中的金装备：玩家主动换掉了它，本轮不再自动替换。
				SUPPRESSED.add(slot);
				iterator.remove();
				continue;
			}
			// 身上仍是记录的金装备：背包槽可能被移动/整理过，尝试重新定位原装备。
			int relocated = findRelocatedOriginalSlot(menu, record.menuSlot(), record.original());
			if (relocated >= 0) {
				entry.setValue(new SwapRecord(relocated, record.original(), record.gold(), -1));
				continue;
			}
			if (record.firstMissTick() == -1) {
				// 第一次找不到：记录起始 tick，之后按真实时间判定宽限。
				entry.setValue(new SwapRecord(record.menuSlot(), record.original(), record.gold(),
					player.tickCount));
				continue;
			}
			if (player.tickCount - record.firstMissTick() < RELOCATE_GIVE_UP_TICKS) {
				// 宽限期内（合成格/副手/箱子/丢地等瞬时状态）：保留记录继续重试。
				continue;
			}
			// 原装备长时间不在背包（被拿走/丢弃）：无法恢复，放弃记录。
			TweakifyMod.LOGGER.warn("Piglin armor swap: original item for {} not found, giving up restore", slot);
			iterator.remove();
		}
	}

	// 在背包菜单里重新定位换下来的原装备（9-35 主背包、36-44 快捷栏、45 副手）；
	// 原装备为空时找一个空槽放回金装备。
	// 多件组件完全相同的物品之间不可区分，优先选离原记录槽最近的一件，让金装备落位更符合直觉。
	static int findRelocatedOriginalSlot(AbstractContainerMenu menu, int excludedSlot, ItemStack original) {
		// 匹配原装备时允许副手（45）；找空槽时只回退到快捷栏末位，避免把金装备塞进副手。
		int end = original.isEmpty()
			? Math.min(InventoryMenu.USE_ROW_SLOT_END - 1, menu.slots.size() - 1)
			: Math.min(InventoryMenu.SHIELD_SLOT, menu.slots.size() - 1);
		if (original.isEmpty()) {
			for (int i = InventoryMenu.INV_SLOT_START; i <= end; i++) {
				if (i != excludedSlot && menu.getSlot(i).getItem().isEmpty()) {
					return i;
				}
			}
			return -1;
		}
		int best = -1;
		int bestDistance = Integer.MAX_VALUE;
		for (int i = InventoryMenu.INV_SLOT_START; i <= end; i++) {
			if (i == excludedSlot || !ItemStack.isSameItemSameComponents(menu.getSlot(i).getItem(), original)) {
				continue;
			}
			int distance = Math.abs(i - excludedSlot);
			if (distance < bestDistance) {
				best = i;
				bestDistance = distance;
			}
		}
		return best;
	}

	// 记录仍然有效：身上仍是那件金装备，背包槽里仍是换下来的原装备（原本为空则仍为空）。
	// 身上只比较物品类型：护甲穿在身上会掉耐久，用完整组件比较会让记录因耐久变化失效。
	private static boolean isRecordValid(LocalPlayer player, AbstractContainerMenu menu,
										 EquipmentSlot slot, SwapRecord record) {
		if (record.menuSlot() < 0 || record.menuSlot() >= menu.slots.size()) {
			return false;
		}
		ItemStack equipped = player.getItemBySlot(slot);
		ItemStack stored = menu.getSlot(record.menuSlot()).getItem();
		boolean stillGold = equipped.getItem() == record.gold().getItem();
		boolean stillOriginal = record.original().isEmpty()
			? stored.isEmpty()
			: ItemStack.isSameItemSameComponents(stored, record.original());
		return stillGold && stillOriginal;
	}

	// 一次 PICKUP 点击：与鼠标左键点击槽位等价，客户端会先本地预测再发送给服务器。
	private static void clickSlot(Minecraft client, LocalPlayer player, int slotId) {
		if (client.gameMode == null || slotId < 0) {
			return;
		}
		client.gameMode.handleContainerInput(player.containerMenu.containerId, slotId, 0,
			ContainerInput.PICKUP, player);
	}

	private static void resetTracking() {
		SWAPPED.clear();
		SUPPRESSED.clear();
		trackedLevel = null;
		trackedPlayer = null;
		piglinNearby = false;
		scanCooldown = 0;
		actionCooldown = 0;
		missedScans = 0;
	}

	// 包私有：供测试在用例之间清理静态状态。
	static void resetForTests() {
		resetTracking();
	}
}
