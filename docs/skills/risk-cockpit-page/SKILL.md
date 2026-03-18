---
name: risk-cockpit-page
description: "Use when redesigning the spring-boot-iot-ui homepage into a commercial risk monitoring cockpit. Applies to navigation-aware homepage work, role entry redesign, risk workflow presentation, product positioning, and converting a debug dashboard into a business-facing landing page without breaking existing frontend routes or backend APIs."
---

# Risk Cockpit Page

Use this skill when working on the homepage / cockpit page in:
- `spring-boot-iot-ui/src/views/CockpitView.vue`

This skill is only for the cockpit page, not for all frontend pages.

## Read First

Read only what is needed from:
- `README.md`
- `AGENTS.md`
- `docs/06-前端开发与CSS规范.md`
- `docs/15-frontend-optimization-plan.md`
- `spring-boot-iot-ui/src/router/index.ts`
- `spring-boot-iot-ui/src/components/AppShell.vue`
- `spring-boot-iot-ui/src/views/CockpitView.vue`
- `spring-boot-iot-ui/src/styles/global.css`

## Documentation Sync Rule

When cockpit changes affect navigation, homepage meaning, product positioning, startup understanding, or frontend information architecture:
- update the existing file `docs/06-前端开发与CSS规范.md`
- update `docs/15-frontend-optimization-plan.md` when shared frontend structure or optimization rules change
- review and update `README.md` if visible product structure or usage guidance changed
- review and update `AGENTS.md` if workflow expectations or required references changed

Do not create duplicate docs. Update the existing source-of-truth files in place.
This rule must be followed by any coding model, including Qwen Code.

## Page Goal

The homepage must feel like a commercial product cockpit, not a debug landing page.

It should answer:
1. what this platform does
2. who it serves
3. what the main workflows are
4. where the user should go next

## Primary Users

Reflect these roles clearly:
- field staff
- operations staff
- developers / implementers
- managers who need overview

## Required Structure

The cockpit should usually contain:
- hero section with product value
- key business metrics
- role-based entry cards
- risk workflow rail
- current platform capability mapping
- roadmap / future evolution section

## Transformation Rules

- do not open with raw request/response data
- do not present only technical module counts without business meaning
- translate technical abilities into business language
- keep links into detailed workbenches obvious
- preserve industrial / monitoring visual language

## Preferred Business Language

Prefer:
- 风险监测驾驶舱
- 风险态势
- 角色工作入口
- 风险处置闭环
- 平台能力映射
- 商业化演进路线

Avoid:
- 调试任务首页
- 接口演示首页
- 纯工程术语堆叠

## Data Strategy

The cockpit may use static or lightweight computed content first.

It does not need new backend APIs to become productized.

Good homepage content sources:
- existing route links
- current activity store
- current known platform capabilities
- current product roadmap

## Visual Direction

- emphasize command-center feeling
- use strong hierarchy
- use risk colors intentionally
- keep quick-scan readability
- avoid generic card walls without narrative

## Workflow

1. inspect current homepage
2. identify what still reads as “debug-only”
3. rewrite sections into product-oriented language
4. ensure role navigation is visible
5. keep current routes working
6. update existing docs in place:
   - `docs/06-前端开发与CSS规范.md`
   - `docs/15-frontend-optimization-plan.md` when shared frontend structure changes
   - `README.md` if product structure or usage changed
   - `AGENTS.md` if workflow rules changed
7. run frontend build

## Validation

Run:

```bash
cd spring-boot-iot-ui
npm run build
```

Then verify:
- homepage route `/` still loads
- navigation links still work
- page communicates product positioning immediately

## Output Expectations

After work, report:
1. changed files
2. what changed in product meaning
3. which existing routes were reused
4. how to verify in browser
5. what remains roadmap / placeholder
6. which existing docs were updated
