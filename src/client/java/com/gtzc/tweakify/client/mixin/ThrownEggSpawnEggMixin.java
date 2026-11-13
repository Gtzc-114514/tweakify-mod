package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnEggThrower;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刷怪蛋投掷的命中逻辑：复用鸡蛋的碰撞与粒子，但不让承载刷怪蛋的鸡蛋
 * 按原版概率生成小鸡；在命中结束后再按刷怪蛋类型生成生物。
 */
@Mixin(ThrownEgg.class)
public class ThrownEggSpawnEggMixin {
	// 原版 onHit 用 nextInt(8) 决定是否生成小鸡；承载刷怪蛋时返回 1 跳过小鸡分支。
	@Redirect(method = "onHit", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0))
	private int tweakify$skipChickenForSpawnEgg(RandomSource random, int bound) {
		ThrownEgg self = (ThrownEgg) (Object) this;
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.functions.throwSpawnEgg
			&& self.getItem().getItem() instanceof SpawnEggItem) {
			return 1;
		}
		return random.nextInt(bound);
	}

	@Inject(method = "onHit", at = @At("TAIL"))
	private void tweakify$spawnMobOnEggHit(HitResult result, CallbackInfo ci) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.functions.throwSpawnEgg) {
			SpawnEggThrower.spawnMob((ThrownEgg) (Object) this, result);
		}
	}
}
