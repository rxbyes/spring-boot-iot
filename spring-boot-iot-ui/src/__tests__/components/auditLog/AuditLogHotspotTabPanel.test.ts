import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogHotspotTabPanel from '@/components/auditLog/AuditLogHotspotTabPanel.vue';

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
});

const StandardChoiceGroupStub = defineComponent({
  name: 'StandardChoiceGroup',
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: `
    <div class="standard-choice-group-stub">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        :data-testid="\`slow-trend-window-\${option.value}\`"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `
});

function hotspotPropsFactory() {
  return {
    slowSummaryLoading: false,
    slowSummaryRows: [
      {
        spanType: 'HTTP',
        domainCode: 'system',
        eventCode: 'query',
        objectType: 'API',
        objectId: 'GET:/api/system',
        latestStartedAt: '2026-04-26 10:00:00',
        latestTraceId: 'trace-hotspot-001',
        maxDurationMs: 1632,
        avgDurationMs: 982,
        totalCount: 5
      },
      {
        spanType: 'SLOW_SQL',
        domainCode: 'message',
        eventCode: 'archive',
        objectType: 'sql',
        objectId: 'iot_message_log',
        latestStartedAt: '2026-04-26 09:40:00',
        latestTraceId: 'trace-hotspot-002',
        maxDurationMs: 2400,
        avgDurationMs: 1560,
        totalCount: 9
      }
    ],
    slowSummaryErrorMessage: '',
    formatSlowSummaryTitle: () => '设备接入查询',
    formatSlowSummaryTarget: () => 'GET /api/system/observability/spans/page',
    formatValue: (value: string | null | undefined) => value ?? '--',
    formatDuration: (value: number | null | undefined) => `${value ?? 0} ms`,
    formatCount: (value: number | null | undefined) => `${value ?? 0}`,
    activeSlowSummary: {
      spanType: 'HTTP',
      domainCode: 'system',
      eventCode: 'query',
      objectType: 'API',
      objectId: 'GET:/api/system',
      latestStartedAt: '2026-04-26 10:00:00',
      latestTraceId: 'trace-hotspot-001',
      maxDurationMs: 1632,
      avgDurationMs: 982,
      totalCount: 5
    },
    selectedSlowSummaryKey: 'HTTP-system-query-API-GET:/api/system',
    slowSpanLoading: false,
    slowSpanTotal: 1,
    slowSpanRows: [
      {
        id: 11,
        traceId: 'trace-span-001',
        spanName: 'queryObservability',
        spanType: 'HTTP',
        status: 'SUCCESS',
        startedAt: '2026-04-26 10:01:00',
        durationMs: 1200
      }
    ],
    slowSpanErrorMessage: '',
    activeSlowTrendSummary: {
      spanType: 'HTTP',
      domainCode: 'system',
      eventCode: 'query',
      objectType: 'API',
      objectId: 'GET:/api/system',
      latestStartedAt: '2026-04-26 10:00:00',
      latestTraceId: 'trace-hotspot-001',
      maxDurationMs: 1632,
      avgDurationMs: 982,
      totalCount: 5
    },
    slowTrendLoading: false,
    slowTrendRows: [
      {
        bucket: '2026-04-26 10',
        bucketStart: '2026-04-26 10:00:00',
        bucketEnd: '2026-04-26 10:59:59',
        totalCount: 5,
        p95DurationMs: 1400,
        p99DurationMs: 1500,
        avgDurationMs: 980,
        maxDurationMs: 1632,
        errorRate: 5
      }
    ],
    slowTrendErrorMessage: '',
    slowTrendWindow: '24h',
    slowTrendWindowOptions: [
      { label: '最近 24 小时', value: '24h' },
      { label: '最近 7 天', value: '7d' }
    ],
    defaultSlowTrendWindow: '24h',
    formatSlowTrendBucketLabel: () => '04-26 10:00',
    formatPercentage: (value: number | null | undefined) => `${value ?? 0}%`,
    scheduledTaskLoading: false,
    scheduledTaskRows: [
      {
        id: 1,
        taskCode: 'archive-job',
        traceId: 'trace-task-001',
        triggerType: 'SCHEDULED',
        triggerExpression: '0 0 * * * ?',
        status: 'SUCCESS',
        startedAt: '2026-04-26 10:05:00',
        durationMs: 321,
        errorMessage: ''
      }
    ],
    scheduledTaskTotal: 1,
    scheduledTaskErrorMessage: '',
    formatScheduledTaskName: () => '日志归档巡检',
    formatScheduledTaskTrigger: () => 'cron: 0 0 * * * ?'
  };
}

describe('AuditLogHotspotTabPanel', () => {
  it('renders a hotspot focus strip and highlights the selected hotspot row', () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: hotspotPropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardChoiceGroup: StandardChoiceGroupStub
        }
      }
    });

    expect(wrapper.find('[data-testid="hotspot-focus-strip"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="hotspot-focus-strip"]').text()).toContain('trace-hotspot-001');
    expect(wrapper.find('[data-testid="hotspot-master-table"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="hotspot-master-row"]').classes()).toContain('is-selected');
  });

  it('emits select-slow-summary when choosing a hotspot row from the master table', async () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: hotspotPropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardChoiceGroup: StandardChoiceGroupStub
        }
      }
    });

    const rows = wrapper.findAll('[data-testid="hotspot-master-row"]');
    await rows[1]?.trigger('click');

    expect(wrapper.emitted('select-slow-summary')?.[0]?.[0]).toMatchObject({
      latestTraceId: 'trace-hotspot-002'
    });
  });

  it('shows slow-summary and scheduled-task sections, but not the system error list', () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: hotspotPropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardChoiceGroup: StandardChoiceGroupStub
        }
      }
    });

    expect(wrapper.find('[data-testid="system-log-hotspot-panel"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('性能慢点 Top');
    expect(wrapper.text()).toContain('调度任务台账');
    expect(wrapper.text()).not.toContain('异常摘要');
  });

  it('emits hotspot interactions for evidence, drilldown, trend, and trend window changes', async () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: hotspotPropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardChoiceGroup: StandardChoiceGroupStub
        }
      }
    });

    const summaryButtons = wrapper.find('[data-testid="hotspot-master-row"]').findAll('button');
    await summaryButtons[0]!.trigger('click');
    await summaryButtons[1]!.trigger('click');
    await summaryButtons[2]!.trigger('click');

    const spanEvidenceButton = wrapper.find('.audit-log-slow-span-drilldown__item').find('button');
    await spanEvidenceButton.trigger('click');

    const taskEvidenceButton = wrapper.find('.audit-log-scheduled-task-ledger__item').find('button');
    await taskEvidenceButton.trigger('click');
    await wrapper.get('[data-testid="slow-trend-window-7d"]').trigger('click');

    expect(wrapper.emitted('open-trace-evidence')?.[0]).toEqual(['trace-hotspot-001']);
    expect(wrapper.emitted('open-slow-span-detail')?.[0]?.[0]).toMatchObject({ latestTraceId: 'trace-hotspot-001' });
    expect(wrapper.emitted('open-slow-trend')?.[0]).toEqual([
      expect.objectContaining({ latestTraceId: 'trace-hotspot-001' }),
      '24h'
    ]);
    expect(wrapper.emitted('open-trace-evidence')?.[1]).toEqual(['trace-span-001']);
    expect(wrapper.emitted('open-trace-evidence')?.[2]).toEqual(['trace-task-001']);
    expect(wrapper.emitted('change-slow-trend-window')?.[0]).toEqual(['7d']);
  });

  it('renders the slow-summary error branch when summary loading succeeds with an error', () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: {
        ...hotspotPropsFactory(),
        slowSummaryRows: [],
        slowSummaryErrorMessage: '慢点汇总查询失败'
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardChoiceGroup: StandardChoiceGroupStub
        }
      }
    });

    expect(wrapper.text()).toContain('慢点汇总查询失败');
  });
});
