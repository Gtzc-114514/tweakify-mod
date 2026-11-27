package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用液体推动：26.3 把流体推力收敛到 {@code EntityFluidInteraction.applyCurrentTo}，
 * 由它把累积的水流/岩浆流按标签施加到实体速度上。
 *
 * <p>只拦这一步：流体高度的追踪（{@code update}）和浮力/下沉逻辑照常运行，
 * 因此在水里仍然能浮、能溺水，只是不再被水流推着走。
 */
@Mixin(EntityFluidInteraction.class)
public class FluidPushMixin {
	@Inject(method = "applyCurrentTo", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableFluidPush(TagKey<Fluid> fluidTag, Entity entity, double strength,
										   CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.modified.disableFluidPush) {
			return;
		}
		if (fluidTag == FluidTags.WATER && config.modified.disableWaterPush) {
			ci.cancel();
		} else if (fluidTag == FluidTags.LAVA && config.modified.disableLavaPush) {
			ci.cancel();
		}
	}
}
