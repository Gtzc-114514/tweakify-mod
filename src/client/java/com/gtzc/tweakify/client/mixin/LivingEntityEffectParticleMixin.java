package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 更好的隐身：隐身实体不再生成药水效果粒子。
 * 原版在客户端 tickEffects 里按剩余效果随机生成粒子，这里在调用处直接跳过。
 */
@Mixin(LivingEntity.class)
public class LivingEntityEffectParticleMixin {
	@Redirect(method = "tickEffects", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private void tweakify$hideInvisibleEffectParticles(Level level, ParticleOptions particle,
													   double x, double y, double z,
													   double dx, double dy, double dz) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		LivingEntity self = (LivingEntity) (Object) this;
		if (config != null && config.betterInvisibility
			&& config.betterInvisibilityEffectParticles && self.isInvisible()) {
			return;
		}
		level.addParticle(particle, x, y, z, dx, dy, dz);
	}
}
