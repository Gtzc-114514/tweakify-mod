package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取可疑方块方块实体数据里的 item 标签名，用于手持信息显示刷出物品。
 */
@Mixin(BrushableBlockEntity.class)
public interface BrushableBlockEntityAccessor {
	@Accessor("ITEM_TAG")
	static String getItemTagKey() {
		throw new AssertionError();
	}
}
