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
  it('renders the executive-brief hierarchy with the device-scale hero first', () => {
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

    expect(wrapper.get('[data-testid="product-detail-hero-total"]').text()).toBe('2486')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-onlineDeviceCount"]').text()).toContain('1842')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-thirtyDaysActiveCount"]').text()).toContain('2117')
  })
})
