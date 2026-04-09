import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'

const {
  mockListProductModels,
  mockPageProductContractReleaseBatches,
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance,
  mockUpdateProduct,
  mockUpdateProductModel,
  mockRollbackProductContractReleaseBatch,
  mockGetGovernanceApprovalOrderDetail,
  mockResubmitGovernanceApprovalOrder,
  mockListDeviceRelations
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockPageProductContractReleaseBatches: vi.fn(),
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockUpdateProductModel: vi.fn(),
  mockRollbackProductContractReleaseBatch: vi.fn(),
  mockGetGovernanceApprovalOrderDetail: vi.fn(),
  mockResubmitGovernanceApprovalOrder: vi.fn(),
  mockListDeviceRelations: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    pageProductContractReleaseBatches: mockPageProductContractReleaseBatches,
    compareProductModelGovernance: mockCompareProductModelGovernance,
    applyProductModelGovernance: mockApplyProductModelGovernance,
    rollbackProductContractReleaseBatch: mockRollbackProductContractReleaseBatch,
    updateProduct: mockUpdateProduct,
    addProductModel: vi.fn(),
    updateProductModel: mockUpdateProductModel,
    deleteProductModel: vi.fn()
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    listDeviceRelations: mockListDeviceRelations
  }
}))

vi.mock('@/api/governanceApproval', () => ({
  governanceApprovalApi: {
    getOrderDetail: mockGetGovernanceApprovalOrderDetail,
    resubmitOrder: mockResubmitGovernanceApprovalOrder
  }
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
    mockCompareProductModelGovernance.mockReset()
    mockApplyProductModelGovernance.mockReset()
    mockUpdateProduct.mockReset()
    mockUpdateProductModel.mockReset()
    mockRollbackProductContractReleaseBatch.mockReset()
    mockGetGovernanceApprovalOrderDetail.mockReset()
    mockResubmitGovernanceApprovalOrder.mockReset()
    mockListDeviceRelations.mockReset()

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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
    await wrapper.findAll('button').find((button) => button.text().includes('确认并提交审批'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockApplyProductModelGovernance).toHaveBeenCalledWith(
      1001,
      { items: expect.any(Array) },
      { approverUserId: '2002' }
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockGetGovernanceApprovalOrderDetail).toHaveBeenCalledWith(88001)
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
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

  it('resubmits the rejected apply approval order with a new approver', async () => {
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
            approverUserId: 3003,
            payloadJson: JSON.stringify({
              version: 1,
              request: { productId: 1001, items: [] },
              execution: null
            }),
            approvalComment: '重新指定复核人',
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="governance-approver-id"]').setValue('3003')
    await wrapper.findAll('button').find((button) => button.text().includes('原单重提'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockResubmitGovernanceApprovalOrder).toHaveBeenCalledWith(88001, {
      approverUserId: '3003'
    })
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
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

  it('blocks apply when governance approver id is missing', async () => {
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

    expect(mockApplyProductModelGovernance).not.toHaveBeenCalled()
  })

  it('sends rollback request with governance approver id', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
    await wrapper.get('[data-testid="contract-field-rollback-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockRollbackProductContractReleaseBatch).toHaveBeenCalledWith(99001, '2002')
    expect(wrapper.text()).toContain('回滚审批已提交')
    expect(wrapper.text()).toContain('88002')
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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')

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
    await wrapper.get('[data-testid="governance-approver-id"]').setValue('2002')
    await findApplyButton(wrapper)?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('样本输入')
    expect(wrapper.text()).toContain('本次生效')
    expect(wrapper.text()).not.toContain('加载失败')
  })
})
