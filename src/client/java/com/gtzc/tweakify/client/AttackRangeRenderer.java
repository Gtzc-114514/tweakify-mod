package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 攻击范围渲染：使用无 OIT 的半透明线条，平面模式画双圆环，
 * 球体模式画经纬线框。线条的固定开销远低于 debugFilledBox 的透明通道。
 *
 * <p>每个条目的三档 LOD 顶点由 {@link AttackRangeGeometryCache} 在条目列表或模式变化时
 * 预构建一次（含三角函数与法线归一化），渲染帧只按距离选档、做视锥剔除并把缓存顶点平移写入。
 */
public final class AttackRangeRenderer {
	private static final AttackRangeGeometryCache CACHE = new AttackRangeGeometryCache();

	private AttackRangeRenderer() {
	}

	// 由 WorldOverlayRenderer 使用 linesTranslucent 提交。
	public static void drawLines(VertexConsumer vertices, PoseStack.Pose pose,
								 List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere,
								 Frustum frustum) {
		CACHE.ensure(entries, sphere, false, paletteRgb());
		writeVisible(vertices, pose, entries, camera, sphere, frustum, CACHE.snapshot());
	}

	// 半透明实心模式：由 WorldOverlayRenderer 使用 debugFilledBox 提交。
	public static void drawFilled(VertexConsumer vertices, PoseStack.Pose pose,
								  List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere,
								  Frustum frustum) {
		CACHE.ensure(entries, sphere, true, paletteRgb());
		writeVisible(vertices, pose, entries, camera, sphere, frustum, CACHE.snapshot());
	}

	// 提交前判断：是否有条目在视锥内；与 writeVisible 共用同一判定，避免提交空几何。
	static boolean hasVisible(List<AttackRangeState.Entry> entries, Frustum frustum) {
		for (AttackRangeState.Entry entry : entries) {
			if (isVisible(entry, frustum)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isVisible(AttackRangeState.Entry entry, Frustum frustum) {
		return frustum == null || frustum.isVisible(entry.bounds());
	}

	// 调色板由配置决定；配置缺失时用旧版蓝色。
	private static int paletteRgb() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return AttackRangePalette.rgbOf(config == null ? TweakifyModConfig.ATTACK_RANGE_PALETTE_BLUE
			: config.functions.attackRangePalette);
	}

	private static void writeVisible(VertexConsumer vertices, PoseStack.Pose pose,
									 List<AttackRangeState.Entry> entries, Vec3 camera,
									 boolean sphere, Frustum frustum,
									 AttackRangeGeometryCache.Snapshot snapshot) {
		for (AttackRangeState.Entry entry : entries) {
			// 视锥剔除：只提交相机可见范围内的攻击范围，减少屏幕外几何。
			if (!isVisible(entry, frustum)) {
				continue;
			}
			AttackRangeGeometryCache.EntryGeometry geometry = snapshot.geometry(entry);
			if (geometry == null) {
				// 新条目还在后台构建：本帧跳过，下一次发布后自动出现。
				continue;
			}
			double dx = entry.x() - camera.x;
			double dz = entry.z() - camera.z;
			double distanceSq = dx * dx + dz * dz;
			// 远处降采样：平面 32/16 格，球体 24 格；命中哪一档就写哪一档的缓存顶点。
			int step = sphere
				? (distanceSq > 24.0 * 24.0 ? 2 : 1)
				: (distanceSq > 32.0 * 32.0 ? 4 : distanceSq > 16.0 * 16.0 ? 2 : 1);
			geometry.forStep(step).writeTo(vertices, pose,
				(float) (geometry.baseX() - camera.x),
				(float) (geometry.baseY() - camera.y),
				(float) (geometry.baseZ() - camera.z));
		}
	}
}
