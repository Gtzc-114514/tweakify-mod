package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 飞行漂移状态机：创造/旁观飞行时松开方向键后立即停止水平移动。
 * 原版飞行依靠 0.91 空气摩擦衰减速度，松开按键后仍会滑行一段距离；
 * 在客户端 tick 开始时（玩家移动之前）清零水平速度即可消除这段漂移。
 */
public final class FlightDriftState {
	private FlightDriftState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.disableFlightDrift || client.player == null) {
			return;
		}

		LocalPlayer player = client.player;
		if (!player.getAbilities().flying || player.isPassenger()) {
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		boolean stopHorizontal = shouldStopHorizontalDrift(hasHorizontalInput(client),
			movement.x != 0.0 || movement.z != 0.0);
		boolean stopVertical = shouldStopVerticalDrift(hasVerticalInput(client),
			config.flightDriftVertical, movement.y != 0.0);
		if (!stopHorizontal && !stopVertical) {
			return;
		}

		player.setDeltaMovement(stopHorizontal ? 0.0 : movement.x,
			stopVertical ? 0.0 : movement.y,
			stopHorizontal ? 0.0 : movement.z);
	}

	// 纯判定：没有水平方向输入但仍有水平速度时需要清零。
	static boolean shouldStopHorizontalDrift(boolean horizontalInput, boolean hasHorizontalVelocity) {
		return !horizontalInput && hasHorizontalVelocity;
	}

	// 纯判定：开启垂直飞行影响、没有升降输入但仍有垂直速度时需要清零。
	static boolean shouldStopVerticalDrift(boolean verticalInput, boolean affectVertical,
										   boolean hasVerticalVelocity) {
		return affectVertical && !verticalInput && hasVerticalVelocity;
	}

	// 任一水平方向键按下即视为仍在主动移动，垂直升降不影响水平漂移判定。
	private static boolean hasHorizontalInput(Minecraft client) {
		return client.options.keyUp.isDown()
			|| client.options.keyDown.isDown()
			|| client.options.keyLeft.isDown()
			|| client.options.keyRight.isDown();
	}

	// 跳跃/潜行键用于飞行升降，任一按下即保留垂直惯性。
	private static boolean hasVerticalInput(Minecraft client) {
		return client.options.keyJump.isDown() || client.options.keyShift.isDown();
	}
}
