package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 疣块分解：合成格里单独放一个下界疣块时，结果槽产出 9 个下界疣。
 *
 * <p>原版配方优先：只有原版没有产出、且合成格里恰好只有一个下界疣块时才接管结果槽。
 * 结果同步沿用原版 slotChangedCraftingGrid 的做法（setRemoteSlot + 发包），
 * 因此客户端能立即看到产物。合成判定在服务端，仅单人/局域网生效。
 */
@Mixin(CraftingMenu.class)
public class CraftingMenuWartDecompositionMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
	private static void tweakify$decomposeWartBlock(AbstractContainerMenu menu, ServerLevel level, Player player,
												   CraftingContainer container, ResultContainer result,
												   RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.wartBlockDecomposition) {
			return;
		}
		// 原版已有产出时不覆盖（例如其它模组加了疣块配方）。
		if (!result.getItem(0).isEmpty()) {
			return;
		}
		int wartBlocks = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (!stack.is(Items.NETHER_WART_BLOCK)) {
				return;
			}
			wartBlocks += stack.getCount();
			if (wartBlocks > 1) {
				return;
			}
		}
		if (wartBlocks != 1) {
			return;
		}

		ItemStack output = new ItemStack(Items.NETHER_WART, 9);
		// 产物不是原版配方，清掉可能残留的 recipeUsed，避免取出时错记合成记录。
		result.setRecipeUsed(null);
		result.setItem(0, output);
		menu.setRemoteSlot(0, output);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
				menu.containerId, menu.incrementStateId(), 0, output));
		}
	}
}
