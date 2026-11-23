package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.LocatorBarHeads;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定位栏头像：与参考实现 {@code Haage001/locator-heads} 相同，直接替换原版圆点的
 * {@code blitSprite} 调用——头像沿用圆点自身的坐标（9x9 槽位）与按距离缩放的贴图尺寸，
 * 过滤命中或“只显示附近”时整条航点连同圆点一起隐藏。
 */
@Mixin(LocatorBar.class)
public class LocatorBarHeadsMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$LOCATOR_HEADS_PROBE = new AtomicBoolean();

	// 26.3 的圆点使用 5 个 int 的 blitSprite（x/y/宽/高/颜色）；箭头才是 4 个 int 的重载。
	// 圆点绘制在 forEachWaypoint 的 lambda 里，因此 method = "*" 让绕点搜索覆盖合成方法。
	@WrapOperation(method = "*", require = 1,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
				+ "(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
	private void tweakify$replaceDotWithHead(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
											  int x, int y, int width, int height, int color,
											  Operation<Void> original,
											  @Local(argsOnly = true) Entity entity,
											  @Local(argsOnly = true) Level level,
											  @Local(argsOnly = true) TrackedWaypoint waypoint) {
		HookProbe.markOnce(TWEAKIFY$LOCATOR_HEADS_PROBE, "locatorBarHeads");
		LocatorBarHeads.DotResult result = LocatorBarHeads.renderDot(graphics, Minecraft.getInstance(),
			entity, level, waypoint, x, y, width, height);
		if (result == LocatorBarHeads.DotResult.VANILLA) {
			original.call(graphics, pipeline, sprite, x, y, width, height, color);
		}
	}
}
