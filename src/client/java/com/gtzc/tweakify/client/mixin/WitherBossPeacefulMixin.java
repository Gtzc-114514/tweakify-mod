package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 和平模式怪物：凋灵重写了 {@code checkDespawn}，不会走 {@link MobPeacefulMixin}
 * 所拦截的 {@code Mob.checkDespawn}，这里单独保留列表中的凋灵。
 */
@Mixin(WitherBoss.class)
public class WitherBossPeacefulMixin {
	@Redirect(method = "checkDespawn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
	private boolean tweakify$keepPeacefulWither(EntityType<?> type) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldStayInPeaceful(
			config.peacefulModeMonsters, config.isPeacefulModeMonster(type))) {
			return true;
		}
		return type.isAllowedInPeaceful();
	}
}
