package com.gtzc.tweakify.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mixin 配置清单与实际源码的双向一致性。
 *
 * <p>mixin 的登记全靠手写 json，两种漏登记都只在实机才暴露：
 * 源码写了 {@code @Mixin} 但没登记时注入不生效且没有任何报错；
 * 登记了但类被改名/删除时客户端启动直接崩溃。这里在构建期锁死：
 * <ul>
 *   <li>json 登记的每个类都必须在对应包目录下存在且声明了 {@code @Mixin}；</li>
 *   <li>包目录下每个声明 {@code @Mixin} 的类都必须被对应 json 登记；</li>
 *   <li>json 的 {@code package} 必须能在 {@code src/client/java} 下找到目录；</li>
 *   <li>配置必须被 {@code fabric.mod.json} 的 {@code mixins} 列表引用，否则不会被加载。</li>
 * </ul>
 * 插件类（{@code MiniHudMixinPlugin} / {@code ModMenuMixinPlugin}）不声明 {@code @Mixin}，
 * 只由 {@code plugin} 字段引用，不参与类清单比对。
 */
class MixinConfigManifestTest {
	private static final Path SOURCE_ROOT = Path.of("src/client/java");
	private static final Path FABRIC_MOD_JSON = Path.of("src/main/resources/fabric.mod.json");
	// 注释或字符串里出现 @Mixin 不算声明：只认行首（允许缩进）的注解。
	private static final Pattern MIXIN_ANNOTATION = Pattern.compile("(?m)^\\s*@Mixin\\b");

	// 三份清单各自独立：主配置 required=true，两份可选配置只在本体模组存在时启用。
	private static final List<Manifest> MANIFESTS = List.of(
		new Manifest(Path.of("src/client/resources/tweakify-mod.client.mixins.json"), SOURCE_ROOT),
		new Manifest(Path.of("src/client/resources/tweakify-mod.minihud.mixins.json"), SOURCE_ROOT),
		new Manifest(Path.of("src/client/resources/tweakify-mod.modmenu.mixins.json"), SOURCE_ROOT));

	@Test
	void everyManifestEntryExistsAsMixinSource() throws IOException {
		for (Manifest manifest : MANIFESTS) {
			JsonObject json = readJson(manifest.jsonPath());
			Path packageDir = packageDirectory(manifest, json);
			Set<String> sources = mixinSourceNames(packageDir);
			for (String entry : manifestEntries(json)) {
				assertTrue(sources.contains(entry),
					manifest.jsonPath() + " 登记了 " + entry + "，但 " + packageDir
						+ " 下没有声明 @Mixin 的 " + entry + ".java（改名或删除后忘记同步清单）");
			}
		}
	}

	@Test
	void everyMixinSourceIsRegistered() throws IOException {
		for (Manifest manifest : MANIFESTS) {
			JsonObject json = readJson(manifest.jsonPath());
			Path packageDir = packageDirectory(manifest, json);
			Set<String> unregistered = new TreeSet<>(mixinSourceNames(packageDir));
			unregistered.removeAll(manifestEntries(json));
			assertTrue(unregistered.isEmpty(),
				packageDir + " 下这些类声明了 @Mixin 但没有登记到 " + manifest.jsonPath()
					+ "：" + unregistered + "（注入不会生效，且不会有任何报错）");
		}
	}

	@Test
	void manifestEntriesAreUnique() throws IOException {
		for (Manifest manifest : MANIFESTS) {
			List<String> entries = manifestEntries(readJson(manifest.jsonPath()));
			List<String> duplicates = new ArrayList<>(new LinkedHashSet<>(entries));
			duplicates.removeIf(entry -> entries.indexOf(entry) == entries.lastIndexOf(entry));
			assertTrue(duplicates.isEmpty(),
				manifest.jsonPath() + " 重复登记：" + duplicates);
		}
	}

	// 包搬到别处后只改源文件、不改 json 时，Mixin 会静默找不到类。
	@Test
	void manifestPackageMatchesSourceDirectory() throws IOException {
		for (Manifest manifest : MANIFESTS) {
			JsonObject json = readJson(manifest.jsonPath());
			String packageName = json.get("package").getAsString();
			assertTrue(Files.isDirectory(packageDirectory(manifest, json)),
				manifest.jsonPath() + " 的 package=" + packageName + " 在 " + manifest.sourceRoot()
					+ " 下没有对应目录（移动包后忘记改 json）");
		}
	}

	// fabric.mod.json 没列出的配置不会被 Mixin 加载，等于整份功能静默失效。
	@Test
	void fabricModJsonDeclaresEveryMixinConfig() throws IOException {
		Set<String> declared = new TreeSet<>();
		for (JsonElement element : readJson(FABRIC_MOD_JSON).getAsJsonArray("mixins")) {
			declared.add(element.getAsJsonObject().get("config").getAsString());
		}
		Set<String> expected = MANIFESTS.stream()
			.map(manifest -> manifest.jsonPath().getFileName().toString())
			.collect(Collectors.toCollection(TreeSet::new));
		assertEquals(expected, declared,
			"fabric.mod.json 的 mixins 列表与 src/client/resources 下的配置不一致");
	}

	private static JsonObject readJson(Path path) throws IOException {
		return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	// 合并 mixins/client/server 三段：本项目只用 client 段，其余两段按 Mixin 语义一并计入。
	private static List<String> manifestEntries(JsonObject json) {
		List<String> entries = new ArrayList<>();
		for (String section : List.of("mixins", "client", "server")) {
			if (!json.has(section)) {
				continue;
			}
			JsonArray array = json.getAsJsonArray(section);
			for (JsonElement element : array) {
				entries.add(element.getAsString());
			}
		}
		return entries;
	}

	private static Path packageDirectory(Manifest manifest, JsonObject json) {
		return manifest.sourceRoot().resolve(json.get("package").getAsString().replace('.', '/'));
	}

	// 只扫描清单对应包目录那一层：子包属于另一份清单，由各自的 manifest 校验。
	// 不声明 @Mixin 的类（插件、辅助类）不算混入，避免误报。
	private static Set<String> mixinSourceNames(Path packageDir) throws IOException {
		Set<String> names = new TreeSet<>();
		try (Stream<Path> files = Files.list(packageDir)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				if (MIXIN_ANNOTATION.matcher(Files.readString(file, StandardCharsets.UTF_8)).find()) {
					String fileName = file.getFileName().toString();
					names.add(fileName.substring(0, fileName.length() - ".java".length()));
				}
			}
		}
		return names;
	}

	private record Manifest(Path jsonPath, Path sourceRoot) {
	}
}
