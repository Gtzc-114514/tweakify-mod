package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;

/**
 * 挖掘冷却状态机：一次左键只挖一个方块。
 * 按住左键期间一旦挖掉一个方块，后续 tick 的 continueAttack 被拦截，
 * 直到松开左键再重新按下才会允许挖下一个方块。
 *
 * <p>标记字段是 private，外部只能通过 {@link #shouldBlockMining()} /
 * {@link #shouldBlockAttack()} 查询。查询处同时校验开关与 tick 序号，
 * 因此不依赖「功能关闭时 tick 一定会复位」这种隐式约定。
 */
public final class MiningCooldownState {
	private static final KeyHoldSession SESSION = new KeyHoldSession();

	// 只在类内更新；读取必须走 shouldBlockMining / shouldBlockAttack。
	private static boolean blockMiningThisTick = false;

	// 写入时所属的 tick 序号：与 TickClock 不一致时标记自动失效。
	private static long markedTick = Long.MIN_VALUE;

	private MiningCooldownState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.miningCooldown || client.player == null) {
			reset();
			return;
		}
		updateFromInput(client.options.keyAttack.isDown());
	}

	// 包私有：真实 tick 与测试共用同一条状态更新路径，只把 Minecraft 输入采集留在 tick 外层。
	// 配置判定不在这里做，shouldBlockXxx 仍会重新校验开关，测试到的就是真实行为。
	static void updateFromInput(boolean attackDown) {
		boolean minedThisSession = SESSION.update(attackDown, false);
		blockMiningThisTick = computeBlockMiningThisTick(attackDown, minedThisSession);
		markedTick = TickClock.current();
	}

	// 包私有：测试之间重置状态，避免共享静态字段互相影响。
	static void resetForTest() {
		reset();
	}

	// 挖掘拦截：总开关关闭或标记不属于当前 tick 时不拦截。
	public static boolean shouldBlockMining() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.miningCooldown && isMarkedThisTick();
	}

	// 攻击拦截：除总开关外还需要 miningCooldownAttacks。
	public static boolean shouldBlockAttack() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.miningCooldown && config.miningCooldownAttacks
			&& isMarkedThisTick();
	}

	// 标记只在写入它的那个 tick 内有效：状态机 tick 没执行时序号对不上，自动失效。
	private static boolean isMarkedThisTick() {
		return blockMiningThisTick && markedTick == TickClock.current();
	}

	// 方块被挖掉时由 MultiPlayerGameModeMixin 回调置位。
	public static void markBlockMined() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.miningCooldown) {
			return;
		}
		SESSION.mark();
	}

	// 攻击实体后同样标记：miningCooldownAttacks 开启时一次按住只打一下。
	public static void markEntityAttacked() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.miningCooldown || !config.miningCooldownAttacks) {
			return;
		}
		SESSION.mark();
	}

	private static void reset() {
		SESSION.reset();
		blockMiningThisTick = false;
		markedTick = Long.MIN_VALUE;
	}

	// 纯拦截判定：按住期间已经挖掉过方块 -> 拦截后续挖掘。
	static boolean computeBlockMiningThisTick(boolean attackDown, boolean minedThisSession) {
		return attackDown && minedThisSession;
	}
}
