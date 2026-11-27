package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * 禁用玩家盔甲渲染的判定：只作用于玩家（{@link AvatarRenderState}），
 * 僵尸、骷髅等穿戴盔甲的生物与盔甲架保持原版。
 */
public final class PlayerArmorVisibility {
	private PlayerArmorVisibility() {
	}

	/**
	 * @param selfEntityId 本地玩家的实体 id；没有本地玩家时传 -1（视为「其他玩家」）
	 */
	public static boolean shouldHideArmor(HumanoidRenderState state, int selfEntityId) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.disablePlayerArmorRendering) {
			return false;
		}
		if (!(state instanceof AvatarRenderState avatar)) {
			return false;
		}
		// 默认隐藏所有玩家的盔甲；子项开启后保留自己第三人称的装备。
		if (!config.modified.playerArmorRenderingOtherPlayersOnly) {
			return true;
		}
		return selfEntityId < 0 || avatar.id != selfEntityId;
	}
}
