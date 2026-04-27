import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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

  it('binds toolbar meta typography to the shared quiet hierarchy tokens', () => {
    const source = readFileSync(
      resolve(import.meta.dirname, '../../components/StandardTableToolbar.vue'),
      'utf8'
    )

    expect(source).toContain('var(--type-toolbar-meta-size)')
    expect(source).toContain('var(--type-label-size)')
    expect(source).toContain('var(--font-letter-spacing-wide)')
  })

  it('uses the lighter workbench toolbar band instead of a secondary floating card', () => {
    const globalCss = readFileSync(
      resolve(import.meta.dirname, '../../styles/global.css'),
      'utf8'
    )

    expect(globalCss).toContain('.table-action-bar {')
    expect(globalCss).toContain('background: transparent;')
    expect(globalCss).toContain('border: 0;')
    expect(globalCss).toContain('.standard-table-toolbar--compact .table-action-bar__right {')
    expect(globalCss).toContain('margin-inline-start: auto;')
    expect(globalCss).toContain('@media (max-width: 960px)')
    expect(globalCss).toContain('justify-content: flex-start;')
  })
})
