package com.gtzc.tweakify.client.ui;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.config.ConfigGuiTab;
import com.gtzc.tweakify.client.config.TestingConfigOptions;
import com.gtzc.tweakify.client.config.TweakifyModConfigOptions;
import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.FavoritesStorage;
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
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TweakifyModConfigScreen extends GuiConfigsBase {
	// 左上角分类标签：全部 / 通用 / 功能 / 显示 / 修改 / 修复 / 优化 / 测试（枚举顺序即按钮顺序）。
	// 功能标签按“分组根”归类：根节点及其全部子项都归入功能，其余进通用/修改。
	// 显示标签放在功能右侧，集中世界内可视化与渲染叠加层。
	private static final int TAB_X = 10;
	private static final int TAB_Y = 30;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 2;

	// 上次选择的分类标签：开启“标签记忆”后，下次打开界面直接停在该标签。
	private static ConfigTab rememberedTab = ConfigTab.ALL;

	private ConfigTab currentTab = initialTab();

	// 标签记忆关闭时按配置的默认标签开始；无配置时回到“全部”。
	private static ConfigTab initialTab() {
		TweakifyModConfig config = TweakifyMod.config();
		if (config.general.rememberSelectedTab) {
			return rememberedTab;
		}
		return tabOfCode(ConfigGuiTab.fromCode(config.general.rememberSelectedTabFallback).code());
	}

	// code → 标签页映射放在界面侧：ConfigGuiTab 只保留 code，避免 config 包反向依赖 ui 包。
	// switch 覆盖全部枚举值，新增标签时编译器会强制同步这里。
	static ConfigTab tabOfCode(String code) {
		return switch (ConfigGuiTab.fromCode(code)) {
			case ALL -> ConfigTab.ALL;
			case GENERAL -> ConfigTab.GENERAL;
			case FUNCTIONS -> ConfigTab.FUNCTIONS;
			case DISPLAY -> ConfigTab.DISPLAY;
			case MODIFIED -> ConfigTab.MODIFIED;
			case FIXING -> ConfigTab.FIXING;
			case OPTIMIZATION -> ConfigTab.OPTIMIZATION;
			case TESTING -> ConfigTab.TESTING;
			case FAVORITES -> ConfigTab.FAVORITES;
		};
	}
	private final List<Button> tabButtons = new ArrayList<>();

	// 包私有：code → tab 映射与标签记忆都只在本界面内使用。
	enum ConfigTab {
		ALL("tweakify-mod.gui.tab.all"),
		GENERAL("tweakify-mod.gui.tab.general"),
		FUNCTIONS("tweakify-mod.gui.tab.functions"),
		DISPLAY("tweakify-mod.gui.tab.display"),
		MODIFIED("tweakify-mod.gui.tab.modified"),
		FIXING("tweakify-mod.gui.tab.fix"),
		OPTIMIZATION("tweakify-mod.gui.tab.optimization"),
		TESTING("tweakify-mod.gui.tab.testing"),
		// 收藏标签位于“测试”右侧：只显示已被收藏的配置项。
		FAVORITES("tweakify-mod.gui.tab.favorites");

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

	// 当前标签：搜索时据此构建完整树。
	ConfigTab currentTab() {
		return this.currentTab;
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
		return ConfigOptionWrapper.createFor(configsForTab(this.currentTab, true));
	}

	// 当前标签的条目：常规界面按折叠状态展开，搜索用完整树；档案编辑器作为界面操作项单独追加。
	static List<IConfigBase> searchConfigs(ConfigTab tab) {
		return configsForTab(tab, false);
	}

	private static List<IConfigBase> configsForTab(ConfigTab tab, boolean respectExpansion) {
		List<IConfigBase> result = new ArrayList<>(visibleConfigs(tab, respectExpansion));
		if (tab == ConfigTab.TESTING) {
			result.add(TestingConfigOptions.PROFILE_EDITOR);
		} else if (tab == ConfigTab.FAVORITES && FavoriteStarRenderer.isEnabled()
			&& FavoritesStorage.isConfigFavorite(TestingConfigOptions.PROFILE_EDITOR.getName())) {
			// 档案编辑器不在配置目录里，不能被树遍历到；收藏后单独追加到收藏标签。
			result.add(TestingConfigOptions.PROFILE_EDITOR);
		}
		return result;
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

	// GuiListBase 不转发拖动事件（只转发点击/释放/滚轮），
	// 列表的左键拖动滚动需要在这里手动转给列表控件。
	@Override
	public boolean onMouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (super.onMouseDragged(event, dragX, dragY)) {
			return true;
		}
		WidgetListConfigOptions list = this.getListWidget();
		return list != null && list.onMouseDragged(event, dragX, dragY);
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

	// 收藏星点击后由条目控件回调：收藏标签需要重新取列表（取消收藏后该行应消失），
	// 其他标签只需要下一帧重绘星标状态。
	void onFavoriteToggled() {
		if (this.currentTab != ConfigTab.FAVORITES) {
			return;
		}
		TweakifyModConfigListWidget list = this.configList();
		if (list != null) {
			list.refreshEntries();
		}
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
		TweakifyModConfigListWidget list = this.configList();
		int scroll = list == null ? 0 : list.getScrollbar().getValue();
		// 半行滚动时只恢复行号会让视口回跳最多一整行：偏移一并带上。
		int pixelOffset = list == null ? 0 : list.scrollPixelOffset();
		VisiblePosition before = visiblePositionOf(configName);
		node.toggle();
		this.initGui();
		VisiblePosition after = visiblePositionOf(configName);
		// 展开/收起的分组在滚动区上方时，下方内容会整体平移：滚动值同步加上行数差，视口内容保持不动。
		if (before.index() >= 0 && after.index() >= 0 && before.index() < scroll) {
			scroll += after.size() - before.size();
		}
		if (list != null) {
			// 越界值会被夹回新的合法范围。
			list.restoreScrollPosition(scroll, pixelOffset);
		}
	}

	// 本界面固定使用自己的列表控件；其他类型（理论上不会出现）按空处理。
	private TweakifyModConfigListWidget configList() {
		return this.getListWidget() instanceof TweakifyModConfigListWidget list ? list : null;
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

	// 按标签扁平化；respectExpansion=false 时忽略折叠状态（搜索用），true 时按当前展开状态输出。
	// 包私有：供收藏标签的回归测试直接校验可见集合。
	static List<IConfigBase> visibleConfigs(ConfigTab tab) {
		return visibleConfigs(tab, true);
	}

	private static List<IConfigBase> visibleConfigs(ConfigTab tab, boolean respectExpansion) {
		List<IConfigBase> result = new ArrayList<>();
		NODE_INFO.clear();
		if (tab == ConfigTab.FAVORITES) {
			if (!FavoriteStarRenderer.isEnabled()) {
				return result;
			}
			// 收藏标签只列收藏项（缩进保留真实层级，但不提供展开按钮），
			// 不把未收藏的父分组当作路径显示，避免收藏清单被分组噪声填满；
			// 开启「收藏包含子项」时，父项下的全部后代随父项一起显示。
			// 先取一次快照：遍历配置树时逐项查询会把同一把锁重复加几十次。
			Set<String> favorites = FavoritesStorage.configFavoriteSnapshot();
			// 配置尚未初始化时按默认值（开启）处理。
			boolean includeChildren = TweakifyMod.config().general.favoriteIncludeChildren;
			for (ConfigGroupNode root : ROOTS) {
				appendFavorites(root, 0, result, favorites, false, includeChildren);
			}
			return result;
		}
		for (ConfigGroupNode root : ROOTS) {
			appendVisible(root, 0, result, tab, respectExpansion);
		}
		return result;
	}

	// 收藏项按配置树顺序展开为扁平清单：先父项后子项，缩进沿用真实深度。
	// ancestorFavorite 表示某个祖先已被收藏：开启 includeChildren 时，整棵子树都进入清单。
	private static void appendFavorites(ConfigGroupNode node, int depth, List<IConfigBase> result,
										Set<String> favorites, boolean ancestorFavorite,
										boolean includeChildren) {
		boolean favorited = favorites.contains(node.config().getName());
		if (favorited || (includeChildren && ancestorFavorite)) {
			result.add(node.config());
			NODE_INFO.put(node.config().getName(), new NodeInfo(depth, false, false));
		}
		boolean childrenInheritFavorite = ancestorFavorite || favorited;
		for (ConfigGroupNode child : node.children()) {
			appendFavorites(child, depth + 1, result, favorites, childrenInheritFavorite, includeChildren);
		}
	}

	private static void appendVisible(ConfigGroupNode node, int depth, List<IConfigBase> result,
									  ConfigTab tab, boolean respectExpansion) {
		if (!shouldShow(node, tab)) {
			return;
		}
		result.add(node.config());
		NODE_INFO.put(node.config().getName(),
			new NodeInfo(depth, node.hasChildren(), respectExpansion ? node.isExpanded() : true));
		if (!respectExpansion || node.isExpanded()) {
			for (ConfigGroupNode child : node.children()) {
				appendVisible(child, depth + 1, result, tab, respectExpansion);
			}
		}
	}

	// 搜索结果显示的节点统一按“已展开、无折叠按钮”排版：匹配项的全部后代已经一并列出，
	// 再显示 [+] 只会让用户以为还要手动打开。
	static void markSearchVisible(Collection<String> names) {
		for (String name : names) {
			NODE_INFO.put(name, new NodeInfo(ConfigCatalog.depthOf(name), false, true));
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
			// 显示标签集中世界内可视化与渲染叠加层。
			case DISPLAY -> category == ConfigCatalog.Tab.DISPLAY;
			// 修改标签只放直接在目录里标记为 MODIFIED 的功能项。
			case MODIFIED -> category == ConfigCatalog.Tab.MODIFIED;
			// 修复标签只放直接在目录里标记为 FIX 的项。
			case FIXING -> category == ConfigCatalog.Tab.FIX;
			// 测试标签只放直接在目录里标记为 TESTING 的项；档案编辑器作为界面操作项单独追加。
			case TESTING -> category == ConfigCatalog.Tab.TESTING;
			// 优化标签只放直接在目录里标记为 OPTIMIZATION 的项。
			case OPTIMIZATION -> category == ConfigCatalog.Tab.OPTIMIZATION;
			case GENERAL -> category == null || category == ConfigCatalog.Tab.FUNCTION_AND_GENERAL;
			// 收藏标签由收藏集合驱动，与目录分类无关。
			case FAVORITES -> false;
		};
	}
}
