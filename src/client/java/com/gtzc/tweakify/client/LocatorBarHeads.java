package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * 定位栏头像，严格对齐 {@code Haage001/locator-heads}：不自己算摆放坐标，
 * 而是把原版圆点的贴图调用整个替换掉，因此头像沿用圆点自身的 x/y（9x9 槽位）
 * 与“按距离缩放”的贴图尺寸；过滤命中或只显示附近时，整条航点都不绘制（圆点也隐藏）。
 *
 * <p>名字与参考实现一致：基准位置在圆点上方 {@code 12 * 倍率} 像素，从下方 20 像素滑入，
 * 透明度上限 0xBF，150ms 三次缓出；颜色跟随描边样式（队伍色 / 固定色 / 白色）。
 *
 * <p>尺寸倍率与参考实现同一张表：1..9 对应 0.5x / 0.6x / 0.7x / 0.8x / 1.0x / 1.1x / 1.2x / 1.3x / 1.5x。
 */
public final class LocatorBarHeads {
	/** 一次圆点绘制的处理结果。 */
	public enum DotResult {
		/** 隐藏该航点（过滤或“只显示附近”命中）。 */
		HIDE,
		/** 退回原版圆点。 */
		VANILLA,
		/** 已经画成头像。 */
		HEAD
	}

	// 参考实现：名字动画 150ms、从下往上滑 20 像素、基准在圆点上方 12 像素。
	private static final int ANIMATION_DURATION_MS = 150;
	private static final float NAME_SLIDE_PIXELS = 20.0F;
	private static final double NAME_BASE_OFFSET = 12.0;
	private static final int NAME_ALPHA = 0xBF;
	// 参考实现把头像与描边放进 0.01 缩放的坐标系，坐标整体放大 100 倍。
	private static final float POSE_SCALE = 0.01F;
	private static final int POSE_UNIT = 100;
	// 原版圆点固定 9x9，参考实现用 x*100+450 求圆心。
	private static final int DOT_SIZE = 9;
	private static final int DOT_HALF_UNIT = DOT_SIZE * POSE_UNIT / 2;
	// 皮肤图集：头正面 (8,8)-(16,16)，帽子层 (40,8)-(48,16)。
	private static final float HEAD_U1 = 1.0F / 8.0F;
	private static final float HEAD_U2 = 2.0F / 8.0F;
	private static final float HEAD_V1 = 1.0F / 8.0F;
	private static final float HEAD_V2 = 2.0F / 8.0F;
	private static final float HAT_U1 = 5.0F / 8.0F;
	private static final float HAT_U2 = 6.0F / 8.0F;
	// “注视”判定：视线与目标方向夹角 20 度以内。
	private static final double LOOK_ANGLE_COS = Math.cos(Math.toRadians(20.0));
	// 名单分隔符：与参考实现一致（逗号 / 分号 / 冒号 / 句点）。
	private static final String FILTER_SEPARATORS = "[,;:.]";
	// 无描边时使用的哨兵值。
	private static final int NO_BORDER = -1;
	// 参考实现按玩家名记录名字动画状态。
	private static final Map<String, Long> NAME_FADE_START = new HashMap<>();
	private static final Map<String, Boolean> NAME_FADE_IN = new HashMap<>();
	// 名单解析缓存：配置列表按内容比较失效，避免每帧对每个航点重新 split/trim/toLowerCase。
	private static final NameListCache INCLUDED_NAMES = new NameListCache();
	private static final NameListCache EXCLUDED_NAMES = new NameListCache();
	// 玩家名宽度缓存：Font 实例在资源重载后会替换，按实例身份整体失效。
	private static final WidthCache NAME_WIDTHS = new WidthCache();
	// 宽度缓存条目上限：在线玩家数量有限，超出时只淘汰一半，避免整表清空造成集中重算。
	static final int MAX_NAME_WIDTH_ENTRIES = 256;

	private LocatorBarHeads() {
	}

	/**
	 * 替换原版圆点：返回值决定调用方是隐藏该航点、退回原版贴图，还是保留已画好的头像。
	 *
	 * @param x      原版圆点的左上角 x（9x9 槽位）
	 * @param y      原版圆点的左上角 y
	 * @param width  原版圆点宽度（恒为 9，保留参数以便与原版签名一致）
	 * @param height 原版圆点高度
	 */
	public static DotResult renderDot(GuiGraphicsExtractor graphics, Minecraft client, Entity camera, Level level,
									  TrackedWaypoint waypoint, int x, int y, int width, int height) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.locatorBarHeads) {
			return DotResult.VANILLA;
		}
		if (client.player == null || client.level == null || camera == null || level == null) {
			return DotResult.HIDE;
		}
		UUID owner = waypoint.id().left().orElse(null);
		if (owner == null || client.getConnection() == null) {
			// 非玩家航点：保持原版图标。
			return DotResult.VANILLA;
		}
		PlayerInfo info = client.getConnection().getPlayerInfo(owner);
		if (info == null) {
			return DotResult.VANILLA;
		}
		String playerName = info.getProfile().name();

		String filterMode = config.functions.locatorBarHeadFilter;
		// 与参考实现一致：INCLUDE 只看包含名单，EXCLUDE 只看排除名单，ALL 不解析任何名单。
		if (!matchesFilter(config, filterMode, playerName)) {
			// 参考实现：过滤命中时整条航点都不画，圆点一并隐藏。
			return DotResult.HIDE;
		}

		boolean needsTarget = config.functions.locatorBarHeadNearbyOnly
			|| TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT.equals(config.functions.locatorBarHeadNames);
		Player target = needsTarget ? level.getPlayerByUUID(owner) : null;
		if (config.functions.locatorBarHeadNearbyOnly && target == null) {
			return DotResult.HIDE;
		}

		PlayerSkin skin = info.getSkin();
		if (skin == null) {
			return DotResult.VANILLA;
		}
		Integer teamRgb = teamRgb(level, playerName);
		drawHead(graphics, client, camera, waypoint, x, y, width, height, skin.body().texturePath(),
			teamRgb, config, playerName, target, info.showHat());
		return DotResult.HEAD;
	}

	// 与参考实现同一张倍率表：5 = 1.0x。
	static double headSizeMultiplier(int index) {
		return switch (index) {
			case 1 -> 0.5;
			case 2 -> 0.6;
			case 3 -> 0.7;
			case 4 -> 0.8;
			case 6 -> 1.1;
			case 7 -> 1.2;
			case 8 -> 1.3;
			case 9 -> 1.5;
			default -> 1.0;
		};
	}

	private static void drawHead(GuiGraphicsExtractor graphics, Minecraft client, Entity camera,
								 TrackedWaypoint waypoint, int x, int y, int width, int height,
								 Identifier skin, Integer teamRgb, TweakifyModConfig config,
								 String playerName, Player target, boolean showHat) {
		// 参考实现 Render Heads：关闭时整条圆点隐藏，只保留名字。
		boolean renderHeads = config.functions.locatorBarHeadRenderHeads;
		// 贴图尺寸沿用原版“按距离插值”的算法，再乘上配置倍率。
		WaypointStyle style = client.gui.hud.getWaypointStyles().get(waypoint.icon().style);
		float near = style.nearDistance();
		float far = style.farDistance();
		float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
		float progress = 1.0F - Mth.clamp((distance - near) / (far - near), 0.0F, 1.0F);
		double multiplier = headSizeMultiplier(config.functions.locatorBarHeadScale);
		int scaledWidth = (int) (Mth.lerpInt(progress, 4 * POSE_UNIT + POSE_UNIT, width * POSE_UNIT) * multiplier);
		int scaledHeight = (int) (Mth.lerpInt(progress, 4 * POSE_UNIT + POSE_UNIT, height * POSE_UNIT) * multiplier);
		int borderRgb = resolveBorderRgb(config, teamRgb);
		int nameRgb = resolveNameRgb(config, teamRgb);

		if (renderHeads) {
			graphics.pose().pushMatrix();
			graphics.pose().scale(POSE_SCALE);
			int centerX = (x * POSE_UNIT + DOT_HALF_UNIT) - (scaledWidth / 2);
			int centerY = (y * POSE_UNIT + DOT_HALF_UNIT) - (scaledHeight / 2);
			if (borderRgb != NO_BORDER) {
				int thickness = (int) (TweakifyModConfig.locatorHeadBorderThicknessPixels(
					config.functions.locatorBarHeadBorderThickness) * POSE_UNIT);
				int argb = 0xFF000000 | borderRgb;
				graphics.fill(centerX - thickness, centerY - thickness,
					centerX + scaledWidth + thickness, centerY, argb);
				graphics.fill(centerX - thickness, centerY + scaledHeight,
					centerX + scaledWidth + thickness, centerY + scaledHeight + thickness, argb);
				graphics.fill(centerX - thickness, centerY, centerX, centerY + scaledHeight, argb);
				graphics.fill(centerX + scaledWidth, centerY,
					centerX + scaledWidth + thickness, centerY + scaledHeight, argb);
			}
			graphics.blit(skin, centerX, centerY, centerX + scaledWidth, centerY + scaledHeight,
				HEAD_U1, HEAD_U2, HEAD_V1, HEAD_V2);
			if (showHat && config.functions.locatorBarHeadHatLayer) {
				graphics.blit(skin, centerX, centerY, centerX + scaledWidth, centerY + scaledHeight,
					HAT_U1, HAT_U2, HEAD_V1, HEAD_V2);
			}
			graphics.pose().popMatrix();
		}

		drawName(graphics, client, config, playerName, target, x, y, width, height, multiplier,
			renderHeads, nameRgb);
	}

	// 名字：与参考实现一致，从下方滑入并淡入；关闭时反向淡出。
	// renderHeads 关闭时名字垂直居中在圆点槽位里（只显名字模式）。
	private static void drawName(GuiGraphicsExtractor graphics, Minecraft client, TweakifyModConfig config,
								 String playerName, Player target, int x, int y, int width, int height,
								 double multiplier, boolean renderHeads, int nameRgb) {
		if (playerName == null) {
			return;
		}
		boolean shouldShow = shouldShowName(config, client, target);
		long now = System.currentTimeMillis();
		boolean animating = NAME_FADE_START.containsKey(playerName);
		if (shouldShow && !animating) {
			NAME_FADE_START.put(playerName, now);
			NAME_FADE_IN.put(playerName, true);
		} else if (!shouldShow && animating && Boolean.TRUE.equals(NAME_FADE_IN.get(playerName))) {
			// 从当前进度反向淡出，避免突然跳回满透明度再消失。
			long start = NAME_FADE_START.getOrDefault(playerName, now);
			float shown = Math.min(1.0F, (now - start) / (float) ANIMATION_DURATION_MS);
			NAME_FADE_START.put(playerName, now - (long) ((1.0F - shown) * ANIMATION_DURATION_MS));
			NAME_FADE_IN.put(playerName, false);
		}
		if (!shouldShow && !animating) {
			return;
		}
		boolean fadeIn = NAME_FADE_IN.getOrDefault(playerName, Boolean.TRUE);
		long start = NAME_FADE_START.getOrDefault(playerName, now);
		float progress = Math.min(1.0F, (now - start) / (float) ANIMATION_DURATION_MS);
		float effective = fadeIn ? progress : 1.0F - progress;
		float eased = 1.0F - (float) Math.pow(1.0F - effective, 3.0F);

		int textX = x + width / 2 - nameWidth(client.font, playerName) / 2;
		int baseY = renderHeads
			? y - (int) (NAME_BASE_OFFSET * multiplier)
			: y + (height - client.font.lineHeight) / 2;
		int textY = baseY + (int) (NAME_SLIDE_PIXELS * (1.0F - eased));
		int alpha = (int) (NAME_ALPHA * eased);
		if (alpha > 0) {
			graphics.text(client.font, playerName, textX, textY, (alpha << 24) | (nameRgb & 0xFFFFFF), true);
		}
		if (!fadeIn && progress >= 1.0F) {
			NAME_FADE_START.remove(playerName);
			NAME_FADE_IN.remove(playerName);
		}
	}

	// 名字时机：NEVER/ALWAYS 不做任何几何计算，只有 LOOKING_AT 才求视线夹角。
	private static boolean shouldShowName(TweakifyModConfig config, Minecraft client, Player target) {
		String mode = config.functions.locatorBarHeadNames;
		if (TweakifyModConfig.LOCATOR_HEAD_NAMES_NEVER.equals(mode)) {
			return false;
		}
		boolean lookingAt = TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT.equals(mode)
			&& isLookingAt(client, target);
		boolean playerListDown = TweakifyModConfig.LOCATOR_HEAD_NAMES_PLAYER_LIST.equals(mode)
			&& client.options.keyPlayerList.isDown();
		return LocatorHeadNames.shouldShow(mode, lookingAt, playerListDown);
	}

	private static boolean isLookingAt(Minecraft client, Player target) {
		Entity camera = client.getCameraEntity();
		if (target == null || camera == null) {
			return false;
		}
		Vec3 direction = target.position().subtract(camera.position());
		if (direction.lengthSqr() < 1.0E-4) {
			return true;
		}
		return camera.getViewVector(1.0F).dot(direction.normalize()) >= LOOK_ANGLE_COS;
	}

	// 描边颜色：关闭或无有效样式时返回哨兵；跟随队伍但没有队伍时不画描边（与参考实现一致）。
	static int resolveBorderRgb(TweakifyModConfig config, Integer teamRgb) {
		String style = config.functions.locatorBarHeadBorder;
		if (!LocatorHeadBorder.isVisible(style)) {
			return NO_BORDER;
		}
		if (LocatorHeadBorder.followsTeam(style)) {
			return teamRgb == null ? NO_BORDER : teamRgb & 0xFFFFFF;
		}
		if (LocatorHeadBorder.usesStaticColor(style)) {
			return LocatorHeadColor.rgbOf(config.functions.locatorBarHeadBorderColor);
		}
		// 未知取值（已由配置清洗器纠正，这里只是兜底）。
		return NO_BORDER;
	}

	// 名字颜色：跟随描边样式；关闭描边或无队伍时用白色（与参考实现一致）。
	static int resolveNameRgb(TweakifyModConfig config, Integer teamRgb) {
		String style = config.functions.locatorBarHeadBorder;
		if (LocatorHeadBorder.followsTeam(style) && teamRgb != null) {
			return teamRgb & 0xFFFFFF;
		}
		if (LocatorHeadBorder.usesStaticColor(style)) {
			return LocatorHeadColor.rgbOf(config.functions.locatorBarHeadBorderColor);
		}
		return 0xFFFFFF;
	}

	// 队伍颜色：没有队伍或队伍没设颜色时返回 null（调用方回退白色）。
	static Integer teamRgb(Level level, String playerName) {
		if (level == null || playerName == null) {
			return null;
		}
		PlayerTeam team = level.getScoreboard().getPlayersTeam(playerName);
		if (team == null) {
			return null;
		}
		return team.getColor().map(TeamColor::rgb).orElse(null);
	}

	// 名单解析：每个条目内部再按分隔符切开，全部转小写便于忽略大小写比较。
	static List<String> splitNames(List<String> entries) {
		if (entries == null || entries.isEmpty()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (String entry : entries) {
			if (entry == null) {
				continue;
			}
			for (String part : entry.split(FILTER_SEPARATORS)) {
				String value = part.trim();
				if (!value.isEmpty()) {
					names.add(value.toLowerCase(Locale.ROOT));
				}
			}
		}
		return names;
	}

	static boolean containsName(List<String> names, String playerName) {
		if (names.isEmpty() || playerName == null) {
			return false;
		}
		String lower = playerName.toLowerCase(Locale.ROOT);
		for (String name : names) {
			if (name.equals(lower)) {
				return true;
			}
		}
		return false;
	}

	// 过滤判定：INCLUDE 只看包含名单，EXCLUDE 只看排除名单，ALL 不读任何名单（保留原短路）。
	// 名单解析结果按内容缓存，配置列表内容变化时自动失效。
	static boolean matchesFilter(TweakifyModConfig config, String filterMode, String playerName) {
		boolean inIncluded = TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE.equals(filterMode)
			&& containsName(INCLUDED_NAMES.parsed(config.getStringList(
				TweakifyModConfig.LOCATOR_HEAD_INCLUDED_PLAYERS_FIELD)), playerName);
		boolean inExcluded = TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE.equals(filterMode)
			&& containsName(EXCLUDED_NAMES.parsed(config.getStringList(
				TweakifyModConfig.LOCATOR_HEAD_EXCLUDED_PLAYERS_FIELD)), playerName);
		return LocatorHeadFilter.allows(filterMode, inIncluded, inExcluded);
	}

	// 玩家名宽度按 Font 实例与名字缓存：定位栏每帧为每个航点测宽。
	private static int nameWidth(Font font, String playerName) {
		return NAME_WIDTHS.width(font, playerName);
	}

	// 包私有：测试之间清空缓存，避免跨用例互相影响。
	static void resetCachesForTest() {
		INCLUDED_NAMES.reset();
		EXCLUDED_NAMES.reset();
		NAME_WIDTHS.reset();
	}

	// 名单解析结果缓存：内容未变时复用已解析列表，配置变化时重新解析。
	static final class NameListCache {
		private List<String> source = List.of();
		private List<String> parsed = List.of();

		List<String> parsed(List<String> entries) {
			List<String> current = entries == null ? List.of() : entries;
			// List.equals 先比引用：配置未变时只是一次引用比较；
			// 内容变化（替换为新列表）时才重新 split/trim/toLowerCase。
			if (current.equals(this.source)) {
				return this.parsed;
			}
			this.source = List.copyOf(current);
			this.parsed = splitNames(this.source);
			return this.parsed;
		}

		void reset() {
			this.source = List.of();
			this.parsed = List.of();
		}
	}

	// 玩家名宽度缓存：owner（生产为 Font）变化时整体失效；超出上限时淘汰一半。
	static final class WidthCache {
		private Object owner;
		private final Map<String, Integer> widths = new HashMap<>();

		int width(Font font, String name) {
			ensureOwner(font);
			Integer cached = this.widths.get(name);
			if (cached != null) {
				return cached;
			}
			int width = font.width(name);
			store(name, width);
			return width;
		}

		// 测试入口：不依赖真实 Font，用任意 owner 与测量函数走同一条缓存路径。
		int widthForTest(Object owner, String name, ToIntFunction<String> measure) {
			ensureOwner(owner);
			Integer cached = this.widths.get(name);
			if (cached != null) {
				return cached;
			}
			int width = measure.applyAsInt(name);
			store(name, width);
			return width;
		}

		private void ensureOwner(Object owner) {
			if (owner != this.owner) {
				this.owner = owner;
				this.widths.clear();
			}
		}

		private void store(String name, int width) {
			if (this.widths.size() >= MAX_NAME_WIDTH_ENTRIES) {
				// 只淘汰一半：整体清空会让下一帧所有玩家名同时重算，造成周期性抖动。
				Iterator<String> iterator = this.widths.keySet().iterator();
				for (int i = 0; i < MAX_NAME_WIDTH_ENTRIES / 2 && iterator.hasNext(); i++) {
					iterator.next();
					iterator.remove();
				}
			}
			this.widths.put(name, width);
		}

		// 包私有：测试断言淘汰确实发生，而不是只看返回值仍然正确。
		int sizeForTest() {
			return this.widths.size();
		}

		boolean containsForTest(String name) {
			return this.widths.containsKey(name);
		}

		void reset() {
			this.owner = null;
			this.widths.clear();
		}
	}
}
