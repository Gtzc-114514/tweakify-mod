package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ResourcePackCompatibilityTargets;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

/**
 * 读取资源包/数据包选择界面的目录，用于区分 resourcepacks 与 datapacks。
 */
@Mixin(PackSelectionScreen.class)
public interface PackSelectionScreenAccessor {
	@Accessor(ResourcePackCompatibilityTargets.SCREEN_PACK_DIR_FIELD)
	Path tweakify$getPackDir();
}
