<template>
  <div class="user-view sys-mgmt-view standard-list-view">
    <PanelCard class="box-card">
      <template #header>
        <div class="card-header">
          <span>账号中心</span>
          <el-button v-permission="'system:user:add'" type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="用户名">
              <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="手机号">
              <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮箱">
              <el-input v-model="searchForm.email" placeholder="请输入邮箱" clearable @keyup.enter="handleSearch" />
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
        <StandardTableTextColumn prop="username" label="用户名" :width="150" />
        <StandardTableTextColumn prop="realName" label="真实姓名" :width="120" />
        <StandardTableTextColumn prop="phone" label="手机号" :width="150" />
        <StandardTableTextColumn prop="email" label="邮箱" :width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="lastLoginTime" label="最后登录时间" :width="180" />
        <StandardTableTextColumn prop="lastLoginIp" label="最后登录 IP" :width="150" />
        <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
        <el-table-column label="操作" width="200" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <el-button v-permission="'system:user:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:user:reset-password'" type="primary" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-permission="'system:user:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
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
        subtitle="统一通过右侧抽屉维护用户基础信息。"
        size="42rem"
        @close="handleDialogClose"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
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
          <el-form-item v-if="!formData.id" label="密码" prop="password">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <StandardDrawerFooter @cancel="dialogVisible = false">
            <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="dialogVisible = false">
              取消
            </el-button>
            <el-button
              v-permission="formData.id ? 'system:user:update' : 'system:user:add'"
              type="primary"
              class="standard-drawer-footer__button standard-drawer-footer__button--primary"
              :loading="submitLoading"
              @click="handleSubmit"
            >
              确定
            </el-button>
          </StandardDrawerFooter>
        </template>
      </StandardFormDrawer>

      <CsvColumnSettingDialog
        v-model="exportColumnDialogVisible"
        title="账号中心导出列设置"
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
import { useServerPagination } from '@/composables/useServerPagination'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { addUser, deleteUser, getUser, pageUsers, resetPassword, updateUser, type User } from '@/api/user'

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const tableData = ref<User[]>([])
const selectedRows = ref<User[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination()

const searchForm = reactive({
  username: '',
  phone: '',
  email: ''
})

const formData = ref<Partial<User>>({
  id: undefined,
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const exportColumns: CsvColumn<User>[] = [
  { key: 'username', label: '用户名' },
  { key: 'realName', label: '真实姓名' },
  { key: 'phone', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'status', label: '状态', formatter: (value) => (Number(value) === 1 ? '启用' : '禁用') },
  { key: 'lastLoginTime', label: '最后登录时间' },
  { key: 'lastLoginIp', label: '最后登录 IP' },
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

const loadUserPage = async () => {
  loading.value = true
  try {
    const res = await pageUsers({
      username: searchForm.username || undefined,
      phone: searchForm.phone || undefined,
      email: searchForm.email || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取用户分页失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserPage()
})

const handleSearch = () => {
  resetPage()
  clearSelection()
  loadUserPage()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.phone = ''
  searchForm.email = ''
  resetPage()
  clearSelection()
  loadUserPage()
}

const handleSelectionChange = (rows: User[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  clearSelection()
  loadUserPage()
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
  downloadRowsAsCsv('账号中心-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv('账号中心-当前结果.csv', tableData.value, getResolvedExportColumns())
}

const resetFormData = (parent?: Partial<User>) => {
  formData.value = {
    id: parent?.id,
    username: parent?.username || '',
    realName: parent?.realName || '',
    phone: parent?.phone || '',
    email: parent?.email || '',
    password: '',
    status: parent?.status ?? 1,
    roleIds: parent?.roleIds
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = async (row: User) => {
  dialogTitle.value = '编辑用户'
  const res = await getUser(row.id as string | number)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
    dialogVisible.value = true
  }
}

const handleDelete = async (row: User) => {
  try {
    await confirmDelete('用户', row.username)
    await deleteUser(row.id as string | number)
    ElMessage.success('删除成功')
    loadUserPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除用户失败', error)
  }
}

const handleResetPassword = async (row: User) => {
  try {
    await confirmAction({
      title: '重置密码',
      message: `确认重置用户“${row.username}”的密码吗？重置后默认密码为 123456。`,
      type: 'warning',
      confirmButtonText: '确认重置'
    })
    await resetPassword(row.id as string | number)
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('重置用户密码失败', error)
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
      await updateUser(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addUser(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadUserPage()
  } catch (error) {
    console.error('提交用户失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const handleSizeChange = (size: number) => {
  setPageSize(size)
  loadUserPage()
}

const handlePageChange = (page: number) => {
  setPageNum(page)
  loadUserPage()
}
</script>
