# Risk Point Detail Drawer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a customer-facing risk-point detail drawer to `/risk-point`, move detailed binding information out of the list table, and keep existing edit / binding-maintenance / pending-promotion flows intact.

**Architecture:** Keep the existing `RiskPointView.vue` page as the orchestration layer, add a dedicated `RiskPointDetailDrawer.vue` read-only component that owns its own detail and binding-group loading with request invalidation, and simplify the list table from a stacked “绑定概览” summary to a lighter “绑定状态” presentation. Reuse existing `getRiskPointById`, `listBindingSummaries`, and `listBindingGroups` APIs instead of adding backend work.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing `StandardWorkbenchPanel`, `StandardDetailDrawer`, `StandardWorkbenchRowActions`, and shared risk-point API contracts.

---

## File Structure

- Create: `spring-boot-iot-ui/src/components/riskPoint/RiskPointDetailDrawer.vue`
  Responsibility: render the read-only risk-point detail drawer, fetch latest detail and grouped formal bindings, and expose edit / maintain-binding / pending-promotion actions.
- Create: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`
  Responsibility: lock detail-drawer loading, summary rendering, grouped metric display, action events, and stale-response protection.
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
  Responsibility: replace the heavy list summary with a lighter binding-status column, add detail entry points, host the new detail drawer, and route detail actions into the existing drawers.
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
  Responsibility: verify the renamed list column, name-click / row-action detail entry, and handoff from detail drawer actions into the existing flows.
- Modify: `docs/02-业务功能与流程说明.md`
  Responsibility: document the new customer-facing detail drawer and the lighter list-state semantics.
- Modify: `docs/03-接口规范与接口清单.md`
  Responsibility: document that `/risk-point` details now consume the existing risk-point detail and grouped-binding APIs as the standard read path.
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: record the list/detail restructuring and the choice to keep binding details out of the main table.
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: update the risk-point page rule from “绑定概览列 + 维护抽屉” to “轻状态列表 + 详情抽屉 + 维护抽屉”.
- Modify: `docs/21-业务功能清单与验收标准.md`
  Responsibility: update acceptance criteria to include detail entry, binding visibility in the detail drawer, and lighter list-state rendering.

### Task 1: Add Red Tests For The New Detail Drawer And Lighter List Behavior

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write the failing component test for the new detail drawer**

Create `RiskPointDetailDrawer.test.ts` with coverage like:

```ts
it('renders initial summary immediately and refreshes with latest detail plus binding groups', async () => {
  mockGetRiskPointById.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      ...createRiskPointRow(),
      description: '最新描述',
      remark: '最新备注',
      updateTime: '2026-04-04 10:00:00'
    }
  })
  mockListBindingGroups.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        deviceId: 21,
        deviceCode: 'DEVICE-021',
        deviceName: '一号倾角仪',
        metricCount: 2,
        metrics: [
          { bindingId: 301, metricIdentifier: 'L1_QJ_1.angle', metricName: '倾角', bindingSource: 'MANUAL' },
          { bindingId: 302, metricIdentifier: 'L1_QJ_1.AZI', metricName: '方位角', bindingSource: 'PENDING_PROMOTION' }
        ]
      }
    ]
  })

  const wrapper = mountDrawer({
    modelValue: true,
    riskPointId: 1,
    initialRiskPoint: createRiskPointRow(),
    initialSummary: { riskPointId: 1, boundDeviceCount: 1, boundMetricCount: 2, pendingBindingCount: 1 }
  })
  await flushPromises()

  expect(wrapper.text()).toContain('示例风险点')
  expect(wrapper.text()).toContain('1 台已绑定设备')
  expect(wrapper.text()).toContain('2 个正式测点')
  expect(wrapper.text()).toContain('一号倾角仪')
  expect(wrapper.text()).toContain('L1_QJ_1.AZI')
  expect(wrapper.text()).toContain('待治理 1 条')
})
```

Also cover stale response invalidation and action forwarding:

```ts
it('ignores stale async responses when the risk point switches quickly', async () => {
  const first = createDeferred<ApiEnvelope<RiskPoint>>()
  const second = createDeferred<ApiEnvelope<RiskPoint>>()
  mockGetRiskPointById.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
  mockListBindingGroups.mockResolvedValue({ code: 200, msg: 'success', data: [] })

  const wrapper = mountDrawer({ modelValue: true, riskPointId: 1, initialRiskPoint: createRiskPointRow() })
  await wrapper.setProps({ riskPointId: 2, initialRiskPoint: { ...createRiskPointRow(), id: 2, riskPointName: '二号风险点' } })

  first.resolve({ code: 200, msg: 'success', data: { ...createRiskPointRow(), id: 1, riskPointName: '旧风险点' } })
  second.resolve({ code: 200, msg: 'success', data: { ...createRiskPointRow(), id: 2, riskPointName: '二号风险点' } })
  await flushPromises()

  expect(wrapper.text()).toContain('二号风险点')
  expect(wrapper.text()).not.toContain('旧风险点')
})

it('emits edit, maintain-binding, and pending-promotion actions from the footer', async () => {
  const wrapper = mountDrawer({ modelValue: true, riskPointId: 1, initialRiskPoint: createRiskPointRow() })
  await flushPromises()

  await wrapper.get('[data-testid=\"detail-edit-action\"]').trigger('click')
  await wrapper.get('[data-testid=\"detail-maintain-binding-action\"]').trigger('click')
  await wrapper.get('[data-testid=\"detail-pending-promotion-action\"]').trigger('click')

  expect(wrapper.emitted('edit')).toHaveLength(1)
  expect(wrapper.emitted('maintain-binding')).toHaveLength(1)
  expect(wrapper.emitted('pending-promotion')).toHaveLength(1)
})
```

- [ ] **Step 2: Write the failing view tests for the list and detail entry**

Extend `RiskPointView.test.ts` with assertions like:

```ts
it('renders a light binding-status column instead of the old stacked binding summary', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: { total: 1, pageNum: 1, pageSize: 10, records: [createRiskPointRow()] }
  })
  mockListBindingSummaries.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [{ riskPointId: 1, boundDeviceCount: 2, boundMetricCount: 5, pendingBindingCount: 1 }]
  })

  const wrapper = mountView()
  await flushPromises()
  await flushPromises()

  expect(wrapper.text()).toContain('绑定状态')
  expect(wrapper.text()).toContain('已绑定 / 待治理')
  expect(wrapper.text()).toContain('2 台设备 · 5 个测点')
  expect(wrapper.text()).not.toContain('绑定概览')
})
```

```ts
it('opens the detail drawer from the risk-point name and the row action', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: { total: 1, pageNum: 1, pageSize: 10, records: [createRiskPointRow()] }
  })
  mockListBindingSummaries.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [{ riskPointId: 1, boundDeviceCount: 1, boundMetricCount: 2, pendingBindingCount: 0 }]
  })

  const wrapper = mountView()
  await flushPromises()
  await flushPromises()

  await wrapper.get('[data-testid=\"risk-point-name-link-1\"]').trigger('click')
  expect(wrapper.findComponent(RiskPointDetailDrawerStub).props('modelValue')).toBe(true)

  await (wrapper.vm as any).handleRiskPointRowAction('detail', createRiskPointRow())
  expect(wrapper.findComponent(RiskPointDetailDrawerStub).props('modelValue')).toBe(true)
})
```

Add one more test to prove detail-drawer actions reopen the existing flows:

```ts
it('routes detail drawer actions into the existing edit and maintenance drawers', async () => {
  const wrapper = mountView()
  ;(wrapper.vm as any).openRiskPointDetail(createRiskPointRow())
  await nextTick()

  wrapper.findComponent(RiskPointDetailDrawerStub).vm.$emit('edit')
  await nextTick()
  expect(wrapper.findAllComponents(StandardFormDrawerStub)[0].props('modelValue')).toBe(true)
})
```

- [ ] **Step 3: Run the targeted Vitest suite to verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts src/__tests__/views/RiskPointView.test.ts --run
```

Expected: FAIL because the new drawer component, name-click detail entry, `detail` row action, and light binding-state rendering do not exist yet.

### Task 2: Implement The Detail Drawer Component And Risk-Point View Wiring

**Files:**
- Create: `spring-boot-iot-ui/src/components/riskPoint/RiskPointDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`

- [ ] **Step 1: Add the new detail drawer component shell**

Create `RiskPointDetailDrawer.vue` around `StandardDetailDrawer` with props and emits like:

```ts
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    riskPointId?: IdType | null
    initialRiskPoint?: RiskPoint | null
    initialSummary?: RiskPointBindingSummary | null
  }>(),
  {
    riskPointId: undefined,
    initialRiskPoint: null,
    initialSummary: null
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
  edit: []
  'maintain-binding': []
  'pending-promotion': []
}>()
```

Render sections for:

```vue
<section class="detail-panel detail-panel--hero">
  <div class="detail-summary-grid">
    <article class="detail-summary-card">
      <span class="detail-summary-card__label">所属组织</span>
      <strong class="detail-summary-card__value">{{ detail?.orgName || initialRiskPoint?.orgName || '--' }}</strong>
      <p class="detail-summary-card__hint">区域：{{ detail?.regionName || initialRiskPoint?.regionName || '--' }}</p>
    </article>
    <article class="detail-summary-card">
      <span class="detail-summary-card__label">正式绑定</span>
      <strong class="detail-summary-card__value">{{ summaryDeviceMetricText }}</strong>
      <p class="detail-summary-card__hint">待治理 {{ summaryPendingCount }} 条</p>
    </article>
  </div>
</section>
```

- [ ] **Step 2: Implement async loading with request invalidation**

Inside the component, load detail and binding groups whenever the drawer opens or the `riskPointId` changes:

```ts
let latestDetailRequestId = 0

const resetState = () => {
  detail.value = props.initialRiskPoint || null
  bindingGroups.value = []
  loading.value = false
  errorMessage.value = ''
  latestDetailRequestId += 1
}

const loadDrawerData = async () => {
  if (!props.modelValue || !props.riskPointId) {
    resetState()
    return
  }
  const requestId = ++latestDetailRequestId
  loading.value = true
  errorMessage.value = ''
  detail.value = props.initialRiskPoint || null

  const [detailResult, groupResult] = await Promise.allSettled([
    getRiskPointById(props.riskPointId),
    listBindingGroups(props.riskPointId)
  ])
  if (requestId !== latestDetailRequestId) {
    return
  }
  // map fulfilled results into detail/bindingGroups; otherwise set friendly error
}
```

Add friendly computed text helpers for:

- `summaryStatusText`
- `summaryDeviceMetricText`
- `summaryPendingCount`
- `drawerTags`
- `drawerTitle`
- `drawerSubtitle`

- [ ] **Step 3: Replace the heavy list column with a light binding-state column and add detail entry points**

In `RiskPointView.vue`, change:

```ts
type RiskPointRowActionCommand = 'detail' | 'edit' | 'maintain-binding' | 'pending-promotion' | 'delete'
```

Update row actions:

```ts
const getRiskPointRowActions = () => [
  { command: 'detail' as const, label: '详情' },
  { command: 'edit' as const, label: '编辑' },
  { command: 'maintain-binding' as const, label: '维护绑定' },
  { command: 'pending-promotion' as const, label: '待治理转正' },
  { command: 'delete' as const, label: '删除' }
]
```

Add detail state:

```ts
const riskPointDetailVisible = ref(false)
const detailRiskPoint = ref<RiskPoint | null>(null)

const openRiskPointDetail = (row: RiskPoint) => {
  detailRiskPoint.value = row
  riskPointDetailVisible.value = true
}

const handleRiskPointDetailClose = () => {
  detailRiskPoint.value = null
}
```

Change the name column to a clickable link:

```vue
<StandardTableTextColumn prop="riskPointName" label="风险点名称" :min-width="180">
  <template #default="{ row }">
    <StandardActionLink
      :data-testid="`risk-point-name-link-${row.id}`"
      @click="openRiskPointDetail(row)"
    >
      {{ row.riskPointName }}
    </StandardActionLink>
  </template>
</StandardTableTextColumn>
```

Replace the old summary column with a lighter state column:

```vue
<el-table-column label="绑定状态" :min-width="180">
  <template #default="{ row }">
    <div class="risk-point-binding-state">
      <strong>{{ getBindingStateLabel(row) }}</strong>
      <span v-if="hasFormalBindings(row)">{{ getBindingStateSummaryText(row) }}</span>
      <span v-if="getBindingSummaryPendingCount(row) > 0">{{ getBindingSummaryPendingText(row) }}</span>
    </div>
  </template>
</el-table-column>
```

- [ ] **Step 4: Mount the detail drawer and wire its actions to existing flows**

Host the component in `RiskPointView.vue`:

```vue
<RiskPointDetailDrawer
  v-model="riskPointDetailVisible"
  :risk-point-id="detailRiskPoint?.id"
  :initial-risk-point="detailRiskPoint"
  :initial-summary="detailRiskPoint ? bindingSummaryMap[Number(detailRiskPoint.id)] || null : null"
  @close="handleRiskPointDetailClose"
  @edit="detailRiskPoint && handleEdit(detailRiskPoint)"
  @maintain-binding="detailRiskPoint && openBindingMaintenance(detailRiskPoint)"
  @pending-promotion="detailRiskPoint && handleOpenPendingPromotion(detailRiskPoint)"
/>
```

When routing out of detail into another drawer, close the detail drawer first so only one side panel stays active:

```ts
const handoffFromDetailToEdit = () => {
  const row = detailRiskPoint.value
  riskPointDetailVisible.value = false
  detailRiskPoint.value = null
  if (row) {
    void handleEdit(row)
  }
}
```

- [ ] **Step 5: Run the new and updated tests to verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts src/__tests__/views/RiskPointView.test.ts --run
```

Expected: PASS with the new detail drawer, light list-state rendering, and detail-entry behavior.

### Task 3: Update Documentation And Re-verify The Risk-Point UI Slice

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update the business and API docs**

Document these points:

```md
- `/risk-point` 列表当前只保留轻量“绑定状态”列，完整设备与测点详情下沉到详情抽屉。
- 风险点名称与行操作 `详情` 当前都会打开只读详情抽屉。
- 详情抽屉内统一承接基础档案、正式绑定设备与测点、待治理提醒，并提供 `编辑风险点 / 维护绑定 / 待治理转正` 三个后续入口。
```

- [ ] **Step 2: Update frontend-governance and acceptance docs**

Add the rule and acceptance points:

```md
- `/risk-point` 主表不得回流设备卡片和测点清单；列表只表达轻状态，详情抽屉承接绑定明细。
- 风险对象中心当前必须同时支持“点名称看详情”和“行操作进详情”。
- 风险点详情抽屉必须保持只读心智，不与编辑抽屉混用。
```

- [ ] **Step 3: Run focused verification for this work**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts src/__tests__/views/RiskPointView.test.ts --run
git diff --check
```

Expected:

- Vitest: all specified suites pass
- `git diff --check`: no whitespace or merge-marker issues
