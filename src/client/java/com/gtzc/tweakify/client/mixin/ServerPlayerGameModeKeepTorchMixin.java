package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.KeepLastTorchFeature;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 不使用最后一根火把：服务端执行方块放置时，如果该火把只剩最后一根，
 * 只把 ItemStack.useOn 替换为 PASS（不放置、不消耗），不影响门/箱子/拉杆等方块交互。
 *
 * <p>注入点选在 useItemOn 内部的 ItemStack.useOn：原版先执行方块自身的交互，
 * 消费后直接 return，根本不会走到这里，因此拦截不会误伤方块交互。
 * 交互判定在服务端，仅单人/局域网（模组同时作用于集成服务端）生效；
 * 远程服务器无法拦截。注入点用 require = 0，冲突时功能失效即可。
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeKeepTorchMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$KEEP_TORCH_PROBE = new AtomicBoolean();

	@WrapOperation(method = "useItemOn", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult tweakify$skipLastTorchPlacement(ItemStack stack, UseOnContext context,
		Operation<InteractionResult> original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		Player player = context.getPlayer();
		if (config != null && config.modified.keepLastTorch && player != null && !player.isCreative()) {
			HookProbe.markOnce(TWEAKIFY$KEEP_TORCH_PROBE, "keepLastTorch");
			if (KeepLastTorchFeature.shouldBlockPlacement(
				stack.getItem(), KeepLastTorchFeature.countTorches(player, stack.getItem()))) {
				return InteractionResult.PASS;
			}
		}
		return original.call(stack, context);
	}
}
