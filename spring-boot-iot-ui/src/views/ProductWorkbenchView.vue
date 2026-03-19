<template>
  <div class="product-asset-view ops-workbench standard-list-view">
    <PanelCard class="ops-hero-card product-hero-card">
      <template #header>
        <div class="product-hero-card__header">
          <div class="product-hero-card__heading">
            <h2 class="product-hero-card__title">产品定义中心</h2>
            <p class="product-hero-card__caption">聚焦产品台账维护，支持筛选、查看、编辑、删除、导出和关联设备跳转。</p>
          </div>
        </div>
      </template>
      <div class="ops-kpi-grid product-hero-card__metrics">
        <MetricCard size="compact" label="启用产品" :value="String(enabledProductCount)" />
        <MetricCard size="compact" label="停用产品" :value="String(disabledProductCount)" />
        <MetricCard size="compact" label="在线设备数" :value="String(onlineDeviceTotal)" />
      </div>
    </PanelCard>

    <PanelCard title="筛选条件" class="ops-filter-card product-filter-card">
      <el-form :model="searchForm" label-position="top" class="ops-filter-form product-filter-form">
        <div class="product-filter-form__row">
          <el-form-item label="产品名称">
            <el-input
              id="query-product-name"
              v-model="searchForm.productName"
              placeholder="请输入产品名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-select v-model="searchForm.nodeType" placeholder="请选择节点类型" clearable>
              <el-option label="直连设备" :value="1" />
              <el-option label="网关设备" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="产品状态">
            <el-select v-model="searchForm.status" placeholder="请选择产品状态" clearable>
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <div class="product-filter-form__actions">
            <StandardActionGroup gap="sm">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </StandardActionGroup>
          </div>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard class="ops-table-card product-table-card">
      <template #header>
        <div class="product-table-card__header">
          <div class="product-table-card__heading">
            <h2 class="product-table-card__title">产品台账</h2>
          </div>
          <StandardActionGroup gap="sm">
            <el-button v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
          </StandardActionGroup>
        </div>
      </template>

      <StandardTableToolbar
        :meta-items="[
          `已选 ${selectedRows.length} 项`,
          `启用 ${enabledProductCount} 个`,
          `停用 ${disabledProductCount} 个`,
          `在线设备 ${onlineDeviceTotal} 台`
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

      <el-table ref="tableRef" v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
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
        <el-table-column label="操作" width="300" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
            <el-button v-permission="'iot:products:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleJumpToDevices(row)">查看设备</el-button>
            <el-button v-permission="'iot:products:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="ops-pagination">
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
      :title="detailTitle"
      :tags="detailTags"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <div v-if="detailData" class="product-detail-stack">
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <h3>产品概览</h3>
          </div>
          <div class="detail-summary-grid">
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">产品状态</span>
              <strong class="detail-summary-card__value">{{ getStatusText(detailData.status) }}</strong>
              <p class="detail-summary-card__hint">当前是否允许继续接入与维护</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">在线设备数</span>
              <strong class="detail-summary-card__value">{{ detailData.onlineDeviceCount || 0 }}</strong>
              <p class="detail-summary-card__hint">该产品当前在线的关联设备数量</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">最近上报</span>
              <strong class="detail-summary-card__value">{{ formatDateTime(detailData.lastReportTime) }}</strong>
              <p class="detail-summary-card__hint">最近一次设备上报时间</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">更新时间</span>
              <strong class="detail-summary-card__value">{{ formatDateTime(detailData.updateTime) }}</strong>
              <p class="detail-summary-card__hint">最近一次台账维护时间</p>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <h3>基础信息</h3>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">产品 ID</span>
              <strong class="detail-field__value">{{ detailData.id }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">产品 Key</span>
              <strong class="detail-field__value">{{ detailData.productKey || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">产品名称</span>
              <strong class="detail-field__value">{{ detailData.productName || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">协议编码</span>
              <strong class="detail-field__value">{{ detailData.protocolCode || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">节点类型</span>
              <strong class="detail-field__value">{{ getNodeTypeText(detailData.nodeType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">数据格式</span>
              <strong class="detail-field__value">{{ detailData.dataFormat || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">厂商</span>
              <strong class="detail-field__value">{{ detailData.manufacturer || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">创建时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.createTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">更新时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.updateTime) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">说明</span>
              <strong class="detail-field__value detail-field__value--plain">{{ detailData.description || '--' }}</strong>
            </div>
          </div>
          <div class="detail-notice">
            <span class="detail-notice__label">维护规则</span>
            <strong class="detail-notice__value">
              产品 Key 创建后保持稳定；变更协议编码或节点类型时会同步写回关联设备基础字段；删除前需先清理关联设备。
            </strong>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import MetricCard from '@/components/MetricCard.vue'
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
const enabledProductCount = computed(() => tableData.value.filter((item) => item.status !== 0).length)
const disabledProductCount = computed(() => tableData.value.filter((item) => item.status === 0).length)
const onlineDeviceTotal = computed(() =>
  tableData.value.reduce((sum, item) => sum + Number(item.onlineDeviceCount || 0), 0)
)

const detailTags = computed(() => {
  if (!detailData.value) {
    return []
  }
  return [
    { label: getStatusText(detailData.value.status), type: detailData.value.status === 1 ? 'success' : 'danger' as const },
    { label: getNodeTypeText(detailData.value.nodeType), type: 'info' as const }
  ]
})

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

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function handleSelectionChange(rows: Product[]) {
  selectedRows.value = rows
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
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName : ''
}

async function loadProductPage() {
  loading.value = true
  try {
    const res = await productApi.pageProducts({
      productName: searchForm.productName || undefined,
      nodeType: searchForm.nodeType,
      status: searchForm.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
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
  resetPage()
  clearSelection()
  void loadProductPage()
}

function handleReset() {
  searchForm.productName = ''
  searchForm.nodeType = undefined
  searchForm.status = undefined
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

.product-detail-stack {
  display: grid;
  gap: 16px;
}

.product-hero-card__header,
.product-table-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  width: 100%;
}

.product-hero-card__heading,
.product-table-card__heading {
  min-width: 0;
}

.product-hero-card__title,
.product-table-card__title {
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

.product-hero-card__metrics {
  margin-top: 0.1rem;
}

.product-filter-form {
  display: grid;
}

.product-filter-form__row {
  display: grid;
  grid-template-columns: minmax(240px, 1.5fr) repeat(2, minmax(180px, 1fr)) auto;
  gap: 12px 16px;
  align-items: end;
}

.product-filter-form__row :deep(.el-form-item) {
  margin-bottom: 0;
}

.product-filter-form__actions {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  min-height: 100%;
}

@media (max-width: 1080px) {
  .product-filter-form__row {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .product-filter-form__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .product-hero-card__header,
  .product-table-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .product-filter-form__row {
    grid-template-columns: 1fr;
  }
}
</style>
