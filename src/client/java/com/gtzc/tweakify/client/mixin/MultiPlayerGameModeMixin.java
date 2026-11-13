package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.MiningCooldownState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 挖掘/攻击冷却与“切换物品不打断挖掘”。
 *
 * <p>destroyBlock / sameDestroyTarget / attack 是战斗与挖掘类模组的必争之地，
 * 四个注入点全部 require = 0：被其它模组整体重写时功能失效即可，不让注入失败拖垮启动。
 * 每个钩子第一次执行时会各记一条 “Mixin hook active” 日志，便于排查静默失效。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	// 四个注入点都在高频路径（每次挖掘 / 每 tick 挖掘 / 每次攻击），
	// 统一用 AtomicBoolean 探针：首次之后只剩一次 volatile 读。
	@Unique
	private static final AtomicBoolean TWEAKIFY$DESTROY_BLOCK_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$SAME_TARGET_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$ATTACK_BLOCK_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$ATTACK_MARK_PROBE = new AtomicBoolean();

	@Shadow
	private BlockPos destroyBlockPos;

	// 仅在方块真正被挖下（destroyBlock 返回 true）时标记：领地保护、基岩、
	// 权限不足等失败分支返回 false，不能算作本次左键已经挖到方块。
	@ModifyReturnValue(method = "destroyBlock", at = @At("RETURN"), require = 0)
	private boolean tweakify$markBlockMined(boolean original, BlockPos pos) {
		HookProbe.markOnce(TWEAKIFY$DESTROY_BLOCK_PROBE, "miningCooldownDestroyBlock");
		if (original) {
			MiningCooldownState.markBlockMined();
		}
		return original;
	}

	// 切换物品不打断挖掘：原版 sameDestroyTarget 同时比较位置与手持物品，
	// 切换物品时返回 false，continueDestroyBlock 会重新 startDestroyBlock 并清空进度。
	// 开关开启时只比较位置，保留当前破坏进度。
	@ModifyReturnValue(method = "sameDestroyTarget", at = @At("RETURN"), require = 0)
	private boolean tweakify$keepMiningProgress(boolean original, BlockPos pos) {
		HookProbe.markOnce(TWEAKIFY$SAME_TARGET_PROBE, "keepMiningProgressOnItemSwitch");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.keepMiningProgressOnItemSwitch) {
			return pos.equals(this.destroyBlockPos);
		}
		return original;
	}

	// 攻击实体也受“一次左键一次”限制：与挖方块共用同一个按住周期。
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$blockAttackAfterFirst(Player player, Entity target, CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$ATTACK_BLOCK_PROBE, "miningCooldownAttack");
		// 两个开关的判定都在状态类内：这里不再重复查配置。
		if (MiningCooldownState.shouldBlockAttack()) {
			ci.cancel();
		}
	}

	@Inject(method = "attack", at = @At("TAIL"), require = 0)
	private void tweakify$markEntityAttacked(Player player, Entity target, CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$ATTACK_MARK_PROBE, "miningCooldownAttackMark");
		MiningCooldownState.markEntityAttacked();
	}
}
