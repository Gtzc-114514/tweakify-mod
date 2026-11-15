package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 单层基岩标记的顶点缓存：markers 列表引用变化时重建一次，
 * 渲染帧只按相机高度挑选可见面并把缓存顶点平移写入。
 *
 * <p>坐标相对方块原点缓存（0..1 范围），写入时再加“方块坐标 - 相机坐标”，
 * 因此相机移动不会让缓存失效。
 */
final class BedrockGeometryCache {
	// 一个可见面：24 个顶点（6 个四边形），坐标相对方块原点。
	record Segment(int start, int count, int blockX, int blockY, int blockZ, boolean top) {
	}

	// 小方块截面占方块中间的 1/2。
	private static final float MIN_OFFSET = 0.25f;
	private static final float MAX_OFFSET = 0.75f;
	// 厚度 = 1 像素（1/16 格），从基岩表面向外凸起。
	private static final float THICKNESS = 1.0f / 16.0f;
	// 与基岩表面留出的极小间距，避免共面 z-fighting。
	private static final float SURFACE_GAP = 0.002f;
	static final int VERTICES_PER_BOX = 24;
	// 旧版固定金色：作为不带颜色参数的入口的默认值，保持测试与兼容调用不变。
	private static final int DEFAULT_FILL_COLOR = 0xFFFFC800;

	private List<BedrockHighlightState.Marker> cachedMarkers = List.of();
	private int cachedColor = -1;
	private final CachedGeometry geometry = new CachedGeometry();
	private final List<Segment> segments = new ArrayList<>();

	// 返回本次调用是否重建了缓存。
	boolean ensure(List<BedrockHighlightState.Marker> markers) {
		return ensure(markers, DEFAULT_FILL_COLOR);
	}

	// 颜色变化也重建：颜色烘在顶点里，不能只靠渲染帧改值。
	boolean ensure(List<BedrockHighlightState.Marker> markers, int color) {
		if (markers == this.cachedMarkers && color == this.cachedColor) {
			return false;
		}
		this.cachedMarkers = markers;
		this.cachedColor = color;
		rebuild(markers, color);
		return true;
	}

	List<Segment> segments() {
		return this.segments;
	}

	CachedGeometry geometry() {
		return this.geometry;
	}

	private void rebuild(List<BedrockHighlightState.Marker> markers, int color) {
		this.segments.clear();
		this.geometry.begin(false, false, markers.size() * VERTICES_PER_BOX * 2);
		for (BedrockHighlightState.Marker marker : markers) {
			BlockPos pos = marker.pos();
			if (marker.topExposed()) {
				// 顶面：方块顶面之上凸起 1 像素。
				int start = this.geometry.vertexCount();
				addBox(this.geometry, 1.0f + SURFACE_GAP, THICKNESS, color);
				this.segments.add(new Segment(start, VERTICES_PER_BOX,
					pos.getX(), pos.getY(), pos.getZ(), true));
			}
			if (marker.bottomExposed()) {
				// 底面：方块底面之下凸起 1 像素。
				int start = this.geometry.vertexCount();
				addBox(this.geometry, -SURFACE_GAP - THICKNESS, THICKNESS, color);
				this.segments.add(new Segment(start, VERTICES_PER_BOX,
					pos.getX(), pos.getY(), pos.getZ(), false));
			}
		}
	}

	// 实心长方体：六个面各写一个四边形（debugFilledBox 不剔除背面，正反面都能看到）。
	private static void addBox(CachedGeometry geometry, float bottomY, float height, int color) {
		float x1 = MIN_OFFSET;
		float x2 = MAX_OFFSET;
		float z1 = MIN_OFFSET;
		float z2 = MAX_OFFSET;
		float y1 = bottomY;
		float y2 = bottomY + height;
		addQuad(geometry, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
		addQuad(geometry, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
		addQuad(geometry, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
		addQuad(geometry, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
		addQuad(geometry, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
		addQuad(geometry, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
	}

	private static void addQuad(CachedGeometry geometry,
								float ax, float ay, float az,
								float bx, float by, float bz,
								float cx, float cy, float cz,
								float dx, float dy, float dz,
								int color) {
		geometry.add(ax, ay, az, color, 0.0f, 0.0f, 0.0f, 0.0f);
		geometry.add(bx, by, bz, color, 0.0f, 0.0f, 0.0f, 0.0f);
		geometry.add(cx, cy, cz, color, 0.0f, 0.0f, 0.0f, 0.0f);
		geometry.add(dx, dy, dz, color, 0.0f, 0.0f, 0.0f, 0.0f);
	}
}
