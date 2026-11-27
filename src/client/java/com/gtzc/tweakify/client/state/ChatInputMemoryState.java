package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 聊天框内容记忆：记住上次关闭聊天框时输入框里的内容，下次打开时恢复。
 *
 * <p>发送消息（Enter）后原版会关闭聊天框，此时输入框仍是刚发送的文本；
 * 这里用 {@link #markSubmitted(String)} 记下发送内容，关闭时识别并清空，
 * 不会把已经发出去的消息重新填回聊天框。
 */
public final class ChatInputMemoryState {
	private static String saved = "";
	private static String submittedText;
	private static boolean sessionActive;

	private ChatInputMemoryState() {
	}

	// 上次关闭聊天框时保留的文本。
	public static String savedText() {
		return saved;
	}

	// 保存本次关闭时的输入内容；纯空白与原版一样视为没有草稿。
	public static void remember(String text) {
		remember(text, TweakifyModConfig.DEFAULT_CHAT_INPUT_MEMORY_LENGTH);
	}

	// 带长度上限的版本：超过上限时截断，避免恢复一份超出输入框容量的草稿。
	public static void remember(String text, int maxLength) {
		if (text == null || text.isBlank()) {
			saved = "";
			return;
		}
		int limit = Math.max(TweakifyModConfig.MIN_CHAT_INPUT_MEMORY_LENGTH,
			Math.min(TweakifyModConfig.MAX_CHAT_INPUT_MEMORY_LENGTH, maxLength));
		saved = text.length() <= limit ? text : text.substring(0, limit);
	}

	// 发送消息：记下发送内容，关闭聊天框时据此判断是否清空记忆。
	public static void markSubmitted(String text) {
		submittedText = text == null ? "" : text;
	}

	// 当前输入框内容是否正是刚发送的文本：是则本次关闭不保留。
	public static boolean consumeSubmitted(String current) {
		boolean submitted = submittedText != null && submittedText.equals(current);
		submittedText = null;
		return submitted;
	}

	// 会话开始：断线重连/切换世界后从空草稿开始。
	public static void beginSession() {
		sessionActive = true;
		saved = "";
		submittedText = null;
	}

	// 会话结束：丢弃草稿与提交标记，避免泄漏到下一个世界/服务器。
	public static void endSession() {
		sessionActive = false;
		saved = "";
		submittedText = null;
	}

	public static boolean isSessionActive() {
		return sessionActive;
	}

	// 预填内容（如按 / 打开聊天框）未编辑时不应覆盖已有草稿。
	public static boolean shouldRemember(String current, String prefill) {
		return prefill == null || prefill.isBlank() || !prefill.equals(current);
	}

	// 命令草稿：以 / 开头（忽略前导空白）的输入。
	public static boolean isCommandDraft(String text) {
		return text != null && text.stripLeading().startsWith("/");
	}

	// 是否保留为草稿：开启“不记忆命令”时丢弃命令草稿，避免下次打开时误执行。
	public static boolean shouldKeepDraft(String text, boolean ignoreCommands) {
		return !(ignoreCommands && isCommandDraft(text));
	}

	// 清空待匹配的发送标记（输入变化、新建界面等使标记失效的场景）。
	public static void clearSubmitted() {
		submittedText = null;
	}

	// 清空记忆与发送标记。
	public static void reset() {
		saved = "";
		submittedText = null;
	}

	// 包私有：供测试在用例之间清理静态状态。
	static void resetForTests() {
		reset();
		sessionActive = false;
	}
}
