package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.client.ShovelSnowLayerState;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 锹单层铲雪：手持锹右键多层雪层时只铲掉一层，而不是整块移除。
 * 交互判定发生在服务端，因此仅在单人/局域网（模组同时作用于集成服务端）时生效。
 *
 * <p>原 HEAD 注入会整段接管 useItemOn，原版的前置守卫与后段副作用必须逐条补齐：
 * 特性开关守卫（Block.isEnabled）与 ITEM_USED_ON_BLOCK 进度触发都在这里复刻。
 *
 * <p>改用 @WrapMethod 后由我们决定是否调用原方法，其它模组包装 useItemOn 不会绕过短路。
 * 钩子是否注入成功看日志 "Mixin hook active: shovelOneSnowLayer"。
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	// useItemOn 每次右键方块都会走到，用 AtomicBoolean 探针避免热路径上的集合哈希。
	@Unique
	private static final AtomicBoolean TWEAKIFY$SHOVEL_SNOW_PROBE = new AtomicBoolean();

	@Shadow
	protected ServerPlayer player;

	@Shadow
	protected ServerLevel level;

	@WrapMethod(method = "useItemOn", require = 0)
	private InteractionResult tweakify$shovelOneSnowLayer(ServerPlayer serverPlayer, Level world, ItemStack stack,
			InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
		HookProbe.markOnce(TWEAKIFY$SHOVEL_SNOW_PROBE, "shovelOneSnowLayer");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || hand != InteractionHand.MAIN_HAND || serverPlayer.isSpectator()) {
			return original.call(serverPlayer, world, stack, hand, hitResult);
		}

		BlockPos pos = hitResult.getBlockPos();
		BlockState state = this.level.getBlockState(pos);
		// 包装器在调用原方法前，原版方法内的保护检查还没执行，这里逐条补齐：
		// 1) 特性开关关闭的方块，原版直接返回 FAIL，不能绕过；
		// 2) 领地/出生点保护与物品冷却，避免绕过服务端权限直接改方块。
		if (!state.getBlock().isEnabled(this.level.enabledFeatures())) {
			return original.call(serverPlayer, world, stack, hand, hitResult);
		}
		if (!this.level.mayInteract(this.player, pos)
			|| this.player.getCooldowns().isOnCooldown(stack)) {
			return original.call(serverPlayer, world, stack, hand, hitResult);
		}
		boolean snowLayer = state.getBlock() instanceof SnowLayerBlock;
		int layers = snowLayer ? state.getValue(SnowLayerBlock.LAYERS) : 0;
		if (!ShovelSnowLayerState.shouldReduceOneLayer(
			config.disableShovelSnowFullBreak, stack.is(ItemTags.SHOVELS), snowLayer, layers)) {
			return original.call(serverPlayer, world, stack, hand, hitResult);
		}

		// 进度触发器要用操作前的物品快照：reduceOneLayer 内部会消耗耐久。
		ItemStack usedStack = stack.copy();
		ShovelSnowLayerState.reduceOneLayer(this.level, this.player, pos, state,
			config.shovelSnowDropSnowball, config.shovelSnowConsumeDurability);
		// 原版后段的 ITEM_USED_ON_BLOCK 不会执行，这里按原版语义补回。
		CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(this.player, pos, usedStack);
		return InteractionResult.SUCCESS;
	}
}
