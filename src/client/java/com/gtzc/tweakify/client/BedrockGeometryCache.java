package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单层基岩标记的顶点缓存：markers 列表引用变化时在后台重建一次，
 * 渲染帧只按相机高度挑选可见面并把缓存顶点平移写入。
 *
 * <p>坐标相对方块原点缓存（0..1 范围），写入时再加“方块坐标 - 相机坐标”，
 * 因此相机移动不会让缓存失效。
 *
 * <p>双缓冲：几何数组与段表绑在同一次发布的 {@link Snapshot} 上，构建期间
 * 渲染继续读上一份完整快照；旧任务不会覆盖更新的代。
 */
final class BedrockGeometryCache {
	// 一个可见面：24 个顶点（6 个四边形），坐标相对方块原点。
	record Segment(int start, int count, int blockX, int blockY, int blockZ, boolean top) {
	}

	// 一次构建的完整产物：几何与段表必须成对发布，避免渲染读到跨代的数组与段。
	static final class Snapshot {
		private final long generation;
		private final CachedGeometry geometry;
		private final List<Segment> segments;

		// 包私有：测试注入旧代验证单调换代。
		Snapshot(long generation, CachedGeometry geometry, List<Segment> segments) {
			this.generation = generation;
			this.geometry = geometry;
			this.segments = segments;
		}

		long generation() {
			return this.generation;
		}

		CachedGeometry geometry() {
			return this.geometry;
		}

		List<Segment> segments() {
			return this.segments;
		}

		int size() {
			return this.segments.size();
		}
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

	private static final Snapshot EMPTY = new Snapshot(0L, new CachedGeometry(), List.of());

	private List<BedrockHighlightState.Marker> cachedMarkers = List.of();
	private int cachedColor = -1;
	// 请求代与已发布快照：后台任务只能发布比当前更新的代。
	private final AtomicLong issuedGeneration = new AtomicLong();
	private final AtomicReference<Snapshot> published = new AtomicReference<>(EMPTY);

	// 返回本次调用是否提交了新的构建请求（空列表会立即发布空快照）。
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
		long generation = this.issuedGeneration.incrementAndGet();
		// 空输入不必占用后台队列：直接发布空快照，同时让在途任务过期。
		if (markers.isEmpty()) {
			publish(new Snapshot(generation, new CachedGeometry(), List.of()));
			return true;
		}
		GeometryBuildWorker.submit(() -> rebuildIfCurrent(markers, color, generation));
		return true;
	}

	// 渲染线程每帧取一次快照：几何与段表来自同一代。
	Snapshot snapshot() {
		return this.published.get();
	}

	List<Segment> segments() {
		return snapshot().segments();
	}

	CachedGeometry geometry() {
		return snapshot().geometry();
	}

	// 后台构建：开始前丢弃已过期的请求；完成后交给发布侧的单调换代兜底。
	private void rebuildIfCurrent(List<BedrockHighlightState.Marker> markers, int color, long generation) {
		if (generation != this.issuedGeneration.get()) {
			return;
		}
		CachedGeometry geometry = new CachedGeometry();
		List<Segment> built = new ArrayList<>(markers.size() * 2);
		geometry.begin(false, false, markers.size() * VERTICES_PER_BOX * 2);
		for (BedrockHighlightState.Marker marker : markers) {
			BlockPos pos = marker.pos();
			if (marker.topExposed()) {
				// 顶面：方块顶面之上凸起 1 像素。
				int start = geometry.vertexCount();
				addBox(geometry, 1.0f + SURFACE_GAP, THICKNESS, color);
				built.add(new Segment(start, VERTICES_PER_BOX,
					pos.getX(), pos.getY(), pos.getZ(), true));
			}
			if (marker.bottomExposed()) {
				// 底面：方块底面之下凸起 1 像素。
				int start = geometry.vertexCount();
				addBox(geometry, -SURFACE_GAP - THICKNESS, THICKNESS, color);
				built.add(new Segment(start, VERTICES_PER_BOX,
					pos.getX(), pos.getY(), pos.getZ(), false));
			}
		}
		publish(new Snapshot(generation, geometry, List.copyOf(built)));
	}

	// 旧任务即使晚于新任务完成，也不能把更旧的快照盖回去（代严格单调）。
	// 包私有：测试直接注入旧代验证这条规则。
	void publish(Snapshot snapshot) {
		while (true) {
			Snapshot current = this.published.get();
			if (current.generation() > snapshot.generation()) {
				return;
			}
			if (this.published.compareAndSet(current, snapshot)) {
				return;
			}
		}
	}

	// 包私有：测试等待后台构建发布到当前请求代；超时返回 false。
	boolean awaitPublishedForTest(long timeoutMillis) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		while (System.nanoTime() < deadline) {
			if (this.published.get().generation() == this.issuedGeneration.get()) {
				return true;
			}
			try {
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
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
