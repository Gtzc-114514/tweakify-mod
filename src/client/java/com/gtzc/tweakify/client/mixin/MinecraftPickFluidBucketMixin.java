package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 中键取物（客户端部分）：
 * 原版准星取目标忽略流体，指向水面时根本没有方块命中，也不会发取物包；
 * 这里单独做一次包含流体的射线检测，命中液体就替玩家发出取物请求。
 *
 * <p>创造模式归「中键获得液体桶」：直接得到桶；
 * 生存模式归「生存模式中键取物」：由服务端切换到背包里已有的桶；
 * 准星对着方块/实体时保持原版逻辑（背包里有对应物品就切到主手）。
 * 物品由服务端换算（见 ServerGamePacketListenerImplPickMixin），因此只在单人 / 局域网生效。
 */
@Mixin(Minecraft.class)
public class MinecraftPickFluidBucketMixin {
	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$pickFluidBucket(CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		Minecraft client = (Minecraft) (Object) this;
		if (config == null || client.player == null || client.level == null || client.gameMode == null) {
			return;
		}
		// 创造模式归“中键获得液体桶”；生存模式归“生存模式中键取物”。
		boolean creative = client.player.hasInfiniteMaterials();
		if (creative ? !config.functions.pickFluidBucket : !config.functions.survivalPickBlock) {
			return;
		}
		// 准星正对着实体时交给原版，避免抢走实体取物。
		if (client.hitResult instanceof EntityHitResult) {
			return;
		}
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			return;
		}
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		// includeFluids = true：射线可以停在液体上，取到的就是液体方块。
		HitResult fluidHit = camera.pick(client.player.blockInteractionRange(), partialTick, true);
		if (!(fluidHit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		if (!(client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof LiquidBlock)) {
			return;
		}
		HookProbe.markOnce("pickFluidBucket");
		client.gameMode.handlePickItemFromBlock(blockHit.getBlockPos(), client.hasControlDown());
		ci.cancel();
	}
}
