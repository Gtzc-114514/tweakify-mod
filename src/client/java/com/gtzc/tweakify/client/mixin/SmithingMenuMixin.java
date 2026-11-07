package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 锻造模板不消耗：原版 onTake 会依次收缩模板/base/添加物槽，
 * 这里拦截模板槽（槽位 0）的收缩调用，保留模板；其他槽照常消耗。
 * 菜单逻辑发生在服务端，因此仅在单人/局域网（模组同时作用于集成服务端）时生效。
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {
	// shrinkStackInSlot 是 SmithingMenu 自身的 private 方法，shadow 它用于转发非模板槽的收缩。
	@Shadow
	private void shrinkStackInSlot(int slot) {
	}

	@Redirect(method = "onTake",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V"))
	private void tweakify$skipTemplateShrink(SmithingMenu menu, int slot) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.disableSmithingTemplateConsumption
			&& slot == SmithingMenu.TEMPLATE_SLOT) {
			return;
		}
		this.shrinkStackInSlot(slot);
	}
}
