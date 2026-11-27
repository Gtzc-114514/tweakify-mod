package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;

/**
 * 消耗品优化（参考 ConsumableOptimizer）：客户端在本地完成进食/饮用，不再等待服务器往返。
 *
 * <p>原版只有在服务端确认后才会结束使用动作，高延迟时会一直卡在进食动画里。
 * 这里在本地把「剩余使用时间归零」的那一帧直接收尾，并短暂吃掉服务器随后回放的
 * 重复音效、完成事件与「仍在使用物品」的状态同步，避免动画重新开始。
 *
 * <p>只在远程服务器生效：单人/局域网主机的集成服务端本来就无延迟，不需要这一层。
 */
public final class ConsumableOptimization {
	// 与参考实现一致：一次性粒子/音效数量。
	private static final int CONSUME_PARTICLES = 16;

	private static final ConsumableOptimizationState STATE = new ConsumableOptimizationState();

	private ConsumableOptimization() {
	}

	// 功能是否处于生效条件：开关开启且当前不是集成服务端。
	public static boolean isActive(TweakifyModConfig config, Minecraft client) {
		return config.optimization.consumableOptimization && !client.hasSingleplayerServer();
	}

	public static boolean isConsumable(ItemStack stack) {
		return !stack.isEmpty() && stack.get(DataComponents.CONSUMABLE) != null;
	}

	// LivingEntity.updateUsingItem 的 HEAD 注入：返回 true 时取消原版逻辑。
	public static boolean handleUpdateUsingItem(LocalPlayer player, ItemStack useItem) {
		Minecraft client = Minecraft.getInstance();
		TweakifyModConfig config = TweakifyMod.config();
		if (client.player != player || !isActive(config, client)) {
			return false;
		}
		Consumable component = useItem.get(DataComponents.CONSUMABLE);
		if (component == null) {
			return false;
		}
		// 已经不能继续吃（饱食度满、物品进入冷却）：结束本次使用，避免客户端空等。
		// getUseItem() 为空说明本地已经释放过，再发释放包只会变成每 tick 一次的无效网络包。
		if (!player.getUseItem().isEmpty()
			&& (!component.canConsume(player, useItem) || player.getCooldowns().isOnCooldown(useItem))) {
			if (client.gameMode != null) {
				client.gameMode.releaseUsingItem(player);
			}
		}
		if (player.getUseItemRemainingTicks() > 0) {
			return false;
		}
		finishConsuming(client, player, config);
		return true;
	}

	private static void finishConsuming(Minecraft client, LocalPlayer player, TweakifyModConfig config) {
		if (!player.isUsingItem()) {
			return;
		}
		ItemStack active = player.getUseItem();
		if (!active.equals(player.getItemInHand(player.getUsedItemHand()))) {
			if (client.gameMode != null) {
				client.gameMode.releaseUsingItem(player);
			}
			return;
		}
		if (active.isEmpty()) {
			return;
		}
		if (config.optimization.consumableOptimizationInstantFeedback) {
			playLocalFeedback(player, active);
		}
		// 等待窗口、换手动画窗口与音效归属半径都按当前 RTT 自适应（见 ConsumableOptimizationState）。
		long now = System.currentTimeMillis();
		long waitMillis = ConsumableOptimizationState.adaptiveServerWaitMillis(playerLatencyMillis(client));
		player.stopUsingItem();
		STATE.startServerWait(now, waitMillis);
		Consumable component = active.get(DataComponents.CONSUMABLE);
		if (config.optimization.consumableOptimizationSuppressServerFeedback && component != null) {
			STATE.suppressSound(component.sound().value());
			STATE.suppressBurp();
		}
		if (config.optimization.consumableOptimizationSkipHandSwapAnimation) {
			STATE.suppressEquipmentAnimation(now, waitMillis);
		}
	}

	// 本地播放进食音效、粒子与打嗝，替代服务端确认后才发出的反馈。
	private static void playLocalFeedback(LocalPlayer player, ItemStack stack) {
		Consumable component = stack.get(DataComponents.CONSUMABLE);
		if (component == null) {
			return;
		}
		component.emitParticlesAndSounds(player.getRandom(), player, stack, CONSUME_PARTICLES);
		if (component.animation() == ItemUseAnimation.EAT || stack.is(Items.HONEY_BOTTLE)) {
			playSound(player, SoundEvents.PLAYER_BURP, 0.5F,
				Mth.nextFloat(player.getRandom(), 0.9F, 1.0F));
		}
	}

	private static void playSound(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
		if (player.level() == null) {
			return;
		}
		player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
			sound, SoundSource.NEUTRAL, volume, pitch);
	}

	// 服务端稍后发来的「使用完成」事件：本地已经播放过音效与粒子，直接丢掉。
	public static boolean shouldDropUseCompletePacket(ClientboundEntityEventPacket packet) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return false;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (!isActive(config, client) || !config.optimization.consumableOptimizationSuppressServerFeedback) {
			return false;
		}
		if (packet.getEventId() != EntityEvent.USE_ITEM_COMPLETE) {
			return false;
		}
		if (packet.getEntity(client.level) != player) {
			return false;
		}
		if (!STATE.isWaitingForServer(System.currentTimeMillis())) {
			return false;
		}
		// 不提前结束窗口：进食音效往往紧跟在完成事件之后到达，需要继续吃掉。
		return true;
	}

	// 服务端回复的进食音效与打嗝：与本地播放重复，各吃掉一次。
	// 只吃自己身上发出的那一次：半径随 RTT 自适应（2–6 格），避免误吞其他玩家的进食声。
	public static boolean shouldSuppressServerSound(ClientboundSoundPacket packet) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return false;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (!isActive(config, client) || !config.optimization.consumableOptimizationSuppressServerFeedback) {
			return false;
		}
		if (!STATE.isWaitingForServer(System.currentTimeMillis())) {
			// 窗口已结束：清掉残留标记，不再拦截任何后续音效。
			STATE.clearSoundSuppression();
			STATE.consumeSuppressedBurp();
			return false;
		}
		double maxDistance = ConsumableOptimizationState.adaptiveSoundRadius(playerLatencyMillis(client));
		double dx = packet.getX() - player.getX();
		double dy = packet.getY() - player.getY();
		double dz = packet.getZ() - player.getZ();
		if (dx * dx + dy * dy + dz * dz > maxDistance * maxDistance) {
			return false;
		}
		SoundEvent sound = packet.getSound().value();
		if (SoundEvents.PLAYER_BURP.equals(sound)) {
			return STATE.consumeSuppressedBurp();
		}
		return STATE.consumeSuppressedSound(sound);
	}

	// 吃完同一堆叠后库存同步到达时的那一次切换：直接替换物品，不播快照动画。
	// 判定按「同一物品且数量减少」或「最后一个物品被吃光」，不依赖一次性布尔标记的时序。
	public static boolean shouldSkipHandSwapAnimation(ItemStack from, ItemStack to) {
		Minecraft client = Minecraft.getInstance();
		TweakifyModConfig config = TweakifyMod.config();
		if (!isActive(config, client) || !config.optimization.consumableOptimizationSkipHandSwapAnimation) {
			return false;
		}
		if (!isConsumable(from)) {
			return false;
		}
		if (!STATE.isEquipmentAnimationSuppressed(System.currentTimeMillis())) {
			return false;
		}
		boolean consumed = to.isEmpty()
			? from.getCount() == 1
			: to.is(from.getItem()) && to.getCount() < from.getCount();
		if (!consumed) {
			// 窗口内其它无关切换正常播动画，标记留给真正的那一次库存变化。
			return false;
		}
		STATE.clearEquipmentAnimation();
		return true;
	}

	// 玩家列表里的延迟估计（毫秒）：未连接或拿不到资料时按 0 处理，退化为最小窗口。
	private static int playerLatencyMillis(Minecraft client) {
		if (client.getConnection() == null || client.player == null) {
			return 0;
		}
		PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
		return info != null ? Math.max(0, info.getLatency()) : 0;
	}

	// 断线或切换世界时复位，避免把上一次会话的标记带进新连接。
	public static void reset() {
		STATE.reset();
	}
}
