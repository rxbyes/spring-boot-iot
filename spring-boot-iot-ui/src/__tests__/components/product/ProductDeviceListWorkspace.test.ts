import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  template: '<div class="device-row-actions-stub">操作</div>'
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  props: ['currentPage', 'pageSize', 'total'],
  emits: ['update:current-page', 'update:page-size', 'current-change', 'size-change'],
  template: `
    <section class="device-pagination-stub">
      <span class="device-pagination-stub__page">{{ currentPage }}</span>
      <span class="device-pagination-stub__size">{{ pageSize }}</span>
      <span class="device-pagination-stub__total">{{ total }}</span>
      <button class="device-pagination-stub__next" type="button" @click="$emit('current-change', 2)">next</button>
      <button class="device-pagination-stub__resize" type="button" @click="$emit('size-change', 20)">resize</button>
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  template: '<section class="product-device-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  template: `
    <section class="product-device-table-column-stub" :data-label="label">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

describe('ProductDeviceListWorkspace', () => {
  it('shows the device list workspace with visible heading and table actions', () => {
    const wrapper = mount(ProductDeviceListWorkspace, {
      props: {
        devices: [
          {
            id: 2001,
            deviceName: '一号终端',
            deviceCode: 'device-001',
            onlineStatus: 1,
            activateStatus: 1,
            firmwareVersion: 'v1.0.0',
            lastReportTime: '2026-03-30T10:00:00'
          }
        ]
      },
      global: {
        stubs: {
          ElTag: true,
          ElTable: ElTableStub,
          ElTableColumn: ElTableColumnStub,
          StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
          StandardPagination: StandardPaginationStub
        }
      }
    })

    expect(wrapper.find('.device-workspace__registry-heading').text()).toContain('设备清单')
    expect(wrapper.text()).not.toContain('设备总数')
    expect(wrapper.text()).not.toContain('在线设备')
    expect(wrapper.text()).not.toContain('离线设备')
    expect(wrapper.text()).not.toContain('在线比例')
    expect(wrapper.find('.device-workspace__registry-heading').text()).not.toContain('设备清册')
    expect(wrapper.find('.device-workspace__table-shell').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)
    expect(wrapper.find('.device-row-actions-stub').exists()).toBe(true)
  })

  it('renders shared pagination and emits page changes upward', async () => {
    const wrapper = mount(ProductDeviceListWorkspace, {
      props: {
        devices: [
          {
            id: 2001,
            deviceName: '一号终端',
            deviceCode: 'device-001',
            onlineStatus: 1,
            activateStatus: 1,
            firmwareVersion: 'v1.0.0',
            lastReportTime: '2026-03-30T10:00:00'
          }
        ],
        pagination: {
          pageNum: 1,
          pageSize: 10,
          total: 25
        }
      },
      global: {
        stubs: {
          ElTag: true,
          ElTable: ElTableStub,
          ElTableColumn: ElTableColumnStub,
          StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
          StandardPagination: StandardPaginationStub
        }
      }
    })

    expect(wrapper.find('.device-pagination-stub').exists()).toBe(true)

    await wrapper.get('.device-pagination-stub__next').trigger('click')
    expect(wrapper.emitted('page-change')).toEqual([[2]])

    await wrapper.get('.device-pagination-stub__resize').trigger('click')
    expect(wrapper.emitted('page-size-change')).toEqual([[20]])
  })
})
