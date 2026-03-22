---
name: frontend-commercial-product
description: "Use when evolving spring-boot-iot-ui from a debugging console into a commercial IoT risk monitoring product. Applies to navigation redesign, homepage/product cockpit redesign, role-based workbenches for field staff / operations / developers, risk workflow presentation, and business-oriented frontend restructuring without breaking current backend APIs."
---

# Frontend Commercial Product

Use this skill when the task is to turn `spring-boot-iot-ui` from a developer-facing debug console into a commercial product frontend for:
- field staff
- operations / maintenance staff
- developers / implementers
- managers who need risk overview dashboards

This skill is for frontend productization work, not backend feature implementation.

## Read First

Before editing, read only what is needed from:
- `AGENTS.md`
- `README.md`
- `docs/01-系统概览与架构说明.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/06-前端开发与CSS规范.md`
- `docs/05-protocol.md`
- `docs/15-前端优化与治理计划.md`

Then inspect the current frontend files that are directly related to the requested page:
- `spring-boot-iot-ui/src/router/index.ts`
- `spring-boot-iot-ui/src/components/AppShell.vue`
- `spring-boot-iot-ui/src/styles/global.css`
- target page under `spring-boot-iot-ui/src/views/`
- supporting APIs under `spring-boot-iot-ui/src/api/iot.ts`
- related types under `spring-boot-iot-ui/src/types/api.ts`

## Product Goal

The frontend should express a commercial product, not just a toolbox.

Translate technical capabilities into business value:
- device access -> monitoring capability
- raw properties -> risk signals
- message logs -> traceability / audit
- MQTT / HTTP debug -> access verification center
- file / firmware debug -> operations and upgrade validation

The platform should communicate four outcomes:
1. discover risk earlier
2. judge severity faster
3. guide field action clearly
4. support remote operations and troubleshooting

## Fixed Constraints

Always preserve these constraints:
- do not break existing backend API contracts unless explicitly asked
- do not invent heavy new dependencies unless clearly necessary
- keep `spring-boot-iot-admin` as the only backend startup module
- keep backend module boundaries intact
- frontend changes should stay inside `spring-boot-iot-ui` unless behavior docs must be updated
- when frontend or backend behavior changes, update the existing source-of-truth docs in place
- always update the corresponding file under `docs/`
- always review whether `README.md` and `AGENTS.md` also need updates
- never create duplicate docs such as `README-v2.md`, `new-frontend-doc.md`, `api-new.md`, or similar replacement files
- this rule also applies when work is executed by Qwen Code or any other coding model

## Documentation Sync Rule

If code changes affect behavior, routes, APIs, workflows, startup commands, testing flow, navigation, product positioning, or page meaning, you must update the existing documentation files in place.

Default documentation targets:
- `docs/06-前端开发与CSS规范.md` for frontend structure, page semantics, and shared UI rules
- `docs/15-前端优化与治理计划.md` for frontend optimization constraints and governance notes
- matching files under `docs/` when backend/API/protocol behavior changed
- `README.md` when usage, startup, validation, or product scope changed
- `AGENTS.md` when development rules, required reading, or workflow assumptions changed

Do not solve documentation drift by creating a parallel file. Update the current source-of-truth file instead.

## Current Product Direction

The commercialized frontend should be organized around these top-level areas:

1. `驾驶舱`
- risk overview
- role entry points
- business workflow summary

2. `风险业务`
- risk point workbench
- event / report / trend entry points

3. `运维中心`
- device operations center
- product template center
- remote operations placeholders

4. `研发与调试`
- access replay center
- file / firmware debug
- protocol / audit / future roadmap

Keep existing debug ability, but move it under a productized information architecture.

## Role Model

Every major page should be understandable from one or more of these roles:

### Field Staff
Focus:
- what is risky
- how severe it is
- whether it should be reported upward
- what report text or conclusion should be used

### Operations Staff
Focus:
- whether devices are online
- whether reporting is fresh
- whether credentials / firmware / thresholds are healthy
- what remote action should happen next

### Developers / Implementers
Focus:
- whether the chain is complete
- whether protocol parsing is correct
- where logs and raw payloads can be checked
- whether access behavior matches design

When redesigning a page, make sure at least one of these role perspectives is explicit in the UI.

## Transformation Rules

### 1. Rename technical pages into business pages

Prefer product names over pure engineering names.

Examples:
- `调试驾驶舱` -> `风险监测驾驶舱`
- `设备洞察` -> `风险点工作台`
- `设备工作台` -> `设备运维中心`
- `HTTP 上报实验台` -> `接入回放台`
- `文件调试台` -> `文件与固件调试`

### 2. Keep data, change interpretation

Do not wait for new backend APIs if existing APIs are enough for an MVP business page.

Use current data to produce lightweight frontend-side interpretation:
- health status
- risk level
- urgency
- recommended next actions
- report draft text

This is allowed as long as you do not claim it is authoritative AI analysis.

### 3. Business-first layout

Pages should answer:
- what is happening now
- why it matters
- what the user should do next
- where deeper audit / logs can be found

Do not start a page with raw JSON or a large form unless the page is specifically a replay/debug page.

### 4. Preserve operational depth

Even after productization, keep audit and debugging visibility accessible:
- raw request / response panels
- message logs
- payload previews
- quick links into replay/debug pages

The product should look business-ready without losing engineering usefulness.

## Recommended Upgrade Order

Use this order unless the user asks for a different page first:

1. global navigation and homepage
2. risk point workbench
3. device operations center
4. access replay center
5. product template center
6. file / firmware debug
7. future roadmap / blueprint pages

## Page Recipes

### Recipe A: Cockpit / Homepage

Goal:
- show platform positioning
- show risk level language
- show role entry points
- show workflow and roadmap

Recommended sections:
- hero with product value
- key metrics
- role workspace cards
- risk workflow rail
- current capability mapping
- roadmap / future evolution cards

Avoid:
- starting with request/response debug panels
- presenting only module counts without business meaning

### Recipe B: Risk Point Workbench

Goal:
- make one device feel like one monitored risk point

Recommended sections:
- point identity and status
- current risk banner
- risk reasons / evidence
- trend preview
- field actions
- operations actions
- engineering actions
- key properties
- report draft
- message log audit

Use existing APIs first:
- `GET /device/code/{deviceCode}`
- `GET /device/{deviceCode}/properties`
- `GET /device/{deviceCode}/message-logs`

### Recipe C: Device Operations Center

Goal:
- make device provisioning and status lookup feel like operations work, not a DTO demo

Recommended sections:
- ops health banner
- maintenance actions
- provisioning form
- lookup panel
- auth baseline
- remote operations placeholders
- engineering troubleshooting notes
- request/response audit

Use existing APIs first:
- `POST /device/add`
- `GET /device/{id}`
- `GET /device/code/{deviceCode}`

### Recipe D: Access Replay Center

Goal:
- unify HTTP / MQTT / encrypted payload validation as an access verification experience

Recommended sections:
- access mode chooser
- payload templates
- topic suggestion
- protocol notes
- replay action panel
- response status and trace panel
- handoff links to risk workbench and logs

Use existing APIs first:
- `POST /message/http/report`
- any already available debug query APIs

### Recipe E: Product Template Center

Goal:
- present products as reusable monitoring templates

Recommended sections:
- product template creation
- protocol / node type explanation
- manufacturer / data format semantics
- lookup and template audit
- future placeholders for model / protocol visualization

Use existing APIs first:
- `POST /device/product/add`
- `GET /device/product/{id}`

## Design Guidance

Keep the visual direction intentional:
- do not fall back to a generic admin template look
- keep the strong industrial / monitoring atmosphere
- prefer deep blue / steel / safety-accent language over random gradients
- use red / orange / yellow / blue as explicit risk semantics
- typography should feel confident and operational

Good visual priorities:
- strong hierarchy
- fast scanning
- obvious action zones
- clear distinction between business panels and audit panels

## Implementation Workflow

For each requested page:

1. inspect the current page and its supporting APIs
2. identify which user role the page should primarily serve
3. preserve existing working calls and data contracts
4. reorganize layout into business sections
5. add only lightweight computed interpretation on the frontend if needed
6. update route title / description if the page meaning changed
7. update the existing docs in place:
   - `docs/06-前端开发与CSS规范.md`
   - `docs/15-前端优化与治理计划.md` when shared frontend rules change
   - related files under `docs/` if behavior changed
   - `README.md` and `AGENTS.md` when their content is affected
8. run frontend build

## Validation

Always validate with:

```bash
cd spring-boot-iot-ui
npm run build
```

If local dev servers are already running, also sanity-check:
- target page route loads
- existing API-backed panels still render
- no old navigation link is broken

## Output Expectations

After completing work, report:
1. changed files
2. what product meaning changed
3. which current backend APIs were reused
4. how to test in the browser
5. what still remains a placeholder or future capability
6. which existing docs were updated

## What Not To Do

Do not:
- move frontend into backend static resources unless explicitly asked
- redesign everything at once without preserving working pages
- claim AI analysis is real if it is only heuristic frontend interpretation
- block commercial productization on backend perfection
- put backend business logic into the frontend

## Good Default Assumption

If the user asks to “optimize a page”, assume they want:
- stronger business framing
- clearer role-oriented actions
- preserved engineering traceability
- minimal backend change

Prefer evolutionary productization over destructive rewrites.
