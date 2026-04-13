# Object Insight Formal Name Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/insight` device property snapshots reuse the same formal field names as `/products -> 当前已生效字段`, even when runtime identifiers use raw alias forms.

**Architecture:** Add a small formal-field resolver inside `DeviceInsightView.vue` that looks up exact product model identifiers first and canonical suffix identifiers second. Use that resolver for snapshot display name, unit, and data type precedence so formal names override object-insight config names and runtime property names whenever a formal field exists.

**Tech Stack:** Vue 3, TypeScript, Vitest, Element Plus stubs, Markdown docs

---

### Task 1: Lock the inconsistency with a failing view test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: Add a regression test for canonical formal-field fallback**

```ts
it('uses the formal field name in the property snapshot when runtime identifiers are raw aliases', async () => {
  // Arrange a product with formal field identifier `signal_4g`
  // and a runtime property identifier `S1_ZT_1.signal_4g`.
  // Keep objectInsight displayName and runtime propertyName intentionally different.
});
```

- [ ] **Step 2: Run the targeted test and confirm it fails for the expected reason**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts -t "uses the formal field name in the property snapshot when runtime identifiers are raw aliases"
```

Expected: `FAIL`, because the snapshot still shows the runtime/configured name instead of the formal field name.

### Task 2: Implement formal-name resolution in the insight view

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: Add a shared identifier normalizer and product-model lookup helper**

```ts
function normalizeMetricIdentifier(identifier: string) {
  const normalized = identifier.trim();
  const lastDotIndex = normalized.lastIndexOf('.');
  if (lastDotIndex < 0 || lastDotIndex === normalized.length - 1) {
    return normalized;
  }
  return normalized.slice(lastDotIndex + 1);
}

function resolveProductModelValue(map: Map<string, string>, identifier: string) {
  return map.get(identifier) || map.get(normalizeMetricIdentifier(identifier)) || '';
}
```

- [ ] **Step 2: Replace direct `Map.get(identifier)` reads in snapshot/formal helpers**

```ts
const formalDisplayName = resolveProductModelValue(productModelDisplayNameMap.value, identifier);
const formalDataType = resolveProductModelValue(productModelDataTypeMap.value, identifier);
const formalUnit = resolveProductModelValue(productModelUnitMap.value, identifier);
```

- [ ] **Step 3: Change property snapshot name precedence to formal > runtime property > configured metric**

```ts
displayName: resolveMetricBaseName(
  identifier,
  resolveProductModelValue(productModelDisplayNameMap.value, identifier),
  property?.propertyName,
  configuredMetric?.displayName,
  series?.displayName
)
```

- [ ] **Step 4: Keep existing fallback behavior for fields without formal models**

Run the same targeted test after implementation.

### Task 3: Verify compatibility with existing insight behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: Review the existing tests that rely on custom metric display names**

Focus on cases where no formal field exists, such as runtime-only custom metrics.

- [ ] **Step 2: Run the full view test file to confirm the new precedence does not break compatible fallback behavior**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts
```

Expected: all tests in the file pass.

### Task 4: Update the documentation and final verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the business doc to state that device property snapshots now reuse formal field names first**

Add concise wording under the `/insight` governance semantics.

- [ ] **Step 2: Add a dated entry to the changelog**

Record the root cause, the formal-name precedence change, and the verification command.

- [ ] **Step 3: Run final verification**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts
git diff --check -- spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/superpowers/specs/2026-04-13-object-insight-formal-name-alignment-design.md docs/superpowers/plans/2026-04-13-object-insight-formal-name-alignment-implementation-plan.md
```

Expected: test command passes and `git diff --check` returns no whitespace errors.
