# Product Model Relation-Aware Child Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the existing `/products` contract governance drawer guide users through parent-device sample normalization to child products and bootstrap missing `iot_device_relation` rows from built-in parent-to-child presets.

**Architecture:** Keep the current customer-facing contract workbench and drawer intact, then extend only the first-step evidence area with a relation-aware extraction mode, parent device context, preset hints, and one-click relation bootstrap. Reuse the existing backend compare contract (`manualExtract.sourceDeviceCode` + `manualExtract.extractMode=relation_child`) and existing `/api/device/relations` endpoints instead of adding new backend interfaces.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing `Standard*` shared drawer patterns, existing Spring Boot relation/governance APIs.

---

## File Structure

- Add: `docs/superpowers/plans/2026-04-05-product-model-relation-aware-child-governance-implementation-plan.md`
  Responsibility: record the approved execution plan for the relation-aware governance enhancement.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  Responsibility: expose relation-aware extraction mode, parent device inputs, preset hints, bootstrap action, and compare payload wiring.
- Modify: `spring-boot-iot-ui/src/api/device.ts`
  Responsibility: add device relation query/create helpers used by the drawer.
- Modify: `spring-boot-iot-ui/src/types/api.ts`
  Responsibility: define device relation payload/VO types and extend governance manual-extract payload types with relation context.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
  Responsibility: lock the relation-aware UI, bootstrap, and compare-request behavior with focused tests.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: record the new `/products` relation-aware governance flow and its shared-front-end constraints in place.

### Task 1: Write Red Tests For Relation-Aware Governance

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: Add a failing test for the relation-aware extraction controls**

Update `ProductModelDesignerDrawer.test.ts` with a case that mounts the drawer for the collector parent product and expects the first-step area to expose:

```ts
expect(wrapper.text()).toContain('父设备样本归一到子产品')
expect(wrapper.text()).toContain('父设备编码')
expect(wrapper.text()).toContain('关系映射预设')
expect(wrapper.text()).toContain('一键补齐设备关系')
expect(wrapper.text()).toContain('按父设备样本把 L1_LF_* / L1_SW_* 归一到子产品正式字段')
```

- [ ] **Step 2: Add a failing test for compare payload wiring**

Add a case that:

```ts
await parentCodeInput.setValue('SK00EA0D1307986')
await extractionModeButton.trigger('click')
await sampleInput.setValue('{"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T16:50:35.000Z":10.86}}}')
await compareButton.trigger('click')

expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
  governanceMode: 'generic',
  normativePresetCode: undefined,
  selectedNormativeIdentifiers: undefined,
  manualExtract: {
    sampleType: 'business',
    samplePayload: '{"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T16:50:35.000Z":10.86}}}',
    sourceDeviceCode: 'SK00EA0D1307986',
    extractMode: 'relation_child'
  },
  manualDraftItems: [],
  includeRuntimeCandidates: true
})
```

Expected red reason: the current drawer does not yet send `sourceDeviceCode` or `extractMode`.

- [ ] **Step 3: Add a failing test for one-click relation bootstrap**

Mock relation list/create calls and add a case that:

```ts
mockListDeviceRelations.mockResolvedValueOnce({ code: 200, msg: 'success', data: [] })
mockCreateDeviceRelation.mockResolvedValue({ code: 200, msg: 'success', data: { id: 1, parentDeviceCode: 'SK00EA0D1307986' } })

await parentCodeInput.setValue('SK00EA0D1307986')
await bootstrapButton.trigger('click')

expect(mockListDeviceRelations).toHaveBeenCalledWith('SK00EA0D1307986')
expect(mockCreateDeviceRelation).toHaveBeenCalledTimes(9)
expect(mockCreateDeviceRelation).toHaveBeenNthCalledWith(1, {
  parentDeviceCode: 'SK00EA0D1307986',
  logicalChannelCode: 'L1_LF_1',
  childDeviceCode: '202018143',
  relationType: 'collector_child',
  canonicalizationStrategy: 'LF_VALUE',
  statusMirrorStrategy: 'SENSOR_STATE',
  enabled: 1,
  remark: '产品契约工作台预置导入'
})
```

Expected red reason: the current drawer has no bootstrap button or relation API calls.

- [ ] **Step 4: Run the focused drawer test and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerDrawer.test.ts --run
```

Expected: FAIL because the relation-aware controls and API calls do not exist yet.

### Task 2: Implement The Minimal Relation-Aware Drawer Flow

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/api/device.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`

- [ ] **Step 1: Extend the relation API and type surface**

In `spring-boot-iot-ui/src/types/api.ts`, add:

```ts
export interface DeviceRelation {
  id?: IdType | null;
  parentDeviceCode: string;
  logicalChannelCode: string;
  childDeviceCode: string;
  childProductId?: IdType | null;
  childProductKey?: string | null;
  relationType: string;
  canonicalizationStrategy: string;
  statusMirrorStrategy?: string | null;
  enabled?: number | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DeviceRelationUpsertPayload {
  parentDeviceCode: string;
  logicalChannelCode: string;
  childDeviceCode: string;
  relationType: string;
  canonicalizationStrategy: string;
  statusMirrorStrategy?: string | null;
  enabled?: number | null;
  remark?: string | null;
}
```

Also extend `ProductModelGovernanceManualExtractPayload` to carry:

```ts
sourceDeviceCode?: string | null;
extractMode?: 'direct' | 'relation_child' | null;
```

Then add to `spring-boot-iot-ui/src/api/device.ts`:

```ts
export function listDeviceRelations(parentDeviceCode: string): Promise<ApiEnvelope<DeviceRelation[]>> {
  return request<DeviceRelation[]>(`/api/device/relations?parentDeviceCode=${encodeURIComponent(parentDeviceCode)}`, {
    method: 'GET'
  })
}

export function createDeviceRelation(payload: DeviceRelationUpsertPayload): Promise<ApiEnvelope<DeviceRelation>> {
  return request<DeviceRelation>('/api/device/relations', {
    method: 'POST',
    body: payload
  })
}
```

- [ ] **Step 2: Add preset data and extraction state to the drawer**

In `ProductModelDesignerDrawer.vue`, add a focused local preset map keyed by parent device code so the drawer can resolve:

```ts
SK00EA0D1307986 -> L1_LF_1..L1_LF_9 -> 202018143..202018139
SK00FB0D1310195 -> L1_SW_1..L1_SW_8 -> 84330701..84330696
```

Each preset entry must also carry the rule defaults used for bootstrap:

```ts
LF: relationType='collector_child', canonicalizationStrategy='LF_VALUE', statusMirrorStrategy='SENSOR_STATE'
SW: relationType='collector_child', canonicalizationStrategy='LEGACY', statusMirrorStrategy='NONE'
```

- [ ] **Step 3: Render relation-aware controls in the first-step panel**

Still in `ProductModelDesignerDrawer.vue`, add:

```ts
'直接核对'
'父设备样本归一到子产品'
'父设备编码'
'关系映射预设'
'一键补齐设备关系'
'按父设备样本把 L1_LF_* / L1_SW_* 归一到子产品正式字段'
```

Behavior rules:

- default to `直接核对`
- switching to relation-aware mode keeps the current sample textarea
- parent code input auto-selects a built-in preset when matched
- if the current product has no applicable normative preset, generic governance fallback still works

- [ ] **Step 4: Wire compare requests to relation-aware manual extract**

Update the `handleCompare` payload assembly to send:

```ts
manualExtract: {
  sampleType: manualSampleType.value,
  samplePayload: trimmedPayload,
  sourceDeviceCode: relationExtractMode.value === 'relation_child' ? normalizedParentDeviceCode : undefined,
  extractMode: relationExtractMode.value
}
```

The payload must remain `undefined` when no sample JSON is present.

- [ ] **Step 5: Implement one-click relation bootstrap with duplicate filtering**

Before creating rows, call `listDeviceRelations(parentDeviceCode)` and build a set of existing logical channels. Then only call `createDeviceRelation` for missing preset rows:

```ts
const existingChannels = new Set(relations.map((item) => `${item.logicalChannelCode}:${item.childDeviceCode}`))
const pendingPayloads = preset.items.filter((item) => !existingChannels.has(`${item.logicalChannelCode}:${item.childDeviceCode}`))
```

After successful creation:

- show a success message with created/skipped counts
- keep the user in the same drawer
- do not clear the sample payload

- [ ] **Step 6: Run the focused drawer test and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerDrawer.test.ts --run
```

Expected: PASS.

### Task 3: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the product-workbench flow docs in place**

Record that `/products` contract governance now supports:

- relation-aware child extraction inside the existing drawer
- parent-device sample normalization driven by `sourceDeviceCode + extractMode=relation_child`
- one-click bootstrap of built-in parent-to-child presets into `iot_device_relation`
- LF and SW preset defaults with their canonicalization strategies

- [ ] **Step 2: Re-run the focused drawer test**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerDrawer.test.ts --run
```

Expected: PASS.

- [ ] **Step 3: Run the repository quality gate**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly reported unrelated pre-existing failure.
