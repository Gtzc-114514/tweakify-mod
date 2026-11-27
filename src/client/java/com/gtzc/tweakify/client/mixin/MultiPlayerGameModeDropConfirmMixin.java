package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.DropConfirmState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 丢弃确认：26.3 的丢弃键最终只调用 {@link MultiPlayerGameMode#dropItem}，
 * 因此在入口拦截即可覆盖 Q / Ctrl+Q，且不影响容器界面里拖出物品的丢弃路径。
 *
 * <p>第一次按键取消本次丢弃并在物品栏上方提示“再按一次确认”；
 * 确认窗口内对同一物品再次按键时放行原版逻辑。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeDropConfirmMixin {
	@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
	private void tweakify$confirmDropItem(LocalPlayer player, boolean dropAll, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		ItemStack stack = player.getInventory().getSelectedItem();
		DropConfirmState.Decision decision = DropConfirmState.evaluate(
			config.functions.dropConfirm, config.functions.dropConfirmItems, itemIdOf(stack));
		if (decision != DropConfirmState.Decision.BLOCK) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.gui != null) {
			// 与原版动作栏消息同一位置：快捷栏上方。
			client.gui.hud.setOverlayMessage(
				Component.translatable("tweakify-mod.message.drop_confirm", stack.getHoverName()), false);
		}
		ci.cancel();
	}

	// 取物品的注册表 ID；空栈返回 null，由状态机按“不拦截”处理。
	private static String itemIdOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id == null ? null : id.toString();
	}
}
