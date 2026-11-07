package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 更好的放置覆盖：允许被放置方块替换的植物范围。
 */
public enum BlockPlacementMode implements CodeOptionEntry {
	FLOWERS(TweakifyModConfig.BLOCK_PLACEMENT_FLOWERS,
		"tweakify-mod.config.general.option.betterBlockPlacementMode.flowers"),
	CROPS(TweakifyModConfig.BLOCK_PLACEMENT_CROPS,
		"tweakify-mod.config.general.option.betterBlockPlacementMode.crops"),
	ALL(TweakifyModConfig.BLOCK_PLACEMENT_ALL,
		"tweakify-mod.config.general.option.betterBlockPlacementMode.all");

	private final String code;
	private final String translationKey;

	BlockPlacementMode(String code, String translationKey) {
		this.code = code;
		this.translationKey = translationKey;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public static BlockPlacementMode fromCode(String code) {
		for (BlockPlacementMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return FLOWERS;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
