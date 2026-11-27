package com.gtzc.tweakify.client.render;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 生物攻击范围的顶点缓存：每个条目预生成 3 档 LOD（step 1/2/4）的顶点，
 * 条目列表或显示/渲染模式变化时在后台重建一次，渲染帧只按距离选档并把缓存顶点平移写入。
 *
 * <p>坐标相对条目中心缓存，写入时再加“条目坐标 - 相机坐标”，
 * 相机移动不会让缓存失效；距离只决定使用哪一档 LOD。
 *
 * <p>双缓冲：构建在 {@link GeometryBuildWorker} 上进行，完成后一次性发布不可变
 * {@link Snapshot}；渲染线程始终读到上一份完整快照，不会看到半成品。每次请求分配
 * 递增 generation，旧任务开始时直接丢弃，发布侧也只接受更新的代。
 */
final class AttackRangeGeometryCache {
	// 每个条目的基准点与三档 LOD 几何；球体模式没有 step 4。
	record EntryGeometry(double baseX, double baseY, double baseZ,
						 CachedGeometry step1, CachedGeometry step2, CachedGeometry step4) {
		CachedGeometry forStep(int step) {
			return step >= 4 ? this.step4 : step == 2 ? this.step2 : this.step1;
		}
	}

	// 一次构建的完整产物：代与只读映射绑定发布，渲染一帧内只取同一次快照。
	static final class Snapshot {
		private final long generation;
		private final IdentityHashMap<AttackRangeState.Entry, EntryGeometry> geometries;

		// 包私有：测试注入旧代验证单调换代。
		Snapshot(long generation, IdentityHashMap<AttackRangeState.Entry, EntryGeometry> geometries) {
			this.generation = generation;
			this.geometries = geometries;
		}

		long generation() {
			return this.generation;
		}

		EntryGeometry geometry(AttackRangeState.Entry entry) {
			return this.geometries.get(entry);
		}

		int size() {
			return this.geometries.size();
		}
	}

	// 透明度按模式固定；RGB 由 attackRangePalette 决定，缓存按 (模式, 颜色) 失效。
	private static final int FLAT_ALPHA = 0xAA;
	private static final int SPHERE_ALPHA = 0x77;
	private static final int FLAT_FILL_ALPHA = 0x55;
	private static final int SPHERE_FILL_ALPHA = 0x2A;
	// 旧版蓝色：不带色调参数的入口（测试/兼容调用）使用。
	private static final int DEFAULT_RGB = 0x3388FF;
	static final int FLAT_SEGMENTS = 24;
	static final int LAT_SEGMENTS = 8;
	static final int LON_SEGMENTS = 8;
	private static final float RING_WIDTH = 0.08f;

	// 三角函数只算一次：重建几何时直接查表。
	private static final float[] FLAT_COS = new float[FLAT_SEGMENTS];
	private static final float[] FLAT_SIN = new float[FLAT_SEGMENTS];
	private static final float[] LAT_SIN = new float[LAT_SEGMENTS + 1];
	private static final float[] LAT_COS = new float[LAT_SEGMENTS + 1];
	private static final float[] LON_COS = new float[LON_SEGMENTS];
	private static final float[] LON_SIN = new float[LON_SEGMENTS];

	static {
		for (int i = 0; i < FLAT_SEGMENTS; i++) {
			double angle = Math.PI * 2.0 * i / FLAT_SEGMENTS;
			FLAT_COS[i] = (float) Math.cos(angle);
			FLAT_SIN[i] = (float) Math.sin(angle);
		}
		for (int lat = 0; lat <= LAT_SEGMENTS; lat++) {
			double theta = Math.PI * lat / LAT_SEGMENTS;
			LAT_SIN[lat] = (float) Math.sin(theta);
			LAT_COS[lat] = (float) Math.cos(theta);
		}
		for (int lon = 0; lon < LON_SEGMENTS; lon++) {
			double phi = Math.PI * 2.0 * lon / LON_SEGMENTS;
			LON_COS[lon] = (float) Math.cos(phi);
			LON_SIN[lon] = (float) Math.sin(phi);
		}
	}

	private static final Snapshot EMPTY = new Snapshot(0L, new IdentityHashMap<>());

	private List<AttackRangeState.Entry> cachedEntries = List.of();
	private boolean cachedSphere;
	private boolean cachedFilled;
	private int cachedRgb = -1;
	// 请求代与已发布快照：都在验证新旧，后台任务只能发布比当前更新的代。
	private final AtomicLong issuedGeneration = new AtomicLong();
	private final AtomicReference<Snapshot> published = new AtomicReference<>(EMPTY);

	// 返回本次调用是否提交了新的构建请求（空列表会立即发布空快照）。
	boolean ensure(List<AttackRangeState.Entry> entries, boolean sphere, boolean filled) {
		return ensure(entries, sphere, filled, DEFAULT_RGB);
	}

	// 颜色变化也重建：颜色烘在顶点里，不能只靠渲染帧改值。
	boolean ensure(List<AttackRangeState.Entry> entries, boolean sphere, boolean filled, int rgb) {
		if (entries == this.cachedEntries && sphere == this.cachedSphere && filled == this.cachedFilled
			&& rgb == this.cachedRgb) {
			return false;
		}
		this.cachedEntries = entries;
		this.cachedSphere = sphere;
		this.cachedFilled = filled;
		this.cachedRgb = rgb;
		long generation = this.issuedGeneration.incrementAndGet();
		// 空输入不必占用后台队列：直接发布空快照，同时让在途任务过期。
		if (entries.isEmpty()) {
			publish(new Snapshot(generation, new IdentityHashMap<>()));
			return true;
		}
		GeometryBuildWorker.submit(() -> rebuildIfCurrent(entries, sphere, filled, rgb, generation));
		return true;
	}

	// 渲染线程每帧取一次快照：同一帧内的查询都落在同一代上。
	Snapshot snapshot() {
		return this.published.get();
	}

	EntryGeometry geometry(AttackRangeState.Entry entry) {
		return snapshot().geometry(entry);
	}

	int cachedSize() {
		return snapshot().size();
	}

	// 后台构建：开始前丢弃已过期的请求；完成后交给发布侧的单调换代兜底。
	private void rebuildIfCurrent(List<AttackRangeState.Entry> entries, boolean sphere, boolean filled,
								  int rgb, long generation) {
		if (generation != this.issuedGeneration.get()) {
			return;
		}
		IdentityHashMap<AttackRangeState.Entry, EntryGeometry> built = new IdentityHashMap<>();
		for (AttackRangeState.Entry entry : entries) {
			// 平面模式以脚底为基准，球体模式以实体中心为基准。
			double baseY = sphere ? entry.centerY() : entry.feetY();
			built.put(entry, new EntryGeometry(entry.x(), baseY, entry.z(),
				build(entry.radius(), sphere, filled, 1, rgb),
				build(entry.radius(), sphere, filled, 2, rgb),
				build(entry.radius(), sphere, filled, 4, rgb)));
		}
		publish(new Snapshot(generation, built));
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

	private static CachedGeometry build(float radius, boolean sphere, boolean filled, int step, int rgb) {
		CachedGeometry geometry = new CachedGeometry();
		// 球体只用到 step 1/2（渲染层按距离只会在两者间选择）。
		if (sphere && step > 2) {
			return geometry;
		}
		if (filled) {
			if (sphere) {
				buildSphereFilled(geometry, radius, step, rgb);
			} else {
				buildFlatFilled(geometry, radius, step, rgb);
			}
		} else if (sphere) {
			buildSphereLines(geometry, radius, step, rgb);
		} else {
			buildFlatLines(geometry, radius, step, rgb);
		}
		return geometry;
	}

	// 透明度按模式固定，RGB 由调色板提供。
	private static int withAlpha(int alpha, int rgb) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	// 平面线框：内外两个同心圆，远处由调用方选更粗的 LOD。
	private static void buildFlatLines(CachedGeometry geometry, float radius, int step, int rgb) {
		int color = withAlpha(FLAT_ALPHA, rgb);
		int segments = (FLAT_SEGMENTS + step - 1) / step;
		float outer = radius;
		float inner = Math.max(0.0f, outer - RING_WIDTH);
		geometry.begin(true, true, segments * (inner > 0.0f ? 4 : 2));
		for (int i = 0; i < FLAT_SEGMENTS; i += step) {
			int next = (i + step) % FLAT_SEGMENTS;
			float cos1 = FLAT_COS[i];
			float sin1 = FLAT_SIN[i];
			float cos2 = FLAT_COS[next];
			float sin2 = FLAT_SIN[next];
			addLine(geometry, outer * cos1, 0.0f, outer * sin1, outer * cos2, 0.0f, outer * sin2,
				color);
			if (inner > 0.0f) {
				addLine(geometry, inner * cos1, 0.0f, inner * sin1, inner * cos2, 0.0f, inner * sin2,
					color);
			}
		}
	}

	// 球体：纬度环 + 经线弧，远处减少经纬密度。
	private static void buildSphereLines(CachedGeometry geometry, float radius, int step, int rgb) {
		int color = withAlpha(SPHERE_ALPHA, rgb);
		geometry.begin(true, true, 512);
		for (int lat = 0; lat <= LAT_SEGMENTS; lat += step) {
			float sinTheta = LAT_SIN[lat];
			float cosTheta = LAT_COS[lat];
			for (int lon = 0; lon < LON_SEGMENTS; lon += step) {
				int nextLon = (lon + step) % LON_SEGMENTS;
				addLine(geometry,
					radius * sinTheta * LON_COS[lon], radius * cosTheta,
					radius * sinTheta * LON_SIN[lon],
					radius * sinTheta * LON_COS[nextLon], radius * cosTheta,
					radius * sinTheta * LON_SIN[nextLon],
					color);
			}
		}
		for (int lon = 0; lon < LON_SEGMENTS; lon += step) {
			float cosPhi = LON_COS[lon];
			float sinPhi = LON_SIN[lon];
			for (int lat = 0; lat < LAT_SEGMENTS; lat += step) {
				int nextLat = lat + step;
				addLine(geometry,
					radius * LAT_SIN[lat] * cosPhi, radius * LAT_COS[lat],
					radius * LAT_SIN[lat] * sinPhi,
					radius * LAT_SIN[nextLat] * cosPhi, radius * LAT_COS[nextLat],
					radius * LAT_SIN[nextLat] * sinPhi,
					color);
			}
		}
	}

	// 实心平面：内外圆环之间的四边形。
	private static void buildFlatFilled(CachedGeometry geometry, float radius, int step, int rgb) {
		int color = withAlpha(FLAT_FILL_ALPHA, rgb);
		int segments = (FLAT_SEGMENTS + step - 1) / step;
		float outer = radius;
		float inner = Math.max(0.0f, outer - RING_WIDTH);
		geometry.begin(false, false, segments * 4);
		for (int i = 0; i < FLAT_SEGMENTS; i += step) {
			int next = (i + step) % FLAT_SEGMENTS;
			float cos1 = FLAT_COS[i];
			float sin1 = FLAT_SIN[i];
			float cos2 = FLAT_COS[next];
			float sin2 = FLAT_SIN[next];
			addQuad(geometry,
				inner * cos1, 0.0f, inner * sin1,
				outer * cos1, 0.0f, outer * sin1,
				outer * cos2, 0.0f, outer * sin2,
				inner * cos2, 0.0f, inner * sin2,
				color);
		}
	}

	// 实心球体：经纬网格四边形。
	private static void buildSphereFilled(CachedGeometry geometry, float radius, int step, int rgb) {
		int color = withAlpha(SPHERE_FILL_ALPHA, rgb);
		geometry.begin(false, false, 512);
		for (int lat = 0; lat < LAT_SEGMENTS; lat += step) {
			float sin1 = LAT_SIN[lat];
			float cos1 = LAT_COS[lat];
			float sin2 = LAT_SIN[lat + step];
			float cos2 = LAT_COS[lat + step];
			for (int lon = 0; lon < LON_SEGMENTS; lon += step) {
				int nextLon = (lon + step) % LON_SEGMENTS;
				float cosPhi1 = LON_COS[lon];
				float sinPhi1 = LON_SIN[lon];
				float cosPhi2 = LON_COS[nextLon];
				float sinPhi2 = LON_SIN[nextLon];
				addQuad(geometry,
					radius * sin1 * cosPhi1, radius * cos1, radius * sin1 * sinPhi1,
					radius * sin1 * cosPhi2, radius * cos1, radius * sin1 * sinPhi2,
					radius * sin2 * cosPhi2, radius * cos2, radius * sin2 * sinPhi2,
					radius * sin2 * cosPhi1, radius * cos2, radius * sin2 * sinPhi1,
					color);
			}
		}
	}

	private static void addLine(CachedGeometry geometry,
								float ax, float ay, float az,
								float bx, float by, float bz,
								int color) {
		float nx = bx - ax;
		float ny = by - ay;
		float nz = bz - az;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length > 1.0e-6f) {
			nx /= length;
			ny /= length;
			nz /= length;
		} else {
			nx = 0.0f;
			ny = 1.0f;
			nz = 0.0f;
		}
		geometry.add(ax, ay, az, color, nx, ny, nz, 1.0f);
		geometry.add(bx, by, bz, color, nx, ny, nz, 1.0f);
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
