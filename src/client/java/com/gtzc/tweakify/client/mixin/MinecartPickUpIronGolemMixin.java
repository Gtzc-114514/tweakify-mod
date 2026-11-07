package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.IronGolemMinecartState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 铁傀儡进矿车：空矿车与铁傀儡接触时，在服务端让铁傀儡坐进矿车。
 *
 * <p>原版矿车没有像船那样的自动拾取逻辑；这里每 4 tick 查询一次碰撞箱内的铁傀儡，
 * 避免大量矿车时每 tick 都做实体扫描。只在服务端执行，乘客由原版同步到客户端。
 */
@Mixin(AbstractMinecart.class)
public class MinecartPickUpIronGolemMixin {
	// 每个矿车自己的检查节流计数：Entity.tickCount 在 AbstractMinecart.tick 中不会递增。
	@Unique
	private int tweakify$pickupTicks;

	@Unique
	private static final AtomicBoolean TWEAKIFY$REPORTED = new AtomicBoolean();

	@Inject(method = "tick", at = @At("TAIL"), require = 0)
	private void tweakify$pickUpIronGolem(CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$REPORTED, "ironGolemMinecart");
		AbstractMinecart minecart = (AbstractMinecart) (Object) this;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (!IronGolemMinecartState.shouldPickUp(config != null && config.ironGolemRideMinecart,
			minecart.level().isClientSide(), !minecart.getPassengers().isEmpty())) {
			return;
		}
		// 每 4 tick 检查一次；用实体 ID 错开相位，避免所有空矿车在同一 tick 集中查询。
		this.tweakify$pickupTicks++;
		if (Math.floorMod(this.tweakify$pickupTicks + minecart.getId(), 4) != 0) {
			return;
		}
		AABB searchBox = IronGolemMinecartState.searchBox(minecart.getBoundingBox());
		List<IronGolem> golems = minecart.level().getEntitiesOfClass(IronGolem.class, searchBox,
			golem -> !golem.isPassenger() && !golem.isVehicle() && golem.isAlive());
		if (!golems.isEmpty()) {
			golems.get(0).startRiding(minecart);
		}
	}
}
