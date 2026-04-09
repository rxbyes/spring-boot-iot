import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import ProductObjectInsightConfigEditor from '@/components/product/ProductObjectInsightConfigEditor.vue'

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

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <label class="el-input-stub">
      <span>{{ modelValue }}</span>
      <input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />
    </label>
  `
})

const ElInputNumberStub = defineComponent({
  name: 'ElInputNumber',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-input-number-stub"
      type="number"
      :value="modelValue"
      @input="$emit('update:modelValue', Number($event.target.value))"
    />
  `
})

const ElSelectStub = defineComponent({
  name: 'ElSelect',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <select class="el-select-stub" :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
      <slot />
    </select>
  `
})

const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: ['label', 'value'],
  template: '<option :value="value">{{ label }}</option>'
})

const ElSwitchStub = defineComponent({
  name: 'ElSwitch',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-switch-stub"
      type="checkbox"
      :checked="Boolean(modelValue)"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
  `
})

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  template: '<div class="el-alert-stub"><slot /></div>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  setup(_, { slots }) {
    return () => h('div', { class: 'el-form-item-stub' }, slots.default?.())
  }
})

describe('ProductObjectInsightConfigEditor', () => {
  it('adds and removes custom metric rows through v-model updates', async () => {
    const updateSpy = vi.fn()
    const wrapper = mount(ProductObjectInsightConfigEditor, {
      props: {
        modelValue: [],
        'onUpdate:modelValue': updateSpy
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          ElInput: ElInputStub,
          ElInputNumber: ElInputNumberStub,
          ElSelect: ElSelectStub,
          ElOption: ElOptionStub,
          ElSwitch: ElSwitchStub,
          ElAlert: ElAlertStub,
          ElTag: ElTagStub,
          ElFormItem: ElFormItemStub
        }
      }
    })

    await wrapper.get('[data-testid="product-object-insight-add"]').trigger('click')

    expect(updateSpy).toHaveBeenCalledWith([
      expect.objectContaining({
        identifier: '',
        displayName: '',
        group: 'status',
        analysisTag: '系统自定义参数'
      })
    ])

    const removeSpy = vi.fn()
    const removeWrapper = mount(ProductObjectInsightConfigEditor, {
      props: {
        modelValue: [
          {
            identifier: 'S1_ZT_1.humidity',
            displayName: '相对湿度',
            group: 'status',
            includeInTrend: true,
            includeInExtension: true,
            analysisTitle: '现场环境补充',
            analysisTag: '系统自定义参数',
            analysisTemplate: '{{label}}当前为{{value}}',
            enabled: true,
            sortNo: 10
          }
        ],
        'onUpdate:modelValue': removeSpy
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          ElInput: ElInputStub,
          ElInputNumber: ElInputNumberStub,
          ElSelect: ElSelectStub,
          ElOption: ElOptionStub,
          ElSwitch: ElSwitchStub,
          ElAlert: ElAlertStub,
          ElTag: ElTagStub,
          ElFormItem: ElFormItemStub
        }
      }
    })

    expect(removeWrapper.findAll('.product-object-insight-config-editor__metric-card')).toHaveLength(1)

    await removeWrapper.get('[data-testid="product-object-insight-remove-0"]').trigger('click')

    expect(removeSpy).toHaveBeenCalledWith([])
  })

  it('quick-adds formal property models as trend focus metrics', async () => {
    const updateSpy = vi.fn()
    const wrapper = mount(ProductObjectInsightConfigEditor, {
      props: {
        modelValue: [],
        availableModels: [
          {
            id: 11,
            productId: 1001,
            modelType: 'property',
            identifier: 'L1_LF_1.value',
            modelName: '裂缝量',
            dataType: 'double',
            sortNo: 6
          }
        ],
        'onUpdate:modelValue': updateSpy
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub,
          ElInput: ElInputStub,
          ElInputNumber: ElInputNumberStub,
          ElSelect: ElSelectStub,
          ElOption: ElOptionStub,
          ElSwitch: ElSwitchStub,
          ElAlert: ElAlertStub,
          ElTag: ElTagStub,
          ElFormItem: ElFormItemStub
        }
      }
    })

    await wrapper.get('[data-testid="product-object-insight-add-measure-L1_LF_1.value"]').trigger('click')

    expect(updateSpy).toHaveBeenCalledWith([
      expect.objectContaining({
        identifier: 'L1_LF_1.value',
        displayName: '裂缝量',
        group: 'measure',
        includeInTrend: true,
        includeInExtension: false,
        enabled: true,
        sortNo: 6
      })
    ])
  })
})
