# Mixin 探针说明

本文档说明 `require = 0` 注入点的 `HookProbe` 探针约定、覆盖情况与已知取舍。

**基线**：本文档描述提交 `429e709`（2026-11-14，探针字段全部统一）之后的状态；新增或修改注入点时以实际代码为准。

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
- **例外（现状，51 处中 45 处遵循）**：以下 6 处把探针放在功能分支或计算之后，属于有意为之，
  它们的探针只证明「功能实际生效」，不证明「钩子已注入」：
  `AnvilMenuMixin`（3 处）、`ItemStackEnchantmentTooltipMixin`、
  `LocalPlayerCutThroughMixin`、`MinecraftPickFluidBucketMixin`。
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

## 覆盖情况（51/51）

统计口径：**非注释**的 `require = 0` 注解数 与 `HookProbe.markOnce` 调用数。

```
非注释 require = 0 注解 : 51
HookProbe.markOnce 调用 : 51
AtomicBoolean 探针字段  : 34（全部为 TWEAKIFY$XXX_PROBE 命名）
逐文件配平             : 28 个 mixin 文件全部 1:1，无遗漏
```

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
print("require=0 :", req)          # 51
print("markOnce  :", probes)       # 51
print("AtomicBool:", fields)       # 34
print("mixin 文件:", len(per_file),
      "全部 1:1:", all(r == p for _, r, p in per_file))   # 28 True
```

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
| `src/client/java/com/gtzc/tweakify/client/mixin/**` | 51 个 `require = 0` 注入点所在 |
| `docs/release-pipeline.md` | 发布流水线说明与 L1–L3 / H2 取舍 |
| `docs/tech-debt.md` | 技术债台账（测试覆盖缺口的评估结论） |
