# 技术债台账

记录已评估但决定暂缓处理的技术债。沿用项目既有做法：先分析影响面与改动范围，再决定是否动手；
暂不修复的条目保留在此，出现真实失败案例或相关基础设施重建时重新评估。

## 已评估条目

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| T1 | `BlockMarkerScanner` 的 `ScanJob` 接线 | section 预筛缺少真实 `LevelChunk` 集成测试：`chunkMayContain[slot] = true` 类变异无法被单测捕获 | 暂缓，不补 GameTest |

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

## 与既有清单的关系

| 文件 | 范围 |
| --- | --- |
| `docs/release-pipeline.md`「已知但决定不改的点」 | CI/发布流水线（L1–L3、H1–H2） |
| `docs/mixin-probes.md`「已知不改清单」 | HookProbe 探针覆盖 |
| 本文件 | 新增的测试覆盖类技术债；不搬运上述条目，避免两处维护漂移 |
