package com.gtzc.tweakify.client;

/**
 * 客户端 tick 序号：状态机在自己的 tick 里记录写入时的序号，读取方校验序号是否仍是当前 tick。
 *
 * <p>这样「跨 tick 的残留标记」在结构上不可能生效，不需要依赖
 * 「功能关闭时 tick 一定会复位」这种隐式约定：只要状态机 tick 没被执行
 * （异常、顺序变化、提前 return），序号就对不上。
 */
final class TickClock {
	private static long current;

	private TickClock() {
	}

	static void advance() {
		current++;
	}

	static long current() {
		return current;
	}
}
