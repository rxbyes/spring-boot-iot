<template>
  <StandardPageShell class="emergency-plan-view">
    <StandardWorkbenchPanel
      title="应急预案库"
      :description="`当前 ${pagination.total} 条应急预案，支持按告警等级和状态治理。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" @click="handleAdd">新增预案</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.planName" placeholder="预案名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.alarmLevel" placeholder="适用告警等级" clearable>
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
        <el-alert
          :title="planAdvice"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `红色 ${redCount} 项`, `橙色 ${orangeCount} 项`]"
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
        element-loading-text="正在刷新应急预案列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`plan-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`plan-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="planList"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="planName" label="预案名称" :min-width="180" />
            <StandardTableTextColumn prop="alarmLevel" label="适用告警等级" :width="120">
              <template #default="{ row }">
                <el-tag :type="getAlarmLevelType(row.alarmLevel || row.riskLevel)" round>
                  {{ getAlarmLevelText(row.alarmLevel || row.riskLevel) }}
                </el-tag>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="description" label="描述" :min-width="220" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
            <el-table-column
              label="操作"
              :width="planActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getPlanRowActions()"
                  @command="(command) => handlePlanRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="add" @click="handleAdd">新增预案</StandardButton>
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
      subtitle="统一通过右侧抽屉维护应急预案与响应步骤。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>维护提示</strong>
          <span>建议先明确适用告警等级和执行状态，再补齐响应步骤与联系人列表，确保应急场景下能够直接复用预案。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>预案基础信息</h3>
                <p>统一维护预案名称、适用告警等级和启停状态，保证不同等级告警场景下快速选用对应预案。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="预案名称" prop="planName">
                <el-input v-model="form.planName" placeholder="请输入预案名称" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="适用告警等级" prop="alarmLevel" class="ops-drawer-grid__full">
                <el-radio-group v-model="form.alarmLevel">
                  <el-radio
                    v-for="option in alarmLevelOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入预案描述、适用范围或启动说明" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>响应编排</h3>
                <p>统一维护步骤清单和联系人列表，便于在处置过程中直接按预案执行并找到对应责任人。</p>
              </div>
            </div>
            <div class="ops-drawer-grid ops-drawer-grid--single">
              <el-form-item label="响应步骤" prop="responseSteps">
                <el-input v-model="form.responseSteps" type="textarea" :rows="6" placeholder="请输入响应步骤（JSON格式）" />
              </el-form-item>
              <el-form-item label="联系人列表" prop="contactList">
                <el-input v-model="form.contactList" type="textarea" :rows="4" placeholder="请输入联系人列表（JSON格式）" />
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
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import {
  fetchAlarmLevelOptions,
  getAlarmLevelTagType,
  getAlarmLevelText,
  normalizeAlarmLevel,
  type AlarmLevelOption
} from '@/utils/alarmLevel';
import { pagePlanList, addPlan, updatePlan, deletePlan } from '../api/emergencyPlan';
import type { EmergencyPlan } from '../api/emergencyPlan';

type EmergencyPlanRowActionCommand = 'edit' | 'delete';

const loading = ref(false);
const formVisible = ref(false);
const planList = ref<EmergencyPlan[]>([]);
const alarmLevelOptions = ref<AlarmLevelOption[]>([]);
const tableRef = ref();
const selectedRows = ref<EmergencyPlan[]>([]);
const planActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'edit', label: '编辑' },
    { command: 'delete', label: '删除' }
  ],
});

const filters = reactive({
  planName: '',
  alarmLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  planName: '',
  alarmLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑预案' : '新增预案'));
const form = reactive({
  id: undefined as number | undefined,
  planName: '',
  alarmLevel: '',
  description: '',
  responseSteps: '',
  contactList: '',
  status: 0
});

const rules = {
  planName: [{ required: true, message: '请输入预案名称', trigger: 'blur' }],
  alarmLevel: [{ required: true, message: '请选择适用告警等级', trigger: 'change' }],
  responseSteps: [{ required: true, message: '请输入响应步骤', trigger: 'blur' }]
};

const submitLoading = ref(false);
const planAdvice = '优先检查红色告警预案和启用中的执行方案';
let latestListRequestId = 0;

const enabledCount = computed(() => planList.value.filter((item) => item.status === 0).length);
const redCount = computed(() => planList.value.filter((item) => normalizeAlarmLevel(item.alarmLevel || item.riskLevel) === 'red').length);
const orangeCount = computed(() => planList.value.filter((item) => normalizeAlarmLevel(item.alarmLevel || item.riskLevel) === 'orange').length);
const hasRecords = computed(() => planList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的应急预案' : '还没有应急预案'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有应急预案，先新增预案，再继续配置告警等级和响应步骤。'
);

const getAlarmLevelType = (alarmLevel: string) => getAlarmLevelTagType(alarmLevel);

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
    { key: 'planName', label: '预案名称' },
    { key: 'alarmLevel', label: (value) => `适用告警等级：${getAlarmLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    planName: '',
    alarmLevel: '',
    status: '' as '' | number
  }
});

const loadPlanList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const res = await pagePlanList({
      planName: appliedFilters.planName || undefined,
      alarmLevel: appliedFilters.alarmLevel || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (requestId !== latestListRequestId) {
      return;
    }
    if (res.code === 200) {
      planList.value = applyPageResult(res.data);
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    console.error('查询预案列表失败', error);
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
  void loadPlanList();
};

const handleReset = () => {
  filters.planName = '';
  filters.alarmLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadPlanList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadPlanList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadPlanList();
};

const handleSelectionChange = (rows: EmergencyPlan[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadPlanList();
};

function getPlanRowActions() {
  return [
    { command: 'edit' as const, label: '编辑' },
    { command: 'delete' as const, label: '删除' }
  ];
}

function handlePlanRowAction(command: EmergencyPlanRowActionCommand, row: EmergencyPlan) {
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
  void loadPlanList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const resetPlanForm = () => {
  form.id = undefined;
  form.planName = '';
  form.alarmLevel = alarmLevelOptions.value[0]?.value || '';
  form.description = '';
  form.responseSteps = '';
  form.contactList = '';
  form.status = 0;
};

const handleAdd = () => {
  resetPlanForm();
  formVisible.value = true;
};

const handleEdit = (row: EmergencyPlan) => {
  form.id = row.id;
  form.planName = row.planName;
  form.alarmLevel = normalizeAlarmLevel(row.alarmLevel || row.riskLevel);
  form.description = row.description || '';
  form.responseSteps = row.responseSteps || '';
  form.contactList = row.contactList || '';
  form.status = row.status;
  formVisible.value = true;
};

const handleDelete = async (row: EmergencyPlan) => {
  try {
    await confirmDelete('预案', row.planName);
    const res = await deletePlan(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadPlanList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除预案失败', error);
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updatePlan(form) : await addPlan(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadPlanList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetPlanForm();
};

onMounted(() => {
  syncAppliedFilters();
  void loadAlarmLevelOptions();
  void loadPlanList();
});

async function loadAlarmLevelOptions() {
  try {
    alarmLevelOptions.value = await fetchAlarmLevelOptions();
    if (!form.alarmLevel) {
      form.alarmLevel = alarmLevelOptions.value[0]?.value || '';
    }
  } catch (error) {
    console.error('加载告警等级字典失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载告警等级字典失败');
  }
}
</script>

<style scoped>
.emergency-plan-view {
  min-width: 0;
}
</style>
