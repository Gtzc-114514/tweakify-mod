package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 关闭指定生物生成：除了 {@code SpawnPlacements.checkSpawnRules}，
 * 幻翼、猫、增援、鸡骑士、巡逻队与结构刷怪等路径会直接调用
 * {@code EntityType.create}，因此在这一层再拦一次。
 *
 * <p>只按 {@link SpawnFeatureState#shouldBlockSpawn} 的来源白名单拦截：
 * 自然/结构/巡逻/增援/鸡骑士/刷怪笼等，以及关闭“允许玩家生成”后的
 * 刷怪蛋/命令/繁殖等玩家来源。
 */
@Mixin(EntityType.class)
public class EntityTypeSpawnBlockMixin {
	// 用 @WrapMethod 在调用原工厂前直接返回 null，避免实体先被创建再丢弃。
	@WrapMethod(method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Lnet/minecraft/world/entity/Entity;")
	private Entity tweakify$blockDisabledSpawn(Level level, EntitySpawnRequest request,
			Operation<Entity> original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || request == null) {
			return original.call(level, request);
		}
		EntityType<?> type = (EntityType<?>) (Object) this;
		if (SpawnFeatureState.shouldBlockSpawn(
			config.functions.disableSpecifiedMobSpawn, config.functions.disabledSpawnAllowPlayerSpawn,
			config.isSpawnDisabled(type), request.reason(), type)) {
			return null;
		}
		return original.call(level, request);
	}
}
