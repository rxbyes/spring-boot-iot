<template>
  <div class="region-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>区域管理</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="区域名称">
              <el-input
                v-model="searchForm.regionName"
                placeholder="请输入区域名�?
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区域编码">
              <el-input
                v-model="searchForm.regionCode"
                placeholder="请输入区域编�?
                clearable
                @keyup.enter="handleSearch"
              />
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

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        row-key="id"
        :tree-props="{ children: 'children' }"
      >
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
        <el-table-column prop="status" label="状�? width="100">
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

      <!-- 表单对话�?-->
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
          <el-form-item label="区域名称" prop="regionName">
            <el-input v-model="formData.regionName" placeholder="请输入区域名�? />
          </el-form-item>
          <el-form-item label="区域编码" prop="regionCode">
            <el-input v-model="formData.regionCode" placeholder="请输入区域编�? />
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
            <el-input-number
              v-model="formData.longitude"
              :min="-180"
              :max="180"
              :step="0.000001"
              placeholder="请输入经�?
            />
          </el-form-item>
          <el-form-item label="纬度" prop="latitude">
            <el-input-number
              v-model="formData.latitude"
              :min="-90"
              :max="90"
              :step="0.000001"
              placeholder="请输入纬�?
            />
          </el-form-item>
          <el-form-item label="状�? prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
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
              placeholder="请输入备�?
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from '@/utils/message'
import { ElMessageBox } from '@/utils/messageBox'
import { Plus } from '@element-plus/icons-vue'
import {
  listRegionTree,
  getRegion,
  addRegion,
  updateRegion,
  deleteRegion
} from '@/api/region'

// 表单引用
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  regionName: '',
  regionCode: '',
  regionType: undefined
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref<any[]>([])

// 加载状�?
const loading = ref(false)

// 对话�?
const dialogVisible = ref(false)
const dialogTitle = ref('新增区域')
const formData = ref({
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

// 表单验证规则
const formRules = {
  regionName: [{ required: true, message: '请输入区域名�?, trigger: 'blur' }],
  regionCode: [{ required: true, message: '请输入区域编�?, trigger: 'blur' }],
  regionType: [{ required: true, message: '请选择区域类型', trigger: 'change' }]
}

// 提交状�?
const submitLoading = ref(false)

// 获取区域�?
const getRegionTree = async () => {
  loading.value = true
  try {
    const res = await listRegionTree()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (error) {
    console.error('获取区域树失�?, error)
  } finally {
    loading.value = false
  }
}

// 初始�?
onMounted(() => {
  getRegionTree()
})

// 处理搜索
const handleSearch = () => {
  // TODO: 实现搜索逻辑
}

// 重置搜索
const handleReset = () => {
  searchForm.regionName = ''
  searchForm.regionCode = ''
  searchForm.regionType = undefined
  getRegionTree()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增区域'
  formData.value = {
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
  }
  dialogVisible.value = true
}

// 新增子级
const handleAddSub = (row: any) => {
  dialogTitle.value = '新增子级'
  formData.value = {
    id: undefined,
    parentId: row.id,
    regionName: '',
    regionCode: '',
    regionType: 'province',
    longitude: undefined,
    latitude: undefined,
    status: 1,
    sortNo: 0,
    remark: ''
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑区域'
  getRegion(row.id).then((res) => {
    if (res.code === 200) {
      formData.value = res.data
      dialogVisible.value = true
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该区域吗？', '警告', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteRegion(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          getRegionTree()
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
      res = await updateRegion(formData.value)
    } else {
      res = await addRegion(formData.value)
    }
    if (res.code === 200) {
      ElMessage.success(formData.value.id ? '更新成功' : '新增成功')
      dialogVisible.value = false
      getRegionTree()
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话�?
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 获取区域类型名称
const getRegionTypeName = (type: string) => {
  const map: Record<string, string> = {
    province: '省份',
    city: '城市',
    district: '区县',
    street: '街道'
  }
  return map[type] || type
}

// 获取区域类型标签
const getRegionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    province: 'primary',
    city: 'warning',
    district: 'info',
    street: 'success'
  }
  return map[type] || 'info'
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  getRegionTree()
}

// 当前页变�?
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  getRegionTree()
}
</script>

<style scoped>
.region-view {
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

