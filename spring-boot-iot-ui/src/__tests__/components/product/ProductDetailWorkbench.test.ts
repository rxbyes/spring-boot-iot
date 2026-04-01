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
  it('renders overview as a journal spread with a lead sheet, scale ledger, contract sheet, and archive notes', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.find('[data-testid="product-detail-hero-plinth"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-lead-sheet"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="product-detail-primary-metric"]').text()).toContain('2486')
    expect(wrapper.find('[data-testid="product-detail-metric-ribbon"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-scale-ledger"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__scale-note')).toHaveLength(3)
    expect(wrapper.find('.product-detail-workbench__journal-grid').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-contract-ledger"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-contract-sheet"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__contract-line')).toHaveLength(3)
    expect(wrapper.find('[data-testid="product-detail-archive-notes"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="product-detail-archive-sheet"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__archive-note')).toHaveLength(3)
    expect(wrapper.find('.product-detail-workbench__exhibit-sheet').exists()).toBe(false)
    expect(wrapper.text()).toContain('核心规模')
    expect(wrapper.text()).toContain('在线覆盖')
    expect(wrapper.text()).toContain('30 日活跃')
    expect(wrapper.text()).toContain('平均在线')
    expect(wrapper.text()).toContain('契约基线')
    expect(wrapper.text()).toContain('档案摘要')
    expect(wrapper.text()).toContain('接入协议')
    expect(wrapper.text()).toContain('节点类型')
    expect(wrapper.text()).toContain('数据格式')
    expect(wrapper.text()).not.toContain('当前判断')
    expect(wrapper.text()).not.toContain('近 30 日已有')
    expect(wrapper.text()).not.toContain('趋势摘要')
    expect(wrapper.text()).not.toContain('接入契约与产品档案')
    expect(wrapper.text()).not.toContain('维护与治理')
  })

  it('keeps the journal lead sheet and line-based sections visible when activity metrics are missing', () => {
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

    expect(wrapper.find('[data-testid="product-detail-lead-sheet"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-scale-ledger"]').exists()).toBe(true)
    expect(wrapper.find('.product-detail-workbench__journal-grid').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-contract-sheet"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="product-detail-archive-sheet"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('在线覆盖')
    expect(wrapper.text()).toContain('--')
    expect(wrapper.text()).toContain('档案摘要')
  })

  it('renders the journal spread without repeating product identity or judgement text inside the body', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.get('[data-testid="product-detail-scale-ledger"]').text()).toContain('74%')
    expect(wrapper.get('[data-testid="product-detail-scale-ledger"]').text()).toContain('2117')
    expect(wrapper.get('[data-testid="product-detail-scale-ledger"]').text()).toContain('128 分钟')
    expect(wrapper.get('[data-testid="product-detail-contract-sheet"]').text()).toContain('mqtt-json')
    expect(wrapper.get('[data-testid="product-detail-contract-sheet"]').text()).toContain('直连设备')
    expect(wrapper.get('[data-testid="product-detail-contract-sheet"]').text()).toContain('JSON')
    expect(wrapper.get('[data-testid="product-detail-archive-sheet"]').text()).toContain('GHLZM')
    expect(wrapper.get('[data-testid="product-detail-archive-sheet"]').text()).toContain('用于边坡监测的 GNSS 终端')
    expect(wrapper.find('.product-detail-workbench__brief-stage').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('north-monitor-gnss-v1')
    expect(wrapper.text()).not.toContain('北斗监测终端')
    expect(wrapper.text()).not.toContain('当前判断')
    expect(wrapper.text()).not.toContain('经营基线已进入持续优化阶段')
  })
})
