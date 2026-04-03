import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductModelGovernanceCompareTable from '@/components/product/ProductModelGovernanceCompareTable.vue'

function mountTable() {
  return mount(ProductModelGovernanceCompareTable, {
    props: {
      rows: [
        {
          modelType: 'property',
          identifier: 'S1_ZT_1.signal_4g',
          compareStatus: 'runtime_only',
          suggestedAction: '继续观察',
          riskFlags: ['manual_missing'],
          suspectedMatches: [],
          runtimeCandidate: {
            modelType: 'property',
            identifier: 'S1_ZT_1.signal_4g',
            modelName: '4G 信号强度',
            dataType: 'integer',
            sourceTables: ['iot_device_property']
          }
        },
        {
          modelType: 'event',
          identifier: 'alarmRaised',
          compareStatus: 'suspected_conflict',
          suggestedAction: '人工裁决',
          riskFlags: ['definition_mismatch'],
          suspectedMatches: [],
          manualCandidate: {
            modelType: 'event',
            identifier: 'alarmRaised',
            modelName: '告警触发',
            eventType: 'warning',
            sourceTables: ['manual_draft']
          },
          formalModel: {
            modelId: '2001',
            modelType: 'event',
            identifier: 'alarmRaised',
            modelName: '告警触发',
            eventType: 'info',
            sourceTables: ['iot_product_model']
          }
        },
        {
          modelType: 'service',
          identifier: 'reboot',
          compareStatus: 'double_aligned',
          suggestedAction: '纳入新增',
          riskFlags: [],
          suspectedMatches: [],
          manualCandidate: {
            modelType: 'service',
            identifier: 'reboot',
            modelName: '重启设备',
            sourceTables: ['manual_draft']
          },
          runtimeCandidate: {
            modelType: 'service',
            identifier: 'reboot',
            modelName: '重启服务',
            sourceTables: ['iot_command_record']
          }
        }
      ],
      decisionState: {
        'property:S1_ZT_1.signal_4g': 'observe',
        'event:alarmRaised': 'review',
        'service:reboot': 'create'
      }
    }
  })
}

describe('ProductModelGovernanceCompareTable', () => {
  it('switches across property event and service rows while rendering status and suggested actions', async () => {
    const wrapper = mountTable()

    expect(wrapper.text()).toContain('S1_ZT_1.signal_4g')
    expect(wrapper.text()).toContain('自动证据独有')
    expect(wrapper.text()).toContain('继续观察')
    expect(wrapper.get('[data-testid="governance-decision-property:S1_ZT_1.signal_4g-observe"]').text()).toBe('继续观察')
    expect(wrapper.get('[data-testid="governance-decision-property:S1_ZT_1.signal_4g-ignore"]').text()).toBe('忽略')
    expect(wrapper.text()).not.toContain('alarmRaised')

    await wrapper.get('[data-testid="governance-type-event"]').trigger('click')
    expect(wrapper.text()).toContain('alarmRaised')
    expect(wrapper.text()).toContain('疑似冲突')
    expect(wrapper.text()).toContain('人工裁决')
    expect(wrapper.get('[data-testid="governance-decision-event:alarmRaised-review"]').text()).toBe('人工裁决')
    expect(wrapper.get('[data-testid="governance-decision-event:alarmRaised-ignore"]').text()).toBe('忽略')

    await wrapper.get('[data-testid="governance-type-service"]').trigger('click')
    expect(wrapper.text()).toContain('reboot')
    expect(wrapper.text()).toContain('双证据一致')
    expect(wrapper.text()).toContain('纳入新增')
    expect(wrapper.get('[data-testid="governance-decision-service:reboot-observe"]').text()).toBe('继续观察')
  })

  it('emits explicit governance decisions for compare rows', async () => {
    const wrapper = mountTable()

    await wrapper.get('[data-testid="governance-type-event"]').trigger('click')
    await wrapper.get('[data-testid="governance-decision-event:alarmRaised-review"]').trigger('click')

    expect(wrapper.emitted('change-decision')).toEqual([
      [{ key: 'event:alarmRaised', decision: 'review' }]
    ])
  })

  it('renders normative evidence labels and raw identifiers in the compare row', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'L1_QJ_1.X',
            compareStatus: 'double_aligned',
            suggestedAction: '纳入新增',
            riskFlags: [],
            suspectedMatches: [],
            manualCandidate: {
              modelType: 'property',
              identifier: 'L1_QJ_1.X',
              modelName: '倾角测点 X 轴倾角',
              evidenceOrigin: 'normative',
              unit: '°',
              normativeSource: '表 B.1',
              rawIdentifiers: ['X', 'angleX']
            },
            runtimeCandidate: {
              modelType: 'property',
              identifier: 'L1_QJ_1.X',
              modelName: '倾角测点 X 轴倾角',
              evidenceOrigin: 'runtime',
              sourceTables: ['iot_device_property']
            }
          }
        ]
      }
    })

    expect(wrapper.text()).toContain('规范证据')
    expect(wrapper.text()).toContain('表 B.1')
    expect(wrapper.text()).toContain('X / angleX')
  })
})
