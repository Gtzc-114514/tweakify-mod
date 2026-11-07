package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectedBlockOutlineStateTest {
	private static final double EPSILON = 1.0e-6;
	private static final BlockPos POS = new BlockPos(0, 64, 0);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void doorLowerHalfConnectsUpwards() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		assertTrue(ConnectedBlockOutlineState.isSupported(lower));
		assertEquals(List.of(POS.above()), ConnectedBlockOutlineState.connectedPositions(lower, POS));
	}

	@Test
	void doorUpperHalfConnectsDownwards() {
		BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
		assertEquals(List.of(POS.below()), ConnectedBlockOutlineState.connectedPositions(upper, POS));
	}

	@Test
	void bedConnectsToTheOtherPart() {
		BlockState bed = Blocks.BED.pick(DyeColor.RED).defaultBlockState()
			.setValue(AbstractBedBlock.PART, BedPart.FOOT)
			.setValue(AbstractBedBlock.FACING, Direction.NORTH);
		assertTrue(ConnectedBlockOutlineState.isSupported(bed));
		assertEquals(List.of(POS.north()), ConnectedBlockOutlineState.connectedPositions(bed, POS));
	}

	@Test
	void singleChestHasNoPartner() {
		BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.SINGLE);
		assertTrue(ConnectedBlockOutlineState.connectedPositions(chest, POS).isEmpty());
	}

	@Test
	void doubleChestConnectsToTheOtherHalf() {
		BlockState chest = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.TYPE, ChestType.LEFT)
			.setValue(ChestBlock.FACING, Direction.NORTH);
		assertEquals(List.of(POS.east()), ConnectedBlockOutlineState.connectedPositions(chest, POS));
	}

	@Test
	void tallPlantConnectsToTheOtherHalf() {
		BlockState lower = Blocks.TALL_GRASS.defaultBlockState()
			.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
		assertTrue(ConnectedBlockOutlineState.isSupported(lower));
		assertEquals(List.of(POS.above()), ConnectedBlockOutlineState.connectedPositions(lower, POS));
	}

	@Test
	void unsupportedBlockHasNoPartner() {
		BlockState stone = Blocks.STONE.defaultBlockState();
		assertFalse(ConnectedBlockOutlineState.isSupported(stone));
		assertTrue(ConnectedBlockOutlineState.connectedPositions(stone, POS).isEmpty());
		assertNull(ConnectedBlockOutlineState.mergedShape(stone, POS, levelOf(Map.of(POS, stone))));
	}

	@Test
	void mergedDoorOutlineUsesTwoBlockVisualHeight() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
		BlockGetter level = levelOf(Map.of(POS, lower, POS.above(), upper));

		VoxelShape lowerMerged = ConnectedBlockOutlineState.mergedShape(lower, POS, level);
		assertNotNull(lowerMerged, "对准门下半时应合并上半");
		assertEquals(0.0, lowerMerged.bounds().minY, EPSILON);
		assertEquals(2.0, lowerMerged.bounds().maxY, EPSILON,
			"门下半的轮廓应覆盖两格视觉高度");

		VoxelShape upperMerged = ConnectedBlockOutlineState.mergedShape(upper, POS.above(), level);
		assertNotNull(upperMerged, "对准门上半时应合并下半");
		assertEquals(-1.0, upperMerged.bounds().minY, EPSILON,
			"门上半的轮廓应向下覆盖一格");
		assertEquals(1.0, upperMerged.bounds().maxY, EPSILON);
	}

	@Test
	void mergedDoubleChestOutlineWidensByOneBlock() {
		BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT);
		BlockPos partner = ChestBlock.getConnectedBlockPos(POS, left);
		BlockState right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);
		BlockGetter level = levelOf(Map.of(POS, left, partner, right));

		VoxelShape single = left.getShape(level, POS);
		VoxelShape merged = ConnectedBlockOutlineState.mergedShape(left, POS, level);

		assertNotNull(merged, "双箱应合并另一格");
		assertTrue(merged.bounds().getXsize() > single.bounds().getXsize()
				|| merged.bounds().getZsize() > single.bounds().getZsize(),
			"合并后的轮廓应在连接方向上变宽");
	}

	@Test
	void chestWithSingleNeighbourDoesNotMerge() {
		BlockState left = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.TYPE, ChestType.LEFT)
			.setValue(ChestBlock.FACING, Direction.NORTH);
		BlockState single = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.TYPE, ChestType.SINGLE)
			.setValue(ChestBlock.FACING, Direction.NORTH);
		BlockGetter level = levelOf(Map.of(POS, left, POS.east(), single));
		assertNull(ConnectedBlockOutlineState.mergedShape(left, POS, level),
			"相邻的独立单箱不能被当作双箱另一半");
	}

	@Test
	void chestWithDifferentFacingDoesNotMerge() {
		BlockState left = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.TYPE, ChestType.LEFT)
			.setValue(ChestBlock.FACING, Direction.NORTH);
		BlockState right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT)
			.setValue(ChestBlock.FACING, Direction.SOUTH);
		BlockGetter level = levelOf(Map.of(POS, left, POS.east(), right));
		assertNull(ConnectedBlockOutlineState.mergedShape(left, POS, level),
			"朝向不同的两个箱子不是同一个双箱");
	}

	@Test
	void bedWithSamePartDoesNotMerge() {
		BlockState head = Blocks.BED.pick(DyeColor.RED).defaultBlockState()
			.setValue(AbstractBedBlock.PART, BedPart.HEAD)
			.setValue(AbstractBedBlock.FACING, Direction.NORTH);
		assertEquals(List.of(POS.south()), ConnectedBlockOutlineState.connectedPositions(head, POS));
		BlockGetter level = levelOf(Map.of(POS, head, POS.south(), head));
		assertNull(ConnectedBlockOutlineState.mergedShape(head, POS, level),
			"两个同向床头不能互相合并");
	}

	@Test
	void doorWithSameHalfDoesNotMerge() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
			.setValue(DoorBlock.FACING, Direction.NORTH);
		BlockGetter level = levelOf(Map.of(POS, lower, POS.above(), lower));
		assertNull(ConnectedBlockOutlineState.mergedShape(lower, POS, level),
			"上下都是下半门时不能合并");
	}

	@Test
	void missingPartnerKeepsVanillaOutline() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		BlockGetter level = levelOf(Map.of(POS, lower));
		assertNull(ConnectedBlockOutlineState.mergedShape(lower, POS, level));
	}

	// 只提供方块状态的轻量世界：门/床/箱子的形状都由状态决定，不需要完整 ClientLevel。
	private static BlockGetter levelOf(Map<BlockPos, BlockState> states) {
		return new BlockGetter() {
			@Override
			public BlockEntity getBlockEntity(BlockPos pos) {
				return null;
			}

			@Override
			public BlockState getBlockState(BlockPos pos) {
				return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
			}

			@Override
			public FluidState getFluidState(BlockPos pos) {
				return Blocks.AIR.defaultBlockState().getFluidState();
			}

			@Override
			public int getHeight() {
				return 384;
			}

			@Override
			public int getMinY() {
				return -64;
			}
		};
	}
}
