import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import RiskInsightTrendPanel from '@/components/RiskInsightTrendPanel.vue';

vi.mock('echarts/core', () => ({
  use: vi.fn(),
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn()
  }))
}));

vi.mock('echarts/charts', () => ({
  LineChart: {}
}));

vi.mock('echarts/renderers', () => ({
  CanvasRenderer: {}
}));

vi.mock('echarts/components', () => ({
  GridComponent: {},
  LegendComponent: {},
  TooltipComponent: {}
}));

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="panel-card-stub">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
});

function mountTrend(
  detail: Record<string, unknown> | null,
  logs: Array<Record<string, unknown>> = [],
  objectType: 'detect' | 'warning' | 'collect' | 'generic' = 'generic'
) {
  return mount(RiskInsightTrendPanel, {
    props: {
      detail,
      logs,
      objectType
    },
    global: {
      stubs: {
        PanelCard: PanelCardStub
      }
    }
  });
}

describe('RiskInsightTrendPanel', () => {
  it('renders empty guidance when there are no trend points', () => {
    const wrapper = mountTrend({
      metricName: '预警灯状态',
      trendPoints: []
    }, [], 'warning');

    expect(wrapper.text()).toContain('属性趋势预览');
    expect(wrapper.text()).toContain('暂无趋势点');
  });

  it('renders summary labels for the selected monitoring object type', () => {
    const wrapper = mountTrend({
      metricName: '预警灯状态',
      metricIdentifier: 'warningLightState',
      latestReportTime: '2026-04-01 09:00:00',
      trendPoints: [
        { reportTime: '2026-04-01 08:00:00', numericValue: 0, value: '0' },
        { reportTime: '2026-04-01 09:00:00', numericValue: 1, value: '1' }
      ]
    }, [], 'warning');

    expect(wrapper.text()).toContain('预警型');
    expect(wrapper.text()).toContain('预警灯状态');
    expect(wrapper.text()).toContain('近 24h 点数');
  });

  it('falls back to device message trends when risk monitoring detail is missing', () => {
    const wrapper = mountTrend(
      null,
      [
        {
          reportTime: '2026-04-01 08:00:00',
          payload: '{"properties":{"temperature":23.5,"humidity":58}}'
        },
        {
          reportTime: '2026-04-01 09:00:00',
          payload: '{"properties":{"temperature":24.1,"humidity":61}}'
        }
      ]
    );

    expect(wrapper.text()).toContain('设备上报趋势');
    expect(wrapper.text()).toContain('temperature');
    expect(wrapper.find('.trend-chart').exists()).toBe(true);
  });
});
