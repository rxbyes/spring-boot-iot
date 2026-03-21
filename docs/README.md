# 文档总览（核心入口）

更新时间：2026-03-22

本目录已重构为“核心文档最小集 + 历史资料归档集”两层结构。2026-03-22 起，帮助中心联动治理的来源映射也统一收口到本文件。目标是让接手研发、GPT 与帮助中心内容维护都能以最少扫描成本建立准确系统认知。

## 1. 最小阅读集（GPT/接手研发）

默认只读以下 5 个文件，即可建立系统级认知：

1. [01-系统概览与架构说明.md](./01-系统概览与架构说明.md)
2. [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)
3. [03-接口规范与接口清单.md](./03-接口规范与接口清单.md)
4. [04-数据库设计与初始化数据.md](./04-数据库设计与初始化数据.md)
5. [07-部署运行与配置说明.md](./07-部署运行与配置说明.md)

适用场景：
- 新成员首次接手
- GPT 快速进入编码上下文
- 排障前建立全局认知

## 2. 按任务补充阅读

在“最小阅读集”基础上按任务补充：

- 测试与验收：补读 [05-自动化测试与质量保障.md](./05-自动化测试与质量保障.md)、[test-scenarios.md](./test-scenarios.md)、[21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md)
- 前端开发：补读 [06-前端开发与CSS规范.md](./06-前端开发与CSS规范.md)、[15-frontend-optimization-plan.md](./15-frontend-optimization-plan.md)
- MQTT / 协议接入：补读 [05-protocol.md](./05-protocol.md)、[14-mqttx-live-runbook.md](./14-mqttx-live-runbook.md)
- Phase 4 交付边界：补读 [19-phase4-progress.md](./19-phase4-progress.md)、[21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md)
- 历史冲突与技术债：补读 [08-变更记录与技术债清单.md](./08-变更记录与技术债清单.md)

## 3. 提示模板工具

如果你需要把当前项目直接交给 GPT / Codex 类模型，可使用：

- [09-GPT接管提示模板.md](./09-GPT接管提示模板.md)

说明：
- `09` 是工具文档，不属于最小必读集
- 它的作用是复用一套固定提示词，减少每次手工组织上下文的成本

## 4. 帮助中心联动治理

### 4.1 四类标签

- `帮助中心来源`：可以提炼进 `/api/system/help-doc/**` 的业务/技术/FAQ 消费稿。
- `内部权威资料`：研发、运维、测试和治理继续直接使用的主资料，不直接原文搬进帮助中心。
- `归档历史`：仅用于追溯旧路线、旧说明或兼容旧链接，不再作为当前帮助中心来源。
- `不入帮助中心`：模板、技能说明等工具型资料，不面向业务消费。

### 4.2 当前盘点结果

- `帮助中心来源`
  - [01-系统概览与架构说明.md](./01-系统概览与架构说明.md)
  - [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)
  - [03-接口规范与接口清单.md](./03-接口规范与接口清单.md)
  - [05-protocol.md](./05-protocol.md)
  - [07-部署运行与配置说明.md](./07-部署运行与配置说明.md)
  - [14-mqttx-live-runbook.md](./14-mqttx-live-runbook.md)
  - [21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md)
  - [README.md](../README.md)
- `内部权威资料`
  - [04-数据库设计与初始化数据.md](./04-数据库设计与初始化数据.md)
  - [05-自动化测试与质量保障.md](./05-自动化测试与质量保障.md)
  - [06-前端开发与CSS规范.md](./06-前端开发与CSS规范.md)
  - [08-变更记录与技术债清单.md](./08-变更记录与技术债清单.md)
  - [09-GPT接管提示模板.md](./09-GPT接管提示模板.md)
  - [15-frontend-optimization-plan.md](./15-frontend-optimization-plan.md)
  - [19-phase4-progress.md](./19-phase4-progress.md)
  - [22-automation-test-issues-20260316.md](./22-automation-test-issues-20260316.md)
  - [device-simulator.md](./device-simulator.md)
  - [protocol-examples.md](./protocol-examples.md)
  - `docs/appendix/*.md`
  - `docs/skills/**/*.md`
- `归档历史`
  - `docs/archive/*.md`
  - [00-overview.md](./00-overview.md)
  - [01-architecture.md](./01-architecture.md)
  - [02-module-structure.md](./02-module-structure.md)
  - [03-database.md](./03-database.md)
  - [04-api.md](./04-api.md)
  - [06-thing-model.md](./06-thing-model.md)
  - [07-message-flow.md](./07-message-flow.md)
  - [10-deploy.md](./10-deploy.md)
  - [12-change-log.md](./12-change-log.md)
  - [14-mqtt-live-runbook.md](./14-mqtt-live-runbook.md)
- `不入帮助中心`
  - `docs/template/*.md`

说明：
- 上述标签只用于帮助中心联动治理，不改变这些文档本身的研发、测试或归档用途。
- 根目录 [README.md](../README.md) 虽不在 `docs/` 目录下，但当前仍作为 FAQ 高频口径补充来源。

### 4.3 首批 13 篇映射表

| 帮助中心文章 | 分类 | 来源文档 | 适用角色 | 关联页面 |
|---|---|---|---|---|
| 产品与设备建档指南 | `business` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[03-接口规范与接口清单.md](./03-接口规范与接口清单.md) | `BUSINESS_STAFF`、`MANAGEMENT_STAFF` | `/products`、`/devices` |
| 告警确认、抑制与关闭操作 | `business` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md) | `BUSINESS_STAFF`、`MANAGEMENT_STAFF` | `/alarm-center` |
| 事件派工、接收、处理、完结流程 | `business` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md) | `BUSINESS_STAFF`、`MANAGEMENT_STAFF` | `/event-disposal` |
| 风险对象、阈值策略、联动预案配置说明 | `business` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[03-接口规范与接口清单.md](./03-接口规范与接口清单.md) | `BUSINESS_STAFF`、`MANAGEMENT_STAFF` | `/risk-point`、`/rule-definition`、`/linkage-rule`、`/emergency-plan` |
| 运营分析中心指标查看说明 | `business` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[21-business-functions-and-acceptance.md](./21-business-functions-and-acceptance.md) | `BUSINESS_STAFF`、`MANAGEMENT_STAFF` | `/report-analysis` |
| HTTP 上报与链路验证中心使用说明 | `technical` | [03-接口规范与接口清单.md](./03-接口规范与接口清单.md)、[07-部署运行与配置说明.md](./07-部署运行与配置说明.md) | `OPS_STAFF`、`DEVELOPER_STAFF`、`SUPER_ADMIN` | `/reporting` |
| MQTT Topic 规范与 `mqtt-json / $dp` 兼容说明 | `technical` | [05-protocol.md](./05-protocol.md)、[14-mqttx-live-runbook.md](./14-mqttx-live-runbook.md) | `OPS_STAFF`、`DEVELOPER_STAFF`、`SUPER_ADMIN` | `/reporting`、`/message-trace` |
| TraceId、链路追踪台、异常观测台排障说明 | `technical` | [01-系统概览与架构说明.md](./01-系统概览与架构说明.md)、[07-部署运行与配置说明.md](./07-部署运行与配置说明.md) | `OPS_STAFF`、`DEVELOPER_STAFF`、`SUPER_ADMIN` | `/message-trace`、`/system-log` |
| 真实环境启动、环境变量与依赖检查说明 | `technical` | [07-部署运行与配置说明.md](./07-部署运行与配置说明.md)、[README.md](../README.md) | `OPS_STAFF`、`DEVELOPER_STAFF`、`SUPER_ADMIN` | `/reporting`、`/system-log` |
| FAQ：产品和设备有什么区别 | `faq` | [README.md](../README.md)、[02-业务功能与流程说明.md](./02-业务功能与流程说明.md) | 默认所有登录用户 | `/products`、`/devices` |
| FAQ：为什么我看不到某个页面或帮助文档 | `faq` | [02-业务功能与流程说明.md](./02-业务功能与流程说明.md)、[03-接口规范与接口清单.md](./03-接口规范与接口清单.md) | 默认所有登录用户 | 通用 |
| FAQ：通知中心与帮助中心怎么用 | `faq` | [README.md](../README.md)、[02-业务功能与流程说明.md](./02-业务功能与流程说明.md) | 默认所有登录用户 | 通用 |
| FAQ：为什么会出现 `401`、无权限或系统内容缺表提示 | `faq` | [03-接口规范与接口清单.md](./03-接口规范与接口清单.md)、[07-部署运行与配置说明.md](./07-部署运行与配置说明.md) | 默认所有登录用户 | 通用 |

### 4.4 帮助文章模板与维护规则

- 正文统一按以下模板组织：`标题`、`适用角色`、`适用页面`、`使用场景`、`操作步骤`、`结果判断`、`常见问题`、`延伸阅读`。
- 帮助中心正文保持“短文档消费”定位，不直接搬运数据库设计、升级脚本、内网地址、历史路线图和排障内部细节。
- `keywords` 建议控制在 `3~6` 个，优先使用页面名、核心对象名和高频检索词。
- `visible_role_codes` 默认按角色成组填写：业务类优先 `BUSINESS_STAFF,MANAGEMENT_STAFF`，技术类优先 `OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN`，FAQ 默认留空表示所有登录用户可见。
- `related_paths` 优先绑定真实菜单路径，帮助中心消费端再结合 `currentPath` 提升当前页面资料排序。
- 后续若业务/API/配置变更，先更新权威文档，再评估是否同步更新 `/help-doc` 的消费稿。

## 5. 文档分层

- 核心层：`01`~`08`，用于日常开发、排障、接口联调与版本演进。
- 历史归档层：`docs/archive/`，用于追溯旧版说明、阶段路线图和过程文档。
- 附录层：`docs/appendix/*.md`，用于代码风格、术语与命名约定。

历史资料入口：

- [archive/README.md](./archive/README.md)

## 6. 权威口径与冲突处理

1. 运行事实以代码与数据库脚本为准。
2. Phase 4 进度以 [19-phase4-progress.md](./19-phase4-progress.md) 为权威。
3. 当“路线图类文档”和“代码现状”冲突时，以代码现状 + 本文档核心集为准，并在 [08-变更记录与技术债清单.md](./08-变更记录与技术债清单.md) 补充冲突记录。
4. Windows 10 共享环境常见工作区示例路径为 `E:\idea\ghatg\spring-boot-iot`；macOS/Linux 以当前仓库实际路径为准。脚本与说明应优先使用“按脚本位置或仓库根目录自动推导”的方式兼容多环境，而不是重新写死绝对路径。
5. 帮助中心是消费层，不替代权威文档；当帮助中心与 `docs/` 冲突时，以 `docs/` 与代码事实为准。

## 7. 标记说明

- `【根据代码推断】`：该结论来自源码/SQL/配置归纳，不是单一文档直接声明。
- `【待确认】`：存在外部环境或产品决策依赖，无法仅凭仓库确认。
- `【文档缺失】`：当前仓库缺少完整说明，建议后续补齐。

## 8. 最小维护规则

- 任何影响行为/API/配置/流程的改动，必须同步更新核心层文档。
- 禁止再新增“平行替代文档”（如 `xxx-v2.md`、`new-xxx.md`）。
- 历史文档的处理（保留/合并后删除/归档）统一记录到 [08-变更记录与技术债清单.md](./08-变更记录与技术债清单.md)。
- 帮助中心文章的新增或改写，必须能追溯到对应的权威来源文档；来源映射优先维护在本文件。
