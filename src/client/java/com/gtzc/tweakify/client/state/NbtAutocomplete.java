package com.gtzc.tweakify.client.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * NBT 自动补全的上下文解析（功能标签）：判断命令输入框的光标是否位于
 * 物品组件方括号 {@code [...]} 或 NBT 花括号 {@code {...}} 内的「键名」位置。
 *
 * <p>只做纯文本解析，不依赖 Minecraft 类型，便于单测覆盖：
 * 括号嵌套、引号转义、值位置（{@code :} / {@code =} 之后）、已是完整物品 ID
 * 与选择器 {@code @e[...]} 的排除都由这里决定，建议项的数据源由调用方提供。
 */
public final class NbtAutocomplete {
	public enum Context {
		/** 不在可补全位置。 */
		NONE,
		/** 物品组件方括号内：补全数据组件 ID。 */
		COMPONENTS,
		/** NBT 花括号内：补全常用 NBT 键名。 */
		NBT
	}

	public record Analysis(Context context, int wordStart, String prefix, Set<String> usedKeys) {
		private static final Analysis NONE = new Analysis(Context.NONE, 0, "", Set.of());

		public boolean isActive() {
			return this.context != Context.NONE;
		}
	}

	private record Container(char kind, int index) {
	}

	private NbtAutocomplete() {
	}

	// 解析光标处的补全上下文；不在命令里、不在容器内或处于值位置时返回非活动结果。
	public static Analysis analyze(String text, int cursor) {
		if (text == null || cursor <= 0 || cursor > text.length()) {
			return Analysis.NONE;
		}
		int firstNonSpace = 0;
		while (firstNonSpace < text.length() && Character.isWhitespace(text.charAt(firstNonSpace))) {
			firstNonSpace++;
		}
		// 只有聊天栏的命令输入（以 / 开头）才补全，避免影响普通聊天文本。
		if (firstNonSpace >= text.length() || text.charAt(firstNonSpace) != '/') {
			return Analysis.NONE;
		}

		Deque<Container> stack = new ArrayDeque<>();
		char quote = 0;
		for (int index = 0; index < cursor; index++) {
			char c = text.charAt(index);
			if (quote != 0) {
				if (c == '\\') {
					index++;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}
			if (c == '\'' || c == '"') {
				quote = c;
			} else if (c == '{' || c == '[') {
				stack.push(new Container(c, index));
			} else if ((c == '}' || c == ']') && !stack.isEmpty()) {
				stack.pop();
			}
		}
		if (stack.isEmpty()) {
			return Analysis.NONE;
		}
		Container container = stack.peek();

		int wordStart = cursor;
		while (wordStart > container.index() + 1 && isKeyChar(text.charAt(wordStart - 1))) {
			wordStart--;
		}
		// 值位置：键已经写完（前面是 : / = / 引号）时不再提示键名。
		int probe = wordStart - 1;
		while (probe > container.index() && Character.isWhitespace(text.charAt(probe))) {
			probe--;
		}
		if (probe > container.index()) {
			char previous = text.charAt(probe);
			if (previous == ':' || previous == '=' || previous == '\'' || previous == '"') {
				return Analysis.NONE;
			}
		}
		String prefix = text.substring(wordStart, cursor);
		Context context = container.kind() == '['
			? componentsContext(text, container.index()) : Context.NBT;
		if (context == Context.NONE) {
			return Analysis.NONE;
		}
		// 当前键值段里已经出现分隔符（组件用 =、NBT 用 :）时，光标在值里而不是键名里。
		if (hasValueSeparator(text, container.index(), cursor, context)) {
			return Analysis.NONE;
		}
		return new Analysis(context, wordStart, prefix,
			usedKeys(text, container.index(), wordStart, context));
	}

	// 方括号前必须是一个物品/方块 ID；选择器 @e[...]、@p[...] 等不参与组件补全。
	private static Context componentsContext(String text, int bracketIndex) {
		int probe = bracketIndex - 1;
		while (probe >= 0 && Character.isWhitespace(text.charAt(probe))) {
			probe--;
		}
		int end = probe;
		while (probe >= 0 && (isKeyChar(text.charAt(probe)) || text.charAt(probe) == '@')) {
			probe--;
		}
		if (end < 0 || probe == end) {
			return Context.NONE;
		}
		String token = text.substring(probe + 1, end + 1);
		return token.charAt(0) == '@' ? Context.NONE : Context.COMPONENTS;
	}

	// 当前键值段里是否已出现分隔符：顶层逗号重置，嵌套容器与引号内忽略。
	private static boolean hasValueSeparator(String text, int containerStart, int cursor, Context context) {
		char separator = context == Context.COMPONENTS ? '=' : ':';
		int depth = 0;
		char quote = 0;
		boolean seen = false;
		for (int index = containerStart + 1; index < cursor; index++) {
			char c = text.charAt(index);
			if (quote != 0) {
				if (c == '\\') {
					index++;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}
			if (c == '\'' || c == '"') {
				quote = c;
			} else if (c == '{' || c == '[') {
				depth++;
			} else if (c == '}' || c == ']') {
				depth--;
			} else if (depth == 0 && c == ',') {
				seen = false;
			} else if (depth == 0 && c == separator) {
				seen = true;
			}
		}
		return seen;
	}

	// 当前容器里已经写过的键：按顶层逗号切分，每段取分隔符之前的部分。
	private static Set<String> usedKeys(String text, int containerStart, int wordStart, Context context) {
		Set<String> used = new LinkedHashSet<>();
		int depth = 0;
		int segmentStart = containerStart + 1;
		char quote = 0;
		for (int index = containerStart + 1; index < wordStart; index++) {
			char c = text.charAt(index);
			if (quote != 0) {
				if (c == '\\') {
					index++;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}
			if (c == '\'' || c == '"') {
				quote = c;
			} else if (c == '{' || c == '[') {
				depth++;
			} else if (c == '}' || c == ']') {
				depth--;
			} else if (c == ',' && depth == 0) {
				addKey(used, text.substring(segmentStart, index), context);
				segmentStart = index + 1;
			}
		}
		addKey(used, text.substring(segmentStart, wordStart), context);
		return used;
	}

	private static void addKey(Set<String> used, String segment, Context context) {
		char separator = context == Context.COMPONENTS ? '=' : ':';
		int end = segment.indexOf(separator);
		String key = (end >= 0 ? segment.substring(0, end) : segment).trim();
		if (key.isEmpty()) {
			return;
		}
		used.add(key);
		used.add(shortName(key));
	}

	// 过滤建议：前缀匹配大小写不敏感（SNBT 键名区分大小写，但输入习惯不应受限），
	// 插入时仍使用候选的规范大小写；同时排除当前容器里写过的键。
	public static List<String> filter(Collection<String> candidates, Analysis analysis) {
		List<String> result = new ArrayList<>();
		if (!analysis.isActive() || candidates == null) {
			return result;
		}
		Set<String> seen = new LinkedHashSet<>();
		for (String candidate : candidates) {
			if (candidate == null || candidate.isEmpty() || !startsWithIgnoreCase(candidate, analysis.prefix())) {
				continue;
			}
			if (analysis.usedKeys().contains(candidate) || analysis.usedKeys().contains(shortName(candidate))) {
				continue;
			}
			if (seen.add(candidate)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private static boolean startsWithIgnoreCase(String value, String prefix) {
		return prefix.isEmpty() || value.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static String shortName(String key) {
		int colon = key.indexOf(':');
		return colon >= 0 ? key.substring(colon + 1) : key;
	}

	// 键名字符：资源位置允许的字符；遇到 : 仍继续，便于补全「已输入命名空间」的组件 ID。
	private static boolean isKeyChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':'
			|| c == '/' || c == '#';
	}
}
