# Message Trace Time And Detail Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `/message-trace` so UTC-marked report times render in China Standard Time and the detail drawer removes repeated header tags while simplifying the "链路标识" section.

**Architecture:** Keep the backend contract unchanged for this round and normalize display in the shared frontend time formatter so explicit UTC or offset timestamps convert to `Asia/Shanghai` while naive timestamps remain unchanged. Update the message trace detail drawer configuration and workbench fields to remove duplicate header tags, keep `TraceId`, and reflow "日志 ID / 创建时间 / Topic" according to the confirmed UI contract.

**Tech Stack:** Vue 3, TypeScript, Vitest, Element Plus, project docs in `docs/`

---

### Task 1: Capture The Expected Display Rules In Tests

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/utils/format.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/format.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: Write the failing formatter tests**

```ts
import { describe, expect, it } from 'vitest';
import { formatDateTime } from '@/utils/format';

describe('formatDateTime', () => {
  it('converts explicit UTC timestamps to Asia/Shanghai time', () => {
    expect(formatDateTime('2026-04-05T10:50:35Z')).toBe('2026/04/05 18:50:35');
  });

  it('keeps naive timestamps unchanged', () => {
    expect(formatDateTime('2026-04-05 10:50:35')).toBe('2026/04/05 10:50:35');
  });
});
```

- [ ] **Step 2: Run the formatter test to verify it fails**

Run: `npm test -- src/__tests__/utils/format.test.ts`
Expected: FAIL because the current formatter does not guarantee the `Asia/Shanghai` conversion and output shape required by the new assertions.

- [ ] **Step 3: Add failing message trace view expectations**

```ts
it('renders UTC report times in China time and removes duplicated header tags', async () => {
  vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [{
        id: 1,
        traceId: 'trace-001',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        messageType: 'report',
        topic: '$dp',
        payload: '{"temperature":24}',
        reportTime: '2026-04-05T10:50:35Z',
        createTime: '2026-04-05 10:50:38'
      }]
    }
  });

  await findButtonByText(wrapper, '详情')!.trigger('click');

  expect(wrapper.text()).toContain('2026/04/05 18:50:35');
  expect(wrapper.text()).toContain('创建时间2026/04/05 10:50:38');
  expect(wrapper.text()).toContain('Topic$dp');
  expect(wrapper.text()).not.toContain('消息类型属性上报');
  expect(wrapper.text()).not.toContain('Trace trace-001');
});
```

- [ ] **Step 4: Run the message trace view test to verify it fails**

Run: `npm test -- src/__tests__/views/MessageTraceView.test.ts`
Expected: FAIL because the current detail drawer still renders inline header tags and the current time formatter does not match the new display rule.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/__tests__/utils/format.test.ts spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "test: capture message trace time and detail layout expectations"
```

### Task 2: Implement The Shared Time Formatting Rule

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/format.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/format.test.ts`

- [ ] **Step 1: Write the minimal formatter implementation**

```ts
const DISPLAY_TIME_ZONE = 'Asia/Shanghai';
const EXPLICIT_ZONE_PATTERN = /(z|[+-]\\d{2}:?\\d{2})$/i;

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--';
  }

  const normalized = String(value).trim();
  if (!normalized) {
    return '--';
  }

  const explicitZone = EXPLICIT_ZONE_PATTERN.test(normalized);
  if (!explicitZone) {
    return normalized.replace('T', ' ').replace(/-/g, '/');
  }

  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: DISPLAY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date);
}
```

- [ ] **Step 2: Run the formatter test to verify it passes**

Run: `npm test -- src/__tests__/utils/format.test.ts`
Expected: PASS with both explicit UTC conversion and naive timestamp preservation covered.

- [ ] **Step 3: Refine the formatter output shape if needed**

```ts
return formatter.format(date).replace(/-/g, '/');
```

- [ ] **Step 4: Re-run the formatter test after the cleanup**

Run: `npm test -- src/__tests__/utils/format.test.ts`
Expected: PASS with the exact `yyyy/MM/dd HH:mm:ss` output still intact.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/utils/format.ts spring-boot-iot-ui/src/__tests__/utils/format.test.ts
git commit -m "fix: normalize UTC display time for message trace"
```

### Task 3: Remove Duplicate Detail Header Tags And Reflow Message Trace Fields

**Files:**
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/components/messageTrace/MessageTraceDetailWorkbench.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: Remove duplicated drawer header tags**

```ts
const detailTags = computed(() => []);
```

- [ ] **Step 2: Update the detail workbench field layout**

```ts
const ledgerIdentityItems = computed<LedgerItem[]>(() => [
  { key: 'id', label: '日志 ID', value: formatValue(props.detail.id) },
  { key: 'createTime', label: '创建时间', value: formatDateTime(props.detail.createTime) },
  { key: 'traceId', label: 'TraceId', value: formatValue(props.detail.traceId), wide: true }
]);

const ledgerContextItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: formatValue(props.detail.deviceCode) },
  { key: 'productKey', label: '产品标识', value: formatValue(props.detail.productKey) },
  { key: 'topic', label: 'Topic', value: formatValue(props.detail.topic), wide: true }
]);
```

- [ ] **Step 3: Reuse the shared formatter everywhere the trace list/detail shows report time**

```vue
{{ formatDateTime(row.reportTime || row.createTime) }}
```

- [ ] **Step 4: Run the message trace view test to verify it passes**

Run: `npm test -- src/__tests__/views/MessageTraceView.test.ts`
Expected: PASS with the simplified drawer header, updated "链路标识" section, and UTC display regression covered.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/components/messageTrace/MessageTraceDetailWorkbench.vue spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "fix: simplify message trace detail drawer"
```

### Task 4: Sync Documentation And Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Document the message trace display contract**

```md
- `/message-trace` 当前统一按中国时区展示显式 UTC / offset 上报时间；无时区的历史时间串继续按原值显示，避免把已是本地时间的日志再次偏移。
- 详情抽屉标题行不再重复显示 `属性上报 / TraceId / Topic` 标签；`链路标识` 当前固定收口为 `日志 ID + 创建时间 + TraceId`。
```

- [ ] **Step 2: Record the frontend governance rule**

```md
- 链路追踪详情抽屉不得在标题行与正文同时重复展示消息类型、TraceId 或 Topic；详情识别信息统一以下方 `链路标识 / 接入上下文` 分组为准。
```

- [ ] **Step 3: Run focused verification**

Run: `npm test -- src/__tests__/utils/format.test.ts src/__tests__/views/MessageTraceView.test.ts`
Expected: PASS with no new failures in the touched trace display paths.

- [ ] **Step 4: Run the project quality guard if the focused tests are green**

Run: `npm run component:guard`
Expected: PASS or no new regressions related to the touched message trace page.

- [ ] **Step 5: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/11-可观测性、日志追踪与消息通知治理.md docs/15-前端优化与治理计划.md docs/08-变更记录与技术债清单.md
git commit -m "docs: sync message trace time and detail contract"
```
