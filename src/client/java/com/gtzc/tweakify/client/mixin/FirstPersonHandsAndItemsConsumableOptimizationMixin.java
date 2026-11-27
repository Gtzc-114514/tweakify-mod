package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.ConsumableOptimization;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 消耗品优化：吃完后同一物品的堆叠替换直接生效，不播放快照式手部动画。
 *
 * <p>只在本地刚刚完成进食的那一次切换上命中（标记一次性消费），其余切换保持原版过渡。
 */
@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsConsumableOptimizationMixin {
	@Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
	private void tweakify$skipHandSwapAnimation(ItemStack from, ItemStack to, LocalPlayer player,
			CallbackInfoReturnable<Boolean> cir) {
		if (ConsumableOptimization.shouldSkipHandSwapAnimation(from, to)) {
			cir.setReturnValue(true);
		}
	}
}
