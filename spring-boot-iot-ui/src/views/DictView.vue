<template>
  <div class="dict-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>字典配置</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="字典名称">
              <el-input
                v-model="searchForm.dictName"
                placeholder="请输入字典名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="字典编码">
              <el-input
                v-model="searchForm.dictCode"
                placeholder="请输入字典编码"
                clearable
                @keyup.enter="handleSearch"
              />
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

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="dictCode" label="字典编码" width="150" />
        <el-table-column prop="dictName" label="字典名称" width="200" />
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
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleItems(row)">字典项</el-button>
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
        width="600px"
        @close="handleDialogClose"
      >
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
        >
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
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>

      <!-- 字典项管理对话框 -->
      <el-dialog
        v-model="itemsDialogVisible"
        title="字典项管理"
        width="800px"
      >
        <el-button type="primary" @click="handleAddItem" style="margin-bottom: 10px;">新增字典项</el-button>
        <el-table
          v-loading="itemsLoading"
          :data="itemsTableData"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column prop="itemName" label="项名称" width="150" />
          <el-table-column prop="itemValue" label="项值" width="150" />
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
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEditItem(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <template #footer>
          <el-button @click="itemsDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listDicts,
  getDict,
  addDict,
  updateDict,
  deleteDict,
  listDictItems,
  addDictItem,
  updateDictItem,
  deleteDictItem
} from '@/api/dict'

// 表单引用
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  dictName: '',
  dictCode: '',
  dictType: undefined
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref<any[]>([])

// 加载状态
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增字典')
const formData = ref({
  id: undefined,
  dictName: '',
  dictCode: '',
  dictType: 'text',
  status: 1,
  sortNo: 0,
  remark: ''
})

// 表单验证规则
const formRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictType: [{ required: true, message: '请选择字典类型', trigger: 'change' }]
}

// 提交状态
const submitLoading = ref(false)

// 字典项管理对话框
const itemsDialogVisible = ref(false)
const itemsTableData = ref<any[]>([])
const itemsLoading = ref(false)
const currentDictId = ref<number>()

// 获取字典列表
const getDictList = async () => {
  loading.value = true
  try {
    const res = await listDicts()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (error) {
    console.error('获取字典列表失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  getDictList()
})

// 处理搜索
const handleSearch = () => {
  // TODO: 实现搜索逻辑
}

// 重置搜索
const handleReset = () => {
  searchForm.dictName = ''
  searchForm.dictCode = ''
  searchForm.dictType = undefined
  getDictList()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增字典'
  formData.value = {
    id: undefined,
    dictName: '',
    dictCode: '',
    dictType: 'text',
    status: 1,
    sortNo: 0,
    remark: ''
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑字典'
  getDict(row.id).then((res) => {
    if (res.code === 200) {
      formData.value = res.data
      dialogVisible.value = true
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该字典吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteDict(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getDictList()
        }
      } catch (error) {
        console.error('删除失败', error)
      }
    })
    .catch(() => {})
}

// 查看字典项
const handleItems = (row: any) => {
  currentDictId.value = row.id
  itemsDialogVisible.value = true
  getDictItems(row.id)
}

// 获取字典项
const getDictItems = async (dictId: number) => {
  itemsLoading.value = true
  try {
    const res = await listDictItems(dictId)
    if (res.code === 200) {
      itemsTableData.value = res.data || []
    }
  } catch (error) {
    console.error('获取字典项失败', error)
  } finally {
    itemsLoading.value = false
  }
}

// 新增字典项
const handleAddItem = () => {
  // TODO: 实现新增字典项逻辑
}

// 编辑字典项
const handleEditItem = (row: any) => {
  // TODO: 实现编辑字典项逻辑
}

// 删除字典项
const handleDeleteItem = (row: any) => {
  ElMessageBox.confirm('确定要删除该字典项吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteDictItem(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          if (currentDictId.value) {
            getDictItems(currentDictId.value)
          }
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
      res = await updateDict(formData.value)
    } else {
      res = await addDict(formData.value)
    }
    if (res.code === 200) {
      ElMessage.success(formData.value.id ? '更新成功' : '新增成功')
      dialogVisible.value = false
      getDictList()
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

// 获取字典类型名称
const getDictTypeName = (type: string) => {
  const map: Record<string, string> = {
    text: '文本',
    number: '数字',
    boolean: '布尔',
    date: '日期'
  }
  return map[type] || type
}

// 获取字典类型标签
const getDictTypeTag = (type: string) => {
  const map: Record<string, string> = {
    text: 'primary',
    number: 'warning',
    boolean: 'info',
    date: 'success'
  }
  return map[type] || 'info'
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  getDictList()
}

// 当前页变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  getDictList()
}
</script>

<style scoped>
.dict-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.text-right {
  text-align: right;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
