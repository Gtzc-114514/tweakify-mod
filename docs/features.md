# ✨ 功能一览

下面按配置界面的标签分为 8 类、共 400 余项配置（默认折叠，点击分类标题展开）。每行以 `配置字段` 开头，后接便于识别的中文名；「类型」列是对应配置控件的类型（布尔值 / 整数 / 浮点 / 选项 / 快捷键 / 字符串列表），「默认值」列是新生成配置文件时的初始值。说明中标注「仅在单人/局域网生效」的功能，判定发生在集成服务端，专用服务器上不会生效。

> **已知问题**：MC 26.3 在 **Vulkan 后端 + NVIDIA 驱动**下渲染实体描边会崩溃（`nvoglv64.dll`）。使用实体高亮时请把图形 API 设为 OpenGL。

<details>
<summary><b>功能 — 新增行为，快捷键最完整（8 组 / 140 余项：物品信息、快捷右键、交互冷却、移动、相机、HUD、传送、世界交互、工具）</b></summary>

#### 交互冷却与副手回退

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `useCooldown` 右键交互冷却 | 冷却组的总开关；关闭时主手、副手的使用冷却全部失效 | 布尔值 | 开 |
| `mainHandUseCooldown` 主手使用冷却 | 一次右键只使用一次，命中的物品由下方列表决定 | 布尔值 | 关 |
| `mainHandUseCooldownItems` 主手使用冷却物品 | 冷却生效的物品 ID 列表 | 字符串列表 | 全部可交互物品 |
| `offhandUseCooldown` 副手使用冷却 | 同上，作用于副手 | 布尔值 | 关 |
| `offhandUseCooldownItems` 副手使用冷却物品 | 冷却生效的物品 ID 列表 | 字符串列表 | 全部可交互物品 |
| `blockOffhandFallback` 副手回退拦截 | 主手物品用完且仍按住右键时，阻止受限副手物品自动接替使用 | 布尔值 | 开 |
| `interceptBlockInteraction` 拦截方块交互 | 副手回退生效时一并拦截方块交互；关闭时仍拦副手方块放置，但放行门、拉杆、按钮、箱子等可交互方块 | 布尔值 | 关 |
| `interceptEntityInteraction` 拦截实体交互 | 副手回退生效时一并拦截副手对实体的交互 | 布尔值 | 关 |
| `restrictedOffhandItems` 受限副手物品 | 只有命中该列表的副手物品会被拦截 | 字符串列表 | 全部光源物品 |
| `miningCooldown` 左键交互冷却 | 一次左键只挖一个方块，需松开再按才能继续；仅在方块真正被破坏时计数 | 布尔值 | 关 |
| `miningCooldownAttacks` 攻击也限一次 | 攻击实体也受限制，与挖方块共用同一次按住周期 | 布尔值 | 关 |
| `elytraFireworkCooldown` 鞘翅烟花限制 | 滑翔中用烟花加速后，要等加速结束才能再次右键使用烟花 | 布尔值 | 关 |
| `elytraFireworkCooldownTicks` 最短间隔 | 两次烟花之间的最短间隔（tick，0 ~ 200）；0 表示等本次加速结束 | 整数 | 0 |

#### 移动与视角

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableFlightDrift` 禁用飞行漂移 | 飞行时松开方向键立即停止水平移动 | 布尔值 | 关 |
| `flightDriftVertical` 垂直飞行影响 | 松开跳跃/潜行后也立即停止升降 | 布尔值 | 关 |
| `creativeFlightNoclip` 创造飞行穿墙 | 创造模式飞行时进入无碰撞状态，可以直接穿过方块；非创造或未飞行时不生效。仅在单人/局域网主机生效；远程服务器由服务端校验，不会放行 | 布尔值 | 关 |
| `autoStepUp` 如履平地 | 自动跨上 1 格高的方块（像马一样）；仅对本地玩家生效 | 布尔值 | 关 |
| `autoStepUpHeight` 跨步高度 | `one_block` 1 格 / `one_and_half` 1.5 格 / `two_blocks` 2 格；越高越容易跨上台阶与斜坡 | 选项 | `one_block` |
| `betterClimbing` 更好的攀爬 | 优化攀爬方块中的移动：向下看快速下降、上爬更快、可从地面起跳等，子项可单独开关；水平方向不减速请用「修改」标签的「经过可攀爬方块不减速」 | 布尔值 | 关 |
| `climbingFastDescend` 向下看加速下降 | 在梯子、藤蔓上向下看时快速下降 | 布尔值 | 开 |
| `climbingFasterAscend` 攀爬上升更快 | 向上攀爬时移动速度更快 | 布尔值 | 开 |
| `climbingJumpFromGround` 攀爬时从地面起跳 | 贴着可攀爬方块站在地面时也能直接起跳 | 布尔值 | 开 |
| `climbingCancelCollision` 无输入时取消碰撞减速 | 没有移动输入时取消攀爬方块边缘的碰撞减速 | 布尔值 | 开 |
| `forceCrawl` 主动爬行 | 开启总开关并给「爬行按键」绑定按键后，按住强制进入爬行姿态，可钻进 1 格高的空间；仅在单人/局域网主机生效 | 布尔值 | 关 |
| `forceCrawlKey` 爬行按键 | 按住爬行、松开站起 | 快捷键 | 未绑定 |

#### 相机运动

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `cameraMotion` 相机运动 | 总开关：转向/横移侧倾、前后/垂直俯仰、静止晃动、爆炸/雷击/挥手震动与鼠标平滑；子项可单独开关 | 布尔值 | 关 |
| `cameraMotionIntensity` 相机运动强度 | 所有相机运动效果的强度百分比（0 ~ 200）；设为 0 可临时关闭全部效果 | 整数 | 100 |
| `cameraMotionTurningRoll` 转向侧倾 | 转动视角时镜头向转向方向轻微侧倾 | 布尔值 | 开 |
| `cameraMotionStrafingRoll` 横移侧倾 | 左右横移时镜头向移动方向轻微侧倾 | 布尔值 | 开 |
| `cameraMotionForwardPitch` 前后移动俯仰 | 前进时镜头轻微下压，后退时轻微上扬 | 布尔值 | 开 |
| `cameraMotionVerticalPitch` 跳跃/下落俯仰 | 跳跃/上升时镜头上扬，下落时下压 | 布尔值 | 开 |
| `cameraMotionIdleSway` 静止晃动 | 停止移动和转向一段时间后镜头轻微晃动，有输入立即恢复 | 布尔值 | 开 |
| `cameraMotionScreenShake` 屏幕震动 | 爆炸、雷击和挥手时产生短促震动，距离越远越弱；雷击震动依赖本地集成服务端，仅在单人/局域网生效 | 布尔值 | 开 |
| `cameraMotionMouseSmoothing` 鼠标平滑 | 平滑快速转视角时的镜头变化，只影响渲染，不改变实际瞄准 | 布尔值 | 关 |
| `cameraMotionFirstPerson` 第一人称生效 | 在第一人称也应用相机运动；默认关闭以避免晕动感 | 布尔值 | 关 |
| `cameraMotionThirdPerson` 第三人称生效 | 在第三人称（背后/正面）应用相机运动 | 布尔值 | 开 |

#### 渲染与 HUD

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `durabilityOverlay` 装备耐久显示 | 在物品栏的耐久条上方叠加耐久文字；满耐久不显示，颜色随剩余耐久由绿转白、黄、红 | 布尔值 | 关 |
| `durabilityDisplayMode` 耐久显示内容 | `percent` 耐久百分比 / `value` 剩余耐久数值（只显示当前剩余，不显示初始耐久）；两者字号一致 | 选项 | `percent` |
| `durabilityOverlayHotbarOnly` 仅快捷栏显示 | 只在快捷栏显示耐久文字，背包格子里不显示 | 布尔值 | 关 |
| `durabilityOverlayLowThreshold` 低耐久提醒阈值 | 剩余耐久低于该百分比（0 ~ 100）时才显示；0 表示始终显示 | 整数 | 0 |
| `durabilityOverlayColorMode` 耐久文字配色 | `auto` 随剩余耐久由绿转白、黄、红 / `white` / `yellow` / `red` 固定单色 | 选项 | `auto` |
| `statusEffectDurationDisplay` 更好的药水显示 | 在 HUD 右侧药水效果图标下方显示剩余时间（秒 / 分:秒 / 2h05 / 2d05）；无尽效果不显示；颜色、低时长过滤与整体缩放可调 | 布尔值 | 关 |
| `statusEffectDurationAlwaysSeconds` 固定按秒显示 | 始终使用秒数（如 125），不使用分:秒与小时/天缩写；长时间效果会缩小字号避免溢出 | 布尔值 | 关 |
| `statusEffectDurationLowThreshold` 低时长阈值 | 只在剩余时间不高于该秒数时显示（0 ~ 600）；0 表示不过滤，适合只关注即将结束的效果 | 整数 | 0 |
| `statusEffectDurationColorMode` 时间文字配色 | `auto` 剩余 10 秒内变红、30 秒内变黄，其余白色 / `white` / `yellow` / `red` 固定单色 | 选项 | `auto` |
| `statusEffectDurationScale` 效果显示大小 | 右上角药水效果图标与时间文字的整体缩放百分比（50% ~ 200%）；数字随图标同步缩放 | 整数 | 100 |
| `itemCountScale` 物品数量缩放 | 物品栏与容器格子里物品数量数字的缩放百分比（50% ~ 200%，100% = 原版）；只改变数量文字，不影响物品图标、耐久条与冷却指示器；数字位数较多时自动限制放大倍数，避免遮挡左侧格子 | 整数 | 100 |
| `enchantingTableInfo` 附魔台附魔信息 | 悬停附魔台的三个附魔选项时，显示该选项实际会给出的附魔名称与等级；按客户端同步的附魔种子计算，多人服务器同样可用 | 布尔值 | 关 |
| `savePlayTime` 存档游玩时长 | 在存档列表右上角显示该存档的累计游玩时长（读取 `stats` 中每个玩家的 `minecraft:play_time` 并累加；多人存档按全部玩家统计） | 布尔值 | 关 |
| `savePlayTimeFormat` 时长显示格式 | `hours` 小数小时（不足 1 小时显示分钟）/ `clock` 时:分 / `duration` X 小时 Y 分 | 选项 | `hours` |

#### 维度传送

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableDimensionTeleport` 禁用维度传送 | 总开关：关闭时以下各项均不生效 | 布尔值 | 关 |
| `dimensionTeleportPlayersOnly` 仅拦截玩家 | 只拦截玩家的维度传送，其他实体仍可正常穿过传送门 | 布尔值 | 关 |
| `disableNetherPortalTeleport` 禁用地狱门传送 | 地狱门拦截开关；开启且黑名单为空时拦截所有实体 | 布尔值 | 开 |
| `netherPortalTeleportBlacklist` 地狱门传送黑名单 | 非空时只拦截名单中的实体 | 字符串列表 | 空 |
| `disableEndPortalTeleport` 禁用末地门传送 | 末地门拦截开关；开启且黑名单为空时拦截所有实体 | 布尔值 | 开 |
| `endPortalTeleportBlacklist` 末地门传送黑名单 | 非空时只拦截名单中的实体 | 字符串列表 | 空 |
| `disableEndGatewayTeleport` 禁用末地折跃门传送 | 阻止实体通过末地折跃门传送；开启且黑名单为空时拦截所有实体 | 布尔值 | 开 |
| `endGatewayTeleportBlacklist` 末地折跃门传送黑名单 | 非空时只拦截名单中的实体 | 字符串列表 | 空 |
| `netherPortalTeleportDelay` 地狱门传送延迟 | 调整玩家通过地狱门的等待时间 | 布尔值 | 关 |
| `netherPortalSurvivalInstantTeleport` 生存模式无延迟传送 | 生存/冒险模式进入地狱门立即传送，与创造模式一致 | 布尔值 | 关 |
| `netherPortalCreativeDelayedTeleport` 创造模式延迟传送 | 创造模式也等待与生存模式相同的延迟，不再秒传 | 布尔值 | 关 |

> 传送判定发生在服务端，因此该功能**仅在单人游戏或自己开设的局域网世界生效**。

#### 世界交互

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `swordCutThroughPlants` 剑穿草打生物 | 主手持剑时，准星指向花草也会直接选中后方的生物，左键不会先把草打掉；有碰撞的植物（如甜浆果丛）不受影响 | 布尔值 | 关 |
| `cutThroughItems` 生效物品 | `sword` 仅剑 / `sword_and_axe` 剑与斧 / `any` 任意手持物品 | 选项 | `sword` |
| `cutThroughPlants` 植物范围 | `flowers` 花草 / `crops` 花草与作物 / `all` 全部无碰撞植物 | 选项 | `all` |
| `cutThroughSneakBypass` 潜行保持原版 | 潜行时恢复原版行为，左键先把植物打掉 | 布尔值 | 开 |
| `pickFluidBucket` 中键获得液体桶 | 准星指向水/岩浆时按中键获得对应液体桶；**仅创造模式**，生存模式请用「生存模式中键取物」；仅在单人/局域网生效 | 布尔值 | 关 |
| `pickFluidBucketFluids` 液体范围 | `water_and_lava` 水与岩浆 / `water_only` 仅水 / `lava_only` 仅岩浆；只约束创造模式的液体取桶，生存取物不受影响 | 选项 | `water_and_lava` |
| `survivalPickBlock` 生存模式中键取物 | 生存模式下按中键，准星指向方块/液体/实体时，背包里有对应物品就切换到主手；没有则保持原版 | 布尔值 | 关 |
| `survivalPickBlockIncludeFluids` 包含液体 | 开启时生存取物也处理水/岩浆（背包里有对应的桶就切换）；关闭后退回原版，只取方块与实体 | 布尔值 | 开 |
| `quickBurnItem` 便捷焚烧 | 背包或容器里拿着物品时，把它悬停在岩浆桶或打火石上右键点击即可焚烧；单人/局域网会真正销毁物品，多人服务器会改为丢弃 | 布尔值 | 关 |
| `quickBurnItemRequireSneak` 需要潜行焚烧 | 开启后需按住潜行键再右键才会焚烧，避免整理背包时误触销毁物品 | 布尔值 | 关 |
| `piglinGoldArmorSwap` 猪灵金装自动切换 | 附近有猪灵时自动把背包里的金制装备换到身上，猪灵离开后换回；仅在生存或冒险模式生效 | 布尔值 | 关 |
| `piglinGoldArmorSwapMode` 自动换装模式 | `auto` 身上没有金制护甲时从背包选一件换上 / `helmet` / `chestplate` / `leggings` / `boots` 只替换指定部位 | 选项 | `auto` |
| `disableShovelSnowFullBreak` 锹单层铲雪 | 手持锹右键多层雪层时一次只铲掉一层，不再整块移除 | 布尔值 | 关 |
| `shovelSnowDropSnowball` 铲雪掉落雪球 | 单层铲雪掉落 1 个雪球；关闭后不产生掉落物 | 布尔值 | 开 |
| `shovelSnowConsumeDurability` 铲雪消耗耐久 | 单层铲雪按普通挖掘消耗锹耐久；关闭后不消耗 | 布尔值 | 开 |
| `disableSpecifiedMobSpawn` 禁用指定生物生成 | 列表中的生物不会通过自然、结构、巡逻、增援、鸡骑士、袭击、唤魔者召唤或刷怪笼等世界生成出现；列表为空时不生效；仅在单人/局域网生效 | 布尔值 | 关 |
| `disabledSpawnEntities` 生物 ID 列表 | 需要禁止生成的生物 ID 列表（如 `minecraft:zombie`）；为空时不生效 | 字符串列表 | 空 |
| `disabledSpawnAllowPlayerSpawn` 允许玩家生成 | 刷怪蛋、命令、繁殖等玩家主动生成的生物不拦截；关闭后这些来源也会被拦截。玩家建造类触发（如铁傀儡、凋灵）恒放行 | 布尔值 | 开 |
| `peacefulModeMonsters` 和平模式怪物 | 允许列表中的怪物在和平难度下生成并保留；列表为空时不生效；仅在单人/局域网生效 | 布尔值 | 关 |
| `peacefulModeMonsterEntities` 和平模式生物 ID 列表 | 和平模式怪物功能使用的生物 ID 列表；为空时不生效 | 字符串列表 | 空 |
| `peacefulModeMonsterSpawn` 和平模式生成怪物 | 列表中的怪物可通过自然生成、刷怪笼、试炼刷怪笼和地狱门等来源在和平难度下生成，并跳过和平难度的自动清除；试炼刷怪笼需生成池全部命中列表 | 布尔值 | 开 |
| `peacefulModeMonsterNoAttack` 和平模式怪物不攻击 | 列表中的怪物在和平难度下不锁定/攻击玩家；关闭时可正常攻击 | 布尔值 | 开 |
| `throwSpawnEgg` 刷怪蛋投掷 | 像鸡蛋、雪球一样把刷怪蛋投掷出去，命中方块或实体后生成对应生物；对刷怪笼只设置类型且不消耗刷怪蛋；仅在单人/局域网生效 | 布尔值 | 关 |
| `throwSpawnEggVelocity` 投掷速度 | 投掷速度百分比（50 ~ 200，100 = 原版鸡蛋速度） | 整数 | 100 |
| `throwSpawnEggSneakBypass` 潜行时保持原版 | 开启后潜行时恢复原版放置，不再投掷刷怪蛋 | 布尔值 | 关 |
| `itemGroups` 物品分组 | 基岩版风格分组：分类页把楼梯、台阶、羊毛等同类物品折叠成组图标，点击图标上的 `+` / `-` 展开/收起，展开的成员紧跟在图标之后并使用专用底板；悬停在组图标上只显示组名 | 布尔值 | 关 |
| `itemGroupsSort` 组内排序 | `default` 保持物品栏原顺序 / `alphabetical` 按物品 ID 字母序（组图标随之变化） | 选项 | `default` |
| `itemGroupsShowItems` 显示组内物品 | 展开状态下图标每 1.5 秒轮播一个组内物品，而不是固定显示首个物品 | 布尔值 | 关 |
| `itemGroupsAddAll` 启用默认组 | 启用内置的默认物品组；关闭后只使用下面的自定义组 | 布尔值 | 开 |
| `customItemGroups` 自定义物品组 | 每行一个组：`标签页ID\|组名\|包含ID(逗号分隔)\|排除ID\|等价ID`，例如 `minecraft:building_blocks\|石英\|quartz`；组只在行首标签页生效（搜索/快捷栏/背包页不成组，只记录一次告警）；标签页 ID 大小写不敏感、缺命名空间按 `minecraft:` 补全（`:combat` 等价 `minecraft:combat`）；行内字段不足/标签页为空/包含项为空的行会在日志提示一次并忽略，整行空白忽略（格式错误行提示需开启物品分组）；组名可写进 `group_name.inventory_item_groups.<组名>` 翻译键 | 字符串列表 | 空 |
| `locatorBarHeads` 更好的定位栏 | 把定位栏上的彩色圆点换成本服玩家的头像，找不到皮肤时保留原版圆点；位于「功能」标签 | 布尔值 | 关 |
| `locatorBarAlwaysShowXp` 经验条与定位栏同时显示 | 强制经验条优先显示，并把定位栏头像叠在经验条上；乘车时不强制 | 布尔值 | 关 |
| `locatorBarHeadRenderHeads` 渲染头像本体 | 关闭后不绘制头像，只保留玩家名字；名字仍按所选显示时机出现 | 布尔值 | 开 |
| `locatorBarHeadScale` 头像尺寸 | 头像倍率档位（1 ~ 9）：0.5x / 0.6x / 0.7x / 0.8x / 1.0x / 1.1x / 1.2x / 1.3x / 1.5x；默认 5 = 原版圆点大小 | 整数 | 5 |
| `locatorBarHeadHatLayer` 显示帽子层 | 叠加玩家皮肤的第二层（帽子/头盔） | 布尔值 | 开 |
| `locatorBarHeadBorder` 头像描边 | `off` 不画 / `static` 固定颜色 / `team` 跟随计分板队伍颜色（无队伍时不画描边，名字仍为白色） | 选项 | `off` |
| `locatorBarHeadBorderThickness` 描边粗细 | 四档：`thin` 0.3 / `medium` 0.5 / `normal` 1.0 / `thick` 2.0 像素 | 选项 | `normal` |
| `locatorBarHeadBorderColor` 描边颜色 | 固定颜色描边使用的颜色；`team` 样式下忽略 | 选项 | `white` |
| `locatorBarHeadNames` 玩家名字 | `never` 从不 / `always` 总是 / `looking_at` 注视时（±20°）/ `player_list` 玩家列表打开时 | 选项 | `never` |
| `locatorBarHeadFilter` 玩家过滤 | `all` 全部 / `include` 仅包含名单 / `exclude` 排除名单；名单为空时 `include` 全部隐藏、`exclude` 全部显示 | 选项 | `all` |
| `locatorBarHeadIncludedPlayers` 包含名单 | 仅在 `include` 模式生效，名单内玩家保留头像；玩家名可写在条目中并用逗号、分号、冒号或句点分隔，忽略大小写 | 字符串列表 | 空 |
| `locatorBarHeadExcludedPlayers` 排除名单 | 仅在 `exclude` 模式生效，名单内玩家隐藏；分隔符与包含名单相同 | 字符串列表 | 空 |
| `locatorBarHeadNearbyOnly` 仅显示附近玩家 | 只给当前已加载（在附近）的玩家画头像，远处航点整条隐藏（含圆点） | 布尔值 | 关 |
| `locatorBarCompass` 显示指南针 | 固定 `高度 - 31`，字母左移 2 像素），边缘淡出；字母画在 HUD 最上层（条带底图、圆点、头像都在它下面）；开启时经验等级数字上移 6 像素避让；Flashback 回放时不显示 | 布尔值 | 关 |
| `locatorBarCompassColor` 指南针颜色 | 方位字母颜色 | 选项 | `white` |
| `locatorBarCompassShadow` 指南针描边 | 给方位字母加四向黑色描边，亮色天空下更清晰 | 布尔值 | 开 |
| `locatorBarCompassCoordinates` 坐标标记 | 用 `+Z` / `-X` / `-Z` / `+X` 代替 `S` / `W` / `N` / `E` | 布尔值 | 关 |
| `betterBlockPlacement` 更好的方块放置 | 手持方块右键时可直接替换花、高草等植物，不必先打掉；有碰撞的甜浆果丛、水草等不受影响；仅在单人/局域网生效 | 布尔值 | 关 |
| `betterBlockPlacementSneakBypass` 潜行时保持原版 | 开启后潜行时不再覆盖植物，按原版流程先打掉植物再放置 | 布尔值 | 开 |
| `betterBlockPlacementMode` 覆盖范围 | `flowers` 花草（花、树苗、高草）/ `crops` 花草与作物 / `all` 全部无碰撞植物 | 选项 | `flowers` |
| `helpfulBlockSelection` 辅助方块选区 | 独立开关：手持同类方块时放宽栅栏、铁栏杆、锁链、末地烛、避雷针、滴水石锥、竹子的选取范围（栅栏/铁栏杆按整格）；潜行时恢复原版选区 | 布尔值 | 关 |
| `betterBoneMeal` 更好的骨粉 | 让骨粉像基岩版一样催熟甘蔗与花；仅在单人/局域网生效 | 布尔值 | 关 |
| `boneMealSugarCane` 骨粉催熟甘蔗 | 一次骨粉把整株甘蔗催到最高 3 格，只消耗 1 个骨粉 | 布尔值 | 开 |
| `boneMealFlowers` 骨粉催熟花 | 在一格高的花周围长出一片相同的花（凋零玫瑰除外，与基岩版一致） | 布尔值 | 开 |

#### 物品信息与快捷右键

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `heldItemInfo` 手持物品信息 | 切换快捷栏物品时，在物品名称下方显示附魔、药水效果、容器内容等信息；子项控制内容、行数、行宽、排版、淡出与背景 | 布尔值 | 关 |
| `heldItemInfoShowName` 显示物品名称 | 把物品名称作为信息列表的第一行 | 布尔值 | 开 |
| `heldItemInfoShowEnchantments` 显示附魔 | 显示物品附魔，以及附魔书里保存的附魔 | 布尔值 | 开 |
| `heldItemInfoShowPotionEffects` 显示药水效果 | 显示药水、药箭等物品自带的效果 | 布尔值 | 开 |
| `heldItemInfoShowContainerContent` 显示容器内容 | 显示潜影盒、收纳袋、箱子等容器物品里的内容 | 布尔值 | 开 |
| `heldItemInfoMergeSimilarContainerItems` 合并相同容器物品 | 容器里同名的物品合并成一行，并累加数量 | 布尔值 | 开 |
| `heldItemInfoShowLore` 显示描述 | 显示物品的自定义描述文字，过长时按行宽折行 | 布尔值 | 开 |
| `heldItemInfoRespectHideFlags` 遵循隐藏标记 | 只显示在物品栏提示框里可见的数据组件，遵循 `HideFlags` | 布尔值 | 开 |
| `heldItemInfoMaxLines` 最大行数 | 信息列表最多显示的行数（含物品名称）；0 表示只显示名称 | 整数 | 6 |
| `heldItemInfoMaxLineLength` 最大行宽 | 单行最大字符宽度（1 ~ 240）；超出后折行 | 整数 | 48 |
| `heldItemInfoBaseFadeDuration` 基础淡出时长 | 单行信息的基础淡出时长（秒）；每多一行按子项追加 | 浮点 | 2.0 |
| `heldItemInfoBackground` 信息背景 | `vanilla` 跟随文字背景无障碍选项 / `always` 始终绘制 / `never` 不绘制 | 选项 | `vanilla` |
| `heldItemInfoPreventOverlap` 避让 HUD | 与血量条、动作栏消息错开，避免重叠 | 布尔值 | 开 |
| `inventoryQuickRightClick` 背包快捷右键 | 在背包、容器或创造模式物品栏中右键点击物品，直接打开对应界面；仅光标为空时触发，服务端需安装本模组（单人/局域网） | 布尔值 | 关 |
| `quickRightClickFunctionalBlocks` 打开功能方块 | 工作台、铁砧、附魔台、锻造台、织布机、制图台、切石机、砂轮 | 布尔值 | 开 |
| `quickRightClickShulkerBoxes` 打开潜影盒 | 潜影盒内容随物品读写；关闭界面时把内容写回原物品 | 布尔值 | 开 |
| `quickRightClickEnderChest` 打开末影箱 | 使用玩家自己的末影箱空间 | 布尔值 | 开 |
| `quickRightClickRequireSneak` 需要潜行 | 需要按住潜行键才触发，避免整理背包时误触 | 布尔值 | 关 |
| `dropConfirm` 丢弃确认 | 按一次丢弃键只显示确认提示，3 秒内再按一次才真正丢弃；覆盖 Q 与 Ctrl+Q | 布尔值 | 关 |
| `dropConfirmItems` 确认物品列表 | 只对列表内的物品启用确认；留空表示所有物品 | 字符串列表 | 空 |

#### 实用工具

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `copyItemId` 复制物体 ID | 总开关；开启后用「复制目标 ID 快捷键」对准方块/实体、或在容器界面里悬停物品复制 ID，聊天栏里的 ID 可点击再次复制 | 布尔值 | 关 |
| `copyTargetId` 复制目标 ID 快捷键 | 对准方块/实体或在容器界面悬停物品时按下，复制其 ID；需开启 `copyItemId` 总开关 | 快捷键 | 未绑定 |
| `copyItemIdFormat` 复制格式 | `plain` 纯 ID / `typed` 带类型前缀（物品/方块/实体）/ `counted` 附带数量 | 选项 | `plain` |
| `copyItemIdNamespace` 保留命名空间 | 复制时保留命名空间前缀（`minecraft:stone`）；关闭后只复制路径部分（`stone`） | 布尔值 | 开 |
| `copyItemIdCloseContainer` 复制后关闭界面 | 在容器界面复制物品 ID 后自动关闭该界面 | 布尔值 | 关 |
| `copyItemIdChatMessage` 聊天栏提示 | 复制成功后在聊天栏显示可点击复制的 ID；关闭后只写入剪贴板 | 布尔值 | 开 |
| `chatTextSelection` 聊天文字框选 | 在聊天界面按住左键拖动选择消息文字，Ctrl+C 复制 | 布尔值 | 关 |
| `chatSelectionColor` 框选高亮颜色 | `blue` / `green` / `orange` / `purple` / `gray` | 选项 | `blue` |
| `chatSelectionOpacity` 框选透明度 | 框选背景的透明度百分比（10 ~ 100）；越大越不透明 | 整数 | 33 |
| `chatSelectionAutoCopy` 松开自动复制 | 松开左键时自动复制选中的文字，不必再按 Ctrl+C | 布尔值 | 关 |
| `chatInputMemory` 聊天框内容记忆 | 关闭聊天框后保留已输入但未发送的内容，下次打开聊天框时自动恢复；发送消息后清空 | 布尔值 | 关 |
| `chatInputMemoryIgnoreCommands` 不记忆命令 | 开启后以 `/` 开头的命令草稿不再记忆，避免下次打开聊天框时误执行未发送的命令 | 布尔值 | 关 |
| `chatFadeProgressBar` 聊天框消失进度条 | 每条新聊天消息的底边绘制一条白色倒计时进度条：长度随剩余显示时间（原版 200 tick）缩短，透明度与消息文字同步；打开聊天界面时保持不透明、只缩短长度 | 布尔值 | 关 |

</details>

<details>
<summary><b>显示 — 世界内可视化与渲染叠加层（9 组 / 49 项：攻击范围、爆炸范围、横扫范围、屏障、基岩、轮廓、高亮、截图、区块中心）</b></summary>

#### 范围可视化

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `attackRangeDisplay` 攻击范围显示 | 以生物或玩家为中心显示半透明线框攻击范围（颜色可调），半径按原版攻击距离计算；每秒刷新 12 次，条目几何未变时复用已有顶点缓存 | 布尔值 | 关 |
| `attackRangeDisplayMode` 范围显示方式 | `flat` 脚下双圆环 / `sphere` 经纬线框球体 | 选项 | `flat` |
| `attackRangeRenderMode` 范围渲染方式 | `wireframe` 线框（无 OIT，性能更好）/ `filled` 半透明实心（观感更完整，可能掉帧）；filled 会额外叠加线条轮廓，避免 Vulkan/Vitrail 下 OIT 实心不可见 | 选项 | `wireframe` |
| `attackRangePalette` 范围颜色 | `blue` / `red` / `green` / `yellow` / `cyan` / `magenta` / `white`；只改 RGB，透明度仍按渲染方式固定；爆炸/横扫范围固定使用红色 | 选项 | `blue` |
| `attackRangeIncludeSelf` 显示自己的范围 | 是否显示本地玩家自己的攻击范围；关闭后只显示其他生物与玩家 | 布尔值 | 关 |
| `attackRangeHostileOnly` 仅显示敌对生物 | 只显示僵尸、骷髅等敌对生物；玩家与被动生物不再显示范围 | 布尔值 | 关 |
| `attackRangeIncludeInvisible` 显示隐身实体 | 默认不显示隐身实体；开启后隐身生物同样显示攻击范围 | 布尔值 | 关 |
| `attackRangeMaxDistance` 范围收集距离 | 收集攻击范围的最大距离（8 ~ 128 格）；爆炸范围也使用该值扫描 | 整数 | 48 |
| `attackRangeMaxTargets` 最大显示数量 | 单次最多显示的数量（8 ~ 256）；超出时保留最近的实体；爆炸范围也使用该上限 | 整数 | 64 |
| `attackRangeEntities` 攻击范围实体 | 需要显示范围的生物/玩家 ID 列表；为空时显示全部生物与玩家 | 字符串列表 | 空 |
| `explosionRangeDisplay` 爆炸范围可视化 | 红色标记 TNT、已激活的 TNT 矿车、苦力怕与末地水晶的爆炸半径；平面/球体与线框/实心模式沿用攻击范围，实心模式会额外叠加线条，避免 Vulkan/Vitrail 下 OIT 实心不可见 | 布尔值 | 关 |
| `sweepingView` 横扫范围可视化 | 手持剑时显示以脚底为中心的 3 格球体攻击范围；瞄准生物、船或矿车时附加目标扩展框（水平 +1 格、垂直 +0.25 格）；子项控制线框/面框/球体、载具目标与颜色；实心模式会额外叠加线条，避免 Vulkan 下实心不可见 | 布尔值 | 关 |
| `sweepingViewWireframe` 横扫目标线框 | 显示目标扩展框的线框 | 布尔值 | 开 |
| `sweepingViewWireframeColor` 横扫线框颜色 | 目标扩展框线框颜色，调色板与攻击范围一致 | 选项 | `red` |
| `sweepingViewFace` 横扫目标面框 | 用网格线填充目标扩展框形成半透明面；不依赖 OIT 或线框子开关，关闭线框时也能在 Vulkan 下显示 | 布尔值 | 开 |
| `sweepingViewFaceSolid` 横扫实心面框 | 开启后用实心四边形替代网格面框；依赖原版 OIT，Vulkan/Vitrail 下可能不可见 | 布尔值 | 关 |
| `sweepingViewFaceColor` 横扫面框颜色 | 目标扩展框面框颜色 | 选项 | `red` |
| `sweepingViewSphere` 横扫球体范围 | 显示以玩家脚底为中心的 3 格球体攻击范围 | 布尔值 | 开 |
| `sweepingViewSphereColor` 横扫球体颜色 | 3 格球体攻击范围颜色 | 选项 | `red` |
| `sweepingViewVehicles` 横扫载具目标 | 瞄准船或矿车时也显示目标扩展框；关闭后仅对生物与玩家生效 | 布尔值 | 开 |
| `betterScreenshot` 更好的 F2 截图 | 接管 F2：截图后在聊天框显示缩略图、时间、文件名与坐标；文件名包含时间与坐标，可点击打开截图文件夹，右侧 `[删除]` 可删除文件 | 布尔值 | 开 |
| `betterScreenshotF9` F9 高清截图 | 按 F9 临时把窗口与主渲染目标切到 3840×2160，按 4K 重新渲染后截图；1K 屏幕也能输出真实 4K 细节，截图后恢复原窗口 | 布尔值 | 关 |
| `betterScreenshotFormat` 截图输出格式 | F2 与 F9 统一使用 `png` / `jpg` / `tga` / `bmp` | 选项 | `png` |
| `chunkCenterIndicator` 区块中心指示 | 原版区块边界显示（F3+G）开启时，在所在区块中心补一条白色竖线；关闭区块边界时本项不生效 | 布尔值 | 关 |

#### 方块与实体渲染

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `customBlockOutline` 自定义方块轮廓 | 总开关：自定义准星方块轮廓的粗细与颜色；高对比度轮廓模式下保持原版轮廓 | 布尔值 | 关 |
| `blockOutlineWidth` 轮廓粗细度 | 线宽（0.5 ~ 8，滑块调节）；部分显卡或渲染后端会把线宽限制为 1 | 浮点 | 2.0 |
| `blockOutlineAnimationSpeed` 动画速度 | 彩虹/呼吸模式的动画速度百分比（10 ~ 400，数值越大越快） | 整数 | 100 |
| `blockOutlinePalette` 轮廓颜色调色板 | `white` / `red` / `green` / `blue` / `yellow` / `cyan` / `magenta` / `orange`，仅「调色板」模式使用 | 选项 | `white` |
| `blockOutlineColorMode` 轮廓彩色模式 | `palette` 固定使用所选颜色 / `rainbow` 随时间循环变色 / `pulse` 亮度周期性变化 | 选项 | `palette` |
| `entityHighlight` 自定义实体高亮 | 为预设列表中的实体显示发光描边；需同时开启一个预设；描边样式与颜色可调 | 布尔值 | 关 |
| `entityHighlightStyle` 描边样式 | `outline` 常亮描边 / `blink` 周期闪烁（一半时间显示、一半时间隐藏） | 选项 | `outline` |
| `entityHighlightBlinkPeriod` 闪烁周期 | 闪烁样式的周期（tick，10 ~ 200）；仅在 `blink` 样式下生效 | 整数 | 60 |
| `entityHighlightColor` 描边颜色 | `vanilla` 原版队伍色（无队伍为白色）/ `white` / `red` / `green` / `blue` / `yellow` / `cyan` / `magenta` | 选项 | `vanilla` |
| `highlightPreset1` / `highlightPreset2` / `highlightPreset3` 高亮预设 | 三个预设互斥，开启一个会自动关闭其他 | 布尔值 | 关 |
| `highlightPreset1Entities` / `highlightPreset2Entities` / `highlightPreset3Entities` 预设实体 | 各预设需要高亮的实体 ID 列表，支持 `minecraft:item` 等掉落物实体 | 字符串列表 | 空 |
| `highlightSingleLayerBedrock` 高亮单层基岩 | 在扫描半径内（默认 24 格）的单层基岩朝向空气的顶面/底面显示凸出 1 像素的实心小方块，便于寻找下界顶棚的可击穿薄弱点；每秒刷新 4 次，扫描结果未变时复用已有顶点缓存 | 布尔值 | 关 |
| `highlightSingleLayerBedrockMaxMarkers` 基岩标记最大数量 | 单次最多显示的标记数（1 ~ 256）；超出时保留最近的 | 整数 | 32 |
| `highlightSingleLayerBedrockScanRadius` 基岩扫描半径 | 扫描单层基岩的半径（8 ~ 32 格）；半径越小，扫描越快、可见范围越近 | 整数 | 24 |
| `highlightSingleLayerBedrockColor` 基岩标记颜色 | `gold` 金色 / `white` / `red` / `green` / `blue` / `cyan`；只影响小方块颜色，切换后顶点缓存自动重建 | 选项 | `gold` |
| `barrierVisibility` 屏障可见 | 周围屏障方块显示与手持屏障物品时相同的原版标记粒子，粒子固定在方块中心并逐方块冷却，不会偏移或重叠；扫描半径与标记间隔可调 | 布尔值 | 关 |
| `barrierVisibilityMaxMarkers` 屏障标记最大数量 | 单次扫描最多保留的标记数（16 ~ 256）；超出时保留离玩家最近的 | 整数 | 256 |
| `barrierVisibilityScanRadius` 屏障扫描半径 | 扫描屏障方块的半径（8 ~ 32 格）；半径越小，扫描越快、可见范围越近 | 整数 | 24 |
| `barrierVisibilityParticleInterval` 屏障标记间隔 | 同一方块两次标记粒子之间的间隔（20 ~ 200 tick，20 tick = 1 秒）；越大越省性能、闪烁越慢 | 整数 | 60 |

</details>

<details>
<summary><b>修改 — 改写原版规则，更接近基岩版（8 组 / 100 余项：界面与进度、移动挖掘、视角显示、实体展示、世界创建提示、覆盖层、提示文本、世界生成）</b></summary>

#### 界面与进度

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `betterAdvancements` 更好的进度 | 进度窗口随屏幕放大，标签过多时自动分页，可固定标签、记住上次标签；子项控制分页/固定/记忆/背景压暗/复制 ID/标签间距 | 布尔值 | 关 |
| `betterAdvancementsPagination` 标签分页 | 标签数量超出窗口宽度时显示前后翻页按钮，不再丢弃超出原版上限（26 个）的标签 | 布尔值 | 开 |
| `betterAdvancementsPinTabs` 固定标签 | 窗口右上角星标按钮可把当前标签固定到右侧竖栏，方便在多个进度树之间切换 | 布尔值 | 开 |
| `betterAdvancementsRememberTab` 记住上次标签 | 重新打开进度界面自动回到上次选中的标签 | 布尔值 | 开 |
| `betterAdvancementsDisableFade` 禁用背景压暗 | 鼠标悬停在进度上时不再压暗进度树背景 | 布尔值 | 关 |
| `betterAdvancementsCopyId` Ctrl+C 复制进度 ID | 在进度界面按住 Ctrl 按 C 复制悬停进度的 ID，并在快捷栏上方提示 | 布尔值 | 开 |
| `betterAdvancementsTabSpacing` 标签间距 | 顶部进度标签之间的水平间距（0 ~ 8 像素），0 为原版紧凑排列 | 整数 | 0 |
| `betterAdvancementsPinnedTabSpacing` 固定标签间距 | 右侧固定标签之间的垂直间距（0 ~ 8 像素） | 整数 | 0 |
| `hideChatSigning` 隐藏聊天签名 | 隐藏聊天消息旁的签名颜色条与标记，以及进入未验证聊天服务器时的提示；仅客户端视觉隐藏，不移除签名，也不阻止他人举报 | 布尔值 | 关 |
| `customWindowIcon` 自定义游戏图标 | 使用指定路径的 PNG 图片作为游戏窗口与任务栏图标；关闭后恢复原版图标 | 布尔值 | 关 |
| `customWindowIconPath` 图标文件路径 | 16 ~ 64 像素 PNG 文件的路径，支持绝对路径或相对游戏目录的路径；修改后自动重新加载 | 字符串 | 空 |

#### 移动与挖掘

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableUseItemSlowdown` 禁用使用减速 | 进食、拉弓、举盾时不再降低移速 | 布尔值 | 关 |
| `useItemSlowdownExceptions` 使用减速例外物品 | 这些物品仍保留原版的使用减速（物品 ID 列表） | 字符串列表 | 空 |
| `useItemSlowdownKeepPercent` 保留使用减速 | 保留原版使用减速的百分比（0 ~ 100）；0 = 完全取消（默认），100 = 与原版相同 | 整数 | 0 |
| `disableSprintFov` 禁用疾跑 FOV 变化 | 疾跑时视野不再放大；速度药水、飞行、拉弓等其它视野效果保持原版 | 布尔值 | 关 |
| `sprintFovIntensity` 保留疾跑视野 | 保留原版疾跑视野强度的百分比（0 ~ 100）；0 = 完全禁用（默认），100 = 等同原版 | 整数 | 0 |
| `keepMiningProgressOnItemSwitch` 切换工具保留挖掘进度 | 像基岩版一样：挖掘中切换物品不再重置进度，继续挖同一个方块 | 布尔值 | 关 |
| `keepMiningProgressMaxDistance` 保留进度距离 | 只在玩家与目标方块距离不超过该格数时保留挖掘进度（0 ~ 16）；0 = 不限制（默认），超出后按原版重置 | 整数 | 0 |
| `climbablePassNoSlowdown` 经过可攀爬方块不减速 | 走过梯子、藤蔓等可攀爬方块时，蹭到边缘也不再被原版夹紧水平速度；攀爬上升/下滑保持原版 | 布尔值 | 关 |
| `climbablePassNoSlowdownSneakOnly` 仅潜行时生效 | 只在按住潜行键时跳过攀爬方块的水平速度夹紧 | 布尔值 | 关 |
| `elytraWaterFlight` 鞘翅水中飞行 | 滑翔时不再被水流移动逻辑接管，潜入水中也能继续保持滑翔姿态与速度 | 布尔值 | 关 |
| `elytraWaterFlightOnlyWater` 仅水中生效 | 只在水中保持滑翔，岩浆里仍按原版下沉 | 布尔值 | 关 |
| `removeSpeedLimit` 取消速度上限 | 关闭服务端 moved too quickly 速度检查，避免高速移动被拉回；碰撞与位置校正保留；仅在单人/局域网主机生效 | 布尔值 | 关 |
| `disableFarmlandTrampling` 禁用耕地踩踏 | 从高处落在耕地上不再把它踩回泥土；仅在单人/局域网生效 | 布尔值 | 关 |
| `farmlandTramplingPlayerOnly` 仅玩家生效 | 只有玩家不会踩坏耕地，动物与村民仍会踩坏 | 布尔值 | 关 |
| `totemFromInventory` 不死图腾背包生效 | 图腾放在快捷栏或背包（主手/副手之外）也能触发不死保护；手部已有图腾时仍优先消耗手中的；仅在单人/局域网生效 | 布尔值 | 关 |
| `disableCobwebSlowdown` 禁用蛛网减速 | 蜘蛛网不再粘住玩家并限制移动与跳跃；仅在单人/局域网生效 | 布尔值 | 关 |
| `cobwebSlowdownAllEntities` 作用于全部实体 | 生物、掉落物等也不再被蛛网减速；默认只解除玩家 | 布尔值 | 关 |
| `disableFluidPush` 禁用液体推动 | 水流与岩浆流不再施加水平推力，浮力与流体高度判定保持原版；仅在单人/局域网生效 | 布尔值 | 关 |
| `disableWaterPush` 禁用水流推力 | 仅取消水流造成的水平推力 | 布尔值 | 开 |
| `disableLavaPush` 禁用岩浆推力 | 仅取消岩浆流造成的水平推力 | 布尔值 | 开 |
| `disableSlimeBlockSlowdown` 禁用粘液块减速 | 站在粘液块上不再被乘算降低水平速度；仅在单人/局域网生效 | 布尔值 | 关 |
| `disableSlimeBlockBounce` 禁用粘液块弹跳 | 同时把粘液块弹性系数改为 0，落地不再弹起 | 布尔值 | 开 |
| `disableHoneyBlockSlowdown` 禁用蜂蜜块减速 | 蜂蜜块不再降低移动速度；灵魂沙等其它减速方块保持原版；仅在单人/局域网生效 | 布尔值 | 关 |
| `disableHoneyBlockStick` 禁用蜂蜜块黏着 | 贴墙时不再被粘住缓降，可以正常下落 | 布尔值 | 开 |

#### 视角与显示

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableHurtShake` 禁用受伤抖动 | 受伤时不再产生视角抖动；行走时的镜头晃动不受影响 | 布尔值 | 关 |
| `hurtShakeKeepDeathTilt` 保留死亡镜头倾斜 | 开启后关闭受伤抖动时仍保留死亡倒下瞬间的镜头倾斜（默认关闭，连死亡倾斜一起取消） | 布尔值 | 关 |
| `disableHurtTint` 禁用受伤变红 | 受伤时不再叠加红色 overlay；死亡变红与受伤闪白保持原版 | 布尔值 | 关 |
| `hurtTintKeepWhiteFlash` 保留受伤闪白 | 关闭受伤变红时仍保留受伤瞬间的白色闪光；关闭后受伤完全不叠加 overlay | 布尔值 | 开 |
| `thirdPersonPlayerName` 第三人称显示名字 | 第三人称（背后/正面）也显示玩家自己的名字；第一人称、隐身、旁观和隐藏 HUD 时保持原版 | 布尔值 | 关 |
| `thirdPersonPlayerNameSneakOnly` 仅潜行时显示 | 只在按住潜行键时显示自己的名字 | 布尔值 | 关 |
| `disableSneakNameHide` 禁用潜行隐藏名字 | 潜行时不再隐藏名字标签：原版超过 32 格就看不到，开启后潜行也能一直看到；队伍可见性、隐身等规则保持原版 | 布尔值 | 关 |
| `betterInvisibility` 更好的隐身 | 隐身时不再显示药水粒子，身上的装备与第三人称手持物品也一并隐藏 | 布尔值 | 关 |
| `betterInvisibilityEffectParticles` 隐藏药水粒子 | 隐身时不再显示药水效果粒子 | 布尔值 | 开 |
| `betterInvisibilityLayers` 隐藏装备与手持物 | 第三人称下同时隐藏盔甲、披风与手持物品 | 布尔值 | 开 |
| `betterInvisibilityLlamaCarpet` 隐藏羊驼地毯 | 羊驼隐身后，身上的地毯与装饰也一并隐藏 | 布尔值 | 开 |
| `nightVisionFadeOut` 夜视淡出 | 夜视快结束时不再闪烁，而是平滑淡出恢复正常 | 布尔值 | 关 |
| `nightVisionFadeOutDuration` 淡出时长 | 平滑淡出的时长（tick，20 tick = 1 秒），范围 20 ~ 400 | 整数 | 200 |
| `nightVisionFadeOutCurve` 淡出曲线 | `smooth` 平滑（两端更慢）/ `linear` 线性 | 选项 | `smooth` |
| `connectedBlockOutline` 连接方块轮廓 | 准星对准门、床、双箱、双层植物等连接方块时，显示整体轮廓（类似基岩版） | 布尔值 | 关 |
| `connectedBlockOutlineDoorFullHeight` 门两格高度 | 开启时门按 2 格视觉高度描边；关闭后门保持原版轮廓 | 布尔值 | 开 |

#### 实体与展示框

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `armorStandArms` 盔甲架手臂 | 所有盔甲架默认显示手臂；单人/局域网下可直接把物品放上双手；远程服务器只影响本地渲染 | 布尔值 | 关 |
| `hideItemFrames` 隐藏物品展示框 | 展示框里装有物品时只渲染物品、不渲染外框；空展示框保持原样；只影响本地渲染 | 布尔值 | 关 |

#### 世界创建提示

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableExperimentalWarning` 禁用实验性功能提示 | 创建含实验性功能的世界、或应用实验性数据包时不再弹出确认提示；世界仍会按原版标记为实验性；子项可分别开关 | 布尔值 | 关 |
| `experimentalWarningWorldCreate` 创建世界提示 | 创建含实验性功能的世界时跳过警告确认，直接开始创建 | 布尔值 | 开 |
| `experimentalWarningDataPacks` 数据包提示 | 应用实验性数据包时跳过确认界面，等同于直接点击「继续」 | 布尔值 | 开 |

#### 屏幕覆盖层与加载界面

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableOverlayEffects` 禁用屏幕覆盖层 | 总开关：关闭细雪、岩浆、水下等全屏覆盖效果；下面的子开关分别控制 | 布尔值 | 关 |
| `disablePowderSnowOverlay` 禁用细雪覆盖层 | 陷入细雪时不再显示冰霜边框覆盖层 | 布尔值 | 开 |
| `disableLavaOverlay` 禁用岩浆覆盖层 | 在岩浆中不再显示橙红色全屏覆盖层 | 布尔值 | 开 |
| `disableWaterOverlay` 禁用水下覆盖层 | 水下不再显示蓝色水面覆盖层 | 布尔值 | 开 |
| `disableWaterFog` 禁用水下雾 | 水下不再有雾与蓝色滤镜（26.3 的雾是独立系统） | 布尔值 | 开 |
| `disableLavaFog` 禁用岩浆雾 | 岩浆里不再有橙色雾 | 布尔值 | 开 |
| `disablePowderSnowFog` 禁用细雪雾 | 陷入细雪时不再有白雾 | 布尔值 | 开 |
| `forceCloseLoadingScreen` 强制关闭加载界面 | 加载世界时立刻关闭“正在加载地形”界面，提前看到世界；世界仍在后台继续加载 | 布尔值 | 关 |
| `forceCloseLoadingScreenInstant` 完全跳过加载界面 | 直接关闭加载界面（完全跳过，可能短暂掉入世界）；仅在上一项开启时生效 | 布尔值 | 关 |
| `forceCloseLoadingScreenKeepFirst` 保留加载文字进度 | 不跳过加载界面的文字与进度（背景和世界仍正常加载） | 布尔值 | 关 |
| `forceCloseLoadingScreenSkipResourcePackFade` 跳过资源包淡入淡出 | 去掉资源包加载屏幕的淡入淡出并立即移除覆盖层（资源加载完成后才进入游戏） | 布尔值 | 开 |
| `disableNetherPortalEffect` 禁用地狱门特效 | 进入下界传送门时不再显示紫色覆盖层、镜头旋转与传送门音效；关界面与传送逻辑不变 | 布尔值 | 关 |
| `disableNetherPortalOverlay` 禁用紫色覆盖层 | 站在传送门里时不再显示紫色半透明覆盖层 | 布尔值 | 开 |
| `disableNetherPortalCamera` 禁用镜头旋转 | 站在传送门里时镜头不再绕视角旋转 | 布尔值 | 开 |
| `disableNetherPortalSound` 禁用传送门音效 | 不再播放传送门的循环音效与传送瞬间的音效 | 布尔值 | 开 |
| `disableResourcePackCompatibilityCheck` 禁用资源包兼容性检查 | 资源包界面不再显示不兼容标记，也不会在启用不兼容资源包时弹出确认提示；仅影响资源包，不影响数据包 | 布尔值 | 关 |
| `resourcePackCompatibilityAlsoDataPacks` 同时忽略数据包 | 数据包界面也不显示不兼容标记、不弹确认框 | 布尔值 | 关 |

#### 提示文本

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `enchantmentLevelArabic` 附魔等级阿拉伯数字 | 附魔等级使用 1、2、3... 代替 I、II、III... | 布尔值 | 关 |
| `enchantmentLevelArabicStyle` 数字样式 | `plain` 锋利 3 / `parentheses` 锋利 (3) | 选项 | `plain` |
| `enchantmentDescriptions` 附魔描述 | 在物品提示中为每个附魔追加一行描述（附魔书同样支持） | 布尔值 | 关 |
| `enchantmentDescriptionsSneakOnly` 仅潜行时显示 | 只在按住潜行键时追加附魔描述，避免提示过长 | 布尔值 | 关 |

#### 世界交互与世界生成

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `disableUnderwaterMiningPenalty` 禁用水下挖掘惩罚 | 在水下挖掘时不再按水下挖掘速度属性减速 | 布尔值 | 关 |
| `underwaterMiningPenaltyKeep` 保留水下惩罚 | 保留原版水下挖掘减速的百分比（0 ~ 100）；0 = 完全去掉（默认），100 = 与原版相同 | 整数 | 0 |
| `disableAirMiningPenalty` 禁用空中挖掘惩罚 | 离地挖掘时不再额外减速 | 布尔值 | 关 |
| `airMiningPenaltyKeep` 保留空中惩罚 | 保留原版空中挖掘减速的百分比（0 ~ 100）；0 = 完全去掉（默认），100 = 与原版相同 | 整数 | 0 |
| `disableCurseEnchantmentRestriction` 移除诅咒附魔 | 砂轮可以去除诅咒附魔（原版会保留诅咒）；仅在单人/局域网生效 | 布尔值 | 关 |
| `curseRemovalScope` 诅咒移除范围 | `all` 全部诅咒 / `binding_only` 仅绑定诅咒 / `vanishing_only` 仅消失诅咒；其它模组的诅咒仅在 `all` 下移除 | 选项 | `all` |
| `removeEnchantmentConflicts` 移除附魔冲突 | 铁砧合并装备时忽略附魔互斥限制，可以把锋利与亡灵杀手等互斥附魔合到同一件装备上；仅在单人/局域网生效 | 布尔值 | 关 |
| `removeEnchantmentConflictsSameGroupOnly` 仅同类附魔 | 只解除同一互斥组（同一 `exclusive_set` 标签）内部的限制；关闭时解除全部互斥限制 | 布尔值 | 关 |
| `disableTooExpensive` 禁用过于昂贵 | 铁砧花费达到 40 级时不再提示“过于昂贵”，结果可以直接取出，等级不足也能取走（等级最低到 0）；仅在单人/局域网生效 | 布尔值 | 关 |
| `tooExpensiveMaxLevelCost` 经验花费上限 | 可承受的最大经验等级花费（0 ~ 1000）；0 = 不限制（默认），超过后按下一项决定阻止还是仅提示 | 整数 | 0 |
| `tooExpensiveWarnOnly` 超限仅提示 | 花费超过费用上限时仍显示“过于昂贵”提示，但允许取出；关闭时超过上限按原版阻止 | 布尔值 | 关 |
| `visibleTraders` 显示锁定交易 | 在村民交易界面显示未来等级（锁定）的交易，村民升级时按顺序解锁；仅在单人/局域网生效 | 布尔值 | 关 |
| `visibleTradersOnlyLocked` 仅显示锁定交易 | 交易界面只显示尚未解锁的未来等级交易，隐藏当前等级已解锁的交易；这些行均为锁定状态，不能点击 | 布尔值 | 关 |
| `keepLastTorch` 不使用最后一根火把 | 放置火把时如果背包里只剩最后一根则不会放置；仅在单人/局域网生效 | 布尔值 | 关 |
| `keepLastTorchMinCount` 保留火把数量 | 剩余火把不超过该数量时阻止放置（1 ~ 8，默认 1 = 只保留最后一根） | 整数 | 1 |
| `disableSmithingTemplateConsumption` 禁用锻造模板消耗 | 锻造台取出成品时保留锻造模板，不再消耗；仅在单人/局域网生效 | 布尔值 | 关 |
| `smithingTemplateKeepChance` 保留概率 | 锻造模板被保留的概率（0 ~ 100）；100 = 必定保留（默认），0 = 完全按原版消耗 | 整数 | 100 |
| `itemDespawnTime` 掉落物消失时间 | 滑块调节 60 ~ 36000 tick（20 tick = 1 秒）；仅在单人/局域网生效 | 整数 | 6000（原版 5 分钟） |
| `itemDespawnPlayerDroppedOnly` 仅玩家丢弃物生效 | 只有玩家主动丢弃的掉落物按上面的时间消失，怪物与方块掉落保持原版 | 布尔值 | 关 |
| `itemDespawnTimeXpOrbs` 经验球消失时间 | 经验球的消失时间（tick），范围同上 | 整数 | 6000 |
| `disableFlatWorldSlimeSpawn` 禁用超平坦史莱姆生成 | 超平坦世界不再生成史莱姆；仅在单人/局域网生效 | 布尔值 | 关 |
| `flatWorldSlimeSpawnAllWorlds` 对所有世界生效 | 所有世界类型都不生成史莱姆，不再只限超平坦 | 布尔值 | 关 |
| `disableBedTooFar` 禁用床距离限制 | 离床再远右键也能直接入睡，不再提示“离床太远”；仅在单人/局域网生效 | 布尔值 | 关 |
| `bedTooFarMaxDistance` 允许入睡距离 | 允许入睡的最大距离（0 ~ 128 格）；0 = 不限制（默认） | 整数 | 0 |
| `ironGolemRideMinecart` 铁傀儡进矿车 | 空矿车碰到铁傀儡时，让铁傀儡坐进矿车；只限普通矿车，漏斗/运输/动力/TNT 等特种矿车不拉铁傀儡；仅在单人/局域网生效 | 布尔值 | 关 |
| `ironGolemRideMinecartMaxDistance` 玩家附近距离 | 只在玩家附近自动上车（0 ~ 64 格）；0 = 不限制（默认） | 整数 | 0 |
| `signOpenContainer` 穿牌打开容器 | 准星对准告示牌时直接打开它背后（或相邻）的容器；潜行右键默认仍按原版编辑告示牌 | 布尔值 | 关 |
| `signOpenContainerSneakBypass` 潜行时保持原版 | 开启时潜行仍编辑告示牌；关闭后潜行也能穿牌打开容器 | 布尔值 | 开 |
| `snowGolemBiomeImmunity` 雪傀儡不受群系影响 | 温暖或干燥群系不再让雪傀儡因群系属性融化；雨、水和熔岩伤害仍按原版；仅在单人/局域网生效 | 布尔值 | 关 |
| `snowGolemBiomeImmunityOverworldOnly` 仅主世界生效 | 只在主世界免疫群系融化；下界与末地仍按原版群系属性融化 | 布尔值 | 关 |
| `wartBlockDecomposition` 疣块分解 | 合成格里单独放一个下界疣块，结果槽直接产出 9 个下界疣（原版已有配方时优先走原版）；仅在单人/局域网生效 | 布尔值 | 关 |
| `wartBlockDecompositionOutput` 疣块产出数量 | 分解一个下界疣块得到的下界疣数量（1 ~ 9） | 整数 | 9 |

</details>

<details>
<summary><b>修复 — 修正原版 Bug（31 项：MC-577、MC-247973、MC-305835 等）</b></summary>

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `fixBoatStuckOnDirtPath` 草径卡船修复 | 只给船增加 1/16 格上台阶高度，消除土径与完整方块之间的卡顿；不改变方块碰撞，不影响放置、摩擦和滑行速度 | 布尔值 | 关 |
| `fixBoatStuckOnDirtPathPlayerOnly` 仅玩家船只 | 只对载有玩家的船生效；空船与动物船保持原版 | 布尔值 | 关 |
| `parrotDanceOnShoulder` 鹦鹉肩膀跳舞 | 像基岩版一样：附近唱片机播放音乐时，站在玩家肩膀上的鹦鹉也会跳舞，而不是保持静止 | 布尔值 | 关 |
| `parrotDanceOnShoulderLocalPlayerOnly` 仅本地玩家 | 只让本地玩家肩膀上的鹦鹉跳舞，其它玩家保持原版姿态 | 布尔值 | 关 |
| `betterBold` 更好的粗体 | 把粗体字形偏移从原版 1.0 像素改为配置百分比（默认 0.5 像素，与 MCPatcher/OptiFine 一致），粗体文字更清晰、不再粘连 | 布尔值 | 关 |
| `betterBoldOffsetPercent` 粗体偏移 | 粗体偏移百分比（10 ~ 100）；50 = 0.5 像素（默认），100 = 原版 1.0 像素 | 整数 | 50 |
| `frostWalkerJumpFix` 冰霜行者跳跃修复 | 跳跃途中也继续在脚下结冰：原版只在站地帧结冰，跳着过水面会踩空落水；仅放宽玩家的冰霜行者判定，卡在载具上或其它附魔效果保持原版；仅在单人/局域网生效 | 布尔值 | 关 |
| `frostWalkerJumpFixJumpOnly` 仅跳跃上升 | 只在向上跳跃时结冰；下落过程仍按原版不结冰 | 布尔值 | 关 |
| `fixMouseButtonInventory` 物品栏鼠标按键修复 | 修复 MC-577：物品栏里绑定到鼠标键的「关闭背包」与「丢弃物品」不再被吞掉；Ctrl+丢弃键仍按原版丢整组 | 布尔值 | 关 |
| `chatLagFix` 聊天卡顿修复 | 修复 MC-247973：收到聊天消息时改为后台拉取屏蔽名单，避免主线程同步网络请求卡顿；首次消息先按未屏蔽处理，后台完成后生效 | 布尔值 | 关 |
| `experienceBarHighLevelFix` 经验条高等级修复 | 修复 MC-79545：等级极高时经验需求整数溢出为负数，经验条不再整条消失；正常等级与原版完全一致 | 布尔值 | 关 |
| `bookGuiVerticalCenterFix` 书本界面居中修复 | 修复 MC-61489：书本查看与书与笔编辑界面向下移动，整体更居中；底部按钮不再贴着屏幕边缘 | 布尔值 | 关 |
| `potionTooltipCategoryFix` 药水效果分类修复 | 修复 MC-84661 / MC-170462：药水类物品提示中的「发光」与「不祥之兆」按有害效果显示为红色，不再误显示为中性效果的蓝色；其它效果不变 | 布尔值 | 关 |
| `shieldBlockHurtSoundFix` 举盾受伤音效修复 | 修复 MC-105068：完全格挡攻击时不再播放玩家受伤音效，避免叠在盾牌格挡/禁用音效上；部分格挡仍有伤害时保持原版；仅在单人/局域网生效 | 布尔值 | 关 |
| `spectatorOverlayFix` 旁观者覆盖层修复 | 修复 MC-217716：进入旁观者模式后不再渲染反胃与下界传送门覆盖层；其它游戏模式保持原版 | 布尔值 | 关 |
| `cooldownIndicatorFix` 冷却指示器闪烁修复 | 修复 MC-268420：高攻速下切换物品时，准星旁攻击冷却进度不再闪一帧；关闭开关时保持原版 | 布尔值 | 关 |
| `fishingLineThirdPersonFix` 第三人称鱼线修复 | 修复 MC-4490：潜行时第三人称鱼线仍连接到鱼竿，不再悬在鱼竿上方；未潜行与第一人称保持原版 | 布尔值 | 关 |
| `respawnHotbarFix` 重生快捷栏修复 | 修复 MC-143474：重生后保留死亡前的快捷栏选中槽位，不再重置到第 1 格；关闭开关时保持原版 | 布尔值 | 关 |
| `sneakAnimationFix` 潜行动画重复修复 | 修复 MC-159163：快速按潜行键时只播放一次潜行动画；只忽略本地玩家的 POSE 同步，其它实体保持原版 | 布尔值 | 关 |
| `skeletonAimingFix` 骷髅瞄准修复 | 修复 MC-121706：骷髅与幻术师射箭时视线随目标上下；仅在单人/局域网生效 | 布尔值 | 关 |
| `saplingGrowthFix` 2×2 树苗生长修复 | 修复 MC-8187：2×2 云杉/丛林树苗在北/西侧有相邻方块时也能生长；仅在单人/局域网生效 | 布尔值 | 关 |
| `bedFallDamageFix` 床边落地伤害修复 | 修复 MC-19830：从高处落入床中时，落地伤害不再被推迟到离床后才结算，上床瞬间立即按当前下落高度结算；仅在单人/局域网生效 | 布尔值 | 关 |
| `spearFixes` 长矛相关修复 | 长矛（蓄力戳刺）相关原版问题的总开关；关闭时下面 8 个子项全部不生效；修复判定发生在服务端，仅在单人/局域网生效 | 布尔值 | 关 |
| `spearFixMc305835` 生物刺击击退 (MC-305835) | 生物用长矛的普通刺击命中时也会造成击退，与原版其它近战武器一致；仅在单人/局域网生效 | 布尔值 | 开 |
| `spearFixMc308786` 蓄力击退角度 (MC-308786) | 蓄力攻击的速度与击退改按水平速度判定，低头攻击时不再丢失击退 | 布尔值 | 开 |
| `spearFixMc303451` 蓄力伤害缺失 (MC-303451) | 击退条件改用相对速度参与判定（伤害条件仍看自身速度），相对速度不足时不再只击退不伤害 | 布尔值 | 开 |
| `spearFixMc310858` 未命中不进入冷却 (MC-310858) | 蓄力碰到目标但速度不足、没有造成伤害时，不再触发 0.5 秒的目标接触冷却 | 布尔值 | 开 |
| `spearFixMc302732` 副手不套用主手附魔 (MC-302732) | 副手长矛的蓄力攻击只读取长矛自身的附魔，不再套用主手物品的附魔 | 布尔值 | 开 |
| `spearFixMc304406` 副手附魔生效 (MC-304406) | 副手长矛的附魔正常生效，例如击退附魔会作用在蓄力攻击上 | 布尔值 | 开 |
| `spearFixMc308270` 副手伤害附魔 (MC-308270) | 副手长矛的锋利、亡灵杀手与节肢杀手会正常增加蓄力攻击伤害 | 布尔值 | 开 |
| `spearFixMc308272` 副手火焰与抢夺 (MC-308272) | 副手长矛的火焰附加与抢夺效果会正常作用于蓄力攻击 | 布尔值 | 开 |

</details>

<details>
<summary><b>优化 — MiniHUD 掉帧与告示牌文本优化（3 项）</b></summary>

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `miniHudFpsOptimization` MiniHUD 掉帧优化 | 把 MiniHUD 的 HUD 行刷新间隔放宽到 150ms，减少重复重算造成的掉帧；仍保持在渲染线程执行，避免并发访问；需要安装 MiniHUD | 布尔值 | 关 |
| `miniHudRefreshIntervalMs` 刷新间隔 | HUD 行刷新间隔（50 ~ 1000 毫秒）；越大越省性能、数字刷新越慢 | 整数 | 150 |
| `signTextOptimization` 告示牌文本优化 | 缓存原始/过滤文本状态并跳过客户端告示牌空 tick；开启后告示牌渲染不显示过滤文本 | 布尔值 | 关 |

</details>

<details>
<summary><b>通用 — 语言、快捷键与列表行为（26 项）</b></summary>

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `language` 界面语言 | 支持 `zh_cn` / `en_us` / `ru_ru` / `fr_fr`；关闭「跟随系统语言」后生效 | 选项 | `zh_cn` |
| `followSystemLanguage` 跟随系统语言 | 使用游戏当前语言，忽略上面的语言选择 | 布尔值 | 开 |
| `openConfigGui` 打开配置界面 | 打开或关闭本模组配置界面的快捷键；任意界面可用 | 快捷键 | G + C |
| `rememberSelectedTab` 标签记忆 | 记住上次选择的分类标签（全部/通用/功能/显示/修改/修复/优化/测试/收藏），下次打开配置界面直接停在该标签 | 布尔值 | 关 |
| `rememberSelectedTabFallback` 默认标签 | 关闭标签记忆或首次打开时停在的标签（全部/通用/功能/显示/修改/修复/优化/测试/收藏） | 选项 | 全部 |
| `favoriteSystem` 收藏系统 | 收藏系统总开关（父项）：关闭后隐藏配置行与存档列表的收藏星，收藏标签不再显示内容；收藏数据保留 | 布尔值 | 开 |
| `favoriteStarStyle` 收藏星样式 | 子设置：`star` 自绘星形 / `nether_star` 原版下界之星；同时作用于配置行右侧与存档列表左侧的收藏星 | 选项 | `star` |
| `favoriteIncludeChildren` 收藏包含子项 | 子设置：收藏父项时，它的全部子项也出现在收藏标签中；关闭后只显示手动收藏的每一项 | 布尔值 | 开 |
| `scrollListMemory` 滚动列表记忆 | 记住列表滚到了哪里，关闭界面再打开回到上次位置 | 布尔值 | 开 |
| `scrollListMemoryPersist` 跨重启记忆 | 把滚动位置写入 `config/tweakify-mod-scroll-memory.json`，重启游戏后仍然生效 | 布尔值 | 关 |
| `malilibScrollMemory` MaLiLib 界面 | 记忆 MaLiLib 配置界面（含本模组配置界面）的滚动位置 | 布尔值 | 开 |
| `modMenuScrollMemory` Mod Menu 列表 | 记忆 Mod Menu 模组列表的滚动位置；未安装 Mod Menu 时不生效 | 布尔值 | 开 |
| `searchHighlight` 搜索关键词高亮 | 搜索命中的部分改用金色显示（本地化名称与英文配置键都高亮）；多个关键词用 `\|` 分隔 | 布尔值 | 开 |
| `searchHighlightColor` 搜索高亮颜色 | 搜索命中片段的颜色：`gold` / `yellow` / `green` / `aqua` / `red` / `white` | 选项 | `gold` |
| `smoothScroll` 平滑滚动与拖动 | 为配置界面列表、创造物品栏、聊天栏、Mod Menu 列表与原版进度界面启用平滑滚动；配置界面还支持按住左键拖动列表（从名称/空白处起拖、移动几像素生效，不影响按钮与滑块）；创造栏与物品分组兼容 | 布尔值 | 开 |
| `smoothScrollConfigList` 配置界面列表 | 平滑滚动 + 左键拖动只作用于 MaLiLib 配置界面 | 布尔值 | 开 |
| `smoothScrollCreative` 创造物品栏 | 创造栏滚动使用缓动过渡，并按像素平滑（自绘槽位底纹随内容上移，不再按整行跳） | 布尔值 | 开 |
| `smoothScrollChat` 聊天栏 | 聊天历史滚动分摊到多帧；新消息的自动上滚仍即时生效 | 布尔值 | 开 |
| `smoothScrollModMenu` Mod Menu 列表 | Mod Menu 模组列表平滑滚动；未安装 Mod Menu 时不生效 | 布尔值 | 开 |
| `smoothScrollAdvancements` 进度界面 | 进度（成就）界面的进度树滚动使用平滑动画；原版进度界面与「更好的进度」界面都生效 | 布尔值 | 开 |
| `smoothScrollDuration` 平滑时长 | 滚动动画收敛时长（30 ~ 400 毫秒）：越大越柔和、越小越跟手 | 整数 | 60 |
| `logCleanup` 日志清理 | 启动时把 logs 目录里的日期编号归档清理到保留数量；latest.log 与未知文件永不删除 | 布尔值 | 关 |
| `logCleanupKeepCount` 保留日志数量 | 保留最新的日志归档数量（0 ~ 100）；0 表示删除全部归档 | 整数 | 10 |
| `logCleanupOnStartup` 启动时清理 | 在游戏启动时自动执行一次清理；下方「立刻清理」按钮始终可手动触发 | 布尔值 | 开 |
| `startupTime` 启动时间 | 加载完成后在快捷栏上方显示本次游戏启动耗时（JVM uptime），每次启动只显示一次 | 布尔值 | 开 |
| `startupTimeLog` 记录到日志 | 同时把启动耗时写入模组日志，便于排查启动变慢 | 布尔值 | 开 |

</details>

> 配置界面还有一个「测试」标签，包含下面几个调试辅助项，以及不是配置项的「档案编辑器」（保存、加载、删除整套配置档案）。

> 「收藏」标签显示手动收藏的配置项：点击任意配置行右侧（重置按钮右边）的星标即可收藏/取消收藏。通用标签的「收藏系统」是父项总开关，关闭后隐藏收藏星并清空收藏标签（收藏数据保留）；子设置「收藏星样式」控制外观，「收藏包含子项」开启后收藏父项会连带显示全部子项。存档列表左侧也有同样的星标：收藏的存档会**置顶显示**，取消收藏后**回到原来的位置**；收藏期间**不可删除**（删除按钮置灰并提示，需先取消收藏）。收藏数据保存在 `config/tweakify-mod-favorites.json`。

<details>
<summary><b>测试 — 调试 HUD 与报表（13 项）</b></summary>

| 功能 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| `debugInfoOverlay` 调试信息叠加层 | 在 HUD 左下角显示坐标、朝向、维度与脚下方块 | 布尔值 | 关 |
| `debugInfoShowPerformance` 显示性能 | 追加帧率与内存占用 | 布尔值 | 开 |
| `debugInfoShowTarget` 显示准星目标 | 追加准星指向的方块/实体 ID | 布尔值 | 开 |
| `debugInfoShowBiome` 显示生物群系 | 追加玩家所在生物群系的 ID | 布尔值 | 关 |
| `debugInfoShowSpeed` 显示移动速度 | 追加水平/垂直速度（格每秒），便于确认飞行、攀爬、如履平地是否生效 | 布尔值 | 关 |
| `debugInfoShowEntities` 显示附近实体 | 按 32 格范围统计附近生物、掉落物与其他实体数量；每 500ms 刷新一次 | 布尔值 | 关 |
| `debugInfoShowConfigStats` 显示配置统计 | 统计功能与显示标签下已开启与已修改的布尔开关数量；每 500ms 刷新一次 | 布尔值 | 关 |
| `debugInfoShowEffects` 显示药水效果 | 追加当前药水效果数量与最短剩余时间 | 布尔值 | 关 |
| `debugInfoShowHooks` 显示钩子探针 | 追加已激活的 Mixin 行为探针列表 | 布尔值 | 关 |
| `debugProbeReport` 钩子探针报表 | 纯快捷键动作：按下后把已激活的探针输出到聊天栏（或剪贴板，见下一项） | 快捷键 | 未绑定 |
| `debugProbeReportCopy` 探针输出到剪贴板 | 开启后探针报表写入剪贴板（每行一个钩子），不再输出到聊天栏 | 布尔值 | 关 |
| `debugEnabledFeaturesReport` 已启用功能报表 | 纯快捷键动作：按下后把已启用的根功能开关名称输出到聊天栏 | 快捷键 | 未绑定 |
| `debugEnabledFeaturesReportCopy` 报表输出到剪贴板 | 开启后报表写入剪贴板（每行一个开关），不再输出到聊天栏 | 布尔值 | 关 |

</details>

> **服务端判定**：下列功能由集成服务端执行，仅在单人游戏或自己开设的局域网世界生效——诅咒附魔、附魔过于昂贵、移除附魔冲突、可见交易、中键获得液体桶、生存模式中键取物、锹单层铲雪、锻造模板、掉落物与经验球寿命、超平坦史莱姆、疣块分解、床距离、放置覆盖、耕地踩踏、骨粉催熟、刷怪蛋投掷、雪傀儡不受群系影响、维度传送、蛛网/液体/粘液块/蜂蜜块物理、冰霜行者跳跃修复、举盾受伤音效修复、骷髅瞄准修复、2×2 树苗生长修复、盔甲架双手放置物品、取消速度上限。
>
> **仅本地渲染/界面**：隐藏物品展示框、附魔台附魔信息、攻击/爆炸/横扫范围与屏障/基岩等世界叠加层不依赖集成服务端，连接任何服务器都可用。

> `itemDespawnTime` 把寿命调长依赖对 `ItemEntity.tick` 内部调用的重定向：若其他模组整体重写了该方法，注入会静默失效，此时日志里不会出现 `Mixin hook active: itemDespawnTick`，功能退回原版行为。

[↑ 返回顶部](#)

