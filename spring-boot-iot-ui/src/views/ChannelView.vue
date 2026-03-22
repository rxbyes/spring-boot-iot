<template>
  <div class="channel-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="通知编排"
      description="统一维护通知渠道配置、启停和测试链路，支持同页治理导出与渠道验证。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" :icon="Plus" @click="handleAdd">新增</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <el-form-item>
              <el-input v-model="searchForm.channelName" placeholder="渠道名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="searchForm.channelCode" placeholder="渠道编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.channelType" placeholder="渠道类型" clearable>
                <el-option v-for="item in CHANNEL_TYPES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `已选 ${selectedRows.length} 项` ]">
          <template #right>
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
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="channelCode" label="渠道编码" :width="150" />
        <StandardTableTextColumn prop="channelName" label="渠道名称" :width="200" />
        <el-table-column prop="channelType" label="渠道类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ getChannelTypeName(row.channelType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="sortNo" label="排序" :width="80" />
        <StandardTableTextColumn prop="remark" label="备注" :min-width="180" />
        <el-table-column label="操作" width="260" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide">
              <StandardActionLink @click="handleEdit(row)">编辑</StandardActionLink>
              <StandardActionLink :disabled="!isTestableChannel(row.channelType)" @click="handleTest(row)">测试通知</StandardActionLink>
              <StandardActionLink @click="handleDelete(row)">删除</StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </template>

      <StandardFormDrawer
        v-model="dialogVisible"
        eyebrow="System Form"
        :title="dialogTitle"
        subtitle="统一通过右侧抽屉维护通知编排与配置 JSON。"
        size="44rem"
        @close="handleDialogClose"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
          <el-form-item label="渠道名称" prop="channelName">
            <el-input v-model="formData.channelName" placeholder="请输入渠道名称" />
          </el-form-item>
          <el-form-item label="渠道编码" prop="channelCode">
            <el-input v-model="formData.channelCode" placeholder="请输入渠道编码" />
          </el-form-item>
          <el-form-item label="渠道类型" prop="channelType">
            <el-select v-model="formData.channelType" placeholder="请选择渠道类型">
              <el-option v-for="item in CHANNEL_TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="配置 JSON" prop="config">
            <el-input v-model="formData.config" type="textarea" :rows="6" :placeholder="configPlaceholder" />
            <div class="form-tip">
              webhook / 微信 / 飞书 / 钉钉 建议配置 `url`，后台异常需在 `scenes` 中包含 `system_error`，高优未读桥接需包含 `in_app_unread_bridge`。
            </div>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="排序" prop="sortNo">
            <el-input-number v-model="formData.sortNo" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </el-form-item>
        </el-form>
        <template #footer>
          <StandardDrawerFooter
            :confirm-loading="submitLoading"
            @cancel="dialogVisible = false"
            @confirm="handleSubmit"
          />
        </template>
      </StandardFormDrawer>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="通知编排导出列设置"
        :options="exportColumnOptions"
        :selected-keys="selectedExportColumnKeys"
        :preset-storage-key="exportColumnStorageKey"
        :presets="exportPresets"
        @confirm="handleExportColumnConfirm"
      />
    </StandardWorkbenchPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { useServerPagination } from '@/composables/useServerPagination'
import {
  CHANNEL_TYPES,
  addChannel,
  deleteChannel,
  getChannelByCode,
  pageChannels,
  testChannel,
  updateChannel,
  type ChannelRecord
} from '@/api/channel'

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增通知编排')
const tableData = ref<ChannelRecord[]>([])
const selectedRows = ref<ChannelRecord[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination()

const searchForm = reactive({
  channelName: '',
  channelCode: '',
  channelType: undefined as string | undefined
})
const appliedFilters = reactive({
  channelName: '',
  channelCode: '',
  channelType: undefined as string | undefined
})

const formData = ref<Partial<ChannelRecord>>({
  id: undefined,
  channelName: '',
  channelCode: '',
  channelType: 'email',
  config: '',
  status: 1,
  sortNo: 0,
  remark: ''
})

const TESTABLE_CHANNEL_TYPES = ['webhook', 'wechat', 'feishu', 'dingtalk']

const isTestableChannel = (channelType?: string) =>
  TESTABLE_CHANNEL_TYPES.includes(String(channelType || '').trim().toLowerCase())

const validateConfig = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const content = String(value || '').trim()
  if (!content) {
    if (isTestableChannel(formData.value.channelType)) {
      callback(new Error('当前渠道类型必须填写配置 JSON'))
      return
    }
    callback()
    return
  }

  try {
    const parsed = JSON.parse(content)
    if (isTestableChannel(formData.value.channelType) && !String(parsed?.url || '').trim()) {
      callback(new Error('当前渠道类型配置中必须包含 url'))
      return
    }
    callback()
  } catch {
    callback(new Error('配置 JSON 格式不正确'))
  }
}

const formRules = {
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  config: [{ validator: validateConfig, trigger: 'blur' }]
}

const configPlaceholders: Record<string, string> = {
  webhook: '{\n  "url": "https://example.com/iot/webhook",\n  "headers": {\n    "Authorization": "Bearer demo-token"\n  },\n  "scenes": ["system_error", "in_app_unread_bridge"],\n  "timeoutMs": 3000,\n  "minIntervalSeconds": 300\n}',
  dingtalk: '{\n  "url": "https://oapi.dingtalk.com/robot/send?access_token=xxx",\n  "scenes": ["system_error", "in_app_unread_bridge"],\n  "timeoutMs": 3000,\n  "minIntervalSeconds": 300\n}',
  wechat: '{\n  "url": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx",\n  "scenes": ["system_error", "in_app_unread_bridge"],\n  "timeoutMs": 3000,\n  "minIntervalSeconds": 300\n}',
  feishu: '{\n  "url": "https://open.feishu.cn/open-apis/bot/v2/hook/xxx",\n  "scenes": ["system_error", "in_app_unread_bridge"],\n  "timeoutMs": 3000,\n  "minIntervalSeconds": 300\n}',
  email: '{\n  "host": "smtp.example.com",\n  "port": 465,\n  "from": "iot-alert@example.com"\n}',
  sms: '{\n  "provider": "demo",\n  "signName": "spring-boot-iot"\n}'
}

const configPlaceholder = computed(
  () => configPlaceholders[String(formData.value.channelType || '').trim()] || '{\n  "url": "https://example.com/iot/webhook"\n}'
)

const exportColumns: CsvColumn<ChannelRecord>[] = [
  { key: 'channelCode', label: '渠道编码' },
  { key: 'channelName', label: '渠道名称' },
  { key: 'channelType', label: '渠道类型', formatter: (value) => getChannelTypeName(String(value || '')) },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'sortNo', label: '排序' },
  { key: 'remark', label: '备注' }
]
const exportColumnStorageKey = 'channel-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['channelCode', 'channelName', 'channelType', 'status'] },
  { label: '管理模板', keys: ['channelCode', 'channelName', 'channelType', 'status', 'sortNo', 'remark'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const exportColumnDialogVisible = ref(false)
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'channelName', label: '渠道名称' },
    { key: 'channelCode', label: '渠道编码' },
    { key: 'channelType', label: (value) => `渠道类型：${getChannelTypeName(String(value || ''))}`, clearValue: undefined, isActive: (value) => Boolean(value) }
  ],
  defaults: {
    channelName: '',
    channelCode: '',
    channelType: undefined
  }
})

const loadChannelPage = async () => {
  loading.value = true
  try {
    const res = await pageChannels({
      channelName: appliedFilters.channelName || undefined,
      channelCode: appliedFilters.channelCode || undefined,
      channelType: appliedFilters.channelType || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取通知渠道分页失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  syncAppliedFilters()
  loadChannelPage()
})

const handleSearch = () => {
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadChannelPage()
}

const handleReset = () => {
  searchForm.channelName = ''
  searchForm.channelCode = ''
  searchForm.channelType = undefined
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadChannelPage()
}

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key)
  resetPage()
  clearSelection()
  loadChannelPage()
}

const handleClearAppliedFilters = () => {
  handleReset()
}

const handleSelectionChange = (rows: ChannelRecord[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  loadChannelPage()
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
  downloadRowsAsCsv('通知编排-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('通知编排-当前结果.csv', tableData.value, getResolvedExportColumns())
}

const resetFormData = (channel?: Partial<ChannelRecord>) => {
  formData.value = {
    id: channel?.id,
    channelName: channel?.channelName || '',
    channelCode: channel?.channelCode || '',
    channelType: channel?.channelType || 'email',
    config: channel?.config || '',
    status: channel?.status ?? 1,
    sortNo: channel?.sortNo ?? 0,
    remark: channel?.remark || ''
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增通知编排'
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = async (row: ChannelRecord) => {
  dialogTitle.value = '编辑通知编排'
  const res = await getChannelByCode(row.channelCode)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
    dialogVisible.value = true
  }
}

const handleDelete = async (row: ChannelRecord) => {
  try {
    await confirmDelete('渠道', row.channelName)
    await deleteChannel(row.id)
    ElMessage.success('删除成功')
    loadChannelPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除通知渠道失败', error)
  }
}

const handleTest = async (row: ChannelRecord) => {
  if (!isTestableChannel(row.channelType)) {
    ElMessage.warning('测试通知仅支持 Webhook / 微信 / 飞书 / 钉钉渠道')
    return
  }
  try {
    await testChannel(row.channelCode)
    ElMessage.success('测试通知已发送')
  } catch (error) {
    console.error('测试通知失败', error)
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (formData.value.id) {
      await updateChannel(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addChannel(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadChannelPage()
  } catch (error) {
    console.error('提交通知渠道失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const getChannelTypeName = (type: string) => {
  const matched = CHANNEL_TYPES.find((item) => item.value === type)
  return matched?.label || type
}

const handleSizeChange = (size: number) => {
  setPageSize(size)
  loadChannelPage()
}

const handlePageChange = (page: number) => {
  setPageNum(page)
  loadChannelPage()
}
</script>

<style scoped>
.form-tip {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
