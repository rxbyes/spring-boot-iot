# Quality Factory Governance Consolidation Design

- Date: `2026-04-25`
- Scope: `spring-boot-iot-ui`, `sql/init-data.sql`, `docs/*`
- Status: `proposed`

## 1. Background

Current quality-factory information architecture has already split business acceptance away from RD authoring, but the overall module still feels oversized because it exposes too many first-level entrances:

1. `/quality-workbench`
2. `/business-acceptance`
3. `/rd-workbench`
4. `/automation-execution`
5. `/automation-results`

Inside that structure, `/rd-workbench` further expands into four more RD authoring pages. The result is a nested “platform inside platform” feeling:

- business roles still see too much engineering vocabulary
- execution and evidence feel detached from the RD authoring chain
- quality-factory overview has to explain too many sibling routes at once
- old compatibility routes (`/automation-assets`, `/automation-test`) keep historical structure alive

The user requested that quality factory be reorganized and merged where possible, with a simpler, smaller, and more coordinated structure.

## 2. Design Goal

Reorganize quality factory into a two-entry model:

1. `业务验收台`
2. `自动化治理台`

The new model must:

- keep business acceptance as the only business-facing quality-factory workflow
- merge RD authoring, execution setup, and evidence review into one governance workbench
- restrict governance workbench visibility to RD, test, and admin roles
- remove the old first-level entrances instead of leaving them as compatibility shells
- preserve existing underlying capabilities by reorganizing surfaces rather than rewriting the engines

## 3. Options Considered

### Option A — Two entrances plus one governance workbench with tabs

Structure:

- `/quality-workbench`
- `/business-acceptance`
- `/business-acceptance/results/:runId`
- `/automation-governance`

Inside `/automation-governance`:

- `资产编排`
- `执行配置`
- `结果证据`

Recommendation: **Yes**

Why:

- actually reduces first-level complexity instead of renaming it
- keeps business and engineering mental models separate
- preserves clear responsibility boundaries inside the governance workbench
- lets execution and evidence sit in the same governance loop as authoring

### Option B — Two entrances outside, but old engineering pages remain separate inside

Structure:

- outer shell becomes `业务验收台 + 自动化治理台`
- governance page mostly forwards to existing separate routes

Recommendation: No

Why not:

- users still experience the same fragmentation after the first click
- documentation and permissions get renamed, but the mental model does not really shrink

### Option C — One giant governance page

Structure:

- authoring, execution, and evidence all in one long page with collapse panels

Recommendation: No

Why not:

- makes the page visually heavy again
- weakens task boundaries
- increases regression risk for future extensions

## 4. Recommended Direction

Adopt **Option A**.

Quality factory will become:

- one business path: `业务验收台`
- one engineering path: `自动化治理台`

`自动化治理台` will be a single route with internal tabs, not a new shell that forwards to the old pages.

## 5. Target Information Architecture

### 5.1 Public structure

Keep:

- `/quality-workbench`
- `/business-acceptance`
- `/business-acceptance/results/:runId`

Add:

- `/automation-governance`

Remove as standalone first-level routes:

- `/rd-workbench`
- `/rd-automation-inventory`
- `/rd-automation-templates`
- `/rd-automation-plans`
- `/rd-automation-handoff`
- `/automation-execution`
- `/automation-results`
- `/automation-assets`
- `/automation-test`

### 5.2 Governance workbench tabs

Primary tabs:

1. `assets` — 资产编排
2. `execution` — 执行配置
3. `evidence` — 结果证据

Secondary workspace under `assets`:

1. `inventory` — 页面盘点
2. `templates` — 场景模板
3. `plans` — 计划编排
4. `handoff` — 交付打包

Suggested URL shape:

- `/automation-governance?tab=assets&assetTab=inventory`
- `/automation-governance?tab=assets&assetTab=templates`
- `/automation-governance?tab=assets&assetTab=plans`
- `/automation-governance?tab=assets&assetTab=handoff`
- `/automation-governance?tab=execution`
- `/automation-governance?tab=evidence`
- `/automation-governance?tab=evidence&runId=<runId>`

## 6. Role and Permission Model

### 6.1 Business-facing path

`业务验收台` remains the only quality-factory entry for:

- business users
- product users
- project managers
- management roles that only consume acceptance results

These roles should be able to:

- select package
- choose environment, account template, and module scope
- launch acceptance
- read business conclusion page

These roles should **not** be required to understand:

- templates
- plan authoring
- registry dependency graph
- evidence governance terminology

### 6.2 Governance-facing path

`自动化治理台` is visible only to:

- RD
- test
- admin / super admin

These roles can use:

- asset authoring workspaces
- execution configuration
- evidence and run detail review

### 6.3 Result deep-link boundary

Business result page currently links to `/automation-results?runId=...`.

After consolidation:

- if current user has governance permission, the action should navigate to `/automation-governance?tab=evidence&runId=...`
- if current user does not have governance permission, the deep evidence action should be hidden

This preserves the business result conclusion while keeping the detailed evidence workflow inside the engineering path.

## 7. UX Structure

### 7.1 Quality workbench overview

`/quality-workbench` becomes a strict two-entry overview:

- `业务验收台`
- `自动化治理台`

The page should no longer describe four sibling entrances. Its language should change from:

- business acceptance + RD workbench + execution + results

to:

- business acceptance path
- automation governance path

Summary metrics and helper copy should also reflect the new model:

- one business workbench
- one governance workbench
- governance workbench internally contains three work zones

### 7.2 Business acceptance page

`/business-acceptance` remains light and business-oriented.

No change in core behavior:

- package selection
- environment / account template / module scope
- launch acceptance
- show “是否通过 / 哪些模块没过”

The page should avoid new governance vocabulary. It remains a consumption surface.

### 7.3 Automation governance page

`/automation-governance` becomes the engineering operating surface.

Top level:

- light hero / summary
- primary tab bar

Tabs:

- `资产编排` shows authoring context and the secondary authoring workspace switch
- `执行配置` hosts current execution-center content
- `结果证据` hosts current results-center content

Within `资产编排`, the four existing RD authoring slices remain, but as internal governance workspaces instead of first-level quality-factory routes.

## 8. Route and Navigation Rules

### 8.1 Old entrances are removed

The user explicitly selected **direct retirement**, not compatibility redirects.

That means:

- old routes should be removed from router registration
- old menu items should be removed from seed/menu definitions
- tests and docs should stop asserting compatibility wrappers

This is a deliberate breaking IA change, not a soft migration.

### 8.2 Internal jumps update to the new route

Examples:

- device-onboarding result jump:
  - from `/automation-results?runId=...`
  - to `/automation-governance?tab=evidence&runId=...`
- business acceptance result deep evidence jump:
  - from `/automation-results?runId=...`
  - to `/automation-governance?tab=evidence&runId=...`
- shell / section-home featured items:
  - replace `研发工场 / 执行中心 / 结果与基线中心`
  - with `自动化治理台`

### 8.3 Section-home model

`sectionWorkspaces.ts` should stop modeling quality factory as:

- business acceptance
- RD workbench
- execution center
- results center

Instead model:

- business acceptance
- automation governance

And inside governance define the three internal work zones for local navigation.

## 9. Implementation Strategy

### 9.1 Reuse, do not rewrite

Existing composables and panels remain the source of truth:

- RD authoring views and supporting composables
- execution workbench composables and panels
- results workbench composables and panels

Implementation should extract or embed the existing page bodies into a new governance container instead of rebuilding those flows from scratch.

### 9.2 New page ownership

Introduce a new page:

- `spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue`

This page owns:

- primary governance tabs
- secondary assets workspace switch
- query synchronization for `tab`, `assetTab`, `runId`
- role-aware rendering

### 9.3 Existing content migration

- `RdWorkbenchLandingView.vue` will no longer survive as a first-level landing page
- `AutomationExecutionView.vue` content migrates into governance `execution`
- `AutomationResultsView.vue` content migrates into governance `evidence`
- RD authoring pages either become governance subviews or embedded content sections

## 10. Data Flow and State Rules

### 10.1 Query-driven navigation

Governance state should be query-driven so links remain shareable and restorable:

- `tab`
- `assetTab`
- `runId`

### 10.2 Evidence preselection

If `runId` exists:

- governance workbench must open `evidence`
- evidence tab must preload the target run
- result/evidence panels must preserve current preselection behavior

### 10.3 Permissions

If a user lacks permission for governance:

- `/automation-governance` should not be shown in menus
- direct navigation should be denied by route permission checks
- business pages should not render governance-only follow-up actions

## 11. Testing Strategy

### 11.1 Frontend route and view tests

Update and extend:

- `AutomationWorkbenchViews.test.ts`
- `sectionHomes.test.ts`
- route guard / permission tests
- shell panel content tests

Key assertions:

1. quality workbench only exposes two main entrances
2. automation governance route exists
3. old RD/execution/results first-level routes are removed
4. business acceptance still works as a lightweight entry
5. governance route restores the correct tab from query
6. `runId` opens governance evidence view correctly
7. business users cannot access governance route

### 11.2 Regression checks

Must verify:

1. business acceptance launch flow still works
2. business acceptance result flow still works
3. device-onboarding acceptance jump still lands on evidence details
4. evidence panels still load run ledger, run detail, evidence list, and preview
5. execution panels still show command preview, registry blockers, and scope calibration

## 12. Documentation Updates

Must update in place:

- `README.md`
- `AGENTS.md`
- `docs/02-业务功能与流程说明.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`
- `docs/21-业务功能清单与验收标准.md`

Need to reflect:

- new two-entry quality-factory model
- governance role boundary
- old entrances retired
- new route and deep-link rules
- updated quality-factory acceptance path descriptions

Menu / seed truth also needs updating in:

- `sql/init-data.sql`

## 13. Risks and Mitigations

### Risk 1 — Breaking existing deep links

Cause:

- old routes are being retired directly

Mitigation:

- update all internal jumps in the same batch
- update tests and docs in the same batch
- explicitly search for all old route references before completion

### Risk 2 — Governance page becomes too heavy

Cause:

- combining too much into one large page

Mitigation:

- use primary tabs plus secondary asset workspace switch
- reuse focused panels instead of flattening all content into one surface

### Risk 3 — Business roles accidentally retain engineering jumps

Cause:

- old result-page actions continue to point to evidence details

Mitigation:

- gate governance follow-up actions by route permission
- add permission tests for business-only visibility

## 14. Acceptance Criteria

This design is complete when:

1. Quality factory overview only presents `业务验收台` and `自动化治理台`
2. `自动化治理台` is visible only to RD/test/admin roles
3. RD authoring, execution, and evidence are reachable from one governance route with internal tabs
4. old RD/execution/results quality-factory entrances are removed from router, menu seeds, docs, and tests
5. `runId` deep links resolve to governance evidence tab
6. business acceptance remains lightweight and business-oriented
7. docs no longer describe the old four-entrance model

## 15. Out of Scope

This design does not include:

- redesigning the acceptance registry engine
- changing readiness CLI behavior
- changing business acceptance execution semantics
- introducing a new backend API
- reintroducing compatibility wrappers for retired routes

