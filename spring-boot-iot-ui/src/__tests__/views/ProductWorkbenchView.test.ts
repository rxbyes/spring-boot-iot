import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductWorkbenchView from '@/views/ProductWorkbenchView.vue'

const { mockRoute, mockRouter, mockPageProducts } = vi.hoisted(() => ({
  mockRoute: {
    path: '/products',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageProducts: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/product', () => ({
  productApi: {
    pageProducts: mockPageProducts,
    getProductById: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    addProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    updateProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    deleteProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
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
  props: ['title', 'description'],
  template: `
    <section class="product-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="product-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="product-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="product-workbench-panel-stub__inline"><slot name="inline-state" /></div>
      <div class="product-workbench-panel-stub__body"><slot /></div>
      <div class="product-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="product-list-filter-header-stub">
      <div class="product-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="product-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="product-row-actions-stub"><slot /></div>'
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button class="product-action-link-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label'],
  template: '<button class="product-action-menu-stub" type="button">{{ label || \'更多\' }}</button>'
})

const sampleProductRow = {
  id: 1001,
  productKey: 'demo-product',
  productName: '演示产品',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'GHLZM',
  status: 1,
  onlineDeviceCount: 0,
  lastReportTime: '2026-03-24T09:00:00',
  updateTime: '2026-03-24T09:00:00'
}

const ElTableStub = defineComponent({
  name: 'ElTable',
  template: '<section class="el-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'width', 'fixed', 'showOverflowTooltip', 'className'],
  setup() {
    return {
      rowScope: sampleProductRow
    }
  },
  template: `
    <div
      class="el-table-column-stub"
      :data-label="label || ''"
      :data-width="String(width ?? '')"
      :data-fixed="fixed || ''"
      :data-overflow="String(showOverflowTooltip ?? '')"
      :data-class-name="className || ''"
    >
      <slot :row="rowScope" />
    </div>
  `
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  template: `
    <section class="product-detail-drawer-stub">
      <div class="product-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="product-detail-drawer-stub__body"><slot /></div>
      <div class="product-detail-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  template: '<section class="product-form-drawer-stub"><slot /><slot name="footer" /></section>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
})

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['title', 'status'],
  template: `
    <section class="iot-access-page-shell">
      <h1>{{ title }}</h1>
      <p>{{ status }}</p>
      <slot name="actions" />
      <slot />
    </section>
  `
})

const IotAccessTabWorkspaceStub = defineComponent({
  name: 'IotAccessTabWorkspace',
  props: ['items', 'defaultKey', 'modelValue'],
  template: `
    <section class="iot-access-tab-workspace">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="iot-access-tab-workspace__tab"
      >
        {{ item.label }}
      </button>
      <slot :active-key="modelValue || defaultKey || items?.[0]?.key || ''" />
    </section>
  `
})

const IotAccessResultSectionStub = defineComponent({
  name: 'IotAccessResultSection',
  props: ['title', 'description'],
  template: `
    <section class="iot-access-result-section">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="toolbar" />
      <slot />
    </section>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  setup() {
    return {
      rowScope: sampleProductRow
    }
  },
  template: `
    <div class="standard-table-text-column-stub" :data-label="label || ''">
      <slot :row="rowScope">{{ label }}</slot>
    </div>
  `
})

const ProductModelDesignerDrawerStub = defineComponent({
  name: 'ProductModelDesignerDrawer',
  props: ['modelValue', 'product'],
  template: `
    <section v-if="modelValue" class="product-model-designer-drawer-stub">
      <h2>基于真实上报提炼产品契约</h2>
      <h3>属性模型</h3>
      <h3>事件模型</h3>
      <h3>服务模型</h3>
      <p>暂无物模型</p>
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
  return shallowMount(ProductWorkbenchView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      renderStubDefaultSlot: true,
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        ProductModelDesignerDrawer: ProductModelDesignerDrawerStub,
        StandardDrawerFooter: true,
        StandardAppliedFiltersBar: true,
        StandardTableToolbar: true,
        StandardInlineState: StandardInlineStateStub,
        StandardPagination: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        IotAccessPageShell: IotAccessPageShellStub,
        IotAccessTabWorkspace: IotAccessTabWorkspaceStub,
        IotAccessResultSection: IotAccessResultSectionStub,
        CsvColumnSettingDialog: true,
        DeviceListDrawer: true,
        EmptyState: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('ProductWorkbenchView', () => {
  beforeEach(() => {
    mockRoute.path = '/products'
    mockRoute.query = {}
    mockRouter.replace.mockReset()
    mockRouter.push.mockReset()
    mockRouter.replace.mockResolvedValue(undefined)
    mockRouter.push.mockResolvedValue(undefined)
    mockPageProducts.mockReset()
    mockPageProducts.mockResolvedValue({
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

  it('renders a compact product workbench header above the ledger', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
    expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true)
    expect(wrapper.text()).toContain('产品定义中心')
    expect(wrapper.text()).toContain('先补齐产品契约，再处理库存治理。')
    expect(wrapper.text()).toContain('产品台账')
    expect(wrapper.text()).toContain('物模型治理')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('新增产品')
    expect(wrapper.text()).not.toContain('产品契约治理')
  })

  it('renders a product-model designer entry and opens the empty designer state', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 1001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        status: 1,
        deviceCount: 0,
        onlineDeviceCount: 0,
        createTime: '2026-03-24T09:00:00',
        updateTime: '2026-03-24T09:00:00'
      }
    ]
    ;(wrapper.vm as any).viewType = 'card'
    await nextTick()

    const designerEntry = wrapper.find('[data-testid="open-product-model-designer"]')
    expect(designerEntry.exists()).toBe(true)

    await designerEntry.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('属性模型')
    expect(wrapper.text()).toContain('事件模型')
    expect(wrapper.text()).toContain('服务模型')
    expect(wrapper.text()).toContain('基于真实上报提炼产品契约')
    expect(wrapper.text()).toContain('暂无物模型')
  })

  it('shows a compact diagnostic intake hint when opened from system-log', async () => {
    mockRoute.query = {
      productKey: 'demo-product',
      traceId: 'trace-001'
    }
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'system-log',
          productKey: 'demo-product',
          traceId: 'trace-001',
          capturedAt: new Date().toISOString()
        }
      })
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('来自异常观测台')
    expect(wrapper.text()).toContain('Trace trace-001')
  })

  it('uses a dedicated action-column layout to avoid clipped menus and uneven spacing', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [sampleProductRow]
    ;(wrapper.vm as any).viewType = 'table'
    await nextTick()

    const actionColumn = wrapper
      .findAll('.el-table-column-stub')
      .find((column) => column.attributes('data-label') === '操作')

    expect(actionColumn?.attributes('data-width')).toBe('304')
    expect(actionColumn?.attributes('data-class-name')).toBe('product-desktop-table__actions-column')
    expect(wrapper.find('.product-table-row-actions').exists()).toBe(true)
    expect(wrapper.find('.product-action-menu-stub').text()).toBe('更多')
  })
})
