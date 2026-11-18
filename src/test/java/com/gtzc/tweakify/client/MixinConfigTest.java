package com.gtzc.tweakify.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 客户端 Mixin 配置使用 @Expression 时必须声明 MixinExtras 最低版本；
 * 漏声明会在 TreeFeature 变换阶段直接导致启动崩溃。
 */
class MixinConfigTest {

	@Test
	void clientConfigDeclaresMixinExtrasVersion() throws IOException {
		Path path = Path.of("src/client/resources/tweakify-mod.client.mixins.json");
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonObject extra = root.getAsJsonObject("mixinextras");
			assertNotNull(extra, "client mixin config must declare mixinextras");
			assertEquals("0.5.5", extra.get("minVersion").getAsString());
		}
	}
}
