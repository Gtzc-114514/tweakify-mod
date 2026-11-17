# 技术债台账

记录已评估的技术债及其处置结论（暂缓 / 已修复）。沿用项目既有做法：先分析影响面与改动范围，再决定是否动手；
暂缓条目保留在此，出现真实失败案例或相关基础设施重建时重新评估；已修复条目保留修复与验证记录，供回归比对。

## 已评估条目

状态列取值：

- `暂缓，<理由>`：尚未做过实机复评；
- `暂缓，<理由>（最近复评 YYYY-MM-DD，见详情）`：已完成桌面复核，实机抽样进度见详情「待办」；
- `已修复（<摘要>，见详情）`；
- `复测未达触发条件（YYYY-MM-DD，见详情）`。

模板约定（每条至少包含下列 bullet，缺失时写明「不适用」）：

- 暂缓条目：现状 / 失败模式（或影响）/ 触发条件 / 判定口径 / 闭环动作 / 缓解措施 / 最近评估（YYYY-MM-DD）/ 待办；
- 已修复条目：修复前现状 / 原影响 / 采用方案 / 修复记录（含提交）/ 验证 / 回归条件；
- 术语：状态列统一写「最近复评 YYYY-MM-DD」（`复测未达触发条件` 的日期即复测日期），详情统一写「最近评估（YYYY-MM-DD）」，旧的标「历史评估（YYYY-MM-DD）」；
- 优先级：P1 阻断 / P2 近期 / P3 观察，仅在详情中标注，不单列。

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| T1 | `BlockMarkerScanner` 的 `ScanJob` 接线 | section 预筛缺少真实 `LevelChunk` 集成测试：`chunkMayContain[slot] = true` 类变异无法被单测捕获 | 已修复（真实 LevelChunk GameTest，见详情） |
| T2 | `BedrockGeometryCache` / `AttackRangeGeometryCache` 的失效判定 | 顶点缓存以列表引用为唯一失效条件，每秒 6/12 次整表重建，内容未变时也重建 | 已修复（摘要去重 + 精确比对，见详情） |
| T3 | `BlockMarkerScanner` / `AttackRangeState` 的扫描与收集 | 65³ 方块扫描与实体收集仍全部在主线程分帧执行，未线程化 | 暂缓，快照方案复杂度高（最近复评 2026-11-16，见详情） |
| T4 | `CreativeModeInventoryGroupingMixin.tweakify$refreshOnConfigChange` | mixin 层配置热更新转发（`containerTick` 注入与 `refreshCurrentTabContents` 调用）无自动化覆盖，决策已抽到 `CreativeItemGroups.onConfigChanged` 并有单测 | 暂缓，测试基础设施不加载 Mixin（最近复评 2026-11-16，见详情） |

> v1.1.0 复评（2026-11-16）：T1 已通过真实 LevelChunk GameTest 闭环；T2 维持“已修复”；
> T3/T4 仍未达到闭环条件（分别缺少实机 profiler 与 Mixin 集成测试），本轮只更新 T3/T4 的评估记录与缓解说明。
> release action 的 Node.js 20 弃用项已闭环，记入 [`docs/release-pipeline.md`](release-pipeline.md)「已知但决定不改的点」L4。

## T1 详情：section 预筛的集成测试缺口

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

## T2 详情：顶点缓存的失效粒度过粗

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
- **历史评估**：不适用（尚未做任何实机复评，检查表记录模板为空）。
- **最近评估（2026-11-16）**：v1.1.0 新增屏障/基岩扫描半径 8 ~ 32、基岩标记上限 1 ~ 256、
  屏障标记上限 16 ~ 256、粒子间隔 20 ~ 200 tick、攻击范围收集距离 8 ~ 128 与上限 8 ~ 256，
  为用户侧提供了更多降负手段；默认值仍为半径 32、上限 256/32、间隔 60 tick、收集 48/64，
  扫描与收集仍在主线程分帧执行，架构未变。未完成 A–E 实机采样，维持暂缓（P3）。
- **待办**：按检查表完成 A–E 采集并回填记录模板；未完成前 T3 维持暂缓。
- **验证清单**：实机 profiler 采集步骤、场景矩阵、数据清单与判定标准见
  [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md)。

## T4 详情：mixin 层配置热更新转发无自动化覆盖

- **现状**：配置变化后“是否重建当前页”的决策已下沉到 `CreativeItemGroups.onConfigChanged`，
  由 `CreativeItemGroupsTest.onConfigChangedRefreshesOnlyCategoryTabs` 覆盖分类页重建、
  非分类页只告警且不清空组状态、null 分支。
  `CreativeModeInventoryGroupingMixin.tweakify$refreshOnConfigChange` 只剩签名比对、调用 `onConfigChanged`
  与按返回值刷新列表；本条只描述该转发链路。
- **失败模式**：转发代码被误删、条件写反（如常数 false）或刷新调用被移除时，单元测试全绿；
  实机表现为配置变化后物品列表不刷新，或非分类页误清空 `currentTab`/渲染快照，
  导致组图标与点击判定错位。
- **触发条件**：修改 `CreativeModeInventoryGroupingMixin` 的 `containerTick` 注入或
  `refreshCurrentTabContents` 调用；或其它模组重写 `CreativeModeInventoryScreen.containerTick`
  导致注入失效（该注入未设 `require = 0`，注入失败会在启动期报错，不属于静默失效）。
- **判定口径**：代码审查 + 实机手动回归：在分类页切换排序/总开关应立即重建列表；
  在搜索页修改配置不应清空搜索结果；在快捷栏/背包页修改配置不应清空预览；
  日志应出现对应告警且只出现一次。
- **闭环动作**：若将来引入可加载 Mixin 的集成测试（Fabric Loader JUnit 或 GameTest），
  补一条“配置变化 → 分类页刷新 / 非分类页仅告警”的转发测试并删除本条；
  否则每次改动该 mixin 按判定口径手动回归。
- **缓解措施**：转发保持单条语句 `if (selectedTab != null && onConfigChanged(selectedTab)) refreshCurrentTabContents(...)`，
  决策与副作用集中在可单测的 `onConfigChanged`；`itemGroups` 的 HookProbe 只能确认点击钩子，转发链路仍需按判定口径手动回归。
- **历史评估（2026-11-15）**：`onConfigChanged` 的三个分支已由单测覆盖；mixin 转发在一行内，
  本次重构已核对与抽取前逐分支等价。项目未引入 Mixin 集成测试，维持暂缓。
- **最近评估（2026-11-16）**：v1.1.0 未修改 `CreativeModeInventoryGroupingMixin` 与
  `CreativeItemGroups.onConfigChanged`，本轮新增的 51 个子设置也不涉及物品分组热更新。
  项目仍未引入 Mixin 集成测试，mixin 转发链路继续依赖手动回归，维持暂缓。
- **待办**：实机回归一次搜索/快捷栏/背包页的配置变化路径并记录结果。

## 与既有清单的关系

| 文件 | 范围 |
| --- | --- |
| `docs/release-pipeline.md`「已知但决定不改的点」 | CI/发布流水线（L1–L4、H1–H2） |
| `docs/mixin-probes.md`「已知不改清单」 | HookProbe 探针覆盖 |
| `docs/t3-profiler-checklist.md` | T3 的实机 profiler 采集步骤、场景矩阵与判定标准；T1 已由 GameTest 闭环，A/B/C 仅保留作可选的历史性能对比 |
| `README.md`「功能一览」 | 屏障/基岩/攻击范围的半径、刷新频率与上限的用户侧声明（本文件的事实源之一） |
| 本文件 | 测试覆盖与性能取舍类技术债（T1–T4）；不搬运上述条目，避免两处维护漂移 |
