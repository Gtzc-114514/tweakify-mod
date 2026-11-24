package com.gtzc.tweakify.client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-19830：提前结算时也必须应用床的 50% 下落伤害减免。
 */
class BedFallDamageFixTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
	@Test
	void appliesBedFallDistanceReduction() {
		assertEquals(5.0F, BedFallDamageFix.damageDistance(10.0, 0.5F), 1.0E-4F,
			"10 格落到床上应为 5 格伤害");
		assertEquals(10.0F, BedFallDamageFix.damageDistance(10.0, 0.0F), 1.0E-4F,
			"没有减免时保持完整距离");
		assertEquals(2.0F, BedFallDamageFix.damageDistance(10.0, 0.8F), 1.0E-4F);
	}

	@Test
	void bedBlockReportsHalfReduction() {
		assertEquals(0.5F, Blocks.BED.red().getFallDistanceReduction(), 1.0E-4F,
			"床的方块属性应与原版 fallOn 减免一致");
	}

	@Test
	void clampsInvalidReduction() {
		assertEquals(10.0F, BedFallDamageFix.damageDistance(10.0, -0.5F), 1.0E-4F);
		assertEquals(0.0F, BedFallDamageFix.damageDistance(10.0, 1.5F), 1.0E-4F);
	}
}
