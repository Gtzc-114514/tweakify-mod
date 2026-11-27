package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ArmorStand.ArmorStandPose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 盔甲架姿态预设（参考基岩版）：提供一组固定的 {@link ArmorStandPose} 与循环逻辑。
 *
 * <p>预设按代码固定顺序循环；配置列表只决定「参与循环的集合与顺序以外」的取舍——
 * 实际顺序始终以本表为准，避免列表顺序变化导致红石脉冲的下一姿态不可预测。
 */
public final class ArmorStandPoses {
	private static final Map<String, ArmorStandPose> PRESETS = buildPresets();

	private ArmorStandPoses() {
	}

	private static Map<String, ArmorStandPose> buildPresets() {
		Rotations sittingLegs = new Rotations(-90.0F, 0.0F, 0.0F);
		Map<String, ArmorStandPose> presets = new LinkedHashMap<>();
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_DEFAULT, ArmorStandPose.DEFAULT);
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_SITTING, new ArmorStandPose(
			ArmorStand.DEFAULT_HEAD_POSE, ArmorStand.DEFAULT_BODY_POSE,
			ArmorStand.DEFAULT_LEFT_ARM_POSE, ArmorStand.DEFAULT_RIGHT_ARM_POSE,
			sittingLegs, sittingLegs));
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_WAVING, new ArmorStandPose(
			new Rotations(0.0F, -20.0F, 0.0F), ArmorStand.DEFAULT_BODY_POSE,
			ArmorStand.DEFAULT_LEFT_ARM_POSE, new Rotations(-160.0F, 0.0F, 0.0F),
			ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE));
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_BOWING, new ArmorStandPose(
			new Rotations(15.0F, 0.0F, 0.0F), new Rotations(25.0F, 0.0F, 0.0F),
			new Rotations(-15.0F, 0.0F, 0.0F), new Rotations(-15.0F, 0.0F, 0.0F),
			ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE));
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_THINKING, new ArmorStandPose(
			new Rotations(10.0F, -15.0F, 0.0F), ArmorStand.DEFAULT_BODY_POSE,
			ArmorStand.DEFAULT_LEFT_ARM_POSE, new Rotations(-100.0F, 0.0F, -15.0F),
			ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE));
		presets.put(TweakifyModConfig.ARMOR_STAND_POSE_CHEERING, new ArmorStandPose(
			new Rotations(-10.0F, 0.0F, 0.0F), ArmorStand.DEFAULT_BODY_POSE,
			new Rotations(-170.0F, 0.0F, 0.0F), new Rotations(-170.0F, 0.0F, 0.0F),
			ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE));
		return Collections.unmodifiableMap(presets);
	}

	// 按配置列表解析出可用姿态：未知代码忽略，重复姿态只保留一次，顺序按本表。
	public static List<ArmorStandPose> resolve(List<String> codes) {
		List<ArmorStandPose> poses = new ArrayList<>();
		if (codes == null) {
			return poses;
		}
		for (Map.Entry<String, ArmorStandPose> entry : PRESETS.entrySet()) {
			if (!containsCode(codes, entry.getKey())) {
				continue;
			}
			if (!poses.contains(entry.getValue())) {
				poses.add(entry.getValue());
			}
		}
		return poses;
	}

	private static boolean containsCode(List<String> codes, String code) {
		for (String raw : codes) {
			if (raw == null) {
				continue;
			}
			String normalized = raw.trim().toLowerCase(Locale.ROOT);
			// 容忍被旧版本误写成命名空间形式的代码（minecraft:sitting），让已污染的配置自愈。
			if (normalized.startsWith("minecraft:")) {
				normalized = normalized.substring("minecraft:".length());
			}
			if (normalized.equals(code)) {
				return true;
			}
		}
		return false;
	}

	// 循环到下一个预设：当前姿态不在列表里时从第一个开始。
	public static ArmorStandPose next(List<ArmorStandPose> presets, ArmorStandPose current) {
		if (presets.isEmpty()) {
			return current;
		}
		int index = presets.indexOf(current);
		return presets.get((index + 1) % presets.size());
	}
}
