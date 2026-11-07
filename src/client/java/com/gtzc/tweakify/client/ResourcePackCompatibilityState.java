package com.gtzc.tweakify.client;

import java.nio.file.Path;

/**
 * 资源包兼容性提示开关的纯判定：只有当前界面目录与 Minecraft 的资源包目录一致时才生效，
 * 数据包使用自己的目录，保持原版行为。
 */
public final class ResourcePackCompatibilityState {
	private ResourcePackCompatibilityState() {
	}

	public static boolean shouldHideCompatibility(boolean enabled, Path packDirectory, Path resourcePackDirectory) {
		if (!enabled || packDirectory == null || resourcePackDirectory == null) {
			return false;
		}
		return packDirectory.toAbsolutePath().normalize()
			.equals(resourcePackDirectory.toAbsolutePath().normalize());
	}
}
