package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 横扫范围可视化，参考 {@code wlm3201/sweeping-view}：
 *
 * <ul>
 *   <li>目标碰撞箱水平扩大 1 格、垂直扩大 0.25 格的目标框，可分别开启线框与面框；</li>
 *   <li>以攻击者碰撞箱底面中心为圆心的 3 格球体攻击范围。</li>
 * </ul>
 *
 * <p>手持剑时至少显示球体（球体开关开启时）；瞄准实体时再附加目标框。
 * 线框、面框、球体各自有独立开关与颜色，攻击范围几何复用
 * {@link AttackRangeGeometryCache} 的缓存，方框每帧只有少量顶点，直接写入。
 */
final class SweepRangeRenderer {
	private static final float ATTACK_RADIUS = 3.0f;
	private static final int DEFAULT_RED_RGB = AttackRangePalette.RED.rgb();
	private static final float LINE_WIDTH = 1.0f;
	private static final int LINE_ALPHA = 0xCC;
	private static final int FACE_GRID_ALPHA = 0x88;
	private static final int FILL_ALPHA = 0x33;
	private static final double FACE_GRID_SPACING = 0.25;
	private static final AABB ATTACK_BOUNDS = new AABB(
		-ATTACK_RADIUS, -ATTACK_RADIUS, -ATTACK_RADIUS,
		ATTACK_RADIUS, ATTACK_RADIUS, ATTACK_RADIUS);
	private static final AttackRangeState.Entry ATTACK_ENTRY = new AttackRangeState.Entry(
		0.0, 0.0, 0.0, 0.0, ATTACK_RADIUS, ATTACK_BOUNDS, 0.0);
	private static final List<AttackRangeState.Entry> ATTACK_ENTRIES = List.of(ATTACK_ENTRY);
	// 实心与线条各用一份缓存：同一帧同时提交两种几何时，不能互相覆盖构建代。
	private static final AttackRangeGeometryCache SPHERE_LINE_CACHE = new AttackRangeGeometryCache();
	private static final AttackRangeGeometryCache SPHERE_FILL_CACHE = new AttackRangeGeometryCache();
	// 首次拿到可显示状态时记录一次，便于区分配置/状态未生效与渲染不可见。
	private static boolean loggedActive;

	private SweepRangeRenderer() {
	}

	record State(AABB targetBox, Vec3 attackCenter) {
	}

	// 手持剑时始终返回状态（至少显示球体）；准星命中生物、船或矿车时再附带目标框。
	static State current(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.sweepingView
			|| client.player == null || client.level == null) {
			loggedActive = false;
			return null;
		}
		if (!client.player.getMainHandItem().is(ItemTags.SWORDS)) {
			return null;
		}
		AABB playerBox = client.player.getBoundingBox();
		Vec3 center = new Vec3(playerBox.getCenter().x, playerBox.minY + 0.05,
			playerBox.getCenter().z);
		Entity target = client.crosshairPickEntity;
		boolean allowedTarget = target != null && target != client.player && target.isAlive()
			&& !target.isRemoved()
			&& (target instanceof LivingEntity
				|| config.functions.sweepingViewVehicles
				&& (target instanceof AbstractBoat || target instanceof AbstractMinecart));
		State state;
		if (!allowedTarget) {
			state = new State(null, center);
		} else {
			AABB box = target.getBoundingBox().inflate(1.0, 0.25, 1.0);
			state = new State(box, center);
		}
		boolean sphere = toggle("sphere");
		boolean targetBox = state.targetBox() != null;
		boolean wireframe = targetBox && toggle("wireframe");
		boolean face = targetBox && toggle("face");
		boolean faceSolid = face && toggle("faceSolid");
		if (!loggedActive && (sphere || wireframe || face)) {
			loggedActive = true;
			TweakifyMod.LOGGER.info("Sweeping view state: sphere={}, wireframe={}, face={}, "
				+ "faceSolid={}, targetBox={}", sphere, wireframe, face, faceSolid, targetBox);
		}
		return state;
	}

	static boolean hasWireframe(State state) {
		return state != null && state.targetBox() != null && toggle("wireframe");
	}

	static boolean hasFace(State state) {
		return state != null && state.targetBox() != null && toggle("face");
	}

	static boolean hasSolidFace(State state) {
		return hasFace(state) && toggle("faceSolid");
	}

	static boolean hasSphere(State state) {
		return state != null && toggle("sphere");
	}

	static boolean hasVisible(State state, Frustum frustum) {
		if (state == null) {
			return false;
		}
		boolean boxVisible = state.targetBox() != null
			&& (hasWireframe(state) || hasFace(state))
			&& (frustum == null || frustum.isVisible(state.targetBox()));
		boolean sphereVisible = hasSphere(state)
			&& (frustum == null || frustum.isVisible(sphereBounds(state)));
		return boxVisible || sphereVisible;
	}

	static void drawWireframe(VertexConsumer vertices, PoseStack.Pose pose, State state, Vec3 camera) {
		if (!hasWireframe(state)) {
			return;
		}
		drawBoxLines(vertices, pose, state.targetBox(), camera,
			withAlpha(LINE_ALPHA, color("wireframe")));
	}

	// 面框使用网格线填充：Vulkan/Vitrail 对自定义 DEBUG_FILLED_BOX 管线支持不可靠，
	// 而 linesTranslucent 已验证可显示；网格线仍独立于 sweepingViewWireframe。
	static void drawFace(VertexConsumer vertices, PoseStack.Pose pose, State state, Vec3 camera) {
		if (!hasFace(state)) {
			return;
		}
		drawFaceGrid(vertices, pose, state.targetBox(), camera,
			withAlpha(FACE_GRID_ALPHA, color("face")));
	}

	// 实心四边形面框：可选，走原版 debugFilledBox/OIT，Vulkan 下可能不可见。
	static void drawFaceSolid(VertexConsumer vertices, PoseStack.Pose pose, State state, Vec3 camera) {
		if (!hasSolidFace(state)) {
			return;
		}
		drawBoxFilled(vertices, pose, state.targetBox(), camera,
			withAlpha(FILL_ALPHA, color("face")));
	}

	static void drawSphereLines(VertexConsumer vertices, PoseStack.Pose pose, State state, Vec3 camera) {
		if (!hasSphere(state)) {
			return;
		}
		drawAttackRange(vertices, pose, state.attackCenter(), camera, SPHERE_LINE_CACHE, false,
			color("sphere"));
	}

	static void drawSphereFilled(VertexConsumer vertices, PoseStack.Pose pose, State state, Vec3 camera) {
		if (!hasSphere(state)) {
			return;
		}
		drawAttackRange(vertices, pose, state.attackCenter(), camera, SPHERE_FILL_CACHE, true,
			color("sphere"));
	}

	private static AABB sphereBounds(State state) {
		Vec3 center = state.attackCenter();
		return new AABB(
			center.x - ATTACK_RADIUS, center.y - ATTACK_RADIUS, center.z - ATTACK_RADIUS,
			center.x + ATTACK_RADIUS, center.y + ATTACK_RADIUS, center.z + ATTACK_RADIUS);
	}

	private static void drawAttackRange(VertexConsumer vertices, PoseStack.Pose pose, Vec3 center,
										Vec3 camera, AttackRangeGeometryCache cache, boolean filled, int rgb) {
		cache.ensure(ATTACK_ENTRIES, true, filled, rgb);
		AttackRangeGeometryCache.EntryGeometry geometry =
			cache.snapshot().geometry(ATTACK_ENTRY);
		if (geometry == null) {
			return;
		}
		geometry.forStep(1).writeTo(vertices, pose,
			(float) (center.x - camera.x),
			(float) (center.y - camera.y),
			(float) (center.z - camera.z));
	}

	private static void drawBoxLines(VertexConsumer vertices, PoseStack.Pose pose, AABB box,
									 Vec3 camera, int color) {
		double minX = box.minX;
		double minY = box.minY;
		double minZ = box.minZ;
		double maxX = box.maxX;
		double maxY = box.maxY;
		double maxZ = box.maxZ;
		// 底面
		line(vertices, pose, camera, minX, minY, minZ, maxX, minY, minZ, color);
		line(vertices, pose, camera, maxX, minY, minZ, maxX, minY, maxZ, color);
		line(vertices, pose, camera, maxX, minY, maxZ, minX, minY, maxZ, color);
		line(vertices, pose, camera, minX, minY, maxZ, minX, minY, minZ, color);
		// 顶面
		line(vertices, pose, camera, minX, maxY, minZ, maxX, maxY, minZ, color);
		line(vertices, pose, camera, maxX, maxY, minZ, maxX, maxY, maxZ, color);
		line(vertices, pose, camera, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		line(vertices, pose, camera, minX, maxY, maxZ, minX, maxY, minZ, color);
		// 竖直棱
		line(vertices, pose, camera, minX, minY, minZ, minX, maxY, minZ, color);
		line(vertices, pose, camera, maxX, minY, minZ, maxX, maxY, minZ, color);
		line(vertices, pose, camera, maxX, minY, maxZ, maxX, maxY, maxZ, color);
		line(vertices, pose, camera, minX, minY, maxZ, minX, maxY, maxZ, color);
	}

	private static void line(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
							 double x1, double y1, double z1, double x2, double y2, double z2,
							 int color) {
		// linesTranslucent 的顶点格式含法线：线宽方向由法线决定，缺法线会让整条线
		// 在 GPU 端退化为零长度。这里按线段方向归一化后同时写给两个端点。
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length > 1.0e-6f) {
			dx /= length;
			dy /= length;
			dz /= length;
		} else {
			dx = 0.0f;
			dy = 1.0f;
			dz = 0.0f;
		}
		vertex(vertices, pose, camera, x1, y1, z1, dx, dy, dz, color);
		vertex(vertices, pose, camera, x2, y2, z2, dx, dy, dz, color);
	}

	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
							   double x, double y, double z, float nx, float ny, float nz,
							   int color) {
		vertices.addVertex(pose,
			(float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
			.setColor(color)
			.setNormal(pose, nx, ny, nz)
			.setLineWidth(LINE_WIDTH);
	}

	// 在六个面上按固定间距画网格线，形成不依赖 OIT 的半透明面框。
	private static void drawFaceGrid(VertexConsumer vertices, PoseStack.Pose pose, AABB box,
									 Vec3 camera, int color) {
		double minX = box.minX;
		double minY = box.minY;
		double minZ = box.minZ;
		double maxX = box.maxX;
		double maxY = box.maxY;
		double maxZ = box.maxZ;
		double epsilon = 1.0e-6;
		for (double x = minX; x <= maxX + epsilon; x += FACE_GRID_SPACING) {
			line(vertices, pose, camera, x, minY, minZ, x, maxY, minZ, color);
			line(vertices, pose, camera, x, minY, maxZ, x, maxY, maxZ, color);
			line(vertices, pose, camera, x, minY, minZ, x, minY, maxZ, color);
			line(vertices, pose, camera, x, maxY, minZ, x, maxY, maxZ, color);
		}
		for (double y = minY; y <= maxY + epsilon; y += FACE_GRID_SPACING) {
			line(vertices, pose, camera, minX, y, minZ, maxX, y, minZ, color);
			line(vertices, pose, camera, minX, y, maxZ, maxX, y, maxZ, color);
			line(vertices, pose, camera, minX, y, minZ, minX, y, maxZ, color);
			line(vertices, pose, camera, maxX, y, minZ, maxX, y, maxZ, color);
		}
		for (double z = minZ; z <= maxZ + epsilon; z += FACE_GRID_SPACING) {
			line(vertices, pose, camera, minX, minY, z, maxX, minY, z, color);
			line(vertices, pose, camera, minX, maxY, z, maxX, maxY, z, color);
			line(vertices, pose, camera, minX, minY, z, minX, maxY, z, color);
			line(vertices, pose, camera, maxX, minY, z, maxX, maxY, z, color);
		}
	}

	private static void drawBoxFilled(VertexConsumer vertices, PoseStack.Pose pose, AABB box,
									  Vec3 camera, int color) {
		double minX = box.minX;
		double minY = box.minY;
		double minZ = box.minZ;
		double maxX = box.maxX;
		double maxY = box.maxY;
		double maxZ = box.maxZ;
		// debugFilledBox 关闭背面剔除，六个面各写一个四边形。
		quad(vertices, pose, camera, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
		quad(vertices, pose, camera, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		quad(vertices, pose, camera, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);
		quad(vertices, pose, camera, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		quad(vertices, pose, camera, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
		quad(vertices, pose, camera, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
	}

	private static void quad(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
							 double x1, double y1, double z1, double x2, double y2, double z2,
							 double x3, double y3, double z3, double x4, double y4, double z4,
							 int color) {
		filledVertex(vertices, pose, camera, x1, y1, z1, color);
		filledVertex(vertices, pose, camera, x2, y2, z2, color);
		filledVertex(vertices, pose, camera, x3, y3, z3, color);
		filledVertex(vertices, pose, camera, x4, y4, z4, color);
	}

	private static void filledVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
									 double x, double y, double z, int color) {
		vertices.addVertex(pose,
			(float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
			.setColor(color);
	}

	// 开关与颜色：配置尚未加载时按默认（开启/红色）处理。
	private static boolean toggle(String group) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null) {
			return true;
		}
		return switch (group) {
			case "wireframe" -> config.functions.sweepingViewWireframe;
			case "face" -> config.functions.sweepingViewFace;
			case "faceSolid" -> config.functions.sweepingViewFaceSolid;
			default -> config.functions.sweepingViewSphere;
		};
	}

	private static int color(String group) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null) {
			return DEFAULT_RED_RGB;
		}
		String code = switch (group) {
			case "wireframe" -> config.functions.sweepingViewWireframeColor;
			case "face" -> config.functions.sweepingViewFaceColor;
			default -> config.functions.sweepingViewSphereColor;
		};
		return code == null ? DEFAULT_RED_RGB : AttackRangePalette.rgbOf(code);
	}

	private static int withAlpha(int alpha, int rgb) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}
}
