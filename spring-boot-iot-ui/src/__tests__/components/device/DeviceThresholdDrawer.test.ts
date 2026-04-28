import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DeviceThresholdDrawer from '@/components/device/DeviceThresholdDrawer.vue'

describe('DeviceThresholdDrawer', () => {
  it('renders grouped threshold sections for the device', () => {
    const wrapper = mount(DeviceThresholdDrawer, {
      props: {
        modelValue: true,
        loading: false,
        errorMessage: '',
        overview: {
          deviceId: '8001',
          deviceCode: 'crack-device-01',
          deviceName: '北坡裂缝设备 01',
          productId: '1001',
          productName: '裂缝监测产品',
          matchedMetricCount: 1,
          missingMetricCount: 0,
          items: [
            {
              riskMetricId: '7001',
              metricIdentifier: 'value',
              metricName: '裂缝值',
              effectiveRules: [
                {
                  ruleId: '3002',
                  ruleName: 'binding-red',
                  ruleScope: 'BINDING',
                  ruleScopeText: '绑定个性',
                  expression: 'value >= 8',
                  alarmLevel: 'red',
                  sourceLabel: '绑定个性',
                  targetLabel: '绑定 9101',
                  riskPointDeviceId: '9101'
                }
              ],
              bindingRules: [
                {
                  ruleId: '3002',
                  ruleName: 'binding-red',
                  ruleScope: 'BINDING',
                  ruleScopeText: '绑定个性',
                  expression: 'value >= 8',
                  alarmLevel: 'red',
                  sourceLabel: '绑定个性',
                  targetLabel: '绑定 9101',
                  riskPointDeviceId: '9101'
                }
              ],
              deviceRules: [],
              productRules: [
                {
                  ruleId: '3001',
                  ruleName: 'product-default',
                  ruleScope: 'PRODUCT',
                  ruleScopeText: '产品默认',
                  expression: 'value >= 6',
                  alarmLevel: 'orange',
                  sourceLabel: '产品默认',
                  targetLabel: '产品 1001'
                }
              ],
              fallbackRules: []
            }
          ]
        }
      },
      global: {
        stubs: {
          StandardDetailDrawer: { template: '<section><slot /><slot name="footer" /></section>' }
        }
      }
    })

    expect(wrapper.text()).toContain('设备名称')
    expect(wrapper.text()).toContain('设备编号')
    expect(wrapper.text()).toContain('当前生效')
    expect(wrapper.text()).toContain('绑定个性')
    expect(wrapper.text()).toContain('产品默认')
    expect(wrapper.text()).toContain('binding-red')
    expect(wrapper.text()).toContain('product-default')
    expect(wrapper.text()).toContain('红色告警')
    expect(wrapper.text()).toContain('橙色告警')
  })

  it('renders the empty state when no metrics are available', () => {
    const wrapper = mount(DeviceThresholdDrawer, {
      props: {
        modelValue: true,
        loading: false,
        errorMessage: '',
        overview: {
          deviceId: '8002',
          deviceCode: 'empty-device',
          deviceName: '空设备',
          productId: '1002',
          productName: '空产品',
          matchedMetricCount: 0,
          missingMetricCount: 0,
          items: []
        }
      },
      global: {
        stubs: {
          StandardDetailDrawer: { template: '<section><slot /><slot name="footer" /></section>' }
        }
      }
    })

    expect(wrapper.text()).toContain('当前设备暂无可展示阈值')
  })
})
