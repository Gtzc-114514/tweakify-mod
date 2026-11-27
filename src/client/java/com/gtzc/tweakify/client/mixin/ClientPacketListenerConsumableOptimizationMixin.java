package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.ConsumableOptimization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 消耗品优化：吃掉服务端延迟回放的重复反馈。
 *
 * <p>两条注入分别对应「使用完成事件」与「进食音效」，都只在本地完成进食后的短窗口内生效，
 * 窗口外完全放行；实体状态同步保持原版，避免误伤重新进食的流程。
 *
 * <p>目标方法的第一条语句就是 {@code PacketUtils.ensureRunningOnSameThread}，HEAD 会先在
 * Netty I/O 线程执行一次，随后被重新排队到主线程再执行一次。这里用 {@code isSameThread()}
 * 守卫：异线程直接放行，既不在网络线程读客户端状态，也让一次性标记只被消费一次。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerConsumableOptimizationMixin {
	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void tweakify$dropUseCompleteEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) {
			return;
		}
		if (ConsumableOptimization.shouldDropUseCompletePacket(packet)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
	private void tweakify$dropDuplicateConsumeSound(ClientboundSoundPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) {
			return;
		}
		if (ConsumableOptimization.shouldSuppressServerSound(packet)) {
			ci.cancel();
		}
	}
}
