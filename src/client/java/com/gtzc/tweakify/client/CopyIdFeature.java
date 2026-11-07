package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.AbstractContainerScreenAccessor;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 复制目标 ID：容器界面优先取鼠标悬停的槽位，其次取准星指向的实体，最后取准星指向的方块。
 * 复制结果按配置格式化后写入剪贴板，可选在聊天栏显示可点击复制的消息、复制后关闭容器。
 */
public final class CopyIdFeature {
	private static final String COPIED_KEY = "tweakify-mod.message.copy_id.copied";
	private static final String NO_TARGET_KEY = "tweakify-mod.message.copy_id.no_target";

	private CopyIdFeature() {
	}

	public static void copyTargetId() {
		Minecraft client = Minecraft.getInstance();
		TweakifyModConfig config = TweakifyMod.CONFIG;
		Target target = resolveTarget(client);
		if (target == null) {
			if (config == null || config.copyItemIdChatMessage) {
				showMessage(client, Component.translatable(NO_TARGET_KEY));
			}
			return;
		}

		String text = formatTarget(target, config);
		client.keyboardHandler.setClipboard(text);
		if (config == null || config.copyItemIdChatMessage) {
			// 聊天栏里重复显示一份可点击的 ID，点击即再次复制。
			Component clickableId = Component.literal(text).withStyle(style -> style
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.CopyToClipboard(text)));
			showMessage(client, Component.translatable(COPIED_KEY, clickableId));
		}
		if (config != null && config.copyItemIdCloseContainer
			&& client.gui.screen() instanceof AbstractContainerScreen<?>) {
			client.gui.setScreen(null);
		}
	}

	// 复制的目标：ID、类型（block/entity/item）与数量（只有物品有）。
	private record Target(String id, String type, int count) {
	}

	private static String formatTarget(Target target, TweakifyModConfig config) {
		CopyIdFormat format = config == null
			? CopyIdFormat.PLAIN
			: CopyIdFormat.fromCode(config.copyItemIdFormat);
		return switch (format) {
			case PLAIN -> target.id();
			case TYPED -> target.type() + ":" + target.id();
			case COUNTED -> target.count() > 1 ? target.count() + "x " + target.id() : target.id();
		};
	}

	private static void showMessage(Minecraft client, Component message) {
		client.gui.chatListener().handleSystemMessage(message, false);
	}

	private static Target resolveTarget(Minecraft client) {
		if (client.gui.screen() instanceof AbstractContainerScreen<?> containerScreen) {
			Slot slot = ((AbstractContainerScreenAccessor) containerScreen).tweakify$getHoveredSlot();
			if (slot != null && slot.hasItem()) {
				ItemStack stack = slot.getItem();
				String id = itemId(stack);
				return id == null ? null : new Target(id, "item", stack.getCount());
			}
		}

		Entity entity = client.crosshairPickEntity;
		if (entity != null) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			if (id != null) {
				return new Target(id.toString(), "entity", 1);
			}
		}

		HitResult hitResult = client.hitResult;
		if (hitResult instanceof BlockHitResult blockHit && client.level != null) {
			BlockPos pos = blockHit.getBlockPos();
			BlockState state = client.level.getBlockState(pos);
			return new Target(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), "block", 1);
		}
		return null;
	}

	private static String itemId(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id == null ? null : id.toString();
	}
}
