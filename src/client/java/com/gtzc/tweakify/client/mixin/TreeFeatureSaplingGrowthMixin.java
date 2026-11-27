package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SaplingGrowthState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-8187：2×2 云杉/丛林树苗在北/西侧有相邻方块时无法生长。
 *
 * <p>26.3 的 getMaxFreeTreeHeight 在基础尺寸为 1 时把 x/z 扫描偏移设为 -1，
 * 使北/西侧的普通方块被当作树干阻挡。这里只把该偏移改为 0，其它尺寸、
 * 其它偏移与配置关闭时保持原版。
 */
@Mixin(TreeFeature.class)
public abstract class TreeFeatureSaplingGrowthMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$SAPLING_GROWTH_PROBE = new AtomicBoolean();

	@Shadow
	public abstract FeatureSize minimumSize();

	@Definition(id = "x", local = @Local(type = int.class, name = "x"))
	@Definition(id = "z", local = @Local(type = int.class, name = "z"))
	@Definition(id = "r", local = @Local(type = int.class, name = "r"))
	@Expression({
		"x = @(-r)",
		"z = @(-r)"
	})
	@ModifyExpressionValue(method = "getMaxFreeTreeHeight",
		at = @At("MIXINEXTRAS:EXPRESSION"), require = 0)
	private int tweakify$fixTwoByTwoOffset(int start,
			@Local(argsOnly = true, index = 2) int maxTreeHeight) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config.fix.saplingGrowthFix;
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$SAPLING_GROWTH_PROBE, "saplingGrowthFix");
		}
		int baseSize = this.minimumSize().getSizeAtHeight(maxTreeHeight, 0);
		return SaplingGrowthState.fixedOffset(enabled, start, baseSize);
	}
}
