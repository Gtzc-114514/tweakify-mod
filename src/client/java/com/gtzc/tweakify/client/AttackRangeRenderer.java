package com.gtzc.tweakify.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 攻击范围渲染：使用无 OIT 的半透明线条，平面模式画双圆环，
 * 球体模式画经纬线框。线条的固定开销远低于 debugFilledBox 的透明通道。
 */
public final class AttackRangeRenderer {
	private static final int FLAT_COLOR = 0xAA3388FF;
	private static final int SPHERE_COLOR = 0x773388FF;
	private static final int FLAT_FILL_COLOR = 0x553388FF;
	private static final int SPHERE_FILL_COLOR = 0x2A3388FF;
	private static final int FLAT_SEGMENTS = 24;
	private static final int LAT_SEGMENTS = 8;
	private static final int LON_SEGMENTS = 8;
	private static final float RING_WIDTH = 0.08f;

	// 三角函数只算一次：圆环/球体每帧只是缩放和平移，避免逐实体、逐段计算 sin/cos。
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

	private AttackRangeRenderer() {
	}

	// 由 WorldOverlayRenderer 使用 linesTranslucent 提交。
	public static void drawLines(VertexConsumer vertices, PoseStack.Pose pose,
								 List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere) {
		if (sphere) {
			renderSphereLines(vertices, pose, entries, camera);
		} else {
			renderFlatLines(vertices, pose, entries, camera);
		}
	}

	// 半透明实心模式：由 WorldOverlayRenderer 使用 debugFilledBox 提交。
	public static void drawFilled(VertexConsumer vertices, PoseStack.Pose pose,
								  List<AttackRangeState.Entry> entries, Vec3 camera, boolean sphere) {
		if (sphere) {
			renderSphereFilled(vertices, pose, entries, camera);
		} else {
			renderFlatFilled(vertices, pose, entries, camera);
		}
	}

	// 平面：内外两个同心圆，远处减少分段。
	private static void renderFlatLines(VertexConsumer vertices, PoseStack.Pose pose,
										List<AttackRangeState.Entry> entries, Vec3 camera) {
		for (AttackRangeState.Entry entry : entries) {
			float centerX = (float) (entry.x() - camera.x);
			float centerY = (float) (entry.feetY() - camera.y);
			float centerZ = (float) (entry.z() - camera.z);
			double dx = entry.x() - camera.x;
			double dz = entry.z() - camera.z;
			double distanceSq = dx * dx + dz * dz;
			int step = distanceSq > 32.0 * 32.0 ? 4 : distanceSq > 16.0 * 16.0 ? 2 : 1;
			float outer = entry.radius();
			float inner = Math.max(0.0f, outer - RING_WIDTH);
			for (int i = 0; i < FLAT_SEGMENTS; i += step) {
				int next = (i + step) % FLAT_SEGMENTS;
				float cos1 = FLAT_COS[i];
				float sin1 = FLAT_SIN[i];
				float cos2 = FLAT_COS[next];
				float sin2 = FLAT_SIN[next];
				addLine(vertices, pose,
					centerX + outer * cos1, centerY, centerZ + outer * sin1,
					centerX + outer * cos2, centerY, centerZ + outer * sin2,
					FLAT_COLOR);
				if (inner > 0.0f) {
					addLine(vertices, pose,
						centerX + inner * cos1, centerY, centerZ + inner * sin1,
						centerX + inner * cos2, centerY, centerZ + inner * sin2,
						FLAT_COLOR);
				}
			}
		}
	}

	// 球体：纬度环 + 经线弧，远处降低经纬密度。
	private static void renderSphereLines(VertexConsumer vertices, PoseStack.Pose pose,
										  List<AttackRangeState.Entry> entries, Vec3 camera) {
		for (AttackRangeState.Entry entry : entries) {
			float centerX = (float) (entry.x() - camera.x);
			float centerY = (float) (entry.centerY() - camera.y);
			float centerZ = (float) (entry.z() - camera.z);
			double dx = entry.x() - camera.x;
			double dz = entry.z() - camera.z;
			double distanceSq = dx * dx + dz * dz;
			int step = distanceSq > 24.0 * 24.0 ? 2 : 1;
			float radius = entry.radius();
			// 纬度环：固定 theta，遍历经度。
			for (int lat = 0; lat <= LAT_SEGMENTS; lat += step) {
				float sinTheta = LAT_SIN[lat];
				float cosTheta = LAT_COS[lat];
				for (int lon = 0; lon < LON_SEGMENTS; lon += step) {
					int nextLon = (lon + step) % LON_SEGMENTS;
					addLine(vertices, pose,
						centerX + radius * sinTheta * LON_COS[lon], centerY + radius * cosTheta,
						centerZ + radius * sinTheta * LON_SIN[lon],
						centerX + radius * sinTheta * LON_COS[nextLon], centerY + radius * cosTheta,
						centerZ + radius * sinTheta * LON_SIN[nextLon],
						SPHERE_COLOR);
				}
			}
			// 经线弧：固定经度，遍历纬度。
			for (int lon = 0; lon < LON_SEGMENTS; lon += step) {
				float cosPhi = LON_COS[lon];
				float sinPhi = LON_SIN[lon];
				for (int lat = 0; lat < LAT_SEGMENTS; lat += step) {
					int nextLat = lat + step;
					addLine(vertices, pose,
						centerX + radius * LAT_SIN[lat] * cosPhi, centerY + radius * LAT_COS[lat],
						centerZ + radius * LAT_SIN[lat] * sinPhi,
						centerX + radius * LAT_SIN[nextLat] * cosPhi, centerY + radius * LAT_COS[nextLat],
						centerZ + radius * LAT_SIN[nextLat] * sinPhi,
						SPHERE_COLOR);
				}
			}
		}
	}

	private static void addLine(VertexConsumer vertices, PoseStack.Pose pose,
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
		vertices.addVertex(pose, ax, ay, az).setColor(color)
			.setNormal(pose, nx, ny, nz).setLineWidth(1.0f);
		vertices.addVertex(pose, bx, by, bz).setColor(color)
			.setNormal(pose, nx, ny, nz).setLineWidth(1.0f);
	}

	// 实心平面：内外圆环之间的四边形。
	private static void renderFlatFilled(VertexConsumer vertices, PoseStack.Pose pose,
									 List<AttackRangeState.Entry> entries, Vec3 camera) {
		for (AttackRangeState.Entry entry : entries) {
			float centerX = (float) (entry.x() - camera.x);
			float centerY = (float) (entry.feetY() - camera.y);
			float centerZ = (float) (entry.z() - camera.z);
			double dx = entry.x() - camera.x;
			double dz = entry.z() - camera.z;
			double distanceSq = dx * dx + dz * dz;
			int step = distanceSq > 32.0 * 32.0 ? 4 : distanceSq > 16.0 * 16.0 ? 2 : 1;
			float outer = entry.radius();
			float inner = Math.max(0.0f, outer - RING_WIDTH);
			for (int i = 0; i < FLAT_SEGMENTS; i += step) {
				int next = (i + step) % FLAT_SEGMENTS;
				float cos1 = FLAT_COS[i];
				float sin1 = FLAT_SIN[i];
				float cos2 = FLAT_COS[next];
				float sin2 = FLAT_SIN[next];
				addQuad(vertices, pose,
					centerX + inner * cos1, centerY, centerZ + inner * sin1,
					centerX + outer * cos1, centerY, centerZ + outer * sin1,
					centerX + outer * cos2, centerY, centerZ + outer * sin2,
					centerX + inner * cos2, centerY, centerZ + inner * sin2,
					FLAT_FILL_COLOR);
			}
		}
	}

	// 实心球体：经纬网格四边形。
	private static void renderSphereFilled(VertexConsumer vertices, PoseStack.Pose pose,
									   List<AttackRangeState.Entry> entries, Vec3 camera) {
		for (AttackRangeState.Entry entry : entries) {
			float centerX = (float) (entry.x() - camera.x);
			float centerY = (float) (entry.centerY() - camera.y);
			float centerZ = (float) (entry.z() - camera.z);
			double dx = entry.x() - camera.x;
			double dz = entry.z() - camera.z;
			double distanceSq = dx * dx + dz * dz;
			int step = distanceSq > 24.0 * 24.0 ? 2 : 1;
			float radius = entry.radius();
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
					addQuad(vertices, pose,
						centerX + radius * sin1 * cosPhi1, centerY + radius * cos1,
						centerZ + radius * sin1 * sinPhi1,
						centerX + radius * sin1 * cosPhi2, centerY + radius * cos1,
						centerZ + radius * sin1 * sinPhi2,
						centerX + radius * sin2 * cosPhi2, centerY + radius * cos2,
						centerZ + radius * sin2 * sinPhi2,
						centerX + radius * sin2 * cosPhi1, centerY + radius * cos2,
						centerZ + radius * sin2 * sinPhi1,
						SPHERE_FILL_COLOR);
				}
			}
		}
	}

	private static void addQuad(VertexConsumer vertices, PoseStack.Pose pose,
								float ax, float ay, float az,
								float bx, float by, float bz,
								float cx, float cy, float cz,
								float dx, float dy, float dz,
								int color) {
		vertices.addVertex(pose, ax, ay, az).setColor(color);
		vertices.addVertex(pose, bx, by, bz).setColor(color);
		vertices.addVertex(pose, cx, cy, cz).setColor(color);
		vertices.addVertex(pose, dx, dy, dz).setColor(color);
	}
}
