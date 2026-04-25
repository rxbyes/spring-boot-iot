# Risk Point Formal Binding Search And Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/risk-point -> 风险绑定 -> 新增正式绑定` support device-code search and load formal metric choices only from the risk-point catalog API.

**Architecture:** Keep the existing `RiskPointView.vue` bind drawer, but align it with the already-governed `RiskPointBindingMaintenanceDrawer` behavior: the device selector becomes filterable, metric loading switches from the generic IoT metrics API to `GET /api/risk-point/devices/{deviceId}/formal-metrics`, and the drawer shows the existing empty-state hint when the selected device has no formal catalog metrics. No backend behavior changes are required because the dedicated API and write-side guard already exist.

**Tech Stack:** Vue 3, Element Plus, Vitest, Vite, existing `spring-boot-iot-ui` API modules and docs markdown.

---

### Task 1: Lock the legacy bind drawer regression with a failing view test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Reference: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write the failing test**

Add dedicated mocks for the formal metric API and assert the legacy bind drawer uses the same contract as the maintenance drawer.

```ts
const {
  mockPageRiskPointList,
  mockGetRiskPointById,
  mockAddRiskPoint,
  mockUpdateRiskPoint,
  mockDeleteRiskPoint,
  mockBindDevice,
  mockListBindingSummaries,
  mockListBindingGroups,
  mockListPendingBindings,
  mockGetPendingCandidates,
  mockPromotePendingBinding,
  mockIgnorePendingBinding,
  mockListBindableDevices,
  mockListFormalBindingMetricOptions,
  mockListOrganizationTree,
  mockListRegions,
  mockListRegionTree,
  mockGetRegion,
  mockListUsers,
  mockGetUser,
  mockGetDictByCode,
  mockListMissingBindings,
  mockElMessageSuccess,
  mockElMessageError,
  mockElMessageWarning,
  mockPermissionStore,
  mockRoute
} = vi.hoisted(() => ({
  mockPageRiskPointList: vi.fn(),
  mockGetRiskPointById: vi.fn(),
  mockAddRiskPoint: vi.fn(),
  mockUpdateRiskPoint: vi.fn(),
  mockDeleteRiskPoint: vi.fn(),
  mockBindDevice: vi.fn(),
  mockListBindingSummaries: vi.fn(),
  mockListBindingGroups: vi.fn(),
  mockListPendingBindings: vi.fn(),
  mockGetPendingCandidates: vi.fn(),
  mockPromotePendingBinding: vi.fn(),
  mockIgnorePendingBinding: vi.fn(),
  mockListBindableDevices: vi.fn(),
  mockListFormalBindingMetricOptions: vi.fn(),
  mockListOrganizationTree: vi.fn(),
  mockListRegions: vi.fn(),
  mockListRegionTree: vi.fn(),
  mockGetRegion: vi.fn(),
  mockListUsers: vi.fn(),
  mockGetUser: vi.fn(),
  mockGetDictByCode: vi.fn(),
  mockListMissingBindings: vi.fn(),
  mockElMessageSuccess: vi.fn(),
  mockElMessageError: vi.fn(),
  mockElMessageWarning: vi.fn(),
  mockPermissionStore: {
    userInfo: {
      id: 9001,
      username: 'editor',
      realName: '当前编辑人',
      displayName: '当前编辑人',
      phone: '13900000001',
      orgId: 7101,
      orgName: '平台运维中心'
    }
  },
  mockRoute: {
    query: {}
  }
}))

vi.mock('@/api/riskPoint', () => ({
  pageRiskPointList: mockPageRiskPointList,
  getRiskPointById: mockGetRiskPointById,
  addRiskPoint: mockAddRiskPoint,
  updateRiskPoint: mockUpdateRiskPoint,
  deleteRiskPoint: mockDeleteRiskPoint,
  bindDevice: mockBindDevice,
  listBindingSummaries: mockListBindingSummaries,
  listBindingGroups: mockListBindingGroups,
  listPendingBindings: mockListPendingBindings,
  getPendingBindingCandidates: mockGetPendingCandidates,
  promotePendingBinding: mockPromotePendingBinding,
  ignorePendingBinding: mockIgnorePendingBinding,
  listBindableDevices: mockListBindableDevices,
  listFormalBindingMetricOptions: mockListFormalBindingMetricOptions
}))

it('loads formal metrics from the risk-point API and keeps the bind drawer device selector filterable', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [createRiskPointRow()]
    }
  })
  mockListFormalBindingMetricOptions.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [{ identifier: 'dispsX', name: 'X轴位移', riskMetricId: 6102, dataType: 'double' }]
  })

  const wrapper = mountView()
  await flushPromises()
  ;(wrapper.vm as any).bindDeviceVisible = true
  ;(wrapper.vm as any).deviceList = [
    { id: 2001, productId: 1001, deviceCode: 'CXH15522812', deviceName: '多维检测仪', orgId: 7101, orgName: '平台运维中心' }
  ]

  ;(wrapper.vm as any).bindForm.deviceId = 2001
  await flushPromises()

  const deviceSelect = wrapper.find('[data-testid="risk-point-bind-device-select"]')
  expect(deviceSelect.attributes('data-filterable')).toBe('true')
  expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith(2001)
  expect((wrapper.vm as any).metricList).toEqual([
    { identifier: 'dispsX', name: 'X轴位移', riskMetricId: 6102, dataType: 'double' }
  ])
})

it('shows the formal-catalog empty hint when the selected device has no published catalog metrics', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [createRiskPointRow()]
    }
  })
  mockListFormalBindingMetricOptions.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: []
  })

  const wrapper = mountView()
  await flushPromises()
  ;(wrapper.vm as any).bindDeviceVisible = true
  ;(wrapper.vm as any).deviceList = [
    { id: 2001, productId: 1001, deviceCode: 'CXH15522812', deviceName: '多维检测仪', orgId: 7101, orgName: '平台运维中心' }
  ]

  ;(wrapper.vm as any).bindForm.deviceId = 2001
  await flushPromises()

  expect(wrapper.text()).toContain('当前设备所属产品暂无可用于风险绑定的正式目录字段')
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts
```

Expected: FAIL because `RiskPointView.vue` still imports `getDeviceMetricOptions` from `@/api/iot`, the device selector is not filterable, and the empty hint is not rendered.

- [ ] **Step 3: Commit the red state only if you intentionally checkpoint locally; otherwise continue immediately**

Do not keep partial production changes here. Move straight into the minimal implementation after confirming the failure reason is the legacy drawer regression.

### Task 2: Implement the frontend fix in `RiskPointView.vue`

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts` (only if exported types need reuse; otherwise leave untouched)
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write the minimal implementation**

Replace the generic metric API import with the dedicated risk-point API, make the device selector filterable, and show the explicit empty hint when a selected device has no formal metrics.

```ts
import {
  addRiskPoint,
  bindDevice,
  deleteRiskPoint,
  getPendingBindingCandidates,
  getRiskPointById,
  ignorePendingBinding,
  listBindableDevices,
  listBindingGroups,
  listBindingSummaries,
  listFormalBindingMetricOptions,
  listPendingBindings,
  pageRiskPointList,
  promotePendingBinding,
  updateRiskPoint
} from '@/api/riskPoint';
```

```ts
const bindMetricEmptyText = computed(() => {
  if (!getIdKey(bindForm.deviceId) || metricList.value.length > 0) {
    return ''
  }
  return '当前设备所属产品暂无可用于风险绑定的正式目录字段。'
})

const loadMetricOptions = async (deviceId: string | number) => {
  try {
    const res = await listFormalBindingMetricOptions(deviceId)
    if (res.code === 200) {
      metricList.value = res.data || []
    }
  } catch (error) {
    logRiskPointRequestError('加载测点选项失败', error)
    showRiskPointRequestError(error, '加载测点列表失败')
  }
}
```

```vue
<el-form-item label="设备">
  <el-select
    v-model="bindForm.deviceId"
    filterable
    placeholder="请选择设备"
    data-testid="risk-point-bind-device-select"
  >
    <el-option
      v-for="device in deviceList"
      :key="device.id"
      :label="`${device.deviceCode} - ${device.deviceName}`"
      :value="device.id"
    >
      {{ device.deviceCode }} - {{ device.deviceName }}
    </el-option>
  </el-select>
</el-form-item>
<el-form-item label="测点">
  <el-select
    v-model="bindForm.metricIdentifier"
    placeholder="请选择测点"
    data-testid="risk-point-bind-metric-select"
  >
    <el-option
      v-for="metric in metricList"
      :key="metric.identifier"
      :label="metric.name"
      :value="metric.identifier"
    >
      {{ metric.name }}
    </el-option>
  </el-select>
</el-form-item>
<p v-if="bindMetricEmptyText" class="ops-drawer-form__hint" data-testid="risk-point-bind-metric-empty">
  {{ bindMetricEmptyText }}
</p>
```

- [ ] **Step 2: Run the targeted view test to verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts
```

Expected: PASS, including the new assertions for filterable device search, formal metric loading, and empty-state hint.

- [ ] **Step 3: Run the closely related binding drawer regression tests**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts
```

Expected: PASS, proving the embedded workbench drawer and the legacy `RiskPointView` drawer now share the same formal-binding contract instead of diverging.

- [ ] **Step 4: Refactor only if duplication is obvious**

If both the view and the maintenance drawer now repeat the exact same empty-hint text or label-building logic, extract only a tiny local helper inside `RiskPointView.vue`. Do not widen this into a shared composable during this fix.

- [ ] **Step 5: Commit the implementation**

```bash
git add spring-boot-iot-ui/src/views/RiskPointView.vue spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "fix: align risk point bind drawer with formal metric catalog"
```

### Task 3: Update docs and verify the final behavior

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Check only: `README.md`
- Check only: `AGENTS.md`

- [ ] **Step 1: Update the API/behavior doc**

Add a short clarification under the existing risk-point binding API section that the `RiskPointView` legacy bind drawer now uses the same `formal-metrics` contract and device-code search behavior as the maintenance workbench.

```md
- `2026-04-25` 起，`/risk-point` 列表页里仍保留的“新增正式绑定”旧抽屉也已与风险绑定工作台维护区对齐：设备选择框支持按 `deviceCode` 输入过滤，选中设备后只调用 `GET /api/risk-point/devices/{deviceId}/formal-metrics` 读取已发布目录字段；若当前设备产品没有可用目录字段，页面会直接提示“当前设备所属产品暂无可用于风险绑定的正式目录字段”，不再回退到通用设备测点接口。
```

- [ ] **Step 2: Record the change in the changelog**

Add a dated entry near the top of `docs/08-变更记录与技术债清单.md` summarizing the legacy drawer fix and the targeted verification command.

```md
- 2026-04-25：`/risk-point` 列表页旧“新增正式绑定”抽屉已与正式风险目录口径重新对齐。此前 `RiskPointView.vue` 仍通过 `GET /api/device/{deviceId}/metrics` 加载通用设备测点，且设备框未开启按编号搜索，导致像 `CXH15522812` 这类监测设备在该入口中既难以定位，也可能看到与正式绑定规则不一致的候选。当前 `spring-boot-iot-ui` 已把旧抽屉设备框改为支持按 `deviceCode` 输入过滤，并统一切到 `GET /api/risk-point/devices/{deviceId}/formal-metrics`；当目录为空时，页面会明确提示“当前设备所属产品暂无可用于风险绑定的正式目录字段”，不再混入通用设备测点。定向验证：`npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`。README.md 与 AGENTS.md 检查后无需变更。
```

- [ ] **Step 3: Run the final verification command set**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts
```

Expected: PASS for all three files.

- [ ] **Step 4: Commit the docs update**

```bash
git add docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md
git commit -m "docs: record risk point formal binding drawer alignment"
```

- [ ] **Step 5: Final cleanup checkpoint**

Run:

```bash
git status --short
```

Expected: no unexpected modified files beyond the planned frontend and docs changes.
