# 发布流水线说明

本文档说明 `.github/workflows/release.yml` 的触发条件、步骤顺序、失败语义与已知取舍，
方便回看而不用翻提交记录与聊天记录。

**基线**：`release.yml` 以提交 `414f1ec`（2026-11-16，`action-gh-release` v2 → v3）为准；本文件随 main 演进，
该文件后续变动时以实际文件为准，步骤名与数量请以 `- name:` 原文重新核对。

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
| 9 | **verify jar mod version** | 见「6 类 `::error::` 分支」 | 6 类 `::error::` | 10–11 跳过 |
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

- 第 6 步的提取规则对齐 Gradle `Properties` 的常用写法（`tr -d '\r'` 去 CRLF、正则允许行首与等号两侧空格、`tail -n 1` 取末值/后覆盖前）；
  不支持 `key: value`，也不裁剪值尾空白（错误文案已在注解中说明）；
- 解析逻辑单点，两个校验步骤通过 `env: GRADLE_VERSION: ${{ steps.gv.outputs.version }}` 消费，不会漂移；
- 解析提前到 build 与 unzip 之前：配置错误最先暴露，不用等完整构建。

## 6 类 `::error::` 分支（verify jar mod version）

| 类别 | 触发条件 | 注解 |
| --- | --- | --- |
| 工具链 | `find`/`unzip`/`python3` 任一缺失 | `runner 镜像缺少 <tool>，无法校验 jar 版本` |
| 目录不存在 | `build/libs` 缺失或不是目录 | `build/libs 不存在或不是目录（build 步骤未产出，或路径被同名文件占用）` |
| 枚举失败 | 目录存在但 `find` 非零（权限/挂载异常） | `无法枚举 build/libs（目录存在但 find 失败，可能是权限或挂载问题）` |
| 无匹配 jar | `find` 成功但零匹配 | `No main jar found in build/libs` |
| 内容问题 | 无 `fabric.mod.json` / jar 损坏 / 缺 `version` | `无法从 <jar> 读取 fabric.mod.json 的 version（文件缺失、jar 损坏或缺少 version 字段）` |
| 版本不一致 | 值与 `GRADLE_VERSION` 不等 | `Version mismatch: fabric.mod.json=X gradle.properties=Y` |

各分支互斥：任一运行只输出一条 `::error::`，且不会出现「失败但无注解」的路径。

## GitHub Release Action 版本

- `create GitHub Release` 使用 `softprops/action-gh-release@v3`（当前 v3.0.4，`runs.using: node24`）；
- 与 v2 的 inputs 兼容：继续使用 `files`、`body_path`、`generate_release_notes`、`fail_on_unmatched_files`；
  `permissions.contents: write` 保持不变；
- v2 在 Actions 上会触发 Node.js 20 弃用注解；v3 起改用 Node.js 24，该注解应在下一次标签发布时消失。

## 发布正文机制（resolve release notes）

- 优先使用 `docs/release-notes/<tag>.md`（`-s` + `grep -q '[^[:space:]]'` 双条件，0 字节或纯空白都视为无效）；
- 无效或缺失时输出 `::warning::` 并回退到 `generate_release_notes: true`（GitHub 自动生成）；
- **约定**：notes 文件必须在**打标签之前**合入 main——tag 触发时 checkout 的是 tag 指向的提交，晚于 tag 的 notes 不会被看到；
- 文件名必须与标签名一致（`vX.Y.Z.md` ↔ `vX.Y.Z`）；
- 发布后如需补充正文，用 `gh release edit <tag> --notes-file docs/release-notes/<tag>.md` 同步；
  注意重跑该标签的 workflow 会以 tag 提交中的 notes 覆盖线上正文，补充内容应以提交方式回写并重新打标签；
- 详细约定见 `docs/release-notes/README.md`。

## 验证记录：v1.0.2 首次真实发布

版本一致性校验链与 notes 机制在 v1.0.2 的发布中首次完整生效：

| 项 | 值 |
| --- | --- |
| Run ID | `43526912662` |
| 对应提交 | `11f6e9a`（`v1.0.2` 标签提交；当时 `verify jar mod version` 为 5 类归因，尚未拆分目录预检/枚举失败） |
| 触发 | push tag `v1.0.2` |
| 结论 | **success**，job 耗时 36 秒（run 39 秒） |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.2` 并写入 `$GITHUB_OUTPUT` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.2` 去前缀后与版本 `1.0.2` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.2` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.2.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.2` 开头（614 字符 / UTF-8 1,156 字节），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.2.jar`（557,981 字节）、`tweakify-mod-1.0.2-sources.jar`（406,450 字节）。

## 验证记录：v1.0.3 发布（CI 细化首次生效）

v1.0.2 之后累积的 CI 改动在本次发布首次真实生效，版本一致性校验链与 notes 机制再次完整通过：

| 项 | 值 |
| --- | --- |
| Run ID | `43543826316` |
| 对应提交 | `84edc61`（`v1.0.3` 标签提交；含手写 notes 与 1.0.3 版本号） |
| 触发 | push tag `v1.0.3` |
| 结论 | **success**，job 耗时 42 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.3` 并写入 `$GITHUB_OUTPUT` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.3` 去前缀后与版本 `1.0.3` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.3` 与版本一致；无 head 预检与 6 类归因正常通过 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.3.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.3` 开头（946 字符），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.3.jar`（562,825 字节）、`tweakify-mod-1.0.3-sources.jar`（410,179 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43543809341`）同为 success。

## 验证记录：v1.0.4 发布

本次无 `release.yml` 改动，版本一致性校验链与 notes 机制按既有流程再次通过：

| 项 | 值 |
| --- | --- |
| Run ID | `43606022836` |
| 对应提交 | `2e4d87c`（`v1.0.4` 标签提交；含 1.0.4 版本号、notes 与版本索引） |
| 触发 | push tag `v1.0.4` |
| 结论 | **success**，job 耗时 47 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.4` 并写入 `$GITHUB_OUTPUT` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.4` 去前缀后与版本 `1.0.4` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.4` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.4.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.4` 开头（1,282 字符），与仓库内 notes 逐字一致，含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.4.jar`（569,837 字节）、`tweakify-mod-1.0.4-sources.jar`（415,124 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43606035408`）同为 success。

> 备注：本次 `git push origin main` 首次失败（`Empty reply from server`），tag 推送成功后再补推 main；
> tag 指向的提交已包含 notes，Release 正文未受影响。

## 验证记录：v1.0.5 发布

本次无 `release.yml` 改动，版本一致性校验链与 notes 机制按既有流程再次通过：

| 项 | 值 |
| --- | --- |
| Run ID | `43611085835` |
| 对应提交 | `23d8fd7`（`v1.0.5` 标签提交；含 1.0.5 版本号、notes 与版本索引） |
| 触发 | push tag `v1.0.5` |
| 结论 | **success**，job 耗时 34 秒（run 36 秒） |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.5` 并写入 `$GITHUB_OUTPUT` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.5` 去前缀后与版本 `1.0.5` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.5` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.5.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.5` 开头（962 字符），与仓库内 notes 逐字一致，含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.5.jar`（571,148 字节）、`tweakify-mod-1.0.5-sources.jar`（416,422 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43610999806`）同为 success。

> 备注：本次 `git push origin main` 连续失败 8 次（`Connection was reset` / `Failed to connect` / `Empty reply`，含 HTTP/1.1 尝试），
> 第 9 次成功；tag 推送重试 1 次。GitHub 443 服务间歇性不可用，不影响版本一致性校验。
>
> v1.0.5 发布后对 notes 的措辞修正（状态管理分述与耗时口径）已用 `gh release edit v1.0.5 --notes-file docs/release-notes/v1.0.5.md` 同步线上；
> 重跑该标签的 release 工作流会用 tag 提交中的旧版正文覆盖，届时需重新 edit。

## 验证记录：v1.0.6 发布

本次无 `release.yml` 改动，版本一致性校验链与 notes 机制按既有流程再次通过；本次同时升级 Fabric Loader 到 0.20.1：

| 项 | 值 |
| --- | --- |
| Run ID | `43625965816` |
| 对应提交 | `54bbfd1`（`v1.0.6` 标签提交；含 notes 与版本索引；Loader 0.20.1 与版本号在父提交 `e9cffbf`） |
| 触发 | push tag `v1.0.6` |
| 结论 | **success**，job 耗时 49 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.6` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.6` 去前缀后与版本 `1.0.6` 一致 | ✅ |
| `build` | Fabric Loader 0.20.1 依赖解析与全量测试通过 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.6` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.6.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.6` 开头（1,004 字符），与仓库内 notes 逐字一致，含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.6.jar`（571,149 字节）、`tweakify-mod-1.0.6-sources.jar`（416,422 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43625946723`）同为 success。

## 验证记录：v1.0.7 发布

本次无 `release.yml` 改动；版本一致性校验链与 notes 机制按既有流程再次通过（首发为功能扩展版本，新增 34 个配置项）：

| 项 | 值 |
| --- | --- |
| Run ID | `43640805357` |
| 对应提交 | `735bbbc`（`v1.0.7` 标签提交；含 1.0.7 版本号、notes 与版本索引） |
| 触发 | push tag `v1.0.7` |
| 结论 | **success**，job 耗时 59 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.7` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.7` 去前缀后与版本 `1.0.7` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.7` 与版本一致（本地同步核对过） | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.7.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.7` 开头（2,902 字符），与仓库内 notes 逐字一致，含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.7.jar`（636,499 字节）、`tweakify-mod-1.0.7-sources.jar`（474,293 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43640792189`）同为 success。

> 备注：本次 `git push origin main` 直连连续失败 7 次（`Empty reply from server`），
> 改用本地代理 `-c http.proxy=http://127.0.0.1:26561` 后首次成功；tag 推送同样走代理。

## 验证记录：v1.0.8 发布

本次无 `release.yml` 改动；版本一致性校验链与 notes 机制按既有流程再次通过（定位栏与参考实现对齐的收尾版本，含配置版本 3 → 4 迁移）：

| 项 | 值 |
| --- | --- |
| Run ID | `43642263037` |
| 对应提交 | `8c9818c`（`v1.0.8` 标签提交；含 1.0.8 版本号、notes 与版本索引） |
| 触发 | push tag `v1.0.8` |
| 结论 | **success**，job 耗时 52 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过**（CI 界面另含 `Set up job`、3 个 `Post` 步骤与 `Complete job`，共 16 行，均通过） |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.0.8` | ✅ |
| `verify tag matches mod version` | 标签 `v1.0.8` 去前缀后与版本 `1.0.8` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.0.8` 与版本一致（本地同步核对过） | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.0.8.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.0.8` 开头（1,076 字符），与仓库内 notes 逐字一致，含 compare 链接 | ✅ |

产物：`tweakify-mod-1.0.8.jar`（640,361 字节）、`tweakify-mod-1.0.8-sources.jar`（477,208 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43642250116`）同为 success。

> 备注：本次沿用本地代理 `-c http.proxy=http://127.0.0.1:26561`，main 与标签均首次推送成功。

## 验证记录：v1.1.0 发布

本次无 `release.yml` 改动；版本一致性校验链与 notes 机制按既有流程再次通过（七轮子设置补齐与全局开关审计）：

| 项 | 值 |
| --- | --- |
| Run ID | `43738777693` |
| 对应提交 | `2262ee0`（`v1.1.0` 标签提交；含 1.1.0 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.0` |
| 结论 | **success**，job 耗时 46 秒（run 50 秒） |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.0` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.0` 去前缀后与版本 `1.1.0` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.0` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.0.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文与仓库内 notes 逐字一致（1,733 字符），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.0.jar`（720,879 字节）、`tweakify-mod-1.1.0-sources.jar`（545,187 字节）。

同时 main 分支 push 触发的 `build.yml`（run `43738760447`）同为 success。

> 备注：本次运行仍使用 `action-gh-release@v2`，Actions 出现 Node.js 20 弃用注解；发布后已将 action 升级到
> `@v3`（v3.0.4，`runs.using: node24`，提交 `414f1ec`），inputs 与 permissions 兼容，下一次标签发布验证该注解消失。
> 升级后的 main 构建（run `43741195244`）为 success。

## 验证记录：v1.1.1 发布

本次无 `release.yml` 改动；首次验证 `action-gh-release@v3`（Node.js 24），也是 12 项原版修复与 T5 缓存的集中发布：

| 项 | 值 |
| --- | --- |
| Run ID | `44158909471` |
| 对应提交 | `06d7639`（`v1.1.1` 标签提交；含 1.1.1 版本号、notes 与探针台账） |
| 触发 | push tag `v1.1.1` |
| 结论 | **success**，job 耗时 59 秒（run 1 分 04 秒） |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.1` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.1` 去前缀后与版本 `1.1.1` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.1` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.1.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文与仓库内 notes 逐字一致（2,576 字节），含 compare 链接；使用 `action-gh-release@v3` | ✅ |

产物：`tweakify-mod-1.1.1.jar`（779,451 字节）、`tweakify-mod-1.1.1-sources.jar`（584,196 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44158881891`）同为 success。

> 备注：本次 main 与标签均通过本地代理首次推送成功；release 页面正文在 PowerShell 控制台显示为乱码属于终端解码问题，GitHub API 读取的正文与仓库 notes 完全一致。

## 验证记录：v1.1.2 发布

本次为 1.1.1 启动崩溃的热修复版本，同时纳入 MC-8187；`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44341351180` |
| 对应提交 | `4dc78bc`（`v1.1.2` 标签提交；含 1.1.2 版本号与 notes） |
| 触发 | push tag `v1.1.2` |
| 结论 | **success**，job 耗时 1 分 12 秒（run 1 分 16 秒） |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.2` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.2` 去前缀后与版本 `1.1.2` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.2` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.2.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文与仓库内 notes 逐字一致（1,275 字节），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.2.jar`（782,023 字节）、`tweakify-mod-1.1.2-sources.jar`（586,682 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44341330348`）同为 success。

> 备注：v1.1.1 的启动崩溃由客户端 Mixin 配置缺少 `mixinextras.minVersion` 导致；1.1.2 已在配置中声明 `0.5.5` 并新增 `MixinConfigTest`。

## 验证记录：v1.1.3 发布

本次为平滑滚动与搜索高亮的功能版本（配置界面 / 创造物品栏 / 聊天栏 / Mod Menu），`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44505264128` |
| 对应提交 | `3976a30`（`v1.1.3` 标签提交；含 1.1.3 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.3` |
| 结论 | **success**，job 耗时 1 分 06 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.3` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.3` 去前缀后与版本 `1.1.3` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.3` 与版本一致（本地同步核对过） | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.3.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.1.3` 开头（36 行），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.3.jar`（817,086 字节）、`tweakify-mod-1.1.3-sources.jar`（620,480 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44505056428`，1 分 13 秒）同为 success。

> 备注：本次 main（`99be866..3976a30`）与标签均通过本地代理 `-c http.proxy=http://127.0.0.1:26561` 首次推送成功；
> 本地构建的 jar 体积（821,265 字节）与 CI 产物略有差异，属打包时间戳差异，不影响版本校验。

## 验证记录：v1.1.4 发布

本次为配置与存档收藏系统的功能版本，并包含全部配置项名称优化与三语言同步；`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44590104794` |
| 对应提交 | `03fa393`（`v1.1.4` 标签提交；含 1.1.4 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.4` |
| 结论 | **success**，job 耗时 53 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.4` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.4` 去前缀后与版本 `1.1.4` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.4` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.4.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.1.4` 开头（33 行），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.4.jar`（845,425 字节）、`tweakify-mod-1.1.4-sources.jar`（643,266 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44590074970`，约 57 秒）同为 success。

> 备注：本次 main（`82de0fe..03fa393`）与标签均通过本地代理 `-c http.proxy=http://127.0.0.1:26561` 推送成功；
> 本地构建的 jar 体积（849,656 字节）与 CI 产物略有差异，属打包时间戳差异，不影响版本校验。

## 验证记录：v1.1.5 发布

本次为窄窗口下配置界面收藏星与重置按钮重叠的补丁修复，`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44593915842` |
| 对应提交 | `ac46309`（`v1.1.5` 标签提交；含 1.1.5 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.5` |
| 结论 | **success**，job 耗时 1 分 05 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.5` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.5` 去前缀后与版本 `1.1.5` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.5` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.5.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.1.5` 开头（23 行），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.5.jar`（845,419 字节）、`tweakify-mod-1.1.5-sources.jar`（643,389 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44593884529`，约 1 分 05 秒）同为 success。

> 备注：本次 main（`7b05164..ac46309`）与标签均通过本地代理 `-c http.proxy=http://127.0.0.1:26561` 推送成功；
> 本地构建的 jar 体积（849,650 字节）与 CI 产物略有差异，属打包时间戳差异，不影响版本校验。

## 验证记录：v1.1.6 发布

本次为技术债台账复查与归档的维护版本（无运行时行为变更），`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44597167936` |
| 对应提交 | `71ced4c`（`v1.1.6` 标签提交；含 1.1.6 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.6` |
| 结论 | **success**，job 耗时 1 分 03 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.6` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.6` 去前缀后与版本 `1.1.6` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.6` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.6.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.1.6` 开头（23 行），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.6.jar`（845,419 字节）、`tweakify-mod-1.1.6-sources.jar`（643,389 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44597142672`，约 1 分 20 秒）同为 success。

> 备注：本次 main（`8035492..71ced4c`）与标签均通过本地代理 `-c http.proxy=http://127.0.0.1:26561` 推送成功；
> 本地构建的 jar 体积（849,650 字节）与 CI 产物略有差异，属打包时间戳差异，不影响版本校验。

## 验证记录：v1.1.7 发布

本次为测试基础设施维护版本（T4 升级为 Fabric Loader 真实 Mixin 集成测试，无运行时行为变更），`release.yml` 无改动：

| 项 | 值 |
| --- | --- |
| Run ID | `44721714448` |
| 对应提交 | `2c1fed4`（`v1.1.7` 标签提交；含 1.1.7 版本号、notes 与版本索引） |
| 触发 | push tag `v1.1.7` |
| 结论 | **success**，job 耗时 1 分 15 秒 |
| 步骤结果 | `release.yml` 定义的 **11 个步骤全部通过** |

关键验证点：

| 步骤 | 验证内容 | 结果 |
| --- | --- | --- |
| `resolve mod version` | 从 `gradle.properties` 解析出 `1.1.7` | ✅ |
| `verify tag matches mod version` | 标签 `v1.1.7` 去前缀后与版本 `1.1.7` 一致 | ✅ |
| `verify jar mod version` | jar 内 `fabric.mod.json` 的 `1.1.7` 与版本一致 | ✅ |
| `resolve release notes` | 找到非空的 `docs/release-notes/v1.1.7.md`，`body_path` 生效 | ✅ |
| `create GitHub Release` | 正文以手写标题 `## Tweakify Mod 1.1.7` 开头（25 行），含 compare 链接 | ✅ |

产物：`tweakify-mod-1.1.7.jar`（845,484 字节）、`tweakify-mod-1.1.7-sources.jar`（643,544 字节）。

同时 main 分支 push 触发的 `build.yml`（run `44721694211`，约 1 分 21 秒）同为 success，CI 中 `loaderTest` 集成用例同步通过。

> 备注：本次 main（`2d8cb87..2c1fed4`）与标签均通过本地代理 `-c http.proxy=http://127.0.0.1:26561` 推送成功；
> 本地构建的 jar 体积（849,715 字节）与 CI 产物略有差异，属打包时间戳差异，不影响版本校验。

## 已知但决定不改的点

| # | 位置 | 内容 | 不修理由 |
| --- | --- | --- | --- |
| L1 | `release.yml` 的 `find` 行 | ~~`find ... 2>/dev/null` 让 ENOENT 与 EACCES 无法区分~~ **已修（`c1d1a8c`）**：去掉 `2>/dev/null` 并新增 `[ ! -d ]` 预检，拆为「目录不存在」「枚举失败」两条注解 | 已完成，保留作历史记录 |
| L2 | `release.yml` 的 `JAR=$(printf ... \| head -n1)` | ~~保留了管道路径~~ **已修（`5ea1ab9`）**：改为纯 bash 参数展开 `${JAR_LIST%%$'\n'*}`，无子进程、无 SIGPIPE 面 | 已完成，保留作历史记录 |
| L3 | `release.yml` 的 `No main jar found in build/libs` 与 `Version mismatch:` 两条注解 | `No main jar found` 为英文、其余中文；`"version": ""` 的 jar 会归因为 mismatch | 既有文案；空 version 的 jar 现实中不会出现 |
| L4 | `release.yml` 的 `create GitHub Release` | ~~`action-gh-release@v2`（node20）触发 Node.js 20 弃用注解~~ **已修（`414f1ec`）**：升级 v3（v3.0.4，`runs.using: node24`），inputs 与 permissions 兼容 | 已完成，保留作历史记录 |
| H1 | 3 个 Mixin 中的 8 个 `require = 0` 注入点 | **已补齐（58/58）**：`LevelRendererOutlineStyleMixin`×1、`VillagerMixin`×6、`mixin/modmenu/ModsScreenScrollMixin`×1 均已配 `HookProbe` 探针，全仓 58 个注入点 1:1 覆盖 | 已完成，保留作历史记录 |
| H2 | `release.yml` 的 jar 读取分支 | 内容问题的三种原因（无清单/损坏/缺字段）合并为一条注解 | stderr 被刻意收敛；如需细分可改为落临时文件后 `cat` |

## 下次发布清单

1. 提升 `gradle.properties` 的 `version`，同步 README 的版本表；
2. 在 `docs/release-notes/` 写好 `<新标签>.md`，**并先提交推送到 main**；
3. 打 annotated 标签：`git tag -a vX.Y.Z -m "Tweakify Mod X.Y.Z"`；
4. `git push origin vX.Y.Z`；
5. 在 Actions 查看 release 运行：`resolve mod version` → `verify tag matches mod version` → `build` → `verify jar mod version` → `resolve release notes` → `create GitHub Release` 全绿即完成。

## 发版前检查清单

适用于 `a5dd88d` 之后的版本。逐项勾选后即可打标签；发现不适用或已失效的条目，顺手更新本节。

### 一、版本与文档（打标签前完成）

- [ ] `gradle.properties` 的 `version` 改为新版本号；
- [ ] `README.md` 版本表同步（`| Tweakify Mod | X.Y.Z |`）；
- [ ] `docs/release-notes/vX.Y.Z.md`：一句话概述 → 按改动主题分节（如 性能 / 测试 / 兼容性 / 构建与发布）→ 末尾 `compare/v上一个...vX.Y.Z` 链接；
- [ ] 若本次改了 Fabric Loader 或依赖版本：同步 `gradle.properties`、`src/main/resources/fabric.mod.json` 的 `depends` 与 README 环境要求表（v1.0.6 的三处同步即此情形）；
- [ ] 若本次改了 Mixin 注入点：`docs/mixin-probes.md` 的覆盖数、字段数、基线一并同步。

### 二、时序（最容易踩的坑）

- [ ] notes 必须在**打标签之前**合入 main——tag 触发时 checkout 的是 tag 指向的提交，晚于 tag 的 notes 不会被看到；
- [ ] 顺序：改版本号 + notes → 提交推送 main → 打 annotated 标签 → 推送标签 → 等 CI。

### 三、CI 首次验证

> `c1d1a8c` 之后的 CI 改动（6 类归因、参数展开取首行、无 head 预检）已在 v1.0.3 发布中首次生效并通过
> （run `43543826316`，详见「验证记录」）；后续发布按本节逐项核对即可。

- [ ] 6 类归因首次生效：正常构建下不会触发枚举失败分支，确认步骤名与顺序与本文档一致即可；
- [ ] 参数展开取首行（`${JAR_LIST%%$'\n'*}`）与无 head 预检通过；
- [ ] `verify tag matches mod version` 日志中 `tag_version` 与 `gradle_version` 一致；
- [ ] `verify jar mod version` 日志中 `jar_version` 等于版本号。

### 四、发布后核对

- [ ] Release 正文以手写标题 `## Tweakify Mod X.Y.Z` 开头（非自动生成的 Full Changelog 单行）；
- [ ] 产物：`tweakify-mod-X.Y.Z.jar` + `sources.jar`，文件名与版本一致；
- [ ] 更新本文档：基线锚点 → main 上最后一次修改 `release.yml` 的提交；「验证记录」追加本次 run 的 ID / 耗时 / 步骤结果 / 产物大小；
- [ ] release 运行无 Node.js 20 弃用注解（`action-gh-release` v3 / node24）；
- [ ] 若本次改动了 `release.yml`：核对 6 类表与注解文本；
- [ ] 「已知不改」清单：L3/H2 仍在，若触及 jar 读取分支可顺带处理 H2。

### 备注

- 打标签用 annotated（`git tag -a`），消息格式 `Tweakify Mod X.Y.Z`；
- 推送遇到 `Connection was reset` 时重试即可（本仓库多次出现，通常 2–3 次内成功）；
- 提交信息文件用 Write 工具生成，避免 PowerShell `Set-Content -Encoding UTF8` 引入 BOM。

## 相关文件

| 文件 | 作用 |
| --- | --- |
| `.github/workflows/release.yml` | 发布流水线本体 |
| `docs/release-notes/README.md` | 发布说明文件的命名与提交时序约定 |
| `docs/release-notes/v1.0.1.md` | 1.0.1 的发布正文 |
| `gradle.properties` | `version` 单一来源（标签与 jar 都以它为准） |
| `docs/tech-debt.md` | 技术债台账（测试覆盖与性能取舍类，T1–T4；条目状态以该文件为准） |
| `docs/t3-profiler-checklist.md` | T3 实机 profiler 采集与判定（T1 复用其 A/B/C 场景） |
