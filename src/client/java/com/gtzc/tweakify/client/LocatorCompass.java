package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 定位栏指南针，逐行对齐 {@code Haage001/locator-heads} 的 {@code LocatorHeadsClient}：
 * 可视范围左右各 60 度、横向铺满 91 像素半宽、纵向固定 {@code screenHeight - 31}，
 * 字母左移 2 像素、透明度随偏离中心线性衰减到一半，可选四向 1 像素黑色描边与坐标标记
 * （{@code +Z/-X/-Z/+X}）。
 *
 * <p>绘制时机与参考实现一致：挂在整个 HUD 的最后（参考用 {@code HudElementRegistry.addLast}），
 * 因此字母在条带底图、圆点与头像之上——没有任何条带背景能盖住它。
 * 唯一附加的偏离是：指南针开启时把经验等级数字整体上移（见 {@link #LEVEL_TEXT_SHIFT}），
 * 避免数字与居中的字母重叠；关闭指南针时原版位置零变化。
 */
public final class LocatorCompass {
	// 与参考实现一致的几何常量。
	static final double HALF_FOV_DEGREES = 60.0;
	static final int HALF_WIDTH = 91;
	static final int COMPASS_Y_FROM_BOTTOM = 31;
	// 指南针开启时把原版经验等级数字上移这么多像素。
	// 原版数字（四向 1 像素描边）的绘制带是 [H-L-27, H-25)，字母墨迹从 H-30 开始；
	// 上移 6 后数字底边落在 H-31（字母字框顶），与字母墨迹留 1 像素空隙，且与字号无关。
	public static final int LEVEL_TEXT_SHIFT = 6;
	private static final int LABEL_X_OFFSET = -2;
	private static final String[] CARDINALS = {"S", "W", "N", "E"};
	private static final String[] COORDINATES = {"+Z", "-X", "-Z", "+X"};
	// Flashback 回放检测：与参考实现一样用反射，未安装时视为否；只查找一次。
	private static final String FLASHBACK_CLASS = "com.moulberry.flashback.Flashback";
	private static Method flashbackReplayMethod;
	private static boolean flashbackLookupDone;

	private LocatorCompass() {
	}

	// Fabric HUD 元素入口：在 TweakifyModClient 里用 HudElementRegistry.addLast 注册，
	// 保证排在原版与其它模组的全部 HUD 元素之后（与参考实现同一种注册方式）。
	public static void renderElement(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		render(graphics, Minecraft.getInstance());
	}

	private static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.locatorBarCompass) {
			return;
		}
		// F1 隐藏 HUD，以及非聊天/容器的界面（ESC 菜单等）都不画。
		if (client.gui == null || client.gui.hud.isHidden() || hidesCompass(client.gui.screen())) {
			return;
		}
		Entity camera = client.getCameraEntity();
		if (camera == null || client.level == null) {
			return;
		}
		// 与参考实现一致：Flashback 回放时不画。
		if (isInFlashbackReplay()) {
			return;
		}
		float yaw = camera.getYRot();
		int centerX = graphics.guiWidth() / 2;
		int y = graphics.guiHeight() - COMPASS_Y_FROM_BOTTOM;
		String[] labels = config.functions.locatorBarCompassCoordinates ? COORDINATES : CARDINALS;
		int rgb = LocatorHeadColor.rgbOf(config.functions.locatorBarCompassColor);
		boolean shadow = config.functions.locatorBarCompassShadow;
		for (int index = 0; index < labels.length; index++) {
			double angleDiff = angleDifference(yaw, index * 90.0);
			Integer x = labelX(centerX, angleDiff);
			if (x == null) {
				continue;
			}
			drawLabel(graphics, client, labels[index], x + LABEL_X_OFFSET, y,
				labelAlpha(angleDiff), rgb, shadow);
		}
	}

	// 与参考实现一致：回放时（Flashback）不画指南针；未安装时返回 false。
	static boolean isInFlashbackReplay() {
		if (!flashbackLookupDone) {
			flashbackLookupDone = true;
			try {
				Method method = Class.forName(FLASHBACK_CLASS).getMethod("isInReplay");
				if (Modifier.isStatic(method.getModifiers())) {
					flashbackReplayMethod = method;
				}
			} catch (ReflectiveOperationException | LinkageError ignored) {
				flashbackReplayMethod = null;
			}
		}
		if (flashbackReplayMethod == null) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(flashbackReplayMethod.invoke(null));
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	// 聊天界面与容器界面仍显示指南针（与参考实现一致），其它界面隐藏。
	static boolean hidesCompass(Screen screen) {
		return screen != null
			&& !(screen instanceof ChatScreen)
			&& !(screen instanceof AbstractContainerScreen);
	}

	// 上移后的经验等级数字行顶（供测试断言两条像素带不相交）。
	static int levelTextTop(int guiHeight, int lineHeight) {
		return guiHeight - lineHeight - 26 - LEVEL_TEXT_SHIFT;
	}

	// 方位角与镜头偏航的差值，归一到 (-180, 180]。
	static double angleDifference(double yaw, double directionDegrees) {
		double normalizedYaw = ((yaw % 360.0) + 360.0) % 360.0;
		double diff = directionDegrees - normalizedYaw;
		while (diff > 180.0) {
			diff -= 360.0;
		}
		while (diff < -180.0) {
			diff += 360.0;
		}
		return diff;
	}

	// 超出可视范围返回 null；否则给出该方位字母的水平坐标。
	static Integer labelX(int centerX, double angleDiff) {
		if (Math.abs(angleDiff) > HALF_FOV_DEGREES) {
			return null;
		}
		return centerX + (int) ((angleDiff / HALF_FOV_DEGREES) * HALF_WIDTH);
	}

	// 越靠边缘越淡：中心 255，边界 128（与参考实现同一条线性曲线）。
	static int labelAlpha(double angleDiff) {
		double distance = Math.min(1.0, Math.abs(angleDiff) / HALF_FOV_DEGREES);
		return (int) ((1.0 - distance * 0.5) * 255.0);
	}

	private static void drawLabel(GuiGraphicsExtractor graphics, Minecraft client, String label,
								  int x, int y, int alpha, int rgb, boolean shadow) {
		if (alpha <= 0) {
			return;
		}
		if (shadow) {
			// 四向偏移的黑字描边，避免在亮色天空上看不清。
			int shadowColor = alpha << 24;
			graphics.text(client.font, label, x - 1, y, shadowColor, false);
			graphics.text(client.font, label, x + 1, y, shadowColor, false);
			graphics.text(client.font, label, x, y - 1, shadowColor, false);
			graphics.text(client.font, label, x, y + 1, shadowColor, false);
		}
		graphics.text(client.font, label, x, y, (alpha << 24) | (rgb & 0xFFFFFF), false);
	}
}
