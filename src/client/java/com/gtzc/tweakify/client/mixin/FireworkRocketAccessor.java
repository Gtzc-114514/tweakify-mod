package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取烟花火箭当前附着的实体：只有附着在玩家身上的烟花才会提供鞘翅加速。
 */
@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketAccessor {
	@Accessor("attachedToEntity")
	LivingEntity tweakify$getAttachedToEntity();
}
