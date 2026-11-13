package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ForceCrawlState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 主动爬行：按住爬行键时把玩家姿态强制为 {@link Pose#SWIMMING}（爬行姿态）。
 *
 * <p>注入 {@code updatePlayerPose} 的 HEAD 并取消原版姿态计算：原版每 tick 都会
 * 根据是否游泳/潜行重新选择姿态，不取消的话强制姿态会被立刻覆盖。
 * 只有在爬行姿态放得下时才强制，钻出 1 格高空间后松开按键即可正常站起。
 */
@Mixin(Player.class)
public abstract class PlayerForceCrawlMixin {
	@Shadow
	protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void tweakify$forceCrawl(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!ForceCrawlState.shouldForceCrawl(self)) {
			return;
		}
		// 睡觉、滑翔、乘骑、飞行与旁观时保持原版姿态，避免与这些状态互相抢姿态。
		if (self.isSleeping() || self.isFallFlying() || self.isPassenger()
			|| self.getAbilities().flying || self.isSpectator()) {
			return;
		}
		if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
			self.setPose(Pose.SWIMMING);
			ci.cancel();
		}
	}
}
