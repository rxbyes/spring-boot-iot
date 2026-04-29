import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: [
    'prop',
    'label',
    'minWidth',
    'width',
    'fixed',
    'align',
    'headerAlign',
    'showOverflowTooltip'
  ],
  setup(props, { slots }) {
    const scope = {
      row: {
        deviceCode: 'D-001',
        deviceName: 'North Slope Gateway',
        productKey: 'demo-product'
      }
    }

    return () =>
      h('div', { class: 'el-table-column-stub', 'data-prop': props.prop as string }, [
        h(
          'div',
          {
            class: 'el-table-column-stub__header',
            'data-tooltip':
              typeof props.showOverflowTooltip === 'object'
                ? JSON.stringify(props.showOverflowTooltip)
                : String(props.showOverflowTooltip)
          },
          slots.header?.({ column: props }) ?? []
        ),
        h(
          'div',
          { class: 'el-table-column-stub__default' },
          slots.default?.(scope) ?? []
        )
      ])
  }
})

describe('StandardTableTextColumn', () => {
  it('passes column props and renders fallback text content when no slots are provided', () => {
    const wrapper = mount(StandardTableTextColumn, {
      props: {
        prop: 'deviceCode',
        label: 'Device Code'
      },
      global: {
        stubs: {
          'el-table-column': ElTableColumnStub
        }
      }
    })

    expect(wrapper.find('.el-table-column-stub').attributes('data-prop')).toBe('deviceCode')
    expect(wrapper.find('.el-table-column-stub__header').text()).toBe('')
    expect(wrapper.find('.el-table-column-stub__default').text()).toContain('D-001')
    expect(wrapper.find('.el-table-column-stub__header').attributes('data-tooltip')).toContain('top-start')
    expect(wrapper.find('.standard-table-text__primary--solo').text()).toBe('D-001')
  })

  it('renders stacked primary and secondary text when a secondary prop is provided', () => {
    const wrapper = mount(StandardTableTextColumn, {
      props: {
        prop: 'deviceName',
        label: 'Device',
        secondaryProp: 'deviceCode'
      },
      global: {
        stubs: {
          'el-table-column': ElTableColumnStub
        }
      }
    })

    expect(wrapper.find('.standard-table-text__primary').text()).toBe('North Slope Gateway')
    expect(wrapper.find('.standard-table-text__secondary').text()).toBe('D-001')
  })

  it('keeps custom default and secondary slots inside the shared text stack', () => {
    const wrapper = mount(StandardTableTextColumn, {
      props: {
        label: 'Product'
      },
      slots: {
        default: '<span class="custom-primary">Demo Product</span>',
        secondary: '<span class="custom-secondary">demo-product</span>'
      },
      global: {
        stubs: {
          'el-table-column': ElTableColumnStub
        }
      }
    })

    expect(wrapper.find('.custom-primary').text()).toBe('Demo Product')
    expect(wrapper.find('.custom-secondary').text()).toBe('demo-product')
    expect(wrapper.find('.standard-table-text__secondary').exists()).toBe(true)
  })
})
