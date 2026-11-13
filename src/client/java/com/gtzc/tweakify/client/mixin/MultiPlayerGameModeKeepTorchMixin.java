package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.KeepLastTorchFeature;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 不使用最后一根火把（客户端预测）：与服务端同名逻辑对称，在本地预测的
 * ItemStack.useOn 处同样跳过最后一根火把，避免"客户端预测已扣物品、服务端不放置"的回滚不一致。
 *
 * <p>只重定向 ItemStack.useOn：方块自身的交互先于它执行并直接返回，
 * 因此门/箱子/拉杆等交互不受影响。方块交互成功后不会走到 useOn，重定向也不会触发。
 * 远程服务器上本模组无法拦截服务端放置（服务端仍会放置），但客户端预测保持不消耗，最终以服务端同步为准。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeKeepTorchMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$KEEP_TORCH_CLIENT_PROBE = new AtomicBoolean();

	@Redirect(method = "performUseItemOn", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult tweakify$skipLastTorchPlacement(ItemStack stack, UseOnContext context) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		Player player = context.getPlayer();
		if (config != null && config.keepLastTorch && player != null && !player.isCreative()) {
			HookProbe.markOnce(TWEAKIFY$KEEP_TORCH_CLIENT_PROBE, "keepLastTorchClient");
			if (KeepLastTorchFeature.shouldBlockPlacement(
				stack.getItem(), KeepLastTorchFeature.countTorches(player, stack.getItem()))) {
				return InteractionResult.PASS;
			}
		}
		return stack.useOn(context);
	}
}
