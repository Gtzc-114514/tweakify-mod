package com.gtzc.tweakify;

import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.ConfigProfiles;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

public class TweakifyModCommands {
	private static final int ITEMS_PER_PAGE = 8;

	private static final String FEEDBACK_ENABLED = "tweakify-mod.command.feedback.enabled";
	private static final String FEEDBACK_DISABLED = "tweakify-mod.command.feedback.disabled";
	private static final String ITEM_ADDED = "tweakify-mod.command.item.added";
	private static final String ITEM_DUPLICATE = "tweakify-mod.command.item.duplicate";
	private static final String ITEM_REMOVED = "tweakify-mod.command.item.removed";
	private static final String ITEM_MISSING = "tweakify-mod.command.item.missing";
	private static final String ITEM_PAGE_RANGE = "tweakify-mod.command.item.page_out_of_range";
	private static final String ITEM_LIST_HEADER = "tweakify-mod.command.item.list_header";
	private static final String ITEM_RESET_DONE = "tweakify-mod.command.item.reset_done";
	private static final String SAVE_FAILED = "tweakify-mod.command.save_failed";
	private static final String PROFILE_SAVED = "tweakify-mod.command.profile.saved";
	private static final String PROFILE_SAVED_WITH_BACKUP =
		"tweakify-mod.command.profile.saved_with_backup";
	private static final String PROFILE_LOADED = "tweakify-mod.command.profile.loaded";
	private static final String PROFILE_MISSING = "tweakify-mod.command.profile.missing";
	private static final String PROFILE_DELETED = "tweakify-mod.command.profile.deleted";
	private static final String PROFILE_SAVE_FAILED = "tweakify-mod.command.profile.save_failed";
	private static final String PROFILE_SAVE_FAILED_WITH_BACKUP =
		"tweakify-mod.command.profile.save_failed_with_backup";
	private static final String PROFILE_LIST_HEADER = "tweakify-mod.command.profile.list_header";
	private static final String PROFILE_LIST_EMPTY = "tweakify-mod.command.profile.list_empty";
	private static final String PROFILE_BROKEN = "tweakify-mod.command.profile.broken";
	private static final String PROFILE_FILE_BROKEN = "tweakify-mod.command.profile.file_broken";
	private static final String PROFILE_FILE_UNREADABLE = "tweakify-mod.command.profile.file_unreadable";

	// 布尔开关命令：命令名即 TweakifyModConfig 字段名与 ConfigCatalog 的登记名，
	// 反馈文案统一为 tweakify-mod.command.label.<name>，新增开关只需在目录里标记 command。
	private record BooleanCommand(String name, String labelKey) {
		static BooleanCommand of(String name) {
			if (!TweakifyModConfig.isBooleanField(name)) {
				throw new IllegalStateException("Command entry is not a boolean config field: " + name);
			}
			return new BooleanCommand(name, "tweakify-mod.command.label." + name);
		}
	}

	private static final List<BooleanCommand> BOOLEAN_COMMANDS = ConfigCatalog.commandEntries().stream()
		.map(entry -> BooleanCommand.of(entry.name()))
		.toList();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		// 不限制 OP 权限：这是纯客户端模组，命令只修改本地配置；
		// 远程服务器的服务端没有本模组，命令本身就不会注册。
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tweakify");

		for (BooleanCommand command : BOOLEAN_COMMANDS) {
			root.then(Commands.literal(command.name())
				.executes(ctx -> toggle(ctx.getSource(), command))
				.then(Commands.argument("value", BoolArgumentType.bool())
					.executes(ctx -> set(ctx.getSource(), command, BoolArgumentType.getBool(ctx, "value")))));
		}

		root.then(Commands.literal("restrictedOffhandItems")
			.then(Commands.literal("add")
				.then(Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
					.executes(TweakifyModCommands::addItem)))
			.then(Commands.literal("remove")
				.then(Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
					.executes(TweakifyModCommands::removeItem)))
			.then(Commands.literal("list")
				.executes(ctx -> listItems(ctx.getSource(), 1))
				.then(Commands.argument("page", IntegerArgumentType.integer(1))
					.executes(ctx -> listItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))))
			.then(Commands.literal("reset").executes(ctx -> resetItems(ctx.getSource())))
		);

		// 配置档案：整套保存/加载/删除/列举。
		root.then(Commands.literal("profile")
			.then(Commands.literal("save")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(TweakifyModCommands::saveProfile)))
			.then(Commands.literal("load")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(TweakifyModCommands::loadProfile)))
			.then(Commands.literal("delete")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(TweakifyModCommands::deleteProfile)))
			.then(Commands.literal("list").executes(ctx -> listProfiles(ctx.getSource())))
		);

		dispatcher.register(root);
	}

	private static int toggle(CommandSourceStack source, BooleanCommand command) {
		return apply(source, command, !TweakifyMod.CONFIG.getBoolean(command.name()));
	}

	private static int set(CommandSourceStack source, BooleanCommand command, boolean value) {
		return apply(source, command, value);
	}

	// 改值后立即写盘：整个「读旧值 + 改值 + 写盘 + 失败回滚」在配置锁内完成，
	// 因此不会把同一时刻客户端线程的其它修改一起回滚掉。
	private static int apply(CommandSourceStack source, BooleanCommand command, boolean value) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		String name = command.name();
		TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
			boolean previous = config.getBoolean(name);
			config.setBoolean(name, value);
			return () -> config.setBoolean(name, previous);
		});
		if (!ensureSaved(source, result)) {
			return 0;
		}
		// 界面里的 MaLiLib 选项还是旧值：请求客户端 tick 做一次全量同步，
		// 否则同一次会话里再触发一次选项同步会把命令的修改覆盖回去。
		config.requestOptionSync();
		return feedback(source, command.labelKey(), value);
	}

	private static int addItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Item item = itemFrom(context, "item");
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		TweakifyModConfig config = TweakifyMod.CONFIG;
		TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
			List<String> previous = config.getRestrictedOffhandItems();
			return config.addItem(id) ? () -> config.replaceRestrictedOffhandItems(previous) : null;
		});
		if (result == TweakifyModConfig.ApplyResult.UNCHANGED) {
			context.getSource().sendFailure(Component.translatable(ITEM_DUPLICATE, id));
			return 0;
		}
		if (!ensureSaved(context.getSource(), result)) {
			return 0;
		}
		config.requestOptionSync();
		context.getSource().sendSuccess(() -> Component.translatable(ITEM_ADDED, id), true);
		return 1;
	}

	private static int removeItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Item item = itemFrom(context, "item");
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		TweakifyModConfig config = TweakifyMod.CONFIG;
		TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
			List<String> previous = config.getRestrictedOffhandItems();
			return config.removeItem(id) ? () -> config.replaceRestrictedOffhandItems(previous) : null;
		});
		if (result == TweakifyModConfig.ApplyResult.UNCHANGED) {
			context.getSource().sendFailure(Component.translatable(ITEM_MISSING, id));
			return 0;
		}
		if (!ensureSaved(context.getSource(), result)) {
			return 0;
		}
		config.requestOptionSync();
		context.getSource().sendSuccess(() -> Component.translatable(ITEM_REMOVED, id), true);
		return 1;
	}

	private static int listItems(CommandSourceStack source, int page) {
		List<String> items = TweakifyMod.CONFIG.getRestrictedOffhandItems();
		int totalPages = Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
		if (page > totalPages) {
			source.sendFailure(Component.translatable(ITEM_PAGE_RANGE, totalPages));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable(ITEM_LIST_HEADER, items.size(), page, totalPages), false);
		int fromIndex = (page - 1) * ITEMS_PER_PAGE;
		int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, items.size());
		for (String id : items.subList(fromIndex, toIndex)) {
			source.sendSuccess(() -> Component.literal("- " + id), false);
		}
		return toIndex - fromIndex;
	}

	private static int resetItems(CommandSourceStack source) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
			List<String> previous = config.getRestrictedOffhandItems();
			config.resetToLightSources();
			return () -> config.replaceRestrictedOffhandItems(previous);
		});
		if (!ensureSaved(source, result)) {
			return 0;
		}
		int count = config.getRestrictedOffhandItems().size();
		config.requestOptionSync();
		source.sendSuccess(() -> Component.translatable(ITEM_RESET_DONE, count), true);
		return count;
	}

	// 把当前整套配置存成命名档案（同名覆盖）。
	private static int saveProfile(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		ConfigProfiles.SaveOutcome outcome = ConfigProfiles.save(name, TweakifyMod.CONFIG);
		if (outcome.result() != ConfigProfiles.Result.OK) {
			if (outcome.backupPath() != null) {
				// 已备份但写盘失败：把备份路径告诉玩家，否则旧档案等于“消失”。
				context.getSource().sendFailure(Component.translatable(
					PROFILE_SAVE_FAILED_WITH_BACKUP, name, outcome.backupPath().toString()));
			} else {
				context.getSource().sendFailure(failureMessage(outcome.result(), name));
			}
			return 0;
		}
		// 成功时也带上备份路径：旧的损坏文件已被备份走，玩家需要知道从哪里找回旧档案。
		Component message = outcome.backupPath() == null
			? Component.translatable(PROFILE_SAVED, name)
			: Component.translatable(PROFILE_SAVED_WITH_BACKUP, name, outcome.backupPath().toString());
		context.getSource().sendSuccess(() -> message, true);
		return 1;
	}

	// 应用档案：文件读写在锁外完成，只有「应用 + 写盘 + 失败回滚」在配置锁内。
	// 档案加载改动的就是全部字段，因此失败时回滚整份快照是等价的。
	private static int loadProfile(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		ConfigProfiles.ReadResult read = ConfigProfiles.read(name);
		if (!read.ok()) {
			context.getSource().sendFailure(failureMessage(read.status(), name));
			return 0;
		}
		ConfigProfiles.Loaded profile = read.profile();

		TweakifyModConfig config = TweakifyMod.CONFIG;
		TweakifyModConfig.ApplyResult result;
		try {
			result = config.applyAndSave(() -> {
				TweakifyModConfig.Snapshot previous = config.snapshot();
				try {
					config.applyFrom(profile.config(), profile.fieldNames());
				} catch (RuntimeException e) {
					// 应用中途失败时先把内存恢复到加载前，避免留下“部分字段已改、磁盘未写”的半应用状态。
					config.restoreSnapshot(previous);
					throw e;
				}
				return () -> config.restoreSnapshot(previous);
			});
		} catch (RuntimeException e) {
			TweakifyMod.LOGGER.error("Failed to apply config profile: {}", name, e);
			context.getSource().sendFailure(Component.translatable(SAVE_FAILED));
			return 0;
		}
		if (result == TweakifyModConfig.ApplyResult.SAVE_FAILED) {
			context.getSource().sendFailure(Component.translatable(SAVE_FAILED));
			return 0;
		}

		config.requestOptionSync();
		// 只统计真正会被应用的字段：版本号与已废弃的字段名不会被写入，不能算进提示数字。
		int applied = (int) profile.fieldNames().stream()
			.filter(TweakifyModConfig::isProfileField).count();
		context.getSource().sendSuccess(() -> Component.translatable(PROFILE_LOADED, name, applied), true);
		return applied;
	}

	private static int deleteProfile(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		ConfigProfiles.Result result = ConfigProfiles.delete(name);
		if (result != ConfigProfiles.Result.OK) {
			context.getSource().sendFailure(failureMessage(result, name));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.translatable(PROFILE_DELETED, name), true);
		return 1;
	}

	// 失败结果到提示文案的统一映射：用穷尽 switch 保证新增枚举值时编译期就能发现。
	private static String failureKey(ConfigProfiles.Result result) {
		return switch (result) {
			case NOT_FOUND -> PROFILE_MISSING;
			case BROKEN -> PROFILE_BROKEN;
			case READ_FAILED -> PROFILE_FILE_UNREADABLE;
			case WRITE_FAILED -> PROFILE_SAVE_FAILED;
			case OK -> throw new IllegalStateException("OK is not a failure result");
		};
	}

	// 只有「档案不存在」的文案需要档案名参数，其余文案自带完整语义。
	private static Component failureMessage(ConfigProfiles.Result result, String name) {
		return result == ConfigProfiles.Result.NOT_FOUND
			? Component.translatable(failureKey(result), name)
			: Component.translatable(failureKey(result));
	}

	private static int listProfiles(CommandSourceStack source) {
		ConfigProfiles.NamesResult result = ConfigProfiles.names();
		// 损坏与读失败要明确提示，否则会误报“还没有任何档案”。
		if (result.status() == ConfigProfiles.Result.BROKEN) {
			source.sendFailure(Component.translatable(PROFILE_FILE_BROKEN));
			return 0;
		}
		if (result.status() == ConfigProfiles.Result.READ_FAILED) {
			source.sendFailure(Component.translatable(PROFILE_FILE_UNREADABLE));
			return 0;
		}
		List<String> names = result.names();
		if (names.isEmpty()) {
			source.sendSuccess(() -> Component.translatable(PROFILE_LIST_EMPTY), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable(PROFILE_LIST_HEADER, names.size()), false);
		for (String name : names) {
			source.sendSuccess(() -> Component.literal("- " + name), false);
		}
		return names.size();
	}

	private static Item itemFrom(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		Holder.Reference<Item> ref = ResourceArgument.getResource(context, name, Registries.ITEM);
		return ref.value();
	}

	// 写盘失败时统一提示；返回是否继续执行后续成功反馈。
	private static boolean ensureSaved(CommandSourceStack source, TweakifyModConfig.ApplyResult result) {
		if (result != TweakifyModConfig.ApplyResult.SAVE_FAILED) {
			return true;
		}
		source.sendFailure(Component.translatable(SAVE_FAILED));
		return false;
	}

	private static int feedback(CommandSourceStack source, String labelKey, boolean value) {
		Component label = Component.translatable(labelKey);
		source.sendSuccess(() -> Component.translatable(value ? FEEDBACK_ENABLED : FEEDBACK_DISABLED, label), true);
		return 1;
	}
}
