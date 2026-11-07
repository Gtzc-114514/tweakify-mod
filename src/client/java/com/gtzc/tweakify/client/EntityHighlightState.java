package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 实体高亮状态：判断当前实体是否应被自定义高亮预设标记为发光。
 * 具体列表匹配交给配置对象，这里只屏蔽非客户端世界与空实体。
 */
public final class EntityHighlightState {
	private EntityHighlightState() {
	}

	public static boolean shouldGlow(Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || entity == null) {
			return false;
		}
		Level level = entity.level();
		if (level == null || !level.isClientSide()) {
			return false;
		}
		return config.shouldHighlightEntity(entity.getType());
	}
}
