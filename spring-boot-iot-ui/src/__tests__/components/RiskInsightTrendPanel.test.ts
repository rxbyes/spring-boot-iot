import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import RiskInsightTrendPanel from '@/components/RiskInsightTrendPanel.vue';

const { mockChartSetOption, mockChartResize, mockChartDispose, mockEchartsInit } = vi.hoisted(() => ({
  mockChartSetOption: vi.fn(),
  mockChartResize: vi.fn(),
  mockChartDispose: vi.fn(),
  mockEchartsInit: vi.fn()
}));

vi.mock('echarts/core', () => ({
  use: vi.fn(),
  init: mockEchartsInit.mockImplementation(() => ({
    setOption: mockChartSetOption,
    resize: mockChartResize,
    dispose: mockChartDispose
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

const ElSegmentedStub = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    options: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue', 'change'],
  template: `
    <div class="el-segmented-stub">
      <button
        v-for="option in options"
        :key="option.value"
        :data-testid="'trend-range-' + option.value"
        type="button"
        @click="$emit('update:modelValue', option.value); $emit('change', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `
});

function mountTrend(groups: Array<Record<string, unknown>> = [], summary: Array<Record<string, unknown>> = []) {
  return mount(RiskInsightTrendPanel, {
    props: {
      rangeCode: '1d',
      groups,
      summary,
      emptyMessage: '请输入设备编码后开始综合分析'
    },
    global: {
      stubs: {
        PanelCard: PanelCardStub,
        'el-segmented': ElSegmentedStub
      }
    }
  });
}

describe('RiskInsightTrendPanel', () => {
  it('uses the simplified trend layout without panel description or series chips', () => {
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
    ]);

    expect(wrapper.text()).toContain('监测数据');
    expect(wrapper.text()).toContain('状态数据');
    expect(wrapper.text()).not.toContain('支持按近一天查看设备监测数据趋势');
    expect(wrapper.findAll('.trend-group__legend-item')).toHaveLength(0);
  });

  it('renders empty guidance when there are no grouped trend series', () => {
    const wrapper = mountTrend();

    expect(wrapper.text()).toContain('属性趋势预览');
    expect(wrapper.text()).toContain('请输入设备编码后开始综合分析');
  });

  it('renders measure and status groups with chinese titles only', () => {
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
    ]);

    expect(wrapper.text()).toContain('监测数据');
    expect(wrapper.text()).toContain('状态数据');
    expect(wrapper.text()).not.toContain('S1_ZT_1.sensor_state.L4_NW_1');
    expect(wrapper.findAll('.trend-group__chart').length).toBe(2);
  });

  it('hides trend summary cards and chart footer notes in the simplified insight layout', () => {
    const wrapper = mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        description: '展示设备本体的监测值折线变化。',
        series: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            buckets: [{ time: '2026-04-09 00:00:00', value: 1224.37, filled: false }]
          }
        ]
      }
    ]);

    expect(wrapper.text()).not.toContain('默认范围');
    expect(wrapper.text()).not.toContain('最近桶值');
    expect(wrapper.findAll('.trend-group__note')).toHaveLength(0);
    expect(wrapper.findAll('.trend-summary__item')).toHaveLength(0);
  });

  it('shows day week month year selectors inside the trend card and emits range changes', async () => {
    const wrapper = mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        series: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            buckets: [{ time: '2026-04-09 00:00:00', value: 1224.37, filled: false }]
          }
        ]
      }
    ]);

    expect(wrapper.text()).toContain('近一天');
    expect(wrapper.text()).toContain('近一周');
    expect(wrapper.text()).toContain('近一月');
    expect(wrapper.text()).toContain('近一年');

    await wrapper.get('[data-testid="trend-range-365d"]').trigger('click');

    expect(wrapper.emitted('change-range')).toEqual([['365d']]);
  });

  it('compresses the one-day view to the active data window when edge buckets are only filled zeros', async () => {
    mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        series: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            buckets: [
              { time: '2026-04-09 00:00:00', value: 0, filled: true },
              { time: '2026-04-09 01:00:00', value: 0, filled: true },
              { time: '2026-04-09 02:00:00', value: 0.58, filled: false },
              { time: '2026-04-09 03:00:00', value: 0.63, filled: false },
              { time: '2026-04-09 04:00:00', value: 0.75, filled: false },
              { time: '2026-04-09 05:00:00', value: 0, filled: true }
            ]
          }
        ]
      }
    ]);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockChartSetOption).toHaveBeenCalled();
    const latestOption = mockChartSetOption.mock.calls.at(-1)?.[0] as { xAxis?: { data?: string[] } } | undefined;
    expect(latestOption?.xAxis?.data).toEqual([
      '2026-04-09 02:00:00',
      '2026-04-09 03:00:00',
      '2026-04-09 04:00:00'
    ]);
  });

  it('uses non-repeating colors across measure and status trend groups', async () => {
    mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        series: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            buckets: [{ time: '2026-04-09 00:00:00', value: 12.4, filled: false }]
          },
          {
            identifier: 'L1_QJ_1.angle',
            displayName: '水平面夹角',
            buckets: [{ time: '2026-04-09 00:00:00', value: 3.1, filled: false }]
          }
        ]
      },
      {
        key: 'status',
        title: '状态数据',
        series: [
          {
            identifier: 'S1_ZT_1.sensor_state',
            displayName: '在线状态',
            buckets: [{ time: '2026-04-09 00:00:00', value: 1, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.battery_dump_energy',
            displayName: '剩余电量',
            buckets: [{ time: '2026-04-09 00:00:00', value: 86, filled: false }]
          }
        ]
      }
    ]);

    await new Promise((resolve) => setTimeout(resolve, 0));

    const measureOption = mockChartSetOption.mock.calls.at(-2)?.[0] as { color?: string[] } | undefined;
    const statusOption = mockChartSetOption.mock.calls.at(-1)?.[0] as { color?: string[] } | undefined;
    const colors = [...(measureOption?.color ?? []), ...(statusOption?.color ?? [])];

    expect(colors).toHaveLength(4);
    expect(new Set(colors).size).toBe(4);
  });

  it('reduces y-axis density and adds visual buffer for small fluctuations', async () => {
    mountTrend([
      {
        key: 'measure',
        title: '监测数据',
        series: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            buckets: [
              { time: '2026-04-09 00:00:00', value: 1224.37, filled: false },
              { time: '2026-04-09 01:00:00', value: 1224.92, filled: false },
              { time: '2026-04-09 02:00:00', value: 1225.11, filled: false }
            ]
          }
        ]
      }
    ]);

    await new Promise((resolve) => setTimeout(resolve, 0));

    const latestOption = mockChartSetOption.mock.calls.at(-1)?.[0] as {
      yAxis?: { splitNumber?: number; min?: number; max?: number };
    } | undefined;

    expect(latestOption?.yAxis?.splitNumber).toBe(4);
    expect(latestOption?.yAxis?.min).toBeLessThan(1224.37);
    expect(latestOption?.yAxis?.max).toBeGreaterThan(1225.11);
    expect((latestOption?.yAxis?.max ?? 0) - (latestOption?.yAxis?.min ?? 0)).toBeGreaterThan(0.74);
  });
});
