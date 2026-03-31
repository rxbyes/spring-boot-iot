import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'

const ElFormStub = {
  name: 'ElForm',
  props: ['model'],
  template: '<form class="el-form-stub"><slot /></form>'
}

const ElCollapseTransitionStub = {
  name: 'ElCollapseTransition',
  template: '<div class="el-collapse-transition-stub"><slot /></div>'
}

const ElButtonStub = {
  name: 'ElButton',
  emits: ['click'],
  template: '<button class="el-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
}

const StandardActionGroupStub = {
  name: 'StandardActionGroup',
  template: '<div class="standard-action-group-stub"><slot /></div>'
}

function createPrimaryFields(count: number) {
  return Array.from({ length: count }, (_, index) => (
    `<div class="el-form-item" data-field-index="${index + 1}">字段${index + 1}</div>`
  )).join('')
}

async function flushComponentTicks() {
  await nextTick()
  await nextTick()
}

describe('StandardListFilterHeader', () => {
  it('collapses primary filters after the fourth item by default and supports expand/collapse', async () => {
    const wrapper = mount(StandardListFilterHeader, {
      props: {
        model: {}
      },
      slots: {
        primary: createPrimaryFields(5),
        actions: '<button class="query-button" type="button">查询</button>'
      },
      global: {
        stubs: {
          ElForm: ElFormStub,
          ElCollapseTransition: ElCollapseTransitionStub,
          ElButton: ElButtonStub,
          StandardActionGroup: StandardActionGroupStub
        }
      }
    })

    await flushComponentTicks()

    const getPrimaryFields = () => wrapper.findAll('.el-form-item')
    const primaryFields = getPrimaryFields()
    const toggleButton = wrapper.find('.el-button-stub')
    expect(primaryFields).toHaveLength(5)
    expect(toggleButton.exists()).toBe(true)
    expect(primaryFields[3].classes()).not.toContain('standard-list-filter-header__primary-field--hidden')
    expect(primaryFields[4].classes()).toContain('standard-list-filter-header__primary-field--hidden')
    expect(toggleButton.text()).toContain('展开全部筛选项')

    await toggleButton.trigger('click')
    await flushComponentTicks()

    expect(getPrimaryFields()[4].classes()).not.toContain('standard-list-filter-header__primary-field--hidden')
    expect(wrapper.find('.el-button-stub').text()).toContain('收起筛选项')

    await wrapper.find('.el-button-stub').trigger('click')
    await flushComponentTicks()

    expect(getPrimaryFields()[4].classes()).toContain('standard-list-filter-header__primary-field--hidden')
    expect(wrapper.find('.el-button-stub').text()).toContain('展开全部筛选项')
  })

  it('keeps the toggle hidden when primary filters do not exceed four items', async () => {
    const wrapper = mount(StandardListFilterHeader, {
      props: {
        model: {}
      },
      slots: {
        primary: createPrimaryFields(4)
      },
      global: {
        stubs: {
          ElForm: ElFormStub,
          ElCollapseTransition: ElCollapseTransitionStub,
          ElButton: ElButtonStub,
          StandardActionGroup: StandardActionGroupStub
        }
      }
    })

    await flushComponentTicks()

    expect(wrapper.find('.el-button-stub').exists()).toBe(false)
    expect(
      wrapper.findAll('.el-form-item').some((field) => field.classes().includes('standard-list-filter-header__primary-field--hidden'))
    ).toBe(false)
  })

  it('preserves the advanced toggle event contract when an advanced slot is present', async () => {
    const wrapper = mount(StandardListFilterHeader, {
      props: {
        model: {},
        showAdvancedToggle: true
      },
      slots: {
        primary: createPrimaryFields(5),
        advanced: '<div class="el-form-item">高级条件</div>'
      },
      global: {
        stubs: {
          ElForm: ElFormStub,
          ElCollapseTransition: ElCollapseTransitionStub,
          ElButton: ElButtonStub,
          StandardActionGroup: StandardActionGroupStub
        }
      }
    })

    await flushComponentTicks()
    await wrapper.find('.el-button-stub').trigger('click')

    expect(wrapper.emitted('toggle-advanced')?.[0]).toEqual([])
  })

  it('marks the filter shell as the refined minimal header', async () => {
    const wrapper = mount(StandardListFilterHeader, {
      props: {
        model: {}
      },
      slots: {
        primary: createPrimaryFields(3),
        actions: '<button class="query-button" type="button">查询</button>'
      },
      global: {
        stubs: {
          ElForm: ElFormStub,
          ElCollapseTransition: ElCollapseTransitionStub,
          ElButton: ElButtonStub,
          StandardActionGroup: StandardActionGroupStub
        }
      }
    })

    await flushComponentTicks()

    expect(wrapper.classes()).toContain('standard-list-filter-header--minimal')
    expect(wrapper.find('.standard-list-filter-header__actions-row').classes()).toContain('standard-list-filter-header__actions-row--minimal')
  })

  it('keeps the filter grid and actions row on shared spacing variables', () => {
    const source = readFileSync(
      resolve(import.meta.dirname, '../../components/StandardListFilterHeader.vue'),
      'utf8'
    )

    expect(source).toContain('--ops-filter-grid-gap')
    expect(source).toContain('--ops-filter-actions-gap')
    expect(source).toContain('StandardActionGroup gap="sm"')
  })
})
