package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.ArmorStandPoses;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ArmorStand.ArmorStandPose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 更好的盔甲架：空手潜行右键盔甲架时循环到下一个预设姿态（参考基岩版的姿势操作）。
 *
 * <p>只在集成服务端执行：姿态通过实体数据同步给客户端，远程服务器不会出现
 * 「本地看起来变了、服务端没变」的假象。普通右键（装备/取物）保持原版。
 */
@Mixin(ArmorStand.class)
public class ArmorStandPoseMixin {
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void tweakify$cyclePoseOnSneakUse(Player player, InteractionHand hand, Vec3 hitPos,
			CallbackInfoReturnable<InteractionResult> cir) {
		ArmorStand stand = (ArmorStand) (Object) this;
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.betterArmorStands || !config.functions.armorStandPoses) {
			return;
		}
		if (stand.level().isClientSide()) {
			return;
		}
		// 标记盔甲架保留原版交互（通常用于装饰/碰撞标记，不参与姿态循环）。
		if (stand.isMarker()) {
			return;
		}
		if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
			return;
		}
		List<ArmorStandPose> presets = ArmorStandPoses.resolve(config.functions.armorStandPosePresets);
		if (presets.isEmpty()) {
			return;
		}
		stand.setArmorStandPose(ArmorStandPoses.next(presets, stand.getArmorStandPose()));
		cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
	}
}
