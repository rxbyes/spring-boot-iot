# Object Insight Chart Layout And Units Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let product operators define formal-field units, make `/insight` reuse the full formal display name with unit, and tighten trend chart spacing so X/Y axes no longer crowd or exaggerate the lines.

**Architecture:** Keep the formal product model as the single editable source for `modelName` and `specsJson.unit`, then have `DeviceInsightView` and `RiskInsightTrendPanel` consume those values consistently. Limit chart changes to shared ECharts config so all three groups (`监测数据 / 状态事件 / 运行参数`) inherit the same spacing and axis rules without device-specific branches.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, ECharts

---

### Task 1: Add failing tests for unit editing and chart layout/name regressions

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`

- [ ] **Step 1: Write the failing unit-edit regression test**

Add a test proving the formal-field editor can submit both renamed `modelName` and `specsJson.unit` for a property model.

- [ ] **Step 2: Run the targeted product-workspace test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts -t "saves formal property unit together with renamed display name"`

Expected: FAIL because the current UI has no unit input and does not submit updated `specsJson.unit`.

- [ ] **Step 3: Write the failing insight-name regression test**

Add a test proving trend series labels prefer the full formal display name with unit suffix, such as `X轴加速度（m/s²）`, and do not fall back to truncated or stale telemetry labels.

- [ ] **Step 4: Run the targeted insight-view test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts -t "uses full formal display names with units for trend series labels"`

Expected: FAIL because the current trend display name chain does not append unit text.

- [ ] **Step 5: Write the failing chart-layout regression tests**

Add tests proving:
- one-day charts leave more bottom space between X-axis labels and the last line
- status-event charts with only `正常/异常` stay visually compact
- numeric trend charts reduce exaggerated Y-axis expansion

- [ ] **Step 6: Run the targeted trend-panel tests to verify they fail**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RiskInsightTrendPanel.test.ts -t "keeps full legend labels and uses gentler axis spacing|compresses status-event y-axis when only normal and abnormal states exist|reduces y-axis exaggeration for numeric trend charts"`

Expected: FAIL because the current legend/axis config does not yet satisfy those assertions.

### Task 2: Implement formal-field unit editing and full-name propagation

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: Add minimal property-unit editing support in the formal-field card**

Expose a unit input alongside the existing rename flow for `property` models only, initialize it from `specsJson.unit`, and submit it through `updateProductModel(..., { specsJson })`.

- [ ] **Step 2: Keep object-insight display names in sync after formal edits**

When a property name changes, continue updating `metadataJson.objectInsight.customMetrics[].displayName`; when unit changes, do not fork a second source of truth.

- [ ] **Step 3: Build the trend display label from formal name plus unit**

In `DeviceInsightView.vue`, derive trend display names from the formal product model first and append `（单位）` when a unit exists, so legend, tooltip, and snapshot naming share one source.

- [ ] **Step 4: Run the focused tests for these edits**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/DeviceInsightView.test.ts`

Expected: PASS.

### Task 3: Tighten shared chart spacing and axis behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue`

- [ ] **Step 1: Update shared legend and grid config**

Prevent legend text clipping and increase bottom chart padding so X-axis labels no longer crowd the plotted line.

- [ ] **Step 2: Reduce numeric Y-axis exaggeration**

Replace the current overly large numeric-axis span heuristic with a gentler padding rule and stable split count.

- [ ] **Step 3: Compress binary status-event vertical range**

When status-event data only spans compact states such as `正常/异常`, keep the Y-axis tight enough that the bottom time labels and state line no longer overlap visually.

- [ ] **Step 4: Run the focused trend-panel tests**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RiskInsightTrendPanel.test.ts`

Expected: PASS.

### Task 4: Update documentation and verify the final slice

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Document where units are maintained**

Record that units are now edited from `/products -> 契约字段 -> 当前已生效字段` and consumed by `/insight`.

- [ ] **Step 2: Document the chart-spacing and full-name rules**

Record the new legend, X-axis spacing, and Y-axis tightening rules for `监测数据 / 状态事件 / 运行参数`.

- [ ] **Step 3: Run the final verification set**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts`

Expected: PASS with all targeted tests green.

- [ ] **Step 4: Run the production build**

Run: `npm --prefix spring-boot-iot-ui run build`

Expected: `✓ built`

- [ ] **Step 5: Run the diff hygiene check**

Run: `git diff --check -- docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`

Expected: no whitespace or merge-marker errors.
