package com.gtzc.tweakify.util;

import com.gtzc.tweakify.TweakifyMod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * require = 0 注入点的探针：钩子第一次真正执行时记一条日志。
 *
 * <p>这类注入为了不拖垮启动放弃了“注入失败即崩溃”，代价是被其他模组改写目标方法时
 * 功能会静默失效。第一次执行时打一条 INFO，至少能确认钩子活着；
 * 功能不生效时对照日志里有没有对应条目，就能区分“没注入”和“注入了但逻辑不生效”。
 *
 * <p>只在低频路径（打开界面、中键、加载界面 tick）调用，避免热路径上的哈希开销。
 * 高频路径（如每个掉落物每 tick 的 tick 头部）改用 {@link #markOnce(AtomicBoolean, String)}：
 * 标记后只剩一次 volatile 读，不再参与集合哈希。
 */
public final class HookProbe {
	private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

	private HookProbe() {
	}

	public static void markOnce(String hook) {
		if (REPORTED.add(hook)) {
			TweakifyMod.LOGGER.info("Mixin hook active: {}", hook);
		}
	}

	// 高频路径专用：调用方持有一个 AtomicBoolean，首次成功后只读一次 volatile。
	public static void markOnce(AtomicBoolean reported, String hook) {
		if (!reported.get() && reported.compareAndSet(false, true)) {
			TweakifyMod.LOGGER.info("Mixin hook active: {}", hook);
		}
	}
}
