package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.repository.PackCompatibility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动自检：Mixin 使用字符串方法名和 @Accessor 字段名，目标改名时需要明确暴露。
 * 这里用 MappingResolver 把 official 名称映射到开发/生产运行时名称，再检查目标是否存在；
 * 失败只输出醒目 ERROR 并返回 false，不拖垮整个客户端，实际生效仍由 HookProbe 确认。
 */
public final class ResourcePackCompatibilitySelfCheck {
	private ResourcePackCompatibilitySelfCheck() {
	}

	public static void verify() {
		verify(FabricLoader.getInstance().getMappingResolver());
	}

	static boolean verify(MappingResolver resolver) {
		String methodName = resolver.mapMethodName(ResourcePackCompatibilityTargets.SOURCE_NAMESPACE,
			ResourcePackCompatibilityTargets.ENTRY_BASE_CLASS,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_DESCRIPTOR);
		String fieldName = resolver.mapFieldName(ResourcePackCompatibilityTargets.SOURCE_NAMESPACE,
			ResourcePackCompatibilityTargets.SCREEN_CLASS,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_DESCRIPTOR);
		boolean ok = verifyTargets(PackSelectionModel.EntryBase.class, methodName,
			PackSelectionScreen.class, fieldName);
		if (ok) {
			TweakifyMod.LOGGER.info("Resource pack compatibility targets present (method {}, field {}); "
				+ "watch for 'Mixin hook active: resourcePackCompatibility' to confirm activation",
				methodName, fieldName);
		}
		return ok;
	}

	static boolean verifyTargets(Class<?> entryBaseClass, String methodName, Class<?> screenClass,
								 String fieldName) {
		List<String> missing = findMissingTargets(entryBaseClass, methodName, screenClass, fieldName);
		if (!missing.isEmpty()) {
			TweakifyMod.LOGGER.error("Tweakify resource pack compatibility targets are missing: {}. "
					+ "The feature will stay disabled; update ResourcePackCompatibilityMixin/"
					+ "PackSelectionScreenAccessor for this Minecraft version.",
				String.join("; ", missing));
			return false;
		}
		return true;
	}

	static List<String> findMissingTargets(Class<?> entryBaseClass, String methodName,
										   Class<?> screenClass, String fieldName) {
		List<String> missing = new ArrayList<>();
		try {
			Method method = entryBaseClass.getDeclaredMethod(methodName);
			if (method.getReturnType() != PackCompatibility.class) {
				missing.add("method " + entryBaseClass.getName() + "." + methodName + " returns "
					+ method.getReturnType().getName() + " instead of PackCompatibility");
			}
		} catch (NoSuchMethodException e) {
			missing.add("method " + entryBaseClass.getName() + "." + methodName + " is missing");
		}
		try {
			Field field = screenClass.getDeclaredField(fieldName);
			if (field.getType() != Path.class) {
				missing.add("field " + screenClass.getName() + "." + fieldName + " has type "
					+ field.getType().getName() + " instead of Path");
			}
		} catch (NoSuchFieldException e) {
			missing.add("field " + screenClass.getName() + "." + fieldName + " is missing");
		}
		return missing;
	}
}
