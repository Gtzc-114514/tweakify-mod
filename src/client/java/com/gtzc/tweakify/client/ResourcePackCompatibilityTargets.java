package com.gtzc.tweakify.client;

/**
 * 资源包兼容性 Mixin 依赖的目标名称。集中在这里，启动自检与注解共用同一份常量，
 * 避免注解里写死字符串后与自检脱节。
 * 这些字符串属于产物 jar 的 official 命名空间，不能改成 Class#getName()，
 * 因为生产运行时类名会变成 intermediary。
 */
public final class ResourcePackCompatibilityTargets {
	public static final String SOURCE_NAMESPACE = "official";
	public static final String ENTRY_BASE_CLASS =
		"net.minecraft.client.gui.screens.packs.PackSelectionModel$EntryBase";
	public static final String SCREEN_CLASS =
		"net.minecraft.client.gui.screens.packs.PackSelectionScreen";
	public static final String ENTRY_COMPATIBILITY_METHOD = "getCompatibility";
	public static final String ENTRY_COMPATIBILITY_DESCRIPTOR =
		"()Lnet/minecraft/server/packs/repository/PackCompatibility;";
	public static final String SCREEN_PACK_DIR_FIELD = "packDir";
	public static final String SCREEN_PACK_DIR_DESCRIPTOR = "Ljava/nio/file/Path;";

	private ResourcePackCompatibilityTargets() {
	}
}
