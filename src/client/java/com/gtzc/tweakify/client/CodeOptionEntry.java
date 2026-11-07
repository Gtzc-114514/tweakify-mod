package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 选项枚举的公共实现：code ↔ 枚举、显示名走语言文件、cycle 循环切换。
 *
 * <p>Java 枚举不能继承类，因此这里用带 default 方法的接口承载样板代码：
 * 各枚举只需声明常量、实现 {@link #code()} 与自己的静态 fromCode。
 */
interface CodeOptionEntry extends IConfigOptionListEntry {
	String code();

	// 显示名的翻译键；返回 null 表示由实现类自行提供显示名（如语言自称）。
	default String translationKey() {
		return null;
	}

	@Override
	default String getStringValue() {
		return this.code();
	}

	@Override
	default String getDisplayName() {
		String key = this.translationKey();
		return key == null ? this.code() : StringUtils.translate(key);
	}

	@Override
	default IConfigOptionListEntry cycle(boolean forward) {
		Object[] constants = getClass().getEnumConstants();
		int length = constants == null ? 0 : constants.length;
		if (length == 0) {
			return this;
		}
		int index = Math.floorMod(((Enum<?>) this).ordinal() + (forward ? 1 : -1), length);
		return (IConfigOptionListEntry) constants[index];
	}
}
