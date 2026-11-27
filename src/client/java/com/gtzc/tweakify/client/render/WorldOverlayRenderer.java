package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 世界叠加层统一提交：基岩高亮、攻击范围、爆炸范围与横扫范围共用提交通道，
 * 减少每帧的管线切换与 draw call（Vulkan 后端对提交次数更敏感）。
 *
 * <p>攻击/爆炸范围沿用攻击范围的平面/球体与线框/实心模式；横扫范围的目标框
 * 线框、面框与球体各有独立开关，因此可能在同一次渲染中同时提交线条与实心几何。
 */
public final class WorldOverlayRenderer {
	private static boolean loggedFilledFallback;

	private WorldOverlayRenderer() {
	}

	public static void render(LevelRenderContext context) {
		Frustum frustum = context.levelState().cameraRenderState.cullFrustum;

		List<BedrockHighlightState.Marker> bedrock = BedrockHighlightState.markers();
		List<AttackRangeState.Entry> ranges = AttackRangeState.entries();
		List<AttackRangeState.Entry> explosions = ExplosionRangeState.entries();
		SweepRangeRenderer.State sweep = SweepRangeRenderer.current(Minecraft.getInstance());
		if (bedrock.isEmpty() && ranges.isEmpty() && explosions.isEmpty() && sweep == null) {
			return;
		}
		Vec3 camera = context.levelState().cameraRenderState.pos;
		boolean bedrockVisible = !bedrock.isEmpty() && BedrockHighlightRenderer.hasVisible(bedrock, camera.y);
		boolean rangesVisible = AttackRangeRenderer.hasVisible(ranges, frustum);
		boolean explosionsVisible = ExplosionRangeRenderer.hasVisible(explosions, frustum);
		// 横扫的三类几何独立判断：线框/面框只作用于目标框，球体单独开关。
		boolean sweepWireframe = SweepRangeRenderer.hasWireframe(sweep);
		boolean sweepFace = SweepRangeRenderer.hasFace(sweep);
		boolean sweepFaceSolid = SweepRangeRenderer.hasSolidFace(sweep);
		boolean sweepFaceGrid = sweepFace && !sweepFaceSolid;
		boolean sweepSphere = SweepRangeRenderer.hasSphere(sweep);
		boolean sweepVisible = SweepRangeRenderer.hasVisible(sweep, frustum);
		if (!bedrockVisible && !rangesVisible && !explosionsVisible && !sweepVisible) {
			return;
		}
		boolean sphere = TweakifyModConfig.ATTACK_RANGE_DISPLAY_SPHERE.equals(TweakifyMod.config().functions.attackRangeDisplayMode);
		boolean filled = TweakifyModConfig.ATTACK_RANGE_RENDER_FILLED.equals(TweakifyMod.config().functions.attackRangeRenderMode);
		if (filled) {
			// 半透明实心模式：基岩、攻击/爆炸范围与横扫球体共用 debugFilledBox；
			// 面框默认走线条网格，只有显式开启实心面框时才走 debugFilledBox。
			if (bedrockVisible || rangesVisible || explosionsVisible || sweepSphere || sweepFaceSolid) {
				context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
					(pose, vertices) -> {
						if (bedrockVisible) {
							BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera);
						}
						if (rangesVisible) {
							AttackRangeRenderer.drawFilled(vertices, pose, ranges, camera, sphere, frustum);
						}
						if (explosionsVisible) {
							ExplosionRangeRenderer.drawFilled(vertices, pose, explosions, camera, sphere, frustum);
						}
						if (sweepSphere) {
							SweepRangeRenderer.drawSphereFilled(vertices, pose, sweep, camera);
						}
						if (sweepFaceSolid) {
							SweepRangeRenderer.drawFaceSolid(vertices, pose, sweep, camera);
						}
					});
			}
			// 实心管线在 Vulkan/Vitrail 下可能不可见：范围几何始终再提交一份不依赖 OIT
			// 的线条，既保留实心外观，也保证至少有一层可见的范围轮廓。
			if (rangesVisible || explosionsVisible || sweepSphere || sweepWireframe || sweepFaceGrid) {
				if (!loggedFilledFallback) {
					loggedFilledFallback = true;
					TweakifyMod.LOGGER.info("Range overlays: filled mode, line fallback requested "
						+ "(ranges={}, explosions={}, sphere={}, wireframe={}, faceGrid={})",
						rangesVisible, explosionsVisible, sweepSphere, sweepWireframe, sweepFaceGrid);
				}
				context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.linesTranslucent(),
					(pose, vertices) -> {
						if (rangesVisible) {
							AttackRangeRenderer.drawLinesFilledFallback(vertices, pose, ranges, camera, sphere, frustum);
						}
						if (explosionsVisible) {
							ExplosionRangeRenderer.drawLines(vertices, pose, explosions, camera, sphere, frustum);
						}
						if (sweepSphere) {
							SweepRangeRenderer.drawSphereLines(vertices, pose, sweep, camera);
						}
						if (sweepWireframe) {
							SweepRangeRenderer.drawWireframe(vertices, pose, sweep, camera);
						}
						if (sweepFaceGrid) {
							SweepRangeRenderer.drawFace(vertices, pose, sweep, camera);
						}
					});
			}
			return;
		}
		// 线框模式：基岩高亮和可选实心面框仍用填充方块，范围几何走无 OIT 的线条通道。
		if (bedrockVisible || sweepFaceSolid) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
				(pose, vertices) -> {
					if (bedrockVisible) {
						BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera);
					}
					if (sweepFaceSolid) {
						SweepRangeRenderer.drawFaceSolid(vertices, pose, sweep, camera);
					}
				});
		}
		if (rangesVisible || explosionsVisible || sweepSphere || sweepWireframe || sweepFaceGrid) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.linesTranslucent(),
				(pose, vertices) -> {
					if (rangesVisible) {
						AttackRangeRenderer.drawLines(vertices, pose, ranges, camera, sphere, frustum);
					}
					if (explosionsVisible) {
						ExplosionRangeRenderer.drawLines(vertices, pose, explosions, camera, sphere, frustum);
					}
					if (sweepSphere) {
						SweepRangeRenderer.drawSphereLines(vertices, pose, sweep, camera);
					}
					if (sweepWireframe) {
						SweepRangeRenderer.drawWireframe(vertices, pose, sweep, camera);
					}
					if (sweepFaceGrid) {
						SweepRangeRenderer.drawFace(vertices, pose, sweep, camera);
					}
				});
		}
	}
}
