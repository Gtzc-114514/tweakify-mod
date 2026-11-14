package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import fi.dy.masa.malilib.config.IConfigBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 调试信息叠加层：在 HUD 左下角按行显示运行时状态。
 *
 * <p>基础行是坐标、朝向、维度与脚下方块；性能、准星目标、生物群系、速度、
 * 附近实体、配置统计、药水效果与钩子探针等追加行由同名子开关控制。
 * 实体与配置统计需要遍历列表，按 500ms 节流刷新；其余行每帧直接读取。
 * 文字沿用原版 HUD 的描边样式，保证在水下/亮色背景上也清晰。
 */
public final class DebugInfoOverlay {
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int LINE_HEIGHT = 10;
	private static final int MARGIN = 2;
	// 钩子列表最多显示这么多项，超出时以 "...(+N)" 结尾，避免叠加层被撑满屏幕。
	private static final int MAX_HOOK_LINES = 6;
	// 附近实体统计的扫描半径（格）。
	private static final double ENTITY_SCAN_RADIUS = 32.0;
	// 实体统计与配置统计的刷新间隔：两者都要遍历列表，按 500ms 节流，
	// 不影响每帧的坐标、速度等轻量行。
	private static final long SUMMARY_REFRESH_NANOS = 500_000_000L;

	private static long lastSummaryAtNanos = Long.MIN_VALUE;
	private static String cachedEntitySummary = "Entities: -";
	private static String cachedConfigSummary = "Config: -";

	private DebugInfoOverlay() {
	}

	// 由 Hud.extractRenderState 末尾调用：此时原版 HUD 已经提交。
	public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.testing.debugInfoOverlay) {
			return;
		}
		if (client.player == null || client.level == null) {
			return;
		}
		List<String> lines = buildLines(client, config);
		// 底边锚在原版条带（经验条/定位栏）上方，并让出一行名字：头像上沿 2 像素 + 名字行 + 1 像素余量。
		int bottom = ContextualBar.EMPTY.top(client.getWindow())
			- (2 + client.font.lineHeight + 1);
		int y = Math.max(0, bottom - LINE_HEIGHT * lines.size());
		for (String line : lines) {
			graphics.text(client.font, line, MARGIN, y, TEXT_COLOR, true);
			y += LINE_HEIGHT;
		}
	}

	// 包私有：游戏内测试可直接校验文本内容，不必依赖渲染。
	static List<String> buildLines(Minecraft client, TweakifyModConfig config) {
		List<String> lines = new ArrayList<>();
		BlockPos pos = client.player.blockPosition();
		lines.add(String.format("XYZ: %.1f / %.1f / %.1f", client.player.getX(),
			client.player.getY(), client.player.getZ()));
		lines.add("Facing: " + client.player.getDirection().getName()
			+ String.format(" (%.1f)", client.player.getYRot()));
		lines.add("Dim: " + client.level.dimension().identifier());
		BlockState below = client.level.getBlockState(pos.below());
		Identifier belowId = BuiltInRegistries.BLOCK.getKey(below.getBlock());
		lines.add("Below: " + (belowId == null ? "unknown" : belowId.toString()));
		if (config.testing.debugInfoShowPerformance) {
			Runtime runtime = Runtime.getRuntime();
			long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
			long maxMb = runtime.maxMemory() / (1024L * 1024L);
			lines.add("FPS: " + client.getFps() + "  Mem: " + usedMb + "/" + maxMb + " MB");
		}
		if (config.testing.debugInfoShowTarget) {
			lines.add("Target: " + resolveTarget(client));
		}
		if (config.testing.debugInfoShowBiome) {
			lines.add("Biome: " + resolveBiome(client));
		}
		if (config.testing.debugInfoShowSpeed) {
			Vec3 velocity = client.player.getDeltaMovement();
			lines.add(formatSpeed(velocity.x, velocity.y, velocity.z));
		}
		boolean showEntities = config.testing.debugInfoShowEntities;
		boolean showConfigStats = config.testing.debugInfoShowConfigStats;
		if (showEntities || showConfigStats) {
			refreshSummaries(client, showEntities, showConfigStats);
			if (showEntities) {
				lines.add(cachedEntitySummary);
			}
			if (showConfigStats) {
				lines.add(cachedConfigSummary);
			}
		}
		if (config.testing.debugInfoShowEffects) {
			lines.add(effectSummary(client));
		}
		if (config.testing.debugInfoShowHooks) {
			appendHookLines(lines);
		}
		return lines;
	}

	private static String resolveTarget(Minecraft client) {
		Entity entity = client.crosshairPickEntity;
		if (entity != null) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			return id == null ? "entity" : id.toString();
		}
		HitResult hit = client.hitResult;
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(
				client.level.getBlockState(blockHit.getBlockPos()).getBlock());
			return id == null ? "block" : id.toString();
		}
		return "-";
	}

	// 所在生物群系 ID；无法解析时显示 unknown。
	private static String resolveBiome(Minecraft client) {
		return client.level.getBiome(client.player.blockPosition()).unwrapKey()
			.map(key -> key.identifier().toString()).orElse("unknown");
	}

	// 玩家速度：水平与垂直分量各自换算为格每秒（tick 速度 × 20），便于确认飞行/爬行等是否生效。
	static String formatSpeed(double dx, double dy, double dz) {
		double horizontal = Math.sqrt(dx * dx + dz * dz) * 20.0;
		double vertical = dy * 20.0;
		return String.format(Locale.ROOT, "Speed: %.2f / %.2f (h/v)", horizontal, vertical);
	}

	// 节流刷新两类重统计：距离上次刷新不足 500ms 时直接复用缓存文本。
	private static void refreshSummaries(Minecraft client, boolean entities, boolean configStats) {
		long now = System.nanoTime();
		if (now - lastSummaryAtNanos < SUMMARY_REFRESH_NANOS) {
			return;
		}
		lastSummaryAtNanos = now;
		if (entities) {
			cachedEntitySummary = entitySummary(client);
		}
		if (configStats) {
			cachedConfigSummary = configSummary();
		}
	}

	// 附近实体分类统计：生物（含玩家）/ 掉落物 / 其他（弹射物、经验球等）。
	private static String entitySummary(Minecraft client) {
		AABB box = client.player.getBoundingBox().inflate(ENTITY_SCAN_RADIUS);
		int mobs = 0;
		int items = 0;
		int others = 0;
		for (Entity entity : client.level.getEntities(client.player, box, e -> !e.isRemoved())) {
			if (entity instanceof ItemEntity) {
				items++;
			} else if (entity instanceof LivingEntity) {
				mobs++;
			} else {
				others++;
			}
		}
		return "Entities: " + mobs + " mobs, " + items + " items, " + others + " other";
	}

	// 只统计“功能”标签的布尔开关：含根功能与它们的子开关，不含测试/修改类开关。
	private static String configSummary() {
		int enabled = 0;
		int changed = 0;
		for (IConfigBoolean toggle : TweakifyModConfigOptions.TOGGLE_OPTIONS) {
			ConfigCatalog.Tab tab = ConfigCatalog.tabOf(toggle.getName());
			if (tab != ConfigCatalog.Tab.FUNCTION && tab != ConfigCatalog.Tab.FUNCTION_AND_GENERAL) {
				continue;
			}
			boolean value = toggle.getBooleanValue();
			if (value) {
				enabled++;
			}
			if (value != toggle.getDefaultBooleanValue()) {
				changed++;
			}
		}
		return "Functions: " + enabled + " on, " + changed + " changed";
	}

	// 药水效果数量与最短剩余时间；全部为无尽效果时单独标注。
	private static String effectSummary(Minecraft client) {
		Collection<MobEffectInstance> effects = client.player.getActiveEffects();
		if (effects.isEmpty()) {
			return "Effects: none";
		}
		int shortestTicks = Integer.MAX_VALUE;
		for (MobEffectInstance effect : effects) {
			if (!effect.isInfiniteDuration()) {
				shortestTicks = Math.min(shortestTicks, effect.getDuration());
			}
		}
		if (shortestTicks == Integer.MAX_VALUE) {
			return "Effects: " + effects.size() + " (all infinite)";
		}
		return "Effects: " + effects.size() + " (min "
			+ EffectDurationFormatter.format(shortestTicks) + ")";
	}

	private static void appendHookLines(List<String> lines) {
		List<String> hooks = HookProbe.reportedHooks();
		if (hooks.isEmpty()) {
			lines.add("Hooks: (none yet)");
			return;
		}
		lines.add("Hooks: " + hooks.size());
		int shown = Math.min(hooks.size(), MAX_HOOK_LINES);
		for (int i = 0; i < shown; i++) {
			lines.add(" - " + hooks.get(i));
		}
		if (hooks.size() > shown) {
			lines.add(" - ...(+" + (hooks.size() - shown) + ")");
		}
	}
}
