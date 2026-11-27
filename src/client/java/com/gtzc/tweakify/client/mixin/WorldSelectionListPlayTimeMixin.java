package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.WorldPlayTime;
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
 * <p>只给单人存档列表显示时长；功能关闭、Realms 或上传列表都清空缓存：
 * 上传列表同样展示本地存档，若不清理会把单人列表的缓存串过去。
 * 搜索过滤复用同一列表实例，{@link com.gtzc.tweakify.client.state.WorldPlayTime#refresh}
 * 内部按引用去重，不会每个按键都重扫磁盘。
 */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListPlayTimeMixin {
	@Inject(method = "fillLevels", at = @At("TAIL"))
	private void tweakify$refreshPlayTime(String filter, List<LevelSummary> levels, CallbackInfo ci) {
		WorldSelectionList list = (WorldSelectionList) (Object) this;
		TweakifyModConfig config = TweakifyMod.config();
		if (((WorldSelectionListAccessor) list).tweakify$entryType()
			!= WorldSelectionList.EntryType.SINGLEPLAYER
			|| !config.functions.savePlayTime) {
			WorldPlayTime.clear();
			return;
		}
		WorldPlayTime.refresh(levels);
	}
}
