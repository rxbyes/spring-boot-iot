# 治理任务台空列表继续处理闭环设计

## 1. 目标

补齐这条实际用户链路的最小闭环：

`/products` 产品定义中心 notice -> `/governance-task` -> 继续处理 -> 对应领域工作台

当前问题不是合同发布、审批、目录发布这些治理能力缺失，而是产品页 notice 与治理任务台列表并非同一实时真相源。产品页能根据 `coverage-overview + latest release batch` 判断“待发布合同”，但治理任务台分页接口当前采用“先读已落库工作项，再后台异步刷新”的方式，导致用户深链进入 `/governance-task?productId=...&workItemCode=PENDING_CONTRACT_RELEASE&workStatus=OPEN` 时，可能暂时看不到任何任务卡，只剩空态文案，无法继续执行。

本轮目标是让治理任务台在“上下文明确但列表为空”的场景下，仍然直接回答“现在可以去哪处理”，不再把用户卡死在控制面空页。

## 2. 范围与非目标

### 2.1 本轮范围

- 仅修复 `/governance-task` 的上下文空态闭环。
- 仅覆盖已有分派能力已经支持的工作项类型。
- 首批重点验证 `PENDING_CONTRACT_RELEASE`，同时设计保持对其他可分派工作项兼容。

### 2.2 非目标

- 不改 `/products` notice 的入口策略，仍保留先深链治理任务台。
- 不把 `/api/governance/work-items` 的异步刷新机制改回同步补算。
- 不新建后端接口，不改工作项生成链路。
- 不在空列表时自动跳转领域页，仍由用户显式点击“继续处理”。

## 3. 设计原则

### 3.1 控制面继续只负责调度与留痕

治理任务台不直接编辑合同、风险绑定或策略真相源。即使列表为空，也只提供“继续处理”调度动作，真正执行仍回到领域页完成。

### 3.2 只在上下文足够明确时补救

普通空列表仍保持现有空态。只有当页面具备明确 query 上下文，且该上下文可以映射到现有分派目标时，才展示补救卡。

### 3.3 复用既有分派规则

空态补救卡不新增第二套跳转逻辑，统一复用 `buildGovernanceTaskDispatchLocation(...)`，保证列表卡片的“去处理”和空态卡的“继续处理”目标一致。

## 4. 目标态交互

当满足以下条件时：

- `/governance-task` 当前列表为空；
- 页面携带明确上下文，例如 `productId + workItemCode=PENDING_CONTRACT_RELEASE`；
- 该上下文可构造为一个可分派的治理任务；

页面空态区改为展示“上下文处理卡”：

- 继续保留“当前没有匹配的治理任务”的事实说明；
- 额外说明“可能仍在同步正式工作项，可先进入领域工作台继续处理”；
- 提供一个主动作按钮 `继续处理`；
- `PENDING_CONTRACT_RELEASE` 点击后跳转：
  `/products?openProductId=<productId>&workbenchView=models&governanceSource=task&workItemCode=PENDING_CONTRACT_RELEASE`

如果 query 上下文不足、或当前 work item code 本身不可派发，则仍使用原空态文案，不显示按钮。

## 5. 实现设计

### 5.1 前端视图

修改 `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`：

- 新增一个基于路由上下文推导的 `emptyDispatchContext` 计算属性。
- 当 `displayTaskList.length === 0` 且 `emptyDispatchContext` 可用时，在空态区域显示补救卡与 `继续处理` 动作。
- `继续处理` 复用现有 `router.push(...)` 分派逻辑。

### 5.2 分派来源

复用 `spring-boot-iot-ui/src/utils/governanceTaskDispatch.ts` 的既有目标路由规则。

本轮不要求从 route query 手工拼接所有任务字段。只构造最小 `GovernanceWorkItem` 形状：

- `workItemCode`
- `productId / subjectId / riskMetricId`
- 必要时从 query 或 `snapshotJson` 读取补充分派上下文

这样既能复用现有派发逻辑，又不会在空态场景复制列表卡的大量显示字段。

## 6. 测试策略

先写前端失败用例，再写实现：

- 当路由为 `productId=9223372036854775807&workItemCode=PENDING_CONTRACT_RELEASE&workStatus=OPEN`，且后端返回 `records=[]` 时：
  - 页面应继续显示空态事实；
  - 页面应额外出现 `继续处理`；
  - 点击后应跳转到 `/products?openProductId=9223372036854775807&workbenchView=models...`

同时保留一条对照语义：

- 没有明确分派上下文的普通空列表，不应平白出现 `继续处理`。

## 7. 文档影响

需要原位更新：

- `docs/02-业务功能与流程说明.md`
- `docs/21-业务功能清单与验收标准.md`
- `docs/08-变更记录与技术债清单.md`

`README.md` 与 `AGENTS.md` 本轮只做检查，不预期改动。
