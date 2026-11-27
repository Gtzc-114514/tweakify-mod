package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SignContainerTargets;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 穿牌打开容器：告示牌挡在容器前面时，准星只能命中告示牌，
 * 于是原版只会去编辑告示牌（或把手里的方块放上去），容器永远打不开。
 *
 * <p>这里在 {@code startUseItem} 最前面改写一次交互目标：命中告示牌且未潜行时，
 * 若邻位（优先命中面的反方向）存在容器，就把同一个交互手势转给容器方块。
 * 潜行时完全不介入，保留告示牌的原版编辑/放置行为；容器打不开时也不取消，
 * 原版流程照常继续。
 *
 * <p>注入点用 require = 0：startUseItem 是最容易被战斗类模组覆盖的方法，
 * 冲突时功能失效即可，是否真正注入由行为探针确认。
 */
@Mixin(Minecraft.class)
public class SignOpenContainerMixin {
	@Shadow
	public LocalPlayer player;

	@Shadow
	public ClientLevel level;

	@Shadow
	public MultiPlayerGameMode gameMode;

	@Shadow
	public HitResult hitResult;

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$openContainerBehindSign(CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || this.player == null || this.level == null || this.gameMode == null) {
			return;
		}
		if (!(this.hitResult instanceof BlockHitResult blockHit)
			|| blockHit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos signPos = blockHit.getBlockPos();
		if (config.modified.signOpenContainerSkipWaxed
			&& this.level.getBlockEntity(signPos) instanceof SignBlockEntity sign
			&& sign.isWaxed()) {
			return;
		}
		if (!SignContainerTargets.shouldOpenContainer(config.modified.signOpenContainer,
			config.modified.signOpenContainerSneakBypass,
			SignContainerTargets.isSign(this.level.getBlockState(signPos)),
			this.player.isShiftKeyDown())) {
			return;
		}
		BlockPos containerPos = SignContainerTargets.findContainer(this.level, signPos, blockHit.getDirection());
		if (containerPos == null) {
			return;
		}
		// 用同一个命中方向的中心点重新构造命中结果：服务端只校验距离，不需要客户端射线一致。
		BlockHitResult target = new BlockHitResult(Vec3.atCenterOf(containerPos),
			blockHit.getDirection(), containerPos, false);
		InteractionResult result = this.gameMode.useItemOn(this.player, InteractionHand.MAIN_HAND, target);
		if (!result.consumesAction()) {
			return;
		}
		HookProbe.markOnce("signOpenContainer");
		// 与原版一致：使用主手物品自带的挥动动画。
		SwingAnimation animation = this.player.getItemInHand(InteractionHand.MAIN_HAND).getInteractAnimation();
		this.player.swing(InteractionHand.MAIN_HAND, animation, false);
		ci.cancel();
	}
}
