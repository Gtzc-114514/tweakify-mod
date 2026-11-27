package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.RespawnHotbarState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-143474：重生后快捷栏选中项重置到第 0 槽。
 *
 * <p>26.3 在 handleRespawn 中创建新的 LocalPlayer，旧玩家的快捷栏选中槽
 * 不会复制过去。这里在 startWaitingForNewLevel 之前把旧槽位写回新玩家，
 * 后续服务端下发的物品仍按原版路径填充。配置关闭时不修改新玩家。
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerRespawnHotbarMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$RESPAWN_HOTBAR_PROBE = new AtomicBoolean();

	@Inject(method = "handleRespawn", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;startWaitingForNewLevel("
				+ "Lnet/minecraft/client/player/LocalPlayer;"
				+ "Lnet/minecraft/client/multiplayer/ClientLevel;"
				+ "Lnet/minecraft/client/gui/screens/LevelLoadingScreen$Reason;)V"))
	private void tweakify$restoreHotbarSlot(ClientboundRespawnPacket packet, CallbackInfo ci,
			@Local(ordinal = 0) LocalPlayer oldPlayer,
			@Local(ordinal = 1) LocalPlayer newPlayer) {
		TweakifyModConfig config = TweakifyMod.config();
		if (oldPlayer == null || newPlayer == null) {
			return;
		}
		// 死亡重生与维度切换都会走 handleRespawn：先用旧玩家死亡状态区分，再比较维度。
		boolean death = oldPlayer.isDeadOrDying();
		boolean dimensionChange = !death && !oldPlayer.level().dimension()
			.equals(packet.commonPlayerSpawnInfo().dimension());
		boolean enabled = RespawnHotbarState.shouldRestore(
			config.fix.respawnHotbarFix, config.fix.respawnHotbarKeepOnDimensionChange, dimensionChange);
		int slot = RespawnHotbarState.slotToRestore(enabled,
			oldPlayer.getInventory().getSelectedSlot());
		if (slot < 0) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$RESPAWN_HOTBAR_PROBE, "respawnHotbarFix");
		newPlayer.getInventory().setSelectedSlot(slot);
	}
}
