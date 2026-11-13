package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.joml.SimplexNoise;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 相机运动（参考 CameraOverhaul）：转向侧倾、横移侧倾、前后/垂直俯仰、
 * 静止晃动、屏幕震动与鼠标平滑。
 *
 * <p>这里只计算“相对原版视角的偏移量”，由 {@code CameraMotionMixin} 叠加到
 * 渲染状态上；玩家实际视角、准星判定与网络同步保持原版。
 */
public final class CameraMotion {
	// 参考模组的默认强度与平滑参数。
	private static final double BASE_TURNING_ROLL_ACCUMULATION = 0.0048;
	private static final double BASE_TURNING_ROLL_INTENSITY = 1.25;
	private static final double BASE_TURNING_ROLL_SMOOTHING = 0.0825;
	private static final double BASE_STRAFING_ROLL_FACTOR = 10.0;
	private static final double BASE_STRAFING_ROLL_SMOOTHING = 0.008;
	private static final double BASE_FORWARD_PITCH_FACTOR = 7.0;
	private static final double BASE_FORWARD_PITCH_SMOOTHING = 0.008;
	private static final double BASE_VERTICAL_PITCH_FACTOR = 2.5;
	private static final double BASE_VERTICAL_PITCH_SMOOTHING = 0.00004;
	private static final double VERTICAL_PITCH_THRESHOLD = 0.4;
	private static final double BASE_MOUSE_SMOOTHING = 16.0;
	private static final double MOUSE_SMOOTHING_THRESHOLD = 0.001;
	private static final double SWAY_INTENSITY = 0.60;
	private static final double SWAY_FREQUENCY = 0.16;
	private static final double SWAY_FADE_IN_DELAY = 0.15;
	private static final double SWAY_FADE_IN_LENGTH = 5.0;
	private static final double SWAY_FADE_OUT_LENGTH = 0.75;
	private static final double SWAY_FADING_SMOOTHNESS = 3.0;
	private static final double SCREEN_SHAKE_MAX_INTENSITY = 2.5;
	private static final double SCREEN_SHAKE_MAX_FREQUENCY = 6.0;
	private static final int MAX_SHAKES = 64;

	private static final List<Shake> SHAKES = new CopyOnWriteArrayList<>();

	// 挥手等一次性动作：由其他 Mixin 写入，相机状态在下一帧消费。
	private static volatile double lastGlobalAction = Double.NEGATIVE_INFINITY;

	private CameraMotion() {
	}

	/** 当前配置对应的效果开关与强度。 */
	public record Settings(int intensityPercent, boolean turningRoll, boolean strafingRoll,
						   boolean forwardPitch, boolean verticalPitch, boolean idleSway,
						   boolean screenShake, boolean mouseSmoothing,
						   boolean firstPerson, boolean thirdPerson) {
		public static Settings fromConfig(TweakifyModConfig config) {
			return new Settings(
				config.functions.cameraMotionIntensity,
				config.functions.cameraMotionTurningRoll,
				config.functions.cameraMotionStrafingRoll,
				config.functions.cameraMotionForwardPitch,
				config.functions.cameraMotionVerticalPitch,
				config.functions.cameraMotionIdleSway,
				config.functions.cameraMotionScreenShake,
				config.functions.cameraMotionMouseSmoothing,
				config.functions.cameraMotionFirstPerson,
				config.functions.cameraMotionThirdPerson);
		}

		public double scale() {
			return Math.clamp(this.intensityPercent,
				TweakifyModConfig.MIN_CAMERA_MOTION_INTENSITY,
				TweakifyModConfig.MAX_CAMERA_MOTION_INTENSITY) / 100.0;
		}

		public boolean enabledFor(boolean inFirstPerson) {
			return inFirstPerson ? this.firstPerson : this.thirdPerson;
		}
	}

	/** 每帧输入：原始相机角度、实体速度、运动状态与时间。 */
	public record Input(float xRot, float yRot, double velocityX, double velocityY,
						double velocityZ, boolean firstPerson, boolean inWater,
						boolean flying, boolean sprinting, boolean riding,
						boolean ridingMount, boolean ridingVehicle, boolean panoramic,
						double nowSeconds, double deltaSeconds,
						double cameraX, double cameraY, double cameraZ) {
	}

	/** 叠加到相机上的角度偏移（度）。 */
	public record Offset(float pitch, float yaw, float roll) {
		public static final Offset ZERO = new Offset(0.0f, 0.0f, 0.0f);

		public boolean isZero() {
			return Math.abs(this.pitch) < 1.0e-4f
				&& Math.abs(this.yaw) < 1.0e-4f
				&& Math.abs(this.roll) < 1.0e-4f;
		}

		Offset scaled(double scale) {
			if (scale == 1.0) {
				return this;
			}
			return new Offset((float) (this.pitch * scale),
				(float) (this.yaw * scale), (float) (this.roll * scale));
		}
	}

	/** 有状态的运动计算：每个相机实例持有一份。 */
	public static final class State {
		private boolean initialized;
		private boolean prevFirstPerson;
		private float prevYaw;
		private float prevPitch;
		private double prevVelocityX;
		private double prevVelocityY;
		private double prevVelocityZ;
		private double lastActionTime;

		private double turningRollTarget;
		private double prevStrafingRoll;
		private double prevForwardPitch;
		private double prevVerticalPitch;

		private double swayFactor;
		private double swayTarget;

		private double continuousYaw;
		private double continuousPitch;
		private double smoothedYaw;
		private double smoothedPitch;

		// 运动状态上下文：游泳/飞行/骑乘等会缩放效果强度。
		private double strafeFactor = 1.0;
		private double forwardFactor = 1.0;
		private double verticalFactor = 1.0;
		private double horizontalSmoothing = 1.0;
		private double cameraSmoothing;

		public Offset update(Input input, Settings settings) {
			if (input.panoramic() || !settings.enabledFor(input.firstPerson())) {
				reset();
				return Offset.ZERO;
			}
			double dt = input.deltaSeconds();
			if (!Double.isFinite(dt)) {
				dt = 0.0;
			}
			dt = Math.clamp(dt, 0.0, 0.1);
			boolean perspectiveChanged = this.initialized
				&& input.firstPerson() != this.prevFirstPerson;
			if (!this.initialized || perspectiveChanged) {
				initialize(input);
			}

			boolean moved = squaredDistance(input.velocityX(), input.velocityY(),
				input.velocityZ(), this.prevVelocityX, this.prevVelocityY, this.prevVelocityZ) > 1.0e-8;
			boolean turned = Math.abs(unwrapStep(input.yRot() - this.prevYaw)) > 1.0e-4
				|| Math.abs(unwrapStep(input.xRot() - this.prevPitch)) > 1.0e-4;
			if (moved || turned) {
				this.lastActionTime = input.nowSeconds();
			}
			if (lastGlobalAction > this.lastActionTime) {
				this.lastActionTime = lastGlobalAction;
			}

			updateContext(input, dt);

			double pitchOffset = 0.0;
			double yawOffset = 0.0;
			double rollOffset = 0.0;

			// 鼠标平滑：把连续的鼠标位移做一阶低通，再折算成当前帧的偏移。
			if (settings.mouseSmoothing() && this.cameraSmoothing > MOUSE_SMOOTHING_THRESHOLD) {
				this.continuousYaw += unwrapStep(input.yRot() - this.prevYaw);
				this.continuousPitch += unwrapStep(input.xRot() - this.prevPitch);
				double step = 1.0 - Math.exp(-(BASE_MOUSE_SMOOTHING / this.cameraSmoothing) * dt);
				this.smoothedYaw += (this.continuousYaw - this.smoothedYaw) * step;
				this.smoothedPitch += (this.continuousPitch - this.smoothedPitch) * step;
				yawOffset += wrapDiff(this.smoothedYaw - input.yRot());
				pitchOffset += wrapDiff(this.smoothedPitch - input.xRot());
			} else {
				this.continuousYaw = input.yRot();
				this.continuousPitch = input.xRot();
				this.smoothedYaw = this.continuousYaw;
				this.smoothedPitch = this.continuousPitch;
			}

			// 静止晃动：玩家一段时间没有移动/转向后缓慢出现，一有输入立即淡出。
			if (settings.idleSway()) {
				if (input.nowSeconds() - this.lastActionTime < SWAY_FADE_IN_DELAY) {
					this.swayTarget = 0.0;
				} else if (this.swayFactor == this.swayTarget) {
					this.swayTarget = 1.0;
				}
				double fadeLength = this.swayTarget > 0 ? SWAY_FADE_IN_LENGTH : SWAY_FADE_OUT_LENGTH;
				double fadeStep = fadeLength > 0 ? dt / fadeLength : 1.0;
				this.swayFactor = stepTowards(this.swayFactor, this.swayTarget, fadeStep);
				double intensity = SWAY_INTENSITY * Math.pow(this.swayFactor, SWAY_FADING_SMOOTHNESS);
				float noiseX = (float) (input.nowSeconds() * SWAY_FREQUENCY);
				pitchOffset += SimplexNoise.noise(noiseX, 420.0f) * intensity;
				yawOffset += SimplexNoise.noise(noiseX, 1337.0f) * intensity;
			} else {
				this.swayFactor = 0.0;
				this.swayTarget = 0.0;
			}

			ForwardVelocity relative = forwardRelativeVelocity(
				input.velocityX(), input.velocityZ(), input.yRot());

			// 跳跃/下落：目标值带阈值，避免站在史莱姆方块上持续抽搐。
			if (settings.verticalPitch()) {
				double target = input.velocityY() * BASE_VERTICAL_PITCH_FACTOR * this.verticalFactor;
				if (Math.abs(target) < VERTICAL_PITCH_THRESHOLD) {
					target = 0.0;
				}
				this.prevVerticalPitch = damp(this.prevVerticalPitch, target,
					BASE_VERTICAL_PITCH_SMOOTHING * this.verticalFactor, dt);
				pitchOffset += this.prevVerticalPitch;
			} else {
				this.prevVerticalPitch = 0.0;
			}

			// 前后移动：向前低头、向后抬头。
			if (settings.forwardPitch()) {
				double target = relative.forward() * BASE_FORWARD_PITCH_FACTOR * this.forwardFactor;
				this.prevForwardPitch = damp(this.prevForwardPitch, target,
					BASE_FORWARD_PITCH_SMOOTHING * this.horizontalSmoothing, dt);
				pitchOffset += this.prevForwardPitch;
			} else {
				this.prevForwardPitch = 0.0;
			}

			// 转向侧倾：根据视角旋转速度累积，再平滑衰减。
			if (settings.turningRoll()) {
				double yawDelta = perspectiveChanged ? 0.0 : this.prevYaw - input.yRot();
				this.turningRollTarget = damp(this.turningRollTarget, 0.0,
					BASE_TURNING_ROLL_SMOOTHING, dt);
				this.turningRollTarget = Math.clamp(
					this.turningRollTarget + yawDelta * BASE_TURNING_ROLL_ACCUMULATION, -1.0, 1.0);
				rollOffset += clamp01(turningEasing(Math.abs(this.turningRollTarget)))
					* BASE_TURNING_ROLL_INTENSITY * Math.signum(this.turningRollTarget);
			} else {
				this.turningRollTarget = 0.0;
			}

			// 横移侧倾：向哪边平移，镜头就向哪边倾斜。
			if (settings.strafingRoll()) {
				double target = -relative.strafe() * BASE_STRAFING_ROLL_FACTOR * this.strafeFactor;
				this.prevStrafingRoll = damp(this.prevStrafingRoll, target,
					BASE_STRAFING_ROLL_SMOOTHING * this.horizontalSmoothing, dt);
				rollOffset += this.prevStrafingRoll;
			} else {
				this.prevStrafingRoll = 0.0;
			}

			if (settings.screenShake()) {
				double[] shake = sampleShakes(input.nowSeconds(),
					input.cameraX(), input.cameraY(), input.cameraZ());
				pitchOffset += shake[0];
				yawOffset += shake[1];
				rollOffset += shake[2];
			}

			this.prevYaw = input.yRot();
			this.prevPitch = input.xRot();
			this.prevVelocityX = input.velocityX();
			this.prevVelocityY = input.velocityY();
			this.prevVelocityZ = input.velocityZ();
			this.prevFirstPerson = input.firstPerson();
			this.initialized = true;

			double scale = settings.scale();
			float pitch = (float) pitchOffset;
			float yaw = (float) yawOffset;
			float roll = (float) rollOffset;
			if (!Float.isFinite(pitch) || !Float.isFinite(yaw) || !Float.isFinite(roll)) {
				reset();
				return Offset.ZERO;
			}
			return new Offset(pitch, yaw, roll).scaled(scale);
		}

		public void reset() {
			this.initialized = false;
			this.prevFirstPerson = false;
			this.prevYaw = 0.0f;
			this.prevPitch = 0.0f;
			this.prevVelocityX = 0.0;
			this.prevVelocityY = 0.0;
			this.prevVelocityZ = 0.0;
			this.lastActionTime = 0.0;
			this.turningRollTarget = 0.0;
			this.prevStrafingRoll = 0.0;
			this.prevForwardPitch = 0.0;
			this.prevVerticalPitch = 0.0;
			this.swayFactor = 0.0;
			this.swayTarget = 0.0;
			this.continuousYaw = 0.0;
			this.continuousPitch = 0.0;
			this.smoothedYaw = 0.0;
			this.smoothedPitch = 0.0;
			this.strafeFactor = 1.0;
			this.forwardFactor = 1.0;
			this.verticalFactor = 1.0;
			this.horizontalSmoothing = 1.0;
			this.cameraSmoothing = 0.0;
		}

		private void initialize(Input input) {
			this.prevYaw = input.yRot();
			this.prevPitch = input.xRot();
			this.prevVelocityX = input.velocityX();
			this.prevVelocityY = input.velocityY();
			this.prevVelocityZ = input.velocityZ();
			this.prevFirstPerson = input.firstPerson();
			this.lastActionTime = input.nowSeconds();
			this.continuousYaw = input.yRot();
			this.continuousPitch = input.xRot();
			this.smoothedYaw = this.continuousYaw;
			this.smoothedPitch = this.continuousPitch;
			this.turningRollTarget = 0.0;
			this.prevStrafingRoll = 0.0;
			this.prevForwardPitch = 0.0;
			this.prevVerticalPitch = 0.0;
			this.swayFactor = 0.0;
			this.swayTarget = 0.0;
			this.initialized = true;
		}

		private void updateContext(Input input, double dt) {
			double strafe = 1.0;
			double forward = 1.0;
			double vertical = 1.0;
			double smoothing = 0.0;
			if (input.ridingVehicle()) {
				strafe = 0.5;
				forward = 0.5;
				vertical = 2.0;
			} else if (input.ridingMount()) {
				strafe = 2.0;
				forward = 0.5;
				smoothing = 1.0;
			} else if (input.inWater()) {
				strafe = -3.0;
				forward = 3.0;
				vertical = 3.0;
				smoothing = 1.5;
			} else if (input.flying()) {
				strafe = -1.0;
				smoothing = 1.0;
			} else if (input.sprinting()) {
				strafe = 1.25;
				forward = 1.357;
				smoothing = 0.6;
			}
			double step = dt <= 0.0 ? 0.0 : 1.0 - Math.exp(-dt / 0.1);
			this.strafeFactor = lerp(this.strafeFactor, strafe, step);
			this.forwardFactor = lerp(this.forwardFactor, forward, step);
			this.verticalFactor = lerp(this.verticalFactor, vertical, step);
			this.cameraSmoothing = lerp(this.cameraSmoothing, smoothing, step);
		}
	}

	record ForwardVelocity(double strafe, double forward) {
	}

	private record Shake(double x, double y, double z, double radius, double trauma,
						 double frequency, double duration, double startTime) {
		boolean positionless() {
			return !Double.isFinite(this.x) || !Double.isFinite(this.y) || !Double.isFinite(this.z);
		}
	}

	/** 爆炸：按距离衰减的短促震动。 */
	public static void addExplosionShake(double x, double y, double z) {
		addShake(x, y, z, 32.0, 1.0, 1.0, 2.0);
	}

	/** 雷击：大范围、低频、较长的震动。 */
	public static void addThunderShake(double x, double y, double z) {
		addShake(x, y, z, 192.0, 0.05, 0.5, 7.0);
	}

	/** 挥手：无位置的轻微震动。 */
	public static void addHandSwingShake() {
		addShake(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY, 0.0, 0.03, 0.5, 0.5);
	}

	static void addShake(double x, double y, double z, double radius, double trauma,
						 double frequency, double duration) {
		addShake(x, y, z, radius, trauma, frequency, duration, nowSeconds());
	}

	static void addShake(double x, double y, double z, double radius, double trauma,
						 double frequency, double duration, double startTime) {
		if (SHAKES.size() >= MAX_SHAKES) {
			SHAKES.removeFirst();
		}
		SHAKES.add(new Shake(x, y, z, radius, trauma, frequency, duration, startTime));
	}

	// 与游戏帧时间同源的单调时钟：相机状态与震动记录共用。
	// 从 0 开始计时，避免 SimplexNoise 的 float 参数在长时间运行后丢失精度。
	private static final long CLOCK_ORIGIN_NANOS = System.nanoTime();

	public static double nowSeconds() {
		return (System.nanoTime() - CLOCK_ORIGIN_NANOS) / 1_000_000_000.0;
	}

	// 挥手等交互动作会立即打断静止晃动。
	public static void notifyPlayerAction() {
		lastGlobalAction = nowSeconds();
	}

	// 测试入口：清除跨帧的一次性动作。
	static void clearGlobalAction() {
		lastGlobalAction = Double.NEGATIVE_INFINITY;
	}

	// 测试入口：清空震动记录，避免测试之间互相干扰。
	static void clearShakes() {
		SHAKES.clear();
	}

	private static double[] sampleShakes(double time, double cameraX, double cameraY,
										 double cameraZ) {
		double total = 0.0;
		double pitch = 0.0;
		double yaw = 0.0;
		double roll = 0.0;
		float sampleBase = (float) (time * SCREEN_SHAKE_MAX_FREQUENCY);
		for (Shake shake : SHAKES) {
			double progress = shake.duration() > 0
				? Math.clamp((time - shake.startTime()) / shake.duration(), 0.0, 1.0)
				: 1.0;
			if (progress >= 1.0) {
				SHAKES.remove(shake);
				continue;
			}
			double decay = 1.0 - progress;
			double intensity = Math.clamp(shake.trauma(), 0.0, 1.0) * decay * decay;
			if (!shake.positionless()) {
				double distance = Math.sqrt(squaredDistance(cameraX, cameraY, cameraZ,
					shake.x(), shake.y(), shake.z()));
				double distanceFactor = 1.0 - Math.min(1.0, distance / shake.radius());
				intensity *= distanceFactor * distanceFactor;
			}
			if (intensity <= 0.0 || !Double.isFinite(intensity)) {
				continue;
			}
			float sampleStep = (float) (sampleBase * shake.frequency());
			pitch += SimplexNoise.noise(sampleStep, -69.0f) * intensity;
			yaw += SimplexNoise.noise(sampleStep, -420.0f) * intensity;
			roll += SimplexNoise.noise(sampleStep, -1337.0f) * intensity;
			total += intensity;
		}
		if (total > 1.0) {
			pitch /= total;
			yaw /= total;
			roll /= total;
		}
		return new double[] {pitch * SCREEN_SHAKE_MAX_INTENSITY,
			yaw * SCREEN_SHAKE_MAX_INTENSITY, roll * SCREEN_SHAKE_MAX_INTENSITY};
	}

	// 把速度从世界坐标旋转到相机的前后/左右坐标。
	static ForwardVelocity forwardRelativeVelocity(double velocityX, double velocityZ,
												   float yaw) {
		double radians = Math.toRadians(360.0 - yaw);
		double sin = Math.sin(radians);
		double cos = Math.cos(radians);
		return new ForwardVelocity(
			(cos * velocityX) - (sin * velocityZ),
			(sin * velocityX) + (cos * velocityZ));
	}

	static double damp(double source, double destination, double smoothing, double dt) {
		return lerp(source, destination, dampStep(smoothing, dt));
	}

	static double dampStep(double smoothing, double dt) {
		return 1.0 - Math.pow(smoothing * smoothing, Math.max(0.0, dt));
	}

	static double lerp(double source, double destination, double step) {
		return source + (destination - source) * Math.clamp(step, 0.0, 1.0);
	}

	static double stepTowards(double current, double target, double step) {
		if (current < target) {
			return Math.min(current + step, target);
		}
		if (current > target) {
			return Math.max(current - step, target);
		}
		return current;
	}

	static double unwrapStep(double degrees) {
		double value = degrees % 360.0;
		if (value <= -180.0) {
			value += 360.0;
		} else if (value > 180.0) {
			value -= 360.0;
		}
		return value;
	}

	static double wrapDiff(double degrees) {
		return unwrapStep(degrees);
	}

	static double clamp01(double value) {
		return Math.clamp(value, 0.0, 1.0);
	}

	private static double turningEasing(double x) {
		// easeInOutCubic
		return x < 0.5 ? (4.0 * x * x * x) : (1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0);
	}

	private static double squaredDistance(double x1, double y1, double z1,
										  double x2, double y2, double z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return dx * dx + dy * dy + dz * dz;
	}
}
