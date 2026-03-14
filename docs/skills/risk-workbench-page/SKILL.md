---
name: risk-workbench-page
description: "Use when redesigning the device insight page into a commercial risk point workbench for field staff, operations staff, and developers. Applies to risk level presentation, evidence sections, action recommendations, report-draft presentation, and business-oriented reorganization of existing device/property/log data without requiring new backend APIs."
---

# Risk Workbench Page

Use this skill when working on:
- `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

This page should become the core risk point workbench for field use.

## Read First

Read only what is needed from:
- `README.md`
- `AGENTS.md`
- `docs/04-api.md`
- `docs/13-frontend-debug-console.md`
- `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- `spring-boot-iot-ui/src/components/PropertyTrendPanel.vue`
- `spring-boot-iot-ui/src/api/iot.ts`
- `spring-boot-iot-ui/src/types/api.ts`

## Documentation Sync Rule

When risk workbench changes affect page behavior, route meaning, workflow guidance, API usage explanation, or product positioning:
- update the existing file `docs/13-frontend-debug-console.md`
- update matching docs under `docs/` if frontend behavior now reflects changed backend/API meaning
- review `README.md` and `AGENTS.md` and update them when their wording becomes stale

Do not generate a new documentation file to describe the new state. Update the existing files in place.
This rule applies to Qwen Code and any other coding model.

## Page Goal

Turn one device page into one risk point page.

The page should help users:
- understand current severity
- see why that severity exists
- decide what to do next
- see supporting logs and properties

## Main User Roles

### Field Staff
- need current risk level
- need simple action guidance
- need report wording

### Operations Staff
- need online state
- need freshness
- need device health clues

### Developers / Implementers
- need raw logs
- need protocol traceability
- need quick jump to replay/debug

## Required Sections

Prefer this structure:
- risk banner
- point profile
- risk reasons / evidence
- trend preview
- field actions
- operations actions
- engineering actions
- key properties
- report draft
- message log audit

## Allowed Frontend Interpretation

You may compute lightweight frontend-side values from existing APIs:
- risk score
- red / orange / yellow / blue level
- freshness status
- action recommendations
- report draft summary

Do not claim this is authoritative AI analysis unless backend AI exists.

## Existing APIs To Reuse

Use these first:
- `GET /device/code/{deviceCode}`
- `GET /device/{deviceCode}/properties`
- `GET /device/{deviceCode}/message-logs`

Do not block the page redesign waiting for new backend APIs.

## Design Rules

- keep business meaning first
- keep engineering audit visible but secondary
- make the top of the page decisive and action-oriented
- avoid starting with giant tables
- preserve trend visibility

## Workflow

1. inspect current page structure
2. identify available fields from current APIs
3. reorganize layout around business sections
4. add lightweight computed interpretation if useful
5. preserve existing data tables/log trace
6. update route meaning if needed
7. update existing docs in place:
   - `docs/13-frontend-debug-console.md`
   - related files under `docs/` if API/workflow meaning changed
   - `README.md` and `AGENTS.md` when needed
8. run frontend build

## Validation

Run:

```bash
cd spring-boot-iot-ui
npm run build
```

Then verify:
- page still loads with `deviceCode`
- existing APIs still populate the page
- risk banner, action panels, and report draft render

## Output Expectations

After work, report:
1. changed files
2. which backend APIs were reused
3. what business-facing sections were added
4. how to test in browser
5. what still depends on future backend / AI support
6. which existing docs were updated
