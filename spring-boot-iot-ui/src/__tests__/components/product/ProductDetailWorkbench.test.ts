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
  it('renders compact card rows with three metrics and one archive summary card', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.find('[data-testid="product-detail-metrics-row"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__metric-card')).toHaveLength(3)
    expect(wrapper.find('[data-testid="product-detail-archive-card"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__archive-item')).toHaveLength(3)
    expect(wrapper.find('.product-detail-workbench__archive-item--description').exists()).toBe(true)
    const copyLabels = wrapper.findAll('.product-detail-workbench__copy-label')
    const copyValues = wrapper.findAll('.product-detail-workbench__copy-value')
    expect(copyLabels).toHaveLength(7)
    expect(copyValues).toHaveLength(6)
    expect(wrapper.findAll('.product-detail-workbench__archive-item .product-detail-workbench__copy-value')).toHaveLength(3)
    wrapper
      .findAll('.product-detail-workbench__archive-item .product-detail-workbench__copy-value')
      .forEach((node) => {
        expect(node.classes()).toContain('product-detail-workbench__copy-value--body')
      })
    expect(wrapper.text()).toContain('在线覆盖')
    expect(wrapper.text()).toContain('30 日活跃')
    expect(wrapper.text()).toContain('平均在线')
    expect(wrapper.text()).toContain('档案摘要')
    expect(wrapper.text()).toContain('厂商')
    expect(wrapper.text()).toContain('最近更新')
    expect(wrapper.text()).toContain('补充说明')
    expect(wrapper.text()).not.toContain('核心规模')
    expect(wrapper.text()).not.toContain('契约基线')
    expect(wrapper.text()).not.toContain('接入协议')
    expect(wrapper.text()).not.toContain('节点类型')
    expect(wrapper.text()).not.toContain('数据格式')
    expect(wrapper.text()).not.toContain('当前判断')
    expect(wrapper.text()).not.toContain('近 30 日已有')
    expect(wrapper.text()).not.toContain('趋势摘要')
    expect(wrapper.text()).not.toContain('接入契约与产品档案')
    expect(wrapper.text()).not.toContain('维护与治理')
  })

  it('keeps overview cards compact with explicit hierarchy hooks for browser verification', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.find('.product-detail-workbench__copy-label--metric').exists()).toBe(true)
    expect(wrapper.find('.product-detail-workbench__copy-value--metric').exists()).toBe(true)
    expect(wrapper.find('.product-detail-workbench__copy-value--body').exists()).toBe(true)
  })

  it('keeps compact cards visible when activity metrics are missing', () => {
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

    expect(wrapper.find('[data-testid="product-detail-metrics-row"]').exists()).toBe(true)
    expect(wrapper.findAll('.product-detail-workbench__metric-card')).toHaveLength(3)
    expect(wrapper.find('[data-testid="product-detail-archive-card"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('在线覆盖')
    expect(wrapper.text()).toContain('--')
    expect(wrapper.text()).toContain('档案摘要')
  })

  it('renders card values without repeating product identity or judgement text inside the body', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(wrapper.get('[data-testid="product-detail-metrics-row"]').text()).toContain('74%')
    expect(wrapper.get('[data-testid="product-detail-metrics-row"]').text()).toContain('2117')
    expect(wrapper.get('[data-testid="product-detail-metrics-row"]').text()).toContain('128 分钟')
    expect(wrapper.get('[data-testid="product-detail-archive-card"]').text()).toContain('GHLZM')
    expect(wrapper.get('[data-testid="product-detail-archive-card"]').text()).toContain('用于边坡监测的 GNSS 终端')
    expect(wrapper.get('[data-testid="product-detail-archive-description"]').text()).toContain(
      '用于边坡监测的 GNSS 终端'
    )
    expect(wrapper.find('.product-detail-workbench__archive-title').classes()).toContain(
      'product-detail-workbench__copy-label'
    )
    expect(
      wrapper.find('.product-detail-workbench__archive-item strong').classes()
    ).toContain('product-detail-workbench__copy-value--body')
    expect(wrapper.text()).not.toContain('north-monitor-gnss-v1')
    expect(wrapper.text()).not.toContain('北斗监测终端')
    expect(wrapper.text()).not.toContain('当前判断')
    expect(wrapper.text()).not.toContain('经营基线已进入持续优化阶段')
    expect(wrapper.text()).not.toContain('mqtt-json')
    expect(wrapper.text()).not.toContain('直连设备')
    expect(wrapper.text()).not.toContain('JSON')
  })
})
