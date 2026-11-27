package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏物品展示框：装有物品时清空展示框模型，只保留悬浮的物品。
 *
 * <p>只影响本地渲染：判定、碰撞、红石信号与地图内容都不变；空展示框保持原样，
 * 避免看不见的空框影响放置与拆除。
 */
@Mixin(ItemFrameRenderer.class)
public class ItemFrameHideMixin {
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;"
			+ "Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
		at = @At("TAIL")
	)
	private void tweakify$hideFilledFrame(ItemFrame frame, ItemFrameRenderState state,
									float partialTick, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.modified.hideItemFrames) {
			return;
		}
		boolean empty = state.item.isEmpty();
		if (empty && !config.modified.hideItemFramesHideEmpty) {
			return;
		}
		if (config.modified.hideItemFramesOnlyMaps && !frame.getItem().is(Items.FILLED_MAP)) {
			return;
		}
		state.frameModel.clear();
	}
}
