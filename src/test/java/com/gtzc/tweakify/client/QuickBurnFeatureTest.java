package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickBurnFeatureTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void burnToolsAreLavaBucketAndFlintAndSteel() {
		assertTrue(QuickBurnFeature.isBurnTool(Items.LAVA_BUCKET));
		assertTrue(QuickBurnFeature.isBurnTool(Items.FLINT_AND_STEEL));
		assertFalse(QuickBurnFeature.isBurnTool(Items.TORCH));
		assertFalse(QuickBurnFeature.isBurnTool(Items.AIR));
	}

	// 配置列表替换默认点燃物；配置缺失时仍回退到岩浆桶/打火石。
	@Test
	void configuredIgnitersReplaceDefaultTools() {
		TweakifyModConfig previous = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		config.setStringList("quickBurnItemIgniters", List.of("minecraft:torch"));
		TweakifyModTestSupport.installConfig(config);
		try {
			assertTrue(QuickBurnFeature.isConfiguredBurnTool(Items.TORCH));
			assertFalse(QuickBurnFeature.isConfiguredBurnTool(Items.LAVA_BUCKET));
		} finally {
			TweakifyModTestSupport.installConfig(previous);
		}
	}

	@Test
	void missingConfigFallsBackToDefaultTools() {
		TweakifyModConfig previous = TweakifyMod.config();
		TweakifyModTestSupport.installConfig(null);
		try {
			assertTrue(QuickBurnFeature.isConfiguredBurnTool(Items.LAVA_BUCKET));
			assertTrue(QuickBurnFeature.isConfiguredBurnTool(Items.FLINT_AND_STEEL));
			assertFalse(QuickBurnFeature.isConfiguredBurnTool(Items.TORCH));
		} finally {
			TweakifyModTestSupport.installConfig(previous);
		}
	}

	// 潜行门控：开启要求后，未潜行不触发（提示也同一条件），关闭时不需要潜行。
	@Test
	void sneakRequirementGatesTheTrigger() {
		assertTrue(QuickBurnFeature.isTriggerAllowed(false, false));
		assertFalse(QuickBurnFeature.isTriggerAllowed(true, false));
		assertTrue(QuickBurnFeature.isTriggerAllowed(true, true));
	}
}
