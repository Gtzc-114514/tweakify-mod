package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界叠加层统一提交：基岩高亮与攻击范围共用同一次 debugFilledBox 提交，
 * 减少每帧的管线切换与 draw call（Vulkan 后端对提交次数更敏感）。
 * 屏障显示由 BarrierVisibilityState 使用原版 marker 粒子实现，不走这里。
 */
public final class WorldOverlayRenderer {
	// 视锥剔除的复用缓冲：避免每帧为剔除分配新列表（只在渲染线程使用）。
	private static final List<AttackRangeState.Entry> CULLED_RANGES = new ArrayList<>();

	private WorldOverlayRenderer() {
	}

	public static void render(LevelRenderContext context) {
		Frustum frustum = context.levelState().cameraRenderState.cullFrustum;

		List<BedrockHighlightState.Marker> bedrock = BedrockHighlightState.markers();
		List<AttackRangeState.Entry> ranges = AttackRangeState.entries();
		// 视锥剔除：只提交相机可见范围内的攻击范围，减少屏幕外几何。
		if (frustum != null && !ranges.isEmpty()) {
			CULLED_RANGES.clear();
			for (AttackRangeState.Entry entry : ranges) {
				if (frustum.isVisible(entry.bounds())) {
					CULLED_RANGES.add(entry);
				}
			}
			// 剔除结果是复用缓冲：必须拷一份不可变快照再交给延迟执行的几何构建，
			// 否则下帧 clear() 后 lambda 可能读到被改写的内容。
			ranges = List.copyOf(CULLED_RANGES);
		}
		if (bedrock.isEmpty() && ranges.isEmpty()) {
			return;
		}
		// 数量上限在 AttackRangeState 按 attackRangeMaxTargets 统一截断，这里不再二次限制。
		List<AttackRangeState.Entry> visibleRanges = ranges;
		boolean sphere = TweakifyMod.CONFIG != null
			&& TweakifyModConfig.ATTACK_RANGE_DISPLAY_SPHERE.equals(TweakifyMod.CONFIG.attackRangeDisplayMode);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		boolean filled = TweakifyMod.CONFIG != null
			&& TweakifyModConfig.ATTACK_RANGE_RENDER_FILLED.equals(TweakifyMod.CONFIG.attackRangeRenderMode);
		if (filled) {
			// 半透明实心模式：攻击范围与基岩高亮共用 debugFilledBox（有 OIT 固定开销）。
			if (!bedrock.isEmpty() || !visibleRanges.isEmpty()) {
				context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
					(pose, vertices) -> {
						if (!bedrock.isEmpty()) {
							BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera);
						}
						if (!visibleRanges.isEmpty()) {
							AttackRangeRenderer.drawFilled(vertices, pose, visibleRanges, camera, sphere);
						}
					});
			}
			return;
		}
		// 线框模式：基岩高亮仍用填充方块，攻击范围走无 OIT 的线条通道。
		if (!bedrock.isEmpty()) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(),
				(pose, vertices) -> BedrockHighlightRenderer.drawGeometry(vertices, pose, bedrock, camera));
		}
		if (!visibleRanges.isEmpty()) {
			context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.linesTranslucent(),
				(pose, vertices) -> AttackRangeRenderer.drawLines(vertices, pose, visibleRanges, camera, sphere));
		}
	}
}
