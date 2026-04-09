# Product Contract Field Single-Page Manual Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `/products` contract-field governance into a single-page manual sample workspace, remove the drawer/runtime-candidate path, and align the product workbench header/cards with the approved compact contract-field design.

**Architecture:** Keep `/products`, product-model CRUD, `compare/apply`, and device-relation APIs, but collapse the governance interaction into `ProductModelDesignerWorkspace`. The frontend becomes “sample input -> compare result -> selected apply -> formal fields” on one page, while the backend compare contract is simplified to manual sample extraction only (`sampleType + deviceStructure + samplePayload + parentDeviceCode + relationMappings`). Runtime/model-candidate APIs and their code paths are deleted instead of hidden.

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Vitest, Spring Boot 4 + Java 17 + MyBatis, Maven, existing `/products` workbench shared components.

---

## File Structure

- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
  Responsibility: make `产品Key` visually stronger, align the top-right `编辑档案` action to the workbench baseline, and remove the strong dynamic prompt copy.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
  Responsibility: align overview/business card height with the related-device card and keep the compact workbench structure consistent.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: become the only contract-field workspace, host manual sample input, compare result, selected apply area, and formal-field list on the same page.
- Delete: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  Responsibility: remove the obsolete second-layer drawer entry.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  Responsibility: keep compare/apply result rendering, but update it to the manual-only contract and simplified source/status copy.
- Delete or trim: `spring-boot-iot-ui/src/components/product/productModelGovernanceRelationPresets.ts`
  Responsibility: remove preset/bootstrap-only code if the final page uses parent-device relation loading + manual row editing instead.
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  Responsibility: keep the workbench on the same route and route “开始补齐契约 / 继续核对字段” into the contract-field page anchor instead of opening a drawer.
- Modify: `spring-boot-iot-ui/src/api/product.ts`
  Responsibility: remove `model-candidates` API calls and keep only product-model CRUD + compare/apply.
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
  Responsibility: replace old `manualExtract + includeRuntimeCandidates + extractMode/sourceDeviceCode` contracts with the new manual-only compare payload.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Delete: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
  Responsibility: lock the single-page interaction and prevent drawer/runtime UI regressions.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
  Responsibility: delete `model-candidates` endpoints and keep only product-model CRUD + compare/apply.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
  Responsibility: remove candidate-oriented service methods and keep only the active governance/product-model API surface.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
  Responsibility: delete runtime candidate building and refactor compare extraction to manual sample only with `single/composite` semantics.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
  Responsibility: move compare input to `sampleType + deviceStructure + samplePayload + parentDeviceCode + relationMappings`.
- Delete: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java`
- Delete: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelCandidateConfirmDTO.java`
- Delete if unused: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelCandidate*.java`
  Responsibility: clear DTO/VO branches left only for removed candidate APIs.
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
  Responsibility: replace runtime/model-candidate cases with manual compare cases for single/composite business/status extraction.
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: sync the new same-page manual contract-field governance behavior and cleanup rules.

### Task 1: Lock The New Single-Page Contract-Field UX With Tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Delete: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: Add failing workspace expectations for the same-page flow**

Add assertions that the workspace itself contains the full governance flow instead of a secondary drawer:

```ts
expect(wrapper.text()).toContain('样本输入')
expect(wrapper.text()).toContain('识别结果')
expect(wrapper.text()).toContain('本次生效')
expect(wrapper.text()).toContain('当前已生效字段')
expect(wrapper.text()).toContain('样本类型')
expect(wrapper.text()).toContain('设备结构')
expect(wrapper.find('[data-testid="contract-field-sample-input"]').exists()).toBe(true)
```

- [ ] **Step 2: Add failing expectations for the new manual-only controls**

Assert that the old runtime/drawer wording is gone and the new manual controls exist:

```ts
expect(wrapper.text()).toContain('业务数据')
expect(wrapper.text()).toContain('状态数据')
expect(wrapper.text()).toContain('单台设备')
expect(wrapper.text()).toContain('复合设备')
expect(wrapper.text()).toContain('提取契约字段')
expect(wrapper.text()).not.toContain('自动提炼')
expect(wrapper.text()).not.toContain('父设备样本归一到子产品')
```

- [ ] **Step 3: Add failing expectations for workbench entry behavior**

Update workbench tests so “开始补齐契约 / 继续核对字段” stay in the same page:

```ts
await wrapper.get('[data-testid="start-contract-field"]').trigger('click')
expect(wrapper.findComponent({ name: 'ProductModelDesignerDrawer' }).exists()).toBe(false)
expect(wrapper.emitted('open-contract-field-anchor')).toBeTruthy()
```

- [ ] **Step 4: Add failing expectations for the compact header/card cleanup**

Lock the approved workbench copy and stronger product-key presentation:

```ts
expect(wrapper.text()).not.toContain('当前已有运行设备，可继续补齐并核对契约字段')
expect(wrapper.find('[data-testid="product-key-hero"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-overview-card"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="related-device-card"]').exists()).toBe(true)
```

- [ ] **Step 5: Run the focused frontend test batch and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  --run
```

Expected: FAIL because the current UI still depends on the drawer/runtime-era structure.

### Task 2: Implement The Single-Page Frontend Workspace

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Delete: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Delete or trim: `spring-boot-iot-ui/src/components/product/productModelGovernanceRelationPresets.ts`

- [ ] **Step 1: Move the whole governance flow into `ProductModelDesignerWorkspace.vue`**

Build the manual workspace directly in the page:

```vue
<section class="product-model-designer__sample-stage">
  <h3>样本输入</h3>
  <div class="product-model-designer__sample-toolbar">
    <StandardChoiceGroup v-model="sampleType" :options="sampleTypeOptions" />
    <StandardChoiceGroup v-model="deviceStructure" :options="deviceStructureOptions" />
    <StandardButton action="confirm" @click="handleCompare">提取契约字段</StandardButton>
  </div>
</section>
```

- [ ] **Step 2: Add the composite-device relation editor inside the same page**

When `deviceStructure === 'composite'`, show only parent code and relation rows:

```vue
<div v-if="deviceStructure === 'composite'" class="product-model-designer__relation-stage">
  <ElInput v-model="parentDeviceCode" placeholder="请输入父设备编码" />
  <div v-for="row in relationMappings" :key="row.key" class="product-model-designer__relation-row">
    <ElInput v-model="row.logicalChannelCode" placeholder="逻辑通道编码" />
    <ElInput v-model="row.childDeviceCode" placeholder="子设备编码" />
  </div>
</div>
```

- [ ] **Step 3: Keep JSON input manual-first and auto-format pasted text**

Use one textarea and a safe formatter:

```ts
function formatJsonText(raw: string) {
  const parsed = JSON.parse(raw)
  return JSON.stringify(parsed, null, 2)
}
```

and only rewrite the text on valid JSON, otherwise keep the original input and show inline error.

- [ ] **Step 4: Keep compare/apply/formal sections on the same page**

Render the approved four-stage order:

```vue
<section>样本输入</section>
<section>识别结果</section>
<section>本次生效</section>
<section>当前已生效字段</section>
```

Do not mount or import `ProductModelDesignerDrawer` anywhere.

- [ ] **Step 5: Update workbench entry and visual alignment**

Wire the entry CTA to the same-page anchor and align the header/cards:

```ts
function handleContractFieldEntry() {
  activeTab.value = 'contract-field'
  nextTick(() => sampleStageRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
```

Also apply the approved copy/visual cleanup:

```vue
<strong data-testid="product-key-hero" class="product-workbench__key-hero">{{ product.productKey }}</strong>
```

and remove the strong dynamic prompt sentence entirely.

- [ ] **Step 6: Run the focused frontend tests and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  --run
```

Expected: PASS.

### Task 3: Clean Frontend APIs, Types, And Obsolete Candidate Paths

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Modify any test files failing on removed exports

- [ ] **Step 1: Replace the compare payload contract with the approved manual-only structure**

Define the new payload shape:

```ts
export interface ProductModelGovernanceRelationMappingPayload {
  logicalChannelCode: string
  childDeviceCode: string
}

export interface ProductModelGovernanceManualComparePayload {
  sampleType: 'business' | 'status'
  deviceStructure: 'single' | 'composite'
  samplePayload: string
  parentDeviceCode?: string | null
  relationMappings?: ProductModelGovernanceRelationMappingPayload[]
}
```

- [ ] **Step 2: Remove unused candidate/runtime exports**

Delete frontend API/type exports tied only to removed endpoints:

```ts
listProductModelCandidates
manualExtractProductModelCandidates
confirmProductModelCandidates
includeRuntimeCandidates
ProductModelManualExtractMode
```

- [ ] **Step 3: Keep only CRUD + compare/apply in `productApi`**

The product API should retain:

```ts
listProductModels
addProductModel
updateProductModel
deleteProductModel
compareProductModelGovernance
applyProductModelGovernance
```

and remove every `/model-candidates` request.

- [ ] **Step 4: Run the frontend type-safe verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  --run
```

Expected: PASS with no missing-type or removed-export failures.

### Task 4: Write Failing Backend Tests For Manual-Only Compare

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Add a failing single-device business-sample compare test**

Cover the new single-device input semantics:

```java
dto.getManualExtract().setSampleType("business");
dto.getManualExtract().setDeviceStructure("single");
dto.getManualExtract().setSamplePayload("""
{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}
""");
```

and assert:

```java
assertEquals(List.of("temperature"), result.getCompareRows().stream()
        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
        .toList());
```

- [ ] **Step 2: Add a failing composite business-sample compare test**

Use the approved crack-parent sample:

```java
dto.getManualExtract().setSampleType("business");
dto.getManualExtract().setDeviceStructure("composite");
dto.getManualExtract().setParentDeviceCode("SK00EA0D1307986");
dto.getManualExtract().setRelationMappings(List.of(mapping("L1_LF_1", "202018143")));
```

and assert:

```java
assertEquals(List.of("value"), result.getCompareRows().stream()
        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
        .toList());
```

- [ ] **Step 3: Add a failing composite status-sample compare test**

Assert status samples map only mirrored sensor state:

```java
assertEquals(List.of("sensor_state"), result.getCompareRows().stream()
        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
        .toList());
```

and explicitly assert parent terminal fields do not leak into child candidates:

```java
assertFalse(result.getCompareRows().stream().anyMatch(row -> "temp".equals(row.getIdentifier())));
```

- [ ] **Step 4: Add a failing validation test for composite relation requirements**

Assert missing composite relation context fails fast:

```java
assertThatThrownBy(() -> service.compareGovernance(productId, dto))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("映射关系");
```

- [ ] **Step 5: Run the focused backend test and verify red**

Run:

```bash
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
```

Expected: FAIL because the backend still accepts the old extract/runtime contract.

### Task 5: Implement Backend Compare Refactor And Delete Candidate Code

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
- Delete: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java`
- Delete: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelCandidateConfirmDTO.java`
- Delete if unused: candidate-only VO/DTO classes and imports

- [ ] **Step 1: Simplify the compare DTO to manual sample extraction only**

Refactor `ManualExtractInput`:

```java
@Data
public static class ManualExtractInput {
    private String sampleType;
    private String deviceStructure;
    private String samplePayload;
    private String parentDeviceCode;
    private List<RelationMappingInput> relationMappings;
}

@Data
public static class RelationMappingInput {
    private String logicalChannelCode;
    private String childDeviceCode;
}
```

and remove:

```java
includeRuntimeCandidates
sourceDeviceCode
extractMode
```

- [ ] **Step 2: Delete the `/model-candidates` controller/service surface**

Remove these endpoints and service methods completely:

```java
GET  /api/device/product/{productId}/model-candidates
POST /api/device/product/{productId}/model-candidates/manual-extract
POST /api/device/product/{productId}/model-candidates/confirm
```

The controller should keep only:

```java
list / add / update / delete / compareGovernance / applyGovernance
```

- [ ] **Step 3: Refactor manual extraction rules inside `ProductModelServiceImpl`**

Normalize the new two-axis semantics:

```java
String sampleType = normalizeSampleType(input.getSampleType());
String deviceStructure = normalizeDeviceStructure(input.getDeviceStructure());
```

Then split extraction logic:

```java
if ("composite".equals(deviceStructure)) {
    return extractCompositeSnapshot(sampleType, input);
}
return extractSingleSnapshot(sampleType, input);
```

- [ ] **Step 4: Keep composite status extraction child-only**

Implement the approved rule:

```java
if ("status".equals(sampleType) && isSensorStateMirror(identifier, logicalChannelCode, rule)) {
    return "sensor_state";
}
```

and explicitly ignore parent-only status fields:

```java
if ("status".equals(sampleType) && isParentTerminalStatus(identifier)) {
    return null;
}
```

- [ ] **Step 5: Delete runtime candidate building and dead compare branches**

Remove code paths tied only to runtime/model-candidate behavior:

```java
buildRuntimeGovernanceCandidates(...)
listModelCandidates(...)
manualExtractModelCandidates(...)
confirmModelCandidates(...)
```

and any `iot_device_property / iot_device_message_log` compare candidate aggregation used only for runtime governance.

- [ ] **Step 6: Run the focused backend test and verify green**

Run:

```bash
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
```

Expected: PASS, aside from known environment-specific Mockito agent issues if they reappear outside this class.

### Task 6: Sync Docs And Run Final Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update README and `docs/02` to the new product-workbench semantics**

Document the approved wording:

```md
`契约字段` 当前在同一页面完成样本录入、字段提取、结果确认和正式字段查看，不再打开二层抽屉。
```

- [ ] **Step 2: Update `docs/03` to the new backend contract**

Replace old compare/manual wording with:

```md
`manualExtract` 当前接收 `sampleType`、`deviceStructure`、`samplePayload`、`parentDeviceCode`、`relationMappings[]`。
```

and remove `/model-candidates*` API descriptions entirely.

- [ ] **Step 3: Update `docs/08` and `docs/15` with the cleanup rules**

Record the behavior and cleanup explicitly:

```md
本轮已删除 `/model-candidates` 与 runtime candidate 无入口代码；契约字段治理当前只保留单页手动样本 compare/apply。
```

- [ ] **Step 4: Run the final frontend + backend verification batch**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  --run
```

Run:

```bash
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
```

Run:

```bash
git diff --stat
```

Expected: targeted tests PASS, and the diff contains only the planned frontend/backend/doc cleanup.

- [ ] **Step 5: If the targeted suites pass, run the project-level quality gate most relevant to this slice**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly reported pre-existing/environment blocker.

## Self-Review

- Spec coverage: this plan covers the single-page contract-field UX, manual-only sample controls, composite parent-device relation mapping, frontend removal of the drawer/runtime path, backend removal of `/model-candidates` and runtime candidates, workbench visual cleanup, and the required docs updates.
- Placeholder scan: no `TODO/TBD/similar to above` markers remain; each task names exact files, concrete expectations, and exact commands.
- Type consistency: the plan uses one compare contract vocabulary everywhere: `sampleType`, `deviceStructure`, `samplePayload`, `parentDeviceCode`, `relationMappings`.
