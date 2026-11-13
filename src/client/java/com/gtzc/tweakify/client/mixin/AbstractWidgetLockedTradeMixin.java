package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.MerchantMenuAccess;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * 可见交易（客户端）：在每帧控件悬停判断里刷新交易按钮的锁定状态，
 * 让未来等级的交易一出现就是灰色、不可点击，而不是要点一下才变灰。
 *
 * <p>TradeOfferButton 是 MerchantScreen 的私有内部类，无法直接引用类型；
 * 这里按类名过滤并用缓存的反射字段读取它所属的屏幕与行号。
 * 没有锁定数据（远程服务器/满级/未安装）时恢复 active=true，不影响正常交易。
 */
@Mixin(AbstractWidget.class)
public class AbstractWidgetLockedTradeMixin {
	@Unique
	private static final String TWEAKIFY$TRADE_BUTTON_CLASS =
		"net.minecraft.client.gui.screens.inventory.MerchantScreen$TradeOfferButton";
	@Unique
	private static Field tweakify$screenField;
	@Unique
	private static Field tweakify$indexField;
	@Unique
	private static boolean tweakify$fieldsResolved;
	@Unique
	private static boolean tweakify$resolveFailed;

	@Inject(method = "isHoveredOrFocused", at = @At("HEAD"), require = 0)
	private void tweakify$updateLockedState(CallbackInfoReturnable<Boolean> cir) {
		AbstractWidget widget = (AbstractWidget) (Object) this;
		if (tweakify$resolveFailed || !widget.getClass().getName().equals(TWEAKIFY$TRADE_BUTTON_CLASS)) {
			return;
		}
		if (!tweakify$resolveFields(widget.getClass())) {
			tweakify$resolveFailed = true;
			return;
		}
		try {
			MerchantScreen screen = (MerchantScreen) tweakify$screenField.get(widget);
			int index = tweakify$indexField.getInt(widget);
			MerchantMenuAccess access = screen.getMenu() instanceof MerchantMenu menu
				&& menu instanceof MerchantMenuAccess menuAccess ? menuAccess : null;
			boolean locked = false;
			if (access != null && access.tweakify$hasLockedOffers()) {
				int scrollOff = ((MerchantScreenAccessor) screen).tweakify$getScrollOff();
				locked = index + scrollOff >= access.tweakify$getOriginalCount();
			}
			widget.active = !locked;
		} catch (IllegalAccessException ignored) {
			tweakify$resolveFailed = true;
		}
	}

	@Unique
	private static boolean tweakify$resolveFields(Class<?> type) {
		if (tweakify$fieldsResolved) {
			return true;
		}
		try {
			Field screen = type.getDeclaredField("this$0");
			screen.setAccessible(true);
			Field index = type.getDeclaredField("index");
			index.setAccessible(true);
			tweakify$screenField = screen;
			tweakify$indexField = index;
			tweakify$fieldsResolved = true;
			return true;
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}
}
