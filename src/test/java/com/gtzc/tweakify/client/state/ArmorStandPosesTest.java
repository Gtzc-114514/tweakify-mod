package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.decoration.ArmorStand.ArmorStandPose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 盔甲架姿态预设：按配置筛选、未知代码忽略、循环与回绕。
 */
class ArmorStandPosesTest {
	// 姿态常量依赖实体类的静态初始化，先按其它测试的做法引导一次。
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void defaultPresetsResolveInBuiltInOrder() {
		List<ArmorStandPose> poses = ArmorStandPoses.resolve(TweakifyModConfig.defaultArmorStandPosePresets());

		assertEquals(TweakifyModConfig.SUPPORTED_ARMOR_STAND_POSE_PRESETS.size(), poses.size());
		assertEquals(ArmorStandPose.DEFAULT, poses.get(0), "第一个预设必须是站立姿态");
	}

	@Test
	void configuredOrderFollowsBuiltInTable() {
		List<ArmorStandPose> forward = ArmorStandPoses.resolve(
			List.of(TweakifyModConfig.ARMOR_STAND_POSE_DEFAULT, TweakifyModConfig.ARMOR_STAND_POSE_SITTING));
		List<ArmorStandPose> reversed = ArmorStandPoses.resolve(
			List.of(TweakifyModConfig.ARMOR_STAND_POSE_SITTING, TweakifyModConfig.ARMOR_STAND_POSE_DEFAULT));

		assertEquals(forward, reversed, "循环顺序固定按内置表，避免列表顺序影响红石结果");
	}

	@Test
	void unknownAndBlankCodesAreIgnored() {
		List<ArmorStandPose> poses = ArmorStandPoses.resolve(
			List.of("not_a_pose", "  ", TweakifyModConfig.ARMOR_STAND_POSE_SITTING));

		assertEquals(1, poses.size());
	}

	@Test
	void namespacedCodesAreAccepted() {
		// 旧版本可能把代码误写成命名空间形式，解析要能自愈。
		List<ArmorStandPose> poses = ArmorStandPoses.resolve(List.of("minecraft:sitting"));

		assertEquals(1, poses.size());
	}

	@Test
	void emptyListDisablesCycling() {
		assertTrue(ArmorStandPoses.resolve(List.of()).isEmpty());
		assertTrue(ArmorStandPoses.resolve(null).isEmpty());
	}

	@Test
	void nextWrapsAroundTheConfiguredList() {
		List<ArmorStandPose> poses = ArmorStandPoses.resolve(
			List.of(TweakifyModConfig.ARMOR_STAND_POSE_DEFAULT, TweakifyModConfig.ARMOR_STAND_POSE_SITTING));
		ArmorStandPose first = poses.get(0);
		ArmorStandPose second = poses.get(1);

		assertEquals(second, ArmorStandPoses.next(poses, first));
		assertEquals(first, ArmorStandPoses.next(poses, second), "走到末尾后回到第一个");
	}

	@Test
	void unknownCurrentPoseStartsFromFirstPreset() {
		List<ArmorStandPose> poses = ArmorStandPoses.resolve(
			List.of(TweakifyModConfig.ARMOR_STAND_POSE_SITTING));
		ArmorStandPose other = ArmorStandPoses.resolve(
			List.of(TweakifyModConfig.ARMOR_STAND_POSE_WAVING)).get(0);

		assertNotEquals(poses.get(0), other);
		assertEquals(poses.get(0), ArmorStandPoses.next(poses, other));
	}
}
