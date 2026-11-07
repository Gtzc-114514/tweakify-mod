package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CutThroughItemMode;
import com.gtzc.tweakify.client.CutThroughPlantMode;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 剑穿草打生物：主手持剑时，准星命中花草也直接选中后方的生物（像 Cut Through 一样），
 * 左键不会先把草打掉。
 *
 * <p>只处理无碰撞、非流体的植物：甜浆果丛这类有碰撞的方块与水草保持原版行为。
 * 26.3 的准星命中计算已从 GameRenderer 移到 LocalPlayer.raycastHitResult，
 * 因此在返回值上做替换，两种取目标方式（普通攻击 / 武器攻击距离组件）都能覆盖。
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerCutThroughMixin {
	@Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true, require = 0)
	private void tweakify$cutThroughPlants(float partialTick, Entity camera,
										   CallbackInfoReturnable<HitResult> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (config == null || !config.swordCutThroughPlants || self.level() == null) {
			return;
		}
		// 潜行时恢复原版：先把植物打掉，方便“先清理再攻击”。
		if (config.cutThroughSneakBypass && self.isShiftKeyDown()) {
			return;
		}
		if (!isAllowedItem(self.getMainHandItem(), CutThroughItemMode.fromCode(config.cutThroughItems))) {
			return;
		}
		if (!(cir.getReturnValue() instanceof BlockHitResult blockHit)
			|| blockHit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockState state = self.level().getBlockState(blockHit.getBlockPos());
		if (!isCuttablePlant(state, CutThroughPlantMode.fromCode(config.cutThroughPlants))) {
			return;
		}
		// 真正需要穿草时才走后面的重算，顺便确认钩子活着（每会话只记一次）。
		HookProbe.markOnce("cutThroughPlants");
		Vec3 eye = self.getEyePosition(partialTick);
		EntityHitResult entityHit = findEntityInFront(self, eye, partialTick);
		if (entityHit == null) {
			return;
		}
		// 植物后方还有实心方块挡着时不替换，避免隔着墙选中生物。
		BlockHitResult obstruction = self.level().clip(new ClipContext(eye, entityHit.getLocation(),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (obstruction.getType() == HitResult.Type.MISS) {
			cir.setReturnValue(entityHit);
		}
	}

	// 与攻击判定一致地重新收集实体命中：视线方向、实体交互距离、可选中实体。
	private static EntityHitResult findEntityInFront(LocalPlayer player, Vec3 eye, float partialTick) {
		double range = player.entityInteractionRange();
		Vec3 view = player.getViewVector(partialTick);
		Vec3 end = eye.add(view.x * range, view.y * range, view.z * range);
		AABB box = player.getBoundingBox().expandTowards(view.scale(range)).inflate(1.0);
		return ProjectileUtil.getEntityHitResult(player, eye, end, box,
			EntitySelector.CAN_BE_PICKED, range * range);
	}

	// 生效物品：依配置决定剑 / 剑与斧 / 任意手持物品。
	private static boolean isAllowedItem(ItemStack stack, CutThroughItemMode mode) {
		return switch (mode) {
			case SWORD -> stack.is(ItemTags.SWORDS);
			case SWORD_AND_AXE -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
			case ANY -> true;
		};
	}

	// 只放行无碰撞、非流体的植物，并按配置的植物范围过滤。
	private static boolean isCuttablePlant(BlockState state, CutThroughPlantMode mode) {
		if (!(state.getBlock() instanceof VegetationBlock) || state.isSolid()
			|| !state.getFluidState().isEmpty()) {
			return false;
		}
		boolean flowerOrGrass = state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
			|| state.is(BlockTags.REPLACEABLE_BY_TREES);
		return switch (mode) {
			case FLOWERS -> flowerOrGrass;
			case CROPS -> flowerOrGrass || state.is(BlockTags.CROPS);
			case ALL -> true;
		};
	}
}
