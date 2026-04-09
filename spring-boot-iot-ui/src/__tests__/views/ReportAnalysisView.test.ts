import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ReportAnalysisView from '@/views/ReportAnalysisView.vue'

vi.mock('echarts/core', () => ({
  use: vi.fn(),
  init: vi.fn(() => ({
    setOption: vi.fn(),
    dispose: vi.fn()
  }))
}))

vi.mock('echarts/charts', () => ({
  LineChart: {},
  BarChart: {},
  PieChart: {}
}))

vi.mock('echarts/renderers', () => ({
  CanvasRenderer: {}
}))

vi.mock('echarts/components', () => ({
  GridComponent: {},
  LegendComponent: {},
  TitleComponent: {},
  TooltipComponent: {}
}))

const {
  mockGetRiskTrendAnalysis,
  mockGetAlarmStatistics,
  mockGetEventClosureAnalysis,
  mockGetDeviceHealthAnalysis
} = vi.hoisted(() => ({
  mockGetRiskTrendAnalysis: vi.fn(),
  mockGetAlarmStatistics: vi.fn(),
  mockGetEventClosureAnalysis: vi.fn(),
  mockGetDeviceHealthAnalysis: vi.fn()
}))

vi.mock('@/api/report', () => ({
  getRiskTrendAnalysis: mockGetRiskTrendAnalysis,
  getAlarmStatistics: mockGetAlarmStatistics,
  getEventClosureAnalysis: mockGetEventClosureAnalysis,
  getDeviceHealthAnalysis: mockGetDeviceHealthAnalysis
}))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="report-analysis-workbench-stub">
      <header>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
        <div class="report-analysis-workbench-stub__actions"><slot name="header-actions" /></div>
      </header>
      <div class="report-analysis-workbench-stub__body"><slot /></div>
    </section>
  `
})

const MetricCardStub = defineComponent({
  name: 'MetricCard',
  props: ['label', 'value'],
  template: '<article class="metric-card-stub">{{ label }} {{ value }}</article>'
})

const ElDatePickerStub = defineComponent({
  name: 'ElDatePicker',
  props: ['modelValue'],
  template: '<div class="el-date-picker-stub">{{ Array.isArray(modelValue) ? modelValue.join(" - ") : "" }}</div>'
})

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('ReportAnalysisView', () => {
  beforeEach(() => {
    mockGetRiskTrendAnalysis.mockReset()
    mockGetAlarmStatistics.mockReset()
    mockGetEventClosureAnalysis.mockReset()
    mockGetDeviceHealthAnalysis.mockReset()

    mockGetRiskTrendAnalysis.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [{ date: '2026-03-30', alarmCount: 3, eventCount: 2 }]
    })
    mockGetAlarmStatistics.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 8, critical: 1, high: 2, medium: 3, low: 2 }
    })
    mockGetEventClosureAnalysis.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 6, closed: 4, unclosed: 2 }
    })
    mockGetDeviceHealthAnalysis.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { onlineRate: 95, healthy: 10, warning: 2, critical: 1 }
    })

    Object.defineProperty(window, 'IntersectionObserver', {
      configurable: true,
      writable: true,
      value: undefined
    })
  })

  it('renders report analysis inside the shared page shell and workbench rhythm', async () => {
    const wrapper = mount(ReportAnalysisView, {
      global: {
        stubs: {
          StandardPageShell: StandardPageShellStub,
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          MetricCard: MetricCardStub,
          ElDatePicker: ElDatePickerStub,
          ElSkeleton: true,
          ElEmpty: true
        }
      }
    })

    await flushPromises()
    await nextTick()

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true)
    expect(wrapper.find('.report-analysis-workbench-stub').exists()).toBe(true)
    expect(wrapper.findComponent(StandardWorkbenchPanelStub).props('title')).toBe('运营分析中心')
    expect(wrapper.findComponent(StandardWorkbenchPanelStub).props('description')).toContain('统一保持平台治理页头与卡片节奏')
    expect(wrapper.find('.report-analysis-workbench-stub__actions .el-date-picker-stub').exists()).toBe(true)
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(4)
    expect(wrapper.text()).toContain('风险趋势分析')
    expect(wrapper.text()).toContain('告警等级分布')
    expect(wrapper.text()).toContain('事件闭环分析')
    expect(wrapper.text()).toContain('设备健康分析')
  })
})
