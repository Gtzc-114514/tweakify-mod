package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.ConsumableOptimization;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 消耗品优化：本地玩家进食/饮用倒计时归零的那一帧直接收尾。
 *
 * <p>{@code updateUsingItem} 在客户端只递减剩余时间、不会完成使用（原版把完成交给服务端），
 * 因此在 HEAD 接管后取消原逻辑，由 {@link ConsumableOptimization} 播放本地反馈并结束使用动作。
 */
@Mixin(LivingEntity.class)
public class LivingEntityConsumableOptimizationMixin {
	@Inject(method = "updateUsingItem", at = @At("HEAD"), cancellable = true)
	private void tweakify$finishConsumableLocally(ItemStack useItem, CallbackInfo ci) {
		// 只有本地玩家走客户端接管，其它实体（含服务端侧）保持原版。
		if (!((Object) this instanceof LocalPlayer player)) {
			return;
		}
		if (ConsumableOptimization.handleUpdateUsingItem(player, useItem)) {
			ci.cancel();
		}
	}
}
