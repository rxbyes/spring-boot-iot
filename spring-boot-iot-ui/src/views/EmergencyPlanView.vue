<template>
  <div class="emergency-plan-view sys-mgmt-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>应急预案</span>
          <el-button type="primary" @click="handleAdd">新增预案</el-button>
        </div>
      </template>

      <el-form :model="filters" label-width="96px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="预案名称">
              <el-input v-model="filters.planName" placeholder="请输入预案名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险等级">
              <el-select v-model="filters.riskLevel" placeholder="请选择风险等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
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
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        :data="planList"
        v-loading="loading"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="planName" label="预案名称" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)">{{ getRiskLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 预案表单对话框 -->
    <el-dialog v-model="formVisible" :title="formTitle" class="sys-dialog" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="预案名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入预案名称" />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-radio-group v-model="form.riskLevel">
            <el-radio value="critical">严重</el-radio>
            <el-radio value="warning">警告</el-radio>
            <el-radio value="info">提醒</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="响应步骤" prop="responseSteps">
          <el-input v-model="form.responseSteps" type="textarea" :rows="5" placeholder="请输入响应步骤（JSON格式）" />
        </el-form-item>
        <el-form-item label="联系人列表" prop="contactList">
          <el-input v-model="form.contactList" type="textarea" :rows="3" placeholder="请输入联系人列表（JSON格式）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">启用</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="formVisible = false">取消</el-button>
        <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { pagePlanList, addPlan, updatePlan, deletePlan } from '../api/emergencyPlan';
import type { EmergencyPlan } from '../api/emergencyPlan';

// 状态
const loading = ref(false);
const formVisible = ref(false);
const planList = ref<EmergencyPlan[]>([]);
const tableRef = ref();
const selectedRows = ref<EmergencyPlan[]>([]);

// 查询条件
const filters = reactive({
  planName: '',
  riskLevel: '',
  status: '' as '' | number
});

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

// 表单
const formRef = ref();
const formTitle = computed(() => form.id ? '编辑预案' : '新增预案');
const form = reactive({
  id: undefined as number | undefined,
  planName: '',
  riskLevel: 'warning',
  description: '',
  responseSteps: '',
  contactList: '',
  status: 0
});

const rules = {
  planName: [{ required: true, message: '请输入预案名称', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  responseSteps: [{ required: true, message: '请输入响应步骤', trigger: 'blur' }]
};

const submitLoading = ref(false);

// 获取风险等级类型
const getRiskLevelType = (riskLevel: string) => {
  switch (riskLevel) {
    case 'critical':
      return 'danger';
    case 'warning':
      return 'warning';
    case 'info':
      return 'info';
    default:
      return 'info';
  }
};

// 获取风险等级文本
const getRiskLevelText = (riskLevel: string) => {
  switch (riskLevel) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return riskLevel;
  }
};

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'info';
    default:
      return 'info';
  }
};

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '启用';
    case 1:
      return '停用';
    default:
      return status.toString();
  }
};

// 获取预案列表
const loadPlanList = async () => {
  loading.value = true;
  try {
    const res = await pagePlanList({
      planName: filters.planName || undefined,
      riskLevel: filters.riskLevel || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
      pageNum: pagination.page,
      pageSize: pagination.size
    });
    if (res.code === 200) {
      planList.value = res.data?.records || [];
      pagination.total = res.data?.total || 0;
    }
  } catch (error) {
    console.error('查询预案列表失败', error);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  pagination.page = 1;
  loadPlanList();
};

// 处理重置
const handleReset = () => {
  filters.planName = '';
  filters.riskLevel = '';
  filters.status = '';
  pagination.page = 1;
  loadPlanList();
};

// 处理大小变化
const handleSizeChange = () => {
  loadPlanList();
};

// 处理页码变化
const handlePageChange = () => {
  loadPlanList();
};

const handleSelectionChange = (rows: EmergencyPlan[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  loadPlanList();
};

// 新增预案
const handleAdd = () => {
  form.id = undefined;
  form.planName = '';
  form.riskLevel = 'warning';
  form.description = '';
  form.responseSteps = '';
  form.contactList = '';
  form.status = 0;
  formVisible.value = true;
};

// 编辑预案
const handleEdit = (row: EmergencyPlan) => {
  form.id = row.id;
  form.planName = row.planName;
  form.riskLevel = row.riskLevel;
  form.description = row.description || '';
  form.responseSteps = row.responseSteps || '';
  form.contactList = row.contactList || '';
  form.status = row.status;
  formVisible.value = true;
};

// 删除预案
const handleDelete = async (row: EmergencyPlan) => {
  try {
    await ElMessageBox.confirm('确定要删除该预案吗？', '删除预案', {
      type: 'warning'
    });
    const res = await deletePlan(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      loadPlanList();
    }
  } catch (error) {
    console.error('删除预案失败', error);
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updatePlan(form) : await addPlan(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      loadPlanList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

// 初始化
onMounted(() => {
  loadPlanList();
});
</script>
