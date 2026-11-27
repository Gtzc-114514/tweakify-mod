package com.gtzc.tweakify.client.state;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定交易网络包：Optional 状态（空 / present）与交易列表的编解码往返。
 * 不含实际交易项：空列表足以覆盖 present 标志与列表编解码路径，
 * 单条交易的构造依赖完整注册表，交由集成/实机路径覆盖。
 */
class LockedTradesPayloadTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void emptyOffersRoundTrip() {
		LockedTradesPayload decoded = roundTrip(new LockedTradesPayload(Optional.empty()));

		assertTrue(decoded.offers().isEmpty(), "空 Optional 往返后仍为空");
		assertEquals(LockedTradesPayload.TYPE, decoded.type());
	}

	@Test
	void presentButEmptyOffersRoundTrip() {
		LockedTradesPayload decoded = roundTrip(
			new LockedTradesPayload(Optional.of(new MerchantOffers())));

		assertTrue(decoded.offers().isPresent(), "present 标志往返后保留");
		assertTrue(decoded.offers().get().isEmpty(), "空交易列表内容不变");
	}

	private static LockedTradesPayload roundTrip(LockedTradesPayload payload) {
		RegistryFriendlyByteBuf buf =
			new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
		try {
			LockedTradesPayload.CODEC.encode(buf, payload);
			return LockedTradesPayload.CODEC.decode(buf);
		} finally {
			buf.release();
		}
	}
}
