package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 禁用玩家盔甲渲染的判定表：总开关、对象是否为玩家、是否连自己一起隐藏。
 */
class PlayerArmorVisibilityTest {
	private static final int SELF_ID = 42;

	@AfterEach
	void resetConfig() {
		TweakifyModTestSupport.installConfig(null);
	}

	@Test
	void disabledByDefault() {
		install(false, false);

		assertFalse(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID), SELF_ID));
	}

	@Test
	void hidesOtherPlayersArmorOnly() {
		install(true, true);

		assertTrue(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID + 1), SELF_ID));
		assertFalse(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID), SELF_ID),
			"「只隐藏其他玩家」保留自己第三人称的盔甲");
	}

	@Test
	void hidesAllPlayerArmorByDefault() {
		install(true, false);

		assertTrue(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID), SELF_ID));
		assertTrue(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID + 1), SELF_ID));
	}

	@Test
	void nonPlayerHumanoidsKeepVanillaArmor() {
		install(true, true);

		assertFalse(PlayerArmorVisibility.shouldHideArmor(new HumanoidRenderState(), SELF_ID),
			"僵尸/骷髅等生物不能受影响");
	}

	@Test
	void missingLocalPlayerIsTreatedAsOtherPlayer() {
		install(true, true);

		assertTrue(PlayerArmorVisibility.shouldHideArmor(avatar(SELF_ID), -1));
	}

	private static AvatarRenderState avatar(int entityId) {
		AvatarRenderState state = new AvatarRenderState();
		state.id = entityId;
		return state;
	}

	private static void install(boolean enabled, boolean otherPlayersOnly) {
		TweakifyModConfig config = new TweakifyModConfig();
		config.modified.disablePlayerArmorRendering = enabled;
		config.modified.playerArmorRenderingOtherPlayersOnly = otherPlayersOnly;
		TweakifyModTestSupport.installConfig(config);
	}
}
