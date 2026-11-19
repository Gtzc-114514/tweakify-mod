package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.resources.Identifier;

/**
 * 收藏星样式：自绘星形 / 原版下界之星。
 *
 * <p>两套外观各自携带 5 张纹理（未收藏、已收藏、悬停两种、聚焦外框）；
 * 纹理在枚举构造时一次性构造好标识符，绘制路径不再拼接字符串。
 *
 * <p>纹理尺寸与资源 PNG 保持一致（星形 9x9、下界之星 13x13），
 * 绘制时按真实分辨率 1:1 居中画在 16x16 交互区域内；替换纹理后需同步这里，
 * 单元测试会读取资源并校验声明尺寸，漏改会直接失败。
 */
public enum FavoriteStarStyle implements CodeOptionEntry {
	STAR(TweakifyModConfig.FAVORITE_STAR_STYLE_STAR,
		"tweakify-mod.config.general.option.favoriteStarStyle.star", "star", 9, 9),
	NETHER_STAR(TweakifyModConfig.FAVORITE_STAR_STYLE_NETHER_STAR,
		"tweakify-mod.config.general.option.favoriteStarStyle.nether_star", "nether_star", 13, 13);

	// 收藏星在两种状态（收藏/未收藏）与两种悬停状态下的外观。
	public enum State {
		EMPTY("empty"),
		FAVORITE("favorite"),
		HOVERED_EMPTY("hovered_empty"),
		HOVERED_FAVORITE("hovered_favorite"),
		FOCUSED_OUTLINE("focused_outline");

		private final String fileName;

		State(String fileName) {
			this.fileName = fileName;
		}
	}

	private static final String TEXTURE_ROOT = "textures/gui/sprites/favorite/";

	private final String code;
	private final String translationKey;
	private final int textureWidth;
	private final int textureHeight;
	private final Identifier empty;
	private final Identifier favorite;
	private final Identifier hoveredEmpty;
	private final Identifier hoveredFavorite;
	private final Identifier focusedOutline;

	FavoriteStarStyle(String code, String translationKey, String directory,
					  int textureWidth, int textureHeight) {
		this.code = code;
		this.translationKey = translationKey;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		String path = TEXTURE_ROOT + directory + "/";
		this.empty = texture(path, State.EMPTY);
		this.favorite = texture(path, State.FAVORITE);
		this.hoveredEmpty = texture(path, State.HOVERED_EMPTY);
		this.hoveredFavorite = texture(path, State.HOVERED_FAVORITE);
		this.focusedOutline = texture(path, State.FOCUSED_OUTLINE);
	}

	private static Identifier texture(String directory, State state) {
		return Identifier.fromNamespaceAndPath("tweakify-mod", directory + state.fileName + ".png");
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	// 纹理像素尺寸（与资源 PNG 一致，测试会校验）。
	public int textureWidth() {
		return this.textureWidth;
	}

	public int textureHeight() {
		return this.textureHeight;
	}

	public Identifier texture(State state) {
		return switch (state) {
			case EMPTY -> this.empty;
			case FAVORITE -> this.favorite;
			case HOVERED_EMPTY -> this.hoveredEmpty;
			case HOVERED_FAVORITE -> this.hoveredFavorite;
			case FOCUSED_OUTLINE -> this.focusedOutline;
		};
	}

	public static FavoriteStarStyle fromCode(String code) {
		for (FavoriteStarStyle style : values()) {
			if (style.code.equals(code)) {
				return style;
			}
		}
		return STAR;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
