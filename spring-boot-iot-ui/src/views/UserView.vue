<template>
  <div class="user-view sys-mgmt-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="用户名">
              <el-input
                v-model="searchForm.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="手机号">
              <el-input
                v-model="searchForm.phone"
                placeholder="请输入手机号"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮箱">
              <el-input
                v-model="searchForm.email"
                placeholder="请输入邮箱"
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
        :data="pagedTableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" width="180" />
        <el-table-column prop="lastLoginIp" label="最后登录IP" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleResetPassword(row)">重置密码</el-button>
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
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="密码" prop="password" v-if="!formData.id">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
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
        title="用户管理导出列设置"
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
import {
  listUsers,
  getUser,
  addUser,
  updateUser,
  deleteUser,
  resetPassword
} from '@/api/user'

// 表单引用
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: '',
  email: '',
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
  { key: 'username', label: '用户名' },
  { key: 'realName', label: '真实姓名' },
  { key: 'phone', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'lastLoginTime', label: '最后登录时间' },
  { key: 'lastLoginIp', label: '最后登录IP' },
  { key: 'createTime', label: '创建时间' }
]
const exportColumnStorageKey = 'user-view'
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['username', 'realName', 'status', 'lastLoginTime', 'lastLoginIp'] },
  { label: '管理模板', keys: ['username', 'realName', 'phone', 'email', 'status', 'createTime'] }
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
const dialogTitle = ref('新增用户')
const formData = ref({
  id: undefined,
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  status: 1
})

// 表单验证规则
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 提交状态
const submitLoading = ref(false)

// 获取用户列表
const getUsers = async () => {
  loading.value = true
  try {
    const res = await listUsers({
      username: searchForm.username || undefined,
      phone: searchForm.phone || undefined,
      email: searchForm.email || undefined,
      status: searchForm.status || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data || []
      pagination.total = res.data?.length || 0
      normalizePageNum()
    }
  } catch (error) {
    console.error('获取用户列表失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  getUsers()
})

// 处理搜索
const handleSearch = () => {
  pagination.pageNum = 1
  getUsers()
}

// 重置搜索
const handleReset = () => {
  searchForm.username = ''
  searchForm.phone = ''
  searchForm.email = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  getUsers()
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
  getUsers()
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
  downloadRowsAsCsv('用户管理-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('用户管理-当前结果.csv', pagedTableData.value, getResolvedExportColumns())
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  formData.value = {
    id: undefined,
    username: '',
    realName: '',
    phone: '',
    email: '',
    password: '',
    status: 1
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑用户'
  getUser(row.id).then((res) => {
    if (res.code === 200) {
      formData.value = res.data
      delete formData.value.password // 编辑时不需要密码
      dialogVisible.value = true
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteUser(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getUsers()
        }
      } catch (error) {
        console.error('删除失败', error)
      }
    })
    .catch(() => {})
}

// 重置密码
const handleResetPassword = (row: any) => {
  ElMessageBox.confirm(`确定要重置用户 "${row.username}" 的密码吗？`, '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await resetPassword(row.id)
        if (res.code === 200) {
          ElMessage.success('密码重置成功，默认密码为 123456')
        }
      } catch (error) {
        console.error('重置密码失败', error)
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
      res = await updateUser(formData.value)
    } else {
      res = await addUser(formData.value)
    }
    if (res.code === 200) {
      ElMessage.success(formData.value.id ? '更新成功' : '新增成功')
      dialogVisible.value = false
      getUsers()
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
.user-view {
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
