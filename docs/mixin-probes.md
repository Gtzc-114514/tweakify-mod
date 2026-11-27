# Mixin 探针说明

本文档说明 `require = 0` 注入点的 `HookProbe` 探针约定、覆盖情况与已知取舍。

**基线**：本文档描述 v1.1.1 发布后（2026-11-18）的探针状态；v1.1.1 新增的修复类注入点与 MC-8187 一并计入，新增或修改注入点时以实际代码为准。

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

- 约定：探针放在 handler 方法体**首行**，位于 config 与功能开关判断**之前**；
- 理由：否则功能关闭时探针不触发，失去「确认钩子活着」的意义；
- **例外（以实际代码为准）**：部分既有注入点把探针放在功能分支或计算之后，属于有意为之，
  它们的探针只证明「功能实际生效」，不证明「钩子已注入」：
  `AnvilMenuMixin`（3 处）、`LocalPlayerCutThroughMixin`、`MinecraftPickFluidBucketMixin`、
  `FrostWalkerJumpFixMixin`（只在确实放宽容忍时才记录）、
  `SignOpenContainerMixin`（只在容器确实被打开时才记录）。
  v1.1.1 新增的修复类注入点大多在配置开启后才记录，同样按“功能实际启用”口径理解；
  新增注入点默认按约定的首行写法，若要写成例外请在方法内注释原因。

### 重载选择

| 重载 | 适用路径 | 示例 |
| --- | --- | --- |
| `markOnce("名称")` | 低频：打开界面、死亡、关闭界面、数据包、中键取物等一次性/稀疏事件 | `visibleTradersOpenScreen` |
| `markOnce(AtomicBoolean, "名称")` | 热路径：每帧、每 tick、区块保存等；标记后只剩一次 volatile 读，不再参与集合哈希 | `customBlockOutlineWidth` |

> 归类依据是「能否被高频重复触发」而非回调名：点击类回调若可能被连点刷屏
> （`slotClicked`、`creativeQuickBurn`）也按热路径处理；反之 `getTooltipLines` 虽每帧调用，
> 但只保留 String 重载（`enchantmentDescriptions`）。

### 字段

- 仅 `AtomicBoolean` 重载需要字段，声明为 `@Unique private static final AtomicBoolean TWEAKIFY$XXX_PROBE`；
- 字符串重载无需字段。

### 命名

- 探针名在模组内**全局唯一**：`HookProbe.REPORTED` 是共享集合，重名会让后执行的钩子静默不打印；
- 同一功能拆到客户端与服务端两侧时用 `Client` / `Server` 后缀区分（如 `pickFluidBucket` / `pickFluidBucketServer`；`keepLastTorch` 是未加后缀的历史命名）；
- 建议按「功能域 + 具体动作」命名（如 `visibleTradersSaveLocked`）。

## 覆盖情况（v1.1.1 基线 73/73）

统计口径：**非注释**的 `require = 0` 注解数 与 `HookProbe.markOnce` 调用数。

v1.1.1 基线（2026-11-18）：

```
非注释 require = 0 注解 : 73
HookProbe.markOnce 调用 : 73
AtomicBoolean 探针字段  : 54（全部为 TWEAKIFY$XXX_PROBE 命名）
mixin 文件             : 48 个含注入/探针的文件（其中两处刻意例外见下表）
```

其中两处刻意不按文件 1:1（基线时的例外，沿用至今）：

| 文件 | 说明 |
| --- | --- |
| `ItemStackEnchantmentTooltipMixin` | 注入点本身不再直接记录，探针在 `EnchantmentTooltipDescriptions` helper 中 |
| `PotionContentsTooltipCategoryMixin` | 一个注入点按「发光」与「不祥之兆」分别记录两个探针 |

当前实测（2026-11-28，同一脚本口径；新增注入点后请重跑本节脚本并同步此块）：

```
非注释 require = 0 注解 : 78
HookProbe.markOnce 调用 : 81
AtomicBoolean 探针字段  : 60
mixin 文件             : 55 个含注入/探针的文件
```

当前共 5 个文件不配平（78 个 `require = 0` 注入点已全部有探针）：

- 上表两处：探针不在注入点内 / 一注多探针；
- `CreateWorldScreenExperimentalPackMixin`、`LocatorBarHeadsMixin`、`WorldOpenFlowsExperimentalWarningMixin`
  的探针挂在 `require >= 1` 的注入点上，属于额外记录，不是缺口。

> 2026-11-27 补齐 3 个缺口：`AbstractContainerScreenQuickRightClickMixin`、
> `CreativeModeInventoryScreenQuickRightClickMixin`、`MultiPlayerGameModeMixin`（`startDestroyBlock`）；
> 同时拆开 `BookEditScreenVerticalCenterMixin` 两处注入共用的探针名，保证探针名全局唯一。
>
> 2026-11-28 新增 2 个探针：`MouseHandlerFastScrollMixin`（`fastScroll`）与
> `CommandSuggestionsNbtAutocompleteMixin`（`nbtAutocomplete`）。这两处是滚动/命令补全类
> 模组的常见改写目标，改为 `require = 0` + 探针，冲突时降级而不是启动失败。

最近补齐的 8 处（`56f1e07` 补齐，`604cd94` 统一字段命名）：

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
import re, glob
files = [f for f in glob.glob("src/client/java/**/*.java", recursive=True)
         if "/mixin/" in f.replace("\\", "/")]
req = probes = fields = 0
per_file = []
for f in files:
    src = open(f, encoding="utf-8").read()
    r = 0
    for l in src.splitlines():
        s = l.strip()
        if s.startswith(("//", "*", "/*")):
            continue
        r += len(re.findall(r"require\s*=\s*0", s))
    p = src.count("HookProbe.markOnce(")
    fld = len(re.findall(r"@Unique\s*\n\s*private static final AtomicBoolean TWEAKIFY\$[A-Z0-9_]+", src))
    req += r; probes += p; fields += fld
    if r or p:
        per_file.append((f, r, p))
print("require=0 :", req)          # v1.1.1 基线 73，2026-11-28 实测 78
print("markOnce  :", probes)       # 基线 73，实测 81
print("AtomicBool:", fields)       # 基线 54，实测 60
print("mixin 文件:", len(per_file),
      "整体配平:", all(r == p for _, r, p in per_file))   # False；当前 5 个文件不配平，见上方说明
```

> CI 侧由 `MixinProbeCoverageTest`（`src/test/java/com/gtzc/tweakify/client`）自动校验，
> 口径比上述脚本更严：逐个 `require = 0` 注入点检查其 handler 方法体内是否有探针；
> 探针名全局唯一（扫描整个 `src/client/java`，含 helper 类）；`AtomicBoolean` 字段与探针名
> 一一对应（同文件同字段只能对应一个名字）；计数前先剥离注释与字符串。
> 例外清单（handler 内无探针 / 一注多探针 / 额外探针）与本节「当前实测」块一一对应，
> 增删例外时两处同步。

## 已知不改清单

| # | 位置 | 内容 | 状态 |
| --- | --- | --- | --- |
| N1 | `LevelExtractorBlockOutlineMixin`、`MinecartPickUpIronGolemMixin`、`ResourcePackCompatibilityMixin` | **已统一**：3 个旧字段 `TWEAKIFY$REPORTED` 已改为 `CONNECTED_OUTLINE_PROBE` / `GOLEM_MINECART_PROBE` / `RESOURCE_PACK_PROBE`，`src` 下已无 `_REPORTED` 字段残留（本行文字为历史记录） | 已完成，保留作历史记录 |
| — | `docs/release-pipeline.md` 的 L1–L3 / H2 | CI 日志精细度与 jar 读取三合一注解 | 见该文档「已知但决定不改的点」 |

> H1（3 个 Mixin 的 8 个注入点缺探针）已于 `56f1e07` 补齐，不再是待办项。

## 相关文件

| 文件 | 作用 |
| --- | --- |
| `src/main/java/com/gtzc/tweakify/util/HookProbe.java` | 探针实现（两种重载） |
| `src/client/java/com/gtzc/tweakify/client/mixin/**` | `require = 0` 注入点所在（v1.1.1 基线 73 个，2026-11-28 实测 78 个） |
| `docs/release-pipeline.md` | 发布流水线说明与 L1–L3 / H2 取舍 |
| `docs/tech-debt.md` | 技术债台账（T1–T3：测试覆盖缺口、顶点缓存、主线程扫描） |
