package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 创造栏自绘底纹的贴图约束：18x18、完全不透明，且按原版容器纹理的槽位像素分布
 * （左上 #373737、右下 #FFFFFF、内部 #8B8B8B）。一旦换成半透明或深色贴图，
 * 分组展开 / 平滑滚动时就会出现“整片格子变灰”或原版边框透出的问题。
 */
class CreativeGridSpriteTest {
	private static final String SPRITE = "/assets/tweakify-mod/textures/gui/sprites/container/grid_slot.png";

	@Test
	void gridSlotIsOpaqueVanillaLookCell() throws Exception {
		try (InputStream in = CreativeGridSpriteTest.class.getResourceAsStream(SPRITE)) {
			assertNotNull(in, "缺少贴图 " + SPRITE);
			BufferedImage image = ImageIO.read(in);
			assertEquals(18, image.getWidth());
			assertEquals(18, image.getHeight());
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					assertEquals(0xFF, image.getRGB(x, y) >>> 24,
						"贴图必须不透明，否则会透出原版烘焙底纹: (" + x + "," + y + ")");
				}
			}
			assertEquals(0xFF373737, image.getRGB(0, 0));
			assertEquals(0xFFFFFFFF, image.getRGB(17, 17));
			assertEquals(0xFF8B8B8B, image.getRGB(9, 9));
			// 原版槽位的两个“内侧角”不是边框色，保持内部色才和烘焙底纹无缝衔接。
			assertEquals(0xFF8B8B8B, image.getRGB(17, 0));
			assertEquals(0xFF8B8B8B, image.getRGB(0, 17));
		}
	}
}
