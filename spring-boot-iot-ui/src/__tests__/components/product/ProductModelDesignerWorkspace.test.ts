import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'

const {
  mockListProductModels,
  mockListProductModelCandidates
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockListProductModelCandidates: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    listProductModelCandidates: mockListProductModelCandidates,
    manualExtractProductModelCandidates: vi.fn(),
    confirmProductModelCandidates: vi.fn(),
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

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle'],
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

const ProductModelDesignerDrawerStub = defineComponent({
  name: 'ProductModelDesignerDrawer',
  props: ['modelValue', 'product'],
  template: '<section class="product-model-designer-drawer-stub" />'
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
  return undefined
}

function mountWorkspace() {
  return mount(ProductModelDesignerWorkspace, {
    props: {
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
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        ProductModelDesignerDrawer: ProductModelDesignerDrawerStub,
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

function findButtonByText(wrapper: ReturnType<typeof mountWorkspace>, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

describe('ProductModelDesignerWorkspace', () => {
  beforeEach(() => {
    mockListProductModels.mockReset()
    mockListProductModelCandidates.mockReset()
    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
  })

  it('loads manual extraction guidance in a government-brief workspace without side-rail clutter', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    expect(mockListProductModelCandidates).not.toHaveBeenCalled()
    expect(wrapper.find('.product-model-designer__hero-stage').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__brief-head').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__header').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__header-kicker').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__headline').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__brief-statement').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__brief-statement').text()).not.toContain('当前判断')
    expect(wrapper.find('.product-model-designer__header-meta').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__brief-strip').exists()).toBe(true)
    expect(wrapper.findAll('.product-model-designer__brief-item')).toHaveLength(3)
    expect(wrapper.find('.product-model-designer__summary-lead').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__workspace-shell').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__candidate-stage').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__candidate-nav').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__flow-strip').exists()).toBe(true)
    expect(wrapper.findAll('.product-model-designer__flow-item')).toHaveLength(3)
    expect(wrapper.find('.product-model-designer__candidate-body-meta').exists()).toBe(false)
    expect(wrapper.find('.product-model-designer__candidate-rail').exists()).toBe(false)
    expect(wrapper.text()).toContain('手动提炼')
    expect(wrapper.text()).toContain('单次只解析一个设备样本')
    expect(wrapper.text()).toContain('业务 / 状态 / 其他')
    expect(wrapper.text()).toContain('进入完整治理')
  })

  it('renders formal mode cards from the same embedded workspace', async () => {
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
          specsJson: '{\"unit\":\"℃\"}',
          sortNo: 10,
          requiredFlag: 1,
          description: '设备温度正式契约'
        }
      ]
    })

    const wrapper = mountWorkspace()
    await flushPromises()
    await nextTick()

    await findButtonByText(wrapper, '正式模型')!.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.product-model-designer__formal-overview').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__formal-stage').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__formal-stage-copy').exists()).toBe(true)
    expect(wrapper.find('.product-model-designer__formal-intro').text()).toContain('正式模型')
    expect(wrapper.text()).toContain('统一维护产品正式物模型')
    expect(wrapper.text()).toContain('温度')
    expect(wrapper.text()).toContain('设备温度正式契约')
    expect(wrapper.find('.product-model-designer__header-meta').exists()).toBe(false)
    expect(mockListProductModelCandidates).not.toHaveBeenCalled()
  })
})
