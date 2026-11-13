package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
