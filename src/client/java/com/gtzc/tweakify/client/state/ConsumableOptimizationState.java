package com.gtzc.tweakify.client.state;

import net.minecraft.sounds.SoundEvent;

/**
 * 消耗品优化的短窗口状态（参考 ConsumableOptimizer）：
 * 记录「客户端已本地完成进食、等待服务端确认」的时间窗口，以及需要抑制的一次性标记。
 *
 * <p>时间由调用方传入毫秒时间戳，便于单测直接验证超时与一次性语义；
 * 标记全部按「一次性」消费，避免长时间吞掉后续正常音效或动画。
 */
public final class ConsumableOptimizationState {
	// 等待服务端确认的最长时间：超过后不再拦截任何数据包，避免服务端不回复时永久抑制。
	public static final long SERVER_WAIT_TIMEOUT_MILLIS = 500L;

	// 手部换手动画的抑制窗口：库存同步要等一个 RTT 才到达，窗口必须比等待窗口宽。
	public static final long EQUIPMENT_ANIMATION_TIMEOUT_MILLIS = 1000L;

	private long waitStartMillis = -1L;
	private boolean burpSuppressed;
	private long equipmentAnimationUntilMillis = -1L;
	private SoundEvent suppressedSound;

	public void startServerWait(long nowMillis) {
		this.waitStartMillis = nowMillis;
	}

	// 是否仍在等待服务端确认；超时后自动结束本次窗口。
	public boolean isWaitingForServer(long nowMillis) {
		if (this.waitStartMillis < 0L) {
			return false;
		}
		if (nowMillis - this.waitStartMillis > SERVER_WAIT_TIMEOUT_MILLIS) {
			this.waitStartMillis = -1L;
			return false;
		}
		return true;
	}

	// 记录本次进食/饮用的音效，用于吃掉服务端随后重复播放的同一个音效。
	public void suppressSound(SoundEvent sound) {
		this.suppressedSound = sound;
	}

	// 一次性：命中后立即清除，之后的同名音效正常播放。
	public boolean consumeSuppressedSound(SoundEvent sound) {
		if (this.suppressedSound == null || !this.suppressedSound.equals(sound)) {
			return false;
		}
		this.suppressedSound = null;
		return true;
	}

	public void clearSoundSuppression() {
		this.suppressedSound = null;
	}

	// 记录需要吃掉的服务端打嗝音效。
	public void suppressBurp() {
		this.burpSuppressed = true;
	}

	// 一次性：命中后立即清除。
	public boolean consumeSuppressedBurp() {
		if (!this.burpSuppressed) {
			return false;
		}
		this.burpSuppressed = false;
		return true;
	}

	// 登记一次手部换手动画抑制：库存同步到达前一直有效，超时自动失效。
	public void suppressEquipmentAnimation(long nowMillis) {
		this.equipmentAnimationUntilMillis = nowMillis + EQUIPMENT_ANIMATION_TIMEOUT_MILLIS;
	}

	public boolean isEquipmentAnimationSuppressed(long nowMillis) {
		if (this.equipmentAnimationUntilMillis < 0L) {
			return false;
		}
		if (nowMillis > this.equipmentAnimationUntilMillis) {
			this.equipmentAnimationUntilMillis = -1L;
			return false;
		}
		return true;
	}

	// 一次性：命中后清除，避免后续手动换手也被跳过动画。
	public void clearEquipmentAnimation() {
		this.equipmentAnimationUntilMillis = -1L;
	}

	// 断线或功能关闭时整体复位。
	public void reset() {
		this.waitStartMillis = -1L;
		this.burpSuppressed = false;
		this.equipmentAnimationUntilMillis = -1L;
		this.suppressedSound = null;
	}
}
