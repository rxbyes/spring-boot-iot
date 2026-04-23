# Product Contract Sample Identifier Clarification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clarify, in the product contract governance workbench and authority docs, when sample extraction keeps full-path identifiers and when it normalizes into direct canonical fields.

**Architecture:** Keep the existing compare/apply pipeline unchanged. Resolve the ambiguity by tightening UI copy and authority docs around the current backend truth: composite samples normalize by relation mapping, while single-device samples preserve or normalize identifiers according to the product’s existing formal contract shape.

**Tech Stack:** Vue 3, TypeScript, Spring Boot Java services, Markdown documentation

---

### Task 1: Lock the implementation scope

**Files:**
- Create: `docs/superpowers/plans/2026-04-19-product-contract-sample-identifier-clarification.md`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: Re-check the current compare/apply truth**

Review:

```text
spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java
spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java
spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue
docs/02-业务功能与流程说明.md
docs/03-接口规范与接口清单.md
docs/appendix/iot-field-governance-and-sop.md
```

Expected: confirm that no new backend switch is required for this clarification round.

- [ ] **Step 2: Verify the working tree before edits**

Run:

```powershell
git status --short
```

Expected: dirty tree is allowed, but only targeted files for this task will be edited.

### Task 2: Clarify sample-input semantics in the product workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Write or update the failing UI test**

Add assertions that the sample-input area explicitly explains:

```ts
expect(wrapper.text()).toContain('单台样本会优先沿用当前产品的正式字段口径')
expect(wrapper.text()).toContain('复合设备会按父设备关系映射归一到子产品字段')
expect(wrapper.text()).toContain('逻辑通道编码用于归属和原始证据，不直接等于正式字段标识')
```

- [ ] **Step 2: Run the focused test to verify the copy is missing**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: FAIL on the new text assertions.

- [ ] **Step 3: Update the workspace copy with the minimal compatible explanation**

Edit the sample-input section so it says:

```text
单台样本会优先沿用当前产品的正式字段口径：单能力/规范产品收口为直接字段，多能力产品保留监测类型编码 + 数据字段这类全路径标识。
复合设备会按父设备关系映射归一到子产品字段；逻辑通道编码用于归属和原始证据，不直接等于正式字段标识。
```

Also tighten the textarea placeholders so single/composite examples reinforce the rule instead of implying one universal identifier style.

- [ ] **Step 4: Run the focused UI test again**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS.

### Task 3: Update the authority docs

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update the business semantics doc**

Document three explicit cases:

```text
单台单能力/规范产品 -> 正式字段以 direct canonical 为主
单台多能力产品 -> 正式字段保留 full-path（监测类型编码 + 数据字段）
复合设备 -> 正式字段以 direct canonical 为主，逻辑通道编码只做归属/证据
```

- [ ] **Step 2: Update the API doc**

Clarify the compare request without changing the wire contract:

```text
sampleType + deviceStructure 只决定样本解析路径，不单独决定正式字段命名；
single 模式下正式字段口径取决于产品已有正式契约与规范场景；
composite 模式下由 relationMappings 的 canonicalizationStrategy / statusMirrorStrategy 决定 canonical 收口。
```

- [ ] **Step 3: Update the SOP appendix**

Add a practical rule table for:

```text
单台多能力设备
单台单能力设备
复合采集器父设备
复合子设备
```

and tie each case to the example identifiers used in the platform.

- [ ] **Step 4: Check whether README.md and AGENTS.md need follow-up**

Run:

```powershell
Select-String -Path README.md,AGENTS.md -Pattern '单台设备|复合设备|契约字段|value / sensor_state|dispsX / dispsY' -Encoding UTF8
```

Expected: confirm whether the new clarification must also be mirrored there.

### Task 4: Verify the change set

**Files:**
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Review: `docs/02-业务功能与流程说明.md`
- Review: `docs/03-接口规范与接口清单.md`
- Review: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: Run the targeted UI test**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS.

- [ ] **Step 2: Run a diff sanity check**

Run:

```powershell
git diff --check -- spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/appendix/iot-field-governance-and-sop.md
```

Expected: no whitespace or conflict-marker errors.

- [ ] **Step 3: Review the final diff**

Run:

```powershell
git diff -- spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/appendix/iot-field-governance-and-sop.md
```

Expected: only the intended clarification changes appear.
