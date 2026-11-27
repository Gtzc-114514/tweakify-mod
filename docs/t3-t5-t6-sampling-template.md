# T3/T5/T6 实机采样记录模板

本文件用于补齐 [`docs/tech-debt.md`](tech-debt.md) 中 T3/T5/T6 尚未采集的项：
T3 的 B（下界顶棚）/E（实体密集）场景与 MSPT/FPS 1% low、T5 的 tooltip/定位栏复采、
T6 的 256 上限回归与写盘峰值。

- **通用口径**（场景矩阵、采集项、判定标准、线程名）见
  [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md) §三–§五，本文件不重复；
- 本文件只提供**可直接填写的表格**；采样明细回填到检查表 §六，`tech-debt.md` 只写中位与结论，
  避免两处维护漂移；
- 表格留空的单元格为待填项，`-` 表示本轮不适用，不要填 0。

## 一、采样元数据（每次会话填一次）

| 项 | 值 |
| --- | --- |
| 采样日期 |  |
| 模组版本 / 提交（`git rev-parse --short HEAD`） |  |
| Minecraft / Fabric Loader / Fabric API |  |
| CPU / 内存 |  |
| 图形后端（OpenGL/Vulkan）/ 分辨率 / 最大帧率 |  |
| 渲染距离 / 模拟距离 |  |
| Spark 版本（或 JFR） |  |
| 存档类型（超平坦/普通）/ 种子 / 关键坐标 |  |
| `pauseOnLostFocus` | false（必须） |

## 二、通用前置（逐项勾选）

- [ ] Spark 已安装（Modrinth 搜索 `spark`，下载 Fabric 版放入 `mods/`；T3/T5/T6 共用）
- [ ] 天气与时间固定：`/gamerule doDaylightCycle false`、`/gamerule doWeatherCycle false`、
      `/weather clear`、`/time set noon`
- [ ] 采样期间客户端保持前台（`pauseOnLostFocus=false`），日志无暂停记录
- [ ] 记录并固定本次使用的配置值（下表「配置」列逐场景填写）
- [ ] 每轮结束后保存 Spark 链接或 JFR 文件名到「证据」列

### 通用 Spark 命令（每轮）

```text
/spark health                              # 记录 TPS 与 MSPT p50/p95/p99
/spark profiler start --timeout 120        # 进入场景后启动，窗口 120 s
# 静止 60 s → 移动 60 s（W/S 摆动或绕圈）
/spark profiler stop                       # 保存链接
/spark tickmonitor --threshold-tick 50     # 只在 >50 ms 的 tick 输出，避免刷屏
```

JFR 备用（Spark 不可用时）：

```text
jcmd <pid> JFR.start name=scan settings=profile duration=120s filename=scan.jfr
jcmd <pid> JFR.stop name=scan
```

## 三、T3：B/E 场景（A/C/D 已有基线，可只在需要时复测）

### 3.1 场景配置与搭建

| 场景 | 搭建方式 | 功能配置 | 轮次 |
| --- | --- | --- | --- |
| B 下界顶棚 | 进入下界，找到 y≈127 的单层基岩顶棚（或 `/fill` 铺一层基岩） | 只开「高亮单层基岩」；扫描半径 24、刷新 4 Hz、标记上限 32 | 冷启动 1 轮 + 热缓存 2 轮 |
| E 刷怪场/村庄 | 村庄或自制刷怪场，生物数 ≥64（超过上限只保留最近 64） | 只开「攻击范围显示」；收集距离 48、上限 64 | 冷启动 1 轮 + 热缓存 2 轮 |
| A/C 复测（可选） | 沿用 [`t3-profiler-checklist.md`](t3-profiler-checklist.md) §二 | 屏障/基岩同前 | 仅当需要与旧基线对比时 |

每轮 = 静止 60 s + 移动 60 s；同一场景的冷启动轮与热缓存轮之间不重启客户端。

### 3.2 记录表（每行一轮，中位行最后填）

| 场景/轮次 | 日期 | 环境 | 提交 | 配置 | tick 占比 | avgLayerUs (µs) | MSPT p50/p95/p99 | FPS 均值 / 1% low | 证据（Spark/JFR） | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B-1（冷启动） |  | 单人 |  |  |  |  |  |  |  |  |
| B-2（热缓存） |  | 单人 |  |  |  |  |  |  |  |  |
| B-3（热缓存） |  | 单人 |  |  |  |  |  |  |  |  |
| **B 中位** |  |  |  |  |  |  |  |  | - |  |
| E-1（冷启动） |  | 单人 |  |  |  |  |  |  |  |  |
| E-2（热缓存） |  | 单人 |  |  |  |  |  |  |  |  |
| E-3（热缓存） |  | 单人 |  |  |  |  |  |  |  |  |
| **E 中位** |  |  |  |  |  |  |  |  | - |  |

- `tick 占比`：B 看 `BlockMarkerScanner.tick`（含子调用），E 看 `AttackRangeState.tick`；
- `avgLayerUs`：按检查表 §三 第 6 步的窗口法换算，B 适用、E 记 `不适用`；
- `MSPT` 来自 `/spark health`，为集成服务端 tick 时间，仅作旁证。

### 3.3 判定（写结论用）

| 结果 | 结论 |
| --- | --- |
| 任一 tick 方法 **> 5%**，或帧时间相对 D 明显恶化且可复现 | 触发：先按建议方案 3 降负（刷新频率/半径/上限），不足再评估快照方案 |
| **2%–5%** 且无复现反馈 | 观察：优先降负，状态维持 |
| **< 2%** 且无复现反馈 | 维持现状，台账 T3 写「复测未达触发条件（YYYY-MM-DD，见详情）」 |
| 收到「开启屏障可见后卡顿」可复现反馈 | 独立触发，不要求占比达标 |

## 四、T5：tooltip 与定位栏复采

### 4.1 场景配置

| 场景 | 准备 | 采集方法（Render thread 过滤） | 轮次 |
| --- | --- | --- | --- |
| tooltip 多附魔 | 一把 ≥5 个附魔的剑（或 `/give` 指定附魔），悬停 60 s | `ItemStack.getTooltipLines`（整体）与 `EnchantmentTooltipDescriptions`（模组缓存） | 3 轮 |
| 定位栏多人 | 2 个客户端 + 局域网内 ≥10 名玩家（或 `/summon` 假玩家不可行时用真实多开） | `LocatorBarHeads.renderDot` | 3 轮 |
| 调试层（可选复测） | 开启 `debugInfoOverlay` | `DebugInfoOverlay.render` / `buildLines` | 3 轮 |

### 4.2 记录表

| 场景 | 轮次 | 方法 | 占比 | 证据（Spark/JFR） | 备注 |
| --- | --- | --- | --- | --- | --- |
| tooltip 多附魔 | 1 | `ItemStack.getTooltipLines` |  |  |  |
| tooltip 多附魔 | 1 | `EnchantmentTooltipDescriptions` |  |  |  |
| tooltip 多附魔 | 2 | `ItemStack.getTooltipLines` |  |  |  |
| tooltip 多附魔 | 2 | `EnchantmentTooltipDescriptions` |  |  |  |
| tooltip 多附魔 | 3 | `ItemStack.getTooltipLines` |  |  |  |
| tooltip 多附魔 | 3 | `EnchantmentTooltipDescriptions` |  |  |  |
| 定位栏多人 | 1 | `LocatorBarHeads.renderDot` |  |  |  |
| 定位栏多人 | 2 | `LocatorBarHeads.renderDot` |  |  |  |
| 定位栏多人 | 3 | `LocatorBarHeads.renderDot` |  |  |  |
| 调试层（可选） | 1 | `DebugInfoOverlay.render` |  |  |  |
| 调试层（可选） | 1 | `DebugInfoOverlay.buildLines` |  |  |  |

### 4.3 判定（写结论用）

| 结果 | 结论 |
| --- | --- |
| 同一采样窗口内五项模组方法**合计 > 2%** | 实施剩余优化（tooltip 整体主要来自原版构建，需评估是否接管，兼容性风险高） |
| **1%–2%** | 观察，记录中位 |
| **< 1%** 且无复现反馈 | 维持，台账 T5 状态保持「已缓解」 |

## 五、T6：256 上限回归与写盘峰值

### 5.1 场景配置

| 场景 | 配置 | 轮次 |
| --- | --- | --- |
| 256 上限渲染回归 | 攻击范围上限 64 → 256、单层基岩上限 32 → 256，两项全开 | 静止 1 轮、移动 1 轮、切换显示模式 1 轮 |
| worker 采样 | 同上（触发几何重建后） | JFR 120 s，过滤 `tweakify-geometry-worker` |
| 写盘峰值 | 打开配置界面，拖动滑块 30 s（高频修改触发节流写盘） | JFR 30 s，过滤 `Render thread` 与 `tweakify-file-writer` |

### 5.2 记录表

| 项 | 轮次 | 指标 | 结果 | 证据 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 256 上限回归 | 静止 | 无闪烁 / 无残留 / 无崩溃 |  |  |  |
| 256 上限回归 | 移动 | 无闪烁 / 无残留 / 无崩溃 |  |  |  |
| 256 上限回归 | 切换模式 | 线条↔实心切换正常 |  |  |  |
| worker 采样 | 1 | `tweakify-geometry-worker` CPU 占用 |  |  |  |
| worker 采样 | 1 | Render thread 中两个 `*GeometryCache` 构建样本 | 应为 **0** |  |  |
| 写盘峰值 | 1 | 主线程 `prepareSaveHandle` / `toJsonTree` 占比 | 应 **< 0.5%** |  |  |
| 写盘峰值 | 1 | 最长主线程阻塞（ms） | 应 **< 5 ms** |  |  |
| 写盘峰值 | 1 | `tweakify-file-writer` 是否按队列顺序完成 | 是/否 |  |  |

### 5.3 判定（写结论用）

| 结果 | 结论 |
| --- | --- |
| worker 不挤占渲染帧时间、无内存持续增长、主线程写盘 < 0.5% 且无 > 5 ms 阻塞 | T6 可闭环，台账 T6 改「已修复（后台化已验证）」 |
| 出现卡顿或帧时间异常 | 回退对应功能的同步构建（保留 T2 摘要去重），台账记录复现路径 |
| 仅完成部分场景 | T6 保持「已缓解，待采样」，在详情「待办」更新已采/未采项 |

## 六、采样后回填台账

1. **明细**：把各轮数据补进 [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md) §六 或本文件表格；
2. **台账**：在 [`docs/tech-debt.md`](tech-debt.md) 对应条目末尾追加一条
   `实机采样（YYYY-MM-DD）` 记录，包含场景、轮次、中位、结论与证据链接；
3. **状态列**按 §3.3 / §4.3 / §5.3 的结论更新：
   - T3：`复测未达触发条件（YYYY-MM-DD，见详情）` 或 「触发」并附降负/预案决定；
   - T5：维持「已缓解」或升级为「已修复（<摘要>）」；
   - T6：`已修复（后台化已验证，见详情）` 或维持「已缓解，待采样」；
4. 若采样结果与旧基线差异 > 20%，在台账里注明「基线漂移」，并记录硬件/驱动/MC 版本变化。

## 七、参考

- [`docs/t3-profiler-checklist.md`](t3-profiler-checklist.md)：T3 通用场景矩阵、采集步骤与 `avgLayerUs` 公式；
- [`docs/tech-debt.md`](tech-debt.md)：T3/T5/T6 的现状、触发条件与判定口径；
- [`docs/mixin-probes.md`](mixin-probes.md)：若怀疑注入失效，先确认 `Mixin hook active` 日志与探针报表。
