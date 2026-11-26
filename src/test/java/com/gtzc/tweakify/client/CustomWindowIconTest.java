package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义窗口图标的纯逻辑：路径解析与 16 ~ 64 像素 PNG 校验。
 */
class CustomWindowIconTest {
	private static final Path GAME_DIR = Path.of("game-directory").toAbsolutePath();

	@Test
	void resolvesAbsoluteAndRelativePaths() {
		Path absolute = Path.of("icons").toAbsolutePath().resolve("icon.png").normalize();
		assertEquals(absolute, CustomWindowIcon.resolvePath(GAME_DIR, absolute.toString()));
		assertEquals(GAME_DIR.resolve("config/icon.png").normalize(),
			CustomWindowIcon.resolvePath(GAME_DIR, "  config/icon.png  "));
		assertNull(CustomWindowIcon.resolvePath(GAME_DIR, "   "));
		assertNull(CustomWindowIcon.resolvePath(GAME_DIR, null));
		assertNull(CustomWindowIcon.resolvePath(null, "icon.png"));
	}

	@Test
	void pngSizeReadsIhdrHeader() {
		assertArrayEquals(new int[] {32, 48}, CustomWindowIcon.pngSize(pngHeader(32, 48)));
		assertArrayEquals(new int[] {16, 16}, CustomWindowIcon.pngSize(pngHeader(16, 16)));
		assertNull(CustomWindowIcon.pngSize(null));
		assertNull(CustomWindowIcon.pngSize(new byte[0]));
		assertNull(CustomWindowIcon.pngSize("not a png".getBytes(StandardCharsets.US_ASCII)),
			"非 PNG 数据应被拒绝");
		assertNull(CustomWindowIcon.pngSize(new byte[23]), "数据不足 IHDR 时应被拒绝");
	}

	@Test
	void supportedSizeIs16To64() {
		assertTrue(CustomWindowIcon.isSupportedSize(16, 16));
		assertTrue(CustomWindowIcon.isSupportedSize(32, 32));
		assertTrue(CustomWindowIcon.isSupportedSize(48, 48));
		assertTrue(CustomWindowIcon.isSupportedSize(64, 64));
		assertTrue(CustomWindowIcon.isSupportedSize(16, 64), "宽高可独立取值");
		assertFalse(CustomWindowIcon.isSupportedSize(15, 32));
		assertFalse(CustomWindowIcon.isSupportedSize(32, 65));
		assertFalse(CustomWindowIcon.isSupportedSize(0, 32));
	}

	// 只构造签名 + IHDR 头；尺寸校验不需要后续像素数据。
	private static byte[] pngHeader(int width, int height) {
		byte[] data = new byte[24];
		byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
		System.arraycopy(signature, 0, data, 0, signature.length);
		writeInt(data, 8, 13);
		data[12] = 'I';
		data[13] = 'H';
		data[14] = 'D';
		data[15] = 'R';
		writeInt(data, 16, width);
		writeInt(data, 20, height);
		return data;
	}

	private static void writeInt(byte[] data, int offset, int value) {
		data[offset] = (byte) (value >>> 24);
		data[offset + 1] = (byte) (value >>> 16);
		data[offset + 2] = (byte) (value >>> 8);
		data[offset + 3] = (byte) value;
	}
}
