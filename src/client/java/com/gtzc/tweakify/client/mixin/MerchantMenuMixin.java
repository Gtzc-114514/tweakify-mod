package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.MerchantMenuAccess;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 可见交易（客户端）：把服务端下发的锁定交易合并进 MerchantMenu.getOffers，
 * 交易界面即可显示未来等级的交易；原始交易数量每次取最新值，供按钮禁用使用。
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin implements MerchantMenuAccess {
	@Unique
	private MerchantOffers tweakify$lockedOffers;

	@Unique
	private int tweakify$originalCount;

	@Override
	public void tweakify$setLockedOffers(MerchantOffers offers) {
		this.tweakify$lockedOffers = offers;
	}

	@Override
	public boolean tweakify$hasLockedOffers() {
		return this.tweakify$lockedOffers != null && !this.tweakify$lockedOffers.isEmpty();
	}

	@Override
	public int tweakify$getOriginalCount() {
		return this.tweakify$originalCount;
	}

	@ModifyReturnValue(method = "getOffers", at = @At("RETURN"), require = 0)
	private MerchantOffers tweakify$combineLockedOffers(MerchantOffers original) {
		// 每次都用最新的真实列表数量，避免补货/声望刷新后 originalCount 过时。
		this.tweakify$originalCount = original.size();
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.visibleTraders
			|| this.tweakify$lockedOffers == null || this.tweakify$lockedOffers.isEmpty()) {
			return original;
		}
		MerchantOffers combined = new MerchantOffers();
		combined.addAll(original);
		combined.addAll(this.tweakify$lockedOffers);
		return combined;
	}

	@Inject(method = "setOffers", at = @At("TAIL"), require = 0)
	private void tweakify$clearLockedOnNewOffers(MerchantOffers offers, CallbackInfo ci) {
		// 服务端菜单不会有锁定数据；短路也避免在服务端线程触碰客户端屏幕状态。
		if (this.tweakify$lockedOffers == null) {
			return;
		}
		// 真实列表更新（升级/补货）后旧的锁定组可能已经解锁：清空等待重新下发。
		this.tweakify$lockedOffers = null;
		// 锁定列表变短后旧的选中项可能越界：仅在越界时复位，并同步容器侧选择。
		if (Minecraft.getInstance().gui.screen() instanceof MerchantScreen screen
			&& screen.getMenu() == (MerchantMenu) (Object) this) {
			MerchantScreenAccessor accessor = (MerchantScreenAccessor) screen;
			MerchantMenu menu = (MerchantMenu) (Object) this;
			if (accessor.tweakify$getShopItem() >= menu.getOffers().size()) {
				accessor.tweakify$setShopItem(0);
				menu.setSelectionHint(0);
			}
		}
	}
}
