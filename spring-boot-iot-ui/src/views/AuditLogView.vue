<template>
  <div class="audit-log-view">
    <StandardWorkbenchPanel
      :title="pageTitle"
      :description="pageDescription"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader
          :model="searchForm"
          :show-advanced="showAdvancedFilters"
          show-advanced-toggle
          :advanced-hint="advancedFilterHint"
          @toggle-advanced="toggleAdvancedFilters"
        >
          <template #primary>
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                :placeholder="quickSearchPlaceholder"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item v-if="isBusinessMode">
              <el-select v-model="searchForm.operationType" placeholder="操作类型" clearable>
                <el-option
                  v-for="item in businessOperationTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.operationModule"
                :placeholder="isSystemMode ? '异常模块' : '操作模块'"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.operationResult" placeholder="操作结果" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-select v-model="searchForm.requestMethod" :placeholder="isSystemMode ? '请求通道' : '请求方法'" clearable>
                <el-option
                  v-for="item in systemRequestMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>
          <template #advanced>
            <el-form-item v-if="isBusinessMode">
              <el-input
                v-model="searchForm.traceId"
                placeholder="TraceId"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-input
                v-model="searchForm.requestUrl"
                placeholder="目标 / URL"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <template v-if="isSystemMode">
              <el-form-item>
                <el-input
                  v-model="searchForm.deviceCode"
                  placeholder="设备编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.productKey"
                  placeholder="产品标识"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.errorCode"
                  placeholder="异常编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.exceptionClass"
                  placeholder="异常类型"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
            </template>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
        <div v-if="appliedQuickSearchValue" class="audit-log-quick-search-tag">
          <el-tag closable class="audit-log-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ appliedQuickSearchValue }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #notices>
        <div class="audit-log-notice-grid">
          <el-alert
            :title="viewTip"
            type="info"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            :title="statsSummaryText"
            type="success"
            :closable="false"
            show-icon
            class="stats-alert"
          />
        </div>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `已选 ${selectedRows.length} 项`,
            isSystemMode ? `异常 ${systemStats.total}` : `审计 ${businessStats.total}`,
            isSystemMode
              ? `今日 ${systemStats.todayCount}`
              : `成功 ${businessStats.successCount}`,
            isSystemMode
              ? `链路 ${systemStats.distinctTraceCount}`
              : `失败 ${businessStats.failureCount}`
          ]"
        >
          <template #right>
            <StandardButton v-if="isSystemMode" action="refresh" link :disabled="!canJumpFromSearch" @click="handleJumpToMessageTrace()">
              链路追踪台
            </StandardButton>
            <StandardButton action="refresh" link @click="openExportColumnSetting">导出列设置</StandardButton>
            <StandardButton action="batch" link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</StandardButton>
            <StandardButton action="refresh" link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</StandardButton>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <el-table
        ref="tableRef"
        v-loading="loading"
        class="audit-log-table"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column v-if="isBusinessMode" prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)">
              {{ getOperationTypeName(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="operationModule" label="操作模块" :width="150" />
        <StandardTableTextColumn prop="operationMethod" label="操作方法" :min-width="180" />
        <StandardTableTextColumn prop="requestUrl" label="请求URL/目标" :min-width="220" />
        <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
        <StandardTableTextColumn v-if="isSystemMode" prop="traceId" label="TraceId" :min-width="180" />
        <StandardTableTextColumn v-if="isSystemMode" prop="deviceCode" label="设备编码" :min-width="140" />
        <StandardTableTextColumn v-if="isSystemMode" prop="productKey" label="产品标识" :min-width="140" />
        <StandardTableTextColumn v-if="isSystemMode" prop="errorCode" label="异常编码" :min-width="120" />
        <StandardTableTextColumn v-if="isSystemMode" prop="exceptionClass" label="异常类型" :min-width="180" />
        <StandardTableTextColumn v-if="isBusinessMode" prop="userName" label="操作用户" :width="120" />
        <StandardTableTextColumn v-if="isBusinessMode" prop="ipAddress" label="操作IP" :width="150" />
        <StandardTableTextColumn v-if="isSystemMode" prop="resultMessage" label="异常摘要" :min-width="220" />
        <StandardTableTextColumn prop="operationTime" label="操作时间" :width="180" />
        <el-table-column prop="operationResult" label="操作结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationResultTag(row.operationResult)" round>
              {{ getOperationResultName(row.operationResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isSystemMode ? 210 : 150" fixed="right">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide">
              <StandardActionLink @click="handleDetail(row)">详情</StandardActionLink>
              <StandardActionLink
                v-if="isSystemMode"
                :disabled="!canJumpToMessageTrace(row)"
                @click="handleJumpToMessageTrace(row)"
              >
                追踪
              </StandardActionLink>
              <StandardActionLink @click="handleDelete(row)">删除</StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>

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

    <AuditLogDetailDrawer
      v-model="detailVisible"
      :title="detailDialogTitle"
      :detail="detailData"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
    />

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      :title="`${pageTitle}导出列设置`"
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
import { ElMessage } from 'element-plus'
import { pageLogs, getAuditLogById, deleteAuditLog, getSystemErrorStats, getBusinessAuditStats, type AuditLogRecord } from '@/api/auditLog'
import { isHandledRequestError } from '@/api/request'
import type { BusinessAuditStats, SystemErrorStats } from '@/types/api'
import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'

type AuditLogViewMode = 'business' | 'system'

const route = useRoute()
const router = useRouter()
const viewMode = computed<AuditLogViewMode>(() => (route.path === '/system-log' ? 'system' : 'business'))
const isSystemMode = computed(() => viewMode.value === 'system')
const isBusinessMode = computed(() => viewMode.value === 'business')
const pageTitle = computed(() => (isSystemMode.value ? '异常观测台' : '审计中心'))
const pageDescription = computed(() =>
  isSystemMode.value
    ? '面向研发、测试与运维的异常观测工作台，聚焦设备接入与后台链路问题。'
    : '面向客户与治理侧的审计留痕中心，默认不展示后台系统异常记录。'
)
const viewTip = computed(() =>
  isSystemMode.value
    ? '异常观测台仅展示 `sys_audit_log` 中 `operation_type=system_error` 的记录，可结合 TraceId、设备编码与“链路追踪台”页面快速串联排障。'
    : '审计中心默认排除 `system_error` 记录；如需排查设备接入或后台异常，请前往“接入智维 > 异常观测台”。'
)
const detailDialogTitle = computed(() => `${pageTitle.value}详情`)
const canJumpFromSearch = computed(() =>
  Boolean(
    appliedFilters.traceId || appliedFilters.deviceCode || appliedFilters.productKey
      || (appliedFilters.requestMethod === 'MQTT' && appliedFilters.requestUrl)
  )
)
const businessOperationTypeOptions = [
  { label: '新增', value: 'insert' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '查询', value: 'select' }
]
const systemRequestMethodOptions = [
  { label: 'MQTT', value: 'MQTT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

// 搜索表单
const searchForm = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const appliedFilters = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const quickSearchKeyword = ref('')
const showAdvancedFilters = ref(false)

// 分页

// 表格数据
const tableData = ref<AuditLogRecord[]>([])
const tableRef = ref()
const selectedRows = ref<AuditLogRecord[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination()
const exportColumns: CsvColumn<any>[] = [
  { key: 'operationType', label: '操作类型', formatter: (value) => getOperationTypeName(String(value || '')) },
  { key: 'operationModule', label: '操作模块' },
  { key: 'operationMethod', label: '操作方法' },
  { key: 'requestUrl', label: '请求URL' },
  { key: 'requestMethod', label: '请求方法' },
  { key: 'traceId', label: 'TraceId' },
  { key: 'deviceCode', label: '设备编码' },
  { key: 'productKey', label: '产品标识' },
  { key: 'errorCode', label: '异常编码' },
  { key: 'exceptionClass', label: '异常类型' },
  { key: 'userName', label: '操作用户' },
  { key: 'ipAddress', label: '操作IP' },
  { key: 'resultMessage', label: '结果消息' },
  { key: 'operationTime', label: '操作时间' },
  { key: 'operationResult', label: '操作结果', formatter: (value) => (Number(value) === 1 ? '成功' : '失败') }
]
const exportColumnStorageKey = computed(() => (isSystemMode.value ? 'system-log-view' : 'business-log-view'))
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = computed(() =>
  isSystemMode.value
    ? [
        { label: '默认模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'resultMessage', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationModule', 'requestUrl', 'requestMethod', 'deviceCode', 'productKey', 'resultMessage', 'operationTime'] },
        { label: '研发模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass', 'resultMessage', 'operationResult', 'operationTime'] }
      ]
    : [
        { label: '默认模板', keys: ['operationType', 'operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationType', 'operationModule', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '管理模板', keys: ['operationType', 'operationModule', 'operationMethod', 'userName', 'operationTime', 'operationResult'] }
      ]
)
const selectedExportColumnKeys = ref<string[]>([])
const exportColumnDialogVisible = ref(false)

// 加载状态
const loading = ref(false)
const statsLoading = ref(false)

const createEmptySystemStats = (): SystemErrorStats => ({
  total: 0,
  todayCount: 0,
  mqttCount: 0,
  systemCount: 0,
  distinctTraceCount: 0,
  distinctDeviceCount: 0,
  topModules: [],
  topExceptionClasses: [],
  topErrorCodes: []
})

const createEmptyBusinessStats = (): BusinessAuditStats => ({
  total: 0,
  todayCount: 0,
  successCount: 0,
  failureCount: 0,
  distinctUserCount: 0,
  topModules: [],
  topUsers: [],
  topOperationTypes: []
})

const systemStats = ref<SystemErrorStats>(createEmptySystemStats())
const businessStats = ref<BusinessAuditStats>(createEmptyBusinessStats())
const quickSearchPlaceholder = computed(() => (isSystemMode.value ? '快速搜索（TraceId）' : '快速搜索（操作用户）'))
const advancedFilterKeys = computed<
  Array<'traceId' | 'deviceCode' | 'productKey' | 'requestUrl' | 'errorCode' | 'exceptionClass'>
>(() =>
  isSystemMode.value
    ? ['requestUrl', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass']
    : ['traceId']
)
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  advancedAppliedCount,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'userName', label: '操作用户', isActive: (value) => isBusinessMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationType', label: (value) => `操作类型：${getOperationTypeName(String(value || ''))}`, clearValue: undefined, isActive: (value) => isBusinessMode.value && value !== undefined },
    { key: 'traceId', label: 'TraceId', advanced: true },
    { key: 'operationModule', label: (value) => `${isSystemMode.value ? '异常模块' : '操作模块'}：${String(value || '').trim()}` },
    { key: 'requestMethod', label: (value) => `请求通道：${String(value || '')}`, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'requestUrl', label: '目标 / URL', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'deviceCode', label: '设备编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'productKey', label: '产品标识', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'errorCode', label: '异常编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'exceptionClass', label: '异常类型', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationResult', label: (value) => `操作结果：${getOperationResultName(Number(value))}`, clearValue: undefined, isActive: (value) => value !== undefined }
  ],
  defaults: {
    userName: '',
    operationType: undefined,
    traceId: '',
    deviceCode: '',
    productKey: '',
    operationModule: '',
    requestMethod: '',
    requestUrl: '',
    errorCode: '',
    exceptionClass: '',
    operationResult: undefined
  }
})
const appliedQuickSearchValue = computed(() => (isSystemMode.value ? appliedFilters.traceId.trim() : appliedFilters.userName.trim()))
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`
})

const statsSummaryText = computed(() => {
  if (statsLoading.value) {
    return '正在加载统计概览...'
  }
  if (isSystemMode.value) {
    const topModule = systemStats.value.topModules[0]?.label || '--'
    return `异常总量 ${systemStats.value.total}，今日 ${systemStats.value.todayCount}，MQTT ${systemStats.value.mqttCount}，链路数 ${systemStats.value.distinctTraceCount}，高频模块 ${topModule}`
  }
  const topUser = businessStats.value.topUsers[0]?.label || '--'
  return `审计总量 ${businessStats.value.total}，今日 ${businessStats.value.todayCount}，成功 ${businessStats.value.successCount}，失败 ${businessStats.value.failureCount}，活跃用户 ${businessStats.value.distinctUserCount}（Top 用户 ${topUser}）`
})

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<Partial<AuditLogRecord>>({})
const detailLoading = ref(false)
const detailErrorMessage = ref('')

const defaultExportKeys = exportColumns.map((column) => String(column.key))

const reloadExportSelection = () => {
  selectedExportColumnKeys.value = loadCsvColumnSelection(exportColumnStorageKey.value, defaultExportKeys)
}

const hasFilledFilter = (value: string | number | undefined) => {
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined
}

const syncQuickSearchKeywordFromFilters = () => {
  quickSearchKeyword.value = isSystemMode.value ? searchForm.traceId : searchForm.userName
}

const applyQuickSearchKeywordToFilters = () => {
  const keyword = quickSearchKeyword.value.trim()
  if (isSystemMode.value) {
    searchForm.traceId = keyword
    return
  }
  searchForm.userName = keyword
}

const syncAdvancedFilterState = () => {
  showAdvancedFilters.value = advancedFilterKeys.value.some((key) => hasFilledFilter(searchForm[key]))
}

const resetSearchForm = () => {
  quickSearchKeyword.value = ''
  searchForm.userName = ''
  searchForm.operationType = undefined
  searchForm.traceId = ''
  searchForm.deviceCode = ''
  searchForm.productKey = ''
  searchForm.operationModule = ''
  searchForm.requestMethod = ''
  searchForm.requestUrl = ''
  searchForm.errorCode = ''
  searchForm.exceptionClass = ''
  searchForm.operationResult = undefined
  showAdvancedFilters.value = false
}

const readRouteQueryValue = (key: string) => {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

const parseOptionalNumber = (value: string) => {
  if (!value) {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const applySystemRouteQuery = () => {
  if (!isSystemMode.value) {
    return
  }
  searchForm.traceId = readRouteQueryValue('traceId')
  searchForm.deviceCode = readRouteQueryValue('deviceCode')
  searchForm.productKey = readRouteQueryValue('productKey')
  searchForm.operationModule = readRouteQueryValue('operationModule')
  searchForm.requestMethod = readRouteQueryValue('requestMethod')
  searchForm.requestUrl = readRouteQueryValue('requestUrl')
  searchForm.errorCode = readRouteQueryValue('errorCode')
  searchForm.exceptionClass = readRouteQueryValue('exceptionClass')
  searchForm.operationResult = parseOptionalNumber(readRouteQueryValue('operationResult'))
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
}

// 获取审计日志查询条件
const buildAuditLogQueryParams = () => ({
  traceId: appliedFilters.traceId,
  operationModule: appliedFilters.operationModule,
  operationResult: appliedFilters.operationResult,
  ...(isBusinessMode.value
    ? {
        userName: appliedFilters.userName,
        operationType: appliedFilters.operationType,
        excludeSystemError: true
      }
    : {
        operationType: 'system_error',
        deviceCode: appliedFilters.deviceCode,
        productKey: appliedFilters.productKey,
        requestMethod: appliedFilters.requestMethod,
        requestUrl: appliedFilters.requestUrl,
        errorCode: appliedFilters.errorCode,
        exceptionClass: appliedFilters.exceptionClass
      })
})

const logPageError = (context: string, error: unknown) => {
  if (!isHandledRequestError(error)) {
    console.error(context, error)
  }
}

// 获取审计日志列表
const getAuditLogList = async () => {
  loading.value = true
  try {
    const res = await pageLogs({
      ...buildAuditLogQueryParams(),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    logPageError('获取审计日志列表失败', error)
  } finally {
    loading.value = false
  }
}

// 获取日志统计
const getAuditLogStats = async () => {
  statsLoading.value = true
  try {
    if (isSystemMode.value) {
      systemStats.value = createEmptySystemStats()
      const res = await getSystemErrorStats(buildAuditLogQueryParams())
      if (res.code === 200 && res.data) {
        systemStats.value = { ...createEmptySystemStats(), ...res.data }
      }
      return
    }

    businessStats.value = createEmptyBusinessStats()
    const res = await getBusinessAuditStats(buildAuditLogQueryParams())
    if (res.code === 200 && res.data) {
      businessStats.value = { ...createEmptyBusinessStats(), ...res.data }
    }
  } catch (error) {
    logPageError('获取日志统计失败', error)
  } finally {
    statsLoading.value = false
  }
}

// 初始化
onMounted(() => {
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
})

watch(viewMode, (newMode, oldMode) => {
  if (newMode === oldMode) {
    return
  }
  resetSearchForm()
  clearSelection()
  resetPage()
  resetTotal()
  detailVisible.value = false
  detailData.value = {}
  detailLoading.value = false
  detailErrorMessage.value = ''
  exportColumnDialogVisible.value = false
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
})

watch(
  () => [
    route.query.traceId,
    route.query.deviceCode,
    route.query.productKey,
    route.query.operationModule,
    route.query.requestMethod,
    route.query.requestUrl,
    route.query.errorCode,
    route.query.exceptionClass,
    route.query.operationResult
  ],
  (current, previous) => {
    if (!isSystemMode.value) {
      return
    }
    if (JSON.stringify(current) === JSON.stringify(previous)) {
      return
    }
    applySystemRouteQuery()
    resetPage()
    clearSelection()
    syncAppliedFilters()
    getAuditLogList()
    getAuditLogStats()
  }
)

const triggerSearch = (resetPageFirst = false) => {
  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  if (resetPageFirst) {
    resetPage()
  }
  clearSelection()
  getAuditLogList()
  getAuditLogStats()
}

// 处理搜索
const handleSearch = () => {
  triggerSearch(true)
}

// 重置搜索
const handleReset = () => {
  resetSearchForm()
  triggerSearch(true)
}

const handleQuickSearch = () => {
  triggerSearch(true)
}

const handleClearQuickSearch = () => {
  quickSearchKeyword.value = ''
  triggerSearch(true)
}

const toggleAdvancedFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const handleClearAppliedFilters = () => {
  handleReset()
}

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key)
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  triggerSearch(true)
}

const handleSelectionChange = (rows: AuditLogRecord[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  triggerSearch(false)
}

const canJumpToMessageTrace = (row?: AuditLogRecord) => {
  const target = row || searchForm
  const requestMethod = 'requestMethod' in target ? target.requestMethod : undefined
  const requestUrl = 'requestUrl' in target ? target.requestUrl : undefined
  return Boolean(target.traceId || target.deviceCode || target.productKey || (requestMethod === 'MQTT' && requestUrl))
}

const handleJumpToMessageTrace = (row?: AuditLogRecord) => {
  const target = row || {
    traceId: quickSearchKeyword.value.trim() || searchForm.traceId,
    deviceCode: searchForm.deviceCode,
    productKey: searchForm.productKey,
    requestMethod: searchForm.requestMethod,
    requestUrl: searchForm.requestUrl
  }
  const requestMethod = 'requestMethod' in target ? target.requestMethod : undefined
  const requestUrl = 'requestUrl' in target ? target.requestUrl : undefined
  router.push({
    path: '/message-trace',
    query: {
      traceId: target.traceId || undefined,
      deviceCode: target.deviceCode || undefined,
      productKey: target.productKey || undefined,
      topic: requestMethod === 'MQTT' ? requestUrl || undefined : undefined
    }
  })
}

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true
}

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey.value, selectedKeys)
}

const getResolvedExportColumns = () => resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)

const handleExportSelected = () => {
  downloadRowsAsCsv(`${pageTitle.value}-选中项.csv`, selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv(`${pageTitle.value}-当前结果.csv`, tableData.value, getResolvedExportColumns())
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  setPageSize(size)
  getAuditLogList()
}

// 当前页变化
const handlePageChange = (page: number) => {
  setPageNum(page)
  getAuditLogList()
}

// 查看详情
const handleDetail = async (row: AuditLogRecord) => {
  if (row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('当前日志缺少主键，无法查看详情')
    return
  }

  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = { ...row }
  try {
    const res = await getAuditLogById(String(row.id))
    if (!res.data || Array.isArray(res.data)) {
      ElMessage.warning(`${pageTitle.value}详情不存在或已删除`)
      detailVisible.value = false
      return
    }
    detailData.value = { ...row, ...res.data }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(`获取${pageTitle.value}详情失败`)
    }
    detailErrorMessage.value = error instanceof Error ? error.message : `获取${pageTitle.value}详情失败`
    logPageError('获取日志详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// 删除
const handleDelete = async (row: AuditLogRecord) => {
  try {
    await confirmAction({
      title: `删除${pageTitle.value}`,
      message: `确认删除当前${pageTitle.value}吗？删除后不可恢复。`,
      type: 'warning',
      confirmButtonText: '确认删除'
    })
    const res = await deleteAuditLog(String(row.id))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getAuditLogList()
      getAuditLogStats()
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    logPageError('删除失败', error)
  }
}

// 获取操作类型名称
const getOperationTypeName = (type: string) => {
  const map: Record<string, string> = {
    insert: '新增',
    update: '修改',
    delete: '删除',
    select: '查询',
    system_error: '系统异常'
  }
  return map[type] || type
}

// 获取操作类型标签
const getOperationTypeTag = (type: string) => {
  const map: Record<string, string> = {
    insert: 'primary',
    update: 'warning',
    delete: 'danger',
    select: 'info',
    system_error: 'danger'
  }
  return map[type] || 'info'
}

const getOperationResultName = (result?: number | null) => {
  if (result === 1) return '成功'
  if (result === 0) return '失败'
  return '-'
}

const getOperationResultTag = (result?: number | null) => {
  if (result === 1) return 'success'
  if (result === 0) return 'danger'
  return 'info'
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detailData.value = {}
    detailLoading.value = false
    detailErrorMessage.value = ''
  }
})
</script>

<style scoped>
.audit-log-view {
  min-width: 0;
}

.audit-log-quick-search-tag {
  margin-top: 0.72rem;
}

.audit-log-quick-search-tag__chip {
  margin: 0;
}

.audit-log-notice-grid {
  display: grid;
  gap: 0.72rem;
}

</style>
