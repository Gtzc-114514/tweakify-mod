package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.config.EntityHighlightColor;
import com.gtzc.tweakify.client.config.EntityHighlightStyle;
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
		TweakifyModConfig config = TweakifyMod.config();
		if (entity == null) {
			return false;
		}
		Level level = entity.level();
		if (level == null || !level.isClientSide()) {
			return false;
		}
		if (!config.shouldHighlightEntity(entity.getType())) {
			return false;
		}
		// 闪烁样式：半个周期显示、半个周期隐藏；常亮样式直接命中。
		if (EntityHighlightStyle.fromCode(config.functions.entityHighlightStyle) == EntityHighlightStyle.BLINK) {
			return isBlinkOn(level.getGameTime(), config.functions.entityHighlightBlinkPeriod);
		}
		return true;
	}

	// 闪烁相位：周期夹紧到允许范围，前一半时间为显示状态。
	public static boolean isBlinkOn(long gameTime, int periodTicks) {
		int period = Math.max(TweakifyModConfig.MIN_ENTITY_HIGHLIGHT_BLINK_PERIOD,
			Math.min(TweakifyModConfig.MAX_ENTITY_HIGHLIGHT_BLINK_PERIOD, periodTicks));
		return Math.floorMod(gameTime, period) < period / 2;
	}

	// 配置的描边颜色；未命中、配置缺失或 vanilla 时返回原值（保留原版队伍色）。
	public static int outlineColor(Entity entity, int original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!shouldGlow(entity)) {
			return original;
		}
		int configured = EntityHighlightColor.argbOf(config.functions.entityHighlightColor);
		return configured != 0 ? configured : original;
	}
}
