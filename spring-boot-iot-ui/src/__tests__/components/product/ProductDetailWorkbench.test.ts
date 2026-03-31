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
  it('renders overview content as lightweight trend and archive grids without a standalone device-scale stage', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(
      wrapper.findAll('[data-testid="product-detail-stage-title"]').map((node) => node.text())
    ).toEqual([
      '趋势摘要与契约档案',
    ])

    expect(wrapper.find('[data-testid="product-detail-hero-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-primary-metric"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-hero-sideboard"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-ledger-stage"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-governance-summary"]').exists()).toBe(false)
    expect(wrapper.find('.product-detail-workbench__trend-grid').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__trend-cell')).toHaveLength(6)
    expect(wrapper.find('.product-detail-workbench__archive-grid').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__archive-cell')).toHaveLength(8)
    expect(wrapper.find('.product-detail-workbench__archive-cell--wide').exists()).toBe(true)
    expect(wrapper.find('.product-detail-workbench__trend-grid').find('strong').exists()).toBe(false)
    expect(wrapper.find('.product-detail-workbench__archive-grid').find('strong').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('经营主判断')
    expect(wrapper.text()).not.toContain('稳定使用中')
    expect(wrapper.text()).not.toContain('维护与治理')
    expect(wrapper.text()).not.toContain('关联设备总量')
    expect(wrapper.text()).not.toContain('Hero Stage')
    expect(wrapper.text()).not.toContain('Trend Stage')
    expect(wrapper.text()).not.toContain('Contract & Archive Stage')
    expect(wrapper.text()).not.toContain('Governance Stage')
  })

  it('keeps the integrated ledger visible with a quiet placeholder when no trend metrics exist', () => {
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

    expect(wrapper.get('[data-testid="product-detail-ledger-stage"]').text()).toContain('当前还没有足够的活跃度样本')
    expect(wrapper.find('.product-detail-workbench__trend-grid').exists()).toBe(false)
    expect(wrapper.text()).toContain('接入契约与产品档案')
    expect(wrapper.text()).not.toContain('维护与治理')
  })

  it('renders the 2x3 trend grid and 3x3 archive grid from the product payload', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.get('[data-testid="product-detail-ledger-stage"]').text()).toContain('趋势摘要')
    expect(wrapper.get('[data-testid="product-detail-ledger-stage"]').text()).toContain('接入契约与产品档案')
    expect(wrapper.find('.product-detail-workbench__trend-grid').text()).toContain('今日活跃')
    expect(wrapper.find('.product-detail-workbench__trend-grid').text()).toContain('最长在线时长')
    expect(wrapper.find('.product-detail-workbench__trend-grid').text()).toContain('趋势判断')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).toContain('mqtt-json')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).toContain('直连设备')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).toContain('GHLZM')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).toContain('启用')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).toContain('用于边坡监测的 GNSS 终端')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).not.toContain('north-monitor-gnss-v1')
    expect(wrapper.find('.product-detail-workbench__archive-grid').text()).not.toContain('北斗监测终端')
    expect(wrapper.text()).not.toContain('接入提示')
    expect(wrapper.text()).not.toContain('维护与治理')
    expect(wrapper.text()).not.toContain('当前建议')
  })
})
