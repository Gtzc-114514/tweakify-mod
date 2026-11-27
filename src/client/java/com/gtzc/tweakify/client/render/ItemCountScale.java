package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * 物品数量数字缩放：只缩放数量文字，不改变物品图标与耐久条。
 *
 * <p>锚点保持在原版右下角（x + 17, y + 9 + 行高），缩放时数字向左上方展开；
 * 文字宽度超过原版右对齐区域时按比例回退，避免盖住左侧格子。100% 时完全交回原版绘制。
 */
public final class ItemCountScale {
	// 原版数量文字从 x+17 右对齐，可用宽度即 17 像素；三位数放大后会越出格子。
	static final float VANILLA_TEXT_WIDTH = 17.0F;
	private ItemCountScale() {
	}

	// 当前缩放百分比；配置未初始化时按原版 100%。
	public static int currentPercent() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.modified.itemCountScale;
	}

	// 原版只在数量不为 1 或显式提供文本时绘制。
	public static boolean shouldDraw(ItemStack stack, String countText) {
		return stack.getCount() != 1 || countText != null;
	}

	public static String textOf(ItemStack stack, String countText) {
		return countText == null ? String.valueOf(stack.getCount()) : countText;
	}

	// 右下角锚点：数字右边界与底边保持原版位置。
	public static float anchorX(int x) {
		return x + 17.0F;
	}

	public static float anchorY(int y, int lineHeight) {
		return y + 9.0F + lineHeight;
	}

	// 根据文字实际宽度限制放大倍数：三位数最多保持原版字号，两位数可放大到约 140%。
	public static float effectiveScale(int percent, int textWidth) {
		float requested = percent / 100.0F;
		if (textWidth <= 0) {
			return requested;
		}
		float maxScale = Math.max(1.0F, VANILLA_TEXT_WIDTH / textWidth);
		return Math.min(requested, maxScale);
	}

	// 返回是否自行绘制；缩放到原版大小（如三位数）时放行原版，保持零差异。
	public static boolean draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int percent) {
		int textWidth = font.width(text);
		float scale = effectiveScale(percent, textWidth);
		if (Math.abs(scale - 1.0F) < 1.0E-4F) {
			return false;
		}
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(anchorX(x), anchorY(y, font.lineHeight));
			graphics.pose().scale(scale);
			graphics.text(font, text, -textWidth, -font.lineHeight, 0xFFFFFFFF, true);
		} finally {
			// 原版绘制可能抛异常：必须恢复矩阵，避免后续整个界面偏移。
			graphics.pose().popMatrix();
		}
		return true;
	}
}
