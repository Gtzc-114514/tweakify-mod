package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.CreativeFlightNoclip;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造飞行穿墙：在 {@code aiStep} 开头把 {@code noPhysics} 设为 true。
 *
 * <p>原版 {@code Player.tick} 会先按 {@code isSpectator()} 重置 noPhysics，随后才进入
 * {@code super.tick()} 的移动逻辑；在 aiStep 开头设置可以保证本次移动就跳过碰撞，同时
 * 不与其它模组对 {@code isSpectator()} 的重定向抢同一个注入点。
 */
@Mixin(Player.class)
public abstract class PlayerCreativeFlightNoclipMixin {
	@Inject(method = "aiStep", at = @At("HEAD"))
	private void tweakify$enableNoPhysics(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (CreativeFlightNoclip.isActive(player)) {
			player.noPhysics = true;
		}
	}
}
