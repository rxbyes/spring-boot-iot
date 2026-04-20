<template>
  <StandardPageShell
    class="product-detail-page"
    :title="pageTitle"
    :description="pageDescription"
  >
    <template #actions>
      <StandardButton action="query" @click="handleBackToList">返回列表</StandardButton>
      <StandardButton action="refresh" @click="handleRefresh">刷新</StandardButton>
    </template>

    <div v-if="loading && !product" class="product-detail-page__state">
      <strong>正在加载产品工作区...</strong>
    </div>
    <div v-else-if="errorMessage && !product" class="product-detail-page__state product-detail-page__state--error">
      <strong>{{ errorMessage }}</strong>
    </div>
    <template v-else-if="product">
      <section class="product-detail-page__hero">
        <div class="product-detail-page__hero-copy">
          <span class="product-detail-page__hero-kicker">{{ product.productKey }}</span>
          <h2>{{ product.productName || product.productKey }}</h2>
          <p>{{ heroDescription }}</p>
        </div>

        <div class="product-detail-page__hero-metrics">
          <article
            v-for="item in heroMetrics"
            :key="item.key"
            class="product-detail-page__metric-card"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small v-if="item.hint">{{ item.hint }}</small>
          </article>
        </div>
      </section>

      <nav class="product-detail-page__tabs" aria-label="产品工作区导航">
        <RouterLink
          v-for="item in tabItems"
          :key="item.key"
          :to="item.to"
          class="product-detail-page__tab"
          :class="{ 'product-detail-page__tab--active': activeSection === item.key }"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.caption }}</small>
        </RouterLink>
      </nav>

      <section class="product-detail-page__content">
        <div v-if="errorMessage" class="product-detail-page__state product-detail-page__state--error">
          <strong>{{ errorMessage }}</strong>
        </div>

        <template v-if="activeSection === 'overview'">
          <div class="product-detail-page__overview-grid">
            <article
              v-for="item in overviewCards"
              :key="item.key"
              class="product-detail-page__overview-card"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small v-if="item.hint">{{ item.hint }}</small>
            </article>
          </div>
          <ProductDetailWorkbench :product="product" />
        </template>

        <ProductDeviceListWorkspace
          v-else-if="activeSection === 'devices'"
          :devices="devices"
          :pagination="devicePagination"
          :error-message="deviceErrorMessage"
          :empty="!devicesLoading && !deviceErrorMessage && devices.length === 0"
          :devices-loading="devicesLoading"
          @view-device="handleViewDevice"
          @page-change="handleDevicePageChange"
          @page-size-change="handleDevicePageSizeChange"
        />

        <ProductModelDesignerWorkspace
          v-else-if="activeSection === 'contracts'"
          :product="product"
          workspace-view="contracts"
          @product-updated="handleProductUpdated"
        />

        <ProductModelDesignerWorkspace
          v-else-if="activeSection === 'mapping-rules'"
          :product="product"
          workspace-view="mapping-rules"
          @product-updated="handleProductUpdated"
        />

        <ProductModelDesignerWorkspace
          v-else
          :product="product"
          workspace-view="releases"
          @product-updated="handleProductUpdated"
        />
      </section>
    </template>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { deviceApi } from '@/api/device'
import { productApi } from '@/api/product'
import { resolveRequestErrorMessage } from '@/api/request'
import StandardButton from '@/components/StandardButton.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import ProductDetailWorkbench from '@/components/product/ProductDetailWorkbench.vue'
import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'
import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import type { Device, Product, ProductOverviewSummary } from '@/types/api'
import {
  buildProductWorkbenchSectionPath,
  normalizeProductWorkbenchSection,
  type ProductWorkbenchSection
} from '@/utils/productWorkbenchRoutes'
import { formatDateTime } from '@/utils/format'

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
  setPageNum: setDevicePageNum,
  setPageSize: setDevicePageSize,
  resetTotal: resetDeviceTotal
} = useServerPagination(10)

const sectionLabels: Record<ProductWorkbenchSection, { label: string; caption: string; description: string }> = {
  overview: {
    label: '产品总览',
    caption: '概览',
    description: '查看产品档案、活跃度和最新发布状态。'
  },
  devices: {
    label: '关联设备',
    caption: '设备',
    description: '查看当前产品下的设备清单与最近上报。'
  },
  contracts: {
    label: '契约字段',
    caption: '契约',
    description: '只保留样本输入、识别结果、本次生效和当前已生效字段。'
  },
  'mapping-rules': {
    label: '映射规则',
    caption: '映射',
    description: '集中维护厂商字段映射建议与映射规则台账。'
  },
  releases: {
    label: '版本台账',
    caption: '版本',
    description: '查看发布批次、回滚试算和跨批次差异。'
  }
}

const productId = computed(() => String(route.params.productId || '').trim())
const activeSection = computed<ProductWorkbenchSection>(() => {
  if (route.name === 'product-devices') {
    return 'devices'
  }
  if (route.name === 'product-contracts') {
    return 'contracts'
  }
  if (route.name === 'product-mapping-rules') {
    return 'mapping-rules'
  }
  if (route.name === 'product-releases') {
    return 'releases'
  }
  return normalizeProductWorkbenchSection('overview')
})

const sectionMeta = computed(() => sectionLabels[activeSection.value])
const pageTitle = computed(() => sectionMeta.value.label)
const pageDescription = computed(() => sectionMeta.value.description)
const heroDescription = computed(() => product.value?.description?.trim() || sectionMeta.value.description)
const tabItems = computed(() =>
  (Object.keys(sectionLabels) as ProductWorkbenchSection[]).map((key) => ({
    key,
    label: sectionLabels[key].label,
    caption: sectionLabels[key].caption,
    to: buildProductWorkbenchSectionPath(productId.value, key)
  }))
)

const heroMetrics = computed(() => [
  {
    key: 'deviceCount',
    label: '关联设备',
    value: String(overviewSummary.value?.deviceCount ?? product.value?.deviceCount ?? 0),
    hint: `在线 ${overviewSummary.value?.onlineDeviceCount ?? product.value?.onlineDeviceCount ?? 0}`
  },
  {
    key: 'formalFieldCount',
    label: '正式字段',
    value: String(overviewSummary.value?.formalFieldCount ?? 0),
    hint: '产品正式物模型'
  },
  {
    key: 'latestReleaseBatchId',
    label: '最新批次',
    value: overviewSummary.value?.latestReleaseBatchId == null ? '--' : String(overviewSummary.value.latestReleaseBatchId),
    hint: overviewSummary.value?.latestReleaseStatus || '尚未发布'
  }
])

const overviewCards = computed(() => [
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDateTime(overviewSummary.value?.lastReportTime || product.value?.lastReportTime),
    hint: '按产品下设备最近一次上报聚合'
  },
  {
    key: 'latestReleaseCreateTime',
    label: '最新发布时间',
    value: formatDateTime(overviewSummary.value?.latestReleaseCreateTime),
    hint: overviewSummary.value?.latestReleaseStatus || '未形成发布批次'
  },
  {
    key: 'latestReleasedFieldCount',
    label: '最近发布字段数',
    value: String(overviewSummary.value?.latestReleasedFieldCount ?? 0),
    hint: '按最新发布批次统计'
  }
])

async function loadProductWorkspace() {
  if (!productId.value) {
    product.value = null
    overviewSummary.value = null
    devices.value = []
    resetDeviceTotal()
    errorMessage.value = '产品编号缺失，无法打开工作区。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const [detailResult, summaryResult] = await Promise.allSettled([
      productApi.getProductById(productId.value),
      productApi.getProductOverviewSummary(productId.value)
    ])

    if (detailResult.status !== 'fulfilled' || detailResult.value.code !== 200 || !detailResult.value.data) {
      throw new Error('产品详情加载失败')
    }

    product.value = detailResult.value.data
    overviewSummary.value =
      summaryResult.status === 'fulfilled' && summaryResult.value.code === 200
        ? summaryResult.value.data || null
        : null

    if (activeSection.value === 'devices') {
      await loadDevices()
    } else {
      devices.value = []
      resetDeviceTotal()
      deviceErrorMessage.value = ''
      devicesLoading.value = false
    }
  } catch (error) {
    product.value = null
    overviewSummary.value = null
    devices.value = []
    resetDeviceTotal()
    errorMessage.value = resolveRequestErrorMessage(error, '加载产品工作区失败')
  } finally {
    loading.value = false
  }
}

async function loadDevices() {
  if (!product.value?.productKey) {
    devices.value = []
    resetDeviceTotal()
    deviceErrorMessage.value = '产品 Key 缺失，无法加载关联设备。'
    return
  }

  devicesLoading.value = true
  deviceErrorMessage.value = ''
  try {
    const response = await deviceApi.pageDevices({
      productKey: product.value.productKey,
      pageNum: devicePagination.pageNum,
      pageSize: devicePagination.pageSize
    })
    if (response.code === 200) {
      devices.value = applyDevicePageResult(response.data)
      return
    }
    devices.value = []
    resetDeviceTotal()
  } catch (error) {
    devices.value = []
    resetDeviceTotal()
    deviceErrorMessage.value = resolveRequestErrorMessage(error, '加载关联设备失败')
  } finally {
    devicesLoading.value = false
  }
}

function handleDevicePageChange(page: number) {
  if (page === devicePagination.pageNum) {
    return
  }
  setDevicePageNum(page)
  void loadDevices()
}

function handleDevicePageSizeChange(size: number) {
  if (size === devicePagination.pageSize) {
    return
  }
  setDevicePageSize(size)
  void loadDevices()
}

function handleBackToList() {
  void router.push('/products')
}

function handleRefresh() {
  void loadProductWorkspace()
}

function handleViewDevice(device: Device) {
  if (!device.deviceCode) {
    return
  }
  void router.push({
    path: '/devices',
    query: {
      deviceCode: device.deviceCode
    }
  })
}

function handleProductUpdated(updatedProduct: Product) {
  product.value = updatedProduct
  void loadProductWorkspace()
}

watch(
  () => productId.value,
  (current, previous) => {
    if (current === previous) {
      return
    }
    devices.value = []
    resetDevicePage()
    resetDeviceTotal()
  }
)

watch(
  () => `${productId.value}:${activeSection.value}`,
  () => {
    void loadProductWorkspace()
  },
  { immediate: true }
)
</script>

<style scoped>
.product-detail-page {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.product-detail-page__state,
.product-detail-page__hero,
.product-detail-page__metric-card,
.product-detail-page__overview-card,
.product-detail-page__content {
  display: grid;
}

.product-detail-page__state {
  gap: 0.28rem;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand-light) 8%, white));
  color: var(--text-secondary);
}

.product-detail-page__state--error {
  border-color: color-mix(in srgb, var(--danger) 18%, var(--panel-border));
  color: color-mix(in srgb, var(--danger) 76%, var(--text-secondary));
}

.product-detail-page__hero {
  gap: 1rem;
  padding: 0.96rem 1.08rem 1.08rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-2xl) + 2px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 16rem),
    linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 18%, white), rgba(255, 255, 255, 0.98));
}

.product-detail-page__hero-copy {
  display: grid;
  gap: 0.28rem;
}

.product-detail-page__hero-kicker {
  color: color-mix(in srgb, var(--brand) 78%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.product-detail-page__hero-copy h2 {
  margin: 0;
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(1.34rem, 2vw, 1.76rem);
  line-height: 1.22;
}

.product-detail-page__hero-copy p {
  margin: 0;
  max-width: 54rem;
  color: var(--text-secondary);
  line-height: 1.66;
}

.product-detail-page__hero-metrics,
.product-detail-page__overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.84rem;
}

.product-detail-page__metric-card,
.product-detail-page__overview-card {
  gap: 0.22rem;
  padding: 0.78rem 0.86rem;
  border: 1px solid color-mix(in srgb, var(--brand) 9%, var(--panel-border));
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.9);
}

.product-detail-page__metric-card span,
.product-detail-page__overview-card span {
  color: var(--text-caption);
  font-size: 0.76rem;
}

.product-detail-page__metric-card strong,
.product-detail-page__overview-card strong {
  color: var(--text-heading);
  font-size: 1rem;
  line-height: 1.28;
}

.product-detail-page__metric-card small,
.product-detail-page__overview-card small {
  color: var(--text-secondary);
  line-height: 1.5;
}

.product-detail-page__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.72rem;
  margin-top: 0.12rem;
}

.product-detail-page__tab {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
  padding: 0.82rem 0.94rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-xl);
  background: var(--bg-card);
  color: var(--text-secondary);
  text-decoration: none;
  transition: border-color var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.product-detail-page__tab span {
  color: inherit;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-page__tab small {
  color: var(--text-caption);
  line-height: 1.5;
}

.product-detail-page__tab:hover {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  color: var(--brand);
  transform: translateY(-1px);
}

.product-detail-page__tab--active {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--panel-border));
  background: color-mix(in srgb, var(--brand-light) 18%, white);
  color: color-mix(in srgb, var(--brand) 82%, var(--text-heading));
}

.product-detail-page__content {
  gap: 1rem;
}

@media (max-width: 900px) {
  .product-detail-page__hero-metrics,
  .product-detail-page__overview-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-page__tabs {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .product-detail-page__tabs {
    grid-template-columns: 1fr;
  }
}
</style>
