<template>
  <div class="role-view sys-mgmt-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="角色名称">
              <el-input
                v-model="searchForm.roleName"
                placeholder="请输入角色名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="角色编码">
              <el-input
                v-model="searchForm.roleCode"
                placeholder="请输入角色编码"
                clearable
                @keyup.enter="handleSearch"
              />
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
        :data="pagedTableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="角色描述" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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

      <!-- 表单对话框 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        class="sys-dialog"
        width="600px"
        @close="handleDialogClose"
      >
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
        >
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
          </el-form-item>
          <el-form-item label="角色编码" prop="roleCode">
            <el-input v-model="formData.roleCode" placeholder="请输入角色编码" />
          </el-form-item>
          <el-form-item label="角色描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              placeholder="请输入角色描述"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="角色管理导出列设置"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { listRoles, getRole, addRole, updateRole, deleteRole } from '@/api/role'

// 表单引用
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  roleName: '',
  roleCode: '',
  status: undefined
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
  { key: 'roleName', label: '角色名称' },
  { key: 'roleCode', label: '角色编码' },
  { key: 'description', label: '角色描述' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'createTime', label: '创建时间' },
  { key: 'updateTime', label: '更新时间' }
]
const exportColumnStorageKey = 'role-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['roleName', 'roleCode', 'status', 'updateTime'] },
  { label: '管理模板', keys: ['roleName', 'roleCode', 'description', 'status', 'createTime'] }
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

const pagedTableData = computed(() => {
  const start = (pagination.pageNum - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return tableData.value.slice(start, end)
})

const normalizePageNum = () => {
  const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
  if (pagination.pageNum > maxPage) {
    pagination.pageNum = maxPage
  }
}

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formData = ref({
  id: undefined,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})

// 表单验证规则
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 提交状态
const submitLoading = ref(false)

// 获取角色列表
const getRoles = async () => {
  loading.value = true
  try {
    const res = await listRoles({
      roleName: searchForm.roleName || undefined,
      roleCode: searchForm.roleCode || undefined,
      status: searchForm.status || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data || []
      pagination.total = res.data?.length || 0
      normalizePageNum()
    }
  } catch (error) {
    console.error('获取角色列表失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  getRoles()
})

// 处理搜索
const handleSearch = () => {
  pagination.pageNum = 1
  getRoles()
}

// 重置搜索
const handleReset = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  getRoles()
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
  getRoles()
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
  downloadRowsAsCsv('角色管理-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('角色管理-当前结果.csv', pagedTableData.value, getResolvedExportColumns())
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增角色'
  formData.value = {
    id: undefined,
    roleName: '',
    roleCode: '',
    description: '',
    status: 1
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑角色'
  getRole(row.id).then((res) => {
    if (res.code === 200) {
      formData.value = res.data
      dialogVisible.value = true
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该角色吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteRole(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getRoles()
        }
      } catch (error) {
        console.error('删除失败', error)
      }
    })
    .catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid: boolean) => {
    if (!valid) return
  })

  submitLoading.value = true
  try {
    let res: any
    if (formData.value.id) {
      res = await updateRole(formData.value)
    } else {
      res = await addRole(formData.value)
    }
    if (res.code === 200) {
      ElMessage.success(formData.value.id ? '更新成功' : '新增成功')
      dialogVisible.value = false
      getRoles()
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  normalizePageNum()
}

// 当前页变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
}
</script>

<style scoped>
.role-view {
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
</style>
