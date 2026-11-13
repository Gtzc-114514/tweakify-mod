# 发布流水线说明

本文档说明 `.github/workflows/release.yml` 的触发条件、步骤顺序、失败语义与已知取舍，
方便回看而不用翻提交记录与聊天记录。

**基线**：截至 2026-11-14，`origin/main` 为 `40a98b0`；`release.yml` 若有后续变动，以实际文件为准。

## 触发条件

| 项 | 值 |
| --- | --- |
| 触发 | `on.push.tags: ['v*']`（只认标签，普通分支 push 不会触发） |
| 权限 | `permissions.contents: write` |
| 并发 | `concurrency.cancel-in-progress: false`（同标签排队而非取消，避免中断在 Release 创建中途） |
| 超时 | `timeout-minutes: 30` |
| Runner | `ubuntu-24.04`，JDK 25（microsoft distribution） |

## 步骤顺序与失败语义

| # | 步骤 | 失败条件 | 失败表现 | 后续步骤 |
| --- | --- | --- | --- | --- |
| 1 | checkout repository | 网络/权限 | action 报错 | 全部跳过 |
| 2 | validate gradle wrapper | wrapper jar 被篡改 | action 报错 | 全部跳过 |
| 3 | setup jdk | JDK 25 不可用 | action 报错 | 全部跳过 |
| 4 | setup gradle | 缓存服务异常 | action 报错 | 全部跳过 |
| 5 | make gradle wrapper executable | 文件缺失 | 命令报错 | 全部跳过 |
| 6 | **resolve mod version**（`id: gv`） | 解析不到 version | `::error::...仅支持形如 version=1.0.1 的单行写法...` | **7–11 全跳过** |
| 7 | **verify tag matches mod version** | 标签 ≠ `gradle.properties` | `::error::Tag vX (version X) does not match ...` | 8–11 跳过 |
| 8 | build | 编译/测试失败 | Gradle 输出 | 9–11 跳过 |
| 9 | **verify jar mod version** | 见「5 类 `::error::` 分支」 | 5 类 `::error::` | 10–11 跳过 |
| 10 | **resolve release notes**（`id: notes`） | **不会失败** | `::warning::` + 回退自动生成 | 11 正常执行 |
| 11 | create GitHub Release | action 失败 / 产物不匹配 | `fail_on_unmatched_files: true` | — |

**关键设计**：全文件无 `if: always()` / `continue-on-error`，任一前置失败都会让后续（含 Release 创建）全部跳过，严格 fail-closed。

## 版本一致性校验链

```
6 resolve mod version     解析一次 gradle.properties → steps.gv.outputs.version
7 verify tag matches      标签 ←→ output         （env 传入）
9 verify jar mod version  jar  ←→ output         （env 传入）
⇒ 传递结论：标签 = gradle.properties = jar
```

- 第 6 步的提取规则与 Gradle 的 `Properties` 语义对齐：`tr -d '\r'` 去 CRLF、正则允许行首与等号两侧空格、`tail -n 1` 取末值（后覆盖前）；
- 解析逻辑单点，两个校验步骤通过 `env: GRADLE_VERSION: ${{ steps.gv.outputs.version }}` 消费，不会漂移；
- 解析提前到 build 与 unzip 之前：配置错误最先暴露，不用等完整构建。

## 5 类 `::error::` 分支（verify jar mod version）

| 类别 | 触发条件 | 注解 |
| --- | --- | --- |
| 工具链 | `find`/`head`/`unzip`/`python3` 任一缺失 | `runner 镜像缺少 <tool>，无法校验 jar 版本` |
| 枚举失败 | `find` 非零（`build/libs` 不存在或权限） | `无法枚举 build/libs（目录不存在或 find 失败）` |
| 无匹配 jar | `find` 成功但零匹配 | `No main jar found in build/libs` |
| 内容问题 | 无 `fabric.mod.json` / jar 损坏 / 缺 `version` | `无法从 <jar> 读取 fabric.mod.json 的 version（文件缺失、jar 损坏或缺少 version 字段）` |
| 版本不一致 | 值与 `GRADLE_VERSION` 不等 | `Version mismatch: fabric.mod.json=X gradle.properties=Y` |

各分支互斥：任一运行只输出一条 `::error::`，且不会出现「失败但无注解」的路径。

## 发布正文机制（resolve release notes）

- 优先使用 `docs/release-notes/<tag>.md`（`-s` + `grep -q '[^[:space:]]'` 双条件，0 字节或纯空白都视为无效）；
- 无效或缺失时输出 `::warning::` 并回退到 `generate_release_notes: true`（GitHub 自动生成）；
- **约定**：notes 文件必须在**打标签之前**合入 main——tag 触发时 checkout 的是 tag 指向的提交，晚于 tag 的 notes 不会被看到；
- 文件名必须与标签名一致（`vX.Y.Z.md` ↔ `vX.Y.Z`）；
- 发布后如需补充正文，用 `gh release edit <tag> --notes-file docs/release-notes/<tag>.md` 同步；
  注意重跑该标签的 workflow 会以 tag 提交中的 notes 覆盖线上正文，补充内容应以提交方式回写并重新打标签；
- 详细约定见 `docs/release-notes/README.md`。

## 已知但决定不改的点

| # | 位置 | 内容 | 不修理由 |
| --- | --- | --- | --- |
| L1 | `release.yml` 的 `find` 行 | `find ... 2>/dev/null` 让 ENOENT（目录不存在）与 EACCES（权限）无法区分 | 只影响日志精细度；build 失败本身已足够醒目 |
| L2 | `release.yml` 的 `JAR=$(printf ... \| head -n1)` | 保留了管道路径（可用 `${JAR_LIST%%$'\n'*}` 替代） | 已验证不会触发 141；仅「理论上更健壮」 |
| L3 | `release.yml` 的三处注解 | `No main jar found` 为英文、其余中文；`"version": ""` 的 jar 会归因为 mismatch | 既有文案；空 version 的 jar 现实中不会出现 |
| H1 | 8 个 Mixin | 既有的 8 个 `require = 0` 注入点仍无 `HookProbe` 探针（`LevelRendererOutlineStyleMixin`×1、`VillagerMixin`×6、`ModsScreenScrollMixin`×1） | 均为既有问题，不构成发布阻塞 |
| H2 | `release.yml` 的 jar 读取分支 | 内容问题的三种原因（无清单/损坏/缺字段）合并为一条注解 | stderr 被刻意收敛；如需细分可改为落临时文件后 `cat` |

## 下次发布清单

1. 提升 `gradle.properties` 的 `version`，同步 README 的版本表；
2. 在 `docs/release-notes/` 写好 `<新标签>.md`，**并先提交推送到 main**；
3. 打 annotated 标签：`git tag -a vX.Y.Z -m "Tweakify Mod X.Y.Z"`；
4. `git push origin vX.Y.Z`；
5. 在 Actions 查看 release 运行：`resolve mod version` → `verify tag matches` → `build` → `verify jar mod version` → `resolve release notes` → `create GitHub Release` 全绿即完成。

## 相关文件

| 文件 | 作用 |
| --- | --- |
| `.github/workflows/release.yml` | 发布流水线本体 |
| `docs/release-notes/README.md` | 发布说明文件的命名与提交时序约定 |
| `docs/release-notes/v1.0.1.md` | 1.0.1 的发布正文 |
| `gradle.properties` | `version` 单一来源（标签与 jar 都以它为准） |
