# 发布说明

本目录存放各版本的发布正文，`release` 工作流在打标签时按文件名自动选用。

## 约定

- **文件名必须与标签名一致**：标签 `vX.Y.Z` 对应本目录下的 `vX.Y.Z.md`；
- **必须在打标签之前提交到 main**：tag 触发时工作流 checkout 的是 tag 指向的提交，晚于 tag 的 notes 不会被看到，会静默回退到自动生成的 release notes（日志中出现一条 warning）；
- **文件不能为空**：空文件同样会触发回退（脚本用 `-s` 判断非空）；
- **正文结构**与既有版本保持一致：一句话概述 → 架构 / 兼容性 / 修复 / 构建与发布 → 末尾附 `compare/v上一个标签...v当前标签` 链接；
- 标签打完后如果补充了 notes，用 `gh release edit <tag> --notes-file docs/release-notes/<tag>.md` 同步到线上 Release。

## 示例：发布 v1.0.2

```bash
# 1) 先写 notes 并提交（必须在 tag 之前）
git add docs/release-notes/v1.0.2.md
git commit -m "docs: 补充 1.0.2 发布说明"
git push origin main

# 2) 再打标签
git tag -a v1.0.2 -m "Tweakify Mod 1.0.2"
git push origin v1.0.2
```
