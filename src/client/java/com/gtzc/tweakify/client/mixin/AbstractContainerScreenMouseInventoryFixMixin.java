package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-577：物品栏里绑定到鼠标键的关闭背包/丢弃物品按键不生效。
 *
 * <p>原版 mouseClicked 只把鼠标键交给槽位点击处理，不会像键盘那样匹配
 * keyInventory/keyDrop；这里在鼠标按下时补上这两个常用动作。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMouseInventoryFixMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$MOUSE_BUTTON_INVENTORY_PROBE = new AtomicBoolean();

	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$handleMouseBoundInventoryKeys(MouseButtonEvent event, boolean isRealMouse,
			CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.fix.fixMouseButtonInventory) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$MOUSE_BUTTON_INVENTORY_PROBE, "mouseButtonInventoryFix");
		Minecraft client = Minecraft.getInstance();
		if (config.fix.mouseButtonInventoryClose && client.options.keyInventory.matchesMouse(event)) {
			((AbstractContainerScreen<?>) (Object) this).onClose();
			cir.setReturnValue(true);
			return;
		}
		if (config.fix.mouseButtonInventoryDrop && this.hoveredSlot != null
			&& client.options.keyDrop.matchesMouse(event)) {
			// 与键盘 Q 一致：按住 Ctrl 时丢弃整组，否则丢一个。
			this.slotClicked(this.hoveredSlot, this.hoveredSlot.index,
				event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
			cir.setReturnValue(true);
		}
	}

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput type);
}
