package com.gtzc.tweakify.client;

/**
 * 通用平滑滚动状态：滚轮与拖动输入只累加到目标位置，每帧按指数缓动把当前位置推向目标
 * （思路与 Minecraft-Smooth-Scrolling 一致：输入累积 + 帧间收敛，而不是一次跳变）。
 *
 * <p>位置与目标的单位由调用方决定（配置列表用像素，创造栏用滚动比例，聊天栏用像素），
 * 都夹在 {@code [0, max]} 内：列表变短或过滤后内容变少时不会停留在越界位置。
 * 缓动按时间推进，帧率变化不影响收敛速度。
 */
public final class SmoothScrollAnimator {
	// 指数缓动的时间常数（秒）：约 0.06 秒收敛九成，接近平滑滚动的默认手感。
	static final double TIME_CONSTANT_SECONDS = 0.06;

	// 与目标的差距小于该值时直接吸附，避免永远逼近却停不下来的抖动（单位同位置）。
	// 单位不同的调用方要自己换算：以“行”计时 0.5 会一口气跳掉半行。
	private static final double DEFAULT_SNAP_DISTANCE = 0.5;

	private double position;
	private double target;

	// 收敛速度：越小越快；由配置项「平滑时长」换算，调用方在推进前设置。
	private double timeConstantSeconds = TIME_CONSTANT_SECONDS;

	// 吸附阈值：调用方可按自己的位置单位设置（创造栏按半像素换算）。
	private double snapDistance = DEFAULT_SNAP_DISTANCE;

	public void setTimeConstant(double seconds) {
		this.timeConstantSeconds = Math.max(0.005, seconds);
	}

	public void setSnapDistance(double distance) {
		this.snapDistance = Math.max(0.0, distance);
	}

	public double position() {
		return this.position;
	}

	public double target() {
		return this.target;
	}

	public boolean isSettled() {
		return this.position == this.target;
	}

	// 追加一段滚动位移（滚轮或拖动）；max 随内容变化，这里按当前上限夹紧。
	public void addDelta(double delta, double max) {
		this.target = clamp(this.target + delta, max);
		this.position = clamp(this.position, max);
	}

	// 外部直接指定位置（滚动条拖动、滚动记忆恢复、键盘翻页）时立即对齐，不做动画。
	public void jumpTo(double value, double max) {
		this.position = clamp(value, max);
		this.target = this.position;
	}

	// 每帧推进：指数缓动，与帧率无关。
	public void update(double deltaSeconds, double max) {
		this.target = clamp(this.target, max);
		this.position = clamp(this.position, max);
		double diff = this.target - this.position;
		if (Math.abs(diff) <= this.snapDistance) {
			this.position = this.target;
			return;
		}
		double factor = 1.0 - Math.exp(-Math.max(0.0, deltaSeconds) / this.timeConstantSeconds);
		this.position += diff * factor;
	}

	// 配置列表专用：把像素位置换算成“起始行 + 亚行像素偏移”。
	public Layout layout(int entryHeight) {
		int height = Math.max(1, entryHeight);
		int row = (int) Math.floor(this.position / height);
		int offset = (int) Math.round(this.position - (double) row * height);
		// 四舍五入可能把偏移推到整行：进位成下一行的零偏移。
		if (offset >= height) {
			row++;
			offset = 0;
		}
		return new Layout(Math.max(0, row), Math.max(0, offset));
	}

	private static double clamp(double value, double max) {
		return Math.max(0.0, Math.min(value, Math.max(0.0, max)));
	}

	public record Layout(int row, int pixelOffset) {
	}
}
