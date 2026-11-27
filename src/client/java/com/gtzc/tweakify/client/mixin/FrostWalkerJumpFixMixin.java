package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.FrostWalkerJumpState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 冰霜行者跳跃修复：原版的 {@code location_changed} 效果带
 * {@code is_on_ground} 要求，起跳后的每一帧都不再结冰，于是跳着过水面会踩空落水。
 *
 * <p>这里放宽的是“冰霜行者这一条 replace_disk 效果”的要求判定：只在该实体是玩家、
 * 没有骑乘、且确实处于离地状态时把不匹配当作匹配，落地时的原版判定完全不变。
 * 位置与半径仍由原版 {@link ReplaceDisk} 计算（偏移 (0,-1,0)、半径随等级），
 * 所以结冰范围与站地时一致。
 */
@Mixin(Enchantment.class)
public class FrostWalkerJumpFixMixin {
	@WrapOperation(method = "runLocationChangedEffects", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/ConditionalEffect;matches("
				+ "Lnet/minecraft/world/level/storage/loot/LootContext;)Z"))
	private boolean tweakify$allowAirborneFrostWalker(ConditionalEffect<?> effect, LootContext context,
													  Operation<Boolean> original) {
		if (original.call(effect, context)) {
			return true;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.fix.frostWalkerJumpFix) {
			return false;
		}
		// 只放宽冰霜行者用的磁盘替换效果，其余附魔效果的要求保持原样。
		if (!(effect.effect() instanceof ReplaceDisk)) {
			return false;
		}
		// 位置上下文由 Enchantment.locationContext 构造，THIS_ENTITY 即穿戴该附魔的实体。
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		// 骑乘中仍按原版处理：否则船上/矿车上会凭空在载具下方刷冰。
		if (!(entity instanceof Player)) {
			return false;
		}
		if (config.fix.frostWalkerJumpFixSprintOnly && !entity.isSprinting()) {
			return false;
		}
		if (!FrostWalkerJumpState.shouldRelax(true, config.fix.frostWalkerJumpFixJumpOnly,
			entity.isPassenger(), entity.onGround(), entity.getDeltaMovement().y)) {
			return false;
		}
		HookProbe.markOnce("frostWalkerJumpFix");
		return true;
	}
}
