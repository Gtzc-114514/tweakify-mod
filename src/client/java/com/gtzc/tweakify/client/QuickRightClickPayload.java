package com.gtzc.tweakify.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求打开背包里某个槽位的物品界面（功能方块/潜影盒/末影箱）。
 *
 * <p>只传当前菜单 id、槽位下标与点击时的潜行状态：物品与内容都由服务端以自己那份菜单数据为准，
 * 客户端无法伪造物品内容；菜单 id 不一致时直接忽略，避免界面切换瞬间打开错误槽位。
 * 带上潜行状态是为了避免“点击后立刻松开 Shift、包到达时服务端判定未潜行”导致点击被吞掉。
 */
public record QuickRightClickPayload(int containerId, int slot, boolean sneaking)
		implements CustomPacketPayload {
	public static final Type<QuickRightClickPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath("tweakify-mod", "quick_right_click"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuickRightClickPayload> CODEC =
		StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.containerId());
				buf.writeVarInt(payload.slot());
				buf.writeBoolean(payload.sneaking());
			},
			buf -> new QuickRightClickPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
