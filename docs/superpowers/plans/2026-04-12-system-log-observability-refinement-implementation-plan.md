# System Log Observability Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/system-log` 的系统异常模式收口为“异常观测台”单主列表页，移除重复统计说明，直出 `详情 / 追踪 / 删除` 行动作，并把详情抽屉改成与设备资产中心一致的扁平工作台风格。

**Architecture:** 保持 `/system-log` 路由和现有数据接口不变，只调整前端命名、工作台结构和详情抽屉信息编排。列表页继续复用 `StandardWorkbenchPanel + StandardListFilterHeader + StandardTableToolbar + StandardWorkbenchRowActions`，详情抽屉继续复用 `StandardDetailDrawer`，但内部结构切换为摘要卡 + 台账分区 + 报文区的设备资产中心语言。

**Tech Stack:** Vue 3 `script setup`、TypeScript、Element Plus、Vitest、共享前端工作台组件。

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - 系统异常模式标题改为 `异常观测台`
  - 移除顶部重复统计 `inline-state`
  - 系统异常模式行操作改为直出 `详情 / 追踪 / 删除`
  - 保持工具栏 `更多操作` 与导出逻辑不变
- Modify: `spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue`
  - 改成设备资产中心风格的扁平详情工作台
  - 保留链路回跳、失败归档回跳、产品/设备治理跳转动作
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - 更新系统异常模式标题、导出标题、行操作与顶部说明预期
- Modify: `spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts`
  - 更新详情抽屉结构断言，锁定新分区与动作
- Modify: `README.md`
  - 同步 `/system-log` 主卡标题与页面语义
- Modify: `docs/02-业务功能与流程说明.md`
  - 同步异常观测台的收口说明
- Modify: `docs/06-前端开发与CSS规范.md`
  - 同步 `/system-log` 新标题与详情风格规则
- Modify: `docs/08-变更记录与技术债清单.md`
  - 记录本轮前端行为调整与验证情况
- Modify: `docs/15-前端优化与治理计划.md`
  - 补充异常观测台统计摘要和行动作治理规则

## Task 1: 锁定系统异常模式的新验收口径

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 写出标题与工具条新预期**

```ts
expect(wrapper.text()).toContain('异常观测台');
expect(wrapper.findComponent(CsvColumnSettingDialogStub).props('title')).toBe('异常观测台导出列设置');
expect(toolbarText).toContain('更多操作');
```

- [ ] **Step 2: 写出“移除顶部重复统计说明”的失败断言**

```ts
expect(wrapper.text()).not.toContain('当前异常 1 条');
expect(wrapper.text()).toContain('来自链路追踪台');
```

- [ ] **Step 3: 写出系统异常模式行操作失败断言**

```ts
const actions = wrapper.findAll('.audit-log-row-actions-stub');
expect(actions.at(0)?.text()).toContain('详情');
expect(actions.at(0)?.text()).toContain('追踪');
expect(actions.at(0)?.text()).toContain('删除');
expect(actions.at(0)?.attributes('data-menu-label')).toBeUndefined();
```

- [ ] **Step 4: 跑定向视图测试确认先失败**

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

## Task 2: 锁定详情抽屉的新结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts`

- [ ] **Step 1: 用新分区名称替换旧说明式断言**

```ts
expect(wrapper.text()).toContain('异常态势与处理概况');
expect(wrapper.text()).toContain('链路与主体台账');
expect(wrapper.text()).toContain('异常诊断与回跳');
expect(wrapper.text()).toContain('请求与响应快照');
```

- [ ] **Step 2: 保留去眉题与回跳动作断言**

```ts
expect(drawer.props('eyebrow')).toBeUndefined();
expect(wrapper.text()).toContain('返回链路追踪');
expect(wrapper.text()).toContain('回看失败归档');
```

- [ ] **Step 3: 跑抽屉测试确认先失败**

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/AuditLogDetailDrawer.test.ts
```

## Task 3: 实现异常观测台列表页收口

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: 改标题与导出标题**

```ts
const pageTitle = computed(() => (isSystemMode.value ? '异常观测台' : '审计中心'))
const exportDialogTitle = computed(() =>
  isSystemMode.value ? '异常观测台导出列设置' : `${pageTitle.value}导出列设置`
)
```

- [ ] **Step 2: 移除系统模式顶部重复 `inline-state`**

```vue
<StandardWorkbenchPanel
  :title="panelTitle"
  :description="pageDescription"
  show-filters
  :show-applied-filters="hasAppliedFilters"
  show-toolbar
  show-pagination
>
```

- [ ] **Step 3: 改系统模式行操作为直出三项**

```ts
const getAuditDirectActions = (row: AuditLogRecord) => {
  if (isSystemMode.value) {
    return [
      { command: 'detail', label: '详情' },
      { command: 'trace', label: '追踪', disabled: !canJumpToMessageTrace(row) },
      { command: 'delete', label: '删除' }
    ]
  }
  return [
    { command: 'detail', label: '详情' },
    { command: 'delete', label: '删除' }
  ]
}
```

- [ ] **Step 4: 清理系统模式不再使用的菜单与统计拼接代码**

```ts
const auditMenuItems = computed(() => [])
```

- [ ] **Step 5: 跑视图测试确认通过**

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

## Task 4: 实现设备资产中心风格的异常详情工作台

**Files:**
- Modify: `spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue`

- [ ] **Step 1: 改成摘要卡 + 台账分区布局**

```vue
<section class="audit-log-detail-workbench__stage" data-testid="audit-log-detail-summary-stage">
  <div class="audit-log-detail-workbench__summary-grid">
    <article v-for="card in summaryCards" :key="card.key" class="audit-log-detail-workbench__summary-card">
      <span class="audit-log-detail-workbench__summary-label">{{ card.label }}</span>
      <span class="audit-log-detail-workbench__summary-value">{{ card.value }}</span>
      <span class="audit-log-detail-workbench__summary-hint">{{ card.hint }}</span>
    </article>
  </div>
</section>
```

- [ ] **Step 2: 用 ledger items 统一链路、主体、异常诊断信息**

```ts
const ledgerSections = computed(() => [
  { key: 'identity', title: '链路与主体台账', items: identityItems.value },
  { key: 'diagnosis', title: '异常诊断与回跳', items: diagnosisItems.value }
])
```

- [ ] **Step 3: 保留请求参数与响应结果的代码块快照**

```vue
<pre class="audit-log-detail-workbench__code-block">{{ formatPayload(detail?.requestParams) }}</pre>
<pre class="audit-log-detail-workbench__code-block">{{ formatPayload(detail?.responseResult) }}</pre>
```

- [ ] **Step 4: 跑抽屉测试确认通过**

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/AuditLogDetailDrawer.test.ts
```

## Task 5: 同步文档与变更记录

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 把 `/system-log` 主标题合同统一改为 `异常观测台`**

```md
- `/products` 主卡标题保留 `产品定义中心`，`/devices` / `/system-log` 分别保留 `设备资产中心` / `异常观测台`。
```

- [ ] **Step 2: 补充本轮行为变化说明**

```md
- `/system-log` 当前移除页头重复异常统计说明，统计摘要继续收口在工具栏 `meta-items`。
- 系统异常模式行操作当前固定直出 `详情 / 追踪 / 删除`，不再保留行内 `更多`。
- 异常详情抽屉当前统一对齐设备资产中心的扁平工作台语法。
```

- [ ] **Step 3: 运行最小定向校验并记录结果**

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts src/__tests__/components/AuditLogDetailDrawer.test.ts src/__tests__/views/OperationsWorkbenchRefinement.test.ts
```

## Self-Review

- 需求 1“异常台账改异常观测台”由 Task 1、Task 3、Task 5 覆盖。
- 需求 2“取消重复统计描述”由 Task 1、Task 3、Task 5 覆盖。
- 需求 3“详情页对齐设备资产中心风格”由 Task 2、Task 4、Task 5 覆盖。
- 需求 4“去掉更多并直出删除”由 Task 1、Task 3、Task 5 覆盖。
- 计划内没有 `TODO/TBD` 占位符，所有受影响文件与验证命令均已列出。

## Execution Handoff

本轮用户已明确要求“开始执行”，按 Inline Execution 在当前会话直接落地，不再停在计划选择。
