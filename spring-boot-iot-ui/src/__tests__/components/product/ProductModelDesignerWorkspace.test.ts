import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'

const {
  mockListProductModels,
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance,
  mockListDeviceRelations
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn(),
  mockListDeviceRelations: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    compareProductModelGovernance: mockCompareProductModelGovernance,
    applyProductModelGovernance: mockApplyProductModelGovernance,
    addProductModel: vi.fn(),
    updateProductModel: vi.fn(),
    deleteProductModel: vi.fn()
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    listDeviceRelations: mockListDeviceRelations
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
  return wrapper.findAll('button').find((button) => button.text().includes('确认并生效'))
}

function mountWorkspace(productOverrides?: Partial<{
  id: number
  productKey: string
  productName: string
  protocolCode: string
  nodeType: number
  deviceCount: number
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
    mockCompareProductModelGovernance.mockReset()
    mockApplyProductModelGovernance.mockReset()
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
        skippedCount: 0
      }
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

    expect(wrapper.text()).toContain('已选 1 项，确认后将写入正式字段')
    expect(wrapper.find('[data-testid="contract-field-apply-receipt"]').exists()).toBe(false)

    await wrapper.get('[data-testid="compare-table-stub-select"]').trigger('click')
    await nextTick()
    await wrapper.findAll('button').find((button) => button.text().includes('确认并生效'))?.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockApplyProductModelGovernance).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="contract-field-apply-receipt"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('本次新增生效')
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
