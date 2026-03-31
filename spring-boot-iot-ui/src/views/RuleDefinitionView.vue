<template>
  <StandardPageShell class="rule-definition-view">
    <StandardWorkbenchPanel
      title="阈值策略"
      :description="`当前 ${pagination.total} 条阈值策略，支持告警触发和转事件配置。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" @click="handleAdd">新增规则</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.ruleName" placeholder="规则名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.metricIdentifier" placeholder="测点标识符" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.alarmLevel" placeholder="告警等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #notices>
        <el-alert
          title="优先核查严重告警和已开启转事件的规则，确保风险触发策略与处置流程保持一致。"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `转事件 ${convertToEventCount} 项`, `严重 ${criticalRuleCount} 项`]"
        >
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div v-if="loading" class="ops-state">正在加载阈值策略列表...</div>
      <div v-else-if="ruleList.length === 0" class="ops-state">暂无符合条件的阈值策略</div>
      <template v-else>
        <el-table
          ref="tableRef"
          :data="ruleList"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="ruleName" label="规则名称" :min-width="180" />
          <StandardTableTextColumn prop="metricIdentifier" label="测点标识符" :width="160" />
          <StandardTableTextColumn prop="metricName" label="测点名称" :width="140" />
          <StandardTableTextColumn prop="expression" label="表达式" :min-width="220" />
          <StandardTableTextColumn prop="duration" label="持续时间(秒)" :width="120" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)" round>{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="convertToEvent" label="转事件" width="100">
            <template #default="{ row }">
              <el-tag :type="row.convertToEvent === 1 ? 'success' : 'info'" round>
                {{ row.convertToEvent === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
            <StandardRowActions variant="table" gap="compact">
                <StandardActionLink @click="handleEdit(row)">编辑</StandardActionLink>
                <StandardActionLink @click="handleDelete(row)">删除</StandardActionLink>
              </StandardRowActions>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #pagination>
        <div class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护阈值策略与告警配置。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>配置提示</strong>
          <span>建议先确认测点标识、阈值表达式和持续时间，再决定是否转事件，以保持告警触发和处置链路的一致性。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>规则基础信息</h3>
                <p>先确认规则名称、测点标识和阈值表达式，保证同类策略具备清晰可读的维护口径。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="规则名称" prop="ruleName">
                <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
              </el-form-item>
              <el-form-item label="测点标识符" prop="metricIdentifier">
                <el-input v-model="form.metricIdentifier" placeholder="请输入测点标识符" />
              </el-form-item>
              <el-form-item label="测点名称" prop="metricName">
                <el-input v-model="form.metricName" placeholder="请输入测点名称" />
              </el-form-item>
              <el-form-item label="表达式" prop="expression">
                <el-input v-model="form.expression" placeholder="例如：value > 100" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>触发策略</h3>
                <p>统一配置触发持续时间、告警等级与转事件开关，确保告警策略与处置闭环匹配。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="持续时间(秒)" prop="duration">
                <el-input-number v-model="form.duration" :min="0" :max="3600" placeholder="请输入持续时间" />
              </el-form-item>
              <el-form-item label="告警等级" prop="alarmLevel">
                <el-select v-model="form.alarmLevel" placeholder="请选择告警等级">
                  <el-option label="严重" value="critical" />
                  <el-option label="警告" value="warning" />
                  <el-option label="提醒" value="info" />
                </el-select>
              </el-form-item>
              <el-form-item label="转事件">
                <el-radio-group v-model="form.convertToEvent">
                  <el-radio :value="0">否</el-radio>
                  <el-radio :value="1">是</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>通知与说明</h3>
                <p>维护通知通道和补充说明，便于后续排障、回顾和跨岗位协同。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="通知方式" class="ops-drawer-grid__full">
                <el-checkbox-group v-model="form.notificationMethods">
                  <el-checkbox label="email">邮件</el-checkbox>
                  <el-checkbox label="sms">短信</el-checkbox>
                  <el-checkbox label="wechat">微信</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="描述" prop="remark" class="ops-drawer-grid__full">
                <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="请输入规则说明、适用范围或维护备注" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        />
      </template>
    </StandardFormDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { pageRuleList, addRule, updateRule, deleteRule } from '../api/ruleDefinition';
import type { RuleDefinition } from '../api/ruleDefinition';

const loading = ref(false);
const formVisible = ref(false);
const ruleList = ref<RuleDefinition[]>([]);
const tableRef = ref();
const selectedRows = ref<RuleDefinition[]>([]);

const filters = reactive({
  ruleName: '',
  metricIdentifier: '',
  alarmLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  ruleName: '',
  metricIdentifier: '',
  alarmLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑规则' : '新增规则'));
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
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metricIdentifier: [{ required: true, message: '请输入测点标识符', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  alarmLevel: [{ required: true, message: '请选择告警等级', trigger: 'change' }]
};

const submitLoading = ref(false);

const enabledCount = computed(() => ruleList.value.filter((item) => item.status === 0).length);
const convertToEventCount = computed(() => ruleList.value.filter((item) => item.convertToEvent === 1).length);
const criticalRuleCount = computed(() => ruleList.value.filter((item) => item.alarmLevel === 'critical').length);

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

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'ruleName', label: '规则名称' },
    { key: 'metricIdentifier', label: '测点标识符' },
    { key: 'alarmLevel', label: (value) => `告警等级：${getAlarmLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    ruleName: '',
    metricIdentifier: '',
    alarmLevel: '',
    status: '' as '' | number
  }
});

const loadRuleList = async () => {
  loading.value = true;
  try {
    const res = await pageRuleList({
      ruleName: appliedFilters.ruleName || undefined,
      metricIdentifier: appliedFilters.metricIdentifier || undefined,
      alarmLevel: appliedFilters.alarmLevel || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (res.code === 200) {
      ruleList.value = applyPageResult(res.data);
    }
  } catch (error) {
    console.error('查询规则列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleReset = () => {
  filters.ruleName = '';
  filters.metricIdentifier = '';
  filters.alarmLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRuleList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRuleList();
};

const handleSelectionChange = (rows: RuleDefinition[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRuleList();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const resetRuleForm = () => {
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
};

const handleAdd = () => {
  resetRuleForm();
  formVisible.value = true;
};

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

const handleDelete = async (row: RuleDefinition) => {
  try {
    await confirmDelete('规则', row.ruleName);
    const res = await deleteRule(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRuleList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除规则失败', error);
  }
};

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
      void loadRuleList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetRuleForm();
};

onMounted(() => {
  syncAppliedFilters();
  void loadRuleList();
});
</script>

<style scoped>
.rule-definition-view {
  min-width: 0;
}
</style>
