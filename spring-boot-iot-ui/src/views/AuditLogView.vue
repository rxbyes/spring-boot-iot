<template>
  <div class="audit-log-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>{{ pageTitle }}</span>
            <p class="page-description">{{ pageDescription }}</p>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col v-if="isBusinessMode" :span="8">
            <el-form-item label="操作用户">
              <el-input
                v-model="searchForm.userName"
                placeholder="请输入操作用户"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="isBusinessMode" :span="8">
            <el-form-item label="操作类型">
              <el-select v-model="searchForm.operationType" placeholder="请选择操作类型" clearable>
                <el-option
                  v-for="item in businessOperationTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="isSystemMode ? '异常模块' : '操作模块'">
              <el-input
                v-model="searchForm.operationModule"
                :placeholder="isSystemMode ? '请输入异常模块' : '请输入操作模块'"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="isSystemMode" :span="8">
            <el-form-item label="请求通道">
              <el-select v-model="searchForm.requestMethod" placeholder="请选择请求通道" clearable>
                <el-option
                  v-for="item in systemRequestMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="isSystemMode" :span="8">
            <el-form-item label="目标 / URL">
              <el-input
                v-model="searchForm.requestUrl"
                placeholder="请输入 topic、生命周期目标或请求地址"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="TraceId">
              <el-input
                v-model="searchForm.traceId"
                placeholder="请输入 TraceId"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="isSystemMode" :span="8">
            <el-form-item label="设备编码">
              <el-input
                v-model="searchForm.deviceCode"
                placeholder="请输入设备编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="isSystemMode" :span="8">
            <el-form-item label="产品标识">
              <el-input
                v-model="searchForm.productKey"
                placeholder="请输入产品标识"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="isSystemMode" :gutter="20">
          <el-col :span="8">
            <el-form-item label="异常编码">
              <el-input
                v-model="searchForm.errorCode"
                placeholder="请输入异常编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="异常类型">
              <el-input
                v-model="searchForm.exceptionClass"
                placeholder="请输入异常类名"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" class="text-right">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </el-col>
        </el-row>
      </el-form>

      <el-alert
        :title="viewTip"
        type="info"
        :closable="false"
        show-icon
        class="view-alert"
      />

      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">已选 {{ selectedRows.length }} 项</span>
        </div>
        <div class="table-action-bar__right">
          <el-button v-if="isSystemMode" link :disabled="!canJumpFromSearch" @click="handleJumpToMessageTrace()">
            消息追踪
          </el-button>
          <el-button link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
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
        <el-table-column prop="operationModule" label="操作模块" width="150" />
        <el-table-column prop="operationMethod" label="操作方法" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="请求URL/目标" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
        <el-table-column v-if="isSystemMode" prop="traceId" label="TraceId" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="isSystemMode" prop="deviceCode" label="设备编码" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="isSystemMode" prop="productKey" label="产品标识" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="isSystemMode" prop="errorCode" label="异常编码" min-width="120" show-overflow-tooltip />
        <el-table-column v-if="isSystemMode" prop="exceptionClass" label="异常类型" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="isBusinessMode" prop="userName" label="操作用户" width="120" />
        <el-table-column v-if="isBusinessMode" prop="ipAddress" label="操作IP" width="150" />
        <el-table-column v-if="isSystemMode" prop="resultMessage" label="异常摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="operationTime" label="操作时间" width="180" />
        <el-table-column prop="operationResult" label="操作结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.operationResult === 1 ? 'success' : 'danger'">
              {{ row.operationResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isSystemMode ? 210 : 150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="isSystemMode"
              type="primary"
              link
              :disabled="!canJumpToMessageTrace(row)"
              @click="handleJumpToMessageTrace(row)"
            >
              追踪
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />

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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageLogs, getAuditLogById, deleteAuditLog, type AuditLogRecord } from '@/api/auditLog'
import type { RequestError } from '@/api/request'
import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'

type AuditLogViewMode = 'business' | 'system'

const route = useRoute()
const router = useRouter()
const viewMode = computed<AuditLogViewMode>(() => (route.path === '/system-log' ? 'system' : 'business'))
const isSystemMode = computed(() => viewMode.value === 'system')
const isBusinessMode = computed(() => viewMode.value === 'business')
const pageTitle = computed(() => (isSystemMode.value ? '系统日志' : '业务日志'))
const pageDescription = computed(() =>
  isSystemMode.value
    ? '面向研发、测试与运维的系统异常排查台，聚焦设备接入与后台链路问题。'
    : '面向客户与治理侧的业务操作留痕，默认不展示后台系统异常记录。'
)
const viewTip = computed(() =>
  isSystemMode.value
    ? '系统日志仅展示 `sys_audit_log` 中 `operation_type=system_error` 的记录，可结合 TraceId、设备编码与“消息追踪”页面快速串联排障。'
    : '业务日志默认排除 `system_error` 记录；如需排查设备接入或后台异常，请前往“设备接入 > 系统日志”。'
)
const detailDialogTitle = computed(() => `${pageTitle.value}详情`)
const canJumpFromSearch = computed(() =>
  Boolean(searchForm.traceId || searchForm.deviceCode || searchForm.productKey || (searchForm.requestMethod === 'MQTT' && searchForm.requestUrl))
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
  exceptionClass: ''
})

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

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<Partial<AuditLogRecord>>({})
const detailLoading = ref(false)
const detailErrorMessage = ref('')

const defaultExportKeys = exportColumns.map((column) => String(column.key))

const reloadExportSelection = () => {
  selectedExportColumnKeys.value = loadCsvColumnSelection(exportColumnStorageKey.value, defaultExportKeys)
}

const resetSearchForm = () => {
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
}

const readRouteQueryValue = (key: string) => {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
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
}

// 获取审计日志列表
const getAuditLogList = async () => {
  loading.value = true
  try {
    const res = await pageLogs({
      traceId: searchForm.traceId,
      operationModule: searchForm.operationModule,
      ...(isBusinessMode.value
        ? {
            userName: searchForm.userName,
            operationType: searchForm.operationType,
            excludeSystemError: true
          }
        : {
            operationType: 'system_error',
            deviceCode: searchForm.deviceCode,
            productKey: searchForm.productKey,
            requestMethod: searchForm.requestMethod,
            requestUrl: searchForm.requestUrl,
            errorCode: searchForm.errorCode,
            exceptionClass: searchForm.exceptionClass
          }),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取审计日志列表失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  reloadExportSelection()
  applySystemRouteQuery()
  getAuditLogList()
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
  getAuditLogList()
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
    route.query.exceptionClass
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
    getAuditLogList()
  }
)

// 处理搜索
const handleSearch = () => {
  resetPage()
  clearSelection()
  getAuditLogList()
}

// 重置搜索
const handleReset = () => {
  resetSearchForm()
  resetPage()
  clearSelection()
  getAuditLogList()
}

const handleSelectionChange = (rows: AuditLogRecord[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  getAuditLogList()
}

const canJumpToMessageTrace = (row?: AuditLogRecord) => {
  const target = row || searchForm
  const requestMethod = 'requestMethod' in target ? target.requestMethod : undefined
  const requestUrl = 'requestUrl' in target ? target.requestUrl : undefined
  return Boolean(target.traceId || target.deviceCode || target.productKey || (requestMethod === 'MQTT' && requestUrl))
}

const handleJumpToMessageTrace = (row?: AuditLogRecord) => {
  const target = row || searchForm
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
    const requestError = error as RequestError | undefined
    if (!requestError?.handled) {
      ElMessage.error(`获取${pageTitle.value}详情失败`)
    }
    detailErrorMessage.value = error instanceof Error ? error.message : `获取${pageTitle.value}详情失败`
    console.error('获取日志详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// 删除
const handleDelete = (row: AuditLogRecord) => {
  ElMessageBox.confirm(`确定要删除该${pageTitle.value}吗？`, '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteAuditLog(String(row.id))
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getAuditLogList()
        }
      } catch (error) {
        console.error('删除失败', error)
      }
    })
    .catch(() => {})
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
  padding: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-description {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.search-form {
  margin-bottom: 12px;
}

.view-alert {
  margin-bottom: 12px;
}

.text-right {
  text-align: right;
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

</style>
