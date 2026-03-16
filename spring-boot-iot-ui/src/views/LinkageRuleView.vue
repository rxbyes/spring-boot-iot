<template>
  <div class="linkage-rule-view">
    <div class="linkage-rule-header">
      <h1>联动规则</h1>
      <el-button type="primary" @click="handleAdd">新增规则</el-button>
    </div>

    <div class="linkage-rule-filters">
      <el-form :model="filters" label-position="left">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="规则名称">
              <el-input v-model="filters.ruleName" placeholder="请输入规则名�? clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状�?>
              <el-select v-model="filters.status" placeholder="请选择状�? clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="linkage-rule-list">
      <el-table :data="ruleList" v-loading="loading" border>
        <el-table-column prop="ruleName" label="规则名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状�? width="100">
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
    </div>

    <div class="linkage-rule-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 规则表单对话�?-->
    <el-dialog v-model="formVisible" :title="formTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名�? />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描�? />
        </el-form-item>
        <el-form-item label="触发条件" prop="triggerCondition">
          <el-input v-model="form.triggerCondition" type="textarea" :rows="5" placeholder="请输入触发条件（JSON格式�? />
        </el-form-item>
        <el-form-item label="动作列表" prop="actionList">
          <el-input v-model="form.actionList" type="textarea" :rows="5" placeholder="请输入动作列表（JSON格式�? />
        </el-form-item>
        <el-form-item label="状�? prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { getRuleList, addRule, updateRule, deleteRule } from '../api/linkageRule';
import type { LinkageRule } from '../api/linkageRule';

// 状�?
const loading = ref(false);
const formVisible = ref(false);
const ruleList = ref<LinkageRule[]>([]);

// 查询条件
const filters = reactive({
  ruleName: '',
  status: ''
});

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

// 表单
const formRef = ref();
const formTitle = computed(() => form.id ? '编辑规则' : '新增规则');
const form = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  description: '',
  triggerCondition: '',
  actionList: '',
  status: 0
});

const rules = {
  ruleName: [{ required: true, message: '请输入规则名�?, trigger: 'blur' }],
  triggerCondition: [{ required: true, message: '请输入触发条�?, trigger: 'blur' }],
  actionList: [{ required: true, message: '请输入动作列�?, trigger: 'blur' }]
};

const submitLoading = ref(false);

// 获取状态类�?
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

// 获取状态文�?
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

// 获取规则列表
const loadRuleList = async () => {
  loading.value = true;
  try {
    const res = await getRuleList({
      ruleName: filters.ruleName || undefined,
      status: filters.status ? parseInt(filters.status) : undefined
    });
    if (res.code === 200) {
      ruleList.value = res.data || [];
      pagination.total = res.data?.length || 0;
    }
  } catch (error) {
    console.error('查询规则列表失败', error);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  pagination.page = 1;
  loadRuleList();
};

// 处理重置
const handleReset = () => {
  filters.ruleName = '';
  filters.status = '';
  pagination.page = 1;
  loadRuleList();
};

// 处理大小变化
const handleSizeChange = () => {
  loadRuleList();
};

// 处理页码变化
const handlePageChange = () => {
  loadRuleList();
};

// 新增规则
const handleAdd = () => {
  form.id = undefined;
  form.ruleName = '';
  form.description = '';
  form.triggerCondition = '';
  form.actionList = '';
  form.status = 0;
  formVisible.value = true;
};

// 编辑规则
const handleEdit = (row: LinkageRule) => {
  form.id = row.id;
  form.ruleName = row.ruleName;
  form.description = row.description || '';
  form.triggerCondition = row.triggerCondition || '';
  form.actionList = row.actionList || '';
  form.status = row.status;
  formVisible.value = true;
};

// 删除规则
const handleDelete = async (row: LinkageRule) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '删除规则', {
      type: 'warning'
    });
    const res = await deleteRule(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      loadRuleList();
    }
  } catch (error) {
    console.error('删除规则失败', error);
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updateRule(form) : await addRule(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      loadRuleList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

// 初始�?
onMounted(() => {
  loadRuleList();
});
</script>

<style scoped>
.linkage-rule-view {
  padding: 20px;
}

.linkage-rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.linkage-rule-header h1 {
  font-size: 24px;
  margin: 0;
}

.linkage-rule-filters {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.linkage-rule-list {
  margin-bottom: 20px;
}

.linkage-rule-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>

