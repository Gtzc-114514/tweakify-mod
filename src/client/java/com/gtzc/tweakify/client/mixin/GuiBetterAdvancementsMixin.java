package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.BetterAdvancementScreen;
import com.gtzc.tweakify.client.BetterAdvancements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 更好的进度：把原版 {@link AdvancementsScreen} 替换成自适应窗口实现。
 *
 * <p>只在精确匹配原版类时替换，其它模组继承的进度界面不受影响；功能关闭时原样放行。
 */
@Mixin(Gui.class)
public class GuiBetterAdvancementsMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen tweakify$replaceAdvancementsScreen(Screen screen) {
		if (screen == null || screen.getClass() != AdvancementsScreen.class || !BetterAdvancements.enabled()) {
			return screen;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null) {
			return screen;
		}
		return new BetterAdvancementScreen(client.getConnection().getAdvancements());
	}
}
