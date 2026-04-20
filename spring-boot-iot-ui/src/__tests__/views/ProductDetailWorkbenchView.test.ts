import { defineComponent, nextTick, reactive } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'

import ProductDetailWorkbenchView from '@/views/ProductDetailWorkbenchView.vue'

const { mockRoute, mockPageDevices, mockGetProductById, mockGetProductOverviewSummary, mockRouterPush, mockRouterReplace } = vi.hoisted(() => ({
  mockRoute: {
    path: '/products/42/overview',
    name: 'product-overview',
    params: { productId: '42' },
    query: {}
  },
  mockPageDevices: vi.fn(),
  mockGetProductById: vi.fn(),
  mockGetProductOverviewSummary: vi.fn(),
  mockRouterPush: vi.fn(),
  mockRouterReplace: vi.fn()
}))

const mockRouteState = reactive(mockRoute)

vi.mock('vue-router', () => ({
  useRoute: () => mockRouteState,
  useRouter: () => ({
    push: mockRouterPush,
    replace: mockRouterReplace
  })
}))

vi.mock('@/api/product', () => ({
  productApi: {
    getProductById: mockGetProductById,
    getProductOverviewSummary: mockGetProductOverviewSummary
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: mockPageDevices
  }
}))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['title', 'description', 'eyebrow', 'breadcrumbs', 'showTitle'],
  template: `
    <section>
      <p class="shell-title">{{ title }}</p>
      <p v-if="eyebrow" class="shell-eyebrow">{{ eyebrow }}</p>
      <pre v-if="breadcrumbs" class="shell-breadcrumbs">{{ JSON.stringify(breadcrumbs) }}</pre>
      <slot name="actions" />
      <slot />
    </section>
  `
})

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  props: ['to'],
  template: `
    <a class="router-link-stub" v-bind="$attrs" :data-to="typeof to === 'string' ? to : JSON.stringify(to)">
      <slot />
    </a>
  `
})

const ProductDeviceListWorkspaceStub = defineComponent({
  name: 'ProductDeviceListWorkspace',
  props: ['pagination'],
  emits: ['viewDevice', 'page-change', 'page-size-change'],
  template: `
    <section class="product-device-list-workspace-stub">
      <span class="device-page-num">{{ pagination?.pageNum }}</span>
      <span class="device-page-size">{{ pagination?.pageSize }}</span>
      <button class="emit-page-2" type="button" @click="$emit('page-change', 2)">page2</button>
      <button class="emit-size-20" type="button" @click="$emit('page-size-change', 20)">size20</button>
    </section>
  `
})

beforeEach(() => {
  mockRouteState.path = '/products/42/overview'
  mockRouteState.name = 'product-overview'
  mockRouteState.params = { productId: '42' }
  mockRouteState.query = {}
  mockGetProductById.mockReset()
  mockGetProductById.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      id: 42,
      productKey: 'nf-collect-rtu-v1',
      productName: '南方测绘 采集型 遥测终端',
      description: '采集型遥测终端设备，协议 mqtt-json，直接接入'
    }
  })
  mockGetProductOverviewSummary.mockReset()
  mockGetProductOverviewSummary.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: null
  })
  mockRouterPush.mockReset()
  mockRouterReplace.mockReset()
  mockPageDevices.mockReset()
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
  })
})

it.each([
  ['product-overview', '产品总览'],
  ['product-contracts', '契约字段'],
  ['product-mapping-rules', '映射规则']
])('titles the page by active section for %s', async (routeName, expectedTitle) => {
  mockRouteState.name = routeName

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: true,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(wrapper.find('.shell-title').text()).toBe(expectedTitle)
  expect(wrapper.find('.shell-eyebrow').exists()).toBe(false)
  expect(wrapper.find('.shell-breadcrumbs').exists()).toBe(false)
})

it('renders single-line workbench tabs and keeps only device and field hero metrics', async () => {
  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  const tabTexts = wrapper
    .findAll('.product-detail-page__tab')
    .map((node) => node.text().replace(/\s+/g, ' ').trim())

  expect(tabTexts).toEqual(['产品总览', '关联设备', '契约字段', '映射规则', '版本台账'])
  expect(wrapper.find('.product-detail-page__tab small').exists()).toBe(false)

  const metricTexts = wrapper
    .findAll('.product-detail-page__metric-card')
    .map((node) => node.text().replace(/\s+/g, ' ').trim())

  expect(metricTexts).toHaveLength(2)
  expect(metricTexts[0]).toContain('关联设备')
  expect(metricTexts[1]).toContain('正式字段')
  expect(wrapper.text()).not.toContain('最新批次')
})

it('marks the active workbench tab and syncs device pagination through the route query', async () => {
  mockRouteState.path = '/products/42/devices'
  mockRouteState.name = 'product-devices'
  mockRouteState.query = {
    pageNum: '3',
    pageSize: '20'
  }
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 25, pageNum: 3, pageSize: 20, records: [] }
  })

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(wrapper.get('[aria-current="page"]').text()).toBe('关联设备')
  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 3,
    pageSize: 20
  })

  await wrapper.get('.emit-page-2').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockRouterReplace).toHaveBeenCalledWith({
    path: '/products/42/devices',
    query: {
      pageNum: 2,
      pageSize: 20
    }
  })

  mockRouteState.query = {
    pageNum: '2',
    pageSize: '20'
  }
  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 2,
    pageSize: 20
  })
})

it('keeps product detail data cached when switching between detail sections', async () => {
  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(mockGetProductById).toHaveBeenCalledTimes(1)
  expect(mockGetProductOverviewSummary).toHaveBeenCalledTimes(1)

  mockRouteState.path = '/products/42/contracts'
  mockRouteState.name = 'product-contracts'
  await flushPromises()
  await nextTick()

  expect(mockGetProductById).toHaveBeenCalledTimes(1)
  expect(mockGetProductOverviewSummary).toHaveBeenCalledTimes(1)

  mockRouteState.path = '/products/42/devices'
  mockRouteState.name = 'product-devices'
  await flushPromises()
  await nextTick()

  expect(mockGetProductById).toHaveBeenCalledTimes(1)
  expect(mockGetProductOverviewSummary).toHaveBeenCalledTimes(1)
  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 10
  })
  expect(wrapper.find('.product-device-list-workspace-stub').exists()).toBe(true)
})

it('requests related devices with server pagination and reacts to pagination events', async () => {
  mockRouteState.path = '/products/42/devices'
  mockRouteState.name = 'product-devices'
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 25, pageNum: 1, pageSize: 10, records: [] }
  })

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 10
  })

  await wrapper.get('.emit-page-2').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockRouterReplace).toHaveBeenLastCalledWith({
    path: '/products/42/devices',
    query: {
      pageNum: 2
    }
  })

  mockRouteState.query = {
    pageNum: '2'
  }
  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 2,
    pageSize: 10
  })

  await wrapper.get('.emit-size-20').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockRouterReplace).toHaveBeenLastCalledWith({
    path: '/products/42/devices',
    query: {
      pageSize: 20
    }
  })

  mockRouteState.query = {
    pageSize: '20'
  }
  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 20
  })
})
