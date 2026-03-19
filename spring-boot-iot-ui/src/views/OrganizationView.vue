<template>
  <div class="organization-view sys-mgmt-view standard-list-view">
    <PanelCard class="box-card">
      <template #header>
        <div class="card-header">
          <span>组织架构</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="组织名称">
              <el-input v-model="searchForm.orgName" placeholder="请输入组织名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="组织编码">
              <el-input v-model="searchForm.orgCode" placeholder="请输入组织编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
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
        title="默认仅分页加载根节点，展开行时按需加载子节点。"
        type="info"
        :closable="false"
        show-icon
        class="view-alert"
      />
      <el-alert
        v-else
        title="搜索模式返回扁平分页结果，不再一次性加载整棵组织树。"
        type="info"
        :closable="false"
        show-icon
        class="view-alert"
      />

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
        row-key="id"
        :lazy="!isFilterMode"
        :load="loadChildren"
        :tree-props="treeProps"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="orgCode" label="组织编码" :width="150" />
        <StandardTableTextColumn prop="orgName" label="组织名称" :width="200" />
        <el-table-column prop="orgType" label="组织类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrgTypeTag(row.orgType)">
              {{ getOrgTypeName(row.orgType) }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="leaderName" label="负责人" :width="120" />
        <StandardTableTextColumn prop="phone" label="联系电话" :width="150" />
        <StandardTableTextColumn prop="email" label="邮箱" :width="200" />
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
            <el-button type="primary" link @click="handleAddSub(row)">新增子级</el-button>
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
        subtitle="统一通过右侧抽屉维护组织机构主数据。"
        size="42rem"
        @close="handleDialogClose"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
          <el-form-item label="组织名称" prop="orgName">
            <el-input v-model="formData.orgName" placeholder="请输入组织名称" />
          </el-form-item>
          <el-form-item label="组织编码" prop="orgCode">
            <el-input v-model="formData.orgCode" placeholder="请输入组织编码" />
          </el-form-item>
          <el-form-item label="组织类型" prop="orgType">
            <el-select v-model="formData.orgType" placeholder="请选择组织类型">
              <el-option label="部门" value="dept" />
              <el-option label="岗位" value="position" />
              <el-option label="团队" value="team" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人" prop="leaderName">
            <el-input v-model="formData.leaderName" placeholder="请输入负责人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
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
        title="组织机构导出列设置"
        :options="exportColumnOptions"
        :selected-keys="selectedExportColumnKeys"
        :preset-storage-key="exportColumnStorageKey"
        :presets="exportPresets"
        @confirm="handleExportColumnConfirm"
      />
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
  addOrganization,
  deleteOrganization,
  getOrganization,
  listOrganizations,
  pageOrganizations,
  updateOrganization,
  type Organization
} from '@/api/organization'

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增组织机构')
const tableData = ref<Organization[]>([])
const selectedRows = ref<Organization[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination()

const searchForm = reactive({
  orgName: '',
  orgCode: '',
  status: undefined as number | undefined
})

const formData = ref<Partial<Organization>>({
  id: undefined,
  parentId: 0,
  orgName: '',
  orgCode: '',
  orgType: 'dept',
  leaderName: '',
  phone: '',
  email: '',
  status: 1,
  sortNo: 0,
  remark: ''
})

const formRules = {
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgType: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
  leaderName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }]
}

const exportColumns: CsvColumn<Organization>[] = [
  { key: 'orgCode', label: '组织编码' },
  { key: 'orgName', label: '组织名称' },
  { key: 'orgType', label: '组织类型', formatter: (value) => getOrgTypeName(String(value || '')) },
  { key: 'leaderName', label: '负责人' },
  { key: 'phone', label: '联系电话' },
  { key: 'email', label: '邮箱' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'sortNo', label: '排序' },
  { key: 'remark', label: '备注' }
]
const exportColumnStorageKey = 'organization-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['orgCode', 'orgName', 'orgType', 'leaderName', 'phone', 'status'] },
  { label: '管理模板', keys: ['orgCode', 'orgName', 'orgType', 'leaderName', 'status', 'sortNo', 'remark'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const exportColumnDialogVisible = ref(false)

const isFilterMode = computed(
  () => Boolean(searchForm.orgName.trim() || searchForm.orgCode.trim() || searchForm.status !== undefined)
)

const treeProps = {
  children: 'children',
  hasChildren: 'hasChildren'
}

const loadOrganizationPage = async () => {
  loading.value = true
  try {
    const res = await pageOrganizations({
      orgName: searchForm.orgName || undefined,
      orgCode: searchForm.orgCode || undefined,
      status: searchForm.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取组织分页失败', error)
  } finally {
    loading.value = false
  }
}

const loadChildren = async (row: Organization, _treeNode: unknown, resolve: (data: Organization[]) => void) => {
  try {
    const res = await listOrganizations(row.id)
    const children = res.data || []
    row.children = children
    row.hasChildren = children.length > 0
    resolve(children)
  } catch (error) {
    console.error('加载组织子节点失败', error)
    resolve([])
  }
}

onMounted(() => {
  loadOrganizationPage()
})

const handleSearch = () => {
  resetPage()
  clearSelection()
  loadOrganizationPage()
}

const handleReset = () => {
  searchForm.orgName = ''
  searchForm.orgCode = ''
  searchForm.status = undefined
  resetPage()
  clearSelection()
  loadOrganizationPage()
}

const handleSelectionChange = (rows: Organization[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  loadOrganizationPage()
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
  downloadRowsAsCsv('组织机构-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const flattenTreeRows = (rows: Organization[]): Organization[] =>
  rows.flatMap((row) => [row, ...(Array.isArray(row.children) ? flattenTreeRows(row.children) : [])])

const handleExportCurrent = () => {
  const rows = isFilterMode.value ? tableData.value : flattenTreeRows(tableData.value)
  downloadRowsAsCsv('组织机构-当前结果.csv', rows, getResolvedExportColumns())
}

const resetFormData = (organization?: Partial<Organization>) => {
  formData.value = {
    id: organization?.id,
    parentId: organization?.parentId ?? 0,
    orgName: organization?.orgName || '',
    orgCode: organization?.orgCode || '',
    orgType: organization?.orgType || 'dept',
    leaderName: organization?.leaderName || '',
    phone: organization?.phone || '',
    email: organization?.email || '',
    status: organization?.status ?? 1,
    sortNo: organization?.sortNo ?? 0,
    remark: organization?.remark || ''
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增组织机构'
  resetFormData()
  dialogVisible.value = true
}

const handleAddSub = (row: Organization) => {
  dialogTitle.value = '新增子级'
  resetFormData({ parentId: row.id })
  dialogVisible.value = true
}

const handleEdit = async (row: Organization) => {
  dialogTitle.value = '编辑组织机构'
  const res = await getOrganization(row.id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
    dialogVisible.value = true
  }
}

const handleDelete = async (row: Organization) => {
  try {
    await confirmDelete('组织', row.orgName)
    await deleteOrganization(row.id)
    ElMessage.success('删除成功')
    loadOrganizationPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除组织失败', error)
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
      await updateOrganization(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addOrganization(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadOrganizationPage()
  } catch (error) {
    console.error('提交组织失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const getOrgTypeName = (type: string) => {
  const map: Record<string, string> = {
    dept: '部门',
    position: '岗位',
    team: '团队'
  }
  return map[type] || type
}

const getOrgTypeTag = (type: string) => {
  const map: Record<string, string> = {
    dept: 'primary',
    position: 'warning',
    team: 'info'
  }
  return map[type] || 'info'
}

const handleSizeChange = (size: number) => {
  setPageSize(size)
  loadOrganizationPage()
}

const handlePageChange = (page: number) => {
  setPageNum(page)
  loadOrganizationPage()
}
</script>
