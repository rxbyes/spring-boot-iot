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

function mountTrend(groups: Array<Record<string, unknown>> = [], summary: Array<Record<string, unknown>> = []) {
  return mount(RiskInsightTrendPanel, {
    props: {
      rangeCode: '7d',
      groups,
      summary,
      emptyMessage: '请输入设备编码后开始综合分析'
    },
    global: {
      stubs: {
        PanelCard: PanelCardStub
      }
    }
  });
}

describe('RiskInsightTrendPanel', () => {
  it('renders empty guidance when there are no grouped trend series', () => {
    const wrapper = mountTrend();

    expect(wrapper.text()).toContain('属性趋势预览');
    expect(wrapper.text()).toContain('请输入设备编码后开始综合分析');
  });

  it('renders measure and status groups with chinese metric names only', () => {
    const wrapper = mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        series: [
          {
            identifier: 'L4_NW_1',
            displayName: '泥水位高程',
            buckets: [{ time: '2026-04-07 00:00:00', value: 2.1, filled: false }]
          }
        ]
      },
      {
        key: 'status',
        title: '状态数据',
        series: [
          {
            identifier: 'S1_ZT_1.sensor_state.L4_NW_1',
            displayName: '传感器在线状态',
            buckets: [{ time: '2026-04-07 00:00:00', value: 1, filled: true }]
          }
        ]
      }
    ], [
      { label: '默认范围', value: '近一周', hint: '最近 7 天' }
    ]);

    expect(wrapper.text()).toContain('监测数据');
    expect(wrapper.text()).toContain('状态数据');
    expect(wrapper.text()).toContain('泥水位高程');
    expect(wrapper.text()).toContain('传感器在线状态');
    expect(wrapper.text()).not.toContain('S1_ZT_1.sensor_state.L4_NW_1');
    expect(wrapper.findAll('.trend-group__chart').length).toBe(2);
  });
});
