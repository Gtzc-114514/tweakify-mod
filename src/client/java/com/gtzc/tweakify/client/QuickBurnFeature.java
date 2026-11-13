package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * 便捷焚烧：在背包或任意容器界面里，光标拿着物品并右键点击岩浆桶/打火石时焚烧该物品。
 *
 * <p>纯客户端模组没有通用的"销毁光标物品"协议：
 * <ul>
 *   <li>单人/局域网（含创造模式）：通过集成服务器清空服务端光标并同步，物品真正销毁；</li>
 *   <li>远程服务器（生存或创造，普通容器）：退化为点击容器外部（丢弃），物品会掉在玩家脚下；</li>
 *   <li>创造物品栏（光标为客户端虚拟物品）：直接本地清空，物品真正消失。</li>
 * </ul>
 * 三种情况都会在玩家位置播放火焰粒子作为反馈。
 */
public final class QuickBurnFeature {
	private QuickBurnFeature() {
	}

	// 可用来焚烧物品的工具：岩浆桶与打火石。
	static boolean isBurnTool(Item item) {
		return item == Items.LAVA_BUCKET || item == Items.FLINT_AND_STEEL;
	}

	// 右键目标槽位是否为可焚烧工具，且光标上确实拿着物品。
	public static boolean canBurn(AbstractContainerMenu menu, Slot slot) {
		return menu != null && slot != null && slot.hasItem()
			&& isBurnTool(slot.getItem().getItem())
			&& !menu.getCarried().isEmpty();
	}

	// 执行焚烧：清空光标物品（单人/局域网真正销毁）并播放火焰粒子。
	public static boolean tryBurn(Minecraft client, AbstractContainerMenu menu) {
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null || menu.getCarried().isEmpty()) {
			return false;
		}
		ItemStack burned = menu.getCarried().copy();
		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			// 单人/局域网（含创造模式）：服务端持有权威光标，必须由服务端清空并同步。
			// 本地先清空是为了界面即时反馈：最坏情况下延迟任务校验失败（关闭/切换容器），
			// 物品会随容器关闭原样退回背包，不会丢失或复制。
			menu.setCarried(ItemStack.EMPTY);
			int containerId = menu.containerId;
			UUID uuid = player.getUUID();
			if (server.isStopped() || !server.isRunning()) {
				// 服务器正在停止：不再提交任务（本地已清空，服务端可能回滚，可接受）。
				TweakifyMod.LOGGER.debug("Quick burn: server stopping, skip authoritative clear");
			} else {
				try {
					server.executeIfPossible(() -> {
						ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
						// 任务延迟执行：菜单已切换或光标内容已变时不能误清。
						if (serverPlayer != null
							&& serverPlayer.containerMenu.containerId == containerId
							&& ItemStack.matches(serverPlayer.containerMenu.getCarried(), burned)) {
							serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
							serverPlayer.containerMenu.broadcastChanges();
							TweakifyMod.LOGGER.info("Quick burn: destroyed item held on cursor");
						}
					});
				} catch (RejectedExecutionException e) {
					TweakifyMod.LOGGER.debug("Quick burn: server stopping, skip authoritative clear");
				}
			}
		} else {
			// 远程服务器（生存或创造）：没有销毁光标物品的协议，点击容器外部等价于丢弃。
			client.gameMode.handleContainerInput(menu.containerId,
				AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0, ContainerInput.PICKUP, player);
			TweakifyMod.LOGGER.debug("Quick burn (remote): dropped carried stack instead of destroying");
		}
		spawnBurnParticles(client, player);
		return true;
	}

	// 创造物品栏：光标完全由客户端管理，直接清空即可。
	public static boolean tryBurnCreative(Minecraft client, AbstractContainerMenu menu) {
		LocalPlayer player = client.player;
		if (player == null || menu.getCarried().isEmpty()) {
			return false;
		}
		menu.setCarried(ItemStack.EMPTY);
		spawnBurnParticles(client, player);
		TweakifyMod.LOGGER.debug("Quick burn: destroyed item held on cursor (creative inventory)");
		return true;
	}

	// 玩家位置附近的火焰/烟雾粒子反馈。
	private static void spawnBurnParticles(Minecraft client, LocalPlayer player) {
		if (client.level == null) {
			return;
		}
		for (int i = 0; i < 6; i++) {
			double dx = (player.getRandom().nextDouble() - 0.5) * 0.6;
			double dz = (player.getRandom().nextDouble() - 0.5) * 0.6;
			client.level.addParticle(ParticleTypes.FLAME,
				player.getX() + dx, player.getY() + 0.4, player.getZ() + dz, 0.0, 0.03, 0.0);
		}
		client.level.addParticle(ParticleTypes.LARGE_SMOKE,
			player.getX(), player.getY() + 0.6, player.getZ(), 0.0, 0.05, 0.0);
	}
}
