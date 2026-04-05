# Role Automation Acceptance And Developer Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate a formal role-based automation acceptance opinion and a developer-facing automation usage guide into the existing quality documentation.

**Architecture:** Keep all new content inside the existing quality documentation system instead of creating parallel files. Extend `docs/05-自动化测试与质量保障.md` with one formal acceptance section and one developer guide section, then record the outcome and scope boundary in `docs/08-变更记录与技术债清单.md`.

**Tech Stack:** Markdown, PowerShell, Maven, Node.js.

---

### Task 1: Lock The Documentation Scope

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

- [ ] **Step 1: Confirm the destination documents**

```md
- Formal acceptance opinion: docs/05-自动化测试与质量保障.md
- Developer automation usage guide: docs/05-自动化测试与质量保障.md
- Change summary and boundary note: docs/08-变更记录与技术债清单.md
```

- [ ] **Step 2: Confirm the writing boundary**

```md
- Do not create README-v2 or standalone acceptance memo files
- Do not duplicate docs/21 role matrix
- Write only the executable conclusion, scope boundary, and operating guide
```

### Task 2: Add The Formal Acceptance Opinion

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`

- [ ] **Step 1: Add a dated formal acceptance section**

```md
## 9. 角色化自动化测试正式验收意见（2026-04-05）
```

- [ ] **Step 2: Record the evidence and conclusion**

```md
- Evidence source: current code, role/menu seed data, docs/21 matrix, fresh targeted verification
- Core conclusion: R&D automation loop is usable; business-side lightweight acceptance is usable; still not zero-maintenance or full-role autonomous
```

- [ ] **Step 3: Write role-by-role judgment**

```md
- Developer / R&D: pass
- Tester: basic pass, but no dedicated TEST_STAFF runtime role
- Business / Acceptance / Product / PM: conditional pass for preset-package consumption
- Ops: not the primary target of business-acceptance entry
- Super admin: pass
```

### Task 3: Add The Developer Usage Guide

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`

- [ ] **Step 1: Add the developer guide section**

```md
## 10. 开发人员自动化测试使用指南
```

- [ ] **Step 2: Write the route and command guidance**

```md
- Which entry to use: /business-acceptance, /rd-workbench, /automation-execution, /automation-results
- Which commands to use: Maven targeted tests, frontend targeted tests, registry runner, local quality gates
```

- [ ] **Step 3: Write the evidence and troubleshooting guidance**

```md
- How to judge pass / fail / blocked
- How to collect registry-run evidence
- How to drill from business result to automation-results
- What still requires developer or tester maintenance
```

### Task 4: Record The Change Summary

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

- [ ] **Step 1: Add one summary bullet for this documentation consolidation**

```md
- 2026-04-05: role-based automation acceptance conclusion and developer usage guide consolidated into docs/05
```

- [ ] **Step 2: State the scope boundary**

```md
- Business-side automation is low-threshold consumption, not full autonomous scenario authoring
- Current evidence center still relies on logs/acceptance ledger files
```

### Task 5: Verify The Update

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

- [ ] **Step 1: Run topology verification**

```bash
node scripts/docs/check-topology.mjs
```

Expected: `Document topology check passed.`

- [ ] **Step 2: Re-run the targeted automation verification referenced by the acceptance opinion**

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts
mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest,AutomationResultQueryServiceImplTest test
```

Expected: frontend targeted tests pass; backend targeted tests pass.
