package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.SpawnFeatureState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 溺尸自身的 spawn rule 在和平难度下会直接返回 false；
 * 列表内生物允许时只改难度，水域/光照等检查继续生效。
 */
@Mixin(Drowned.class)
public class DrownedPeacefulSpawnMixin {
	@Redirect(method = "checkDrownedSpawnRules", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/ServerLevelAccessor;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private static Difficulty tweakify$allowPeacefulDrownedSpawn(ServerLevelAccessor level,
																 EntityType<?> type, ServerLevelAccessor levelAccessor,
																 EntitySpawnReason reason, BlockPos pos,
																 RandomSource random) {
		return SpawnFeatureState.peacefulSpawnDifficulty(level, type);
	}
}
