package com.gtzc.tweakify.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraMotionTest {
	private static final CameraMotion.Settings ALL_EFFECTS = new CameraMotion.Settings(
		100, true, true, true, true, true, true, false, true, true);

	@BeforeEach
	void resetGlobalState() {
		CameraMotion.clearShakes();
		CameraMotion.clearGlobalAction();
	}

	@Test
	void forwardRelativeVelocityUsesCameraYaw() {
		CameraMotion.ForwardVelocity southForward = CameraMotion.forwardRelativeVelocity(0.0, 1.0, 0.0f);
		assertEquals(0.0, southForward.strafe(), 1.0e-9);
		assertEquals(1.0, southForward.forward(), 1.0e-9);

		CameraMotion.ForwardVelocity eastStrafe = CameraMotion.forwardRelativeVelocity(1.0, 0.0, 0.0f);
		assertEquals(1.0, eastStrafe.strafe(), 1.0e-9);
		assertEquals(0.0, eastStrafe.forward(), 1.0e-9);

		CameraMotion.ForwardVelocity westForward = CameraMotion.forwardRelativeVelocity(-1.0, 0.0, 90.0f);
		assertEquals(0.0, westForward.strafe(), 1.0e-9);
		assertEquals(1.0, westForward.forward(), 1.0e-9);
	}

	@Test
	void anglesAreUnwrappedAcrossTheSeam() {
		assertEquals(10.0, CameraMotion.unwrapStep(370.0), 1.0e-9);
		assertEquals(-10.0, CameraMotion.unwrapStep(-370.0), 1.0e-9);
		assertEquals(170.0, CameraMotion.unwrapStep(170.0), 1.0e-9);
		assertEquals(-170.0, CameraMotion.wrapDiff(190.0), 1.0e-9);
	}

	@Test
	void dampingMovesTowardTargetWithoutOvershoot() {
		double first = CameraMotion.damp(0.0, 10.0, 0.1, 1.0 / 60.0);
		double second = CameraMotion.damp(first, 10.0, 0.1, 1.0 / 60.0);
		assertTrue(first > 0.0 && first < 10.0);
		assertTrue(second > first && second < 10.0);
		assertEquals(0.0, CameraMotion.dampStep(0.1, 0.0), 1.0e-9);
	}

	@Test
	void firstPersonAndPanoramicCanBeDisabled() {
		CameraMotion.State state = new CameraMotion.State();
		CameraMotion.Settings thirdPersonOnly = new CameraMotion.Settings(
			100, true, true, true, true, true, true, false, false, true);
		CameraMotion.Offset offset = state.update(input(true, 0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0), thirdPersonOnly);
		assertTrue(offset.isZero());
	}

	@Test
	void zeroIntensityDisablesAllEffects() {
		CameraMotion.State state = new CameraMotion.State();
		CameraMotion.Settings zero = new CameraMotion.Settings(
			0, true, true, true, true, true, true, false, true, true);
		CameraMotion.Offset offset = state.update(input(false, 20.0, 0.0, 0.0, 0.0f, 0.0f, 0.0), zero);
		assertTrue(offset.isZero());
	}

	@Test
	void turningRightAndLeftProduceOppositeRoll() {
		CameraMotion.State right = new CameraMotion.State();
		CameraMotion.Offset rightOffset = feedTurn(right, 50.0f, 40.0f);
		CameraMotion.State left = new CameraMotion.State();
		CameraMotion.Offset leftOffset = feedTurn(left, 40.0f, 50.0f);
		assertNotEquals(0.0f, rightOffset.roll(), 1.0e-6f);
		assertNotEquals(0.0f, leftOffset.roll(), 1.0e-6f);
		assertEquals(Math.signum(rightOffset.roll()), -Math.signum(leftOffset.roll()), 1.0e-6);
	}

	@Test
	void strafeVelocityRollsCameraTowardTheMovement() {
		CameraMotion.State right = new CameraMotion.State();
		CameraMotion.Offset rightOffset = feedVelocity(right, 0.2, 0.0, 0.0, 0.0, 0.0f);
		CameraMotion.State left = new CameraMotion.State();
		CameraMotion.Offset leftOffset = feedVelocity(left, -0.2, 0.0, 0.0, 0.0, 0.0f);
		assertTrue(rightOffset.roll() < 0.0f, "right strafe should roll negatively: " + rightOffset);
		assertTrue(leftOffset.roll() > 0.0f, "left strafe should roll positively: " + leftOffset);
	}

	@Test
	void forwardAndVerticalVelocityPitchTheCamera() {
		CameraMotion.State forward = new CameraMotion.State();
		CameraMotion.Offset forwardOffset = feedVelocity(forward, 0.0, 0.0, 0.2, 0.0, 0.0f);
		assertTrue(forwardOffset.pitch() > 0.0f, "moving forward should pitch down: " + forwardOffset);

		CameraMotion.State jump = new CameraMotion.State();
		CameraMotion.Offset jumpOffset = feedVelocity(jump, 0.0, 0.4, 0.0, 0.0, 0.0f);
		assertTrue(jumpOffset.pitch() > 0.0f, "jumping should pitch the camera: " + jumpOffset);
	}

	@Test
	void idleSwayAppearsAfterThePlayerStopsMoving() {
		CameraMotion.State state = new CameraMotion.State();
		state.update(input(false, 0, 0, 0, 0.0f, 0.0f, 0.0), ALL_EFFECTS);
		CameraMotion.Offset offset = CameraMotion.Offset.ZERO;
		for (int i = 1; i <= 200; i++) {
			offset = state.update(input(false, 0, 0, 0, 0.0f, 0.0f, i * 0.05), ALL_EFFECTS);
		}
		assertTrue(Math.abs(offset.pitch()) > 1.0e-4f || Math.abs(offset.yaw()) > 1.0e-4f,
			"idle sway should produce a small non-zero offset: " + offset);
	}

	@Test
	void screenShakeDecaysAndDisappears() {
		CameraMotion.Settings shakeOnly = new CameraMotion.Settings(
			100, false, false, false, false, false, true, false, true, true);
		CameraMotion.State state = new CameraMotion.State();
		CameraMotion.addShake(0.0, 0.0, 0.0, 32.0, 1.0, 1.0, 1.0, 100.0);
		CameraMotion.Offset active = state.update(input(false, 0, 0, 0, 0.0f, 0.0f, 100.1), shakeOnly);
		assertNotEquals(CameraMotion.Offset.ZERO, active);
		CameraMotion.Offset expired = state.update(input(false, 0, 0, 0, 0.0f, 0.0f, 102.0), shakeOnly);
		assertTrue(expired.isZero(), "expired shake should be cleared: " + expired);
	}

	@Test
	void mouseSmoothingConvergesBackToTheRawAngle() {
		CameraMotion.Settings smoothing = new CameraMotion.Settings(
			100, false, false, false, false, false, false, true, true, true);
		CameraMotion.State state = new CameraMotion.State();
		CameraMotion.Offset offset = CameraMotion.Offset.ZERO;
		for (int i = 1; i <= 60; i++) {
			offset = state.update(inputSprint(false, i * 0.2f, i * 0.016), smoothing);
		}
		double magnitude = Math.abs(offset.yaw());
		for (int i = 61; i <= 400; i++) {
			offset = state.update(inputSprint(false, 12.0f, i * 0.016), smoothing);
		}
		assertTrue(Math.abs(offset.yaw()) < magnitude,
			"smoothed yaw offset should shrink after the turn stops");
	}

	private static CameraMotion.Offset feedTurn(CameraMotion.State state, float fromYaw, float toYaw) {
		state.update(input(false, 0.0, 0.0, 0.0, fromYaw, 0.0f, 0.0), ALL_EFFECTS);
		CameraMotion.Offset offset = CameraMotion.Offset.ZERO;
		for (int i = 1; i <= 20; i++) {
			offset = state.update(input(false, 0.0, 0.0, 0.0, toYaw, 0.0f, i * 0.016), ALL_EFFECTS);
		}
		return offset;
	}

	private static CameraMotion.Offset feedVelocity(CameraMotion.State state, double vx, double vy,
													double vz, double unused, float yaw) {
		state.update(input(false, vx, vy, vz, yaw, 0.0f, 0.0), ALL_EFFECTS);
		CameraMotion.Offset offset = CameraMotion.Offset.ZERO;
		for (int i = 1; i <= 120; i++) {
			offset = state.update(input(false, vx, vy, vz, yaw, 0.0f, i * 0.016), ALL_EFFECTS);
		}
		return offset;
	}

	private static CameraMotion.Input input(boolean firstPerson, double vx, double vy, double vz,
											float yaw, float pitch, double now) {
		return new CameraMotion.Input(pitch, yaw, vx, vy, vz, firstPerson, false,
			false, false, false, false, false, false, now, 0.016, 0.0, 0.0, 0.0);
	}

	private static CameraMotion.Input inputSprint(boolean firstPerson, float yaw, double now) {
		return new CameraMotion.Input(0.0f, yaw, 0.0, 0.0, 0.0, firstPerson, false,
			false, true, false, false, false, false, now, 0.016, 0.0, 0.0, 0.0);
	}
}
