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
          slots.default?.({ row: { deviceCode: 'D-001' } }) ?? []
        )
      ])
  }
})

describe('StandardTableTextColumn', () => {
  it('passes column props without creating custom slots when none are provided', () => {
    const wrapper = mount(StandardTableTextColumn, {
      props: {
        prop: 'deviceCode',
        label: '设备编码'
      },
      global: {
        stubs: {
          'el-table-column': ElTableColumnStub
        }
      }
    })

    expect(wrapper.find('.el-table-column-stub').attributes('data-prop')).toBe('deviceCode')
    expect(wrapper.find('.el-table-column-stub__header').text()).toBe('')
    expect(wrapper.find('.el-table-column-stub__default').text()).toBe('')
    expect(wrapper.find('.el-table-column-stub__header').attributes('data-tooltip')).toContain('top-start')
  })

  it('renders header and default slots through a stable slot object', () => {
    const wrapper = mount(StandardTableTextColumn, {
      props: {
        prop: 'deviceCode',
        label: '设备编码'
      },
      slots: {
        header: '<span class="custom-header">自定义表头</span>',
        default: '<span class="custom-cell">自定义单元格</span>'
      },
      global: {
        stubs: {
          'el-table-column': ElTableColumnStub
        }
      }
    })

    expect(wrapper.find('.custom-header').text()).toBe('自定义表头')
    expect(wrapper.find('.custom-cell').text()).toBe('自定义单元格')
  })
})
