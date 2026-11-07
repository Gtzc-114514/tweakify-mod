package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒附魔可移除：原版砂轮只清理非诅咒附魔，开关开启时改为清空全部附魔。
 * 结果计算发生在服务端，因此仅在单人/局域网（模组同时作用于集成服务端）时生效。
 */
@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {
	@Inject(method = "removeNonCursesFrom", at = @At("HEAD"), cancellable = true)
	private void tweakify$removeAllEnchantments(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.disableCurseEnchantmentRestriction) {
			return;
		}

		// 复制后再清理，避免改动对比槽里的原物品；清空后附魔书按原版逻辑变为普通书。
		ItemStack result = stack.copy();
		ItemEnchantments remaining = EnchantmentHelper.updateEnchantments(result,
			enchantments -> enchantments.removeIf(holder -> true));
		if (result.is(Items.ENCHANTED_BOOK) && remaining.isEmpty()) {
			result = result.transmuteCopy(Items.BOOK);
		}
		result.set(DataComponents.REPAIR_COST, 0);
		cir.setReturnValue(result);
	}
}
