---
name: ops-center-page
description: "Use when redesigning the device workbench into a commercial operations center for maintenance staff. Applies to device provisioning UX, device health/status interpretation, auth-baseline presentation, remote-operations placeholders, troubleshooting guidance, and business-oriented restructuring while preserving current create/query APIs."
---

# Ops Center Page

Use this skill when working on:
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`

This page should evolve from a device DTO form into an operations center.

## Read First

Read only what is needed from:
- `README.md`
- `AGENTS.md`
- `docs/03-接口规范与接口清单.md`
- `docs/06-前端开发与CSS规范.md`
- `docs/15-前端优化与治理计划.md`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/api/iot.ts`
- `spring-boot-iot-ui/src/types/api.ts`
- `spring-boot-iot-ui/src/components/ResponsePanel.vue`

## Documentation Sync Rule

When operations center changes affect device workflows, route meaning, API usage explanation, maintenance guidance, or product navigation:
- update the existing file `docs/06-前端开发与CSS规范.md`
- update `docs/15-前端优化与治理计划.md` when optimization rules, shared patterns, or style drift records change
- update matching files under `docs/` if behavior or workflow semantics changed
- review and update `README.md` and `AGENTS.md` when their instructions or scope become outdated

Never create parallel docs to describe the new state. Update the current source-of-truth files in place.
This requirement also applies when work is executed by Qwen Code or any other coding model.

## Page Goal

The page should help operations staff:
- create devices correctly
- inspect online status quickly
- confirm auth baseline
- understand current maintenance priority
- know what remote action is expected next

## Primary User

Primary:
- operations / maintenance staff

Secondary:
- developers / implementers

## Required Sections

Prefer this structure:
- operations health banner
- maintenance action panel
- device provisioning form
- lookup / status panel
- auth baseline panel
- remote operations placeholder panel
- engineering troubleshooting notes
- request/response audit

## Existing APIs To Reuse

Use these first:
- `POST /device/add`
- `GET /device/{id}`
- `GET /device/code/{deviceCode}`

Do not require new backend APIs just to improve page positioning.

## Allowed Frontend Interpretation

You may compute lightweight values from current data:
- ops health score
- online/offline summary
- reporting freshness
- auth readiness
- maintenance suggestions

Do not pretend remote control exists if it is only a placeholder.

## Design Rules

- keep provisioning usable
- elevate lookup results into maintenance language
- make auth fields understandable for MQTT operations
- keep remote operations as a clear future slot
- preserve request/response visibility for implementation teams

## Workflow

1. inspect the current device workbench
2. keep working create/query flows
3. add business framing for operations use
4. compute simple maintenance summary if helpful
5. preserve audit panels
6. update existing docs in place:
   - `docs/06-前端开发与CSS规范.md`
   - `docs/15-前端优化与治理计划.md` when shared frontend rules change
   - related files under `docs/` if workflow/API meaning changed
   - `README.md` and `AGENTS.md` when needed
7. run frontend build

## Validation

Run:

```bash
cd spring-boot-iot-ui
npm run build
```

Then verify:
- device create flow still works visually
- query-by-id and query-by-code still work
- ops health and auth baseline render from current data

## Output Expectations

After work, report:
1. changed files
2. what changed in operations-facing meaning
3. which APIs were preserved
4. how to test in browser
5. which remote operations remain placeholders
6. which existing docs were updated
