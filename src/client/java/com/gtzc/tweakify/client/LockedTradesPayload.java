package com.gtzc.tweakify.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * 服务端 → 客户端：村民未来等级（锁定）的交易列表。
 * 客户端收到后合并到当前打开的 MerchantMenu，显示为不可点击的灰色条目。
 */
public record LockedTradesPayload(Optional<MerchantOffers> offers) implements CustomPacketPayload {
	public static final Type<LockedTradesPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath("tweakify-mod", "locked_trades"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LockedTradesPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeBoolean(payload.offers().isPresent());
			payload.offers().ifPresent(offers -> MerchantOffers.STREAM_CODEC.encode(buf, offers));
		},
		buf -> buf.readBoolean()
			? new LockedTradesPayload(Optional.of(MerchantOffers.STREAM_CODEC.decode(buf)))
			: new LockedTradesPayload(Optional.empty())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
