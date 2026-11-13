package com.gtzc.tweakify.config;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * 一个配置字段的定位信息：字段名、所属分区字段与实际字段。
 *
 * <p>{@code sectionField} 为 null 表示字段直接声明在 {@link TweakifyModConfig} 上，
 * 否则它是 facade 上持有分区的 final 字段。静态索引只保存 {@link Field} 引用、
 * 不保存任何实例，读写时再按分区字段解引用 owner，
 * 因此拆成多个分区后字段表仍能在类加载期一次性构建。
 */
record ConfigFieldRef(String name, Field sectionField, Field valueField) {
	Class<?> type() {
		return this.valueField.getType();
	}

	// 字段的完整声明类型（含泛型）：List<String> 与 List 在反序列化时的元素行为不同，
	// 必须用泛型类型才能保持与拆分前 Gson 反射绑定一致的字符串强制转换。
	Type genericType() {
		return this.valueField.getGenericType();
	}

	Object get(TweakifyModConfig config) {
		try {
			return this.valueField.get(owner(config));
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot read config field " + this.name, e);
		}
	}

	void set(TweakifyModConfig config, Object value) {
		try {
			this.valueField.set(owner(config), value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot write config field " + this.name, e);
		}
	}

	private Object owner(TweakifyModConfig config) {
		if (this.sectionField == null) {
			return config;
		}
		try {
			return this.sectionField.get(config);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot read config section " + this.sectionField.getName(), e);
		}
	}
}
