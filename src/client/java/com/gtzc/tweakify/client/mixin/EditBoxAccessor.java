package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 {@code EditBox.updateTextPosition()}。
 *
 * <p>输入框的文字坐标是 {@code textX / textY} 两个私有缓存，只有这个私有方法会重算；
 * 原版外框用 {@code getX()/getY()} 绘制，文字却用缓存坐标。MaLiLib 的
 * {@code GuiTextFieldGeneric} 覆盖 {@code setY} 只改自己的 shadow 字段，
 * 所以亚行滚动平移输入框后会出现“外框跟着动、框内数字留在原地”。
 * 平移后调用一次本方法即可让文字与光标跟上。
 */
@Mixin(EditBox.class)
public interface EditBoxAccessor {
	@Invoker("updateTextPosition")
	void tweakify$updateTextPosition();
}
