import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createRequestError } from '@/api/request'
import DeviceWorkbenchView from '@/views/DeviceWorkbenchView.vue'

const { mockRoute, mockRouter, mockPageDevices, mockExportDevices, mockGetDeviceThresholds, mockPermissions, mockDownloadRowsAsCsv } = vi.hoisted(() => ({
  mockRoute: {
    path: '/devices',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageDevices: vi.fn(),
  mockExportDevices: vi.fn(),
  mockGetDeviceThresholds: vi.fn(),
  mockPermissions: new Set<string>([
    'iot:devices:add',
    'iot:devices:update',
    'iot:devices:delete',
    'iot:devices:export',
    'iot:devices:replace'
  ]),
  mockDownloadRowsAsCsv: vi.fn()
}))

function setMockPermissions(...permissions: string[]) {
  mockPermissions.clear()
  permissions.forEach((permission) => mockPermissions.add(permission))
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: mockPageDevices,
    exportDevices: mockExportDevices,
    getDeviceThresholds: mockGetDeviceThresholds,
    listDeviceOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
    getDeviceById: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    getDeviceCapabilities: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    executeDeviceCapability: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    pageDeviceCommands: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: { total: 0, pageNum: 1, pageSize: 10, records: [] } }),
    getDeviceOnboardingSuggestion: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    batchActivateOnboardingSuggestions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    deleteDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    batchDeleteDevices: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    batchAddDevices: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    replaceDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    updateDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    addDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
  }
}))

vi.mock('@/utils/csv', () => ({
  downloadRowsAsCsv: mockDownloadRowsAsCsv
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
    hasPermission: (code: string) => mockPermissions.has(code)
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
    <section class="device-workbench-panel-stub standard-workbench-panel--workbench-foundation">
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
    <section class="standard-page-shell-stub standard-page-shell--workbench-foundation">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="device-list-filter-header-stub standard-list-filter-header--workbench-foundation">
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
  props: ['variant', 'gap', 'distribution', 'directItems', 'menuItems'],
  template: `
    <div
      class="device-workbench-row-actions-stub standard-workbench-row-actions--quiet"
      :data-variant="variant"
      :data-distribution="distribution"
    >
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

const ElFormStub = defineComponent({
  name: 'ElForm',
  methods: {
    validate() {
      return Promise.resolve(true)
    },
    clearValidate() {
      return undefined
    }
  },
  template: '<form class="el-form-stub"><slot /></form>'
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'id', 'placeholder'],
  emits: ['update:modelValue', 'clear'],
  template: `
    <input
      :id="id"
      class="el-input-stub"
      :value="modelValue"
      :placeholder="placeholder"
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
  props: ['label', 'items'],
  template: '<button class="standard-action-menu-stub" type="button">{{ label || \'鏇村\' }}</button>'
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: {
    modelValue: Boolean,
    eyebrow: String,
    title: String,
    subtitle: String,
    tags: Array,
    tagLayout: String,
    hideHeader: Boolean
  },
  template: `
    <section class="device-detail-drawer-stub" :data-visible="String(modelValue)">
      <div v-if="!hideHeader" class="device-detail-drawer-stub__header">
        <p v-if="eyebrow">{{ eyebrow }}</p>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
        <div class="device-detail-drawer-stub__tags">
          <span
            v-for="tag in tags || []"
            :key="tag.label"
            class="device-detail-drawer-stub__tag"
          >
            {{ tag.label }}
          </span>
        </div>
      </div>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const DeviceCapabilityWorkbenchDrawerStub = defineComponent({
  name: 'DeviceCapabilityWorkbenchDrawer',
  props: ['modelValue', 'device', 'overview', 'commands', 'capabilityLoading', 'commandLoading'],
  emits: ['update:modelValue', 'executeCapability', 'refreshCommands'],
  template: `
    <section class="device-capability-workbench-drawer-stub" :data-visible="String(modelValue)">
      <p class="device-capability-workbench-drawer-stub__code">{{ device?.deviceCode }}</p>
      <p class="device-capability-workbench-drawer-stub__name">{{ device?.deviceName }}</p>
      <p class="device-capability-workbench-drawer-stub__product">{{ device?.productName }}</p>
      <p class="device-capability-workbench-drawer-stub__capability">{{ overview?.productCapabilityType }}</p>
      <slot />
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

const DeviceOnboardingSuggestionDrawerStub = defineComponent({
  name: 'DeviceOnboardingSuggestionDrawer',
  props: ['modelValue', 'suggestion', 'loading', 'errorMessage', 'sourceRow'],
  template: `
    <section class="device-onboarding-suggestion-drawer-stub">
      <p>{{ sourceRow?.deviceCode }}</p>
      <p>{{ suggestion?.recommendedProductKey }}</p>
      <p>{{ suggestion?.recommendedFamilyCode }}</p>
      <p>{{ suggestion?.recommendedTemplateCode }}</p>
      <p>{{ (suggestion?.ruleGaps || []).join(' / ') }}</p>
    </section>
  `
})

const DeviceThresholdDrawerStub = defineComponent({
  name: 'DeviceThresholdDrawer',
  props: ['modelValue', 'loading', 'errorMessage', 'overview'],
  template: '<section class="device-threshold-drawer-stub" />'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  methods: {
    clearSelection() {
      return undefined
    }
  },
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
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        StandardInlineState: StandardInlineStateStub,
        StandardButton: StandardButtonStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        DeviceCapabilityWorkbenchDrawer: DeviceCapabilityWorkbenchDrawerStub,
        DeviceThresholdDrawer: DeviceThresholdDrawerStub,
        DeviceOnboardingSuggestionDrawer: DeviceOnboardingSuggestionDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('DeviceWorkbenchView', () => {
  beforeEach(() => {
    setMockPermissions(
      'iot:devices:add',
      'iot:devices:update',
      'iot:devices:delete',
      'iot:devices:export',
      'iot:devices:replace'
    )
    mockRoute.path = '/devices'
    mockRoute.query = {}
    mockRouter.replace.mockReset()
    mockRouter.push.mockReset()
    mockRouter.replace.mockResolvedValue(undefined)
    mockRouter.push.mockResolvedValue(undefined)
    mockPageDevices.mockReset()
    mockExportDevices.mockReset()
    mockGetDeviceThresholds.mockReset()
    mockDownloadRowsAsCsv.mockReset()
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
    mockExportDevices.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockGetDeviceThresholds.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        deviceId: '0',
        deviceCode: '',
        deviceName: '',
        productId: '',
        productName: '',
        matchedMetricCount: 0,
        missingMetricCount: 0,
        items: []
      }
    })
    installSessionStorageMock()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.warning).mockReset()
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

    expect(wrapper.find('.standard-page-shell--workbench-foundation').exists()).toBe(true)
    expect(wrapper.find('.standard-workbench-panel--workbench-foundation').exists()).toBe(true)
    expect(wrapper.find('.standard-list-filter-header--workbench-foundation').exists()).toBe(true)
    expect(detailDrawer.props('eyebrow')).toBeUndefined()
    expect(formDrawer.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('设备资产中心')
    expect(wrapper.text()).not.toContain('设备台账')
    expect(wrapper.text()).not.toContain('DEVICE ASSET')
  })

  it('shows the shared system busy copy when device page loading returns 500', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageDevices.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', false, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).toHaveBeenCalledWith('系统繁忙，请稍后重试！')
    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalledWith('获取设备分页失败')

    errorSpy.mockRestore()
  })

  it('does not show a second toast when the device page request error is already handled', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageDevices.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', true, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalled()

    errorSpy.mockRestore()
  })

  it('keeps the quick-search box as a single entry while expanding its match scope to product fields', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    const quickSearch = wrapper.get('#quick-search')
    expect(quickSearch.attributes('placeholder')).toBe('快速搜索（设备编码、设备名称、产品 Key、产品名称）')
    expect(wrapper.text()).not.toContain('设备编码：')
    expect(wrapper.text()).not.toContain('产品 Key：')
  })

  it('applies the quick-search keyword when clicking query', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    await wrapper.get('#quick-search').setValue('autotest-device-001')
    await nextTick()

    const queryButton = wrapper
      .findAll('button.standard-button-stub')
      .find((button) => button.text() === '查询')

    expect(queryButton).toBeTruthy()

    await queryButton!.trigger('click')
    await flushPromises()

    expect(mockRouter.replace).toHaveBeenCalledWith({
      path: '/devices',
      query: {
        keyword: 'autotest-device-001'
      }
    })
  })

  it('restores the device detail drawer title and description while keeping the simplified workbench body', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).detailData = {
      id: 1,
      deviceName: '北坡监测终端',
      deviceCode: 'device-001',
      registrationStatus: 1
    }
    await nextTick()

    const detailDrawer = wrapper.findComponent(StandardDetailDrawerStub)
    expect(detailDrawer.props('hideHeader')).toBe(false)
    expect(String(detailDrawer.props('title'))).toBe('北坡监测终端')
    expect(String(detailDrawer.props('subtitle'))).toBe('统一查看资产判断、部署台账、运行台账与建档补充。')
    expect(((detailDrawer.props('tags') as Array<unknown>) || [])).toHaveLength(0)
    expect(detailDrawer.text()).toContain('北坡监测终端')
    expect(detailDrawer.text()).toContain('统一查看资产判断、部署台账、运行台账与建档补充。')
    expect(detailDrawer.text()).not.toContain('已登记')
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

  it('shows edit for unregistered rows when create permission exists', async () => {
    setMockPermissions('iot:devices:add')
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        sourceRecordId: 7001,
        productKey: 'shadow-product',
        productName: '未登记产品',
        deviceCode: 'shadow-device-01',
        deviceName: '未登记设备',
        registrationStatus: 0,
        assetSourceType: 'invalid_report_state',
        createTime: '2026-04-12T09:00:00'
      }
    ]
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')

    expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '详情',
      '编辑'
    ])
  })

  it('opens the lightweight capability drawer from device operation without reusing the detail drawer', async () => {
    const { deviceApi } = await import('@/api/device')
    vi.mocked(deviceApi.getDeviceCapabilities).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceCode: '6260370286',
        productId: '202603192100560271',
        productKey: 'zhd-warning-sound-light-alarm-v1',
        productCapabilityType: 'WARNING',
        subType: 'BROADCAST',
        onlineExecutable: true,
        capabilities: [
          {
            code: 'broadcast_play',
            name: '播放内容',
            group: '广播预警',
            enabled: true,
            requiresOnline: true,
            paramsSchema: {}
          }
        ]
      }
    })
    vi.mocked(deviceApi.pageDeviceCommands).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            commandId: 'CMD-001',
            serviceIdentifier: 'broadcast_play',
            status: 'SENT',
            sendTime: '2026-04-24T10:50:00',
            topic: '/iot/broadcast/6260370286'
          }
        ]
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 2001,
        productKey: 'zhd-warning-sound-light-alarm-v1',
        productName: '中海达预警塔声光报警器',
        deviceCode: '6260370286',
        deviceName: '中海达声光报警器-1',
        registrationStatus: 1,
        onlineStatus: 1,
        activateStatus: 1,
        deviceStatus: 1
      }
    ]
    await nextTick()

    await (wrapper.vm as any).handleRowAction('capability', (wrapper.vm as any).tableData[0])
    await flushPromises()
    await nextTick()

    const capabilityDrawer = wrapper.findComponent(DeviceCapabilityWorkbenchDrawerStub)
    const detailDrawer = wrapper.findComponent(StandardDetailDrawerStub)
    expect(capabilityDrawer.props('modelValue')).toBe(true)
    expect(detailDrawer.props('modelValue')).toBe(false)
    expect((capabilityDrawer.props('device') as Record<string, unknown>).deviceCode).toBe('6260370286')
    expect((capabilityDrawer.props('device') as Record<string, unknown>).deviceName).toBe('中海达声光报警器-1')
    expect((capabilityDrawer.props('overview') as Record<string, unknown>).productCapabilityType).toBe('WARNING')
  })

  it('shows onboarding suggestion for unregistered rows and loads the drawer payload', async () => {
    const { deviceApi } = await import('@/api/device')
    vi.mocked(deviceApi.getDeviceOnboardingSuggestion).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-unregistered-001',
        deviceCode: 'shadow-device-01',
        recommendedProductKey: 'south_rtu',
        recommendedFamilyCode: 'legacy-dp',
        recommendedTemplateCode: 'legacy-dp-crack-v1',
        ruleGaps: ['推荐产品尚未形成正式合同发布，转正前需先发布契约字段。']
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        sourceRecordId: 7001,
        productKey: 'south_rtu',
        productName: '未登记产品',
        deviceCode: 'shadow-device-01',
        deviceName: '未登记设备',
        registrationStatus: 0,
        assetSourceType: 'invalid_report_state',
        lastTraceId: 'trace-unregistered-001'
      }
    ]
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')

    expect(((cardRowActions?.props('menuItems') as Array<{ label: string }>) || []).map((item) => item.label)).toContain('接入建议')

    await (wrapper.vm as any).handleRowAction('suggestion', (wrapper.vm as any).tableData[0])
    await flushPromises()
    await nextTick()

    expect(deviceApi.getDeviceOnboardingSuggestion).toHaveBeenCalledWith('trace-unregistered-001')
    const suggestionDrawer = wrapper.findComponent(DeviceOnboardingSuggestionDrawerStub)
    expect(suggestionDrawer.props('modelValue')).toBe(true)
    expect((suggestionDrawer.props('suggestion') as Record<string, unknown>).recommendedTemplateCode).toBe('legacy-dp-crack-v1')
    expect(wrapper.text()).toContain('south_rtu')
    expect(wrapper.text()).toContain('legacy-dp')
  })

  it('adds batch activation to toolbar actions for confirmed unregistered selections', async () => {
    const { deviceApi } = await import('@/api/device')
    const { confirmAction } = await import('@/utils/confirm')
    vi.mocked(confirmAction).mockResolvedValue(undefined)
    vi.mocked(deviceApi.batchActivateOnboardingSuggestions).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        requestedCount: 1,
        activatedCount: 1,
        rejectedCount: 0,
        activatedTraceIds: ['trace-unregistered-001'],
        activatedDeviceCodes: ['shadow-device-01'],
        errors: []
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        sourceRecordId: 7001,
        productKey: 'south_rtu',
        productName: '未登记产品',
        deviceCode: 'shadow-device-01',
        deviceName: '未登记设备',
        registrationStatus: 0,
        assetSourceType: 'invalid_report_state',
        lastTraceId: 'trace-unregistered-001'
      }
    ]
    ;(wrapper.vm as any).selectedRows = [...(wrapper.vm as any).tableData]
    await nextTick()

    const actionMenu = wrapper.findComponent(StandardActionMenuStub)
    const menuLabels = ((actionMenu.props('items') as Array<{ label: string }>) || []).map((item) => item.label)
    expect(menuLabels).toContain('批量转正式设备')

    await (wrapper.vm as any).handleToolbarAction('batch-activate')
    await flushPromises()
    await nextTick()

    expect(confirmAction).toHaveBeenCalledTimes(1)
    expect(deviceApi.batchActivateOnboardingSuggestions).toHaveBeenCalledWith({
      traceIds: ['trace-unregistered-001'],
      confirmed: true
    })
  })

  it('switches unregistered edit into register mode with add-permission submit copy', async () => {
    setMockPermissions('iot:devices:add')
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).handleEdit({
      sourceRecordId: 7001,
      productKey: 'shadow-product',
      deviceCode: 'shadow-device-01',
      deviceName: '未登记设备',
      registrationStatus: 0,
      assetSourceType: 'invalid_report_state'
    })
    await nextTick()

    const formDrawer = wrapper.findComponent(StandardFormDrawerStub)
    expect(formDrawer.props('title')).toBe('登记设备')
    expect(String(formDrawer.props('subtitle'))).toContain('未登记上报线索')
    expect((wrapper.vm as any).formSubmitText).toBe('提交设备建档')
    expect((wrapper.vm as any).submitPermission).toBe('iot:devices:add')
    expect(wrapper.text()).not.toContain('保存设备变更')
  })

  it('submits register mode through addDevice and removes the stale row from the unregistered view', async () => {
    setMockPermissions('iot:devices:add')
    const { deviceApi } = await import('@/api/device')
    vi.mocked(deviceApi.addDevice).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 8101,
        productKey: 'shadow-product',
        productName: '姝ｅ紡浜у搧',
        deviceCode: 'shadow-device-01',
        deviceName: '北坡正式设备',
        registrationStatus: 1,
        activateStatus: 1,
        deviceStatus: 1
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()
    ;(wrapper.vm as any).tableData = [
      {
        sourceRecordId: 7001,
        productKey: 'shadow-product',
        productName: '未登记产品',
        deviceCode: 'shadow-device-01',
        deviceName: '未登记设备',
        registrationStatus: 0,
        assetSourceType: 'invalid_report_state'
      }
    ]
    ;(wrapper.vm as any).pagination.total = 1
    ;(wrapper.vm as any).appliedFilters.registrationStatus = 0
    ;(wrapper.vm as any).handleEdit((wrapper.vm as any).tableData[0])
    await nextTick()

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()
    await nextTick()

    expect(deviceApi.addDevice).toHaveBeenCalledWith(
      expect.objectContaining({
        productKey: 'shadow-product',
        deviceCode: 'shadow-device-01'
      })
    )
    expect(deviceApi.updateDevice).not.toHaveBeenCalled()
    expect((wrapper.vm as any).tableData).toEqual([])
    expect((wrapper.vm as any).pagination.total).toBe(0)
    expect(vi.mocked(ElMessage.success)).toHaveBeenCalledWith('登记成功')
  })

  it('injects the new registered row back into the current result when register mode finishes in the combined view', async () => {
    setMockPermissions('iot:devices:add')
    const { deviceApi } = await import('@/api/device')
    const createdRow = {
      id: 8102,
      productKey: 'shadow-product',
      productName: '姝ｅ紡浜у搧',
      deviceCode: 'shadow-device-01',
        deviceName: '北坡正式设备',
      registrationStatus: 1,
      activateStatus: 1,
      deviceStatus: 1
    }
    vi.mocked(deviceApi.addDevice).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: createdRow
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()
    ;(wrapper.vm as any).tableData = [
      {
        sourceRecordId: 7001,
        productKey: 'shadow-product',
        productName: '未登记产品',
        deviceCode: 'shadow-device-01',
        deviceName: '未登记设备',
        registrationStatus: 0,
        assetSourceType: 'invalid_report_state'
      }
    ]
    ;(wrapper.vm as any).pagination.pageNum = 1
    ;(wrapper.vm as any).pagination.pageSize = 10
    ;(wrapper.vm as any).pagination.total = 1
    ;(wrapper.vm as any).appliedFilters.registrationStatus = undefined
    mockPageDevices.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createdRow]
      }
    })
    ;(wrapper.vm as any).handleEdit((wrapper.vm as any).tableData[0])
    await nextTick()

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()
    await nextTick()

    expect((wrapper.vm as any).tableData).toEqual([
      expect.objectContaining({
        id: 8102,
        deviceCode: 'shadow-device-01',
        registrationStatus: 1
      })
    ])
    expect((wrapper.vm as any).pagination.total).toBe(1)
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
    expect(cardRowActions?.classes()).toContain('standard-workbench-row-actions--quiet')
    expect(tableRowActions?.classes()).toContain('standard-workbench-row-actions--quiet')
    expect(cardRowActions?.props('gap')).toBeUndefined()
    expect(tableRowActions?.props('gap')).toBeUndefined()
    expect(tableRowActions?.props('distribution')).toBeUndefined()
    expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
        '详情',
        '编辑'
      ])
    expect(((cardRowActions?.props('menuItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '查看设备阈值',
      '更换',
      '洞察',
      '删除'
    ])

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(Number(actionColumn?.props('width'))).toBeGreaterThanOrEqual(160)
  })

  it('adds device operation actions for registered devices when capability view permission exists', async () => {
    setMockPermissions('iot:devices:add', 'iot:device-capability:view')
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 2002,
        productKey: 'demo-product',
        productName: '演示产品',
        deviceCode: 'demo-device-02',
        deviceName: '演示设备 02',
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

    expect(((cardRowActions?.props('menuItems') as Array<{ label: string }>) || []).map((item) => item.label)).toContain(
      '设备操作'
    )
  })

  it('shows threshold action for registered rows and opens the drawer with backend data', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: '8001',
        deviceCode: 'crack-device-01',
        deviceName: '北坡裂缝设备 01',
        productName: '裂缝监测产品',
        registrationStatus: 1,
        onlineStatus: 1,
        activateStatus: 1,
        deviceStatus: 1,
        nodeType: 1
      }
    ]
    mockGetDeviceThresholds.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        deviceId: '8001',
        deviceCode: 'crack-device-01',
        deviceName: '北坡裂缝设备 01',
        productId: '1001',
        productName: '裂缝监测产品',
        matchedMetricCount: 1,
        missingMetricCount: 0,
        items: []
      }
    })
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')
    expect(((cardRowActions?.props('menuItems') as Array<{ label: string }>) || []).map((item) => item.label)).toContain(
      '查看设备阈值'
    )

    await (wrapper.vm as any).handleRowAction('threshold', (wrapper.vm as any).tableData[0])
    await flushPromises()
    await nextTick()

    expect(mockGetDeviceThresholds).toHaveBeenCalledWith('8001')
    const thresholdDrawer = wrapper.findComponent(DeviceThresholdDrawerStub)
    expect(thresholdDrawer.props('modelValue')).toBe(true)
    expect((thresholdDrawer.props('overview') as Record<string, unknown>).deviceCode).toBe('crack-device-01')
  })

  it('exports the applied search result set through the backend endpoint instead of tableData', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).appliedFilters.keyword = 'north-slope'
    ;(wrapper.vm as any).appliedFilters.registrationStatus = 1
    mockExportDevices.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        { id: '1', deviceCode: 'north-01', deviceName: '北坡 01', registrationStatus: 1 },
        { id: '2', deviceCode: 'north-02', deviceName: '北坡 02', registrationStatus: 1 }
      ]
    })

    await (wrapper.vm as any).handleToolbarAction('export-search')
    await flushPromises()

    expect(mockExportDevices).toHaveBeenCalledWith({
      deviceId: undefined,
      keyword: 'north-slope',
      productKey: undefined,
      productName: undefined,
      deviceCode: undefined,
      deviceName: undefined,
      onlineStatus: undefined,
      activateStatus: undefined,
      deviceStatus: undefined,
      registrationStatus: 1
    })
    expect(mockDownloadRowsAsCsv).toHaveBeenCalledWith(
      '设备资产中心-搜索结果.csv',
      expect.arrayContaining([expect.objectContaining({ deviceCode: 'north-01' }), expect.objectContaining({ deviceCode: 'north-02' })]),
      expect.any(Array)
    )
  })

  it('shows the organization ledger in both the list source and rendered device cards', async () => {
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
        orgId: 7101,
        orgName: '平台运维中心',
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

    expect(wrapper.text()).toContain('所属机构')
    expect(((wrapper.vm as any).tableData[0] as Record<string, unknown>).orgName).toBe('平台运维中心')

    const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')
    expect(source).toContain('label="所属机构"')
    expect(source).toContain('>所属机构</span>')
  })

  it('keeps the device workbench on the shared list surface and trims toolbar density', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('standard-mobile-record-grid')
    expect(source).toContain('label="设备名称"')
    expect(source).toContain('label="设备编号"')
    expect(source).not.toContain('secondary-prop="deviceCode"')
    expect(source).not.toContain('label="接入协议" :width="120"')
    expect(source).not.toContain('standard-mobile-record-card__field-label">接入协议')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).toContain('已登记 ${registeredCount} 台')
    expect(source).not.toContain('`未登记 ${unregisteredCount} 台`')
  })

  it('delegates detail layout to the dedicated device detail workbench component', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')

    expect(source).toContain("from '@/components/device/DeviceDetailWorkbench.vue'")
    expect(source).toContain("from '@/components/device/DeviceCapabilityWorkbenchDrawer.vue'")
    expect(source).toContain("from '@/components/device/DeviceThresholdDrawer.vue'")
    expect(source).toContain('<DeviceDetailWorkbench')
    expect(source).toContain('<DeviceCapabilityWorkbenchDrawer')
    expect(source).toContain('<DeviceThresholdDrawer')
    expect(source).toContain(':device="detailData"')
    expect(source).toContain(':device="capabilityDevice"')
    expect(source).toContain('formatDeviceReportTime')
    expect(source).not.toContain('tag-layout="title-inline"')
    expect(source).not.toContain(':tags="detailTags"')
    expect(source).not.toContain('<h3>璧勪骇姒傝</h3>')
    expect(source).not.toContain('<h3>璧勪骇妗ｆ</h3>')
    expect(source).not.toContain('<h3>鎷撴墤鍏崇郴</h3>')
    expect(source).not.toContain('<h3>杩愮淮淇℃伅</h3>')
    expect(source).not.toContain('<h3>认证信息</h3>')
    expect(source).not.toContain('<h3>上报档案</h3>')
    expect(source).not.toContain('已先展示列表摘要，正在补全完整详情。')
    expect(source).not.toContain('已先填入当前摘要，正在补全最新设备档案。')
  })
})
