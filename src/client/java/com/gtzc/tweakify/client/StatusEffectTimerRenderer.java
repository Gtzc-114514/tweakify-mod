package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 药水效果时间显示：在 HUD 右侧效果图标的下方叠加剩余时间。
 *
 * <p>坐标与 {@code Hud.extractEffects} 保持一致：有益效果从右向左排一行，
 * 有害效果整体下移 26 像素；图标背景 24x24，图标本体 18x18 且偏移 3 像素。
 * 文字默认按 0.5 倍缩放贴在图标底边内侧，长时间效果改用小时/天单位；
 * 开启“固定按秒”后文本可能更长，超出图标宽度时按比例缩小。
 */
public final class StatusEffectTimerRenderer {
	// 原版效果图标的排列参数。
	private static final int EFFECT_SLOT_WIDTH = 25;
	private static final int EFFECT_ROW_OFFSET = 26;
	private static final int EFFECT_BASE_Y = 1;
	private static final int DEMO_OFFSET = 15;
	private static final int EFFECT_BOX_SIZE = 24;
	private static final int EFFECT_ICON_OFFSET = 3;
	private static final int EFFECT_ICON_SIZE = 18;

	// 文字固定 0.5 倍：字形高约 4.5 像素，能放进图标下沿。
	// 默认格式最多 5 个字符；改为“始终按秒”后可能更长，超出图标宽度时再按比例缩小。
	private static final float TEXT_SCALE = 0.5f;
	private static final float MIN_TEXT_SCALE = 0.3f;
	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int COLOR_YELLOW = 0xFFFFFF55;
	private static final int COLOR_RED = 0xFFFF5555;
	// auto 配色分档：剩余 10 秒内变红，30 秒内变黄，其余白色。
	private static final int AUTO_RED_SECONDS = 10;
	private static final int AUTO_YELLOW_SECONDS = 30;

	// 复用的排序缓冲：效果集合很小，每帧清空重填即可，
	// 避免每帧复制集合与排序时新建列表（渲染线程单线程访问）。
	private static final List<MobEffectInstance> SORTED_EFFECTS = new ArrayList<>();
	private static final Comparator<MobEffectInstance> DESCENDING = Comparator.reverseOrder();

	private StatusEffectTimerRenderer() {
	}

	// 供渲染与游戏内测试共用的文本位置：坐标均为 GUI 单位。
	public record TextPlacement(String text, float x, float y, float width, float height) {
		public float right() {
			return this.x + this.width;
		}

		public float bottom() {
			return this.y + this.height;
		}
	}

	// 由 Hud.extractEffects 末尾调用：此时背景与图标已经提交，文字会绘制在最上层。
	public static void render(GuiGraphicsExtractor gui, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.statusEffectDurationDisplay || client.player == null) {
			return;
		}
		Collection<MobEffectInstance> activeEffects = client.player.getActiveEffects();
		if (activeEffects.isEmpty()) {
			return;
		}

		// 与原版相同的顺序：按效果自身排序后反向遍历。
		SORTED_EFFECTS.clear();
		SORTED_EFFECTS.addAll(activeEffects);
		SORTED_EFFECTS.sort(DESCENDING);
		int thresholdSeconds = config.functions.statusEffectDurationLowThreshold;
		String colorMode = config.functions.statusEffectDurationColorMode;
		int beneficialCount = 0;
		int harmfulCount = 0;
		for (MobEffectInstance effect : SORTED_EFFECTS) {
			if (!effect.showIcon()) {
				continue;
			}
			// 低时长过滤：阈值 0 时全部显示；只留下即将结束的效果。
			if (!shouldShowForThreshold(effect.getDuration(), thresholdSeconds)) {
				continue;
			}
			int x = gui.guiWidth();
			int y = EFFECT_BASE_Y;
			if (client.isDemo()) {
				y += DEMO_OFFSET;
			}
			if (effect.getEffect().value().isBeneficial()) {
				beneficialCount++;
				x -= EFFECT_SLOT_WIDTH * beneficialCount;
			} else {
				harmfulCount++;
				x -= EFFECT_SLOT_WIDTH * harmfulCount;
				y += EFFECT_ROW_OFFSET;
			}
			renderTimer(gui, client.font, effect, x, y, timerColorArgb(colorMode, effect.getDuration()));
		}
	}

	// 计算文字位置：固定 0.5 倍，底边贴住图标下沿；坐标取整避免字体像素落在半像素上。
	public static TextPlacement placementFor(Font font, MobEffectInstance effect, int x, int y) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean alwaysSeconds = config != null && config.functions.statusEffectDurationAlwaysSeconds;
		return placementFor(font, effect, x, y, alwaysSeconds);
	}

	// 重载：缓存/测试可直接指定格式，不必依赖全局配置。
	public static TextPlacement placementFor(Font font, MobEffectInstance effect, int x, int y,
			boolean alwaysSeconds) {
		// 无尽效果没有剩余时间，保持图标干净。
		if (effect.isInfiniteDuration()) {
			return null;
		}
		String text = EffectDurationFormatter.format(effect.getDuration(), alwaysSeconds);
		int textWidth = font.width(text);
		float scale = TEXT_SCALE;
		// 秒模式可能超过 5 个字符：超出图标宽度时按比例缩小，避免溢出到相邻图标。
		if (textWidth * scale > EFFECT_ICON_SIZE) {
			scale = Math.max(MIN_TEXT_SCALE, EFFECT_ICON_SIZE / (float) textWidth);
		}
		float scaledWidth = textWidth * scale;
		float scaledHeight = font.lineHeight * scale;
		float drawX = Math.round(x + (EFFECT_BOX_SIZE - scaledWidth) / 2.0f);
		// 文字底边贴住图标下沿（背景 y + 3 + 18）；向下取整保证不越过图标。
		float drawY = (float) Math.floor(y + EFFECT_ICON_OFFSET + EFFECT_ICON_SIZE - scaledHeight);
		return new TextPlacement(text, drawX, drawY, scaledWidth, scaledHeight);
	}

	private static void renderTimer(GuiGraphicsExtractor gui, Font font, MobEffectInstance effect, int x, int y,
									int color) {
		TextPlacement placement = placementFor(font, effect, x, y);
		if (placement == null) {
			return;
		}
		float scale = placement.height() / font.lineHeight;
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(placement.x(), placement.y());
		pose.scale(scale, scale);
		gui.text(font, placement.text(), 0, 0, color, true);
		pose.popMatrix();
	}

	// 低时长过滤：阈值 0 表示不过滤；无尽效果（duration ≤ 0）由 placementFor 处理，这里放行。
	public static boolean shouldShowForThreshold(int durationTicks, int thresholdSeconds) {
		return thresholdSeconds <= 0 || durationTicks <= 0 || durationTicks <= thresholdSeconds * 20;
	}

	// 剩余时间文字颜色：auto 在 10 秒内变红、30 秒内变黄，其余白色。
	public static int timerColorArgb(String colorMode, int durationTicks) {
		return switch (StatusDurationColorMode.fromCode(colorMode)) {
			case WHITE -> COLOR_WHITE;
			case YELLOW -> COLOR_YELLOW;
			case RED -> COLOR_RED;
			case AUTO -> {
				int seconds = Math.max(0, durationTicks / 20);
				yield seconds <= AUTO_RED_SECONDS ? COLOR_RED
					: seconds <= AUTO_YELLOW_SECONDS ? COLOR_YELLOW : COLOR_WHITE;
			}
		};
	}
}
