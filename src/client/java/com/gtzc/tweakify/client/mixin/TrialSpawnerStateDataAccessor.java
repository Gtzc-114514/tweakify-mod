package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取试炼刷怪笼方块实体数据里的生成数据键，用于手持信息显示将生成的生物。
 */
@Mixin(TrialSpawnerStateData.class)
public interface TrialSpawnerStateDataAccessor {
	@Accessor("TAG_SPAWN_DATA")
	static String getSpawnDataKey() {
		throw new AssertionError();
	}
}
