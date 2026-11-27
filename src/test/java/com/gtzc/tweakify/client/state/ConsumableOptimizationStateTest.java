package com.gtzc.tweakify.client.state;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消耗品优化的短窗口状态：等待窗口超时、音效与打嗝的一次性消费、手部动画标记的复位。
 */
class ConsumableOptimizationStateTest {
	private static final SoundEvent EAT_SOUND =
		SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.generic.eat"));
	private static final SoundEvent OTHER_SOUND =
		SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.player.burp"));

	@Test
	void waitWindowEndsAfterTimeout() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.startServerWait(1000L);

		assertTrue(state.isWaitingForServer(1000L));
		assertTrue(state.isWaitingForServer(1000L + ConsumableOptimizationState.SERVER_WAIT_TIMEOUT_MILLIS));
		assertFalse(state.isWaitingForServer(1000L + ConsumableOptimizationState.SERVER_WAIT_TIMEOUT_MILLIS + 1L),
			"超过等待上限后必须放行后续数据包");
		assertFalse(state.isWaitingForServer(2000L), "超时后窗口已自动结束");
	}

	@Test
	void suppressedSoundIsConsumedOnlyOnce() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.suppressSound(EAT_SOUND);

		assertTrue(state.consumeSuppressedSound(EAT_SOUND));
		assertFalse(state.consumeSuppressedSound(EAT_SOUND), "同一个音效只吃掉一次");
	}

	@Test
	void suppressedSoundIgnoresOtherSounds() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.suppressSound(EAT_SOUND);

		assertFalse(state.consumeSuppressedSound(OTHER_SOUND));
		assertTrue(state.consumeSuppressedSound(EAT_SOUND), "不匹配的查询不能清掉已登记的抑制音效");
	}

	@Test
	void suppressedBurpIsConsumedOnlyOnce() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.suppressBurp();

		assertTrue(state.consumeSuppressedBurp());
		assertFalse(state.consumeSuppressedBurp());
	}

	@Test
	void equipmentAnimationWindowExpires() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.suppressEquipmentAnimation(1000L);

		assertTrue(state.isEquipmentAnimationSuppressed(1000L));
		assertTrue(state.isEquipmentAnimationSuppressed(
			1000L + ConsumableOptimizationState.EQUIPMENT_ANIMATION_TIMEOUT_MILLIS));
		assertFalse(state.isEquipmentAnimationSuppressed(
			1000L + ConsumableOptimizationState.EQUIPMENT_ANIMATION_TIMEOUT_MILLIS + 1L),
			"库存同步迟迟不到时必须自动失效");
	}

	@Test
	void equipmentAnimationFlagIsClearedByQuery() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.suppressEquipmentAnimation(0L);

		assertTrue(state.isEquipmentAnimationSuppressed(0L));
		state.clearEquipmentAnimation();
		assertFalse(state.isEquipmentAnimationSuppressed(0L));
	}

	@Test
	void resetClearsEveryFlag() {
		ConsumableOptimizationState state = new ConsumableOptimizationState();
		state.startServerWait(0L);
		state.suppressSound(EAT_SOUND);
		state.suppressBurp();
		state.suppressEquipmentAnimation(0L);

		state.reset();

		assertFalse(state.isWaitingForServer(0L));
		assertFalse(state.consumeSuppressedSound(EAT_SOUND));
		assertFalse(state.consumeSuppressedBurp());
		assertFalse(state.isEquipmentAnimationSuppressed(0L));
	}
}
