# IoT Access Diagnostic Flow Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不新增后端聚合接口、不改动既有路由骨架的前提下，为 `接入智维` 补齐“流程联动 + 诊断辅助”第一阶段能力，让 `链路验证中心`、`链路追踪台`、`异常观测台`、`数据校验台`、`失败归档` 和治理页之间形成连续排查流。

**Architecture:** 新增一份轻量前端共享工具 `iotAccessDiagnostics`，统一管理 `DiagnosticContext`、`DiagnosticFinding`、query / sessionStorage 恢复与来源文案；页面继续复用现有 `StandardWorkbenchPanel`、`StandardInlineState`、`StandardDetailDrawer`、`StandardButton` 和紧凑 command strip，不引入第二套视觉容器。实施顺序固定为“共享上下文 -> 入口页联动 -> 追踪 / 观测 / 校验 -> 失败归档治理回跳 -> 治理页接收来源 -> 文档与验证”。

**Tech Stack:** Vue 3, TypeScript, Vue Router, Element Plus, Vitest, Vite, existing Standard* shared components

---

## File Structure

### New files to create

- `spring-boot-iot-ui/src/utils/iotAccessDiagnostics.ts`
- `spring-boot-iot-ui/src/__tests__/utils/iotAccessDiagnostics.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts`

### Existing files to modify

- `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
- `spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue`
- `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `docs/02-业务功能与流程说明.md`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Implementation Notes

- `DiagnosticContext` 只承接高价值排查字段：`sourcePage`、`deviceCode`、`traceId`、`productKey`、`topic`、`sessionId`、`transportMode`、`reportStatus`、`capturedAt`。
- query 只放高价值且短字段：`deviceCode`、`traceId`、`productKey`、`topic`；`sourcePage`、`sessionId`、`transportMode`、`reportStatus` 进入 `sessionStorage`。
- `sessionStorage` 必须带 TTL，避免旧排查上下文长期污染治理页入口。
- `DiagnosticFinding` 只做规则化判断，不做自由文本总结；页面层自己产出规则，结构由共享工具统一。
- 不新增新的“大诊断卡”；页级结论继续落在 command strip meta / inline-state，结果级结论落在主战区标题附近，对象级结论落在行操作或详情抽屉。
- 治理回跳阶段只做“带问题进入治理”，不在本计划内实现治理评分、批量修复或待办队列。
- 当前工作区存在其他未提交改动，每个提交都必须显式 `git add` 本任务文件，禁止 `git add .`。

### Task 1: 建立共享 `DiagnosticContext` / `DiagnosticFinding` 工具层

**Files:**
- Create: `spring-boot-iot-ui/src/utils/iotAccessDiagnostics.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/iotAccessDiagnostics.test.ts`

- [ ] **Step 1: 先写共享上下文工具的失败测试**

在 `spring-boot-iot-ui/src/__tests__/utils/iotAccessDiagnostics.test.ts` 新增：

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  loadDiagnosticContext,
  persistDiagnosticContext,
  resolveDiagnosticContext
} from '@/utils/iotAccessDiagnostics';

function installSessionStorageMock() {
  const store = new Map<string, string>();
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, value: string) => {
        store.set(key, value);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

describe('iotAccessDiagnostics', () => {
  beforeEach(() => {
    installSessionStorageMock();
  });

  it('keeps only query-safe keys in route params', () => {
    expect(
      buildDiagnosticRouteQuery({
        sourcePage: 'reporting',
        deviceCode: 'demo-device-01',
        traceId: 'trace-001',
        productKey: 'demo-product',
        topic: '/sys/demo-product/demo-device-01/thing/property/post',
        sessionId: 'session-001',
        reportStatus: 'sent'
      })
    ).toEqual({
      deviceCode: 'demo-device-01',
      traceId: 'trace-001',
      productKey: 'demo-product',
      topic: '/sys/demo-product/demo-device-01/thing/property/post'
    });
  });

  it('persists auxiliary fields in sessionStorage and restores them with route priority', () => {
    persistDiagnosticContext({
      sourcePage: 'reporting',
      deviceCode: 'demo-device-01',
      traceId: 'trace-001',
      productKey: 'demo-product',
      sessionId: 'session-001',
      transportMode: 'mqtt',
      reportStatus: 'pending',
      capturedAt: '2026-03-28T10:00:00.000Z'
    });

    expect(loadDiagnosticContext()?.sessionId).toBe('session-001');

    const restored = resolveDiagnosticContext({
      traceId: 'trace-002',
      productKey: 'demo-product'
    });

    expect(restored).toMatchObject({
      sourcePage: 'reporting',
      deviceCode: 'demo-device-01',
      traceId: 'trace-002',
      productKey: 'demo-product',
      sessionId: 'session-001',
      transportMode: 'mqtt',
      reportStatus: 'pending'
    });
  });

  it('returns readable source labels for compact strip copy', () => {
    expect(describeDiagnosticSource('reporting')).toBe('链路验证中心');
    expect(describeDiagnosticSource('access-error')).toBe('失败归档');
  });
});
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/utils/iotAccessDiagnostics.test.ts --run
```

Expected:

- 测试失败，因为 `src/utils/iotAccessDiagnostics.ts` 还不存在。

- [ ] **Step 3: 写最小共享实现**

创建 `spring-boot-iot-ui/src/utils/iotAccessDiagnostics.ts`：

```ts
import type { LocationQuery } from 'vue-router';

export type DiagnosticSourcePage =
  | 'reporting'
  | 'message-trace'
  | 'system-log'
  | 'file-debug'
  | 'access-error'
  | 'products'
  | 'devices';

export type DiagnosticReportStatus =
  | 'ready'
  | 'sent'
  | 'pending'
  | 'timeline-missing'
  | 'validated'
  | 'failed';

export interface DiagnosticContext {
  sourcePage: DiagnosticSourcePage;
  deviceCode?: string | null;
  traceId?: string | null;
  productKey?: string | null;
  topic?: string | null;
  sessionId?: string | null;
  transportMode?: 'http' | 'mqtt' | null;
  reportStatus?: DiagnosticReportStatus | null;
  capturedAt: string;
}

export interface DiagnosticFinding {
  kind: 'identity' | 'contract' | 'correlation' | 'timeline' | 'runtime' | 'validation';
  level: 'info' | 'success' | 'warning' | 'danger';
  title: string;
  summary: string;
  reason: string;
  nextActionLabel?: string;
  nextActionTarget?: string;
}

const diagnosticContextStorageKey = 'iot-access:diagnostic-context';
const diagnosticContextTtlMs = 30 * 60 * 1000;

function normalizeText(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function getSessionStorage() {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.sessionStorage;
}

export function buildDiagnosticRouteQuery(context?: Partial<DiagnosticContext> | null) {
  return {
    deviceCode: normalizeText(context?.deviceCode),
    traceId: normalizeText(context?.traceId),
    productKey: normalizeText(context?.productKey),
    topic: normalizeText(context?.topic)
  };
}

export function persistDiagnosticContext(context?: Partial<DiagnosticContext> | null) {
  const sourcePage = context?.sourcePage;
  if (!sourcePage) {
    return;
  }
  const storage = getSessionStorage();
  if (!storage) {
    return;
  }
  storage.setItem(
    diagnosticContextStorageKey,
    JSON.stringify({
      sourcePage,
      deviceCode: normalizeText(context.deviceCode) || null,
      traceId: normalizeText(context.traceId) || null,
      productKey: normalizeText(context.productKey) || null,
      topic: normalizeText(context.topic) || null,
      sessionId: normalizeText(context.sessionId) || null,
      transportMode: normalizeText(context.transportMode) || null,
      reportStatus: normalizeText(context.reportStatus) || null,
      capturedAt: normalizeText(context.capturedAt) || new Date().toISOString()
    })
  );
}

export function loadDiagnosticContext(): DiagnosticContext | null {
  const storage = getSessionStorage();
  if (!storage) {
    return null;
  }
  const raw = storage.getItem(diagnosticContextStorageKey);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<DiagnosticContext>;
    if (!parsed.sourcePage) {
      return null;
    }
    const capturedAt = normalizeText(parsed.capturedAt);
    const capturedMs = capturedAt ? Date.parse(capturedAt) : Number.NaN;
    if (!Number.isFinite(capturedMs) || Date.now() - capturedMs > diagnosticContextTtlMs) {
      storage.removeItem(diagnosticContextStorageKey);
      return null;
    }
    return {
      sourcePage: parsed.sourcePage,
      deviceCode: normalizeText(parsed.deviceCode) || null,
      traceId: normalizeText(parsed.traceId) || null,
      productKey: normalizeText(parsed.productKey) || null,
      topic: normalizeText(parsed.topic) || null,
      sessionId: normalizeText(parsed.sessionId) || null,
      transportMode: normalizeText(parsed.transportMode) as 'http' | 'mqtt' | undefined,
      reportStatus: normalizeText(parsed.reportStatus) as DiagnosticReportStatus | undefined,
      capturedAt
    };
  } catch {
    storage.removeItem(diagnosticContextStorageKey);
    return null;
  }
}

export function resolveDiagnosticContext(query: LocationQuery): DiagnosticContext | null {
  const stored = loadDiagnosticContext();
  if (!stored) {
    return null;
  }
  return {
    ...stored,
    deviceCode: normalizeText(query.deviceCode) || stored.deviceCode || null,
    traceId: normalizeText(query.traceId) || stored.traceId || null,
    productKey: normalizeText(query.productKey) || stored.productKey || null,
    topic: normalizeText(query.topic) || stored.topic || null
  };
}

export function describeDiagnosticSource(sourcePage?: DiagnosticSourcePage | null) {
  switch (sourcePage) {
    case 'reporting':
      return '链路验证中心';
    case 'message-trace':
      return '链路追踪台';
    case 'system-log':
      return '异常观测台';
    case 'file-debug':
      return '数据校验台';
    case 'access-error':
      return '失败归档';
    case 'products':
      return '产品定义中心';
    case 'devices':
      return '设备资产中心';
    default:
      return '';
  }
}
```

- [ ] **Step 4: 运行工具测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/utils/iotAccessDiagnostics.test.ts --run
```

Expected:

- `src/__tests__/utils/iotAccessDiagnostics.test.ts` 全部通过。

- [ ] **Step 5: 提交共享工具底座**

```bash
git add spring-boot-iot-ui/src/utils/iotAccessDiagnostics.ts spring-boot-iot-ui/src/__tests__/utils/iotAccessDiagnostics.test.ts
git commit -m "feat: add iot access diagnostic context helpers"
```

### Task 2: 让 `链路验证中心` 在发送后写入上下文并给出下一步动作

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`

- [ ] **Step 1: 先补失败测试，锁定发送后持久化与继续排查动作**

在 `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts` 追加：

```ts
function installSessionStorageMock() {
  const store = new Map<string, string>();
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, value: string) => {
        store.set(key, value);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

it('persists diagnostic context after successful send and forwards it to follow-up actions', async () => {
  installSessionStorageMock();
  vi.mocked(getDeviceByCode).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      id: 1,
      deviceCode: 'demo-device-01',
      deviceName: '演示设备',
      productKey: 'demo-product',
      protocolCode: 'mqtt-json'
    }
  });
  vi.mocked(reportByHttp).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      sessionId: 'session-001',
      traceId: 'trace-001',
      timelineAvailable: true,
      correlationPending: false
    }
  });
  vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      sessionId: 'session-001',
      traceId: 'trace-001',
      transportMode: 'HTTP',
      correlationPending: false,
      timeline: {
        traceId: 'trace-001',
        steps: []
      }
    }
  });

  const wrapper = mountView();
  await queryDevice(wrapper);
  await wrapper.find('#payload').setValue('{"temp":22}');
  await wrapper.find('form').trigger('submit.prevent');
  await flushPromises();
  await nextTick();

  expect(window.sessionStorage.setItem).toHaveBeenCalledWith(
    'iot-access:diagnostic-context',
    expect.stringContaining('"sourcePage":"reporting"')
  );
  expect(wrapper.text()).toContain('已拿到 trace，可进入链路追踪');

  await findButtonByText(wrapper, '继续链路追踪')!.trigger('click');

  expect(mockRouter.push).toHaveBeenLastCalledWith({
    path: '/message-trace',
    query: {
      deviceCode: 'demo-device-01',
      traceId: 'trace-001',
      productKey: 'demo-product',
      topic: '/sys/demo-product/demo-device-01/thing/property/post'
    }
  });
});
```

- [ ] **Step 2: 运行测试确认新断言失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts --run
```

Expected:

- 测试失败，因为页面还没有写入 `DiagnosticContext`，也没有 `继续链路追踪` 按钮与对应跳转 query。

- [ ] **Step 3: 在 `ReportWorkbenchView.vue` 接入上下文持久化与建议动作**

在 `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue` 中补以下实现：

```ts
import {
  buildDiagnosticRouteQuery,
  persistDiagnosticContext,
  type DiagnosticContext,
  type DiagnosticFinding
} from '@/utils/iotAccessDiagnostics';

const reportingFinding = computed<DiagnosticFinding>(() => {
  if (!resolvedDevice.value) {
    return {
      kind: 'identity',
      level: 'info',
      title: '设备身份未校准',
      summary: '先查询设备，再执行模拟上报。',
      reason: '当前产品 Key、协议编码和 clientId 仍未锁定。'
    };
  }
  if (messageFlowTraceId.value) {
    return {
      kind: 'correlation',
      level: 'success',
      title: '已拿到 trace，可进入链路追踪',
      summary: '当前发送已完成链路关联，建议继续查看时间线。',
      reason: `TraceId ${messageFlowTraceId.value} 已可用于跨页追踪。`,
      nextActionLabel: '继续链路追踪',
      nextActionTarget: '/message-trace'
    };
  }
  if (messageFlowSession.value?.correlationPending) {
    return {
      kind: 'correlation',
      level: 'warning',
      title: 'MQTT 已发布，等待消费回流',
      summary: 'session 已存在但 trace 仍未绑定，可先看异常观测或等待回流。',
      reason: `Session ${messageFlowSessionId.value || '--'} 已建立但时间线仍未完成。`,
      nextActionLabel: '查看异常观测',
      nextActionTarget: '/system-log'
    };
  }
  return {
    kind: 'identity',
    level: 'info',
    title: '设备身份已就绪，可直接发送',
    summary: '产品契约和协议编码已锁定，可以继续模拟上报。',
    reason: `设备 ${normalizedText(reportForm.deviceCode) || '--'} 已完成查询。`,
    nextActionLabel: '打开数据校验',
    nextActionTarget: '/file-debug'
  };
});

const currentDiagnosticContext = computed<DiagnosticContext | null>(() => {
  if (!resolvedDevice.value) {
    return null;
  }
  return {
    sourcePage: 'reporting',
    deviceCode: normalizedText(reportForm.deviceCode) || null,
    traceId: messageFlowTraceId.value || null,
    productKey: resolvedProductKey.value || null,
    topic: normalizedText(reportForm.topic) || null,
    sessionId: messageFlowSessionId.value || null,
    transportMode: messageFlowSubmittedTransportMode.value || transportMode.value,
    reportStatus: messageFlowTraceId.value
      ? 'sent'
      : messageFlowSession.value?.correlationPending
        ? 'pending'
        : sendStatusText.value === '可发送'
          ? 'ready'
          : 'failed',
    capturedAt: new Date().toISOString()
  };
});

function persistCurrentDiagnosticContext(overrides: Partial<DiagnosticContext> = {}) {
  if (!currentDiagnosticContext.value) {
    return;
  }
  persistDiagnosticContext({
    ...currentDiagnosticContext.value,
    ...overrides
  });
}

function jumpToMessageTraceWithContext() {
  if (!currentDiagnosticContext.value) {
    return;
  }
  persistCurrentDiagnosticContext();
  router.push({
    path: '/message-trace',
    query: buildDiagnosticRouteQuery(currentDiagnosticContext.value)
  });
}

function jumpToSystemLogWithContext() {
  if (!currentDiagnosticContext.value) {
    return;
  }
  persistCurrentDiagnosticContext();
  router.push({
    path: '/system-log',
    query: {
      ...buildDiagnosticRouteQuery(currentDiagnosticContext.value),
      requestUrl: currentDiagnosticContext.value.topic || undefined,
      requestMethod: currentDiagnosticContext.value.topic ? 'MQTT' : undefined
    }
  });
}

function jumpToFileDebugWithContext() {
  if (!currentDiagnosticContext.value?.deviceCode) {
    return;
  }
  persistCurrentDiagnosticContext();
  router.push({
    path: '/file-debug',
    query: {
      deviceCode: currentDiagnosticContext.value.deviceCode,
      traceId: currentDiagnosticContext.value.traceId || undefined,
      productKey: currentDiagnosticContext.value.productKey || undefined
    }
  });
}
```

并把 command strip 改为只展示强相关动作：

```vue
<p class="reporting-command-strip__meta">{{ reportingFinding.summary }}</p>
<div class="reporting-command-strip__actions">
  <StandardButton
    v-if="reportingFinding.nextActionTarget === '/message-trace'"
    action="refresh"
    plain
    @click="jumpToMessageTraceWithContext"
  >
    继续链路追踪
  </StandardButton>
  <StandardButton
    v-if="currentDiagnosticContext?.deviceCode"
    action="reset"
    plain
    @click="jumpToSystemLogWithContext"
  >
    查看异常观测
  </StandardButton>
  <StandardButton
    v-if="currentDiagnosticContext?.deviceCode"
    action="refresh"
    plain
    @click="jumpToFileDebugWithContext"
  >
    打开数据校验
  </StandardButton>
</div>
```

并在发送成功和恢复最近 session 后补持久化：

```ts
if (submitSessionId) {
  messageFlowSessionId.value = submitSessionId;
  await loadMessageFlowSession(submitSessionId, transportMode.value === 'mqtt');
}
persistCurrentDiagnosticContext({
  sessionId: submitSessionId || null,
  traceId: normalizedText(response.data?.traceId) || messageFlowTraceId.value || null,
  reportStatus: transportMode.value === 'mqtt' && !normalizedText(response.data?.traceId) ? 'pending' : 'sent'
});
```

```ts
async function restoreRecentSession(session: MessageFlowRecentSession) {
  ...
  await loadMessageFlowSession(...);
  persistCurrentDiagnosticContext({
    sessionId,
    traceId: normalizedText(session.traceId) || messageFlowTraceId.value || null,
    topic: normalizedText(session.topic) || normalizedText(reportForm.topic) || null,
    transportMode: messageFlowSubmittedTransportMode.value || transportMode.value,
    reportStatus: normalizedText(session.traceId) ? 'sent' : 'pending'
  });
}
```

- [ ] **Step 4: 运行视图测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts --run
```

Expected:

- `ReportWorkbenchView` 现有测试与新增“持久化上下文 + 下一步动作”测试均通过。

- [ ] **Step 5: 提交链路验证中心联动能力**

```bash
git add spring-boot-iot-ui/src/views/ReportWorkbenchView.vue spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts
git commit -m "feat: connect report workbench diagnostic flow"
```

### Task 3: 让 `链路追踪台` 恢复上下文并输出规则化判断

**Files:**
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: 先写链路追踪恢复与跳转的失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts` 追加：

```ts
function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}));
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, next: string) => {
        store.set(key, next);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

it('restores diagnostic context from session storage when route query is incomplete', async () => {
  installSessionStorageMock({
    'iot-access:diagnostic-context': JSON.stringify({
      sourcePage: 'reporting',
      deviceCode: 'demo-device-01',
      traceId: 'trace-001',
      productKey: 'demo-product',
      topic: '/sys/demo-product/demo-device-01/thing/property/post',
      capturedAt: new Date().toISOString()
    })
  });
  mockRoute.query = { traceId: 'trace-001' };
  vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue(createPageResponse());
  vi.mocked(messageApi.pageMessageTraceStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      recentHourCount: 1,
      recent24HourCount: 1,
      dispatchFailureCount: 0
    }
  });
  vi.mocked(messageApi.getMessageFlowOpsOverview).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      runtimeStartedAt: '2026-03-28T10:00:00',
      sessionCounts: [],
      correlationCounts: [],
      lookupCounts: [],
      stageMetrics: []
    }
  });
  vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  });

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('来自链路验证中心');
  expect(wrapper.text()).toContain('当前 Trace 可继续查 system_error');
});

it('jumps to file-debug with the active trace context', async () => {
  installSessionStorageMock();
  mockRoute.query = {
    traceId: 'trace-001',
    deviceCode: 'demo-device-01',
    productKey: 'demo-product',
    topic: '/sys/demo-product/demo-device-01/thing/property/post'
  };
  vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue(createPageResponse());
  vi.mocked(messageApi.pageMessageTraceStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      recentHourCount: 1,
      recent24HourCount: 1,
      dispatchFailureCount: 0
    }
  });
  vi.mocked(messageApi.getMessageFlowOpsOverview).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      runtimeStartedAt: '2026-03-28T10:00:00',
      sessionCounts: [],
      correlationCounts: [],
      lookupCounts: [],
      stageMetrics: []
    }
  });
  vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  });

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  await wrapper.findAll('button').find((item) => item.text().includes('数据校验台'))!.trigger('click');

  expect(mockRouter.push).toHaveBeenLastCalledWith({
    path: '/file-debug',
    query: {
      deviceCode: 'demo-device-01',
      traceId: 'trace-001',
      productKey: 'demo-product'
    }
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts --run
```

Expected:

- 新测试失败，因为页面还没有从 `sessionStorage` 恢复来源上下文，也没有 `数据校验台` 跳转动作。

- [ ] **Step 3: 在 `MessageTraceView.vue` 接入上下文恢复、页级判断与下一步动作**

在 `spring-boot-iot-ui/src/views/MessageTraceView.vue` 中补以下逻辑：

```ts
import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticFinding
} from '@/utils/iotAccessDiagnostics';

const restoredDiagnosticContext = ref(resolveDiagnosticContext(route.query));

const traceFinding = computed<DiagnosticFinding>(() => {
  if (appliedFilters.traceId && pagination.total > 0) {
    return {
      kind: 'timeline',
      level: 'success',
      title: '当前 Trace 可继续查 system_error',
      summary: '时间线已命中，可转异常观测台继续排查。',
      reason: `TraceId ${appliedFilters.traceId} 已命中追踪列表。`,
      nextActionLabel: '异常观测台',
      nextActionTarget: '/system-log'
    };
  }
  if (!appliedFilters.traceId && recentMessageFlowSessions.value.length > 0) {
    return {
      kind: 'correlation',
      level: 'warning',
      title: 'Trace 缺失，优先恢复最近会话',
      summary: '最近 message-flow 仍活跃，可直接恢复上下文。',
      reason: '当前没有有效 TraceId，但最近会话可用。'
    };
  }
  if (timelineExpired.value) {
    return {
      kind: 'timeline',
      level: 'warning',
      title: '时间线已过期',
      summary: '仅保留消息日志，建议转异常观测或失败归档。',
      reason: 'detail trace timeline 查询为空。',
      nextActionLabel: '失败归档',
      nextActionTarget: '/message-trace?mode=access-error'
    };
  }
  return {
    kind: 'runtime',
    level: 'info',
    title: '先看 TraceId，再看最近会话',
    summary: '当前结果可继续联动 system_error、失败归档和数据校验。',
    reason: '追踪台继续作为链路排查主战区。'
  };
});

function applyRouteQuery() {
  const context = resolveDiagnosticContext(route.query);
  restoredDiagnosticContext.value = context;
  searchForm.deviceCode = readQueryValue('deviceCode') || context?.deviceCode || '';
  searchForm.productKey = readQueryValue('productKey') || context?.productKey || '';
  searchForm.traceId = readQueryValue('traceId') || context?.traceId || '';
  searchForm.messageType = readQueryValue('messageType');
  searchForm.topic = readQueryValue('topic') || context?.topic || '';
  syncQuickSearchKeywordFromFilters();
  syncAdvancedFilterState();
}

const traceStripStatus = computed(() => {
  const sourceLabel = describeDiagnosticSource(restoredDiagnosticContext.value?.sourcePage);
  return [sourceLabel ? `来自${sourceLabel}` : '', traceFinding.value.summary].filter(Boolean).join(' · ');
});

function persistTraceContext(source?: Partial<DeviceMessageLog>) {
  persistDiagnosticContext({
    sourcePage: 'message-trace',
    deviceCode: source?.deviceCode || appliedFilters.deviceCode || restoredDiagnosticContext.value?.deviceCode || null,
    traceId: source?.traceId || appliedFilters.traceId || restoredDiagnosticContext.value?.traceId || null,
    productKey: source?.productKey || appliedFilters.productKey || restoredDiagnosticContext.value?.productKey || null,
    topic: source?.topic || appliedFilters.topic || restoredDiagnosticContext.value?.topic || null,
    reportStatus: source?.traceId || appliedFilters.traceId ? 'sent' : 'timeline-missing',
    capturedAt: new Date().toISOString()
  });
}

function jumpToAccessError(row?: DeviceMessageLog) {
  const source = row || {
    traceId: appliedFilters.traceId,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    topic: appliedFilters.topic
  };
  persistTraceContext(source);
  router.push({
    path: '/message-trace',
    query: {
      mode: 'access-error',
      ...buildDiagnosticRouteQuery(source)
    }
  });
}

function jumpToFileDebug(row?: DeviceMessageLog) {
  const source = row || {
    traceId: appliedFilters.traceId,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    topic: appliedFilters.topic
  };
  if (!source.deviceCode) {
    return;
  }
  persistTraceContext(source);
  router.push({
    path: '/file-debug',
    query: {
      deviceCode: source.deviceCode || undefined,
      traceId: source.traceId || undefined,
      productKey: source.productKey || undefined
    }
  });
}
```

并把顶部和详情动作收敛为强相关入口：

```vue
<p class="message-trace-command-strip__meta">{{ traceStripStatus }}</p>
<div class="message-trace-command-strip__actions">
  <StandardButton action="refresh" plain @click="jumpToSystemLog()">异常观测台</StandardButton>
  <StandardButton action="reset" plain :disabled="!canJumpWithRow({ deviceCode: appliedFilters.deviceCode, traceId: appliedFilters.traceId, productKey: appliedFilters.productKey, topic: appliedFilters.topic })" @click="jumpToAccessError()">
    失败归档
  </StandardButton>
  <StandardButton action="refresh" plain :disabled="!appliedFilters.deviceCode" @click="jumpToFileDebug()">
    数据校验台
  </StandardButton>
</div>
```

```vue
<div class="detail-notice">
  <span class="detail-notice__label">排查建议</span>
  <strong class="detail-notice__value">{{ detailRouteAdvice }}</strong>
  <div class="detail-notice__actions">
    <StandardButton action="refresh" link :disabled="!canJumpWithRow(detailData)" @click="jumpToSystemLog(detailData)">异常观测台</StandardButton>
    <StandardButton action="reset" link :disabled="!canJumpWithRow(detailData)" @click="jumpToAccessError(detailData)">失败归档</StandardButton>
    <StandardButton action="refresh" link :disabled="!detailData.deviceCode" @click="jumpToFileDebug(detailData)">数据校验台</StandardButton>
  </div>
</div>
```

同时在恢复最近会话时更新共享上下文：

```ts
function applyRecentMessageFlowSession(session: MessageFlowRecentSession) {
  ...
  persistDiagnosticContext({
    sourcePage: 'message-trace',
    deviceCode: session.deviceCode || null,
    traceId: session.traceId || null,
    productKey: restoredDiagnosticContext.value?.productKey || null,
    topic: session.topic || null,
    sessionId: session.sessionId || null,
    reportStatus: session.traceId ? 'sent' : 'pending',
    capturedAt: new Date().toISOString()
  });
  triggerSearch(true);
}
```

- [ ] **Step 4: 运行链路追踪测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts --run
```

Expected:

- `MessageTraceView` 新旧测试全部通过。

- [ ] **Step 5: 提交链路追踪联动增强**

```bash
git add spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "feat: restore trace workbench diagnostic context"
```

### Task 4: 让 `异常观测台` 恢复上下文并在详情中保留回跳语义

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts`

- [ ] **Step 1: 先写失败测试，锁定 system-log 恢复与详情回跳**

在 `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts` 追加：

```ts
function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}));
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, next: string) => {
        store.set(key, next);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

it('restores system-log filters from persisted diagnostic context', async () => {
  installSessionStorageMock({
    'iot-access:diagnostic-context': JSON.stringify({
      sourcePage: 'message-trace',
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      topic: '$dp',
      capturedAt: new Date().toISOString()
    })
  });
  mockRoute.path = '/system-log';
  mockRoute.query = { traceId: 'trace-001' };
  vi.mocked(pageLogs).mockResolvedValue(createPageResponse());
  vi.mocked(getSystemErrorStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      todayCount: 1,
      mqttCount: 1,
      systemCount: 0,
      distinctTraceCount: 1,
      distinctDeviceCount: 1,
      topModules: [],
      topExceptionClasses: [],
      topErrorCodes: []
    }
  });

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('来自链路追踪台');
  expect(wrapper.text()).toContain('当前异常 1 条');
});
```

创建 `spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts`：

```ts
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue';

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<section v-if="modelValue"><slot /></section>'
});

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
});

describe('AuditLogDetailDrawer', () => {
  it('renders reverse diagnostic actions in system-error mode', async () => {
    const wrapper = mount(AuditLogDetailDrawer, {
      props: {
        modelValue: true,
        title: '异常详情',
        detail: {
          operationType: 'system_error',
          operationResult: 0,
          traceId: 'trace-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product'
        },
        showTraceAction: true,
        showAccessErrorAction: true
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub,
          StandardButton: StandardButtonStub
        }
      }
    });

    expect(wrapper.text()).toContain('返回链路追踪');
    expect(wrapper.text()).toContain('回看失败归档');
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts src/__tests__/components/AuditLogDetailDrawer.test.ts --run
```

Expected:

- `AuditLogView` 新断言失败，因为 `system-log` 尚未读取共享上下文来源。
- `AuditLogDetailDrawer` 测试失败，因为抽屉还没有回跳动作属性。

- [ ] **Step 3: 在 `AuditLogView.vue` / `AuditLogDetailDrawer.vue` 接入恢复与回跳**

在 `spring-boot-iot-ui/src/views/AuditLogView.vue` 中补：

```ts
import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticFinding
} from '@/utils/iotAccessDiagnostics';

const restoredDiagnosticContext = computed(() => (isSystemMode.value ? resolveDiagnosticContext(route.query) : null));

const systemFinding = computed<DiagnosticFinding>(() => {
  if (systemStats.value.total > 0) {
    return {
      kind: 'runtime',
      level: 'warning',
      title: '已命中 system_error',
      summary: `当前异常 ${systemStats.value.total} 条，可回链路追踪继续复盘。`,
      reason: `当前 Trace 覆盖 ${systemStats.value.distinctTraceCount} 条链路。`
    };
  }
  return {
    kind: 'runtime',
    level: 'info',
    title: '当前未命中异常',
    summary: '建议回到链路追踪或失败归档继续排查。',
    reason: 'system_error 列表当前为空。'
  };
});

const systemStripStatus = computed(() => {
  const sourceLabel = describeDiagnosticSource(restoredDiagnosticContext.value?.sourcePage);
  return [sourceLabel ? `来自${sourceLabel}` : '', systemFinding.value.summary].filter(Boolean).join(' · ');
});

const applySystemRouteQuery = () => {
  if (!isSystemMode.value) {
    return;
  }
  const context = resolveDiagnosticContext(route.query);
  searchForm.traceId = readRouteQueryValue('traceId') || context?.traceId || '';
  searchForm.deviceCode = readRouteQueryValue('deviceCode') || context?.deviceCode || '';
  searchForm.productKey = readRouteQueryValue('productKey') || context?.productKey || '';
  searchForm.requestMethod = readRouteQueryValue('requestMethod') || (context?.topic ? 'MQTT' : '');
  searchForm.requestUrl = readRouteQueryValue('requestUrl') || context?.topic || '';
  ...
};

function persistSystemContext(source?: Partial<AuditLogRecord>) {
  persistDiagnosticContext({
    sourcePage: 'system-log',
    traceId: source?.traceId || quickSearchKeyword.value.trim() || searchForm.traceId || restoredDiagnosticContext.value?.traceId || null,
    deviceCode: source?.deviceCode || searchForm.deviceCode || restoredDiagnosticContext.value?.deviceCode || null,
    productKey: source?.productKey || searchForm.productKey || restoredDiagnosticContext.value?.productKey || null,
    topic: ('requestUrl' in (source || {}) ? source?.requestUrl : searchForm.requestUrl) || restoredDiagnosticContext.value?.topic || null,
    reportStatus: systemStats.value.total > 0 ? 'failed' : 'timeline-missing',
    capturedAt: new Date().toISOString()
  });
}
```

并在跳转前写入共享上下文：

```ts
const handleJumpToMessageTrace = (row?: AuditLogRecord) => {
  const target = row || { ... };
  persistSystemContext(target);
  router.push({
    path: '/message-trace',
    query: {
      ...buildDiagnosticRouteQuery({
        deviceCode: target.deviceCode,
        traceId: target.traceId,
        productKey: target.productKey,
        topic: requestMethod === 'MQTT' ? requestUrl || undefined : undefined
      })
    }
  });
};

const handleJumpToAccessError = (row?: AuditLogRecord) => {
  const target = row || { ... };
  persistSystemContext(target);
  router.push({
    path: '/message-trace',
    query: {
      mode: 'access-error',
      ...buildDiagnosticRouteQuery({
        deviceCode: target.deviceCode,
        traceId: target.traceId,
        productKey: target.productKey,
        topic: requestMethod === 'MQTT' ? requestUrl || undefined : undefined
      }),
      errorCode: 'errorCode' in target ? target.errorCode || undefined : undefined,
      exceptionClass: 'exceptionClass' in target ? target.exceptionClass || undefined : undefined
    }
  });
};
```

在 `spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue` 中新增紧凑动作区：

```vue
<div v-if="showTraceAction || showAccessErrorAction || showProductAction || showDeviceAction" class="detail-notice__actions">
  <StandardButton v-if="showTraceAction" action="refresh" link @click="emit('jump-message-trace')">返回链路追踪</StandardButton>
  <StandardButton v-if="showAccessErrorAction" action="reset" link @click="emit('jump-access-error')">回看失败归档</StandardButton>
  <StandardButton v-if="showProductAction" action="refresh" link @click="emit('jump-product-governance')">产品定义中心</StandardButton>
  <StandardButton v-if="showDeviceAction" action="refresh" link @click="emit('jump-device-governance')">设备资产中心</StandardButton>
</div>
```

并补上 props / emits：

```ts
const props = defineProps<{
  modelValue: boolean
  title: string
  detail: Partial<AuditLogRecord>
  loading?: boolean
  errorMessage?: string
  showTraceAction?: boolean
  showAccessErrorAction?: boolean
  showProductAction?: boolean
  showDeviceAction?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'jump-message-trace'): void
  (event: 'jump-access-error'): void
  (event: 'jump-product-governance'): void
  (event: 'jump-device-governance'): void
}>()
```

最后在 `AuditLogView.vue` 给抽屉传值：

```vue
<AuditLogDetailDrawer
  v-model="detailVisible"
  :title="detailDialogTitle"
  :detail="detailData"
  :loading="detailLoading"
  :error-message="detailErrorMessage"
  :show-trace-action="isSystemMode && canJumpToMessageTrace(detailData)"
  :show-access-error-action="isSystemMode && canJumpToMessageTrace(detailData)"
  @jump-message-trace="handleJumpToMessageTrace(detailData)"
  @jump-access-error="handleJumpToAccessError(detailData)"
/>
```

- [ ] **Step 4: 运行 system-log 与详情抽屉测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts src/__tests__/components/AuditLogDetailDrawer.test.ts --run
```

Expected:

- `AuditLogView` 和 `AuditLogDetailDrawer` 新旧测试全部通过。

- [ ] **Step 5: 提交异常观测台恢复与回跳**

```bash
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/components/AuditLogDetailDrawer.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts spring-boot-iot-ui/src/__tests__/components/AuditLogDetailDrawer.test.ts
git commit -m "feat: add system log diagnostic handoff"
```

### Task 5: 让 `数据校验台` 与 `失败归档` 共享排查上下文并预留治理回跳

**Files:**
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- Modify: `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts`

- [ ] **Step 1: 先写失败测试，锁定 file-debug 恢复与失败归档治理回跳**

在 `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts` 追加：

```ts
const { mockRoute } = vi.hoisted(() => ({
  mockRoute: {
    query: {} as Record<string, unknown>
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}));
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, next: string) => {
        store.set(key, next);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

it('restores deviceCode from persisted diagnostic context', () => {
  installSessionStorageMock({
    'iot-access:diagnostic-context': JSON.stringify({
      sourcePage: 'message-trace',
      deviceCode: 'demo-device-02',
      traceId: 'trace-001',
      productKey: 'demo-product',
      capturedAt: new Date().toISOString()
    })
  });
  mockRoute.query = { traceId: 'trace-001' };

  const wrapper = mount(FilePayloadDebugView, {
    global: {
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        StandardInlineState: true,
        StandardInfoGrid: true,
        PanelCard: PanelCardStub,
        EmptyState: true,
        ResponsePanel: ResponsePanelStub,
        StandardButton: true,
        ElInput: true
      }
    }
  });

  expect(wrapper.text()).toContain('来自链路追踪台');
  expect(wrapper.text()).toContain('demo-device-02');
});
```

创建 `spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts`：

```ts
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AccessErrorArchivePanel from '@/components/AccessErrorArchivePanel.vue';
import { accessErrorApi } from '@/api/accessError';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {
      mode: 'access-error',
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product'
    } as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/accessError', () => ({
  accessErrorApi: {
    pageAccessErrors: vi.fn(),
    getAccessErrorStats: vi.fn(),
    getAccessErrorById: vi.fn()
  }
}));

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  template: '<section><slot name="filters" /><slot name="toolbar" /><slot /><slot name="pagination" /></section>'
});

describe('AccessErrorArchivePanel', () => {
  beforeEach(() => {
    mockRouter.push.mockReset();
    vi.mocked(accessErrorApi.pageAccessErrors).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            traceId: 'trace-001',
            deviceCode: 'demo-device-01',
            productKey: 'demo-product',
            topic: '$dp',
            errorMessage: 'contract mismatch',
            createTime: '2026-03-28 10:00:00'
          }
        ]
      }
    });
    vi.mocked(accessErrorApi.getAccessErrorStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        recentHourCount: 1,
        recent24HourCount: 1,
        distinctTraceCount: 1,
        distinctDeviceCount: 1,
        topFailureStages: [],
        topErrorCodes: [],
        topExceptionClasses: [],
        topProtocolCodes: [],
        topTopics: []
      }
    });
    vi.mocked(accessErrorApi.getAccessErrorById).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        traceId: 'trace-001',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        contractSnapshot: '{"expected":"demo-product"}'
      }
    });
  });

  it('offers governance jump actions from failure detail', async () => {
    const wrapper = mount(AccessErrorArchivePanel, {
      props: {
        viewMode: 'access-error',
        viewModeOptions: [
          { label: '链路追踪', value: 'message-trace' },
          { label: '失败归档', value: 'access-error' }
        ]
      },
      global: {
        stubs: {
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardAppliedFiltersBar: true,
          StandardTableToolbar: true,
          StandardPagination: true,
          StandardTableTextColumn: true,
          StandardRowActions: true,
          StandardActionLink: true,
          StandardDetailDrawer: defineComponent({
            name: 'StandardDetailDrawer',
            props: ['modelValue'],
            template: '<section v-if="modelValue"><slot /></section>'
          }),
          StandardChoiceGroup: true,
          StandardButton: defineComponent({
            name: 'StandardButton',
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          }),
          ElTable: true,
          ElTableColumn: true,
          ElInput: true,
          ElFormItem: true,
          ElTag: true,
          ElAlert: true
        }
      }
    });

    await (wrapper.vm as any).handleDetail({ id: 1, traceId: 'trace-001', deviceCode: 'demo-device-01', productKey: 'demo-product' });
    await (wrapper.vm as any).jumpToProductGovernance();

    expect(mockRouter.push).toHaveBeenLastCalledWith({
      path: '/products',
      query: {
        productKey: 'demo-product',
        traceId: 'trace-001'
      }
    });
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts --run
```

Expected:

- `FilePayloadDebugView` 新断言失败，因为当前页未读上下文来源。
- `AccessErrorArchivePanel` 测试失败，因为详情里还没有治理回跳函数。

- [ ] **Step 3: 在 `FilePayloadDebugView.vue` / `AccessErrorArchivePanel.vue` 接入恢复、结论和治理跳转**

在 `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue` 中补：

```ts
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticFinding
} from '@/utils/iotAccessDiagnostics';

const route = useRoute();
const restoredDiagnosticContext = computed(() => resolveDiagnosticContext(route.query));
const defaultDeviceCode = restoredDiagnosticContext.value?.deviceCode || 'demo-device-01';

const validationFinding = computed<DiagnosticFinding>(() => {
  if (!lastFetchTime.value) {
    return {
      kind: 'validation',
      level: 'info',
      title: '等待刷新校验结果',
      summary: '先确定设备，再核对文件快照和固件聚合。',
      reason: '当前还没有抓取结果。'
    };
  }
  if (fileSnapshots.value.length > 0 && firmwareAggregates.value.length === 0) {
    return {
      kind: 'validation',
      level: 'warning',
      title: '当前设备有快照，无聚合',
      summary: '优先核对 C.4 是否完整写入。',
      reason: `设备 ${normalizedDeviceCode.value} 已命中文件快照但固件聚合为空。`
    };
  }
  if (fileSnapshots.value.length === 0 && firmwareAggregates.value.length === 0) {
    return {
      kind: 'validation',
      level: 'warning',
      title: '当前设备无快照无聚合',
      summary: '建议回链路追踪确认是否收到文件类消息，再看异常观测。',
      reason: `设备 ${normalizedDeviceCode.value} 本次校验未命中任何文件数据。`
    };
  }
  return {
    kind: 'validation',
    level: 'success',
    title: '当前设备校验结果完整',
    summary: '文件快照和固件聚合已可联合核对。',
    reason: `设备 ${normalizedDeviceCode.value} 已返回 ${fileSnapshots.value.length} 条快照与 ${firmwareAggregates.value.length} 条聚合。`
  };
});

const validationStripStatus = computed(() => {
  const sourceLabel = describeDiagnosticSource(restoredDiagnosticContext.value?.sourcePage);
  return [sourceLabel ? `来自${sourceLabel}` : '', validationFinding.value.summary].filter(Boolean).join(' · ');
});
```

并在刷新成功/失败后持久化：

```ts
persistDiagnosticContext({
  sourcePage: 'file-debug',
  deviceCode: normalizedDeviceCode.value,
  traceId: restoredDiagnosticContext.value?.traceId || null,
  productKey: restoredDiagnosticContext.value?.productKey || null,
  topic: restoredDiagnosticContext.value?.topic || null,
  reportStatus: snapshotResponse.data.length || firmwareResponse.data.length ? 'validated' : 'timeline-missing',
  capturedAt: new Date().toISOString()
});
```

在 `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue` 中新增上下文恢复与治理回跳：

```ts
import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext
} from '@/utils/iotAccessDiagnostics';

const restoredDiagnosticContext = ref(resolveDiagnosticContext(route.query));

function applyRouteQuery() {
  const context = resolveDiagnosticContext(route.query);
  restoredDiagnosticContext.value = context;
  searchForm.traceId = readQueryValue('traceId') || context?.traceId || '';
  searchForm.deviceCode = readQueryValue('deviceCode') || context?.deviceCode || '';
  searchForm.productKey = readQueryValue('productKey') || context?.productKey || '';
  searchForm.topic = readQueryValue('topic') || context?.topic || '';
  ...
}

function persistAccessErrorContext(source?: Partial<DeviceAccessErrorLog>) {
  persistDiagnosticContext({
    sourcePage: 'access-error',
    traceId: source?.traceId || appliedFilters.traceId || restoredDiagnosticContext.value?.traceId || null,
    deviceCode: source?.deviceCode || appliedFilters.deviceCode || restoredDiagnosticContext.value?.deviceCode || null,
    productKey: source?.productKey || appliedFilters.productKey || restoredDiagnosticContext.value?.productKey || null,
    topic: source?.topic || appliedFilters.topic || restoredDiagnosticContext.value?.topic || null,
    reportStatus: 'failed',
    capturedAt: new Date().toISOString()
  });
}

function jumpToProductGovernance() {
  if (!detailData.value.productKey) {
    return;
  }
  persistAccessErrorContext(detailData.value);
  router.push({
    path: '/products',
    query: {
      productKey: detailData.value.productKey,
      traceId: detailData.value.traceId || undefined
    }
  });
}

function jumpToDeviceGovernance() {
  if (!detailData.value.deviceCode) {
    return;
  }
  persistAccessErrorContext(detailData.value);
  router.push({
    path: '/devices',
    query: {
      deviceCode: detailData.value.deviceCode,
      productKey: detailData.value.productKey || undefined,
      traceId: detailData.value.traceId || undefined
    }
  });
}
```

并在契约快照区下方加紧凑动作：

```vue
<div class="detail-notice__actions">
  <StandardButton action="refresh" link :disabled="!detailData.productKey" @click="jumpToProductGovernance">产品定义中心</StandardButton>
  <StandardButton action="refresh" link :disabled="!detailData.deviceCode" @click="jumpToDeviceGovernance">设备资产中心</StandardButton>
</div>
```

- [ ] **Step 4: 运行 file-debug 与失败归档测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts --run
```

Expected:

- `FilePayloadDebugView` 和 `AccessErrorArchivePanel` 新旧测试全部通过。

- [ ] **Step 5: 提交数据校验与失败归档联动**

```bash
git add spring-boot-iot-ui/src/views/FilePayloadDebugView.vue spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts
git commit -m "feat: wire validation and access error diagnostic handoff"
```

### Task 6: 让治理页接收“带问题进入治理”的来源语义

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 先写治理页接收来源的失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 追加：

```ts
it('shows a compact diagnostic intake hint when opened from system-log', async () => {
  mockRoute.query = {
    productKey: 'demo-product',
    traceId: 'trace-001'
  };
  window.sessionStorage.setItem(
    'iot-access:diagnostic-context',
    JSON.stringify({
      sourcePage: 'system-log',
      productKey: 'demo-product',
      traceId: 'trace-001',
      capturedAt: new Date().toISOString()
    })
  );

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('来自异常观测台');
  expect(wrapper.text()).toContain('Trace trace-001');
});
```

在 `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts` 追加：

```ts
it('shows a compact diagnostic intake hint when opened from access-error', async () => {
  mockRoute.query = {
    deviceCode: 'demo-device-01',
    productKey: 'demo-product',
    traceId: 'trace-001'
  };
  window.sessionStorage.setItem(
    'iot-access:diagnostic-context',
    JSON.stringify({
      sourcePage: 'access-error',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      traceId: 'trace-001',
      capturedAt: new Date().toISOString()
    })
  );

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('来自失败归档');
  expect(wrapper.text()).toContain('demo-device-01');
});
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected:

- 新测试失败，因为产品页和设备页还没有接收共享排查来源提示。

- [ ] **Step 3: 在治理页用现有 inline-state 接收来源上下文**

在 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 中补：

```ts
import { describeDiagnosticSource, resolveDiagnosticContext } from '@/utils/iotAccessDiagnostics'

const diagnosticContext = computed(() => resolveDiagnosticContext(route.query))
const diagnosticEntryMessage = computed(() => {
  if (!diagnosticContext.value) {
    return ''
  }
  const sourceLabel = describeDiagnosticSource(diagnosticContext.value.sourcePage)
  const traceLabel = diagnosticContext.value.traceId ? `Trace ${diagnosticContext.value.traceId}` : ''
  return [sourceLabel ? `来自${sourceLabel}` : '', traceLabel, '优先核对产品契约、协议编码与物模型完整性。']
    .filter(Boolean)
    .join(' · ')
})
const workbenchInlineMessage = computed(() => listRefreshMessage.value || diagnosticEntryMessage.value)
const workbenchInlineTone = computed(() => (listRefreshState.value === 'error' ? 'error' : 'info'))
const showListInlineState = computed(() => Boolean(workbenchInlineMessage.value) && (hasRecords.value || Boolean(diagnosticEntryMessage.value)))
```

并把模板里的 inline-state 改为：

```vue
<StandardInlineState
  :message="workbenchInlineMessage"
  :tone="workbenchInlineTone"
/>
```

在 `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` 中做同构接入：

```ts
import { describeDiagnosticSource, resolveDiagnosticContext } from '@/utils/iotAccessDiagnostics'

const diagnosticContext = computed(() => resolveDiagnosticContext(route.query))
const diagnosticEntryMessage = computed(() => {
  if (!diagnosticContext.value) {
    return ''
  }
  const sourceLabel = describeDiagnosticSource(diagnosticContext.value.sourcePage)
  const traceLabel = diagnosticContext.value.traceId ? `Trace ${diagnosticContext.value.traceId}` : ''
  const deviceLabel = diagnosticContext.value.deviceCode ? `设备 ${diagnosticContext.value.deviceCode}` : ''
  return [sourceLabel ? `来自${sourceLabel}` : '', traceLabel, deviceLabel, '优先核对登记状态、在线态与失败来源。']
    .filter(Boolean)
    .join(' · ')
})
const workbenchInlineMessage = computed(() => listRefreshMessage.value || diagnosticEntryMessage.value)
const workbenchInlineTone = computed(() => (listRefreshState.value === 'error' ? 'error' : 'info'))
const showListInlineState = computed(() => Boolean(workbenchInlineMessage.value) && (hasRecords.value || Boolean(diagnosticEntryMessage.value)))
```

- [ ] **Step 4: 运行治理页测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected:

- 产品页、设备页现有测试与新增“带问题进入治理”提示测试均通过。

- [ ] **Step 5: 提交治理页接收来源能力**

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: surface diagnostic intake hints in governance pages"
```

### Task 7: 同步文档并完成前端验证闭环

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Review-only: `README.md`
- Review-only: `AGENTS.md`

- [ ] **Step 1: 原位更新业务 / 前端文档**

在 `docs/02-业务功能与流程说明.md` 追加或改写以下内容：

```md
- `链路验证中心` 在发送成功后会写入统一 `DiagnosticContext`，跳转 `链路追踪台`、`异常观测台`、`数据校验台` 时默认带入 `deviceCode / traceId / productKey / topic`。
- `链路追踪台`、`异常观测台`、`失败归档` 会恢复最近排查上下文，并在顶部状态条显示来源与下一步建议。
- `失败归档` 详情支持回到 `产品定义中心`、`设备资产中心`，实现“带问题进入治理”。
```

在 `docs/06-前端开发与CSS规范.md` 追加：

```md
- 接入智维诊断页的页级结论继续放在 command strip / inline-state，不得新增厚重诊断说明墙。
- 跨页联动统一复用 `DiagnosticContext`，高价值字段走 route query，辅助字段走 `sessionStorage`，避免在单页内再造私有缓存协议。
```

在 `docs/08-变更记录与技术债清单.md` 追加：

```md
- 2026-03-28：补齐 `接入智维` 第一阶段进阶能力，新增 `DiagnosticContext`、规则化 `DiagnosticFinding` 与治理回跳入口。
```

在 `docs/15-前端优化与治理计划.md` 追加：

```md
- 接入智维后续增强继续沿用“主战区优先 + 状态条判断 + 强相关动作”语法，跨页联动不能退化成新的入口墙。
- 治理页接收诊断来源时，优先复用 `StandardInlineState`，不要再在产品页、设备页顶部堆叠新的说明卡。
```

- [ ] **Step 2: 复核 `README.md` 与 `AGENTS.md` 是否需要同步**

执行复核动作并记录结论：

```md
- 若 `README.md`、`AGENTS.md` 现有描述已足以覆盖“接入智维跨页联动为前端工作台行为增强”，则保持不改，并在最终说明里写明“已复核，无需更新”。
- 只有当两份文档明确声明了旧的交互行为或与本轮联动方式冲突时，才原位补一句说明，不创建平行文档。
```

- [ ] **Step 3: 运行定向测试、构建与前端门禁**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/utils/iotAccessDiagnostics.test.ts src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/components/AuditLogDetailDrawer.test.ts src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts --run
npm run build
npm run component:guard
npm run list:guard
npm run style:guard
cd ..
node scripts/run-quality-gates.mjs
```

Expected:

- 定向 Vitest 全绿。
- `npm run build` 通过。
- `component:guard`、`list:guard`、`style:guard` 通过。
- `node scripts/run-quality-gates.mjs` 通过。

- [ ] **Step 4: 提交文档与验证收尾**

```bash
git add docs/02-业务功能与流程说明.md docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: record iot access diagnostic flow enhancement"
```

## Coverage Check

- `DiagnosticContext`、`DiagnosticFinding`、query + sessionStorage 恢复：Task 1
- `/reporting` 写入上下文 + 下一步建议：Task 2
- `/message-trace` 来源恢复 + 失败归档 / 数据校验联动：Task 3
- `/system-log` 来源恢复 + 反向回跳：Task 4
- `/file-debug` 来源恢复 + 规则化校验结论：Task 5
- `失败归档` 治理回跳预留：Task 5
- `产品定义中心` / `设备资产中心` 接收来源上下文：Task 6
- 文档同步、README/AGENTS 复核、验证命令：Task 7
