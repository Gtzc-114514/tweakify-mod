package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.mojang.authlib.services.MinecraftServicesUserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-247973：收到聊天消息时同步拉取屏蔽名单会卡住客户端。
 *
 * <p>原版 isBlockedPlayer 在 blockList 为空时直接调用 fetchBlockList()，
 * 网络请求阻塞调用线程；这里改为后台拉取并先返回 false，后续调用复用结果。
 */
@Mixin(value = MinecraftServicesUserApiService.class, remap = false)
public abstract class MinecraftServicesUserApiServiceChatLagMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$CHAT_LAG_PROBE = new AtomicBoolean();

	@Shadow
	private Set<UUID> blockList;

	@Unique
	private volatile Set<UUID> tweakify$asyncBlockList;

	@Unique
	private volatile long tweakify$asyncExpiresAt;

	@Unique
	private final AtomicBoolean tweakify$fetching = new AtomicBoolean();

	@Inject(method = "isBlockedPlayer", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$fetchBlockListAsync(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.fix.chatLagFix) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$CHAT_LAG_PROBE, "chatLagFix");
		Set<UUID> list = this.blockList;
		int cacheSeconds = config.fix.chatLagFixCacheSeconds;
		if (list == null) {
			list = this.tweakify$asyncBlockList;
			if (list != null && cacheSeconds > 0
				&& System.currentTimeMillis() >= this.tweakify$asyncExpiresAt) {
				this.tweakify$asyncBlockList = null;
				list = null;
			}
		}
		if (list != null) {
			cir.setReturnValue(list.contains(uuid));
			return;
		}
		if (this.tweakify$fetching.compareAndSet(false, true)) {
			int timeoutSeconds = config.fix.chatLagFixTimeoutSeconds;
			CompletableFuture.supplyAsync(() -> {
				try {
					return this.fetchBlockList();
				} catch (RuntimeException error) {
					return null;
				}
			}).completeOnTimeout(null, timeoutSeconds, TimeUnit.SECONDS)
				.whenComplete((fetched, error) -> {
					if (fetched != null) {
						this.tweakify$asyncBlockList = Set.copyOf(fetched);
						if (cacheSeconds > 0) {
							this.tweakify$asyncExpiresAt = System.currentTimeMillis()
								+ cacheSeconds * 1000L;
						}
					}
					this.tweakify$fetching.set(false);
				});
		}
		// 首次调用不等网络：先按“未屏蔽”处理，后台完成后后续消息使用新名单。
		cir.setReturnValue(false);
	}

	@Shadow
	private Set<UUID> fetchBlockList() {
		throw new AssertionError("Mixin shadow not applied");
	}
}
