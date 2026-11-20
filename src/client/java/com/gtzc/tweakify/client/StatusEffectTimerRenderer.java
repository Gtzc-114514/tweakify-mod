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
import java.util.function.ToIntFunction;

/**
 * 药水效果时间显示：在 HUD 右侧效果图标的下方叠加剩余时间。
 *
 * <p>坐标与 {@code Hud.extractEffects} 保持一致：有益效果从右向左排一行，
 * 有害效果整体下移 26 像素；图标背景 24x24，图标本体 18x18 且偏移 3 像素。
 * 文字默认按 0.5 倍缩放贴在图标底边内侧，长时间效果改用小时/天单位；
 * 开启“固定按秒”后文本可能更长，超出图标宽度时按比例缩小。
 *
 * <p>排序、文本格式化与 {@link TextPlacement} 在客户端 tick 结束时构建一次，
 * 渲染帧只遍历缓存绘制；玩家、字体、GUI 宽度或演示模式变化时缓存失效并重建一次。
 * 整体缩放由 HUD 注入包裹原版效果渲染，时间文字与图标共用同一缩放矩阵。
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

	// 复用的排序缓冲：效果集合很小，每 tick 清空重填即可，
	// 避免复制集合与排序时新建列表（客户端 tick 与渲染同线程）。
	private static final List<MobEffectInstance> SORTED_EFFECTS = new ArrayList<>();
	private static final Comparator<MobEffectInstance> DESCENDING = Comparator.reverseOrder();
	// tick 构建、渲染帧只读的缓存；玩家/字体/GUI 宽度/演示模式变化时失效。
	private static final TimerCache CACHE = new TimerCache();

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

	// 布局所需的纯输入：把 MobEffectInstance 的判定结果提前采样，
	// 便于单测在不加载注册表与渲染上下文的情况下覆盖排列。
	record EffectSnapshot(boolean showIcon, int durationTicks, boolean beneficial, boolean infinite) {
	}

	// 渲染帧只读的条目：文本、位置与颜色都在 tick 构建。
	record CachedTimer(TextPlacement placement, int color) {
	}

	// tick 级更新：排序、过滤、格式化与 TextPlacement 都只在这里构建一次。
	static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.statusEffectDurationDisplay || client.player == null) {
			CACHE.reset();
			return;
		}
		int guiWidth = guiWidth(client);
		boolean demo = client.isDemo();
		CACHE.update(rebuildTimers(client, guiWidth, demo, config), client.player, client.font, guiWidth, demo);
	}

	// 由 Hud.extractEffects 末尾调用：缓存命中时只遍历预构建条目并绘制。
	public static void render(GuiGraphicsExtractor gui, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.statusEffectDurationDisplay || client.player == null) {
			return;
		}
		int guiWidth = guiWidth(client);
		boolean demo = client.isDemo();
		if (!CACHE.matches(client.player, client.font, guiWidth, demo)) {
			// 窗口缩放、资源重载或首次启用：就地重建一次，避免等下一 tick 期间位置错误。
			// 常规更新仍由 tick 驱动，缓存命中时渲染帧只读。
			CACHE.update(rebuildTimers(client, guiWidth, demo, config), client.player, client.font, guiWidth, demo);
		}
		for (CachedTimer timer : CACHE.timers()) {
			drawTimer(gui, client.font, timer);
		}
	}

	// tick 与 render 共用窗口 GUI 缩放宽：避免 gui.guiWidth() 与窗口口径不一致时每帧 miss。
	private static int guiWidth(Minecraft client) {
		return client.getWindow().getGuiScaledWidth();
	}

	// 整体缩放系数：功能关闭或配置未初始化时为 1；百分比在允许范围内夹紧。
	public static float effectScale(TweakifyModConfig config) {
		if (config == null || !config.functions.statusEffectDurationDisplay) {
			return 1.0f;
		}
		int percent = Math.max(TweakifyModConfig.MIN_STATUS_EFFECT_SCALE,
			Math.min(TweakifyModConfig.MAX_STATUS_EFFECT_SCALE, config.functions.statusEffectDurationScale));
		return percent / 100.0f;
	}

	// 以右上角 (right, 0) 为锚点压入缩放矩阵；与 EffectScaleScope.close 成对。
	// 单独抽成作用域便于单测验证锚点与矩阵栈平衡。
	public static EffectScaleScope pushAnchoredScale(Matrix3x2fStack pose, int right, float scale) {
		pose.pushMatrix();
		pose.translate(right, 0.0f);
		pose.scale(scale, scale);
		pose.translate(-right, 0.0f);
		return new EffectScaleScope(pose);
	}

	public static final class EffectScaleScope implements AutoCloseable {
		private final Matrix3x2fStack pose;
		private boolean closed;

		private EffectScaleScope(Matrix3x2fStack pose) {
			this.pose = pose;
		}

		// 幂等：重复 close 不能多弹一层，避免破坏调用方的矩阵栈。
		@Override
		public void close() {
			if (this.closed) {
				return;
			}
			this.closed = true;
			this.pose.popMatrix();
		}
	}

	private static List<CachedTimer> rebuildTimers(Minecraft client, int guiWidth, boolean demo,
			TweakifyModConfig config) {
		Collection<MobEffectInstance> activeEffects = client.player.getActiveEffects();
		if (activeEffects.isEmpty()) {
			// 没有效果时不做排序/采样：空缓存同样是有效结果。
			return List.of();
		}
		List<EffectSnapshot> snapshots = snapshotEffects(activeEffects);
		return buildTimers(snapshots, client.font::width, client.font.lineHeight, guiWidth, demo,
			config.functions.statusEffectDurationLowThreshold,
			config.functions.statusEffectDurationColorMode,
			config.functions.statusEffectDurationAlwaysSeconds);
	}

	// 与原版相同的顺序：按效果自身排序后再采样；排序只在 tick/失效重建时发生。
	private static List<EffectSnapshot> snapshotEffects(Collection<MobEffectInstance> effects) {
		SORTED_EFFECTS.clear();
		SORTED_EFFECTS.addAll(effects);
		SORTED_EFFECTS.sort(DESCENDING);
		List<EffectSnapshot> snapshots = new ArrayList<>(SORTED_EFFECTS.size());
		for (MobEffectInstance effect : SORTED_EFFECTS) {
			snapshots.add(new EffectSnapshot(effect.showIcon(), effect.getDuration(),
				effect.getEffect().value().isBeneficial(), effect.isInfiniteDuration()));
		}
		return snapshots;
	}

	// 纯构建：输入已按原版顺序排好；渲染帧不再做过滤、格式化或位置计算。
	static List<CachedTimer> buildTimers(List<EffectSnapshot> effects, ToIntFunction<String> measure,
			int lineHeight, int guiWidth, boolean demo, int thresholdSeconds, String colorMode,
			boolean alwaysSeconds) {
		List<CachedTimer> timers = new ArrayList<>(effects.size());
		int beneficialCount = 0;
		int harmfulCount = 0;
		for (EffectSnapshot effect : effects) {
			if (!effect.showIcon()) {
				continue;
			}
			// 低时长过滤：阈值 0 时全部显示；只留下即将结束的效果。
			if (!shouldShowForThreshold(effect.durationTicks(), thresholdSeconds)) {
				continue;
			}
			int x = guiWidth;
			int y = EFFECT_BASE_Y;
			if (demo) {
				y += DEMO_OFFSET;
			}
			if (effect.beneficial()) {
				beneficialCount++;
				x -= EFFECT_SLOT_WIDTH * beneficialCount;
			} else {
				harmfulCount++;
				x -= EFFECT_SLOT_WIDTH * harmfulCount;
				y += EFFECT_ROW_OFFSET;
			}
			TextPlacement placement = placementFor(measure, lineHeight, effect.durationTicks(),
				effect.infinite(), x, y, alwaysSeconds);
			if (placement == null) {
				continue;
			}
			timers.add(new CachedTimer(placement, timerColorArgb(colorMode, effect.durationTicks())));
		}
		return timers;
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
		return placementFor(font::width, font.lineHeight, effect.getDuration(), false, x, y, alwaysSeconds);
	}

	// 纯布局：不依赖 Font/注册表，测试用测量函数与行高替换字体。
	static TextPlacement placementFor(ToIntFunction<String> measure, int lineHeight, int durationTicks,
			boolean infinite, int x, int y, boolean alwaysSeconds) {
		if (infinite) {
			return null;
		}
		String text = EffectDurationFormatter.format(durationTicks, alwaysSeconds);
		int textWidth = measure.applyAsInt(text);
		float scale = TEXT_SCALE;
		// 秒模式可能超过 5 个字符：超出图标宽度时按比例缩小，避免溢出到相邻图标。
		if (textWidth * scale > EFFECT_ICON_SIZE) {
			scale = Math.max(MIN_TEXT_SCALE, EFFECT_ICON_SIZE / (float) textWidth);
		}
		float scaledWidth = textWidth * scale;
		float scaledHeight = lineHeight * scale;
		float drawX = Math.round(x + (EFFECT_BOX_SIZE - scaledWidth) / 2.0f);
		// 文字底边贴住图标下沿（背景 y + 3 + 18）；向下取整保证不越过图标。
		float drawY = (float) Math.floor(y + EFFECT_ICON_OFFSET + EFFECT_ICON_SIZE - scaledHeight);
		return new TextPlacement(text, drawX, drawY, scaledWidth, scaledHeight);
	}

	// 渲染帧只做矩阵变换与文字提交，不再计算文本/位置。
	private static void drawTimer(GuiGraphicsExtractor gui, Font font, CachedTimer timer) {
		TextPlacement placement = timer.placement();
		float scale = placement.height() / font.lineHeight;
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(placement.x(), placement.y());
		pose.scale(scale, scale);
		gui.text(font, placement.text(), 0, 0, timer.color(), true);
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

	// 缓存有效性：玩家、字体、GUI 宽度与演示模式任一变化即失效；
	// 失效时由 tick 或渲染侧重建一次，避免缩放窗口期间沿用旧坐标。
	// 仅客户端渲染线程访问（客户端 tick 与 HUD extract 同线程）：不做同步。
	static final class TimerCache {
		private List<CachedTimer> timers = List.of();
		private Object player;
		private Object font;
		private int guiWidth = -1;
		private boolean demo;
		private boolean valid;

		boolean matches(Object player, Object font, int guiWidth, boolean demo) {
			return this.valid && this.player == player && this.font == font
				&& this.guiWidth == guiWidth && this.demo == demo;
		}

		void update(List<CachedTimer> timers, Object player, Object font, int guiWidth, boolean demo) {
			this.timers = List.copyOf(timers);
			this.player = player;
			this.font = font;
			this.guiWidth = guiWidth;
			this.demo = demo;
			this.valid = true;
		}

		List<CachedTimer> timers() {
			return this.timers;
		}

		void reset() {
			this.timers = List.of();
			this.player = null;
			this.font = null;
			this.guiWidth = -1;
			this.demo = false;
			this.valid = false;
		}
	}
}
