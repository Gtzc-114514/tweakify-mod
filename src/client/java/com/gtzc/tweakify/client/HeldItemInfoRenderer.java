package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.HudAccessor;
import com.gtzc.tweakify.config.FunctionConfig;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 手持物品信息的渲染状态（参考 held-item-info）。
 *
 * <p>物品切换或内容相关配置变化时重建行；渲染帧只做定位、背景与文字绘制。
 * 原版“选中物品名称”的绘制被 {@link com.gtzc.tweakify.client.mixin.HeldItemInfoHudMixin}
 * 在方法头部接管，因此不会与同样重定向内部文字调用的模组（如 held-item-info）冲突。
 */
public final class HeldItemInfoRenderer {
	// 参考实现用于推算原版 y 的字体高度；与 Font.lineHeight 一致。
	private static final int FONT_HEIGHT = 9;

	private static List<Line> lines = Collections.emptyList();
	private static int maxWidth = -1;
	private static int lastTooltipY = -1;

	// 影响提示内容的配置快照：游戏内改开关或过滤名单后无需切换物品。
	private static int lastContentSignature;

	// 语言/字体快照：切换后重新测量行宽，避免文字变了但居中/背景框宽度没变。
	private static int lastFontSignature = Integer.MIN_VALUE;

	// 构建异常后暂时回退到原版名称渲染，避免每 tick 重试同一个坏物品并刷日志。
	private static boolean failed;

	// 容器界面关闭后请求重建：背包快捷右键编辑手持潜影盒等场景会改变物品内容，
	// 但物品实例不变，需要主动刷新一次缓存。
	private static volatile boolean rebuildRequested;

	// 上一 tick 的功能开关状态：重新开启时强制重建（包括失败后的重试）。
	private static boolean enabledLastTick;

	private HeldItemInfoRenderer() {
	}

	/**
	 * 一行提示：文字与按当前字体预先量好的宽度（每帧直接用，避免重复测量）。
	 */
	public record Line(Component text, int width) {
	}

	// 当前功能是否可用（关闭或构建失败时 Mixin 回退到原版渲染）。
	public static boolean isActive() {
		TweakifyModConfig config = TweakifyMod.config();
		return config != null && config.functions.heldItemInfo && !failed;
	}

	public static void clear() {
		lines = Collections.emptyList();
		maxWidth = -1;
		lastTooltipY = -1;
		lastContentSignature = 0;
		failed = false;
	}

	// 每帧开始时清掉上一帧的提示位置：动作栏消息的避让只在本帧绘制后生效。
	public static void beforeFrame() {
		lastTooltipY = -1;
	}

	// 请求下一次 tick 重建（可由任意线程调用，实际重建仍在客户端线程）。
	public static void requestRebuild() {
		rebuildRequested = true;
	}

	// 取走并清除重建请求。
	public static boolean consumeRebuildRequest() {
		if (!rebuildRequested) {
			return false;
		}
		rebuildRequested = false;
		return true;
	}

	// 记录功能开关状态，返回本次是否发生切换。
	public static boolean noteEnabled(boolean enabled) {
		if (enabled == enabledLastTick) {
			return false;
		}
		enabledLastTick = enabled;
		return true;
	}

	// 原版选中名称的 y：ContextualBar 底边距 + 高度 + 两行血量 + 1 + 字体高度（26.3 为 59）。
	public static int vanillaTooltipYOffset() {
		return ContextualBar.MARGIN_BOTTOM + ContextualBar.HEIGHT
			+ 2 * HudAccessor.getTweakifyLineHeight() + 1 + FONT_HEIGHT;
	}

	// 语言或字体切换后重新测量行宽：文本在绘制时按当前语言解析，缓存宽度必须同步更新。
	public static void refreshFontMetrics(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null) {
			return;
		}
		int signature = System.identityHashCode(Language.getInstance());
		signature = 31 * signature
			+ (config.general.language == null ? 0 : config.general.language.hashCode());
		signature = 31 * signature + (config.general.followSystemLanguage ? 1 : 0);
		if (signature == lastFontSignature) {
			return;
		}
		lastFontSignature = signature;
		if (lines.isEmpty()) {
			return;
		}
		List<Line> remeasured = new ArrayList<>(lines.size());
		for (Line line : lines) {
			remeasured.add(new Line(line.text(), client.font.width(line.text())));
		}
		lines = remeasured;
		maxWidth = -1;
	}

	// 内容相关配置是否变化（排版参数由渲染时实时读取，不需要重建）。
	public static boolean contentSignatureChanged(FunctionConfig functions) {
		return contentSignature(functions) != lastContentSignature;
	}

	// 手写累加哈希：每 tick 都会调用，避免 Objects.hash 的 varargs 数组与装箱分配。
	private static int contentSignature(FunctionConfig f) {
		int hash = 1;
		hash = mix(hash, f.heldItemInfoShowName);
		hash = mix(hash, f.heldItemInfoShowEnchantments);
		hash = mix(hash, f.heldItemInfoShowPotionEffects);
		hash = mix(hash, f.heldItemInfoShowContainerContent);
		hash = mix(hash, f.heldItemInfoMergeSimilarContainerItems);
		hash = mix(hash, f.heldItemInfoShowFireworkAttributes);
		hash = mix(hash, f.heldItemInfoShowCommandBlockInfo);
		hash = mix(hash, f.heldItemInfoShowBeehiveContent);
		hash = mix(hash, f.heldItemInfoShowSpawnerEntity);
		hash = mix(hash, f.heldItemInfoShowCrossbowProjectiles);
		hash = mix(hash, f.heldItemInfoShowLore);
		hash = mix(hash, f.heldItemInfoShowUnbreakable);
		hash = mix(hash, f.heldItemInfoShowSignText);
		hash = mix(hash, f.heldItemInfoShowMusicDiscDescription);
		hash = mix(hash, f.heldItemInfoShowPaintingDescription);
		hash = mix(hash, f.heldItemInfoShowGoatHornInstrument);
		hash = mix(hash, f.heldItemInfoShowBookMeta);
		hash = mix(hash, f.heldItemInfoShowFilledMapId);
		hash = mix(hash, f.heldItemInfoShowEntityBucketContent);
		hash = mix(hash, f.heldItemInfoShowBlockState);
		hash = mix(hash, f.heldItemInfoShowHiddenLinesCount);
		hash = mix(hash, f.heldItemInfoMaxLines);
		hash = mix(hash, f.heldItemInfoMaxLineLength);
		hash = mix(hash, f.heldItemInfoMaxCommandLines);
		hash = mix(hash, f.heldItemInfoMaxLoreLines);
		hash = mix(hash, f.heldItemInfoRespectHideFlags);
		hash = mix(hash, f.heldItemInfoShowOnlyFilteredEnchantments);
		hash = mix(hash, f.heldItemInfoFilteredEnchantments.hashCode());
		return hash;
	}

	private static int mix(int hash, int value) {
		return 31 * hash + value;
	}

	private static int mix(int hash, boolean value) {
		return 31 * hash + (value ? 1 : 0);
	}

	/**
	 * 重新构建提示行。返回是否与当前缓存不同（不同时调用方应重置淡出倒计时）。
	 */
	public static boolean update(Minecraft client, ItemStack stack) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.heldItemInfo || client.level == null) {
			clear();
			return false;
		}
		FunctionConfig functions = config.functions;
		Item.TooltipContext tooltipContext = Item.TooltipContext.of(client.level);
		List<Component> newInfo;
		try {
			newInfo = HeldItemInfoTooltip.build(stack, functions, tooltipContext,
				client.font.getSplitter(), client.isTextFilteringEnabled());
		} catch (RuntimeException e) {
			TweakifyMod.LOGGER.error("Failed to build held item info for {}", stack, e);
			// 记住本次签名：坏物品不再每 tick 重试，切换物品或改配置时才重试。
			int signature = contentSignature(functions);
			clear();
			lastContentSignature = signature;
			failed = true;
			return false;
		}
		lastContentSignature = contentSignature(functions);
		failed = false;
		if (isEquivalent(lines, newInfo)) {
			return false;
		}
		lines = toLines(newInfo, client.font);
		maxWidth = -1;
		return true;
	}

	private static List<Line> toLines(List<Component> texts, Font font) {
		List<Line> result = new ArrayList<>(texts.size());
		for (Component text : texts) {
			result.add(new Line(text, font.width(text)));
		}
		return result;
	}

	private static boolean isEquivalent(List<Line> existing, List<Component> texts) {
		if (existing.size() != texts.size()) {
			return false;
		}
		Iterator<Line> lineIt = existing.iterator();
		Iterator<Component> textIt = texts.iterator();
		while (lineIt.hasNext()) {
			if (!lineIt.next().text().equals(textIt.next())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 按参考实现的排版绘制全部提示行。
	 *
	 * @param textY 原版名称的 y（guiHeight - 59）
	 * @param textColor 原版按剩余时间算好的 ARGB 颜色（含淡出透明度）
	 */
	public static void render(GuiGraphicsExtractor graphics, Font font, int textY, int textColor,
			Minecraft client, @Nullable Player cameraPlayer, int lastHealth, int displayHealth) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || lines.isEmpty()) {
			return;
		}
		FunctionConfig functions = config.functions;
		int lineHeight = functions.heldItemInfoLineHeight;

		textY -= (int) ((lineHeight - functions.heldItemInfoOffsetPerExtraLine) * (lines.size() - 1))
			+ functions.heldItemInfoVerticalOffset;

		if (functions.heldItemInfoShowName && lines.size() > 1) {
			textY -= functions.heldItemInfoItemNameSpacing;
		}

		if (functions.heldItemInfoPreventOverlap && client.gameMode != null
			&& client.gameMode.canHurtPlayer() && cameraPlayer != null) {
			textY -= getHealthBarsTotalHeight(cameraPlayer, lastHealth, displayHealth)
				- HudAccessor.getTweakifyLineHeight();
		}

		lastTooltipY = textY;
		drawBackground(graphics, textY, client, functions);

		int index = 0;
		for (Line line : lines) {
			int x = (graphics.guiWidth() - line.width()) / 2;
			graphics.text(font, line.text(), x, textY, textColor);
			textY += lineHeight;
			if (index == 0 && functions.heldItemInfoShowName) {
				textY += functions.heldItemInfoItemNameSpacing;
			}
			index++;
		}
	}

	// 动作栏消息与加高后的提示重叠时整体上移，行为与参考实现一致。
	public static void shiftOverlayMessage(GuiGraphicsExtractor graphics) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.heldItemInfoPreventOverlap || lastTooltipY < 0) {
			return;
		}
		int tooltipYOffset = graphics.guiHeight() - lastTooltipY;
		int difference = vanillaTooltipYOffset() - tooltipYOffset;
		if (difference < 0) {
			graphics.pose().translate(0, difference);
		}
	}

	private static void drawBackground(GuiGraphicsExtractor graphics, int y, Minecraft client,
			FunctionConfig functions) {
		int backgroundColor = switch (HeldItemInfoBackground.fromCode(functions.heldItemInfoBackground)) {
			case VANILLA -> client.options.getBackgroundColor(0);
			case ALWAYS -> ARGB.colorFromFloat(
				client.options.textBackgroundOpacity().get().floatValue(), 0, 0, 0);
			case NEVER -> 0;
		};
		if (ARGB.alpha(backgroundColor) == 0) {
			return;
		}
		int height = functions.heldItemInfoLineHeight * lines.size();
		if (functions.heldItemInfoShowName && lines.size() > 1) {
			height += functions.heldItemInfoItemNameSpacing;
		}
		int padding = 2;
		computeMaxWidth();
		int scaledWidth = graphics.guiWidth();
		graphics.fill(
			(scaledWidth - maxWidth) / 2 - padding,
			y - padding,
			(scaledWidth + maxWidth) / 2 + padding,
			y + height + padding,
			backgroundColor);
	}

	private static void computeMaxWidth() {
		if (maxWidth >= 0) {
			return;
		}
		maxWidth = 0;
		for (Line line : lines) {
			if (line.width() > maxWidth) {
				maxWidth = line.width();
			}
		}
	}

	// 与 Hud.extractPlayerHealth 相同的血量行高计算：多行血量（高血量/伤害吸收）时提示继续上移。
	private static int getHealthBarsTotalHeight(Player player, int lastHealth, int displayHealth) {
		float totalHalfHearts = Math.max(
				(float) player.getAttributeValue(Attributes.MAX_HEALTH),
				Math.max(lastHealth, displayHealth))
			+ Mth.ceil(player.getAbsorptionAmount());
		int rows = Mth.ceil(totalHalfHearts / 2 / HudAccessor.getTweakifyNumHeartsPerRow());
		int lineHeight = HudAccessor.getTweakifyLineHeight();
		int rowOffset = Math.max(lineHeight - (rows - 2), 3);
		return lineHeight + (rows - 1) * rowOffset;
	}

	// 淡出倒计时：参考实现按行数延长显示时间。
	public static int fadeTicks(FunctionConfig functions) {
		return (int) (SharedConstants.TICKS_PER_SECOND
			* (functions.heldItemInfoBaseFadeDuration
				+ functions.heldItemInfoFadeDurationPerExtraLine * (lines.size() - 1)));
	}
}
