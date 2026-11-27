package com.gtzc.tweakify.client.state;

import java.nio.file.Path;

/**
 * 资源包兼容性提示开关的纯判定：只有当前界面目录与 Minecraft 的资源包目录一致时才生效，
 * 数据包使用自己的目录，保持原版行为。
 */
public final class ResourcePackCompatibilityState {
	private ResourcePackCompatibilityState() {
	}

	public static boolean shouldHideCompatibility(boolean enabled, Path packDirectory, Path resourcePackDirectory) {
		return shouldHideCompatibility(enabled, false, packDirectory, resourcePackDirectory);
	}

	// alsoDataPacks 开启时不再要求目录等于资源包目录，数据包界面也一并忽略兼容性。
	public static boolean shouldHideCompatibility(boolean enabled, boolean alsoDataPacks,
											 Path packDirectory, Path resourcePackDirectory) {
		if (!enabled || packDirectory == null || resourcePackDirectory == null) {
			return false;
		}
		if (alsoDataPacks) {
			return true;
		}
		return packDirectory.toAbsolutePath().normalize()
			.equals(resourcePackDirectory.toAbsolutePath().normalize());
	}
}
