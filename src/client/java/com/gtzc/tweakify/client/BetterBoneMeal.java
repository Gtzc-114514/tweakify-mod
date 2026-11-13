package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 更好的骨粉催熟：补齐原版（Java 版）不能用骨粉的两类方块，行为对齐基岩版。
 *
 * <p>甘蔗：一次骨粉把整株催到最高 3 格（只消耗 1 个骨粉）。
 * 花：对一格高的花使用骨粉，在周围长出一片相同的花（凋零玫瑰除外）。
 *
 * <p>通过 {@code BoneMealItem.growCrop} 的 {@code @WrapMethod} 接入，返回 {@code null} 表示
 * 不是本功能处理的方块，交回原版逻辑。世界改动只在服务端执行，客户端只做预测。
 */
public final class BetterBoneMeal {
	// 花扩散的最大数量与随机取点范围（7x7）。
	private static final int FLOWER_SPREAD_COUNT = 5;
	private static final int FLOWER_SPREAD_RADIUS = 3;

	// 骨粉成功的粒子数量与扩散范围（与原版 addGrowthParticles 的量级一致）。
	private static final int GROWTH_PARTICLES = 45;
	private static final double PARTICLE_RADIUS = 1.5;
	private static final double PARTICLE_Y_SPREAD = 0.5;

	private BetterBoneMeal() {
	}

	// 返回 null = 不归本功能处理；true/false = 已处理并给出最终结果。
	public static Boolean tryGrow(ItemStack stack, Level level, BlockPos pos) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.betterBoneMeal) {
			return null;
		}
		// 远程服务器没有本模组的服务端注入：客户端不要虚假预测成功，交还原版行为。
		if (level.isClientSide() && !Minecraft.getInstance().hasSingleplayerServer()) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (config.boneMealSugarCane && state.is(Blocks.SUGAR_CANE)) {
			return growSugarCane(stack, level, pos);
		}
		if (config.boneMealFlowers && state.getBlock() instanceof FlowerBlock) {
			return spreadFlowers(stack, level, pos, state);
		}
		return null;
	}

	// 甘蔗：从被点击的那一节向上/向下找到整株，一次补齐到 3 格。
	private static boolean growSugarCane(ItemStack stack, Level level, BlockPos clickedPos) {
		BlockPos base = clickedPos;
		while (level.getBlockState(base.below()).is(Blocks.SUGAR_CANE)) {
			base = base.below();
		}
		BlockPos top = base;
		int height = 1;
		while (level.getBlockState(top.above()).is(Blocks.SUGAR_CANE)) {
			top = top.above();
			height++;
		}
		if (height >= 3) {
			return false;
		}

		// 上方空间可能只够长 1 格：能长多少长多少，一个骨粉只消耗一次。
		int available = 0;
		BlockPos growPos = top.above();
		while (height + available < 3 && !level.isOutsideBuildHeight(growPos)
			&& level.isEmptyBlock(growPos)) {
			available++;
			growPos = growPos.above();
		}
		if (available == 0) {
			return false;
		}

		if (level instanceof ServerLevel serverLevel) {
			BlockPos cursor = top.above();
			BlockState caneState = Blocks.SUGAR_CANE.defaultBlockState();
			int placed = 0;
			for (int i = 0; i < available; i++) {
				if (serverLevel.setBlockAndUpdate(cursor, caneState)) {
					placed++;
				}
				cursor = cursor.above();
			}
			// 所有位置都写入失败时不能白扣骨粉。
			if (placed == 0) {
				return false;
			}
			stack.shrink(1);
			spawnGrowthParticles(serverLevel, clickedPos);
		}
		return true;
	}

	// 花：像基岩版一样在周围扩散同类花，最多 5 朵；没有可生长的位置时不消耗骨粉。
	private static boolean spreadFlowers(ItemStack stack, Level level, BlockPos origin, BlockState flowerState) {
		// 基岩版的骨粉对凋零玫瑰无效。
		if (flowerState.is(Blocks.WITHER_ROSE)) {
			return false;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			// 客户端只做预测：有任何可扩散位置就返回成功，让手臂正常挥动。
			return hasFlowerSpot(level, origin, flowerState);
		}

		RandomSource random = serverLevel.getRandom();
		// 先收集全部合法位置，再随机选最多 5 个：只要客户端预测有一个位置，服务端就一定能种下至少一朵。
		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -FLOWER_SPREAD_RADIUS; dx <= FLOWER_SPREAD_RADIUS; dx++) {
			for (int dz = -FLOWER_SPREAD_RADIUS; dz <= FLOWER_SPREAD_RADIUS; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos pos = origin.offset(dx, 0, dz);
				if (serverLevel.isEmptyBlock(pos) && flowerState.canSurvive(serverLevel, pos)) {
					candidates.add(pos);
				}
			}
		}
		if (candidates.isEmpty()) {
			return false;
		}

		int placeCount = Math.min(FLOWER_SPREAD_COUNT, candidates.size());
		for (int i = 0; i < placeCount; i++) {
			BlockPos pos = candidates.remove(random.nextInt(candidates.size()));
			serverLevel.setBlockAndUpdate(pos, flowerState);
		}
		stack.shrink(1);
		spawnGrowthParticles(serverLevel, origin);
		return true;
	}

	// 客户端预测用的确定性检查：7x7 范围内是否存在可放置同类花的位置。
	private static boolean hasFlowerSpot(Level level, BlockPos origin, BlockState flowerState) {
		for (int dx = -FLOWER_SPREAD_RADIUS; dx <= FLOWER_SPREAD_RADIUS; dx++) {
			for (int dz = -FLOWER_SPREAD_RADIUS; dz <= FLOWER_SPREAD_RADIUS; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos pos = origin.offset(dx, 0, dz);
				if (level.isEmptyBlock(pos) && flowerState.canSurvive(level, pos)) {
					return true;
				}
			}
		}
		return false;
	}

	// 甘蔗/花都不是 BonemealableBlock，原版 1505 事件不会产生粒子，这里由服务端补发一份。
	private static void spawnGrowthParticles(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
			pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			GROWTH_PARTICLES, PARTICLE_RADIUS, PARTICLE_Y_SPREAD, PARTICLE_RADIUS, 0.0);
	}
}
