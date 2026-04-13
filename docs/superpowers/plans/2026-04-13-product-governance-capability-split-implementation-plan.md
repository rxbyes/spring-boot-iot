# Product Governance Capability Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/products` and `契约字段` capability-aware so only monitoring products use metric governance, while collecting/warning/video products expose device-only risk binding and unknown products surface capability confirmation.

**Architecture:** Introduce a shared front-end capability resolver backed by `metadataJson.governance.productCapabilityType` with keyword fallback, then drive `/products`, `ProductModelDesignerWorkspace`, and `编辑档案` from that resolver. Keep monitoring behavior intact, update risk-point binding capability utilities to include `COLLECTING`, and rewrite docs/tests around the new semantics.

**Tech Stack:** Vue 3, TypeScript, Vitest, Element Plus, existing product metadata helpers

---

### Task 1: Product capability resolver and metadata contract

**Files:**
- Create: `spring-boot-iot-ui/src/utils/productGovernanceCapability.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/productGovernanceCapability.test.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`

- [ ] **Step 1: Write the failing utility tests**

Add tests for:
- metadata explicit `MONITORING / COLLECTING / WARNING / VIDEO / UNKNOWN`
- product-name fallback for `监测型 / 采集型 / 预警型 / 视频`
- derived flags `supportsMetricGovernance / supportsDeviceOnlyRiskBinding`

- [ ] **Step 2: Run the utility tests to confirm RED**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/productGovernanceCapability.test.ts`

- [ ] **Step 3: Implement the shared resolver**

Implement:
- metadata parsing for `metadataJson.governance.productCapabilityType`
- normalized enum + labels
- derived applicability flags
- keyword fallback

- [ ] **Step 4: Run the utility tests to confirm GREEN**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/productGovernanceCapability.test.ts`

### Task 2: `/products` notice area becomes capability-aware

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Write/update failing view tests**

Cover:
- monitoring product still shows metric publish/binding/policy tasks
- collecting product hides metric-governance pending counts and shows device-only risk binding copy
- unknown product shows capability pending copy and no false “暂不适用”
- submit path persists selected capability into `metadataJson.governance`

- [ ] **Step 2: Run the focused view tests to confirm RED**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 3: Implement the minimal page changes**

Implement:
- capability parsing for focused product
- separate capability note area from pending-task list
- capability-aware summary/closed-out copy
- `编辑档案` capability selector state save/restore

- [ ] **Step 4: Run the focused view tests to confirm GREEN**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

### Task 3: `契约字段` governance cards become capability-aware

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Write/update failing workspace tests**

Cover:
- monitoring product still shows four-step governance semantics
- collecting product shows `设备级风险点绑定`
- collecting product no longer says `风险点绑定暂不适用` or `阈值策略暂不适用`
- unknown product shows capability pending guidance

- [ ] **Step 2: Run the focused workspace tests to confirm RED**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 3: Implement the workspace card split**

Implement:
- shared capability resolver usage
- capability-specific step cards / notes
- existing release-batch and version-ledger behavior preserved

- [ ] **Step 4: Run the focused workspace tests to confirm GREEN**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

### Task 4: Align risk-point capability utility with collecting products

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/riskPointDeviceBindingCapability.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/riskPointDeviceBindingCapability.test.ts`

- [ ] **Step 1: Write the failing collecting-capability tests**

Cover:
- `COLLECTING` normalizes correctly
- collecting devices do not support metric binding
- collecting devices use device-only hint/button semantics

- [ ] **Step 2: Run the utility tests to confirm RED**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/riskPointDeviceBindingCapability.test.ts`

- [ ] **Step 3: Implement the minimal utility update**

Update enum normalization, label, binding capability, and hint text.

- [ ] **Step 4: Run the utility tests to confirm GREEN**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/riskPointDeviceBindingCapability.test.ts`

### Task 5: Documentation updates and regression verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update docs in place**

Replace old “无可入目录字段即整体暂不适用” wording with capability-aware governance wording, and document the new `编辑档案 -> 产品能力` truth source.

- [ ] **Step 2: Run the focused regression suite**

Run:
- `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/productGovernanceCapability.test.ts`
- `pnpm vitest run spring-boot-iot-ui/src/__tests__/utils/riskPointDeviceBindingCapability.test.ts`
- `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `pnpm vitest run spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 3: Run a broader governance gate if the focused suite stays green**

Run: `node scripts/run-governance-contract-gates.mjs`

