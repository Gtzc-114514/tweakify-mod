package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 经验球消失时间：与原版 ItemEntity 的处理方式一致。
 *
 * <p>tick 末尾原版固定按 6000 tick 判定消失，这里两处配合：
 * 拦截该 discard 调用（配置更长时阻止提前消失），并在 tick 末尾按配置值主动丢弃（配置更短时立即生效）。
 */
@Mixin(ExperienceOrb.class)
public class ExperienceOrbDespawnMixin {
	private static final int VANILLA_LIFETIME = 6000;

	@Shadow
	private int age;

	// tick 每个经验球每 tick 执行一次：两处注入点都用 AtomicBoolean 版本，避免热路径上的集合哈希。
	@Unique
	private static final AtomicBoolean TWEAKIFY$TICK_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$GUARD_PROBE = new AtomicBoolean();

	// 两处都用 require = 0：其它模组整体重写 tick 时功能失效即可，不能让注入失败拖垮启动。
	// 与 ItemEntityMixin 的处理保持一致（版本更新或模组冲突时优先降级而非崩溃）。
	@Inject(method = "tick", at = @At("TAIL"), require = 0)
	private void tweakify$expireWhenDue(CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$TICK_PROBE, "xpOrbDespawnTick");
		if (this.age >= tweakify$configuredLifetime()) {
			((ExperienceOrb) (Object) this).discard();
		}
	}

	@WrapOperation(method = "tick", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
	private void tweakify$guardVanillaDespawn(ExperienceOrb self, Operation<Void> original) {
		HookProbe.markOnce(TWEAKIFY$GUARD_PROBE, "xpOrbDespawnGuard");
		if (this.age < tweakify$configuredLifetime()) {
			return;
		}
		original.call(self);
	}

	private static int tweakify$configuredLifetime() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null ? VANILLA_LIFETIME : config.itemDespawnTimeXpOrbs;
	}
}
