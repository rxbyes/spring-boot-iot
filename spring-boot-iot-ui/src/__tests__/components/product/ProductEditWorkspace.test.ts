import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductEditWorkspace from '@/components/product/ProductEditWorkspace.vue'

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

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub" :data-tone="tone">{{ message }}</div>'
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

describe('ProductEditWorkspace', () => {
  it('renders the edit workspace as a journal revision section with a ruler and draft sheet', async () => {
    const wrapper = mount(ProductEditWorkspace, {
      props: {
        model: {
          productKey: 'south-monitor',
          productName: '南方监测终端',
          protocolCode: 'mqtt-json',
          nodeType: 1,
          dataFormat: 'JSON',
          manufacturer: 'GHLZM',
          description: '南方场站监测终端',
          status: 1
        },
        rules: {
          productKey: [{ required: true, message: '请输入产品 Key', trigger: 'blur' }]
        },
        editing: true,
        submitLoading: false,
        refreshState: 'warning',
        refreshMessage: '最新档案已取回；你已开始编辑，当前未自动覆盖表单。'
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          StandardInlineState: StandardInlineStateStub,
          ElForm: ElFormStub,
          ElFormItem: true,
          ElInput: true,
          ElSelect: true,
          ElOption: true
        }
      }
    })

    expect(wrapper.find('.product-edit-workspace__hero').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__chapter-header').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__headline').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__brief-head').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__revision-head').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__journal-head').exists()).toBe(true)
    expect(wrapper.find('.product-edit-workspace__summary-band').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__section-kicker').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__section-note').text()).toContain('当前变更会直接回写')
    expect(wrapper.find('.product-edit-workspace__brief-band').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__revision-board').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__revision-ruler').exists()).toBe(true)
    expect(wrapper.findAll('.product-edit-workspace__revision-ruler-item')).toHaveLength(3)
    expect(wrapper.find('.product-edit-workspace__revision-ruler-item small').exists()).toBe(false)
    expect(wrapper.find('.product-edit-workspace__notice').exists()).toBe(false)
    expect(wrapper.find('.standard-inline-state-stub').exists()).toBe(true)
    expect(wrapper.find('.product-edit-workspace__draft-sheet').exists()).toBe(true)
    expect(wrapper.find('.product-edit-workspace__form-intro').exists()).toBe(false)
    expect(wrapper.text()).toContain('基础档案')
    expect(wrapper.text()).toContain('接入基线')
    expect(wrapper.text()).toContain('补充说明')
    expect(wrapper.text()).toContain('最新档案已取回')

    await wrapper.get('[data-testid="product-edit-cancel"]').trigger('click')
    await wrapper.get('[data-testid="product-edit-submit"]').trigger('click')

    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(wrapper.emitted('submit')).toHaveLength(1)
  })
})
