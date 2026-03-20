<template>
  <div class="product-asset-view ops-workbench standard-list-view">
    <PanelCard class="ops-hero-card ops-table-card product-workbench-card">
      <template #header>
        <div class="product-hero-card__header">
          <div class="product-hero-card__heading">
            <h2 class="product-hero-card__title">产品定义中心</h2>
            <p class="product-hero-card__caption">聚焦产品台账维护，支持筛选、查看、编辑、删除、导出和关联设备跳转。</p>
          </div>
          <StandardActionGroup gap="sm">
            <el-button v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
          </StandardActionGroup>
        </div>
      </template>

      <div class="product-workbench-card__filters">
        <el-form :model="searchForm" class="product-inline-filter" @submit.prevent>
          <div class="product-inline-filter__row">
            <el-form-item class="product-inline-filter__item">
              <el-input
                id="query-product-name"
                v-model="searchForm.productName"
                placeholder="产品名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item class="product-inline-filter__item">
              <el-select v-model="searchForm.nodeType" placeholder="节点类型" clearable>
                <el-option label="直连设备" :value="1" />
                <el-option label="网关设备" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item class="product-inline-filter__item">
              <el-select v-model="searchForm.status" placeholder="产品状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <div class="product-inline-filter__actions">
              <StandardActionGroup gap="sm">
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
              </StandardActionGroup>
            </div>
          </div>
        </el-form>
      </div>

      <div v-if="hasAppliedFilters" class="product-applied-filters">
        <span class="product-applied-filters__label">已生效筛选</span>
        <div class="product-applied-filters__list">
          <el-tag
            v-for="tag in activeFilterTags"
            :key="tag.key"
            closable
            class="product-applied-filters__tag"
            @close="removeAppliedFilter(tag.key)"
          >
            {{ tag.label }}
          </el-tag>
        </div>
        <el-button link class="product-applied-filters__clear" @click="handleClearAppliedFilters">清空全部</el-button>
      </div>

      <StandardTableToolbar
        :meta-items="[
          `已选 ${selectedRows.length} 项`,
          `启用 ${enabledProductCount} 个`,
          `停用 ${disabledProductCount} 个`
        ]"
      >
        <template #right>
          <el-button v-permission="'iot:products:export'" link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="selectedRows.length === 0" @click="handleExportSelected">
            导出选中
          </el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="tableData.length === 0" @click="handleExportCurrent">
            导出当前结果
          </el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

      <div
        v-loading="loading"
        class="product-result-panel"
        element-loading-text="正在刷新产品列表"
        element-loading-background="rgba(248, 250, 255, 0.78)"
      >
        <template v-if="hasRecords">
          <div class="product-mobile-list">
            <div class="product-mobile-list__grid">
              <article v-for="row in tableData" :key="getProductRowKey(row)" class="product-mobile-card">
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
                  <span class="product-mobile-card__meta-item">{{ getNodeTypeText(row.nodeType) }}</span>
                  <span class="product-mobile-card__meta-item">{{ row.protocolCode || '--' }}</span>
                  <span class="product-mobile-card__meta-item">{{ row.dataFormat || '--' }}</span>
                </div>

                <div class="product-mobile-card__info">
                  <div class="product-mobile-card__field">
                    <span>厂商</span>
                    <strong>{{ formatTextValue(row.manufacturer) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>关联设备</span>
                    <strong>{{ formatCount(row.deviceCount) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>最近上报</span>
                    <strong>{{ formatDateTime(row.lastReportTime) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>更新时间</span>
                    <strong>{{ formatDateTime(row.updateTime) }}</strong>
                  </div>
                </div>

                <div class="product-mobile-card__actions">
                  <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                  <el-button v-permission="'iot:products:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRowAction(command, row)">
                    <el-button type="primary" link>
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="devices">查看设备</el-dropdown-item>
                        <el-dropdown-item v-permission="'iot:products:delete'" command="delete">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
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
            <el-table-column label="操作" width="180" fixed="right" :show-overflow-tooltip="false">
              <template #default="{ row }">
                <div class="product-table-actions">
                  <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                  <el-button v-permission="'iot:products:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRowAction(command, row)">
                    <el-button type="primary" link>
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="devices">查看设备</el-dropdown-item>
                        <el-dropdown-item v-permission="'iot:products:delete'" command="delete">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="product-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="product-empty-state__actions">
            <el-button v-if="hasAppliedFilters" @click="handleClearAppliedFilters">清空筛选条件</el-button>
            <el-button v-else v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
          </div>
        </div>
      </div>

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
    </PanelCard>

    <StandardDetailDrawer
      v-model="detailVisible"
      class="product-detail-drawer"
      eyebrow="产品定义详情"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <div v-if="detailData" class="product-detail-layout">
        <section :class="['product-detail-zone', 'product-detail-zone--overview', { 'product-detail-zone--danger': detailData.status === 0 }]">
          <header class="product-detail-zone__header">
            <span class="product-detail-zone__kicker">产品汇总</span>
            <p class="product-detail-zone__intro">先看状态、设备规模和最近上报。</p>
          </header>

          <div class="product-detail-overview-grid">
            <article :class="['product-detail-overview-lead', { 'product-detail-overview-lead--danger': detailData.status === 0 }]">
              <span class="product-detail-overview-lead__eyebrow">当前判断</span>
              <strong class="product-detail-overview-lead__title">{{ detailOperationHeadline }}</strong>
              <p class="product-detail-overview-lead__text">{{ detailOperationSummary }}</p>
              <div class="product-detail-overview-progress">
                <div class="product-detail-overview-progress__track">
                  <span class="product-detail-overview-progress__fill" :style="{ width: `${detailOnlineRatioPercent}%` }" />
                </div>
                <span class="product-detail-overview-progress__caption">在线设备占关联设备的比例</span>
              </div>
              <div class="product-detail-overview-lead__meta">
                <span>产品状态：{{ getStatusText(detailData.status) }}</span>
                <span>当前阶段：{{ detailLifecycleStage }}</span>
              </div>
            </article>

            <div class="product-detail-overview-metrics">
              <article v-for="metric in detailSummaryMetrics" :key="metric.key" class="product-detail-overview-metric">
                <span class="product-detail-overview-metric__label">{{ metric.label }}</span>
                <strong class="product-detail-overview-metric__value">{{ metric.value }}</strong>
                <p class="product-detail-overview-metric__hint">{{ metric.hint }}</p>
              </article>
            </div>
          </div>
        </section>

        <section class="product-detail-zone product-detail-zone--ledger">
          <div class="product-detail-ledger-grid">
            <article class="product-detail-ledger-card product-detail-ledger-card--contract">
              <header class="product-detail-card-header">
                <h3>接入契约</h3>
                <p>核对协议、节点类型和上报格式。</p>
              </header>
              <div class="product-detail-contract-list">
                <article v-for="item in detailContractCards" :key="item.key" class="product-detail-contract-item">
                  <span class="product-detail-contract-item__label">{{ item.label }}</span>
                  <strong class="product-detail-contract-item__value" :title="item.value">{{ item.value }}</strong>
                </article>
              </div>
            </article>

            <article class="product-detail-ledger-card product-detail-ledger-card--archive">
              <header class="product-detail-card-header">
                <h3>产品档案</h3>
                <p>核对编号、Key、厂商和建档时间。</p>
              </header>
              <div class="product-detail-archive-grid">
                <article class="product-detail-archive-item product-detail-archive-item--full">
                  <span class="product-detail-archive-item__label">产品编号</span>
                  <strong class="product-detail-archive-item__value" :title="detailArchiveIdText">{{ detailArchiveIdText }}</strong>
                </article>
                <article class="product-detail-archive-item product-detail-archive-item--full">
                  <span class="product-detail-archive-item__label">产品 Key</span>
                  <strong class="product-detail-archive-item__value" :title="detailArchiveProductKeyText">
                    {{ detailArchiveProductKeyText }}
                  </strong>
                </article>
                <article class="product-detail-archive-item">
                  <span class="product-detail-archive-item__label">厂商</span>
                  <strong class="product-detail-archive-item__value" :title="detailArchiveManufacturerText">
                    {{ detailArchiveManufacturerText }}
                  </strong>
                </article>
                <article class="product-detail-archive-item">
                  <span class="product-detail-archive-item__label">创建时间</span>
                  <strong class="product-detail-archive-item__value">{{ detailArchiveCreateDateText }}</strong>
                </article>
              </div>
              <article class="product-detail-description-card">
                <span class="product-detail-description-card__label">产品说明</span>
                <strong class="product-detail-description-card__value">{{ detailDescriptionText }}</strong>
              </article>
            </article>
          </div>
        </section>

        <section class="product-detail-zone product-detail-zone--governance">
          <header class="product-detail-zone__header">
            <span class="product-detail-zone__kicker">维护与治理</span>
            <p class="product-detail-zone__intro">建议、规则和变更前检查分层展示。</p>
          </header>
          <div class="product-detail-governance-grid">
            <article
              :class="[
                'product-detail-governance-card',
                'product-detail-governance-card--lead',
                { 'product-detail-governance-card--danger': detailData.status === 0 }
              ]"
            >
              <span class="product-detail-governance-card__label">当前建议</span>
              <strong class="product-detail-governance-card__title">{{ detailGovernanceHeadline }}</strong>
              <p class="product-detail-governance-card__text">{{ detailGovernanceNotice }}</p>
            </article>

            <article class="product-detail-governance-card">
              <span class="product-detail-governance-card__label">维护规则</span>
              <ul class="product-detail-governance-list">
                <li v-for="item in detailMaintenanceRules" :key="item">{{ item }}</li>
              </ul>
            </article>

            <article class="product-detail-governance-card">
              <span class="product-detail-governance-card__label">变更前确认</span>
              <ul class="product-detail-governance-list">
                <li v-for="item in detailChangeChecklist" :key="item">{{ item }}</li>
              </ul>
            </article>
          </div>
        </section>
      </div>

      <template #footer>
        <StandardDrawerFooter @cancel="detailVisible = false">
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="detailVisible = false">
            关闭
          </el-button>
          <el-button
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :disabled="!detailData?.productKey"
            @click="handleJumpToDevices(detailData)"
          >
            查看设备
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardDetailDrawer>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
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
        </el-form>
      </div>

      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          :confirm-text="submitButtonText"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </el-button>
          <el-button
            id="product-submit-button"
            v-permission="submitPermission"
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ submitButtonText }}
          </el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import { productApi } from '@/api/product'
import { useServerPagination } from '@/composables/useServerPagination'
import type { Product, ProductAddPayload } from '@/types/api'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { formatDateTime } from '@/utils/format'

interface ProductSearchForm {
  productName: string
  nodeType: number | undefined
  status: number | undefined
}

type ProductFilterKey = keyof ProductSearchForm

interface ProductFormState extends ProductAddPayload {}

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailErrorMessage = ref('')
const editingProductId = ref<string | number | null>(null)

const tableData = ref<Product[]>([])
const selectedRows = ref<Product[]>([])
const detailData = ref<Product | null>(null)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'product-definition-center'

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

const createDefaultFormData = (): ProductFormState => ({
  productKey: '',
  productName: '',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: '',
  description: '',
  status: 1
})

const formData = reactive<ProductFormState>(createDefaultFormData())

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination(10)

const formTitle = computed(() => (editingProductId.value ? '编辑产品' : '新增产品'))
const submitButtonText = computed(() => (editingProductId.value ? '保存' : '新增'))
const submitPermission = computed(() => (editingProductId.value ? 'iot:products:update' : 'iot:products:add'))
const detailTitle = computed(() => detailData.value?.productName || detailData.value?.productKey || '产品详情')
const detailSubtitle = computed(() => '按汇总、接入方式、档案信息和维护建议四个板块查看。')
const enabledProductCount = computed(() => tableData.value.filter((item) => item.status !== 0).length)
const disabledProductCount = computed(() => tableData.value.filter((item) => item.status === 0).length)
const hasRecords = computed(() => tableData.value.length > 0)
const activeFilterTags = computed(() => {
  const tags: Array<{ key: ProductFilterKey; label: string }> = []
  const productName = appliedFilters.productName.trim()
  if (productName) {
    tags.push({ key: 'productName', label: `产品名称：${productName}` })
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
const detailDescriptionText = computed(
  () =>
    detailData.value?.description?.trim() ||
    '当前没有补充说明，可结合接入方式、设备规模和维护建议判断是否继续使用。'
)
const detailAssociationHint = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount === 0) {
    return '当前还没有关联设备。'
  }
  if (onlineCount === null) {
    return `当前有 ${deviceCount} 台关联设备。`
  }
  return `当前有 ${deviceCount} 台关联设备，在线 ${onlineCount} 台。`
})
const detailLastReportHint = computed(() =>
  detailData.value?.lastReportTime ? '最近一次设备上报时间。' : '当前还没有收到设备上报。'
)
const detailOnlineRatioText = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount <= 0 || onlineCount === null) {
    return '--'
  }
  return `${Math.round((onlineCount / deviceCount) * 100)}%`
})
const detailOnlineRatioPercent = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount <= 0 || onlineCount === null) {
    return 0
  }
  return Math.min(100, Math.max(0, Math.round((onlineCount / deviceCount) * 100)))
})
const detailLifecycleStage = computed(() => {
  if (!detailData.value) {
    return '--'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if (detailData.value.status === 0) {
    return '已停用'
  }
  if ((deviceCount ?? 0) > 0) {
    return '稳定使用中'
  }
  return '接入调试中'
})
const detailOperationHeadline = computed(() => {
  if (!detailData.value) {
    return '正在加载产品信息'
  }
  if (detailData.value.status === 0) {
    return '这个产品当前已停用'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  const onlineCount = parseCount(detailData.value.onlineDeviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '这个产品还在接入准备阶段'
  }
  if ((onlineCount ?? 0) > 0) {
    return '这个产品下还有设备在线'
  }
  return '这个产品下有设备，但当前都不在线'
})
const detailGovernanceNotice = computed(() => {
  if (!detailData.value) {
    return '当前没有维护建议。'
  }
  if (detailData.value.status === 0) {
    return '当前产品已停用，新增设备、设备替换、设备上报和指令下发都会被系统拦截。'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  const onlineCount = parseCount(detailData.value.onlineDeviceCount)
  if ((deviceCount ?? 0) > 0 || (onlineCount ?? 0) > 0) {
    return '当前已有现场设备在使用这个产品。修改协议、节点类型或数据格式前，请先确认兼容性，避免影响现网设备。'
  }
  return '当前还没有设备正式使用，可以继续做接入联调；如需调整 Product Key 或协议规则，建议先确认命名和边界。'
})
const detailOperationSummary = computed(() => {
  if (!detailData.value) {
    return '正在整理当前产品的状态、接入方式和维护信息。'
  }
  if (detailData.value.status === 0) {
    return '先确认是否还有设备在用，再决定要不要继续保留这条产品定义。'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '当前还没有设备使用，适合继续做接入联调和模板整理。'
  }
  return '当前已经有设备在用，变更前先评估对现场设备的影响。'
})
const detailSummaryMetrics = computed(() => [
  {
    key: 'deviceCount',
    label: '关联设备数',
    value: formatCount(detailData.value?.deviceCount),
    hint: detailAssociationHint.value
  },
  {
    key: 'onlineDeviceCount',
    label: '在线设备数',
    value: formatCount(detailData.value?.onlineDeviceCount),
    hint: '当前在线的设备数量。'
  },
  {
    key: 'onlineRatio',
    label: '在线比例',
    value: detailOnlineRatioText.value,
    hint: parseCount(detailData.value?.deviceCount) ? '在线设备在全部关联设备中的比例' : '当前没有设备，暂不统计'
  },
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDateTime(detailData.value?.lastReportTime),
    hint: detailLastReportHint.value
  }
])
const detailContractCards = computed(() => [
  { key: 'protocolCode', label: '协议编码', value: formatTextValue(detailData.value?.protocolCode) },
  { key: 'nodeType', label: '节点类型', value: getNodeTypeText(detailData.value?.nodeType) },
  { key: 'dataFormat', label: '数据格式', value: formatTextValue(detailData.value?.dataFormat) }
])
const detailArchiveIdText = computed(() => formatTextValue(detailData.value?.id))
const detailArchiveProductKeyText = computed(() => formatTextValue(detailData.value?.productKey))
const detailArchiveManufacturerText = computed(() => formatTextValue(detailData.value?.manufacturer))
const detailArchiveCreateDateText = computed(() => formatDate(detailData.value?.createTime))
const detailGovernanceHeadline = computed(() => {
  if (!detailData.value) {
    return '正在整理维护建议'
  }
  if (detailData.value.status === 0) {
    return '先核查停用对现有设备的影响'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '当前可继续作为新设备接入模板'
  }
  return '当前已有设备在用，变更前先做影响评估'
})
const detailMaintenanceRules = computed(() => [
  '产品 Key 建立后尽量保持稳定，不建议直接改名。',
  '协议编码、节点类型和数据格式属于接入核心规则。',
  '调整时要兼顾历史日志、设备替换和接入检索的一致性。'
])
const detailChangeChecklist = computed(() => [
  '先确认现场是否已经有设备在使用。',
  '再确认协议或物模型变化是否需要新建产品版本。',
  '最后确认调整后不会影响设备建档和上报链路。'
])

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

function parseCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? count : null
}

function formatCount(value?: number | null) {
  const count = parseCount(value)
  return count === null ? '--' : String(count)
}

function formatTextValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function formatDate(value?: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

function getProductRowKey(row?: Partial<Product> | null) {
  if (!row) {
    return ''
  }
  if (row.id !== undefined && row.id !== null && row.id !== '') {
    return String(row.id)
  }
  return row.productKey ? String(row.productKey) : ''
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
    status: source?.status ?? 1
  })
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

function applyRouteQueryToFilters() {
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName.trim() : ''
}

async function loadProductPage() {
  loading.value = true
  try {
    const res = await productApi.pageProducts({
      productName: searchForm.productName.trim() || undefined,
      nodeType: searchForm.nodeType,
      status: searchForm.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
      syncAppliedFilters()
    }
  } catch (error) {
    console.error('获取产品分页失败', error)
    ElMessage.error('获取产品分页失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(id: string | number) {
  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = null
  try {
    const res = await productApi.getProductById(id)
    if (res.code === 200) {
      detailData.value = res.data
    }
  } catch (error) {
    detailErrorMessage.value = error instanceof Error ? error.message : '加载产品详情失败'
  } finally {
    detailLoading.value = false
  }
}

async function loadEditableDetail(id: string | number) {
  const res = await productApi.getProductById(id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
  }
}

function handleSearch() {
  searchForm.productName = searchForm.productName.trim()
  resetPage()
  clearSelection()
  void loadProductPage()
}

function handleReset() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void loadProductPage()
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
  void loadProductPage()
}

function handleClearAppliedFilters() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void loadProductPage()
}

function handleRefresh() {
  clearSelection()
  void loadProductPage()
}

function handleAdd() {
  editingProductId.value = null
  resetFormData()
  formVisible.value = true
}

async function handleEdit(row: Product) {
  try {
    editingProductId.value = row.id
    await loadEditableDetail(row.id)
    formVisible.value = true
  } catch (error) {
    console.error('加载产品编辑详情失败', error)
    ElMessage.error('加载产品详情失败')
  }
}

function handleOpenDetail(row: Product) {
  void openDetail(row.id)
}

function handleRowAction(command: string | number | object, row: Product) {
  if (command === 'devices') {
    handleJumpToDevices(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

function handleJumpToDevices(row?: Product | null) {
  if (!row?.productKey) {
    return
  }
  void router.push({
    path: '/devices',
    query: {
      productKey: row.productKey
    }
  })
}

async function handleDelete(row: Product) {
  try {
    await confirmDelete('产品', row.productName || row.productKey)
    await productApi.deleteProduct(row.id)
    ElMessage.success('删除成功')
    clearSelection()
    if (tableData.value.length === 1 && pagination.pageNum > 1) {
      setPageNum(pagination.pageNum - 1)
    }
    await loadProductPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '删除产品失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (editingProductId.value) {
      await productApi.updateProduct(editingProductId.value, { ...formData })
      ElMessage.success('更新成功')
    } else {
      await productApi.addProduct({ ...formData })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    clearSelection()
    resetPage()
    await loadProductPage()
  } catch (error) {
    console.error('提交产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '提交产品失败')
  } finally {
    submitLoading.value = false
  }
}

function handleFormClose() {
  formRef.value?.clearValidate()
  resetFormData()
  editingProductId.value = null
}

function handleSizeChange(size: number) {
  setPageSize(size)
  clearSelection()
  void loadProductPage()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  void loadProductPage()
}

watch(
  () => route.query.productName,
  () => {
    applyRouteQueryToFilters()
    resetPage()
    clearSelection()
    void loadProductPage()
  }
)

onMounted(async () => {
  applyRouteQueryToFilters()
  await loadProductPage()
})
</script>

<style scoped>
.product-asset-view {
  gap: 16px;
}

:deep(.product-detail-drawer .el-drawer__header) {
  padding: 20px 28px 16px;
}

:deep(.product-detail-drawer .el-drawer__body) {
  padding: 12px 28px 22px;
}

:deep(.product-detail-drawer .detail-drawer__heading h2) {
  margin-top: 0.28rem;
  font-size: clamp(1.88rem, 2.35vw, 2.24rem);
  letter-spacing: -0.015em;
}

:deep(.product-detail-drawer .detail-drawer__subtitle) {
  margin-top: 0.42rem;
  max-width: 38rem;
  font-size: 13px;
  line-height: 1.54;
}

.product-detail-layout {
  display: grid;
  gap: 10px;
}

.product-detail-zone {
  position: relative;
  overflow: hidden;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 6px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.92));
  box-shadow:
    0 6px 18px rgba(24, 45, 77, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-zone::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--brand) 72%, white),
    color-mix(in srgb, var(--accent) 52%, white),
    color-mix(in srgb, var(--brand-bright) 54%, white)
  );
}

.product-detail-zone--overview {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(244, 248, 255, 0.97), rgba(255, 255, 255, 0.95));
}

.product-detail-zone--ledger {
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--brand) 6%, transparent), transparent 48%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.product-detail-zone--governance {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent) 8%, transparent), transparent 45%),
    linear-gradient(180deg, rgba(249, 252, 255, 0.97), rgba(246, 249, 255, 0.94));
}

.product-detail-zone--danger {
  border-color: color-mix(in srgb, var(--danger) 20%, var(--panel-border));
}

.product-detail-zone__header {
  display: grid;
  gap: 0.18rem;
  margin-bottom: 0.62rem;
}

.product-detail-zone__kicker {
  color: var(--text-heading);
  font-size: 1.42rem;
  font-weight: 700;
  line-height: 1.34;
  letter-spacing: -0.01em;
}

.product-detail-zone__intro {
  margin: 0;
  color: var(--text-caption);
  font-size: 12.5px;
  line-height: 1.5;
}

.product-detail-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.14fr) minmax(0, 0.86fr);
  gap: 8px;
  align-items: start;
}

.product-detail-overview-lead {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.9rem 0.94rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 5px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 12%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.95));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 20px rgba(24, 55, 92, 0.05);
}

.product-detail-overview-lead--danger {
  border-color: color-mix(in srgb, var(--danger) 18%, transparent);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--danger) 10%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(255, 249, 249, 0.98), rgba(255, 243, 243, 0.95));
}

.product-detail-overview-lead__eyebrow {
  color: color-mix(in srgb, var(--brand) 64%, var(--text-caption-2));
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.08em;
}

.product-detail-overview-lead__title {
  color: var(--text-heading);
  font-size: clamp(1.2rem, 2vw, 1.48rem);
  font-weight: 700;
  line-height: 1.34;
}

.product-detail-overview-lead__text {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.58;
}

.product-detail-overview-progress {
  display: grid;
  gap: 0.3rem;
}

.product-detail-overview-progress__track {
  position: relative;
  overflow: hidden;
  height: 0.42rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 12%, white);
}

.product-detail-overview-progress__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand), color-mix(in srgb, var(--accent) 76%, var(--brand)));
}

.product-detail-overview-progress__caption {
  color: var(--text-caption-2);
  font-size: 11.5px;
  line-height: 1.48;
}

.product-detail-overview-lead__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.36rem 0.48rem;
}

.product-detail-overview-lead__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.65rem;
  padding: 0.24rem 0.58rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, transparent);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-caption);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-overview-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.product-detail-overview-metric {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
  padding: 0.74rem 0.8rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.product-detail-overview-metric__label {
  color: var(--text-caption-2);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-overview-metric__value {
  color: var(--text-heading);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.3;
  word-break: break-word;
}

.product-detail-overview-metric__hint {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.product-detail-ledger-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.76fr) minmax(0, 1.24fr);
  gap: 8px;
  align-items: start;
}

.product-detail-ledger-card {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.82rem 0.86rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 5px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.product-detail-ledger-card--contract {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 6%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(249, 252, 255, 0.98), rgba(246, 250, 255, 0.93));
}

.product-detail-card-header {
  display: grid;
  gap: 0.2rem;
}

.product-detail-card-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.26rem;
  font-weight: 700;
  line-height: 1.34;
  letter-spacing: -0.01em;
}

.product-detail-card-header p {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.48;
}

.product-detail-contract-list {
  display: grid;
  gap: 8px;
}

.product-detail-contract-item {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
  padding: 0.74rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-contract-item__label {
  color: var(--text-caption-2);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-contract-item__value {
  color: var(--text-heading);
  font-size: 1.92rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.01em;
  word-break: break-word;
}

.product-detail-archive-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.product-detail-archive-item {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
  padding: 0.74rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-archive-item--full {
  grid-column: 1 / -1;
}

.product-detail-archive-item__label {
  color: var(--text-caption-2);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-archive-item__value {
  color: var(--text-heading);
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.44;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-detail-description-card {
  display: grid;
  gap: 0.26rem;
  margin-top: 0.22rem;
  padding: 0.74rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-description-card__label {
  color: var(--text-caption-2);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-description-card__value {
  color: var(--text-heading);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.54;
  word-break: break-word;
}

.product-detail-governance-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) repeat(2, minmax(0, 1fr));
  gap: 8px;
  align-items: start;
}

.product-detail-governance-card {
  display: grid;
  gap: 0.36rem;
  min-width: 0;
  padding: 0.78rem 0.84rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-governance-card--lead {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 9%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.94));
}

.product-detail-governance-card--danger {
  border-color: color-mix(in srgb, var(--danger) 18%, transparent);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--danger) 10%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(255, 249, 249, 0.98), rgba(255, 243, 243, 0.95));
}

.product-detail-governance-card__label {
  color: color-mix(in srgb, var(--brand) 42%, var(--text-caption-2));
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-governance-card__title {
  color: var(--text-heading);
  font-size: 1.24rem;
  font-weight: 700;
  line-height: 1.36;
}

.product-detail-governance-card__text {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.54;
}

.product-detail-governance-list {
  display: grid;
  gap: 0.36rem;
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-caption);
  font-size: 12.5px;
  line-height: 1.52;
}

.product-hero-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  width: 100%;
}

.product-hero-card__heading {
  min-width: 0;
}

.product-hero-card__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.08rem;
}

.product-hero-card__caption {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
}

.product-workbench-card__filters {
  margin-bottom: 0.72rem;
}

.product-applied-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem 0.75rem;
  margin-bottom: 0.72rem;
}

.product-applied-filters__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.product-applied-filters__list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 0.45rem;
  min-width: 0;
}

.product-applied-filters__tag {
  margin: 0;
}

.product-applied-filters__clear {
  margin-left: auto;
  padding-inline: 0.08rem;
}

.product-inline-filter {
  display: grid;
}

.product-inline-filter__row {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(2, minmax(168px, 1fr)) auto;
  gap: 14px 18px;
  align-items: end;
}

.product-inline-filter__row :deep(.el-form-item) {
  margin-bottom: 0;
  min-width: 0;
}

.product-inline-filter__item {
  min-width: 0;
}

.product-inline-filter__actions {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  min-height: 100%;
}

.product-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.product-result-panel {
  position: relative;
  isolation: isolate;
  min-height: 14rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 255, 0.76));
}

.product-result-panel :deep(.el-loading-mask) {
  border-radius: inherit;
  background: rgba(248, 250, 255, 0.78) !important;
  backdrop-filter: blur(5px);
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

.product-mobile-list__grid {
  display: grid;
  gap: 12px;
}

.product-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
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

.product-mobile-card__meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 1.6rem;
  padding: 0.2rem 0.58rem;
  border-radius: var(--radius-pill);
  background: rgba(78, 89, 105, 0.08);
  color: var(--text-caption);
  font-size: 11.5px;
  line-height: 1.4;
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

.product-mobile-card__field span {
  color: var(--text-caption-2);
  font-size: 11.5px;
  line-height: 1.4;
}

.product-mobile-card__field strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.52;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-mobile-card__actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  justify-content: flex-start;
}

.product-mobile-card__actions :deep(.el-button) {
  margin-left: 0;
  padding-inline: 0.1rem;
}

.product-desktop-table {
  display: block;
}

.product-table-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.18rem;
  white-space: nowrap;
}

.product-table-actions :deep(.el-button) {
  margin-left: 0;
  padding-inline: 0.08rem;
}

@media (max-width: 1080px) {
  .product-detail-overview-grid,
  .product-detail-ledger-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-overview-metrics,
  .product-detail-governance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-inline-filter__row {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
    gap: 12px 14px;
  }
}

@media (max-width: 720px) {
  .product-hero-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .product-detail-archive-grid,
  .product-detail-overview-metrics,
  .product-detail-governance-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-zone {
    padding: 0.82rem 0.84rem;
  }

  .product-detail-zone__kicker,
  .product-detail-card-header h3 {
    font-size: 1.2rem;
  }

  .product-detail-contract-item__value {
    font-size: 1.52rem;
  }

  .product-inline-filter__row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .product-applied-filters {
    align-items: flex-start;
  }

  .product-applied-filters__clear {
    margin-left: 0;
  }

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
</style>
