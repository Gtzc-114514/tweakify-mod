package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.util.Util;

/**
 * 平滑滚动的共用开关与跨界面状态。
 *
 * <p>开关来自「通用」标签的 {@code smoothScroll}：关闭后所有界面回到原版的逐格 / 逐行滚动。
 * 配置列表、创造物品栏与聊天栏的状态保存在各自控件的 mixin 里；
 * Mod Menu 的列表控件属于可选模组，滚轮与逐帧推进分处两个 mixin，状态只能放在这里共享。
 */
public final class SmoothScrolling {
	// 聊天栏按行滚动：与原版 Font.lineHeight 一致。
	public static final int CHAT_LINE_HEIGHT = 9;

	// 创造物品栏的物品格：面板内 9 列 × 5 行、每格 18 像素，首格在 (9, 18)。
	public static final int CREATIVE_GRID_X = 9;
	public static final int CREATIVE_GRID_Y = 18;
	public static final int CREATIVE_GRID_WIDTH = 162;
	public static final int CREATIVE_GRID_HEIGHT = 90;
	public static final int CREATIVE_SLOT_SIZE = 18;
	// 物品格共 9x5 = 45 个槽位（背包/快捷栏槽位不在此范围）。
	private static final int CREATIVE_SLOT_COUNT = 45;
	// 原版悬停高亮贴图是 24x24，比槽位向四周外扩 4 像素。
	private static final int CREATIVE_HOVER_OVERHANG = 4;

	// Mod Menu 列表控件用类名判断：它是可选依赖，运行时不能直接引用类，避免未安装时类加载失败。
	private static final String MOD_MENU_LIST_CLASS = "com.terraformersmc.modmenu.gui.widget.ModListWidget";

	// 单帧最大时间步长：卡顿后一次性追上目标会显得突兀。
	private static final double MAX_FRAME_SECONDS = 0.1;

	private static final SmoothScrollAnimator MOD_MENU = new SmoothScrollAnimator();
	private static double modMenuApplied = Double.NaN;
	private static long modMenuFrameMillis;
	// 最近一次逐帧推进的时间：滚轮入口靠它确认“平滑驱动确实在跑”，否则退回原版滚动。
	private static long modMenuFrameSeenMillis;

	// 创造栏网格的当前状态：起始行、亚行像素偏移、可滚动行数。
	// 创造栏的槽位按整行摆放，滚动时把槽位整体上移 shift 像素，并额外画一行预告内容。
	private static int creativeAppliedRow;
	private static int creativeShiftPixels;
	private static int creativeRows;
	// 菜单槽位实际填进的起始行（由 ItemPickerMenu.scrollTo 上报）：-1 表示尚未记录。
	private static int creativeMenuRow = -1;

	private SmoothScrolling() {
	}

	// 配置尚未加载时按默认（开启）处理；各界面子开关都基于它。
	private static boolean enabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null || config.general.smoothScroll;
	}

	// 各界面子开关：总开关关闭时全部失效。
	public static boolean configListEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return enabled() && (config == null || config.general.smoothScrollConfigList);
	}

	public static boolean creativeEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return enabled() && (config == null || config.general.smoothScrollCreative);
	}

	public static boolean chatEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return enabled() && (config == null || config.general.smoothScrollChat);
	}

	public static boolean modMenuEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return enabled() && (config == null || config.general.smoothScrollModMenu);
	}

	// 平滑时长（秒）：配置项以毫秒暴露，夹到合法范围后作为动画器的时间常数。
	public static double timeConstantSeconds() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		int millis = config == null
			? TweakifyModConfig.DEFAULT_SMOOTH_SCROLL_DURATION
			: config.general.smoothScrollDuration;
		millis = Math.max(TweakifyModConfig.MIN_SMOOTH_SCROLL_DURATION,
			Math.min(TweakifyModConfig.MAX_SMOOTH_SCROLL_DURATION, millis));
		return millis / 1000.0;
	}

	public static boolean isModMenuList(Object widget) {
		return widget != null && MOD_MENU_LIST_CLASS.equals(widget.getClass().getName());
	}

	// 逐帧推进是否在正常运行：可选依赖的注入失败时滚轮入口必须退回原版，
	// 否则会变成“列表完全不能滚动”。
	public static boolean modMenuFrameAlive() {
		return Util.getMillis() - modMenuFrameSeenMillis < 250L;
	}

	// 创造栏每帧上报网格状态；rows = 0 表示不可滚动，网格保持原版渲染。
	public static void updateCreativeGridState(int appliedRow, int shiftPixels, int rows) {
		creativeAppliedRow = appliedRow;
		creativeShiftPixels = shiftPixels;
		creativeRows = rows;
	}

	public static void resetCreativeGridState() {
		updateCreativeGridState(0, 0, 0);
		creativeMenuRow = -1;
	}

	public static int creativeAppliedRow() {
		return creativeAppliedRow;
	}

	public static int creativeShiftPixels() {
		return creativeShiftPixels;
	}

	// 槽位内容真正从哪一行开始：与渲染用的 floor 行不一致时说明原版刚按四舍五入重填过。
	public static int creativeMenuRow() {
		return creativeMenuRow;
	}

	public static void setCreativeMenuRow(int row) {
		creativeMenuRow = row;
	}

	// 槽位对应的展示索引：网格生效时槽位内容从 menuRow 起按 9 列依次填充，可以精确算出，
	// 不再依赖“用前两个槽位反推偏移”的启发式（成员副本的重复物品会让它偏一位，图标格会闪）。
	// 返回 -1 表示当前拿不到精确值，调用方应回退到原启发式。
	public static int creativeDisplayIndex(int slotIndex) {
		return creativeMenuRow >= 0 && creativeGridActive()
			&& slotIndex >= 0 && slotIndex < CREATIVE_SLOT_COUNT
			? creativeMenuRow * 9 + slotIndex : -1;
	}

	// 悬停高亮绘制时的裁剪上边界：第一行固定放开 4 像素（高亮相对槽位的外扩量），
	// 不随亚行偏移继续上移，避免滚到较深半行时压到面板顶部的标签标题；
	// 其余行仍按网格上沿裁剪。
	public static int hoverHighlightClipTop(int slotY) {
		return slotY > CREATIVE_GRID_Y ? CREATIVE_GRID_Y : CREATIVE_GRID_Y - CREATIVE_HOVER_OVERHANG;
	}

	// 创造栏是否用自绘网格（可滚动且子开关开启）。
	public static boolean creativeGridActive() {
		return creativeRows > 0 && creativeEnabled();
	}

	// 屏幕坐标是否落在创造栏物品格内：只有格内才需要亚行偏移，
	// 背包/快捷栏/盔甲槽必须保持原样，否则点击会错位到相邻槽位。
	public static boolean insideCreativeGrid(double mouseX, double mouseY, int leftPos, int topPos) {
		return mouseX >= leftPos + CREATIVE_GRID_X
			&& mouseX < leftPos + CREATIVE_GRID_X + CREATIVE_GRID_WIDTH
			&& mouseY >= topPos + CREATIVE_GRID_Y
			&& mouseY < topPos + CREATIVE_GRID_Y + CREATIVE_GRID_HEIGHT;
	}

	// 把连续的“行位置”换算成起始行 + 亚行像素偏移。
	// 四舍五入把偏移推到整行时进位，保证偏移始终落在 [0, slotSize)。
	public static CreativeScrollLayout creativeLayout(double position, int slotSize) {
		double clamped = Math.max(0.0, position);
		int row = (int) Math.floor(clamped + 1.0e-6);
		int shift = (int) Math.round((clamped - row) * slotSize);
		if (shift >= slotSize) {
			row++;
			shift = 0;
		}
		return new CreativeScrollLayout(Math.max(0, row), Math.max(0, Math.min(slotSize - 1, shift)));
	}

	public record CreativeScrollLayout(int row, int shiftPixels) {
	}

	// 分组展开/收起后恢复顶部行：把当前亚行偏移（fraction，单位：行）一并带过去，
	// 否则平滑滚动下停在半行时展开分组，整片物品格会向上跳最多一行。
	public static float restoredScrollPosition(int topRow, double fraction, int rows) {
		if (rows <= 0) {
			return 0.0f;
		}
		double kept = Double.isFinite(fraction) ? Math.max(0.0, Math.min(0.999, fraction)) : 0.0;
		int row = Math.max(0, Math.min(topRow, rows));
		return (float) ((row + kept) / (double) rows);
	}

	// Mod Menu 滚轮：只累加目标位置，真正的滚动由列表每帧渲染时写回。
	public static void modMenuScrollBy(double deltaPixels, double maxPixels) {
		MOD_MENU.addDelta(deltaPixels, maxPixels);
	}

	// Mod Menu 每帧推进：外部改过滚动值（滚动记忆恢复、键盘翻页、拖动滚动条）时立即对齐。
	public static void updateModMenu(double currentAmount, double maxPixels, java.util.function.DoubleConsumer setter) {
		double dt = frameDeltaSeconds();
		if (!modMenuEnabled()) {
			resetModMenu();
			return;
		}
		modMenuFrameSeenMillis = Util.getMillis();
		MOD_MENU.setTimeConstant(timeConstantSeconds());
		// 首次推进（或外部改过滚动值，如滚动记忆恢复、键盘翻页）时先对齐，
		// 否则会把列表拉回 0 把刚恢复的位置覆盖掉。
		if (Double.isNaN(modMenuApplied) || Math.abs(currentAmount - modMenuApplied) > 1.0e-3) {
			MOD_MENU.jumpTo(currentAmount, maxPixels);
		}
		MOD_MENU.update(dt, maxPixels);
		double value = MOD_MENU.position();
		if (value != modMenuApplied) {
			modMenuApplied = value;
			setter.accept(value);
		}
	}

	public static void resetModMenu() {
		MOD_MENU.jumpTo(0.0, 0.0);
		modMenuApplied = Double.NaN;
		modMenuFrameMillis = 0L;
		modMenuFrameSeenMillis = 0L;
	}

	// 帧间隔（秒）：首帧或长时间未渲染时按 0 处理，避免一次性跳过整段动画。
	private static double frameDeltaSeconds() {
		long now = Util.getMillis();
		double dt = modMenuFrameMillis == 0L
			? 0.0
			: Math.min(MAX_FRAME_SECONDS, (now - modMenuFrameMillis) / 1000.0);
		modMenuFrameMillis = now;
		return dt;
	}
}
