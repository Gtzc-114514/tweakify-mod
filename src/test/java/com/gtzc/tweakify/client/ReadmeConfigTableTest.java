package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.ConfigCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `docs/features.md`「功能一览」与配置目录的一致性：
 * 表首以 {@code `字段名`} 标注的配置项必须都能在 {@link ConfigCatalog} 中找到。
 *
 * <p>只解析每个表格行开头的反引号，命令（/tweakify、/fconfig）与正文中的代码片段不参与校验；
 * 不校验项数、顺序或说明文字，避免文档微调导致测试变脆。
 */
class ReadmeConfigTableTest {
	private static final Path FEATURES = Path.of("docs/features.md");
	private static final Pattern ROW_FIELD = Pattern.compile("^\\| `([^`]+)`");
	private static final Pattern SECTION_START = Pattern.compile("^# .*功能一览.*$");

	@Test
	void featureTableFieldsExistInConfigCatalog() throws IOException {
		List<String> lines = Files.readAllLines(FEATURES, StandardCharsets.UTF_8);
		List<String> fields = featureTableFields(lines);
		assertFalse(fields.isEmpty(), "README 功能表必须能解析出配置项");
		assertTrue(fields.size() > 300, "功能表项数异常偏少: " + fields.size());

		List<String> missing = new ArrayList<>();
		for (String field : fields) {
			if (field.startsWith("/")) {
				continue; // 命令行不参与配置目录校验
			}
			if (ConfigCatalog.entryOf(field) == null) {
				missing.add(field);
			}
		}
		assertTrue(missing.isEmpty(), "README 功能表引用了不存在的配置项: " + missing);
	}

	private static List<String> featureTableFields(List<String> lines) {
		List<String> fields = new ArrayList<>();
		boolean inSection = false;
		for (String line : lines) {
			if (SECTION_START.matcher(line).matches()) {
				inSection = true;
				continue;
			}
			if (!inSection) {
				continue;
			}
			if (line.startsWith("## ")) {
				break; // 进入下一章节
			}
			Matcher matcher = ROW_FIELD.matcher(line);
			if (matcher.find()) {
				fields.add(matcher.group(1));
			}
		}
		return fields;
	}
}
