package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CameraMotion;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 收到爆炸包时给相机运动记录一次震动（与 CameraOverhaul 的爆炸震动一致）。
 */
@Mixin(ClientPacketListener.class)
public class ExplosionShakeMixin {
	@Inject(method = "handleExplosion", at = @At("RETURN"))
	private void tweakify$addExplosionShake(ClientboundExplodePacket packet, CallbackInfo callback) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.cameraMotion || !config.functions.cameraMotionScreenShake) {
			return;
		}
		Vec3 center = packet.center();
		CameraMotion.addExplosionShake(center.x, center.y, center.z);
	}
}
