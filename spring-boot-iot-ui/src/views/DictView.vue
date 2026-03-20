<template>
  <div class="dict-view sys-mgmt-view standard-list-view">
    <PanelCard class="box-card">
      <template #header>
        <div class="card-header">
          <span>数据字典</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="字典名称">
              <el-input v-model="searchForm.dictName" placeholder="请输入字典名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="字典编码">
              <el-input v-model="searchForm.dictCode" placeholder="请输入字典编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="字典类型">
              <el-select v-model="searchForm.dictType" placeholder="请选择字典类型" clearable>
                <el-option label="文本" value="text" />
                <el-option label="数字" value="number" />
                <el-option label="布尔" value="boolean" />
                <el-option label="日期" value="date" />
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

      <StandardTableToolbar :meta-items="[ `已选 ${selectedRows.length} 项` ]">
        <template #right>
          <el-button link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

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
        <StandardTableTextColumn prop="dictCode" label="字典编码" :width="150" />
        <StandardTableTextColumn prop="dictName" label="字典名称" :width="200" />
        <el-table-column prop="dictType" label="字典类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDictTypeTag(row.dictType)">
              {{ getDictTypeName(row.dictType) }}
            </el-tag>
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
        <el-table-column label="操作" width="200" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleItems(row)">字典项</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

      <StandardFormDrawer
        v-model="dialogVisible"
        eyebrow="System Form"
        :title="dialogTitle"
        subtitle="统一通过右侧抽屉维护字典分类主数据。"
        size="42rem"
        @close="handleDialogClose"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
          <el-form-item label="字典名称" prop="dictName">
            <el-input v-model="formData.dictName" placeholder="请输入字典名称" />
          </el-form-item>
          <el-form-item label="字典编码" prop="dictCode">
            <el-input v-model="formData.dictCode" placeholder="请输入字典编码" />
          </el-form-item>
          <el-form-item label="字典类型" prop="dictType">
            <el-select v-model="formData.dictType" placeholder="请选择字典类型">
              <el-option label="文本" value="text" />
              <el-option label="数字" value="number" />
              <el-option label="布尔" value="boolean" />
              <el-option label="日期" value="date" />
            </el-select>
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

      <StandardFormDrawer
        v-model="itemsDialogVisible"
        eyebrow="Dictionary Items"
        :title="currentDictName ? `${currentDictName} 字典项` : '字典项管理'"
        subtitle="统一通过右侧抽屉查看和维护字典项明细，支持新增、编辑、导出与刷新。"
        size="58rem"
        @close="handleItemsDialogClose"
      >
        <StandardTableToolbar :meta-items="[ `已选 ${selectedItemRows.length} 项` ]">
          <template #right>
            <el-button link @click="handleAddItem">新增字典项</el-button>
            <el-button link @click="openItemExportColumnSetting">导出列设置</el-button>
            <el-button link :disabled="selectedItemRows.length === 0" @click="handleExportSelectedItems">导出选中</el-button>
            <el-button link :disabled="itemsTableData.length === 0" @click="handleExportCurrentItems">导出当前结果</el-button>
            <el-button link :disabled="selectedItemRows.length === 0" @click="clearItemSelection">清空选中</el-button>
            <el-button link @click="handleRefreshItems">刷新列表</el-button>
          </template>
        </StandardTableToolbar>
        <el-table
          ref="itemsTableRef"
          v-loading="itemsLoading"
          :data="itemsTableData"
          border
          stripe
          style="width: 100%"
          @selection-change="handleItemSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="itemName" label="项名称" :width="150" />
          <StandardTableTextColumn prop="itemValue" label="项值" :width="150" />
          <el-table-column prop="itemType" label="项类型" width="120">
            <template #default="{ row }">
              <el-tag>{{ row.itemType || 'string' }}</el-tag>
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
          <el-table-column label="操作" width="200" fixed="right" :show-overflow-tooltip="false">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEditItem(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <template #footer>
          <StandardDrawerFooter
            :show-cancel="false"
            confirm-text="关闭"
            @confirm="handleItemsDialogClose"
          />
        </template>
      </StandardFormDrawer>

      <StandardFormDrawer
        v-model="itemDialogVisible"
        eyebrow="Dictionary Item"
        :title="itemDialogTitle"
        :subtitle="currentDictName ? `所属字典：${currentDictName}` : '统一通过右侧抽屉维护字典项信息。'"
        size="36rem"
        @close="handleItemDialogClose"
      >
        <el-form ref="itemFormRef" :model="itemFormData" :rules="itemFormRules" label-width="100px">
          <el-form-item label="项名称" prop="itemName">
            <el-input v-model="itemFormData.itemName" placeholder="请输入字典项名称" />
          </el-form-item>
          <el-form-item label="项值" prop="itemValue">
            <el-input v-model="itemFormData.itemValue" placeholder="请输入字典项值" />
          </el-form-item>
        </el-form>
        <template #footer>
          <StandardDrawerFooter
            :confirm-loading="itemSubmitLoading"
            @cancel="handleItemDialogClose"
            @confirm="handleItemSubmit"
          />
        </template>
      </StandardFormDrawer>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="数据字典导出列设置"
        :options="exportColumnOptions"
        :selected-keys="selectedExportColumnKeys"
        :preset-storage-key="exportColumnStorageKey"
        :presets="exportPresets"
        @confirm="handleExportColumnConfirm"
      />

      <CsvColumnSettingDialog
        v-model="itemExportColumnDialogVisible"
        title="字典项导出列设置"
        :options="itemExportColumnOptions"
        :selected-keys="selectedItemExportColumnKeys"
        :preset-storage-key="itemExportColumnStorageKey"
        :presets="itemExportPresets"
        @confirm="handleItemExportColumnConfirm"
      />
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
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
  addDict,
  addDictItem,
  deleteDict,
  deleteDictItem,
  getDict,
  listDictItems,
  pageDicts,
  updateDict,
  updateDictItem,
  type Dict,
  type DictItem
} from '@/api/dict'
import type { IdType } from '@/types/api'

const formRef = ref()
const itemFormRef = ref()
const tableRef = ref()
const itemsTableRef = ref()

const loading = ref(false)
const itemsLoading = ref(false)
const submitLoading = ref(false)
const itemSubmitLoading = ref(false)
const dialogVisible = ref(false)
const itemsDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const dialogTitle = ref('新增字典')
const itemDialogTitle = ref('新增字典项')
const tableData = ref<Dict[]>([])
const itemsTableData = ref<DictItem[]>([])
const selectedRows = ref<Dict[]>([])
const selectedItemRows = ref<DictItem[]>([])
const currentDictId = ref<IdType>()
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination()
const currentDictName = ref('')

const searchForm = reactive({
  dictName: '',
  dictCode: '',
  dictType: undefined as string | undefined
})

const formData = ref<Partial<Dict>>({
  id: undefined,
  dictName: '',
  dictCode: '',
  dictType: 'text',
  status: 1,
  sortNo: 0,
  remark: ''
})

const itemFormData = ref<Partial<DictItem>>({
  id: undefined,
  dictId: undefined,
  itemName: '',
  itemValue: '',
  itemType: 'string',
  status: 1,
  sortNo: 1,
  remark: ''
})

const formRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictType: [{ required: true, message: '请选择字典类型', trigger: 'change' }]
}

const itemFormRules = {
  itemName: [{ required: true, message: '请输入字典项名称', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }]
}

const exportColumns: CsvColumn<Dict>[] = [
  { key: 'dictCode', label: '字典编码' },
  { key: 'dictName', label: '字典名称' },
  { key: 'dictType', label: '字典类型', formatter: (value) => getDictTypeName(String(value || '')) },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'sortNo', label: '排序' },
  { key: 'remark', label: '备注' }
]
const exportColumnStorageKey = 'dict-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['dictCode', 'dictName', 'dictType', 'status'] },
  { label: '管理模板', keys: ['dictCode', 'dictName', 'dictType', 'status', 'sortNo', 'remark'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const exportColumnDialogVisible = ref(false)

const itemExportColumns: CsvColumn<DictItem>[] = [
  { key: 'itemName', label: '项名称' },
  { key: 'itemValue', label: '项值' },
  { key: 'itemType', label: '项类型' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'sortNo', label: '排序' }
]
const itemExportColumnStorageKey = 'dict-item-view'
const itemExportColumnOptions = toCsvColumnOptions(itemExportColumns)
const itemExportPresets = [
  { label: '默认模板', keys: itemExportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['itemName', 'itemValue', 'itemType', 'status'] },
  { label: '管理模板', keys: ['itemName', 'itemValue', 'itemType', 'status', 'sortNo'] }
]
const selectedItemExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    itemExportColumnStorageKey,
    itemExportColumns.map((column) => String(column.key))
  )
)
const itemExportColumnDialogVisible = ref(false)

const loadDictPage = async () => {
  loading.value = true
  try {
    const res = await pageDicts({
      dictName: searchForm.dictName || undefined,
      dictCode: searchForm.dictCode || undefined,
      dictType: searchForm.dictType || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取字典分页失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDictPage()
})

const handleSearch = () => {
  resetPage()
  clearSelection()
  loadDictPage()
}

const handleReset = () => {
  searchForm.dictName = ''
  searchForm.dictCode = ''
  searchForm.dictType = undefined
  resetPage()
  clearSelection()
  loadDictPage()
}

const handleSelectionChange = (rows: Dict[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  loadDictPage()
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
  downloadRowsAsCsv('数据字典-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('数据字典-当前结果.csv', tableData.value, getResolvedExportColumns())
}

const resetFormData = (dict?: Partial<Dict>) => {
  formData.value = {
    id: dict?.id,
    dictName: dict?.dictName || '',
    dictCode: dict?.dictCode || '',
    dictType: dict?.dictType || 'text',
    status: dict?.status ?? 1,
    sortNo: dict?.sortNo ?? 0,
    remark: dict?.remark || ''
  }
}

const resetItemFormData = (item?: Partial<DictItem>) => {
  itemFormData.value = {
    id: item?.id,
    dictId: currentDictId.value,
    itemName: item?.itemName || '',
    itemValue: item?.itemValue || '',
    itemType: item?.itemType || 'string',
    status: item?.status ?? 1,
    sortNo: item?.sortNo ?? itemsTableData.value.length + 1,
    remark: item?.remark || ''
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增字典'
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = async (row: Dict) => {
  dialogTitle.value = '编辑字典'
  const res = await getDict(row.id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
    dialogVisible.value = true
  }
}

const handleDelete = async (row: Dict) => {
  try {
    await confirmDelete('字典', row.dictName)
    await deleteDict(row.id)
    ElMessage.success('删除成功')
    loadDictPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除字典失败', error)
  }
}

const handleItems = (row: Dict) => {
  currentDictId.value = row.id
  currentDictName.value = row.dictName
  handleItemDialogClose()
  itemsDialogVisible.value = true
  getDictItems(row.id)
}

const getDictItems = async (dictId: IdType) => {
  itemsLoading.value = true
  try {
    const res = await listDictItems(dictId)
    if (res.code === 200) {
      itemsTableData.value = res.data || []
      selectedItemRows.value = []
    }
  } catch (error) {
    console.error('获取字典项失败', error)
  } finally {
    itemsLoading.value = false
  }
}

const handleItemSelectionChange = (rows: DictItem[]) => {
  selectedItemRows.value = rows
}

const clearItemSelection = () => {
  itemsTableRef.value?.clearSelection()
  selectedItemRows.value = []
}

const handleRefreshItems = () => {
  clearItemSelection()
  if (currentDictId.value !== undefined) {
    getDictItems(currentDictId.value)
  }
}

const openItemExportColumnSetting = () => {
  itemExportColumnDialogVisible.value = true
}

const handleItemExportColumnConfirm = (selectedKeys: string[]) => {
  selectedItemExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(itemExportColumnStorageKey, selectedKeys)
}

const getResolvedItemExportColumns = () => resolveCsvColumns(itemExportColumns, selectedItemExportColumnKeys.value)

const handleExportSelectedItems = () => {
  downloadRowsAsCsv('字典项管理-选中项.csv', selectedItemRows.value, getResolvedItemExportColumns())
}

const handleExportCurrentItems = () => {
  downloadRowsAsCsv('字典项管理-当前结果.csv', itemsTableData.value, getResolvedItemExportColumns())
}

const handleAddItem = () => {
  if (currentDictId.value === undefined) {
    ElMessage.warning('请先选择字典')
    return
  }
  itemDialogTitle.value = '新增字典项'
  resetItemFormData({
    sortNo: itemsTableData.value.length + 1
  })
  itemDialogVisible.value = true
}

const handleEditItem = (row: DictItem) => {
  if (currentDictId.value === undefined) {
    ElMessage.warning('请先选择字典')
    return
  }
  itemDialogTitle.value = '编辑字典项'
  resetItemFormData(row)
  itemDialogVisible.value = true
}

const handleDeleteItem = async (row: DictItem) => {
  try {
    await confirmDelete('字典项', row.itemName)
    await deleteDictItem(row.id)
    ElMessage.success('删除成功')
    if (currentDictId.value !== undefined) {
      getDictItems(currentDictId.value)
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除字典项失败', error)
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
      await updateDict(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addDict(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadDictPage()
  } catch (error) {
    console.error('提交字典失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleItemSubmit = async () => {
  if (currentDictId.value === undefined) {
    ElMessage.warning('请先选择字典')
    return
  }

  const valid = await itemFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  itemSubmitLoading.value = true
  try {
    if (itemFormData.value.id) {
      await updateDictItem({
        ...itemFormData.value,
        dictId: currentDictId.value,
        itemName: itemFormData.value.itemName?.trim(),
        itemValue: itemFormData.value.itemValue?.trim()
      })
      ElMessage.success('更新字典项成功')
    } else {
      await addDictItem({
        ...itemFormData.value,
        dictId: currentDictId.value,
        itemName: itemFormData.value.itemName?.trim(),
        itemValue: itemFormData.value.itemValue?.trim()
      })
      ElMessage.success('新增字典项成功')
    }
    handleItemDialogClose()
    await getDictItems(currentDictId.value)
  } catch (error) {
    console.error('提交字典项失败', error)
  } finally {
    itemSubmitLoading.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const handleItemsDialogClose = () => {
  itemsDialogVisible.value = false
  clearItemSelection()
  itemsTableData.value = []
  currentDictId.value = undefined
  currentDictName.value = ''
  handleItemDialogClose()
}

const handleItemDialogClose = () => {
  itemDialogVisible.value = false
  itemFormRef.value?.clearValidate?.()
  resetItemFormData()
}

const getDictTypeName = (type: string) => {
  const map: Record<string, string> = {
    text: '文本',
    number: '数字',
    boolean: '布尔',
    date: '日期'
  }
  return map[type] || type
}

const getDictTypeTag = (type: string) => {
  const map: Record<string, string> = {
    text: 'primary',
    number: 'warning',
    boolean: 'info',
    date: 'success'
  }
  return map[type] || 'info'
}

const handleSizeChange = (size: number) => {
  setPageSize(size)
  loadDictPage()
}

const handlePageChange = (page: number) => {
  setPageNum(page)
  loadDictPage()
}
</script>
