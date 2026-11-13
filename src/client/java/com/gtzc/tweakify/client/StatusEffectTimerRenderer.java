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
 * 文字固定按 0.5 倍缩放贴在图标底边内侧，长时间效果改用小时/天单位。
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
	// 时长格式已限制为最多 5 个字符，不再按宽度缩小，避免小数缩放让字体发虚。
	private static final float TEXT_SCALE = 0.5f;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

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
		List<MobEffectInstance> effects = new ArrayList<>(activeEffects);
		effects.sort(Comparator.reverseOrder());
		int beneficialCount = 0;
		int harmfulCount = 0;
		for (MobEffectInstance effect : effects) {
			if (!effect.showIcon()) {
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
			renderTimer(gui, client.font, effect, x, y);
		}
	}

	// 计算文字位置：固定 0.5 倍，底边贴住图标下沿；坐标取整避免字体像素落在半像素上。
	public static TextPlacement placementFor(Font font, MobEffectInstance effect, int x, int y) {
		// 无尽效果没有剩余时间，保持图标干净。
		if (effect.isInfiniteDuration()) {
			return null;
		}
		String text = EffectDurationFormatter.format(effect.getDuration());
		int textWidth = font.width(text);
		float scaledWidth = textWidth * TEXT_SCALE;
		float drawX = Math.round(x + (EFFECT_BOX_SIZE - scaledWidth) / 2.0f);
		// 文字底边贴住图标下沿（背景 y + 3 + 18）；向下取整保证不越过图标。
		float drawY = (float) Math.floor(y + EFFECT_ICON_OFFSET + EFFECT_ICON_SIZE - font.lineHeight * TEXT_SCALE);
		return new TextPlacement(text, drawX, drawY, scaledWidth, font.lineHeight * TEXT_SCALE);
	}

	private static void renderTimer(GuiGraphicsExtractor gui, Font font, MobEffectInstance effect, int x, int y) {
		TextPlacement placement = placementFor(font, effect, x, y);
		if (placement == null) {
			return;
		}
		float scale = placement.height() / font.lineHeight;
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(placement.x(), placement.y());
		pose.scale(scale, scale);
		gui.text(font, placement.text(), 0, 0, TEXT_COLOR, true);
		pose.popMatrix();
	}
}
