package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 和平模式怪物：{@code /summon} 在创建实体前会单独检查
 * {@code isAllowedInPeaceful}，这里让列表内生物通过该检查。
 * 实际创建仍受 {@code EntityType.create}、玩家生成开关和列表控制。
 */
@Mixin(SummonCommand.class)
public class SummonCommandPeacefulMixin {
	@Redirect(method = "createEntity", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
	private static boolean tweakify$allowConfiguredPeacefulSummon(EntityType<?> type) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldAllowPeacefulSpawn(
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(type))) {
			return true;
		}
		return type.isAllowedInPeaceful();
	}
}
