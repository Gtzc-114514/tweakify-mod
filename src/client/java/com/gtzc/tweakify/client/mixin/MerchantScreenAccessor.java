package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取/复位 MerchantScreen 的滚动偏移与当前选中项：
 * 按钮 index 是行号，真实交易索引 = index + scrollOff。
 */
@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
	@Accessor("scrollOff")
	int tweakify$getScrollOff();

	@Accessor("shopItem")
	int tweakify$getShopItem();

	@Accessor("shopItem")
	void tweakify$setShopItem(int value);
}
