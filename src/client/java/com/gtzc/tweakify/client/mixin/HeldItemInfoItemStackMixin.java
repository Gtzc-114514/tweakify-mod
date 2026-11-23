package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.HeldItemInfoState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 手持物品信息：追加弩的装填物时隐藏原版的 intangible projectile 提示行
 * （参考实现通过同一个注入实现，避免提示里多出无意义的“弹射物”行）。
 */
@Mixin(ItemStack.class)
public class HeldItemInfoItemStackMixin {
	@Inject(method = "addUnitComponentToTooltip", at = @At("HEAD"), cancellable = true)
	private void tweakify$hideIntangibleProjectile(DataComponentType<?> type, Component component,
			TooltipDisplay display, Consumer<Component> builder, CallbackInfo ci) {
		if (type == DataComponents.INTANGIBLE_PROJECTILE
			&& HeldItemInfoState.isHidingIntangibleProjectile()) {
			ci.cancel();
		}
	}
}
