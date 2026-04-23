# Runtime Display Rule UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a low-friction governance path from `/insight` into runtime display rules, surface draft candidates and static preview inside `mapping-rules`, and show formal-field coverage reminders plus quick disable on existing rules.

**Architecture:** Keep all backend truth unchanged and implement this round entirely in the existing frontend route structure. `/insight` only deep-links candidate context into `/products/:productId/mapping-rules`; `ProductModelDesignerWorkspace` passes current formal property identifiers downward; `ProductRuntimeMetricDisplayRulePanel` derives candidate/previews/coverage locally from route query, loaded rules, and loaded formal fields.

**Tech Stack:** Vue 3 `<script setup>`, Vue Router, Vitest, Vue Test Utils, existing runtime display rule API client, shared `StandardButton`.

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
  - Replace the non-formal-field hint with a deep-link governance action and carry draft context in route query.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  - Pass formal property identifiers into the runtime display rule panel.
- Modify: `spring-boot-iot-ui/src/components/product/ProductRuntimeMetricDisplayRulePanel.vue`
  - Read candidate draft query, render candidate card, render static preview, show formal-coverage badges, and offer quick disable.
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

### Task 1: Add The Failing `/insight` Deep-Link Test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: Write the failing test for non-formal-field governance action**

```ts
it('routes non-formal snapshot rows to runtime display rule governance with candidate query', async () => {
  vi.mocked(getDeviceProperties).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        identifier: 'S1_ZT_1.humidity',
        propertyName: '相对湿度',
        propertyValue: '66',
        valueType: 'double',
        unit: '%RH',
        updateTime: '2026-04-21 09:30:00'
      }
    ]
  })
  vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: []
  })

  const wrapper = mountView()
  await flushPromises()
  await flushPromises()

  await wrapper.get('[data-testid="property-snapshot-govern-S1_ZT_1-humidity"]').trigger('click')

  expect(mockRouter.push).toHaveBeenCalledWith({
    path: '/products/501/mapping-rules',
    query: {
      rawIdentifier: 'S1_ZT_1.humidity',
      displayName: '相对湿度',
      unit: '%RH',
      deviceCode: 'SK00EB0D1308313',
      runtimeGovernanceDraft: '1',
      source: 'insight'
    }
  })
})
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
npm exec vitest run src/__tests__/views/DeviceInsightView.test.ts
```

Expected: FAIL because non-formal rows still render only the text hint and do not navigate.

- [ ] **Step 3: Implement the minimal deep-link behavior**

```ts
interface PropertySnapshotRow extends Partial<DeviceProperty> {
  identifier: string
  propertyValue: string
  valueType: string
  displayName: string
  displayUnit: string
  displayTime: string
  formalIdentifier: string
  canEditFormalField: boolean
  canGovernRuntimeDisplay: boolean
}

function handleGovernRuntimeDisplay(row: PropertySnapshotRow) {
  const productId = device.value?.productId
  if (productId == null || !normalizeText(row.identifier)) {
    return
  }
  void router.push({
    path: buildProductWorkbenchSectionPath(productId, 'mapping-rules'),
    query: {
      rawIdentifier: row.identifier,
      displayName: normalizeText(row.displayName) ?? '',
      unit: normalizeText(row.displayUnit) ?? '',
      deviceCode: normalizedDeviceCode.value,
      runtimeGovernanceDraft: '1',
      source: 'insight'
    }
  })
}
```

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Run:

```bash
npm exec vitest run src/__tests__/views/DeviceInsightView.test.ts
```

Expected: PASS with the new action rendered only for non-formal rows.

### Task 2: Add The Failing Panel Candidate/Preview/Coverage Tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductRuntimeMetricDisplayRulePanel.vue`

- [ ] **Step 1: Write failing tests for candidate adoption, preview, coverage, and quick disable**

```ts
it('adopts route-query candidate into the form and shows static preview', async () => {
  mockRoute.query = {
    rawIdentifier: 'S1_ZT_1.humidity',
    displayName: '相对湿度',
    unit: '%RH',
    deviceCode: 'DEVICE-001',
    runtimeGovernanceDraft: '1',
    source: 'insight'
  }
  const wrapper = mountPanel({
    formalPropertyIdentifiers: ['value']
  })

  await flushPromises()
  await flushPromises()

  expect(wrapper.text()).toContain('待治理候选')
  expect(wrapper.text()).toContain('DEVICE-001')

  await wrapper.get('[data-testid="runtime-display-rule-candidate-adopt"]').trigger('click')

  expect((wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').element as HTMLInputElement).value).toBe('S1_ZT_1.humidity')
  expect(wrapper.get('[data-testid="runtime-display-rule-preview"]').text()).toContain('设备属性快照')
  expect(wrapper.get('[data-testid="runtime-display-rule-preview"]').text()).toContain('对象洞察')
})

it('marks rules covered by formal property identifiers and quick-disables them', async () => {
  mockListRuntimeMetricDisplayRules.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 100,
      records: [
        {
          id: 9001,
          productId: 1001,
          scopeType: 'PRODUCT',
          rawIdentifier: 'value',
          displayName: '裂缝值',
          unit: 'mm',
          status: 'ACTIVE',
          versionNo: 3
        }
      ]
    }
  })

  const wrapper = mountPanel({
    formalPropertyIdentifiers: ['value']
  })

  await flushPromises()
  await flushPromises()

  expect(wrapper.text()).toContain('已被正式字段覆盖')

  await wrapper.get('[data-testid="runtime-display-rule-disable-9001"]').trigger('click')

  expect(mockUpdateRuntimeMetricDisplayRule).toHaveBeenCalledWith(1001, 9001, {
    scopeType: 'PRODUCT',
    rawIdentifier: 'value',
    displayName: '裂缝值',
    unit: 'mm',
    status: 'DISABLED',
    scenarioCode: null,
    deviceFamily: null,
    protocolCode: null
  })
})
```

- [ ] **Step 2: Run the focused panel test and verify RED**

Run:

```bash
npm exec vitest run src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts
```

Expected: FAIL because the panel currently has no route-query candidate logic, no preview, no coverage badge, and no quick-disable action.

- [ ] **Step 3: Implement the minimal panel behavior**

```ts
const props = defineProps<{
  productId?: IdType | null
  formalPropertyIdentifiers?: string[]
}>()

const route = useRoute()
const dismissedCandidateKey = ref('')
const formalIdentifierSet = computed(() => new Set((props.formalPropertyIdentifiers ?? []).map((item) => item.trim()).filter(Boolean)))
const draftCandidate = computed(() => {
  if (route.query.runtimeGovernanceDraft !== '1') {
    return null
  }
  const rawIdentifier = readQueryText(route.query.rawIdentifier)
  if (!rawIdentifier) {
    return null
  }
  return {
    rawIdentifier,
    displayName: readQueryText(route.query.displayName),
    unit: readQueryText(route.query.unit),
    deviceCode: readQueryText(route.query.deviceCode),
    source: readQueryText(route.query.source) || 'insight'
  }
})

const formPreview = computed(() => ({
  scopeLabel: scopeTypeLabel(form.scopeType),
  scopeSignature: buildPreviewScopeSignature(form),
  affectsLabels: ['设备属性快照', '历史趋势', '对象洞察'],
  coveredByFormalField: formalIdentifierSet.value.has(form.rawIdentifier.trim()),
  conflictsWithExisting: rows.value.some((row) => isSameSignature(row, form))
}))

async function quickDisable(row: RuntimeMetricDisplayRule) {
  if (!hasProductId(props.productId) || row.id == null) {
    return
  }
  await updateRuntimeMetricDisplayRule(props.productId, row.id, {
    scopeType: normalizeScopeType(row.scopeType),
    rawIdentifier: row.rawIdentifier?.trim() || '',
    displayName: row.displayName?.trim() || '',
    unit: normalizeNullableText(row.unit),
    status: 'DISABLED',
    scenarioCode: normalizeNullableText(row.scenarioCode),
    deviceFamily: normalizeNullableText(row.deviceFamily),
    protocolCode: normalizeNullableText(row.protocolCode)
  })
}
```

- [ ] **Step 4: Re-run the focused panel test and verify GREEN**

Run:

```bash
npm exec vitest run src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts
```

Expected: PASS with candidate rendering, preview, covered badge, and disable behavior locked.

### Task 3: Add The Failing Workspace Wiring Test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: Write the failing prop-wiring test**

```ts
it('passes formal property identifiers into runtime display rule governance', async () => {
  const wrapper = mountWorkspace(undefined, 'mapping-rules')
  await flushPromises()
  await flushPromises()

  expect(wrapper.get('[data-testid="runtime-display-rule-panel-props"]').text()).toBe('1001|value')
})
```

- [ ] **Step 2: Run the focused workspace test and verify RED**

Run:

```bash
npm exec vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: FAIL because only `productId` is currently passed to the runtime display rule panel stub.

- [ ] **Step 3: Implement the minimal prop wiring**

```ts
const formalPropertyIdentifiers = computed(() =>
  models.value
    .filter((item) => item.modelType === 'property')
    .map((item) => item.identifier)
    .filter((item) => Boolean(item?.trim()))
)
```

```vue
<ProductRuntimeMetricDisplayRulePanel
  :product-id="props.product?.id ?? null"
  :formal-property-identifiers="formalPropertyIdentifiers"
/>
```

- [ ] **Step 4: Re-run the focused workspace test and verify GREEN**

Run:

```bash
npm exec vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS with the panel receiving current formal property identifiers.

### Task 4: Update Docs After Behavior Changes

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: Update the product/mapping-rules governance wording**

```md
- `/insight` 的非正式字段可直接一键跳到 `/products/:productId/mapping-rules` 的运行态名称/单位治理表单。
- `mapping-rules -> 运行态名称/单位治理` 当前会显示来自对象洞察的待治理候选、本次生效静态预览，以及“已被正式字段覆盖”的停用提醒。
```

- [ ] **Step 2: Update change log and frontend governance rules**

```md
- 2026-04-21：运行态名称/单位治理补齐“对象洞察直达治理 + 待治理候选 + 静态预览 + 正式覆盖提醒/快捷停用”体验增强。
```

- [ ] **Step 3: Verify the doc edits stay aligned with current behavior**

Run:

```bash
git diff -- README.md AGENTS.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md docs/21-业务功能清单与验收标准.md docs/appendix/iot-field-governance-and-sop.md
```

Expected: only the newly implemented UX behavior is documented; no backend truth model changes are claimed.

### Task 5: Final Verification

**Files:**
- Modify: none

- [ ] **Step 1: Run the focused UI tests**

Run:

```bash
npm exec vitest run src/__tests__/views/DeviceInsightView.test.ts src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS with all three targeted suites green.

- [ ] **Step 2: Run the frontend build**

Run:

```bash
npm run build
```

Expected: build succeeds without type errors.

- [ ] **Step 3: Run whitespace/self-check**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only the intended source and doc files are modified.
