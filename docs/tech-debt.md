# 技术债台账

记录已评估但决定暂缓处理的技术债。沿用项目既有做法：先分析影响面与改动范围，再决定是否动手；
暂不修复的条目保留在此，出现真实失败案例或相关基础设施重建时重新评估。

## 已评估条目

状态列取值：`暂缓，<理由>` / `已修复（<摘要>，见详情）` / `复测未达触发条件（YYYY-MM-DD，见 T3 验证清单）`。

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| T1 | `BlockMarkerScanner` 的 `ScanJob` 接线 | section 预筛缺少真实 `LevelChunk` 集成测试：`chunkMayContain[slot] = true` 类变异无法被单测捕获 | 暂缓，不补 GameTest |
| T2 | `BedrockGeometryCache` / `AttackRangeGeometryCache` 的失效判定 | 顶点缓存以列表引用为唯一失效条件，每秒 6/12 次整表重建，内容未变时也重建 | 已修复（摘要去重 + 精确比对，见详情） |
| T3 | `BlockMarkerScanner` / `AttackRangeState` 的扫描与收集 | 65³ 方块扫描与实体收集仍全部在主线程分帧执行，未线程化 | 暂缓，快照方案复杂度高 |

## T1 详情：section 预筛的集成测试缺口

- **现状**：`sectionNeedsScan(...)` 的纯函数决策（屏障/基岩开关组合、section 索引越界、null section）已有 21 个单测覆盖；
  未覆盖的是 `ScanJob` 中一行接线 `chunkMayContain[slot] = BlockMarkerScanner.sectionNeedsScan(chunk, y, this.target)`，
  以及它与真实 `LevelChunk.getSectionIndex/getSectionsCount/getSection` 的交互。
- **失败模式**：把该行改成 `= true` 只是取消预筛，扫描到的方块与最终标记结果仍然正确，
  只会让方块读取量与耗时回到优化前的水平——属于**性能回退**，不是功能错误。
- **不补 GameTest 的理由**：
  1. 功能正确性风险为零：被跳过的 section 在原逐方块逻辑下也不会产出任何标记（纯函数决策 + `maybeHas` 语义已论证）；
  2. 项目已刻意移除自动 GameTest（客户端启动、依赖与测试世界构造都要重建，CI 不再启动客户端），
     为捕获一个常量变异重建整套基础设施，维护成本远高于收益；
  3. 该变异只可能由人为删除优化引入，diff 中只有一行，代码审查成本极低。
- **缓解措施**：
  - 赋值处已有注释说明「false 时整段跳过方块读取」，误改时容易被审查注意到；
  - 若未来为更高价值的端到端场景重建 GameTest 基础设施，顺带补一条「屏障/基岩 section 跳过」场景，边际成本才低；
  - 实机排查性能回退时，可参考 v1.0.3 验证期间使用的 `[perf-sample]` 统计方式（已在合入前移除）。
- **最近评估（2026-11-15）**：触发条件当前不成立——`chunkMayContain` 赋值与注释仍在 `BlockMarkerScanner.scanLayer` 中，
  1.0.4/1.0.5 的扫描改造未触碰该行，且无性能回退报告；项目未重建 GameTest。
  结论：维持“暂缓，不补 GameTest”（优先级 P3）。
  后续观察条件：出现真实误删/改坏导致的可感知性能回退，或为其他高价值场景重建 GameTest 基础设施时重新评估。

## T2 详情：顶点缓存的失效粒度过粗

- **现状**：`BedrockGeometryCache` / `AttackRangeGeometryCache` 以“列表引用是否变化”作为唯一失效条件。
  `BlockMarkerScanner` 每轮扫描结束、`AttackRangeState` 每 1.67 tick 都会发布新的 `List.copyOf` 实例，
  因此缓存按每秒 6/12 次整表重建——即使玩家静止、实体与标记位置完全没变。
- **影响**：每次重建都重新计算全部段与三档 LOD 顶点（最坏 256 个基岩标记 × 48 顶点、256 个条目 × 3 档几何），
  并重新填充数组。属 CPU 与 GC 压力，不会造成渲染错误；相对改造前“每帧重建”仍是约 5–10 倍的净收益，
  默认上限（基岩 32、攻击范围 64）下量级很小。
- **建议方案**：
  1. 在发布处做轻量内容摘要（size + 全量几何字段）比对，摘要相同后再精确比对，内容未变则复用旧列表引用；
  2. 或让 `AttackRangeState` / `BedrockHighlightState` 维护单调递增的 `geometryVersion`，缓存比较版本号而非引用；
  3. 或缓存额外比较条目数 + 少量摘要字段后再决定是否重建。
  三种方案都不改变“引用变化即失效”的契约注释，只减少无效重建。
- **触发条件**：实机出现 GC 抖动或帧时间周期性尖峰，且 profiler 显示几何重建占 tick 时间超过约 1%；
  或把基岩/攻击范围上限调到 256 并同时开启多个渲染功能时。在此之前保持现状。
- **修复记录**：`BedrockHighlightState.setMarkers` 与 `AttackRangeState.publishEntries` 在发布前计算内容摘要
  （基岩：size + `Marker.hashCode()`；攻击范围：size + 坐标/半径，不含随玩家移动变化的 `distanceSq`），
  摘要相同后再做一次精确比对（基岩逐条 `equals`，攻击范围只比几何字段），两者都相同才保留旧引用；
  因此哈希碰撞不会漏判内容变化，最多损失一次重建收益，几何不会停在旧内容。
  摘要比较使用 `Objects.equals(publishedHash, hash)`，可空 `Integer` 与 `int` 混用不会退化为引用比较。
  清理路径：攻击范围关闭功能与 `resetForTest()` 共用 `resetState()`，基岩关闭功能走 `clear()`，
  两者都会把摘要置空（`publishedHash = null`）。
  攻击范围按距离排序时用 `x → feetY → z → radius` 作并列规则，等距实体的顺序确定，
  不会因迭代顺序变化而误重建或让最近目标在轮次间抖动。

## T3 详情：扫描与收集仍在主线程分帧执行

- **现状**：`BlockMarkerScanner` 的 65³ 方块扫描每 tick 推进 22 层（约 3 tick 一轮，单 tick 最多 9.3 万次位置遍历，
  section 预筛后真实方块读取更少）；`AttackRangeState` 的实体范围查询依赖 `ClientLevel`，只能在客户端主线程执行。
  两者都未线程化，也不使用原版 `Util.backgroundExecutor()`。
- **影响**：屏障/基岩密集场景（冒险地图、下界顶棚）主线程估算每 tick 2–5 ms；普通场景 section 预筛后开销很低。
  直接后台读 `ClientLevel`/`LevelChunk`/`PalettedContainer` 会与区块加载、卸载、光照写入竞争，
  可能读到半更新数据甚至越界，因此不能简单“把循环丢到线程池”。
- **建议方案**：
  1. 保持现状：section 预筛 + 每 tick 有界分帧是当前性价比最高的方案；
  2. 若将来需要线程化，采用“主线程快照 + 后台解析”：主线程分帧调用 `LevelChunkSection.copy()`
     （内部为 `PalettedContainer.copy()`）取 section 快照（65³ 体积约 125 个 section，每 tick 约 42 个、84–168 KB），
     后台线程解析快照产出标记，结果回主线程应用；快照记录基准 center，玩家移动超阈值或换世界时丢弃结果；
  3. 更简单的降负手段优先：降低刷新频率、缩小扫描半径或降低标记上限。
- **触发条件**：实机在屏障/基岩密集或实体密集场景帧率明显下降，且 profiler 确认 `BlockMarkerScanner.tick`
  或 `AttackRangeState.tick` 占主线程超过约 5%；或收到“开启屏障可见后卡顿”的可复现反馈。
  在此之前不引入线程与快照生命周期管理。
- **验证清单**：实机 profiler 采集步骤、场景矩阵、数据清单与判定标准见
  [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md)。

## 与既有清单的关系

| 文件 | 范围 |
| --- | --- |
| `docs/release-pipeline.md`「已知但决定不改的点」 | CI/发布流水线（L1–L3、H1–H2） |
| `docs/mixin-probes.md`「已知不改清单」 | HookProbe 探针覆盖 |
| `docs/t3-profiler-checklist.md` | T3 的实机 profiler 采集步骤、场景矩阵与判定标准 |
| 本文件 | 测试覆盖与性能取舍类技术债（T1–T3）；不搬运上述条目，避免两处维护漂移 |
