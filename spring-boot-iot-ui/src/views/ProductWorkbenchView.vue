<template>
  <div class="product-asset-view ops-workbench standard-list-view">
    <PanelCard
      eyebrow="Product Asset Workspace"
      title="产品定义中心"
      description="围绕产品台账、接入协议、库存归属和状态维护组织统一入口，让业务、运维和实施人员都能快速确认哪些产品已建档、当前是否已有库存设备。"
      class="ops-hero-card"
    >
      <template #actions>
        <el-button v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="产品总数" :value="String(pagination.total)" :badge="{ label: 'Product', tone: 'brand' }" />
        <MetricCard label="当前页已建库存" :value="String(inventoryProductCount)" :badge="{ label: 'Inventory', tone: 'success' }" />
        <MetricCard label="当前页待补库存" :value="String(noInventoryProductCount)" :badge="{ label: 'Pending', tone: 'warning' }" />
        <MetricCard label="当前页停用产品" :value="String(disabledProductCount)" :badge="{ label: 'Review', tone: 'danger' }" />
      </div>
      <div class="ops-inline-note">
        当前交付先完成产品定义中心首个可用闭环：产品台账可见、详情可查、新增、编辑、删除、导出，以及按产品查看关联库存设备。
        产品 Key 作为接入身份标识，创建后保持稳定；协议编码和节点类型变更时会同步写回当前关联设备的基础字段。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Product Filters"
      title="筛选条件"
      description="优先按产品 Key、产品名称、协议、节点类型和启停状态筛出需要补录、停用或继续维护的产品台账。"
      class="ops-filter-card"
    >
      <el-form :model="searchForm" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="产品 Key">
              <el-input v-model="searchForm.productKey" placeholder="请输入产品 Key" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="产品名称">
              <el-input v-model="searchForm.productName" placeholder="请输入产品名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="协议编码">
              <el-input v-model="searchForm.protocolCode" placeholder="请输入协议编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="节点类型">
              <el-select v-model="searchForm.nodeType" placeholder="请选择节点类型" clearable>
                <el-option label="直连设备" :value="1" />
                <el-option label="网关设备" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="产品状态">
              <el-select v-model="searchForm.status" placeholder="请选择产品状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="ops-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Product Inventory"
      title="产品台账列表"
      :description="`当前共 ${pagination.total} 条产品记录，支持详情、编辑、删除、导出和按产品查看关联设备库存。`"
      class="ops-table-card"
    >
      <StandardTableToolbar
        :meta-items="[
          `已选 ${selectedRows.length} 项`,
          `已建库存 ${inventoryProductCount} 个`,
          `待补库存 ${noInventoryProductCount} 个`,
          `停用 ${disabledProductCount} 个`
        ]"
      >
        <template #right>
          <el-button v-permission="'iot:products:export'" link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
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
        <el-table-column prop="deviceCount" label="库存设备数" width="110" align="center" />
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
      eyebrow="Product Asset Profile"
      :title="detailTitle"
      subtitle="统一查看产品主数据、接入基线和当前库存归属情况。"
      :tags="detailTags"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <div v-if="detailData" class="product-detail-stack">
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>库存概览</h3>
              <p>先回答“产品是否已建档、是否已有库存设备、当前还有多少在线资产可继续运维”。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">产品状态</span>
              <strong class="detail-summary-card__value">{{ getStatusText(detailData.status) }}</strong>
              <p class="detail-summary-card__hint">当前是否允许继续使用</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">库存设备数</span>
              <strong class="detail-summary-card__value">{{ detailData.deviceCount || 0 }}</strong>
              <p class="detail-summary-card__hint">{{ getInventoryStatusText(detailData.deviceCount) }}</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">在线设备数</span>
              <strong class="detail-summary-card__value">{{ detailData.onlineDeviceCount || 0 }}</strong>
              <p class="detail-summary-card__hint">帮助运维判断当前接入负载</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">最近设备上报</span>
              <strong class="detail-summary-card__value">{{ formatDateTime(detailData.lastReportTime) }}</strong>
              <p class="detail-summary-card__hint">来自该产品下最新一台设备</p>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>产品档案</h3>
              <p>维护产品身份、协议与节点类型，确保设备建档、接入校验和库存核查使用同一份产品基线。</p>
            </div>
          </div>
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
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>维护规则</h3>
              <p>产品台账既服务业务确认库存，也服务接入运维，因此需要保持身份标识稳定并控制关键基线的修改范围。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">当前规则</span>
              <strong class="detail-field__value detail-field__value--plain">
                产品 Key 创建后保持稳定；若修改协议编码或节点类型，系统会同步写回当前关联设备的基础字段。删除产品前必须先清空该产品下的库存设备。
              </strong>
            </div>
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
            查看关联设备
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardDetailDrawer>

    <StandardFormDrawer
      v-model="formVisible"
      eyebrow="Product Asset Form"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护产品身份、协议基线和启停状态。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>维护提示</strong>
          <span>建议先定义产品身份与协议，再允许设备建档。产品 Key 作为接入身份标识，创建后不允许修改；停用产品前请先核查关联库存设备是否仍在使用。</span>
        </div>

        <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础档案</h3>
                <p>维护产品唯一身份、展示名称和厂商归属，方便业务、运维和实施人员统一确认台账对象。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="产品 Key" prop="productKey">
                <el-input v-model="formData.productKey" :disabled="Boolean(editingProductId)" placeholder="请输入产品 Key，例如 accept-http-product-01" />
              </el-form-item>
              <el-form-item label="产品名称" prop="productName">
                <el-input v-model="formData.productName" placeholder="请输入产品名称" />
              </el-form-item>
              <el-form-item label="厂商">
                <el-input v-model="formData.manufacturer" placeholder="请输入厂商名称" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>接入基线</h3>
                <p>产品会决定设备建档时继承的协议编码和节点类型，因此需要在这里维护统一的接入口径。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="协议编码" prop="protocolCode">
                <el-input v-model="formData.protocolCode" placeholder="请输入协议编码，例如 mqtt-json" />
              </el-form-item>
              <el-form-item label="节点类型" prop="nodeType">
                <el-select v-model="formData.nodeType" placeholder="请选择节点类型">
                  <el-option label="直连设备" :value="1" />
                  <el-option label="网关设备" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据格式">
                <el-input v-model="formData.dataFormat" placeholder="请输入数据格式，例如 JSON" />
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
              <div>
                <h3>说明与维护建议</h3>
                <p>补充业务背景、适用场景、厂商约束或接入注意事项，减少后续库存和接入排障成本。</p>
              </div>
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
          :confirm-text="editingProductId ? '保存产品变更' : '提交产品定义'"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </el-button>
          <el-button
            v-permission="submitPermission"
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ editingProductId ? '保存产品变更' : '提交产品定义' }}
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="产品定义中心导出列设置"
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
  productKey: string
  productName: string
  protocolCode: string
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
  productKey: '',
  productName: '',
  protocolCode: '',
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
const submitPermission = computed(() => (editingProductId.value ? 'iot:products:update' : 'iot:products:add'))
const detailTitle = computed(() => detailData.value?.productName || detailData.value?.productKey || '产品详情')
const inventoryProductCount = computed(() => tableData.value.filter((item) => Number(item.deviceCount || 0) > 0).length)
const noInventoryProductCount = computed(() => tableData.value.filter((item) => Number(item.deviceCount || 0) <= 0).length)
const disabledProductCount = computed(() => tableData.value.filter((item) => item.status === 0).length)

const detailTags = computed(() => {
  if (!detailData.value) {
    return []
  }
  return [
    { label: getStatusText(detailData.value.status), type: detailData.value.status === 1 ? 'success' : 'danger' as const },
    { label: getInventoryStatusText(detailData.value.deviceCount), type: Number(detailData.value.deviceCount || 0) > 0 ? 'success' : 'warning' as const }
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
  { key: 'deviceCount', label: '库存设备数' },
  { key: 'onlineDeviceCount', label: '在线设备数' },
  { key: 'lastReportTime', label: '最近设备上报', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'createTime', label: '创建时间', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'updateTime', label: '更新时间', formatter: (value) => formatDateTime(String(value || '')) }
]

const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '库存核查', keys: ['productKey', 'productName', 'status', 'deviceCount', 'onlineDeviceCount', 'lastReportTime'] },
  { label: '基础档案', keys: ['id', 'productKey', 'productName', 'protocolCode', 'nodeType', 'dataFormat', 'manufacturer', 'createTime', 'updateTime'] }
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

function getInventoryStatusText(deviceCount?: number | null) {
  return Number(deviceCount || 0) > 0 ? '已建库存' : '待补库存'
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
  searchForm.productKey = typeof route.query.productKey === 'string' ? route.query.productKey : ''
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName : ''
}

async function loadProductPage() {
  loading.value = true
  try {
    const res = await productApi.pageProducts({
      productKey: searchForm.productKey || undefined,
      productName: searchForm.productName || undefined,
      protocolCode: searchForm.protocolCode || undefined,
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
  searchForm.productKey = ''
  searchForm.productName = ''
  searchForm.protocolCode = ''
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
  () => [route.query.productKey, route.query.productName] as const,
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
  padding: 20px;
  display: grid;
  gap: 16px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}

.ops-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.ops-inline-note {
  padding: 12px 14px;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid rgba(42, 63, 95, 0.1);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-caption);
  line-height: 1.7;
}

.ops-filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.ops-pagination {
  margin-top: 16px;
}

.product-detail-stack {
  display: grid;
  gap: 16px;
}

.ops-drawer-stack {
  display: grid;
  gap: 16px;
}

.ops-drawer-note {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, transparent);
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 42%);
}

.ops-drawer-note strong {
  color: var(--text-heading);
  font-size: 14px;
}

.ops-drawer-note span {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.7;
}

.ops-drawer-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.88);
}

.ops-drawer-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
}

.ops-drawer-section__header p {
  margin: 6px 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.ops-drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.ops-drawer-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.ops-drawer-grid__full {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .product-asset-view {
    padding: 16px;
  }

  .ops-drawer-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .ops-filter-actions {
    justify-content: stretch;
  }
}
</style>
