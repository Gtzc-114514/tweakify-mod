package com.gtzc.tweakify.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置分区基类：每个分区持有自己的一组配置字段，并负责这些字段的清洗。
 *
 * <p>分区实例以 public final 字段声明在 {@link TweakifyModConfig} 上，
 * 由 {@link TweakifyModConfig#fieldRefs()} 递归索引成扁平字段表；
 * JSON 键名等于字段名，因此按分区拆分不改变配置文件格式与快照键。
 */
abstract class ConfigSection {
	// 执行本分区的字段级清洗，返回是否修正了值；没有校验器的分区保持默认实现。
	boolean sanitize() {
		return false;
	}

	// 已登记字段级校验器的字段名，供启动期覆盖断言与测试使用。
	List<String> sanitizedFieldNames() {
		return List.of();
	}

	@FunctionalInterface
	interface FieldSanitizerAction {
		boolean sanitize();
	}

	// 一个校验器可以负责多个字段：成对的数值需要一起夹紧。
	record FieldSanitizer(List<String> fields, FieldSanitizerAction action) {
		static FieldSanitizer of(String field, FieldSanitizerAction action) {
			return new FieldSanitizer(List.of(field), action);
		}
	}

	// 执行全部校验器，返回是否修正了值。
	static boolean runSanitizers(List<FieldSanitizer> sanitizers) {
		boolean changed = false;
		for (FieldSanitizer sanitizer : sanitizers) {
			changed |= sanitizer.action().sanitize();
		}
		return changed;
	}

	// 已登记校验器的字段名。
	static List<String> fieldNamesOf(List<FieldSanitizer> sanitizers) {
		List<String> names = new ArrayList<>();
		for (FieldSanitizer sanitizer : sanitizers) {
			names.addAll(sanitizer.fields());
		}
		return names;
	}

	// 启动期断言：校验器登记表必须与分区字段一一对应。
	// 三类问题都会在分区构造期直接失败：登记了不存在的字段名（拼错或跨分区）、
	// 同一字段重复登记、本分区的数值/枚举字段漏登记。
	// 布尔开关、列表与热键由 facade 按类型统一清洗，不要求登记。
	final void assertSanitizerCoverage(List<String> coveredFieldNames) {
		Set<String> declared = new HashSet<>();
		for (Field field : getClass().getDeclaredFields()) {
			if (!isIndexSkipped(field)) {
				declared.add(field.getName());
			}
		}

		Set<String> covered = new HashSet<>();
		for (String name : coveredFieldNames) {
			if (!declared.contains(name)) {
				throw new IllegalStateException("Sanitizer registered for unknown config field: "
					+ name + " in " + getClass().getSimpleName());
			}
			if (!covered.add(name)) {
				throw new IllegalStateException("Config field has more than one sanitizer: "
					+ name + " in " + getClass().getSimpleName());
			}
		}

		for (Field field : getClass().getDeclaredFields()) {
			if (isIndexSkipped(field)) {
				continue;
			}
			Class<?> type = field.getType();
			if (type == boolean.class || List.class.isAssignableFrom(type)
				|| (type == String.class && field.getName().startsWith(TweakifyModConfig.HOTKEY_FIELD_PREFIX))) {
				continue;
			}
			if (!covered.contains(field.getName())) {
				throw new IllegalStateException("Config field without sanitizer: " + field.getName());
			}
		}
	}

	// 与字段索引保持同一口径：static / transient / synthetic 都不是配置项。
	private static boolean isIndexSkipped(Field field) {
		int modifiers = field.getModifiers();
		return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic();
	}
}
