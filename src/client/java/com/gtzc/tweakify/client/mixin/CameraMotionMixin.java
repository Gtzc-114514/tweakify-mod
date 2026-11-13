package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CameraMotion;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把 {@link CameraMotion} 计算出的角度偏移叠加到相机的渲染状态上。
 *
 * <p>只修改 {@code CameraRenderState} 的 xRot/yRot、orientation 与
 * viewRotationMatrix，不改 Camera 的实际视角；因此鼠标视角、碰撞与准星
 * 判定保持原版，第一/第三人称可以分别开关。
 */
@Mixin(Camera.class)
public class CameraMotionMixin {
	@Unique
	private final CameraMotion.State tweakify$cameraMotion = new CameraMotion.State();

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tweakify$applyCameraMotion(CameraRenderState state, DeltaTracker deltaTracker,
			CallbackInfo callback) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.cameraMotion || player == null
				|| state.isPanoramicMode) {
			tweakify$cameraMotion.reset();
			return;
		}

		Entity vehicle = player.getVehicle();
		Entity controlled = vehicle != null ? vehicle : player;
		Vec3 velocity = controlled.getDeltaMovement();
		double cameraX = state.pos == null ? 0.0 : state.pos.x;
		double cameraY = state.pos == null ? 0.0 : state.pos.y;
		double cameraZ = state.pos == null ? 0.0 : state.pos.z;
		double deltaSeconds = deltaTracker.getRealtimeDeltaTicks() / 20.0;
		CameraMotion.Input input = new CameraMotion.Input(
			state.xRot, state.yRot,
			velocity.x, velocity.y, velocity.z,
			state.isFirstPerson,
			player.isSwimming(),
			player.isFallFlying(),
			player.isSprinting(),
			vehicle != null,
			vehicle instanceof Animal,
			vehicle instanceof VehicleEntity,
			state.isPanoramicMode,
			CameraMotion.nowSeconds(),
			deltaSeconds,
			cameraX, cameraY, cameraZ);
		CameraMotion.Offset offset = tweakify$cameraMotion.update(
			input, CameraMotion.Settings.fromConfig(config));
		if (offset.isZero()) {
			return;
		}
		tweakify$applyOffset(state, offset);
	}

	// 把三个轴的角度偏移合成一个局部旋转：orientation *= Rx * Ry * Rz，
	// 视图矩阵左乘它的逆，保证渲染朝向与相机朝向严格一致。
	@Unique
	private static void tweakify$applyOffset(CameraRenderState state, CameraMotion.Offset offset) {
		if (state.orientation == null || state.viewRotationMatrix == null) {
			return;
		}
		state.xRot += offset.pitch();
		state.yRot += offset.yaw();

		Quaternionf delta = new Quaternionf();
		if (offset.pitch() != 0.0f) {
			delta.rotateX(offset.pitch() * Mth.DEG_TO_RAD);
		}
		if (offset.yaw() != 0.0f) {
			delta.rotateY(offset.yaw() * Mth.DEG_TO_RAD);
		}
		if (offset.roll() != 0.0f) {
			delta.rotateZ(offset.roll() * Mth.DEG_TO_RAD);
		}
		state.orientation.mul(delta);

		Matrix4f inverse = new Matrix4f().rotation(new Quaternionf(delta).conjugate());
		state.viewRotationMatrix.mulLocal(inverse);
	}
}
