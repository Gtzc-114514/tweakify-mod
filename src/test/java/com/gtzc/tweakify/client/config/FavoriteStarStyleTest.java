package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 收藏星样式与几何：代码往返、纹理路径完整、存档行星标不压住内容。
 * 配置行的按钮/滚动条布局断言见 ui 包的 TweakifyModConfigScreenTest。
 */
class FavoriteStarStyleTest {

	@Test
	void codesRoundTripAndUnknownFallsBackToStar() {
		for (FavoriteStarStyle style : FavoriteStarStyle.values()) {
			assertEquals(style, FavoriteStarStyle.fromCode(style.code()));
			assertTrue(style.translationKey()
					.startsWith("tweakify-mod.config.general.option.favoriteStarStyle."),
				"unexpected translation key: " + style.translationKey());
		}
		assertEquals(TweakifyModConfig.FAVORITE_STAR_STYLE_STAR, FavoriteStarStyle.STAR.code());
		assertEquals(TweakifyModConfig.FAVORITE_STAR_STYLE_NETHER_STAR,
			FavoriteStarStyle.NETHER_STAR.code());
		assertEquals(FavoriteStarStyle.STAR, FavoriteStarStyle.fromCode("not-a-style"));
		assertEquals(FavoriteStarStyle.STAR, FavoriteStarStyle.fromCode(null));
	}

	@Test
	void everyStateHasADistinctTexture() {
		for (FavoriteStarStyle style : FavoriteStarStyle.values()) {
			Set<Identifier> textures = new HashSet<>();
			for (FavoriteStarStyle.State state : FavoriteStarStyle.State.values()) {
				Identifier texture = style.texture(state);
				assertNotNull(texture);
				String path = texture.toString();
				assertTrue(path.contains("textures/gui/sprites/favorite/"),
					"unexpected texture path: " + path);
				assertTrue(path.endsWith(".png"), "texture must be a png: " + path);
				assertTrue(textures.add(texture), "duplicate texture for " + style + "/" + state);
			}
			assertEquals(FavoriteStarStyle.State.values().length, textures.size());
		}
	}

	// 每套样式的 5 张资源真实尺寸必须与枚举声明一致（替换纹理后漏改会在这里失败）。
	@Test
	void textureFilesMatchDeclaredSize() throws IOException {
		for (FavoriteStarStyle style : FavoriteStarStyle.values()) {
			for (FavoriteStarStyle.State state : FavoriteStarStyle.State.values()) {
				Identifier texture = style.texture(state);
				String resource = toResourcePath(texture);
				try (InputStream stream = FavoriteStarStyleTest.class.getResourceAsStream(resource)) {
					assertNotNull(stream, "missing texture resource: " + resource);
					BufferedImage image = ImageIO.read(stream);
					assertNotNull(image, "unreadable texture: " + resource);
					assertEquals(style.textureWidth(), image.getWidth(),
						"texture width mismatch for " + resource);
					assertEquals(style.textureHeight(), image.getHeight(),
						"texture height mismatch for " + resource);
				}
			}
		}
	}

	private static String toResourcePath(Identifier texture) {
		String text = texture.toString();
		int colon = text.indexOf(':');
		assertTrue(colon > 0, "unexpected texture identifier: " + text);
		return "/assets/" + text.substring(0, colon) + "/" + text.substring(colon + 1);
	}

	// 存档行：内容从 rowLeft + 2 开始、星标 16px，右边界不能越过内容起点。
	@Test
	void worldStarSitsLeftOfTheRowContent() {
		int rowLeft = 40;
		int starX = FavoriteStarRenderer.worldStarX(rowLeft);
		assertTrue(starX >= rowLeft - 20, "star must stay near the row left edge");
		assertTrue(starX + FavoriteStarRenderer.SIZE <= rowLeft + 2,
			"star must not overlap the 32x32 icon or text");
		int contentY = 100;
		assertEquals(contentY + 8, FavoriteStarRenderer.worldStarY(contentY));
		assertTrue(FavoriteStarRenderer.isOver(starX, contentY + 8, starX, contentY + 8));
		assertFalse(FavoriteStarRenderer.isOver(
			starX + FavoriteStarRenderer.SIZE, contentY + 8, starX, contentY + 8));
	}

	// 配置行：星标贴在重置/打开按钮右边，右边界不能越过行尾与滚动条。
	// 该布局断言已移到 ui 包的 TweakifyModConfigScreenTest（同包访问控件预留常量）。
}
