<div align="center">

<img src="docs/icon-128.png" width="96" alt="Tweakify Mod icon">

# Tweakify Mod

**把原版里零散、不可配置的行为，整理成可开关、可绑定快捷键的选项。**

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-0ea5e9?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.20.1%2B-f59e0b?style=flat-square)
![Environment](https://img.shields.io/badge/Env-Client-8b5cf6?style=flat-square)
![Requires](https://img.shields.io/badge/Requires-MaLiLib-red?style=flat-square)

</div>

[快速开始](#quick-start) · [功能一览](#features) · [配置文件](#config-file) · [环境要求](#requirements) · [命令](#commands)

> 纯客户端模组（`environment: client`），专用服务器无需安装；涉及服务端判定的功能仅在单人 / 局域网世界生效。

<a name="quick-start"></a>

## 🚀 快速开始

1. 安装 **Fabric Loader**（>= 0.20.1），用它启动 Minecraft **26.3**（需要 Java 25 运行时）。
2. 从 [Releases](https://github.com/Gtzc-114514/tweakify-mod/releases) 下载本模组的 jar，连同必需的 **Fabric API** 一起放进 `.minecraft/mods/`。
3. 安装 **MaLiLib**（必需，配置界面依赖它），同样放进 `mods/`。
4. 可选：安装 **Mod Menu**，在模组列表里获得本模组的配置入口；不装也能用 `G + C` 或 `/fconfig` 打开配置界面。

进游戏后按 `G + C` 打开配置界面即可开始调整；完整版本要求见下方「环境要求」一节。

## ✨ 功能概览

功能按配置界面顶部的标签分 7 类，完整清单见[功能一览](#features)。

| 标签 | 主要能力 |
| --- | --- |
| 功能 | 交互冷却与副手回退、如履平地、更好的攀爬、主动爬行、相机运动、轮廓与高亮、计时、攻击范围、存档时长、维度传送、刷怪蛋、物品分组、定位栏头像 |
| 修改 | 使用物品不减速、飞行漂移、疾跑视野、挖掘打断、鞘翅水中飞行、附魔/铁砧限制、掉落物寿命、生物生成、移动物理、不死图腾、耕地踩踏 |
| 修复 | 草径卡船、聊天卡顿、经验条溢出、书本居中、鱼线悬空、树苗生长等原版问题 |
| 优化 | MiniHUD 掉帧优化 |
| 通用 | 界面语言、配置快捷键、标签与滚动记忆、搜索高亮、平滑滚动、收藏星样式 |
| 测试 | 调试 HUD、钩子探针与报表、配置档案编辑器 |
| 收藏 | 手动收藏的配置项（点击配置行右侧星标收藏/取消；存档列表左侧的星标可收藏存档并置顶，收藏的存档不可删除） |

<a name="requirements"></a>

## 🧩 环境要求

| 项 | 版本 |
| --- | --- |
| Tweakify Mod | 1.1.7 |
| Minecraft | 26.3 |
| Fabric Loader | >= 0.20.1 |
| Fabric API | 0.163.0+26.3 |
| MaLiLib | 0.30.2（必需） |
| Mod Menu | 21.0.0（可选，仅用于提供模组列表入口） |
| MiniHUD | 0.41.2（可选，仅用于 MiniHUD 掉帧优化） |
| Java | 25 |

<a name="features"></a>

## 📖 功能一览

下面按配置界面的标签分为 7 类、共 290 余项配置（默认折叠，点击分类标题展开）。每行以 `配置字段` 开头，后接便于识别的中文名；「默认」列是新生成配置文件时的初始值。说明中标注「仅在单人/局域网生效」的功能，判定发生在集成服务端，专用服务器上不会生效。

> **已知问题**：MC 26.3 在 **Vulkan 后端 + NVIDIA 驱动**下渲染实体描边会崩溃（`nvoglv64.dll`）。使用实体高亮时请把图形 API 设为 OpenGL。

<details>
<summary><b>功能 — 新增行为，快捷键最完整（7 组 / 150 余项：交互冷却、移动、相机、HUD、传送、世界交互、工具）</b></summary>

#### 交互冷却与副手回退

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `useCooldown` 右键交互冷却 | 开 | 冷却组的总开关；关闭时主手、副手的使用冷却全部失效 |
| `mainHandUseCooldown` 主手使用冷却 | 关 | 一次右键只使用一次，命中的物品由下方列表决定 |
| `mainHandUseCooldownItems` 主手使用冷却物品 | 全部可交互物品 | 冷却生效的物品 ID 列表 |
| `offhandUseCooldown` 副手使用冷却 | 关 | 同上，作用于副手 |
| `offhandUseCooldownItems` 副手使用冷却物品 | 全部可交互物品 | 冷却生效的物品 ID 列表 |
| `blockOffhandFallback` 副手回退拦截 | 开 | 主手物品用完且仍按住右键时，阻止受限副手物品自动接替使用 |
| `interceptBlockInteraction` 拦截方块交互 | 关 | 副手回退生效时一并拦截方块交互；关闭时仍拦副手方块放置，但放行门、拉杆、按钮、箱子等可交互方块 |
| `interceptEntityInteraction` 拦截实体交互 | 关 | 副手回退生效时一并拦截副手对实体的交互 |
| `restrictedOffhandItems` 受限副手物品 | 全部光源物品 | 只有命中该列表的副手物品会被拦截 |
| `miningCooldown` 左键交互冷却 | 关 | 一次左键只挖一个方块，需松开再按才能继续；仅在方块真正被破坏时计数 |
| `miningCooldownAttacks` 攻击也限一次 | 关 | 攻击实体也受限制，与挖方块共用同一次按住周期 |
| `elytraFireworkCooldown` 鞘翅烟花限制 | 关 | 滑翔中用烟花加速后，要等加速结束才能再次右键使用烟花 |
| `elytraFireworkCooldownTicks` 最短间隔 | 0 | 两次烟花之间的最短间隔（tick，0 ~ 200）；0 表示等本次加速结束 |

#### 移动与视角

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableFlightDrift` 禁用飞行漂移 | 关 | 飞行时松开方向键立即停止水平移动 |
| `flightDriftVertical` 垂直飞行影响 | 关 | 松开跳跃/潜行后也立即停止升降 |
| `autoStepUp` 如履平地 | 关 | 自动跨上 1 格高的方块（像马一样）；仅对本地玩家生效 |
| `autoStepUpHeight` 跨步高度 | `one_block` | `one_block` 1 格 / `one_and_half` 1.5 格 / `two_blocks` 2 格；越高越容易跨上台阶与斜坡 |
| `betterClimbing` 更好的攀爬 | 关 | 优化攀爬方块中的移动：向下看快速下降、上爬更快、可从地面起跳等，子项可单独开关；水平方向不减速请用「修改」标签的「经过可攀爬方块不减速」 |
| `climbingFastDescend` 向下看加速下降 | 开 | 在梯子、藤蔓上向下看时快速下降 |
| `climbingFasterAscend` 攀爬上升更快 | 开 | 向上攀爬时移动速度更快 |
| `climbingJumpFromGround` 攀爬时从地面起跳 | 开 | 贴着可攀爬方块站在地面时也能直接起跳 |
| `climbingCancelCollision` 无输入时取消碰撞减速 | 开 | 没有移动输入时取消攀爬方块边缘的碰撞减速 |
| `forceCrawl` 主动爬行 | 关 | 开启总开关并给「爬行按键」绑定按键后，按住强制进入爬行姿态，可钻进 1 格高的空间；仅在单人/局域网主机生效 |
| `forceCrawlKey` 爬行按键 | 未绑定 | 按住爬行、松开站起 |

#### 相机运动

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `cameraMotion` 相机运动 | 关 | 总开关：转向/横移侧倾、前后/垂直俯仰、静止晃动、爆炸/雷击/挥手震动与鼠标平滑；子项可单独开关 |
| `cameraMotionIntensity` 相机运动强度 | 100 | 所有相机运动效果的强度百分比（0 ~ 200）；设为 0 可临时关闭全部效果 |
| `cameraMotionTurningRoll` 转向侧倾 | 开 | 转动视角时镜头向转向方向轻微侧倾 |
| `cameraMotionStrafingRoll` 横移侧倾 | 开 | 左右横移时镜头向移动方向轻微侧倾 |
| `cameraMotionForwardPitch` 前后移动俯仰 | 开 | 前进时镜头轻微下压，后退时轻微上扬 |
| `cameraMotionVerticalPitch` 跳跃/下落俯仰 | 开 | 跳跃/上升时镜头上扬，下落时下压 |
| `cameraMotionIdleSway` 静止晃动 | 开 | 停止移动和转向一段时间后镜头轻微晃动，有输入立即恢复 |
| `cameraMotionScreenShake` 屏幕震动 | 开 | 爆炸、雷击和挥手时产生短促震动，距离越远越弱；雷击震动依赖本地集成服务端，仅在单人/局域网生效 |
| `cameraMotionMouseSmoothing` 鼠标平滑 | 关 | 平滑快速转视角时的镜头变化，只影响渲染，不改变实际瞄准 |
| `cameraMotionFirstPerson` 第一人称生效 | 关 | 在第一人称也应用相机运动；默认关闭以避免晕动感 |
| `cameraMotionThirdPerson` 第三人称生效 | 开 | 在第三人称（背后/正面）应用相机运动 |

#### 渲染与 HUD

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `customBlockOutline` 自定义方块轮廓 | 关 | 总开关：自定义准星方块轮廓的粗细与颜色；高对比度轮廓模式下保持原版轮廓 |
| `blockOutlineWidth` 轮廓粗细度 | 2.0 | 线宽（0.5 ~ 8，滑块调节）；部分显卡或渲染后端会把线宽限制为 1 |
| `blockOutlineAnimationSpeed` 动画速度 | 100 | 彩虹/呼吸模式的动画速度百分比（10 ~ 400，数值越大越快） |
| `blockOutlinePalette` 轮廓颜色调色板 | `white` | `white` / `red` / `green` / `blue` / `yellow` / `cyan` / `magenta` / `orange`，仅「调色板」模式使用 |
| `blockOutlineColorMode` 轮廓彩色模式 | `palette` | `palette` 固定使用所选颜色 / `rainbow` 随时间循环变色 / `pulse` 亮度周期性变化 |
| `entityHighlight` 自定义实体高亮 | 关 | 为预设列表中的实体显示发光描边；需同时开启一个预设；描边样式与颜色可调 |
| `entityHighlightStyle` 描边样式 | `outline` | `outline` 常亮描边 / `blink` 周期闪烁（一半时间显示、一半时间隐藏） |
| `entityHighlightBlinkPeriod` 闪烁周期 | 60 | 闪烁样式的周期（tick，10 ~ 200）；仅在 `blink` 样式下生效 |
| `entityHighlightColor` 描边颜色 | `vanilla` | `vanilla` 原版队伍色（无队伍为白色）/ `white` / `red` / `green` / `blue` / `yellow` / `cyan` / `magenta` |
| `highlightPreset1` / `highlightPreset2` / `highlightPreset3` 高亮预设 | 关 | 三个预设互斥，开启一个会自动关闭其他 |
| `highlightPreset1Entities` / `highlightPreset2Entities` / `highlightPreset3Entities` 预设实体 | 空 | 各预设需要高亮的实体 ID 列表，支持 `minecraft:item` 等掉落物实体 |
| `highlightSingleLayerBedrock` 高亮单层基岩 | 关 | 在扫描半径内（默认 32 格）的单层基岩朝向空气的顶面/底面显示凸出 1 像素的实心小方块，便于寻找下界顶棚的可击穿薄弱点；每秒刷新 6 次，扫描结果未变时复用已有顶点缓存 |
| `highlightSingleLayerBedrockMaxMarkers` 基岩标记最大数量 | 32 | 单次最多显示的标记数（1 ~ 256）；超出时保留最近的 |
| `highlightSingleLayerBedrockScanRadius` 基岩扫描半径 | 32 | 扫描单层基岩的半径（8 ~ 32 格）；半径越小，扫描越快、可见范围越近 |
| `highlightSingleLayerBedrockColor` 基岩标记颜色 | `gold` | `gold` 金色 / `white` / `red` / `green` / `blue` / `cyan`；只影响小方块颜色，切换后顶点缓存自动重建 |
| `barrierVisibility` 屏障可见 | 关 | 周围屏障方块显示与手持屏障物品时相同的原版标记粒子，粒子固定在方块中心并逐方块冷却，不会偏移或重叠；扫描半径与标记间隔可调 |
| `barrierVisibilityMaxMarkers` 屏障标记最大数量 | 256 | 单次扫描最多保留的标记数（16 ~ 256）；超出时保留离玩家最近的 |
| `barrierVisibilityScanRadius` 屏障扫描半径 | 32 | 扫描屏障方块的半径（8 ~ 32 格）；半径越小，扫描越快、可见范围越近 |
| `barrierVisibilityParticleInterval` 屏障标记间隔 | 60 | 同一方块两次标记粒子之间的间隔（20 ~ 200 tick，20 tick = 1 秒）；越大越省性能、闪烁越慢 |
| `durabilityOverlay` 装备耐久显示 | 关 | 在物品栏的耐久条上方叠加耐久文字；满耐久不显示，颜色随剩余耐久由绿转白、黄、红 |
| `durabilityDisplayMode` 耐久显示内容 | `percent` | `percent` 耐久百分比 / `value` 剩余耐久数值（只显示当前剩余，不显示初始耐久）；两者字号一致 |
| `durabilityOverlayHotbarOnly` 仅快捷栏显示 | 关 | 只在快捷栏显示耐久文字，背包格子里不显示 |
| `durabilityOverlayLowThreshold` 低耐久提醒阈值 | 0 | 剩余耐久低于该百分比（0 ~ 100）时才显示；0 表示始终显示 |
| `durabilityOverlayColorMode` 耐久文字配色 | `auto` | `auto` 随剩余耐久由绿转白、黄、红 / `white` / `yellow` / `red` 固定单色 |
| `statusEffectDurationDisplay` 药水效果时间显示 | 关 | 在 HUD 右侧药水效果图标下方显示剩余时间（秒 / 分:秒 / 2h05 / 2d05）；无尽效果不显示；颜色与低时长过滤可调 |
| `statusEffectDurationAlwaysSeconds` 固定按秒显示 | 关 | 始终使用秒数（如 125），不使用分:秒与小时/天缩写；长时间效果会缩小字号避免溢出 |
| `statusEffectDurationLowThreshold` 低时长阈值 | 0 | 只在剩余时间不高于该秒数时显示（0 ~ 600）；0 表示不过滤，适合只关注即将结束的效果 |
| `statusEffectDurationColorMode` 时间文字配色 | `auto` | `auto` 剩余 10 秒内变红、30 秒内变黄，其余白色 / `white` / `yellow` / `red` 固定单色 |
| `attackRangeDisplay` 攻击范围显示 | 关 | 以生物或玩家为中心显示半透明线框攻击范围（颜色可调），半径按原版攻击距离计算；每秒刷新 12 次，条目几何未变时复用已有顶点缓存 |
| `attackRangeDisplayMode` 范围显示方式 | `flat` | `flat` 脚下双圆环 / `sphere` 经纬线框球体 |
| `attackRangeRenderMode` 范围渲染方式 | `wireframe` | `wireframe` 蓝色线框（无 OIT，性能更好）/ `filled` 半透明实心（观感更完整，可能掉帧） |
| `attackRangePalette` 范围颜色 | `blue` | `blue` / `red` / `green` / `yellow` / `cyan` / `magenta` / `white`；只改 RGB，透明度仍按渲染方式固定 |
| `attackRangeIncludeSelf` 显示自己的范围 | 关 | 是否显示本地玩家自己的攻击范围；关闭后只显示其他生物与玩家 |
| `attackRangeHostileOnly` 仅显示敌对生物 | 关 | 只显示僵尸、骷髅等敌对生物；玩家与被动生物不再显示范围 |
| `attackRangeIncludeInvisible` 显示隐身实体 | 关 | 默认不显示隐身实体；开启后隐身生物同样显示攻击范围 |
| `attackRangeMaxDistance` 范围收集距离 | 48 | 收集攻击范围的最大距离（8 ~ 128 格） |
| `attackRangeMaxTargets` 最大显示数量 | 64 | 单次最多显示的数量（8 ~ 256）；超出时保留最近的实体 |
| `attackRangeEntities` 攻击范围实体 | 空 | 需要显示范围的生物/玩家 ID 列表；为空时显示全部生物与玩家 |
| `savePlayTime` 存档游玩时长 | 关 | 在存档列表右上角显示该存档的累计游玩时长（读取 `stats` 中每个玩家的 `minecraft:play_time` 并累加；多人存档按全部玩家统计） |
| `savePlayTimeFormat` 时长显示格式 | `hours` | `hours` 小数小时（不足 1 小时显示分钟）/ `clock` 时:分 / `duration` X 小时 Y 分 |

#### 维度传送

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableDimensionTeleport` 禁用维度传送 | 关 | 总开关：关闭时以下各项均不生效 |
| `dimensionTeleportPlayersOnly` 仅拦截玩家 | 关 | 只拦截玩家的维度传送，其他实体仍可正常穿过传送门 |
| `disableNetherPortalTeleport` 禁用地狱门传送 | 开 | 地狱门拦截开关；开启且黑名单为空时拦截所有实体 |
| `netherPortalTeleportBlacklist` 地狱门传送黑名单 | 空 | 非空时只拦截名单中的实体 |
| `disableEndPortalTeleport` 禁用末地门传送 | 开 | 末地门拦截开关；开启且黑名单为空时拦截所有实体 |
| `endPortalTeleportBlacklist` 末地门传送黑名单 | 空 | 非空时只拦截名单中的实体 |
| `disableEndGatewayTeleport` 禁用末地折跃门传送 | 开 | 阻止实体通过末地折跃门传送；开启且黑名单为空时拦截所有实体 |
| `endGatewayTeleportBlacklist` 末地折跃门传送黑名单 | 空 | 非空时只拦截名单中的实体 |
| `netherPortalTeleportDelay` 地狱门传送延迟 | 关 | 调整玩家通过地狱门的等待时间 |
| `netherPortalSurvivalInstantTeleport` 生存模式无延迟传送 | 关 | 生存/冒险模式进入地狱门立即传送，与创造模式一致 |
| `netherPortalCreativeDelayedTeleport` 创造模式延迟传送 | 关 | 创造模式也等待与生存模式相同的延迟，不再秒传 |

> 传送判定发生在服务端，因此该功能**仅在单人游戏或自己开设的局域网世界生效**。

#### 世界交互

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `swordCutThroughPlants` 剑穿草打生物 | 关 | 主手持剑时，准星指向花草也会直接选中后方的生物，左键不会先把草打掉；有碰撞的植物（如甜浆果丛）不受影响 |
| `cutThroughItems` 生效物品 | `sword` | `sword` 仅剑 / `sword_and_axe` 剑与斧 / `any` 任意手持物品 |
| `cutThroughPlants` 植物范围 | `all` | `flowers` 花草 / `crops` 花草与作物 / `all` 全部无碰撞植物 |
| `cutThroughSneakBypass` 潜行保持原版 | 开 | 潜行时恢复原版行为，左键先把植物打掉 |
| `pickFluidBucket` 中键获得液体桶 | 关 | 准星指向水/岩浆时按中键获得对应液体桶；**仅创造模式**，生存模式请用「生存模式中键取物」；仅在单人/局域网生效 |
| `pickFluidBucketFluids` 液体范围 | `water_and_lava` | `water_and_lava` 水与岩浆 / `water_only` 仅水 / `lava_only` 仅岩浆；只约束创造模式的液体取桶，生存取物不受影响 |
| `survivalPickBlock` 生存模式中键取物 | 关 | 生存模式下按中键，准星指向方块/液体/实体时，背包里有对应物品就切换到主手；没有则保持原版 |
| `survivalPickBlockIncludeFluids` 包含液体 | 开 | 开启时生存取物也处理水/岩浆（背包里有对应的桶就切换）；关闭后退回原版，只取方块与实体 |
| `quickBurnItem` 便捷焚烧 | 关 | 背包或容器里拿着物品时，把它悬停在岩浆桶或打火石上右键点击即可焚烧；单人/局域网会真正销毁物品，多人服务器会改为丢弃 |
| `quickBurnItemRequireSneak` 需要潜行焚烧 | 关 | 开启后需按住潜行键再右键才会焚烧，避免整理背包时误触销毁物品 |
| `piglinGoldArmorSwap` 猪灵金装自动切换 | 关 | 附近有猪灵时自动把背包里的金制装备换到身上，猪灵离开后换回；仅在生存或冒险模式生效 |
| `piglinGoldArmorSwapMode` 自动换装模式 | `auto` | `auto` 身上没有金制护甲时从背包选一件换上 / `helmet` / `chestplate` / `leggings` / `boots` 只替换指定部位 |
| `disableShovelSnowFullBreak` 锹单层铲雪 | 关 | 手持锹右键多层雪层时一次只铲掉一层，不再整块移除 |
| `shovelSnowDropSnowball` 铲雪掉落雪球 | 开 | 单层铲雪掉落 1 个雪球；关闭后不产生掉落物 |
| `shovelSnowConsumeDurability` 铲雪消耗耐久 | 开 | 单层铲雪按普通挖掘消耗锹耐久；关闭后不消耗 |
| `disableSpecifiedMobSpawn` 禁用指定生物生成 | 关 | 列表中的生物不会通过自然、结构、巡逻、增援、鸡骑士、袭击、唤魔者召唤或刷怪笼等世界生成出现；列表为空时不生效；仅在单人/局域网生效 |
| `disabledSpawnEntities` 生物 ID 列表 | 空 | 需要禁止生成的生物 ID 列表（如 `minecraft:zombie`）；为空时不生效 |
| `disabledSpawnAllowPlayerSpawn` 允许玩家生成 | 开 | 刷怪蛋、命令、繁殖等玩家主动生成的生物不拦截；关闭后这些来源也会被拦截。玩家建造类触发（如铁傀儡、凋灵）恒放行 |
| `peacefulModeMonsters` 和平模式怪物 | 关 | 允许列表中的怪物在和平难度下生成并保留；列表为空时不生效；仅在单人/局域网生效 |
| `peacefulModeMonsterEntities` 和平模式生物 ID 列表 | 空 | 和平模式怪物功能使用的生物 ID 列表；为空时不生效 |
| `peacefulModeMonsterSpawn` 和平模式生成怪物 | 开 | 列表中的怪物可通过自然生成、刷怪笼、试炼刷怪笼和地狱门等来源在和平难度下生成，并跳过和平难度的自动清除；试炼刷怪笼需生成池全部命中列表 |
| `peacefulModeMonsterNoAttack` 和平模式怪物不攻击 | 开 | 列表中的怪物在和平难度下不锁定/攻击玩家；关闭时可正常攻击 |
| `throwSpawnEgg` 刷怪蛋投掷 | 关 | 像鸡蛋、雪球一样把刷怪蛋投掷出去，命中方块或实体后生成对应生物；对刷怪笼只设置类型且不消耗刷怪蛋；仅在单人/局域网生效 |
| `throwSpawnEggVelocity` 投掷速度 | 100 | 投掷速度百分比（50 ~ 200，100 = 原版鸡蛋速度） |
| `throwSpawnEggSneakBypass` 潜行时保持原版 | 关 | 开启后潜行时恢复原版放置，不再投掷刷怪蛋 |
| `itemGroups` 物品分组 | 关 | 与 `BizCub/inventory-item-groups` 一致的基岩版分组：分类页把楼梯、台阶、羊毛等同类物品折叠成组图标，点击图标上的 `+` / `-` 展开/收起，展开的成员紧跟在图标之后并使用专用底板；悬停在组图标上只显示组名 |
| `itemGroupsSort` 组内排序 | `default` | `default` 保持物品栏原顺序 / `alphabetical` 按物品 ID 字母序（组图标随之变化） |
| `itemGroupsShowItems` 显示组内物品 | 关 | 展开状态下图标每 1.5 秒轮播一个组内物品，而不是固定显示首个物品 |
| `itemGroupsAddAll` 启用默认组 | 开 | 启用内置的默认物品组；关闭后只使用下面的自定义组 |
| `customItemGroups` 自定义物品组 | 空 | 每行一个组：`标签页ID|组名|包含ID(逗号分隔)|排除ID|等价ID`，例如 `minecraft:building_blocks|石英|quartz`；组只在行首标签页生效（搜索/快捷栏/背包页不成组，只记录一次告警）；标签页 ID 大小写不敏感、缺命名空间按 `minecraft:` 补全（`:combat` 等价 `minecraft:combat`）；行内字段不足/标签页为空/包含项为空的行会在日志提示一次并忽略，整行空白忽略（格式错误行提示需开启物品分组）；组名可写进 `group_name.inventory_item_groups.<组名>` 翻译键 |
| `locatorBarHeads` 定位栏优化 | 关 | 把定位栏上的彩色圆点换成本服玩家的头像（参考 locator-heads）；找不到皮肤时保留原版圆点 |
| `locatorBarAlwaysShowXp` 经验条与定位栏同时显示 | 关 | 与参考 alwaysShowXP 一致：强制经验条优先显示，并把定位栏头像叠在经验条上；乘车时不强制 |
| `locatorBarHeadScale` 头像尺寸 | 5 | 头像倍率档位（1 ~ 9）：0.5x / 0.6x / 0.7x / 0.8x / 1.0x / 1.1x / 1.2x / 1.3x / 1.5x；默认 5 = 原版圆点大小 |
| `locatorBarHeadHatLayer` 显示帽子层 | 开 | 叠加玩家皮肤的第二层（帽子/头盔） |
| `locatorBarHeadBorder` 头像描边 | `off` | `off` 不画 / `static` 固定颜色 / `team` 跟随计分板队伍颜色（无队伍时白色） |
| `locatorBarHeadBorderThickness` 描边粗细 | `normal` | 与参考实现同一张表：`thin` 0.3 / `medium` 0.5 / `normal` 1.0 / `thick` 2.0 像素 |
| `locatorBarHeadBorderColor` 描边颜色 | `white` | 固定颜色描边使用的颜色；`team` 样式下忽略 |
| `locatorBarHeadNames` 玩家名字 | `never` | `never` 从不 / `always` 总是 / `looking_at` 注视时（±20°）/ `player_list` 玩家列表打开时 |
| `locatorBarHeadFilter` 玩家过滤 | `all` | `all` 全部 / `include` 仅包含名单 / `exclude` 排除名单；名单为空时 `include` 全部隐藏、`exclude` 全部显示 |
| `locatorBarHeadIncludedPlayers` 包含名单 | 空 | 与参考实现一致：仅在 `include` 模式生效，名单内玩家保留头像；玩家名可写在条目中并用逗号、分号、冒号或句点分隔，忽略大小写 |
| `locatorBarHeadExcludedPlayers` 排除名单 | 空 | 与参考实现一致：仅在 `exclude` 模式生效，名单内玩家隐藏；分隔符与包含名单相同 |
| `locatorBarHeadNearbyOnly` 仅显示附近玩家 | 关 | 只给当前已加载（在附近）的玩家画头像，远处航点整条隐藏（含圆点，与参考实现一致） |
| `locatorBarCompass` 显示指南针 | 关 | 与参考实现同一套坐标与画法（固定 `高度 - 31`，字母左移 2 像素），边缘淡出；字母画在 HUD 最上层（条带底图、圆点、头像都在它下面）；开启时经验等级数字上移 6 像素避让；Flashback 回放时不显示 |
| `locatorBarCompassColor` 指南针颜色 | `white` | 方位字母颜色 |
| `locatorBarCompassShadow` 指南针描边 | 开 | 给方位字母加四向黑色描边，亮色天空下更清晰 |
| `locatorBarCompassCoordinates` 坐标标记 | 关 | 用 `+Z` / `-X` / `-Z` / `+X` 代替 `S` / `W` / `N` / `E` |
| `betterBlockPlacement` 更好的方块放置 | 关 | 手持方块右键时可直接替换花、高草等植物，不必先打掉；有碰撞的甜浆果丛、水草等不受影响；仅在单人/局域网生效 |
| `betterBlockPlacementSneakBypass` 潜行时保持原版 | 开 | 开启后潜行时不再覆盖植物，按原版流程先打掉植物再放置 |
| `betterBlockPlacementMode` 覆盖范围 | `flowers` | `flowers` 花草（花、树苗、高草）/ `crops` 花草与作物 / `all` 全部无碰撞植物 |
| `betterBoneMeal` 更好的骨粉 | 关 | 让骨粉像基岩版一样催熟甘蔗与花；仅在单人/局域网生效 |
| `boneMealSugarCane` 骨粉催熟甘蔗 | 开 | 一次骨粉把整株甘蔗催到最高 3 格，只消耗 1 个骨粉 |
| `boneMealFlowers` 骨粉催熟花 | 开 | 在一格高的花周围长出一片相同的花（凋零玫瑰除外，与基岩版一致） |

#### 实用工具

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `copyItemId` 复制物体 ID | 关 | 总开关；开启后用「复制目标 ID 快捷键」对准方块/实体、或在容器界面里悬停物品复制 ID，聊天栏里的 ID 可点击再次复制 |
| `copyTargetId` 复制目标 ID 快捷键 | 未绑定 | 对准方块/实体或在容器界面悬停物品时按下，复制其 ID；需开启 `copyItemId` 总开关 |
| `copyItemIdFormat` 复制格式 | `plain` | `plain` 纯 ID / `typed` 带类型前缀（物品/方块/实体）/ `counted` 附带数量 |
| `copyItemIdNamespace` 保留命名空间 | 开 | 复制时保留命名空间前缀（`minecraft:stone`）；关闭后只复制路径部分（`stone`） |
| `copyItemIdCloseContainer` 复制后关闭界面 | 关 | 在容器界面复制物品 ID 后自动关闭该界面 |
| `copyItemIdChatMessage` 聊天栏提示 | 开 | 复制成功后在聊天栏显示可点击复制的 ID；关闭后只写入剪贴板 |
| `chatTextSelection` 聊天文字框选 | 关 | 在聊天界面按住左键拖动选择消息文字，Ctrl+C 复制 |
| `chatSelectionColor` 框选高亮颜色 | `blue` | `blue` / `green` / `orange` / `purple` / `gray` |
| `chatSelectionOpacity` 框选透明度 | 33 | 框选背景的透明度百分比（10 ~ 100）；越大越不透明 |
| `chatSelectionAutoCopy` 松开自动复制 | 关 | 松开左键时自动复制选中的文字，不必再按 Ctrl+C |
| `chatInputMemory` 聊天框内容记忆 | 关 | 关闭聊天框后保留已输入但未发送的内容，下次打开聊天框时自动恢复；发送消息后清空 |
| `chatInputMemoryIgnoreCommands` 不记忆命令 | 关 | 开启后以 `/` 开头的命令草稿不再记忆，避免下次打开聊天框时误执行未发送的命令 |

</details>

<details>
<summary><b>修改 — 改写原版规则，更接近基岩版（5 组 / 90 项：移动挖掘、视角显示、覆盖层、提示文本、世界生成）</b></summary>

#### 移动与挖掘

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableUseItemSlowdown` 禁用使用减速 | 关 | 进食、拉弓、举盾时不再降低移速 |
| `useItemSlowdownExceptions` 使用减速例外物品 | 空 | 这些物品仍保留原版的使用减速（物品 ID 列表） |
| `useItemSlowdownKeepPercent` 保留使用减速 | 0 | 保留原版使用减速的百分比（0 ~ 100）；0 = 完全取消（默认），100 = 与原版相同 |
| `disableSprintFov` 禁用疾跑 FOV 变化 | 关 | 疾跑时视野不再放大；速度药水、飞行、拉弓等其它视野效果保持原版 |
| `sprintFovIntensity` 保留疾跑视野 | 0 | 保留原版疾跑视野强度的百分比（0 ~ 100）；0 = 完全禁用（默认），100 = 等同原版 |
| `keepMiningProgressOnItemSwitch` 切换工具保留挖掘进度 | 关 | 像基岩版一样：挖掘中切换物品不再重置进度，继续挖同一个方块 |
| `keepMiningProgressMaxDistance` 保留进度距离 | 0 | 只在玩家与目标方块距离不超过该格数时保留挖掘进度（0 ~ 16）；0 = 不限制（默认），超出后按原版重置 |
| `climbablePassNoSlowdown` 经过可攀爬方块不减速 | 关 | 走过梯子、藤蔓等可攀爬方块时，蹭到边缘也不再被原版夹紧水平速度；攀爬上升/下滑保持原版 |
| `climbablePassNoSlowdownSneakOnly` 仅潜行时生效 | 关 | 只在按住潜行键时跳过攀爬方块的水平速度夹紧 |
| `elytraWaterFlight` 鞘翅水中飞行 | 关 | 滑翔时不再被水流移动逻辑接管，潜入水中也能继续保持滑翔姿态与速度 |
| `elytraWaterFlightOnlyWater` 仅水中生效 | 关 | 只在水中保持滑翔，岩浆里仍按原版下沉 |
| `disableFarmlandTrampling` 禁用耕地踩踏 | 关 | 从高处落在耕地上不再把它踩回泥土；仅在单人/局域网生效 |
| `farmlandTramplingPlayerOnly` 仅玩家生效 | 关 | 只有玩家不会踩坏耕地，动物与村民仍会踩坏 |
| `totemFromInventory` 不死图腾背包生效 | 关 | 图腾放在快捷栏或背包（主手/副手之外）也能触发不死保护；手部已有图腾时仍优先消耗手中的；仅在单人/局域网生效 |
| `disableCobwebSlowdown` 禁用蛛网减速 | 关 | 蜘蛛网不再粘住玩家并限制移动与跳跃；仅在单人/局域网生效 |
| `cobwebSlowdownAllEntities` 作用于全部实体 | 关 | 生物、掉落物等也不再被蛛网减速；默认只解除玩家 |
| `disableFluidPush` 禁用液体推动 | 关 | 水流与岩浆流不再施加水平推力，浮力与流体高度判定保持原版；仅在单人/局域网生效 |
| `disableWaterPush` 禁用水流推力 | 开 | 仅取消水流造成的水平推力 |
| `disableLavaPush` 禁用岩浆推力 | 开 | 仅取消岩浆流造成的水平推力 |
| `disableSlimeBlockSlowdown` 禁用粘液块减速 | 关 | 站在粘液块上不再被乘算降低水平速度；仅在单人/局域网生效 |
| `disableSlimeBlockBounce` 禁用粘液块弹跳 | 开 | 同时把粘液块弹性系数改为 0，落地不再弹起 |
| `disableHoneyBlockSlowdown` 禁用蜂蜜块减速 | 关 | 蜂蜜块不再降低移动速度；灵魂沙等其它减速方块保持原版；仅在单人/局域网生效 |
| `disableHoneyBlockStick` 禁用蜂蜜块黏着 | 开 | 贴墙时不再被粘住缓降，可以正常下落 |

#### 视角与显示

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableHurtShake` 禁用受伤抖动 | 关 | 受伤时不再产生视角抖动；行走时的镜头晃动不受影响 |
| `hurtShakeKeepDeathTilt` 保留死亡镜头倾斜 | 关 | 开启后关闭受伤抖动时仍保留死亡倒下瞬间的镜头倾斜（默认关闭，连死亡倾斜一起取消） |
| `disableHurtTint` 禁用受伤变红 | 关 | 受伤时不再叠加红色 overlay；死亡变红与受伤闪白保持原版 |
| `hurtTintKeepWhiteFlash` 保留受伤闪白 | 开 | 关闭受伤变红时仍保留受伤瞬间的白色闪光；关闭后受伤完全不叠加 overlay |
| `thirdPersonPlayerName` 第三人称显示名字 | 关 | 第三人称（背后/正面）也显示玩家自己的名字；第一人称、隐身、旁观和隐藏 HUD 时保持原版 |
| `thirdPersonPlayerNameSneakOnly` 仅潜行时显示 | 关 | 只在按住潜行键时显示自己的名字 |
| `betterInvisibility` 更好的隐身 | 关 | 隐身时不再显示药水粒子，身上的装备与第三人称手持物品也一并隐藏 |
| `betterInvisibilityEffectParticles` 隐藏药水粒子 | 开 | 隐身时不再显示药水效果粒子 |
| `betterInvisibilityLayers` 隐藏装备与手持物 | 开 | 第三人称下同时隐藏盔甲、披风与手持物品 |
| `betterInvisibilityLlamaCarpet` 隐藏羊驼地毯 | 开 | 羊驼隐身后，身上的地毯与装饰也一并隐藏 |
| `nightVisionFadeOut` 夜视淡出 | 关 | 夜视快结束时不再闪烁，而是平滑淡出恢复正常 |
| `nightVisionFadeOutDuration` 淡出时长 | 200 | 平滑淡出的时长（tick，20 tick = 1 秒），范围 20 ~ 400 |
| `nightVisionFadeOutCurve` 淡出曲线 | `smooth` | `smooth` 平滑（两端更慢）/ `linear` 线性 |
| `connectedBlockOutline` 连接方块轮廓 | 关 | 准星对准门、床、双箱、双层植物等连接方块时，显示整体轮廓（类似基岩版） |
| `connectedBlockOutlineDoorFullHeight` 门两格高度 | 开 | 开启时门按 2 格视觉高度描边；关闭后门保持原版轮廓 |

#### 屏幕覆盖层与加载界面

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableOverlayEffects` 禁用屏幕覆盖层 | 关 | 总开关：关闭细雪、岩浆、水下等全屏覆盖效果；下面的子开关分别控制 |
| `disablePowderSnowOverlay` 禁用细雪覆盖层 | 开 | 陷入细雪时不再显示冰霜边框覆盖层 |
| `disableLavaOverlay` 禁用岩浆覆盖层 | 开 | 在岩浆中不再显示橙红色全屏覆盖层 |
| `disableWaterOverlay` 禁用水下覆盖层 | 开 | 水下不再显示蓝色水面覆盖层 |
| `disableWaterFog` 禁用水下雾 | 开 | 水下不再有雾与蓝色滤镜（26.3 的雾是独立系统） |
| `disableLavaFog` 禁用岩浆雾 | 开 | 岩浆里不再有橙色雾 |
| `disablePowderSnowFog` 禁用细雪雾 | 开 | 陷入细雪时不再有白雾 |
| `forceCloseLoadingScreen` 强制关闭加载界面 | 关 | 加载世界时立刻关闭“正在加载地形”界面，提前看到世界；世界仍在后台继续加载 |
| `forceCloseLoadingScreenInstant` 完全跳过加载界面 | 关 | 直接关闭加载界面（完全跳过，可能短暂掉入世界）；仅在上一项开启时生效 |
| `forceCloseLoadingScreenKeepFirst` 保留加载文字进度 | 关 | 不跳过加载界面的文字与进度（背景和世界仍正常加载） |
| `forceCloseLoadingScreenSkipResourcePackFade` 跳过资源包淡入淡出 | 开 | 去掉资源包加载屏幕的淡入淡出并立即移除覆盖层（资源加载完成后才进入游戏） |
| `disableNetherPortalEffect` 禁用地狱门特效 | 关 | 进入下界传送门时不再显示紫色覆盖层、镜头旋转与传送门音效；关界面与传送逻辑不变 |
| `disableNetherPortalOverlay` 禁用紫色覆盖层 | 开 | 站在传送门里时不再显示紫色半透明覆盖层 |
| `disableNetherPortalCamera` 禁用镜头旋转 | 开 | 站在传送门里时镜头不再绕视角旋转 |
| `disableNetherPortalSound` 禁用传送门音效 | 开 | 不再播放传送门的循环音效与传送瞬间的音效 |
| `disableResourcePackCompatibilityCheck` 禁用资源包兼容性检查 | 关 | 资源包界面不再显示不兼容标记，也不会在启用不兼容资源包时弹出确认提示；仅影响资源包，不影响数据包 |
| `resourcePackCompatibilityAlsoDataPacks` 同时忽略数据包 | 关 | 数据包界面也不显示不兼容标记、不弹确认框 |

#### 提示文本

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `enchantmentLevelArabic` 附魔等级阿拉伯数字 | 关 | 附魔等级使用 1、2、3... 代替 I、II、III... |
| `enchantmentLevelArabicStyle` 数字样式 | `plain` | `plain` 锋利 3 / `parentheses` 锋利 (3) |
| `enchantmentDescriptions` 附魔描述 | 关 | 在物品提示中为每个附魔追加一行描述（附魔书同样支持） |
| `enchantmentDescriptionsSneakOnly` 仅潜行时显示 | 关 | 只在按住潜行键时追加附魔描述，避免提示过长 |

#### 世界交互与世界生成

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableUnderwaterMiningPenalty` 禁用水下挖掘惩罚 | 关 | 在水下挖掘时不再按水下挖掘速度属性减速 |
| `underwaterMiningPenaltyKeep` 保留水下惩罚 | 0 | 保留原版水下挖掘减速的百分比（0 ~ 100）；0 = 完全去掉（默认），100 = 与原版相同 |
| `disableAirMiningPenalty` 禁用空中挖掘惩罚 | 关 | 离地挖掘时不再额外减速 |
| `airMiningPenaltyKeep` 保留空中惩罚 | 0 | 保留原版空中挖掘减速的百分比（0 ~ 100）；0 = 完全去掉（默认），100 = 与原版相同 |
| `disableCurseEnchantmentRestriction` 移除诅咒附魔 | 关 | 砂轮可以去除诅咒附魔（原版会保留诅咒）；仅在单人/局域网生效 |
| `curseRemovalScope` 诅咒移除范围 | `all` | `all` 全部诅咒 / `binding_only` 仅绑定诅咒 / `vanishing_only` 仅消失诅咒；其它模组的诅咒仅在 `all` 下移除 |
| `removeEnchantmentConflicts` 移除附魔冲突 | 关 | 铁砧合并装备时忽略附魔互斥限制，可以把锋利与亡灵杀手等互斥附魔合到同一件装备上；仅在单人/局域网生效 |
| `removeEnchantmentConflictsSameGroupOnly` 仅同类附魔 | 关 | 只解除同一互斥组（同一 `exclusive_set` 标签）内部的限制；关闭时解除全部互斥限制 |
| `disableTooExpensive` 禁用过于昂贵 | 关 | 铁砧花费达到 40 级时不再提示“过于昂贵”，结果可以直接取出，等级不足也能取走（等级最低到 0）；仅在单人/局域网生效 |
| `tooExpensiveMaxLevelCost` 经验花费上限 | 0 | 可承受的最大经验等级花费（0 ~ 1000）；0 = 不限制（默认），超过后按下一项决定阻止还是仅提示 |
| `tooExpensiveWarnOnly` 超限仅提示 | 关 | 花费超过费用上限时仍显示“过于昂贵”提示，但允许取出；关闭时超过上限按原版阻止 |
| `visibleTraders` 显示锁定交易 | 关 | 在村民交易界面显示未来等级（锁定）的交易，村民升级时按顺序解锁；仅在单人/局域网生效 |
| `visibleTradersOnlyLocked` 仅显示锁定交易 | 关 | 交易界面只显示尚未解锁的未来等级交易，隐藏当前等级已解锁的交易；这些行均为锁定状态，不能点击 |
| `keepLastTorch` 不使用最后一根火把 | 关 | 放置火把时如果背包里只剩最后一根则不会放置；仅在单人/局域网生效 |
| `keepLastTorchMinCount` 保留火把数量 | 1 | 剩余火把不超过该数量时阻止放置（1 ~ 8，默认 1 = 只保留最后一根） |
| `disableSmithingTemplateConsumption` 禁用锻造模板消耗 | 关 | 锻造台取出成品时保留锻造模板，不再消耗；仅在单人/局域网生效 |
| `smithingTemplateKeepChance` 保留概率 | 100 | 锻造模板被保留的概率（0 ~ 100）；100 = 必定保留（默认），0 = 完全按原版消耗 |
| `itemDespawnTime` 掉落物消失时间 | 6000（原版 5 分钟） | 滑块调节 60 ~ 36000 tick（20 tick = 1 秒）；仅在单人/局域网生效 |
| `itemDespawnPlayerDroppedOnly` 仅玩家丢弃物生效 | 关 | 只有玩家主动丢弃的掉落物按上面的时间消失，怪物与方块掉落保持原版 |
| `itemDespawnTimeXpOrbs` 经验球消失时间 | 6000 | 经验球的消失时间（tick），范围同上 |
| `disableFlatWorldSlimeSpawn` 禁用超平坦史莱姆生成 | 关 | 超平坦世界不再生成史莱姆；仅在单人/局域网生效 |
| `flatWorldSlimeSpawnAllWorlds` 对所有世界生效 | 关 | 所有世界类型都不生成史莱姆，不再只限超平坦 |
| `disableBedTooFar` 禁用床距离限制 | 关 | 离床再远右键也能直接入睡，不再提示“离床太远”；仅在单人/局域网生效 |
| `bedTooFarMaxDistance` 允许入睡距离 | 0 | 允许入睡的最大距离（0 ~ 128 格）；0 = 不限制（默认） |
| `ironGolemRideMinecart` 铁傀儡进矿车 | 关 | 空矿车碰到铁傀儡时，让铁傀儡坐进矿车；只限普通矿车，漏斗/运输/动力/TNT 等特种矿车不拉铁傀儡；仅在单人/局域网生效 |
| `ironGolemRideMinecartMaxDistance` 玩家附近距离 | 0 | 只在玩家附近自动上车（0 ~ 64 格）；0 = 不限制（默认） |
| `signOpenContainer` 穿牌打开容器 | 关 | 准星对准告示牌时直接打开它背后（或相邻）的容器；潜行右键默认仍按原版编辑告示牌 |
| `signOpenContainerSneakBypass` 潜行时保持原版 | 开 | 开启时潜行仍编辑告示牌；关闭后潜行也能穿牌打开容器 |
| `snowGolemBiomeImmunity` 雪傀儡不受群系影响 | 关 | 温暖或干燥群系不再让雪傀儡因群系属性融化；雨、水和熔岩伤害仍按原版；仅在单人/局域网生效 |
| `snowGolemBiomeImmunityOverworldOnly` 仅主世界生效 | 关 | 只在主世界免疫群系融化；下界与末地仍按原版群系属性融化 |
| `wartBlockDecomposition` 疣块分解 | 关 | 合成格里单独放一个下界疣块，结果槽直接产出 9 个下界疣（原版已有配方时优先走原版）；仅在单人/局域网生效 |
| `wartBlockDecompositionOutput` 疣块产出数量 | 9 | 分解一个下界疣块得到的下界疣数量（1 ~ 9） |

</details>

<details>
<summary><b>修复 — 修正原版 Bug（21 项：MC-577、MC-247973、MC-79545 等）</b></summary>

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `fixBoatStuckOnDirtPath` 草径卡船修复 | 关 | 只给船增加 1/16 格上台阶高度，消除土径与完整方块之间的卡顿；不改变方块碰撞，不影响放置、摩擦和滑行速度 |
| `fixBoatStuckOnDirtPathPlayerOnly` 仅玩家船只 | 关 | 只对载有玩家的船生效；空船与动物船保持原版 |
| `parrotDanceOnShoulder` 鹦鹉肩膀跳舞 | 关 | 像基岩版一样：附近唱片机播放音乐时，站在玩家肩膀上的鹦鹉也会跳舞，而不是保持静止 |
| `parrotDanceOnShoulderLocalPlayerOnly` 仅本地玩家 | 关 | 只让本地玩家肩膀上的鹦鹉跳舞，其它玩家保持原版姿态 |
| `betterBold` 更好的粗体 | 关 | 把粗体字形偏移从原版 1.0 像素改为配置百分比（默认 0.5 像素，与 MCPatcher/OptiFine 一致），粗体文字更清晰、不再粘连 |
| `betterBoldOffsetPercent` 粗体偏移 | 50 | 粗体偏移百分比（10 ~ 100）；50 = 0.5 像素（默认），100 = 原版 1.0 像素 |
| `frostWalkerJumpFix` 冰霜行者跳跃修复 | 关 | 跳跃途中也继续在脚下结冰：原版只在站地帧结冰，跳着过水面会踩空落水；仅放宽玩家的冰霜行者判定，卡在载具上或其它附魔效果保持原版；仅在单人/局域网生效 |
| `frostWalkerJumpFixJumpOnly` 仅跳跃上升 | 关 | 只在向上跳跃时结冰；下落过程仍按原版不结冰 |
| `fixMouseButtonInventory` 物品栏鼠标按键修复 | 关 | 修复 MC-577：物品栏里绑定到鼠标键的「关闭背包」与「丢弃物品」不再被吞掉；Ctrl+丢弃键仍按原版丢整组 |
| `chatLagFix` 聊天卡顿修复 | 关 | 修复 MC-247973：收到聊天消息时改为后台拉取屏蔽名单，避免主线程同步网络请求卡顿；首次消息先按未屏蔽处理，后台完成后生效 |
| `experienceBarHighLevelFix` 经验条高等级修复 | 关 | 修复 MC-79545：等级极高时经验需求整数溢出为负数，经验条不再整条消失；正常等级与原版完全一致 |
| `bookGuiVerticalCenterFix` 书本界面居中修复 | 关 | 修复 MC-61489：书本查看与书与笔编辑界面向下移动，整体更居中；底部按钮不再贴着屏幕边缘 |
| `potionTooltipCategoryFix` 药水效果分类修复 | 关 | 修复 MC-84661 / MC-170462：药水类物品提示中的「发光」与「不祥之兆」按有害效果显示为红色，不再误显示为中性效果的蓝色；其它效果不变 |
| `shieldBlockHurtSoundFix` 举盾受伤音效修复 | 关 | 修复 MC-105068：完全格挡攻击时不再播放玩家受伤音效，避免叠在盾牌格挡/禁用音效上；部分格挡仍有伤害时保持原版；仅在单人/局域网生效 |
| `spectatorOverlayFix` 旁观者覆盖层修复 | 关 | 修复 MC-217716：进入旁观者模式后不再渲染反胃与下界传送门覆盖层；其它游戏模式保持原版 |
| `cooldownIndicatorFix` 冷却指示器闪烁修复 | 关 | 修复 MC-268420：高攻速下切换物品时，准星旁攻击冷却进度不再闪一帧；关闭开关时保持原版 |
| `fishingLineThirdPersonFix` 第三人称鱼线修复 | 关 | 修复 MC-4490：潜行时第三人称鱼线仍连接到鱼竿，不再悬在鱼竿上方；未潜行与第一人称保持原版 |
| `respawnHotbarFix` 重生快捷栏修复 | 关 | 修复 MC-143474：重生后保留死亡前的快捷栏选中槽位，不再重置到第 1 格；关闭开关时保持原版 |
| `sneakAnimationFix` 潜行动画重复修复 | 关 | 修复 MC-159163：快速按潜行键时只播放一次潜行动画；只忽略本地玩家的 POSE 同步，其它实体保持原版 |
| `skeletonAimingFix` 骷髅瞄准修复 | 关 | 修复 MC-121706：骷髅与幻术师射箭时视线随目标上下；仅在单人/局域网生效 |
| `saplingGrowthFix` 2×2 树苗生长修复 | 关 | 修复 MC-8187：2×2 云杉/丛林树苗在北/西侧有相邻方块时也能生长；仅在单人/局域网生效 |

</details>

<details>
<summary><b>优化 — MiniHUD 掉帧优化（2 项）</b></summary>

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `miniHudFpsOptimization` MiniHUD 掉帧优化 | 关 | 把 MiniHUD 的 HUD 行刷新间隔放宽到 150ms，减少重复重算造成的掉帧；仍保持在渲染线程执行，避免并发访问；需要安装 MiniHUD |
| `miniHudRefreshIntervalMs` 刷新间隔 | 150 | HUD 行刷新间隔（50 ~ 1000 毫秒）；越大越省性能、数字刷新越慢 |

</details>

<details>
<summary><b>通用 — 语言、快捷键与列表行为（17 项）</b></summary>

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `language` 界面语言 | `zh_cn` | 支持 `zh_cn` / `en_us` / `ru_ru` / `fr_fr`；关闭「跟随系统语言」后生效 |
| `followSystemLanguage` 跟随系统语言 | 开 | 使用游戏当前语言，忽略上面的语言选择 |
| `openConfigGui` 打开配置界面 | G + C | 打开或关闭本模组配置界面的快捷键；任意界面可用 |
| `rememberSelectedTab` 标签记忆 | 关 | 记住上次选择的分类标签（全部/通用/功能/修改/修复/优化/测试/收藏），下次打开配置界面直接停在该标签 |
| `rememberSelectedTabFallback` 默认标签 | 全部 | 关闭标签记忆或首次打开时停在的标签（全部/通用/功能/修改/修复/优化/测试/收藏） |
| `favoriteStarStyle` 收藏星样式 | `star` | `star` 自绘星形 / `nether_star` 原版下界之星；同时作用于配置行右侧与存档列表左侧的收藏星 |
| `scrollListMemory` 滚动列表记忆 | 开 | 记住列表滚到了哪里，关闭界面再打开回到上次位置 |
| `scrollListMemoryPersist` 跨重启记忆 | 关 | 把滚动位置写入 `config/tweakify-mod-scroll-memory.json`，重启游戏后仍然生效 |
| `malilibScrollMemory` MaLiLib 界面 | 开 | 记忆 MaLiLib 配置界面（含本模组配置界面）的滚动位置 |
| `modMenuScrollMemory` Mod Menu 列表 | 开 | 记忆 Mod Menu 模组列表的滚动位置；未安装 Mod Menu 时不生效 |
| `searchHighlight` 搜索关键词高亮 | 开 | 搜索命中的部分改用金色显示（本地化名称与英文配置键都高亮）；多个关键词用 `\|` 分隔 |
| `smoothScroll` 平滑滚动与拖动 | 开 | 为配置界面列表、创造物品栏、聊天栏与 Mod Menu 列表启用平滑滚动；配置界面还支持按住左键拖动列表（从名称/空白处起拖、移动几像素生效，不影响按钮与滑块）；创造栏与物品分组兼容 |
| `smoothScrollConfigList` 配置界面列表 | 开 | 平滑滚动 + 左键拖动只作用于 MaLiLib 配置界面 |
| `smoothScrollCreative` 创造物品栏 | 开 | 创造栏滚动使用缓动过渡，并按像素平滑（自绘槽位底纹随内容上移，不再按整行跳） |
| `smoothScrollChat` 聊天栏 | 开 | 聊天历史滚动分摊到多帧；新消息的自动上滚仍即时生效 |
| `smoothScrollModMenu` Mod Menu 列表 | 开 | Mod Menu 模组列表平滑滚动；未安装 Mod Menu 时不生效 |
| `smoothScrollDuration` 平滑时长 | 60 | 滚动动画收敛时长（30 ~ 400 毫秒）：越大越柔和、越小越跟手 |

</details>

> 配置界面还有一个「测试」标签，包含下面几个调试辅助项，以及不是配置项的「档案编辑器」（保存、加载、删除整套配置档案）。

> 「收藏」标签只显示手动收藏的配置项：点击任意配置行右侧（重置按钮右边）的星标即可收藏/取消收藏。存档列表左侧也有同样的星标：收藏的存档会**置顶显示**，取消收藏后**回到原来的位置**；收藏期间**不可删除**（删除按钮置灰并提示，需先取消收藏）。收藏数据保存在 `config/tweakify-mod-favorites.json`，外观由「通用」标签的「收藏星样式」决定。

<details>
<summary><b>测试 — 调试 HUD 与报表（13 项）</b></summary>

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `debugInfoOverlay` 调试信息叠加层 | 关 | 在 HUD 左下角显示坐标、朝向、维度与脚下方块 |
| `debugInfoShowPerformance` 显示性能 | 开 | 追加帧率与内存占用 |
| `debugInfoShowTarget` 显示准星目标 | 开 | 追加准星指向的方块/实体 ID |
| `debugInfoShowBiome` 显示生物群系 | 关 | 追加玩家所在生物群系的 ID |
| `debugInfoShowSpeed` 显示移动速度 | 关 | 追加水平/垂直速度（格每秒），便于确认飞行、攀爬、如履平地是否生效 |
| `debugInfoShowEntities` 显示附近实体 | 关 | 按 32 格范围统计附近生物、掉落物与其他实体数量；每 500ms 刷新一次 |
| `debugInfoShowConfigStats` 显示配置统计 | 关 | 统计功能标签下已开启与已修改的布尔开关数量；每 500ms 刷新一次 |
| `debugInfoShowEffects` 显示药水效果 | 关 | 追加当前药水效果数量与最短剩余时间 |
| `debugInfoShowHooks` 显示钩子探针 | 关 | 追加已激活的 Mixin 行为探针列表 |
| `debugProbeReport` 钩子探针报表 | 未绑定 | 纯快捷键动作：按下后把已激活的探针输出到聊天栏（或剪贴板，见下一项） |
| `debugProbeReportCopy` 探针输出到剪贴板 | 关 | 开启后探针报表写入剪贴板（每行一个钩子），不再输出到聊天栏 |
| `debugEnabledFeaturesReport` 已启用功能报表 | 未绑定 | 纯快捷键动作：按下后把已启用的根功能开关名称输出到聊天栏 |
| `debugEnabledFeaturesReportCopy` 报表输出到剪贴板 | 关 | 开启后报表写入剪贴板（每行一个开关），不再输出到聊天栏 |

</details>

> **服务端判定**：下列功能由集成服务端执行，仅在单人游戏或自己开设的局域网世界生效——诅咒附魔、附魔过于昂贵、移除附魔冲突、可见交易、中键获得液体桶、生存模式中键取物、锹单层铲雪、锻造模板、掉落物与经验球寿命、超平坦史莱姆、疣块分解、床距离、放置覆盖、耕地踩踏、骨粉催熟、刷怪蛋投掷、雪傀儡不受群系影响、维度传送、蛛网/液体/粘液块/蜂蜜块物理、冰霜行者跳跃修复、举盾受伤音效修复、骷髅瞄准修复、2×2 树苗生长修复。

> `itemDespawnTime` 把寿命调长依赖对 `ItemEntity.tick` 内部调用的重定向：若其他模组整体重写了该方法，注入会静默失效，此时日志里不会出现 `Mixin hook active: itemDespawnTick`，功能退回原版行为。

<a name="commands"></a>

[↑ 返回顶部](#)

## 🧾 命令

| 命令 | 说明 |
| --- | --- |
| `/tweakify <开关>` | 切换指定开关（需权限等级 ≥2，OP 满足；单人世界房主自动放行） |
| `/tweakify <开关> <true\|false>` | 直接设置 |
| `/tweakify restrictedOffhandItems add <物品>` | 加入受限副手列表 |
| `/tweakify restrictedOffhandItems remove <物品>` | 移出受限副手列表 |
| `/tweakify restrictedOffhandItems list [页码]` | 查看受限副手列表 |
| `/tweakify restrictedOffhandItems reset` | 重置为默认光源物品列表 |
| `/tweakify profile save <名称>` | 把当前整套配置保存为命名档案（同名覆盖） |
| `/tweakify profile load <名称>` | 应用已有档案；写盘失败时整份回滚 |
| `/tweakify profile delete <名称>` | 删除档案 |
| `/tweakify profile list` | 列出全部档案；文件损坏/读失败会单独提示 |
| `/fconfig` | 打开配置界面（客户端命令） |

修改命令权限后，请按「手动回归：/tweakify 命令权限」一节验证三种玩家身份。

档案操作也可以直接在配置界面的「测试」标签点击「档案编辑器」完成（保存、加载、删除）；
界面的删除需要连续点击两次确认，命令的删除直接生效。

[↑ 返回顶部](#)

## ⌨️ 快捷键

- **打开/关闭配置界面**：默认 **G + C**（对应配置项 `openConfigGui`），任意界面可用
- **切换快捷键**：带快捷键槽的根功能可以绑定一个按键，按下即切换该开关，默认全部未绑定；除「修改」标签的「禁用屏幕覆盖层」与「测试」标签的「调试信息叠加层」外，带切换键的都是「功能」标签的根功能，子设置不提供
- **钩子探针报表**：给「测试」标签的「钩子探针报表」绑定按键后，按下即把已激活的 Mixin 行为探针打到聊天栏；开启「探针输出到剪贴板」后改为写入剪贴板（纯动作，没有开关）
- **已启用功能报表**：给「测试」标签的「已启用功能报表」绑定按键后，按下即把已启用的根功能开关打到聊天栏（纯动作，没有开关）
- **主动爬行**：需要先在配置界面开启「主动爬行」总开关，并给子项「爬行按键」绑定一个按键；按住爬行、松开站起
- **复制物体 ID**：需要先在配置界面里给「复制目标 ID 快捷键」绑定按键（默认未绑定），且「复制物体 ID」开关为开启状态；总开关所在行的快捷键只负责切换开关

[↑ 返回顶部](#)

## 🔍 排查

- 日志里的 `Mixin hook active: <名称>` 表示对应 `require = 0` 注入点已经执行过；功能开了却没效果时，先看这条在不在，能区分“没注入”和“注入了但逻辑不生效”。
- 连接远程服务器时会提示一次：铁砧、铲雪、掉落物寿命、史莱姆生成、耕地踩踏、中键取物、移动物理（蛛网/液体/粘液块/蜂蜜块）与冰霜行者跳跃修复等功能在服务端执行，只在单人游戏或局域网主机生效。
- 打开 Mod Menu 列表时会不断记录滚动位置，但写盘只在关闭界面或每 10 秒节流一次，不会每帧同步 IO。

<a name="config-file"></a>

[↑ 返回顶部](#)

## 💾 配置文件

`config/tweakify-mod.json`，由配置界面或命令自动保存。文件损坏时会被重命名为 `tweakify-mod.json.broken-<时间戳>` 并重新生成默认配置；若备份失败则保留原文件、不覆盖。若存在旧版 `config/fantasy-mod.json`，首次启动会自动迁移为 `tweakify-mod.json`；配置档案单独存放在 `config/tweakify-mod-profiles.json`，收藏数据存放在 `config/tweakify-mod-favorites.json`（损坏时同样备份为 `.broken-<时间戳>`）。

文件里带有 `configVersion` 字段：旧版本配置读取后会自动升级并回写，便于以后调整字段语义时做迁移。从版本 2 起，`scrollListMemory` 默认开启；版本 1 的旧配置读取后会升级并回写。

从版本 3 开始，旧的 `motionCameraTilt` / `hotkeyMotionCameraTilt` 会迁移到 `cameraMotion` / `hotkeyCameraMotion`，旧的 `mergeCreativeItemEntries` / `hotkeyMergeCreativeItemEntries` 会迁移到 `itemGroups` / `hotkeyItemGroups`；迁移后旧键不再写回。

写入采用「临时文件 + 原子替换」，写盘失败时会把内存里的配置整体回滚到上一次成功的快照，不会留下半新半旧的 JSON。

[↑ 返回顶部](#)

## ➕ 新增配置项

配置项的注册关系收敛到了少数几个地方，新增一个开关只需四步：

1. 在 `TweakifyModConfig` 加字段（默认值写在字段初始化里）。
2. 在 `ConfigCatalog` 的 `ENTRIES` 表加一行（父项 / 分类 / 是否需要 `/tweakify` 命令）。
3. 在 `TweakifyModConfigOptions` 声明对应的 MaLiLib 选项（含 `.apply(...)` 语言前缀）。
4. 在 4 份语言文件补 `name` / `prettyName` / `comment`（需要命令时再补 `command.label.<name>`）。

启动时会校验「配置字段 ⟷ 配置目录 ⟷ MaLiLib 选项」双向覆盖，漏登记会直接抛异常而不是静默丢项；配置界面的树、分类标签、统计、快捷键与命令表都由目录派生。

[↑ 返回顶部](#)

## 🛠️ 构建与测试

```bash
./gradlew build          # 编译 + 打包
./gradlew test           # 单元测试
./gradlew runClient      # 启动开发客户端
```

Windows 下使用 `gradlew.bat`。需要环境变量 `JAVA_HOME` 指向 JDK 25。

[↑ 返回顶部](#)

## 🧪 手动回归：/tweakify 命令权限

客户端 GameTest 已移除，`/tweakify` 的真实命令树权限不再有自动化覆盖。修改
`TweakifyModCommands.canUseCommands` 或 `register` 中的 `requires` 后，请按本清单手动回归。
测试覆盖缺口与相关取舍（如方块扫描 section 预筛）记录在 [`docs/tech-debt.md`](docs/tech-debt.md)。

权限规则：**权限等级 ≥2（管理员；OP 默认等级 4 也满足）或单人世界房主放行**，其余玩家拒绝。该判定同时决定命令是否
下发到客户端（补全列表里能否看到 `/tweakify`），因此“命令完全不出现”和“提示权限不足”
都属于拒绝的正常表现。

### 场景 1：单人未开作弊的房主

1. 创建新世界，关闭「允许作弊」。
2. 进入世界后按 `T` 输入 `/twe`，确认补全列表出现 `tweakify`。
3. 执行 `/tweakify betterBold`，确认聊天栏出现切换反馈。
4. 用 `G + C` 或 `/fconfig` 打开配置界面，确认「更好的粗体」同步为开启。
5. 退出并重启客户端，确认 `config/tweakify-mod.json` 中 `betterBold` 仍为 `true`。

预期：房主权限等级为 0，但凭单人房主身份放行；命令可补全、可执行，并与配置界面/文件同步。

### 场景 2：OP 玩家（权限等级 ≥2）

1. 单人世界开启「允许作弊」并成为 OP；或由局域网世界的房主给自己 OP。本模组是纯客户端，
   专用服务器不加载，`/tweakify` 不会注册。
2. 输入 `/twe`，确认 `tweakify` 出现在补全中。
3. 执行 `/tweakify betterBold false`，确认是直接设置而不是切换。
4. 执行 `/tweakify betterBold true` 恢复。

预期：权限等级 ≥ 2 放行，`<开关> <true|false>` 直接设置。

### 场景 3：局域网访客（未授权）

1. 房主创建世界并「对局域网开放」，访客加入。
2. 访客输入 `/twe`，确认补全中**没有** `tweakify`。
3. 访客直接执行 `/tweakify betterBold`，确认提示未知命令（命令未下发）或权限不足，且房主配置没有变化。
4. 访客执行 `/fconfig`，确认仍能打开访客自己的配置界面。

预期：访客权限等级 0 且不是单人房主，命令被拒绝；客户端侧功能（`/fconfig`、配置界面）不受影响。

### 场景 4（可选）：访客被授予命令权限

1. 房主在「对局域网开放」界面开启「允许作弊」（访客获得命令权限），或给访客 OP。
2. 访客重新连接后输入 `/twe`，确认 `tweakify` 出现并可执行。

预期：权限等级 ≥2 放行，与是否房主无关。被授权的访客执行 `/tweakify` 修改的是房主集成服务端
进程里的配置（不是访客自己的本地配置），测试后请恢复房主配置。若测试环境支持把玩家设为等级 1
（如权限管理插件），可额外确认等级 1 仍被拒绝。

### 回归记录

至少记录：游戏版本、场景、是否出现补全、命令反馈文本、`config/tweakify-mod.json` 对应字段值，
以及 `logs/latest.log` 中是否有异常。

[↑ 返回顶部](#)

## 📜 许可证

MIT

> 说明：`src/main/resources/assets/tweakify-mod/textures/gui/sprites/container/` 下的
> `icon_slot.png` / `item_slot.png` / `plus.png` / `minus.png` 四张精灵图来自
> [BizCub/inventory-item-groups](https://github.com/BizCub/inventory-item-groups)（MIT），
> 与项目整体使用相同的 MIT 许可。
>
> 收藏星纹理位于 `assets/tweakify-mod/textures/gui/sprites/favorite/`：星形一套为自绘；
> 下界之星一套取自 Minecraft 原版物品纹理（版权归 Mojang），仅用于在界面上模仿原版外观。
