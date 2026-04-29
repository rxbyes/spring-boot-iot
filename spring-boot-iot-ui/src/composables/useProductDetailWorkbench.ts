import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { deviceApi } from '@/api/device'
import { productApi } from '@/api/product'
import { resolveRequestErrorMessage } from '@/api/request'
import { useServerPagination } from '@/composables/useServerPagination'
import type { Device, Product, ProductOverviewSummary } from '@/types/api'
import {
  resolveProductWorkbenchSectionByRouteName,
  type ProductWorkbenchSection
} from '@/utils/productWorkbenchRoutes'

const defaultDevicePageSize = 10

function readRouteQueryString(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string') {
    return ''
  }
  return raw.trim()
}

function parseRoutePositiveIntQuery(value: unknown, fallback: number) {
  const raw = readRouteQueryString(value)
  if (!raw) {
    return fallback
  }
  const parsed = Number(raw)
  if (!Number.isFinite(parsed) || parsed < 1) {
    return fallback
  }
  return Math.trunc(parsed)
}

function normalizeQueryValue(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (raw === undefined || raw === null || raw === '') {
    return undefined
  }
  return String(raw)
}

function assignDeviceQueryValue(
  query: Record<string, unknown>,
  key: 'pageNum' | 'pageSize',
  value: number | undefined
) {
  if (value === undefined) {
    delete query[key]
    return
  }
  query[key] = value
}

export function useProductDetailWorkbench() {
  const route = useRoute()
  const router = useRouter()

  const product = ref<Product | null>(null)
  const overviewSummary = ref<ProductOverviewSummary | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')
  const devicesLoading = ref(false)
  const deviceErrorMessage = ref('')
  const devices = ref<Device[]>([])
  const {
    pagination: devicePagination,
    applyPageResult: applyDevicePageResult,
    resetPage: resetDevicePage,
    resetTotal: resetDeviceTotal
  } = useServerPagination(defaultDevicePageSize)

  const productId = computed(() => String(route.params.productId || '').trim())
  const activeSection = computed<ProductWorkbenchSection>(() =>
    resolveProductWorkbenchSectionByRouteName(route.name)
  )

  let latestProductRequestId = 0
  let latestDeviceRequestId = 0

  function clearDeviceState() {
    devices.value = []
    resetDeviceTotal()
    deviceErrorMessage.value = ''
    devicesLoading.value = false
  }

  function applyDevicePaginationFromRoute() {
    devicePagination.pageSize = parseRoutePositiveIntQuery(route.query.pageSize, defaultDevicePageSize)
    devicePagination.pageNum = parseRoutePositiveIntQuery(route.query.pageNum, 1)
  }

  function hasSameDeviceRouteQuery(nextQuery: Record<string, unknown>) {
    return (
      normalizeQueryValue(route.query.pageNum) === normalizeQueryValue(nextQuery.pageNum) &&
      normalizeQueryValue(route.query.pageSize) === normalizeQueryValue(nextQuery.pageSize)
    )
  }

  async function syncDevicePaginationToRoute() {
    const nextQuery: Record<string, unknown> = { ...(route.query || {}) }
    assignDeviceQueryValue(nextQuery, 'pageNum', devicePagination.pageNum > 1 ? devicePagination.pageNum : undefined)
    assignDeviceQueryValue(
      nextQuery,
      'pageSize',
      devicePagination.pageSize !== defaultDevicePageSize ? devicePagination.pageSize : undefined
    )

    if (hasSameDeviceRouteQuery(nextQuery)) {
      return
    }

    await router.replace({
      path: route.path,
      query: nextQuery
    })
  }

  async function loadProductContext() {
    const requestId = ++latestProductRequestId

    if (!productId.value) {
      product.value = null
      overviewSummary.value = null
      clearDeviceState()
      loading.value = false
      errorMessage.value = '当前链接缺少有效产品上下文，请返回产品定义中心重新选择产品。'
      return false
    }

    loading.value = true
    errorMessage.value = ''
    try {
      const [detailResult, summaryResult] = await Promise.allSettled([
        productApi.getProductById(productId.value),
        productApi.getProductOverviewSummary(productId.value)
      ])

      if (requestId !== latestProductRequestId) {
        return false
      }

      if (detailResult.status !== 'fulfilled' || detailResult.value.code !== 200 || !detailResult.value.data) {
        throw new Error('产品详情加载失败')
      }

      product.value = detailResult.value.data
      overviewSummary.value =
        summaryResult.status === 'fulfilled' && summaryResult.value.code === 200
          ? summaryResult.value.data || null
          : null
      return true
    } catch (error) {
      if (requestId !== latestProductRequestId) {
        return false
      }
      product.value = null
      overviewSummary.value = null
      clearDeviceState()
      errorMessage.value = '未找到可用的产品上下文，请返回产品定义中心重新选择产品。'
      return false
    } finally {
      if (requestId === latestProductRequestId) {
        loading.value = false
      }
    }
  }

  async function loadDevices() {
    const productKey = product.value?.productKey?.trim()
    if (!productKey) {
      clearDeviceState()
      deviceErrorMessage.value = '产品 Key 缺失，无法加载关联设备。'
      return
    }

    const requestId = ++latestDeviceRequestId
    devicesLoading.value = true
    deviceErrorMessage.value = ''
    try {
      const response = await deviceApi.pageDevices({
        productKey,
        pageNum: devicePagination.pageNum,
        pageSize: devicePagination.pageSize
      })

      if (requestId !== latestDeviceRequestId) {
        return
      }

      if (response.code === 200) {
        devices.value = applyDevicePageResult(response.data)
        return
      }
      clearDeviceState()
    } catch (error) {
      if (requestId !== latestDeviceRequestId) {
        return
      }
      clearDeviceState()
      deviceErrorMessage.value = resolveRequestErrorMessage(error, '加载关联设备失败')
    } finally {
      if (requestId === latestDeviceRequestId) {
        devicesLoading.value = false
      }
    }
  }

  async function refreshProductWorkspace() {
    const loaded = await loadProductContext()
    if (!loaded) {
      return
    }
    if (activeSection.value === 'devices') {
      applyDevicePaginationFromRoute()
      await loadDevices()
      return
    }
    clearDeviceState()
  }

  async function handleDevicePageChange(page: number) {
    if (page === devicePagination.pageNum) {
      return
    }
    devicePagination.pageNum = page
    await syncDevicePaginationToRoute()
  }

  async function handleDevicePageSizeChange(size: number) {
    if (size === devicePagination.pageSize) {
      return
    }
    devicePagination.pageSize = size
    devicePagination.pageNum = 1
    await syncDevicePaginationToRoute()
  }

  function handleProductUpdated(updatedProduct: Product) {
    product.value = updatedProduct
    void refreshProductWorkspace()
  }

  watch(
    () => productId.value,
    (current, previous) => {
      if (current === previous) {
        return
      }
      resetDevicePage()
      clearDeviceState()
      void loadProductContext()
    },
    { immediate: true }
  )

  watch(
    () => [activeSection.value, product.value?.productKey, route.query.pageNum, route.query.pageSize] as const,
    ([section, productKey], previous) => {
      if (section !== 'devices') {
        if (previous?.[0] === 'devices') {
          clearDeviceState()
        }
        return
      }

      applyDevicePaginationFromRoute()
      if (!productKey) {
        return
      }
      void loadDevices()
    },
    { immediate: true }
  )

  return {
    productId,
    activeSection,
    product,
    overviewSummary,
    loading,
    errorMessage,
    devices,
    devicesLoading,
    deviceErrorMessage,
    devicePagination,
    refreshProductWorkspace,
    handleDevicePageChange,
    handleDevicePageSizeChange,
    handleProductUpdated
  }
}
