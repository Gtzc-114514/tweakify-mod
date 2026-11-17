package com.gtzc.tweakify.client;

/**
 * 书本界面垂直居中（MC-61489）的纯函数。
 *
 * <p>原版把书本背景固定在顶部 y=2，窗口越高越偏上；这里按剩余高度给出
 * 统一的下移量，供背景、编辑框与按钮共用。除以 3 而不是 2，是为了给底部
 * 的“完成/署名”按钮留出空间，窗口化时按钮不会贴到屏幕底边。
 */
public final class BookScreenLayout {
	private BookScreenLayout() {
	}

	// 关闭时返回 0，调用方保持原版坐标；开启后返回需要整体下移的像素数。
	public static int verticalOffset(boolean enabled, int screenHeight, int imageHeight) {
		if (!enabled) {
			return 0;
		}
		return (screenHeight - imageHeight) / 3;
	}
}
