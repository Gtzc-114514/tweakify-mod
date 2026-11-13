package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.CreativeItemGroupAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品分组展开后的成员槽位使用基岩版风格的灰色底：在 {@code extractSlot} 绘制物品之前填充，
 * 这样灰色是真正的槽位背景，而不是覆盖在物品上的半透明层。
 */
@Mixin(AbstractContainerScreen.class)
public class CreativeModeInventorySlotBackgroundMixin {
	// 与基岩版物品分组的展开槽位接近的中灰色，避免纯黑背景过重。
	private static final int EXPANDED_GROUP_BACKGROUND = 0xFF8B8B8B;

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void tweakify$drawExpandedGroupBackground(GuiGraphicsExtractor graphics, Slot slot,
			int mouseX, int mouseY, CallbackInfo callback) {
		if (!((Object) this instanceof CreativeItemGroupAccess access)
			|| !access.tweakify$isExpandedGroupMember(slot)) {
			return;
		}
		// extractContents 已把绘制姿势平移到 GUI 左上角，这里必须使用槽位局部坐标。
		graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, EXPANDED_GROUP_BACKGROUND);
	}
}
