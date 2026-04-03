# Automation Results Evidence Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add evidence-list and text-preview capabilities to the automation results workbench while keeping the current recent-run loading and manual JSON import paths intact.

**Architecture:** Extend the report module with two read-only evidence endpoints derived from existing `logs/acceptance/registry-run-*.json` artifacts. Reuse the current recent-run selection flow in the frontend, add evidence state to `useAutomationRegistryWorkbench`, and render a dedicated evidence panel on the results page.

**Tech Stack:** Spring Boot 4, Java 17, Jackson 3, Vue 3, Vitest, shared workbench components.

---

## File Map

### New files

- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultEvidenceItemVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultEvidenceContentVO.java`
- `spring-boot-iot-ui/src/components/AutomationResultEvidencePanel.vue`

### Modified files

- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultQueryService.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/AutomationResultController.java`
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/AutomationResultControllerTest.java`
- `spring-boot-iot-ui/src/types/automation.ts`
- `spring-boot-iot-ui/src/api/automationResults.ts`
- `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
- `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
- `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
- `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
- `docs/03-接口规范与接口清单.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`

## Task 1: Lock the backend evidence contract

**Files:**
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/AutomationResultControllerTest.java`

- [ ] **Step 1: Write the failing service tests for evidence listing and preview**
- [ ] **Step 2: Run `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AutomationResultQueryServiceImplTest,AutomationResultControllerTest test` and verify it fails for missing evidence APIs**
- [ ] **Step 3: Add failing controller route checks for `/api/report/automation-results/{runId}/evidence` and `/api/report/automation-results/{runId}/evidence/content`**

## Task 2: Implement backend evidence APIs

**Files:**
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultEvidenceItemVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultEvidenceContentVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultQueryService.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/AutomationResultController.java`

- [ ] **Step 1: Add service methods for evidence listing and content preview**
- [ ] **Step 2: Restrict readable files to the current run’s referenced files under `logs/acceptance`**
- [ ] **Step 3: Return UTF-8 text preview with truncation metadata**
- [ ] **Step 4: Re-run the report-module tests and verify they pass**

## Task 3: Lock frontend evidence behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Extend the failing composable test to require evidence list and preview loading**
- [ ] **Step 2: Extend the view contract test to require `<AutomationResultEvidencePanel` on the results page**
- [ ] **Step 3: Run `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts` and verify it fails for missing evidence state/UI**

## Task 4: Implement frontend evidence panel

**Files:**
- Create: `spring-boot-iot-ui/src/components/AutomationResultEvidencePanel.vue`
- Modify: `spring-boot-iot-ui/src/types/automation.ts`
- Modify: `spring-boot-iot-ui/src/api/automationResults.ts`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/views/AutomationResultsView.vue`

- [ ] **Step 1: Add frontend types and API methods for evidence list/content**
- [ ] **Step 2: Add evidence state/actions to `useAutomationRegistryWorkbench`**
- [ ] **Step 3: Render the evidence panel in `AutomationResultsView.vue` and auto-load the run-summary artifact after selecting a recent run**
- [ ] **Step 4: Re-run the targeted Vitest suite and verify it passes**

## Task 5: Sync docs and verify

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Document the new evidence endpoints and results-page evidence preview behavior**
- [ ] **Step 2: Re-run the report-module tests**
- [ ] **Step 3: Re-run the targeted frontend tests**
- [ ] **Step 4: Run `node scripts/run-quality-gates.mjs`**

## Self-Review

### Spec coverage

This plan covers:
1. run-level evidence list
2. text preview endpoint
3. results-page evidence panel
4. docs and verification

### Placeholder scan

No TODO/TBD placeholders remain.

### Type consistency

The plan keeps naming consistent around:
- `evidence`
- `runId`
- `path`
- `content`
