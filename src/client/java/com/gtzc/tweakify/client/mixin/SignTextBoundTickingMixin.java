package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 告示牌文本优化：跳过客户端告示牌的 block entity tick。
 *
 * <p>与服务端无关，仅影响本地已加载的 {@link ClientLevel}。
 */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public class SignTextBoundTickingMixin<T extends BlockEntity> {
	@Shadow
	@Final
	private T blockEntity;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void tweakify$skipClientSignTick(CallbackInfo ci) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.optimization.signTextOptimization) {
			return;
		}
		// 只跳过原版告示牌，避免误伤第三方模组给子类挂载的客户端 ticker。
		if (blockEntity.getClass() == SignBlockEntity.class
			&& blockEntity.getLevel() instanceof ClientLevel) {
			ci.cancel();
		}
	}
}
