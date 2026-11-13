package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ConnectedBlockOutlineState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接方块轮廓：在方块轮廓提取完成后，把门、床、双箱、双层植物等整体合并成一个边框。
 *
 * <p>使用 {@code @At("TAIL")}：提取方法在没有命中方块时会提前 return，
 * 只在正常生成轮廓后替换 shape，避免对空状态重复处理。
 */
@Mixin(LevelExtractor.class)
public class LevelExtractorBlockOutlineMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$CONNECTED_OUTLINE_PROBE = new AtomicBoolean();

	@Inject(method = "extractBlockOutline", at = @At("TAIL"), require = 0)
	private void tweakify$mergeConnectedOutline(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$CONNECTED_OUTLINE_PROBE, "connectedBlockOutline");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.connectedBlockOutline) {
			return;
		}
		BlockOutlineRenderState outline = levelRenderState.blockOutlineRenderState;
		if (outline == null) {
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		BlockPos pos = outline.pos();
		BlockState state = level.getBlockState(pos);
		VoxelShape merged = ConnectedBlockOutlineState.mergedShape(state, pos, level);
		if (merged == null) {
			return;
		}
		// 保留原 state 的半透明/高对比度与调试用形状，只替换实际渲染的 shape。
		levelRenderState.blockOutlineRenderState = new BlockOutlineRenderState(pos, outline.isTranslucent(),
			outline.highContrast(), merged, outline.collisionShape(), outline.occlusionShape(),
			outline.interactionShape());
	}
}
