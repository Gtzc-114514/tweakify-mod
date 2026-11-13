package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * 交互使用冷却状态机：一次右键只使用一次（按主/副手分别记录）。
 * 按下右键开启一个会话，会话内某只手使用过一次后，该手后续的 startUseItem 被拦截；
 * 松开右键或再次按下时复位会话。
 */
public final class UseCooldownState {
	private static final KeyHoldSession MAIN_HAND = new KeyHoldSession();
	private static final KeyHoldSession OFF_HAND = new KeyHoldSession();

	private UseCooldownState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		// 总开关关闭或两手冷却都关闭时不再跟踪会话，省掉每 tick 的按键读取。
		if (config == null || !config.functions.useCooldown
			|| (!config.functions.mainHandUseCooldown && !config.functions.offhandUseCooldown)
			|| client.player == null) {
			MAIN_HAND.reset();
			OFF_HAND.reset();
			return;
		}

		// 按下与松开都复位：会话只在按住期间有效。
		boolean useDown = client.options.keyUse.isDown();
		MAIN_HAND.update(useDown, false);
		OFF_HAND.update(useDown, false);
	}

	// 由 LocalPlayerMixin 在 itemUsed 时回调，标记对应手已经使用过。
	public static void markHandUsed(InteractionHand hand) {
		if (hand == InteractionHand.MAIN_HAND) {
			MAIN_HAND.mark();
		} else if (hand == InteractionHand.OFF_HAND) {
			OFF_HAND.mark();
		}
	}

	public static boolean mainUsedThisSession() {
		return MAIN_HAND.isActive();
	}

	public static boolean offhandUsedThisSession() {
		return OFF_HAND.isActive();
	}

	// 纯判定：某只手是否应被使用冷却拦截。
	public static boolean shouldBlockHand(boolean handEnabled, boolean handUsedThisSession, boolean itemRestricted) {
		return handEnabled && handUsedThisSession && itemRestricted;
	}
}
