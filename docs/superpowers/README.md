# Superpowers 过程证据索引

> 文档定位：`docs/superpowers/specs` 与 `docs/superpowers/plans` 的过程证据索引。
> 适用角色：项目负责人、研发、测试、交付负责人、AI 协作助手。
> 权威级别：过程证据索引；不替代 `docs/` 根目录权威文档。
> 上游来源：`docs/superpowers/specs/*.md`、`docs/superpowers/plans/*.md`、`docs/08-变更记录与技术债清单.md`。
> 下游消费：任务追溯、方案复盘、下一轮拆解。
> 更新时间：2026-04-24

本目录保存设计与实施过程证据。日常编码默认不需要逐篇阅读本目录；需要追溯某一轮设计、执行计划或取舍时，先读本索引，再进入对应 `specs` 或 `plans` 文件。

## 1. 状态口径

| 状态 | 含义 |
|---|---|
| 已落地 | 对应能力已进入当前代码或权威文档基线 |
| 部分落地 | 主链路已完成，但仍有复验、扩面或长期治理项 |
| 计划中 | 已有设计或计划，尚未完成主实现 |
| 已被主文档吸收 | 结论已回写权威文档，原文件仅作过程追溯 |
| 历史参考 | 已退出当前执行路径，只保留背景价值 |

## 2. 主题索引

| 主题 | 当前状态 | 代表过程文档 | 对应权威文档 | 当前结论 |
|---|---|---|---|---|
| 产品定义与契约治理 | 部分落地 | `specs/2026-04-19-product-governance-reset-and-products-ia-design.md`、`plans/2026-04-20-product-workbench-polish.md` | `../02-业务功能与流程说明.md`、`../21-业务功能清单与验收标准.md` | `/products` 已拆为主列表和五段详情子路由，契约、映射与版本治理继续在产品定义中心内收口 |
| 对象洞察与 telemetry | 部分落地 | `specs/2026-04-23-object-insight-trend-legacy-identifier-graceful-cleanup-design.md`、`specs/2026-04-23-l3-l4-auto-canonicalization-design.md` | `../02-业务功能与流程说明.md`、`../04-数据库设计与初始化数据.md` | telemetry v2、历史窗口查询和运行态字段归一已进入基线，拓扑角色化洞察仍按后续计划推进 |
| 无代码接入与协议治理 | 部分落地 | `specs/2026-04-18-no-code-device-onboarding-p0-p1-design.md`、`specs/2026-04-18-protocol-governance-browser-expansion-design.md` | `../02-业务功能与流程说明.md`、`../03-接口规范与接口清单.md` | `/device-onboarding` 已形成 intake、模板包和批量编排最小闭环，不代表任意设备已全自动零代码接入 |
| 设备资产与能力操作 | 计划中 | `specs/2026-04-24-device-capability-design.md`、`specs/2026-04-24-device-asset-operation-simplification-design.md` | `../02-业务功能与流程说明.md`、`../21-业务功能清单与验收标准.md` | 设备详情与设备操作职责拆分，能力命令台账进入下一轮实施重点 |
| 风险闭环与治理控制面 | 部分落地 | `specs/2026-04-10-governance-control-plane-domain-execution-design.md`、`specs/2026-04-13-governance-task-view-selector-design.md` | `../19-第四阶段交付边界与复验进展.md`、`../08-变更记录与技术债清单.md` | 控制面承担调度、审计和 replay 收口，领域真相仍由产品、风险与系统域执行 |
| 质量工场与真实环境验收 | 部分落地 | `specs/2026-04-25-quality-factory-governance-consolidation-design.md`、`specs/2026-04-25-quality-factory-result-archive-index-design.md` | `../05-自动化测试与质量保障.md`、`../真实环境测试与验收手册.md` | 质量工场已收口为业务验收台与自动化治理台，覆盖治理与 readiness 已具备，结果归档索引和统一 CI 仍在推进 |
| 前端治理与页面结构 | 部分落地 | `specs/2026-03-31-global-list-workbench-template-rollout-design.md`、`specs/2026-04-20-product-detail-tab-refresh-design.md` | `../06-前端开发与CSS规范.md`、`../15-前端优化与治理计划.md` | 共享列表、抽屉、分页和接入智维页面结构持续收口，页面私有样式仍需长期治理 |
| 数据库 schema 与治理 registry | 已落地 | `specs/2026-04-14-database-schema-governance-design.md`、`specs/2026-04-15-schema-governance-framework-design.md` | `../04-数据库设计与初始化数据.md`、`../08-变更记录与技术债清单.md` | 结构真相源固定为 `schema/`，治理真相源固定为 `schema-governance/` |
| 智能助手协作与文档治理 | 部分落地 | `plans/2026-03-31-iot-business-governance-docs-consolidation-plan.md`、`specs/2026-04-24-documentation-deep-consolidation-design.md` | `../../README.md`、`../08-变更记录与技术债清单.md` | 文档默认阅读路径和过程证据层继续分离，新增或迁移文档必须更新索引 |

## 3. 使用规则

1. 默认接手项目时先读 `../../README.md` 与 `../README.md` 指向的权威主文档，不把本目录作为最小阅读集。
2. 查某项能力为什么这样设计时，按主题进入 `specs`。
3. 查某轮任务如何拆解和验证时，按主题进入 `plans`。
4. 若过程文档结论已经回写主文档，后续维护以主文档为准。
5. 新增 `specs` 或 `plans` 时，同步更新本索引的主题、状态或代表文档。
