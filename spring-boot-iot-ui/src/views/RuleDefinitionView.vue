<template>
  <div class="rule-definition-view">
    <div class="rule-definition-header">
      <h1>阈值规则配�?/h1>
      <el-button type="primary" @click="handleAdd">新增规则</el-button>
    </div>

    <div class="rule-definition-filters">
      <el-form :model="filters" label-position="left">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="规则名称">
              <el-input v-model="filters.ruleName" placeholder="请输入规则名�? clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="测点标识�?>
              <el-input v-model="filters.metricIdentifier" placeholder="请输入测点标识符" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="告警等级">
              <el-select v-model="filters.alarmLevel" placeholder="请选择告警等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
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

    <div class="rule-definition-list">
      <el-table :data="ruleList" v-loading="loading" border>
        <el-table-column prop="ruleName" label="规则名称" />
        <el-table-column prop="metricIdentifier" label="测点标识�? width="150" />
        <el-table-column prop="metricName" label="测点名称" width="120" />
        <el-table-column prop="expression" label="表达�? width="200" />
        <el-table-column prop="duration" label="持续时间(�?" width="120" />
        <el-table-column prop="alarmLevel" label="告警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="convertToEvent" label="转事�? width="100">
          <template #default="{ row }">
            <el-tag :type="row.convertToEvent === 1 ? 'success' : 'info'">
              {{ row.convertToEvent === 1 ? '�? : '�? }}
            </el-tag>
          </template>
        </el-table-column>
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

    <div class="rule-definition-pagination">
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名�? />
        </el-form-item>
        <el-form-item label="测点标识�? prop="metricIdentifier">
          <el-input v-model="form.metricIdentifier" placeholder="请输入测点标识符" />
        </el-form-item>
        <el-form-item label="测点名称" prop="metricName">
          <el-input v-model="form.metricName" placeholder="请输入测点名�? />
        </el-form-item>
        <el-form-item label="表达�? prop="expression">
          <el-input v-model="form.expression" placeholder="例如：value > 100" />
        </el-form-item>
        <el-form-item label="持续时间(�?" prop="duration">
          <el-input-number v-model="form.duration" :min="0" :max="3600" placeholder="请输入持续时�? />
        </el-form-item>
        <el-form-item label="告警等级" prop="alarmLevel">
          <el-select v-model="form.alarmLevel" placeholder="请选择告警等级" style="width: 100%">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="提醒" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知方式">
          <el-checkbox-group v-model="form.notificationMethods">
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="wechat">微信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="转事�?>
          <el-radio-group v-model="form.convertToEvent">
            <el-radio :label="0">�?/el-radio>
            <el-radio :label="1">�?/el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状�? prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入描�? />
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
import { getRuleList, addRule, updateRule, deleteRule } from '../api/ruleDefinition';
import type { RuleDefinition } from '../api/ruleDefinition';

// 状�?
const loading = ref(false);
const formVisible = ref(false);
const ruleList = ref<RuleDefinition[]>([]);

// 查询条件
const filters = reactive({
  ruleName: '',
  metricIdentifier: '',
  alarmLevel: '',
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
  metricIdentifier: '',
  metricName: '',
  expression: '',
  duration: 0,
  alarmLevel: 'info',
  notificationMethods: [] as string[],
  convertToEvent: 0,
  status: 0,
  remark: ''
});

const rules = {
  ruleName: [{ required: true, message: '请输入规则名�?, trigger: 'blur' }],
  metricIdentifier: [{ required: true, message: '请输入测点标识符', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  alarmLevel: [{ required: true, message: '请选择告警等级', trigger: 'change' }]
};

const submitLoading = ref(false);

// 获取告警等级类型
const getAlarmLevelType = (level: string) => {
  switch (level) {
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

// 获取告警等级文本
const getAlarmLevelText = (level: string) => {
  switch (level) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return level;
  }
};

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
      metricIdentifier: filters.metricIdentifier || undefined,
      alarmLevel: filters.alarmLevel || undefined,
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
  filters.metricIdentifier = '';
  filters.alarmLevel = '';
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
  form.metricIdentifier = '';
  form.metricName = '';
  form.expression = '';
  form.duration = 0;
  form.alarmLevel = 'info';
  form.notificationMethods = [];
  form.convertToEvent = 0;
  form.status = 0;
  form.remark = '';
  formVisible.value = true;
};

// 编辑规则
const handleEdit = (row: RuleDefinition) => {
  form.id = row.id;
  form.ruleName = row.ruleName;
  form.metricIdentifier = row.metricIdentifier;
  form.metricName = row.metricName;
  form.expression = row.expression;
  form.duration = row.duration;
  form.alarmLevel = row.alarmLevel;
  form.notificationMethods = row.notificationMethods ? row.notificationMethods.split(',') : [];
  form.convertToEvent = row.convertToEvent;
  form.status = row.status;
  form.remark = row.remark || '';
  formVisible.value = true;
};

// 删除规则
const handleDelete = async (row: RuleDefinition) => {
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
    const formData = {
      ...form,
      notificationMethods: form.notificationMethods.length > 0 ? form.notificationMethods.join(',') : undefined
    };
    const res = form.id ? await updateRule(formData) : await addRule(formData);
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
.rule-definition-view {
  padding: 20px;
}

.rule-definition-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.rule-definition-header h1 {
  font-size: 24px;
  margin: 0;
}

.rule-definition-filters {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.rule-definition-list {
  margin-bottom: 20px;
}

.rule-definition-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>

