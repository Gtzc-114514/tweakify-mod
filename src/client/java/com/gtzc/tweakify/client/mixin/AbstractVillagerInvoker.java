package com.gtzc.tweakify.client.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 AbstractVillager 的受保护方法，用于生成锁定交易。
 */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerInvoker {
	@Invoker("addOffersFromTradeSet")
	void tweakify$addOffersFromTradeSet(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> tradeSet);
}
