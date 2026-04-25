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
          normativeIdentifier: 'device_reboot',
          normativeName: '设备重启',
          riskReady: true,
          rawIdentifiers: ['restart', 'reboot'],
          manualCandidate: {
            modelType: 'service',
            identifier: 'reboot',
            modelName: '重启设备',
            evidenceOrigin: 'sample_json',
            sourceTables: ['manual_sample']
          },
          runtimeCandidate: {
            modelType: 'service',
            identifier: 'reboot',
            modelName: '重启服务',
            evidenceOrigin: 'runtime',
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
  it('groups rows by simple customer decisions and renders compact result cards', async () => {
    const wrapper = mountTable()

    expect(wrapper.text()).toContain('可直接生效')
    expect(wrapper.text()).toContain('待确认')
    expect(wrapper.text()).toContain('继续观察')
    expect(wrapper.text()).toContain('存在差异')
    expect(wrapper.text()).toContain('候选中文名')
    expect(wrapper.text()).toContain('样例值')
    expect(wrapper.text()).toContain('正式字段：暂无')
    expect(wrapper.text()).toContain('正式字段标识：')
    expect(wrapper.text()).toContain('类型:')
    expect(wrapper.text()).toContain('规范字段：设备重启')
    expect(wrapper.text()).toContain('当前建议：')
    expect(wrapper.text()).toContain('原始字段：restart / reboot')
    expect(wrapper.text()).toContain('可进入风险闭环')
    expect(wrapper.text()).toContain('来自上报样本')
    expect(wrapper.text()).toContain('reboot')
    expect(wrapper.text()).not.toContain('建议人工确认')
    expect(wrapper.text()).not.toContain('S1_ZT_1.signal_4g')

    await wrapper.get('[data-testid="governance-group-observe"]').trigger('click')
    expect(wrapper.text()).toContain('S1_ZT_1.signal_4g')
    expect(wrapper.text()).toContain('来自运行数据')
    expect(wrapper.text()).toContain('证据还不够，先继续观察')
    expect(wrapper.get('[data-testid="governance-decision-property:S1_ZT_1.signal_4g-observe"]').text()).toBe('继续观察')

    await wrapper.get('[data-testid="governance-group-conflict"]').trigger('click')
    expect(wrapper.text()).toContain('alarmRaised')
    expect(wrapper.text()).toContain('正式模型已存在')
    expect(wrapper.text()).toContain('正式字段：已存在')
    expect(wrapper.get('[data-testid="governance-decision-event:alarmRaised-review"]').text()).toBe('待确认')
    expect(wrapper.get('[data-testid="governance-decision-event:alarmRaised-ignore"]').text()).toBe('忽略')
  })

  it('emits explicit governance decisions for compare rows', async () => {
    const wrapper = mountTable()

    await wrapper.get('[data-testid="governance-group-conflict"]').trigger('click')
    await wrapper.get('[data-testid="governance-decision-event:alarmRaised-review"]').trigger('click')

    expect(wrapper.emitted('change-decision')).toEqual([
      [{ key: 'event:alarmRaised', decision: 'review' }]
    ])
  })

  it('renders short source labels instead of the old evidence headings', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'L1_QJ_1.X',
            compareStatus: 'formal_exists',
            suggestedAction: '纳入修订',
            riskFlags: ['formal_baseline'],
            suspectedMatches: [],
            manualCandidate: {
              modelType: 'property',
              identifier: 'L1_QJ_1.X',
              modelName: '倾角测点 X 轴倾角',
              evidenceOrigin: 'sample_json',
              unit: '°',
              sourceTables: ['manual_sample']
            },
            runtimeCandidate: {
              modelType: 'property',
              identifier: 'L1_QJ_1.X',
              modelName: '倾角测点 X 轴倾角',
              evidenceOrigin: 'runtime',
              sourceTables: ['iot_device_property']
            },
            formalModel: {
              modelId: '1001',
              modelType: 'property',
              identifier: 'L1_QJ_1.X',
              modelName: '倾角测点 X 轴倾角',
              evidenceOrigin: 'formal',
              sourceTables: ['iot_product_model']
            }
          }
        ]
      }
    })

    expect(wrapper.text()).toContain('来自上报样本')
    expect(wrapper.text()).toContain('来自运行数据')
    expect(wrapper.text()).toContain('正式模型已存在')
    expect(wrapper.text()).toContain('正式字段：已存在')
    expect(wrapper.text()).toContain('正式模型已存在，可按需纳入修订')
    expect(wrapper.text()).not.toContain('规范依据')
    expect(wrapper.text()).not.toContain('运行依据')
    expect(wrapper.text()).not.toContain('当前正式契约')
  })

  it('renders short parent-child source chips when protocol template evidence is present', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'value',
            compareStatus: 'runtime_only',
            suggestedAction: '继续观察',
            riskFlags: [],
            suspectedMatches: [],
            runtimeCandidate: {
              modelType: 'property',
              identifier: 'value',
              modelName: '裂缝值',
              dataType: 'double',
              sourceTables: ['iot_device_property', 'iot_message_log'],
              protocolTemplateEvidence: {
                templateCodes: ['crack_child_template'],
                childDeviceCodes: ['202018143'],
                canonicalizationStrategies: ['LF_VALUE'],
                statusMirrorApplied: true,
                parentRemovalKeys: ['L1_LF_1']
              }
            }
          }
        ]
      }
    })

    expect(wrapper.text()).toContain('来自父设备归一')
    expect(wrapper.text()).toContain('裂缝模板')
    expect(wrapper.text()).toContain('202018143')
    expect(wrapper.text()).not.toContain('查看技术依据')
  })

  it('renders normative match explanations and keeps ambiguous rows in manual review', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'value',
            compareStatus: 'double_aligned',
            suggestedAction: '纳入新增',
            riskFlags: [],
            suspectedMatches: [],
            normativeMatchStatus: 'AMBIGUOUS',
            normativeMatchSource: 'CODE_PREFIX_FALLBACK',
            normativeMatchReason: '依据 L4/NW + leaf=value 命中多个规范候选，请人工确认',
            normativeCandidates: ['phase5-mud-level / value / 泥水位', 'phase6-radar / value / 雷达水位'],
            rawIdentifiers: ['L4_NW_1'],
            manualCandidate: {
              modelType: 'property',
              identifier: 'value',
              modelName: '泥水位',
              dataType: 'double',
              evidenceOrigin: 'sample_json',
              sourceTables: ['manual_sample']
            }
          }
        ],
        decisionState: {
          'property:value': 'review'
        }
      }
    })

    expect(wrapper.text()).toContain('规范识别：候选冲突')
    expect(wrapper.text()).toContain('识别来源：编码兜底')
    expect(wrapper.text()).toContain('识别依据：依据 L4/NW + leaf=value 命中多个规范候选，请人工确认')
    expect(wrapper.text()).toContain('规范候选：phase5-mud-level / value / 泥水位 / phase6-radar / value / 雷达水位')
    expect(wrapper.text()).toContain('规范候选存在冲突，请先确认或补充映射规则')
    expect(wrapper.get('[data-testid="governance-decision-property:value-review"]').text()).toBe('待确认')
  })

  it('emits governance actions with raw identifiers for ambiguous and missed normative rows', async () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'value',
            compareStatus: 'double_aligned',
            suggestedAction: '人工确认',
            riskFlags: [],
            suspectedMatches: [],
            normativeMatchStatus: 'AMBIGUOUS',
            rawIdentifiers: ['L4_NW_1'],
            manualCandidate: {
              modelType: 'property',
              identifier: 'value',
              modelName: '泥水位',
              dataType: 'double'
            }
          },
          {
            modelType: 'property',
            identifier: 'L9_UNKNOWN_1.value',
            compareStatus: 'runtime_only',
            suggestedAction: '继续观察',
            riskFlags: [],
            suspectedMatches: [],
            normativeMatchStatus: 'MISSED',
            rawIdentifiers: ['L9_UNKNOWN_1'],
            runtimeCandidate: {
              modelType: 'property',
              identifier: 'L9_UNKNOWN_1.value',
              modelName: '未知字段',
              dataType: 'double'
            }
          }
        ],
        decisionState: {
          'property:value': 'review',
          'property:L9_UNKNOWN_1.value': 'observe'
        }
      }
    })

    await wrapper.get('[data-testid="governance-group-conflict"]').trigger('click')
    await wrapper.get('[data-testid="governance-action-property:value-mapping-rule"]').trigger('click')

    expect(wrapper.emitted('start-governance')?.[0]?.[0]).toEqual({
      key: 'property:value',
      target: 'mapping-rule',
      rawIdentifier: 'L4_NW_1'
    })

    await wrapper.get('[data-testid="governance-group-observe"]').trigger('click')
    await wrapper.get('[data-testid="governance-action-property:L9_UNKNOWN_1.value-runtime-display-rule"]').trigger('click')

    expect(wrapper.emitted('start-governance')?.[1]?.[0]).toEqual({
      key: 'property:L9_UNKNOWN_1.value',
      target: 'runtime-display-rule',
      rawIdentifier: 'L9_UNKNOWN_1'
    })
  })

  it('keeps source chips simple when protocol template evidence is absent', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'value',
            compareStatus: 'runtime_only',
            suggestedAction: '继续观察',
            riskFlags: [],
            suspectedMatches: [],
            runtimeCandidate: {
              modelType: 'property',
              identifier: 'value',
              modelName: '裂缝值',
              dataType: 'double',
              sourceTables: ['iot_device_property', 'iot_message_log']
            }
          }
        ]
      }
    })

    expect(wrapper.text()).not.toContain('来自父设备归一')
    expect(wrapper.text()).not.toContain('查看技术依据')
  })

  it('marks the row title as candidate display name so it is not confused with the formal identifier', () => {
    const wrapper = mount(ProductModelGovernanceCompareTable, {
      props: {
        rows: [
          {
            modelType: 'property',
            identifier: 'S1_ZT_1.battery_dump_energy',
            compareStatus: 'manual_only',
            suggestedAction: '继续观察',
            riskFlags: ['runtime_missing'],
            suspectedMatches: [],
            manualCandidate: {
              modelType: 'property',
              identifier: 'S1_ZT_1.battery_dump_energy',
              modelName: '电池剩余电量',
              dataType: 'integer',
              evidenceOrigin: 'sample_json',
              sourceTables: ['manual_sample']
            }
          }
        ]
      }
    })

    expect(wrapper.text()).toContain('候选中文名')
    expect(wrapper.text()).toContain('电池剩余电量')
    expect(wrapper.text()).toContain('正式字段标识：S1_ZT_1.battery_dump_energy')
  })
})
