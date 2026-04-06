import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent, h, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createRequestError } from '@/api/request'
import ProductWorkbenchView from '@/views/ProductWorkbenchView.vue'

const {
  mockRoute,
  mockRouter,
  mockPageProducts,
  mockGetProductById,
  mockAddProduct,
  mockUpdateProduct,
  mockDeleteProduct
} = vi.hoisted(() => ({
  mockRoute: {
    path: '/products',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageProducts: vi.fn(),
  mockGetProductById: vi.fn(),
  mockAddProduct: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockDeleteProduct: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/product', () => ({
  productApi: {
    pageProducts: mockPageProducts,
    getProductById: mockGetProductById,
    addProduct: mockAddProduct,
    updateProduct: mockUpdateProduct,
    deleteProduct: mockDeleteProduct
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
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="product-workbench-panel-stub">
      <p class="product-workbench-panel-stub__eyebrow">{{ eyebrow }}</p>
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
    <section class="product-list-filter-header-stub">
      <div class="product-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="product-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="product-table-toolbar-stub">
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'gap', 'directItems', 'menuItems'],
  emits: ['command'],
  template: `
    <div class="product-workbench-row-actions-stub" :data-variant="variant">
      <button
        v-for="item in directItems || []"
        :key="item.key || item.command"
        type="button"
        class="product-workbench-row-actions-stub__direct"
        :data-testid="item.dataTestid"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <span class="product-workbench-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </div>
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

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['size', 'eyebrow', 'title', 'subtitle'],
  template: `
    <section class="product-detail-drawer-stub" :data-size="size">
      <p v-if="eyebrow" class="product-detail-drawer-stub__eyebrow">{{ eyebrow }}</p>
      <h2 class="product-detail-drawer-stub__title">{{ title }}</h2>
      <p class="product-detail-drawer-stub__subtitle">{{ subtitle }}</p>
      <div class="product-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="product-detail-drawer-stub__body"><slot /></div>
      <div class="product-detail-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const ProductDetailWorkbenchStub = defineComponent({
  name: 'ProductDetailWorkbench',
  props: ['product'],
  template: '<section class="product-detail-workbench-stub">{{ product?.productName }}</section>'
})

const ProductBusinessWorkbenchDrawerStub = defineComponent({
  name: 'ProductBusinessWorkbenchDrawer',
  props: ['modelValue', 'product', 'activeView'],
  emits: ['update:modelValue', 'update:activeView', 'saved'],
  template: `
    <section v-if="modelValue" class="product-business-workbench-drawer-stub">
      <h2 class="product-business-workbench-drawer-stub__title">{{ product?.productName }}</h2>
      <p class="product-business-workbench-drawer-stub__key">{{ product?.productKey }}</p>
      <div class="product-business-workbench-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <p data-testid="product-business-workbench-active-view">{{ activeView }}</p>
      <slot v-if="activeView === 'overview'" name="overview" />
      <slot v-else-if="activeView === 'models'" name="models" />
      <slot v-else-if="activeView === 'devices'" name="devices" />
      <slot v-else name="edit" />
    </section>
  `
})

const ProductModelDesignerWorkspaceStub = defineComponent({
  name: 'ProductModelDesignerWorkspace',
  props: ['product'],
  template: '<section class="product-model-designer-workspace-stub">{{ product?.productKey }}</section>'
})

const ProductDeviceListWorkspaceStub = defineComponent({
  name: 'ProductDeviceListWorkspace',
  props: ['devices', 'loading', 'errorMessage', 'empty', 'devicesLoading'],
  template: `
    <section class="product-device-list-workspace-stub">
      <span class="product-device-list-workspace-stub__count">{{ devices?.length ?? 0 }}</span>
      <span class="product-device-list-workspace-stub__loading">{{ loading ? 'loading' : 'ready' }}</span>
    </section>
  `
})

const ProductEditWorkspaceStub = defineComponent({
  name: 'ProductEditWorkspace',
  props: ['model', 'editing'],
  emits: ['cancel', 'submit'],
  setup(props, { expose }) {
    expose({
      validate: () => Promise.resolve(true),
      clearValidate: () => undefined
    })
    return () =>
      h('section', { class: 'product-edit-workspace-stub' }, [
        h('span', props.model?.productName || ''),
        h('span', props.editing ? 'editing' : 'creating')
      ])
  }
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  template: '<section class="product-form-drawer-stub"><slot /><slot name="footer" /></section>'
})

const DeviceListDrawerStub = defineComponent({
  name: 'DeviceListDrawer',
  props: ['eyebrow', 'title'],
  template: '<section class="device-list-drawer-stub">{{ title }}</section>'
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

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['prop', 'label', 'minWidth', 'width'],
  template: `
    <section class="standard-table-text-column-stub" :data-prop="prop">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  emits: ['selection-change'],
  template: '<section class="el-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['prop', 'label', 'width', 'fixed', 'type', 'align', 'className', 'showOverflowTooltip'],
  template: `
    <section class="el-table-column-stub" :data-prop="prop || type || 'column'">
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
  return shallowMount(ProductWorkbenchView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      renderStubDefaultSlot: true,
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardTableToolbar: StandardTableToolbarStub,
        ProductBusinessWorkbenchDrawer: ProductBusinessWorkbenchDrawerStub,
        ProductDetailWorkbench: ProductDetailWorkbenchStub,
        ProductModelDesignerWorkspace: ProductModelDesignerWorkspaceStub,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductEditWorkspace: ProductEditWorkspaceStub,
        StandardDrawerFooter: true,
        StandardAppliedFiltersBar: true,
        StandardInlineState: StandardInlineStateStub,
        StandardPagination: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        CsvColumnSettingDialog: true,
        DeviceListDrawer: DeviceListDrawerStub,
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
    mockGetProductById.mockReset()
    mockAddProduct.mockReset()
    mockUpdateProduct.mockReset()
    mockDeleteProduct.mockReset()
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
    mockGetProductById.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockAddProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockUpdateProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockDeleteProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    installSessionStorageMock()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.warning).mockReset()
  })

  it('renders the product page inside the shared governance shell without the legacy eyebrow tier', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('产品定义中心')
    expect(wrapper.text()).toContain('新增产品')
    expect(wrapper.text()).toContain('统一维护产品台账')
    expect(wrapper.text()).not.toContain('PRODUCT CENTER')
  })

  it('keeps the product toolbar focused by collapsing low-frequency actions into a more-actions menu', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('批量操作')
    expect(wrapper.text()).toContain('刷新列表')
    expect(wrapper.text()).toContain('更多操作')
    expect(wrapper.text()).not.toContain('表格视图')
    expect(wrapper.text()).not.toContain('卡片视图')
    expect(wrapper.text()).not.toContain('导出列设置')
    expect(wrapper.text()).not.toContain('导出选中')
    expect(wrapper.text()).not.toContain('导出当前结果')
    expect(wrapper.text()).not.toContain('清空选中')
  })

  it('keeps /products as a single main list with desktop table and responsive mobile cards', async () => {
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
    await nextTick()

    expect(wrapper.find('.product-mobile-list').exists()).toBe(true)
    expect(wrapper.find('.product-desktop-table').exists()).toBe(true)

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)

    expect(rowActions.some((component) => component.props('variant') === 'card')).toBe(true)
    expect(rowActions.some((component) => component.props('variant') === 'table')).toBe(true)
  })

  it('shows the shared system busy copy when product page loading returns 500', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageProducts.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', false, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).toHaveBeenCalledWith('系统繁忙，请稍后重试！')
    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalledWith('获取产品分页失败')

    errorSpy.mockRestore()
  })

  it('does not show a second toast when the product page request error is already handled', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageProducts.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', true, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalled()

    errorSpy.mockRestore()
  })

  it('opens the unified business workbench from the single direct entry with overview as the default view', async () => {
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
    await nextTick()

    const workbenchEntry = wrapper.find('[data-testid="open-product-business-workbench"]')
    expect(workbenchEntry.exists()).toBe(true)

    await workbenchEntry.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(true)
    expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('overview')
    expect(wrapper.find('.product-detail-workbench-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('演示产品')
    expect(wrapper.text()).toContain('demo-product')
  })

  it('reuses the shared workbench row-actions component for both table and mobile product rows', async () => {
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
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')
    const tableRowActions = rowActions.find((component) => component.props('variant') === 'table')

    expect(cardRowActions?.exists()).toBe(true)
    expect(tableRowActions?.exists()).toBe(true)
    expect(cardRowActions?.props('gap')).toBeUndefined()
    expect(tableRowActions?.props('gap')).toBeUndefined()
    expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '进入工作台',
      '删除'
    ])
    expect(((tableRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '进入工作台',
      '删除'
    ])
    expect(((cardRowActions?.props('menuItems') as Array<unknown>) || []).length).toBe(0)
    expect(((tableRowActions?.props('menuItems') as Array<unknown>) || []).length).toBe(0)

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(String(actionColumn?.props('width'))).toBe('152')
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

  it('opens the unified business workbench with overview as the default detail view', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    ;(wrapper.vm as any).handleRowAction('detail', product)
    await nextTick()

    expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(true)
    expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('overview')
    expect(wrapper.get('.product-business-workbench-drawer-stub__title').text()).toBe('演示产品')
  })

  it('keeps devices in the same workbench and opens edit from the header action', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    ;(wrapper.vm as any).handleRowAction('devices', product)
    await nextTick()
    expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('devices')
    expect(wrapper.find('.product-device-list-workspace-stub').exists()).toBe(true)

    ;(wrapper.vm as any).handleRowAction('detail', product)
    await nextTick()
    expect(wrapper.find('.product-business-workbench-drawer-stub__header-actions').text()).toContain('编辑档案')
    await wrapper.get('[data-testid="open-product-workbench-edit"]').trigger('click')
    await nextTick()
    expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('edit')
    expect(wrapper.find('.product-edit-workspace-stub').exists()).toBe(true)
  })

  it('keeps the unified workbench context in sync after saving product edits', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      manufacturer: 'GHLZM',
      description: '原始说明',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    const updatedProduct = {
      ...product,
      productName: '演示产品（已更新）',
      manufacturer: '更新厂商'
    }

    mockUpdateProduct.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: updatedProduct
    })

    ;(wrapper.vm as any).handleRowAction('edit', product)
    await nextTick()

    ;(wrapper.vm as any).formData.productName = '演示产品（已更新）'
    ;(wrapper.vm as any).formData.manufacturer = '更新厂商'

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()
    await nextTick()

    expect(mockUpdateProduct).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        productName: '演示产品（已更新）',
        manufacturer: '更新厂商'
      })
    )
    expect((wrapper.vm as any).businessWorkbenchVisible).toBe(true)
    expect((wrapper.vm as any).businessWorkbenchProduct.productName).toBe('演示产品（已更新）')
    expect((wrapper.vm as any).detailData.productName).toBe('演示产品（已更新）')
  })

  it('removes the standalone device and model drawers from the /products page composition', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.findComponent(DeviceListDrawerStub).exists()).toBe(false)
    expect(wrapper.findComponent(StandardDetailDrawerStub).exists()).toBe(false)
  })

  it('labels the applied quick-search chip as quick search instead of product name', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).appliedFilters.productName = 'demo-product'
    await nextTick()

    expect((wrapper.vm as any).activeFilterTags[0].label).toBe('快速搜索：demo-product')
  })

  it('advertises product key support in the quick-search placeholder copy', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/ProductWorkbenchView.vue'), 'utf8')

    expect(source).toContain('快速搜索（产品名称、产品 Key、厂商）')
  })

  it('uses the shared list surface and mobile-card grammar', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/ProductWorkbenchView.vue'), 'utf8')

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('standard-mobile-record-grid')
    expect(source).toContain('standard-mobile-record-card')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
  })
})
