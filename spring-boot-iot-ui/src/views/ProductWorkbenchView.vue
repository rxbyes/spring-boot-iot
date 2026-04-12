<template>
  <StandardPageShell class="product-asset-view">
    <StandardWorkbenchPanel
      title="产品定义中心"
      description="统一维护产品台账、协议绑定与接入契约。"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      :show-inline-state="showListInlineState"
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <!-- 快速搜索：支持产品名称、产品 Key、厂商关键词搜索 -->
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                placeholder="快速搜索（产品名称、产品 Key、厂商）"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.nodeType" placeholder="节点类型" clearable>
                <el-option label="直连设备" :value="1" />
                <el-option label="网关设备" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.status" placeholder="产品状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
            <StandardButton v-permission="'iot:products:add'" action="add" @click="handleAdd">新增产品</StandardButton>
          </template>
        </StandardListFilterHeader>
        <!-- 快速搜索标签 -->
        <div v-if="quickSearchKeyword" class="product-quick-search-tag">
          <el-tag closable class="product-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ quickSearchKeyword }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="removeAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #notices>
        <div class="product-governance-notice-stack">
          <el-alert
            :title="governanceSummaryTitle"
            type="info"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            v-if="governanceErrorMessage"
            :title="governanceErrorMessage"
            type="warning"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            v-else-if="governanceTaskItems.length"
            :title="`当前聚焦产品仍有 ${governanceTaskItems.length} 项治理待办`"
            type="warning"
            :closable="false"
            show-icon
            class="view-alert"
          >
            <ul class="product-governance-task-list">
              <li v-for="task in governanceTaskItems" :key="task.key">
                <div class="product-governance-task-list__content">
                  <strong>{{ task.title }}</strong>
                  <span>{{ task.detail }}</span>
                </div>
                <StandardButton action="refresh" link @click="openGovernanceTask(task.path)">去处理</StandardButton>
              </li>
            </ul>
          </el-alert>
          <el-alert
            v-else-if="governanceFocusProduct && !governanceLoading"
            title="当前聚焦产品治理链路已收口，可继续抽检契约发布与策略有效性。"
            type="success"
            :closable="false"
            show-icon
            class="view-alert"
          />
        </div>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `已选 ${selectedRows.length} 项`,
            `启用 ${enabledProductCount} 个`,
            `停用 ${disabledProductCount} 个`
          ]"
        >
          <template #right>
            <!-- 批量操作下拉菜单 -->
            <el-dropdown
              v-permission="'iot:products:update'"
              :disabled="selectedRows.length === 0"
              trigger="click"
              @command="(command) => handleBatchCommand(command, selectedRows)"
            >
              <StandardActionLink :disabled="selectedRows.length === 0">
                <span>批量操作</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </StandardActionLink>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="enable" divided>
                    <el-icon><Top /></el-icon>
                    <span>启用</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="disable">
                    <el-icon><Bottom /></el-icon>
                    <span>停用</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="delete">
                    <el-icon><Delete /></el-icon>
                    <span>删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <StandardActionMenu
              label="更多操作"
              :items="productToolbarActions"
              @command="handleToolbarAction"
            />
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <template #inline-state>
        <StandardInlineState
          :message="workbenchInlineMessage"
          :tone="workbenchInlineTone"
        />
      </template>

      <div
        v-loading="loading && hasRecords"
        class="product-result-panel standard-list-surface"
        element-loading-text="正在刷新产品列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="product-loading-state" aria-live="polite" aria-busy="true">
          <div class="product-loading-state__summary">
            <span v-for="item in 3" :key="item" class="product-loading-pulse product-loading-pill" />
          </div>

          <div class="product-loading-state__desktop">
            <div class="product-loading-table product-loading-table--header">
              <span v-for="item in 8" :key="`head-${item}`" class="product-loading-pulse product-loading-line product-loading-line--header" />
            </div>
            <div v-for="row in 5" :key="`row-${row}`" class="product-loading-table product-loading-table--row">
              <span class="product-loading-pulse product-loading-square" />
              <span class="product-loading-pulse product-loading-line product-loading-line--key" />
              <span class="product-loading-pulse product-loading-line product-loading-line--title" />
              <span class="product-loading-pulse product-loading-line product-loading-line--short" />
              <span class="product-loading-pulse product-loading-line product-loading-line--short" />
              <span class="product-loading-pulse product-loading-line product-loading-line--meta" />
              <span class="product-loading-pulse product-loading-pill product-loading-pill--status" />
              <span class="product-loading-pulse product-loading-line product-loading-line--time" />
            </div>
          </div>

          <div class="product-loading-state__mobile">
            <article v-for="card in 3" :key="`card-${card}`" class="product-loading-mobile-card">
              <div class="product-loading-mobile-card__header">
                <span class="product-loading-pulse product-loading-square" />
                <div class="product-loading-mobile-card__heading">
                  <span class="product-loading-pulse product-loading-line product-loading-line--title" />
                  <span class="product-loading-pulse product-loading-line product-loading-line--meta" />
                </div>
                <span class="product-loading-pulse product-loading-pill product-loading-pill--status" />
              </div>
              <div class="product-loading-mobile-card__meta">
                <span v-for="item in 3" :key="`meta-${card}-${item}`" class="product-loading-pulse product-loading-pill" />
              </div>
              <div class="product-loading-mobile-card__info">
                <div v-for="item in 4" :key="`field-${card}-${item}`" class="product-loading-mobile-card__field">
                  <span class="product-loading-pulse product-loading-line product-loading-line--label" />
                  <span class="product-loading-pulse product-loading-line product-loading-line--value" />
                </div>
              </div>
            </article>
          </div>
        </div>

        <template v-else-if="hasRecords">
          <div class="product-mobile-list standard-mobile-record-list">
            <div class="product-mobile-list__grid standard-mobile-record-grid">
              <article
                v-for="row in tableData"
                :key="getProductRowKey(row)"
                class="product-mobile-card standard-mobile-record-card"
              >
                <div class="product-mobile-card__header">
                  <el-checkbox
                    :model-value="isRowSelected(row)"
                    @change="(checked) => handleMobileSelectionChange(row, Boolean(checked))"
                  />
                  <div class="product-mobile-card__heading">
                    <strong class="product-mobile-card__title">{{ row.productName || '--' }}</strong>
                    <span class="product-mobile-card__sub">{{ row.productKey || '--' }}</span>
                  </div>
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" round>{{ getStatusText(row.status) }}</el-tag>
                </div>

                <div class="product-mobile-card__meta">
                  <span class="product-mobile-card__meta-item standard-mobile-record-card__meta-item">{{ getNodeTypeText(row.nodeType) }}</span>
                  <span class="product-mobile-card__meta-item standard-mobile-record-card__meta-item">{{ row.protocolCode || '--' }}</span>
                  <span class="product-mobile-card__meta-item standard-mobile-record-card__meta-item">{{ row.dataFormat || '--' }}</span>
                </div>

                <div class="product-mobile-card__info">
                  <div class="product-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">厂商</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.manufacturer) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">关联设备</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatCount(row.deviceCount) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">最近上报</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatDateTime(row.lastReportTime) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">更新时间</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatDateTime(row.updateTime) }}</strong>
                  </div>
                </div>

                <StandardWorkbenchRowActions
                  variant="card"
                  class="product-mobile-card__actions"
                  :direct-items="getProductDirectActions('card')"
                  :menu-items="productRowActions"
                  @command="(command) => handleRowAction(command, row)"
                />
              </article>
            </div>
          </div>

          <el-table
            ref="tableRef"
            class="product-desktop-table"
            :data="tableData"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="productKey" label="产品 Key" :min-width="170" />
            <StandardTableTextColumn prop="productName" label="产品名称" :min-width="180" />
            <StandardTableTextColumn prop="protocolCode" label="协议编码" :width="140" />
            <el-table-column prop="nodeType" label="节点类型" width="120">
              <template #default="{ row }">
                <el-tag round>{{ getNodeTypeText(row.nodeType) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="dataFormat" label="数据格式" :width="120" />
            <StandardTableTextColumn prop="manufacturer" label="厂商" :min-width="150" />
            <el-table-column prop="status" label="产品状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="onlineDeviceCount" label="在线设备数" width="110" align="center" />
            <StandardTableTextColumn prop="lastReportTime" label="最近设备上报" :width="180">
              <template #default="{ row }">{{ formatDateTime(row.lastReportTime) }}</template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="updateTime" label="更新时间" :width="180">
              <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
            </StandardTableTextColumn>
            <el-table-column
              label="操作"
              :width="productActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getProductDirectActions('table')"
                  :menu-items="productRowActions"
                  @command="(command) => handleRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="product-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="product-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else v-permission="'iot:products:add'" action="add" @click="handleAdd">新增产品</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <ProductBusinessWorkbenchDrawer
      v-model="businessWorkbenchVisible"
      :active-view="businessWorkbenchActiveView"
      :product="businessWorkbenchProduct"
      @update:activeView="handleBusinessWorkbenchViewChange"
    >
      <template #header-actions>
        <StandardButton data-testid="open-product-workbench-edit" @click="handleBusinessWorkbenchEdit">
          编辑档案
        </StandardButton>
      </template>

      <template #overview>
        <ProductDetailWorkbench
          v-if="businessWorkbenchLoadedViews.overview && (detailData || businessWorkbenchProduct)"
          :product="detailData || businessWorkbenchProduct"
        />
      </template>

      <template #models>
        <ProductModelDesignerWorkspace
          v-if="businessWorkbenchLoadedViews.models && businessWorkbenchProduct"
          :product="businessWorkbenchProduct"
          @product-updated="handleBusinessWorkbenchProductUpdated"
        />
      </template>

      <template #devices>
        <ProductDeviceListWorkspace
          v-if="businessWorkbenchLoadedViews.devices && businessWorkbenchProduct"
          :devices="deviceListData"
          :loading="devicesLoading && deviceListData.length === 0 && !deviceListErrorMessage"
          :error-message="deviceListErrorMessage"
          :empty="!devicesLoading && !deviceListErrorMessage && deviceListTotal === 0"
          :devices-loading="devicesLoading"
          @view-device="handleViewDevice"
        />
      </template>

      <template #edit>
        <ProductEditWorkspace
          v-if="businessWorkbenchLoadedViews.edit"
          ref="editWorkspaceRef"
          :model="formData"
          :object-insight-metrics="objectInsightMetricRows"
          :available-models="editAvailableModels"
          :rules="formRules"
          :editing="Boolean(editingProductId)"
          :submit-loading="submitLoading"
          :refresh-state="formRefreshState"
          :refresh-message="formRefreshMessage"
          @update:object-insight-metrics="handleObjectInsightMetricsChange"
          @cancel="handleCancelEdit"
          @submit="handleSubmit"
        />
      </template>
    </ProductBusinessWorkbenchDrawer>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      size="42rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <el-form ref="createFormRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>基础档案</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="产品 Key" prop="productKey">
                <el-input
                  id="product-key"
                  v-model="formData.productKey"
                  :disabled="Boolean(editingProductId)"
                  placeholder="请输入产品 Key，例如 accept-http-product-01"
                />
              </el-form-item>
              <el-form-item label="产品名称" prop="productName">
                <el-input id="product-name" v-model="formData.productName" placeholder="请输入产品名称" />
              </el-form-item>
              <el-form-item label="厂商">
                <el-input v-model="formData.manufacturer" placeholder="请输入厂商名称" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>接入基线</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="协议编码" prop="protocolCode">
                <el-input id="protocol-code" v-model="formData.protocolCode" placeholder="请输入协议编码，例如 mqtt-json" />
              </el-form-item>
              <el-form-item label="节点类型" prop="nodeType">
                <el-select v-model="formData.nodeType" placeholder="请选择节点类型">
                  <el-option label="直连设备" :value="1" />
                  <el-option label="网关设备" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据格式">
                <el-input id="data-format" v-model="formData.dataFormat" placeholder="请输入数据格式，例如 JSON" />
              </el-form-item>
              <el-form-item label="产品状态">
                <el-select v-model="formData.status" placeholder="请选择产品状态">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>补充说明</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="说明" class="ops-drawer-grid__full">
                <el-input v-model="formData.description" type="textarea" :rows="5" placeholder="请输入产品说明、接入约束或适用场景" />
              </el-form-item>
            </div>
          </section>

          <ProductObjectInsightConfigEditor
            :model-value="objectInsightMetricRows"
            :available-models="[]"
            @update:model-value="handleObjectInsightMetricsChange"
          />
        </el-form>
      </div>

      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          :confirm-text="submitButtonText"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </StandardButton>
          <StandardButton
            id="product-submit-button"
            v-permission="submitPermission"
            action="confirm"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ submitButtonText }}
          </StandardButton>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="产品导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />

  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ArrowDown, Bottom, Delete, Top } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import ProductBusinessWorkbenchDrawer, {
  type ProductBusinessWorkbenchView
} from '@/components/product/ProductBusinessWorkbenchDrawer.vue'
import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'
import ProductDetailWorkbench from '@/components/product/ProductDetailWorkbench.vue'
import ProductEditWorkspace from '@/components/product/ProductEditWorkspace.vue'
import ProductModelDesignerWorkspace from '@/components/product/ProductModelDesignerWorkspace.vue'
import ProductObjectInsightConfigEditor from '@/components/product/ProductObjectInsightConfigEditor.vue'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import { productApi, type ProductContractReleaseBatch } from '@/api/product'
import { deviceApi } from '@/api/device'
import { getRiskGovernanceCoverageOverview, type RiskGovernanceCoverageOverview } from '@/api/riskGovernance'
import { useServerPagination } from '@/composables/useServerPagination'
import { recordActivity } from '@/stores/activity'
import { usePermissionStore } from '@/stores/permission'
import type {
  Device,
  PageResult,
  Product,
  ProductAddPayload,
  ProductModel,
  ProductObjectInsightCustomMetricConfig
} from '@/types/api'
import {
  buildProductPageCacheKey,
  cloneProductDetailCacheEntry,
  cloneProductPageCacheEntry,
  createProductDetailCacheEntry,
  createProductPageCacheEntry,
  deserializeProductDetailCacheEntries,
  deserializeProductPageCacheEntries,
  getNextProductPageQuery,
  getProductRowKey,
  isProductDetailCacheFresh,
  isProductPageCacheFresh,
  matchesProductFilters,
  mergeLocalProductRow,
  prependLocalProductRow,
  type ProductDetailCacheEntry,
  type ProductPageCacheEntry,
  type ProductPageQuerySnapshot,
  removeLocalProductRow,
  removeSelectedProductSnapshot,
  resolveProductPageLoadStrategy,
  replaceSelectedProductSnapshot,
  serializeProductDetailCacheEntries,
  serializeProductPageCacheEntries,
  shouldRefreshProductDetail
} from '@/views/productWorkbenchState'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'
import { formatDateTime } from '@/utils/format'
import { describeDiagnosticSource, resolveDiagnosticContext } from '@/utils/iotAccessDiagnostics'
import {
  buildProductMetadataJson,
  parseProductObjectInsightMetrics,
  validateProductObjectInsightMetrics
} from '@/utils/productObjectInsightConfig'

function formatCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

function formatPercentValue(value?: number | null, digits = 1) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '--'
  }
  return `${numeric.toFixed(digits)}%`
}

function resolvePublishableContractPropertyCount(coverage?: RiskGovernanceCoverageOverview | null) {
  const publishableCount = Number(coverage?.publishableContractPropertyCount)
  if (Number.isFinite(publishableCount) && publishableCount >= 0) {
    return publishableCount
  }
  const fallbackContractCount = Number(coverage?.contractPropertyCount)
  return Number.isFinite(fallbackContractCount) && fallbackContractCount >= 0 ? fallbackContractCount : 0
}

interface ProductSearchForm {
  productName: string
  nodeType: number | undefined
  status: number | undefined
}

type ProductFilterKey = keyof ProductSearchForm

interface ProductFormState extends ProductAddPayload {}

interface ProductEditWorkspaceExposed {
  validate: () => Promise<boolean>
  clearValidate: () => void
}

interface ProductRowAction {
  key?: string
  command: 'delete'
  label: string
}

interface ProductDirectAction {
  key?: string
  command: 'detail' | 'delete'
  label: string
  disabled?: boolean
  title?: string
  dataTestid?: string
}

interface ProductToolbarAction {
  key?: string
  command: 'export-config' | 'export-selected' | 'export-current' | 'clear-selection'
  label: string
  disabled?: boolean
}

interface GovernanceTaskItem {
  key: 'pending-contract-release' | 'pending-metric-publish' | 'pending-risk-binding' | 'pending-policy'
  title: string
  detail: string
  path: string
}

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const tableRef = ref<TableInstance>()
const createFormRef = ref<FormInstance>()
const editWorkspaceRef = ref<ProductEditWorkspaceExposed>()

const loading = ref(false)
const submitLoading = ref(false)
const formVisible = ref(false)
const formRefreshing = ref(false)
const detailLoading = ref(false)
const diagnosticContext = computed(() => resolveDiagnosticContext(route.query as Record<string, unknown>))
const businessWorkbenchVisible = ref(false)
const businessWorkbenchActiveView = ref<ProductBusinessWorkbenchView>('overview')
const businessWorkbenchProduct = ref<Product | null>(null)
const handledGovernanceWorkbenchRouteKey = ref('')
const businessWorkbenchLoadedViews = reactive<Record<ProductBusinessWorkbenchView, boolean>>({
  overview: false,
  models: false,
  devices: false,
  edit: false
})

// 设备列表抽屉相关状态
const deviceListData = ref<Device[]>([])
const deviceListTotal = ref(0)
const deviceListOnlineCount = ref(0)
const deviceListOfflineCount = ref(0)
const devicesLoading = ref(false)
const deviceListErrorMessage = ref('')

// 当前选择的产品
const currentProduct = ref<Product | null>(null)
const editAvailableModels = ref<ProductModel[]>([])
const detailRefreshing = ref(false)
const detailErrorMessage = ref('')
const listRefreshMessage = ref('')
const listRefreshState = ref<'info' | 'error' | ''>('')
const formRefreshMessage = ref('')
const formRefreshState = ref<'info' | 'warning' | 'error' | ''>('')
// detailRefreshErrorMessage 已移除，不再
const editingProductId = ref<string | number | null>(null)

const tableData = ref<Product[]>([])
const selectedRows = ref<Product[]>([])
const detailData = ref<Product | null>(null)
const governanceLoading = ref(false)
const governanceErrorMessage = ref('')
const governanceCoverageOverview = ref<RiskGovernanceCoverageOverview | null>(null)
const latestContractReleaseBatch = ref<ProductContractReleaseBatch | null>(null)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'product-definition-center'
const defaultPageSize = 10
let latestListRequestId = 0
let latestDetailRequestId = 0
let latestEditRequestId = 0
let latestEditModelRequestId = 0
let latestGovernanceRequestId = 0
let listAbortController: AbortController | null = null
let listPrefetchAbortController: AbortController | null = null
let detailAbortController: AbortController | null = null
let editAbortController: AbortController | null = null
const productDetailCache = new Map<string, ProductDetailCacheEntry>()
const productPageCache = new Map<string, ProductPageCacheEntry>()
const productDetailCacheTtlMs = 5 * 60_000
const productDetailCacheLimit = 12
const productDetailCacheSessionStorageKey = 'iot.products.detail-cache'
const productPageCacheTtlMs = 30_000
const productPageCacheLimit = 8
const productPageCacheSessionStorageKey = 'iot.products.page-cache'
let activeEditSessionId = 0
let formDirtySinceOpen = false
let suppressFormDirtyTracking = false

const searchForm = reactive<ProductSearchForm>({
  productName: '',
  nodeType: undefined,
  status: undefined
})
const appliedFilters = reactive<ProductSearchForm>({
  productName: '',
  nodeType: undefined,
  status: undefined
})

const quickSearchKeyword = computed({
  get: () => searchForm.productName,
  set: (value: string) => {
    searchForm.productName = value
  }
})

const createDefaultFormData = (): ProductFormState => ({
  productKey: '',
  productName: '',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: '',
  description: '',
  metadataJson: '',
  status: 1
})

const formData = reactive<ProductFormState>(createDefaultFormData())
const objectInsightMetricRows = ref<ProductObjectInsightCustomMetricConfig[]>([])

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize, setTotal } = useServerPagination(defaultPageSize)

const formTitle = computed(() => (editingProductId.value ? '编辑产品' : '新增产品'))
const submitButtonText = computed(() => (editingProductId.value ? '保存' : '新增'))
const submitPermission = computed(() => (editingProductId.value ? 'iot:products:update' : 'iot:products:add'))
const enabledProductCount = computed(() => tableData.value.filter((item) => item.status !== 0).length)
const disabledProductCount = computed(() => tableData.value.filter((item) => item.status === 0).length)
const hasRecords = computed(() => tableData.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const diagnosticEntryMessage = computed(() => {
  if (!diagnosticContext.value) {
    return ''
  }
  const sourceLabel = describeDiagnosticSource(diagnosticContext.value.sourcePage)
  const traceLabel = diagnosticContext.value.traceId ? `Trace ${diagnosticContext.value.traceId}` : ''
  return [sourceLabel ? `来自${sourceLabel}` : '', traceLabel, '优先核对产品契约、协议编码与物模型完整性。']
    .filter(Boolean)
    .join(' · ')
})
const workbenchInlineMessage = computed(() => listRefreshMessage.value || diagnosticEntryMessage.value)
const workbenchInlineTone = computed<'info' | 'error'>(() => (listRefreshState.value === 'error' ? 'error' : 'info'))
const showListInlineState = computed(() => Boolean(workbenchInlineMessage.value) && (hasRecords.value || Boolean(diagnosticEntryMessage.value)))
const governanceFocusProduct = computed(() => businessWorkbenchProduct.value || currentProduct.value || tableData.value[0] || null)
const productWorkbenchRouteContextKeys = [
  'openProductId',
  'workbenchView',
  'governanceSource',
  'workItemCode',
  'governanceBoundary',
  'subjectOwnership',
  'governanceFocus'
] as const
const governanceTaskItems = computed<GovernanceTaskItem[]>(() => {
  if (!governanceFocusProduct.value || governanceLoading.value || !governanceCoverageOverview.value) {
    return []
  }

  const tasks: GovernanceTaskItem[] = []
  const coverage = governanceCoverageOverview.value
  const productId = governanceFocusProduct.value.id
  const hasReleaseBatch = Boolean(latestContractReleaseBatch.value?.id)
  const publishableContractPropertyCount = resolvePublishableContractPropertyCount(coverage)

  if (!hasReleaseBatch) {
    tasks.push({
      key: 'pending-contract-release',
      title: '待发布合同',
      detail: '当前产品还没有正式合同发布批次，请先完成 compare/apply 并发布。',
      path: buildGovernanceTaskPath(productId, 'PENDING_CONTRACT_RELEASE')
    })
  }

  if (publishableContractPropertyCount > Number(coverage.publishedRiskMetricCount ?? 0)) {
    tasks.push({
      key: 'pending-metric-publish',
      title: '待发布风险指标目录',
      detail: `可入目录字段 ${formatCount(publishableContractPropertyCount)} 项中，目录发布 ${formatCount(coverage.publishedRiskMetricCount)} 项。`,
      path: buildProductWorkbenchPath(productId, 'models')
    })
  }

  if (Number(coverage.bindingCoverageRate ?? 0) < 100) {
    tasks.push({
      key: 'pending-risk-binding',
      title: '待绑定风险点',
      detail: `目录指标 ${formatCount(coverage.publishedRiskMetricCount)} 项中，已绑定 ${formatCount(coverage.boundRiskMetricCount)} 项。`,
      path: buildGovernanceTaskPath(productId, 'PENDING_RISK_BINDING')
    })
  }

  if (Number(coverage.ruleCoverageRate ?? 0) < 100) {
    tasks.push({
      key: 'pending-policy',
      title: '待补阈值策略',
      detail: `已绑定指标 ${formatCount(coverage.boundRiskMetricCount)} 项中，策略覆盖 ${formatCount(coverage.ruleCoveredRiskMetricCount)} 项。`,
      path: '/rule-definition'
    })
  }

  return tasks
})
const governanceSummaryTitle = computed(() => {
  const focusProduct = governanceFocusProduct.value
  if (!focusProduct) {
    return '请先选择产品，系统会自动展示当前聚焦产品的治理链路进度。'
  }
  if (governanceLoading.value) {
    return `正在同步 ${focusProduct.productName || focusProduct.productKey || '当前产品'} 的治理进度...`
  }
  if (!governanceCoverageOverview.value) {
    return `当前聚焦产品：${focusProduct.productName || focusProduct.productKey || '--'}。治理概览暂未返回，请稍后重试。`
  }

  const coverage = governanceCoverageOverview.value
  const publishableContractPropertyCount = resolvePublishableContractPropertyCount(coverage)
  const summarySegments = [
    `当前聚焦产品：${focusProduct.productName || focusProduct.productKey || '--'}`,
    `合同字段 ${formatCount(coverage.contractPropertyCount)} 项`,
    publishableContractPropertyCount < Number(coverage.contractPropertyCount ?? 0)
      ? `可入目录 ${formatCount(publishableContractPropertyCount)} 项`
      : null,
    `目录发布 ${formatCount(coverage.publishedRiskMetricCount)} 项`,
    `风险点绑定 ${formatCount(coverage.boundRiskMetricCount)} 项`,
    `策略覆盖率 ${formatPercentValue(coverage.ruleCoverageRate)}`
  ].filter((segment): segment is string => Boolean(segment))
  return `${summarySegments.join('，')}。`
})
const productRowActions = computed<ProductRowAction[]>(() => [])
const productActionColumnWidth = computed(() =>
  resolveWorkbenchActionColumnWidth({
    directItems: getProductDirectActions('table')
  })
)
const productToolbarActions = computed<ProductToolbarAction[]>(() => {
  const actions: ProductToolbarAction[] = []

  if (permissionStore.hasPermission('iot:products:export')) {
    actions.push({ key: 'export-config', command: 'export-config', label: '导出列设置' })
    actions.push({
      key: 'export-selected',
      command: 'export-selected',
      label: '导出选中',
      disabled: selectedRows.value.length === 0
    })
    actions.push({
      key: 'export-current',
      command: 'export-current',
      label: '导出当前结果',
      disabled: tableData.value.length === 0
    })
  }

  actions.push({
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  })

  return actions
})
const activeFilterTags = computed(() => {
  const tags: Array<{ key: ProductFilterKey; label: string }> = []
  const productName = appliedFilters.productName.trim()
  if (productName) {
    tags.push({ key: 'productName', label: `快速搜索：${productName}` })
  }
  if (appliedFilters.nodeType !== undefined) {
    tags.push({ key: 'nodeType', label: `节点类型：${getNodeTypeText(appliedFilters.nodeType)}` })
  }
  if (appliedFilters.status !== undefined) {
    tags.push({ key: 'status', label: `产品状态：${getStatusText(appliedFilters.status)}` })
  }
  return tags
})
const hasAppliedFilters = computed(() => activeFilterTags.value.length > 0)
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的产品' : '还没有产品定义'))
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有产品定义，先新增产品，再继续设备接入、建档和维护。'
)

const formRules: FormRules<ProductFormState> = {
  productKey: [{ required: true, message: '请输入产品 Key', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  protocolCode: [{ required: true, message: '请输入协议编码', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
}

const exportColumns: CsvColumn<Product>[] = [
  { key: 'id', label: '产品 ID' },
  { key: 'productKey', label: '产品 Key' },
  { key: 'productName', label: '产品名称' },
  { key: 'protocolCode', label: '协议编码' },
  { key: 'nodeType', label: '节点类型', formatter: (value) => getNodeTypeText(Number(value)) },
  { key: 'status', label: '产品状态', formatter: (value) => getStatusText(Number(value)) },
  { key: 'dataFormat', label: '数据格式' },
  { key: 'manufacturer', label: '厂商' },
  { key: 'onlineDeviceCount', label: '在线设备数' },
  { key: 'lastReportTime', label: '最近设备上报', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'createTime', label: '创建时间', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'updateTime', label: '更新时间', formatter: (value) => formatDateTime(String(value || '')) }
]

const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '状态核查', keys: ['productKey', 'productName', 'status', 'onlineDeviceCount', 'lastReportTime'] },
  {
    label: '基础档案',
    keys: ['id', 'productKey', 'productName', 'protocolCode', 'nodeType', 'dataFormat', 'manufacturer', 'createTime', 'updateTime']
  }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const selectedRowKeySet = computed(() => new Set(selectedRows.value.map((item) => getProductRowKey(item)).filter(Boolean)))

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  return '--'
}

function getStatusText(value?: number | null) {
  return value === 0 ? '停用' : '启用'
}

function formatTextValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function getProductDirectActions(variant: 'table' | 'card'): ProductDirectAction[] {
  const actions: ProductDirectAction[] = [
    {
      key: 'detail',
      command: 'detail',
      label: '进入工作台',
      title: '进入产品经营工作台',
      dataTestid: 'open-product-business-workbench'
    }
  ]

  if (permissionStore.hasPermission('iot:products:delete')) {
    actions.push({
      key: 'delete',
      command: 'delete',
      label: '删除'
    })
  }

  return actions
}

function getProductDetailCacheKey(row?: Partial<Product> | null) {
  return getProductRowKey(row)
}

function getCachedProductDetail(row?: Partial<Product> | null) {
  const cacheKey = getProductDetailCacheKey(row)
  if (!cacheKey) {
    return null
  }
  const entry = cloneProductDetailCacheEntry(productDetailCache.get(cacheKey))
  if (!entry) {
    return null
  }
  if (!isProductDetailCacheFresh(entry, productDetailCacheTtlMs)) {
    productDetailCache.delete(cacheKey)
    persistProductDetailCache()
    return null
  }
  return { ...entry.detail }
}

function cacheProductDetail(product?: Product | null) {
  const cacheKey = getProductDetailCacheKey(product)
  if (!cacheKey || !product) {
    return
  }
  const entry = createProductDetailCacheEntry(product)
  productDetailCache.delete(cacheKey)
  productDetailCache.set(cacheKey, entry)

  while (productDetailCache.size > productDetailCacheLimit) {
    const oldestKey = productDetailCache.keys().next().value
    if (!oldestKey) {
      break
    }
    productDetailCache.delete(oldestKey)
  }

  persistProductDetailCache()
}

function removeCachedProductDetail(row?: Partial<Product> | null) {
  const cacheKey = getProductDetailCacheKey(row)
  if (!cacheKey) {
    return
  }
  productDetailCache.delete(cacheKey)
  persistProductDetailCache()
}

function resolveDetailSnapshot(row: Product, cachedDetail: Product | null) {
  // 使用列表返回的 row 数据作为快照，确保详情页始终有数据显示
  return {
    ...row,
    description: cachedDetail?.description ?? row.description ?? null,
    metadataJson: cachedDetail?.metadataJson ?? row.metadataJson ?? null
  }
}

function abortListRequest() {
  listAbortController?.abort()
  listAbortController = null
}

function abortListPrefetchRequest() {
  listPrefetchAbortController?.abort()
  listPrefetchAbortController = null
}

function abortDetailRequest() {
  detailAbortController?.abort()
  detailAbortController = null
}

function abortEditRequest() {
  editAbortController?.abort()
  editAbortController = null
}

function resetFormData(source?: Partial<Product>) {
  Object.assign(formData, createDefaultFormData(), {
    productKey: source?.productKey || '',
    productName: source?.productName || '',
    protocolCode: source?.protocolCode || 'mqtt-json',
    nodeType: source?.nodeType ?? 1,
    dataFormat: source?.dataFormat || 'JSON',
    manufacturer: source?.manufacturer || '',
    description: source?.description || '',
    metadataJson: source?.metadataJson || '',
    status: source?.status ?? 1
  })
  objectInsightMetricRows.value = parseProductObjectInsightMetrics(source?.metadataJson)
}

function applyFormDataWithoutDirty(source?: Partial<Product>) {
  suppressFormDirtyTracking = true
  try {
    resetFormData(source)
  } finally {
    suppressFormDirtyTracking = false
  }
}

function clearFormRefreshState() {
  formRefreshing.value = false
  formRefreshMessage.value = ''
  formRefreshState.value = ''
}

function clearListRefreshState() {
  listRefreshMessage.value = ''
  listRefreshState.value = ''
}

function handleObjectInsightMetricsChange(value: ProductObjectInsightCustomMetricConfig[]) {
  objectInsightMetricRows.value = value
}

function handleBusinessWorkbenchProductUpdated(product: Product) {
  cacheProductDetail(product)
  currentProduct.value = product
  detailData.value = product
  businessWorkbenchProduct.value = product
  if (matchesCurrentFilters(product)) {
    mergeLocalTableRow(product)
  } else {
    removeLocalTableRow(product)
  }
  rebuildVisibleProductPageCache()
  if (editingProductId.value && String(editingProductId.value) === String(product.id) && !formDirtySinceOpen) {
    applyFormDataWithoutDirty(product)
  }
}

function markBusinessWorkbenchViewLoaded(view: ProductBusinessWorkbenchView) {
  businessWorkbenchLoadedViews[view] = true
}

function resetBusinessWorkbenchLoadedViews() {
  businessWorkbenchLoadedViews.overview = false
  businessWorkbenchLoadedViews.models = false
  businessWorkbenchLoadedViews.devices = false
  businessWorkbenchLoadedViews.edit = false
}

function getBusinessWorkbenchProductKey() {
  return businessWorkbenchProduct.value?.productKey || currentProduct.value?.productKey || ''
}

function handleBusinessWorkbenchViewChange(view: ProductBusinessWorkbenchView) {
  businessWorkbenchActiveView.value = view
  markBusinessWorkbenchViewLoaded(view)

  if (view === 'devices') {
    const productKey = getBusinessWorkbenchProductKey()
    if (productKey) {
      void loadDeviceList(productKey)
    }
  }
}

function handleCancelEdit() {
  const snapshot = businessWorkbenchProduct.value || currentProduct.value
  applyFormDataWithoutDirty(snapshot ?? undefined)
  clearFormRefreshState()
  editWorkspaceRef.value?.clearValidate()
  if (businessWorkbenchVisible.value) {
    handleBusinessWorkbenchViewChange('overview')
  }
}

function isAbortError(error: unknown) {
  return error instanceof Error && error.name === 'AbortError'
}

function buildCurrentProductPageQuery(): ProductPageQuerySnapshot {
  return {
    productName: searchForm.productName.trim(),
    nodeType: searchForm.nodeType,
    status: searchForm.status,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

function getProductPageSessionStorage() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage
}

function hydrateProductPageCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeProductPageCacheEntries(
    storage.getItem(productPageCacheSessionStorageKey),
    productPageCacheTtlMs,
    productPageCacheLimit
  )

  productPageCache.clear()
  entries.forEach((entry: ProductPageCacheEntry) => {
    productPageCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(productPageCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断页面加载
    }
  }
}

function hydrateProductDetailCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeProductDetailCacheEntries(
    storage.getItem(productDetailCacheSessionStorageKey),
    productDetailCacheTtlMs,
    productDetailCacheLimit
  )

  productDetailCache.clear()
  entries.forEach((entry: ProductDetailCacheEntry) => {
    productDetailCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(productDetailCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断页面加载
    }
  }
}

function persistProductPageCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (productPageCache.size === 0) {
      storage.removeItem(productPageCacheSessionStorageKey)
      return
    }
    storage.setItem(
      productPageCacheSessionStorageKey,
      serializeProductPageCacheEntries(productPageCache.values(), productPageCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断列表主流程
  }
}

function persistProductDetailCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (productDetailCache.size === 0) {
      storage.removeItem(productDetailCacheSessionStorageKey)
      return
    }
    storage.setItem(
      productDetailCacheSessionStorageKey,
      serializeProductDetailCacheEntries(productDetailCache.values(), productDetailCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断详情主流程
  }
}

function getCachedProductPage(query: ProductPageQuerySnapshot) {
  const cacheKey = buildProductPageCacheKey(query)
  return cloneProductPageCacheEntry(productPageCache.get(cacheKey))
}

function cacheProductPage(query: ProductPageQuerySnapshot, pageResult: PageResult<Product>) {
  const entry = createProductPageCacheEntry(query, pageResult)
  productPageCache.delete(entry.key)
  productPageCache.set(entry.key, entry)

  while (productPageCache.size > productPageCacheLimit) {
    const oldestKey = productPageCache.keys().next().value
    if (!oldestKey) {
      break
    }
    productPageCache.delete(oldestKey)
  }

  persistProductPageCache()
}

function applyCachedProductPage(entry: ProductPageCacheEntry) {
  tableData.value = applyPageResult({
    total: entry.total,
    pageNum: entry.pageNum,
    pageSize: entry.pageSize,
    records: entry.records
  })
  syncAppliedFilters()
  void syncTableSelection()
}

function cacheVisibleProductPage() {
  cacheProductPage(buildCurrentProductPageQuery(), {
    total: pagination.total,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    records: tableData.value
  })
}

function clearProductPageCache() {
  abortListPrefetchRequest()
  productPageCache.clear()
  persistProductPageCache()
}

function rebuildVisibleProductPageCache() {
  clearProductPageCache()
  if (pagination.total <= 0 || tableData.value.length === 0) {
    return
  }
  cacheVisibleProductPage()
}

async function prefetchNextProductPage(query: ProductPageQuerySnapshot, total: number) {
  const nextQuery = getNextProductPageQuery(query, total)
  if (!nextQuery) {
    return
  }

  const cachedPage = getCachedProductPage(nextQuery)
  if (isProductPageCacheFresh(cachedPage, productPageCacheTtlMs)) {
    return
  }

  abortListPrefetchRequest()
  const controller = new AbortController()
  listPrefetchAbortController = controller

  try {
    const res = await productApi.pageProducts(
      {
        productName: nextQuery.productName || undefined,
        nodeType: nextQuery.nodeType,
        status: nextQuery.status,
        pageNum: nextQuery.pageNum,
        pageSize: nextQuery.pageSize
      },
      {
        signal: controller.signal
      }
    )
    if (res.code === 200 && res.data) {
      cacheProductPage(nextQuery, res.data)
    }
  } catch (error) {
    if (!isAbortError(error)) {
      console.warn('预取产品分页失败', error)
    }
  } finally {
    if (listPrefetchAbortController === controller) {
      listPrefetchAbortController = null
    }
  }
}

function syncAppliedFilters() {
  appliedFilters.productName = searchForm.productName.trim()
  appliedFilters.nodeType = searchForm.nodeType
  appliedFilters.status = searchForm.status
}

function clearSearchForm() {
  searchForm.productName = ''
  searchForm.nodeType = undefined
  searchForm.status = undefined
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function matchesCurrentFilters(product: Product) {
  return matchesProductFilters(product, {
    productName: searchForm.productName,
    nodeType: searchForm.nodeType,
    status: searchForm.status
  })
}

// 快速搜索：支持产品名称、产品 Key、厂商关键词搜索
function handleQuickSearch() {
  const keyword = searchForm.productName.trim()
  if (!keyword) {
    return
  }

  searchForm.productName = keyword
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleClearQuickSearch() {
  searchForm.productName = ''
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function replaceSelectedRowSnapshot(product: Product) {
  selectedRows.value = replaceSelectedProductSnapshot(selectedRows.value, product)
}

function removeSelectedRowSnapshot(row?: Partial<Product> | null) {
  selectedRows.value = removeSelectedProductSnapshot(selectedRows.value, row)
}

function mergeLocalTableRow(product: Product) {
  const nextRows = mergeLocalProductRow(tableData.value, product)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleProductPage()
  replaceSelectedRowSnapshot(product)
  void syncTableSelection()
  return true
}

function prependLocalTableRow(product: Product) {
  tableData.value = prependLocalProductRow(tableData.value, product, pagination.pageSize)
  cacheVisibleProductPage()
  replaceSelectedRowSnapshot(product)
  void syncTableSelection()
}

function removeLocalTableRow(row?: Partial<Product> | null) {
  const nextRows = removeLocalProductRow(tableData.value, row)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleProductPage()
  removeSelectedRowSnapshot(row)
  void syncTableSelection()
  return true
}

async function syncTableSelection() {
  await nextTick()
  if (!tableRef.value) {
    return
  }
  tableRef.value.clearSelection()
  const selectedKeys = selectedRowKeySet.value
  tableData.value.forEach((row) => {
    if (selectedKeys.has(getProductRowKey(row))) {
      tableRef.value?.toggleRowSelection(row, true)
    }
  })
}

function handleSelectionChange(rows: Product[]) {
  selectedRows.value = rows
}

function isRowSelected(row: Product) {
  return selectedRowKeySet.value.has(getProductRowKey(row))
}

function handleMobileSelectionChange(row: Product, checked: boolean) {
  const rowKey = getProductRowKey(row)
  const nextRows = checked
    ? [...selectedRows.value.filter((item) => getProductRowKey(item) !== rowKey), row]
    : selectedRows.value.filter((item) => getProductRowKey(item) !== rowKey)
  selectedRows.value = tableData.value.filter((item) => nextRows.some((selected) => getProductRowKey(selected) === getProductRowKey(item)))
  void syncTableSelection()
}

function openExportColumnSetting() {
  exportColumnDialogVisible.value = true
}

function handleExportColumnConfirm(selectedKeys: string[]) {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys)
}

function getResolvedExportColumns() {
  return resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)
}

function handleExportSelected() {
  downloadRowsAsCsv('产品定义中心-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

function handleExportCurrent() {
  downloadRowsAsCsv('产品定义中心-当前结果.csv', tableData.value, getResolvedExportColumns())
}

function handleToolbarAction(command: string | number | object) {
  switch (command) {
    case 'export-config':
      openExportColumnSetting()
      break
    case 'export-selected':
      handleExportSelected()
      break
    case 'export-current':
      handleExportCurrent()
      break
    case 'clear-selection':
      clearSelection()
      break
    default:
      break
  }
}

function applyRouteQueryToFilters() {
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName.trim() : ''
  searchForm.nodeType = parseRouteNumberQuery(route.query.nodeType)
  searchForm.status = parseRouteNumberQuery(route.query.status)
  pagination.pageNum = parseRoutePositiveIntQuery(route.query.pageNum, 1)
  pagination.pageSize = parseRoutePositiveIntQuery(route.query.pageSize, defaultPageSize)
}

function parseRouteStringQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string') {
    return undefined
  }
  const trimmed = raw.trim()
  return trimmed || undefined
}

function resolveRouteWorkbenchView(value: unknown): ProductBusinessWorkbenchView {
  const view = parseRouteStringQuery(value)
  return view === 'overview' || view === 'models' || view === 'devices' || view === 'edit'
    ? view
    : 'overview'
}

function parseRouteNumberQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string' || raw.trim() === '') {
    return undefined
  }
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : undefined
}

function parseRoutePositiveIntQuery(value: unknown, fallback: number) {
  const parsed = parseRouteNumberQuery(value)
  if (!parsed || parsed < 1) {
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

function assignListQueryValue(
  query: Record<string, unknown>,
  key: 'productName' | 'nodeType' | 'status' | 'pageNum' | 'pageSize',
  value: string | number | undefined
) {
  if (value === undefined || value === '') {
    delete query[key]
    return
  }
  query[key] = String(value)
}

function hasSameListRouteQuery(nextQuery: Record<string, unknown>) {
  return (
    normalizeQueryValue(route.query.productName) === normalizeQueryValue(nextQuery.productName) &&
    normalizeQueryValue(route.query.nodeType) === normalizeQueryValue(nextQuery.nodeType) &&
    normalizeQueryValue(route.query.status) === normalizeQueryValue(nextQuery.status) &&
    normalizeQueryValue(route.query.pageNum) === normalizeQueryValue(nextQuery.pageNum) &&
    normalizeQueryValue(route.query.pageSize) === normalizeQueryValue(nextQuery.pageSize)
  )
}

async function syncListRouteQuery() {
  const nextQuery: Record<string, unknown> = { ...route.query }
  const trimmedProductName = searchForm.productName.trim()

  assignListQueryValue(nextQuery, 'productName', trimmedProductName || undefined)
  assignListQueryValue(nextQuery, 'nodeType', searchForm.nodeType)
  assignListQueryValue(nextQuery, 'status', searchForm.status)
  assignListQueryValue(nextQuery, 'pageNum', pagination.pageNum > 1 ? pagination.pageNum : undefined)
  assignListQueryValue(nextQuery, 'pageSize', pagination.pageSize !== defaultPageSize ? pagination.pageSize : undefined)

  if (hasSameListRouteQuery(nextQuery)) {
    await loadProductPage()
    return
  }

  await router.replace({
    path: route.path,
    query: nextQuery
  })
}

function buildDetailPreview(row: Product) {
  return {
    ...row,
    description: row.description ?? null
  }
}

async function loadProductPage(options: { silent?: boolean; force?: boolean; silentMessage?: string } = {}) {
  const requestId = ++latestListRequestId
  const query = buildCurrentProductPageQuery()
  const cachedPage = getCachedProductPage(query)
  const loadStrategy = resolveProductPageLoadStrategy({
    hasCachedPage: Boolean(cachedPage),
    hasFreshCache: isProductPageCacheFresh(cachedPage, productPageCacheTtlMs),
    force: options.force === true,
    silent: options.silent === true
  })
  const hadVisibleResult = Boolean(cachedPage) || tableData.value.length > 0

  abortListPrefetchRequest()
  abortListRequest()
  if (cachedPage) {
    applyCachedProductPage(cachedPage)
  }

  if (loadStrategy.useFreshCacheOnly) {
    clearListRefreshState()
    loading.value = false
    void prefetchNextProductPage(query, cachedPage.total)
    return
  }

  const controller = new AbortController()
  listAbortController = controller
  const silent = loadStrategy.silentRequest
  const preserveVisibleResult = silent && hadVisibleResult

  if (preserveVisibleResult) {
    listRefreshState.value = 'info'
    listRefreshMessage.value = options.silentMessage || '已先展示当前结果，正在后台校验最新数据。'
  } else {
    clearListRefreshState()
  }

  loading.value = !preserveVisibleResult
  try {
    const res = await productApi.pageProducts(
      {
        productName: query.productName || undefined,
        nodeType: query.nodeType,
        status: query.status,
        pageNum: query.pageNum,
        pageSize: query.pageSize
      },
      {
        signal: controller.signal
      }
    )
    if (requestId !== latestListRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      clearListRefreshState()
      tableData.value = applyPageResult(res.data)
      syncAppliedFilters()
      cacheProductPage(query, res.data)
      void syncTableSelection()
      void prefetchNextProductPage(query, res.data.total)
      await resolveGovernanceRouteWorkbench()
    }
  } catch (error) {
    if (requestId !== latestListRequestId || isAbortError(error)) {
      return
    }
    console.error('获取产品分页失败', error)
    if (preserveVisibleResult) {
      listRefreshState.value = 'error'
      listRefreshMessage.value = '最新数据校验失败，当前先展示已有结果。'
    } else {
      clearListRefreshState()
      if (!isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '获取产品分页失败'))
      }
    }
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false
    }
    if (listAbortController === controller) {
      listAbortController = null
    }
  }
}

async function openDetail(row: Product) {
  const requestId = ++latestDetailRequestId
  const cachedDetail = getCachedProductDetail(row)
  const detailSnapshot = resolveDetailSnapshot(row, cachedDetail)
  abortDetailRequest()

  currentProduct.value = row
  businessWorkbenchVisible.value = true
  handleBusinessWorkbenchViewChange('overview')
  detailLoading.value = false
  detailErrorMessage.value = ''
  detailData.value = detailSnapshot
  businessWorkbenchProduct.value = detailSnapshot

  // 如果没有缓存或者需要刷新详情，则发起后台补数请求
  if (!cachedDetail && shouldRefreshProductDetail(row, cachedDetail)) {
    const controller = new AbortController()
    detailAbortController = controller
    detailRefreshing.value = true

    try {
      const res = await productApi.getProductById(row.id, {
        signal: controller.signal
      })
      if (requestId !== latestDetailRequestId) {
        return
      }
      if (res.code === 200 && res.data) {
        detailData.value = res.data
        businessWorkbenchProduct.value = res.data
        cacheProductDetail(res.data)
      }
    } catch (error) {
      if (requestId !== latestDetailRequestId || isAbortError(error)) {
        return
      }
      // 静默失败，不显示红色提示，保持现有数据（detailSnapshot）
      console.warn('完整详情补充失败', error)
    } finally {
      if (requestId === latestDetailRequestId) {
        detailLoading.value = false
        detailRefreshing.value = false
      }
      if (detailAbortController === controller) {
        detailAbortController = null
      }
    }
  } else {
    detailRefreshing.value = false
  }
}

async function refreshEditableDetail(row: Product, editSessionId: number, cachedDetail: Product | null) {
  if (!shouldRefreshProductDetail(row, cachedDetail)) {
    clearFormRefreshState()
    return
  }

  const requestId = ++latestEditRequestId
  abortEditRequest()
  const controller = new AbortController()
  editAbortController = controller
  formRefreshing.value = true
  formRefreshState.value = 'info'
  formRefreshMessage.value = ''

  try {
    const res = await productApi.getProductById(row.id, {
      signal: controller.signal
    })
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingProductId.value !== row.id
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      cacheProductDetail(res.data)
      if (!formDirtySinceOpen) {
        applyFormDataWithoutDirty(res.data)
        editWorkspaceRef.value?.clearValidate()
        clearFormRefreshState()
      } else {
        formRefreshState.value = 'warning'
        formRefreshMessage.value = '最新档案已取回；你已开始编辑，当前未自动覆盖表单。'
      }
    }
  } catch (error) {
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingProductId.value !== row.id ||
      isAbortError(error)
    ) {
      return
    }
    formRefreshState.value = 'error'
    formRefreshMessage.value =
      error instanceof Error ? `最新档案补充失败：${error.message}` : '最新档案补充失败，当前先保留已填入内容。'
  } finally {
    if (requestId === latestEditRequestId) {
      formRefreshing.value = false
    }
    if (editAbortController === controller) {
      editAbortController = null
    }
  }
}

async function resolveGovernanceRouteWorkbench() {
  const openProductId = parseRouteStringQuery(route.query.openProductId)
  if (!openProductId) {
    handledGovernanceWorkbenchRouteKey.value = ''
    return
  }
  const targetView = resolveRouteWorkbenchView(route.query.workbenchView)
  const routeKey = `${openProductId}:${targetView}`
  if (handledGovernanceWorkbenchRouteKey.value === routeKey) {
    return
  }

  let targetProduct = tableData.value.find((item) => String(item.id) === openProductId) || null
  if (!targetProduct) {
    try {
      const response = await productApi.getProductById(openProductId, {})
      if (response.code === 200 && response.data) {
        targetProduct = response.data
      }
    } catch (error) {
      console.warn('治理控制面产品上下文补数失败', error)
      return
    }
  }

  if (!targetProduct) {
    return
  }

  handledGovernanceWorkbenchRouteKey.value = routeKey

  if (targetView === 'overview') {
    await openDetail(targetProduct)
    return
  }

  if (targetView === 'edit') {
    openEditWorkbench(targetProduct, targetProduct)
    return
  }

  currentProduct.value = targetProduct
  businessWorkbenchProduct.value = targetProduct
  businessWorkbenchVisible.value = true
  handleBusinessWorkbenchViewChange(targetView)
}

async function refreshEditAvailableModels(productId: string | number, editSessionId: number) {
  const requestId = ++latestEditModelRequestId
  editAvailableModels.value = []
  try {
    const res = await productApi.listProductModels(productId)
    if (
      requestId !== latestEditModelRequestId ||
      editSessionId !== activeEditSessionId ||
      String(editingProductId.value) !== String(productId)
    ) {
      return
    }
    editAvailableModels.value = res.data ?? []
  } catch {
    if (
      requestId !== latestEditModelRequestId ||
      editSessionId !== activeEditSessionId ||
      String(editingProductId.value) !== String(productId)
    ) {
      return
    }
    editAvailableModels.value = []
  }
}

function handleSearch() {
  searchForm.productName = searchForm.productName.trim()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleReset() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function removeAppliedFilter(key: ProductFilterKey) {
  if (key === 'productName') {
    searchForm.productName = ''
  } else if (key === 'nodeType') {
    searchForm.nodeType = undefined
  } else {
    searchForm.status = undefined
  }
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleClearAppliedFilters() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleRefresh() {
  clearSelection()
  clearProductPageCache()
  void loadProductPage({ force: true })
}

function handleAdd() {
  activeEditSessionId += 1
  abortEditRequest()
  latestEditModelRequestId += 1
  editingProductId.value = null
  editAvailableModels.value = []
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty()
  formVisible.value = true
}

function openEditWorkbench(row: Product, initialProduct?: Product | null) {
  const cachedDetail = getCachedProductDetail(row)
  const editSnapshot = resolveDetailSnapshot(initialProduct || row, cachedDetail)

  activeEditSessionId += 1
  const editSessionId = activeEditSessionId
  abortEditRequest()
  editingProductId.value = row.id
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty(editSnapshot)
  currentProduct.value = row
  businessWorkbenchProduct.value = editSnapshot
  editAvailableModels.value = []
  businessWorkbenchVisible.value = true
  handleBusinessWorkbenchViewChange('edit')
  editWorkspaceRef.value?.clearValidate()
  void refreshEditAvailableModels(row.id, editSessionId)
  void refreshEditableDetail(row, editSessionId, cachedDetail)
}

function handleEdit(row: Product) {
  openEditWorkbench(row)
}

function handleOpenDetail(row: Product) {
  void openDetail(row)
}

function handleOpenDeviceListDrawer(row: Product) {
  currentProduct.value = row
  businessWorkbenchProduct.value = row
  businessWorkbenchVisible.value = true
  handleBusinessWorkbenchViewChange('devices')
}

function handleOpenProductModelDesigner(row: Product) {
  currentProduct.value = row
  businessWorkbenchProduct.value = row
  businessWorkbenchVisible.value = true
  handleBusinessWorkbenchViewChange('models')
}

function handleBusinessWorkbenchEdit() {
  const sourceProduct = businessWorkbenchProduct.value || currentProduct.value
  if (!sourceProduct) {
    return
  }
  openEditWorkbench(currentProduct.value || sourceProduct, sourceProduct)
}

function openGovernanceTask(path: string) {
  const focusProduct = governanceFocusProduct.value
  recordActivity({
    title: `产品治理待办跳转 · ${path}`,
    detail: `聚焦产品 ${focusProduct?.productKey || '--'} 进入 ${path}`,
    tag: 'product-governance-task'
  })
  void router.push(path)
}

function buildGovernanceTaskPath(productId: number | string | null | undefined, workItemCode: string) {
  const query = new URLSearchParams()
  if (productId != null && String(productId).trim()) {
    query.set('productId', String(productId))
  }
  query.set('workStatus', 'OPEN')
  query.set('workItemCode', workItemCode)
  return `/governance-task?${query.toString()}`
}

function buildProductWorkbenchPath(
  productId: number | string | null | undefined,
  workbenchView: ProductBusinessWorkbenchView = 'models'
) {
  const query = new URLSearchParams()
  if (productId != null && String(productId).trim()) {
    query.set('openProductId', String(productId))
  }
  query.set('workbenchView', workbenchView)
  return `/products?${query.toString()}`
}

async function clearProductWorkbenchRouteContext() {
  if (!parseRouteStringQuery(route.query.openProductId)) {
    return
  }
  const nextQuery: Record<string, unknown> = { ...route.query }
  let changed = false
  for (const key of productWorkbenchRouteContextKeys) {
    if (key in nextQuery) {
      delete nextQuery[key]
      changed = true
    }
  }
  if (!changed) {
    return
  }
  handledGovernanceWorkbenchRouteKey.value = ''
  await router.replace({
    path: route.path,
    query: nextQuery
  })
}

async function loadGovernanceSnapshot(product: Product | null) {
  const requestId = ++latestGovernanceRequestId
  if (!product?.id) {
    governanceCoverageOverview.value = null
    latestContractReleaseBatch.value = null
    governanceErrorMessage.value = ''
    governanceLoading.value = false
    return
  }

  governanceLoading.value = true
  governanceErrorMessage.value = ''
  try {
    const [coverageRes, releaseRes] = await Promise.all([
      getRiskGovernanceCoverageOverview(product.id),
      productApi.pageProductContractReleaseBatches(product.id, { pageNum: 1, pageSize: 1 })
    ])

    if (requestId !== latestGovernanceRequestId) {
      return
    }

    governanceCoverageOverview.value = coverageRes.code === 200 ? coverageRes.data || null : null
    latestContractReleaseBatch.value = releaseRes.code === 200 ? releaseRes.data?.records?.[0] || null : null
  } catch (error) {
    if (requestId !== latestGovernanceRequestId) {
      return
    }
    governanceCoverageOverview.value = null
    latestContractReleaseBatch.value = null
    governanceErrorMessage.value = resolveRequestErrorMessage(error, '治理进度加载失败，请稍后重试。')
  } finally {
    if (requestId === latestGovernanceRequestId) {
      governanceLoading.value = false
    }
  }
}

// 查看设备
function handleViewDevice(device: Device) {
  console.log('view device', device)
  // TODO: 实现查看设备的逻辑
}

// 加载设备列表
async function loadDeviceList(productKey: string) {
  devicesLoading.value = true
  deviceListErrorMessage.value = ''
  try {
    const res = await deviceApi.pageDevices({
      productKey,
      pageNum: 1,
      pageSize: 100
    })
    if (res.code === 200 && res.data) {
      const devices = res.data.records || []
      deviceListData.value = devices
      deviceListTotal.value = res.data.total || 0
      deviceListOnlineCount.value = devices.filter((d) => d.onlineStatus === 1).length
      deviceListOfflineCount.value = devices.filter((d) => d.onlineStatus !== 1).length
    }
  } catch (error) {
    console.error('获取设备列表失败', error)
    deviceListErrorMessage.value = error instanceof Error ? error.message : '获取设备列表失败'
    deviceListData.value = []
    deviceListTotal.value = 0
    deviceListOnlineCount.value = 0
    deviceListOfflineCount.value = 0
  } finally {
    devicesLoading.value = false
  }
}

function handleRowAction(command: string | number | object, row: Product) {
  if (command === 'detail') {
    handleOpenDetail(row)
    return
  }
  if (command === 'edit') {
    handleEdit(row)
    return
  }
  if (command === 'model') {
    handleOpenProductModelDesigner(row)
    return
  }
  if (command === 'devices') {
    handleOpenDeviceListDrawer(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

async function handleBatchCommand(command: string, rows: Product[]) {
  const rowCount = rows.length
  if (rowCount === 0) {
    return
  }

  try {
    if (command === 'enable') {
      await confirmAction({
        title: '确认启用',
        message: `确定要启用选中的 ${rowCount} 个产品吗？启用后可正常接入设备`,
        type: 'warning',
        confirmButtonText: '确定'
      })

      // 辅助函数：将 null 转换为 undefined
      function normalizeProductPayload(row: Product): ProductAddPayload {
        return {
          productKey: row.productKey,
          productName: row.productName,
          protocolCode: row.protocolCode,
          nodeType: row.nodeType,
          dataFormat: row.dataFormat ?? undefined,
          manufacturer: row.manufacturer ?? undefined,
          description: row.description ?? undefined,
          metadataJson: row.metadataJson ?? undefined,
          status: row.status ?? 1
        }
      }

      // 批量启用
      for (const row of rows) {
        await productApi.updateProduct(row.id, normalizeProductPayload({ ...row, status: 1 }))
      }
      ElMessage.success(`已启用 ${rowCount} 个产品`)
      rows.forEach((row) => {
        const updatedRow = { ...row, status: 1 }
        mergeLocalTableRow(updatedRow)
        replaceSelectedRowSnapshot(updatedRow)
      })
      void loadProductPage({ silent: true })
    } else if (command === 'disable') {
      await confirmAction({
        title: '确认停用',
        message: `确定要停用选中的 ${rowCount} 个产品吗？停用后将无法新增设备，但不影响现有设备`,
        type: 'warning',
        confirmButtonText: '确定'
      })

      // 辅助函数：将 null 转换为 undefined
      function normalizeProductPayload(row: Product): ProductAddPayload {
        return {
          productKey: row.productKey,
          productName: row.productName,
          protocolCode: row.protocolCode,
          nodeType: row.nodeType,
          dataFormat: row.dataFormat ?? undefined,
          manufacturer: row.manufacturer ?? undefined,
          description: row.description ?? undefined,
          metadataJson: row.metadataJson ?? undefined,
          status: row.status ?? 1
        }
      }

      // 批量停用
      for (const row of rows) {
        await productApi.updateProduct(row.id, normalizeProductPayload({ ...row, status: 0 }))
      }
      ElMessage.success(`已停用 ${rowCount} 个产品`)
      rows.forEach((row) => {
        const updatedRow = { ...row, status: 0 }
        mergeLocalTableRow(updatedRow)
        replaceSelectedRowSnapshot(updatedRow)
      })
      void loadProductPage({ silent: true })
    } else if (command === 'delete') {
      await handleDeleteBatch(rows)
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('批量操作失败', error)
    ElMessage.error(error instanceof Error ? error.message : '批量操作失败')
  }
}

async function handleDeleteBatch(rows: Product[]) {
  try {
    await confirmAction({
      title: '确认删除',
      message: `确定要删除选中的 ${rows.length} 个产品吗？此操作不可恢复`,
      type: 'warning',
      confirmButtonText: '确定'
    })

    // 批量删除
    await Promise.all(rows.map((row) => productApi.deleteProduct(row.id)))
    ElMessage.success(`已删除 ${rows.length} 个产品`)

    rows.forEach((row) => {
      removeCachedProductDetail(row)
      removeLocalTableRow(row)
      removeSelectedRowSnapshot(row)
    })

    setTotal(pagination.total - rows.length)
    
    // 如果当前页没有数据了，翻到上一页
    if (tableData.value.length === 0 && pagination.pageNum > 1) {
      clearProductPageCache()
      setPageNum(pagination.pageNum - 1)
      clearSelection()
      await syncListRouteQuery()
      return
    }

    rebuildVisibleProductPageCache()
    if (tableData.value.length === 0) {
      clearSelection()
    }
    void loadProductPage({ silent: true, force: true })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('批量删除产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '批量删除产品失败')
  }
}

async function handleDelete(row: Product) {
  try {
    await confirmDelete('产品', row.productName || row.productKey)
    await productApi.deleteProduct(row.id)
    ElMessage.success('删除成功')
    removeCachedProductDetail(row)
    const removedFromCurrentPage = removeLocalTableRow(row)
    setTotal(pagination.total - 1)
    if (tableData.value.length === 0 && pagination.pageNum > 1) {
      clearProductPageCache()
      setPageNum(pagination.pageNum - 1)
      clearSelection()
      await syncListRouteQuery()
      return
    }
    rebuildVisibleProductPageCache()
    if (!removedFromCurrentPage) {
      clearSelection()
    }
    void loadProductPage({ silent: true, force: true })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '删除产品失败')
  }
}

async function handleSubmit() {
  const valid = editingProductId.value
    ? await editWorkspaceRef.value?.validate()
    : await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const validationMessage = validateProductObjectInsightMetrics(objectInsightMetricRows.value)
  if (validationMessage) {
    ElMessage.error(validationMessage)
    return
  }

  const payload: ProductAddPayload = {
    ...formData,
    metadataJson: buildProductMetadataJson(objectInsightMetricRows.value, formData.metadataJson)
  }

  submitLoading.value = true
  try {
    if (editingProductId.value) {
      const res = await productApi.updateProduct(editingProductId.value, payload)
      cacheProductDetail(res.data)
      currentProduct.value = res.data
      detailData.value = res.data
      businessWorkbenchProduct.value = res.data
      if (matchesCurrentFilters(res.data)) {
        mergeLocalTableRow(res.data)
      } else {
        removeLocalTableRow(res.data)
      }
      rebuildVisibleProductPageCache()
      applyFormDataWithoutDirty(res.data)
      editWorkspaceRef.value?.clearValidate()
      clearFormRefreshState()
      formDirtySinceOpen = false
      ElMessage.success('更新成功')
      void loadProductPage({ silent: true, force: true })
    } else {
      const res = await productApi.addProduct(payload)
      cacheProductDetail(res.data)
      ElMessage.success('新增成功')
      formVisible.value = false
      clearSelection()

      if (pagination.pageNum === 1 && matchesCurrentFilters(res.data)) {
        prependLocalTableRow(res.data)
        setTotal(pagination.total + 1)
        rebuildVisibleProductPageCache()
        void loadProductPage({ silent: true, force: true })
      } else if (pagination.pageNum === 1) {
        clearProductPageCache()
        void loadProductPage({ silent: true })
      } else {
        clearProductPageCache()
        resetPage()
        await syncListRouteQuery()
      }
    }
  } catch (error) {
    console.error('提交产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '提交产品失败')
  } finally {
    submitLoading.value = false
  }
}

function handleFormClose() {
  activeEditSessionId += 1
  abortEditRequest()
  latestEditModelRequestId += 1
  editAvailableModels.value = []
  createFormRef.value?.clearValidate?.()
  clearFormRefreshState()
  formDirtySinceOpen = false
  applyFormDataWithoutDirty()
  editingProductId.value = null
}

function handleSizeChange(size: number) {
  setPageSize(size)
  clearSelection()
  void syncListRouteQuery()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  void syncListRouteQuery()
}

watch(
  () => [route.query.productName, route.query.nodeType, route.query.status, route.query.pageNum, route.query.pageSize],
  () => {
    applyRouteQueryToFilters()
    clearSelection()
    void loadProductPage()
  }
)

watch(
  () => [route.query.openProductId, route.query.workbenchView],
  () => {
    if (!parseRouteStringQuery(route.query.openProductId)) {
      handledGovernanceWorkbenchRouteKey.value = ''
      return
    }
    void resolveGovernanceRouteWorkbench()
  }
)

watch(
  () => String(governanceFocusProduct.value?.id || ''),
  () => {
    void loadGovernanceSnapshot(governanceFocusProduct.value)
  },
  { immediate: true }
)

watch(businessWorkbenchVisible, (visible) => {
  if (visible) {
    return
  }
  void clearProductWorkbenchRouteContext()
  latestDetailRequestId += 1
  abortDetailRequest()
  detailLoading.value = false
  detailRefreshing.value = false
  detailErrorMessage.value = ''
  detailData.value = null
  businessWorkbenchProduct.value = null
  deviceListErrorMessage.value = ''
  resetBusinessWorkbenchLoadedViews()
  if (editingProductId.value) {
    activeEditSessionId += 1
    abortEditRequest()
    latestEditModelRequestId += 1
    editAvailableModels.value = []
    editWorkspaceRef.value?.clearValidate()
    clearFormRefreshState()
    formDirtySinceOpen = false
    applyFormDataWithoutDirty()
    editingProductId.value = null
  }
})

watch(
  formData,
  () => {
    if (!businessWorkbenchVisible.value || !editingProductId.value || suppressFormDirtyTracking) {
      return
    }
    formDirtySinceOpen = true
  },
  { deep: true, flush: 'sync' }
)

onBeforeUnmount(() => {
  abortListRequest()
  abortListPrefetchRequest()
  abortDetailRequest()
  abortEditRequest()
})

onMounted(async () => {
  hydrateProductDetailCache()
  hydrateProductPageCache()
  applyRouteQueryToFilters()
  await loadProductPage()
})
</script>

<style scoped>
.product-asset-view {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
}

:deep(.product-detail-drawer .el-drawer__header) {
  padding: 24px 28px 16px;
  border-bottom: 1px solid var(--panel-border);
}

:deep(.product-detail-drawer .el-drawer__body) {
  padding: 20px 28px 24px;
}

:deep(.product-detail-drawer .detail-drawer__heading h2) {
  margin-top: 0.24rem;
  font-size: clamp(1.5rem, 2vw, 1.75rem);
  letter-spacing: -0.015em;
  font-weight: 600;
  color: var(--text-heading);
}

:deep(.product-detail-drawer .detail-drawer__subtitle) {
  margin-top: 0.4rem;
  max-width: 38rem;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-caption);
}

/* 快速搜索标签 */
.product-quick-search-tag {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.product-quick-search-tag__chip {
  cursor: pointer;
}

.product-governance-notice-stack {
  display: grid;
  gap: 0.6rem;
}

.product-governance-task-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.56rem;
}

.product-governance-task-list li {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.72rem;
}

.product-governance-task-list__content {
  display: grid;
  gap: 0.2rem;
}

.product-governance-task-list__content strong {
  color: var(--text-heading);
  font-size: 0.9rem;
  line-height: 1.4;
}

.product-governance-task-list__content span {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.5;
}

.product-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.product-result-panel :deep(.el-loading-spinner .el-loading-text) {
  margin-top: 0.72rem;
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.product-result-panel :deep(.el-loading-spinner .path) {
  stroke: var(--brand);
}

.product-empty-state {
  display: grid;
  justify-items: center;
  padding: 0.4rem 0 0.2rem;
}

.product-empty-state :deep(.empty-state) {
  padding-block: 3.25rem 2rem;
}

.product-empty-state__actions {
  display: flex;
  justify-content: center;
}

.product-loading-state {
  display: grid;
  gap: 14px;
  min-height: 14rem;
  padding: 0.72rem 0.1rem 0.2rem;
}

.product-loading-state__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.product-loading-state__desktop {
  display: grid;
  gap: 10px;
}

.product-loading-state__mobile {
  display: none;
  gap: 12px;
}

.product-loading-table {
  display: grid;
  grid-template-columns: 0.38fr 1.35fr 1.55fr 0.96fr 0.9fr 1.1fr 0.78fr 1.18fr;
  gap: 12px;
  align-items: center;
}

.product-loading-table--header {
  padding: 0 0.82rem;
}

.product-loading-table--row {
  padding: 0.92rem 0.82rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: var(--shadow-inset-highlight-78);
}

.product-loading-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: var(--shadow-inset-highlight-76);
}

.product-loading-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.product-loading-mobile-card__heading {
  display: grid;
  gap: 0.3rem;
}

.product-loading-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.product-loading-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.product-loading-mobile-card__field {
  display: grid;
  gap: 0.28rem;
}

.product-loading-pulse {
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(228, 235, 246, 0.8), rgba(244, 248, 255, 0.98), rgba(228, 235, 246, 0.8));
  background-size: 220% 100%;
  animation: product-loading-shimmer 1.35s ease-in-out infinite;
}

.product-loading-pulse::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.46);
  border-radius: inherit;
}

.product-loading-line {
  display: block;
  height: 0.82rem;
  border-radius: var(--radius-pill);
}

.product-loading-line--header {
  height: 0.72rem;
}

.product-loading-line--key {
  width: 88%;
}

.product-loading-line--title {
  width: 92%;
}

.product-loading-line--short {
  width: 72%;
}

.product-loading-line--meta {
  width: 78%;
}

.product-loading-line--time {
  width: 100%;
}

.product-loading-line--label {
  width: 52%;
  height: 0.68rem;
}

.product-loading-line--value {
  width: 82%;
  height: 0.84rem;
}

.product-loading-pill {
  display: inline-flex;
  width: 6rem;
  height: 1.42rem;
  border-radius: var(--radius-pill);
}

.product-loading-pill--status {
  width: 4.6rem;
}

.product-loading-square {
  display: block;
  width: 1rem;
  height: 1rem;
  border-radius: 0.3rem;
}

.product-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.product-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.product-mobile-card__title {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.product-mobile-card__sub {
  overflow: hidden;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.product-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.product-mobile-card__field {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.product-mobile-card__field .standard-mobile-record-card__field-value {
  overflow: hidden;
  display: block;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desktop-table {
  display: block;
}

@media (max-width: 720px) {
  .product-mobile-list {
    display: block;
  }

  .product-desktop-table {
    display: none;
  }

  .product-mobile-card__info {
    grid-template-columns: 1fr;
  }
}

@keyframes product-loading-shimmer {
  0% {
    background-position: 100% 50%;
  }

  100% {
    background-position: -100% 50%;
  }
}

</style>
