package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.BrushableBlockEntityAccessor;
import com.gtzc.tweakify.client.mixin.TrialSpawnerStateDataAccessor;
import com.gtzc.tweakify.config.FunctionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.IdentifierException;
import net.minecraft.client.StringSplitter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiscFragmentItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 手持物品信息的提示行构建器（参考 held-item-info）。
 *
 * <p>与原版 HUD 的物品名称使用同一份数据：名称行 + 按开关追加的附魔、容器内容、
 * 告示牌文字等。行数、行宽与各内容开关都从配置读取，默认值与参考实现一致。
 * 构建只在切换快捷栏物品时发生（见 {@link HeldItemInfoRenderer}），渲染帧只读结果。
 */
public final class HeldItemInfoTooltip {
	// 正文默认颜色（与原版提示框一致）。
	public static final ChatFormatting DEFAULT_COLOR = ChatFormatting.GRAY;

	// 折行宽度估算的平均字符宽度。
	public static final double AVERAGE_CHARACTER_WIDTH = 6;

	// 告示牌正反面之间的分隔行。
	private static final Component SIGN_SIDE_SEPARATOR = Component
		.translatable("tweakify-mod.held_item_info.sign_side_separator")
		.withStyle(DEFAULT_COLOR);

	private static final Style LORE_STYLE = Style.EMPTY.withColor(DEFAULT_COLOR).withItalic(true);

	private static final Component UNBREAKABLE_TEXT =
		Component.translatable("item.unbreakable").withStyle(DEFAULT_COLOR);

	// 非法附魔 ID 只提醒一次，避免每次切换物品都刷一条 WARN。
	private static final Set<String> WARNED_INVALID_ENCHANTMENT_IDS = ConcurrentHashMap.newKeySet();

	private final ItemStack stack;
	private final FunctionConfig config;
	private final Item.TooltipContext tooltipContext;
	private final StringSplitter stringSplitter;
	private final boolean textFilteringEnabled;
	private final TooltipDisplay displayComponent;
	private final List<Component> lines;

	// 真实行数：包含因超过 maxLines 被隐藏的行。
	private int realSize;

	private HeldItemInfoTooltip(ItemStack stack, FunctionConfig config, Item.TooltipContext tooltipContext,
			StringSplitter stringSplitter, boolean textFilteringEnabled) {
		this.stack = stack;
		this.config = config;
		this.tooltipContext = tooltipContext;
		this.stringSplitter = stringSplitter;
		this.textFilteringEnabled = textFilteringEnabled;
		this.displayComponent = config.heldItemInfoRespectHideFlags
			? stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
			: TooltipDisplay.DEFAULT;
		this.lines = new ArrayList<>(Math.max(0, config.heldItemInfoMaxLines));
	}

	public static List<Component> build(ItemStack stack, FunctionConfig config,
			Item.TooltipContext tooltipContext, StringSplitter stringSplitter,
			boolean textFilteringEnabled) {
		HeldItemInfoTooltip builder = new HeldItemInfoTooltip(
			stack, config, tooltipContext, stringSplitter, textFilteringEnabled);
		builder.appendAll();
		return builder.build();
	}

	private void appendAll() {
		if (this.config.heldItemInfoShowName) {
			this.append(this.stackName());
		}

		if (!this.displayComponent.hideTooltip()) {
			this.appendItemSpecific();
			this.appendComponents();
		}
	}

	// 与 Hud.extractSelectedItemName 相同的名称样式：稀有度颜色 + 自定义名称斜体。
	private Component stackName() {
		MutableComponent name = Component.empty().append(this.stack.getHoverName())
			.withStyle(this.stack.getRarity().color());
		if (this.stack.has(DataComponents.CUSTOM_NAME)) {
			name.withStyle(ChatFormatting.ITALIC);
		}
		return name;
	}

	// 物品/方块自身的提示（参考实现通过 TooltipAppender 实现，这里改为直接判断类型）。
	private void appendItemSpecific() {
		Item item = this.stack.getItem();

		// 唱片碎片：走物品自身的 appendHoverText（显示曲目名）。
		if (item instanceof DiscFragmentItem && this.config.heldItemInfoShowMusicDiscDescription) {
			this.appendItemHoverText();
		}

		// 画/展示框：走物品自身的 appendHoverText（标题、作者、尺寸），统一改成灰色。
		if (item instanceof HangingEntityItem && this.config.heldItemInfoShowPaintingDescription) {
			this.appendItemHoverText();
		}

		if (item instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();

			// 命令方块：显示方块实体数据里的命令。
			if (block instanceof CommandBlock && this.config.heldItemInfoShowCommandBlockInfo) {
				this.appendCommandBlockCommand();
			}

			// 刷怪笼/试炼刷怪笼：显示将要生成的生物名。
			if ((block instanceof SpawnerBlock || block instanceof TrialSpawnerBlock)
				&& this.config.heldItemInfoShowSpawnerEntity) {
				this.appendSpawnerEntity(block instanceof TrialSpawnerBlock);
			}

			// 可疑的沙子/砾石：显示刷出的物品。
			if (block instanceof BrushableBlock && this.config.heldItemInfoShowContainerContent) {
				this.appendBrushableContent();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void appendItemHoverText() {
		List<Component> tooltip = new ArrayList<>();
		this.stack.getItem().appendHoverText(this.stack, this.tooltipContext,
			this.displayComponent, tooltip::add, TooltipFlag.NORMAL);
		for (Component line : tooltip) {
			this.append(withDefaultColor(line));
		}
	}

	private void appendCommandBlockCommand() {
		CompoundTag data = this.blockEntityData();
		if (data == null) {
			return;
		}
		data.getString("Command").ifPresent(raw -> {
			String command = raw.trim();
			if (command.isEmpty()) {
				return;
			}
			int maxLines = Math.min(this.config.heldItemInfoMaxCommandLines, this.getRemainingLines());
			for (MutableComponent line : this.wrap(command, maxLines)) {
				this.append(line.withStyle(DEFAULT_COLOR));
			}
		});
	}

	private void appendSpawnerEntity(boolean trialSpawner) {
		String spawnDataKey = trialSpawner
			? TrialSpawnerStateDataAccessor.getSpawnDataKey()
			: BaseSpawner.SPAWN_DATA_TAG;
		TypedEntityData<BlockEntityType<?>> data =
			this.getComponentForDisplay(DataComponents.BLOCK_ENTITY_DATA);
		if (data == null) {
			return;
		}
		Component text = Spawner.getSpawnEntityDisplayName(data, spawnDataKey);
		if (text != null) {
			this.append(text);
		}
	}

	// 可疑方块：从方块实体数据里读取 item 标签并显示成“物品 xN”一行。
	private void appendBrushableContent() {
		CompoundTag data = this.blockEntityData();
		if (data == null) {
			return;
		}
		HolderLookup.Provider registries = this.tooltipContext.registries();
		if (registries == null) {
			return;
		}
		try (ProblemReporter.ScopedCollector reporter =
				 new ProblemReporter.ScopedCollector(TweakifyMod.LOGGER)) {
			TagValueInput.create(reporter, registries, data)
				.read(BrushableBlockEntityAccessor.getItemTagKey(), ItemStack.CODEC)
				.ifPresent(this::appendStackLine);
		} catch (RuntimeException e) {
			TweakifyMod.LOGGER.debug("Unable to read brushable block content", e);
		}
	}

	private void appendComponents() {
		// 与参考实现相同的追加顺序：先单项组件，再容器、附魔、描述等。
		if (this.config.heldItemInfoShowEntityBucketContent) {
			this.appendComponent(DataComponents.TROPICAL_FISH_PATTERN);
		}
		if (this.config.heldItemInfoShowGoatHornInstrument) {
			this.appendComponent(DataComponents.INSTRUMENT);
		}
		if (this.config.heldItemInfoShowFilledMapId) {
			this.appendComponent(DataComponents.MAP_ID);
		}
		if (this.config.heldItemInfoShowBeehiveContent) {
			this.appendComponent(DataComponents.BEES);
		}
		if (this.config.heldItemInfoShowContainerContent) {
			this.appendContainerContent();
		}
		if (this.config.heldItemInfoShowBookMeta) {
			this.appendComponent(DataComponents.WRITTEN_BOOK_CONTENT);
		}
		if (this.config.heldItemInfoShowCrossbowProjectiles) {
			HeldItemInfoState.setHidingIntangibleProjectile(true);
			try {
				this.appendTransformedComponent(DataComponents.CHARGED_PROJECTILES,
					HeldItemInfoTooltip::withDefaultColor);
			} finally {
				HeldItemInfoState.setHidingIntangibleProjectile(false);
			}
		}
		if (this.config.heldItemInfoShowFireworkAttributes) {
			this.appendComponent(DataComponents.FIREWORKS);
			this.appendComponent(DataComponents.FIREWORK_EXPLOSION);
		}
		if (this.config.heldItemInfoShowPotionEffects) {
			this.appendPotionEffects();
		}
		if (this.config.heldItemInfoShowMusicDiscDescription) {
			this.appendMusicDiscDescription();
		}
		if (this.config.heldItemInfoShowEnchantments) {
			this.appendEnchantmentComponents();
		}
		if (this.config.heldItemInfoShowLore) {
			this.appendLore();
		}
		if (this.config.heldItemInfoShowUnbreakable) {
			this.appendUnbreakable();
		}
		if (this.config.heldItemInfoShowSignText) {
			this.appendSignText();
		}
		if (this.config.heldItemInfoShowPotionEffects) {
			this.appendComponent(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
		}
		if (this.config.heldItemInfoShowBlockState) {
			this.appendComponent(DataComponents.BLOCK_STATE);
		}
	}

	private void appendContainerContent() {
		this.appendComponent(DataComponents.CONTAINER_LOOT);

		List<ItemStack> contents = new ArrayList<>();
		ItemContainerContents container = this.getComponentForDisplay(DataComponents.CONTAINER);
		if (container != null) {
			for (ItemStackTemplate template : container.nonEmptyItems()) {
				contents.add(template.create());
			}
		}
		BundleContents bundle = this.getComponentForDisplay(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			for (ItemStackTemplate template : bundle.items()) {
				contents.add(template.create());
			}
		}

		if (this.config.heldItemInfoMergeSimilarContainerItems) {
			Map<Component, Integer> merged = new LinkedHashMap<>();
			for (ItemStack content : contents) {
				if (!content.isEmpty()) {
					merged.merge(content.getHoverName(), content.getCount(), Integer::sum);
				}
			}
			for (Map.Entry<Component, Integer> entry : merged.entrySet()) {
				this.append(countLine(entry.getKey(), entry.getValue()));
			}
		} else {
			for (ItemStack content : contents) {
				if (!content.isEmpty()) {
					this.append(countLine(content.getHoverName(), content.getCount()));
				}
			}
		}
	}

	// “物品 xN”一行：与参考实现一样复用原版容器提示的翻译键。
	private static Component countLine(Component name, int count) {
		return Component.translatable("item.container.item_count", count, name)
			.withStyle(DEFAULT_COLOR);
	}

	// 刷出物品使用同一条格式（物品 xN）。
	private void appendStackLine(ItemStack stack) {
		if (!stack.isEmpty()) {
			this.append(countLine(stack.getHoverName(), stack.getCount()));
		}
	}

	private void appendPotionEffects() {
		// 只保留“饮用后获得效果”的部分：遇到第一个空行（原版的“当施加时”分组）停止。
		AtomicBoolean shouldAddLine = new AtomicBoolean(true);
		this.appendComponent(DataComponents.POTION_CONTENTS, text -> {
			if (isBlank(text)) {
				shouldAddLine.set(false);
			} else if (shouldAddLine.get()) {
				this.append(text);
			}
		});
	}

	private void appendMusicDiscDescription() {
		JukeboxPlayable playable = this.getComponentForDisplay(DataComponents.JUKEBOX_PLAYABLE);
		if (playable != null) {
			this.append(() -> ComponentUtils.mergeStyles(
				playable.song().value().description(), Style.EMPTY.withColor(DEFAULT_COLOR)));
		}
	}

	private void appendEnchantmentComponents() {
		Set<Identifier> filtered = new LinkedHashSet<>();
		for (String raw : this.config.heldItemInfoFilteredEnchantments) {
			String id = raw == null ? "" : raw;
			try {
				filtered.add(Identifier.parse(id));
			} catch (IdentifierException e) {
				if (WARNED_INVALID_ENCHANTMENT_IDS.add(id)) {
					TweakifyMod.LOGGER.warn("Ignoring invalid enchantment id in held item info filter: {}", id);
				}
			}
		}
		HeldItemInfoState.setEnchantmentFilter(filtered, this.config.heldItemInfoShowOnlyFilteredEnchantments);
		try {
			this.appendComponent(DataComponents.ENCHANTMENTS);
			this.appendComponent(DataComponents.STORED_ENCHANTMENTS);
		} finally {
			HeldItemInfoState.clearEnchantmentFilter();
		}
	}

	private void appendLore() {
		ItemLore lore = this.getComponentForDisplay(DataComponents.LORE);
		if (lore == null) {
			return;
		}
		int maxRemainingLoreLines = this.config.heldItemInfoMaxLoreLines;
		for (Component line : lore.lines()) {
			int maxLines = Math.min(maxRemainingLoreLines, this.getRemainingLines());
			List<MutableComponent> wrapped = this.wrap(line, maxLines);
			maxRemainingLoreLines -= wrapped.size();
			for (MutableComponent part : wrapped) {
				this.append(ComponentUtils.mergeStyles(part, LORE_STYLE));
			}
		}
	}

	private void appendUnbreakable() {
		if (this.getComponentForDisplay(DataComponents.UNBREAKABLE) != null) {
			this.append(UNBREAKABLE_TEXT);
		}
	}

	private void appendSignText() {
		List<MutableComponent> frontLines = this.signSideText(DataComponents.SIGN_TEXT_FRONT);
		List<MutableComponent> backLines = this.signSideText(DataComponents.SIGN_TEXT_BACK);
		if (!frontLines.isEmpty()) {
			this.appendAll(frontLines);
		}
		if (!backLines.isEmpty()) {
			if (!frontLines.isEmpty()) {
				this.append(SIGN_SIDE_SEPARATOR);
			}
			this.appendAll(backLines);
		}
	}

	private List<MutableComponent> signSideText(DataComponentType<SignText> type) {
		SignText signText = this.getComponentForDisplay(type);
		if (signText == null) {
			return Collections.emptyList();
		}
		List<Component> messages = signText.getMessages(this.textFilteringEnabled);
		List<MutableComponent> result = new ArrayList<>(messages.size());
		for (Component message : messages) {
			String text = message.getString();
			if (!text.isBlank()) {
				result.add(Component.literal(text).withStyle(DEFAULT_COLOR));
			}
		}
		return result;
	}

	private void appendComponent(DataComponentType<? extends TooltipProvider> type) {
		this.appendComponent(type, this::append);
	}

	private void appendTransformedComponent(DataComponentType<? extends TooltipProvider> type,
			java.util.function.UnaryOperator<Component> transformation) {
		// 与参考实现一致：该行已超出上限时不再执行转换（可能原地修改共享组件）。
		this.appendComponent(type, text -> {
			if (this.canAdd()) {
				text = transformation.apply(text);
			}
			this.append(text);
		});
	}

	private void appendComponent(DataComponentType<? extends TooltipProvider> type,
			java.util.function.Consumer<Component> consumer) {
		TooltipProvider component = this.getComponentForDisplay(type);
		if (component != null) {
			component.addToTooltip(this.tooltipContext, consumer, TooltipFlag.NORMAL,
				this.stack.getComponents());
		}
	}

	// 请求的组件存在且在提示框里可见时返回，否则返回 null。
	private <T> T getComponentForDisplay(DataComponentType<T> type) {
		T component = this.stack.get(type);
		if (component != null && this.displayComponent.shows(type)) {
			return component;
		}
		return null;
	}

	private CompoundTag blockEntityData() {
		TypedEntityData<BlockEntityType<?>> data =
			this.getComponentForDisplay(DataComponents.BLOCK_ENTITY_DATA);
		return data != null ? data.copyTagWithoutId() : null;
	}

	private boolean canAdd() {
		return this.getRemainingLines() > 0;
	}

	private int getRemainingLines() {
		return this.config.heldItemInfoMaxLines - this.lines.size();
	}

	private void append(Component text) {
		this.realSize++;
		if (this.canAdd()) {
			this.lines.add(text);
		}
	}

	private void appendAll(List<? extends Component> newLines) {
		this.realSize += newLines.size();
		for (Component line : newLines) {
			if (this.canAdd()) {
				this.lines.add(line);
			} else {
				break;
			}
		}
	}

	private void append(Supplier<Component> textSupplier) {
		this.realSize++;
		if (this.canAdd()) {
			this.lines.add(textSupplier.get());
		}
	}

	private List<Component> build() {
		if (this.realSize > this.config.heldItemInfoMaxLines
			&& this.config.heldItemInfoShowHiddenLinesCount && !this.lines.isEmpty()) {
			int hidden = this.realSize - this.config.heldItemInfoMaxLines + 1;
			this.lines.set(this.lines.size() - 1,
				Component.translatable("item.container.more_items", hidden)
					.withStyle(DEFAULT_COLOR, ChatFormatting.ITALIC));
		}
		return this.lines;
	}

	// 折行：maxLines <= 0 时不返回任何行；截断时在最后一行补 “...”。
	private List<MutableComponent> wrap(String text, int maxLines) {
		if (maxLines <= 0) {
			return Collections.emptyList();
		}
		int maxLength = (int) Math.min(1.25 * maxLines * this.config.heldItemInfoMaxLineLength,
			Integer.MAX_VALUE);
		if (maxLength <= 0) {
			return Collections.emptyList();
		}
		boolean wasShortened = false;
		if (text.length() > maxLength) {
			wasShortened = true;
			text = text.substring(0, maxLength);
		}

		List<MutableComponent> result = new ArrayList<>(maxLines);
		String source = text;
		this.stringSplitter.splitLines(source, this.maxWidth(), Style.EMPTY, false,
			(style, start, end) -> result.add(Component.literal(source.substring(start, end))));

		if (result.size() > maxLines) {
			result.subList(maxLines, result.size()).clear();
			wasShortened = true;
		}
		if (wasShortened && !result.isEmpty()) {
			result.getLast().append("...");
		}
		return result;
	}

	private List<MutableComponent> wrap(FormattedText text, int maxLines) {
		if (maxLines <= 0) {
			return Collections.emptyList();
		}
		List<FormattedText> parts = this.stringSplitter.splitLines(text, this.maxWidth(), Style.EMPTY);
		List<MutableComponent> result = new ArrayList<>(maxLines);
		for (FormattedText part : parts) {
			result.add(Component.literal(part.getString()));
			if (result.size() >= maxLines) {
				if (parts.size() > maxLines) {
					result.get(maxLines - 1).append("...");
				}
				break;
			}
		}
		return result;
	}

	private int maxWidth() {
		double width = this.config.heldItemInfoMaxLineLength * AVERAGE_CHARACTER_WIDTH;
		return width >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) width;
	}

	// 参考实现把非可变组件复制一份后上色，避免修改共享组件。
	private static MutableComponent withDefaultColor(Component text) {
		if (text instanceof MutableComponent mutable) {
			return mutable.withStyle(DEFAULT_COLOR);
		}
		return text.copy().withStyle(DEFAULT_COLOR);
	}

	private static boolean isBlank(FormattedText text) {
		return text.visit(content -> content.isBlank() ? Optional.empty() : Optional.of(Unit.INSTANCE))
			.isEmpty();
	}
}
