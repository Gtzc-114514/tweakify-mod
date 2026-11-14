# Tweakify Mod

Minecraft **26.3 / Fabric** 纯客户端微调模组。把原版中零散、不可配置的行为整理成可开关、可绑定快捷键的选项，并提供基于 MaLiLib 的统一配置界面；配置修改即时持久化，专用服务器无需安装。

> 纯客户端模组（`environment: client`）；涉及服务端判定的功能仅在单人 / 局域网世界生效。

## 功能概览

功能按配置界面顶部的标签组织，与游戏内看到的分组一致：

- **功能** — 新增原版没有的行为：交互冷却与副手回退拦截、如履平地、更好的攀爬、主动爬行、相机运动、自定义方块轮廓与实体高亮、耐久与药水计时、生物攻击范围、维度传送控制、刷怪蛋投掷、物品分组等。
- **修改** — 改写原版规则：使用物品不减速、移除飞行漂移、禁用疾跑视野变化、切换工具不打断挖掘、鞘翅水中飞行、移除附魔与铁砧限制、掉落物寿命、生物生成与和平模式、耕地踩踏等。
- **修复** — 修正原版瑕疵：草径卡船、鹦鹉肩膀不跳舞、粗体字形粘连。
- **优化** — MiniHUD 掉帧优化。
- **通用** — 界面语言、打开配置界面的快捷键、标签记忆、滚动列表记忆。

## 环境要求

| 项 | 版本 |
| --- | --- |
| Tweakify Mod | 1.0.3 |
| Minecraft | 26.3 |
| Fabric Loader | >= 0.19.5 |
| Fabric API | 0.163.0+26.3 |
| MaLiLib | 0.30.2（必需） |
| Mod Menu | 21.0.0（可选，仅用于提供模组列表入口） |
| MiniHUD | 0.41.2（可选，仅用于 MiniHUD 掉帧优化） |
| Java | 25 |

## 功能一览

下面按配置界面的标签分节。每行以 `配置字段` 开头，后接便于识别的中文名；「默认」列是新生成配置文件时的初始值。说明中标注「仅在单人/局域网生效」的功能，判定发生在集成服务端，专用服务器上不会生效。

### 功能

新增原版没有的行为，也是快捷键支持最完整的一组。

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
| `disableFlightDrift` 移除飞行漂移 | 关 | 飞行时松开方向键立即停止水平移动 |
| `flightDriftVertical` 垂直飞行影响 | 关 | 松开跳跃/潜行后也立即停止升降 |
| `autoStepUp` 如履平地 | 关 | 自动跨上 1 格高的方块（像马一样）；仅对本地玩家生效 |
| `betterClimbing` 更好的攀爬 | 关 | 优化攀爬方块中的移动：向下看快速下降、上爬更快、可从地面起跳等，子项可单独开关；水平方向不减速请用「修改」标签的「经过可攀爬方块不减速」 |
| `climbingFastDescend` 向下看加速下降 | 开 | 在梯子、藤蔓上向下看时快速下降 |
| `climbingFasterAscend` 攀爬上升更快 | 开 | 向上攀爬时移动速度更快 |
| `climbingJumpFromGround` 攀爬时从地面起跳 | 开 | 贴着可攀爬方块站在地面时也能直接起跳 |
| `climbingCancelCollision` 无输入时不碰撞减速 | 开 | 没有移动输入时取消攀爬方块边缘的碰撞减速 |
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
| `entityHighlight` 自定义实体高亮 | 关 | 为预设列表中的实体显示发光描边；需同时开启一个预设 |
| `highlightPreset1` / `highlightPreset2` / `highlightPreset3` 高亮预设 | 关 | 三个预设互斥，开启一个会自动关闭其他 |
| `highlightPreset1Entities` / `highlightPreset2Entities` / `highlightPreset3Entities` 预设实体 | 空 | 各预设需要高亮的实体 ID 列表，支持 `minecraft:item` 等掉落物实体 |
| `highlightSingleLayerBedrock` 高亮单层基岩 | 关 | 在周围 32 格内的单层基岩朝向空气的顶面/底面显示凸出 1 像素的金色实心小方块，便于寻找下界顶棚的可击穿薄弱点；每秒刷新 6 次 |
| `highlightSingleLayerBedrockMaxMarkers` 基岩标记最大数量 | 32 | 单次最多显示的标记数（1 ~ 256）；超出时保留最近的 |
| `barrierVisibility` 屏障可见 | 关 | 周围屏障方块显示与手持屏障物品时相同的原版标记粒子，粒子固定在方块中心并逐方块冷却，不会偏移或重叠；扫描半径 32 格，每秒刷新 6 次 |
| `durabilityOverlay` 装备耐久显示 | 关 | 在物品栏的耐久条上方叠加耐久文字；满耐久不显示，颜色随剩余耐久由绿转白、黄、红 |
| `durabilityDisplayMode` 耐久显示内容 | `percent` | `percent` 耐久百分比 / `value` 剩余耐久数值（只显示当前剩余，不显示初始耐久）；两者字号一致 |
| `durabilityOverlayHotbarOnly` 仅快捷栏显示 | 关 | 只在快捷栏显示耐久文字，背包格子里不显示 |
| `durabilityOverlayLowThreshold` 低耐久提醒阈值 | 0 | 剩余耐久低于该百分比（0 ~ 100）时才显示；0 表示始终显示 |
| `statusEffectDurationDisplay` 药水效果时间显示 | 关 | 在 HUD 右侧药水效果图标下方显示剩余时间（秒 / 分:秒 / 2h05 / 2d05）；无尽效果不显示 |
| `attackRangeDisplay` 生物攻击范围 | 关 | 以生物或玩家为中心显示半透明蓝色线框攻击范围，半径按原版攻击距离计算；每秒刷新 12 次 |
| `attackRangeDisplayMode` 范围显示方式 | `flat` | `flat` 脚下双圆环 / `sphere` 经纬线框球体 |
| `attackRangeRenderMode` 范围渲染方式 | `wireframe` | `wireframe` 蓝色线框（无 OIT，性能更好）/ `filled` 半透明实心（观感更完整，可能掉帧） |
| `attackRangeIncludeSelf` 显示自己的范围 | 关 | 是否显示本地玩家自己的攻击范围；关闭后只显示其他生物与玩家 |
| `attackRangeMaxDistance` 范围收集距离 | 48 | 收集攻击范围的最大距离（8 ~ 128 格） |
| `attackRangeMaxTargets` 最大显示数量 | 64 | 单次最多显示的数量（8 ~ 256）；超出时保留最近的实体 |
| `attackRangeEntities` 攻击范围实体 | 空 | 需要显示范围的生物/玩家 ID 列表；为空时显示全部生物与玩家 |

> 已知问题：MC 26.3 在 **Vulkan 后端 + NVIDIA 驱动**下渲染实体描边会崩溃（`nvoglv64.dll`）。使用实体高亮时请把图形 API 设为 OpenGL。

#### 维度传送

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableDimensionTeleport` 禁用维度传送 | 关 | 总开关：关闭时以下各项均不生效 |
| `dimensionTeleportPlayersOnly` 只拦玩家 | 关 | 只拦截玩家的维度传送，其他实体仍可正常穿过传送门 |
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
| `survivalPickBlock` 生存模式中键取物 | 关 | 生存模式下按中键，准星指向方块/液体/实体时，背包里有对应物品就切换到主手；没有则保持原版 |
| `quickBurnItem` 便捷焚烧 | 关 | 背包或容器里拿着物品时，把它悬停在岩浆桶或打火石上右键点击即可焚烧；单人/局域网会真正销毁物品，多人服务器会改为丢弃 |
| `piglinGoldArmorSwap` 防猪灵装备切换 | 关 | 附近有猪灵时自动把背包里的金制装备换到身上，猪灵离开后换回；仅在生存或冒险模式生效 |
| `piglinGoldArmorSwapMode` 切换模式 | `auto` | `auto` 身上没有金制护甲时从背包选一件换上 / `helmet` / `chestplate` / `leggings` / `boots` 只替换指定部位 |
| `disableShovelSnowFullBreak` 锹单层铲雪 | 关 | 手持锹右键多层雪层时一次只铲掉一层，不再整块移除 |
| `shovelSnowDropSnowball` 铲雪掉落雪球 | 开 | 单层铲雪掉落 1 个雪球；关闭后不产生掉落物 |
| `shovelSnowConsumeDurability` 铲雪消耗耐久 | 开 | 单层铲雪按普通挖掘消耗锹耐久；关闭后不消耗 |
| `disableSpecifiedMobSpawn` 关闭指定生物生成 | 关 | 列表中的生物不会通过自然、结构、巡逻、增援、鸡骑士、袭击、唤魔者召唤或刷怪笼等世界生成出现；列表为空时不生效；仅在单人/局域网生效 |
| `disabledSpawnEntities` 生物 ID 列表 | 空 | 需要禁止生成的生物 ID 列表（如 `minecraft:zombie`）；为空时不生效 |
| `disabledSpawnAllowPlayerSpawn` 允许玩家生成 | 开 | 刷怪蛋、命令、繁殖等玩家主动生成的生物不拦截；关闭后这些来源也会被拦截。玩家建造类触发（如铁傀儡、凋灵）恒放行 |
| `peacefulModeMonsters` 和平模式怪物 | 关 | 允许列表中的怪物在和平难度下生成并保留；列表为空时不生效；仅在单人/局域网生效 |
| `peacefulModeMonsterEntities` 和平模式生物 ID 列表 | 空 | 和平模式怪物功能使用的生物 ID 列表；为空时不生效 |
| `peacefulModeMonsterSpawn` 和平模式中生成指定怪物 | 开 | 列表中的怪物可通过自然生成、刷怪笼、试炼刷怪笼和地狱门等来源在和平难度下生成，并跳过和平难度的自动清除；试炼刷怪笼需生成池全部命中列表 |
| `peacefulModeMonsterNoAttack` 和平模式怪物不攻击 | 开 | 列表中的怪物在和平难度下不锁定/攻击玩家；关闭时可正常攻击 |
| `throwSpawnEgg` 刷怪蛋投掷 | 关 | 像鸡蛋、雪球一样把刷怪蛋投掷出去，命中方块或实体后生成对应生物；对刷怪笼只设置类型且不消耗刷怪蛋；仅在单人/局域网生效 |
| `itemGroups` 物品分组 | 关 | 在创造模式分类页把楼梯、台阶、羊毛、药水等同类物品折叠成组图标；点击组图标展开/收起 |
| `itemGroupsSort` 组内排序 | `default` | `default` 保持物品栏原顺序 / `alphabetical` 按物品 ID 字母序 |
| `betterBlockPlacement` 更好的放置覆盖 | 关 | 手持方块右键时可直接替换花、高草等植物，不必先打掉；有碰撞的甜浆果丛、水草等不受影响；仅在单人/局域网生效 |
| `betterBlockPlacementMode` 覆盖范围 | `flowers` | `flowers` 花草（花、树苗、高草）/ `crops` 花草与作物 / `all` 全部无碰撞植物 |
| `betterBoneMeal` 更好的骨粉催熟 | 关 | 让骨粉像基岩版一样催熟甘蔗与花；仅在单人/局域网生效 |
| `boneMealSugarCane` 骨粉催熟甘蔗 | 开 | 一次骨粉把整株甘蔗催到最高 3 格，只消耗 1 个骨粉 |
| `boneMealFlowers` 骨粉催熟花 | 开 | 在一格高的花周围长出一片相同的花（凋零玫瑰除外，与基岩版一致） |

#### 实用工具

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `copyItemId` 复制物体 ID | 关 | 总开关；开启后用「复制目标 ID 快捷键」对准方块/实体、或在容器界面里悬停物品复制 ID，聊天栏里的 ID 可点击再次复制 |
| `copyTargetId` 复制目标 ID 快捷键 | 未绑定 | 对准方块/实体或在容器界面悬停物品时按下，复制其 ID；需开启 `copyItemId` 总开关 |
| `copyItemIdFormat` 复制格式 | `plain` | `plain` 纯 ID / `typed` 带类型前缀（物品/方块/实体）/ `counted` 附带数量 |
| `copyItemIdCloseContainer` 复制后关闭界面 | 关 | 在容器界面复制物品 ID 后自动关闭该界面 |
| `copyItemIdChatMessage` 聊天栏提示 | 开 | 复制成功后在聊天栏显示可点击复制的 ID；关闭后只写入剪贴板 |
| `chatTextSelection` 聊天文字框选 | 关 | 在聊天界面按住左键拖动选择消息文字，Ctrl+C 复制 |
| `chatSelectionColor` 框选高亮颜色 | `blue` | `blue` / `green` / `orange` / `purple` / `gray` |
| `chatSelectionAutoCopy` 松开自动复制 | 关 | 松开左键时自动复制选中的文字，不必再按 Ctrl+C |
| `chatInputMemory` 聊天框内容记忆 | 关 | 关闭聊天框后保留已输入但未发送的内容，下次打开聊天框时自动恢复；发送消息后清空 |

### 修改

调整原版规则，行为上更接近基岩版或其它工具链的常见做法。

#### 移动与挖掘

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableUseItemSlowdown` 禁用使用物品减速 | 关 | 进食、拉弓、举盾时不再降低移速 |
| `useItemSlowdownExceptions` 减速例外物品 | 空 | 这些物品仍保留原版的使用减速（物品 ID 列表） |
| `disableSprintFov` 禁用疾跑 FOV 变化 | 关 | 疾跑时视野不再放大；速度药水、飞行、拉弓等其它视野效果保持原版 |
| `keepMiningProgressOnItemSwitch` 禁用切换工具挖掘打断 | 关 | 像基岩版一样：挖掘中切换物品不再重置进度，继续挖同一个方块 |
| `climbablePassNoSlowdown` 经过可攀爬方块不减速 | 关 | 走过梯子、藤蔓等可攀爬方块时，蹭到边缘也不再被原版夹紧水平速度；攀爬上升/下滑保持原版 |
| `elytraWaterFlight` 鞘翅水中飞行 | 关 | 滑翔时不再被水流移动逻辑接管，潜入水中也能继续保持滑翔姿态与速度 |
| `elytraWaterFlightOnlyWater` 仅水中生效 | 关 | 只在水中保持滑翔，岩浆里仍按原版下沉 |
| `disableFarmlandTrampling` 禁用耕地踩踏 | 关 | 从高处落在耕地上不再把它踩回泥土；仅在单人/局域网生效 |
| `farmlandTramplingPlayerOnly` 仅玩家生效 | 关 | 只有玩家不会踩坏耕地，动物与村民仍会踩坏 |

#### 视角与显示

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableHurtShake` 禁用受伤抖动 | 关 | 受伤时不再产生视角抖动；行走时的镜头晃动不受影响 |
| `disableHurtTint` 禁用受伤变红 | 关 | 受伤时不再叠加红色 overlay；死亡变红与受伤闪白保持原版 |
| `thirdPersonPlayerName` 第三人称名字显示 | 关 | 第三人称（背后/正面）也显示玩家自己的名字；第一人称、隐身、旁观和隐藏 HUD 时保持原版 |
| `betterInvisibility` 更好的隐身 | 关 | 隐身时不再显示药水粒子，身上的装备与第三人称手持物品也一并隐藏 |
| `betterInvisibilityEffectParticles` 隐藏药水粒子 | 开 | 隐身时不再显示药水效果粒子 |
| `betterInvisibilityLayers` 隐藏装备与手持物 | 开 | 第三人称下同时隐藏盔甲、披风与手持物品 |
| `betterInvisibilityLlamaCarpet` 隐藏羊驼地毯 | 开 | 羊驼隐身后，身上的地毯与装饰也一并隐藏 |
| `nightVisionFadeOut` 夜视淡出 | 关 | 夜视快结束时不再闪烁，而是平滑淡出恢复正常 |
| `nightVisionFadeOutDuration` 淡出时长 | 200 | 平滑淡出的时长（tick，20 tick = 1 秒），范围 20 ~ 400 |
| `nightVisionFadeOutCurve` 淡出曲线 | `smooth` | `smooth` 平滑（两端更慢）/ `linear` 线性 |
| `connectedBlockOutline` 连接方块轮廓 | 关 | 准星对准门、床、双箱、双层植物等连接方块时，显示整体轮廓（类似基岩版） |

#### 屏幕覆盖层与加载界面

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableOverlayEffects` 禁用屏幕覆盖层 | 关 | 总开关：关闭细雪、岩浆、水下等全屏覆盖效果；下面的子开关分别控制 |
| `disablePowderSnowOverlay` 禁用细雪覆盖 | 开 | 陷入细雪时不再显示冰霜边框覆盖层 |
| `disableLavaOverlay` 禁用岩浆覆盖 | 开 | 在岩浆中不再显示橙红色全屏覆盖层 |
| `disableWaterOverlay` 禁用水下覆盖 | 开 | 水下不再显示蓝色水面覆盖层 |
| `disableWaterFog` 禁用水下雾 | 开 | 水下不再有雾与蓝色滤镜（26.3 的雾是独立系统） |
| `disableLavaFog` 禁用岩浆雾 | 开 | 岩浆里不再有橙色雾 |
| `disablePowderSnowFog` 禁用细雪雾 | 开 | 陷入细雪时不再有白雾 |
| `forceCloseLoadingScreen` 强制关闭加载屏幕 | 关 | 加载世界时立刻关闭“正在加载地形”界面，提前看到世界；世界仍在后台继续加载 |
| `forceCloseLoadingScreenInstant` 完全关闭加载屏幕 | 关 | 直接关闭加载界面（完全跳过，可能短暂掉入世界）；仅在上一项开启时生效 |
| `forceCloseLoadingScreenKeepFirst` 保留加载界面 | 关 | 不跳过加载界面的文字与进度（背景和世界仍正常加载） |
| `forceCloseLoadingScreenSkipResourcePackFade` 资源包加载加速 | 开 | 去掉资源包加载屏幕的淡入淡出并立即移除覆盖层（资源加载完成后才进入游戏） |
| `disableNetherPortalEffect` 禁用地狱门特效 | 关 | 进入下界传送门时不再显示紫色覆盖层、镜头旋转与传送门音效；关界面与传送逻辑不变 |
| `disableNetherPortalOverlay` 禁用紫色覆盖层 | 开 | 站在传送门里时不再显示紫色半透明覆盖层 |
| `disableNetherPortalCamera` 禁用镜头旋转 | 开 | 站在传送门里时镜头不再绕视角旋转 |
| `disableNetherPortalSound` 禁用传送门音效 | 开 | 不再播放传送门的循环音效与传送瞬间的音效 |
| `disableResourcePackCompatibilityCheck` 关闭资源包兼容性提示 | 关 | 资源包界面不再显示不兼容标记，也不会在启用不兼容资源包时弹出确认提示；仅影响资源包，不影响数据包 |

#### 提示文本

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `enchantmentLevelArabic` 附魔等级阿拉伯数字 | 关 | 附魔等级使用 1、2、3... 代替 I、II、III... |
| `enchantmentDescriptions` 附魔描述 | 关 | 在物品提示中为每个附魔追加一行描述（附魔书同样支持） |

#### 世界交互与世界生成

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableUnderwaterMiningPenalty` 禁用水下挖掘惩罚 | 关 | 在水下挖掘时不再按水下挖掘速度属性减速 |
| `disableAirMiningPenalty` 禁用空中挖掘惩罚 | 关 | 离地挖掘时不再额外减速 |
| `disableCurseEnchantmentRestriction` 诅咒附魔可移除 | 关 | 砂轮可以去除诅咒附魔（原版会保留诅咒）；仅在单人/局域网生效 |
| `removeEnchantmentConflicts` 移除附魔冲突 | 关 | 铁砧合并装备时忽略附魔互斥限制，可以把锋利与亡灵杀手等互斥附魔合到同一件装备上；仅在单人/局域网生效 |
| `disableTooExpensive` 禁用附魔过于昂贵 | 关 | 铁砧花费达到 40 级时不再提示“过于昂贵”，结果可以直接取出，等级不足也能取走（等级最低到 0）；仅在单人/局域网生效 |
| `visibleTraders` 可见交易显示 | 关 | 在村民交易界面显示未来等级（锁定）的交易，村民升级时按顺序解锁；仅在单人/局域网生效 |
| `keepLastTorch` 不使用最后一根火把 | 关 | 放置火把时如果背包里只剩最后一根则不会放置；仅在单人/局域网生效 |
| `disableSmithingTemplateConsumption` 锻造模板不消耗 | 关 | 锻造台取出成品时保留锻造模板，不再消耗；仅在单人/局域网生效 |
| `itemDespawnTime` 掉落物消失时间 | 6000（原版 5 分钟） | 滑块调节 60 ~ 36000 tick（20 tick = 1 秒）；仅在单人/局域网生效 |
| `itemDespawnPlayerDroppedOnly` 仅玩家丢弃物生效 | 关 | 只有玩家主动丢弃的掉落物按上面的时间消失，怪物与方块掉落保持原版 |
| `itemDespawnTimeXpOrbs` 经验球消失时间 | 6000 | 经验球的消失时间（tick），范围同上 |
| `disableFlatWorldSlimeSpawn` 超平坦无史莱姆 | 关 | 超平坦世界不再生成史莱姆；仅在单人/局域网生效 |
| `flatWorldSlimeSpawnAllWorlds` 对所有世界生效 | 关 | 所有世界类型都不生成史莱姆，不再只限超平坦 |
| `disableBedTooFar` 禁用床太远 | 关 | 离床再远右键也能直接入睡，不再提示“离床太远”；仅在单人/局域网生效 |
| `ironGolemRideMinecart` 铁傀儡进矿车 | 关 | 空矿车碰到铁傀儡时，让铁傀儡坐进矿车；仅在单人/局域网生效 |
| `snowGolemBiomeImmunity` 雪傀儡不受群系影响 | 关 | 温暖或干燥群系不再让雪傀儡因群系属性融化；雨、水和熔岩伤害仍按原版；仅在单人/局域网生效 |
| `wartBlockDecomposition` 疣块分解 | 关 | 合成格里单独放一个下界疣块，结果槽直接产出 9 个下界疣（原版已有配方时优先走原版）；仅在单人/局域网生效 |

### 修复

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `fixBoatStuckOnDirtPath` 草径卡船修复 | 关 | 只给船增加 1/16 格上台阶高度，消除土径与完整方块之间的卡顿；不改变方块碰撞，不影响放置、摩擦和滑行速度 |
| `parrotDanceOnShoulder` 鹦鹉肩膀跳舞 | 关 | 像基岩版一样：附近唱片机播放音乐时，站在玩家肩膀上的鹦鹉也会跳舞，而不是保持静止 |
| `betterBold` 更好的粗体 | 关 | 把粗体字形偏移从原版 1.0 像素改为 0.5 像素（与 MCPatcher/OptiFine 一致），粗体文字更清晰、不再粘连 |

### 优化

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `miniHudFpsOptimization` MiniHUD 掉帧优化 | 关 | 把 MiniHUD 的 HUD 行刷新间隔放宽到 150ms，减少重复重算造成的掉帧；仍保持在渲染线程执行，避免并发访问；需要安装 MiniHUD |

### 通用

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `language` 界面语言 | `zh_cn` | 支持 `zh_cn` / `en_us` / `ru_ru` / `fr_fr`；关闭「跟随系统语言」后生效 |
| `followSystemLanguage` 跟随系统语言 | 开 | 使用游戏当前语言，忽略上面的语言选择 |
| `openConfigGui` 打开配置界面 | G + C | 打开或关闭本模组配置界面的快捷键；任意界面可用 |
| `rememberSelectedTab` 标签记忆 | 关 | 记住上次选择的分类标签（全部/通用/功能/修改/修复/优化），下次打开配置界面直接停在该标签 |
| `scrollListMemory` 滚动列表记忆 | 开 | 记住列表滚到了哪里，关闭界面再打开回到上次位置 |
| `scrollListMemoryPersist` 跨重启记忆 | 关 | 把滚动位置写入 `config/tweakify-mod-scroll-memory.json`，重启游戏后仍然生效 |
| `malilibScrollMemory` MaLiLib 配置界面 | 开 | 记忆 MaLiLib 配置界面（含本模组配置界面）的滚动位置 |
| `modMenuScrollMemory` Mod Menu 模组列表 | 开 | 记忆 Mod Menu 模组列表的滚动位置；未安装 Mod Menu 时不生效 |

> 配置界面还有一个「测试」标签，里面只有「档案编辑器」（保存、加载、删除整套配置档案），它不是配置项，不会写入配置文件。

> **服务端判定**：下列功能由集成服务端执行，仅在单人游戏或自己开设的局域网世界生效——诅咒附魔、附魔过于昂贵、移除附魔冲突、可见交易、中键获得液体桶、生存模式中键取物、锹单层铲雪、锻造模板、掉落物与经验球寿命、超平坦史莱姆、疣块分解、床距离、放置覆盖、耕地踩踏、骨粉催熟、刷怪蛋投掷、雪傀儡不受群系影响、维度传送。

> `itemDespawnTime` 把寿命调长依赖对 `ItemEntity.tick` 内部调用的重定向：若其他模组整体重写了该方法，注入会静默失效，此时日志里不会出现 `Mixin hook active: itemDespawnTick`，功能退回原版行为。

## 命令

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

## 快捷键

- **打开/关闭配置界面**：默认 **G + C**（对应配置项 `openConfigGui`），任意界面可用
- **切换快捷键**：带快捷键槽的根功能可以绑定一个按键，按下即切换该开关，默认全部未绑定；除「修改」标签的「禁用屏幕覆盖层」外，带切换键的都是「功能」标签的根功能，子设置不提供
- **主动爬行**：需要先在配置界面开启「主动爬行」总开关，并给子项「爬行按键」绑定一个按键；按住爬行、松开站起
- **复制物体 ID**：需要先在配置界面里给「复制目标 ID 快捷键」绑定按键（默认未绑定），且「复制物体 ID」开关为开启状态；总开关所在行的快捷键只负责切换开关

## 排查

- 日志里的 `Mixin hook active: <名称>` 表示对应 `require = 0` 注入点已经执行过；功能开了却没效果时，先看这条在不在，能区分“没注入”和“注入了但逻辑不生效”。
- 连接远程服务器时会提示一次：铁砧、铲雪、掉落物寿命、史莱姆生成、耕地踩踏、中键取物等功能在服务端执行，只在单人游戏或局域网主机生效。
- 打开 Mod Menu 列表时会不断记录滚动位置，但写盘只在关闭界面或每 10 秒节流一次，不会每帧同步 IO。

## 配置文件

`config/tweakify-mod.json`，由配置界面或命令自动保存。文件损坏时会被重命名为 `tweakify-mod.json.broken-<时间戳>` 并重新生成默认配置；若备份失败则保留原文件、不覆盖。若存在旧版 `config/fantasy-mod.json`，首次启动会自动迁移为 `tweakify-mod.json`；配置档案单独存放在 `config/tweakify-mod-profiles.json`。

文件里带有 `configVersion` 字段：旧版本配置读取后会自动升级并回写，便于以后调整字段语义时做迁移。从版本 2 起，`scrollListMemory` 默认开启；版本 1 的旧配置读取后会升级并回写。

从版本 3 开始，旧的 `motionCameraTilt` / `hotkeyMotionCameraTilt` 会迁移到 `cameraMotion` / `hotkeyCameraMotion`，旧的 `mergeCreativeItemEntries` / `hotkeyMergeCreativeItemEntries` 会迁移到 `itemGroups` / `hotkeyItemGroups`；迁移后旧键不再写回。

写入采用「临时文件 + 原子替换」，写盘失败时会把内存里的配置整体回滚到上一次成功的快照，不会留下半新半旧的 JSON。

## 新增配置项

配置项的注册关系收敛到了少数几个地方，新增一个开关只需四步：

1. 在 `TweakifyModConfig` 加字段（默认值写在字段初始化里）。
2. 在 `ConfigCatalog` 的 `ENTRIES` 表加一行（父项 / 分类 / 是否需要 `/tweakify` 命令）。
3. 在 `TweakifyModConfigOptions` 声明对应的 MaLiLib 选项（含 `.apply(...)` 语言前缀）。
4. 在 4 份语言文件补 `name` / `prettyName` / `comment`（需要命令时再补 `command.label.<name>`）。

启动时会校验「配置字段 ⟷ 配置目录 ⟷ MaLiLib 选项」双向覆盖，漏登记会直接抛异常而不是静默丢项；配置界面的树、分类标签、统计、快捷键与命令表都由目录派生。

## 构建与测试

```bash
./gradlew build          # 编译 + 打包
./gradlew test           # 单元测试
./gradlew runClient      # 启动开发客户端
```

Windows 下使用 `gradlew.bat`。需要环境变量 `JAVA_HOME` 指向 JDK 25。

## 手动回归：/tweakify 命令权限

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

## 许可证

CC0-1.0
