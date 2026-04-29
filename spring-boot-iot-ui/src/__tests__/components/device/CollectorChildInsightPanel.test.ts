import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import CollectorChildInsightPanel from '@/components/device/CollectorChildInsightPanel.vue'
import type { CollectorChildInsightOverview } from '@/types/api'

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
})

const overview: CollectorChildInsightOverview = {
  parentDeviceCode: 'COLLECTOR-001',
  parentOnlineStatus: 1,
  childCount: 3,
  reachableChildCount: 2,
  sensorStateReportedCount: 2,
  missingChildCount: 1,
  staleChildCount: 1,
  recommendedMetricCount: 2,
  children: [
    {
      logicalChannelCode: 'L1_LF_1',
      childDeviceCode: 'LASER-001',
      childDeviceName: '1# 激光测点',
      childProductKey: 'nf-monitor-laser-rangefinder-v1',
      collectorLinkState: 'reachable',
      sensorStateValue: '0',
      sensorStateHealth: 'REPORTED_NORMAL',
      lastReportTime: '2026-04-28 09:15:00',
      recommendedMetricIdentifiers: ['value'],
      metrics: [
        {
          identifier: 'value',
          displayName: '激光测距值',
          propertyValue: '10.86',
          unit: 'mm',
          recommended: true,
          reportTime: '2026-04-28 09:15:00'
        }
      ]
    },
    {
      logicalChannelCode: 'L1_LF_2',
      childDeviceCode: 'LASER-002',
      childDeviceName: '2# 激光测点',
      childProductKey: 'nf-monitor-laser-rangefinder-v1',
      collectorLinkState: 'unreachable',
      sensorStateValue: null,
      sensorStateHealth: 'MISSING',
      lastReportTime: null,
      recommendedMetricIdentifiers: [],
      metrics: []
    }
  ]
}

describe('CollectorChildInsightPanel', () => {
  it('renders the collector overview with an identity-first summary and lightweight child cards', () => {
    const wrapper = mount(CollectorChildInsightPanel, {
      props: {
        overview
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(wrapper.text()).toContain('子设备总览')
    expect(wrapper.text()).toContain('采集器视角')
    expect(wrapper.text()).toContain('COLLECTOR-001')
    expect(wrapper.text()).toContain('在线状态：在线')
    expect(wrapper.text()).toContain('建议指标')
    expect(wrapper.text()).toContain('待关注')
    expect(wrapper.text()).toContain('1# 激光测点')
    expect(wrapper.text()).toContain('子产品：nf-monitor-laser-rangefinder-v1')
    expect(wrapper.text()).toContain('最近指标 1 项')
    expect(wrapper.text()).toContain('建议')
    expect(wrapper.text()).toContain('当前子设备暂无监测指标快照。')

    expect(wrapper.find('.collector-child-insight-panel__summary-card--primary').exists()).toBe(true)
    expect(wrapper.findAll('.collector-child-insight-panel__summary-card--compact').length).toBeGreaterThanOrEqual(3)
    expect(wrapper.find('.collector-child-insight-panel__metric--recommended').exists()).toBe(true)
    expect(wrapper.findAll('.collector-child-insight-panel__child-meta-pill').length).toBeGreaterThan(0)
  })

  it('keeps the source on the shared collector diagnostics grammar', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../../components/device/CollectorChildInsightPanel.vue'), 'utf8')

    expect(source).toContain('collector-child-insight-panel__summary-card--primary')
    expect(source).toContain('collector-child-insight-panel__summary-stats')
    expect(source).toContain('collector-child-insight-panel__child-meta-pill')
    expect(source).toContain('collector-child-insight-panel__metric--recommended')
    expect(source).not.toContain('collector-child-insight-panel__summary-card--warn')
  })
})
