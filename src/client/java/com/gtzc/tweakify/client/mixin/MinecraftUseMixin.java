package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ElytraFireworkState;
import com.gtzc.tweakify.client.OffhandFallbackState;
import com.gtzc.tweakify.client.UseCooldownState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 统一拦截 startUseItem：先做右键使用冷却判定，再做副手回退拦截判定。
 * 两处逻辑都只取消本次使用，合并到同一个注入点可以避免两个 mixin 抢同一处，
 * 并让判定顺序固定下来。
 *
 * <p>注入点用 require = 0：startUseItem 极易被战斗/使用类模组 @Overwrite，
 * 冲突时功能失效即可，不拖垮启动。钩子是否真正注入由行为探针确认：
 * 第一次右键时日志里应出现 "Mixin hook active: startUseItem"。
 */
@Mixin(Minecraft.class)
public class MinecraftUseMixin {
	// startUseItem 每次右键都会走到，用 AtomicBoolean 探针避免热路径上的集合哈希。
	@Unique
	private static final AtomicBoolean TWEAKIFY$START_USE_PROBE = new AtomicBoolean();

	@Shadow
	public LocalPlayer player;

	@Shadow
	public HitResult hitResult;

	@Shadow
	public ClientLevel level;

	@Shadow
	public Options options;

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$beforeStartUseItem(CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$START_USE_PROBE, "startUseItem");
		if (tweakify$blockUseCooldown() || tweakify$blockOffhandFallback() || tweakify$blockElytraFirework()) {
			ci.cancel();
			return;
		}
		tweakify$markElytraFireworkUsed();
	}

	// 滑翔中使用烟花后，等烟花加速结束才能再次使用。
	private boolean tweakify$blockElytraFirework() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.elytraFireworkCooldown || this.player == null) {
			return false;
		}
		return this.player.isFallFlying()
			&& ElytraFireworkState.shouldBlockUse()
			&& tweakify$isAimingAtAir()
			&& tweakify$hasFireworkInHands();
	}

	// 允许使用烟花时记录本次使用；只在真正会触发烟花加速的场景下置位。
	private void tweakify$markElytraFireworkUsed() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.elytraFireworkCooldown || this.player == null) {
			return;
		}
		if (this.player.isFallFlying() && tweakify$isAimingAtAir() && tweakify$hasFireworkInHands()) {
			ElytraFireworkState.markUsed();
		}
	}

	private boolean tweakify$isAimingAtAir() {
		return this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS;
	}

	private boolean tweakify$hasFireworkInHands() {
		return this.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.FIREWORK_ROCKET)
			|| this.player.getItemInHand(InteractionHand.OFF_HAND).is(Items.FIREWORK_ROCKET);
	}

	// 一次右键只使用一次：按主/副手分会话，命中冷却物品列表的手在用过一次后被拦截。
	private boolean tweakify$blockUseCooldown() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.useCooldown || this.player == null) {
			return false;
		}
		if (!this.options.keyUse.isDown()) {
			return false;
		}

		ItemStack mainStack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack offStack = this.player.getItemInHand(InteractionHand.OFF_HAND);

		// 主手优先；主手为空则按副手判定（近似原版 hand 选择优先级）。
		if (!mainStack.isEmpty()) {
			return UseCooldownState.shouldBlockHand(config.functions.mainHandUseCooldown,
				UseCooldownState.mainUsedThisSession(), config.isMainHandUseRestricted(mainStack.getItem()));
		}
		if (!offStack.isEmpty()) {
			return UseCooldownState.shouldBlockHand(config.functions.offhandUseCooldown,
				UseCooldownState.offhandUsedThisSession(), config.isOffhandUseRestricted(offStack.getItem()));
		}
		return false;
	}

	// 主手用完瞬间阻止副手接替使用（受命中目标与两个交互开关影响）。
	private boolean tweakify$blockOffhandFallback() {
		// 开关与 tick 判定都在状态类内：这里不再直接读可变标志。
		if (!OffhandFallbackState.shouldBlockOffhandFallback()) {
			return false;
		}

		TweakifyModConfig config = TweakifyMod.CONFIG;
		HitResult.Type type = this.hitResult != null ? this.hitResult.getType() : null;
		OffhandFallbackState.HitTarget target;
		boolean offhandIsBlockItem = false;
		boolean hitBlockInteractive = false;
		if (type == HitResult.Type.ENTITY) {
			target = OffhandFallbackState.HitTarget.ENTITY;
		} else if (type == HitResult.Type.BLOCK) {
			target = OffhandFallbackState.HitTarget.BLOCK;
			offhandIsBlockItem = this.player.getOffhandItem().getItem() instanceof BlockItem;
			hitBlockInteractive = this.level != null
				&& OffhandFallbackState.isInteractiveBlock(
					this.level.getBlockState(((BlockHitResult) this.hitResult).getBlockPos()));
		} else {
			target = OffhandFallbackState.HitTarget.MISS;
		}

		return OffhandFallbackState.shouldCancelHit(target,
			config.functions.interceptBlockInteraction,
			config.functions.interceptEntityInteraction,
			offhandIsBlockItem, hitBlockInteractive);
	}
}
