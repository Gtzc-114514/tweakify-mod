package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.ConfigProfiles;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置档案编辑器：保存当前整套配置、加载/删除已有档案。
 *
 * <p>入口在配置界面的“测试”标签。档案读写复用 {@link ConfigProfiles}，
 * 加载时通过 {@link TweakifyModConfig#applyAndSave} 保证失败回滚。
 */
public final class ProfileEditorScreen extends Screen {
	private static final int LIST_TOP = 82;
	private static final int ROW_HEIGHT = 24;
	private static final int MAX_VISIBLE_ROWS = 8;
	private static final long DELETE_CONFIRM_MS = 5000L;
	private static final int ERROR_COLOR = 0xFFFF5555;
	private static final int SUCCESS_COLOR = 0xFF55FF55;
	private static final int TEXT_COLOR = 0xFFE0E0E0;

	private final Screen parent;
	private final List<String> names = new ArrayList<>();
	private final List<Button> rowButtons = new ArrayList<>();
	private EditBox nameField;
	private int scrollOffset;
	private ConfigProfiles.Result listStatus = ConfigProfiles.Result.OK;
	private String pendingDelete;
	private long pendingDeleteAt;
	private Component status = Component.empty();
	private int statusColor = TEXT_COLOR;

	public ProfileEditorScreen(Screen parent) {
		super(Component.translatable("tweakify-mod.gui.profile_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.reloadNames();
		this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll()));

		int center = this.width / 2;
		this.nameField = new EditBox(this.font, center - 160, 36, 190, 20,
			Component.translatable("tweakify-mod.gui.profile_editor.name"));
		this.nameField.setMaxLength(48);
		this.addRenderableWidget(this.nameField);
		this.setInitialFocus(this.nameField);

		this.addRenderableWidget(Button.builder(
				Component.translatable("tweakify-mod.gui.profile_editor.save"), button -> this.saveCurrent())
			.bounds(center + 40, 36, 100, 20).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("tweakify-mod.gui.profile_editor.refresh"), button -> this.refreshUi())
			.bounds(center - 160, 60, 80, 20).build());

		this.rebuildRows();

		this.addRenderableWidget(Button.builder(
				Component.translatable("tweakify-mod.gui.profile_editor.back"), button -> this.onClose())
			.bounds(center - 40, this.height - 30, 80, 20).build());
	}

	// 只重建档案行按钮，滚动时不再读盘，也不抢走名称输入框焦点。
	private void rebuildRows() {
		for (Button button : this.rowButtons) {
			this.removeWidget(button);
		}
		this.rowButtons.clear();

		int visibleRows = this.visibleRows();
		for (int row = 0; row < visibleRows && this.scrollOffset + row < this.names.size(); row++) {
			String name = this.names.get(this.scrollOffset + row);
			int y = LIST_TOP + row * ROW_HEIGHT;
			Button load = Button.builder(
					Component.translatable("tweakify-mod.gui.profile_editor.load"), button -> this.loadProfile(name))
				.bounds(this.width - 165, y, 60, 20).build();
			Button delete = Button.builder(
					Component.translatable("tweakify-mod.gui.profile_editor.delete"), button -> this.deleteProfile(name))
				.bounds(this.width - 85, y, 60, 20).build();
			this.addRenderableWidget(load);
			this.addRenderableWidget(delete);
			this.rowButtons.add(load);
			this.rowButtons.add(delete);
		}
	}

	private void reloadNames() {
		this.names.clear();
		ConfigProfiles.NamesResult result = ConfigProfiles.names();
		this.listStatus = result.status();
		if (result.status() == ConfigProfiles.Result.OK) {
			this.names.addAll(result.names());
			return;
		}
		this.setStatus(Component.translatable(result.status() == ConfigProfiles.Result.BROKEN
			? "tweakify-mod.command.profile.file_broken"
			: "tweakify-mod.command.profile.file_unreadable"), ERROR_COLOR);
	}

	private int visibleRows() {
		return Math.max(1, Math.min(MAX_VISIBLE_ROWS, (this.height - LIST_TOP - 45) / ROW_HEIGHT));
	}

	private int maxScroll() {
		return Math.max(0, this.names.size() - this.visibleRows());
	}

	private void refreshUi() {
		String name = this.nameField == null ? "" : this.nameField.getValue();
		this.pendingDelete = null;
		this.rebuildWidgets();
		if (this.nameField != null) {
			this.nameField.setValue(name);
		}
	}

	private void saveCurrent() {
		this.pendingDelete = null;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		String name = this.nameField == null ? "" : this.nameField.getValue().trim();
		if (config == null) {
			this.setStatus(Component.translatable("tweakify-mod.command.profile.save_failed", name), ERROR_COLOR);
			return;
		}
		if (name.isEmpty()) {
			this.setStatus(Component.translatable("tweakify-mod.gui.profile_editor.name_required"), ERROR_COLOR);
			return;
		}

		ConfigProfiles.SaveOutcome outcome = ConfigProfiles.save(name, config);
		if (outcome.result() != ConfigProfiles.Result.OK) {
			if (outcome.backupPath() == null) {
				this.setStatus(Component.translatable("tweakify-mod.command.profile.save_failed", name), ERROR_COLOR);
			} else {
				this.setStatus(Component.translatable("tweakify-mod.command.profile.save_failed_with_backup",
					name, outcome.backupPath().toString()), ERROR_COLOR);
			}
			this.refreshUi();
			return;
		}
		if (outcome.backupPath() == null) {
			this.setStatus(Component.translatable("tweakify-mod.command.profile.saved", name), SUCCESS_COLOR);
		} else {
			this.setStatus(Component.translatable("tweakify-mod.command.profile.saved_with_backup",
				name, outcome.backupPath().toString()), SUCCESS_COLOR);
		}
		this.refreshUi();
	}

	private void loadProfile(String name) {
		this.pendingDelete = null;
		ConfigProfiles.ReadResult read = ConfigProfiles.read(name);
		if (!read.ok()) {
			this.setStatus(this.failureMessage(read.status(), name,
				"tweakify-mod.command.profile.load_failed"), ERROR_COLOR);
			return;
		}

		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			this.setStatus(Component.translatable("tweakify-mod.command.profile.load_failed", name), ERROR_COLOR);
			return;
		}
		try {
			// 先提交尚未写盘的修改；applyAndSave 的写盘会排在同一串行队列并等待，
			// 更早的旧写一定先完成，不会覆盖刚加载的档案。
			TweakifyModConfigOptions.flushPendingSaveNow();
			TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
				TweakifyModConfig.Snapshot previous = config.snapshot();
				try {
					config.applyFrom(read.profile().config(), read.profile().fieldNames());
				} catch (RuntimeException e) {
					config.restoreSnapshot(previous);
					throw e;
				}
				return true;
			});
			if (result == TweakifyModConfig.ApplyResult.SAVE_FAILED) {
				this.setStatus(Component.translatable("tweakify-mod.command.profile.load_failed", name), ERROR_COLOR);
				return;
			}
		} catch (RuntimeException e) {
			TweakifyMod.LOGGER.error("Failed to apply config profile from editor: {}", name, e);
			this.setStatus(Component.translatable("tweakify-mod.command.profile.load_failed", name), ERROR_COLOR);
			return;
		}

		config.requestOptionSync();
		TweakifyModConfigOptions.syncFromConfig();
		int applied = (int) read.profile().fieldNames().stream()
			.filter(TweakifyModConfig::isProfileField).count();
		this.setStatus(Component.translatable("tweakify-mod.command.profile.loaded", name, applied), SUCCESS_COLOR);
	}

	private void deleteProfile(String name) {
		long now = System.currentTimeMillis();
		if (!name.equals(this.pendingDelete) || now - this.pendingDeleteAt > DELETE_CONFIRM_MS) {
			this.pendingDelete = name;
			this.pendingDeleteAt = now;
			this.setStatus(Component.translatable(
				"tweakify-mod.gui.profile_editor.delete_confirm", name), ERROR_COLOR);
			return;
		}
		this.pendingDelete = null;
		ConfigProfiles.Result result = ConfigProfiles.delete(name);
		if (result != ConfigProfiles.Result.OK) {
			this.setStatus(this.failureMessage(result, name,
				"tweakify-mod.command.profile.delete_failed"), ERROR_COLOR);
			return;
		}
		this.setStatus(Component.translatable("tweakify-mod.command.profile.deleted", name), SUCCESS_COLOR);
		this.refreshUi();
	}

	private Component failureMessage(ConfigProfiles.Result result, String name, String writeFailureKey) {
		String key = switch (result) {
			case NOT_FOUND -> "tweakify-mod.command.profile.missing";
			case BROKEN -> "tweakify-mod.command.profile.broken";
			case READ_FAILED -> "tweakify-mod.command.profile.file_unreadable";
			case WRITE_FAILED -> writeFailureKey;
			case OK -> throw new IllegalStateException("OK is not a failure result");
		};
		return result == ConfigProfiles.Result.NOT_FOUND || result == ConfigProfiles.Result.WRITE_FAILED
			? Component.translatable(key, name)
			: Component.translatable(key);
	}

	private void setStatus(Component status, int color) {
		this.status = status;
		this.statusColor = color;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.names.size() > this.visibleRows()) {
			int next = Math.max(0, Math.min(this.maxScroll(), this.scrollOffset - (int) Math.signum(scrollY)));
			if (next != this.scrollOffset) {
				this.scrollOffset = next;
				this.rebuildRows();
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
		this.extractBackground(gui, mouseX, mouseY, partialTick);
		super.extractRenderState(gui, mouseX, mouseY, partialTick);

		String title = this.title.getString();
		gui.text(this.font, title, (this.width - this.font.width(title)) / 2, 14, 0xFFFFFFFF, true);
		gui.text(this.font, Component.translatable("tweakify-mod.gui.profile_editor.list").getString(),
			20, 66, 0xFFA0A0A0, false);

		if (this.names.isEmpty()) {
			if (this.listStatus == ConfigProfiles.Result.OK) {
				gui.text(this.font, Component.translatable("tweakify-mod.gui.profile_editor.empty").getString(),
					24, LIST_TOP + 4, 0xFFA0A0A0, false);
			}
		} else {
			for (int row = 0; row < this.visibleRows() && this.scrollOffset + row < this.names.size(); row++) {
				String name = this.names.get(this.scrollOffset + row);
				String display = this.font.plainSubstrByWidth(name, this.width - 190);
				gui.text(this.font, display, 24, LIST_TOP + row * ROW_HEIGHT + 6, TEXT_COLOR, false);
			}
			if (this.names.size() > this.visibleRows()) {
				int hintY = LIST_TOP + this.visibleRows() * ROW_HEIGHT + 4;
				if (hintY + 9 <= this.height - 48) {
					gui.text(this.font, Component.translatable("tweakify-mod.gui.profile_editor.scroll_hint").getString(),
						24, hintY, 0xFFA0A0A0, false);
				}
			}
		}

		if (!this.status.getString().isEmpty()) {
			gui.text(this.font, this.status.getString(), 20, this.height - 48, this.statusColor, false);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
