# Mixin 探针说明

本文档说明 `require = 0` 注入点的 `HookProbe` 探针约定、覆盖情况与已知取舍。

**基线**：截至 2026-11-14，`origin/main` 为 `604cd94`；新增或修改注入点时以实际代码为准。

## 为什么需要探针

`require = 0` 的注入点为了不让单个冲突拖垮启动，放弃了「注入失败即崩溃」。代价是
被其他模组改写目标方法时，功能会**静默失效**。`HookProbe` 在钩子第一次真正执行时输出：

```
Mixin hook active: <探针名>
```

据此可以区分两种情况：

| 日志表现 | 含义 |
| --- | --- |
| 没有对应条目 | 注入失败（目标方法/字段不匹配，`require = 0` 静默跳过） |
| 有条目但功能不生效 | 注入成功，但逻辑未生效（配置、状态、条件判断） |

## 约定

### 位置

- 探针放在 handler 方法体**首行**，位于 config 与功能开关判断**之前**；
- 否则功能关闭时探针不触发，失去「确认钩子活着」的意义。

### 重载选择

| 重载 | 适用路径 | 示例 |
| --- | --- | --- |
| `markOnce("名称")` | 低频：打开界面、死亡、关闭界面、数据包、中键取物等一次性/稀疏事件 | `visibleTradersOpenScreen` |
| `markOnce(AtomicBoolean, "名称")` | 热路径：每帧、每 tick、区块保存等；标记后只剩一次 volatile 读，不再参与集合哈希 | `customBlockOutlineWidth` |

### 字段

- 仅 `AtomicBoolean` 重载需要字段，声明为 `@Unique private static final AtomicBoolean TWEAKIFY$XXX_PROBE`；
- 字符串重载无需字段。

### 命名

- 探针名在模组内**全局唯一**：`HookProbe.REPORTED` 是共享集合，重名会让后执行的钩子静默不打印；
- 同一功能拆到客户端与服务端两侧时用 `Client` / `Server` 后缀区分（如 `keepLastTorchClient` / `keepLastTorch`）；
- 建议按「功能域 + 具体动作」命名（如 `visibleTradersSaveLocked`）。

## 覆盖情况（51/51）

统计口径：**非注释**的 `require = 0` 注解数 与 `HookProbe.markOnce` 调用数。

```
非注释 require = 0 注解 : 51
HookProbe.markOnce 调用 : 51
AtomicBoolean 探针字段  : 30
逐文件配平             : 28 个 mixin 文件全部 1:1，无遗漏
```

最近补齐的 8 处（提交 `56f1e07`、`604cd94`）：

| 注入点 | 探针名 | 类型 |
| --- | --- | --- |
| `LevelRendererOutlineStyleMixin` `submitHitOutline` index=6 | `customBlockOutlineWidth` | AtomicBoolean |
| `ModsScreenScrollMixin` `removed` HEAD | `modMenuScrollFlush` | String |
| `VillagerMixin` `addAdditionalSaveData` TAIL | `visibleTradersSaveLocked` | AtomicBoolean |
| `VillagerMixin` `readAdditionalSaveData` TAIL | `visibleTradersReadLocked` | AtomicBoolean |
| `VillagerMixin` `die` HEAD | `visibleTradersClearOnDeath` | String |
| `VillagerMixin` `updateSpecialPrices` TAIL | `visibleTradersResendPrices` | String |
| `VillagerMixin` `setVillagerData` HEAD | `visibleTradersSetData` | String |
| `VillagerMixin` `startTrading` TAIL | `visibleTradersOpenScreen` | String |

### 校验方法

统计时必须**排除注释行**（部分文件的注释里也写了 `require = 0` 字样，直接按行计数会多算）：

```python
import re, glob, os
req = probes = 0
for f in glob.glob("src/client/java/**/*.java", recursive=True):
    if "/mixin/" not in f.replace("\\", "/"):
        continue
    for l in open(f, encoding="utf-8"):
        s = l.strip()
        if s.startswith(("//", "*", "/*")):
            continue
        if re.search(r"require\s*=\s*0", s):
            req += 1
    probes += open(f, encoding="utf-8").read().count("HookProbe.markOnce(")
print(req, probes)  # 应为 51 51
```

## 已知不改清单

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| N1 | `LevelExtractorBlockOutlineMixin`、`MinecartPickUpIronGolemMixin`、`ResourcePackCompatibilityMixin` | 这 3 个更早的探针字段仍用旧命名 `TWEAKIFY$REPORTED`（不是 `XXX_PROBE` 风格） | 纯命名一致性，不影响功能；未纳入 H1 范围 |
| — | `docs/release-pipeline.md` 的 L1–L3 / H2 | CI 日志精细度与 jar 读取三合一注解 | 见该文档「已知但决定不改的点」 |

> H1（3 个 Mixin 的 8 个注入点缺探针）已于 `56f1e07` 补齐，不再是待办项。

## 相关文件

| 文件 | 作用 |
| --- | --- |
| `src/main/java/com/gtzc/tweakify/util/HookProbe.java` | 探针实现（两种重载） |
| `src/client/java/com/gtzc/tweakify/client/mixin/**` | 51 个 `require = 0` 注入点所在 |
| `docs/release-pipeline.md` | 发布流水线说明与 L1–L3 / H2 取舍 |
