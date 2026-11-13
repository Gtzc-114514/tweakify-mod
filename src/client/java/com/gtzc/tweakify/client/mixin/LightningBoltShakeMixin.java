package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CameraMotion;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 雷击点燃方块时给相机运动记录一次低频长震动。
 */
@Mixin(LightningBolt.class)
public class LightningBoltShakeMixin {
	@Inject(method = "spawnFire", at = @At("RETURN"))
	private void tweakify$addThunderShake(int num, CallbackInfo callback) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.cameraMotion || !config.cameraMotionScreenShake) {
			return;
		}
		LightningBolt bolt = (LightningBolt) (Object) this;
		CameraMotion.addThunderShake(bolt.getX(), bolt.getY(), bolt.getZ());
	}
}
