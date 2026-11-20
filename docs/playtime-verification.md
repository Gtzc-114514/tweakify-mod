# 存档游玩时长实机验证记录

记录「存档游玩时长」功能在 2026-11-21 的实机回归：重点验证 `players/stats`
与旧 `stats` 目录的合并逻辑不会重复计数，以及 hours / clock / duration
三种显示格式。

## 环境与构建

| 项 | 值 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1 |
| 相关提交 | `3a0e5fb`（功能）、`a80ea36`（合并修复） |
| 启动方式 | `gradlew.bat runClient`（JDK 25） |
| 对照存档 | `新的世界`（创造，`players/stats` 69126 tick）<br>`新的世界 (1)`（生存，1316 tick） |

## 构造场景

为 `新的世界 (1)` 临时新增旧目录 `stats/`：

- 同名 UUID 文件 `8647fa2e-0d3d-396f-b7fb-761893f2c64f.json`：`play_time = 999999`
  （应被 `players/stats` 覆盖，不能累加）；
- 额外文件 `legacy-extra.json`：`play_time = 72000`（应补入）。

预期合并值：`1316 + 72000 = 73316` tick（1 小时 1 分）。

## 结果

| 格式 | `新的世界 (1)` 实测 | `新的世界` 对照 | 结论 |
| --- | --- | --- | --- |
| `hours` | 1.0 小时 | 57 分钟 | 同名旧文件未累加（否则 13.9 小时），额外旧文件已补入（否则 1 分钟） |
| `clock` | 1:01 | 0:57 | 与合并值一致 |
| `duration` | 1 小时 1 分 | 57 分钟 | 与合并值一致 |

结论：`players/stats` 与旧 `stats` 按文件名取并集、同名以 `players/stats` 为准；
**无重复计数、无漏计**；三种格式切换均生效。

## 复现步骤

1. 关闭客户端，备份 `saves/<世界>/players/stats`；
2. 在 `saves/<世界>/stats/` 放入与 `players/stats` 同名的 JSON（任意值）和一个不同名 JSON；
3. 在 `run/config/tweakify-mod.json` 设置 `savePlayTime = true`，按需切换 `savePlayTimeFormat`；
4. `gradlew.bat runClient`，进入「单人游戏」查看条目右上角的时长，分别核对三种格式；
5. 完成后删除 `saves/<世界>/stats/`，恢复配置。

## 相关链接

- 功能与配置说明：[README 功能一览](../README.md#features)
- 回退与合并实现：`WorldPlayTime.readPlayTimeTicks`（`players/stats` 优先，
  旧 `stats` 按文件名补缺失项）
