import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductDetailWorkbench from '@/components/product/ProductDetailWorkbench.vue'
import type { Product } from '@/types/api'

const baseProduct: Product = {
  id: 1001,
  productKey: 'north-monitor-gnss-v1',
  productName: '北斗监测终端',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'GHLZM',
  description: '用于边坡监测的 GNSS 终端',
  status: 1,
  deviceCount: 2486,
  onlineDeviceCount: 1842,
  lastReportTime: '2026-03-29T09:15:00',
  createTime: '2026-03-01T10:00:00',
  updateTime: '2026-03-29T10:00:00',
  todayActiveCount: 826,
  sevenDaysActiveCount: 1634,
  thirtyDaysActiveCount: 2117,
  avgOnlineDuration: 128,
  maxOnlineDuration: 540
}

describe('ProductDetailWorkbench', () => {
  it('renders the flattened four-part detail hierarchy with one primary hero stage', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(
      wrapper.findAll('[data-testid="product-detail-stage-title"]').map((node) => node.text())
    ).toEqual([
      '设备规模与经营判断',
      '活跃趋势与状态判断',
      '接入契约与产品档案',
      '维护与治理'
    ])

    expect(wrapper.find('[data-testid="product-detail-hero-stage"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-primary-metric"]').text()).toContain('关联设备总量')
    expect(wrapper.find('[data-testid="product-detail-secondary-metrics"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-judgement-stage"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-contract-archive-stage"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="product-detail-hero-total"]').text()).toBe('2486')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-onlineDeviceCount-value"]').text()).toBe('1842')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-thirtyDaysActiveCount-value"]').text()).toBe('2117')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-avgOnlineDuration-value"]').text()).toBe('128 分钟')
    expect(wrapper.text()).not.toContain('Hero Stage')
    expect(wrapper.text()).not.toContain('Trend Stage')
    expect(wrapper.text()).not.toContain('Contract & Archive Stage')
    expect(wrapper.text()).not.toContain('Governance Stage')
  })

  it('keeps the judgement stage visible with a quiet placeholder when no activity metrics exist', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: {
          ...baseProduct,
          todayActiveCount: null,
          sevenDaysActiveCount: null,
          thirtyDaysActiveCount: null,
          avgOnlineDuration: null,
          maxOnlineDuration: null
        }
      }
    })

    expect(wrapper.get('[data-testid="product-detail-judgement-stage"]').text()).toContain('当前还没有足够的活跃度样本')
    expect(wrapper.find('[data-testid="product-detail-trend-metrics"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('接入契约')
    expect(wrapper.text()).toContain('产品档案')
    expect(wrapper.text()).toContain('维护与治理')
  })

  it('renders trend, contract, archive, and governance details from the product payload', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.get('[data-testid="product-detail-trend-metrics"]').text()).toContain('今日活跃')
    expect(wrapper.get('[data-testid="product-detail-trend-metrics"]').text()).toContain('最长在线时长')
    expect(wrapper.get('[data-testid="product-detail-contract"]').text()).toContain('mqtt-json')
    expect(wrapper.get('[data-testid="product-detail-contract"]').text()).toContain('直连设备')
    expect(wrapper.get('[data-testid="product-detail-archive"]').text()).toContain('GHLZM')
    expect(wrapper.get('[data-testid="product-detail-archive"]').text()).toContain('north-monitor-gnss-v1')
    expect(wrapper.get('[data-testid="product-detail-archive"]').text()).toContain('用于边坡监测的 GNSS 终端')
    expect(wrapper.get('[data-testid="product-detail-governance"]').text()).toContain('当前建议')
    expect(wrapper.get('[data-testid="product-detail-governance"]').text()).toContain('当前已有设备在用')
    expect(wrapper.get('[data-testid="product-detail-governance"]').text()).toContain('影响评估')
    expect(wrapper.get('[data-testid="product-detail-governance"]').text()).toContain('维护规则')
    expect(wrapper.get('[data-testid="product-detail-governance"]').text()).toContain('变更前确认')
  })
})
