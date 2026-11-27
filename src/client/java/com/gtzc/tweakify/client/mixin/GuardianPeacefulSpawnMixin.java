package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.SpawnFeatureState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 守卫者自身的 spawn rule 在和平难度下会直接返回 false；
 * 列表内生物允许时只改难度，水域与空间检查继续生效。
 */
@Mixin(Guardian.class)
public class GuardianPeacefulSpawnMixin {
	@Redirect(method = "checkGuardianSpawnRules", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/LevelAccessor;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private static Difficulty tweakify$allowPeacefulGuardianSpawn(LevelAccessor level,
																  EntityType<?> type, LevelAccessor levelAccessor,
																  EntitySpawnReason reason, BlockPos pos,
																  RandomSource random) {
		return SpawnFeatureState.peacefulSpawnDifficulty(level, type);
	}
}
