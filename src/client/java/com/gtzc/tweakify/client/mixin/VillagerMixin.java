package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.LockedTradeService;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 可见交易（服务端）：村民升级时改用预先生成的锁定交易，
 * 同时保留原版后续逻辑（updateSpecialPrices 会刷新交易界面与议价），
 * 保证交易界面里看到的未来交易与升级后实际获得的完全一致。
 */
@Mixin(Villager.class)
public class VillagerMixin {
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
	private void tweakify$saveLockedTrades(ValueOutput output, CallbackInfo ci) {
		LockedTradeService.save((Villager) (Object) this, output);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
	private void tweakify$readLockedTrades(ValueInput input, CallbackInfo ci) {
		LockedTradeService.restore((Villager) (Object) this, input);
	}

	@Redirect(method = "updateTrades", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/npc/villager/Villager;addOffersFromTradeSet(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/trading/MerchantOffers;Lnet/minecraft/resources/ResourceKey;)V"))
	private void tweakify$useLockedGroup(Villager villager, ServerLevel level, MerchantOffers offers,
										 ResourceKey<TradeSet> tradeSet) {
		HookProbe.markOnce("visibleTradersUpdateTrades");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.visibleTraders) {
			((AbstractVillagerInvoker) villager).tweakify$addOffersFromTradeSet(level, offers, tradeSet);
			return;
		}
		// offers 为空表示首次惰性生成（当前等级交易），必须交给原版；
		// 只有升级（已有交易）才消耗锁定组，避免把下一级交易当成当前等级发放。
		if (!offers.isEmpty()) {
			Optional<MerchantOffers> locked = LockedTradeService.consumeForUpgrade(villager);
			if (locked.isPresent() && !locked.get().isEmpty()) {
				offers.addAll(locked.get());
				return;
			}
		}
		((AbstractVillagerInvoker) villager).tweakify$addOffersFromTradeSet(level, offers, tradeSet);
	}

	@Inject(method = "die", at = @At("HEAD"), require = 0)
	private void tweakify$clearOnDeath(DamageSource source, CallbackInfo ci) {
		// 村民死亡后锁定数据已无意义，避免 UUID 缓存长期占用。
		LockedTradeService.clear((Villager) (Object) this);
	}

	@Inject(method = "updateSpecialPrices", at = @At("TAIL"), require = 0)
	private void tweakify$resendLockedTrades(Player player, CallbackInfo ci) {
		// 升级/补货/声望刷新都会重发交易列表（客户端会清空旧锁定），
		// 这里在重发后补发剩余的锁定交易，界面无需重开。
		if (player instanceof ServerPlayer serverPlayer) {
			LockedTradeService.send((Villager) (Object) this, serverPlayer);
		}
	}

	@Inject(method = "setVillagerData", at = @At("HEAD"), require = 0)
	private void tweakify$clearOnProfessionChange(VillagerData data, CallbackInfo ci) {
		// 只有职业变化才需要清空（原版会重建 offers）；升级也会调 setVillagerData，
		// 在升级路径上清空会让锁定组按新等级重新生成，导致交易整体错位一级。
		Villager villager = (Villager) (Object) this;
		if (!villager.getVillagerData().profession().equals(data.profession())) {
			LockedTradeService.clear(villager);
		} else if (data.level() > villager.getVillagerData().level()) {
			// 升级在 setVillagerData 之后才走 updateTrades：先按旧等级备好未来组，
			// 否则从未打开过交易界面的村民会把“再下一级”当成升级奖励发放。
			LockedTradeService.ensureGenerated(villager);
		}
	}

	@Inject(method = "startTrading", at = @At("TAIL"), require = 0)
	private void tweakify$sendAfterOpenScreen(Player player, CallbackInfo ci) {
		// startTrading 内部先 updateSpecialPrices 再 openTradingScreen，
		// 在 TAIL 发送时 open-screen 包已入队，客户端能正确接收锁定列表。
		if (player instanceof ServerPlayer serverPlayer) {
			LockedTradeService.send((Villager) (Object) this, serverPlayer);
		}
	}
}
