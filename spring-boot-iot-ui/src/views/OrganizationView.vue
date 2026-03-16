<template>
  <div class="organization-view sys-mgmt-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>组织机构管理</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="组织名称">
              <el-input
                v-model="searchForm.orgName"
                placeholder="请输入组织名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="组织编码">
              <el-input
                v-model="searchForm.orgCode"
                placeholder="请输入组织编码"
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
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
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
        row-key="id"
        :tree-props="{ children: 'children' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="orgCode" label="组织编码" width="150" />
        <el-table-column prop="orgName" label="组织名称" width="200" />
        <el-table-column prop="orgType" label="组织类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrgTypeTag(row.orgType)">
              {{ getOrgTypeName(row.orgType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="leaderName" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
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
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { downloadRowsAsCsv } from '@/utils/csv'
import {
  listOrganizationTree,
  getOrganization,
  addOrganization,
  updateOrganization,
  deleteOrganization
} from '@/api/organization'

// 表单引用
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  orgName: '',
  orgCode: '',
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
const sourceTreeData = ref<any[]>([])
const tableRef = ref()
const selectedRows = ref<any[]>([])

// 加载状态
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增组织机构')
const formData = ref({
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

// 表单验证规则
const formRules = {
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgType: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
  leaderName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }]
}

// 提交状态
const submitLoading = ref(false)

// 获取组织机构树
const getOrganizationTree = async () => {
  loading.value = true
  try {
    const res = await listOrganizationTree()
    if (res.code === 200) {
      sourceTreeData.value = res.data || []
      tableData.value = sourceTreeData.value
      pagination.total = countTreeNodes(tableData.value)
    }
  } catch (error) {
    console.error('获取组织机构树失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  getOrganizationTree()
})

const normalizeKeyword = (value?: string) => (value || '').trim().toLowerCase()

const nodeMatchesSearch = (node: any) => {
  const orgNameKeyword = normalizeKeyword(searchForm.orgName)
  const orgCodeKeyword = normalizeKeyword(searchForm.orgCode)
  const statusMatched = searchForm.status === undefined || node.status === searchForm.status
  const orgNameMatched = !orgNameKeyword || String(node.orgName || '').toLowerCase().includes(orgNameKeyword)
  const orgCodeMatched = !orgCodeKeyword || String(node.orgCode || '').toLowerCase().includes(orgCodeKeyword)
  return statusMatched && orgNameMatched && orgCodeMatched
}

const filterOrganizationTree = (nodes: any[]): any[] => {
  return nodes
    .map((node) => {
      const filteredChildren = Array.isArray(node.children) ? filterOrganizationTree(node.children) : []
      if (nodeMatchesSearch(node) || filteredChildren.length > 0) {
        return {
          ...node,
          children: filteredChildren
        }
      }
      return null
    })
    .filter(Boolean) as any[]
}

const countTreeNodes = (nodes: any[]): number => {
  return nodes.reduce((count, node) => count + 1 + countTreeNodes(node.children || []), 0)
}

const applyOrganizationFilters = () => {
  tableData.value = filterOrganizationTree(sourceTreeData.value)
  pagination.total = countTreeNodes(tableData.value)
}

// 处理搜索
const handleSearch = () => {
  applyOrganizationFilters()
}

// 重置搜索
const handleReset = () => {
  searchForm.orgName = ''
  searchForm.orgCode = ''
  searchForm.status = undefined
  tableData.value = sourceTreeData.value
  pagination.total = countTreeNodes(tableData.value)
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
  getOrganizationTree()
}

const handleExportSelected = () => {
  downloadRowsAsCsv('组织机构-选中项.csv', selectedRows.value)
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增组织机构'
  formData.value = {
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
  }
  dialogVisible.value = true
}

// 新增子级
const handleAddSub = (row: any) => {
  dialogTitle.value = '新增子级'
  formData.value = {
    id: undefined,
    parentId: row.id,
    orgName: '',
    orgCode: '',
    orgType: 'dept',
    leaderName: '',
    phone: '',
    email: '',
    status: 1,
    sortNo: 0,
    remark: ''
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑组织机构'
  getOrganization(row.id).then((res) => {
    if (res.code === 200) {
      formData.value = res.data
      dialogVisible.value = true
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该组织机构吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteOrganization(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getOrganizationTree()
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
      res = await updateOrganization(formData.value)
    } else {
      res = await addOrganization(formData.value)
    }
    if (res.code === 200) {
      ElMessage.success(formData.value.id ? '更新成功' : '新增成功')
      dialogVisible.value = false
      getOrganizationTree()
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

// 获取组织类型名称
const getOrgTypeName = (type: string) => {
  const map: Record<string, string> = {
    dept: '部门',
    position: '岗位',
    team: '团队'
  }
  return map[type] || type
}

// 获取组织类型标签
const getOrgTypeTag = (type: string) => {
  const map: Record<string, string> = {
    dept: 'primary',
    position: 'warning',
    team: 'info'
  }
  return map[type] || 'info'
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  getOrganizationTree()
}

// 当前页变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  getOrganizationTree()
}
</script>

<style scoped>
.organization-view {
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
