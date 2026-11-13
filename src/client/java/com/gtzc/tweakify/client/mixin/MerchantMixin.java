package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.LockedTradeService;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 可见交易（服务端）：村民打开交易界面时，把未来等级（锁定）的交易同步给玩家。
 */
@Mixin(Merchant.class)
public interface MerchantMixin {
	@Inject(method = "openTradingScreen", at = @At("TAIL"), require = 0)
	private void tweakify$sendLockedTrades(Player player, Component title, int level, CallbackInfo ci) {
		HookProbe.markOnce("visibleTradersSend");
		if (!((Object) this instanceof Villager villager)) {
			return;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			LockedTradeService.send(villager, serverPlayer);
		}
	}
}
