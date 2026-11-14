package com.gtzc.tweakify.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 单层基岩标记渲染：在方块顶面/底面各放一个凸出 1 像素的实心小方块（立体），
 * 不遮挡时从任何角度都能看出厚度。
 *
 * <p>几何在扫描结果变化时由 {@link BedrockGeometryCache} 预构建一次（含六个面与坐标），
 * 渲染帧只按相机高度挑选可见面并把缓存顶点平移写入，不再逐帧计算方块坐标。
 * 被其他方块挡住的部分由深度测试自然剔除。
 */
public final class BedrockHighlightRenderer {
	private static final BedrockGeometryCache CACHE = new BedrockGeometryCache();

	private BedrockHighlightRenderer() {
	}

	// 由 WorldOverlayRenderer 统一提交：把可见面的缓存顶点写进共享顶点缓冲。
	public static void drawGeometry(VertexConsumer vertices, PoseStack.Pose pose,
									List<BedrockHighlightState.Marker> markers, Vec3 camera) {
		CACHE.ensure(markers);
		CachedGeometry geometry = CACHE.geometry();
		for (BedrockGeometryCache.Segment segment : CACHE.segments()) {
			// 相机看不到的那一面在提交前就跳过，和原实现一致。
			if (!BedrockHighlightState.isFaceVisibleFrom(segment.top(), camera.y, segment.blockY())) {
				continue;
			}
			// 相减必须在 double 域完成：先把相机坐标降为 float 再减，
			// 远坐标（|x| ≥ 1e6）下量化误差会让高亮与方块错位。
			geometry.writeRange(vertices, pose, segment.start(), segment.count(),
				(float) (segment.blockX() - camera.x),
				(float) (segment.blockY() - camera.y),
				(float) (segment.blockZ() - camera.z));
		}
	}
}
