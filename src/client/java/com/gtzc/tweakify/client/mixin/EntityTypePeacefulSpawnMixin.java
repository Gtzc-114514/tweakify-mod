package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 和平模式怪物：{@code EntityType.create} 在真正实例化前还会调用 {@code canSpawn}，
 * 其中同样会因和平难度拒绝列表中的生物。这里只绕过该处限制，
 * 保留特性开关、列表与其它生成条件。
 */
@Mixin(EntityType.class)
public class EntityTypePeacefulSpawnMixin {
	@Redirect(method = "canSpawn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
	private static boolean tweakify$allowPeacefulSpawn(EntityType<?> type) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldAllowPeacefulSpawn(
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(type))) {
			return true;
		}
		return type.isAllowedInPeaceful();
	}
}
