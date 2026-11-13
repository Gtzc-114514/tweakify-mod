package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ShoulderParrotDanceState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.world.entity.animal.parrot.Parrot;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 鹦鹉肩膀跳舞：原版肩膀上的鹦鹉固定使用 {@code ON_SHOULDER} 姿态。
 *
 * <p>在图层写入姿态时拦截：附近唱片机正在播放时改写成 {@code PARTY}，
 * 让肩膀上的鹦鹉像基岩版一样跟着音乐跳舞。
 */
@Mixin(ParrotOnShoulderLayer.class)
public class ParrotOnShoulderLayerMixin {
	@Redirect(method = "submitOnShoulder", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/renderer/entity/state/ParrotRenderState;"
			+ "pose:Lnet/minecraft/client/model/animal/parrot/ParrotModel$Pose;",
		opcode = Opcodes.PUTFIELD))
	private void tweakify$danceOnShoulder(ParrotRenderState state, ParrotModel.Pose pose,
										  PoseStack poseStack, SubmitNodeCollector collector, int light,
										  AvatarRenderState avatarState, Parrot.Variant variant,
										  float yRot, float xRot, boolean leftShoulder) {
		state.pose = pose == ParrotModel.Pose.ON_SHOULDER
			&& ShoulderParrotDanceState.shouldDance(avatarState)
			? ParrotModel.Pose.PARTY
			: pose;
	}
}
