package com.gtzc.tweakify.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T4：在 Fabric Loader JUnit 的 CLIENT 环境中真实加载客户端 Mixin。
 *
 * <p>本测试运行在独立的 loaderTest 源集，避免 Fabric Loader JUnit 改变普通单测
 * 的注册表/tags 初始化状态。断言 {@code CreativeModeInventoryScreen} 已被织入
 * {@code CreativeModeInventoryGroupingMixin} 的 {@code @Unique} 字段，
 * 且目标方法 {@code containerTick} 仍存在；注入配置错误会在类加载时直接失败。
 *
 * <p>转发决策本身由 {@code CreativeItemGroupsTest} 覆盖，源码接线由
 * {@code CreativeModeInventoryGroupingMixinContractTest} 覆盖；本测试只验证
 * “Mixin 在真实 Loader 环境中被应用”。
 */
class CreativeModeInventoryGroupingMixinLoaderTest {
	@Test
	void clientMixinIsAppliedByFabricLoader() throws Exception {
		assertEquals(EnvType.CLIENT, FabricLoader.getInstance().getEnvironmentType(),
			"Fabric Loader JUnit 必须以 client 环境运行");
		Class<?> screen = Class.forName(
			"net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen", false,
			CreativeModeInventoryGroupingMixinLoaderTest.class.getClassLoader());
		assertTrue(hasField(screen, "tweakify$lastConfigSignature"),
			"CreativeModeInventoryGroupingMixin 的 @Unique 字段未被织入");
		assertTrue(hasMethod(screen, "containerTick"),
			"Mixin 目标方法 containerTick 不存在");
	}

	private static boolean hasField(Class<?> type, String name) {
		for (Field field : type.getDeclaredFields()) {
			if (field.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMethod(Class<?> type, String name) {
		for (Method method : type.getDeclaredMethods()) {
			if (method.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
