package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnEggThrower;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 刷怪蛋投掷：把原版“在方块上直接生成”改成先投掷鸡蛋，命中后再生成。
 * useOn 覆盖瞄准方块的情况，use 覆盖瞄准空气/液体的情况。
 */
@Mixin(SpawnEggItem.class)
public class SpawnEggItemThrowMixin {
	// 用 @WrapMethod 自行决定是否调用原方法，避免其它模组包装 useOn 时我们的短路被绕过。
	@WrapMethod(method = "useOn")
	private InteractionResult tweakify$throwSpawnEggAtBlock(UseOnContext context,
			Operation<InteractionResult> original) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.throwSpawnEgg) {
			return original.call(context);
		}
		Player player = context.getPlayer();
		if (player == null) {
			return original.call(context);
		}
		// 刷怪蛋点刷怪笼/试炼刷怪笼是原版“设置刷怪类型”的分支，不能改成投掷。
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Spawner) {
			InteractionResult spawnerResult = tweakify$configureSpawner(context, player);
			return spawnerResult != null ? spawnerResult : original.call(context);
		}
		InteractionResult result = SpawnEggThrower.throwEgg(
			context.getLevel(), player, context.getItemInHand(), context.getHand());
		return result != InteractionResult.PASS ? result : original.call(context);
	}

	@WrapMethod(method = "use")
	private InteractionResult tweakify$throwSpawnEggInAir(Level level, Player player, InteractionHand hand,
			Operation<InteractionResult> original) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.throwSpawnEgg) {
			return original.call(level, player, hand);
		}
		ItemStack stack = player.getItemInHand(hand);
		InteractionResult result = SpawnEggThrower.throwEgg(level, player, stack, hand);
		return result != InteractionResult.PASS ? result : original.call(level, player, hand);
	}

	// 只设置刷怪笼类型，不消耗刷怪蛋：这是本功能对刷怪笼分支的明确约定。
	private static InteractionResult tweakify$configureSpawner(UseOnContext context, Player player) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}
		// 刷怪笼方块被服务端禁用时交回原版，由原版发出提示并返回 FAIL。
		if (!serverLevel.isSpawnerBlockEnabled()) {
			return null;
		}

		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof Spawner spawner)) {
			return null;
		}

		ItemStack stack = context.getItemInHand();
		EntityType<?> type = SpawnEggItem.getType(stack);
		if (type == null || !type.canSpawn(level)) {
			return null;
		}

		RandomSource random = level.getRandom();
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
		if (data != null && serverLevel.getServer() != null
			&& serverLevel.getServer().getPlayerList().isOp(player.nameAndId())) {
			spawner.setEntityData(data, random);
		} else {
			spawner.setEntityId(type, random);
		}

		BlockState state = level.getBlockState(pos);
		level.sendBlockUpdated(pos, state, state, 3);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}
}
