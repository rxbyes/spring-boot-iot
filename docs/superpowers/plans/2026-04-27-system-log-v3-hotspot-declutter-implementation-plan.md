# System Log V3 Hotspot Declutter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `/system-log` 的 `观测热点` 页签 into a single-master-object workbench with one hotspot table and one segmented drilldown zone instead of parallel detail blocks.

**Architecture:** Keep the existing observability APIs and selection model in `AuditLogView.vue`, but simplify the panel contract so the hotspot component renders one master table, one focus strip, and one drilldown area controlled by a segmented mode. Scheduled tasks stay available as “相关任务”, but move inside the drilldown zone instead of living as a separate surface.

**Tech Stack:** Vue 3 `script setup`, TypeScript, Element Plus loading directives, shared `StandardButton` / `StandardChoiceGroup`, Vitest + Vue Test Utils, Vite build.

---

### Task 1: Refactor the hotspot panel into master table + segmented drilldown

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`

- [ ] **Step 1: Write the failing component test**

Add a test that locks the new interaction shape: the hotspot panel should render one drilldown mode switch, default to `最近样本`, and keep `调度任务` hidden until the user selects the related-task segment.

```ts
const StandardChoiceGroupStub = defineComponent({
  name: 'StandardChoiceGroup',
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: `
    <div data-testid="hotspot-drilldown-switch">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        :data-testid="\`hotspot-drilldown-\${option.value}\`"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `
});

it('defaults to recent samples and only shows scheduled tasks inside the related-task segment', async () => {
  const wrapper = mount(AuditLogHotspotTabPanel, {
    props: {
      ...hotspotPropsFactory(),
      hotspotDrilldownView: 'samples',
      hotspotDrilldownOptions: [
        { label: '最近样本', value: 'samples' },
        { label: '趋势', value: 'trends' },
        { label: '相关任务', value: 'tasks' }
      ]
    },
    global: {
      stubs: {
        StandardButton: StandardButtonStub,
        StandardChoiceGroup: StandardChoiceGroupStub
      }
    }
  });

  expect(wrapper.find('[data-testid="hotspot-drilldown-switch"]').exists()).toBe(true);
  expect(wrapper.text()).toContain('最近样本');
  expect(wrapper.text()).not.toContain('调度任务台账');

  await wrapper.get('[data-testid="hotspot-drilldown-tasks"]').trigger('click');

  expect(wrapper.emitted('change-hotspot-drilldown-view')?.[0]).toEqual(['tasks']);
});
```

- [ ] **Step 2: Run the focused component test and verify it fails**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
```

Expected: FAIL because `hotspotDrilldownView`, `hotspotDrilldownOptions`, and `change-hotspot-drilldown-view` do not exist yet, and the old template still renders `调度任务台账` as a standalone section.

- [ ] **Step 3: Implement the hotspot panel refactor**

Update `AuditLogHotspotTabPanel.vue` so the drilldown content is controlled by a segmented mode and the scheduled-task ledger moves under the same drilldown surface.

```vue
<section
  v-if="activeHotspot"
  data-testid="hotspot-detail-section"
  class="audit-log-hotspot-detail standard-list-surface"
>
  <header class="audit-log-hotspot-detail__header">
    <div>
      <h3>热点下钻</h3>
      <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
    </div>
    <StandardChoiceGroup
      data-testid="hotspot-drilldown-switch"
      :model-value="hotspotDrilldownView"
      :options="hotspotDrilldownOptions"
      @update:model-value="emit('change-hotspot-drilldown-view', $event)"
    />
  </header>

  <section v-if="hotspotDrilldownView === 'samples'" class="audit-log-slow-span-drilldown">
    <header class="audit-log-slow-span-drilldown__header">
      <div>
        <h3>最近样本</h3>
        <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
      </div>
      <span>{{ slowSpanTotal }} 条</span>
    </header>
  </section>

  <section v-else-if="hotspotDrilldownView === 'trends'" class="audit-log-slow-trend-drilldown">
    <header class="audit-log-slow-trend-drilldown__header">
      <div>
        <h3>趋势</h3>
        <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
      </div>
      <span>{{ slowTrendRows.length }} 桶</span>
    </header>
  </section>

  <section v-else class="audit-log-scheduled-task-ledger audit-log-scheduled-task-ledger--embedded">
    <header class="audit-log-scheduled-task-ledger__header">
      <div>
        <h3>调度任务台账</h3>
      </div>
      <span>{{ scheduledTaskRows.length }} / {{ scheduledTaskTotal }}</span>
    </header>
  </section>
</section>
```

Also update the component contract:

```ts
type HotspotDrilldownView = 'samples' | 'trends' | 'tasks';
type HotspotDrilldownOption = { label: string; value: HotspotDrilldownView };

const props = defineProps<{
  hotspotDrilldownView: HotspotDrilldownView;
  hotspotDrilldownOptions: HotspotDrilldownOption[];
  slowSummaryLoading: boolean;
  slowSummaryRows: SlowSummaryRow[];
  slowSpanRows: ObservabilitySpan[];
  slowTrendRows: ObservabilitySlowSpanTrend[];
  scheduledTaskRows: ObservabilityScheduledTask[];
}>();

const emit = defineEmits<{
  (event: 'change-hotspot-drilldown-view', view: HotspotDrilldownView): void;
  (event: 'select-slow-summary', row: SlowSummaryRow): void;
  (event: 'open-trace-evidence', traceId: string): void;
  (event: 'change-slow-trend-window', value: SlowTrendWindowKey): void;
}>();
```

Keep the focus strip and master table intact, but delete the old standalone scheduled-task `<section>` that sat outside the drilldown surface.

- [ ] **Step 4: Run the focused component test again**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
```

Expected: PASS. The panel should now emit the drilldown-view event and no longer show `调度任务台账` by default.

- [ ] **Step 5: Commit the panel-only refactor**

```powershell
git add spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
git commit -m "refactor: declutter system log hotspot panel"
```

### Task 2: Wire drilldown state and hotspot interactions in the system-log view

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Write the failing view test**

Add a view-level test that proves the tab now uses one drilldown surface: after entering `观测热点`, the panel should default to `最近样本`, and switching to `相关任务` should reveal the scheduled ledger without leaving the hotspot workbench.

```ts
it('uses one hotspot drilldown surface and reveals scheduled tasks only in the related-task view', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();
  await triggerSystemLogTab(wrapper, 'hotspots');

  expect(wrapper.find('[data-testid="hotspot-detail-section"]').text()).toContain('最近样本');
  expect(wrapper.text()).not.toContain('调度任务台账');

  await wrapper.get('[data-testid="hotspot-drilldown-tasks"]').trigger('click');
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('调度任务台账');
  expect(wrapper.find('[data-testid="hotspot-master-table"]').exists()).toBe(true);
});
```

- [ ] **Step 2: Run the focused view test and verify it fails**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts -t "uses one hotspot drilldown surface and reveals scheduled tasks only in the related-task view"
```

Expected: FAIL because `AuditLogView.vue` does not provide drilldown-view props or handlers yet, and the old layout still renders the scheduled-task section outside the drilldown area.

- [ ] **Step 3: Implement view-state wiring and refresh behavior**

In `AuditLogView.vue`, introduce explicit drilldown-view state, pass it into the hotspot panel, and keep refresh/selection behavior aligned with the new surface.

```ts
type HotspotDrilldownView = 'samples' | 'trends' | 'tasks';

const hotspotDrilldownView = ref<HotspotDrilldownView>('samples');

const hotspotDrilldownOptions = [
  { label: '最近样本', value: 'samples' as const },
  { label: '趋势', value: 'trends' as const },
  { label: '相关任务', value: 'tasks' as const }
];

const handleHotspotDrilldownViewChange = (view: HotspotDrilldownView) => {
  hotspotDrilldownView.value = view;
};
```

Pass the new contract into the panel:

```vue
<AuditLogHotspotTabPanel
  :hotspot-drilldown-view="hotspotDrilldownView"
  :hotspot-drilldown-options="hotspotDrilldownOptions"
  @change-hotspot-drilldown-view="handleHotspotDrilldownViewChange"
  @select-slow-summary="handleSlowSummarySelect"
  @open-trace-evidence="openTraceEvidenceByTraceId"
  @open-slow-span-detail="loadSlowSpanDrilldown"
  @open-slow-trend="loadSlowTrendDrilldown"
  @change-slow-trend-window="handleSlowTrendWindowChange"
/>
```

Keep existing data loading behavior for `slowSummary`, `slowSpan`, `slowTrend`, and `scheduledTask` intact so the UI declutter does not alter API truth or page-summary counts.

- [ ] **Step 4: Run the hotspot-related view tests**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts -t "system-log|hotspot"
```

Expected: PASS for the new drilldown-surface test and the existing hotspot regressions covering selected-hotspot persistence, evidence opening, trend switching, and scheduled-task error fallback.

- [ ] **Step 5: Commit the view wiring**

```powershell
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: unify system log hotspot drilldown"
```

### Task 3: Polish hotspot wording, layout density, and end-to-end verification

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add the final regression assertions**

Extend the component and view tests so they verify the text contract that matters for the decluttered page:

```ts
expect(wrapper.text()).toContain('热点下钻');
expect(wrapper.text()).toContain('最近样本');
expect(wrapper.text()).toContain('趋势');
expect(wrapper.text()).toContain('相关任务');
expect(wrapper.text()).not.toContain('调度任务台账');
```

For the view test, add the final positive assertion after selecting `相关任务`:

```ts
expect(wrapper.find('[data-testid="hotspot-detail-section"]').text()).toContain('调度任务台账');
```

- [ ] **Step 2: Run the tests and confirm the wording expectations fail first if needed**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: If the wording or test ids still reflect the old structure, this run should expose the mismatches before the last styling pass.

- [ ] **Step 3: Finish the surface polish in the hotspot panel**

Adjust labels/classes so the hotspot page reads like one workbench instead of stacked modules.

```vue
<header class="audit-log-hotspot-detail__header">
  <div>
    <h3>热点下钻</h3>
    <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
  </div>
</header>
```

```css
.audit-log-hotspot-detail {
  display: grid;
  gap: 16px;
}

.audit-log-scheduled-task-ledger--embedded {
  border: 0;
  box-shadow: none;
  padding: 0;
}
```

Keep the master table visually primary; the drilldown area should feel secondary but still integrated.

- [ ] **Step 4: Run the final hotspot verification suite**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts
npm run build
```

Expected:
- Vitest PASS for hotspot component + view tests
- Vite build PASS with no new TypeScript/template errors

- [ ] **Step 5: Commit the final hotspot polish**

```powershell
git add spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "style: streamline system log hotspot workbench"
```
