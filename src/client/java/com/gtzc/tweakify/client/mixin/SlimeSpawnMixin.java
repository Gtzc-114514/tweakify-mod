package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 超平坦无史莱姆：超平坦世界的史莱姆生成判定直接返回 false。
 * 生成判定发生在服务端，因此仅在单人/局域网（模组同时作用于集成服务端）时生效。
 */
@Mixin(Slime.class)
public class SlimeSpawnMixin {
	@ModifyReturnValue(method = "checkSlimeSpawnRules", at = @At("RETURN"))
	private static boolean tweakify$disableFlatWorldSlimeSpawn(boolean original, EntityType<?> type,
			LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.disableFlatWorldSlimeSpawn) {
			return original;
		}
		// 对所有世界类型生效。
		if (config.flatWorldSlimeSpawnAllWorlds) {
			return false;
		}
		if (level instanceof ServerLevel serverLevel
			&& serverLevel.getChunkSource().getGenerator() instanceof FlatLevelSource) {
			return false;
		}
		return original;
	}
}
