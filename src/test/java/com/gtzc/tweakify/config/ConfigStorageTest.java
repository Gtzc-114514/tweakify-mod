package com.gtzc.tweakify.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 覆盖落盘层的边界：没有父目录的路径必须直接返回失败，
 * 不能把 NPE 抛给调用方——异步写盘任务里的异常会静默丢失回滚与日志。
 */
class ConfigStorageTest {

	@Test
	void writeAtomicallyRejectsPathWithoutParent() {
		assertFalse(ConfigStorage.writeAtomically("{}", Path.of("no-parent.json")));
	}
}
