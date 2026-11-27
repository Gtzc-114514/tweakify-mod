package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.render.WorldPlayTimeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 存档列表条目：在条目内容右上角绘制游玩时长。
 *
 * <p>时长数据在 fillLevels 时读取并缓存（见 {@link WorldSelectionListPlayTimeMixin}），
 * 这里只做文本宽度与右对齐计算，不做文件 IO。
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryPlayTimeMixin {
	@Inject(method = "extractContent", at = @At("TAIL"))
	private void tweakify$drawPlayTime(GuiGraphicsExtractor gui, int mouseX, int mouseY,
									   boolean hovered, float partialTicks, CallbackInfo ci) {
		WorldPlayTimeRenderer.draw(gui, (WorldSelectionList.WorldListEntry) (Object) this);
	}
}
