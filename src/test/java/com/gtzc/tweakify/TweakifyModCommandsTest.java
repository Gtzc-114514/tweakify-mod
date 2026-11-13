package com.gtzc.tweakify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /tweakify 命令权限矩阵：权限等级 ≥2（GAMEMASTERS）或单人世界房主放行。
 *
 * <p>权限判定拆成两个布尔量，避免单元测试依赖真实 MinecraftServer/ServerPlayer；
 * 真实命令树的接线（注册、requires、isSingleplayerOwner）没有自动化覆盖，
 * 改动 /tweakify 命令后需要手动验证单人房主、OP、局域网访客三种场景。
 */
class TweakifyModCommandsTest {
	@Test
	void commandPermissionMatrix() {
		// OP / 权限等级 2。
		assertTrue(TweakifyModCommands.canUseCommands(true, false), "operator must pass");
		// 单人未开作弊的房主：权限为 0，但 isSingleplayerOwner 为 true。
		assertTrue(TweakifyModCommands.canUseCommands(false, true), "singleplayer owner must pass");
		// 单人开作弊的房主同时满足两个条件。
		assertTrue(TweakifyModCommands.canUseCommands(true, true), "operator owner must pass");
		// 局域网访客：既不是 OP 也不是单人房主。
		assertFalse(TweakifyModCommands.canUseCommands(false, false), "LAN guest must be rejected");
	}

	@Test
	void grantedGuestPassesThroughPermissionLevel() {
		// 局域网访客被授予命令权限（等级 2）：权限判定只看等级，不依赖单人房主身份。
		assertTrue(TweakifyModCommands.canUseCommands(true, false),
			"guest granted level 2 must pass without being the singleplayer owner");
	}
}
