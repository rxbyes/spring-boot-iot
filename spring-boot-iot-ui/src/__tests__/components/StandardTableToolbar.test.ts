import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardTableToolbar from '@/components/StandardTableToolbar.vue'

describe('StandardTableToolbar', () => {
  it('uses the refined toolbar shell and keeps meta items in the quiet-info rail', () => {
    const wrapper = mount(StandardTableToolbar, {
      props: {
        metaItems: ['已选 2 项', '启用 18 个']
      },
      slots: {
        right: '<button type="button" class="toolbar-action">刷新列表</button>'
      }
    })

    expect(wrapper.classes()).toContain('standard-table-toolbar--minimal')
    expect(wrapper.find('.table-action-bar__left').classes()).toContain('standard-table-toolbar__meta-rail')
    expect(wrapper.findAll('.table-action-bar__meta')).toHaveLength(2)
    expect(wrapper.find('.toolbar-action').exists()).toBe(true)
  })
})
