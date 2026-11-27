package com.gtzc.tweakify.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Mixin 注入点签名静态校验：{@code method} / {@code at.target} 必须能在目标类中找到真实成员。
 *
 * <p>背景：{@code require = 0} 的注入点在目标方法改名或签名变化后会被 Mixin 静默跳过，
 * 功能失效但构建、启动与 loaderTest 都不会报错。v1.1.9.1 修复的定位栏头像就是这类问题：
 * 26.3 的 {@code LocatorBar} 把 {@code blitSprite} 从 7 参改成 6 参，注入目标仍写 7 参，
 * 头像功能从 1.1.7 起从未生效，直到双客户端采样才被发现。
 *
 * <p>目标类从编译产物的 {@code @Mixin} 注解读取（类注解不会被 Mixin AP 剥离）；
 * 注入注解会被 Loom 的 Mixin 注解处理器从 class 文件剥离，因此从源码解析。
 * 校验范围：
 * <ul>
 *   <li>{@code @Inject} / {@code @Redirect} / {@code @ModifyArg} / {@code @ModifyArgs} /
 *       {@code @ModifyConstant} / {@code @ModifyVariable} 与 MixinExtras 的
 *       {@code @WrapMethod} / {@code @WrapOperation} / {@code @ModifyReturnValue} /
 *       {@code @ModifyExpressionValue} / {@code @WrapWithCondition} 的 {@code method} 属性；</li>
 *   <li>上述注解的 {@code @At.target}（INVOKE 方法、FIELD 字段、NEW 构造器）；</li>
 *   <li>{@code @Accessor} 字段与 {@code @Invoker} 方法。</li>
 * </ul>
 *
 * <p>跳过：{@code method = "*"}、HEAD / RETURN / TAIL 等无 target 的注入点、CONSTANT 常量，
 * 以及测试 classpath 上不可用的可选模组目标类（计入 skipped 并在失败信息中打印）。
 */
class MixinInjectionTargetTest {
	private static final Path SOURCE_ROOT = Path.of("src/client/java");

	private static final List<String> MANIFESTS = List.of(
		"tweakify-mod.client.mixins.json",
		"tweakify-mod.minihud.mixins.json",
		"tweakify-mod.modmenu.mixins.json");

	private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

	// 需要校验 method / at.target 的注入注解（简单名，源码解析时匹配）。
	private static final Pattern INJECTION_ANNOTATION = Pattern.compile(
		"@(Inject|Redirect|ModifyArg|ModifyArgs|ModifyConstant|ModifyVariable"
			+ "|WrapOperation|WrapMethod|ModifyReturnValue|ModifyExpressionValue|WrapWithCondition)\\b");

	private static final Pattern ACCESSOR_OR_INVOKER = Pattern.compile("@(Accessor|Invoker)\\b");
	private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern METHOD_ATTRIBUTE = Pattern.compile("\\bmethod\\s*=\\s*");
	private static final Pattern AT_ATTRIBUTE = Pattern.compile("\\bat\\s*=\\s*@At\\b");
	private static final Pattern VALUE_ATTRIBUTE = Pattern.compile("\\bvalue\\s*=\\s*");
	private static final Pattern TARGET_ATTRIBUTE = Pattern.compile("\\btarget\\s*=\\s*");
	// @ModifyVariable 的参数槽位属性：argsOnly=true 时 index 必须落在目标方法的参数起始槽位上。
	private static final Pattern INDEX_ATTRIBUTE = Pattern.compile("\\bindex\\s*=\\s*(\\d+)");
	private static final Pattern ARGS_ONLY_ATTRIBUTE = Pattern.compile("\\bargsOnly\\s*=\\s*(true|false)");

	// 目标类不可加载时的豁免：只允许「可选模组不在测试 classpath」这类与签名无关的原因。
	private static final Set<String> SKIPPED_TARGETS = Set.of();

	// 源码里的一条注入声明：memberName 为 Accessor/Invoker 的成员名，handlerName 为处理器方法名。
	// index/argsOnly 仅对 @ModifyVariable 有意义（其余注入为 -1 / false）。
	private record Injection(String annotation, List<String> methods, String atValue, String atTarget,
			String memberName, String handlerName, int index, boolean argsOnly) {
	}

	@Test
	void everyInjectionTargetSignatureExists() throws Exception {
		ClassLoader loader = getClass().getClassLoader();
		List<String> failures = new ArrayList<>();
		Set<String> skipped = new LinkedHashSet<>();
		Set<String> visitedTargets = new LinkedHashSet<>();
		int checked = 0;

		for (String manifestPath : MANIFESTS) {
			JsonObject manifest = readManifest(manifestPath);
			String packageName = manifest.get("package").getAsString();
			for (JsonElement element : manifest.getAsJsonArray("client")) {
				String mixinName = element.getAsString();
				byte[] classBytes = readClass(loader,
					packageName.replace('.', '/') + "/" + mixinName + ".class");
				if (classBytes == null) {
					failures.add(mixinName + " 的 class 文件不在 classpath 上");
					continue;
				}
				Set<String> targets = readMixinTargets(ClassFile.of().parse(classBytes));
				if (targets.isEmpty()) {
					// 插件类（MiniHudMixinPlugin / ModMenuMixinPlugin）不声明 @Mixin。
					continue;
				}
				Path sourcePath = SOURCE_ROOT.resolve(
					packageName.replace('.', '/') + "/" + mixinName + ".java");
				if (!Files.isRegularFile(sourcePath)) {
					failures.add(mixinName + " 的源码不在 " + SOURCE_ROOT + " 下");
					continue;
				}
				for (Injection injection : parseInjections(Files.readString(sourcePath, StandardCharsets.UTF_8))) {
					checked++;
					checkInjection(mixinName, ClassFile.of().parse(classBytes), injection, targets, loader,
						failures, skipped, visitedTargets);
				}
			}
		}

		String summary = "校验注入点 " + checked + " 个，目标类 " + visitedTargets.size()
			+ " 个，豁免 " + skipped.size() + " 个";
		if (checked == 0) {
			fail(summary + "：没有读到任何注入点，校验链路无效");
		}
		assertTrue(failures.isEmpty(), summary + "，签名失配如下：\n" + String.join("\n", failures));
	}

	private static void checkInjection(String mixin, ClassModel mixinModel, Injection injection,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped,
			Set<String> visitedTargets) {
		if (injection.memberName() != null) {
			checkMemberName(mixin, injection.annotation(), injection.memberName(), targets, loader,
				failures, skipped);
			return;
		}
		for (String methodSpec : injection.methods()) {
			checkMethodSpec(mixin, injection.annotation(), methodSpec, targets, loader, failures,
				skipped, visitedTargets);
		}
		if (injection.atTarget() != null && !injection.atTarget().isEmpty()) {
			checkAt(mixin, injection.methods(), injection.atValue(), injection.atTarget(), targets,
				loader, failures, skipped, visitedTargets);
			checkWrapOperationHandler(mixinModel, mixin, injection, loader, failures);
		}
		checkArgsOnlyIndex(mixin, mixinModel, injection, targets, loader, failures);
	}

	// @ModifyVariable(argsOnly = true, index = N) 的槽位校验：
	// 目标方法在 index 处必须正好是某个参数的起始槽位（double/long 占两个槽），
	// 且处理器参数类型与该参数类型一致。实例方法的 slot 0（this）、
	// 双槽类型的第二个槽位与参数类型写错都会在这里被拦住；
	// 两个同类型参数之间的选择仍靠人工核对（无 LVT 时无法从字节码区分）。
	private static void checkArgsOnlyIndex(String mixin, ClassModel mixinModel, Injection injection,
			Set<String> targets, ClassLoader loader, List<String> failures) {
		if (!"ModifyVariable".equals(injection.annotation()) || !injection.argsOnly()
			|| injection.index() < 0) {
			return;
		}
		MethodModel handler = findHandlerModel(mixinModel, injection.handlerName());
		if (handler == null) {
			failures.add(mixin + " 的 @ModifyVariable 处理器 " + injection.handlerName() + " 不在 mixin 类中");
			return;
		}
		List<Class<?>> handlerTypes;
		try {
			handlerTypes = MethodType.fromMethodDescriptorString(
				handler.methodType().stringValue(), loader).parameterList();
		} catch (RuntimeException error) {
			failures.add(mixin + " 的 @ModifyVariable 处理器描述符无法解析: " + error.getMessage());
			return;
		}
		for (String targetName : targets) {
			Class<?> target = loadTargetQuietly(targetName, loader);
			if (target == null) {
				continue;
			}
			Method method = findTargetMethod(target, injection.methods());
			if (method == null) {
				continue; // method 属性校验那一轮已经报过
			}
			Class<?> slotType = parameterAtSlot(method, injection.index());
			if (slotType == null) {
				failures.add(mixin + " 的 @ModifyVariable index=" + injection.index()
					+ " 不是 " + targetName + "." + method.getName() + " 的参数起始槽位");
				continue;
			}
			if (!handlerTypes.contains(slotType)) {
				failures.add(mixin + " 的 @ModifyVariable 处理器参数类型与 " + targetName + "."
					+ method.getName() + " 的槽位 " + injection.index()
					+ "（" + slotType.getSimpleName() + "）不匹配");
			}
		}
	}

	private static MethodModel findHandlerModel(ClassModel model, String name) {
		if (name == null) {
			return null;
		}
		for (MethodModel method : model.methods()) {
			if (method.methodName().stringValue().equals(name)) {
				return method;
			}
		}
		return null;
	}

	// 槽位校验专用：不可加载的目标类直接跳过，与方法签名校验同一口径（不额外计入豁免）。
	private static Class<?> loadTargetQuietly(String name, ClassLoader loader) {
		try {
			return Class.forName(name, false, loader);
		} catch (Throwable error) {
			return null;
		}
	}

	// 取注入方法声明里的第一个同名方法；method 属性已单独校验过存在性。
	private static Method findTargetMethod(Class<?> target, List<String> specs) {
		for (String spec : specs) {
			if (spec.isEmpty() || "*".equals(spec)) {
				continue;
			}
			int paren = spec.indexOf('(');
			String name = paren >= 0 ? spec.substring(0, paren) : spec;
			for (Class<?> current = target; current != null; current = current.getSuperclass()) {
				for (Method method : current.getDeclaredMethods()) {
					if (method.getName().equals(name)) {
						return method;
					}
				}
			}
		}
		return null;
	}

	// 参数起始槽位：实例方法 slot 0 为 this，double/long 占两个槽位。
	private static Class<?> parameterAtSlot(Method method, int index) {
		int slot = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
		for (Class<?> type : method.getParameterTypes()) {
			if (slot == index) {
				return type;
			}
			slot += (type == long.class || type == double.class) ? 2 : 1;
		}
		return null;
	}

	private static int intAttribute(String args, Pattern pattern) {
		Matcher matcher = pattern.matcher(args);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
	}

	private static boolean booleanAttribute(String args, Pattern pattern) {
		Matcher matcher = pattern.matcher(args);
		return matcher.find() && Boolean.parseBoolean(matcher.group(1));
	}

	// Accessor/Invoker：注解值就是目标字段/方法名。
	private static void checkMemberName(String mixin, String annotation, String name,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped) {
		for (String targetName : targets) {
			Class<?> target = loadTarget(targetName, loader, skipped);
			if (target == null) {
				continue;
			}
			boolean field = "Accessor".equals(annotation);
			boolean found = field ? hasField(target, name) : hasMethodNamed(target, name);
			if (!found) {
				failures.add(mixin + " 的 @" + annotation + "(\"" + name + "\") 在目标 "
					+ targetName + " 中不存在");
			}
		}
	}

	private static void checkMethodSpec(String mixin, String annotation, String spec,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped,
			Set<String> visitedTargets) {
		if (spec.isEmpty() || "*".equals(spec)) {
			return;
		}
		String name;
		MethodType type = null;
		boolean paramsOnly = false;
		if (spec.indexOf('(') >= 0) {
			int paren = spec.indexOf('(');
			name = spec.substring(0, paren);
			String descriptor = spec.substring(paren);
			paramsOnly = descriptor.endsWith(")");
			type = methodType(paramsOnly ? descriptor + "V" : descriptor, loader, mixin,
				"@" + annotation, spec, failures);
			if (type == null) {
				return;
			}
		} else {
			name = spec;
		}
		for (String targetName : targets) {
			Class<?> target = loadTarget(targetName, loader, skipped);
			if (target == null) {
				continue;
			}
			visitedTargets.add(targetName);
			boolean found = "<init>".equals(name)
				? hasConstructor(target, type)
				: paramsOnly ? hasMethodParams(target, name, type) : hasMethod(target, name, type);
			if (!found) {
				failures.add(mixin + " 的 @" + annotation + " method=\"" + spec + "\" 在目标 "
					+ targetName + " 中不存在");
			}
		}
	}

	// @At.target 的三种形式：INVOKE 方法、FIELD 字段、NEW 构造器；其余注入点不校验 target。
	private static void checkAt(String mixin, List<String> methods, String value, String target,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped,
			Set<String> visitedTargets) {
		if (value == null) {
			return;
		}
		switch (value) {
			case "INVOKE" -> {
				checkMemberSpec(mixin, target, targets, loader, failures, skipped, visitedTargets, false);
				checkCallSite(mixin, methods, target, targets, loader, failures, skipped, false);
			}
			case "FIELD" -> {
				checkMemberSpec(mixin, target, targets, loader, failures, skipped, visitedTargets, true);
				checkCallSite(mixin, methods, target, targets, loader, failures, skipped, true);
			}
			case "NEW" -> {
				checkConstructorSpec(mixin, target, loader, failures, skipped);
				checkNewSite(mixin, methods, target, targets, loader, failures, skipped);
			}
			case "CONSTANT" -> {
				// target 是常量值，不是成员签名。
			}
			default -> {
				// HEAD/RETURN/TAIL 没有 target；LOAD/STORE 等不以成员签名为目标。
			}
		}
	}

	// 调用点校验：目标方法/字段必须在被注入类的方法指令中真的被使用。
	// 只校验成员存在不够：LocatorBar 案例里 7 参 blitSprite 在 GuiGraphicsExtractor 中存在，
	// 但 LocatorBar 只调 6 参版本，require = 0 下仍会静默跳过。
	private static void checkCallSite(String mixin, List<String> methods, String spec,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped,
			boolean field) {
		ParsedMember member = parseMember(spec);
		if (member == null) {
			return;
		}
		for (String targetName : targets) {
			byte[] bytes = readClass(loader, targetName.replace('.', '/') + ".class");
			if (bytes == null) {
				skipped.add(targetName + "（字节不可读，跳过调用点校验）");
				continue;
			}
			ClassModel model = ClassFile.of().parse(bytes);
			List<MethodModel> selected = selectMethods(model, methods);
			boolean found = field ? hasFieldAccess(selected, member) : hasInvocation(selected, member);
			if (!found) {
				failures.add(mixin + " 的 at.target \"" + spec + "\" 在 " + targetName
					+ " 的注入方法中没有调用点（require = 0 下会静默跳过）");
			}
		}
	}

	private static void checkNewSite(String mixin, List<String> methods, String spec,
			Set<String> targets, ClassLoader loader, List<String> failures, Set<String> skipped) {
		ParsedMember member = parseMember(spec);
		if (member == null || member.owner() == null) {
			return;
		}
		for (String targetName : targets) {
			byte[] bytes = readClass(loader, targetName.replace('.', '/') + ".class");
			if (bytes == null) {
				skipped.add(targetName + "（字节不可读，跳过 NEW 调用点校验）");
				continue;
			}
			ClassModel model = ClassFile.of().parse(bytes);
			boolean found = false;
			for (MethodModel method : selectMethods(model, methods)) {
				CodeModel code = method.findAttribute(Attributes.code()).orElse(null);
				if (code == null) {
					continue;
				}
				for (CodeElement element : code) {
					if (element instanceof NewObjectInstruction instruction
							&& instruction.className().asInternalName().replace('/', '.').equals(member.owner())) {
						found = true;
						break;
					}
				}
				if (found) {
					break;
				}
			}
			if (!found) {
				failures.add(mixin + " 的 at.target \"" + spec + "\" 在 " + targetName
					+ " 的注入方法中没有 new 指令");
			}
		}
	}

	// target 字符串的成员部分：Lowner;name(desc)ret、Lowner;name:type，或无 owner 的 name(desc)。
	private record ParsedMember(String owner, String name, String desc) {
	}

	private static ParsedMember parseMember(String spec) {
		if (spec.startsWith("L")) {
			int semicolon = spec.indexOf(';');
			if (semicolon < 0) {
				return null;
			}
			String owner = spec.substring(1, semicolon).replace('/', '.');
			String rest = spec.substring(semicolon + 1);
			int paren = rest.indexOf('(');
			if (paren >= 0) {
				return new ParsedMember(owner, rest.substring(0, paren), rest.substring(paren));
			}
			int colon = rest.indexOf(':');
			if (colon >= 0) {
				return new ParsedMember(owner, rest.substring(0, colon), rest.substring(colon + 1));
			}
			return new ParsedMember(owner, rest, null);
		}
		int paren = spec.indexOf('(');
		if (paren >= 0) {
			return new ParsedMember(null, spec.substring(0, paren), spec.substring(paren));
		}
		return null;
	}

	// method 属性指定的注入方法；为空或 "*" 时扫描目标类所有方法。
	private static List<MethodModel> selectMethods(ClassModel model, List<String> methods) {
		if (methods.isEmpty() || methods.contains("*")) {
			return model.methods();
		}
		List<MethodModel> selected = new ArrayList<>();
		for (String spec : methods) {
			if (spec.isEmpty() || "*".equals(spec)) {
				continue;
			}
			String name = spec;
			String descriptor = null;
			int paren = spec.indexOf('(');
			if (paren >= 0) {
				name = spec.substring(0, paren);
				descriptor = spec.substring(paren);
			}
			for (MethodModel method : model.methods()) {
				if (!method.methodName().stringValue().equals(name)) {
					continue;
				}
				if (descriptor == null || method.methodType().stringValue().equals(descriptor)) {
					selected.add(method);
				}
			}
		}
		return selected;
	}

	private static boolean hasInvocation(List<MethodModel> methods, ParsedMember member) {
		for (MethodModel method : methods) {
			CodeModel code = method.findAttribute(Attributes.code()).orElse(null);
			if (code == null) {
				continue;
			}
			for (CodeElement element : code) {
				if (element instanceof InvokeInstruction instruction
						&& (member.owner() == null
							|| instruction.owner().asInternalName().replace('/', '.').equals(member.owner()))
						&& instruction.name().stringValue().equals(member.name())
						&& (member.desc() == null || instruction.type().stringValue().equals(member.desc()))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasFieldAccess(List<MethodModel> methods, ParsedMember member) {
		for (MethodModel method : methods) {
			CodeModel code = method.findAttribute(Attributes.code()).orElse(null);
			if (code == null) {
				continue;
			}
			for (CodeElement element : code) {
				if (element instanceof FieldInstruction instruction
						&& (member.owner() == null
							|| instruction.owner().asInternalName().replace('/', '.').equals(member.owner()))
						&& instruction.name().stringValue().equals(member.name())
						&& (member.desc() == null || instruction.type().stringValue().equals(member.desc()))) {
					return true;
				}
			}
		}
		return false;
	}

	private static void checkMemberSpec(String mixin, String spec, Set<String> targets,
			ClassLoader loader, List<String> failures, Set<String> skipped, Set<String> visitedTargets,
			boolean field) {
		String owner;
		String rest;
		if (spec.startsWith("L")) {
			int semicolon = spec.indexOf(';');
			if (semicolon < 0) {
				return;
			}
			owner = spec.substring(1, semicolon).replace('/', '.');
			rest = spec.substring(semicolon + 1);
		} else {
			// 无 owner 的 target 在被注入方法所在类里查找，mixin 的每个目标都要满足。
			for (String targetName : targets) {
				checkMemberOnClass(mixin, spec, targetName, loader, failures, skipped, visitedTargets, field);
			}
			return;
		}
		checkMemberOnClass(mixin, rest, owner, loader, failures, skipped, visitedTargets, field);
	}

	private static void checkMemberOnClass(String mixin, String memberSpec, String ownerName,
			ClassLoader loader, List<String> failures, Set<String> skipped, Set<String> visitedTargets,
			boolean field) {
		Class<?> owner = loadTarget(ownerName, loader, skipped);
		if (owner == null) {
			return;
		}
		visitedTargets.add(ownerName);
		String name;
		String descriptor = null;
		if (field) {
			int colon = memberSpec.indexOf(':');
			name = colon >= 0 ? memberSpec.substring(0, colon) : memberSpec;
			descriptor = colon >= 0 ? memberSpec.substring(colon + 1) : null;
		} else {
			int paren = memberSpec.indexOf('(');
			name = paren >= 0 ? memberSpec.substring(0, paren) : memberSpec;
			descriptor = paren >= 0 ? memberSpec.substring(paren) : null;
		}
		boolean found;
		if (field) {
			found = descriptor == null
				? hasField(owner, name)
				: hasFieldOfType(owner, name, descriptor, loader);
		} else if (descriptor == null) {
			found = hasMethodNamed(owner, name);
		} else if ("<init>".equals(name)) {
			// @At(INVOKE) 也可能指向构造器：按构造器校验。
			MethodType type = methodType(descriptor, loader, mixin, "at.target", memberSpec, failures);
			if (type == null) {
				return;
			}
			found = hasConstructor(owner, type);
		} else {
			boolean paramsOnly = descriptor.endsWith(")");
			MethodType type = methodType(paramsOnly ? descriptor + "V" : descriptor, loader, mixin,
				"at.target", memberSpec, failures);
			if (type == null) {
				return;
			}
			found = paramsOnly ? hasMethodParams(owner, name, type) : hasMethod(owner, name, type);
		}
		if (!found) {
			failures.add(mixin + " 的 at.target \"" + memberSpec + "\" 在 " + ownerName + " 中不存在");
		}
	}

	private static void checkConstructorSpec(String mixin, String spec,
			ClassLoader loader, List<String> failures, Set<String> skipped) {
		if (!spec.startsWith("L")) {
			// 无 owner 的构造器描述符：目标类要从指令上下文推断，静态校验无法定位。
			skipped.add(mixin + " 的 at.target \"" + spec + "\" 为无 owner 的构造器");
			return;
		}
		int semicolon = spec.indexOf(';');
		if (semicolon < 0) {
			return;
		}
		String owner = spec.substring(1, semicolon).replace('/', '.');
		String descriptor = spec.substring(semicolon + 1);
		int paren = descriptor.indexOf('(');
		if (paren < 0) {
			return;
		}
		descriptor = descriptor.substring(paren);
		Class<?> type = loadTarget(owner, loader, skipped);
		if (type == null) {
			return;
		}
		MethodType typeDesc = methodType(descriptor, loader, mixin, "at.target", spec, failures);
		if (typeDesc == null) {
			return;
		}
		boolean found = false;
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			if (MethodType.methodType(void.class, constructor.getParameterTypes()).equals(typeDesc)) {
				found = true;
				break;
			}
		}
		if (!found && !hasConstructorInSuper(type, typeDesc)) {
			StringBuilder diag = new StringBuilder();
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				diag.append(java.util.Arrays.toString(constructor.getParameterTypes())).append(' ');
			}
			failures.add(mixin + " 的 at.target \"" + spec + "\" 构造器在 " + owner
				+ " 中不存在 [ctors=" + diag + "]");
		}
	}

	// @WrapOperation 的处理器参数必须按顺序覆盖 target 的参数序列（允许 @Local 等额外参数
	// 与末尾的 Operation）。这能捕获「target 重载仍存在、但注入到了另一处重载」的失配：
	// LocatorBar 的 7 参 blitSprite 在 GuiGraphicsExtractor 中存在（箭头），
	// 但圆点用的是 6 参，处理器参数与 7 参不匹配。
	private static void checkWrapOperationHandler(ClassModel mixinModel, String mixin, Injection injection,
			ClassLoader loader, List<String> failures) {
		if (!"WrapOperation".equals(injection.annotation()) || injection.handlerName() == null
				|| injection.atTarget() == null) {
			return;
		}
		ParsedMember target = parseMember(injection.atTarget());
		if (target == null || target.desc() == null || !target.desc().startsWith("(")) {
			return;
		}
		MethodModel handler = null;
		for (MethodModel method : mixinModel.methods()) {
			if (method.methodName().stringValue().equals(injection.handlerName())) {
				handler = method;
				break;
			}
		}
		if (handler == null) {
			failures.add(mixin + " 的 @WrapOperation 处理器 " + injection.handlerName() + " 不在 mixin 类中");
			return;
		}
		try {
			MethodType targetType = MethodType.fromMethodDescriptorString(target.desc(), loader);
			MethodType handlerType = MethodType.fromMethodDescriptorString(
				handler.methodType().stringValue(), loader);
			if (!isSubsequence(targetType.parameterList(), handlerType.parameterList())) {
				failures.add(mixin + " 的 @WrapOperation 处理器 " + injection.handlerName()
					+ " 参数与 target \"" + injection.atTarget() + "\" 不匹配（可能注入了错误的重载）");
			}
		} catch (RuntimeException error) {
			failures.add(mixin + " 的 @WrapOperation target \"" + injection.atTarget()
				+ "\" 描述符无法解析: " + error.getMessage());
		}
	}

	private static boolean isSubsequence(List<Class<?>> target, List<Class<?>> handler) {
		int index = 0;
		for (Class<?> type : handler) {
			if (index < target.size() && target.get(index).equals(type)) {
				index++;
			}
		}
		return index == target.size();
	}

	// 构造器只查目标类自身；methodType 为空表示任意构造器。
	private static boolean hasConstructor(Class<?> type, MethodType methodType) {
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			if (methodType == null
					|| MethodType.methodType(void.class, constructor.getParameterTypes()).equals(methodType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasConstructorInSuper(Class<?> type, MethodType typeDesc) {
		for (Class<?> current = type.getSuperclass(); current != null; current = current.getSuperclass()) {
			for (Constructor<?> constructor : current.getDeclaredConstructors()) {
				if (MethodType.methodType(void.class, constructor.getParameterTypes()).equals(typeDesc)) {
					return true;
				}
			}
		}
		return false;
	}

	// 方法查找沿父类链进行；接口的 default/static 方法同样算命中。
	private static boolean hasMethod(Class<?> type, String name, MethodType methodType) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			if (declaresMethod(current, name, methodType)) {
				return true;
			}
		}
		return declaresMethodInInterfaces(type, name, methodType);
	}

	private static boolean declaresMethod(Class<?> owner, String name, MethodType methodType) {
		for (Method method : owner.getDeclaredMethods()) {
			if (!method.getName().equals(name)) {
				continue;
			}
			if (methodType == null
					|| methodType.equals(MethodType.methodType(method.getReturnType(), method.getParameterTypes()))) {
				return true;
			}
		}
		return false;
	}

	private static boolean declaresMethodInInterfaces(Class<?> type, String name, MethodType methodType) {
		for (Class<?> iface : type.getInterfaces()) {
			if (declaresMethod(iface, name, methodType)
					|| declaresMethodInInterfaces(iface, name, methodType)) {
				return true;
			}
		}
		return false;
	}

	// 参数签名匹配（method / at.target 允许省略返回类型）。
	private static boolean hasMethodParams(Class<?> type, String name, MethodType paramsOnly) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(name)
						&& MethodType.methodType(void.class, method.getParameterTypes()).equals(paramsOnly)) {
					return true;
				}
			}
		}
		for (Class<?> iface : type.getInterfaces()) {
			if (hasMethodParams(iface, name, paramsOnly)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMethodNamed(Class<?> type, String name) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(name)) {
					return true;
				}
			}
		}
		for (Class<?> iface : type.getInterfaces()) {
			if (hasMethodNamed(iface, name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasField(Class<?> type, String name) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasFieldOfType(Class<?> type, String name, String descriptor, ClassLoader loader) {
		Class<?> fieldType;
		try {
			fieldType = MethodType.fromMethodDescriptorString("()" + descriptor, loader).returnType();
		} catch (RuntimeException error) {
			return true; // 描述符无法解析时按名字已命中，避免误报。
		}
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name) && field.getType().equals(fieldType)) {
					return true;
				}
			}
		}
		return false;
	}

	private static MethodType methodType(String descriptor, ClassLoader loader, String mixin,
			String annotation, String spec, List<String> failures) {
		try {
			return MethodType.fromMethodDescriptorString(descriptor, loader);
		} catch (RuntimeException error) {
			failures.add(mixin + " 的 " + annotation + " \"" + spec + "\" 描述符无法解析: "
				+ error.getMessage());
			return null;
		}
	}

	private static Class<?> loadTarget(String name, ClassLoader loader, Set<String> skipped) {
		try {
			return Class.forName(name, false, loader);
		} catch (Throwable error) {
			if (!SKIPPED_TARGETS.contains(name)) {
				skipped.add(name + "（不可加载: " + error.getClass().getSimpleName() + "）");
			}
			return null;
		}
	}

	private static JsonObject readManifest(String path) throws IOException {
		try (InputStream stream = MixinInjectionTargetTest.class.getClassLoader()
				.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IOException("清单不在 classpath 上: " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
				.getAsJsonObject();
		}
	}

	private static byte[] readClass(ClassLoader loader, String resource) {
		try (InputStream stream = loader.getResourceAsStream(resource)) {
			return stream == null ? null : stream.readAllBytes();
		} catch (IOException error) {
			return null;
		}
	}

	// @Mixin 是 CLASS retention：读 RuntimeInvisibleAnnotations，value 是 Class 常量，targets 是字符串。
	// 这个类级注解不会被 Mixin AP 剥离，因此目标类仍然从编译产物读取。
	private static Set<String> readMixinTargets(ClassModel model) {
		RuntimeInvisibleAnnotationsAttribute annotations = model
			.findAttribute(Attributes.runtimeInvisibleAnnotations()).orElse(null);
		if (annotations == null) {
			return Set.of();
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
			return targets;
		}
		return Set.of();
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
			String descriptor = classValue.className().stringValue();
			String name = descriptor;
			if (name.startsWith("L") && name.endsWith(";")) {
				name = name.substring(1, name.length() - 1);
			}
			targets.add(name.replace('/', '.'));
		}
	}

	// ---- 源码解析：注入注解会被 Mixin AP 从 class 文件剥离，只能从源码读。 ----

	static List<Injection> parseInjections(String source) {
		String code = stripComments(source);
		List<Injection> result = new ArrayList<>();

		Matcher accessor = ACCESSOR_OR_INVOKER.matcher(code);
		while (accessor.find()) {
			int paren = code.indexOf('(', accessor.end());
			if (paren < 0) {
				continue;
			}
			String args = extractParen(code, paren);
			String name = firstString(args);
			if (name != null) {
				result.add(new Injection(accessor.group(1), List.of(), null, null, name, null, -1, false));
			}
		}

		Matcher injection = INJECTION_ANNOTATION.matcher(code);
		while (injection.find()) {
			int paren = code.indexOf('(', injection.end());
			if (paren < 0) {
				continue;
			}
			String args = extractParen(code, paren);
			List<String> methods = methodValues(args);
			String atValue = null;
			String atTarget = null;
			Matcher at = AT_ATTRIBUTE.matcher(args);
			if (at.find()) {
				int atParen = args.indexOf('(', at.end());
				if (atParen >= 0) {
					String atArgs = extractParen(args, atParen);
					atValue = atValue(atArgs);
					atTarget = targetValue(atArgs);
				}
			}
			if (!methods.isEmpty() || atTarget != null) {
				int close = closingParen(code, paren);
				String handlerName = close < 0 ? null : findHandlerName(code, close + 1);
				// 只有 @ModifyVariable 会用这两个属性做槽位校验（@ModifyArg 的 index 含义不同）。
				int index = intAttribute(args, INDEX_ATTRIBUTE);
				boolean argsOnly = booleanAttribute(args, ARGS_ONLY_ATTRIBUTE);
				result.add(new Injection(injection.group(1), methods, atValue, atTarget, null, handlerName,
					index, argsOnly));
			}
		}
		return result;
	}

	// 去掉 // 与 /* */ 注释，保留字符串内容，避免注释里的注解被误读。
	private static String stripComments(String source) {
		StringBuilder out = new StringBuilder(source.length());
		int index = 0;
		while (index < source.length()) {
			char c = source.charAt(index);
			if (c == '"') {
				int end = index + 1;
				while (end < source.length()) {
					char inner = source.charAt(end);
					if (inner == '\\') {
						end += 2;
						continue;
					}
					if (inner == '"') {
						end++;
						break;
					}
					end++;
				}
				out.append(source, index, Math.min(end, source.length()));
				index = Math.min(end, source.length());
			} else if (c == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
				int end = source.indexOf('\n', index);
				index = end < 0 ? source.length() : end;
			} else if (c == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
				int end = source.indexOf("*/", index + 2);
				index = end < 0 ? source.length() : end + 2;
			} else {
				out.append(c);
				index++;
			}
		}
		return out.toString();
	}

	// 从 open 位置的 '(' 起做括号匹配，返回内部文本（字符串感知）。
	private static String extractParen(String text, int open) {
		int depth = 0;
		for (int index = open; index < text.length(); index++) {
			char c = text.charAt(index);
			if (c == '"') {
				index++;
				while (index < text.length()) {
					char inner = text.charAt(index);
					if (inner == '\\') {
						index += 2;
						continue;
					}
					if (inner == '"') {
						break;
					}
					index++;
				}
			} else if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
				if (depth == 0) {
					return text.substring(open + 1, index);
				}
			}
		}
		return "";
	}

	// 从 open 位置的 '(' 起找匹配的右括号位置；找不到返回 -1。
	private static int closingParen(String text, int open) {
		int depth = 0;
		for (int index = open; index < text.length(); index++) {
			char c = text.charAt(index);
			if (c == '"') {
				index++;
				while (index < text.length()) {
					char inner = text.charAt(index);
					if (inner == '\\') {
						index += 2;
						continue;
					}
					if (inner == '"') {
						break;
					}
					index++;
				}
			} else if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
				if (depth == 0) {
					return index;
				}
			}
		}
		return -1;
	}

	// 注解右括号后的第一个方法名：跳过修饰符与返回类型，取 '(' 前的标识符。
	private static String findHandlerName(String code, int from) {
		int paren = code.indexOf('(', from);
		if (paren < 0) {
			return null;
		}
		int end = paren;
		while (end > from && Character.isWhitespace(code.charAt(end - 1))) {
			end--;
		}
		int start = end;
		while (start > from && (Character.isJavaIdentifierPart(code.charAt(start - 1))
				|| code.charAt(start - 1) == '$')) {
			start--;
		}
		String name = code.substring(start, end);
		return name.isEmpty() ? null : name;
	}

	// method 属性：单个字符串（可跨行 + 拼接）、{"a", "b"} 数组，或常量引用（无法静态解析，跳过）。
	private static List<String> methodValues(String args) {
		List<String> methods = new ArrayList<>();
		Matcher attribute = METHOD_ATTRIBUTE.matcher(args);
		if (!attribute.find()) {
			return methods;
		}
		int start = skipSpaces(args, attribute.end());
		if (start < args.length() && args.charAt(start) == '{') {
			int end = args.indexOf('}', start);
			if (end < 0) {
				return methods;
			}
			Matcher literal = STRING_LITERAL.matcher(args.substring(start + 1, end));
			while (literal.find()) {
				methods.add(unescape(literal.group(1)));
			}
			return methods;
		}
		String value = readStringExpression(args, start);
		if (value != null) {
			methods.add(value);
		}
		return methods;
	}

	// 读取从 start 开始的字符串表达式，支持跨行的 "a" + "b" 拼接；非常量形式返回 null。
	private static String readStringExpression(String text, int start) {
		int index = skipSpaces(text, start);
		if (index >= text.length() || text.charAt(index) != '"') {
			return null;
		}
		StringBuilder value = new StringBuilder();
		while (index < text.length()) {
			Matcher literal = STRING_LITERAL.matcher(text);
			literal.region(index, text.length());
			if (!literal.find()) {
				break;
			}
			value.append(unescape(literal.group(1)));
			index = skipSpaces(text, literal.end());
			if (index < text.length() && text.charAt(index) == '+') {
				index = skipSpaces(text, index + 1);
				continue;
			}
			break;
		}
		return value.length() == 0 ? null : value.toString();
	}

	private static int skipSpaces(String text, int index) {
		while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
			index++;
		}
		return index;
	}

	// @At 支持 @At("HEAD") 与 @At(value = "INVOKE", target = "...") 两种写法。
	private static String atValue(String atArgs) {
		Matcher value = VALUE_ATTRIBUTE.matcher(atArgs);
		if (value.find()) {
			return readStringExpression(atArgs, value.end());
		}
		return readStringExpression(atArgs, 0);
	}

	private static String targetValue(String atArgs) {
		Matcher target = TARGET_ATTRIBUTE.matcher(atArgs);
		return target.find() ? readStringExpression(atArgs, target.end()) : null;
	}

	private static String firstString(String args) {
		Matcher literal = STRING_LITERAL.matcher(args);
		return literal.find() ? unescape(literal.group(1)) : null;
	}

	private static String unescape(String value) {
		return value.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
