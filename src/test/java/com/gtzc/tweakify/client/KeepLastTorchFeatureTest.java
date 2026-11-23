package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeepLastTorchFeatureTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void allTorchItemsAreRecognized() {
		assertTrue(KeepLastTorchFeature.isTorch(Items.TORCH));
		assertTrue(KeepLastTorchFeature.isTorch(Items.SOUL_TORCH));
		assertTrue(KeepLastTorchFeature.isTorch(Items.COPPER_TORCH));
		assertTrue(KeepLastTorchFeature.isTorch(Items.REDSTONE_TORCH));
		assertFalse(KeepLastTorchFeature.isTorch(Items.STONE));
		assertFalse(KeepLastTorchFeature.isTorch(Items.AIR));
	}

	@Test
	void onlyTheLastTorchIsBlocked() {
		assertTrue(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 1));
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 2));
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.STONE, 1));
	}

	@Test
	void offhandSpareAllowsPlacingLastTorch() {
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 1, 1, true));
		assertTrue(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 1, 1, false));
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.STONE, 1, 1, true),
			"非火把仍然不拦截");
	}

	// 可配置阈值：剩余数量不超过阈值时阻挡；阈值夹紧到 1 ~ 8。
	@Test
	void thresholdBlocksUpToConfiguredCount() {
		assertTrue(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 3, 3));
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 4, 3));
		assertTrue(KeepLastTorchFeature.shouldBlockPlacement(Items.TORCH, 1, 0),
			"低于下限时夹紧到 1");
		assertFalse(KeepLastTorchFeature.shouldBlockPlacement(Items.STONE, 1, 8));
	}
}
