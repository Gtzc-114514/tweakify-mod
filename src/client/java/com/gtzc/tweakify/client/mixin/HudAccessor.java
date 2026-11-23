package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 HUD 的私有排版常量：血量行高与每行心数。
 * 手持物品信息的“避开血量条”计算与原版完全一致，需要这两个常量。
 *
 * <p>方法名带 tweakify 前缀：held-item-info 等同类模组也会向 Hud 合并同名访问器，
 * 重名会让 Mixin 在启动时直接失败。
 */
@Mixin(Hud.class)
public interface HudAccessor {
	@Accessor("NUM_HEARTS_PER_ROW")
	static int getTweakifyNumHeartsPerRow() {
		throw new AssertionError();
	}

	@Accessor("LINE_HEIGHT")
	static int getTweakifyLineHeight() {
		throw new AssertionError();
	}
}
