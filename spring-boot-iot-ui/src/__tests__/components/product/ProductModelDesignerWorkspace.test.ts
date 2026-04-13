import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'

import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'
import { createRequestError } from '@/api/request'

const {
  mockListProductModels,
  mockPageProductContractReleaseBatches,
  mockPageRiskMetricCatalogs,
  mockGetRiskGovernanceReleaseBatchDiff,
  mockGetProductContractReleaseBatchImpact,
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance,
  mockUpdateProduct,
  mockUpdateProductModel,
  mockDeleteProductModel,
  mockRollbackProductContractReleaseBatch,
  mockResubmitProductGovernanceApproval,
  mockGetGovernanceApprovalOrderDetail,
  mockResubmitGovernanceApprovalOrder,
  mockListDeviceRelations,
  mockRouter
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockPageProductContractReleaseBatches: vi.fn(),
  mockPageRiskMetricCatalogs: vi.fn(),
  mockGetRiskGovernanceReleaseBatchDiff: vi.fn(),
  mockGetProductContractReleaseBatchImpact: vi.fn(),
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockUpdateProductModel: vi.fn(),
  mockDeleteProductModel: vi.fn(),
  mockRollbackProductContractReleaseBatch: vi.fn(),
  mockResubmitProductGovernanceApproval: vi.fn(),
  mockGetGovernanceApprovalOrderDetail: vi.fn(),
  mockResubmitGovernanceApprovalOrder: vi.fn(),
  mockListDeviceRelations: vi.fn(),
  mockRouter: {
    push: vi.fn()
  }
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    pageProductContractReleaseBatches: mockPageProductContractReleaseBatches,
    getProductContractReleaseBatchImpact: mockGetProductContractReleaseBatchImpact,
    compareProductModelGovernance: mockCompareProductModelGovernance,
    applyProductModelGovernance: mockApplyProductModelGovernance,
    rollbackProductContractReleaseBatch: mockRollbackProductContractReleaseBatch,
    resubmitProductGovernanceApproval: mockResubmitProductGovernanceApproval,
    updateProduct: mockUpdateProduct,
    addProductModel: vi.fn(),
    updateProductModel: mockUpdateProductModel,
    deleteProductModel: mockDeleteProductModel
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    listDeviceRelations: mockListDeviceRelations
  }
}))

vi.mock('@/api/riskGovernance', () => ({
  pageRiskMetricCatalogs: mockPageRiskMetricCatalogs,
  getRiskGovernanceReleaseBatchDiff: mockGetRiskGovernanceReleaseBatchDiff
}))

vi.mock('@/api/governanceApproval', () => ({
  governanceApprovalApi: {
    getOrderDetail: mockGetGovernanceApprovalOrderDetail,
    resubmitOrder: mockResubmitGovernanceApprovalOrder
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  }
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      class="standard-button-stub"
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const CompareTableStub = defineComponent({
  name: 'ProductModelGovernanceCompareTable',
  props: ['rows', 'decisionState'],
  emits: ['change-decision'],
  setup(props, { emit }) {
    return () =>
      h('section', { class: 'compare-table-stub' }, [
        h('span', `rows:${props.rows?.length ?? 0}`),
        h(
          'button',
          {
            type: 'button',
            'data-testid': 'compare-table-stub-select',
            onClick: () => emit('change-decision', { key: 'property:value', decision: 'create' })
          },
          'select'
        )
      ])
  }
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type', 'rows', 'placeholder'],
  emits: ['update:modelValue', 'blur'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      class="el-input-stub el-input-stub--textarea"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="$emit('blur')"
    />
    <input
      v-else
      class="el-input-stub"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="$emit('blur')"
    />
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function findApplyButton(wrapper: ReturnType<typeof mountWorkspace>) {
  return wrapper.findAll('button').find((button) => button.text().includes('确认并提交审批'))
}

function mountWorkspace(productOverrides?: Partial<{
  id: number
  productKey: string
  productName: string
  protocolCode: string
  nodeType: number
  deviceCount: number
  metadataJson: string | null
}>){
  return mount(ProductModelDesignerWorkspace, {
    props: {
      product: {
        id: 1001,
        productKey: 'south-crack-sensor-v1',
        productName: '南方裂缝传感器',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        deviceCount: 3,
        metadataJson: null,
        ...productOverrides
      }
    },
    attachTo: document.body,
    global: {
      stubs: {
        StandardButton: StandardButtonStub,
        ProductModelGovernanceCompareTable: CompareTableStub,
        ElInput: ElInputStub,
        ElTag: true
      }
    }
  })
}

describe('ProductModelDesignerWorkspace', () => {
  beforeEach(() => {
    mockListProductModels.mockReset()
    mockPageProductContractReleaseBatches.mockReset()
    mockPageRiskMetricCatalogs.mockReset()
    mockGetRiskGovernanceReleaseBatchDiff.mockReset()
    mockGetProductContractReleaseBatchImpact.mockReset()
    mockCompareProductModelGovernance.mockReset()
    mockApplyProductModelGovernance.mockReset()
    mockUpdateProduct.mockReset()
    mockUpdateProductModel.mockReset()
    mockRollbackProductContractReleaseBatch.mockReset()
    mockDeleteProductModel.mockReset()
    mockResubmitProductGovernanceApproval.mockReset()
    mockGetGovernanceApprovalOrderDetail.mockReset()
    mockResubmitGovernanceApprovalOrder.mockReset()
    mockListDeviceRelations.mockReset()
    mockRouter.push.mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.warning).mockReset()

    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 2001,
          modelType: 'property',
          identifier: 'value',
          modelName: '裂缝值',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: 'mm'
          }),
          description: '正式字段'
        }
      ]
    })
    mockPageProductContractReleaseBatches.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 1,
        records: [{ id: 99001 }]
      }
    })
    mockCompareProductModelGovernance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        summary: {},
        compareRows: [
          {
            modelType: 'property',
            identifier: 'value',
            compareStatus: 'double_aligned',
            suggestedAction: '纳入新增',
            riskFlags: [],
            suspectedMatches: [],
            manualCandidate: {
              modelType: 'property',
              identifier: 'value',
              modelName: '裂缝值',
              dataType: 'double'
            }
          }
        ]
      }
    })
    mockGetProductContractReleaseBatchImpact.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        batchId: 99001,
        addedCount: 1,
        removedCount: 2,
        changedCount: 3,
        unchangedCount: 4,
        dependencySummary: {
          affectedRiskMetricCount: 2,
          affectedRiskPointBindingCount: 3,
          affectedRuleCount: 1,
          affectedLinkageBindingCount: 1,
          affectedEmergencyPlanBindingCount: 1,
          affectedRiskMetrics: [
            {
              riskMetricId: 6101,
              contractIdentifier: 'value',
              normativeIdentifier: 'value',
              riskMetricCode: 'metric.value',
              riskMetricName: '裂缝值',
              metricRole: 'MEASURE',
              lifecycleStatus: 'ACTIVE'
            }
          ],
          affectedRiskPointBindings: [
            {
              bindingId: 7101,
              riskPointId: 5101,
              riskPointName: '北坡风险点',
              deviceId: 3101,
              deviceCode: 'DEVICE-3101',
              deviceName: '北坡设备',
              riskMetricId: 6101,
              metricIdentifier: 'value',
              metricName: '裂缝值'
            }
          ],
          affectedRules: [
            {
              ruleId: 8101,
              ruleName: '裂缝值红色阈值',
              riskMetricId: 6101,
              metricIdentifier: 'value',
              metricName: '裂缝值',
              alarmLevel: 'red'
            }
          ],
          affectedLinkageBindings: [
            {
              bindingId: 8201,
              linkageRuleId: 8301,
              linkageRuleName: '裂缝值联动',
              riskMetricId: 6101,
              bindingStatus: 'ACTIVE'
            }
          ],
          affectedEmergencyPlanBindings: [
            {
              bindingId: 8401,
              emergencyPlanId: 8501,
              emergencyPlanName: '裂缝值应急预案',
              riskMetricId: 6101,
              bindingStatus: 'ACTIVE',
              alarmLevel: 'red'
            }
          ]
        },
        impactItems: [
          {
            changeType: 'ADDED',
            modelType: 'property',
            identifier: 'value',
            changedFields: []
          },
          {
            changeType: 'UPDATED',
            modelType: 'property',
            identifier: 'sensor_state',
            changedFields: ['modelName']
          }
        ]
      }
    })
    mockPageRiskMetricCatalogs.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 20,
        records: []
      }
    })
    mockGetRiskGovernanceReleaseBatchDiff.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
    mockApplyProductModelGovernance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        createdCount: 1,
        updatedCount: 0,
        skippedCount: 0,
        approvalOrderId: 88001,
        approvalStatus: 'PENDING',
        executionPending: true
      }
    })
    mockUpdateProduct.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1001,
        productKey: 'south-crack-sensor-v1',
        productName: '南方裂缝传感器',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        deviceCount: 3,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'value',
                displayName: '裂缝值',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    })
    mockUpdateProductModel.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 2001,
        modelType: 'property',
        identifier: 'value',
        modelName: '裂缝量',
        dataType: 'double',
        specsJson: JSON.stringify({
          unit: 'mm'
        }),
        description: '正式字段'
      }
    })
    mockRollbackProductContractReleaseBatch.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        targetBatchId: 99001,
        approvalOrderId: 88002,
        approvalStatus: 'PENDING',
        executionPending: true
      }
    })
    mockDeleteProductModel.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
    mockGetGovernanceApprovalOrderDetail.mockImplementation(async (orderId: number) => ({
      code: 200,
      msg: 'success',
      data: {
        order: {
          id: orderId,
          actionCode: orderId === 88002 ? 'PRODUCT_CONTRACT_ROLLBACK' : 'PRODUCT_CONTRACT_RELEASE_APPLY',
          actionName: orderId === 88002 ? '合同回滚' : '合同发布',
          subjectType: 'PRODUCT',
          subjectId: 1001,
          status: 'PENDING',
          operatorUserId: 1001,
          approverUserId: 2002,
          payloadJson: JSON.stringify({
            version: 1,
            request: orderId === 88002 ? { batchId: 99001 } : { productId: 1001, items: [] },
            execution: null
          }),
          approvalComment: null,
          approvedTime: null,
          createTime: '2026-04-08 10:00:00',
          updateTime: '2026-04-08 10:00:00'
        },
        transitions: [
          {
            id: 1,
            fromStatus: null,
            toStatus: 'PENDING',
            actorUserId: 1001,
            transitionComment: 'submit',
            createTime: '2026-04-08 10:00:00'
          }
        ]
      }
    }))
    mockResubmitGovernanceApprovalOrder.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
    mockResubmitProductGovernanceApproval.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
    mockListDeviceRelations.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          parentDeviceCode: 'SK00EA0D1307986',
          logicalChannelCode: 'L1_LF_1',
          childDeviceCode: '202018143',
          relationType: 'collector_child',
          canonicalizationStrategy: 'LF_VALUE',
          statusMirrorStrategy: 'SENSOR_STATE'
        }
      ]
    })
  })

  it('renders the approved single-page contract-field flow and removes drawer-era copy', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('样本输入')
    expect(wrapper.text()).toContain('识别结果')
    expect(wrapper.text()).toContain('本次生效')
    expect(wrapper.text()).toContain('当前已生效字段')
    expect(wrapper.text()).toContain('样本类型')
    expect(wrapper.text()).toContain('设备结构')
    expect(wrapper.text()).toContain('业务数据')
    expect(wrapper.text()).toContain('状态数据')
    expect(wrapper.text()).toContain('单台设备')
    expect(wrapper.text()).toContain('复合设备')
    expect(wrapper.text()).toContain('提取契约字段')
    expect(wrapper.find('[data-testid="contract-field-sample-input"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('自动提炼')
    expect(wrapper.text()).not.toContain('父设备样本归一到子产品')
  })

  it('shows collector boundary note in composite collector mode', async () => {
    const wrapper = mountWorkspace({
      id: 6006,
      productKey: 'nf-monitor-collector-v1',
      productName: '南方测绘 监测型 采集器',
      protocolCode: 'mqtt-json',
      nodeType: 2
    })
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
    await nextTick()

    expect(wrapper.get('[data-testid="collector-boundary-note"]').text()).toContain('采集器产品只治理自身状态字段')
    expect(wrapper.get('[data-testid="collector-boundary-note"]').text()).toContain('子设备字段请到子产品治理')
  })

  it('renders collector empty guidance when compare result is empty in composite collector mode', async () => {
    mockCompareProductModelGovernance.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 6006,
        summary: {},
        compareRows: []
      }
    })

    const wrapper = mountWorkspace({
      id: 6006,
      productKey: 'nf-monitor-collector-v1',
      productName: '南方测绘 监测型 采集器',
      protocolCode: 'mqtt-json',
      nodeType: 2
    })
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="composite-parent-device-code"]').setValue('SK00EA0D1307988')
    await wrapper.get('input[data-testid^="relation-logical-"]').setValue('L1_LF_1')
    await wrapper.get('input[data-testid^="relation-child-"]').setValue('202018108')
    await wrapper
      .get('[data-testid="contract-field-sample-input"]')
      .setValue('{"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.get('[data-testid="collector-boundary-empty"]').text()).toContain('子设备字段请到子产品治理')
    expect(wrapper.text()).toContain('采集器页只治理自身字段；子设备字段请到子产品治理后再确认并提交审批')
    expect(findApplyButton(wrapper)?.attributes('disabled')).toBeDefined()
  })

  it('loads release history and shows risk metric catalog rows for the selected release batch', async () => {
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 99002,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'RELEASED',
            releasedFieldCount: 2,
            createTime: '2026-04-10 18:00:00'
          },
          {
            id: 99001,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'ROLLED_BACK',
            releasedFieldCount: 1,
            createTime: '2026-04-09 18:00:00'
          }
        ]
      }
    })
    mockPageRiskMetricCatalogs.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 9101,
            releaseBatchId: 99002,
            contractIdentifier: 'value',
            riskMetricName: '裂缝监测值',
            metricRole: 'PRIMARY',
            lifecycleStatus: 'ACTIVE'
          }
        ]
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(mockPageRiskMetricCatalogs).toHaveBeenCalledWith({
      productId: 1001,
      releaseBatchId: 99002,
      pageNum: 1,
      pageSize: 20
    })
    expect(wrapper.text()).toContain('版本台账')
    expect(wrapper.text()).toContain('批次 99002')
    expect(wrapper.text()).toContain('裂缝监测值')
  })

  it('shows explicit downstream governance steps after contract release and routes to risk binding and threshold policy', async () => {
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 99002,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'RELEASED',
            releasedFieldCount: 2,
            createTime: '2026-04-10 18:00:00'
          }
        ]
      }
    })
    mockPageRiskMetricCatalogs.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 9101,
            releaseBatchId: 99002,
            contractIdentifier: 'value',
            riskMetricName: '裂缝监测值',
            metricRole: 'PRIMARY',
            lifecycleStatus: 'ACTIVE'
          }
        ]
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('治理下一步')
    expect(wrapper.text()).toContain('目录发布已随合同批次同步')
    expect(wrapper.text()).toContain('去风险对象中心')
    expect(wrapper.text()).toContain('去阈值策略')

    await wrapper.get('[data-testid="contract-field-next-risk-point"]').trigger('click')
    expect(mockRouter.push).toHaveBeenCalledWith({ path: '/risk-point' })

    mockRouter.push.mockClear()

    await wrapper.get('[data-testid="contract-field-next-rule"]').trigger('click')
    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/rule-definition',
      query: {
        governanceSource: 'task',
        workItemCode: 'PENDING_THRESHOLD_POLICY',
        governanceAction: 'create',
        riskMetricId: 9101,
        metricIdentifier: 'value',
        metricName: '裂缝监测值'
      }
    })
  })

  it('marks downstream governance as not applicable when the latest release batch has no catalog metrics', async () => {
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 99002,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'RELEASED',
            releasedFieldCount: 9,
            createTime: '2026-04-10 18:00:00'
          }
        ]
      }
    })
    mockPageRiskMetricCatalogs.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 20,
        records: []
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('治理下一步')
    expect(wrapper.text()).toContain('当前批次暂无可入目录字段')
    expect(wrapper.text()).toContain('风险点绑定暂不适用')
    expect(wrapper.text()).toContain('阈值策略暂不适用')
    expect(wrapper.find('[data-testid="contract-field-next-risk-point"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="contract-field-next-rule"]').exists()).toBe(false)
  })

  it('loads release batch diff against the previous batch and renders contract and metric deltas', async () => {
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 99002,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'RELEASED',
            releasedFieldCount: 2,
            createTime: '2026-04-10 18:00:00'
          },
          {
            id: 99001,
            scenarioCode: 'phase1-crack',
            releaseStatus: 'ROLLED_BACK',
            releasedFieldCount: 1,
            createTime: '2026-04-09 18:00:00'
          }
        ]
      }
    })
    mockGetRiskGovernanceReleaseBatchDiff.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        baselineBatch: {
          id: 99001,
          releaseStatus: 'ROLLED_BACK'
        },
        targetBatch: {
          id: 99002,
          releaseStatus: 'RELEASED'
        },
        baselineContractFieldCount: 1,
        targetContractFieldCount: 2,
        baselineMetricCount: 1,
        targetMetricCount: 2,
        addedContractCount: 1,
        removedContractCount: 0,
        changedContractCount: 1,
        unchangedContractCount: 0,
        addedMetricCount: 1,
        removedMetricCount: 0,
        changedMetricCount: 1,
        unchangedMetricCount: 0,
        contractDiffItems: [
          {
            changeType: 'ADDED',
            modelType: 'property',
            identifier: 'humidity',
            changedFields: []
          },
          {
            changeType: 'UPDATED',
            modelType: 'property',
            identifier: 'value',
            changedFields: ['modelName']
          }
        ],
        metricDiffItems: [
          {
            changeType: 'ADDED',
            contractIdentifier: 'humidity',
            riskMetricName: '湿度监测值',
            metricRole: 'SECONDARY',
            lifecycleStatus: 'ACTIVE',
            changedFields: []
          },
          {
            changeType: 'UPDATED',
            contractIdentifier: 'value',
            riskMetricName: '裂缝监测值',
            metricRole: 'PRIMARY',
            lifecycleStatus: 'ACTIVE',
            changedFields: ['riskMetricName']
          }
        ]
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(mockGetRiskGovernanceReleaseBatchDiff).toHaveBeenCalledWith({
      baselineBatchId: 99001,
      targetBatchId: 99002
    })
    expect(wrapper.text()).toContain('跨批次差异对账')
    expect(wrapper.text()).toContain('批次 99002 对比 99001')
    expect(wrapper.text()).toContain('humidity')
    expect(wrapper.text()).toContain('湿度监测值')
  })

  it('loads existing device relations and exposes manual mapping rows in composite mode', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
    await nextTick()

    const parentInput = wrapper.get('[data-testid="composite-parent-device-code"]')
    await parentInput.setValue('SK00EA0D1307986')
    await wrapper.findAll('button').find((button) => button.text().includes('读取已有关系'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockListDeviceRelations).toHaveBeenCalledWith('SK00EA0D1307986')
    const logicalInput = wrapper.get('[data-testid="relation-logical-L1_LF_1-202018143"]')
    const childInput = wrapper.get('[data-testid="relation-child-L1_LF_1-202018143"]')
    expect((logicalInput.element as HTMLInputElement).value).toBe('L1_LF_1')
    expect((childInput.element as HTMLInputElement).value).toBe('202018143')
  })

  it('submits loaded relation strategies in composite compare payload', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="composite-parent-device-code"]').setValue('SK00EA0D1307986')
    await wrapper.findAll('button').find((button) => button.text().includes('读取已有关系'))?.trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue(
      '{"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:34:06.000Z":10.86}}}'
    )
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      manualExtract: {
        sampleType: 'business',
        deviceStructure: 'composite',
        samplePayload: '{\n  "SK00EA0D1307986": {\n    "L1_LF_1": {\n      "2026-04-05T20:34:06.000Z": 10.86\n    }\n  }\n}',
        parentDeviceCode: 'SK00EA0D1307986',
        relationMappings: [
          {
            logicalChannelCode: 'L1_LF_1',
            childDeviceCode: '202018143',
            canonicalizationStrategy: 'LF_VALUE',
            statusMirrorStrategy: 'SENSOR_STATE'
          }
        ]
      }
    })
  })

  it('infers deep displacement relation strategies for manual composite rows', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="composite-parent-device-code"]').setValue('SK00FB0D1310195')
    await wrapper.findAll('input').find((input) => input.attributes('placeholder') === '逻辑通道编码')?.setValue('L1_SW_1')
    await wrapper.findAll('input').find((input) => input.attributes('placeholder') === '子设备编码')?.setValue('84330701')
    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue(
      '{"SK00FB0D1310195":{"L1_SW_1":{"2026-04-09T13:53:10.000Z":{"dispsX":-0.0166,"dispsY":-0.0368}}}}'
    )
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      manualExtract: {
        sampleType: 'business',
        deviceStructure: 'composite',
        samplePayload:
          '{\n  "SK00FB0D1310195": {\n    "L1_SW_1": {\n      "2026-04-09T13:53:10.000Z": {\n        "dispsX": -0.0166,\n        "dispsY": -0.0368\n      }\n    }\n  }\n}',
        parentDeviceCode: 'SK00FB0D1310195',
        relationMappings: [
          {
            logicalChannelCode: 'L1_SW_1',
            childDeviceCode: '84330701',
            canonicalizationStrategy: 'LEGACY',
            statusMirrorStrategy: 'SENSOR_STATE'
          }
        ]
      }
    })
  })

  it('can mark a formal property as a measure trend focus metric from the contract-field workspace', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-trend-measure-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProduct).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        metadataJson: expect.stringContaining('"identifier":"value"')
      })
    )
    expect(mockUpdateProduct).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        metadataJson: expect.stringContaining('"group":"measure"')
      })
    )
    expect(wrapper.emitted('product-updated')?.[0]?.[0]).toEqual(
      expect.objectContaining({
        metadataJson: expect.stringContaining('"identifier":"value"')
      })
    )
  })

  it('uses aligned object-insight action names for measure status-event and runtime groups', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('设为监测数据')
    expect(wrapper.text()).toContain('设为状态事件')
    expect(wrapper.text()).toContain('设为运行参数')
    expect(wrapper.text()).not.toContain('设为状态趋势')
  })

  it('can mark a formal property as a status-event focus metric from the contract-field workspace', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-trend-status-event-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProduct).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        metadataJson: expect.stringContaining('"group":"status"')
      })
    )
  })

  it('can delete a formal model from the contract-field workspace', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-delete-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockDeleteProductModel).toHaveBeenCalledWith(1001, 2001)
  })

  it('submits the new manual compare payload and keeps apply on the same page', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      manualExtract: {
        sampleType: 'business',
        deviceStructure: 'single',
        samplePayload: '{\n  "device-001": {\n    "temperature": {\n      "2026-04-05T20:14:06.000Z": 26.5\n    }\n  }\n}'
      }
    })

    expect(wrapper.text()).toContain('已选 1 项，确认后将提交审批')
    expect(wrapper.find('[data-testid="contract-field-apply-receipt"]').exists()).toBe(false)

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await wrapper.findAll('button').find((button) => button.text().includes('确认并提交审批'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockApplyProductModelGovernance).toHaveBeenCalledWith(
      1001,
      { items: expect.any(Array) },
      {}
    )
    expect(wrapper.find('[data-testid="contract-field-apply-receipt"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('本次申请新增')
    expect(wrapper.text()).toContain('审批单')
    expect(wrapper.text()).toContain('88001')
  })

  it('loads apply approval detail and shows pending approval status after submit', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockGetGovernanceApprovalOrderDetail).toHaveBeenCalledWith(88001)
    expect(wrapper.text()).toContain('审批提交回执')
    expect(wrapper.text()).toContain('审批状态')
    expect(wrapper.text()).toContain('待审批')
  })

  it('shows the executed release batch after approval detail reports approved', async () => {
    mockGetGovernanceApprovalOrderDetail.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        order: {
          id: 88001,
          actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
          actionName: '合同发布',
          subjectType: 'PRODUCT',
          subjectId: 1001,
          status: 'APPROVED',
          operatorUserId: 1001,
          approverUserId: 2002,
          payloadJson: JSON.stringify({
            version: 1,
            request: { productId: 1001, items: [] },
            execution: {
              executedAt: '2026-04-08T10:05:00',
              result: {
                createdCount: 1,
                updatedCount: 0,
                skippedCount: 0,
                releaseBatchId: 99009
              }
            }
          }),
          approvalComment: '审批通过',
          approvedTime: '2026-04-08 10:05:00',
          createTime: '2026-04-08 10:00:00',
          updateTime: '2026-04-08 10:05:00'
        },
        transitions: []
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.text()).toContain('发布批次')
    expect(wrapper.text()).toContain('99009')
  })

  it('shows rejected approval comment after loading approval detail', async () => {
    mockGetGovernanceApprovalOrderDetail.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        order: {
          id: 88001,
          actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
          actionName: '合同发布',
          subjectType: 'PRODUCT',
          subjectId: 1001,
          status: 'REJECTED',
          operatorUserId: 1001,
          approverUserId: 2002,
          payloadJson: JSON.stringify({
            version: 1,
            request: { productId: 1001, items: [] },
            execution: null
          }),
          approvalComment: '字段单位缺失',
          approvedTime: null,
          createTime: '2026-04-08 10:00:00',
          updateTime: '2026-04-08 10:03:00'
        },
        transitions: []
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('已驳回')
    expect(wrapper.text()).toContain('字段单位缺失')
  })

  it('renames formal fields inline and persists the updated chinese model name', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-rename-2001"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="formal-model-name-input-2001"]').setValue('裂缝量')
    await wrapper.get('[data-testid="formal-model-name-save-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProductModel).toHaveBeenCalledWith(1001, 2001, expect.objectContaining({
      modelType: 'property',
      identifier: 'value',
      modelName: '裂缝量',
      dataType: 'double'
    }))
    expect(wrapper.text()).toContain('裂缝量')
  })

  it('saves formal property unit together with renamed display name', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-rename-2001"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="formal-model-name-input-2001"]').setValue('裂缝量')
    await wrapper.get('[data-testid="formal-model-unit-input-2001"]').setValue('cm')
    await wrapper.get('[data-testid="formal-model-name-save-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProductModel).toHaveBeenCalledWith(1001, 2001, expect.objectContaining({
      modelType: 'property',
      identifier: 'value',
      modelName: '裂缝量',
      specsJson: expect.stringContaining('"unit":"cm"')
    }))
  })

  it('sends specsJson as null when clearing the formal property unit', async () => {
    mockUpdateProductModel.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 2001,
        modelType: 'property',
        identifier: 'value',
        modelName: '裂缝值',
        dataType: 'double',
        specsJson: null,
        description: '正式字段'
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-rename-2001"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="formal-model-unit-input-2001"]').setValue('')
    await wrapper.get('[data-testid="formal-model-name-save-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProductModel).toHaveBeenCalledWith(1001, 2001, expect.objectContaining({
      modelType: 'property',
      identifier: 'value',
      modelName: '裂缝值',
      specsJson: null
    }))
  })

  it('syncs renamed formal field names into existing object-insight trend config', async () => {
    mockUpdateProduct.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 1001,
        productKey: 'south-crack-sensor-v1',
        productName: '南方裂缝传感器',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        deviceCount: 3,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'value',
                displayName: '裂缝量',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    })

    const wrapper = mountWorkspace({
      metadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'value',
              displayName: '裂缝值',
              group: 'measure',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true,
              sortNo: 10
            }
          ]
        }
      })
    })
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-rename-2001"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="formal-model-name-input-2001"]').setValue('裂缝量')
    await wrapper.get('[data-testid="formal-model-name-save-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockUpdateProduct).toHaveBeenCalledWith(1001, expect.objectContaining({
      metadataJson: expect.stringContaining('"displayName":"裂缝量"')
    }))
  })

  it('does not add a second toast when handled object-insight sync errors happen after rename save', async () => {
    mockUpdateProduct.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', true, 500))

    const wrapper = mountWorkspace({
      metadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'value',
              displayName: '裂缝值',
              group: 'measure',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true,
              sortNo: 10
            }
          ]
        }
      })
    })
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="formal-model-rename-2001"]').trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="formal-model-name-input-2001"]').setValue('裂缝量')
    await wrapper.get('[data-testid="formal-model-name-save-2001"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(ElMessage.success).toHaveBeenCalledWith('正式字段名称与单位已更新')
    expect(ElMessage.warning).not.toHaveBeenCalled()
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('resubmits the rejected apply approval order through the product governance endpoint', async () => {
    mockGetGovernanceApprovalOrderDetail
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          order: {
            id: 88001,
            actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
            actionName: '合同发布',
            subjectType: 'PRODUCT',
            subjectId: 1001,
            status: 'REJECTED',
            operatorUserId: 1001,
            approverUserId: 2002,
            payloadJson: JSON.stringify({
              version: 1,
              request: { productId: 1001, items: [] },
              execution: null
            }),
            approvalComment: '字段单位缺失',
            approvedTime: null,
            createTime: '2026-04-08 10:00:00',
            updateTime: '2026-04-08 10:03:00'
          },
          transitions: []
        }
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          order: {
            id: 88001,
            actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
            actionName: '合同发布',
            subjectType: 'PRODUCT',
            subjectId: 1001,
            status: 'PENDING',
            operatorUserId: 1001,
            approverUserId: 99000001,
            payloadJson: JSON.stringify({
              version: 1,
              request: { productId: 1001, items: [] },
              execution: null
            }),
            approvalComment: '系统固定复核人',
            approvedTime: null,
            createTime: '2026-04-08 10:00:00',
            updateTime: '2026-04-08 10:06:00'
          },
          transitions: []
        }
      })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.findAll('button').find((button) => button.text().includes('原单重提'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockResubmitProductGovernanceApproval).toHaveBeenCalledWith(88001)
    expect(mockResubmitGovernanceApprovalOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('待审批')
  })

  it('clears rejected approval receipt so the user can create a new approval order', async () => {
    mockGetGovernanceApprovalOrderDetail.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        order: {
          id: 88001,
          actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
          actionName: '合同发布',
          subjectType: 'PRODUCT',
          subjectId: 1001,
          status: 'REJECTED',
          operatorUserId: 1001,
          approverUserId: 2002,
          payloadJson: JSON.stringify({
            version: 1,
            request: { productId: 1001, items: [] },
            execution: null
          }),
          approvalComment: '字段单位缺失',
          approvedTime: null,
          createTime: '2026-04-08 10:00:00',
          updateTime: '2026-04-08 10:03:00'
        },
        transitions: []
      }
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.findAll('button').find((button) => button.text().includes('修改内容后新建审批'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.find('[data-testid="contract-field-apply-receipt"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="contract-field-apply-approval-status"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('样本输入')
  })

  it('keeps core contract sections while removing redundant workflow and history shell copy', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('样本输入')
    expect(wrapper.text()).toContain('识别结果')
    expect(wrapper.text()).toContain('当前已生效字段')
    expect(wrapper.text()).not.toContain('待输入样本')
    expect(wrapper.text()).not.toContain('待确认识别结果')
    expect(wrapper.text()).not.toContain('待提交审批')
    expect(wrapper.text()).not.toContain('审批中')
    expect(wrapper.text()).not.toContain('已发布 / 可回滚')
    expect(wrapper.text()).not.toContain('发布批次与风险联动')
  })

  it('submits apply without rendering a manual governance approver input', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.find('[data-testid="governance-approver-id"]').exists()).toBe(false)
    expect(mockApplyProductModelGovernance).toHaveBeenCalledWith(
      1001,
      { items: expect.any(Array) },
      {}
    )
  })

  it('sends rollback request without a manual governance approver id', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-rollback-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockRollbackProductContractReleaseBatch).toHaveBeenCalledWith(99001)
    expect(wrapper.text()).toContain('回滚审批已提交')
    expect(wrapper.text()).toContain('88002')
  })

  it('loads rollback preview from the latest release batch impact', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(mockGetProductContractReleaseBatchImpact).toHaveBeenCalledWith(99001)
    expect(wrapper.text()).toContain('回滚试算')
    expect(wrapper.text()).toContain('将删除 1')
    expect(wrapper.text()).toContain('将恢复 2')
    expect(wrapper.text()).toContain('将回退 3')
    expect(wrapper.text()).toContain('受影响风险指标 2')
    expect(wrapper.text()).toContain('受影响风险点绑定 3')
    expect(wrapper.text()).toContain('受影响阈值规则 1')
    expect(wrapper.text()).toContain('value')
    expect(wrapper.text()).toContain('sensor_state')
  })

  it('renders object-level dependency detail groups in rollback preview', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('裂缝值')
    expect(wrapper.text()).toContain('北坡风险点')
    expect(wrapper.text()).toContain('裂缝值红色阈值')
    expect(wrapper.text()).toContain('裂缝值联动')
    expect(wrapper.text()).toContain('裂缝值应急预案')
  })

  it('disables confirm apply after a successful activation to prevent duplicate submissions', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()

    const applyButton = findApplyButton(wrapper)
    expect(applyButton).toBeTruthy()
    await applyButton?.trigger('click')
    await flushPromises()
    await nextTick()

    const refreshedApplyButton = findApplyButton(wrapper)
    expect(mockApplyProductModelGovernance).toHaveBeenCalledTimes(1)
    expect(refreshedApplyButton?.attributes('disabled')).toBeDefined()
  })

  it('keeps the workspace visible when apply fails instead of switching to the load failure notice', async () => {
    mockApplyProductModelGovernance.mockReset()
    mockApplyProductModelGovernance.mockRejectedValue(new Error('系统繁忙，请稍后重试！'))

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="contract-field-sample-input"]').setValue('{"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}')
    await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('样本输入')
    expect(wrapper.text()).toContain('本次生效')
    expect(wrapper.text()).not.toContain('加载失败')
  })
})
