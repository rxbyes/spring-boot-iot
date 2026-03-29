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

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="iot-access-page-shell-stub">
      <nav class="iot-access-page-shell-stub__breadcrumbs">
        <span v-for="item in breadcrumbs || []" :key="item.label">{{ item.label }}</span>
      </nav>
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
        IotAccessPageShell: IotAccessPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        StandardInlineState: StandardInlineStateStub
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

  it('renders the device page inside the two-level access shell', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.iot-access-page-shell-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('接入智维')
    expect(wrapper.text()).toContain('设备资产中心')
    expect(wrapper.text()).toContain('设备台账')
    expect(wrapper.text()).toContain('DEVICE ASSET')
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
})
