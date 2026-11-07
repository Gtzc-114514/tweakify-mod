# Tweakify Mod

Minecraft **26.3 / Fabric** 的客户端工具模组：这是一个调整优化原版机制的工具，所有改动都做成可开关、可设快捷键的配置项，并提供一套基于 MaLiLib 的配置界面。

> 纯客户端模组（`environment: client`），专用服务器无需安装。

## 环境要求

| 项 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | >= 0.19.5 |
| Fabric API | 0.163.0+26.3 |
| MaLiLib | 0.30.2（必需） |
| Mod Menu | 21.0.0（可选，仅用于提供模组列表入口） |
| Java | 25 |

## 功能一览

「默认」列为新生成配置文件时的初始值。

### 副手回退与交互

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `blockOffhandFallback` 副手回退拦截 | 开 | 主手物品用完且仍按住右键时，阻止受限副手物品自动接替使用 |
| `interceptBlockInteraction` 拦截方块交互 | 关 | 副手回退生效时一并拦截方块交互；关闭时仍拦副手方块放置 |
| `interceptEntityInteraction` 拦截实体交互 | 关 | 副手回退生效时一并拦截实体交互 |
| `restrictedOffhandItems` 受限副手物品 | 全部光源物品 | 仅命中该列表的副手物品会被拦截 |

### 交互冷却

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `useCooldown` 右键交互冷却（总开关） | 开 | 关闭时主/副手使用冷却全部失效 |
| `mainHandUseCooldown` 主手使用冷却 | 关 | 一次右键只使用一次，按物品列表生效 |
| `offhandUseCooldown` 副手使用冷却 | 关 | 同上，作用于副手 |
| `mainHandUseCooldownItems` / `offhandUseCooldownItems` | 全部可交互物品 | 冷却生效的物品 ID 列表 |
| `miningCooldown` 左键交互冷却 | 关 | 一次左键只挖一个方块；仅在方块真正被破坏时计数 |
| `miningCooldownAttacks` 攻击也限一次 | 关 | 攻击实体也受一次左键一次的限制（与挖方块共用按住周期） |
| `elytraFireworkCooldown` 鞘翅烟花限制 | 关 | 滑翔中使用烟花加速后，等加速结束才能再次右键使用烟花；可设置快捷键开关 |
| `elytraFireworkCooldownTicks` 最短间隔 | 0 | 两次烟花之间的最短间隔（tick，0 = 等本次加速结束） |

### 移动与视角

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableUseItemSlowdown` 禁用使用物品减速 | 关 | 进食、拉弓、举盾时不再降低移速 |
| `useItemSlowdownExceptions` 减速例外物品 | 空 | 这些物品仍保留原版的使用减速（物品 ID 列表） |
| `disableHurtShake` 禁用受伤抖动 | 关 | 不影响行走时的镜头晃动 |
| `disableFlightDrift` 移除飞行漂移 | 关 | 飞行时松开方向键立即停止水平移动 |
| `flightDriftVertical` 垂直飞行影响 | 关 | 松开跳跃/潜行后也立即停止升降 |
| `elytraWaterFlight` 鞘翅水中飞行 | 关 | 滑翔时不再被水流移动逻辑接管，潜入水中也能继续保持滑翔姿态与速度 |
| `elytraWaterFlightOnlyWater` 仅水中生效 | 关 | 开启后只在水中保持滑翔，岩浆里仍按原版下沉 |
| `disableSprintFov` 禁用疾跑 FOV 变化 | 关 | 疾跑时视野不再放大；速度药水、飞行、拉弓等其它视野效果保持原版 |
| `keepMiningProgressOnItemSwitch` 禁用切换工具挖掘打断 | 关 | 像基岩版一样：挖掘中切换物品不再重置进度，继续挖同一个方块 |

### 渲染

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableHurtTint` 禁用受伤变红 | 关 | 保留死亡变红与受伤闪白 |
| `entityHighlight` 自定义实体高亮 | 关 | 为预设列表中的实体显示发光描边 |
| `highlightPreset1/2/3` 高亮预设 | 关 | 三个预设互斥，开启一个会自动关闭其他 |
| `highlightPreset1/2/3Entities` | 空 | 各预设需要高亮的实体 ID 列表 |
| `highlightSingleLayerBedrock` 高亮单层基岩 | 关 | 在周围单层基岩朝向空气的顶面/底面上显示凸出 1 像素的实心金色小方块，便于寻找下界顶棚的可击穿薄弱点 |
| `highlightSingleLayerBedrockMaxMarkers` 基岩标记最大数量 | 32 | 单次最多显示的基岩标记数量，1 ~ 256；超出时保留最近的 |
| `barrierVisibility` 屏障可见 | 关 | 周围屏障方块显示与手持屏障物品时相同的原版方块标记粒子，粒子固定在方块中心且逐方块冷却，避免偏移与重叠；扫描半径 16 格，每秒刷新两次 |
| `durabilityOverlay` 装备耐久显示 | 关 | 在物品栏的耐久条上方叠加耐久文字，满耐久时不显示，颜色随剩余耐久由绿转白、黄、红 |
| `durabilityDisplayMode` 耐久显示内容 | `percent` | `percent` 耐久百分比 / `value` 剩余耐久数值（只显示当前剩余，不显示初始耐久）；两种内容字号一致 |
| `durabilityOverlayHotbarOnly` 仅快捷栏显示 | 关 | 只在快捷栏显示耐久文字，背包格子里不显示 |
| `durabilityOverlayLowThreshold` 低耐久提醒阈值 | 0 | 剩余耐久低于该百分比时才显示，0 表示始终显示 |
| `betterInvisibility` 更好的隐身 | 关 | 隐身时不再显示药水粒子，身上的装备与第三人称手持物品也一并隐藏 |
| `betterInvisibilityEffectParticles` 隐藏药水粒子 | 开 | 隐身时不再显示药水效果粒子 |
| `betterInvisibilityLayers` 隐藏装备与手持物 | 开 | 第三人称下同时隐藏盔甲、披风与手持物品 |
| `betterInvisibilityLlamaCarpet` 隐藏羊驼地毯 | 开 | 羊驼隐身后，身上的地毯与装饰也一并隐藏 |
| `attackRangeDisplay` 攻击范围显示 | 关 | 以生物或玩家为中心显示半透明蓝色线框攻击范围，半径按原版攻击距离计算；可设置快捷键开关 |
| `attackRangeDisplayMode` 范围显示方式 | `flat` | `flat` 脚下双圆环 / `sphere` 经纬线框球体 |
| `attackRangeRenderMode` 范围渲染方式 | `wireframe` | `wireframe` 蓝色线框（无 OIT，性能更好）/ `filled` 半透明实心（观感更完整，可能掉帧） |
| `attackRangeIncludeSelf` 显示自己的范围 | 关 | 是否显示本地玩家自己的攻击范围 |
| `attackRangeMaxDistance` 范围收集距离 | 48 | 收集范围的最大距离，8 ~ 128 格 |
| `attackRangeMaxTargets` 最大显示数量 | 64 | 单次最多显示的攻击范围数量，8 ~ 256；超出时保留最近的实体 |
| `attackRangeEntities` 攻击范围实体 | 空 | 需要显示攻击范围的生物/玩家 ID 列表；为空时显示全部生物与玩家 |
| `nightVisionFadeOut` 夜视淡出 | 关 | 夜视效果快结束时不再闪烁，而是平滑淡出恢复正常 |
| `nightVisionFadeOutDuration` 淡出时长 | 200 | 平滑淡出的时长（tick），20 ~ 400 |
| `nightVisionFadeOutCurve` 淡出曲线 | `smooth` | `smooth` 平滑（两端更慢）/ `linear` 线性 |
| `disableNetherPortalEffect` 禁用地狱门特效 | 关 | 进入下界传送门时不再显示紫色覆盖层、镜头旋转与传送门音效（原有的关界面、传送逻辑不变） |
| `disableNetherPortalOverlay` 禁用紫色覆盖层 | 开 | 站在传送门里时不再显示紫色半透明覆盖层 |
| `disableNetherPortalCamera` 禁用镜头旋转 | 开 | 站在传送门里时镜头不再绕视角旋转 |
| `disableNetherPortalSound` 禁用传送门音效 | 开 | 不再播放传送门的循环音效与传送瞬间的音效 |
| `forceCloseLoadingScreen` 强制关闭加载屏幕 | 关 | 加载世界时立刻关闭“正在加载地形”界面，提前看到世界；世界仍在后台继续加载（属于「修改」标签） |

> 已知问题：MC 26.3 在 **Vulkan 后端 + NVIDIA 驱动**下渲染实体描边会崩溃（`nvoglv64.dll`）。使用实体高亮时请把图形 API 设为 OpenGL。

### 屏幕覆盖层

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableOverlayEffects` 禁用屏幕覆盖层（总开关） | 关 | 关闭细雪、岩浆、水下等全屏覆盖效果；下面的子开关分别控制；可设置快捷键开关 |
| `disablePowderSnowOverlay` 禁用细雪覆盖 | 开 | 陷入细雪时不再显示冰霜边框覆盖层 |
| `disableLavaOverlay` 禁用岩浆覆盖 | 开 | 在岩浆中不再显示橙红色全屏覆盖层 |
| `disableWaterOverlay` 禁用水下覆盖 | 开 | 水下不再显示蓝色水面覆盖层 |
| `disableWaterFog` 禁用水下雾 | 开 | 水下不再有雾与蓝色滤镜（26.3 的雾是独立系统） |
| `disableLavaFog` 禁用岩浆雾 | 开 | 岩浆里不再有橙色雾 |
| `disablePowderSnowFog` 禁用细雪雾 | 开 | 陷入细雪时不再有白雾 |

### 维度传送

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableDimensionTeleport` 禁用维度传送（总开关） | 关 | 总开关关闭时以下各项均不生效 |
| `dimensionTeleportPlayersOnly` 只拦玩家 | 关 | 只拦截玩家的维度传送，其他实体仍可正常穿过传送门 |
| `disableNetherPortalTeleport` 禁用地狱门传送 | 开 | 开启且黑名单为空时拦截所有实体 |
| `disableEndPortalTeleport` 禁用末地门传送 | 开 | 同上 |
| `disableEndGatewayTeleport` 禁用末地折跃门传送 | 开 | 阻止实体通过末地折跃门传送；需要总开关开启，黑名单为空时拦截全部实体 |
| `endGatewayTeleportBlacklist` 末地折跃门传送黑名单 | 空 | 非空时只拦截名单中的实体 |
| `netherPortalTeleportBlacklist` / `endPortalTeleportBlacklist` | 空 | 非空时只拦截名单中的实体 |

> 传送判定发生在服务端，因此该功能**仅在单人游戏或自己开设的局域网世界生效**。

### 世界交互

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `disableCurseEnchantmentRestriction` 诅咒附魔可移除 | 关 | 砂轮可以去除诅咒附魔（原版会保留诅咒） |
| `swordCutThroughPlants` 剑穿草打生物 | 关 | 主手持剑时，准星指向花草也会直接选中后方的生物，左键不会先把草打掉；有碰撞的植物（如甜浆果丛）不受影响（属于「功能」标签） |
| `cutThroughItems` 生效物品 | `sword` | `sword` 仅剑 / `sword_and_axe` 剑与斧 / `any` 任意手持物品 |
| `cutThroughPlants` 植物范围 | `all` | `flowers` 花草 / `crops` 花草与作物 / `all` 全部无碰撞植物 |
| `cutThroughSneakBypass` 潜行保持原版 | 开 | 潜行时恢复原版行为，左键先把植物打掉 |
| `pickFluidBucket` 中键获得液体桶 | 关 | 准星指向水/岩浆时按中键获得对应液体桶（**仅创造模式**）；仅在单人/局域网世界生效（属于「功能」标签） |
| `survivalPickBlock` 生存模式中键取物 | 关 | 独立功能：生存模式下按中键，准星指向方块/液体/实体时，背包里有对应物品就切换到主手；没有则保持原版；可设置快捷键开关 |
| `disableTooExpensive` 禁用附魔过于昂贵 | 关 | 铁砧花费达到 40 级时不再提示“过于昂贵”，结果可以直接取出且等级不足也能取走（等级最低到 0） |
| `disableShovelSnowFullBreak` 锹单层铲雪 | 关 | 手持锹右键多层雪层时一次只铲掉一层 |
| `shovelSnowDropSnowball` 铲雪掉落雪球 | 开 | 单层铲雪掉落 1 个雪球；关闭后不产生掉落物 |
| `shovelSnowConsumeDurability` 铲雪消耗耐久 | 开 | 单层铲雪按普通挖掘消耗锹耐久；关闭后不消耗 |
| `fixBoatStuckOnDirtPath` 草径卡船修复 | 关 | 只给船增加 1/16 格上台阶高度，消除土径与完整方块之间的卡顿；不改变方块碰撞，不影响放置、摩擦和滑行速度 |
| `disableSmithingTemplateConsumption` 锻造模板不消耗 | 关 | 锻造台取出成品时保留锻造模板，不再消耗；仅在单人/局域网世界生效 |
| `itemDespawnTime` 掉落物消失时间 | 6000（原版 5 分钟） | 滑块调节 60 ~ 36000 tick（20 tick = 1 秒）；仅在单人/局域网世界生效 |
| `itemDespawnPlayerDroppedOnly` 仅玩家丢弃物生效 | 关 | 只有玩家主动丢弃的掉落物按上面的时间消失，怪物与方块掉落保持原版 |
| `itemDespawnTimeXpOrbs` 经验球消失时间 | 6000 | 经验球的消失时间（tick），范围同上 |
| `disableFlatWorldSlimeSpawn` 超平坦无史莱姆 | 关 | 超平坦世界不再生成史莱姆；仅在单人/局域网世界生效 |
| `flatWorldSlimeSpawnAllWorlds` 对所有世界生效 | 关 | 所有世界类型都不生成史莱姆，不再只限超平坦 |
| `wartBlockDecomposition` 疣块分解 | 关 | 合成格里单独放一个下界疣块，结果槽直接产出 9 个下界疣（原版已有配方时优先走原版）；仅在单人/局域网世界生效 |
| `disableBedTooFar` 禁用床太远 | 关 | 离床再远右键也能直接入睡，不再提示“离床太远”；仅在单人/局域网世界生效 |
| `betterBlockPlacement` 更好的放置覆盖 | 关 | 手持方块右键时可直接替换花、高草等植物（有碰撞的甜浆果丛、水草等不受影响）；仅在单人/局域网世界生效 |
| `betterBlockPlacementMode` 覆盖范围 | `flowers` | `flowers` 花草（花、树苗、高草）/ `crops` 花草与作物 / `all` 全部无碰撞植物 |
| `disableFarmlandTrampling` 禁用耕地踩踏 | 关 | 从高处落在耕地上不再把它踩回泥土；仅在单人/局域网世界生效 |
| `farmlandTramplingPlayerOnly` 仅玩家生效 | 关 | 只有玩家不会踩坏耕地，动物与村民仍会踩坏 |

> 以上依赖服务端判定的功能（诅咒附魔、附魔过于昂贵、中键获得液体桶、生存模式中键取物、铲雪、草径、锻造模板、掉落物与经验球寿命、超平坦史莱姆、疣块分解、床距离、放置覆盖、耕地踩踏）**仅在单人游戏或自己开设的局域网世界生效**。
>
> `itemDespawnTime` 把寿命调长依赖对 `ItemEntity.tick` 内部调用的重定向：若其他模组整体重写了该方法，注入会静默失效，此时日志里会有一条 `Item despawn hook is missing` 警告，功能退回原版行为。

### 实用工具

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `copyItemId` 复制物体 ID | 关 | 总开关；开启后用子项「复制目标 ID 快捷键」对准方块/实体、或在容器界面里悬停物品复制 ID，聊天栏里的 ID 可点击再次复制。开关所在行的快捷键只负责切换开关 |
| `copyItemIdFormat` 复制格式 | `plain` | `plain` 纯 ID / `typed` 带类型前缀（物品/方块/实体）/ `counted` 附带数量 |
| `copyItemIdCloseContainer` 复制后关闭界面 | 关 | 在容器界面复制物品 ID 后自动关闭该界面 |
| `copyItemIdChatMessage` 聊天栏提示 | 开 | 复制成功后在聊天栏显示可点击复制的 ID；关闭后只写入剪贴板 |
| `chatTextSelection` 聊天文字框选 | 关 | 在聊天界面按住左键拖动选择消息文字，Ctrl+C 复制 |
| `chatSelectionColor` 框选高亮颜色 | `blue` | `blue` / `green` / `orange` / `purple` / `gray` |
| `chatSelectionAutoCopy` 松开自动复制 | 关 | 松开左键时自动复制选中的文字，不必再按 Ctrl+C |
| `scrollListMemory` 滚动列表记忆 | 开 | 记住列表滚到了哪里，关闭界面再打开回到上次位置（默认开启）；位于「通用」标签 |
| `scrollListMemoryPersist` 跨重启记忆 | 关 | 把滚动位置写入 `config/tweakify-mod-scroll-memory.json`，重启游戏后仍然生效 |
| `malilibScrollMemory` MaLiLib 配置界面 | 开 | 记忆 MaLiLib 配置界面（含本模组配置界面）的滚动位置 |
| `modMenuScrollMemory` Mod Menu 模组列表 | 开 | 记忆 Mod Menu 模组列表的滚动位置；未安装 Mod Menu 时不生效 |
| `enchantmentLevelArabic` 附魔等级阿拉伯数字 | 关 | 附魔等级使用 1、2、3... 代替 I、II、III... |

### 界面语言

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| `followSystemLanguage` 跟随系统语言 | 开 | 使用游戏当前语言 |
| `rememberSelectedTab` 标签记忆 | 关 | 记住上次选择的分类标签（全部/功能/通用/修改），下次打开配置界面直接停在该标签；属于「通用」标签 |
| `language` 界面语言 | `zh_cn` | 支持 `zh_cn` / `en_us` / `ru_ru` / `fr_fr`，关闭「跟随系统语言」后生效 |

## 命令

| 命令 | 说明 |
| --- | --- |
| `/tweakify <开关>` | 切换指定开关（需 OP / 权限等级 2） |
| `/tweakify <开关> <true\|false>` | 直接设置 |
| `/tweakify restrictedOffhandItems add <物品>` | 加入受限副手列表 |
| `/tweakify restrictedOffhandItems remove <物品>` | 移出受限副手列表 |
| `/tweakify restrictedOffhandItems list [页码]` | 查看受限副手列表 |
| `/tweakify restrictedOffhandItems reset` | 重置为默认光源物品列表 |
| `/fconfig` | 打开配置界面（客户端命令） |

## 快捷键

- 打开/关闭配置界面：默认 **G + C**
- 「功能」标签只给根功能提供切换快捷键（默认未绑定），子设置不带快捷键
- 复制物体 ID：需要先在配置界面里给「复制目标 ID 快捷键」绑定按键（默认未绑定），且「复制物体 ID」开关为开启状态

## 排查

- 日志里的 `Mixin hook active: <名称>` 表示对应 `require = 0` 注入点已经执行过；功能开了却没效果时，先看这条在不在，能区分“没注入”和“注入了但逻辑不生效”。
- 连接远程服务器时会提示一次：铁砧、铲雪、掉落物寿命、史莱姆生成、耕地踩踏、中键取物等功能在服务端执行，只在单人游戏或局域网主机生效。
- 打开 Mod Menu 列表时会不断记录滚动位置，但写盘只在关闭界面或每 10 秒节流一次，不会每帧同步 IO。

## 配置文件

`config/tweakify-mod.json`，由配置界面或命令自动保存。文件损坏时会被重命名为 `tweakify-mod.json.broken-<时间戳>` 并重新生成默认配置。

文件里带有 `configVersion` 字段：旧版本配置读取后会自动升级并回写，便于以后调整字段语义时做迁移。

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

## 许可证

CC0-1.0
