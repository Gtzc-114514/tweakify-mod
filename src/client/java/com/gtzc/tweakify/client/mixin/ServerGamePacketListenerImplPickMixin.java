package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 中键取物（服务端部分）：
 * 原版取物按方块换算物品，水/岩浆方块没有对应物品，会直接返回。
 * 这里把空结果换成对应的液体桶，后续的“已有就切换、创造模式直接给”仍走原版逻辑。
 *
 * <p>创造模式下归「中键获得液体桶」，生存模式下归「生存模式中键取物」；
 * 只处理液体方块本身，水浸方块（如含水楼梯）仍取方块物品。
 * 仅在单人 / 局域网（集成服务端）生效。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplPickMixin {
	@Shadow
	public ServerPlayer player;

	@WrapOperation(method = "handlePickItemFromBlock", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;"
			+ "getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)"
			+ "Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack tweakify$fluidBucketInsteadOfAir(BlockState state, LevelReader level, BlockPos pos,
													   boolean includeData, Operation<ItemStack> original) {
		// 低频路径（仅中键取物包），用字符串版本即可；名字带 Server 后缀以区别于客户端同名探针。
		HookProbe.markOnce("pickFluidBucketServer");
		ItemStack cloneStack = original.call(state, level, pos, includeData);
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !cloneStack.isEmpty()) {
			return cloneStack;
		}
		// 创造模式归“中键获得液体桶”；生存模式归“生存模式中键取物”。
		boolean creative = this.player.hasInfiniteMaterials();
		if (creative ? !config.functions.pickFluidBucket : !config.functions.survivalPickBlock) {
			return cloneStack;
		}
		if (!(state.getBlock() instanceof LiquidBlock)) {
			return cloneStack;
		}
		if (state.is(Blocks.WATER)) {
			return new ItemStack(Items.WATER_BUCKET);
		}
		if (state.is(Blocks.LAVA)) {
			return new ItemStack(Items.LAVA_BUCKET);
		}
		return cloneStack;
	}
}
