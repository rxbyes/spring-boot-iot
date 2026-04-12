# 平台工作台职责与命名优化总路线图

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不重构后端模块边界的前提下，先收口前端工作台的命名、职责边界、角色入口和跨页治理语义；这些优化稳定后，再编写内部培训版产品使用说明书。

**Architecture:** 后端 Maven 模块边界当前总体合理，下一阶段不做“大拆大并”。本轮重点治理前端信息架构，包括一级工作台命名、二级分组、领域页与控制面边界、角色入口、排障路径和权限解释；必要时补少量读侧接口或前端状态契约。由于范围横跨多个相对独立子系统，本文件只作为母路线图，实际实施必须拆成多个子实施计划分批完成。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Pinia、Spring Boot 4、Java 17、现有 `docs/` 文档体系。

---

## 基线判断

- 后端模块职责当前总体合理：`system`、`device`、`message`、`protocol`、`telemetry`、`alarm`、`report` 等边界没有出现必须立刻重构的结构性错误。
- 当前主要问题不在后端分层，而在前端产品语义：命名、分组、入口顺序、控制面和领域页的边界表达还不够稳定。
- `平台治理` 同时承载“传统系统管理”和“治理控制面”并非技术错误，但在用户认知上语义过宽，容易让管理者和客户误以为所有治理动作都应在该分组直接处理。
- `/products` 现在已经不是狭义“产品管理”，而是“产品定义 + 契约治理 + 版本治理 + 风险目录入口”的复合工作台；语义没有错，但当前名称和结构提示不足。
- `接入智维` 的真实排障链路已经存在，但对一线使用者和新接手研发者来说，第一步去哪、第二步看什么，还不够显式。
- `质量工场` 已经完成“业务验收”和“研发自动化”的首轮拆分，但页面命名、受众表达和低权限用户入口控制还可以继续收口。
- 在这些语义收口前直接编写产品使用说明书，文档很快会被后续页面优化打穿，培训材料也会出现“文档按旧语义写、系统按新语义跑”的问题。

## 优先级矩阵

| 优先级 | 工作主题 | 当前问题 | 目标结果 |
| --- | --- | --- | --- |
| P0 | 一级工作台命名与职责基线 | 五大工作台已成型，但角色理解口径未完全冻结 | 让“这个工作台是给谁用、解决什么问题”一眼可读 |
| P0 | `平台治理` 二级分组重构 | 系统管理与治理控制面混在同一语义层 | 保留一个一级入口，但在二级结构显式拆层 |
| P0 | `/products` 工作台语义减负 | 页面承载过多职责，用户易误判它只是产品台账 | 明确升级为“产品定义与契约治理中心” |
| P0 | `接入智维` 排障路径显式化 | 诊断链路存在，但“先去哪”仍依赖经验 | 形成标准诊断决策树和跨页引导 |
| P1 | `质量工场` 角色分层继续收口 | 业务验收和研发工场已拆，但客户认知仍容易混 | 非研发用户默认只看到业务验收消费路径 |
| P1 | 权限可见性与空态解释治理 | 当前“看不到菜单/进得去但无数据”的解释不够友好 | 让角色培训和日常排障有统一解释口径 |
| P2 | 首页与帮助中心培训化表达 | 当前已有帮助/消息体系，但培训导流仍偏散 | 为后续说明书和客户培训提供稳定入口 |

## 不在本轮做的事

- 不重构后端 Maven 模块边界。
- 不新增与现有领域页平行的第二套业务页面。
- 不把治理控制面改造成真实业务写入真相源。
- 不在本轮直接编写最终版产品使用说明书。

## Task 1: 一级工作台命名与职责基线收口（P0）

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/components/AppShell.vue`
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/12-帮助文档与系统内容治理.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 冻结五大一级工作台的单一句式定义，格式统一为“谁来用 + 解决什么问题 + 不负责什么”。
- [ ] 为每个一级工作台补齐固定的角色推荐入口，不再只展示页面集合。
- [ ] 在总览页明确区分“领域工作台”和“控制/支持工作台”，避免用户把所有页面都理解成同一层级。
- [ ] 统一首页、侧边栏、命令面板、帮助文档中的工作台命名，消除“平台治理 / 系统管理 / 系统治理”并行口径。

**验收标准：**
- 管理者、一线使用者、研发者、运维者各自都能在 10 秒内判断自己的默认入口。
- 首页、工作台总览、帮助中心、README、业务文档中不再出现同一能力多种一级命名。
- 新用户在不依赖口头讲解的前提下，能理解五大工作台不是“按技术模块分组”，而是“按业务职责分组”。

## Task 2: `平台治理` 二级分组重构（P0）

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceApprovalView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceSecurityView.vue`
- Modify: `spring-boot-iot-ui/src/views/OrganizationView.vue`
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
- Modify: `spring-boot-iot-ui/src/views/MenuView.vue`
- Modify: `spring-boot-iot-ui/src/views/RegionView.vue`
- Modify: `spring-boot-iot-ui/src/views/DictView.vue`
- Modify: `spring-boot-iot-ui/src/views/ChannelView.vue`
- Modify: `spring-boot-iot-ui/src/views/InAppMessageView.vue`
- Modify: `spring-boot-iot-ui/src/views/HelpDocView.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/12-帮助文档与系统内容治理.md`
- Modify: `docs/13-数据权限与多租户模型.md`

- [ ] 保留 `平台治理` 一级名称，但把二级入口显式分成 `身份与组织`、`系统配置`、`治理控制面`、`安全与审计` 四组。
- [ ] 在 `治理任务台 / 治理运维台 / 治理审批台 / 权限与密钥治理` 中统一补一句边界说明：控制面负责发现、调度、审批、留痕，不直接编辑领域真相源。
- [ ] 在 `组织 / 账号 / 角色 / 菜单 / 区域 / 字典 / 通知 / 帮助 / 审计` 等传统系统管理页，统一补“本页解决什么，不解决什么”的轻量说明。
- [ ] 把“平台治理是否等于系统管理”这个认知问题直接写进帮助中心 FAQ 和培训词典。

**验收标准：**
- 管理者能区分“账号授权配置”与“治理任务推进”是两类页面，而不是同一动作链的前后步骤。
- 客户在首次培训中不会再把控制面页面当作产品合同、风险绑定或规则真相的直接编辑页。
- 任意治理控制面跳转到领域页时，页面能明确回答“为什么跳过来、接下来在哪个领域页执行”。

## Task 3: `/products` 工作台语义减负（P0）

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/productWorkbenchState.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 把 `/products` 的对外语义从“产品定义中心”升级为“产品定义与契约治理中心”，至少在标题、副标题、帮助文案和跳转上下文中体现。
- [ ] 在工作台内部显式分层：`产品主数据`、`关联设备`、`契约字段治理`、`版本台账/风险目录`，避免所有动作都被用户理解成“改产品信息”。
- [ ] 对来自治理任务台、对象洞察、接入诊断链路的深链上下文，统一使用可读标签表达来源目的，而不是只带技术 query。
- [ ] 给客户和项目经理一套非研发表述，避免他们把“字段 compare/apply/版本批次”误解为研发专属概念。

**验收标准：**
- 产品经理和管理者能说清 `/products` 不只是产品台账，而是产品治理真相入口。
- 一线使用者进入 `/products` 后，不会把“契约字段治理”误判成必须由研发才能使用的隐藏功能。
- 从 `治理任务台` 打开待发布合同时，用户能理解自己进入的是“领域执行页”，而不是回到了普通产品列表。

## Task 4: `接入智维` 排障路径显式化（P0）

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/reportWorkbenchState.ts`
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/deviceWorkbenchState.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/真实环境测试与验收手册.md`

- [ ] 把现有接入排障链路固化成统一决策树：`链路验证 -> 链路追踪/异常观测/数据校验 -> 产品或设备治理`。
- [ ] 在 `接入智维总览` 明确告诉用户“什么时候先去模拟上报，什么时候先查 TraceId，什么时候直接去产品/设备治理页”。
- [ ] 统一诊断页到治理页的来源提示文案，固定表达为“发现问题的页面 + 建议核对对象 + 下一步动作”。
- [ ] 对失败归档、异常观测、链路追踪三者的差异补齐一句话解释，避免一线人员凭经验判断。

**验收标准：**
- 运维和研发新人能在 3 分钟内找到一条稳定的标准排障路径。
- 培训材料可以直接复用这条路径，而不需要讲师口头补大量“经验规则”。
- 从任一诊断页回跳到产品或设备页时，页面能告诉用户当前是在修正什么问题，而不是只做机械跳转。

## Task 5: `质量工场` 角色分层继续收口（P1）

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
- Modify: `spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationExecutionView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationInventoryView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationTemplatesView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationPlansView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationHandoffView.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 进一步强化“业务验收消费路径”和“研发自动化维护路径”的二级分层，不再让客户默认看到完整工程资产语义。
- [ ] 对非研发角色收口默认文案，只强调验收包、环境、账号模板、模块范围、通过结论和失败证据。
- [ ] 对研发角色保留 `研发工场 / 执行中心 / 结果与基线中心` 的工程化入口，但避免其语义回流到业务验收入口。
- [ ] 在帮助中心和培训页形成一句固定话术：业务验收看结果，研发工场维护资产，执行中心组织运行，结果中心沉淀证据。

**验收标准：**
- 项目经理、产品、客户验收人员首次进入 `质量工场` 时，不会误以为需要理解模板、计划编排、基线导入等研发概念。
- 研发和测试仍能完整进入底层自动化链路，不因角色分层而损失能力。
- 业务验收培训材料可以独立成册，不需要把研发自动化部分混写进去。

## Task 6: 权限可见性与空态解释治理（P1）

**Files:**
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Modify: `spring-boot-iot-ui/src/stores/permission.ts`
- Modify: `spring-boot-iot-ui/src/components/AppShell.vue`
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
- Modify: `spring-boot-iot-ui/src/views/MenuView.vue`
- Modify: `docs/12-帮助文档与系统内容治理.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 统一“无菜单权限”“可进页面但无按钮权限”“可进页面但无数据权限”三类空态解释模板。
- [ ] 在角色培训材料中固定输出 3 步排查法：先看菜单授权，再看角色按钮，再看组织/租户/数据范围。
- [ ] 为平台帮助中心补齐“为什么我看不到这个页面/数据”的自助说明，而不是只让用户找管理员。
- [ ] 如需后端支持，补充最小读侧权限解释字段，但不改动现有核心鉴权模型。

**验收标准：**
- 一线用户遇到“页面没有/页面空白/列表无数据”时，能先自助判断是菜单、按钮还是数据范围问题。
- 管理者和运维在培训中可以用统一话术解释权限，不再各自发明口径。
- 角色授权和数据范围治理的说明能沉淀到帮助中心和最终说明书中。

## Task 7: 产品说明书编写前置门槛（P0 Gate）

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/12-帮助文档与系统内容治理.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Create: `docs/产品使用说明书-内部培训版.md`

- [ ] 一级工作台命名、二级分组、角色推荐入口全部冻结。
- [ ] `/products`、`/device-access`、`/system-management`、`/quality-workbench` 的培训口径全部稳定。
- [ ] 接入诊断链路、治理控制面边界、权限解释口径全部形成统一文案。
- [ ] 先完成一次管理者、一线使用者、研发者、运维者四类角色的 walkthrough，再启动正式手册写作。

**验收标准：**
- 说明书编写时不再需要反复修改一级/二级目录名。
- 手册里的页面路径、角色入口、排障入口、账号样例和接口说明能直接对应稳定页面。
- 培训讲义与系统真实页面语义一致，不再依赖口头修正。

## 推荐拆分顺序

- [ ] 子计划 A：一级工作台命名与 `平台治理` 二级分组重构
- [ ] 子计划 B：`/products` 工作台语义减负与治理入口重命名
- [ ] 子计划 C：`接入智维` 诊断决策树和跨页引导优化
- [ ] 子计划 D：`质量工场` 角色分层与帮助中心培训化表达
- [ ] 子计划 E：权限可见性与空态解释治理
- [ ] 子计划 F：基于稳定 IA 的内部培训版产品使用说明书

## 说明书启动条件

- 只有当 Task 1 至 Task 4 至少全部完成并验收通过后，才开始编写产品使用说明书。
- 如果 Task 5 或 Task 6 未完成，说明书中必须明确标注为“当前版本仍在优化中的能力”，否则会把临时口径固化进培训材料。
- 说明书编写顺序固定为：先角色入口图，再业务主链路，再页面路径，再账号样例，再关键接口，再排障入口。
