# Object Insight Important Metrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let product editing and contract-field workspaces choose which formal property metrics must appear in the object insight trend preview, with one shared product-level source of truth.

**Architecture:** Keep `metadataJson.objectInsight.customMetrics[]` as the only persisted configuration. Add UI helpers that write the same structure from both product editing and contract-field views, then update object insight runtime to honor `enabled`, `sortNo`, and “trend-only priority” semantics.

**Tech Stack:** Vue 3, Element Plus, Vitest, existing `productApi.updateProduct` product metadata flow.

---

### Task 1: Add reusable object-insight metric helpers

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts`

- [ ] Write failing tests for creating/upserting/removing trend-focus metric rows from formal product models.
- [ ] Run `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/productObjectInsightConfig.test.ts` and confirm failure.
- [ ] Implement helper functions for quick-add, group lookup, upsert, and removal.
- [ ] Re-run the same test command and confirm pass.

### Task 2: Respect formal trend configuration at runtime

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`

- [ ] Write failing tests proving enabled focus metrics move to the front of trend groups and disabled metrics are excluded.
- [ ] Run `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/deviceInsightCapability.test.ts` and confirm failure.
- [ ] Implement product-level config ordering/exclusion logic using `enabled`, `sortNo`, and `includeInExtension`.
- [ ] Re-run the same test command and confirm pass.

### Task 3: Expose quick-add actions in product editing

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts`

- [ ] Write failing editor tests for adding a formal property model as a measure/status trend focus metric.
- [ ] Run `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts` and confirm failure.
- [ ] Implement candidate formal-model quick-add UI and load available property models in product editing flows.
- [ ] Re-run the same test command and confirm pass.

### Task 4: Add contract-field quick actions

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] Write failing tests for setting/removing trend-focus metrics from `当前已生效字段`.
- [ ] Run `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts` and confirm failure.
- [ ] Implement immediate `updateProduct` write-back and parent product refresh event handling.
- [ ] Re-run the same test command and confirm pass.

### Task 5: Sync docs and run focused verification

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] Update docs to describe the single source of truth and the three entry points.
- [ ] Run `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/productObjectInsightConfig.test.ts src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/DeviceInsightView.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts`
- [ ] Run `git diff --check` and confirm there are no new conflict markers or syntax issues.
