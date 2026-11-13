package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BoatStepHeightState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 草径卡船修复：土径碰撞高度是 15/16，船从土径驶向完整方块时会被
 * 1/16 的台阶卡住。这里只给船增加 1/16 的上台阶高度，不改变方块碰撞，
 * 因此不会影响船在土径上的放置、摩擦和滑行速度。
 *
 * <p>在返回值上取 max：其它模组已经给船设置更大步高时不会被压回 1/16。
 */
@Mixin(Entity.class)
public abstract class BoatStepHeightMixin {
	@ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"))
	private float tweakify$boatStepHeight(float original) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.fixBoatStuckOnDirtPath
			&& (Object) this instanceof AbstractBoat) {
			return BoatStepHeightState.stepHeight(true, original);
		}
		return original;
	}
}
