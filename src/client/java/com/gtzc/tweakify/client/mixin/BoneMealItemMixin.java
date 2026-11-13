package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.BetterBoneMeal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 更好的骨粉催熟：在 {@link BoneMealItem#growCrop} 入口接入甘蔗与花的额外行为。
 *
 * <p>返回给原版的 true 会沿用 useOn 的成功路径（挥动、振动、1505 粒子事件），
 * 因此这里只需要负责世界改动、消耗骨粉与补充自定义粒子。
 */
@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
	// 用 @WrapMethod 自行决定是否调用原方法，避免其它模组包装 growCrop 时我们的短路被绕过。
	@WrapMethod(method = "growCrop")
	private static boolean tweakify$betterBoneMeal(ItemStack stack, Level level, BlockPos pos,
			Operation<Boolean> original) {
		Boolean result = BetterBoneMeal.tryGrow(stack, level, pos);
		return result != null ? result : original.call(stack, level, pos);
	}
}
