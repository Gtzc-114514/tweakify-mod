package com.gtzc.tweakify.client.state;

import net.minecraft.sounds.SoundEvent;

/**
 * 消耗品优化的短窗口状态（参考 ConsumableOptimizer）：
 * 记录「客户端已本地完成进食、等待服务端确认」的时间窗口，以及需要抑制的一次性标记。
 *
 * <p>时间由调用方传入毫秒时间戳，便于单测直接验证超时与一次性语义；
 * 窗口与音效归属半径按 RTT 自适应（3×RTT 夹在 500ms–2s；半径 2–6 格），
 * 高延迟下既不会过早放弃抑制，也不会无限期地吞掉后续数据包。
 * 标记全部按「一次性」消费，避免长时间吞掉后续正常音效或动画。
 */
public final class ConsumableOptimizationState {
	// 等待服务端确认的下限：本地或低延迟时足够覆盖一次往返。
	public static final long MIN_SERVER_WAIT_TIMEOUT_MILLIS = 500L;
	// 等待窗口上限：避免长时间吃掉后续正常的数据包。
	public static final long MAX_SERVER_WAIT_TIMEOUT_MILLIS = 2000L;
	// 默认窗口（= 下限），保留旧名供默认路径使用。
	public static final long SERVER_WAIT_TIMEOUT_MILLIS = MIN_SERVER_WAIT_TIMEOUT_MILLIS;

	// 服务端音效的归属半径：基础 2 格，按 RTT 每 100ms 放宽 0.3 格（补偿等待期间玩家移动），最多 6 格。
	public static final double SOUND_RADIUS_BASE = 2.0D;
	public static final double SOUND_RADIUS_PER_MILLIS = 0.003D;
	public static final double SOUND_RADIUS_MAX = 6.0D;

	// 手部换手动画的抑制窗口：至少 1s，且要比服务端等待窗口宽 500ms（库存同步本身也要一个 RTT）。
	public static final long EQUIPMENT_ANIMATION_TIMEOUT_MILLIS = 1000L;
	public static final long EQUIPMENT_ANIMATION_MARGIN_MILLIS = 500L;

	private long waitStartMillis = -1L;
	private long serverWaitTimeoutMillis = SERVER_WAIT_TIMEOUT_MILLIS;
	private boolean burpSuppressed;
	private long equipmentAnimationUntilMillis = -1L;
	private SoundEvent suppressedSound;

	// 自适应等待窗口：服务端完成事件与音效各占一段往返，按 3×RTT 估算，夹在 [500ms, 2s]。
	public static long adaptiveServerWaitMillis(int latencyMillis) {
		long estimate = 3L * Math.max(0, latencyMillis);
		return Math.max(MIN_SERVER_WAIT_TIMEOUT_MILLIS, Math.min(MAX_SERVER_WAIT_TIMEOUT_MILLIS, estimate));
	}

	// 自适应音效归属半径：RTT 越长玩家移动越多，半径从 2 格线性放宽到 6 格。
	public static double adaptiveSoundRadius(int latencyMillis) {
		double radius = SOUND_RADIUS_BASE + SOUND_RADIUS_PER_MILLIS * Math.max(0, latencyMillis);
		return Math.min(SOUND_RADIUS_MAX, radius);
	}

	public void startServerWait(long nowMillis) {
		startServerWait(nowMillis, SERVER_WAIT_TIMEOUT_MILLIS);
	}

	// timeoutMillis 由 adaptiveServerWaitMillis 计算；这里再夹一次范围，防止调用方误传。
	public void startServerWait(long nowMillis, long timeoutMillis) {
		this.waitStartMillis = nowMillis;
		this.serverWaitTimeoutMillis = Math.max(MIN_SERVER_WAIT_TIMEOUT_MILLIS,
			Math.min(MAX_SERVER_WAIT_TIMEOUT_MILLIS, timeoutMillis));
	}

	// 是否仍在等待服务端确认；超时后自动结束本次窗口。
	public boolean isWaitingForServer(long nowMillis) {
		if (this.waitStartMillis < 0L) {
			return false;
		}
		if (nowMillis - this.waitStartMillis > this.serverWaitTimeoutMillis) {
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
		suppressEquipmentAnimation(nowMillis, SERVER_WAIT_TIMEOUT_MILLIS);
	}

	// 窗口随服务端等待时间放宽：高延迟下库存同步也更晚到达。
	public void suppressEquipmentAnimation(long nowMillis, long serverWaitMillis) {
		long waited = Math.max(MIN_SERVER_WAIT_TIMEOUT_MILLIS,
			Math.min(MAX_SERVER_WAIT_TIMEOUT_MILLIS, serverWaitMillis));
		long window = Math.max(EQUIPMENT_ANIMATION_TIMEOUT_MILLIS,
			waited + EQUIPMENT_ANIMATION_MARGIN_MILLIS);
		this.equipmentAnimationUntilMillis = nowMillis + window;
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
		this.serverWaitTimeoutMillis = SERVER_WAIT_TIMEOUT_MILLIS;
		this.burpSuppressed = false;
		this.equipmentAnimationUntilMillis = -1L;
		this.suppressedSound = null;
	}
}
