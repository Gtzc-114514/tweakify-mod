package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 物品栏耐久条上的耐久文字。
 *
 * <p>文字按 0.5 倍缩放绘制在耐久条上方，单行占约 5 像素高，不遮挡原版耐久条。
 * 显示内容分耐久百分比与剩余耐久数值两种，两者使用同一字号；
 * 颜色随剩余耐久变化：绿 → 白 → 黄 → 红。
 */
public final class DurabilityOverlayRenderer {
	// 百分比与剩余耐久共用的文字缩放比例：0.5 后字形高约 5 像素。
	private static final float TEXT_SCALE = 0.5f;
	// 剩余耐久位数过多（超出物品槽宽度）时才继续缩小，但不小于此值。
	private static final float MIN_TEXT_SCALE = 0.2f;
	// 文字可用显示宽度：物品槽宽 16，左右各留 0.5 像素。
	private static final float MAX_TEXT_WIDTH = 15.0f;
	// 单行文字底边挨在耐久条（y + 13）上方，留 1 像素间隙。
	private static final float TEXT_BOTTOM = 12.0f;
	// 单个物品槽的宽度，用于文字水平居中。
	private static final float SLOT_WIDTH = 16.0f;

	// 剩余耐久分界：高于 75% 绿，高于 50% 白，高于 25% 黄，其余红。
	private static final double HIGH_RATIO = 0.75;
	private static final double MEDIUM_RATIO = 0.5;
	private static final double LOW_RATIO = 0.25;

	private static final int GREEN = 0xFF55FF55;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int YELLOW = 0xFFFFFF55;
	private static final int RED = 0xFFFF5555;

	private DurabilityOverlayRenderer() {
	}

	// 由物品装饰绘制完成后调用：x/y 为物品槽左上角。
	public static void render(GuiGraphicsExtractor gui, Font font, ItemStack stack, int x, int y) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.durabilityOverlay || stack == null) {
			return;
		}
		// 仅在 HUD 快捷栏显示：打开任意界面（背包/容器）时不显示。
		if (config.durabilityOverlayHotbarOnly && Minecraft.getInstance().gui.screen() != null) {
			return;
		}
		// 满耐久时原版耐久条本身不可见，这里同样不显示文字。
		if (!stack.isBarVisible()) {
			return;
		}
		int max = stack.getMaxDamage();
		if (max <= 0) {
			return;
		}
		int remaining = Math.max(0, max - stack.getDamageValue());
		// 低阈值模式：只在剩余耐久不高于该百分比时显示。
		int threshold = config.durabilityOverlayLowThreshold;
		if (threshold > 0 && remaining * 100 > max * threshold) {
			return;
		}
		TextMetrics metrics = metricsFor(modeFor(config), remaining, max, font);
		String text = metrics.text();

		// 两种显示内容统一使用 TEXT_SCALE；只有位数多到放不下时才额外缩小。
		int textWidth = metrics.width();
		float scale = TEXT_SCALE;
		if (textWidth * TEXT_SCALE > MAX_TEXT_WIDTH) {
			scale = Math.max(MIN_TEXT_SCALE, MAX_TEXT_WIDTH / textWidth);
		}
		float top = TEXT_BOTTOM - font.lineHeight * scale;
		drawCentered(gui, font, text, textWidth, x, y + top, scale, colorFor(remaining, max));
	}

	// 显示内容解析结果缓存：配置变化频率很低，不必每帧每个物品槽都遍历枚举。
	private static String cachedModeCode;
	private static DurabilityDisplayMode cachedMode = DurabilityDisplayMode.PERCENT;

	// 文本与宽度缓存：物品栏每帧都会对所有槽位调用一次，
	// 同一件物品的「模式 + 剩余 + 总量」组合重复率很高，避免重复拼字符串与测宽。
	// 键压成一个 long（模式 2 位 + 剩余 31 位 + 总量 31 位），不在每帧每槽分配对象。
	private record TextMetrics(String text, int width) {
	}

	private static final Map<Long, TextMetrics> TEXT_CACHE = new HashMap<>();
	private static final int MAX_TEXT_CACHE_ENTRIES = 256;

	private static TextMetrics metricsFor(DurabilityDisplayMode mode, int remaining, int max, Font font) {
		long key = textCacheKey(mode, remaining, max);
		TextMetrics cached = TEXT_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		String text = textFor(mode, remaining, max);
		TextMetrics metrics = new TextMetrics(text, font.width(text));
		if (TEXT_CACHE.size() >= MAX_TEXT_CACHE_ENTRIES) {
			// 超出上限时只淘汰一半：整体清空会让下一帧所有槽位同时重算，造成周期性抖动。
			Iterator<Long> iterator = TEXT_CACHE.keySet().iterator();
			for (int i = 0; i < MAX_TEXT_CACHE_ENTRIES / 2 && iterator.hasNext(); i++) {
				iterator.next();
				iterator.remove();
			}
		}
		TEXT_CACHE.put(key, metrics);
		return metrics;
	}

	// 键布局：高 2 位模式序号，中间 31 位剩余耐久，低 31 位总量。
	private static long textCacheKey(DurabilityDisplayMode mode, int remaining, int max) {
		return ((long) mode.ordinal() << 62) | ((long) remaining << 31) | max;
	}

	private static DurabilityDisplayMode modeFor(TweakifyModConfig config) {
		String code = config.durabilityDisplayMode;
		if (!Objects.equals(code, cachedModeCode)) {
			cachedModeCode = code;
			cachedMode = DurabilityDisplayMode.fromCode(code);
		}
		return cachedMode;
	}

	// 剩余耐久对应的文字颜色。
	public static int colorFor(int remaining, int max) {
		double ratio = max <= 0 ? 0.0 : (double) remaining / max;
		if (ratio > HIGH_RATIO) {
			return GREEN;
		}
		if (ratio > MEDIUM_RATIO) {
			return WHITE;
		}
		if (ratio > LOW_RATIO) {
			return YELLOW;
		}
		return RED;
	}

	// 按显示内容生成文字：百分比或剩余耐久数值。
	public static String textFor(DurabilityDisplayMode mode, int remaining, int max) {
		return switch (mode == null ? DurabilityDisplayMode.PERCENT : mode) {
			case PERCENT -> percentText(remaining, max);
			case VALUE -> remainingText(remaining);
		};
	}

	// 剩余耐久百分比，四舍五入到整数。
	public static String percentText(int remaining, int max) {
		if (max <= 0) {
			return "0%";
		}
		return Math.round(remaining * 100.0f / max) + "%";
	}

	// 剩余耐久数值，不显示初始总耐久。
	public static String remainingText(int remaining) {
		return Integer.toString(remaining);
	}

	// 以物品槽水平居中绘制一行缩放文字，带阴影保证在物品图标上也能看清。
	private static void drawCentered(GuiGraphicsExtractor gui, Font font, String text, int textWidth,
								 int x, float y, float scale, int color) {
		float scaledWidth = textWidth * scale;
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		gui.text(font, text, Math.round((SLOT_WIDTH - scaledWidth) / 2.0f / scale), 0, color, true);
		pose.popMatrix();
	}
}
