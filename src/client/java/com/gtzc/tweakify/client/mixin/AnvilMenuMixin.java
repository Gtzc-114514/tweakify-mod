package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.AnvilPickupState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用附魔过于昂贵：花费达到 40 级时铁砧仍产出结果，且等级不足也能取走。
 *
 * <p>原版用“是否创造模式”这一条件跳过过于昂贵的清空，这里在功能开启时直接当成创造模式；
 * 两个注入点都用 require = 0，其它铁砧类模组改写方法时功能失效即可，不拖垮启动。
 * 两个钩子第一次执行时会各记一条 “Mixin hook active” 日志，
 * 排查“开关开了但没效果”时先看日志里是不是两条都在。
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Shadow
	private DataSlot cost;

	// createResult 里有两处创造模式判定：第一处用于不兼容附魔，第二处才是过于昂贵。
	@Redirect(method = "createResult", require = 0, at = @At(value = "INVOKE", ordinal = 1,
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
	private boolean tweakify$ignoreTooExpensive(Player player) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.disableTooExpensive;
		if (enabled) {
			HookProbe.markOnce("anvilCreateResult");
		}
		return enabled || player.hasInfiniteMaterials();
	}

	// 等级不足时原版不允许取走结果；放行后扣等级由原版把等级夹到 0。
	// hasStack 必须参与判定：输出槽为空时放行会让后续 onTake 拿到空物品并照常扣经验。
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$allowPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.disableTooExpensive;
		if (!AnvilPickupState.shouldAllowPickup(enabled, hasStack, this.cost.get())) {
			return;
		}
		HookProbe.markOnce("anvilMayPickup");
		cir.setReturnValue(true);
	}
}
