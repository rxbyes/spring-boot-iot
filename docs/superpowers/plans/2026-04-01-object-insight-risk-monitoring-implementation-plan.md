# Object Insight Risk-Monitoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/insight` 从纯设备洞察页改造成以 `risk-monitoring` 实时详情为主口径的风险运营对象洞察页，同时删除非核心内容并完成文档同步。

**Architecture:** 页面保持单一 `StandardPageShell + StandardWorkbenchPanel` 骨架，查询参数支持 `bindingId` 优先、`deviceCode` 回退。风险等级、趋势和监测对象上下文来自 `riskMonitoring`，设备详情/属性/日志继续作为补充数据；趋势图改用对象洞察专用组件，避免与旧的任意属性趋势抽象混用。

**Tech Stack:** Vue 3、TypeScript、Vitest、Element Plus、ECharts、项目现有共享工作台组件。

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
  - 重写数据加载流程，支持绑定解析、风险监测详情主口径和页面内容收口。
- Create: `spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue`
  - 展示风险监测趋势点、对象类型和核心摘要。
- Create: `spring-boot-iot-ui/src/utils/deviceInsight.ts`
  - 承担绑定选择、对象类型推断、研判依据和草稿生成等纯函数逻辑。
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
  - 覆盖新数据流、旧内容移除和多绑定切换表现。
- Create: `spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts`
  - 覆盖绑定选择和对象类型推断。
- Create: `spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`
  - 覆盖趋势卡的单点/无点/多点表现。
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

### Task 1: Lock The New Contract With Failing Tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts`

- [ ] **Step 1: Write the failing utility tests**

```ts
import { describe, expect, it } from 'vitest';
import { pickPrimaryBinding, resolveInsightObjectType } from '@/utils/deviceInsight';

describe('deviceInsight utils', () => {
  it('prefers latest report time when selecting a primary binding', () => {
    const selected = pickPrimaryBinding([
      { bindingId: 1, latestReportTime: '2026-04-01 09:00:00', onlineStatus: 0, riskLevel: 'INFO' },
      { bindingId: 2, latestReportTime: '2026-04-01 10:00:00', onlineStatus: 1, riskLevel: 'WARNING' }
    ]);

    expect(selected?.bindingId).toBe(2);
  });

  it('classifies warning devices by metric or product keywords', () => {
    expect(resolveInsightObjectType({
      metricIdentifier: 'warningLightState',
      metricName: '预警灯状态',
      productName: '边坡预警终端'
    })).toBe('warning');
  });
});
```

- [ ] **Step 2: Run utility test to verify it fails**

Run: `npm run test -- src/__tests__/utils/deviceInsight.test.ts`
Expected: FAIL with module or export not found for `@/utils/deviceInsight`.

- [ ] **Step 3: Write the failing view tests**

```ts
it('renders risk-monitoring-first insight content and removes legacy action cards', async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain('当前风险等级');
  expect(wrapper.text()).toContain('属性趋势预览');
  expect(wrapper.text()).toContain('风险分析草稿');
  expect(wrapper.text()).not.toContain('当前建议动作');
  expect(wrapper.text()).not.toContain('一线建议');
  expect(wrapper.text()).not.toContain('运维建议');
  expect(wrapper.text()).not.toContain('研发建议');
});

it('shows binding switcher when one device hits multiple monitoring bindings', async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain('监测对象切换');
});
```

- [ ] **Step 4: Run view test to verify it fails**

Run: `npm run test -- src/__tests__/views/DeviceInsightView.test.ts`
Expected: FAIL because the current page still renders legacy sections and has no binding switcher.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts
git commit -m "test: lock object insight risk-monitoring contract"
```

### Task 2: Build Pure Insight Logic Helpers

**Files:**
- Create: `spring-boot-iot-ui/src/utils/deviceInsight.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts`

- [ ] **Step 1: Write the minimal helper implementation**

```ts
import type { RiskMonitoringDetail, RiskMonitoringListItem } from '@/api/riskMonitoring';
import type { DeviceMessageLog, DeviceProperty } from '@/types/api';

export type InsightObjectType = 'detect' | 'warning' | 'collect' | 'generic';

export function pickPrimaryBinding(items: RiskMonitoringListItem[]): RiskMonitoringListItem | null {
  return [...items].sort((left, right) => {
    const leftTime = left.latestReportTime ? new Date(left.latestReportTime).getTime() : 0;
    const rightTime = right.latestReportTime ? new Date(right.latestReportTime).getTime() : 0;
    if (leftTime !== rightTime) return rightTime - leftTime;
    if ((left.onlineStatus ?? 0) !== (right.onlineStatus ?? 0)) return (right.onlineStatus ?? 0) - (left.onlineStatus ?? 0);
    return riskLevelWeight(right.riskLevel) - riskLevelWeight(left.riskLevel);
  })[0] ?? null;
}

export function resolveInsightObjectType(source: Partial<RiskMonitoringDetail>): InsightObjectType {
  const keywordSource = `${source.metricIdentifier || ''} ${source.metricName || ''} ${source.productName || ''} ${source.riskPointName || ''}`.toLowerCase();
  if (/(warning|warn|预警|告警|广播|闪灯)/.test(keywordSource)) return 'warning';
  if (/(rain|water|采集|雨量|水位|采样)/.test(keywordSource)) return 'collect';
  if (/(gnss|angle|displacement|detect|检测|位移|倾角)/.test(keywordSource)) return 'detect';
  return 'generic';
}
```

- [ ] **Step 2: Run utility tests to verify they pass**

Run: `npm run test -- src/__tests__/utils/deviceInsight.test.ts`
Expected: PASS

- [ ] **Step 3: Extend helpers for page composition**

```ts
export function buildInsightReasons(detail: RiskMonitoringDetail | null, properties: DeviceProperty[], logs: DeviceMessageLog[]) {
  const reasons = [];
  if (!detail) return reasons;
  if ((detail.monitorStatus || '').toUpperCase() === 'ALARM') {
    reasons.push({ title: '当前监测对象处于告警中', tag: '监测状态', description: '建议优先核查最近告警与事件闭环。' });
  }
  if (detail.onlineStatus !== 1) {
    reasons.push({ title: '设备当前离线', tag: '在线状态', description: '当前设备离线，监测数据连续性存在风险。' });
  }
  if (!properties.length) {
    reasons.push({ title: '缺少属性快照', tag: '数据完整性', description: '当前缺少可直接核查的最新属性快照。' });
  }
  if (!logs.length) {
    reasons.push({ title: '缺少消息日志', tag: '审计链路', description: '当前缺少用于复盘链路的消息日志。' });
  }
  return reasons.slice(0, 6);
}
```

- [ ] **Step 4: Re-run utility tests**

Run: `npm run test -- src/__tests__/utils/deviceInsight.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/utils/deviceInsight.ts spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts
git commit -m "feat: add object insight risk-monitoring helpers"
```

### Task 3: Replace The Page Data Flow And Layout

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: Add failing view behavior for binding resolution**

```ts
vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 2,
      pageNum: 1,
      pageSize: 10,
      records: [
        { bindingId: 11, deviceCode: 'demo-device-01', riskLevel: 'INFO', onlineStatus: 1, latestReportTime: '2026-04-01 08:00:00', metricIdentifier: 'rainfall' },
        { bindingId: 22, deviceCode: 'demo-device-01', riskLevel: 'WARNING', onlineStatus: 1, latestReportTime: '2026-04-01 09:00:00', metricIdentifier: 'warningLightState' }
      ]
    }
  }),
  getRiskMonitoringDetail: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { bindingId: 22, riskLevel: 'WARNING', riskPointName: '北坡预警点', metricIdentifier: 'warningLightState', metricName: '预警灯状态', trendPoints: [] }
  })
}));
```

- [ ] **Step 2: Run the view test to verify the new binding-driven expectation fails**

Run: `npm run test -- src/__tests__/views/DeviceInsightView.test.ts`
Expected: FAIL because `DeviceInsightView` does not call `riskMonitoring` APIs yet.

- [ ] **Step 3: Implement the minimal page data flow**

```ts
const bindingId = computed(() => parseNumberQuery(route.query.bindingId));
const bindingOptions = ref<RiskMonitoringListItem[]>([]);
const selectedBindingId = ref<number | null>(bindingId.value);
const riskDetail = ref<RiskMonitoringDetail | null>(null);

async function resolveBindingAndLoad() {
  if (bindingId.value) {
    selectedBindingId.value = bindingId.value;
  } else {
    const listResponse = await getRiskMonitoringList({ deviceCode: normalizedDeviceCode.value, pageNum: 1, pageSize: 20 });
    bindingOptions.value = listResponse.data.records;
    selectedBindingId.value = pickPrimaryBinding(bindingOptions.value)?.bindingId ? Number(pickPrimaryBinding(bindingOptions.value)?.bindingId) : null;
  }

  if (!selectedBindingId.value) {
    riskDetail.value = null;
    errorMessage.value = '当前设备未纳入风险监测绑定。';
    return;
  }

  const detailResponse = await getRiskMonitoringDetail(selectedBindingId.value);
  riskDetail.value = detailResponse.data;
}
```

- [ ] **Step 4: Replace the page content with the new retained sections**

```vue
<section class="quad-grid">
  <MetricCard v-for="metric in overviewMetrics" :key="metric.label" :label="metric.label" :value="metric.value" :hint="metric.hint" :badge="metric.badge" />
</section>

<section v-if="bindingOptions.length > 1" class="insight-binding-switcher">
  <span>监测对象切换</span>
  <el-segmented v-model="selectedBindingId" :options="bindingSegmentOptions" />
</section>

<section class="two-column-grid">
  <PanelCard title="基础档案">...</PanelCard>
  <PanelCard title="研判依据">...</PanelCard>
</section>

<RiskInsightTrendPanel :detail="riskDetail" :object-type="insightObjectType" />

<section class="two-column-grid">
  <PanelCard title="关键监测指标">...</PanelCard>
  <PanelCard title="风险分析草稿">...</PanelCard>
</section>

<PanelCard title="设备属性快照">...</PanelCard>
```

- [ ] **Step 5: Run the view tests to verify they pass**

Run: `npm run test -- src/__tests__/views/DeviceInsightView.test.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts
git commit -m "feat: switch object insight to risk-monitoring detail"
```

### Task 4: Add The Dedicated Trend Panel

**Files:**
- Create: `spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: Write the failing component tests**

```ts
it('renders empty guidance when there are no trend points', () => {
  const wrapper = mountTrend({ trendPoints: [] });
  expect(wrapper.text()).toContain('暂无趋势点');
});

it('renders summary labels for the selected monitoring object type', () => {
  const wrapper = mountTrend({
    metricName: '预警灯状态',
    trendPoints: [
      { reportTime: '2026-04-01 08:00:00', numericValue: 0, value: '0' },
      { reportTime: '2026-04-01 09:00:00', numericValue: 1, value: '1' }
    ]
  }, 'warning');
  expect(wrapper.text()).toContain('预警型');
});
```

- [ ] **Step 2: Run the trend component test to verify it fails**

Run: `npm run test -- src/__tests__/components/RiskInsightTrendPanel.test.ts`
Expected: FAIL with missing component file.

- [ ] **Step 3: Implement the minimal dedicated trend panel**

```vue
<template>
  <PanelCard title="属性趋势预览" :description="panelDescription">
    <div v-if="points.length" class="trend-summary">
      <article class="trend-summary__item">
        <span>对象类型</span>
        <strong>{{ objectTypeLabel }}</strong>
      </article>
      <article class="trend-summary__item">
        <span>最新值</span>
        <strong>{{ latestValue }}</strong>
      </article>
      <article class="trend-summary__item">
        <span>最小值</span>
        <strong>{{ minValue }}</strong>
      </article>
      <article class="trend-summary__item">
        <span>最大值</span>
        <strong>{{ maxValue }}</strong>
      </article>
    </div>
    <div v-if="points.length > 1" ref="chartRef" class="trend-chart" />
    <div v-else class="empty-state">{{ points.length ? '趋势点不足 2 个，当前仅展示最近测点值。' : '暂无趋势点。' }}</div>
  </PanelCard>
</template>
```

- [ ] **Step 4: Run the component test to verify it passes**

Run: `npm run test -- src/__tests__/components/RiskInsightTrendPanel.test.ts`
Expected: PASS

- [ ] **Step 5: Re-run the page test to verify integration stays green**

Run: `npm run test -- src/__tests__/views/DeviceInsightView.test.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: add risk insight trend panel"
```

### Task 5: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update business and acceptance docs**

```md
- `对象洞察台` 当前已切换为“风险监测详情为主、设备接口为辅”的数据口径。
- `/insight` 支持 `bindingId` 优先、`deviceCode` 回退。
- 页面只保留核心指标、基础档案、研判依据、属性趋势预览、关键监测指标、设备属性快照与风险分析草稿。
```

- [ ] **Step 2: Update frontend governance docs**

```md
- `/insight` 的趋势卡已切换为风险监测专用趋势组件，不再复用旧的泛化属性趋势抽象。
- 对象洞察台删除了“当前建议动作 / 一线建议 / 运维建议 / 研发建议 / 消息日志与审计回看”独立大卡，避免风险运营页承载第二套角色动作中心。
```

- [ ] **Step 3: Run targeted tests**

Run: `npm run test -- src/__tests__/utils/deviceInsight.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts src/__tests__/views/DeviceInsightView.test.ts`
Expected: PASS

- [ ] **Step 4: Run build verification**

Run: `npm run build`
Expected: build completes successfully

- [ ] **Step 5: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: update object insight risk-monitoring contract"
```
