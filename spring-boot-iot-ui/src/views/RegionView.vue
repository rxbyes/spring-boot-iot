<template>
  <div class="region-view sys-mgmt-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>区域管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="区域名称">
              <el-input v-model="searchForm.regionName" placeholder="请输入区域名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区域编码">
              <el-input v-model="searchForm.regionCode" placeholder="请输入区域编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区域类型">
              <el-select v-model="searchForm.regionType" placeholder="请选择区域类型" clearable>
                <el-option label="省份" value="province" />
                <el-option label="城市" value="city" />
                <el-option label="区县" value="district" />
                <el-option label="街道" value="street" />
              </el-select>
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
        v-if="!isFilterMode"
        title="默认仅分页加载根区域，展开行时按需加载子区域。"
        type="info"
        :closable="false"
        show-icon
        class="view-alert"
      />
      <el-alert
        v-else
        title="搜索模式返回扁平分页结果，不再加载整棵区域树。"
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
          <el-button link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        row-key="id"
        :lazy="!isFilterMode"
        :load="loadChildren"
        :tree-props="treeProps"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="regionCode" label="区域编码" width="150" />
        <el-table-column prop="regionName" label="区域名称" width="200" />
        <el-table-column prop="regionType" label="区域类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getRegionTypeTag(row.regionType)">
              {{ getRegionTypeName(row.regionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="longitude" label="经度" width="120" />
        <el-table-column prop="latitude" label="纬度" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleAddSub(row)">新增子级</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />

      <StandardFormDrawer
        v-model="dialogVisible"
        eyebrow="System Form"
        :title="dialogTitle"
        subtitle="统一通过右侧抽屉维护区域层级与坐标信息。"
        size="42rem"
        @close="handleDialogClose"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
          <el-form-item label="区域名称" prop="regionName">
            <el-input v-model="formData.regionName" placeholder="请输入区域名称" />
          </el-form-item>
          <el-form-item label="区域编码" prop="regionCode">
            <el-input v-model="formData.regionCode" placeholder="请输入区域编码" />
          </el-form-item>
          <el-form-item label="区域类型" prop="regionType">
            <el-select v-model="formData.regionType" placeholder="请选择区域类型">
              <el-option label="省份" value="province" />
              <el-option label="城市" value="city" />
              <el-option label="区县" value="district" />
              <el-option label="街道" value="street" />
            </el-select>
          </el-form-item>
          <el-form-item label="经度" prop="longitude">
            <el-input-number v-model="formData.longitude" :min="-180" :max="180" :step="0.000001" />
          </el-form-item>
          <el-form-item label="纬度" prop="latitude">
            <el-input-number v-model="formData.latitude" :min="-90" :max="90" :step="0.000001" />
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
          <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </template>
      </StandardFormDrawer>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="区域管理导出列设置"
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import {
  addRegion,
  deleteRegion,
  getRegion,
  listRegions,
  pageRegions,
  updateRegion,
  type Region
} from '@/api/region'

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增区域')
const tableData = ref<Region[]>([])
const selectedRows = ref<Region[]>([])

const searchForm = reactive({
  regionName: '',
  regionCode: '',
  regionType: undefined as string | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formData = ref<Partial<Region>>({
  id: undefined,
  parentId: 0,
  regionName: '',
  regionCode: '',
  regionType: 'province',
  longitude: undefined,
  latitude: undefined,
  status: 1,
  sortNo: 0,
  remark: ''
})

const formRules = {
  regionName: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
  regionCode: [{ required: true, message: '请输入区域编码', trigger: 'blur' }],
  regionType: [{ required: true, message: '请选择区域类型', trigger: 'change' }]
}

const exportColumns: CsvColumn<Region>[] = [
  { key: 'regionCode', label: '区域编码' },
  { key: 'regionName', label: '区域名称' },
  { key: 'regionType', label: '区域类型', formatter: (value) => getRegionTypeName(String(value || '')) },
  { key: 'longitude', label: '经度' },
  { key: 'latitude', label: '纬度' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'sortNo', label: '排序' },
  { key: 'remark', label: '备注' }
]
const exportColumnStorageKey = 'region-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['regionCode', 'regionName', 'regionType', 'longitude', 'latitude', 'status'] },
  { label: '管理模板', keys: ['regionCode', 'regionName', 'regionType', 'status', 'sortNo', 'remark'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const exportColumnDialogVisible = ref(false)

const isFilterMode = computed(
  () => Boolean(searchForm.regionName.trim() || searchForm.regionCode.trim() || searchForm.regionType)
)

const treeProps = {
  children: 'children',
  hasChildren: 'hasChildren'
}

const loadRegionPage = async () => {
  loading.value = true
  try {
    const res = await pageRegions({
      regionName: searchForm.regionName || undefined,
      regionCode: searchForm.regionCode || undefined,
      regionType: searchForm.regionType || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取区域分页失败', error)
  } finally {
    loading.value = false
  }
}

const loadChildren = async (row: Region, _treeNode: unknown, resolve: (data: Region[]) => void) => {
  try {
    const res = await listRegions(row.id)
    const children = res.data || []
    row.children = children
    row.hasChildren = children.length > 0
    resolve(children)
  } catch (error) {
    console.error('加载区域子节点失败', error)
    resolve([])
  }
}

onMounted(() => {
  loadRegionPage()
})

const handleSearch = () => {
  pagination.pageNum = 1
  clearSelection()
  loadRegionPage()
}

const handleReset = () => {
  searchForm.regionName = ''
  searchForm.regionCode = ''
  searchForm.regionType = undefined
  pagination.pageNum = 1
  clearSelection()
  loadRegionPage()
}

const handleSelectionChange = (rows: Region[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  loadRegionPage()
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
  downloadRowsAsCsv('区域管理-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const flattenTreeRows = (rows: Region[]): Region[] =>
  rows.flatMap((row) => [row, ...(Array.isArray(row.children) ? flattenTreeRows(row.children) : [])])

const handleExportCurrent = () => {
  const rows = isFilterMode.value ? tableData.value : flattenTreeRows(tableData.value)
  downloadRowsAsCsv('区域管理-当前结果.csv', rows, getResolvedExportColumns())
}

const resetFormData = (region?: Partial<Region>) => {
  formData.value = {
    id: region?.id,
    parentId: region?.parentId ?? 0,
    regionName: region?.regionName || '',
    regionCode: region?.regionCode || '',
    regionType: region?.regionType || 'province',
    longitude: region?.longitude,
    latitude: region?.latitude,
    status: region?.status ?? 1,
    sortNo: region?.sortNo ?? 0,
    remark: region?.remark || ''
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增区域'
  resetFormData()
  dialogVisible.value = true
}

const handleAddSub = (row: Region) => {
  dialogTitle.value = '新增子级'
  resetFormData({ parentId: row.id })
  dialogVisible.value = true
}

const handleEdit = async (row: Region) => {
  dialogTitle.value = '编辑区域'
  const res = await getRegion(row.id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
    dialogVisible.value = true
  }
}

const handleDelete = (row: Region) => {
  ElMessageBox.confirm(`确定要删除区域“${row.regionName}”吗？`, '警告', { type: 'warning' })
    .then(async () => {
      await deleteRegion(row.id)
      ElMessage.success('删除成功')
      loadRegionPage()
    })
    .catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (formData.value.id) {
      await updateRegion(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addRegion(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadRegionPage()
  } catch (error) {
    console.error('提交区域失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const getRegionTypeName = (type: string) => {
  const map: Record<string, string> = {
    province: '省份',
    city: '城市',
    district: '区县',
    street: '街道'
  }
  return map[type] || type
}

const getRegionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    province: 'primary',
    city: 'warning',
    district: 'info',
    street: 'success'
  }
  return map[type] || 'info'
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadRegionPage()
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadRegionPage()
}
</script>

<style scoped>
.region-view {
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
