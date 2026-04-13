# 治理任务台任务视角下拉收口设计

## 1. 目标

把 `/governance-task` 从“横向一排分类按钮 + 列表卡片”的形态，收口为更明确的控制面工作台语法：

- 快速搜索
- 任务视角下拉
- 工作状态下拉
- 查询 / 重置
- 任务导向卡
- 任务卡 / 空态继续处理

页面定位保持不变：`治理任务台` 仍然是“统一调度、判断下一步、进入领域页处理、留痕”的控制面入口，而不是纯只读记录页。

## 2. 当前问题

当前页面虽然已经支持：

- `推荐优先处理`
- `待审批`
- `待发布合同`
- `待绑定风险点`
- `待补阈值`
- `待补联动/预案`
- `待运营复盘`

但这些能力是通过一排横向按钮表达的，存在 3 个问题：

1. 首屏像“分类标签墙”，更像运营列表，而不是正式工作台。
2. 分类项一旦继续增加，横向空间会快速膨胀。
3. 分类维度与 `工作状态` 维度并列关系不够清晰，用户不容易理解“我是先选任务视角，再选状态，还是两个都在切列表”。

## 3. 设计原则

### 3.1 保留控制面定位，不退回纯列表页

这次只调整筛选入口表达，不削弱治理任务台的控制面角色。任务导向卡、任务卡上的 `去处理 / 确认 / 阻塞 / 关闭 / 复盘 / 决策说明` 仍然保留。

### 3.2 只改前端入口表达，不改既有 query 语义

现有 URL 和后端查询契约已经稳定：

- `view=recommended`
- `executionStatus=PENDING_APPROVAL`
- `workItemCode=PENDING_CONTRACT_RELEASE`
- `workItemCode=PENDING_RISK_BINDING`
- `workItemCode=PENDING_THRESHOLD_POLICY`
- `workItemCode=PENDING_LINKAGE_PLAN`
- `workItemCode=PENDING_REPLAY`

本轮不新增新 query 字段，不改后端接口，只把“按钮入口”改成“单一下拉入口”。

### 3.3 任务视角与工作状态保持正交

用户先决定“看哪类任务”，再决定“看这类任务当前处于什么状态”。因此 `任务视角` 和 `工作状态` 应该都是下拉，但语义不同，不能混成一个复合字段。

## 4. 目标态交互

### 4.1 筛选区结构

治理任务台筛选区固定为三栏：

1. `快速搜索`
2. `任务视角`
3. `工作状态`

其中：

- `快速搜索` 保持现有占位文案和行为不变。
- `任务视角` 为新的下拉入口，替代整排分类按钮。
- `工作状态` 继续沿用现有下拉。

### 4.2 任务视角下拉选项

下拉选项固定为：

- `全部`
- `推荐优先处理`
- `待审批`
- `待发布合同`
- `待绑定风险点`
- `待补阈值`
- `待补联动/预案`
- `待运营复盘`

### 4.3 选项与 query 映射

映射规则保持现状：

- `全部`
  - 清空 `view / executionStatus / workItemCode`
- `推荐优先处理`
  - 写入 `view=recommended`
- `待审批`
  - 写入 `executionStatus=PENDING_APPROVAL`
- `待发布合同`
  - 写入 `workItemCode=PENDING_CONTRACT_RELEASE`
- `待绑定风险点`
  - 写入 `workItemCode=PENDING_RISK_BINDING`
- `待补阈值`
  - 写入 `workItemCode=PENDING_THRESHOLD_POLICY`
- `待补联动/预案`
  - 写入 `workItemCode=PENDING_LINKAGE_PLAN`
- `待运营复盘`
  - 写入 `workItemCode=PENDING_REPLAY`

这意味着：

- 首页、产品定义中心、审批台等既有深链都不需要改。
- 任务视角只是换一种 UI 表达，不影响链接回放和浏览器刷新恢复。

## 5. 页面结构变化

### 5.1 删除的内容

- 删除筛选区下方整排 `StandardButton` 分类条。

### 5.2 保留的内容

- `任务导向` 卡
- 工具条统计
- 任务卡
- 空态
- 空态 `继续处理`
- 分页

### 5.3 不变的能力

这次不改变：

- 任务排序
- 推荐优先处理算法
- 任务卡动作
- 决策说明抽屉
- 复盘抽屉
- 空态派发逻辑

## 6. 实现设计

### 6.1 前端

修改 `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`：

- 在 `filters` 中加入 `taskView`
- `syncFiltersFromRoute()` 从现有 query 反推当前视角值
- 删除 `governance-task-category-strip`
- 在 `StandardListFilterHeader` 里增加 `任务视角` 的 `<select>`
- 查询、重置、分页、尺寸切换时继续统一通过现有 `replaceTaskRouteQuery(...)` 回写 query

### 6.2 测试

修改 `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`：

- 原有“点击分类按钮”用例改为“切换任务视角下拉”
- 保留 `待审批 -> executionStatus=PENDING_APPROVAL` 映射回归
- 增加“路由 query 能正确回显任务视角下拉值”的回归

## 7. 取舍说明

本轮没有采用“保留少量高频快捷项 + 其余放入下拉”的混合方案，原因是它会引入第二套选择入口，让用户继续面对“按钮和下拉谁是主筛选”的歧义。

本轮也没有把 `任务视角` 和 `工作状态` 合并成一个超大下拉，因为那会把“看什么任务”和“看该任务的哪个状态”混成一个维度，后续反而更难扩展。

## 8. 文档影响

需要原位更新：

- `docs/02-业务功能与流程说明.md`
- `docs/21-业务功能清单与验收标准.md`
- `docs/08-变更记录与技术债清单.md`

`README.md` 与 `AGENTS.md` 预计无需变更。
