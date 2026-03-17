<template>
  <div class="audit-log-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>审计日志</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="操作用户">
              <el-input
                v-model="searchForm.userName"
                placeholder="请输入操作用户"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
            <el-col :span="8">
              <el-form-item label="操作类型">
                <el-select v-model="searchForm.operationType" placeholder="请选择操作类型" clearable>
                  <el-option label="新增" value="insert" />
                  <el-option label="修改" value="update" />
                  <el-option label="删除" value="delete" />
                  <el-option label="查询" value="select" />
                  <el-option label="系统异常" value="system_error" />
                </el-select>
              </el-form-item>
            </el-col>
          <el-col :span="8">
            <el-form-item label="操作模块">
              <el-input
                v-model="searchForm.operationModule"
                placeholder="请输入操作模块"
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

      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">已选 {{ selectedRows.length }} 项</span>
        </div>
        <div class="table-action-bar__right">
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
        <el-table-column prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)">
              {{ getOperationTypeName(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationModule" label="操作模块" width="150" />
        <el-table-column prop="operationMethod" label="操作方法" />
        <el-table-column prop="requestUrl" label="请求URL/目标" />
        <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
        <el-table-column prop="userName" label="操作用户" width="120" />
        <el-table-column prop="ipAddress" label="操作IP" width="150" />
        <el-table-column prop="operationTime" label="操作时间" width="180" />
        <el-table-column prop="operationResult" label="操作结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.operationResult === 1 ? 'success' : 'danger'">
              {{ row.operationResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
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

      <!-- 详情对话框 -->
      <el-dialog
        v-model="detailVisible"
        title="审计日志详情"
        width="800px"
      >
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="租户ID">{{ detailData.tenantId }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ getOperationTypeName(detailData.operationType) }}</el-descriptions-item>
          <el-descriptions-item label="操作模块">{{ detailData.operationModule }}</el-descriptions-item>
          <el-descriptions-item label="操作方法">{{ detailData.operationMethod }}</el-descriptions-item>
          <el-descriptions-item label="请求URL/目标">{{ detailData.requestUrl }}</el-descriptions-item>
          <el-descriptions-item label="请求方法/通道">{{ detailData.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="操作用户">{{ detailData.userName }}</el-descriptions-item>
          <el-descriptions-item label="操作IP">{{ detailData.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatDate(detailData.operationTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作结果" :span="2">
            <el-tag v-if="detailData.operationResult !== undefined && detailData.operationResult !== null" :type="getOperationResultTag(detailData.operationResult)">
              {{ getOperationResultName(detailData.operationResult) }}
            </el-tag>
            <el-text v-else>-</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="请求参数" :span="2">
            <el-text v-if="detailData.requestParams" wrap class="detail-payload">{{ formatDetailPayload(detailData.requestParams) }}</el-text>
            <el-text v-else wrap>-</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="响应结果" :span="2">
            <el-text v-if="detailData.responseResult" wrap class="detail-payload">{{ formatDetailPayload(detailData.responseResult) }}</el-text>
            <el-text v-else wrap>-</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="结果消息" :span="2">
            <el-text v-if="detailData.resultMessage" wrap>{{ detailData.resultMessage }}</el-text>
            <el-text v-else wrap>-</el-text>
          </el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="detailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="审计日志导出列设置"
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageLogs, getAuditLogById, deleteAuditLog, type AuditLogRecord } from '@/api/auditLog'
import type { RequestError } from '@/api/request'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'

// 搜索表单
const searchForm = reactive({
  userName: '',
  operationType: undefined,
  operationModule: ''
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref<any[]>([])
const tableRef = ref()
const selectedRows = ref<any[]>([])
const exportColumns: CsvColumn<any>[] = [
  { key: 'operationType', label: '操作类型', formatter: (value) => getOperationTypeName(String(value || '')) },
  { key: 'operationModule', label: '操作模块' },
  { key: 'operationMethod', label: '操作方法' },
  { key: 'requestUrl', label: '请求URL' },
  { key: 'requestMethod', label: '请求方法' },
  { key: 'userName', label: '操作用户' },
  { key: 'ipAddress', label: '操作IP' },
  { key: 'operationTime', label: '操作时间' },
  { key: 'operationResult', label: '操作结果', formatter: (value) => (Number(value) === 1 ? '成功' : '失败') }
]
const exportColumnStorageKey = 'audit-log-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  {
    label: '运维模板',
    keys: ['operationType', 'operationModule', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult']
  },
  { label: '管理模板', keys: ['operationType', 'operationModule', 'operationMethod', 'userName', 'operationTime', 'operationResult'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const exportColumnDialogVisible = ref(false)

// 加载状态
const loading = ref(false)

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<Partial<AuditLogRecord>>({})

// 获取审计日志列表
const getAuditLogList = async () => {
  loading.value = true
  try {
    const res = await pageLogs({
      userName: searchForm.userName,
      operationType: searchForm.operationType,
      operationModule: searchForm.operationModule,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      pagination.total = Number(res.data?.total || 0)
    }
  } catch (error) {
    console.error('获取审计日志列表失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  getAuditLogList()
})

// 处理搜索
const handleSearch = () => {
  pagination.pageNum = 1
  getAuditLogList()
}

// 重置搜索
const handleReset = () => {
  searchForm.userName = ''
  searchForm.operationType = undefined
  searchForm.operationModule = ''
  getAuditLogList()
}

const handleSelectionChange = (rows: any[]) => {
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

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true
}

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys)
}

const getResolvedExportColumns = () => resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)

const handleExportSelected = () => {
  downloadRowsAsCsv('审计日志-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('审计日志-当前结果.csv', tableData.value, getResolvedExportColumns())
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  getAuditLogList()
}

// 当前页变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  getAuditLogList()
}

// 查看详情
const handleDetail = async (row: AuditLogRecord) => {
  if (row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('当前审计日志缺少主键，无法查看详情')
    return
  }

  try {
    const res = await getAuditLogById(row.id)
    if (!res.data || Array.isArray(res.data)) {
      ElMessage.warning('审计日志详情不存在或已删除')
      return
    }
    detailData.value = { ...row, ...res.data }
    detailVisible.value = true
  } catch (error) {
    const requestError = error as RequestError | undefined
    if (!requestError?.handled) {
      ElMessage.error('获取审计日志详情失败')
    }
    console.error('获取审计日志详情失败', error)
  }
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该审计日志吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteAuditLog(row.id)
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

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const formatDetailPayload = (payload?: string) => {
  if (!payload) return '-'
  const text = String(payload)
  const trimmed = text.trim()
  if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2)
    } catch {
      return text
    }
  }
  return text
}
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

.search-form {
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

.detail-payload {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
}
</style>
