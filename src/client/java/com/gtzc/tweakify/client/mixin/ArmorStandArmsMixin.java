package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 盔甲架手臂：让所有盔甲架的 {@code showArms} 视为开启。
 *
 * <p>同一次改写同时覆盖渲染（客户端）与服务端交互，因此单人/局域网下可以直接把物品
 * 放上盔甲架双手；远程服务器只影响本地渲染，服务端仍按它自己的 NBT 判定交互。
 */
@Mixin(ArmorStand.class)
public class ArmorStandArmsMixin {
	@ModifyReturnValue(method = "showArms", at = @At("RETURN"))
	private boolean tweakify$forceShowArms(boolean original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.armorStandArms) {
			return true;
		}
		return original;
	}
}
