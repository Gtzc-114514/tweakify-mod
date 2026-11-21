package com.gtzc.tweakify.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 爆炸范围渲染：复用攻击范围的几何生成与视锥剔除，使用独立缓存并固定红色。
 */
final class ExplosionRangeRenderer {
	// 实心与线条各一份缓存，避免同一帧同时提交时反复重建。
	private static final AttackRangeGeometryCache LINE_CACHE = new AttackRangeGeometryCache();
	private static final AttackRangeGeometryCache FILL_CACHE = new AttackRangeGeometryCache();
	private static final int RED_RGB = AttackRangePalette.RED.rgb();

	private ExplosionRangeRenderer() {
	}

	static boolean hasVisible(List<AttackRangeState.Entry> entries, Frustum frustum) {
		return AttackRangeRenderer.hasVisible(entries, frustum);
	}

	static void drawLines(VertexConsumer vertices, PoseStack.Pose pose,
						  List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere,
						  Frustum frustum) {
		AttackRangeRenderer.drawLines(vertices, pose, entries, camera, sphere, frustum, LINE_CACHE, RED_RGB);
	}

	static void drawFilled(VertexConsumer vertices, PoseStack.Pose pose,
						   List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere,
						   Frustum frustum) {
		AttackRangeRenderer.drawFilled(vertices, pose, entries, camera, sphere, frustum, FILL_CACHE, RED_RGB);
	}
}
