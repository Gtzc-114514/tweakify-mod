package com.gtzc.tweakify.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 单层基岩标记渲染：在方块顶面/底面各放一个凸出 1 像素的实心小方块（立体），
 * 不遮挡时从任何角度都能看出厚度。所有标记合并成一次自定义几何提交，
 * 被其他方块挡住的部分由深度测试自然剔除；相机看不到的那一面在提交前就跳过。
 */
public final class BedrockHighlightRenderer {
	private static final int FILL_COLOR = 0xFFFFC800;
	// 小方块截面占方块中间的 1/2。
	private static final float MIN_OFFSET = 0.25f;
	private static final float MAX_OFFSET = 0.75f;
	// 厚度 = 1 像素（1/16 格），从基岩表面向外凸起。
	private static final float THICKNESS = 1.0f / 16.0f;
	// 与基岩表面留出的极小间距，避免共面 z-fighting。
	private static final float SURFACE_GAP = 0.002f;

	private BedrockHighlightRenderer() {
	}

	// 由 WorldOverlayRenderer 统一提交：把标记几何写进同一个顶点缓冲。
	public static void drawGeometry(VertexConsumer vertices, PoseStack.Pose pose,
									List<BedrockHighlightState.Marker> markers, Vec3 camera) {
		for (BedrockHighlightState.Marker marker : markers) {
			BlockPos pos = marker.pos();
			float x1 = (float) (pos.getX() - camera.x) + MIN_OFFSET;
			float x2 = (float) (pos.getX() - camera.x) + MAX_OFFSET;
			float y = (float) (pos.getY() - camera.y);
			float z1 = (float) (pos.getZ() - camera.z) + MIN_OFFSET;
			float z2 = (float) (pos.getZ() - camera.z) + MAX_OFFSET;
			if (marker.topExposed()
				&& BedrockHighlightState.isFaceVisibleFrom(true, camera.y, pos.getY())) {
				float bottom = y + 1.0f + SURFACE_GAP;
				addBox(vertices, pose, x1, bottom, z1, x2, bottom + THICKNESS, z2);
			}
			if (marker.bottomExposed()
				&& BedrockHighlightState.isFaceVisibleFrom(false, camera.y, pos.getY())) {
				float top = y - SURFACE_GAP;
				addBox(vertices, pose, x1, top - THICKNESS, z1, x2, top, z2);
			}
		}
	}

	// 实心长方体：六个面各写一个四边形（debugFilledBox 不剔除背面，正反面都能看到）。
	private static void addBox(VertexConsumer vertices, PoseStack.Pose pose,
							   float x1, float y1, float z1, float x2, float y2, float z2) {
		addQuad(vertices, pose, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		addQuad(vertices, pose, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
		addQuad(vertices, pose, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
		addQuad(vertices, pose, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
		addQuad(vertices, pose, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
		addQuad(vertices, pose, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
	}

	private static void addQuad(VertexConsumer vertices, PoseStack.Pose pose,
								float ax, float ay, float az,
								float bx, float by, float bz,
								float cx, float cy, float cz,
								float dx, float dy, float dz) {
		vertices.addVertex(pose, ax, ay, az).setColor(FILL_COLOR);
		vertices.addVertex(pose, bx, by, bz).setColor(FILL_COLOR);
		vertices.addVertex(pose, cx, cy, cz).setColor(FILL_COLOR);
		vertices.addVertex(pose, dx, dy, dz).setColor(FILL_COLOR);
	}
}
