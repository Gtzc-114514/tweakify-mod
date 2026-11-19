package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露存档列表的条目类型：只有单人存档（SINGLEPLAYER）需要显示收藏星。
 */
@Mixin(WorldSelectionList.class)
public interface WorldSelectionListAccessor {
	@Accessor("entryType")
	WorldSelectionList.EntryType tweakify$entryType();
}
