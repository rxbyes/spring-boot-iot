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
                <el-option
                  v-for="option in alarmLevelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
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
        <div class="rule-definition-notice-stack">
          <el-alert
            title="优先核查红色告警和已开启转事件的规则，确保风险触发策略与处置流程保持一致。"
            type="info"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            v-if="missingPolicyTotal > 0"
            :title="`待配置阈值策略 ${missingPolicyTotal} 项，已绑定测点还没有进入统一判级。`"
            type="warning"
            :closable="false"
            show-icon
            class="view-alert"
          >
            <ul class="rule-definition-governance-list">
              <li v-for="item in missingPolicyItems" :key="`${item.riskPointId || 'rp'}-${item.metricIdentifier || item.deviceCode}`">
                <strong>{{ item.metricName || item.metricIdentifier || '--' }}</strong>
                <span>{{ item.riskPointName || '未命名风险点' }}</span>
                <span>{{ item.deviceCode || '--' }} · {{ item.deviceName || '未命名设备' }}</span>
              </li>
            </ul>
          </el-alert>
        </div>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `转事件 ${convertToEventCount} 项`, `待配 ${missingPolicyTotal} 项`, `红色 ${redRuleCount} 项`]"
        >
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新阈值策略列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`rule-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`rule-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
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
            <el-table-column
              label="操作"
              :width="ruleActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRuleRowActions()"
                  @command="(command) => handleRuleRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="add" @click="handleAdd">新增规则</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
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
                  <el-option
                    v-for="option in alarmLevelOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
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
import { useRoute } from 'vue-router';
import { ElMessage } from '@/utils/message';
import EmptyState from '@/components/EmptyState.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import {
  fetchAlarmLevelOptions,
  getAlarmLevelTagType,
  getAlarmLevelText,
  normalizeAlarmLevel,
  type AlarmLevelOption
} from '@/utils/alarmLevel';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { listMissingPolicies, type RiskGovernanceGapItem } from '@/api/riskGovernance';
import { pageRuleList, addRule, updateRule, deleteRule } from '../api/ruleDefinition';
import type { RuleDefinition } from '../api/ruleDefinition';

type RuleRowActionCommand = 'edit' | 'delete';

const loading = ref(false);
const formVisible = ref(false);
const ruleList = ref<RuleDefinition[]>([]);
const alarmLevelOptions = ref<AlarmLevelOption[]>([]);
const missingPolicyItems = ref<RiskGovernanceGapItem[]>([]);
const tableRef = ref();
const selectedRows = ref<RuleDefinition[]>([]);
const ruleActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'edit', label: '编辑' },
    { command: 'delete', label: '删除' }
  ],
});

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

const route = useRoute();
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑规则' : '新增规则'));
const form = reactive({
  id: undefined as number | undefined,
  riskMetricId: undefined as number | undefined,
  ruleName: '',
  metricIdentifier: '',
  metricName: '',
  expression: '',
  duration: 0,
  alarmLevel: 'blue',
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
const missingPolicyTotal = ref(0);
let latestListRequestId = 0;
let governanceCreateHandled = false;

const enabledCount = computed(() => ruleList.value.filter((item) => item.status === 0).length);
const convertToEventCount = computed(() => ruleList.value.filter((item) => item.convertToEvent === 1).length);
const redRuleCount = computed(() => ruleList.value.filter((item) => normalizeAlarmLevel(item.alarmLevel) === 'red').length);
const hasRecords = computed(() => ruleList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的阈值策略' : '还没有阈值策略'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有阈值策略，先新增规则，再继续告警触发和事件转化治理。'
);

const getAlarmLevelType = (level: string) => getAlarmLevelTagType(level);

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
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const [listResult, governanceResult] = await Promise.allSettled([
      pageRuleList({
        ruleName: appliedFilters.ruleName || undefined,
        metricIdentifier: appliedFilters.metricIdentifier || undefined,
        alarmLevel: appliedFilters.alarmLevel || undefined,
        status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }),
      listMissingPolicies({
        pageNum: 1,
        pageSize: 3
      })
    ]);
    if (requestId !== latestListRequestId) {
      return;
    }
    if (listResult.status === 'fulfilled' && listResult.value.code === 200) {
      ruleList.value = applyPageResult(listResult.value.data);
    } else {
      ruleList.value = [];
    }

    if (governanceResult.status === 'fulfilled' && governanceResult.value.code === 200) {
      missingPolicyItems.value = governanceResult.value.data.records ?? [];
      missingPolicyTotal.value = governanceResult.value.data.total ?? 0;
    } else {
      missingPolicyItems.value = [];
      missingPolicyTotal.value = 0;
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    console.error('查询规则列表失败', error);
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
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

function getRuleRowActions() {
  return [
    { command: 'edit' as const, label: '编辑' },
    { command: 'delete' as const, label: '删除' }
  ];
}

function handleRuleRowAction(command: RuleRowActionCommand, row: RuleDefinition) {
  if (command === 'edit') {
    handleEdit(row);
    return;
  }
  void handleDelete(row);
}

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

function applyRouteQueryToFilters() {
  filters.ruleName = parseRouteStringQuery(route.query.ruleName);
  filters.metricIdentifier = parseRouteStringQuery(route.query.metricIdentifier);
  filters.alarmLevel = parseRouteStringQuery(route.query.alarmLevel);
  filters.status = parseRouteNumberQuery(route.query.status) ?? '';
}

function parseRouteStringQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  return typeof raw === 'string' ? raw.trim() : '';
}

function parseRouteNumberQuery(value: unknown) {
  const text = parseRouteStringQuery(value);
  if (!text) {
    return undefined;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function parseGovernanceCreateContext() {
  if (parseRouteStringQuery(route.query.governanceAction) !== 'create') {
    return null;
  }
  if (parseRouteStringQuery(route.query.governanceSource) !== 'task') {
    return null;
  }
  if (parseRouteStringQuery(route.query.workItemCode) !== 'PENDING_THRESHOLD_POLICY') {
    return null;
  }
  return {
    riskMetricId: parseRouteNumberQuery(route.query.riskMetricId),
    metricIdentifier: parseRouteStringQuery(route.query.metricIdentifier),
    metricName: parseRouteStringQuery(route.query.metricName)
  };
}

const loadAlarmLevelOptionList = async () => {
  try {
    alarmLevelOptions.value = await fetchAlarmLevelOptions();
    if (!form.alarmLevel) {
      form.alarmLevel = alarmLevelOptions.value[0]?.value || 'blue';
    }
  } catch (error) {
    console.error('加载告警等级字典失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载告警等级字典失败');
  }
};

const resetRuleForm = () => {
  form.id = undefined;
  form.riskMetricId = undefined;
  form.ruleName = '';
  form.metricIdentifier = '';
  form.metricName = '';
  form.expression = '';
  form.duration = 0;
  form.alarmLevel = alarmLevelOptions.value[0]?.value || 'blue';
  form.notificationMethods = [];
  form.convertToEvent = 0;
  form.status = 0;
  form.remark = '';
};

const handleAdd = () => {
  resetRuleForm();
  formVisible.value = true;
};

function applyGovernanceCreateContext() {
  if (governanceCreateHandled) {
    return;
  }
  const context = parseGovernanceCreateContext();
  if (!context) {
    return;
  }
  governanceCreateHandled = true;
  handleAdd();
  form.riskMetricId = context.riskMetricId;
  form.metricIdentifier = context.metricIdentifier;
  form.metricName = context.metricName;
}

const handleEdit = (row: RuleDefinition) => {
  form.id = row.id;
  form.riskMetricId = row.riskMetricId == null ? undefined : Number(row.riskMetricId);
  form.ruleName = row.ruleName;
  form.metricIdentifier = row.metricIdentifier;
  form.metricName = row.metricName;
  form.expression = row.expression;
  form.duration = row.duration;
  form.alarmLevel = normalizeAlarmLevel(row.alarmLevel) || alarmLevelOptions.value[0]?.value || 'blue';
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
  applyRouteQueryToFilters();
  syncAppliedFilters();
  applyGovernanceCreateContext();
  void loadAlarmLevelOptionList();
  void loadRuleList();
});
</script>

<style scoped>
.rule-definition-view {
  min-width: 0;
}

.rule-definition-notice-stack,
.rule-definition-governance-list {
  display: grid;
  gap: 0.75rem;
}

.rule-definition-governance-list {
  margin: 0;
  padding-left: 1rem;
}

.rule-definition-governance-list li {
  display: grid;
  gap: 0.15rem;
  color: var(--text-secondary);
}

.rule-definition-governance-list strong {
  color: var(--text-primary);
}
</style>
