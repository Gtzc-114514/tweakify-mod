package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.EnchantingTableInfo;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 附魔台附魔信息：悬停三个附魔选项按钮时显示该档位实际会给出的附魔。
 *
 * <p>只增加提示，不改变按钮可用性与点击逻辑；计算在悬停时进行，未悬停的帧不触碰注册表。
 */
@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenInfoMixin {
	@Inject(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
		at = @At("TAIL")
	)
	private void tweakify$showEnchantingInfo(GuiGraphicsExtractor gui, int mouseX, int mouseY,
											float partialTick, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		AbstractContainerMenu containerMenu = ((MenuAccess<?>) (Object) this).getMenu();
		if (!config.functions.enchantingTableInfo
			|| !(containerMenu instanceof EnchantmentMenu menu)) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		if (config.functions.enchantingTableInfoRequireSneak
			&& (client.player == null || !client.player.isShiftKeyDown())) {
			return;
		}
		ItemStack stack = menu.getSlot(0).getItem();
		if (stack.isEmpty()) {
			return;
		}
		AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) (Object) this;
		for (int slot = 0; slot < 3; slot++) {
			// 与原版 isHovering 一致：x = leftPos + 60，y = topPos + 14 + 19 * slot，108 × 17。
			int x = screen.tweakify$getLeftPos() + 60;
			int y = screen.tweakify$getTopPos() + 14 + 19 * slot;
			if (mouseX < x || mouseX >= x + 108 || mouseY < y || mouseY >= y + 17) {
				continue;
			}
			List<Component> lines = EnchantingTableInfo.describeOffers(
				client.level.registryAccess(), stack, menu.getEnchantmentSeed(), slot, menu.costs[slot],
				config.functions.enchantingTableInfoShowCost,
				config.functions.enchantingTableInfoShowMaxLevel);
			if (!lines.isEmpty()) {
				gui.setComponentTooltipForNextFrame(client.font, lines, mouseX, mouseY);
			}
			return;
		}
	}
}
