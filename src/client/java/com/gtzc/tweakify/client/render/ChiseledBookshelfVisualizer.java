package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.FunctionConfig;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雕刻书架可视化（参考 Chiseled Bookshelf Visualizer）：
 * 准星指向雕刻书架时，在准星下方按槽位显示 6 格书本内容。
 *
 * <p>书本内容本身不在原版同步数据里（方块状态只同步「该格是否有书」），因此：
 * <ul>
 *   <li>单人/局域网主机：tick 中把读取请求丢给集成服务端线程，读到后发布为不可变快照；</li>
 *   <li>远程服务器：退回客户端已知的区块加载副本，只对已加载过的格子有效；</li>
 *   <li>两处都没有内容时，已占用格显示为「内容未知」而不是留空。</li>
 * </ul>
 */
public final class ChiseledBookshelfVisualizer {
	public static final String UNKNOWN_CONTENT_KEY = "tweakify-mod.gui.chiseled_bookshelf_visualizer.unknown";

	private static final int SLOT_COUNT = 6;
	private static final int SLOT_SIZE = 18;
	private static final int SLOT_GAP = 2;
	private static final int LABEL_GAP = 4;
	private static final int LABEL_LINE_HEIGHT = 10;
	// 集成服务端数据的最短刷新间隔（tick）：避免每 tick 都往服务端线程丢任务。
	private static final int SERVER_REFRESH_INTERVAL_TICKS = 20;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int ENCHANTMENT_COLOR = 0xFFC0C0C0;
	private static final int SLOT_BACKGROUND_OCCUPIED = 0x80303030;
	private static final int SLOT_BACKGROUND_EMPTY = 0x80202020;
	private static final int HOVER_BORDER_COLOR = 0xFFFFFFFF;
	private static final int UNKNOWN_COLOR = 0xFFB0B0B0;

	// 当前准星目标（客户端线程每 tick 写入，渲染帧读取）。
	private static volatile Target target = Target.EMPTY;
	// 集成服务端回填的内容：pos 与当前目标一致时优先使用。
	private static volatile ServerSnapshot serverSnapshot = ServerSnapshot.EMPTY;
	// 会话代际：断线/换世界时递增，丢弃仍排在服务端队列里的旧读取结果。
	private static final AtomicLong GENERATION = new AtomicLong();

	private static long tickCounter;
	private static BlockPos lastRequestedPos;
	private static int lastRequestedSignature = -1;
	private static long lastRequestTick;

	private ChiseledBookshelfVisualizer() {
	}

	// 槽位内容 + 占用状态的不可变快照。
	private record Target(BlockPos pos, int hoveredSlot, ItemStack[] stacks, boolean[] occupied) {
		static final Target EMPTY = new Target(null, -1, new ItemStack[0], new boolean[0]);
	}

	private record ServerSnapshot(BlockPos pos, ItemStack[] stacks) {
		static final ServerSnapshot EMPTY = new ServerSnapshot(null, new ItemStack[0]);
	}

	// Fabric HUD 元素入口：在 TweakifyModClient 里用 HudElementRegistry.addLast 注册。
	public static void renderElement(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		render(graphics, Minecraft.getInstance());
	}

	// 每 tick 解析一次准星目标：渲染帧只读快照，不做射线检测。
	public static void tick(Minecraft client) {
		tickCounter++;
		TweakifyModConfig config = TweakifyMod.config();
		FunctionConfig functions = config.functions;
		if (!functions.chiseledBookshelfVisualizer || client.level == null || client.player == null
			|| client.gui == null || client.gui.hud.isHidden()) {
			target = Target.EMPTY;
			return;
		}
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			target = Target.EMPTY;
			return;
		}
		HitResult hit = camera.pick(functions.chiseledBookshelfVisualizerMaxDistance, 0.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			target = Target.EMPTY;
			return;
		}
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = client.level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChiseledBookShelfBlock bookshelf)) {
			target = Target.EMPTY;
			return;
		}
		boolean[] occupied = occupiedSlots(state);
		OptionalInt hovered = bookshelf.getHitSlot(blockHit, state.getValue(ChiseledBookShelfBlock.FACING));
		ItemStack[] stacks = knownStacks(client, pos);
		target = new Target(pos.immutable(), hovered.isPresent() ? hovered.getAsInt() : -1,
			stacks, occupied);
		requestServerSnapshot(client, pos, occupied);
	}

	private static boolean[] occupiedSlots(BlockState state) {
		boolean[] occupied = new boolean[SLOT_COUNT];
		List<BooleanProperty> properties = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES;
		for (int slot = 0; slot < SLOT_COUNT && slot < properties.size(); slot++) {
			occupied[slot] = state.getValue(properties.get(slot));
		}
		return occupied;
	}

	// 优先使用集成服务端回填的副本；没有时读客户端方块实体（区块加载时同步过来的数据）。
	private static ItemStack[] knownStacks(Minecraft client, BlockPos pos) {
		ServerSnapshot snapshot = serverSnapshot;
		if (snapshot.pos != null && snapshot.pos.equals(pos)) {
			return snapshot.stacks;
		}
		if (!(client.level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) {
			return emptyStacks();
		}
		return copyOf(shelf.getItems());
	}

	private static ItemStack[] copyOf(NonNullList<ItemStack> items) {
		ItemStack[] stacks = new ItemStack[SLOT_COUNT];
		for (int slot = 0; slot < SLOT_COUNT; slot++) {
			stacks[slot] = slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
		}
		return stacks;
	}

	private static ItemStack[] emptyStacks() {
		ItemStack[] stacks = new ItemStack[SLOT_COUNT];
		Arrays.fill(stacks, ItemStack.EMPTY);
		return stacks;
	}

	// 单人/局域网主机：把读取请求交给集成服务端线程，读到的内容整体替换后发布。
	private static void requestServerSnapshot(Minecraft client, BlockPos pos, boolean[] occupied) {
		MinecraftServer server = client.getSingleplayerServer();
		if (server == null || client.level == null) {
			return;
		}
		ServerLevel level = server.getLevel(client.level.dimension());
		if (level == null) {
			return;
		}
		int signature = signatureOf(occupied);
		boolean sameTarget = pos.equals(lastRequestedPos);
		if (sameTarget && signature == lastRequestedSignature
			&& tickCounter - lastRequestTick < SERVER_REFRESH_INTERVAL_TICKS) {
			return;
		}
		lastRequestedPos = pos.immutable();
		lastRequestedSignature = signature;
		lastRequestTick = tickCounter;
		BlockPos immutablePos = lastRequestedPos;
		long generation = GENERATION.get();
		server.execute(() -> {
			// 会话已切换（断线/换世界）：旧结果不能再发布给新世界。
			if (GENERATION.get() != generation) {
				return;
			}
			try {
				if (!(level.getBlockEntity(immutablePos) instanceof ChiseledBookShelfBlockEntity shelf)) {
					return;
				}
				NonNullList<ItemStack> items = shelf.getItems();
				ItemStack[] stacks = new ItemStack[SLOT_COUNT];
				for (int slot = 0; slot < SLOT_COUNT; slot++) {
					stacks[slot] = slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY;
				}
				serverSnapshot = new ServerSnapshot(immutablePos, stacks);
			} catch (RuntimeException error) {
				// 方块实体在关卡卸载阶段不可访问时不能把异常漏到服务端线程。
				TweakifyMod.LOGGER.warn("Failed to read chiseled bookshelf contents", error);
			}
		});
	}

	private static int signatureOf(boolean[] occupied) {
		int signature = 0;
		for (int slot = 0; slot < occupied.length; slot++) {
			if (occupied[slot]) {
				signature |= 1 << slot;
			}
		}
		return signature;
	}

	private static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.config();
		FunctionConfig functions = config.functions;
		if (!functions.chiseledBookshelfVisualizer || client.font == null) {
			return;
		}
		Target current = target;
		if (current.pos == null) {
			return;
		}
		if (client.gui == null || client.gui.hud.isHidden() || hidesPanel(client.gui.screen())) {
			return;
		}
		int centerX = graphics.guiWidth() / 2;
		int centerY = graphics.guiHeight() / 2;
		float scale = functions.chiseledBookshelfVisualizerScale / 100.0F;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		// 以准星为中心缩放：面板大小变化时仍保持居中。
		pose.translate(centerX, centerY);
		pose.scale(scale, scale);
		pose.translate(-centerX, -centerY);
		try {
			drawPanel(graphics, client, functions, current, centerX, centerY + SLOT_SIZE + 2);
		} finally {
			pose.popMatrix();
		}
	}

	// 聊天界面与容器界面保留面板，其它界面（ESC 菜单等）隐藏。
	static boolean hidesPanel(Screen screen) {
		return screen != null
			&& !(screen instanceof ChatScreen)
			&& !(screen instanceof AbstractContainerScreen);
	}

	private static void drawPanel(GuiGraphicsExtractor graphics, Minecraft client, FunctionConfig functions,
			Target current, int centerX, int rowY) {
		boolean allSlots = functions.chiseledBookshelfVisualizerShowAllSlots;
		int firstSlot = allSlots ? 0 : Math.max(0, current.hoveredSlot);
		int count = allSlots ? SLOT_COUNT : (current.hoveredSlot >= 0 ? 1 : 0);
		if (count == 0) {
			return;
		}
		int totalWidth = count * SLOT_SIZE + (count - 1) * SLOT_GAP;
		int startX = centerX - totalWidth / 2;
		for (int index = 0; index < count; index++) {
			int slot = firstSlot + index;
			if (slot < 0 || slot >= SLOT_COUNT) {
				continue;
			}
			int x = startX + index * (SLOT_SIZE + SLOT_GAP);
			boolean occupied = current.occupied.length > slot && current.occupied[slot];
			graphics.fill(x, rowY, x + SLOT_SIZE, rowY + SLOT_SIZE,
				occupied ? SLOT_BACKGROUND_OCCUPIED : SLOT_BACKGROUND_EMPTY);
			if (slot == current.hoveredSlot) {
				graphics.outline(x - 1, rowY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, HOVER_BORDER_COLOR);
			}
			ItemStack stack = current.stacks.length > slot ? current.stacks[slot] : ItemStack.EMPTY;
			if (!stack.isEmpty()) {
				graphics.item(stack, x + 1, rowY + 1);
			} else if (occupied) {
				graphics.centeredText(client.font, "?", x + SLOT_SIZE / 2, rowY + 5, UNKNOWN_COLOR);
			}
		}
		drawSlotLabel(graphics, client, functions, current, centerX, rowY + SLOT_SIZE + LABEL_GAP);
	}

	// 准星所指格子的书本名称；附魔书按子设置在名称下方追加附魔行。
	private static void drawSlotLabel(GuiGraphicsExtractor graphics, Minecraft client, FunctionConfig functions,
			Target current, int centerX, int labelY) {
		int slot = current.hoveredSlot;
		if (slot < 0 || slot >= SLOT_COUNT) {
			return;
		}
		ItemStack stack = current.stacks.length > slot ? current.stacks[slot] : ItemStack.EMPTY;
		boolean occupied = current.occupied.length > slot && current.occupied[slot];
		List<Component> lines = new ArrayList<>();
		if (!stack.isEmpty()) {
			lines.add(stack.getHoverName());
			if (functions.chiseledBookshelfVisualizerShowEnchantments) {
				appendEnchantmentLines(stack, lines);
			}
		} else if (occupied) {
			lines.add(Component.translatable(UNKNOWN_CONTENT_KEY));
		}
		int y = labelY;
		for (Component line : lines) {
			int color = y == labelY ? NAME_COLOR : ENCHANTMENT_COLOR;
			graphics.centeredText(client.font, line, centerX, y, color);
			y += LABEL_LINE_HEIGHT;
		}
	}

	private static void appendEnchantmentLines(ItemStack stack, List<Component> lines) {
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored != null && !stored.isEmpty()) {
			appendEnchantmentLines(stored, lines);
			return;
		}
		ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
		if (enchantments != null && !enchantments.isEmpty()) {
			appendEnchantmentLines(enchantments, lines);
		}
	}

	private static void appendEnchantmentLines(ItemEnchantments enchantments, List<Component> lines) {
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			lines.add(Enchantment.getFullname(enchantment, enchantments.getLevel(enchantment)));
		}
	}

	// 断线时清空快照，避免回到标题界面后残留上一次会话的书架内容。
	public static void clear() {
		GENERATION.incrementAndGet();
		target = Target.EMPTY;
		serverSnapshot = ServerSnapshot.EMPTY;
		lastRequestedPos = null;
		lastRequestedSignature = -1;
		lastRequestTick = 0L;
	}
}
