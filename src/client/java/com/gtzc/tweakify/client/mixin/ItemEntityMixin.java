package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 掉落物消失时间。
 *
 * <p>不能用 @ModifyConstant 改 tick 里的 6000 常量：architectury 的 MixinItemEntity 已经用
 * @ModifyConstant 占用了同一处，Mixin 会跳过同优先级的第二个常量修改器，导致注入失败并让游戏崩溃。
 *
 * <p>改成两个互不冲突的注入点：
 * <ul>
 *   <li>tick 头部：年龄达到配置值时主动 discard（配置比原版短时立即生效，也覆盖原版常量被其他模组改大的情况）；</li>
 *   <li>拦截 tick 里的 discard 调用：年龄还没到配置值时取消移除（配置比原版长时阻止提前消失）。</li>
 * </ul>
 * 两处都是 require = 0：极端情况下（其他模组整体重写 tick）功能失效即可，绝不让注入失败拖垮启动。
 * 注入是否真的生效由行为探针确认：handler 被合并进目标类不代表注入成功，
 * 只有 tick 头部实际执行时才会打印 "Mixin hook active: itemDespawnTick"。
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {
	private static final int VANILLA_LIFETIME = 6000;

	@Unique
	private static final AtomicBoolean TWEAKIFY$TICK_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$GUARD_PROBE = new AtomicBoolean();

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$expireWhenDue(CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$TICK_PROBE, "itemDespawnTick");
		ItemEntity self = (ItemEntity) (Object) this;
		if (self.getAge() >= tweakify$configuredLifetime(self)) {
			self.discard();
			ci.cancel();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"), require = 0)
	private void tweakify$guardVanillaDespawn(ItemEntity self) {
		HookProbe.markOnce(TWEAKIFY$GUARD_PROBE, "itemDespawnGuard");
		// tick 开头对空物品实体也会调用 discard：这一处与寿命无关，必须照常丢弃。
		// 空物品分支在原版 tick 里会先 return，age 不会增长；若拦截这一处，空物品实体会永不消失。
		if (self.getItem().isEmpty() || self.getAge() >= tweakify$configuredLifetime(self)) {
			self.discard();
		}
	}

	private static int tweakify$configuredLifetime(ItemEntity self) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return VANILLA_LIFETIME;
		}
		// 仅对玩家丢弃的掉落物生效：怪物掉落、方块掉落等保持原版时长。
		if (config.itemDespawnPlayerDroppedOnly && !(self.getOwner() instanceof Player)) {
			return VANILLA_LIFETIME;
		}
		return config.itemDespawnTime;
	}
}
