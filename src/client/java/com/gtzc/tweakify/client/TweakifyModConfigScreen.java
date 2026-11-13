package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TweakifyModConfigScreen extends GuiConfigsBase {
	// 左上角分类标签：全部 / 通用 / 功能 / 修改 / 修复 / 优化 / 测试（枚举顺序即按钮顺序）。
	// 功能标签按“分组根”归类：根节点及其全部子项都归入功能，其余进通用/修改。
	private static final int TAB_X = 10;
	private static final int TAB_Y = 30;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 2;

	// 上次选择的分类标签：开启“标签记忆”后，下次打开界面直接停在该标签。
	private static ConfigTab rememberedTab = ConfigTab.ALL;

	private ConfigTab currentTab = initialTab();

	// 标签记忆关闭时总是从“全部”开始。
	private static ConfigTab initialTab() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.rememberSelectedTab ? rememberedTab : ConfigTab.ALL;
	}
	private final List<Button> tabButtons = new ArrayList<>();

	// 包私有：供配置树与分类标签的完整性测试引用。
	enum ConfigTab {
		ALL("tweakify-mod.gui.tab.all"),
		GENERAL("tweakify-mod.gui.tab.general"),
		FUNCTIONS("tweakify-mod.gui.tab.functions"),
		MODIFIED("tweakify-mod.gui.tab.modified"),
		FIXING("tweakify-mod.gui.tab.fix"),
		OPTIMIZATION("tweakify-mod.gui.tab.optimization"),
		TESTING("tweakify-mod.gui.tab.testing");

		private final String translationKey;

		ConfigTab(String translationKey) {
			this.translationKey = translationKey;
		}

		String translationKey() {
			return this.translationKey;
		}
	}
	// 配置树：静态保存，展开状态跨重开界面保留。
	private static final List<ConfigGroupNode> ROOTS = buildTree();
	private static final Map<String, ConfigGroupNode> NODES_BY_NAME = new HashMap<>();
	private static final Map<String, NodeInfo> NODE_INFO = new HashMap<>();

	static {
		indexNodes();
	}

	public TweakifyModConfigScreen() {
		this(null);
	}

	public TweakifyModConfigScreen(Screen parent) {
		super(10, 50, TweakifyMod.MOD_ID, parent, "tweakify-mod.gui.title.configs", getModVersion());
		this.setParent(parent);
		TweakifyModConfigOptions.syncFromConfig();
	}

	private static String getModVersion() {
		return FabricLoader.getInstance().getModContainer(TweakifyMod.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	@Override
	public void initGui() {
		super.initGui();
		this.clearOptions();

		// 快捷键编辑完成后（选中态清除时）立即同步到模组配置并写盘。
		// clearOptions 会清空监听列表，因此每次 initGui 都要重新注册。
		this.addKeybindChangeListener(TweakifyModConfigOptions::syncAndSave);
		this.addTabButtons();
	}

	// 关闭界面时立即刷出未写盘的修改，避免节流窗口内退出导致最后一笔配置丢失。
	@Override
	public void onClose() {
		TweakifyModConfigOptions.flushPendingSaveNow();
		super.onClose();
	}

	@Override
	protected int getConfigWidth() {
		return 210;
	}

	// 启用 MaLiLib 内建的搜索框：可按字段名、界面显示名，或输入 "modified" 筛出改过默认值的项。
	// 搜索只在当前标签页内过滤（先按标签筛、再按搜索筛）。
	@Override
	protected boolean useKeybindSearch() {
		return true;
	}

	// 使用双层标签布局的列表控件：名称行下方追加小号灰色英文配置键。
	@Override
	protected WidgetListConfigOptions createListWidget(int x, int y) {
		return new TweakifyModConfigListWidget(x, y, this.getBrowserWidth(), this.getBrowserHeight(),
			this.getConfigWidth(), 0.0f, this.useKeybindSearch(), this);
	}

	@Override
	public List<ConfigOptionWrapper> getConfigs() {
		List<IConfigBase> visible = new ArrayList<>(visibleConfigs(this.currentTab));
		if (this.currentTab == ConfigTab.TESTING) {
			visible.add(TweakifyModConfigOptions.PROFILE_EDITOR);
		}
		return ConfigOptionWrapper.createFor(visible);
	}

	// 在右下角绘制配置统计：总数 / 可用 / 已修改 / 已禁用。
	@Override
	public void drawContents(GuiContext gui, int mouseX, int mouseY, float partialTicks) {
		super.drawContents(gui, mouseX, mouseY, partialTicks);
		this.drawTabButtons(gui, mouseX, mouseY, partialTicks);
		this.drawStatsFooter(gui);
	}

	// 原版按钮不归 MaLiLib 的控件列表管理，因此在这里手动渲染。
	private void drawTabButtons(GuiContext gui, int mouseX, int mouseY, float partialTicks) {
		for (Button button : this.tabButtons) {
			button.extractRenderState(gui, mouseX, mouseY, partialTicks);
		}
	}

	// 先分发给标签按钮，未命中时再交给 MaLiLib 的列表事件链。
	@Override
	public boolean onMouseClicked(MouseButtonEvent event, boolean isRealMouse) {
		for (Button button : this.tabButtons) {
			if (button.mouseClicked(event, isRealMouse)) {
				return true;
			}
		}
		return super.onMouseClicked(event, isRealMouse);
	}

	// 使用原版 Button 创建分类标签；当前标签禁用置灰。
	private void addTabButtons() {
		this.tabButtons.clear();
		int x = TAB_X;
		for (ConfigTab tab : ConfigTab.values()) {
			Component label = Component.translatable(tab.translationKey());
			int width = Math.max(this.getStringWidth(label.getString()) + 10, 36);
			Button button = Button.builder(label, b -> this.switchTab(tab))
				.bounds(x, TAB_Y, width, TAB_HEIGHT)
				.build();
			button.active = tab != this.currentTab;
			this.tabButtons.add(button);
			x += width + TAB_GAP;
		}
	}

	private void switchTab(ConfigTab tab) {
		if (this.currentTab == tab) {
			return;
		}
		this.currentTab = tab;
		// 始终记录当前标签，开关关闭时只是不读取。
		rememberedTab = tab;
		// initGui() 不会重建列表控件（GuiListBase 只在控件为 null 时懒创建），
		// 所以这里设置的滚动值会作用于随后被复用的同一个实例，切标签后回到顶部。
		// 不调用 reCreateListWidget()：丢弃控件会连带丢掉未应用的修改。
		if (this.getListWidget() != null) {
			this.getListWidget().resetScrollbarPosition();
		}
		this.initGui();
	}

	private void drawStatsFooter(GuiContext gui) {
		int total = 0;
		int enabled = 0;
		int modified = 0;
		int disabled = 0;
		for (IConfigBoolean toggle : TweakifyModConfigOptions.TOGGLE_OPTIONS) {
			total++;
			boolean value = toggle.getBooleanValue();
			if (value) {
				enabled++;
			} else {
				disabled++;
			}
			if (value != toggle.getDefaultBooleanValue()) {
				modified++;
			}
		}
		String text = StringUtils.translate("tweakify-mod.gui.stats.footer", total, enabled, modified, disabled);
		int x = this.width - this.getStringWidth(text) - 10;
		int y = this.height - 12;
		this.drawString(gui, text, x, y, 0xFFA0A0A0);
	}

	// 点击 [+]/[-] 展开按钮：切换分组展开状态后刷新列表。
	// 不重建列表控件（重建会把滚动条归零），而是记下当前滚动位置再放回去。
	public void toggleGroup(String configName) {
		ConfigGroupNode node = NODES_BY_NAME.get(configName);
		if (node == null || !node.hasChildren()) {
			return;
		}
		int scroll = this.getListWidget() == null ? 0 : this.getListWidget().getScrollbar().getValue();
		VisiblePosition before = visiblePositionOf(configName);
		node.toggle();
		this.initGui();
		VisiblePosition after = visiblePositionOf(configName);
		// 展开/收起的分组在滚动区上方时，下方内容会整体平移：滚动值同步加上行数差，视口内容保持不动。
		if (before.index() >= 0 && after.index() >= 0 && before.index() < scroll) {
			scroll += after.size() - before.size();
		}
		if (this.getListWidget() != null) {
			// setValue 会把越界值夹回新的合法范围。
			this.getListWidget().getScrollbar().setValue(scroll);
		}
	}

	private record VisiblePosition(int index, int size) {
	}

	// 条目在当前标签下的可见序号与列表总行数；不在列表中时序号为 -1。
	private VisiblePosition visiblePositionOf(String configName) {
		List<IConfigBase> visible = visibleConfigs(this.currentTab);
		int index = -1;
		for (int i = 0; i < visible.size(); i++) {
			if (visible.get(i).getName().equals(configName)) {
				index = i;
				break;
			}
		}
		return new VisiblePosition(index, visible.size());
	}

	// 配置项对应的树节点信息，供条目控件读取缩进与展开按钮状态。
	public static NodeInfo getNodeInfo(IConfigBase config) {
		return config == null ? null : NODE_INFO.get(config.getName());
	}

	public record NodeInfo(int depth, boolean hasChildren, boolean expanded) {
	}

	// 固定树结构由配置目录（ConfigCatalog）描述：右键交互冷却为总开关，主/副手使用冷却与副手回退为子项；
	// 各自的物品列表再作为对应功能的子项，展开后单独成行。
	private static List<ConfigGroupNode> buildTree() {
		List<ConfigGroupNode> roots = new ArrayList<>();
		Map<String, ConfigGroupNode> nodes = new HashMap<>();
		for (ConfigCatalog.Entry entry : ConfigCatalog.entries()) {
			IConfigBase option = TweakifyModConfigOptions.optionOf(entry.name());
			if (option == null) {
				throw new IllegalStateException("Missing option for config entry: " + entry.name());
			}
			ConfigGroupNode node = new ConfigGroupNode(option);
			nodes.put(entry.name(), node);
			if (entry.isRoot()) {
				roots.add(node);
			} else {
				ConfigGroupNode parent = nodes.get(entry.parent());
				if (parent == null) {
					throw new IllegalStateException("Parent node not built yet: " + entry.name());
				}
				parent.child(node);
			}
		}
		return roots;
	}

	private static void indexNodes() {
		NODES_BY_NAME.clear();
		for (ConfigGroupNode root : ROOTS) {
			indexNode(root);
		}
	}

	// 树中登记的全部配置项名。包私有：测试用它校验 ALL_OPTIONS 是否被界面完整覆盖。
	static Set<String> treeConfigNames() {
		Set<String> names = new HashSet<>();
		Deque<ConfigGroupNode> pending = new ArrayDeque<>(ROOTS);
		while (!pending.isEmpty()) {
			ConfigGroupNode node = pending.removeFirst();
			names.add(node.config().getName());
			pending.addAll(node.children());
		}
		return Set.copyOf(names);
	}

	private static void indexNode(ConfigGroupNode node) {
		NODES_BY_NAME.put(node.config().getName(), node);
		for (ConfigGroupNode child : node.children()) {
			indexNode(child);
		}
	}

	// 按当前标签把可见节点扁平化，同时刷新 NODE_INFO 供条目控件读取。
	private static List<IConfigBase> visibleConfigs(ConfigTab tab) {
		List<IConfigBase> result = new ArrayList<>();
		NODE_INFO.clear();
		for (ConfigGroupNode root : ROOTS) {
			appendVisible(root, 0, result, tab);
		}
		return result;
	}

	private static void appendVisible(ConfigGroupNode node, int depth, List<IConfigBase> result,
									  ConfigTab tab) {
		if (!shouldShow(node, tab)) {
			return;
		}
		result.add(node.config());
		NODE_INFO.put(node.config().getName(),
			new NodeInfo(depth, node.hasChildren(), node.isExpanded()));
		if (node.isExpanded()) {
			for (ConfigGroupNode child : node.children()) {
				appendVisible(child, depth + 1, result, tab);
			}
		}
	}

	// 节点自身或其任一后代命中分类时，该节点作为路径显示。
	private static boolean shouldShow(ConfigGroupNode node, ConfigTab tab) {
		if (matchesTab(node.config(), tab)) {
			return true;
		}
		for (ConfigGroupNode child : node.children()) {
			if (shouldShow(child, tab)) {
				return true;
			}
		}
		return false;
	}

	// 分类归属由配置目录决定：功能/修改/修复/优化标签在目录里登记一次，子项自动继承。
	static boolean matchesTab(IConfigBase config, ConfigTab tab) {
		ConfigCatalog.Tab category = ConfigCatalog.tabOf(config.getName());
		return switch (tab) {
			case ALL -> true;
			// 双标签项同时出现在“功能”与“通用”两个标签（当前无此类配置项，保留支持）。
			case FUNCTIONS -> category == ConfigCatalog.Tab.FUNCTION
				|| category == ConfigCatalog.Tab.FUNCTION_AND_GENERAL;
			// 修改标签只放直接在目录里标记为 MODIFIED 的功能项。
			case MODIFIED -> category == ConfigCatalog.Tab.MODIFIED;
			// 修复标签只放直接在目录里标记为 FIX 的项。
			case FIXING -> category == ConfigCatalog.Tab.FIX;
			// 测试标签只放直接在目录里标记为 TESTING 的项；档案编辑器作为界面操作项单独追加。
			case TESTING -> category == ConfigCatalog.Tab.TESTING;
			// 优化标签只放直接在目录里标记为 OPTIMIZATION 的项。
			case OPTIMIZATION -> category == ConfigCatalog.Tab.OPTIMIZATION;
			case GENERAL -> category == null || category == ConfigCatalog.Tab.FUNCTION_AND_GENERAL;
		};
	}
}
