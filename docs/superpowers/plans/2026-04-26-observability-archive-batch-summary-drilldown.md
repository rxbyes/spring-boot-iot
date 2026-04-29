# G5 归档批次摘要卡下钻 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `/system-log` 的归档批次摘要卡既能一键替换异常筛选，又能从“最近异常批次”直接定位并自动打开详情抽屉。

**Architecture:** 本轮只做前端本地交互增强，不新增后端接口，也不引入 URL query 状态。`AuditLogView.vue` 负责维护摘要卡选中态、焦点批次号、自动开抽屉和页面内降级提示；`AuditLogView.test.ts` 负责锁定摘要卡驱动筛选、定位、自动开抽屉和未命中降级提示的行为。

**Tech Stack:** Vue 3 `script setup`、Vitest、Vue Test Utils、现有 `/system-log` 可观测台账读侧。

---

### Task 1: 为摘要卡筛选交互补失败用例

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 写“异常批次 / 执行偏差 / 剩余过期”三张卡的失败用例**

```ts
it('replaces archive batch filters from summary cards', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  vi.mocked(pageObservabilityMessageArchiveBatches).mockClear()
  vi.mocked(getObservabilityMessageArchiveBatchOverview).mockClear()

  await wrapper.get('[data-testid="archive-batch-overview-abnormal"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(pageObservabilityMessageArchiveBatches).toHaveBeenCalledWith({
    sourceTable: 'iot_message_log',
    onlyAbnormal: true,
    pageNum: 1,
    pageSize: 5
  })

  vi.mocked(pageObservabilityMessageArchiveBatches).mockClear()
  await wrapper.get('[data-testid="archive-batch-overview-drifted"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(pageObservabilityMessageArchiveBatches).toHaveBeenCalledWith({
    sourceTable: 'iot_message_log',
    compareStatus: 'DRIFTED',
    pageNum: 1,
    pageSize: 5
  })

  vi.mocked(pageObservabilityMessageArchiveBatches).mockClear()
  await wrapper.get('[data-testid="archive-batch-overview-remaining"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(pageObservabilityMessageArchiveBatches).toHaveBeenCalledWith({
    sourceTable: 'iot_message_log',
    onlyAbnormal: true,
    pageNum: 1,
    pageSize: 5
  })
})
```

- [ ] **Step 2: 跑单测确认它先失败**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts -t "replaces archive batch filters from summary cards"
```

Expected: FAIL，提示缺少摘要卡 `data-testid` 或点击后没有触发新的筛选请求。

- [ ] **Step 3: 先补最小测试支撑数据和断言结构**

```ts
expect(ledger.text()).toContain('异常批次')
expect(ledger.text()).toContain('执行偏差总量')
expect(ledger.text()).toContain('剩余过期总量')
expect(wrapper.find('[data-testid="archive-batch-overview-abnormal"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="archive-batch-overview-drifted"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="archive-batch-overview-remaining"]').exists()).toBe(true)
```

- [ ] **Step 4: 再跑单测，确认失败点集中在实现缺失**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts -t "replaces archive batch filters from summary cards"
```

Expected: FAIL，且失败集中在“预期调用参数不匹配”。

- [ ] **Step 5: 提交测试基线**

```bash
git add spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "test: cover archive batch summary card filters"
```

### Task 2: 为“最近异常批次”定位直开与未命中降级补失败用例

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 写“最近异常批次定位并自动开抽屉”的失败用例**

```ts
it('opens latest abnormal archive batch from summary card', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  vi.mocked(pageObservabilityMessageArchiveBatches).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 5,
      records: [
        {
          id: 99,
          batchNo: 'iot_message_log-20260426090100',
          sourceTable: 'iot_message_log',
          status: 'FAILED',
          compareStatus: 'DRIFTED',
          compareStatusLabel: '有偏差'
        }
      ]
    }
  })

  await wrapper.get('[data-testid="archive-batch-overview-latest"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(pageObservabilityMessageArchiveBatches).toHaveBeenLastCalledWith({
    sourceTable: 'iot_message_log',
    onlyAbnormal: true,
    pageNum: 1,
    pageSize: 5
  })
  expect(getObservabilityMessageArchiveBatchCompare).toHaveBeenCalledWith(
    'iot_message_log-20260426090100'
  )
  expect(getObservabilityMessageArchiveBatchReportPreview).toHaveBeenCalledWith(
    'iot_message_log-20260426090100'
  )
})
```

- [ ] **Step 2: 写“最近异常批次未命中当前页”的失败用例**

```ts
it('shows a focus hint when latest abnormal batch is not in current page', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  vi.mocked(pageObservabilityMessageArchiveBatches).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 5,
      records: []
    }
  })

  await wrapper.get('[data-testid="archive-batch-overview-latest"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.text()).toContain('最近异常批次不在当前结果中，请调整时间范围后重试')
})
```

- [ ] **Step 3: 跑单测确认这两个行为先失败**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts -t "latest abnormal"
```

Expected: FAIL，提示没有最新异常卡入口、没有自动开抽屉或没有页面内提示。

- [ ] **Step 4: 再补“重置会清空摘要卡状态”的失败用例**

```ts
it('clears summary selection and focus hint on reset', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  await wrapper.get('[data-testid="archive-batch-overview-abnormal"]').trigger('click')
  await flushPromises()
  await nextTick()

  await wrapper.get('[data-testid="archive-batch-reset-button"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.audit-log-archive-batch-ledger__overview-card.is-active').exists()).toBe(false)
  expect(wrapper.text()).not.toContain('最近异常批次不在当前结果中，请调整时间范围后重试')
})
```

- [ ] **Step 5: 提交定位/降级测试基线**

```bash
git add spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "test: cover archive batch summary drilldown"
```

### Task 3: 在 `AuditLogView.vue` 实现摘要卡下钻与自动开抽屉

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 添加摘要卡状态、焦点批次和页面内提示状态**

```ts
type ArchiveBatchSummarySelection = 'NONE' | 'ABNORMAL' | 'DRIFTED' | 'REMAINING' | 'LATEST_ABNORMAL'

const messageArchiveBatchSummarySelection = ref<ArchiveBatchSummarySelection>('NONE')
const messageArchiveBatchFocusedBatchNo = ref('')
const messageArchiveBatchAutoOpenFocused = ref(false)
const messageArchiveBatchFocusHint = ref('')
```

- [ ] **Step 2: 给摘要卡 computed 补充 key、激活态和可点击语义**

```ts
const messageArchiveBatchOverviewCards = computed(() => [
  {
    key: 'ABNORMAL',
    testId: 'archive-batch-overview-abnormal',
    clickable: true,
    active: messageArchiveBatchSummarySelection.value === 'ABNORMAL',
    label: '异常批次',
    value: formatOptionalCount(overview?.abnormalBatches),
    meta: `总批次 ${formatOptionalCount(overview?.totalBatches)}`
  },
  {
    key: 'LATEST_ABNORMAL',
    testId: 'archive-batch-overview-latest',
    clickable: Boolean(overview?.latestAbnormalBatch),
    active: messageArchiveBatchSummarySelection.value === 'LATEST_ABNORMAL',
    label: '最近异常批次',
    value: formatValue(overview?.latestAbnormalBatch),
    meta: formatValue(overview?.latestAbnormalOccurredAt)
  }
])
```

- [ ] **Step 3: 补摘要卡点击处理函数，统一替换异常筛选**

```ts
const applyMessageArchiveBatchSummarySelection = (
  selection: ArchiveBatchSummarySelection,
  options: { focusBatchNo?: string; autoOpen?: boolean } = {}
) => {
  messageArchiveBatchSummarySelection.value = selection
  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFocusedBatchNo.value = options.focusBatchNo || ''
  messageArchiveBatchAutoOpenFocused.value = Boolean(options.autoOpen && options.focusBatchNo)

  switch (selection) {
    case 'ABNORMAL':
      messageArchiveBatchFilters.compareStatus = ''
      messageArchiveBatchFilters.onlyAbnormal = true
      break
    case 'DRIFTED':
      messageArchiveBatchFilters.compareStatus = 'DRIFTED'
      messageArchiveBatchFilters.onlyAbnormal = false
      break
    case 'REMAINING':
    case 'LATEST_ABNORMAL':
      messageArchiveBatchFilters.compareStatus = ''
      messageArchiveBatchFilters.onlyAbnormal = true
      break
    default:
      break
  }
}
```

- [ ] **Step 4: 用一个统一执行器包住刷新、回滚和自动开抽屉**

```ts
const syncMessageArchiveBatchSummarySelectionFromFilters = () => {
  if (messageArchiveBatchFilters.compareStatus === 'DRIFTED') {
    messageArchiveBatchSummarySelection.value = 'DRIFTED'
    return
  }
  if (messageArchiveBatchFilters.onlyAbnormal) {
    messageArchiveBatchSummarySelection.value =
      messageArchiveBatchSummarySelection.value === 'REMAINING' ? 'REMAINING' : 'ABNORMAL'
    return
  }
  messageArchiveBatchSummarySelection.value = 'NONE'
}

const tryOpenFocusedArchiveBatch = async () => {
  const batchNo = messageArchiveBatchFocusedBatchNo.value.trim()
  if (!batchNo) {
    return
  }
  const target = messageArchiveBatchRows.value.find((row) => String(row.batchNo || '').trim() === batchNo)
  if (!target) {
    messageArchiveBatchFocusHint.value = '最近异常批次不在当前结果中，请调整时间范围后重试'
    messageArchiveBatchAutoOpenFocused.value = false
    return
  }
  if (messageArchiveBatchAutoOpenFocused.value) {
    await openMessageArchiveBatchDetail(target)
    messageArchiveBatchAutoOpenFocused.value = false
  }
}

const runMessageArchiveBatchSummaryAction = async (
  selection: ArchiveBatchSummarySelection,
  options: { focusBatchNo?: string; autoOpen?: boolean } = {}
) => {
  const previousSelection = messageArchiveBatchSummarySelection.value
  const previousFilters = { ...messageArchiveBatchFilters }
  const previousHint = messageArchiveBatchFocusHint.value
  applyMessageArchiveBatchSummarySelection(selection, options)
  try {
    await getMessageArchiveBatchLedger()
    await getMessageArchiveBatchOverview()
    await tryOpenFocusedArchiveBatch()
  } catch (error) {
    Object.assign(messageArchiveBatchFilters, previousFilters)
    messageArchiveBatchSummarySelection.value = previousSelection
    messageArchiveBatchFocusHint.value = previousHint
    messageArchiveBatchFocusedBatchNo.value = ''
    messageArchiveBatchAutoOpenFocused.value = false
    throw error
  }
}
```

- [ ] **Step 5: 调整模板、筛选入口和样式，补 `data-testid`、激活态、焦点提示和目标行高亮**

```vue
<article
  v-for="item in messageArchiveBatchOverviewCards"
  :key="item.key"
  :data-testid="item.testId"
  :class="[
    'audit-log-archive-batch-ledger__overview-card',
    { 'is-active': item.active, 'is-disabled': item.clickable === false }
  ]"
  @click="item.clickable && handleMessageArchiveBatchOverviewCardClick(item.key)"
>
  <span>{{ item.label }}</span>
  <strong>{{ item.value }}</strong>
  <p>{{ item.meta }}</p>
</article>

<div v-if="messageArchiveBatchFocusHint" class="audit-log-slow-summary__empty">
  {{ messageArchiveBatchFocusHint }}
</div>
```

```ts
const handleMessageArchiveBatchOverviewCardClick = async (selection: ArchiveBatchSummarySelection) => {
  const latestBatchNo = String(messageArchiveBatchOverview.value?.latestAbnormalBatch || '').trim()
  if (selection === 'LATEST_ABNORMAL' && !latestBatchNo) {
    return
  }
  await runMessageArchiveBatchSummaryAction(selection, {
    focusBatchNo: selection === 'LATEST_ABNORMAL' ? latestBatchNo : undefined,
    autoOpen: selection === 'LATEST_ABNORMAL'
  })
}

const handleMessageArchiveBatchSearch = () => {
  syncMessageArchiveBatchSummarySelectionFromFilters()
  refreshMessageArchiveBatchLedger()
}

const resetMessageArchiveBatchFilters = () => {
  messageArchiveBatchSummarySelection.value = 'NONE'
  messageArchiveBatchFocusedBatchNo.value = ''
  messageArchiveBatchAutoOpenFocused.value = false
  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFilters.batchNo = ''
  messageArchiveBatchFilters.status = ''
  messageArchiveBatchFilters.compareStatus = ''
  messageArchiveBatchFilters.onlyAbnormal = false
  messageArchiveBatchFilters.dateFrom = ''
  messageArchiveBatchFilters.dateTo = ''
  refreshMessageArchiveBatchLedger()
}
```

- [ ] **Step 6: 跑视图回归，确认 G5 行为和 G2/G3/G4 旧行为同时通过**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected: PASS，包含新增摘要卡下钻用例和现有归档批次详情抽屉用例。

- [ ] **Step 7: 提交前端实现**

```bash
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: add archive batch summary drilldown"
```

### Task 4: 同步文档并完成最终验证

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`

- [ ] **Step 1: 在 `docs/08` 记录 G5 交互收口**

```md
- 2026-04-26：可观测证据链补齐 G5 归档批次摘要卡下钻。`/system-log` 的前 3 张摘要卡当前可直接替换异常筛选，`最近异常批次` 卡会在当前页内定位对应批次并自动打开详情抽屉；若目标批次不在当前 `5` 条结果中，则只给出页面内轻提示，不自动翻页。
```

- [ ] **Step 2: 在 `docs/11`、`README.md`、`AGENTS.md` 补运行口径**

```md
- `/api/system/observability/message-archive-batches/overview` 的前端消费已升级为“摘要卡可筛选、最近异常批次可直开详情”。
- `/system-log` 当前支持按摘要卡快速切换 `onlyAbnormal / compareStatus=DRIFTED`，并可从 `latestAbnormalBatch` 一步进入归档批次详情抽屉。
```

- [ ] **Step 3: 跑最小 API + 视图回归**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: PASS，API query builder 和 `/system-log` 视图用例均通过。

- [ ] **Step 4: 跑补丁洁净检查**

Run:

```bash
git diff --check
```

Expected: 无输出。

- [ ] **Step 5: 提交文档与验证收口**

```bash
git add README.md AGENTS.md docs/08-变更记录与技术债清单.md docs/11-可观测性、日志追踪与消息通知治理.md
git commit -m "docs: record archive batch summary drilldown"
```

## Spec Coverage Check

- 摘要卡替换异常筛选：Task 1、Task 3 覆盖。
- `最近异常批次` 定位并自动开抽屉：Task 2、Task 3 覆盖。
- 失败与降级提示：Task 2、Task 3 覆盖。
- 不新增后端接口 / 不做 URL query / 不自动翻页：Task 3 只修改前端状态与模板，Task 4 仅同步文档。
- 文档同步与回归：Task 4 覆盖。
