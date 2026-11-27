package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 探针与 {@code require = 0} 注入点的配平校验。
 *
 * <p>{@code require = 0} 的注入点被其它模组改写目标方法后会静默失效，只能靠
 * {@code Mixin hook active: <探针名>} 日志确认存活（见 {@code docs/mixin-probes.md}）。
 * 本测试锁定四类约定：
 * <ul>
 *   <li><b>逐注入点</b>：每个 {@code require = 0} 注解对应的 handler 方法体内必须有
 *       {@code HookProbe.markOnce} 调用，而不只是文件级计数配平；</li>
 *   <li><b>探针名全局唯一</b>：扫描整个 {@code src/client/java}（含 helper 类），
 *       {@code HookProbe.REPORTED} 是共享集合，重名会让后执行的钩子静默不打印；</li>
 *   <li><b>字段与名字一一对应</b>：{@code markOnce(AtomicBoolean, String)} 的去重键是字段，
 *       两个名字共用一个字段同样会让后者永不打印；声明与使用必须完全一致；</li>
 *   <li><b>例外不过期</b>：handler 内无探针、一注多探针、探针挂在 {@code require >= 1}
 *       注入点上的文件集中登记，实际计数与登记不符时失败。</li>
 * </ul>
 *
 * <p>计数前先做轻量词法剥离（注释与字符串），避免注释或字符串里的
 * {@code require = 0} / {@code HookProbe.markOnce(} 干扰结果。新增注入点后请同步
 * {@code docs/mixin-probes.md} 的「当前实测」块；本测试只锁关系与例外，不锁总数。
 */
class MixinProbeCoverageTest {
	private static final Path CLIENT_ROOT = Path.of("src/client/java");
	private static final Path MIXIN_ROOT = CLIENT_ROOT.resolve("com/gtzc/tweakify/client/mixin");
	private static final String PROBE_CALL = "HookProbe.markOnce(";

	private static final Pattern REQUIRE_ZERO = Pattern.compile("require\\s*=\\s*0");
	private static final Pattern INJECTION_ANNOTATION = Pattern.compile(
		"@(?:Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyReturnValue"
			+ "|ModifyExpressionValue|WrapOperation|WrapWithCondition|WrapMethod)\\b");
	// 两种重载：markOnce("name") 与 markOnce(FIELD, "name")；第二个分支同时捕获字段名。
	private static final Pattern PROBE_CALL_SHAPE = Pattern.compile(
		"HookProbe\\.markOnce\\(\\s*(?:\"([^\"]+)\"|([\\w$]+),\\s*\"([^\"]+)\")");
	private static final Pattern PROBE_FIELD_DECL = Pattern.compile(
		"AtomicBoolean\\s+(TWEAKIFY\\$[A-Z0-9_]+_PROBE)\\s*=");
	private static final Pattern ANY_PROBE_FIELD_DECL = Pattern.compile(
		"AtomicBoolean\\s+(TWEAKIFY\\$[A-Z0-9_]+)\\s*=");
	private static final Pattern METHOD_NAME = Pattern.compile("([\\w$]+)\\s*\\(");
	private static final String METHOD_END = "\t}";

	// 文件级布局例外：文件 -> {非注释 require = 0 数, markOnce 数}。
	// 与 docs/mixin-probes.md「当前共 5 个文件不配平」一一对应；登记值与实际不符即失败。
	private static final Map<String, int[]> LAYOUT_EXCEPTIONS = new LinkedHashMap<>();

	// require = 0 注入点的 handler 方法体内没有探针：探针在 helper 类中。
	// 键格式：文件名#方法名。
	private static final Set<String> PROBES_IN_HELPER = Set.of(
		"ItemStackEnchantmentTooltipMixin.java#tweakify$appendEnchantmentDescriptions");

	// helper 中的探针仍需参与全局唯一性校验；登记其所在文件，防止探针被删后测试仍绿。
	private static final Map<String, String> HELPER_PROBES = Map.of(
		"enchantmentDescriptions", "EnchantmentTooltipDescriptions.java");

	static {
		LAYOUT_EXCEPTIONS.put("ItemStackEnchantmentTooltipMixin.java", new int[]{1, 0});
		LAYOUT_EXCEPTIONS.put("PotionContentsTooltipCategoryMixin.java", new int[]{1, 2});
		LAYOUT_EXCEPTIONS.put("CreateWorldScreenExperimentalPackMixin.java", new int[]{0, 1});
		LAYOUT_EXCEPTIONS.put("LocatorBarHeadsMixin.java", new int[]{0, 1});
		LAYOUT_EXCEPTIONS.put("WorldOpenFlowsExperimentalWarningMixin.java", new int[]{0, 1});
	}

	@Test
	void everyRequireZeroInjectionHasAProbeInItsHandler() throws IOException {
		List<String> failures = new ArrayList<>();
		List<String> staleExceptions = new ArrayList<>();
		Set<String> visitedExceptions = new LinkedHashSet<>();
		List<String> deltas = new ArrayList<>();
		int requireZeroCount = 0;
		int probeCount = 0;

		for (Path file : listJavaFiles(MIXIN_ROOT)) {
			String name = file.getFileName().toString();
			String code = stripSource(Files.readString(file, StandardCharsets.UTF_8), false);
			int injections = countRequireZero(code);
			int probes = countOccurrences(code, PROBE_CALL);
			requireZeroCount += injections;
			probeCount += probes;
			deltas.add(name + ": " + injections + "/" + probes);

			int[] registered = LAYOUT_EXCEPTIONS.get(name);
			if (registered != null) {
				visitedExceptions.add(name);
				if (registered[0] != injections || registered[1] != probes) {
					staleExceptions.add(name + " 登记为 " + registered[0] + "/" + registered[1]
						+ "，实际为 " + injections + "/" + probes);
				}
			} else if (probes < injections) {
				failures.add(name + ": require=0 " + injections + " 个，探针 " + probes + " 个");
			} else if (injections == 0 && probes > 0) {
				failures.add(name + ": 没有 require=0 注入点却有探针（新增额外探针请登记例外）");
			}

			for (InjectionSite site : requireZeroSites(code)) {
				String key = name + "#" + site.method();
				if (PROBES_IN_HELPER.contains(key)) {
					continue;
				}
				if (site.probes() < 0) {
					failures.add(key + ": 无法解析 handler 方法体（检查缩进格式或更新解析逻辑）");
				} else if (site.probes() == 0) {
					failures.add(key + ": require=0 注入点的方法体内没有探针");
				}
			}
		}

		assertTrue(failures.isEmpty(),
			"存在没有探针的 require=0 注入点（或未登记的额外探针）：\n"
				+ String.join("\n", failures) + "\n各文件 require=0/探针：" + String.join("，", deltas));
		assertTrue(staleExceptions.isEmpty(),
			"探针例外登记已过期，请更新 MixinProbeCoverageTest：\n" + String.join("\n", staleExceptions));
		assertEquals(LAYOUT_EXCEPTIONS.keySet(), visitedExceptions,
			"例外清单包含不存在或未被扫描到的文件：" + LAYOUT_EXCEPTIONS.keySet());

		int expectedDelta = 0;
		for (int[] registered : LAYOUT_EXCEPTIONS.values()) {
			expectedDelta += registered[1] - registered[0];
		}
		assertEquals(expectedDelta, probeCount - requireZeroCount,
			"探针与注入点的差额与例外清单不一致：require=0 " + requireZeroCount + "，探针 " + probeCount
				+ "；各文件：" + String.join("，", deltas));
	}

	@Test
	void probeNamesAndFieldsAreConsistent() throws IOException {
		Map<String, String> nameOwners = new LinkedHashMap<>();
		Map<String, Set<String>> fieldNames = new LinkedHashMap<>();
		List<String> duplicateNames = new ArrayList<>();
		List<String> reusedFields = new ArrayList<>();
		Set<String> declaredFields = new LinkedHashSet<>();
		Set<String> misnamedFields = new LinkedHashSet<>();
		int callCount = 0;
		int parsedCount = 0;

		for (Path file : listJavaFiles(CLIENT_ROOT)) {
			String name = file.getFileName().toString();
			String raw = Files.readString(file, StandardCharsets.UTF_8);
			callCount += countOccurrences(stripSource(raw, false), PROBE_CALL);

			// 保留字符串、去掉注释：名字与字段声明的提取都以这份文本为准。
			String withoutComments = stripSource(raw, true);
			Matcher anyField = ANY_PROBE_FIELD_DECL.matcher(withoutComments);
			while (anyField.find()) {
				declaredFields.add(name + "#" + anyField.group(1));
				if (!anyField.group(1).endsWith("_PROBE")) {
					misnamedFields.add(name + ": " + anyField.group(1));
				}
			}
			Matcher probe = PROBE_CALL_SHAPE.matcher(withoutComments);
			while (probe.find()) {
				parsedCount++;
				String probeName = probe.group(1) != null ? probe.group(1) : probe.group(3);
				String previous = nameOwners.putIfAbsent(probeName, name);
				if (previous != null) {
					duplicateNames.add(probeName + " 同时出现在 " + previous + " 与 " + name);
				}
				String field = probe.group(2);
				if (field != null) {
					// 字段属于具体的 mixin 类：不同类可以各自声明同名静态字段（如两个 TICK_PROBE）。
					fieldNames.computeIfAbsent(name + "#" + field, key -> new LinkedHashSet<>()).add(probeName);
				}
			}
		}

		assertTrue(duplicateNames.isEmpty(),
			"探针名必须全局唯一（重名会让后执行的钩子静默不打印）：\n"
				+ String.join("\n", duplicateNames));
		assertTrue(misnamedFields.isEmpty(),
			"AtomicBoolean 探针字段应以 _PROBE 结尾：\n" + String.join("\n", misnamedFields));
		assertEquals(callCount, parsedCount,
			"存在无法解析的 markOnce 调用（拼接、常量名或静态导入），请改回字面量全限定调用");
		for (Map.Entry<String, Set<String>> entry : fieldNames.entrySet()) {
			if (entry.getValue().size() > 1) {
				reusedFields.add(entry.getKey() + " 被多个探针名复用：" + entry.getValue());
			}
		}
		assertTrue(reusedFields.isEmpty(),
			"AtomicBoolean 探针字段只能对应一个名字（复用会让后一个探针永不打印）：\n"
				+ String.join("\n", reusedFields));
		assertEquals(declaredFields, fieldNames.keySet(),
			"探针字段声明与使用不一致（声明未使用或使用未声明）：声明 " + declaredFields
				+ "，使用 " + fieldNames.keySet());
		for (Map.Entry<String, String> helper : HELPER_PROBES.entrySet()) {
			assertTrue(nameOwners.containsKey(helper.getKey()),
				"helper 探针已丢失：" + helper.getKey() + " 应出现在 " + helper.getValue());
		}
	}

	private record InjectionSite(String method, int probes) {
	}

	// 解析每个 require = 0 注入注解及其 handler 方法体，统计方法体内的探针数。
	// 依赖项目统一格式：方法体以含 { 的行开始、以单 tab 的 } 行结束。
	// 解析失败记 probes = -1，由调用方当成失败处理，避免格式漂移后退化为静默漏检。
	private static List<InjectionSite> requireZeroSites(String code) {
		List<InjectionSite> sites = new ArrayList<>();
		String[] lines = code.split("\\R", -1);
		for (int i = 0; i < lines.length; i++) {
			if (!INJECTION_ANNOTATION.matcher(lines[i]).find()) {
				continue;
			}
			int end = annotationEnd(lines, i);
			if (end < 0) {
				sites.add(new InjectionSite("?", -1));
				continue;
			}
			String block = String.join("\n", Arrays.copyOfRange(lines, i, end + 1));
			if (!REQUIRE_ZERO.matcher(block).find()) {
				i = end;
				continue;
			}
			int bodyStart = -1;
			for (int j = end + 1; j < lines.length; j++) {
				String trimmed = lines[j].trim();
				if (trimmed.endsWith("{")) {
					bodyStart = j;
					break;
				}
				if (trimmed.endsWith(";")) {
					break;
				}
			}
			if (bodyStart < 0) {
				sites.add(new InjectionSite("?(body)", -1));
				i = end;
				continue;
			}
			int bodyEnd = -1;
			for (int j = bodyStart + 1; j < lines.length; j++) {
				if (lines[j].equals(METHOD_END)) {
					bodyEnd = j;
					break;
				}
			}
			if (bodyEnd < 0) {
				sites.add(new InjectionSite("?", -1));
				i = bodyStart;
				continue;
			}
			String declaration = String.join(" ", Arrays.copyOfRange(lines, end + 1, bodyStart + 1));
			String body = String.join("\n", Arrays.copyOfRange(lines, bodyStart, bodyEnd + 1));
			sites.add(new InjectionSite(methodName(declaration), countOccurrences(body, PROBE_CALL)));
			i = bodyEnd;
		}
		return sites;
	}

	// 注解参数可能跨行且嵌套括号；字符串已被剥离，括号只来自代码本身。
	private static int annotationEnd(String[] lines, int start) {
		int depth = 0;
		for (int i = start; i < lines.length; i++) {
			depth += countOccurrences(lines[i], "(") - countOccurrences(lines[i], ")");
			if (depth <= 0) {
				return i;
			}
		}
		return -1;
	}

	private static String methodName(String declaration) {
		Matcher matcher = METHOD_NAME.matcher(declaration);
		String name = "?";
		while (matcher.find()) {
			name = matcher.group(1);
		}
		return name;
	}

	private static int countRequireZero(String code) {
		Matcher matcher = REQUIRE_ZERO.matcher(code);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	// 轻量词法剥离：keepStrings 为 true 时保留字符串内容（提取探针名用），
	// 为 false 时连字符串内容一起删除（计数用，避免字符串里的同名字样干扰）。
	private static String stripSource(String source, boolean keepStrings) {
		StringBuilder out = new StringBuilder(source.length());
		int i = 0;
		while (i < source.length()) {
			char c = source.charAt(i);
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
				while (i < source.length() && source.charAt(i) != '\n') {
					i++;
				}
				continue;
			}
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < source.length()
					&& !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
					if (source.charAt(i) == '\n') {
						out.append('\n');
					}
					i++;
				}
				i = Math.min(source.length(), i + 2);
				continue;
			}
			if (c == '"' || c == '\'') {
				int start = i;
				i++;
				while (i < source.length()) {
					char inner = source.charAt(i);
					if (inner == '\\') {
						i += 2;
						continue;
					}
					if (inner == c) {
						i++;
						break;
					}
					if (inner == '\n') {
						break;
					}
					i++;
				}
				if (keepStrings) {
					out.append(source, start, Math.min(i, source.length()));
				} else {
					out.append(c).append(c);
				}
				continue;
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}

	private static int countOccurrences(String source, String needle) {
		return source.split(Pattern.quote(needle), -1).length - 1;
	}

	private static List<Path> listJavaFiles(Path root) throws IOException {
		assertTrue(Files.isDirectory(root), "源码目录不存在：" + root.toAbsolutePath());
		try (Stream<Path> files = Files.walk(root)) {
			List<Path> result = files.filter(path -> path.toString().endsWith(".java")).toList();
			assertFalse(result.isEmpty(), "未扫描到任何 Java 源文件（疑似空目录或稀疏检出）："
				+ root.toAbsolutePath());
			return result;
		}
	}
}
