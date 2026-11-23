package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 爆炸范围：收集附近即将爆炸的 TNT、已激活的 TNT 矿车、苦力怕与末地水晶，
 * 按原版爆炸威力换算成以实体为中心的红色范围。渲染沿用攻击范围的平面/球体与线框/实心模式。
 *
 * <p>刷新频率 6 次/秒；范围条目与攻击范围共用 {@link AttackRangeState.Entry}，
 * 但使用独立的顶点缓存，避免与攻击范围的颜色/内容互相顶掉。
 */
public final class ExplosionRangeState {
	// 原版爆炸半径约为威力 × 2：TNT 威力 4，普通/充能苦力怕 3/6，末地水晶 6。
	static final float TNT_RADIUS = 8.0f;
	static final float CREEPER_RADIUS = 6.0f;
	static final float CHARGED_CREEPER_RADIUS = 12.0f;
	static final float END_CRYSTAL_RADIUS = 12.0f;
	private static final int REFRESHES_PER_SECOND = 6;
	private static final RefreshScheduler REFRESH = new RefreshScheduler(REFRESHES_PER_SECOND);
	// 附近 TNT 方块的扫描半径：方块不是实体，需要单独的轻量扫描；只扫近处，避免扩大每 tick 开销。
	private static final int TNT_BLOCK_SCAN_RADIUS = 12;

	private static List<AttackRangeState.Entry> entries = List.of();
	private static Integer publishedHash;
	private static boolean wasEnabled;
	// 首次产生条目时记录一次，便于区分配置/状态未生效与渲染不可见。
	private static boolean loggedActive;

	private ExplosionRangeState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.functions.explosionRangeDisplay
			&& client.player != null && client.level != null;
		if (!enabled) {
			resetState();
			return;
		}
		boolean refreshDue = REFRESH.tick();
		if (!refreshDue && wasEnabled) {
			return;
		}
		wasEnabled = true;
		// 扫描距离与数量上限复用攻击范围的配置，保持两种可视化手感一致。
		double maxDistance = Mth.clamp(config.functions.attackRangeMaxDistance,
			TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE, TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE);
		int maxTargets = Mth.clamp(config.functions.attackRangeMaxTargets,
			TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS, TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS);
		AABB searchBox = client.player.getBoundingBox().inflate(maxDistance);
		double maxDistanceSq = maxDistance * maxDistance;
		List<AttackRangeState.Entry> found = new ArrayList<>();
		for (Entity entity : client.level.getEntities(client.player, searchBox,
			e -> (config.functions.explosionRangeShowTnt && e instanceof PrimedTnt)
				|| (config.functions.explosionRangeShowTntMinecarts && e instanceof MinecartTNT)
				|| (config.functions.explosionRangeShowCreepers && e instanceof Creeper)
				|| (config.functions.explosionRangeShowEndCrystals && e instanceof EndCrystal))) {
			if (!entity.isAlive() || entity.isRemoved()) {
				continue;
			}
			float radius = radiusOf(entity);
			if (radius <= 0.0f) {
				continue;
			}
			double distanceSq = entity.distanceToSqr(client.player);
			if (distanceSq > maxDistanceSq) {
				continue;
			}
			double x = entity.getX();
			double feetY = entity.getY() + 0.05;
			double centerY = entity.getY() + entity.getBbHeight() / 2.0;
			double z = entity.getZ();
			AABB bounds = new AABB(x - radius, centerY - radius, z - radius,
				x + radius, centerY + radius, z + radius);
			found.add(new AttackRangeState.Entry(x, feetY, centerY, z, radius, bounds, distanceSq));
		}
		if (config.functions.explosionRangeShowTnt) {
			addTntBlocks(client, found, maxDistanceSq);
		}
		publishEntries(found, maxTargets);
		if (!loggedActive && !entries.isEmpty()) {
			loggedActive = true;
			TweakifyMod.LOGGER.info("Explosion range active: {} markers", entries.size());
		}
	}

	// TNT 方块没有实体生命周期：在玩家周围有限半径内扫描，避免放置后“看不到任何效果”。
	private static void addTntBlocks(Minecraft client, List<AttackRangeState.Entry> found,
									 double maxDistanceSq) {
		BlockPos center = BlockPos.containing(client.player.getX(), client.player.getY(), client.player.getZ());
		BlockPos min = center.offset(-TNT_BLOCK_SCAN_RADIUS, -TNT_BLOCK_SCAN_RADIUS, -TNT_BLOCK_SCAN_RADIUS);
		BlockPos max = center.offset(TNT_BLOCK_SCAN_RADIUS, TNT_BLOCK_SCAN_RADIUS, TNT_BLOCK_SCAN_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = client.level.getBlockState(pos);
			if (!state.is(Blocks.TNT)) {
				continue;
			}
			double x = pos.getX() + 0.5;
			double y = pos.getY();
			double z = pos.getZ() + 0.5;
			double distanceSq = client.player.distanceToSqr(x, y + 0.5, z);
			if (distanceSq > maxDistanceSq) {
				continue;
			}
			double centerY = y + 0.5;
			AABB bounds = new AABB(x - TNT_RADIUS, centerY - TNT_RADIUS, z - TNT_RADIUS,
				x + TNT_RADIUS, centerY + TNT_RADIUS, z + TNT_RADIUS);
			found.add(new AttackRangeState.Entry(x, y + 0.05, centerY, z, TNT_RADIUS, bounds, distanceSq));
		}
	}

	// 苦力怕显示潜在爆炸范围（不需要先点燃）；TNT 矿车只在已激活时显示。
	static float radiusOf(Entity entity) {
		if (entity instanceof PrimedTnt) {
			return TNT_RADIUS;
		}
		if (entity instanceof MinecartTNT minecart) {
			return minecart.isPrimed() ? TNT_RADIUS : 0.0f;
		}
		if (entity instanceof Creeper creeper) {
			return creeper.isPowered() ? CHARGED_CREEPER_RADIUS : CREEPER_RADIUS;
		}
		if (entity instanceof EndCrystal) {
			return END_CRYSTAL_RADIUS;
		}
		return 0.0f;
	}

	static void publishEntries(List<AttackRangeState.Entry> found, int maxTargets) {
		found.sort(AttackRangeState.BY_DISTANCE);
		List<AttackRangeState.Entry> visible = found.subList(0, Math.min(found.size(), maxTargets));
		int hash = AttackRangeState.contentHash(visible);
		if (Objects.equals(publishedHash, hash) && AttackRangeState.sameGeometry(entries, visible)) {
			return;
		}
		publishedHash = hash;
		entries = List.copyOf(visible);
	}

	public static List<AttackRangeState.Entry> entries() {
		return entries;
	}

	static void resetForTest() {
		resetState();
	}

	private static void resetState() {
		entries = List.of();
		publishedHash = null;
		wasEnabled = false;
		loggedActive = false;
		REFRESH.reset();
	}
}
