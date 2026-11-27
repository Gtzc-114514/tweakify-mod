package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.render.SneakAnimationState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-159163：快速按潜行键时潜行动画会播放两次。
 *
 * <p>本地玩家的姿态由客户端输入控制；服务端回传的 POSE 数据如果晚到，
 * 会把本地姿态短暂改回去，形成第二次动画。这里在 assignValues 前移除
 * 本地玩家数据包中的 POSE 项，其它实体保持原版同步。
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerSneakAnimationMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$SNEAK_ANIMATION_PROBE = new AtomicBoolean();

	@Inject(method = "handleSetEntityData", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/syncher/SynchedEntityData;assignValues(Ljava/util/List;)V"))
	private void tweakify$stripLocalPlayerPose(ClientboundSetEntityDataPacket packet, CallbackInfo ci,
			@Local(ordinal = 0) Entity entity) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config.fix.sneakAnimationFix;
		Minecraft client = Minecraft.getInstance();
		boolean onlyOnGround = config.fix.sneakAnimationFixOnlyOnGround;
		boolean onlyWhenSneaking = config.fix.sneakAnimationFixOnlyWhenSneaking;
		boolean onGround = entity != null && entity.onGround();
		boolean sneaking = entity != null && entity.isShiftKeyDown();
		if (!SneakAnimationState.shouldStripPose(enabled, onlyOnGround, onlyWhenSneaking,
			entity != null && entity == client.player, onGround, sneaking)) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$SNEAK_ANIMATION_PROBE, "sneakAnimationFix");
		packet.packedItems().removeIf(value -> value.serializer().equals(EntityDataSerializers.POSE));
	}
}
