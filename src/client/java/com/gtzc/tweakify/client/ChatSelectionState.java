package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.ChatComponentAccessor;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Mth;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天框文字框选：在聊天界面按住左键拖动选择消息文字，Ctrl+C 复制。
 * 行布局与 ChatComponent 保持一致：消息从底部向上排列，行高随聊天行距设置变化。
 */
public final class ChatSelectionState {
	// 与 ChatComponent 内部布局一致：文本缩进 4，底部给输入框留 40，基础行高 9。
	private static final int MESSAGE_INDENT = 4;
	private static final int BOTTOM_MARGIN = 40;
	private static final int BASE_LINE_HEIGHT = 9;
	private static final int SELECTION_COLOR = 0x5533AAFF;

	private static boolean selecting;
	private static boolean hasSelection;
	private static double startX;
	private static double startY;
	private static double endX;
	private static double endY;

	// 行文本与前缀宽度缓存：拖动选择时每帧只查表，不再重复测量字体。
	// 上限避免聊天框长时间开着、反复换行时旧 Line 对象一直留在强引用缓存里。
	private static final Map<GuiMessage.Line, LineMetrics> LINE_CACHE = new IdentityHashMap<>();
	private static final int MAX_LINE_CACHE_ENTRIES = 512;

	private ChatSelectionState() {
	}

	// 由 ChatScreen.mouseClicked 调用：只处理聊天消息区域，输入框区域交给原版。
	public static void begin(double x, double y) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.chatTextSelection) {
			clear();
			return;
		}
		int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		if (y >= guiHeight - 14) {
			clear();
			return;
		}
		selecting = true;
		hasSelection = true;
		startX = x;
		startY = y;
		endX = x;
		endY = y;
	}

	// 每帧由 ChatScreen.extractRenderState 调用：左键松开时结束拖动，选择结果保留。
	public static void updateFromMouse(Minecraft client, int mouseX, int mouseY) {
		if (!selecting) {
			return;
		}
		if (!client.mouseHandler.isLeftPressed()) {
			selecting = false;
			// 松开鼠标时自动复制选中文字（可选）。
			TweakifyModConfig config = TweakifyMod.CONFIG;
			if (config != null && config.chatSelectionAutoCopy) {
				String text = selectedText(client);
				if (!text.isEmpty()) {
					client.keyboardHandler.setClipboard(text);
				}
			}
			return;
		}
		endX = mouseX;
		endY = mouseY;
	}

	public static void clear() {
		// 功能关闭时每帧都会走到这里：没有状态可清时直接返回，省掉一次缓存清理。
		if (!selecting && !hasSelection && LINE_CACHE.isEmpty()) {
			return;
		}
		selecting = false;
		hasSelection = false;
		LINE_CACHE.clear();
	}

	public static boolean hasSelection() {
		return hasSelection && (startX != endX || startY != endY);
	}

	// 复制选中的聊天文本；没有有效选择时返回空字符串。
	public static String selectedText(Minecraft client) {
		if (!hasSelection()) {
			return "";
		}
		ChatComponent chat = client.gui.hud.getChat();
		List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).tweakify$getTrimmedMessages();
		int scroll = ((ChatComponentAccessor) chat).tweakify$getChatScrollbarPos();
		int visibleLines = Math.min(lines.size() - scroll, chat.getLinesPerPage());
		if (visibleLines <= 0) {
			return "";
		}
		Layout layout = layout(client);
		Selection first = pointAt(client, layout, lines, scroll, visibleLines, startX, startY);
		Selection second = pointAt(client, layout, lines, scroll, visibleLines, endX, endY);
		Selection top = isBefore(first, second) ? first : second;
		Selection bottom = top == first ? second : first;

		StringBuilder result = new StringBuilder();
		boolean firstLine = true;
		for (int row = top.row(); row >= bottom.row(); row--) {
			String text = metrics(lines.get(scroll + row), client.font).text();
			int from = row == top.row() ? top.character() : 0;
			int to = row == bottom.row() ? bottom.character() : text.length();
			from = Mth.clamp(from, 0, text.length());
			to = Mth.clamp(to, 0, text.length());
			if (from > to) {
				int swap = from;
				from = to;
				to = swap;
			}
			if (!firstLine) {
				result.append('\n');
			}
			firstLine = false;
			result.append(text, from, to);
		}
		return result.toString();
	}

	// 以半透明色覆盖选中区域；在聊天文本之后绘制，文字仍然可读。
	public static void render(GuiGraphicsExtractor gui, Minecraft client) {
		if (!hasSelection()) {
			return;
		}
		ChatComponent chat = client.gui.hud.getChat();
		List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).tweakify$getTrimmedMessages();
		int scroll = ((ChatComponentAccessor) chat).tweakify$getChatScrollbarPos();
		int visibleLines = Math.min(lines.size() - scroll, chat.getLinesPerPage());
		if (visibleLines <= 0) {
			return;
		}
		Layout layout = layout(client);
		Selection first = pointAt(client, layout, lines, scroll, visibleLines, startX, startY);
		Selection second = pointAt(client, layout, lines, scroll, visibleLines, endX, endY);
		Selection top = isBefore(first, second) ? first : second;
		Selection bottom = top == first ? second : first;

		for (int row = top.row(); row >= bottom.row(); row--) {
			LineMetrics line = metrics(lines.get(scroll + row), client.font);
			String text = line.text();
			int from = row == top.row() ? top.character() : 0;
			int to = row == bottom.row() ? bottom.character() : text.length();
			from = Mth.clamp(from, 0, text.length());
			to = Mth.clamp(to, 0, text.length());
			if (from > to) {
				int swap = from;
				from = to;
				to = swap;
			}
			double left = layout.textLeft() + line.widths()[from] * layout.scale();
			double right = layout.textLeft() + line.widths()[to] * layout.scale();
			double rowBottom = layout.bottom() - row * layout.rowHeight();
			double rowTop = rowBottom - layout.rowHeight();
			if (right > left) {
				gui.fill((int) Math.floor(left), (int) Math.floor(rowTop),
					(int) Math.ceil(right), (int) Math.ceil(rowBottom), selectionColor());
			}
		}
	}

	// 框选颜色由配置决定，读取失败时回退到默认蓝色。
	private static int selectionColor() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null ? SELECTION_COLOR
			: ChatSelectionColor.fromCode(config.chatSelectionColor).argb();
	}

	private record Layout(double scale, int bottom, double rowHeight, double textLeft) {
	}

	private record Selection(int row, int character) {
	}

	private record LineMetrics(String text, int[] widths) {
	}

	// 视觉顺序：屏幕上方（行号更大）在前；同一行内字符索引小的在前。
	private static boolean isBefore(Selection first, Selection second) {
		return first.row() > second.row()
			|| (first.row() == second.row() && first.character() <= second.character());
	}

	private static Layout layout(Minecraft client) {
		double scale = client.options.chatScale().get();
		int guiHeight = client.getWindow().getGuiScaledHeight();
		double lineSpacing = client.options.chatLineSpacing().get();
		int lineHeight = (int) (BASE_LINE_HEIGHT * (lineSpacing + 1.0));
		return new Layout(scale, guiHeight - BOTTOM_MARGIN, lineHeight * scale,
			MESSAGE_INDENT * scale);
	}

	private static Selection pointAt(Minecraft client, Layout layout, List<GuiMessage.Line> lines,
									 int scroll, int visibleLines, double x, double y) {
		int row = Mth.clamp((int) Math.floor((layout.bottom() - y) / layout.rowHeight()),
			0, visibleLines - 1);
		LineMetrics line = metrics(lines.get(scroll + row), client.font);
		double localX = (x - layout.textLeft()) / layout.scale();
		return new Selection(row, charIndexAt(line.widths(), line.text(), localX));
	}

	// 鼠标 x 对应的字符插入位置：按前缀宽度缓存查表，落在字符左半时取前一个位置。
	private static int charIndexAt(int[] widths, String text, double localX) {
		if (localX <= 0) {
			return 0;
		}
		int index = 0;
		while (index < text.length()) {
			int codePoint = text.codePointAt(index);
			int charCount = Character.charCount(codePoint);
			int start = widths[index];
			int end = widths[index + charCount];
			if (localX < start + (end - start) / 2.0) {
				return index;
			}
			index += charCount;
		}
		return text.length();
	}

	// 行文本只解析一次，同时预先计算每个字符边界的前缀宽度。
	private static LineMetrics metrics(GuiMessage.Line line, Font font) {
		LineMetrics cached = LINE_CACHE.get(line);
		if (cached != null) {
			return cached;
		}
		String text = lineText(line);
		int[] widths = new int[text.length() + 1];
		int width = 0;
		int index = 0;
		while (index < text.length()) {
			int codePoint = text.codePointAt(index);
			int charCount = Character.charCount(codePoint);
			widths[index] = width;
			width += font.width(new String(Character.toChars(codePoint)));
			index += charCount;
			widths[index] = width;
		}
		LineMetrics metrics = new LineMetrics(text, widths);
		if (LINE_CACHE.size() >= MAX_LINE_CACHE_ENTRIES) {
			LINE_CACHE.clear();
		}
		LINE_CACHE.put(line, metrics);
		return metrics;
	}

	private static String lineText(GuiMessage.Line line) {
		StringBuilder text = new StringBuilder();
		line.content().accept((index, style, codePoint) -> {
			text.appendCodePoint(codePoint);
			return true;
		});
		return text.toString();
	}
}
