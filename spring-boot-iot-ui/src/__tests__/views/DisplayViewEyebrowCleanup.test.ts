import { createPinia } from 'pinia'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const mockRouterPush = vi.fn()
const mockRouterReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
    replace: mockRouterReplace
  }),
  useRoute: () => ({
    query: {},
    params: {}
  }),
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: ['to'],
    template: '<a class="router-link-stub" :data-to="String(to)"><slot /></a>'
  })
}))

vi.mock('../../api/report', () => ({
  getRiskTrendAnalysis: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      { alarmCount: 12, eventCount: 4 },
      { alarmCount: 15, eventCount: 5 }
    ]
  }),
  getAlarmStatistics: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 18,
      critical: 3,
      high: 5
    }
  }),
  getEventClosureAnalysis: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 10,
      closed: 8,
      unclosed: 2
    }
  }),
  getDeviceHealthAnalysis: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 20,
      onlineRate: 92
    }
  })
}))

vi.mock('../../api/riskGovernance', () => ({
  getRiskGovernanceDashboardOverview: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      totalProductCount: 10,
      governedProductCount: 8,
      pendingProductGovernanceCount: 2,
      releasedProductCount: 7,
      pendingContractReleaseCount: 3,
      publishedRiskMetricCount: 12,
      boundRiskMetricCount: 9,
      ruleCoveredRiskMetricCount: 7,
      pendingRiskBindingCount: 4,
      pendingPolicyCount: 2,
      rawStageVendorCount: 2,
      rawStageProductCount: 3,
      rawStageVendorNames: ['南方测绘', '中海达'],
      rawStageProductNames: ['GNSS位移监测仪', '裂缝计'],
      pendingReplayCount: 1,
      governanceCompletionRate: 80,
      metricBindingCoverageRate: 75,
      policyCoverageRate: 77.8
    }
  })
}))

import AutomationTestCenterView from '@/views/AutomationTestCenterView.vue'
import CockpitView from '@/views/CockpitView.vue'
import FutureLabView from '@/views/FutureLabView.vue'

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="display-view-panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <slot />
      <slot name="actions" />
    </section>
  `
})

const ResponsePanelStub = defineComponent({
  name: 'ResponsePanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="display-view-response-panel-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <slot />
    </section>
  `
})

function buildPanelGlobal() {
  return {
    plugins: [createPinia()],
    stubs: {
      PanelCard: PanelCardStub,
      ResponsePanel: ResponsePanelStub,
      MetricCard: true,
      StandardActionGroup: true,
      StandardButton: true,
      StandardTableToolbar: true,
      StandardTableTextColumn: true,
      StandardFlowRail: true,
      AutomationExecutionConfigPanel: true,
      AutomationSuggestionPanel: true,
      AutomationPageDiscoveryPanel: true,
      AutomationScenarioEditor: true,
      AutomationPlanImportDrawer: true,
      AutomationManualPageDrawer: true,
      RouterLink: true,
      ElTable: true,
      ElTableColumn: true,
      ElTag: true
    }
  }
}

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('display view eyebrow cleanup', () => {
  it('keeps the automation center hero and cards free from legacy English eyebrow tiers', async () => {
    const wrapper = mount(AutomationTestCenterView, {
      global: buildPanelGlobal()
    })

    await flushPromises()

    const panelCards = wrapper.findAllComponents(PanelCardStub)
    const responsePanel = wrapper.findComponent(ResponsePanelStub)

    expect(panelCards.length).toBeGreaterThan(0)
    expect(panelCards.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    if (responsePanel.exists()) {
      expect(responsePanel.props('eyebrow')).toBeUndefined()
    }
    expect(wrapper.text()).not.toContain('Automation Studio')
  })

  it('keeps the cockpit view on Chinese hierarchy without the legacy English eyebrow tiers', async () => {
    const wrapper = mount(CockpitView, {
      global: buildPanelGlobal()
    })

    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text().includes('管理人员'))?.trigger('click')
    await flushPromises()

    const panelCards = wrapper.findAllComponents(PanelCardStub)

    expect(panelCards.length).toBeGreaterThan(0)
    expect(panelCards.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    expect(wrapper.text()).not.toContain('Risk Data Cockpit')
    expect(wrapper.text()).toContain('原始字段阶段 2 个厂商 / 3 个产品')
    expect(wrapper.text()).toContain('南方测绘 / 中海达')
    expect(wrapper.text()).toContain('GNSS位移监测仪 / 裂缝计')
  })

  it('keeps the future lab cards free from English eyebrow layers', () => {
    const wrapper = mount(FutureLabView, {
      global: buildPanelGlobal()
    })

    const panelCards = wrapper.findAllComponents(PanelCardStub)

    expect(panelCards.length).toBeGreaterThan(0)
    expect(panelCards.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    expect(wrapper.text()).not.toContain('Future Lab')
    expect(wrapper.text()).not.toContain('Planned Capability')
    expect(wrapper.text()).not.toContain('Integration Notes')
    expect(wrapper.text()).not.toContain('Roadmap Bridge')
  })
})
