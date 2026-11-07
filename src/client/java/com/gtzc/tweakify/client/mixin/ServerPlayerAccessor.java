package com.gtzc.tweakify.client.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 ServerPlayer 的私有距离判定，供“禁用床太远”在跳过检查时仍能调用原逻辑。
 */
@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
	@Invoker("bedInRange")
	boolean tweakify$bedInRange(BlockPos pos, Direction direction);
}
