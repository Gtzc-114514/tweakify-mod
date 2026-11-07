package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * 锹单层铲雪：手持锹右键多层雪层时只铲掉一层，并把剩余雪层留在原位置。
 * 交互判定发生在服务端，因此仅在单人/局域网（模组同时作用于集成服务端）时生效。
 */
public final class ShovelSnowLayerState {
	private ShovelSnowLayerState() {
	}

	// 纯判定：开关开启、手持锹、目标是多层雪层。
	public static boolean shouldReduceOneLayer(boolean enabled, boolean holdingShovel, boolean snowLayer, int layers) {
		return enabled && holdingShovel && snowLayer && layers > 1;
	}

	// 减掉一层并模拟一次使用：按配置决定是否掉落雪球、是否消耗耐久。
	public static void reduceOneLayer(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state,
									  boolean dropSnowball, boolean consumeDurability) {
		int layers = state.getValue(SnowLayerBlock.LAYERS);
		level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1), Block.UPDATE_ALL);
		// 补回原版铲雪的音效与游戏事件：注入点在 useItemOn 头部，原版的声音不会执行。
		level.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		if (player.getAbilities().instabuild) {
			return;
		}
		if (dropSnowball) {
			Block.popResource(level, pos, new ItemStack(Items.SNOWBALL));
		}
		if (consumeDurability) {
			player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
	}
}
