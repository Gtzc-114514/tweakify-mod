package com.gtzc.tweakify.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全量客户端 Mixin 织入验证：主配置里的每个目标类都要能在 Loader JUnit 中加载成功。
 *
 * <p>判定依据：{@code require >= 1} 的注入点在目标类加载时就会校验，找不到目标方法/字段
 * 会直接抛 {@code InvalidInjectionException} 让加载失败；因此「目标类能加载」等价于
 * 「这批注入点全部命中」。{@code require = 0} 的 73 个探针式注入点按设计允许静默跳过
 * （见 {@code docs/mixin-probes.md}），不在本测试的判定范围内。
 *
 * <p>mixin 类本身不能 {@code Class.forName}（Mixin 会抛 {@code IllegalClassLoadError}），
 * 所以目标类名从编译产物的 {@code @Mixin} 注解里读：用 JDK 自带的 classfile API 解析
 * class 字节，不经过类加载器。只加载、不初始化目标类，避免 headless 环境执行依赖 GL
 * 的静态初始化；织入发生在 defineClass 阶段，不受影响。
 *
 * <p>先加载哨兵目标类 {@code CreativeModeInventoryScreen} 并确认 {@code @Unique} 字段存在，
 * 证明主配置确实被 Fabric Loader 注册；否则下面的遍历会在没有任何织入的情况下空转通过。
 */
class AllMixinApplicationLoaderTest {
	private static final String MANIFEST = "tweakify-mod.client.mixins.json";
	private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";
	private static final String SENTINEL_TARGET =
		"net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen";
	private static final String SENTINEL_FIELD = "tweakify$lastConfigSignature";

	// headless Loader JUnit 里无法加载的目标类：属于测试环境限制，按目标类跳过。
	// 只放「确认与注入配置无关」的类，并在注释里写明失败原因。
	private static final Set<String> UNLOADABLE_TARGETS = Set.of();

	@Test
	void everyClientMixinAppliesOnTargetLoad() throws Exception {
		assertEquals(EnvType.CLIENT, FabricLoader.getInstance().getEnvironmentType(),
			"Fabric Loader JUnit 必须以 client 环境运行");
		ClassLoader loader = getClass().getClassLoader();
		assertSentinelApplied(loader);

		JsonObject manifest = readManifest();
		String packageName = manifest.get("package").getAsString();
		JsonArray entries = manifest.getAsJsonArray("client");
		assertTrue(entries.size() > 0, MANIFEST + " 的 client 清单为空");

		List<String> failures = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		Set<String> attemptedTargets = new LinkedHashSet<>();
		int processedMixins = 0;
		int loadedTargets = 0;

		for (JsonElement element : entries) {
			String mixinName = element.getAsString();
			processedMixins++;
			byte[] classBytes = readMixinBytes(loader, packageName, mixinName);
			if (classBytes == null) {
				failures.add(mixinName + " 的 class 文件不在 classpath 上");
				continue;
			}
			TargetResolution resolution;
			try {
				resolution = readTargets(classBytes);
			} catch (Throwable error) {
				failures.add(mixinName + " 解析 class 文件失败: " + describe(error));
				continue;
			}
			if (resolution.error() != null) {
				failures.add(mixinName + " " + resolution.error());
				continue;
			}
			if (resolution.names().isEmpty()) {
				failures.add(mixinName + " 的 @Mixin 注解没有 value/targets");
				continue;
			}
			for (String target : resolution.names()) {
				if (!attemptedTargets.add(target)) {
					continue;
				}
				try {
					Class.forName(target, false, loader);
					loadedTargets++;
				} catch (Throwable error) {
					if (UNLOADABLE_TARGETS.contains(target)) {
						skipped.add(target);
					} else {
						failures.add(mixinName + " -> " + target + ": " + describe(error));
					}
				}
			}
		}

		String summary = "清单 " + entries.size() + " 条，处理 " + processedMixins
			+ " 个 mixin，加载目标类 " + loadedTargets + " 个，白名单跳过 " + skipped.size() + " 个";
		assertEquals(entries.size(), processedMixins, "有 mixin 条目未被处理");
		assertTrue(failures.isEmpty(), summary + "，失败如下：\n" + String.join("\n", failures));
		assertTrue(loadedTargets + skipped.size() > 0, "没有加载任何目标类：" + summary);
	}

	// 哨兵：证明主配置已被 Loader 注册且 @Unique 成员确实织入，避免整条链路空转通过。
	private static void assertSentinelApplied(ClassLoader loader) throws ClassNotFoundException {
		Class<?> target = Class.forName(SENTINEL_TARGET, false, loader);
		boolean found = false;
		for (Field field : target.getDeclaredFields()) {
			if (field.getName().equals(SENTINEL_FIELD)) {
				found = true;
				break;
			}
		}
		assertTrue(found, "哨兵目标 " + SENTINEL_TARGET + " 缺少 " + SENTINEL_FIELD
			+ "：主 mixin 配置没有被 Fabric Loader 应用，本测试无法证明任何织入");
	}

	private static byte[] readMixinBytes(ClassLoader loader, String packageName, String mixinName) {
		String resource = packageName.replace('.', '/') + "/" + mixinName + ".class";
		try (InputStream stream = loader.getResourceAsStream(resource)) {
			return stream == null ? null : stream.readAllBytes();
		} catch (IOException error) {
			return null;
		}
	}

	// @Mixin 是 CLASS retention（不保留到运行时），所以读的是 RuntimeInvisibleAnnotations；
	// value 是 Class 常量数组（描述符形式），targets 是字符串数组，两种写法都要支持。
	private static TargetResolution readTargets(byte[] classBytes) {
		ClassModel model = ClassFile.of().parse(classBytes);
		RuntimeInvisibleAnnotationsAttribute annotations = model
			.findAttribute(Attributes.runtimeInvisibleAnnotations()).orElse(null);
		if (annotations == null) {
			return new TargetResolution(Set.of(), "没有类注解（RuntimeInvisibleAnnotations）");
		}
		for (Annotation annotation : annotations.annotations()) {
			if (!MIXIN_ANNOTATION.equals(annotation.className().stringValue())) {
				continue;
			}
			Set<String> targets = new LinkedHashSet<>();
			for (AnnotationElement element : annotation.elements()) {
				String name = element.name().stringValue();
				if ("targets".equals(name)) {
					collectStrings(element.value(), targets);
				} else if ("value".equals(name)) {
					collectClasses(element.value(), targets);
				}
			}
			return new TargetResolution(targets, null);
		}
		return new TargetResolution(Set.of(), "没有 @Mixin 注解");
	}

	private static void collectStrings(AnnotationValue value, Set<String> targets) {
		if (value instanceof AnnotationValue.OfArray array) {
			for (AnnotationValue element : array.values()) {
				collectStrings(element, targets);
			}
		} else if (value instanceof AnnotationValue.OfString string) {
			targets.add(string.stringValue());
		}
	}

	private static void collectClasses(AnnotationValue value, Set<String> targets) {
		if (value instanceof AnnotationValue.OfArray array) {
			for (AnnotationValue element : array.values()) {
				collectClasses(element, targets);
			}
		} else if (value instanceof AnnotationValue.OfClass classValue) {
			targets.add(binaryName(classValue.className().stringValue()));
		}
	}

	// 注解里的 Class 常量是描述符形式 Lpkg/Type;；数组形式 [L...; 本项目未使用。
	private static String binaryName(String descriptor) {
		String name = descriptor;
		if (name.startsWith("L") && name.endsWith(";")) {
			name = name.substring(1, name.length() - 1);
		}
		return name.replace('/', '.');
	}

	private static JsonObject readManifest() throws IOException {
		try (InputStream stream = AllMixinApplicationLoaderTest.class.getClassLoader()
			.getResourceAsStream(MANIFEST)) {
			assertTrue(stream != null, "classpath 上找不到 " + MANIFEST);
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
				.getAsJsonObject();
		}
	}

	// Mixin 的失败原因常包在 cause 链里（如 InvalidInjectionException 包一层），
	// 展开后 CI 日志能直接定位到具体注入点。
	private static String describe(Throwable error) {
		StringBuilder message = new StringBuilder(error.toString());
		Throwable cause = error.getCause();
		while (cause != null) {
			message.append(" <- ").append(cause);
			cause = cause.getCause();
		}
		return message.toString();
	}

	private record TargetResolution(Set<String> names, String error) {
	}
}
