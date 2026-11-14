package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 世界叠加层统一提交：基岩高亮与攻击范围共用同一次 debugFilledBox 提交，
 * 减少每帧的管线切换与 draw call（Vulkan 后端对提交次数更敏感）。
 *
 * <p>两者的顶点都在数据变化时由各自的缓存预构建，渲染帧只做视锥剔除与顶点写入；
 * 攻击范围的视锥剔除下沉到渲染器内部，避免每帧复制一份可见条目列表。
 */
public final class WorldOverlayRenderer {
	private WorldOverlayRenderer() {
	}

	public static void render(LevelRenderContext context) {
		Frustum frustum = context.levelState().cameraRenderState.cullFrustum;

		List<BedrockHighlightState.Marker> bedrock = BedrockHighlightState.markers();
		List<AttackRangeState.Entry> ranges = AttackRangeState.entries();
		if (bedrock.isEmpty() && ranges.isEmpty()) {
			return;
		}
		Vec3 camera = context.levelState().cameraRenderState.pos;
		// 提交前可见性：基岩按相机高度选面，攻击范围按视锥剔除；
		// 两边都不可见时不提交 0 顶点的几何（Vulkan 后端对提交次数敏感）。
		boolean bedrockVisible = !bedrock.isEmpty() && BedrockHighlightRenderer.hasVisible(bedrock, camera.y);
		boolean rangesVisible = AttackRangeRenderer.hasVisible(ranges, frustum);
		if (!bedrockVisible && !rangesVisible) {
			return;
		}
		// 数量上限在 AttackRangeState 按 attackRangeMaxTargets 统一截断，这里不再二次限制。
		boolean sphere = TweakifyMod.CONFIG != null
			&& TweakifyModConfig.ATTACK_RANGE_DISPLAY_SPHERE.equals(TweakifyMod.CONFIG.functions.attackRangeDisplayMode);
		boolean filled = TweakifyMod.CONFIG != null
			&& TweakifyModConfig.ATTACK_RANGE_RENDER_FILLED.equals(TweakifyMod.CONFIG.functions.attackRangeRenderMode);
		if (filled) {
			// 半透明实心模式：攻击范围与基岩高亮共用 debugFilledBox（有 OIT 固定开销）。
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
				(pose, vertices) -> {
					if (bedrockVisible) {
						BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera);
					}
					if (rangesVisible) {
						AttackRangeRenderer.drawFilled(vertices, pose, ranges, camera, sphere, frustum);
					}
				});
			return;
		}
		// 线框模式：基岩高亮仍用填充方块，攻击范围走无 OIT 的线条通道。
		if (bedrockVisible) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
				(pose, vertices) -> BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera));
		}
		if (rangesVisible) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.linesTranslucent(),
				(pose, vertices) -> AttackRangeRenderer.drawLines(vertices, pose, ranges, camera, sphere,
					frustum));
		}
	}
}
