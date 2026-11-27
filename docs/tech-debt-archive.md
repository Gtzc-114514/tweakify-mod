# 技术债归档（已闭环）

本文件归档已闭环的技术债详情（T1/T2/T4/T7/T8/T9/T10）；
现行跟踪与状态表见 [`docs/tech-debt.md`](tech-debt.md)。
归档条目不再参与日常复评，仅在各自「回归条件」触发时重开。

| # | 状态 | 闭环摘要 |
| --- | --- | --- |
| T1 | 已归档 | 真实 `LevelChunk` GameTest 覆盖 section 预筛接线与读取数/section 集合 |
| T2 | 已修复 | 发布侧摘要 + 精确比对，内容未变时复用几何 |
| T4 | 已修复 | 热更新转发抽到 `CreativeItemGroups.onConfigChanged`，真实 Loader 集成 + 源码契约 |
| T7 | 已修复 | 窄窗口放不下时隐藏收藏星，不再夹到重置按钮上方 |
| T8 | 已修复 | 真实 Loader 全量织入验证（187 条 / 124 个目标类） |
| T9 | 已修复 | 6 处测试专用 public 收回包私有 |
| T10 | 已修复 | UI 侧 `tabOfCode` 映射，消除 `config → ui` 依赖 |

<a id="t1"></a>
## T1 详情（已归档）：section 预筛的集成测试缺口

- **修复前现状**：`sectionNeedsScan(...)` 的纯函数决策由 `BlockMarkerScannerTest` 覆盖；未覆盖的是
  `ScanJob` 中 `chunkMayContain[slot] = BlockMarkerScanner.sectionNeedsScan(...)` 与真实
  `LevelChunk.getSectionIndex/getSectionsCount/getSection` 的接线。把该行改成 `= true` 只会取消预筛，
  标记结果仍正确，因此原缺口属于性能回退而非功能错误。
- **原影响**：section 预筛失效时，屏障/基岩扫描会退化为逐方块读取，单层读取量可从命中 section 的 16×16
  扩大到整个 65×65，帧时间上升但功能测试仍全绿。
- **采用方案**：重建 Fabric Loom server GameTest（`fabricApi.configureTests`），在
  `src/gametest/java/.../BlockMarkerScannerGameTest` 中使用真实 `ServerLevel`/`LevelChunk`，
  分别覆盖 A（只开屏障）、B（只开基岩）、C（两者全开），并增加两个负 y 场景
  （世界最底层 section 0、扫描盒越过 minY 时的越界跳过）：
  - 测试在固定世界坐标放置分属不同 section 的屏障与基岩，正 y 场景扫描 49³，负 y 场景扫描 17³；
  - `ScanJob` 暴露逐方块读取计数与读取过的 section 集合；读取数必须等于命中 section 层数 × 目标区块 16×16，
    section 集合必须精确等于目标组合（防止 target 写反或漏扫）；诊断集合只在 GameTest 构造时开启，
    生产 `createJob` 不分配集合/键对象；
  - 若 `chunkMayContain` 被改成 `true`，正 y A/B/C 读取数变为 117,649，两个负 y 场景变为 4,913，
    分别对 expected 1024/3328/4352/4096/3328 失败，证明测试不是假通过。
- **修复记录**：`build.gradle` 增加 GameTest 源集配置（只启用 server 测试，不涉及 eula；`runGameTest`
  不可缓存，构建注释标注 `-x runGameTest` 逃生口）；新增
  `src/gametest/java/com/gtzc/tweakify/client/BlockMarkerScannerGameTest.java` 与
  `src/gametest/resources/fabric.mod.json`；`BlockMarkerScanner.ScanJob` 改用 `Level`，
  新增读取计数、读取 section 集合与测试访问器；`BlockMarkerScannerTest` 增加
  `scanParamsFollowEnabledTargets` 覆盖 `createJob` 的开关/半径/上限参数接线；`check` 自动执行 `runGameTest`。
- **验证**：`gradlew test` 通过；`gradlew build` 运行 5 个自研 GameTest（日志总计 6 个 required tests）全部通过；
  临时把预筛赋值改为 `true` 后 A/B/C 与两个负 y 场景全部按读取数断言失败，随后恢复并再次构建通过。
- **回归条件**：若删除或绕过 `src/gametest`、改动 `ScanJob` 的 section 预筛、或 GameTest 读取计数与
  命中 section 的预期不一致，重新评估本条并保留 GameTest 作为基线。

<a id="t2"></a>
## T2 详情（已归档）：顶点缓存的失效粒度过粗

- **修复前现状**：`BedrockGeometryCache` / `AttackRangeGeometryCache` 以“列表引用是否变化”作为唯一失效条件。
  `BlockMarkerScanner` 每轮扫描结束、`AttackRangeState` 每 1.67 tick 都会发布新的 `List.copyOf` 实例，
  因此缓存按每秒 6/12 次整表重建——即使玩家静止、实体与标记位置完全没变。
- **原影响**（修复前）：每次重建都重新计算全部段与三档 LOD 顶点（最坏 256 个基岩标记 × 48 顶点、256 个条目 × 3 档几何），
  并重新填充数组。属 CPU 与 GC 压力，不会造成渲染错误；相对改造前“每帧重建”仍是约 5–10 倍的净收益，
  默认上限（基岩 32、攻击范围 64）下量级很小。
- **采用方案**：方案 1（发布处内容摘要 + 精确比对），未采用 `geometryVersion` 与缓存侧摘要两种备选；
  实施见「修复记录」。
- **修复记录（v1.0.5，`081383c`…`2bd00ff`）**：`BedrockHighlightState.setMarkers` 与 `AttackRangeState.publishEntries`
  在发布前计算内容摘要（基岩：size + `Marker.hashCode()`；攻击范围：size + 坐标/半径，不含随玩家移动变化的 `distanceSq`），
  摘要相同后再做一次精确比对（基岩逐条 `equals`，攻击范围只比几何字段），两者都相同才保留旧引用；
  因此哈希碰撞不会漏判内容变化，最多损失一次重建收益，几何不会停在旧内容。
  摘要比较使用 `Objects.equals(publishedHash, hash)`，可空 `Integer` 与 `int` 混用不会退化为引用比较。
  清理路径：攻击范围关闭功能与 `resetForTest()` 共用 `resetState()`，基岩关闭功能走 `clear()`，
  两者都会把摘要置空（`publishedHash = null`）。
  攻击范围按距离排序时用 `x → feetY → z → radius` 作并列规则，等距实体的顺序确定，
  不会因迭代顺序变化而误重建或让最近目标在轮次间抖动。
- **验证**：`BedrockGeometryCacheTest` / `AttackRangeGeometryCacheTest` 覆盖缓存失效条件、LOD 顶点数与坐标基准；
  `BedrockHighlightStateTest` / `AttackRangeStateTest` 覆盖发布去重、顺序交换、等距并列与截断；
  发布记录见 [`docs/release-notes/v1.0.5.md`](release-notes/v1.0.5.md)。
- **回归条件**：实机出现 GC 抖动或帧时间周期性尖峰，且 profiler 显示几何重建占 tick 时间超过约 1%；
  或把基岩/攻击范围上限调到 256 并同时开启多个渲染功能时。复现时重开为暂缓条目并沿用本阈值。
  与 T3 的关系：T3 用 > 5% 判扫描/收集总占比，本条只看几何重建占比，两者互不替代；
  同时超过时先按 T3 建议方案 3 降负。
- **v1.1.0 复评（2026-11-16）**：基岩高亮新增扫描半径与标记颜色，攻击范围新增颜色调色板；
  颜色已作为缓存键参与失效判定，仅在配置变化时重建，默认路径仍走摘要去重。
  相关几何缓存与状态测试通过，未发现回归，状态维持“已修复”。

<a id="t4"></a>
## T4 详情：mixin 层配置热更新转发无自动化覆盖

- **修复前现状**：配置变化后“是否重建当前页”的决策已下沉到 `CreativeItemGroups.onConfigChanged`，
  由 `CreativeItemGroupsTest.onConfigChangedRefreshesOnlyCategoryTabs` 覆盖分类页重建、
  非分类页只告警且不清空组状态、null 分支；但 `CreativeModeInventoryGroupingMixin.tweakify$refreshOnConfigChange`
  的「签名比对 → 调用 onConfigChanged → 按返回值刷新」链路没有自动化覆盖，项目测试基础设施不加载 Mixin。
- **原影响**：转发代码被误删、条件写反或刷新调用被移除时，单测全绿，只能靠手动回归发现；
  实机表现为配置变化后物品列表不刷新，或非分类页误清空 `currentTab`/渲染快照，
  导致组图标与点击判定错位。
- **采用方案（可测性接缝 + 真实 Loader 集成 + 源码契约）**：
  - 把「非空判断 + onConfigChanged」收敛为 `CreativeItemGroups.shouldRefreshAfterConfigChange(CreativeModeTab)`；
    Mixin 的 `containerTick` 注入只调用该接缝并按返回值刷新，不再内联条件；
  - 新增接缝单测 `CreativeItemGroupsTest.shouldRefreshAfterConfigChangeMatchesOnConfigChanged`，
    覆盖 null / 非分类页 / 分类页三种返回值；
  - 新增 `CreativeModeInventoryGroupingMixinContractTest` 源码契约测试，锁定三处接线：
    `@Inject(method = "containerTick")`、`CreativeItemGroups.shouldRefreshAfterConfigChange(selectedTab)`、
    `refreshCurrentTabContents(selectedTab.getDisplayItems())`；任一处被删除/改名/条件写反即失败；
  - 真实 Loader 集成：`fabric-loader-junit:0.20.1` 只在独立 `loaderTest` 源集/任务中引入
    （`src/loaderTest/java`，`check` 依赖该任务）；`CreativeModeInventoryGroupingMixinLoaderTest`
    在 CLIENT 环境加载 `CreativeModeInventoryScreen`，断言 `tweakify$lastConfigSignature` 字段
    与 `containerTick` 方法存在；注入配置错误会在类加载时直接失败；
  - 不把 Fabric Loader JUnit 加到普通 `test`：实测会让 `Bootstrap.bootStrap()` 在
    `CreativeItemGroupsTest` 触发 `Unbound tags`（Loader 已冻结注册表但未加载数据包 tags），
    因此用独立源集隔离，普通 708 个用例保持原环境。
- **修复记录（提交 `3559cb1`，基于 `78e411d`，待发布）**：
  - `3559cb1`：`build.gradle` 新增 `loaderTest` 源集/任务与 `fabric-loader-junit` 依赖；
    新增 `CreativeModeInventoryGroupingMixinLoaderTest`；`.gitignore` 忽略 `.fabric/`；
  - `78e411d`：`CreativeItemGroups` 新增转发接缝；`CreativeModeInventoryGroupingMixin` 改为调用接缝；
    `CreativeItemGroupsTest` 新增接缝测试；新增 `CreativeModeInventoryGroupingMixinContractTest`。
- **验证**：`gradlew test` **708** 个单测通过；`gradlew loaderTest` 1 个集成用例通过
  （CLIENT 环境 + Mixin 织入）；`gradlew build --rerun-tasks` 与 6 个 GameTest 通过；
  CI 的 `./gradlew build` 通过 `check` 自动执行 `loaderTest`。
- **回归条件**：若删除/拆分接缝、改变 `containerTick` 注入方法名，或移除列表刷新调用，
  接缝测试/源码契约会失败；若删除 `loaderTest` 源集、任务或依赖，真实加载保护失效，
  需重新评估本条。

<a id="t7"></a>
## T7 详情：窄窗口行内收藏星与重置按钮重叠

- **修复前现状**：`FavoriteStarRenderer.clampStarX` 会把星标夹回行内；
  `TweakifyModConfigOptionWidget.addFavoriteStar` 以「控件行最右按钮右边界 + 2」为期望位置。
  当 `browserEntryWidth - maxLabelWidth - 10 - RIGHT_RESERVE < MIN_CONFIG_WIDTH(120)` 时，
  `effectiveConfigWidth` 被 120 的下限抬高，控件与重置按钮溢出预留区；星标被夹到行尾后，
  左边界落进重置按钮矩形（按钮宽 20、星标 16）。星标后加入 `subWidgets`、最后渲染，
  会盖住重置按钮右半部分；点击由 `WidgetContainer.onMouseClicked` 按顺序命中重置按钮，收藏星失效。
- **原影响**：仅出现在极端窄窗口（4x/自动缩放叠加长名称，约行宽 < `maxLabelWidth + 186`）；
  表现为视觉重叠与星标不可点，正常窗口不受影响；重置按钮本身的功能不受损（只是星标被抢）。
- **采用方案**：星标定位改为「能完整落在行内才显示」：
  `FavoriteStarRenderer.starXIfFits(controlsRight, rowX, rowWidth)` 只在星标（16px + 2px 右留白）
  完整落在行内时返回位置，否则返回 `-1`；`addFavoriteStar` 收到 `-1` 直接不添加星标。
  正常行仍在重置/打开按钮右边 2px；删除 `clampStarX`，不再把星标挪到重叠位置。
- **修复记录（v1.1.5，`7b05164`）**：`FavoriteStarRenderer.clampStarX` → `starXIfFits`；
  `TweakifyModConfigOptionWidget.addFavoriteStar` 增加空间不足分支；
  `FavoriteStarStyleTest` 新增 `starIsHiddenWhenRowCannotFitIt`（刚好放下 / 少 1 像素 /
  按钮溢出 / 极端窄行），替换旧的 clamp 测试；发布记录见
  [`docs/release-notes/v1.1.5.md`](release-notes/v1.1.5.md) 与
  [`docs/release-pipeline.md`](release-pipeline.md) 的 v1.1.5 验证记录（run `44593915842`）。
- **验证**：`gradlew test` 706 个单测通过；`starIsHiddenWhenRowCannotFitIt` 四个边界全部通过，
  正常布局断言 `rowStarSitsRightOfTheResetButtonAndStaysClear` 继续通过；`gradlew build` 与
  6 个 GameTest 通过；v1.1.5 release 流水线 11 个步骤全绿。
- **回归条件**：若重新允许星标在空间不足时「夹到其它位置」，或调整 `RIGHT_RESERVE` /
  `MIN_CONFIG_WIDTH` 让控件再次溢出预留区并与星标相交，重开本条；正常窗口下星标位置断言
  （`rowStarSitsRightOfTheResetButtonAndStaysClear`）与窄行隐藏断言（`starIsHiddenWhenRowCannotFitIt`）
  作为回归基线。

<a id="t8"></a>
## T8 详情：全量 mixin 织入验证

> 本节数字为 2026-11-21 落地时的基线（145 条 / 100 个目标类 / 73 个探针）；
> 当前口径为 187 条 / 124 个目标类 / 76 个探针，见上方 2026-11-27 复评。

- **修复前现状**：`loaderTest` 源集（T4 建立）只加载 `CreativeModeInventoryScreen` 一个目标类，
  其余 mixin 的注入点没有自动化校验；MC 升级导致目标方法改名或签名变化时，`require >= 1`
  的注入点会让客户端在启动阶段抛 `InvalidInjectionException` 崩溃，`require = 0` 的探针点
  则静默失效，两者都要等到实机启动才暴露。
- **原影响**：主配置 145 个 mixin 覆盖 100 个去重目标类（其中 2 个目标是 MaLiLib 的
  `GuiConfigsBase` / `GuiListBase`）。任何一个 `require >= 1` 注入点失效，对应功能直接不可用
  或客户端无法启动；原有自动化只锁住 `CreativeModeInventoryGroupingMixin` 一条链路。
- **采用方案（编译产物解析 + 只加载不初始化 + 哨兵）**：
  - `AllMixinApplicationLoaderTest` 从 classpath 读取 `tweakify-mod.client.mixins.json`，
    遍历 145 个条目；
  - mixin 类本身不能 `Class.forName`（Mixin 抛 `IllegalClassLoadError`），改用 JDK 25 自带的
    `java.lang.classfile` 解析编译产物里的 `@Mixin`；`@Mixin` 是 CLASS retention，属性位于
    `RuntimeInvisibleAnnotations` 而不是 `RuntimeVisibleAnnotations`；
  - `value`（Class 常量，描述符形式，本项目主写法）与 `targets`（字符串数组）两种写法都支持；
  - 目标类以 `initialize=false` 加载：织入发生在 defineClass 阶段，跳过 `<clinit>` 后 headless
    环境没有不可加载的目标类，**可加载白名单为空**；
  - 哨兵：先加载 `CreativeModeInventoryScreen` 并断言 `tweakify$lastConfigSignature` 存在，
    证明主配置确实被 Loader 注册；否则遍历会在未织入的情况下空转通过；
  - 复用 T4 的 `loaderTest` 源集与 `check` 依赖（`build.gradle` L130），CI 的 `./gradlew build`
    自动执行，无需改 workflow。
- **覆盖边界**：
  - 覆盖：主配置 145 个 mixin、100 个目标类上 `require >= 1` 的全部注入点；目标类加载成功
    等价于该类上所有注入点在 APPLY 阶段通过（定位失败会抛 `InvalidInjectionException`）；
  - 豁免：73 个 `require = 0` 探针点（以 [`docs/mixin-probes.md`](mixin-probes.md) 为事实源）
    按设计允许静默跳过，不在本测试判定范围；它们的存活仍只能靠实机日志
    `Mixin hook active: <探针名>` 确认；
  - 不覆盖 `tweakify-mod.minihud.mixins.json` / `tweakify-mod.modmenu.mixins.json`：目标是
    MiniHUD/ModMenu 的类，不在 loaderTest 类路径，`required=false` 且由各自 plugin 在类
    存在时才启用；
  - 不覆盖运行期行为：只证明注入点存在，不证明逻辑在配置与状态下正确（由各自单测覆盖）；
  - 白名单用途：只允许放「headless 环境不可加载」的目标类，不得用于掩盖注入错误；
    当前为空，新增条目必须在注释里写明失败原因。
- **修复记录（提交 `36b7fa8`）**：新增
  `src/loaderTest/java/com/gtzc/tweakify/client/AllMixinApplicationLoaderTest.java`，
  与 T4 的 `CreativeModeInventoryGroupingMixinLoaderTest` 共用 `loaderTest` 任务。
- **验证**：`gradlew loaderTest` 2 例通过；`gradlew build` 通过（746 单测 + 2 loaderTest +
  6 GameTest）。变异验证：
  - `ChatScreenMixin` 的 `@Inject(method = "init")` 改为不存在的方法后，只有 `ChatScreen`
    目标失败并展开 `InvalidInjectionException: could not find any targets matching ...`，
    其余 99 个目标正常加载，证明是逐个目标真实校验而非整批跳过；
  - 从 `fabric.mod.json` 移除主 mixin 配置后，哨兵断言失败（「主 mixin 配置没有被 Fabric
    Loader 应用」），不会空转通过。
- **回归条件**：若删除或绕过该测试、把注解读取改回 `RuntimeVisibleAnnotations`
  （会全部报「没有类注解」）、更换哨兵目标或字段、或 `check` 不再依赖 `loaderTest`，重开本条；
  把 `require = 0` 改为 `require >= 1` 时该注入点自动进入覆盖范围，需确认构建通过并同步
  更新 [`docs/mixin-probes.md`](mixin-probes.md) 的统计。
- **补充门禁（2026-11-21，静态注入点校验）**：`require = 0` 的签名失配只能靠实机探针发现
  （v1.1.9.1 修复的定位栏头像即 26.3 把 `blitSprite` 从 7 参改为 6 参、注入目标仍写 7 参，
  头像从 1.1.7 起从未生效）。新增 `MixinInjectionTargetTest` 补齐静态校验：
  - 注入注解会被 Loom 的 Mixin AP 从 class 文件剥离，因此从源码解析（支持跨行 `+` 拼接、
    `{"a","b"}` 数组与常量引用跳过）；目标类仍从编译产物的 `@Mixin` 读取；
  - 校验 `method` / `@At.target` 的成员存在性、`@At(INVOKE/FIELD/NEW)` 的调用点（目标类
    字节码中的 invoke/field/new 指令），以及 `@WrapOperation` 处理器参数与 target 参数的
    子序列匹配（捕获「重载仍存在但注入到错误重载」）；
  - 覆盖 218 个注入点、115 个目标类；豁免 2 项（无 owner 的 NEW 描述符无法静态定位）；
  - 变异验证：`LocatorBarHeadsMixin` 的 `blitSprite` 改回 7 参后测试失败并点名处理器；
  - 边界：不证明运行期行为，`require = 0` 的探针存活仍需实机日志确认；
    可选模组目标类不可加载时计入豁免（当前为空）。

<a id="t9"></a>
## T9 详情（已修复）：跨包测试专用 public 成员

- **修复前现状**：client 包拆分（`876e343`）后，以下 6 个成员在 `src/main` / `src/client` 中没有生产调用，
  仅因跨包测试或根包 `LangFilesTest` 访问而放宽为 public：
  - `state/PiglinArmorSwapState.ARMOR_SLOTS` ← `config/PiglinArmorSwapModeTest`；
  - `ui/TweakifyModConfigListWidget.RIGHT_RESERVE` ← `config/FavoriteStarStyleTest`；
  - `render/FavoriteStarRenderer.rowStarX` ← `config/FavoriteStarStyleTest`；
  - `render/WorldPlayTimeRenderer.xForRightAligned` ← `state/WorldPlayTimeTest`；
  - `state/CreativeItemGroups.definitionsFor` ← 根包 `LangFilesTest`；
  - `config/CodeOptionEntry`（接口）← 根包 `LangFilesTest`（实现枚举同包，接口本身可保持包私有）。
  原 15 处同类放宽中，`BlockMarkerScanner.ScanJob`、`SectionKey`、两个构造器、`step` 与 4 个
  `*ForTest` 共 9 处已在 `BlockMarkerScannerGameTest` 迁入 `client.render` 后回退包私有。
- **原影响**：非测试代码可以访问这些成员，包边界被弱化；运行期行为与性能不受影响。
  风险在于后人把测试接缝当成正式 API 依赖，或在重构中删除/改名后仅由测试失败发现。
- **采用方案（测试归位 + 断言等价化 + 反射）**：
  1. `PiglinArmorSwapModeTest.autoAllowsEveryArmorSlot` 改为遍历 `EquipmentSlot` 四件套
     （常量内容已由 `PiglinArmorSwapStateTest.armorSlotsAreProcessedTopDown` 覆盖）；
  2. 配置行布局断言（`RIGHT_RESERVE`）移入 `ui/TweakifyModConfigScreenTest`；
     `starIsHiddenWhenRowCannotFitIt`（`rowStarX`）移入新建 `render/FavoriteStarRendererTest`；
  3. `xForRightAligned` 断言移入新建 `render/WorldPlayTimeRendererTest`；
  4. 默认组名语言键测试移入新建 `state/CustomGroupTranslationsTest`（与 `definitionsFor` 同包）；
  5. `LangFilesTest` 改为反射调用 `translationKey()`（`setAccessible(true)` 绕过包私有接口），
     源码扫描与语言键校验保持不变。
- **修复记录（2026-11-27，当前工作区改动，提交待定）**：6 处 `public` 全部回退包私有；
  新增 `render/FavoriteStarRendererTest`、`render/WorldPlayTimeRendererTest`、
  `state/CustomGroupTranslationsTest`；`FavoriteStarStyleTest` 移除两个布局测试，
  `WorldPlayTimeTest` / `LangFilesTest` / `PiglinArmorSwapModeTest` 同步精简。
- **验证**：`gradlew test` 861 例、`gradlew loaderTest` 2 例、`gradlew build`（6 GameTest）全部通过；
  6 处成员声明无 `public` 修饰符。
- **回归条件**：若 `src/main` / `src/client` 出现对这些成员的跨包调用（应转为正式 API 并补注释），
  或新增测试再次跨包访问时，重开本条。

<a id="t10"></a>
## T10 详情（已修复）：ConfigGuiTab 反依赖 UI 的 ConfigTab

- **修复前现状**：`config/ConfigGuiTab` 持有 `private final TweakifyModConfigScreen.ConfigTab tab`，
  构造器与 `tab()` 都引用 UI 包 `TweakifyModConfigScreen` 的嵌套枚举；
  `TweakifyModConfigScreen.initialTab()` 通过 `ConfigGuiTab.fromCode(...).tab()` 取分类。
  因此出现 `config → ui` 反向依赖，与 `ui → config` 形成包循环，`ConfigTab` 被迫 public。
- **原影响**：config 层无法脱离 UI 单独编译/测试；新增配置标签时需同时修改 UI 枚举与 config 枚举。
  运行期行为与性能不受影响；全仓 `config → ui` 仅 `ConfigGuiTab` 一处。
- **采用方案（UI 侧映射 + 枚举穷尽 switch）**：
  - `ConfigGuiTab` 只保留 `code()` 与 `translationKey()`，移除 `tab` 字段、构造参数与 `tab()`；
  - `TweakifyModConfigScreen` 新增包私有 `tabOfCode(String)`，对 `ConfigGuiTab.fromCode(code)`
    做 switch 映射——枚举穷尽 switch 让新增标签在编译期强制同步，未知 code 由 `fromCode` 回退 ALL；
  - `initialTab()` 改为 `tabOfCode(ConfigGuiTab.fromCode(...).code())`；
  - `ConfigTab` 收回包私有（包外已无引用）。
- **修复记录（2026-11-27，当前工作区改动，提交待定）**：`ConfigGuiTab` 去掉对 `client.ui` 的 import
  与 9 个 `ConfigTab` 构造参数；新增 `TweakifyModConfigScreen.tabOfCode`；`ConfigTab` 改包私有；
  `ConfigGuiTabTest` 移除 `tab()` 与数量断言，`TweakifyModConfigScreenTest` 新增
  `tabMappingCoversEveryConfigGuiTab`（逐 code 比对翻译键 + 未知 code 回退）。
- **验证**：`gradlew test` 861 例、`gradlew loaderTest` 2 例、`gradlew build`（6 GameTest）全部通过；
  `grep "client.ui." src/client/java/.../client/config` 为 0 处。
- **回归条件**：若 `client.config` 再次出现 `client.ui` 的 import，或新增 `ConfigGuiTab` 枚举值未同步
  `tabOfCode`（编译失败或映射缺失）时，重开本条。

<a id="t11"></a>
## T11 详情（已修复）：P2/P3 工程化与结构优化收尾

- **修复前现状**：P1/P2 审查清单中的工程化项在 2026-11-27 集中收尾前：
  - CI 只有 Linux 完整构建，Windows 路径/编码/大小写问题只能在本地或发布后暴露；
  - `LockedTradesPayload.CODEC` 只有生产调用，无空/present 状态的编解码回归；
  - 配置版本迁移与旧键清洗（约 120 行）集中在 `TweakifyModConfig`（1813 行）；
  - README「功能一览」与正文混排（747 行，其中表格约 500 行），检索与维护成本高；
  - GameTest 只能通过 `-x runGameTest` 跳过（任务名由插件生成，`-x` 失效时会报错）；
  - `client/state` 的 47 个 `*State` 类中 26 个无字段（纯规则/判断），命名与实际职责不符。
- **原影响**：Windows 平台问题缺少持续验证；网络包编解码无回归；配置类职责过载；
  README 过长；离线/仅改文档时构建仍需启动真实 GameTest 服务端；纯规则类的 `State`
  后缀误导读者。运行期行为不受影响。
- **采用方案**：
  1. CI 新增 `windows-compile` job（windows-latest + JDK 25，只跑三个源集编译，
     与 Linux build 并行）；
  2. `LockedTradesPayloadTest`：空 Optional 与 present 空列表的 codec 往返；
  3. 抽取 `ConfigMigration`：版本修正、版本 2/3/4 迁移、缺失列表默认值；
  4. README 功能表迁移到 [`docs/features.md`](features.md)，保留 `#features` 锚点，
     `ReadmeConfigTableTest` 改为解析新文件；
  5. 新增 `-PskipGameTest`（只禁用 `runGameTest` 执行，CI 默认仍执行）；
  6. 纯规则类 `*State` → `*Rules`：批次 1 22 个、批次 2 2 个；有字段状态类、
     `FlightDriftState` 状态机与 `SpawnFeatureState` 接线枢纽保留 `State`。
- **修复记录（2026-11-27）**：`3e8355a`（codec 测试）、`15acff0`（CI Windows job）、
  `6e4f062`（ConfigMigration）、`69aa1e4`（features.md）、`09cabc4`（skipGameTest）、
  `b1829bf`（*Rules 批次 1）、`b1c9454`（*Rules 批次 2）。
- **验证**：`gradlew test` 885 例、`gradlew loaderTest` 2 例、`gradlew build`（6 GameTest）
  全部通过；GitHub Actions `build` 与 `Windows compile` 均 success。
- **回归条件**：Windows runner 编译失败、`LockedTradesPayload` 协议变更无回归、
  新增配置版本迁移未在 `ConfigMigration` 接线、功能表与 `ConfigCatalog` 不一致、
  CI 误传 `-PskipGameTest`、纯规则类重新引入静态字段时，重开本条。
