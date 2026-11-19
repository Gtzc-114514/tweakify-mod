package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平滑滚动状态机：输入只改目标、每帧缓动逼近，最后换算成“起始行 + 亚行像素偏移”。
 * 这里覆盖收敛、越界夹紧与行/像素换算的边界，避免动画把列表滚出范围或卡在半行。
 */
class SmoothScrollAnimatorTest {
	private static final int ENTRY_HEIGHT = 24;

	@Test
	void updateConvergesTowardsTargetAndSnaps() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(100.0, 1000.0);

		for (int i = 0; i < 200; i++) {
			animator.update(0.05, 1000.0);
		}

		assertEquals(100.0, animator.position(), 1.0e-9);
		assertEquals(100.0, animator.target(), 1.0e-9);
		assertTrue(animator.isSettled(), "animation should settle on the target");
	}

	// 指数缓动按时间推进：拆成两小步与一次大步的结果必须一致，否则帧率会改变手感。
	@Test
	void easingIsFrameRateIndependent() {
		SmoothScrollAnimator oneStep = new SmoothScrollAnimator();
		oneStep.addDelta(100.0, 1000.0);
		oneStep.update(0.1, 1000.0);

		SmoothScrollAnimator twoSteps = new SmoothScrollAnimator();
		twoSteps.addDelta(100.0, 1000.0);
		twoSteps.update(0.05, 1000.0);
		twoSteps.update(0.05, 1000.0);

		assertEquals(oneStep.position(), twoSteps.position(), 1.0e-9);
	}

	@Test
	void deltaIsClampedToScrollRange() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();

		animator.addDelta(500.0, 100.0);
		assertEquals(100.0, animator.target(), 1.0e-9);

		settle(animator, 100.0);
		assertEquals(100.0, animator.position(), 1.0e-9);

		animator.addDelta(-500.0, 100.0);
		assertEquals(0.0, animator.target(), 1.0e-9);
	}

	// 列表变短（分组收起、搜索过滤）时上限变小：位置与目标都要跟着夹回去。
	@Test
	void shrinkingRangeClampsCurrentPosition() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(200.0, 200.0);
		settle(animator, 200.0);

		animator.update(0.05, 48.0);

		assertEquals(48.0, animator.position(), 1.0e-9);
		assertEquals(48.0, animator.target(), 1.0e-9);
	}

	@Test
	void layoutSplitsRowAndPixelOffset() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(30.0, 1000.0);
		settle(animator, 1000.0);

		SmoothScrollAnimator.Layout layout = animator.layout(ENTRY_HEIGHT);

		assertEquals(1, layout.row());
		assertEquals(6, layout.pixelOffset());
	}

	// 四舍五入正好到整行时必须进位，否则会算出“第 0 行偏移 24 像素”这种非法布局。
	@Test
	void pixelOffsetRoundsUpIntoNextRow() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(23.6, 1000.0);
		settle(animator, 1000.0);

		SmoothScrollAnimator.Layout layout = animator.layout(ENTRY_HEIGHT);

		assertEquals(1, layout.row());
		assertEquals(0, layout.pixelOffset());
	}

	@Test
	void jumpToSnapsWithoutAnimation() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(120.0, 500.0);
		settle(animator, 500.0);

		animator.jumpTo(48.0, 500.0);

		assertEquals(48.0, animator.position(), 1.0e-9);
		assertEquals(48.0, animator.target(), 1.0e-9);
		assertTrue(animator.isSettled(), "jump should not leave an animation running");

		animator.jumpTo(900.0, 500.0);
		assertEquals(500.0, animator.position(), 1.0e-9);
	}

	// 吸附阈值：差距在半像素内时直接落到目标，避免无限逼近留下的抖动。
	@Test
	void tinyDistanceSnapsInOneFrame() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.addDelta(0.4, 100.0);

		animator.update(0.0, 100.0);

		assertEquals(0.4, animator.position(), 1.0e-9);
		assertTrue(animator.isSettled(), "sub-pixel distance should snap");
	}

	// 创造栏以“行”为单位：默认 0.5 的阈值会一步跳掉半行（9 像素），
	// 调用方按半像素换算后必须继续缓动，否则收尾会有明显卡顿。
	@Test
	void customSnapDistanceKeepsSubRowMotion() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.setSnapDistance(0.5 / 18.0);
		animator.addDelta(1.0, 100.0);

		animator.update(0.016, 100.0);

		assertTrue(animator.position() > 0.0 && animator.position() < 1.0,
			"调小阈值后必须逐帧逼近，不能一步吸附");
	}

	private static void settle(SmoothScrollAnimator animator, double maxPixels) {
		for (int i = 0; i < 200; i++) {
			animator.update(0.05, maxPixels);
		}
	}
}
