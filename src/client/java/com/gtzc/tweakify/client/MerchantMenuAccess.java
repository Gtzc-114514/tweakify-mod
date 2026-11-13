package com.gtzc.tweakify.client;

import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 客户端 MerchantMenu 的锁定交易扩展：由网络包写入，供 getOffers 合并与按钮禁用使用。
 */
public interface MerchantMenuAccess {
	void tweakify$setLockedOffers(MerchantOffers offers);

	boolean tweakify$hasLockedOffers();

	int tweakify$getOriginalCount();
}
