# 治理任务台统一搜索与推荐优先处理设计

## 1. 目标

把 `/governance-task` 从“能看任务列表”提升为“能快速找到任务并明确下一步”的正式工作台，首批解决两个高频问题：

- 用户无法通过审批单号快速定位待审批工作项，例如 `2043187508765708289`。
- 治理任务量增大后，页面缺少统一搜索、分类收口和推荐优先处理能力，只能逐条翻找。

本轮目标是让治理任务台直接回答三件事：

1. 这条治理任务能不能搜到。
2. 这批任务当前属于哪一类。
3. 现在应该优先处理哪几条。

## 2. 当前问题

当前 `GET /api/governance/work-items` 只支持：

- `workItemCode`
- `workStatus`
- `subjectType`
- `subjectId`
- `productId`
- `riskMetricId`
- `assigneeUserId`

虽然返回结果里已经包含：

- `approvalOrderId`
- `releaseBatchId`
- `productKey`
- `deviceCode`
- `traceId`
- `taskCategory`
- `executionStatus`

但这些字段还没有进入正式查询模型，也没有进入治理任务台的首屏筛选交互。

因此会出现两个直接问题：

1. 审批桥接场景下，用户明明知道审批单号，却不能在治理任务台直接搜到对应工作项。
2. 页面没有“待审批”“推荐优先处理”这类跨任务类型的工作视角，导致任务越多，越难找到应该先做什么。

## 3. 设计原则

### 3.1 只扩现有控制面对象，不新造平行接口

本轮继续复用 `GET /api/governance/work-items`，在现有分页接口上新增查询能力，不再为搜索单独新增旁路接口。

### 3.2 分类与推荐必须建立在服务端可查询条件之上

不能只做页面本地筛选。审批单号、发布批次号等上下文字段必须进入服务端查询，否则用户只能筛当前页数据，无法解决跨分页定位问题。

### 3.3 页面仍保持单页工作台模式

`/governance-task` 继续保留单页工作台，不拆新路由，不新增私有壳层，复用已有共享筛选和分页模式。

### 3.4 推荐优先处理优先复用现有治理语义

推荐优先处理不引入新的评分引擎；优先复用已有 `workStatus / priorityLevel / executionStatus / recommendation` 语义，把当前已经存在的治理判断真正呈现出来。

## 4. 目标态能力

### 4.1 统一快速搜索

治理任务台新增一个统一搜索框，支持一框查询以下字段：

- `approvalOrderId`
- `releaseBatchId`
- `productKey`
- `deviceCode`
- `traceId`

页面占位文案直接说明支持范围，例如：

`快速搜索（审批单号、发布批次号、产品标识、设备编码、TraceId）`

### 4.2 分类快捷收口

治理任务台新增快捷分类入口，首批覆盖：

- `全部`
- `推荐优先处理`
- `待审批`
- `待发布合同`
- `待绑定风险点`
- `待补阈值`
- `待补联动/预案`
- `待运营复盘`

其中：

- `待发布合同 / 待绑定风险点 / 待补阈值 / 待补联动或预案 / 待运营复盘` 继续基于 `workItemCode` 收口。
- `待审批` 作为跨任务类型视角，基于 `executionStatus=PENDING_APPROVAL` 收口。
- `推荐优先处理` 作为预置工作视角，不是新的数据库字段。

### 4.3 推荐优先处理

“推荐优先处理”首批按以下规则收口：

- 只看 `workStatus=OPEN` 的任务。
- 优先显示 `priorityLevel=P1/P2`。
- 同时优先展示存在 `recommendation.suggestedAction` 的任务。
- 若同优先级存在 `executionStatus=PENDING_APPROVAL` 或 `IN_PROGRESS` 的任务，保持靠前，帮助用户尽快完成闭环。

该视角的目标不是替代所有排序逻辑，而是为“今天先做什么”提供一个开箱即用的任务入口。

## 5. 后端设计

### 5.1 扩展分页查询模型

在 `GovernanceWorkItemPageQuery` 中新增：

- `keyword`
- `executionStatus`

本轮不新增独立的 `taskCategory` 查询参数。原因是首批分类已经能由 `workItemCode` 和 `executionStatus` 满足，避免过早暴露重复筛选维度。

### 5.2 统一关键词匹配规则

`keyword` 的服务端匹配规则如下：

- `approvalOrderId`：精确匹配
- `releaseBatchId`：精确匹配
- `productKey`：精确优先，必要时模糊兜底
- `deviceCode`：精确优先，必要时模糊兜底
- `traceId`：精确优先，必要时模糊兜底

为了避免数字审批单号被模糊查询污染，`approvalOrderId` 与 `releaseBatchId` 统一按精确匹配处理；字符串类上下文先走等值命中，再允许模糊兜底，以兼顾稳定性和可用性。

### 5.3 扩展分页过滤条件

`buildPageWrapper` 在原有条件基础上增加：

- `executionStatus` 精确过滤
- `keyword` 统一复合过滤

复合过滤逻辑应使用单个 `and/or` 包裹，避免与现有 `workStatus / workItemCode / productId` 等条件串联后产生语义漂移。

### 5.4 默认排序调整

当前分页接口按 `createTime desc, id desc` 排序，无法回答“下一步先处理什么”。

本轮默认排序调整为：

1. `OPEN` 优先于 `ACKED / BLOCKED / RESOLVED / CLOSED`
2. `priorityLevel` 按 `P1 -> P2 -> P3`
3. `updateTime desc`
4. `id desc`

这样即使不进入“推荐优先处理”视角，治理任务台首屏也会优先呈现真正需要先动的任务。

### 5.5 非目标

本轮后端不做：

- 新增独立搜索接口
- 新增全文检索引擎
- 改写治理工作项生成链路
- 为“推荐优先处理”新增数据库持久化标签

## 6. 前端设计

### 6.1 工作台结构

`/governance-task` 继续复用：

- `StandardWorkbenchPanel`
- `StandardListFilterHeader`
- `StandardTableToolbar`
- `StandardPagination`
- 现有任务卡与详情抽屉

不新增页面私有壳层，不引入新一级路由。

### 6.2 筛选区布局

筛选区改成正式筛选头，首批包含：

- 统一快速搜索框
- 任务分类快捷入口
- 工作状态筛选
- 重置 / 查询动作

搜索与分类都进入 URL query，保证以下场景可回放：

- 首页治理经营卡片深链进来
- `/products` 或 `/governance-approval` 跳回任务台继续排查
- 浏览器刷新后仍保留当前工作视角

### 6.3 分类与 query 映射

页面内部维护一个轻量分类映射：

- `全部`：不附加分类 query
- `推荐优先处理`：写入 `view=recommended`
- `待审批`：写入 `executionStatus=PENDING_APPROVAL`
- `待发布合同`：写入 `workItemCode=PENDING_CONTRACT_RELEASE`
- `待绑定风险点`：写入 `workItemCode=PENDING_RISK_BINDING`
- `待补阈值`：写入 `workItemCode=PENDING_THRESHOLD_POLICY`
- `待补联动/预案`：写入 `workItemCode=PENDING_LINKAGE_PLAN`
- `待运营复盘`：写入 `workItemCode=PENDING_REPLAY`

其中 `view=recommended` 只作为前端预置视图标识，不要求后端新增同名参数。

### 6.4 推荐优先处理呈现

当进入 `view=recommended` 时，页面继续请求正式分页接口，但会默认带上：

- `workStatus=OPEN`

前端在结果展示层补充两个动作：

- 首屏摘要直接回答“推荐先处理 X 条”
- 卡片按推荐规则做稳定排序展示

若服务端默认排序已按优先级收口，前端只需做有限补充排序，不重复发明第二套优先级体系。

### 6.5 卡片锚点与摘要优化

当前卡片锚点优先显示 `productKey / deviceCode / traceId`。本轮调整为：

1. `approvalOrderId`
2. `releaseBatchId`
3. `productKey`
4. `deviceCode`
5. `traceId`
6. 现有 snapshot 回退字段

首屏任务摘要同步增强为直接回答：

- 当前命中总数
- 待处理数量
- 当前分类
- 推荐优先处理数量

这样当用户按审批单号搜索时，结果卡片首屏就能看到审批单号本身，不需要再点抽屉核对。

## 7. 数据流

### 7.1 搜索定位

1. 用户在治理任务台输入审批单号、发布批次号、产品标识、设备编码或 TraceId。
2. 页面把关键词写入路由 query，并触发 `GET /api/governance/work-items`。
3. 后端按 `keyword + 其他结构化条件` 返回匹配分页。
4. 页面首屏直接展示命中的工作项，并保留“去处理 / 决策说明 / 复盘 / 确认 / 阻塞 / 关闭”既有动作。

### 7.2 分类工作

1. 用户点击某个分类入口。
2. 页面把分类映射为 query。
3. 后端按正式条件过滤分页。
4. 页面摘要与卡片列表同步切换到该工作视角。

### 7.3 推荐优先处理

1. 用户点击“推荐优先处理”。
2. 页面进入 `view=recommended` 并带上 `workStatus=OPEN`。
3. 后端按正式分页返回待处理任务。
4. 页面按推荐规则呈现优先列表，并明确建议下一步动作。

## 8. 错误处理与兼容性

### 8.1 查询兼容

旧 deep link 若只带 `workItemCode / productId / workStatus`，页面仍按旧逻辑可用；新增 `keyword / executionStatus / view` 都是增量能力，不破坏现有链接。

### 8.2 空结果提示

当关键词没有命中任何任务时，空态文案需要明确告诉用户：

- 当前没有匹配的治理任务
- 可以尝试切换分类或清空关键词

避免继续沿用泛化文案，让用户误以为系统没有生成任务。

### 8.3 推荐视图降级

如果 `view=recommended` 下没有结果，页面不自动跳回全部列表，而是明确反馈“当前没有待处理的推荐任务”，保持视图稳定。

## 9. 验收标准

本轮至少验证以下场景：

1. 在 `/governance-task` 输入审批单号 `2043187508765708289`，能直接定位到对应待审批任务。
2. 输入发布批次号、产品标识、设备编码、TraceId 时，能各自命中对应任务。
3. 点击“待审批”后，只返回 `executionStatus=PENDING_APPROVAL` 的工作项。
4. 点击“待发布合同 / 待绑定风险点 / 待补阈值 / 待补联动或预案 / 待运营复盘”后，能按对应 `workItemCode` 收口。
5. 点击“推荐优先处理”后，首屏以 `OPEN + P1/P2` 为主，并保留推荐动作说明。
6. 刷新页面、复制链接重开页面后，关键词、分类、状态和分页仍能恢复。

## 10. 文档影响

实现时需要原位更新：

- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/08-变更记录与技术债清单.md`
- 如页面交互规则有新增约束，补充到 `docs/15-前端优化与治理计划.md`

如最终接口字段或页面命名与本设计不同，文档以实现后的真实行为为准，不保留平行说明。

## 11. 非目标

本轮不做：

- 把治理任务台改造成审批台替代页
- 新增多路由 tab 工作区
- 引入 Elasticsearch 或其他全文检索组件
- 为所有控制面页面统一补搜索，首批只覆盖 `/governance-task`
- 自动替用户执行治理动作或自动审批
