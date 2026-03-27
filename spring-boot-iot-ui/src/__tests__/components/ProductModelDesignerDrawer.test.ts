import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerDrawer from '@/components/product/ProductModelDesignerDrawer.vue'

const {
  mockListProductModels,
  mockListProductModelCandidates,
  mockConfirmProductModelCandidates
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockListProductModelCandidates: vi.fn(),
  mockConfirmProductModelCandidates: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    listProductModelCandidates: mockListProductModelCandidates,
    confirmProductModelCandidates: mockConfirmProductModelCandidates,
    addProductModel: vi.fn(),
    updateProductModel: vi.fn(),
    deleteProductModel: vi.fn()
  }
}))

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
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

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="standard-detail-drawer-stub">
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue'],
  emits: ['update:modelValue', 'close'],
  template: `
    <section v-if="modelValue" class="standard-form-drawer-stub">
      <slot />
      <slot name="footer" />
    </section>
  `
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

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  template: '<footer class="standard-drawer-footer-stub"><slot /></footer>'
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button type="button" class="standard-action-link-stub" @click="$emit(\'click\')"><slot /></button>'
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="standard-row-actions-stub"><slot /></div>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElCheckboxStub = defineComponent({
  name: 'ElCheckbox',
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue', 'change'],
  template: `
    <input
      class="el-checkbox-stub"
      type="checkbox"
      :checked="Boolean(modelValue)"
      :disabled="Boolean(disabled)"
      @change="$emit('update:modelValue', $event.target.checked); $emit('change', $event.target.checked)"
    />
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type'],
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      class="el-input-stub el-input-stub--textarea"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      class="el-input-stub"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  setup(_, { slots, expose }) {
    expose({
      validate: () => Promise.resolve(true),
      clearValidate: () => undefined
    })
    return () => h('form', { class: 'el-form-stub' }, slots.default?.())
  }
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function candidateResult() {
  return {
    productId: 1001,
    summary: {
      propertyEvidenceCount: 3,
      propertyCandidateCount: 1,
      eventEvidenceCount: 0,
      eventCandidateCount: 0,
      serviceEvidenceCount: 0,
      serviceCandidateCount: 0,
      needsReviewCount: 0,
      existingModelCount: 0,
      createdCount: 0,
      skippedCount: 0,
      conflictCount: 0,
      eventHint: '暂无真实事件证据，当前产品最近 30 天未发现稳定事件上报。',
      serviceHint: '当前真实库 iot_command_record 字段仍未完全对齐服务标识，暂不自动生成服务模型。',
      lastExtractedAt: '2026-03-27T12:00:00'
    },
    propertyCandidates: [
      {
        modelType: 'property',
        identifier: 'S1_ZT_1.signal_4g',
        modelName: '4G 信号强度',
        dataType: 'integer',
        sortNo: 110,
        requiredFlag: 0,
        description: '归属设备状态属性，用于反映终端运行、联网或传感器状态，不应与业务测点混写。',
        groupKey: 'device_status',
        confidence: 0.93,
        needsReview: false,
        candidateStatus: 'ready',
        evidenceCount: 1,
        messageEvidenceCount: 1,
        lastReportTime: '2026-03-27T11:58:00',
        sourceTables: ['iot_device_property', 'iot_device_message_log']
      }
    ],
    eventCandidates: [],
    serviceCandidates: []
  }
}

function mountDrawer() {
  return mount(ProductModelDesignerDrawer, {
    props: {
      modelValue: true,
      product: {
        id: 1001,
        productKey: 'south-monitor',
        productName: '南方监测终端',
        protocolCode: 'mqtt-json',
        nodeType: 1
      }
    },
    global: {
      stubs: {
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardActionLink: StandardActionLinkStub,
        StandardRowActions: StandardRowActionsStub,
        ElTag: ElTagStub,
        ElCheckbox: ElCheckboxStub,
        ElInput: ElInputStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElSelect: true,
        ElOption: true,
        ElSwitch: true,
        ElInputNumber: true
      }
    }
  })
}

function findButtonByText(wrapper: ReturnType<typeof mountDrawer>, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

describe('ProductModelDesignerDrawer', () => {
  beforeEach(() => {
    mockListProductModels.mockReset()
    mockListProductModelCandidates.mockReset()
    mockConfirmProductModelCandidates.mockReset()
    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListProductModelCandidates.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: candidateResult()
    })
    mockConfirmProductModelCandidates.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        createdCount: 1,
        skippedCount: 0,
        conflictCount: 0
      }
    })
  })

  it('loads candidate evidence and shows honest empty states for event and service', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    expect(mockListProductModelCandidates).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('真实证据概览')
    expect(wrapper.text()).toContain('候选目录')
    expect(wrapper.text()).toContain('4G 信号强度')
    expect(wrapper.text()).toContain('归属设备状态属性')
    expect(wrapper.text()).toContain('暂无真实事件证据')
    expect(wrapper.text()).toContain('iot_command_record')
  })

  it('renders candidate mode as a staged workspace with hero, overview and confirm rail', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.product-model-designer__hero-stage').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__hero-product').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__overview').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__candidate-workspace').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__candidate-rail').exists()).toBe(true)
    expect(wrapper.text()).toContain('基于真实上报提炼产品契约')
    expect(wrapper.text()).toContain('真实证据概览')
  })

  it('renders formal mode with type overview cards and unified model cards', async () => {
    mockListProductModels.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 2001,
          modelType: 'property',
          identifier: 'temperature',
          modelName: '温度',
          dataType: 'decimal',
          specsJson: '{"unit":"℃"}',
          sortNo: 10,
          requiredFlag: 1,
          description: '设备温度正式契约'
        }
      ]
    })

    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    await findButtonByText(wrapper, '正式模型')!.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.product-model-designer__formal-overview').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__formal-stage').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__card-surface').exists()).toBe(true)
    expect(wrapper.text()).toContain('统一维护产品正式物模型')
    expect(wrapper.text()).toContain('温度')
    expect(wrapper.text()).toContain('设备温度正式契约')
  })

  it('falls back to formal models when candidate extraction endpoint is unavailable', async () => {
    mockListProductModelCandidates.mockRejectedValueOnce(new Error('系统繁忙，请稍后重试！'))

    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    expect(mockListProductModels).toHaveBeenCalledWith(1001)
    expect(mockListProductModelCandidates).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('暂无物模型')
    expect(findButtonByText(wrapper, '新增属性模型')).toBeTruthy()
    expect(findButtonByText(wrapper, '确认并写入正式物模型')).toBeUndefined()
  })

  it('confirms selected candidates into formal models', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    const confirmButton = findButtonByText(wrapper, '确认并写入正式物模型')
    expect(confirmButton).toBeTruthy()

    await confirmButton!.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockConfirmProductModelCandidates).toHaveBeenCalledTimes(1)
    expect(mockConfirmProductModelCandidates.mock.calls[0]?.[0]).toBe(1001)
    expect(mockConfirmProductModelCandidates.mock.calls[0]?.[1]).toEqual({
      items: [
        expect.objectContaining({
          identifier: 'S1_ZT_1.signal_4g',
          modelName: '4G 信号强度',
          dataType: 'integer'
        })
      ]
    })
  })
})
