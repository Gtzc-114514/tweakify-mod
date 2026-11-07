package com.gtzc.tweakify.client;

/**
 * 按住键的会话状态机：按下瞬间取一个初始值、松开复位、按住期间保持。
 *
 * <p>挖掘冷却（按下即复位）、使用冷却（按下/松开复位）、副手回退（按下时采样主手是否有物品）
 * 都是同一条转移规则的不同参数，集中在这里实现，避免三处各写一份并各自漂移。
 */
final class KeyHoldSession {
	private boolean lastDown;
	private boolean active;

	// 每 tick 调用一次：按下瞬间用 valueOnPress 覆盖，松开清空，按住期间保持上一次的值。
	boolean update(boolean down, boolean valueOnPress) {
		this.active = next(down, this.lastDown, valueOnPress, this.active);
		this.lastDown = down;
		return this.active;
	}

	// 纯转移规则：便于单元测试覆盖按下/松开/保持三种边界。
	static boolean next(boolean down, boolean lastDown, boolean valueOnPress, boolean current) {
		if (down && !lastDown) {
			return valueOnPress;
		}
		if (!down) {
			return false;
		}
		return current;
	}

	// 会话内由外部事件置位（如“方块已被挖掉”“该手已经使用过”）。
	void mark() {
		this.active = true;
	}

	boolean isActive() {
		return this.active;
	}

	void reset() {
		this.lastDown = false;
		this.active = false;
	}
}
