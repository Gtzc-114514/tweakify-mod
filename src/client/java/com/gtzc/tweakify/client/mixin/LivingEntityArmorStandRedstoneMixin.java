package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.ArmorStandPoses;
import com.gtzc.tweakify.client.state.ArmorStandRedstoneTrigger;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ArmorStand.ArmorStandPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 更好的盔甲架：红石脉冲驱动姿态循环。
 *
 * <p>盔甲架没有自己的 {@code tick} 可注入，这里挂在 {@code LivingEntity.tick} 上，
 * 先用 instanceof 过滤，再按「上一 tick 无信号、本 tick 有信号」的上升沿切到下一个预设，
 * 因此持续通电不会反复切换。首次观测只播种状态：实体重建（区块重载/重进世界）后若线路仍通电，
 * 不能把“初次看到通电”当成脉冲，否则每次进世界都会白跳一个姿态。
 * 只在集成服务端执行，姿态由实体数据同步。
 */
@Mixin(LivingEntity.class)
public class LivingEntityArmorStandRedstoneMixin {
	// 上升沿状态机：首 tick 只播种电平，持续通电不会反复切换。
	@Unique
	private final ArmorStandRedstoneTrigger tweakify$armorStandPoseTrigger = new ArmorStandRedstoneTrigger();

	@Inject(method = "tick", at = @At("HEAD"))
	private void tweakify$cyclePoseOnRedstone(CallbackInfo ci) {
		if (!((Object) this instanceof ArmorStand stand)) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.betterArmorStands || !config.functions.armorStandPoses
			|| !config.functions.armorStandPosesRedstone) {
			return;
		}
		if (stand.level().isClientSide()) {
			return;
		}
		boolean powered = stand.level().hasNeighborSignal(stand.blockPosition());
		if (!this.tweakify$armorStandPoseTrigger.update(powered)) {
			return;
		}
		List<ArmorStandPose> presets = ArmorStandPoses.resolve(config.functions.armorStandPosePresets);
		if (presets.isEmpty()) {
			return;
		}
		stand.setArmorStandPose(ArmorStandPoses.next(presets, stand.getArmorStandPose()));
	}
}
