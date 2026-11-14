# T3 实机 profiler 验证检查表

本清单对应 [`docs/tech-debt.md`](tech-debt.md) 的 T3（扫描与收集仍在主线程分帧执行）。
T3 的启动条件是“profiler 确认 `BlockMarkerScanner.tick` 占主线程超过约 5%”，
在完成本清单的采集与判定前，T3 保持暂缓。

## 一、前置条件

- 客户端版本与待测提交一致：记录 `git rev-parse --short HEAD` 与 `gradle.properties` 的 `version`；
- 安装 Spark（推荐）或准备 JFR 启动参数；
- 固定配置：屏障/基岩扫描半径 32、刷新 6 次/秒、标记上限使用默认值，避免场景间配置漂移；
- 记录硬件：CPU 型号、内存、图形后端（OpenGL/Vulkan）、游戏内渲染距离。

## 二、场景矩阵

每个场景采集“静止 30 秒 + 移动 30 秒”，重复 3 次（冷启动 1 次、热缓存 2 次）：

| # | 场景 | 功能配置 | 目的 |
| --- | --- | --- | --- |
| A | 屏障密集自制地图（约 1000 个屏障） | 只开屏障可见 | 最坏屏障场景 |
| B | 下界顶棚（单层基岩，y≈127） | 只开基岩高亮 | 最坏基岩场景 |
| C | A + B 同时开启 | 两项全开 | 合并扫描负载 |
| D | 普通主世界空旷区 | 两项全关 | 对照组 |
| E | 刷怪场或村庄（≥64 生物） | 攻击范围开启 | 实体收集侧 |

## 三、采集步骤

### Spark（推荐）

1. `/spark health` 记录整体 TPS/MSPT；
2. 进入场景后执行 `/spark profiler start --timeout 120`；
3. 静止 60 秒后开始移动 60 秒（或按场景矩阵分段采集）；
4. `/spark profiler stop`，保存火焰图链接；
5. `/spark tickmonitor --threshold 0` 同步观察 tick 时间分布。

### JFR（备用）

启动参数加入：

```text
-XX:StartFlightRecording=filename=scan.jfr,duration=120s,settings=profile
```

用 JMC 打开 `scan.jfr`，过滤 `Render thread`，检索 `BlockMarkerScanner.tick` / `ScanJob.step` / `scanLayer`。

## 四、采集项与数据清单

| 项 | 说明 |
| --- | --- |
| 火焰图占比 | `Render thread` 中 `BlockMarkerScanner.tick`（含子调用）的累计采样百分比 |
| MSPT | `/spark health` 或 tickmonitor 的 p50 / p95 / p99 |
| FPS | 30 秒平均值与 1% low |
| 元数据 | 维度、坐标、种子、实体数、已加载区块数、CPU、渲染距离、图形后端 |
| 日志 | `logs/latest.log` 中的异常、GC 告警与 Tweakify 相关 warning |
| 原始文件 | Spark 链接或 JFR 文件名（原始大文件不入库，只记录链接/文件名与摘要） |

## 五、判定标准

| 结果 | 判定 |
| --- | --- |
| `BlockMarkerScanner.tick` 占比 **> 5%**，或 A/B/C 的 p95 MSPT 相对 D 增加 **≥ 2 ms 且可复现** | 满足 T3 触发条件，可按技术债中的“主线程快照 + 后台解析”预案启动改造 |
| 占比 **2%–5%** | 继续观察；优先考虑降低刷新频率、缩小扫描半径或降低标记上限 |
| 占比 **< 2%** | 维持现状，并在 `docs/tech-debt.md` 的 T3 条目记录“已验证不成立”及日期 |

## 六、记录模板

| 场景 | 版本/提交 | 配置 | 采样时长 | tick 占比 | MSPT p50/p95/p99 | FPS 均值/1% low | Spark/JFR | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A | | | | | | | | |
| B | | | | | | | | |
| C | | | | | | | | |
| D | | | | | | | | |
| E | | | | | | | | |

## 七、结果处理

- 达标：按 `docs/tech-debt.md` T3 的“主线程快照 + 后台解析”预案实施，先提交可回滚的分步计划再动手；
- 不达标：更新 T3 状态与日期，保留本表作为后续复测基线；
- 任何场景出现崩溃或明显卡顿：先记录 `logs/latest.log` 与 Spark/JFR 证据，再按“可复现卡顿反馈”条件重新评估。
