package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.CreativeFlightNoclip;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 创造飞行穿墙：在 {@code Entity} 收集碰撞体时跳过方块碰撞。
 *
 * <p>只替换方块碰撞查询，实体碰撞与世界边界仍按原版处理；判定集中在
 * {@link CreativeFlightNoclip}，非本地创造飞行玩家不受影响。
 */
@Mixin(Entity.class)
public class EntityCreativeFlightNoclipMixin {
	@Redirect(method = "collectCollidersIgnoringWorldBorder"
			+ "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;"
			+ "Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getBlockCollisions"
				+ "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"))
	private static Iterable<VoxelShape> tweakify$skipBlockCollisions(Level level, Entity source, AABB box) {
		if (CreativeFlightNoclip.shouldBypass(source)) {
			return List.of();
		}
		return level.getBlockCollisions(source, box);
	}
}
