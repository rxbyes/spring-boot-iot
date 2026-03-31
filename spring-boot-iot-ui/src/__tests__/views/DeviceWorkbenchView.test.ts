import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DeviceWorkbenchView from '@/views/DeviceWorkbenchView.vue'

const { mockRoute, mockRouter, mockPageDevices } = vi.hoisted(() => ({
  mockRoute: {
    path: '/devices',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageDevices: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: mockPageDevices,
    listDeviceOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
    getDeviceById: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    deleteDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    batchDeleteDevices: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    batchAddDevices: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    replaceDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    updateDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    addDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
  }
}))

vi.mock('@/api/accessError', () => ({
  accessErrorApi: {
    getAccessErrorById: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
  }
}))

vi.mock('@/api/product', () => ({
  productApi: {
    getAllProducts: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
  }
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    hasPermission: () => true
  })
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: vi.fn(),
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

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="device-workbench-panel-stub">
      <p class="device-workbench-panel-stub__eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="device-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="device-workbench-panel-stub__applied"><slot name="applied-filters" /></div>
      <div class="device-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="device-workbench-panel-stub__inline"><slot name="inline-state" /></div>
      <div class="device-workbench-panel-stub__body"><slot /></div>
      <div class="device-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="device-list-filter-header-stub">
      <div class="device-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="device-list-filter-header-stub__advanced"><slot name="advanced" /></div>
      <div class="device-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="device-table-toolbar-stub">
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'gap', 'directItems', 'menuItems'],
  template: `
    <div class="device-workbench-row-actions-stub" :data-variant="variant">
      <span
        v-for="item in directItems || []"
        :key="item.key || item.command"
        class="device-workbench-row-actions-stub__direct"
      >
        {{ item.label }}
      </span>
      <span class="device-workbench-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </div>
  `
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  template: '<div class="el-form-item-stub"><slot /></div>'
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'id'],
  emits: ['update:modelValue', 'clear'],
  template: `
    <input
      :id="id"
      class="el-input-stub"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target && $event.target.value)"
    />
  `
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label'],
  template: '<button class="standard-action-menu-stub" type="button">{{ label || \'更多\' }}</button>'
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['eyebrow', 'title', 'subtitle'],
  template: `
    <section class="device-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['eyebrow', 'title', 'subtitle'],
  template: `
    <section class="device-form-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  template: '<section class="device-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'width', 'className', 'fixed', 'type', 'align', 'showOverflowTooltip'],
  template: `
    <section class="device-table-column-stub" :data-label="label" :data-width="width" :data-class-name="className">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}))
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, value: string) => {
        store.set(key, value)
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key)
      })
    }
  })
}

function mountView() {
  return shallowMount(DeviceWorkbenchView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        StandardInlineState: StandardInlineStateStub,
        StandardButton: StandardButtonStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('DeviceWorkbenchView', () => {
  beforeEach(() => {
    mockRoute.path = '/devices'
    mockRoute.query = {}
    mockRouter.replace.mockReset()
    mockRouter.push.mockReset()
    mockRouter.replace.mockResolvedValue(undefined)
    mockRouter.push.mockResolvedValue(undefined)
    mockPageDevices.mockReset()
    mockPageDevices.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    installSessionStorageMock()
  })

  it('renders the quick-search workbench without missing template bindings', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.device-workbench-panel-stub').exists()).toBe(true)
    expect(wrapper.find('#quick-search').exists()).toBe(true)
    expect(mockPageDevices).toHaveBeenCalled()
    expect(
      warnSpy.mock.calls.some((call) => call.some((item) => String(item).includes('quickSearchKeyword')))
    ).toBe(false)
    expect(
      warnSpy.mock.calls.some((call) => call.some((item) => String(item).includes('handleQuickSearch')))
    ).toBe(false)
    expect(
      errorSpy.mock.calls.some((call) => call.some((item) => String(item).includes('handleQuickSearch')))
    ).toBe(false)

    warnSpy.mockRestore()
    errorSpy.mockRestore()
  })

  it('renders the device page inside the shared governance shell without the legacy eyebrow tier', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    const detailDrawer = wrapper.findComponent(StandardDetailDrawerStub)
    const formDrawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true)
    expect(detailDrawer.props('eyebrow')).toBeUndefined()
    expect(formDrawer.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('设备台账')
    expect(wrapper.text()).not.toContain('DEVICE ASSET')
  })

  it('keeps the device toolbar focused by collapsing secondary actions into a more-actions menu', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('刷新列表')
    expect(wrapper.text()).toContain('更多操作')
    expect(wrapper.text()).not.toContain('批量删除')
    expect(wrapper.text()).not.toContain('导出列设置')
    expect(wrapper.text()).not.toContain('导出选中')
    expect(wrapper.text()).not.toContain('导出当前结果')
    expect(wrapper.text()).not.toContain('清空选中')
  })

  it('shows a compact diagnostic intake hint when opened from access-error', async () => {
    mockRoute.query = {
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      traceId: 'trace-001'
    }
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'access-error',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          traceId: 'trace-001',
          capturedAt: new Date().toISOString()
        }
      })
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('来自失败归档')
    expect(wrapper.text()).toContain('demo-device-01')
  })

  it('reuses the shared workbench row-actions component for registered device cards', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 2001,
        productKey: 'demo-product',
        productName: '演示产品',
        deviceCode: 'demo-device-01',
        deviceName: '演示设备',
        registrationStatus: 1,
        onlineStatus: 1,
        activateStatus: 1,
        deviceStatus: 1,
        nodeType: 1,
        protocolCode: 'mqtt-json',
        createTime: '2026-03-24T09:00:00',
        lastReportTime: '2026-03-24T09:00:00'
      }
    ]
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')
    const tableRowActions = rowActions.find((component) => component.props('variant') === 'table')

    expect(cardRowActions?.exists()).toBe(true)
    expect(tableRowActions?.exists()).toBe(true)
    expect(cardRowActions?.props('gap')).toBe('compact')
    expect(tableRowActions?.props('gap')).toBe('compact')
    expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
        '详情',
        '编辑'
      ])
    expect(((cardRowActions?.props('menuItems') as Array<unknown>) || []).length).toBe(3)

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(String(actionColumn?.props('width'))).toBe('136')
  })

  it('keeps the device workbench on the shared list surface and trims toolbar density', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('standard-mobile-record-grid')
    expect(source).toContain('compact')
    expect(source).toContain('已登记 ${registeredCount} 台')
    expect(source).not.toContain('`未登记 ${unregisteredCount} 台`')
  })
})
