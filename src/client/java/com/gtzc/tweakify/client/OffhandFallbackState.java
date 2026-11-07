package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class OffhandFallbackState {
	private static final KeyHoldSession SESSION = new KeyHoldSession();

	// 只在类内更新；读取必须走 shouldBlockOffhandFallback。
	private static boolean blockThisTick = false;

	// 写入时所属的 tick 序号：与 TickClock 不一致时标记自动失效。
	private static long markedTick = Long.MIN_VALUE;

	private OffhandFallbackState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.blockOffhandFallback || client.player == null) {
			reset();
			return;
		}

		boolean useDown = client.options.keyUse.isDown();
		boolean mainHandHasItem = !client.player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();

		// 按下瞬间记录主手当时是否有物品；松开复位；按住期间保持。
		boolean mainHandHadItem = SESSION.update(useDown, mainHandHasItem);

		ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack offHand = client.player.getItemInHand(InteractionHand.OFF_HAND);

		// 拦截窗口：按住期间主手从“有物品”变空，且副手物品在受限列表中。
		// 只有窗口成立时才查受限列表，避免每 tick 都做一次注册表查找。
		boolean mainHandEmpty = mainHand.isEmpty();
		boolean restricted = useDown && mainHandHadItem && mainHandEmpty
			&& config.isOffhandRestricted(offHand.getItem());
		updateFromInput(useDown, mainHandHadItem, mainHandEmpty, restricted);
	}

	// 包私有：真实 tick 与测试共用同一条状态更新路径，只把 Minecraft 输入采集留在 tick 外层。
	// 配置判定不在这里做，shouldBlockOffhandFallback 仍会重新校验开关，测试到的就是真实行为。
	static void updateFromInput(boolean useDown, boolean mainHandHadItem,
								boolean mainHandEmpty, boolean restricted) {
		blockThisTick = computeBlockThisTick(useDown, mainHandHadItem, mainHandEmpty, restricted);
		markedTick = TickClock.current();
	}

	// 包私有：测试之间重置状态，避免共享静态字段互相影响。
	static void resetForTest() {
		reset();
	}

	// 副手回退拦截：总开关关闭或标记不属于当前 tick 时不拦截。
	public static boolean shouldBlockOffhandFallback() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.blockOffhandFallback
			&& blockThisTick && markedTick == TickClock.current();
	}

	private static void reset() {
		SESSION.reset();
		blockThisTick = false;
		markedTick = Long.MIN_VALUE;
	}

	// 纯拦截判定：与游戏状态无关，便于单元测试。
	static boolean computeBlockThisTick(boolean useDown, boolean mainHandHadItem,
										boolean mainHandEmpty, boolean restricted) {
		return useDown && mainHandHadItem && mainHandEmpty && restricted;
	}

	// startUseItem 的命中目标分类，便于把“是否取消”写成纯函数。
	public enum HitTarget {
		MISS,
		BLOCK,
		ENTITY
	}

	// 纯判定：当前命中目标是否需要取消本次使用。
	// 无命中（MISS）始终取消——那正是副手物品使用（吃、拉弓等）的入口；
	// 实体交互由实体开关控制。
	// 方块交互开关开启时一律拦截；关闭时仍拦截副手方块物品对方块的放置，
	// 只放行门、拉杆等可交互方块的交互（此时放置本就会失败，交互自然落到方块上）。
	public static boolean shouldCancelHit(HitTarget target, boolean interceptBlockInteraction,
										  boolean interceptEntityInteraction, boolean offhandIsBlockItem,
										  boolean hitBlockInteractive) {
		if (target == null) {
			return true;
		}
		return switch (target) {
			case ENTITY -> interceptEntityInteraction;
			case BLOCK -> interceptBlockInteraction || (offhandIsBlockItem && !hitBlockInteractive);
			case MISS -> true;
		};
	}

	// 判断命中的方块是否“可交互”（门、拉杆、按钮、箱子等）。
	// 带方块实体的方块（箱子、熔炉等）一律视为可交互；其余按类型表判断，新增类型只改这张表。
	private static final List<Class<?>> INTERACTIVE_BLOCK_TYPES = List.of(
		DoorBlock.class,
		TrapDoorBlock.class,
		LeverBlock.class,
		ButtonBlock.class,
		BasePressurePlateBlock.class,
		FenceGateBlock.class,
		NoteBlock.class,
		CraftingTableBlock.class,
		JukeboxBlock.class,
		CommandBlock.class,
		StructureBlock.class,
		CakeBlock.class,
		RespawnAnchorBlock.class,
		AnvilBlock.class,
		CartographyTableBlock.class,
		SmithingTableBlock.class,
		LoomBlock.class,
		GrindstoneBlock.class,
		StonecutterBlock.class,
		ComposterBlock.class,
		FlowerPotBlock.class,
		AbstractCauldronBlock.class,
		RepeaterBlock.class,
		ComparatorBlock.class,
		DragonEggBlock.class,
		SweetBerryBushBlock.class);

	public static boolean isInteractiveBlock(BlockState state) {
		if (state == null) {
			return false;
		}
		if (state.hasBlockEntity()) {
			return true;
		}
		for (Class<?> type : INTERACTIVE_BLOCK_TYPES) {
			if (type.isInstance(state.getBlock())) {
				return true;
			}
		}
		return false;
	}
}
