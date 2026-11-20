# 技术债台账

记录已评估的技术债及其处置结论（暂缓 / 已修复）。沿用项目既有做法：先分析影响面与改动范围，再决定是否动手；
暂缓条目保留在此，出现真实失败案例或相关基础设施重建时重新评估；已修复条目保留修复与验证记录，供回归比对。

## 已评估条目

状态列取值：

- `暂缓，<理由>`：尚未做过实机复评；
- `暂缓，<理由>（最近复评 YYYY-MM-DD，见详情）`：已完成桌面复核，实机抽样进度见详情「待办」；
- `已缓解，待采样（<摘要>）（最近复评 YYYY-MM-DD，见详情）`：主要风险已由预防性措施消除，仅剩采样/复测确认；
- `已修复（<摘要>，见详情）`；
- `复测未达触发条件（YYYY-MM-DD，见详情）`。

模板约定（每条至少包含下列 bullet，缺失时写明「不适用」）：

- 暂缓条目：现状 / 失败模式（或影响）/ 触发条件 / 判定口径 / 闭环动作 / 缓解措施 / 最近评估（YYYY-MM-DD）/ 待办；
- 已修复条目：修复前现状 / 原影响 / 采用方案 / 修复记录（含提交）/ 验证 / 回归条件；
- 术语：状态列统一写「最近复评 YYYY-MM-DD」（`复测未达触发条件` 的日期即复测日期），详情统一写「最近评估（YYYY-MM-DD）」，旧的标「历史评估（YYYY-MM-DD）」；
- 优先级：P1 阻断 / P2 近期 / P3 观察，仅在详情中标注，不单列。

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| T3 | `BlockMarkerScanner` / `AttackRangeState` 的扫描与收集 | 65³ 方块扫描与实体收集仍全部在主线程分帧执行，未线程化 | 暂缓，快照方案复杂度高（最近复评 2026-11-20，见详情） |
| T4 | `CreativeModeInventoryGroupingMixin.tweakify$refreshOnConfigChange` | mixin 层配置热更新转发（`containerTick` 注入与 `refreshCurrentTabContents` 调用）无自动化覆盖，决策已抽到 `CreativeItemGroups.onConfigChanged` 并有单测 | 已修复（真实 Loader 集成 + 源码契约，见详情） |
| T5 | `LocatorBarHeads` / `StatusEffectTimerRenderer` / `DebugInfoOverlay` / `ItemStackEnchantmentTooltipMixin` / `ConnectedBlockOutlineState` | HUD/GUI 每帧重复解析与分配：定位栏名单、效果计时文本、调试行、附魔 tooltip、连接方块轮廓合并；可缓存化 | 已缓解，待采样（第 1–5 项已预防性实施，采样优先级低于 T3/T6；最近复评 2026-11-20，见详情） |
| T6 | `AttackRangeGeometryCache` / `BedrockGeometryCache` 重建 | 几何重建仍在主线程；输入为不可变数据，可移入后台 worker（配置序列化已于 2026-11-21 异步化，见详情） | 暂缓，几何重建未线程化（最近复评 2026-11-21，见详情） |
| T7 | `FavoriteStarRenderer` / `TweakifyModConfigOptionWidget` 的行内收藏星定位 | 极端窄窗口（4x/自动缩放 + 长名称）下星标被夹到行尾后落进重置按钮矩形，视觉重叠且点击被按钮优先命中 | 已修复（空间不足时隐藏星标，见详情） |

### 已闭环归档

| # | 位置 | 闭环摘要 | 详情 |
| --- | --- | --- | --- |
| T1 | `BlockMarkerScanner` 的 `ScanJob` 接线 | 真实 `LevelChunk` GameTest 覆盖预筛接线与读取数/section 集合 | 见下方 T1 详情（已归档） |
| T2 | `BedrockGeometryCache` / `AttackRangeGeometryCache` 的失效判定 | 发布侧摘要 + 精确比对，内容未变时复用几何 | 见下方 T2 详情（已归档） |

> v1.1.0 复评（2026-11-16）：T1 已通过真实 LevelChunk GameTest 闭环；T2 维持“已修复”；
> T3/T4 仍未达到闭环条件（分别缺少实机 profiler 与 Mixin 集成测试），本轮只更新 T3/T4 的评估记录与缓解说明。
> release action 的 Node.js 20 弃用项已闭环，记入 [`docs/release-pipeline.md`](release-pipeline.md)「已知但决定不改的点」L4。
>
> 2026-11-17 新增 T5/T6：分别覆盖 HUD/GUI 每帧重复计算与「几何重建 + 配置序列化」后台化候选。
> 两条均为桌面复核后的暂缓条目；T5 第 1–5 项已预防性实施（见详情），等待采样判定。
>
> v1.1.5（2026-11-20）新增 T7 并直接闭环：窄窗口下收藏星改为「放不下即隐藏」，不再夹到重置按钮上方；修复提交 `7b05164`，发布记录见 [`docs/release-notes/v1.1.5.md`](release-notes/v1.1.5.md)。
>
> v1.1.5 后复查（2026-11-20）：T1/T2 仍闭环，归档为「已闭环历史记录」；T5 五项预防性缓存均在，
> 降级为「已缓解，待采样」并调低采样优先级；T3/T4/T6 对照当前代码仍成立，已补充文件与代码位置依据
> （见各详情「最近评估（2026-11-20）」）。
>
> 2026-11-20 T4 闭环：转发条件收敛为 `CreativeItemGroups.shouldRefreshAfterConfigChange` 接缝，
> 新增接缝单测与 `CreativeModeInventoryGroupingMixinContractTest` 源码契约测试（提交 `78e411d`），
> 不再依赖手动回归。
>
> 2026-11-20 T4 真实加载升级：Fabric Loader JUnit 0.20.1 在独立 loaderTest 源集以 CLIENT
> 环境加载客户端 Mixin（`CreativeModeInventoryScreen` 已织入 `tweakify$lastConfigSignature`），
> 普通 test 保持 708 个用例；提交 `3559cb1`。
>
> 2026-11-21 T6 配置序列化落地：写盘任务内基于不可变 JSON 树执行 pretty printing，主线程只建树并提交；
> 新增内容/键序一致性、快照隔离与提交顺序测试，`gradlew test` 714 例与 `gradlew build`（loaderTest + 6 GameTest）通过。
> 几何重建仍暂缓。

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

## T3 详情：扫描与收集仍在主线程分帧执行

- **现状**：`BlockMarkerScanner` 的默认 65³（半径 32，可配置 8 ~ 32）方块扫描每 tick 推进 22 层
  （约 3 tick 一轮，单 tick 最多 9.3 万次位置遍历，section 预筛后真实方块读取更少）；
  `AttackRangeState` 的实体范围查询依赖 `ClientLevel`，只能在客户端主线程执行。
  两者都未线程化，也不使用原版 `Util.backgroundExecutor()`。
- **影响**：屏障/基岩密集场景（冒险地图、下界顶棚）按 65³（默认半径 32）全量遍历粗估每 tick 2–5 ms，未按 section 预筛命中率折算、
  也未经采样确认；若实测落在该区间上半段（≥ 2.5 ms，约占 50 ms tick 的 5%），`BlockMarkerScanner.tick`
  占比即触及 5% 触发线；具体以 `docs/t3-profiler-checklist.md` §五 判定为准。普通场景 section 预筛后开销很低。
  直接后台读 `ClientLevel`/`LevelChunk`/`PalettedContainer` 会与区块加载、卸载、光照写入竞争，
  可能读到半更新数据甚至越界，因此不能简单“把循环丢到线程池”。
- **采用方案**：暂不线程化，保持 section 预筛 + 每 tick 有界分帧；触发后先按建议方案 3 降负，再评估快照方案（P3 观察）。
- **建议方案**：
  1. 保持现状：section 预筛 + 每 tick 有界分帧是当前性价比最高的方案；
  2. 若将来需要线程化，采用“主线程快照 + 后台解析”：主线程分帧调用 `LevelChunkSection.copy()`
     （内部为 `PalettedContainer.copy()`）取 section 快照（65³ 体积约 125 个 section，每 tick 约 42 个、84–168 KB），
     后台线程解析快照产出标记，结果回主线程应用；快照记录基准 center，玩家移动超阈值或换世界时丢弃结果；
  3. 更简单的降负手段优先：降低刷新频率、缩小扫描半径（配置项 8 ~ 32）或降低标记上限。
- **触发条件**：实机在屏障/基岩密集或实体密集场景帧率明显下降，且 profiler 确认 `BlockMarkerScanner.tick`
  或 `AttackRangeState.tick` 占主线程超过约 5%；或收到“开启屏障可见后卡顿”的可复现反馈。
  在此之前不引入线程与快照生命周期管理。
- **判定口径（详见检查表）**：绝对占比 > 5% 触发 / 2%–5% 观察 / < 2% 且无复现反馈维持现状；
  「开启屏障可见后卡顿」的可复现反馈为独立触发路径，不要求占比达标。
  与 T1 的关系：T1 已由 GameTest 覆盖 section 预筛接线；本条仍用绝对占比判总成本，两者度量不同问题。
- **闭环动作**：达标先按建议方案 3 降负，不足再按“主线程快照 + 后台解析”预案实施；
  不达标把 T3 状态写为「复测未达触发条件（YYYY-MM-DD，见详情）」；重评后同步表格状态与日期（详见检查表 §七）。
- **缓解措施**：保持现状（section 预筛 + 每 tick 有界分帧）；触发后优先降负，见「建议方案」第 3 条。
- **历史评估（2026-11-16）**：v1.1.0 新增屏障/基岩扫描半径 8 ~ 32、基岩标记上限 1 ~ 256、
  屏障标记上限 16 ~ 256、粒子间隔 20 ~ 200 tick、攻击范围收集距离 8 ~ 128 与上限 8 ~ 256，
  为用户侧提供了更多降负手段；默认值仍为半径 32、上限 256/32、间隔 60 tick、收集 48/64，
  扫描与收集仍在主线程分帧执行，架构未变。未完成 A–E 实机采样，维持暂缓（P3）。
- **最近评估（2026-11-20）**：对照当前代码复查：`BlockMarkerScanner.java` 的
  `LAYERS_PER_TICK = 22`（L42–L43）、`tick`（L124）与 `ScanJob.advance` 的每 tick 22 层分帧
  （L256–L259）未变；`BlockMarkerScanner` / `AttackRangeState` / 两个 GeometryCache 中无
  `Executor` / `Thread` / `CompletableFuture` 用法（Grep 0 匹配），扫描与实体收集仍在主线程；
  `docs/t3-profiler-checklist.md` L92–L120 的 A–E 记录模板仍为空。架构未变、触发条件未采样，
  维持暂缓（P3）。
- **待办**：按检查表完成 A–E 采集并回填记录模板；未完成前 T3 维持暂缓（采样优先级高于 T5）。
- **验证清单**：实机 profiler 采集步骤、场景矩阵、数据清单与判定标准见
  [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md)。

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

## T5 详情（已缓解，待采样）：HUD/GUI 每帧重复计算

- **优先级**：P3 观察（已缓解，采样优先级低于 T3/T6）；触发后按 P2 近期处理。
- **现状**：以下路径原本都在渲染帧内执行，且每次重新解析/格式化/分配：
  - `LocatorBarHeads.renderDot` 每个航点每帧调用 `splitNames(...)`（`split` + `trim` + `toLowerCase`
    建列表），并按玩家名调用 `font.width`（已于 2026-11-17 加缓存）；
  - `StatusEffectTimerRenderer.render` 每帧复制并排序效果集合，为每个效果调用
    `EffectDurationFormatter.format` 并新建 `TextPlacement`（已于 2026-11-17 改为 tick 级缓存）；
  - `DebugInfoOverlay.buildLines` 每帧 3 次 `String.format`、一次生物群系解析与新建 `ArrayList`
    （实体/配置统计按 500ms 节流，2026-11-18 修复初值溢出导致的永不刷新；其余行由签名 + 500ms
    缓存覆盖）；
  - `ItemStackEnchantmentTooltipMixin.getTooltipLines` 在悬停期间每次提取都新建 3 个列表、逐行
    `getString()`、每个附魔 `Component.translatable` 并线性查找对应附魔行
    （已于 2026-11-18 加描述计划缓存）；
  - `ConnectedBlockOutlineState.mergedShape` 在准星命中门/床/双箱/双层植物时每帧 `Shapes.or` + `move`
    （已于 2026-11-18 加形状缓存）；
  可复用模板：`DurabilityOverlayRenderer` 已用 long key + 上限淘汰缓存文本与测宽结果。
- **影响**：Render thread 上的固定小额开销，单次微秒级，叠加后反映为 HUD 帧时间与年轻代分配；
  不会造成渲染错误。定位栏与 tooltip 的开销随在线玩家/附魔数量线性增长，人数多或附魔多时放大。
- **失败模式**：缓存键漏掉配置、语言、物品组件或方块状态变化时显示陈旧内容；缓存无界增长；
  整体清空式淘汰造成周期性重算抖动（`DurabilityOverlayRenderer` 的注释已记录这一点）。
- **触发条件**：Spark/JFR 过滤 Render thread 后，上述方法（含子调用）**合计**占比 > 2%；
  或收到可复现的「悬停 tooltip / HUD 掉帧」反馈（独立触发路径，不要求占比达标）。
- **判定口径**：合计 > 2% 实施 / 1%–2% 观察 / < 1% 且无复现反馈维持现状。
  采集步骤、线程名口径与记录格式沿用 [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md)
  §三、§四；采样时在数据清单中单列 `LocatorBarHeads.renderDot`、`StatusEffectTimerRenderer.render`、
  `DebugInfoOverlay.buildLines`、`ItemStack.getTooltipLines`、`ConnectedBlockOutlineState.mergedShape`。
- **闭环动作**：按项拆成独立可回滚提交，优先无跨帧状态依赖的项：
  1. 定位栏名单与文字宽度缓存（已完成预防性实施）；
  2. 状态效果计时改为 tick 更新、渲染帧查表（已完成预防性实施）；
  3. 附魔 tooltip 按「`ItemStack` 附魔组件 + sneak 状态」缓存（已完成预防性实施）；
  4. 调试叠加层整行按 500ms 缓存（已完成预防性实施）；
  5. 连接方块轮廓按 `(pos, state)` 缓存，准星切换或方块更新时失效（已完成预防性实施）。
  每项保留「缓存未命中路径与旧路径等价」的断言；完成后复测占比，达标则删除本条或标为已修复。
- **预防性实施（2026-11-17）**：第 1 项（定位栏包含/排除名单解析与玩家名文字宽度缓存）与
  第 2 项（药水效果计时的排序、格式化与 `TextPlacement` 改为 tick 级缓存）已先在
  `LocatorBarHeads` / `StatusEffectTimerRenderer` 落地：第 1 项按名单内容与 `Font` 实例失效，
  第 2 项按玩家/字体/GUI 宽度/演示模式失效。两者都是预防性改动，
  **不替代本节触发条件的采样判定**，其余 2 项仍按判定口径执行。
- **预防性实施（2026-11-18）**：第 3 项（附魔 tooltip 描述计划缓存）已下沉到
  `EnchantmentTooltipDescriptions`：按注册表/物品/附魔组件/组件类型/潜行门控/`Language` 失效，
  只缓存名称文本与描述组件，插入位置仍按当前 tooltip 行重新定位。该项同样是预防性改动，
  **不替代本节触发条件的采样判定**。
- **预防性实施（2026-11-18）**：第 4 项（调试叠加层整行文本缓存）已在 `DebugInfoOverlay` 落地：
  签名覆盖玩家位置/朝向/准星目标/世界/语言/子开关（世界/玩家/目标用身份哈希，不持引用），
  变化时立即重建，静止时每 500ms 刷新；功能关闭或玩家/世界为空时清空缓存。
  该项同样是预防性改动，**不替代本节触发条件的采样判定**。
- **预防性实施（2026-11-18）**：第 5 项（连接方块轮廓缓存）已在 `ConnectedBlockOutlineState` 落地：
  键包含准星 `(pos, state)`、邻居状态、世界/玩家身份哈希与门高度配置；邻居方块更新、准星切换、
  换世界/玩家时失效，8 条 LRU。该项同样是预防性改动，**不替代本节触发条件的采样判定**。
- **缺陷修复（2026-11-18）**：`DebugInfoOverlay` 的摘要节流原以 `Long.MIN_VALUE` 作为上次刷新时间，
  `now - last` 溢出使 `Entities:`/`Config:` 永不刷新；已改为带首次标记的 `SummaryThrottle`，
  首次立即刷新、后续按 500ms 间隔刷新，并补 `Long.MIN_VALUE` 防溢出回归测试。该缺陷在 T5 前已存在，
  不是缓存改动引入。
- **缓解措施**：实施前保持现状；用户侧关闭 `locatorBarHeads`、`statusEffectDurationDisplay`、
  `debugInfoOverlay` 可完全绕过对应开销。
- **历史评估（2026-11-18）**：桌面复核列出上述 5 个热点，均为每帧重复解析/分配；
  尚未实机采样；第 1–5 项已全部预防性落地，并修复调试摘要节流的既有溢出缺陷，等待采样判定。
- **最近评估（2026-11-20）**：对照当前代码复查：台账列出的 5 个热点均已落地预防性缓存——
  `LocatorBarHeads` 的 `NameListCache` / `WidthCache`（L79–L82、L352、L375）、
  `StatusEffectTimerRenderer.TimerCache`（L54、L251）、`EnchantmentTooltipDescriptions.PlanCache`
  （L49、L159）、`DebugInfoOverlay` 的 `LineCache` / `SummaryThrottle`（L55–L56、L88–L89）、
  `ConnectedBlockOutlineState` 的 `MergedShapeCache`（L35、L206），对应 5 个测试均存在。
  原始「每帧重复解析/分配」风险已消除，状态降级为「已缓解，待采样」；仅剩一次 Render thread
  采样确认，采样优先级低于 T3/T6。
- **待办**：完成一次 Render thread 采样并回填合计占比；已降级为「已缓解，待采样」，按结果决定
  标为已修复或复测未达触发条件。
- **验证方式**：`LocatorBarHeadsTest` 已覆盖名单缓存复用/内容变化失效、宽度缓存字体变化失效与
  淘汰后取值正确；`StatusEffectTimerRendererTest` 已覆盖缓存键失效、tick 构建与无缓存
  `placementFor` 等价、无尽效果占位、阈值/隐藏图标过滤、演示偏移与按秒格式化；
  `EnchantmentTooltipDescriptionsTest` 已覆盖缓存键六个维度失效、LRU 上限与命中复用、
  同名附魔行配对、尾部回退以及与无缓存构建的文本等价；
  `DebugInfoOverlayTest` 已覆盖整行缓存同签名复用、签名变化/间隔到期重建、reset 与无缓存等价，
  以及摘要节流首次/间隔/`Long.MIN_VALUE` 防溢出；
  `ConnectedBlockOutlineStateTest` 已覆盖形状缓存命中、准星切换/邻居更新/换世界或玩家失效、
  reset 重建与无缓存形状等价；缓存必须有上限与半量淘汰（参照 `DurabilityOverlayRenderer`），
  并断言「淘汰后结果与无缓存一致」；tooltip 用现有提示行序断言校验追加位置不变；
  `gradlew test` 与 `gradlew build`。

## T6 详情：几何重建仍在主线程（配置序列化已异步化）

- **优先级**：P3 观察；几何项达标后按 P2 近期处理。
- **现状**：
  - `AttackRangeGeometryCache.ensure` 在条目列表、显示/渲染模式或颜色变化时重建：每个条目
    3 档 LOD（默认 64、上限 256 条），每次新建 `EntryGeometry` 与 3 个 `CachedGeometry` 数组；
  - `BedrockGeometryCache.ensure` 在标记列表或颜色变化时重建，每个基岩最多 2 个可见面 × 24 顶点
    （默认 32、上限 256）；
  - `TweakifyModConfig.prepareSaveHandle` / `submitSaveAsyncHandle` 与
    `TweakifyModConfigOptions.flushPendingSaveNow` 自 2026-11-21 起只在配置锁内构建不可变 JSON 树
    （`ConfigStorage.toJsonTree(Map)`），提交时仍分配落盘序号；pretty printing（`GSON.toJson`）与
    磁盘 IO 都在写盘任务内执行，写盘线程不再读取配置字段值（只读 `TweakifyMod.CONFIG` 引用与路径）；
    `writeSynchronously()` 保留主线程序列化作为退出兜底（见「实施记录」）。
  已有基础设施：单线程 `AsyncFileWriter`、提交序号与 `Snapshot` 回滚；T2 的摘要去重已闭环，
  T3 的扫描快照方案与本条独立，不在此重复。
- **影响**：几何重建默认在内容变化时以 6/12 Hz 触发，高上限（256/256）时单次可达十万级顶点计算
  与数百 KB 数组垃圾；不影响正确性，但属于可感知的周期性卡顿来源。配置序列化的 pretty printing
  与磁盘 IO 已移出主线程，主线程只剩建树。
- **失败模式**：
  - 几何：后台构建期间渲染线程读到半构建数组，导致渲染错误或崩溃；
  - 几何：过期结果覆盖新结果（玩家移动、换世界、改颜色/模式、配置回滚）；
  - 几何：退出时后台任务未排空，丢失最后一次写盘；
  - 配置：快照树与直接 `toJson` 的键序/null 跳过规则漂移，导致配置文件格式变化——已由
    `snapshotJsonTreeMatchesDirectToJson` 锁定；
  - 配置：任务执行期间读取配置字段，把提交后的新值写进本次落盘——已由
    `prepareSaveHandleWritesSnapshotTakenAtSubmitTime` / `flushPendingSaveNowWritesSnapshotTakenAtSubmitTime` 锁定。
- **触发条件**：Spark/JFR 中 `AttackRangeGeometryCache.rebuild` + `BedrockGeometryCache.rebuild`
  合计 > 1% Render thread（沿用 T2 的回归线）；或实机把标记/条目上限调到 256
  并同时开启多个渲染功能。（配置序列化项已完成，不再作为触发条件。）
- **判定口径**：几何 1% 触发 / 0.5%–1% 观察 / < 0.5% 维持。
- **闭环动作**：
  1. 几何：引入单个 daemon worker 与双缓冲；后台构建不可变快照（`EntryGeometry` 表或
     `CachedGeometry` + 段表），用 `AtomicReference` 与 generation 发布，渲染线程只读当前快照；
     换世界、功能关闭、颜色/模式变化或退出时丢弃过期结果；缓存失效仍按引用相等，保留 T2 摘要去重；
  2. 配置（已完成，2026-11-21）：写盘任务内基于不可变 JSON 树执行 pretty printing，提交序号仍在
     提交时分配；退出兜底的 `writeSynchronously()` 保留主线程序列化；
  3. 两项分开提交，不合并为一个改动。
- **缓解措施**：几何继续用摘要去重 + LOD + 上限，用户侧可降低上限或关闭对应渲染功能。
- **实施记录（2026-11-21，待提交）**：
  - `ConfigStorage` 新增基于快照值表的 `toJsonTree(Map)` 与写盘重载
    `writeAtomically(JsonObject, Path, long)`：树在配置锁内一次性生成，任务内只做 pretty printing
    与落盘，序列化异常转为失败返回值；
  - `TweakifyModConfig` 新增 `toJsonTreeSnapshot()` / `writeJsonTree(...)`；
    `prepareSaveHandle` / `submitSaveAsyncHandle` 改为快照树路径，`writeSynchronously()` 保持不变；
  - `TweakifyModConfigOptions.flushPendingSaveNow` 改为先建快照树再入队，建树失败仍走原回滚 +
    `syncFromConfig` 路径；`writeConfig` 后台任务只接收冻结的树；
  - 验证：`gradlew test` **714** 个用例通过（新增 6 条）；`gradlew build` 通过 loaderTest 与 6 个 GameTest。
- **历史评估（2026-11-17）**：桌面复核确认两处输入均为不可变数据（发布后的 `List.copyOf` 与
  `Snapshot`），可安全移出主线程；未采样、未实施。
- **历史评估（2026-11-20）**：对照当时代码复查：两个几何缓存仍在主线程执行，无 `Executor` /
  `Thread` / `CompletableFuture` 用法；`flushPendingSaveNow` 仍在客户端线程调用 `config.toJson()`；
  `prepareSaveHandle` 仍在配置锁内同步序列化。两项均未后台化、触发条件未采样。
- **最近评估（2026-11-21）**：配置序列化已落地并通过一致性/隔离/提交顺序测试；几何两项仍无
  `Executor` / `Thread` / `CompletableFuture` 用法，维持暂缓（P3）。
- **待办**：采样几何两项占比；达标先做双缓冲改造并补并发测试；配置项观察发版后是否出现格式漂移反馈。
- **验证方式**：`AttackRangeGeometryCacheTest`、`BedrockGeometryCacheTest` 断言顶点数、LOD 选档与
  缓存失效；新增并发测试用有界等待验证「旧任务结果不覆盖新快照」与「关闭后无残留线程」；
  `TweakifyModConfigTest` 的 `snapshotJsonTreeMatchesDirectToJson` /
  `snapshotJsonTreeFreezesValuesAtBuildTime` / `prepareSaveHandleWritesSnapshotTakenAtSubmitTime` /
  `prepareSaveHandleSubmitsInCallOrder`（排队旧快照 vs 同步兜底写，序号必须在提交时分配）、
  `ConfigStorageTest.writeAtomicallyWithJsonTreeWritesFormattedContent`、
  `TweakifyModConfigOptionsTest.flushPendingSaveNowWritesSnapshotTakenAtSubmitTime` 锁定配置序列化路径；
  手动在 256 上限下跑一次渲染回归；`gradlew test` 与 `gradlew build`。

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

## 与既有清单的关系

| 文件 | 范围 |
| --- | --- |
| `docs/release-pipeline.md`「已知但决定不改的点」 | CI/发布流水线（L1–L4、H1–H2） |
| `docs/mixin-probes.md`「已知不改清单」 | HookProbe 探针覆盖 |
| `docs/t3-profiler-checklist.md` | T3 的实机 profiler 采集步骤、场景矩阵与判定标准；T5/T6 复用其 §三/§四 的采集步骤与线程名口径，不新增独立清单；T1 已由 GameTest 闭环，A/B/C 仅保留作可选的历史性能对比 |
| `README.md`「功能一览」 | 屏障/基岩/攻击范围的半径、刷新频率与上限的用户侧声明（本文件的事实源之一） |
| 本文件 | 测试覆盖与性能取舍类技术债（现行 T3–T7；T1/T2 已闭环归档）；不搬运上述条目，避免两处维护漂移 |
