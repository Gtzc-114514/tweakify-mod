package com.gtzc.tweakify.client.mixin.minihud;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

/**
 * MiniHUD 是可选依赖：未安装时整份 Mixin 配置都不应用，
 * 避免目标类不存在导致启动报错。
 */
public class MiniHudMixinPlugin implements IMixinConfigPlugin {
	private static final String MINI_HUD_ID = "minihud";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().isModLoaded(MINI_HUD_ID);
	}
}
