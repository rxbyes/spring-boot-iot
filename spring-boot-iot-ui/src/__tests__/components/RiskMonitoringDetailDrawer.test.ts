import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RiskMonitoringDetailDrawer from '@/components/RiskMonitoringDetailDrawer.vue'
import { getRiskMonitoringDetail } from '@/api/riskMonitoring'

vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringDetail: vi.fn()
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="risk-monitoring-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
})

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('RiskMonitoringDetailDrawer', () => {
  beforeEach(() => {
    vi.mocked(getRiskMonitoringDetail).mockReset()
    vi.mocked(getRiskMonitoringDetail).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        bindingId: 1,
        riskPointName: '泵房液位',
        deviceCode: 'demo-device-01',
        riskPointLevel: 'level_1',
        currentRiskLevel: 'warning',
        riskLevel: 'warning',
        monitorStatus: 'NORMAL',
        onlineStatus: 1,
        currentValue: '26.5',
        unit: 'm',
        recentAlarms: [
          {
            id: 10,
            alarmTitle: '液位红色告警',
            alarmLevel: 'red',
            triggerTime: '2026-04-02 08:30:00'
          }
        ],
        recentEvents: [
          {
            id: 11,
            eventTitle: '泵房巡检事件',
            riskLevel: 'orange',
            triggerTime: '2026-04-02 08:35:00'
          }
        ]
      }
    })
  })

  it('keeps the monitoring detail drawer in Chinese without the legacy English eyebrow tier', async () => {
    const wrapper = mount(RiskMonitoringDetailDrawer, {
      props: {
        modelValue: true,
        bindingId: 1
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub,
          ElTag: true
        }
      }
    })

    await flushPromises()

    const drawer = wrapper.findComponent(StandardDetailDrawerStub)

    expect(getRiskMonitoringDetail).toHaveBeenCalledWith(1)
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('泵房液位')
    expect(wrapper.text()).toContain('监测概览')
    expect(wrapper.text()).toContain('风险点等级')
    expect(wrapper.text()).toContain('一级风险点')
    expect(wrapper.text()).toContain('当前风险态势')
    expect(wrapper.text()).toContain('告警等级')
    expect(wrapper.text()).not.toContain('Risk Monitoring Detail')
  })
})
