package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.AbstractVillagerInvoker;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 可见交易（服务端侧）：为村民生成当前等级以上的“锁定交易”，
 * 打开交易界面时同步给客户端显示，村民升级时按顺序解锁。
 *
 * <p>等价于 Visible Traders 的锁定交易：生成按等级分组、每升一级放出一组；
 * 数据随村民存档（addAdditionalSaveData/readAdditionalSaveData），
 * 因此村民卸载、切换维度、重进世界后仍然一致。
 * 缓存用 UUID 作为 key：Entity.equals/hashCode 基于运行时实体 id，
 * 而读档时 id 尚未分配，用实体对象作 key 会让所有村民互相覆盖。
 * 仅单人/局域网有效（远程服务器需要安装本模组才会下发数据）。
 */
public final class LockedTradeService {
	private static final int MAX_LEVEL = 5;
	private static final String SAVE_KEY = "tweakify_locked_offers";
	private static final Map<UUID, List<MerchantOffers>> LOCKED = new HashMap<>();

	private LockedTradeService() {
	}

	private static UUID key(Villager villager) {
		return villager.getUUID();
	}

	// 取（或生成）村民的锁定交易：从下一级一直生成到最高级，每级一组。
	// 缓存长度不足（按当前等级应有 MAX_LEVEL - level 组）时重建，避免读档长度不匹配或等级变化后错位。
	private static List<MerchantOffers> getOrGenerate(Villager villager) {
		int level = villager.getVillagerData().level();
		List<MerchantOffers> locked = LOCKED.get(key(villager));
		if (locked != null && locked.size() >= MAX_LEVEL - level) {
			return locked;
		}
		// 客户端没有服务端世界：不生成也不缓存（返回可变空表，调用方可能 remove）。
		if (!(villager.level() instanceof ServerLevel)) {
			return new ArrayList<>();
		}
		List<MerchantOffers> generated = new ArrayList<>();
		generate(villager, generated);
		LOCKED.put(key(villager), generated);
		return generated;
	}

	private static void generate(Villager villager, List<MerchantOffers> target) {
		if (!(villager.level() instanceof ServerLevel level)) {
			return;
		}
		int current = villager.getVillagerData().level();
		for (int lvl = current + 1; lvl <= MAX_LEVEL; lvl++) {
			MerchantOffers offers = new MerchantOffers();
			VillagerData data = villager.getVillagerData().withLevel(lvl);
			ResourceKey<TradeSet> trades = data.profession().value().getTrades(lvl);
			if (trades != null) {
				((AbstractVillagerInvoker) villager).tweakify$addOffersFromTradeSet(level, offers, trades);
			}
			// 空组也保留：保证“第 n 组 == 下一级”的对应关系，升级时不会错位。
			target.add(offers);
		}
	}

	// 等级变化前先按旧等级备好未来组：升级时 {@code setVillagerData} 先于 {@code updateTrades}
	// 执行，若等到升级后再生成缓存，就会把“再下一级”的交易当成本次升级发放（整体错位一级）。
	// 从未打开过交易界面的村民也会在这里提前生成，保证升级路径与界面显示用的是同一份缓存。
	public static void ensureGenerated(Villager villager) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.visibleTraders) {
			return;
		}
		getOrGenerate(villager);
	}

	// 村民升级：按下一次等级取一组锁定交易（不跳过空组，空组由调用方回退原版）。
	public static Optional<MerchantOffers> consumeForUpgrade(Villager villager) {
		List<MerchantOffers> locked = getOrGenerate(villager);
		if (locked.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(locked.remove(0));
	}

	// 合并所有剩余锁定交易，用于下发给客户端。
	public static Optional<MerchantOffers> condense(Villager villager) {
		List<MerchantOffers> locked = getOrGenerate(villager);
		MerchantOffers combined = new MerchantOffers();
		for (MerchantOffers offers : locked) {
			combined.addAll(offers);
		}
		return combined.isEmpty() ? Optional.empty() : Optional.of(combined);
	}

	// 打开交易界面时把锁定交易发给玩家；客户端未安装本模组时跳过。
	// 先 condense 生成缓存再判断 canSend：否则“从未打开过界面”的村民在升级时
	// 会按升级后的等级重新生成，导致发放的交易整体错位一级。
	public static void send(Villager villager, ServerPlayer player) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.visibleTraders) {
			return;
		}
		Optional<MerchantOffers> locked = condense(villager);
		if (locked.isEmpty() || !ServerPlayNetworking.canSend(player, LockedTradesPayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new LockedTradesPayload(locked));
	}

	// 随村民存档，保证卸载/重进世界后看到的未来交易与升级后获得的完全一致。
	public static void save(Villager villager, ValueOutput output) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.visibleTraders) {
			return;
		}
		List<MerchantOffers> locked = LOCKED.get(key(villager));
		if (locked == null || locked.isEmpty()) {
			return;
		}
		output.store(SAVE_KEY, MerchantOffers.CODEC.listOf(), locked);
	}

	public static void restore(Villager villager, ValueInput input) {
		input.read(SAVE_KEY, MerchantOffers.CODEC.listOf())
			.ifPresent(offers -> LOCKED.put(key(villager), new ArrayList<>(offers)));
	}

	public static void clear(Villager villager) {
		LOCKED.remove(key(villager));
	}
}
