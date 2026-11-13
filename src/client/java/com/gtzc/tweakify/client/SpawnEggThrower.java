package com.gtzc.tweakify.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 刷怪蛋投掷：复用鸡蛋实体承载刷怪蛋物品，命中后在落点生成对应生物。
 * 生成动作发生在服务端，因此和原版刷怪蛋一样只在单人/局域网主机生效。
 */
public final class SpawnEggThrower {
	private SpawnEggThrower() {
	}

	public static boolean isSupported(Level level) {
		return !level.isClientSide() || Minecraft.getInstance().getSingleplayerServer() != null;
	}

	public static InteractionResult throwEgg(Level level, Player player, ItemStack stack,
											 InteractionHand hand) {
		if (!isSupported(level) || stack.isEmpty()) {
			return InteractionResult.PASS;
		}

		EntityType<?> type = SpawnEggItem.getType(stack);
		if (type == null || !type.canSpawn(level)) {
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel serverLevel) {
			ThrownEgg egg = new ThrownEgg(serverLevel, player, stack.copyWithCount(1));
			egg.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
			egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			serverLevel.addFreshEntity(egg);

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EGG_THROW, SoundSource.NEUTRAL, 0.5F,
				0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
			player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
			stack.consume(1, player);
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * 鸡蛋实体命中后的服务端生成。命中方块时优先使用被命中面的外侧一格，
	 * 避免把生物放进墙里或地面里；命中实体或找不到空位时再向上寻找。
	 */
	public static void spawnMob(ThrownEgg egg, HitResult result) {
		if (!(egg.level() instanceof ServerLevel level)) {
			return;
		}

		ItemStack stack = egg.getItem();
		if (!(stack.getItem() instanceof SpawnEggItem)) {
			return;
		}

		EntityType<?> type = SpawnEggItem.getType(stack);
		if (type == null) {
			return;
		}

		BlockPos spawnPos = findSpawnPos(level, type, result);
		if (spawnPos == null) {
			return;
		}
		Entity owner = egg.getOwner();
		LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
		type.spawn(level, stack.copyWithCount(1), livingOwner, spawnPos,
			EntitySpawnReason.SPAWN_ITEM_USE, false, false);
	}

	private static BlockPos findSpawnPos(Level level, EntityType<?> type, HitResult result) {
		BlockPos origin = BlockPos.containing(result.getLocation());
		BlockPos adjacent = result instanceof BlockHitResult blockHit
			? blockHit.getBlockPos().relative(blockHit.getDirection())
			: null;
		return firstFreeSpawnPosition(spawnCandidates(origin, adjacent),
			pos -> isFree(level, type, pos));
	}

	// 纯候选序列：方块命中先看命中面外侧，再看上方三格，最后看命中点上方三格。
	static List<BlockPos> spawnCandidates(BlockPos origin, BlockPos adjacent) {
		List<BlockPos> candidates = new ArrayList<>(8);
		if (adjacent != null) {
			for (int i = 0; i <= 3; i++) {
				candidates.add(adjacent.above(i));
			}
		}
		for (int i = 0; i <= 3; i++) {
			candidates.add(origin.above(i));
		}
		return candidates;
	}

	// 可注入判定的纯选择函数：测试不需要构造 Level 或实体类型。
	static BlockPos firstFreeSpawnPosition(List<BlockPos> candidates, Predicate<BlockPos> isFree) {
		for (BlockPos candidate : candidates) {
			if (isFree.test(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	// 使用实体自身的生成包围盒检查，避免高生物被放进一格高的缝隙里。
	private static boolean isFree(Level level, EntityType<?> type, BlockPos pos) {
		return level.noCollision(type.getSpawnAABB(
			pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}
}
