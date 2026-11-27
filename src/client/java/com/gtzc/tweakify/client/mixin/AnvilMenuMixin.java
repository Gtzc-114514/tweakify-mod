package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.AnvilPickupState;
import com.gtzc.tweakify.client.state.EnchantmentConflictRules;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 铁砧扩展：禁用“过于昂贵”并可选移除附魔互斥限制。
 *
 * <p>原版用“是否创造模式”这一条件跳过过于昂贵的清空，这里在功能开启时直接当成创造模式；
 * 合并附魔时的互斥判定（Enchantment.areCompatible）在“移除附魔冲突”开启时一律视为兼容。
 * 三个注入点都用 require = 0，其它铁砧类模组改写方法时功能失效即可，不拖垮启动。
 * 三个钩子第一次执行时会各记一条 “Mixin hook active” 日志：
 * anvilCreateResult（过于昂贵）、anvilEnchantmentConflicts（附魔冲突）、anvilMayPickup（等级不足）。
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Shadow
	private DataSlot cost;

	// createResult 里有两处创造模式判定：第一处用于不兼容附魔，第二处才是过于昂贵。
	// 第二处执行时 cost 已写入 DataSlot，因此可以直接用“可承受费用上限”判定。
	@WrapOperation(method = "createResult", require = 0, at = @At(value = "INVOKE", ordinal = 1,
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
	private boolean tweakify$ignoreTooExpensive(Player player, Operation<Boolean> original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config.modified.disableTooExpensive
			&& AnvilPickupState.shouldAllowResult(true, this.cost.get(),
				config.modified.tooExpensiveMaxLevelCost, config.modified.tooExpensiveWarnOnly)) {
			HookProbe.markOnce("anvilCreateResult");
			return true;
		}
		return original.call(player);
	}

	// 等级不足时原版不允许取走结果；放行后扣等级由原版把等级夹到 0。
	// hasStack 必须参与判定：输出槽为空时放行会让后续 onTake 拿到空物品并照常扣经验。
	// 超过费用上限且未开启“仅提示”时直接阻止（即使玩家等级足够）。
	@ModifyReturnValue(method = "mayPickup(Lnet/minecraft/world/entity/player/Player;Z)Z",
		at = @At("RETURN"), require = 0)
	private boolean tweakify$allowPickup(boolean original, Player player, boolean hasStack) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.disableTooExpensive) {
			return original;
		}
		int cost = this.cost.get();
		if (AnvilPickupState.shouldBlockByCost(true, cost,
			config.modified.tooExpensiveMaxLevelCost, config.modified.tooExpensiveWarnOnly)) {
			return false;
		}
		if (AnvilPickupState.shouldAllowPickup(true, hasStack, cost,
			config.modified.tooExpensiveMaxLevelCost, config.modified.tooExpensiveWarnOnly)) {
			HookProbe.markOnce("anvilMayPickup");
			return true;
		}
		return original;
	}

	// createResult 合并附魔时的互斥判定：开启后视为兼容；
	// “仅同类”时要求双方互斥组来自同一个 exclusive_set 标签，否则保留原版判定。
	@WrapOperation(method = "createResult", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"))
	private boolean tweakify$ignoreEnchantmentConflicts(Holder<Enchantment> first, Holder<Enchantment> second,
			Operation<Boolean> original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.removeEnchantmentConflicts) {
			return original.call(first, second);
		}
		Player player = ((ItemCombinerMenuAccessor) (Object) this).tweakify$getPlayer();
		if (config.modified.removeEnchantmentConflictsRequireSneak
			&& (player == null || !player.isShiftKeyDown())) {
			return original.call(first, second);
		}
		boolean sameGroup = EnchantmentConflictRules.sameExclusiveGroup(
			first.value().exclusiveSet().unwrapKey(), second.value().exclusiveSet().unwrapKey());
		if (!EnchantmentConflictRules.shouldBypass(true,
			config.modified.removeEnchantmentConflictsSameGroupOnly, sameGroup)) {
			return original.call(first, second);
		}
		HookProbe.markOnce("anvilEnchantmentConflicts");
		return true;
	}
}
