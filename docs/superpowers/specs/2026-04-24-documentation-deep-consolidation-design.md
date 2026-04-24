# 文档深度整合设计

> 日期：2026-04-24
> 范围：`README.md`、`AGENTS.md`、`docs/*.md`、`docs/archive`、`spring-boot-iot-ui/docs`、`docs/superpowers/specs`、`docs/superpowers/plans`
> 目标：把当前项目文档从“过程记录堆叠”收口为“权威入口 + 当前总结 + 过程证据索引 + 历史归档”的可维护体系。

## 1. 背景

当前主仓库受 Git 管理的 Markdown 文件约 `348` 个，其中 `docs/superpowers/specs` 与 `docs/superpowers/plans` 合计约 `270` 个，已经成为主要的过程文档堆。`docs/` 根目录当前已有文档操作系统、白名单和 `scripts/docs/check-topology.mjs`，且拓扑校验通过，因此本次整合不应绕过既有治理规则。

本轮不纳入 `.worktrees` 下的 Markdown 副本；它们属于隔离工作树，不是主仓库文档真相。

## 2. 设计目标

1. 日常接手时，入口文档只回答“现在是什么状态、该读哪里、不能做什么”。
2. 项目负责人能在一处看到“已完成、未完成、下一步开展什么”。
3. `docs/superpowers/specs` / `plans` 继续保留为过程证据，但不再进入默认阅读路径。
4. 旧 UI 子项目规划与当前 `/products` 真相不再互相冲突。
5. 所有迁移、合并和删除都可被 `node scripts/docs/check-topology.mjs` 校验。

## 3. 分层模型

### 3.1 权威阅读层

保留并压缩以下入口文档职责：

- `README.md`：项目总入口、当前交付基线摘要、启动与验收入口、最小阅读路径。
- `AGENTS.md`：编码助手规则、分支治理、真实环境规则、文档维护规则。
- `docs/README.md`：文档操作系统首页、权威职责矩阵、变更触发矩阵。

详细事实回写到对应权威文档：

- `docs/01-系统概览与架构说明.md`：系统定位、模块边界、固定主链路。
- `docs/02-业务功能与流程说明.md`：业务域、页面流程、产品/设备/风险/治理语义。
- `docs/03-接口规范与接口清单.md`：接口、鉴权、分页、OpenAPI 口径。
- `docs/04-数据库设计与初始化数据.md`：schema registry、init 脚本、治理 registry。
- `docs/05-自动化测试与质量保障.md` 与 `docs/真实环境测试与验收手册.md`：测试策略、真实环境验收。
- `docs/06-前端开发与CSS规范.md` 与 `docs/15-前端优化与治理计划.md`：前端规范、页面治理、共享组件约束。
- `docs/16-阶段规划与迭代路线图.md`：下一阶段主线。
- `docs/19-第四阶段交付边界与复验进展.md`：Phase 4 边界与复验状态。
- `docs/21-业务功能清单与验收标准.md`：交付能力矩阵与验收标准。
- `docs/08-变更记录与技术债清单.md`：当前有效变更摘要、技术债、文档治理结论。

### 3.2 当前总结层

新增或强化一个“全盘总结”入口，优先放在 `docs/16-阶段规划与迭代路线图.md` 或新增 `docs/18-当前工作全盘总结.md`。若新增根目录活跃文档，必须同步更新拓扑白名单和 `docs/README.md`。

总结固定回答：

- 当前稳定基线。
- 已完成能力。
- 部分完成或仍需复验能力。
- 明确未完成能力。
- 下一轮建议主线。
- 不纳入本轮的事项。

推荐优先不新增根目录编号文档，而是在 `docs/16` 中增加“当前全盘总结”章节，并在 `docs/README.md` 建立直达入口；若内容膨胀超过阶段规划职责，再拆成新的编号权威文档。

### 3.3 过程证据层

新增 `docs/superpowers/README.md`，作为 `specs` 与 `plans` 的索引和状态总览。索引按主题分组：

- 产品定义与契约治理
- 对象洞察与 telemetry
- 无代码接入与协议治理
- 设备资产与能力操作
- 风险闭环与治理控制面
- 质量工场与真实环境验收
- 前端治理与页面结构
- 数据库 schema 与治理 registry
- 智能助手协作与文档治理

每个条目至少包含：

- 文件路径
- 类型：`design` 或 `plan`
- 状态：`已落地`、`部分落地`、`计划中`、`已被主文档吸收`、`历史参考`
- 对应权威文档
- 简短说明

本轮不批量删除 `specs/plans` 正文；只有当某个过程文件已经完整被主文档吸收、且有归档索引可追溯时，才允许后续单独提出删除。

### 3.4 历史归档层

继续使用 `docs/archive/` 承接历史正文和完整快照。新增归档时同步更新：

- `docs/archive/README.md`
- `docs/08-变更记录与技术债清单.md` 的文档归档处理结论
- `docs/README.md` 的文档分层和直达入口

旧文档如果仍有有效事实，先把事实合并到权威文档，再把原文移入 archive 或标记为历史参考。

## 4. UI 子项目文档处理

`spring-boot-iot-ui/docs/24-product-definition-evolution-plan.md`、`25-product-detail-active-metrics.md`、`26-product-definition-implement-status.md`、`27-pending-implementation-details.md` 与当前产品定义中心真实路由和治理模型存在明显时效差异。

处理原则：

1. 当前仍有效的产品定义、对象洞察、前端治理事实合并到 `docs/02`、`docs/06`、`docs/15`、`docs/21`。
2. 旧规划正文迁入 `docs/archive/`，或在 `spring-boot-iot-ui/docs/README.md` 中集中标为历史参考。
3. 修正失效链接，例如 `spring-boot-iot-ui/README.md` 中指向旧 `docs/13-frontend-debug-console.md` 的路径。
4. 子项目 README 只保留启动、联调、技术栈和指向主文档的链接，不再维护独立产品路线图。

## 5. 删除与合并规则

允许删除：

- 已迁入 `docs/archive/` 且索引已更新的旧过程文档。
- 子项目中已被主文档吸收且继续保留会误导当前事实的旧规划文档。
- 已失效的短跳转或重复入口，但必须先通过拓扑校验确认不会破坏链接。

不允许删除：

- `schema` 或 `schema-governance` 生成物对应的文档，除非先回写 registry 并重新渲染。
- `docs/archive` 中作为完整快照保存的历史证据。
- 当前仍被 `README.md`、`AGENTS.md`、`docs/README.md` 或主文档引用的文件。
- `.worktrees` 下的副本文档；它们不属于本轮主仓库治理对象。

## 6. 验证策略

文档整合完成后至少执行：

```bash
node scripts/docs/check-topology.mjs
```

如涉及链接迁移，还需执行：

```bash
rg -n "被删除或迁移的文件名" README.md AGENTS.md docs spring-boot-iot-ui -g '*.md'
```

如涉及 schema 生成附录，不在本轮手改，应改走 registry 渲染链。

## 7. 非目标

- 不改业务代码、接口、schema 或前端页面。
- 不重新设计文档体系命名规则。
- 不批量删除 `docs/superpowers/specs` / `plans` 正文。
- 不处理 `.worktrees` 副本文档。
- 不把帮助中心当成新的研发文档真相源。

## 8. 预期结果

完成实施后，项目应具备：

1. 更短、更清晰的入口阅读路径。
2. 一份可维护的当前工作全盘总结。
3. 一份 `docs/superpowers` 过程证据索引。
4. UI 子项目旧规划不再覆盖当前产品定义中心事实。
5. 文档拓扑校验继续通过。
