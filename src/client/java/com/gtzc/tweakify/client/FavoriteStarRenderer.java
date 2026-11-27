package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 收藏星的统一绘制与几何计算：配置行右侧与存档行左侧共用同一套外观。
 *
 * <p>每套样式的纹理按真实分辨率 1:1 居中绘制在 16x16 交互区域内（尺寸取自
 * {@link FavoriteStarStyle}），不缩放像素画；鼠标悬停时叠加聚焦外框。
 */
public final class FavoriteStarRenderer {
	// 与交互区域一致：点击命中区域固定 16x16，纹理可以更小。
	public static final int SIZE = 16;

	// 存档行图标 32x32，星标贴在图标左侧并垂直居中。
	private static final int WORLD_ICON_SIZE = 32;
	private static final int WORLD_STAR_GAP = 2;

	// 配置行星标与右侧按钮（重置/打开）之间的固定间隙。
	private static final int ROW_STAR_GAP = 2;

	private FavoriteStarRenderer() {
	}

	// 收藏系统总开关：配置尚未加载时按开启处理。
	public static boolean isEnabled() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.general.favoriteSystem;
	}

	// 当前样式：配置尚未加载时按默认（自绘星形）处理。
	public static FavoriteStarStyle selectedStyle() {
		TweakifyModConfig config = TweakifyMod.config();
		return FavoriteStarStyle.fromCode(config.general.favoriteStarStyle);
	}

	public static void draw(GuiGraphicsExtractor gui, int x, int y, boolean favorite, boolean hovered) {
		if (!isEnabled()) {
			return;
		}
		drawStar(gui, x, y, favorite, hovered);
	}

	// 只按当前收藏星样式与状态绘制，不检查收藏系统总开关。
	// 进度界面的固定按钮即使关闭收藏系统也应继续可用，但仍跟随所选外观。
	public static void drawStar(GuiGraphicsExtractor gui, int x, int y, boolean favorite, boolean hovered) {
		FavoriteStarStyle style = selectedStyle();
		FavoriteStarStyle.State state;
		if (favorite) {
			state = hovered ? FavoriteStarStyle.State.HOVERED_FAVORITE : FavoriteStarStyle.State.FAVORITE;
		} else {
			state = hovered ? FavoriteStarStyle.State.HOVERED_EMPTY : FavoriteStarStyle.State.EMPTY;
		}
		blit(gui, style, style.texture(state), x, y);
		if (hovered) {
			// 聚焦外框叠加在基础星标上：不改变星标本体，只补一圈亮边。
			blit(gui, style, style.texture(FavoriteStarStyle.State.FOCUSED_OUTLINE), x, y);
		}
	}

	// 按纹理真实分辨率 1:1 绘制，并居中在 16x16 交互区域内。
	private static void blit(GuiGraphicsExtractor gui, FavoriteStarStyle style,
							 Identifier texture, int x, int y) {
		int width = style.textureWidth();
		int height = style.textureHeight();
		int drawX = x + (SIZE - width) / 2;
		int drawY = y + (SIZE - height) / 2;
		gui.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0.0f, 0.0f,
			width, height, width, height);
	}

	// 存档行星标：贴在世界图标（contentX 起 32 宽）左侧，右边界不越过 contentX。
	public static int worldStarX(int rowLeft) {
		return rowLeft - SIZE - WORLD_STAR_GAP;
	}

	public static int worldStarY(int contentY) {
		return contentY + (WORLD_ICON_SIZE - SIZE) / 2;
	}

	// 配置行星标：贴在控件行最右侧按钮（重置/打开）右边，而不是行尾。
	static int rowStarX(int controlsRight) {
		return controlsRight + ROW_STAR_GAP;
	}

	// 星标右侧留白内放不下时返回 -1（不显示）：
	// 不再“夹到重置按钮上方”，避免视觉重叠并防止星标点击被重置按钮优先抢走。
	static int starXIfFits(int controlsRight, int rowX, int rowWidth) {
		int starX = rowStarX(controlsRight);
		int max = rowX + rowWidth - SIZE - ROW_STAR_GAP;
		return starX <= max ? starX : -1;
	}

	static int rowStarY(int rowY, int rowHeight) {
		return rowY + (rowHeight - SIZE) / 2;
	}

	public static boolean isOver(int mouseX, int mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
	}
}
