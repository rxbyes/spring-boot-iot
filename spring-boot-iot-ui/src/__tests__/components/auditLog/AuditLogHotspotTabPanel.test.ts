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
  template: '<div class="standard-choice-group-stub">{{ modelValue }}</div>'
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
      }
    ],
    slowSummaryErrorMessage: '',
    formatSlowSummaryTitle: () => '设备接入查询',
    formatSlowSummaryTarget: () => 'GET /api/system/observability/spans/page',
    formatValue: (value: string | null | undefined) => value ?? '--',
    formatDuration: (value: number | null | undefined) => `${value ?? 0} ms`,
    formatCount: (value: number | null | undefined) => `${value ?? 0}`,
    activeSlowSummary: null,
    slowSpanLoading: false,
    slowSpanTotal: 0,
    slowSpanRows: [],
    slowSpanErrorMessage: '',
    activeSlowTrendSummary: null,
    slowTrendLoading: false,
    slowTrendRows: [],
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
});
