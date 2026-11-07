package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 字符串列表编辑控件：在文本框中输入物品 ID 前缀后按 Tab 弹出候选菜单
 * （命令补全风格：Tab/上下箭头在候选中循环选择并实时回填文本框、鼠标点击
 * 候选直接选中、Enter/Esc 关闭菜单），并对当前输入做物品 ID 有效性检测；
 * 无候选或未聚焦文本框时保留 MaLiLib 默认的 Tab 焦点切换。
 */
public class TweakifyModStringListWidget extends WidgetListStringListEdit {
	// SDL scancode：MaLiLib 按键事件用 scancode 而非 keycode。
	private static final int KEY_ENTER = 40;
	private static final int KEY_ESCAPE = 41;
	private static final int KEY_TAB = 43;
	private static final int KEY_DOWN = 81;
	private static final int KEY_UP = 82;

	// 候选菜单最多同时显示的行数。
	public static final int MAX_VISIBLE_COMPLETIONS = 8;

	// 聚焦文本框输入相对物品注册表的有效性。
	public enum Validity {
		NONE,
		VALID,
		INVALID
	}

	// 聚焦文本框在屏幕上的边界，供对话框定位候选菜单。
	public record TextFieldBounds(int x, int y, int width, int height) {
	}

	// 候选菜单状态：activeCompletions 非空即菜单打开。
	private List<String> activeCompletions = List.of();
	private int completionIndex;
	private int completionScrollOffset;

	// 按 ID 排序的注册表快照：注册表在运行期不变，按注册表缓存（static，跨对话框复用）。
	private static Registry<?> cachedRegistry;
	private static List<Identifier> cachedIds = List.of();

	public TweakifyModStringListWidget(int x, int y, int width, int height, int configWidth,
									  GuiStringListEdit parent) {
		super(x, y, width, height, configWidth, parent);
	}

	@Override
	public boolean onKeyTyped(KeyEvent event) {
		int key = event.key();
		if (this.isCompletionMenuOpen()) {
			// 菜单打开时，Tab/上下箭头在候选中循环选择并实时回填文本框。
			if (key == KEY_TAB || key == KEY_DOWN) {
				this.cycleCompletion(event.hasShiftDown() ? -1 : 1);
				return true;
			}
			if (key == KEY_UP) {
				this.cycleCompletion(-1);
				return true;
			}
			if (key == KEY_ENTER || key == KEY_ESCAPE) {
				this.closeCompletionMenu();
				return true;
			}
			// 其它按键可能改变文本，关闭菜单后交给默认处理。
			this.closeCompletionMenu();
			return super.onKeyTyped(event);
		}
		if (key == KEY_TAB && !event.hasShiftDown() && this.openCompletionMenu()) {
			return true;
		}
		return super.onKeyTyped(event);
	}

	@Override
	public boolean onCharTyped(CharacterEvent event) {
		boolean result = super.onCharTyped(event);
		this.closeCompletionMenu();
		return result;
	}

	// Tab 触发：唯一候选直接补全，多候选弹出菜单并选中第一个，无候选返回 false。
	private boolean openCompletionMenu() {
		TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper = this.getFocusedTextField();
		if (wrapper == null) {
			return false;
		}
		String input = wrapper.textField().getValue().trim();
		if (input.isEmpty()) {
			return false;
		}
		List<String> candidates = getIdCompletions(input);
		if (candidates.size() == 1) {
			applyCompletion(wrapper, candidates.get(0));
			return true;
		}
		if (candidates.size() > 1) {
			this.activeCompletions = candidates;
			this.completionIndex = 0;
			this.completionScrollOffset = 0;
			applyCompletion(wrapper, candidates.get(0));
			return true;
		}
		return false;
	}

	// 按 delta 循环移动菜单选中项（超出可见窗口时滚动跟随），并回填聚焦文本框。
	public void cycleCompletion(int delta) {
		int size = this.activeCompletions.size();
		if (size == 0) {
			return;
		}
		this.completionIndex = ((this.completionIndex + delta) % size + size) % size;
		this.ensureIndexVisible();
		TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper = this.getFocusedTextField();
		if (wrapper != null) {
			applyCompletion(wrapper, this.activeCompletions.get(this.completionIndex));
		}
	}

	// 鼠标点击可见行 visibleRow（含滚动偏移）选中候选并关闭菜单。
	public void applyCompletionAt(int visibleRow) {
		int size = this.activeCompletions.size();
		if (size == 0 || visibleRow < 0) {
			return;
		}
		int index = Math.min(this.completionScrollOffset + visibleRow, size - 1);
		this.completionIndex = index;
		TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper = this.getFocusedTextField();
		if (wrapper != null) {
			applyCompletion(wrapper, this.activeCompletions.get(index));
		}
		this.closeCompletionMenu();
	}

	// 关闭候选菜单（保留文本框当前值）。
	public void closeCompletionMenu() {
		this.activeCompletions = List.of();
	}

	public boolean isCompletionMenuOpen() {
		return !this.activeCompletions.isEmpty();
	}

	public List<String> getActiveCompletions() {
		return this.activeCompletions;
	}

	public int getCompletionIndex() {
		return this.completionIndex;
	}

	public int getCompletionScrollOffset() {
		return this.completionScrollOffset;
	}

	// 聚焦文本框的屏幕边界，无聚焦时返回 null。
	public TextFieldBounds getFocusedTextFieldBounds() {
		TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper = this.getFocusedTextField();
		if (wrapper == null) {
			return null;
		}
		GuiTextFieldGeneric textField = wrapper.textField();
		return new TextFieldBounds(textField.getX(), textField.getY(), textField.getWidth(), textField.getHeight());
	}

	// 候选菜单选中项移出可见窗口时滚动跟随。
	private void ensureIndexVisible() {
		if (this.completionIndex < this.completionScrollOffset) {
			this.completionScrollOffset = this.completionIndex;
		} else if (this.completionIndex >= this.completionScrollOffset + MAX_VISIBLE_COMPLETIONS) {
			this.completionScrollOffset = this.completionIndex - MAX_VISIBLE_COMPLETIONS + 1;
		}
	}

	// 写入补全结果并通知配置监听器；泛型方法避免通配符捕获导致的类型不兼容。
	private static <T extends GuiTextFieldGeneric> void applyCompletion(TextFieldWrapper<T> wrapper, String completion) {
		T textField = wrapper.textField();
		textField.setValue(completion);
		textField.moveCursorToEnd(false);
		wrapper.listener().onTextChange(textField);
	}

	// 聚焦文本框当前输入是否存在于对应注册表（空输入返回 NONE）。
	// 状态提示每帧绘制，输入未变时直接复用上次结果，不重复解析 ID 与查注册表。
	private String validatedValue;
	private Validity validatedResult = Validity.NONE;

	public Validity getFocusedValidity() {
		TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper = this.getFocusedTextField();
		if (wrapper == null) {
			return Validity.NONE;
		}
		String value = wrapper.textField().getValue().trim();
		if (value.isEmpty()) {
			return Validity.NONE;
		}
		if (value.equals(this.validatedValue)) {
			return this.validatedResult;
		}
		Identifier id = Identifier.tryParse(value);
		Validity result = id != null && registry().containsKey(id) ? Validity.VALID : Validity.INVALID;
		this.validatedValue = value;
		this.validatedResult = result;
		return result;
	}

	private TextFieldWrapper<? extends GuiTextFieldGeneric> getFocusedTextField() {
		for (TextFieldWrapper<? extends GuiTextFieldGeneric> wrapper : this.textFields) {
			if (wrapper.isFocused()) {
				return wrapper;
			}
		}
		return null;
	}

	// 列表类型自带注册表；未声明类型的列表按物品处理。
	private Registry<?> registry() {
		return this.config instanceof IdConfigStringList list
			? list.registry()
			: BuiltInRegistries.ITEM;
	}

	// 注册表全部 ID，按字符串排序后缓存（跨对话框实例共享）。
	private static List<Identifier> sortedIds(Registry<?> registry) {
		if (registry != cachedRegistry) {
			List<Identifier> ids = new ArrayList<>(registry.keySet());
			ids.sort(Comparator.comparing(Identifier::toString));
			cachedIds = List.copyOf(ids);
			cachedRegistry = registry;
		}
		return cachedIds;
	}

	// 收集以输入为前缀的 ID：含命名空间时匹配完整 ID，否则同时匹配 ID 路径。
	private List<String> getIdCompletions(String input) {
		String prefix = input.toLowerCase(Locale.ROOT);
		boolean hasNamespace = prefix.indexOf(':') != -1;
		List<String> candidates = new ArrayList<>();
		for (Identifier id : sortedIds(registry())) {
			String key = id.toString();
			if (hasNamespace ? key.startsWith(prefix) : key.startsWith(prefix) || id.getPath().startsWith(prefix)) {
				candidates.add(key);
			}
		}
		return candidates;
	}
}
