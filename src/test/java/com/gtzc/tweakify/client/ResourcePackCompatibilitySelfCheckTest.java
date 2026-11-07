package com.gtzc.tweakify.client;

import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackCompatibilitySelfCheckTest {
	@Test
	void realTargetsArePresent() {
		assertTrue(ResourcePackCompatibilitySelfCheck.findMissingTargets(
			PackSelectionModel.EntryBase.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			PackSelectionScreen.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD).isEmpty());
		assertTrue(ResourcePackCompatibilitySelfCheck.verifyTargets(
			PackSelectionModel.EntryBase.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			PackSelectionScreen.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD));
	}

	@Test
	void verifyUsesOfficialNamespaceAndMappedNames() {
		AtomicReference<String> namespace = new AtomicReference<>();
		MappingResolver resolver = resolverReturning("getCompatibility", "packDir", namespace);

		assertTrue(ResourcePackCompatibilitySelfCheck.verify(resolver));
		assertEquals("official", namespace.get());
	}

	@Test
	void verifyFailsWhenMappedTargetsAreMissing() {
		MappingResolver resolver = resolverReturning(
			"missingCompatibility", "missingPackDir", new AtomicReference<>());

		assertFalse(ResourcePackCompatibilitySelfCheck.verify(resolver));
	}

	@Test
	void missingMethodIsReported() {
		assertFalse(ResourcePackCompatibilitySelfCheck.verifyTargets(
			MissingMethod.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			PackSelectionScreen.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD));
	}

	@Test
	void wrongMethodReturnTypeIsReported() {
		assertFalse(ResourcePackCompatibilitySelfCheck.findMissingTargets(
			WrongReturn.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			PackSelectionScreen.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD).isEmpty());
	}

	@Test
	void missingFieldIsReported() {
		assertFalse(ResourcePackCompatibilitySelfCheck.verifyTargets(
			PackSelectionModel.EntryBase.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			MissingField.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD));
	}

	@Test
	void wrongFieldTypeIsReported() {
		assertFalse(ResourcePackCompatibilitySelfCheck.findMissingTargets(
			PackSelectionModel.EntryBase.class,
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
			WrongField.class,
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD).isEmpty());
	}

	@Test
	void classConstantsMatchClassLiterals() {
		assertEquals(PackSelectionModel.EntryBase.class.getName(),
			ResourcePackCompatibilityTargets.ENTRY_BASE_CLASS);
		assertEquals(PackSelectionScreen.class.getName(),
			ResourcePackCompatibilityTargets.SCREEN_CLASS);
	}

	@Test
	void descriptorsMatchReflection() throws Exception {
		Method method = PackSelectionModel.EntryBase.class.getDeclaredMethod(
			ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD);
		assertEquals(ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_DESCRIPTOR,
			Type.getMethodDescriptor(method));

		Field field = PackSelectionScreen.class.getDeclaredField(
			ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD);
		assertEquals(ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_DESCRIPTOR,
			Type.getDescriptor(field.getType()));
	}

	private static MappingResolver resolverReturning(String methodName, String fieldName,
													 AtomicReference<String> namespace) {
		return (MappingResolver) Proxy.newProxyInstance(
			MappingResolver.class.getClassLoader(),
			new Class<?>[]{MappingResolver.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "mapMethodName" -> {
					namespace.set((String) args[0]);
					yield methodName;
				}
				case "mapFieldName" -> fieldName;
				case "getNamespaces" -> List.of("official", "intermediary", "named");
				case "getCurrentRuntimeNamespace" -> "official";
				default -> null;
			});
	}

	private static final class MissingMethod {
	}

	private static final class WrongReturn {
		@SuppressWarnings("unused")
		public String getCompatibility() {
			return "";
		}
	}

	private static final class MissingField {
	}

	private static final class WrongField {
		@SuppressWarnings("unused")
		private String packDir;
	}
}
