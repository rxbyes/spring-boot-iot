# Quality Factory Result Archive Index Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a logs-based automation result archive index so the governance evidence workspace can query stable run-level records by package, environment, runner type, status, and time without introducing a database table.

**Architecture:** Keep `logs/acceptance` and `registry-run-*.json` as the source of truth, add a shared archive-index schema, build offline artifacts in `scripts/auto`, and make the report module consume a `.latest.json` index with automatic refresh when it is missing or stale. The frontend continues to live inside `/automation-governance?tab=evidence`, but upgrades the ledger header and actions to use the new index-backed filters.

**Tech Stack:** Node.js ESM scripts, Spring Boot report module, Vue 3, Vitest, JUnit 5, Maven

---

### Task 1: Lock the archive-index schema with Node tests first

**Files:**
- Create: `scripts/auto/automation-result-archive-index-lib.mjs`
- Create: `scripts/auto/generate-automation-result-archive-index.mjs`
- Create: `scripts/auto/automation-result-archive-index.test.mjs`

- [ ] **Step 1: Write a failing test for the run-level archive schema**

Cover these behaviors in `scripts/auto/automation-result-archive-index.test.mjs`:

1. Two valid `registry-run-*.json` files produce:
   - `automation-result-index.latest.json`
   - one timestamped JSON
   - one timestamped Markdown
2. `facets.packageCodes`, `facets.environmentCodes`, and `facets.runnerTypes` are deduplicated and sorted.
3. `evidenceItems` include the report file plus related/scenario evidence.
4. Invalid JSON files are recorded in `skippedFiles` instead of crashing the run.

- [ ] **Step 2: Run the new Node test file and verify RED**

Run:

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

Expected: FAIL because the archive-index library and CLI do not exist yet.

- [ ] **Step 3: Implement the minimal Node library and CLI**

Create:

1. `scripts/auto/automation-result-archive-index-lib.mjs`
   - parse `registry-run-*.json`
   - normalize `runId / status / summary / packageCode / environmentCode / runnerTypes`
   - collect `evidenceItems`
   - build `facets`
   - render JSON and Markdown payloads
2. `scripts/auto/generate-automation-result-archive-index.mjs`
   - parse CLI args
   - call the library
   - write `.latest.json` and timestamped JSON/Markdown

- [ ] **Step 4: Re-run the Node archive test and verify GREEN**

Run:

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

Expected: PASS

- [ ] **Step 5: Commit the archive CLI slice**

```bash
git add scripts/auto/automation-result-archive-index-lib.mjs scripts/auto/generate-automation-result-archive-index.mjs scripts/auto/automation-result-archive-index.test.mjs
git commit -m "feat: add automation result archive index cli"
```

### Task 2: Make the report module read and refresh the archive index

**Files:**
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultArchiveIndexService.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImpl.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveFacetVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveRefreshVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultQueryService.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/AutomationResultController.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunSummaryVO.java`
- Create: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImplTest.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`

- [ ] **Step 1: Add failing Java tests for index refresh and new filters**

Add tests that prove:

1. `pageRuns()` reads `.latest.json` instead of walking every registry file when the index is fresh.
2. missing or stale `.latest.json` triggers an automatic rebuild.
3. `pageRuns()` supports `packageCode` and `environmentCode`.
4. `listRecentRuns()` uses indexed records.
5. `listFacets()` returns `statuses / runnerTypes / packageCodes / environmentCodes`.
6. `refreshIndex()` returns a summary with indexed run count and skipped file count.

- [ ] **Step 2: Run the report-module tests and verify RED**

Run:

```bash
./mvnw -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest test
```

Expected: FAIL because the new service, VO types, and query filters are missing.

- [ ] **Step 3: Implement the archive-index service and query integration**

Implementation requirements:

1. `AutomationResultArchiveIndexServiceImpl` must:
   - scan `resultsDir`
   - parse `registry-run-*.json`
   - normalize run-level records into the agreed schema
   - write `automation-result-index.latest.json`
   - optionally write timestamped JSON/Markdown for manual refresh operations
2. `AutomationResultQueryServiceImpl` must:
   - call the archive-index service before `pageRuns()` and `listRecentRuns()`
   - keep `getRunDetail()` and evidence endpoints on the raw files
   - filter by `packageCode` and `environmentCode`
3. `AutomationResultController` must expose:
   - `GET /api/report/automation-results/facets`
   - `POST /api/report/automation-results/refresh-index`

- [ ] **Step 4: Re-run the report-module tests and verify GREEN**

Run:

```bash
./mvnw -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest test
```

Expected: PASS

- [ ] **Step 5: Commit the backend index-backed query slice**

```bash
git add spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultArchiveIndexService.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImpl.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveFacetVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveRefreshVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/AutomationResultQueryService.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/AutomationResultController.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunSummaryVO.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImplTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java
git commit -m "feat: index automation result archive queries"
```

### Task 3: Upgrade the governance evidence workspace with index-backed filters

**Files:**
- Modify: `spring-boot-iot-ui/src/api/automationResults.ts`
- Modify: `spring-boot-iot-ui/src/types/automation.ts`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/components/AutomationRecentRunsPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/automationGovernance/AutomationEvidenceWorkspaceSection.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Add failing Vitest coverage for package/environment filters and refresh-index**

Add tests that prove:

1. the ledger query sends `packageCode` and `environmentCode`;
2. facets are loaded and mapped into the filter selects;
3. clicking `刷新索引` calls the refresh endpoint, then reloads the ledger;
4. the evidence workspace layout still renders inside `/automation-governance?tab=evidence`.

- [ ] **Step 2: Run the focused frontend tests and verify RED**

Run:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Workdir:

```bash
/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/.worktrees/quality-factory-result-archive-index/spring-boot-iot-ui
```

Expected: FAIL because the new filters, facets, and refresh-index action are not implemented yet.

- [ ] **Step 3: Implement the minimal frontend integration**

Implementation requirements:

1. `automationResults.ts` adds:
   - `listAutomationResultFacets()`
   - `refreshAutomationResultIndex()`
2. `AutomationResultRunPageQuery` and `AutomationResultLedgerFilters` add:
   - `packageCode`
   - `environmentCode`
3. `useAutomationRegistryWorkbench.ts`:
   - loads facets on mount
   - stores package/environment options
   - sends new filters in `pageAutomationResults`
   - exposes a `refreshResultArchiveIndex()` action
4. `AutomationRecentRunsPanel.vue`:
   - adds package/environment selects
   - keeps the existing calm, symmetric filter bar
   - adds a separate `刷新索引` action

- [ ] **Step 4: Re-run the focused frontend tests and verify GREEN**

Run:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit the governance evidence workspace upgrade**

```bash
git add spring-boot-iot-ui/src/api/automationResults.ts spring-boot-iot-ui/src/types/automation.ts spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts spring-boot-iot-ui/src/components/AutomationRecentRunsPanel.vue spring-boot-iot-ui/src/components/automationGovernance/AutomationEvidenceWorkspaceSection.vue spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add archive-index filters to automation governance"
```

### Task 4: Synchronize documentation and process evidence

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/superpowers/README.md`

- [ ] **Step 1: Update the authoritative docs**

Document:

1. the new archive-index CLI command;
2. the meaning of `.latest.json` vs timestamped JSON/Markdown;
3. the governance evidence workspace filter dimensions;
4. the boundary that archive indexing improves queryability but does not replace real-environment acceptance.

- [ ] **Step 2: Run documentation and formatting verification**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: PASS

- [ ] **Step 3: Commit the documentation sync**

```bash
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md docs/superpowers/README.md
git commit -m "docs: document automation result archive indexing"
```

### Task 5: Run final verification on the complete slice

**Files:**
- All files from Tasks 1-4

- [ ] **Step 1: Run the Node archive-index tests**

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

- [ ] **Step 2: Run the report-module verification tests**

```bash
./mvnw -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest test
```

- [ ] **Step 3: Run the focused frontend test suite**

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Workdir:

```bash
/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/.worktrees/quality-factory-result-archive-index/spring-boot-iot-ui
```

- [ ] **Step 4: Run the frontend build**

```bash
npm run build
```

Workdir:

```bash
/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/.worktrees/quality-factory-result-archive-index/spring-boot-iot-ui
```

- [ ] **Step 5: Run topology and diff sanity checks**

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

- [ ] **Step 6: Prepare integration**

If all checks pass, either:

1. merge back to `codex/dev` after verifying the main workspace is clean, or
2. keep the worktree branch for review until the user asks for integration.
