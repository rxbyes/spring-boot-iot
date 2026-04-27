# System Log Error Detail-First Cluster Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `/system-log` -> `异常排查` so the default experience loads only error details, while cluster analysis becomes an on-demand full-stage view that returns to detail with cluster filters applied.

**Architecture:** Keep the existing backend cluster API and refactor only the frontend state model. Replace the current eager `clustered/all` flow with a view-mode state machine that separates `detail` and `clusters`, caches the last cluster result for return navigation, and applies selected cluster refiners back into the detail search model.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Element Plus, Vitest, Vue Test Utils

---

### Task 1: Rewrite the panel contract around detail-first and cluster-stage actions

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`

- [ ] **Step 1: Write the failing component tests**

```ts
it('shows the detail table first and renders the cluster entry in the filter header', () => {
  const wrapper = mountPanel({
    errorViewMode: 'detail',
    clusterQueryActive: false,
    clusterContextSummary: ''
  })

  expect(wrapper.text()).toContain('按异常分组查看')
  expect(wrapper.text()).not.toContain('异常分组主表')
})

it('shows the cluster stage with return actions when the grouped view is active', () => {
  const wrapper = mountPanel({
    errorViewMode: 'clusters',
    clusterRows: [createClusterRow()],
    clusterQueryActive: true
  })

  expect(wrapper.text()).toContain('返回异常明细')
  expect(wrapper.text()).toContain('当前筛选条件下的异常分组')
})

it('shows a lightweight refiner context in detail mode after selecting a cluster', () => {
  const wrapper = mountPanel({
    errorViewMode: 'detail',
    clusterQueryActive: true,
    clusterContextSummary: 'message.mqtt / BizException / 500'
  })

  expect(wrapper.text()).toContain('当前按分组定位')
  expect(wrapper.text()).toContain('清除分组定位')
  expect(wrapper.text()).toContain('返回异常分组结果')
})
```

- [ ] **Step 2: Run the component test file to verify RED**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts
```

Expected: FAIL because `AuditLogErrorTabPanel.vue` still expects `detailClusterMode` and renders the grouped table first.

- [ ] **Step 3: Update the panel props and emitted events with minimal template changes**

```ts
const props = defineProps<{
  errorViewMode: 'detail' | 'clusters'
  clusterQueryActive: boolean
  clusterContextSummary: string
  canReturnToClusterResults: boolean
}>()

const emit = defineEmits<{
  (event: 'open-clusters'): void
  (event: 'return-to-details'): void
  (event: 'retry-clusters'): void
  (event: 'apply-cluster', clusterKey: string): void
  (event: 'clear-cluster-refiner'): void
  (event: 'return-to-clusters'): void
}>()
```

```vue
<StandardButton v-if="errorViewMode === 'detail'" action="search" @click="emit('open-clusters')">
  按异常分组查看
</StandardButton>

<section v-if="errorViewMode === 'clusters'" class="audit-log-system-panel__cluster-stage">
  <StandardButton action="reset" @click="emit('return-to-details')">返回异常明细</StandardButton>
</section>

<section v-if="errorViewMode === 'detail' && clusterQueryActive && clusterContextSummary" class="audit-log-system-panel__cluster-context">
  <span>当前按分组定位：{{ clusterContextSummary }}</span>
  <StandardButton action="reset" @click="emit('clear-cluster-refiner')">清除分组定位</StandardButton>
  <StandardButton v-if="canReturnToClusterResults" action="search" @click="emit('return-to-clusters')">
    返回异常分组结果
  </StandardButton>
</section>
```

- [ ] **Step 4: Re-run the component test file to verify GREEN**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts
```

Expected: PASS for the rewritten panel behavior tests.


### Task 2: Lock view-level behavior with failing tests before touching the page state machine

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: Write the failing view tests for default detail-first loading**

```ts
it('does not load system error clusters on the default system error view', async () => {
  mountView()
  await flushPromises()

  expect(pageSystemErrorClusters).not.toHaveBeenCalled()
  expect(pageLogs).toHaveBeenCalledWith(
    expect.objectContaining({ operationType: 'system_error' })
  )
})

it('opens the cluster view with the current form values instead of only applied filters', async () => {
  const wrapper = mountView()
  await flushPromises()

  await wrapper.find('input[placeholder="请输入异常模块"]').setValue('message.mqtt')
  await wrapper.find('button').filter((button) => button.text().includes('按异常分组查看'))[0].trigger('click')
  await flushPromises()

  expect(pageSystemErrorClusters).toHaveBeenLastCalledWith(
    expect.objectContaining({ operationModule: 'message.mqtt' })
  )
})

it('returns to detail with cluster refiner fields after a cluster is selected', async () => {
  const wrapper = mountView()
  await flushPromises()

  await openClusterStage(wrapper)
  await clickClusterRow(wrapper, 'cluster-mqtt-500')
  await flushPromises()

  expect(pageLogs).toHaveBeenLastCalledWith(
    expect.objectContaining({
      operationModule: 'message.mqtt',
      exceptionClass: 'com.ghlzm.iot.common.exception.BizException',
      errorCode: '500'
    })
  )
})
```

- [ ] **Step 2: Run the view test file to verify RED**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
```

Expected: FAIL because the current page eagerly loads clusters and keeps the grouped-first behavior.

- [ ] **Step 3: Extend the view tests for invalidation and grouped fallback behavior**

```ts
it('clears the cluster return context after the user edits filters and searches again', async () => {
  const wrapper = mountView()
  await flushPromises()

  await openClusterStage(wrapper)
  await clickClusterRow(wrapper, 'cluster-mqtt-500')
  await flushPromises()

  await wrapper.find('input[placeholder="请输入设备编码"]').setValue('demo-device-02')
  await clickSearch(wrapper)
  await flushPromises()

  expect(wrapper.text()).not.toContain('返回异常分组结果')
  expect(wrapper.text()).not.toContain('当前按分组定位')
})

it('stays in the cluster stage and offers retry when the cluster request fails', async () => {
  vi.mocked(pageSystemErrorClusters).mockRejectedValueOnce(new Error('cluster failed'))

  const wrapper = mountView()
  await flushPromises()
  await openClusterStage(wrapper)
  await flushPromises()

  expect(wrapper.text()).toContain('返回异常明细')
  expect(wrapper.text()).toContain('重试')
  expect(wrapper.text()).not.toContain('已回退为全部异常明细')
})
```

- [ ] **Step 4: Re-run the focused view tests after the implementation lands**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
```

Expected: PASS with the new detail-first workflow.


### Task 3: Replace the old `detailClusterMode` flow with an explicit view state machine

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: Introduce the minimal new state shape**

```ts
type ErrorViewMode = 'detail' | 'clusters'

type ClusterRefiner = {
  clusterKey: string
  operationModule: string
  exceptionClass: string
  errorCode: string
}

const errorViewMode = ref<ErrorViewMode>('detail')
const clusterRowsSnapshot = ref<SystemErrorClusterRow[]>([])
const clusterQuerySignature = ref('')
const clusterQueryFormSnapshot = ref<Record<string, unknown> | null>(null)
const selectedClusterRefiner = ref<ClusterRefiner | null>(null)
const clusterStageErrorMessage = ref('')
```

- [ ] **Step 2: Replace eager cluster loading with explicit cluster-stage entry**

```ts
const loadAuditWorkbenchData = () => {
  if (isSystemMode.value) {
    void getAuditLogStats()
    void getAuditLogList()
    return
  }
  void getAuditLogList()
  void getAuditLogStats()
}

const openSystemErrorClusters = async () => {
  syncAdvancedFilterState()
  applyQuickSearchKeywordToFilters()
  syncSearchFormToClusterQueryContext()
  errorViewMode.value = 'clusters'
  clusterStageErrorMessage.value = ''
  await loadSystemErrorClustersFromCurrentForm()
}
```

- [ ] **Step 3: Apply cluster refiners back into detail mode instead of expanding inline**

```ts
const applySystemErrorCluster = async (clusterKey: string) => {
  const cluster = clusterRowsSnapshot.value.find((item) => item.clusterKey === clusterKey)
  if (!cluster) return

  selectedClusterRefiner.value = {
    clusterKey: cluster.clusterKey,
    operationModule: cluster.operationModule ?? '',
    exceptionClass: cluster.exceptionClass ?? '',
    errorCode: cluster.errorCode ?? ''
  }

  searchForm.operationModule = selectedClusterRefiner.value.operationModule
  searchForm.exceptionClass = selectedClusterRefiner.value.exceptionClass
  searchForm.errorCode = selectedClusterRefiner.value.errorCode
  syncAppliedFilters()
  errorViewMode.value = 'detail'
  resetPage()
  clearSelection()
  await getAuditLogList()
}
```

- [ ] **Step 4: Clear stale grouped context when the user changes the active query**

```ts
const clearClusterNavigationContext = () => {
  clusterRowsSnapshot.value = []
  clusterQuerySignature.value = ''
  clusterQueryFormSnapshot.value = null
  selectedClusterRefiner.value = null
  clusterStageErrorMessage.value = ''
}

const triggerSearch = (resetPageFirst = true) => {
  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  clearClusterNavigationContext()
  errorViewMode.value = 'detail'
  if (resetPageFirst) resetPage()
  clearSelection()
  loadAuditWorkbenchData()
}
```


### Task 4: Rework the panel template so clustered view is a full-stage table, not an inline expansion

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`

- [ ] **Step 1: Remove inline expansion and make cluster rows an apply-and-return action**

```vue
<el-table
  v-if="errorViewMode === 'clusters' && clusterRows.length > 0"
  data-testid="system-error-cluster-table"
  :data="clusterRows"
  row-key="clusterKey"
  @row-click="(row) => emit('apply-cluster', row.clusterKey)"
>
```

- [ ] **Step 2: Keep failure and empty states inside the cluster stage only**

```vue
<StandardInlineState
  v-if="errorViewMode === 'clusters' && clusterStageErrorMessage"
  :message="clusterStageErrorMessage"
  tone="warning"
/>

<StandardInlineState
  v-else-if="errorViewMode === 'clusters' && !clusterLoading && clusterRows.length === 0"
  message="当前条件下没有异常分组结果"
  tone="info"
/>
```

- [ ] **Step 3: Keep the detail table as the only detail surface**

```vue
<section v-if="errorViewMode === 'detail'" class="audit-log-system-panel__detail-stage standard-list-surface">
  <el-table data-testid="system-error-detail-table" :data="tableData">
```

- [ ] **Step 4: Re-run the component tests after the template and emits settle**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts
```

Expected: PASS and no remaining assertions about inline expansion or fallback copy.


### Task 5: Verify the whole slice and leave the page in a truthful state

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: Run the focused regression suite**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
```

Expected: PASS with the new detail-first and cluster-stage behavior.

- [ ] **Step 2: Run the UI build**

Run:

```powershell
npm run build
```

Expected: build succeeds with exit code `0`.

- [ ] **Step 3: Smoke-check the requirements against the spec**

Checklist:

```md
- [ ] 默认首屏只加载异常明细，不默认请求分组接口
- [ ] 筛选区右侧有“按异常分组查看”
- [ ] 分组视图整页切换，含“返回异常明细”
- [ ] 点击分组后自动回到明细，并带回分组条件
- [ ] 明细页显示轻量分组定位上下文
- [ ] 修改筛选并重新查询后，旧分组返回入口失效
- [ ] 分组空态和失败态留在分组视图内，不再回退解释到明细页
```
