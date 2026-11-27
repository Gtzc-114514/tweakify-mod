package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ChunkCenterIndicator;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 区块中心指示：原版区块边界渲染完成后，在所在区块中心补一条竖线。
 *
 * <p>只在原版 {@code ChunkBorderRenderer} 真正运行时追加，因此与 F3+G 的显示状态天然同步。
 */
@Mixin(ChunkBorderRenderer.class)
public class ChunkBorderRendererChunkCenterMixin {
	@Inject(method = "emitGizmos", at = @At("RETURN"))
	private void tweakify$drawChunkCenter(double camX, double camY, double camZ, DebugValueAccess debugValues,
									  Frustum frustum, float partialTicks, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.chunkCenterIndicator) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			return;
		}
		Vec3[] line = ChunkCenterIndicator.centerLine(
			SectionPos.of(camera.blockPosition()), client.level.getMinY(), client.level.getMaxY());
		Gizmos.line(line[0], line[1], ChunkCenterIndicator.LINE_COLOR, ChunkCenterIndicator.LINE_WIDTH);
	}
}
