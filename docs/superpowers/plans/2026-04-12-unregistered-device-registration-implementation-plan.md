# Unregistered Device Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `/devices` unregistered rows reuse the full device form and submit directly into the existing archive flow so the record becomes registered after save.

**Architecture:** Keep the backend contract unchanged and treat unregistered-row editing as a frontend-only `register` mode layered on top of the existing device form. The view will distinguish `create / edit / register`, route `register` through `deviceApi.addDevice`, remove the stale unregistered row from local caches, optionally inject the new registered row back into the current page, then silently refresh from the server.

**Tech Stack:** Vue 3 + `<script setup lang="ts">`, Vitest + Vue Test Utils, existing `deviceApi`, Markdown docs under `docs/`

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
  - Add explicit form mode state, permission-aware row edit actions, register-specific copy, register submit branch, and register source-row cache cleanup.
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
  - Add mutable permission mocking plus focused tests for unregistered edit visibility and register submit behavior.
- Modify: `docs/02-业务功能与流程说明.md`
  - Replace the “未登记上报名单保持只读” wording with the new “可详情排障 + 编辑建档” behavior.
- Modify: `docs/03-接口规范与接口清单.md`
  - Document that `/devices` unregistered editing reuses `POST /api/device/add` and turns the row into a registered device.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Add the 2026-04-12 change-log entry and verification evidence for the device workbench change.

### Task 1: Expose Unregistered Edit Entry And Register Copy

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`

- [ ] **Step 1: Write the failing tests for unregistered edit visibility and register-mode drawer copy**

Add a mutable permission set near the existing hoisted mocks, replace the permission-store mock to read from that set, then append the two tests below to `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`:

```ts
const { mockRoute, mockRouter, mockPageDevices, mockPermissions } = vi.hoisted(() => ({
  mockRoute: {
    path: '/devices',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageDevices: vi.fn(),
  mockPermissions: new Set<string>([
    'iot:devices:add',
    'iot:devices:update',
    'iot:devices:delete',
    'iot:devices:export',
    'iot:devices:replace'
  ])
}))

function setMockPermissions(...permissions: string[]) {
  mockPermissions.clear()
  permissions.forEach((permission) => mockPermissions.add(permission))
}

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    hasPermission: (code: string) => mockPermissions.has(code)
  })
}))

it('shows edit for unregistered rows when create permission exists', async () => {
  setMockPermissions('iot:devices:add')
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).tableData = [
    {
      sourceRecordId: 7001,
      productKey: 'shadow-product',
      productName: '未登记产品',
      deviceCode: 'shadow-device-01',
      deviceName: '未登记设备',
      registrationStatus: 0,
      assetSourceType: 'invalid_report_state',
      createTime: '2026-04-12T09:00:00'
    }
  ]
  await nextTick()

  const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
  const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')
  const tableRowActions = rowActions.find((component) => component.props('variant') === 'table')

  expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
    '详情',
    '编辑'
  ])
  expect(((tableRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
    '详情',
    '编辑'
  ])
})

it('switches unregistered edit into register mode with add-permission submit copy', async () => {
  setMockPermissions('iot:devices:add')
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).handleEdit({
    sourceRecordId: 7001,
    productKey: 'shadow-product',
    deviceCode: 'shadow-device-01',
    deviceName: '未登记设备',
    registrationStatus: 0,
    assetSourceType: 'invalid_report_state'
  })
  await nextTick()

  const formDrawer = wrapper.findComponent(StandardFormDrawerStub)
  expect(formDrawer.props('title')).toBe('登记设备')
  expect(String(formDrawer.props('subtitle'))).toContain('未登记上报线索')
  expect(wrapper.text()).toContain('提交设备建档')
  expect(wrapper.text()).not.toContain('保存设备变更')
})
```

- [ ] **Step 2: Run the focused Vitest contract and verify it fails**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts -t "shows edit for unregistered rows when create permission exists|switches unregistered edit into register mode with add-permission submit copy"
```

Expected:

```text
FAIL  src/__tests__/views/DeviceWorkbenchView.test.ts
+ expected directItems to include "编辑" for registrationStatus=0 rows
+ expected form drawer title to be "登记设备"
```

- [ ] **Step 3: Write the minimal implementation for form mode, copy, and row-action permissions**

Patch `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` by introducing an explicit form mode, changing edit permission resolution, and routing unregistered edit opens into `register` mode:

```ts
type DeviceFormMode = 'create' | 'edit' | 'register'

const formMode = ref<DeviceFormMode>('create')
const registerSourceRow = ref<Device | null>(null)

const formTitle = computed(() => {
  if (formMode.value === 'edit') {
    return '编辑设备'
  }
  if (formMode.value === 'register') {
    return '登记设备'
  }
  return '新增设备'
})

const formSubtitle = computed(() =>
  formMode.value === 'register'
    ? '基于未登记上报线索补齐正式设备档案，提交后会直接转为已登记设备。'
    : '统一通过右侧抽屉维护设备主数据、父子拓扑、状态、认证字段和部署信息。'
)

const formSubmitText = computed(() => (formMode.value === 'edit' ? '保存设备变更' : '提交设备建档'))
const submitPermission = computed(() => (formMode.value === 'edit' ? 'iot:devices:update' : 'iot:devices:add'))

function hasEditPermissionForRow(row?: Device | null) {
  if (!row) {
    return false
  }
  return isRegisteredDeviceRow(row)
    ? permissionStore.hasPermission('iot:devices:update')
    : permissionStore.hasPermission('iot:devices:add')
}

function canEditDeviceRow(row?: Device | null) {
  return Boolean(row)
}

function getDeviceDirectActions(row: Device): DeviceDirectAction[] {
  const actions: DeviceDirectAction[] = [{ key: 'detail', command: 'detail', label: '详情' }]

  if (canEditDeviceRow(row) && hasEditPermissionForRow(row)) {
    actions.push({ key: 'edit', command: 'edit', label: '编辑' })
  }

  return actions
}

function handleAdd() {
  activeEditSessionId += 1
  abortEditRequest()
  formMode.value = 'create'
  registerSourceRow.value = null
  editingDeviceId.value = null
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty()
  formVisible.value = true
  formRef.value?.clearValidate()
  void loadProducts()
  void loadDeviceOptions()
}

function handleEdit(row: Device) {
  if (!canEditDeviceRow(row) || !hasEditPermissionForRow(row)) {
    return
  }

  const cachedDetail = getCachedDeviceDetail(row)
  const editSnapshot = resolveDetailSnapshot(row, cachedDetail)

  activeEditSessionId += 1
  const editSessionId = activeEditSessionId
  abortEditRequest()
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty(editSnapshot)
  formVisible.value = true
  formRef.value?.clearValidate()
  void loadProducts()
  void loadDeviceOptions()

  if (isRegisteredDeviceRow(row) && row.id !== undefined && row.id !== null && row.id !== '') {
    formMode.value = 'edit'
    registerSourceRow.value = null
    editingDeviceId.value = row.id
    void refreshEditableDetail(row, editSessionId, cachedDetail)
    return
  }

  formMode.value = 'register'
  registerSourceRow.value = { ...row }
  editingDeviceId.value = null
}

function handleFormClose() {
  activeEditSessionId += 1
  abortEditRequest()
  formRef.value?.clearValidate()
  clearFormRefreshState()
  formDirtySinceOpen = false
  applyFormDataWithoutDirty()
  formMode.value = 'create'
  registerSourceRow.value = null
  editingDeviceId.value = null
}

watch(
  formData,
  () => {
    if (!formVisible.value || formMode.value === 'create' || suppressFormDirtyTracking) {
      return
    }
    formDirtySinceOpen = true
  },
  { deep: true, flush: 'sync' }
)
```

Update the template copy in the same file:

```vue
<StandardFormDrawer
  v-model="formVisible"
  :title="formTitle"
  :subtitle="formSubtitle"
  size="44rem"
  @close="handleFormClose"
>
  <div class="ops-drawer-note">
    <strong>维护提示</strong>
    <span>
      {{
        formMode === 'register'
          ? '当前记录来自未登记上报线索；请核对产品归属、设备编码、父子拓扑和认证字段，提交后会直接转成已登记设备。'
          : '设备列表先服务“库存可见、责任清晰、操作可追踪”。建议至少补齐产品归属、设备编码、激活状态、设备状态和部署位置。'
      }}
    </span>
  </div>

  <StandardDrawerFooter
    :confirm-loading="submitLoading"
    :confirm-text="formSubmitText"
    @cancel="formVisible = false"
    @confirm="handleSubmit"
  >
    <StandardButton
      v-permission="submitPermission"
      action="confirm"
      class="standard-drawer-footer__button standard-drawer-footer__button--primary"
      :loading="submitLoading"
      @click="handleSubmit"
    >
      {{ formSubmitText }}
    </StandardButton>
  </StandardDrawerFooter>
</StandardFormDrawer>
```

- [ ] **Step 4: Run the focused tests again and verify they pass**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts -t "shows edit for unregistered rows when create permission exists|switches unregistered edit into register mode with add-permission submit copy"
```

Expected:

```text
PASS  src/__tests__/views/DeviceWorkbenchView.test.ts
+ 2 passed
```

- [ ] **Step 5: Commit the entry-point and copy changes**

Run:

```bash
git add spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: open unregistered device registration form"
```

### Task 2: Route Register Submit Through `addDevice` And Reconcile Local Rows

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`

- [ ] **Step 1: Write the failing tests for register submit behavior**

Append the submit-flow tests below to `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`:

```ts
it('submits register mode through addDevice and removes the stale row from the unregistered view', async () => {
  setMockPermissions('iot:devices:add')
  const { deviceApi } = await import('@/api/device')
  vi.mocked(deviceApi.addDevice).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      id: 8101,
      productKey: 'shadow-product',
      productName: '正式产品',
      deviceCode: 'shadow-device-01',
      deviceName: '北坡正式设备',
      registrationStatus: 1,
      activateStatus: 1,
      deviceStatus: 1
    }
  })

  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).formRef = {
    validate: vi.fn().mockResolvedValue(true),
    clearValidate: vi.fn()
  }
  ;(wrapper.vm as any).tableData = [
    {
      sourceRecordId: 7001,
      productKey: 'shadow-product',
      productName: '未登记产品',
      deviceCode: 'shadow-device-01',
      deviceName: '未登记设备',
      registrationStatus: 0,
      assetSourceType: 'invalid_report_state'
    }
  ]
  ;(wrapper.vm as any).pagination.total = 1
  ;(wrapper.vm as any).appliedFilters.registrationStatus = 0
  ;(wrapper.vm as any).handleEdit((wrapper.vm as any).tableData[0])
  await nextTick()

  await (wrapper.vm as any).handleSubmit()
  await flushPromises()
  await nextTick()

  expect(deviceApi.addDevice).toHaveBeenCalledWith(
    expect.objectContaining({
      productKey: 'shadow-product',
      deviceCode: 'shadow-device-01'
    })
  )
  expect(deviceApi.updateDevice).not.toHaveBeenCalled()
  expect((wrapper.vm as any).tableData).toEqual([])
  expect((wrapper.vm as any).pagination.total).toBe(0)
  expect(vi.mocked(ElMessage.success)).toHaveBeenCalledWith('登记成功')
})

it('injects the new registered row back into the current result when register mode finishes in the combined view', async () => {
  setMockPermissions('iot:devices:add')
  const { deviceApi } = await import('@/api/device')
  vi.mocked(deviceApi.addDevice).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      id: 8102,
      productKey: 'shadow-product',
      productName: '正式产品',
      deviceCode: 'shadow-device-01',
      deviceName: '北坡正式设备',
      registrationStatus: 1,
      activateStatus: 1,
      deviceStatus: 1
    }
  })

  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).formRef = {
    validate: vi.fn().mockResolvedValue(true),
    clearValidate: vi.fn()
  }
  ;(wrapper.vm as any).tableData = [
    {
      sourceRecordId: 7001,
      productKey: 'shadow-product',
      productName: '未登记产品',
      deviceCode: 'shadow-device-01',
      deviceName: '未登记设备',
      registrationStatus: 0,
      assetSourceType: 'invalid_report_state'
    }
  ]
  ;(wrapper.vm as any).pagination.pageNum = 1
  ;(wrapper.vm as any).pagination.pageSize = 10
  ;(wrapper.vm as any).pagination.total = 1
  ;(wrapper.vm as any).appliedFilters.registrationStatus = undefined
  ;(wrapper.vm as any).handleEdit((wrapper.vm as any).tableData[0])
  await nextTick()

  await (wrapper.vm as any).handleSubmit()
  await flushPromises()
  await nextTick()

  expect((wrapper.vm as any).tableData).toEqual([
    expect.objectContaining({
      id: 8102,
      deviceCode: 'shadow-device-01',
      registrationStatus: 1
    })
  ])
  expect((wrapper.vm as any).pagination.total).toBe(1)
})
```

- [ ] **Step 2: Run the focused submit-flow tests and verify they fail**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts -t "submits register mode through addDevice and removes the stale row from the unregistered view|injects the new registered row back into the current result when register mode finishes in the combined view"
```

Expected:

```text
FAIL  src/__tests__/views/DeviceWorkbenchView.test.ts
+ expected success toast "登记成功" but received "新增成功"
+ expected stale unregistered row to be removed from tableData
```

- [ ] **Step 3: Write the minimal implementation for register submit reconciliation**

Patch `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` to track the source row for register mode, branch `handleSubmit`, and reconcile local state:

```ts
function finalizeArchiveCreate(created: Device, sourceRow?: Device | null) {
  clearDeviceOptionCache()
  cacheDeviceDetail(created)
  clearSelection()

  let nextTotal = pagination.total
  const removedCount = sourceRow ? removeLocalTableRows([sourceRow]) : 0
  if (removedCount > 0) {
    nextTotal = Math.max(0, nextTotal - removedCount)
    removeCachedDeviceDetail(sourceRow)
  }

  const shouldInsertCreated = appliedFilters.registrationStatus !== 0 && matchesCurrentFilters(created)
  if (shouldInsertCreated) {
    if (removedCount === 0) {
      nextTotal += 1
    }
    if (pagination.pageNum === 1) {
      prependLocalTableRow(created)
    } else {
      rebuildVisibleDevicePageCache()
    }
  } else {
    rebuildVisibleDevicePageCache()
  }

  setTotal(nextTotal)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const submitMode = formMode.value
  submitLoading.value = true
  try {
    if (submitMode === 'edit') {
      const res = await deviceApi.updateDevice(editingDeviceId.value as string | number, { ...formData })
      clearDeviceOptionCache()
      cacheDeviceDetail(res.data)
      if (matchesCurrentFilters(res.data)) {
        mergeLocalTableRow(res.data)
      } else if (removeLocalTableRow(res.data)) {
        setTotal(pagination.total - 1)
      }
      rebuildVisibleDevicePageCache()
      ElMessage.success('更新成功')
    } else {
      const res = await deviceApi.addDevice({ ...formData })
      if (submitMode === 'register') {
        finalizeArchiveCreate(res.data, registerSourceRow.value)
        ElMessage.success('登记成功')
      } else {
        clearDeviceOptionCache()
        cacheDeviceDetail(res.data)
        clearSelection()
        if (matchesCurrentFilters(res.data)) {
          setTotal(pagination.total + 1)
          if (pagination.pageNum === 1) {
            prependLocalTableRow(res.data)
          } else {
            rebuildVisibleDevicePageCache()
          }
        } else {
          rebuildVisibleDevicePageCache()
        }
        ElMessage.success('新增成功')
      }
    }

    formVisible.value = false
    void loadDevicePage({
      silent: true,
      force: true,
      silentMessage:
        submitMode === 'edit'
          ? '已提交设备更新，正在后台刷新列表。'
          : submitMode === 'register'
            ? '已完成设备登记，正在后台刷新列表。'
            : '已新增设备，正在后台刷新列表。'
    })
  } catch (error) {
    console.error('提交设备失败', error)
    ElMessage.error(
      error instanceof Error
        ? submitMode === 'register'
          ? `登记失败：${error.message}`
          : error.message
        : submitMode === 'register'
          ? '登记失败'
          : '提交设备失败'
    )
  } finally {
    submitLoading.value = false
  }
}

function handleFormClose() {
  activeEditSessionId += 1
  abortEditRequest()
  formRef.value?.clearValidate()
  clearFormRefreshState()
  formDirtySinceOpen = false
  applyFormDataWithoutDirty()
  formMode.value = 'create'
  registerSourceRow.value = null
  editingDeviceId.value = null
}
```

Keep `handleEdit()` assigning `registerSourceRow.value = { ...row }` for unregistered rows and `registerSourceRow.value = null` for `create` and `edit`.

- [ ] **Step 4: Run the submit-flow tests again and verify they pass**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts -t "submits register mode through addDevice and removes the stale row from the unregistered view|injects the new registered row back into the current result when register mode finishes in the combined view"
```

Expected:

```text
PASS  src/__tests__/views/DeviceWorkbenchView.test.ts
+ 2 passed
```

- [ ] **Step 5: Commit the register submit flow**

Run:

```bash
git add spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: register unregistered devices from device workbench"
```

### Task 3: Sync Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Verify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Verify: `spring-boot-iot-ui`

- [ ] **Step 1: Update the behavior and API docs in place**

Apply the following documentation edits:

In `docs/02-业务功能与流程说明.md`, replace the old read-only bullet with:

```md
- 未登记上报名单当前仍用于“发现设备、补档建账、定位失败阶段”，但不再是只读名单：`/devices` 中可继续查看详情、失败阶段、Topic、TraceId 与原始载荷，同时也可直接通过 `编辑` 打开与已登记设备相同的完整表单完成正式建档；提交成功后，该记录会转成 `已登记` 设备，未登记来源不会继续回流。
```

In `docs/03-接口规范与接口清单.md`, append this bullet immediately after the existing `POST /api/device/add` invalid-report note:

```md
- `/devices` 当前允许对未登记行直接点击 `编辑` 完成建档；前端不会调用新的专用接口，而是复用现有 `POST /api/device/add` 提交完整设备表单。建档成功后，该记录会转入 `iot_device` 主档，并继续复用现有 invalid report resolve 逻辑收口未登记最新态。
```

At the top of `docs/08-变更记录与技术债清单.md` under `### 1.1 协作流程与分支治理`, insert:

```md
- 2026-04-12：`/devices` 设备资产中心已放开未登记设备的“编辑转建档”闭环。此前未登记上报名单只能详情排障，前端点击编辑会直接提示“未登记设备暂不支持编辑”；当前 `DeviceWorkbenchView.vue` 已引入 `create / edit / register` 三态表单模式，未登记行在具备 `iot:devices:add` 权限时也会显示 `编辑`，打开后复用完整设备表单并以 `POST /api/device/add` 正式建档。提交成功后页面会移除旧未登记摘要、按当前筛选条件回填新已登记设备，并继续静默刷新列表；已登记设备原有 `PUT /api/device/{id}` 编辑链路保持不变。定向验证已通过：`npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts`、`npm --prefix spring-boot-iot-ui run build`。本轮同步更新 `docs/02`、`docs/03`、`docs/08`；README.md 与 AGENTS.md 检查后无需变更。
```

- [ ] **Step 2: Run doc sanity checks and the focused frontend regression**

Run:

```powershell
git grep -n "未登记上报名单当前只用于“发现设备、补档建账、定位失败阶段”，因此在 `/devices` 中保持只读" -- docs/02-业务功能与流程说明.md
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceWorkbenchView.test.ts
```

Expected:

```text
git grep: no matches
PASS  src/__tests__/views/DeviceWorkbenchView.test.ts
```

- [ ] **Step 3: Run the final build-level verification**

Run:

```powershell
npm --prefix spring-boot-iot-ui run build
git diff --check
```

Expected:

```text
- `npm --prefix spring-boot-iot-ui run build` exits 0 and prints the standard Vite production build summary
- `git diff --check` prints nothing
```

- [ ] **Step 4: Commit the doc sync and verification pass**

Run:

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md
git commit -m "docs: sync unregistered device registration flow"
```
