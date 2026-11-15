package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackCompatibilityStateTest {
	private static final Path RESOURCE_PACK_DIR = Path.of("game", "resourcepacks");
	private static final Path DATA_PACK_DIR = Path.of("game", "datapacks");

	@Test
	void hidesCompatibilityOnlyForResourcePackDirectory() {
		assertTrue(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, RESOURCE_PACK_DIR, RESOURCE_PACK_DIR));
		assertTrue(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, RESOURCE_PACK_DIR.toAbsolutePath(), RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, DATA_PACK_DIR, RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			false, RESOURCE_PACK_DIR, RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, null, RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, RESOURCE_PACK_DIR, null));
	}

	// 开启“同时忽略数据包”后不再要求目录匹配；关闭时仍只作用于资源包目录。
	@Test
	void alsoDataPacksExtendsScopeToAnyPackDirectory() {
		assertTrue(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, true, DATA_PACK_DIR, RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			false, true, DATA_PACK_DIR, RESOURCE_PACK_DIR));
		assertFalse(ResourcePackCompatibilityState.shouldHideCompatibility(
			true, true, null, RESOURCE_PACK_DIR));
	}
}
