package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 辅助方块选区：只调整客户端选取形状，不改变服务端碰撞。
 *
 * <p>手持与目标相同的方块且未潜行时：栅栏与铁栏杆的选取形状扩成整格，
 * 末地烛、锁链、避雷针、滴水石锥、竹子等细长方块向外扩 2/16 格。
 * 潜行时恢复原版选区，便于放下方块后立即贴墙操作。
 */
public final class HelpfulBlockSelection {
	// 细长方块的选取形状向外扩大的距离（格）。
	static final double EXPAND = 2.0D / 16.0D;

	private HelpfulBlockSelection() {
	}

	// 混入入口：配置关闭、上下文无效或形状为空时原样返回。
	// 配置关闭或方块不在支持列表时不访问客户端玩家，避免让高频的 getShape 查询变慢。
	public static VoxelShape adjust(Block block, CollisionContext context, VoxelShape original) {
		if (block == null || context == null || original == null || original.isEmpty()) {
			return original;
		}
		if (!enabled() || !isHelpfulBlock(block)) {
			return original;
		}
		return adjust(true, block, original, isSneaking(), isHoldingMatchingItem(block, context));
	}

	// 纯逻辑入口：不读配置、不访问客户端玩家，便于单元测试覆盖全部门控条件。
	static VoxelShape adjust(boolean enabled, Block block, VoxelShape original,
			boolean sneaking, boolean holdingMatching) {
		if (!enabled || sneaking || !holdingMatching || original == null || original.isEmpty()
			|| !isHelpfulBlock(block)) {
			return original;
		}
		// 栅栏与铁栏杆的碰撞形状很窄，直接按整格选取。
		if (block instanceof FenceBlock || block instanceof IronBarsBlock) {
			return Shapes.block();
		}
		return expand(original);
	}

	// 配置缺失或关闭时保持原版选区。
	static boolean enabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.functions.helpfulBlockSelection;
	}

	// 需要放宽选区的细长/可连接方块；普通方块不处理。
	static boolean isHelpfulBlock(Block block) {
		return block instanceof FenceBlock
			|| block instanceof IronBarsBlock
			|| block instanceof EndRodBlock
			|| block instanceof BambooStalkBlock
			|| block instanceof LightningRodBlock
			|| block instanceof PointedDripstoneBlock
			|| block instanceof ChainBlock;
	}

	// 按原形状的外包围盒向外扩 2/16 格，并夹在单个方块内。
	static VoxelShape expand(VoxelShape shape) {
		if (shape == null || shape.isEmpty()) {
			return shape;
		}
		AABB bounds = shape.bounds();
		return Shapes.box(
			Math.max(0.0D, bounds.minX - EXPAND),
			Math.max(0.0D, bounds.minY - EXPAND),
			Math.max(0.0D, bounds.minZ - EXPAND),
			Math.min(1.0D, bounds.maxX + EXPAND),
			Math.min(1.0D, bounds.maxY + EXPAND),
			Math.min(1.0D, bounds.maxZ + EXPAND));
	}

	// 主手匹配由 CollisionContext 判断；副手单独补上，避免只认主手。
	private static boolean isHoldingMatchingItem(Block block, CollisionContext context) {
		if (context.isHoldingItem(block.asItem())) {
			return true;
		}
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.player != null
			&& minecraft.player.getOffhandItem().getItem() == block.asItem();
	}

	private static boolean isSneaking() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.player != null && minecraft.player.isShiftKeyDown();
	}
}
