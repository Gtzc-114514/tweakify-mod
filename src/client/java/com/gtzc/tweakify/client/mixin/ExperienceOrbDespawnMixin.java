package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	// 两处都用 require = 0：其它模组整体重写 tick 时功能失效即可，不能让注入失败拖垮启动。
	// 与 ItemEntityMixin 的处理保持一致（版本更新或模组冲突时优先降级而非崩溃）。
	@Inject(method = "tick", at = @At("TAIL"), require = 0)
	private void tweakify$expireWhenDue(CallbackInfo ci) {
		if (this.age >= tweakify$configuredLifetime()) {
			((ExperienceOrb) (Object) this).discard();
		}
	}

	@Redirect(method = "tick", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
	private void tweakify$guardVanillaDespawn(ExperienceOrb self) {
		if (this.age < tweakify$configuredLifetime()) {
			return;
		}
		self.discard();
	}

	private static int tweakify$configuredLifetime() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config == null ? VANILLA_LIFETIME : config.itemDespawnTimeXpOrbs;
	}
}
