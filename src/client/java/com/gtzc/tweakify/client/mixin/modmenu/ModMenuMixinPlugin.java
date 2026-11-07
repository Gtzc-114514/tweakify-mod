package com.gtzc.tweakify.client.mixin.modmenu;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

/**
 * Mod Menu 是可选依赖：未安装时整份 Mixin 配置都不应用，
 * 避免目标类不存在导致启动报错。
 */
public class ModMenuMixinPlugin implements IMixinConfigPlugin {
	private static final String MOD_MENU_ID = "modmenu";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().isModLoaded(MOD_MENU_ID);
	}
}
