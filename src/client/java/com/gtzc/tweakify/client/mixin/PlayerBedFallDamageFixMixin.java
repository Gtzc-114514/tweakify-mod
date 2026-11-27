package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BedFallDamageFix;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MC-19830：从高处落入床中时，原版会把落地伤害推迟到离床后才结算。
 *
 * <p>上床成功时立即按当前 {@code fallDistance} 结算并清零；只在服务端执行，
 * 避免客户端预测重复扣血。客户端模组只能覆盖单人/局域网主机。
 */
@Mixin(Player.class)
public class PlayerBedFallDamageFixMixin {
	@Inject(method = "startSleepInBed", at = @At("RETURN"))
	private void tweakify$applyPendingFallDamage(AbstractBedBlock bedBlock, BlockState bedBlockState,
												 BedRule rule, BlockPos pos,
												 CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.fix.bedFallDamageFix) {
			return;
		}
		Player player = (Player) (Object) this;
		if (player.level().isClientSide() || player.fallDistance <= 0.0) {
			return;
		}
		Either<Player.BedSleepingProblem, Unit> result = cir.getReturnValue();
		if (result == null || result.right().isEmpty()) {
			return;
		}
		player.causeFallDamage(BedFallDamageFix.damageDistance(
			player.fallDistance, player.level().getBlockState(pos).getBlock().getFallDistanceReduction()),
			1.0F, player.damageSources().fall());
		player.resetFallDistance();
	}
}
