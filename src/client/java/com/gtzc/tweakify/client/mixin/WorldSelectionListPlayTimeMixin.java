package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.WorldPlayTime;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 存档列表每次重新填充（打开界面、搜索过滤）后刷新游玩时长缓存。
 *
 * <p>只在单人存档列表生效：Realms/上传世界没有本地 stats。
 * 功能关闭时清空缓存，避免关闭后仍占用内存。
 */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListPlayTimeMixin {
	@Inject(method = "fillLevels", at = @At("TAIL"))
	private void tweakify$refreshPlayTime(String filter, List<LevelSummary> levels, CallbackInfo ci) {
		WorldSelectionList list = (WorldSelectionList) (Object) this;
		if (((WorldSelectionListAccessor) list).tweakify$entryType()
			!= WorldSelectionList.EntryType.SINGLEPLAYER) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.functions.savePlayTime) {
			WorldPlayTime.refresh(levels);
		} else {
			WorldPlayTime.clear();
		}
	}
}
