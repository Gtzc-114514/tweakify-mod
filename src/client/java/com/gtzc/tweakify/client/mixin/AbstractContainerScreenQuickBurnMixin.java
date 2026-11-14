package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.QuickBurnFeature;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 便捷焚烧：光标拿着物品、右键点击岩浆桶/打火石槽位时焚烧该物品，
 * 并在满足条件时于鼠标上方显示红色提示。
 *
 * <p>slotClicked 写全描述符：26.3 的同名重载有多个，避免注入目标歧义。
 * 注入点用 require = 0：界面类可能被其它模组改写，冲突时功能失效即可。
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenQuickBurnMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$QUICK_BURN_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$QUICK_BURN_HINT_PROBE = new AtomicBoolean();

	// 原版悬停槽位：与 tooltip 使用同一个来源，保证提示与实际点击目标一致。
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$burnOnRightClick(Slot slot, int slotId, int mouseButton, ContainerInput type,
											CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$QUICK_BURN_PROBE, "slotClicked");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.quickBurnItem) {
			return;
		}
		// 只有右键 + PICKUP 才是"拿着物品点击槽位"的交互；左键/快速移动保持原版。
		if (mouseButton != 1 || type != ContainerInput.PICKUP) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		AbstractContainerMenu menu = ((AbstractContainerScreen<?>) (Object) this).getMenu();
		if (QuickBurnFeature.canBurn(menu, slot) && QuickBurnFeature.tryBurn(client, menu)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void tweakify$renderBurnHint(GuiGraphicsExtractor gui, int mouseX, int mouseY,
										  float partialTick, CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$QUICK_BURN_HINT_PROBE, "quickBurnHint");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.quickBurnItem) {
			return;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (!QuickBurnFeature.canBurn(screen.getMenu(), this.hoveredSlot)) {
			return;
		}
		// 画在鼠标上方、物品 tooltip 之外，避免与 tooltip 重叠。
		// 每次现建 Component：TranslatableContents 会按 Language 实例缓存解析结果，
		// 而复用同一个 Component 会让模组语言覆盖（查询期逻辑，不替换 Language 实例）
		// 在切换配置语言后不立即生效。这里每次悬停只建一个小对象，开销可忽略。
		gui.text(screen.getFont(), Component.translatable("tweakify-mod.message.quick_burn.hint"),
			mouseX + 8, mouseY - 24, 0xFFFF5555);
	}
}
