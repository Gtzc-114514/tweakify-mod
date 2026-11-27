package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SnowGolemBiomeState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 雪傀儡不再因为温暖/干燥群系的环境属性而融化。
 *
 * <p>只拦截 {@link EnvironmentAttributes#SNOW_GOLEM_MELTS} 的查询；雨、水和熔岩造成的
 * 伤害仍走原版逻辑，避免把“群系免疫”扩大成完全无敌。融化判定在服务端执行，
 * 因此该功能只在单人游戏或自己开设的局域网世界生效。
 */
@Mixin(SnowGolem.class)
public class SnowGolemBiomeImmunityMixin {
	@Redirect(
		method = "aiStep",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;"
				+ "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;"
				+ "Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"
		)
	)
	private Object tweakify$skipBiomeMelting(EnvironmentAttributeSystem system,
			EnvironmentAttribute<?> attribute, Vec3 pos) {
		TweakifyModConfig config = TweakifyMod.config();
		if (attribute == EnvironmentAttributes.SNOW_GOLEM_MELTS) {
			SnowGolem self = (SnowGolem) (Object) this;
			boolean overworld = self.level().dimension() == Level.OVERWORLD;
			if (config.modified.snowGolemBiomeImmunityOnlyWhenRaining
				&& !self.level().isRainingAt(self.blockPosition())) {
				return system.getValue(attribute, pos);
			}
			if (SnowGolemBiomeState.shouldSuppressMelting(config.modified.snowGolemBiomeImmunity,
				config.modified.snowGolemBiomeImmunityOverworldOnly, overworld)) {
				return Boolean.FALSE;
			}
		}
		return system.getValue(attribute, pos);
	}
}
