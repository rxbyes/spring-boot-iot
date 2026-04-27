<template>
  <StandardPageShell class="event-disposal-view">
    <StandardWorkbenchPanel
      title="事件列表"
      :description="`当前 ${pagination.total} 条事件记录，支持派发、关闭和导出复核。`"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.deviceCode" placeholder="设备编码" clearable />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable>
                <el-option
                  v-for="option in riskLevelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="待派发" :value="0" />
                <el-option label="已派发" :value="1" />
                <el-option label="处理中" :value="2" />
                <el-option label="待验收" :value="3" />
                <el-option label="已关闭" :value="4" />
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
          title="优先关注待派发和处理中事件，快速定位仍在闭环中的风险事项。"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `待派发 ${stats.pendingEvents} 项`,
            `已派发 ${stats.dispatchedEvents} 项`,
            `处理中 ${stats.processingEvents} 项`,
            `已关闭 ${stats.closedEvents} 项`,
            `已选 ${selectedRows.length} 项`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            <StandardActionMenu
              label="更多操作"
              :items="eventToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新事件列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`event-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`event-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table ref="tableRef" :data="pagedEventList" border stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn
              prop="eventTitle"
              label="事件"
              secondary-prop="eventCode"
              :min-width="240"
            />
            <el-table-column prop="riskLevel" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getRiskLevelType(row.riskLevel)" round>{{ getRiskLevelText(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="regionName" label="区域" :width="120" />
            <StandardTableTextColumn prop="riskPointName" label="风险点" :width="150" />
            <StandardTableTextColumn prop="deviceName" label="设备名称" :width="150" />
            <StandardTableTextColumn prop="metricName" label="测点名称" :width="150" />
            <StandardTableTextColumn prop="currentValue" label="当前值" :width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="triggerTime" label="触发时间" :width="180" />
            <el-table-column
              label="操作"
              :width="eventActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getEventRowActions(row)"
                  @command="(command) => handleEventRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="refresh" @click="handleRefresh">刷新列表</StandardButton>
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

    <EventDetailDrawer
      v-model="detailVisible"
      :detail="detail"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
    />

    <StandardFormDrawer
      v-model="dispatchVisible"
      title="工单派发"
      subtitle="配置派发对象与处理时限。"
      size="34rem"
      @close="closeDispatchDialog"
    >
      <el-form :model="dispatchForm" label-position="left" class="event-drawer-form">
        <el-form-item label="派发人">
          <el-input v-model="dispatchForm.dispatchUserName" disabled />
        </el-form-item>
        <el-form-item label="接收人">
          <el-select v-model="dispatchForm.receiveUser" placeholder="请选择接收人" style="width: 100%">
            <el-option label="张三" :value="1" />
            <el-option label="李四" :value="2" />
            <el-option label="王五" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="到场时限(小时)">
          <el-input-number v-model="dispatchForm.arrivalTimeLimit" :min="1" :max="24" />
        </el-form-item>
        <el-form-item label="完成时限(小时)">
          <el-input-number v-model="dispatchForm.completionTimeLimit" :min="1" :max="72" />
        </el-form-item>
      </el-form>
      <template #footer>
        <StandardDrawerFooter
          confirm-text="确认派发"
          @cancel="closeDispatchDialog"
          @confirm="handleDispatchConfirm"
        />
      </template>
    </StandardFormDrawer>

    <StandardFormDrawer
      v-model="closeVisible"
      title="事件关闭"
      subtitle="填写关闭原因并完成事件收口。"
      size="34rem"
      @close="closeCloseDialog"
    >
      <el-form :model="closeForm" label-position="left" class="event-drawer-form">
        <el-form-item label="关闭原因">
          <el-input v-model="closeForm.closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <StandardDrawerFooter
          confirm-text="确认关闭"
          confirm-type="danger"
          danger
          @cancel="closeCloseDialog"
          @confirm="handleCloseConfirm"
        />
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="事件协同台导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue';
import EmptyState from '@/components/EmptyState.vue';
import EventDetailDrawer from '@/components/EventDetailDrawer.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardActionMenu from '@/components/StandardActionMenu.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { resolveWorkbenchActionColumnWidthByRows } from '@/utils/adaptiveActionColumn';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv';
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns';
import { confirmAction, isConfirmCancelled } from '@/utils/confirm';
import { fetchRiskLevelOptions, getRiskLevelTagType, getRiskLevelText, type RiskLevelOption } from '@/utils/riskLevel';

import { closeEvent, dispatchEvent, getEventDetail, getEventList } from '../api/alarm';
import type { EventRecord } from '../api/alarm';

type EventRowActionCommand = 'detail' | 'dispatch' | 'close';

const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailErrorMessage = ref('');
const dispatchVisible = ref(false);
const closeVisible = ref(false);
const eventList = ref<EventRecord[]>([]);
const detail = ref<EventRecord | null>(null);
const riskLevelOptions = ref<RiskLevelOption[]>([]);
const dispatchTarget = ref<EventRecord | null>(null);
const closeTarget = ref<EventRecord | null>(null);
const tableRef = ref();
const selectedRows = ref<EventRecord[]>([]);
const exportColumns: CsvColumn<EventRecord>[] = [
  { key: 'eventCode', label: '事件编号' },
  { key: 'eventTitle', label: '事件标题' },
  { key: 'riskLevel', label: '风险等级', formatter: (value) => getRiskLevelText(String(value || '')) },
  { key: 'regionName', label: '区域' },
  { key: 'riskPointName', label: '风险点' },
  { key: 'deviceName', label: '设备名称' },
  { key: 'metricName', label: '测点名称' },
  { key: 'currentValue', label: '当前值' },
  { key: 'status', label: '状态', formatter: (value) => getStatusText(Number(value)) },
  { key: 'triggerTime', label: '触发时间' }
];
const exportColumnStorageKey = 'event-disposal-view';
const exportColumnOptions = toCsvColumnOptions(exportColumns);
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  {
    label: '运维模板',
    keys: ['eventCode', 'eventTitle', 'riskLevel', 'deviceName', 'metricName', 'currentValue', 'status', 'triggerTime']
  },
  { label: '管理模板', keys: ['eventCode', 'eventTitle', 'riskLevel', 'regionName', 'riskPointName', 'status', 'triggerTime'] }
];
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
);
const exportColumnDialogVisible = ref(false);
let latestListRequestId = 0;
const eventActionColumnWidth = computed(() =>
  resolveWorkbenchActionColumnWidthByRows({
    rows: pagedEventList.value.map((row) => ({
      directItems: getEventRowActions(row)
    })),
    fallback: {
      directItems: [
        { command: 'detail', label: '详情' },
        { command: 'dispatch', label: '派发' },
        { command: 'close', label: '关闭' }
      ]
    }
  })
);
const eventToolbarActions = computed(() => [
  {
    key: 'export-config',
    command: 'export-config',
    label: '导出列设置'
  },
  {
    key: 'export-selected',
    command: 'export-selected',
    label: '导出选中',
    disabled: selectedRows.value.length === 0
  },
  {
    key: 'export-current',
    command: 'export-current',
    label: '导出当前结果',
    disabled: eventList.value.length === 0
  },
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  }
]);

const stats = ref({
  pendingEvents: 0,
  dispatchedEvents: 0,
  processingEvents: 0,
  closedEvents: 0
});

const filters = reactive({
  deviceCode: '',
  riskLevel: '',
  status: ''
});
const appliedFilters = reactive({
  deviceCode: '',
  riskLevel: '',
  status: ''
});

const { pagination, applyLocalRecords, resetPage, setPageSize, setPageNum, setTotal } = useServerPagination();
const pagedEventList = computed(() => applyLocalRecords(eventList.value));
const hasRecords = computed(() => eventList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的事件记录' : '当前还没有事件记录'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整筛选条件，或者直接清空当前筛选。'
    : '当前还没有事件记录，建议先刷新列表，或检查告警转事件和闭环处置链路是否正常。'
);

const dispatchForm = reactive({
  dispatchUserName: '系统管理员',
  receiveUser: 1,
  arrivalTimeLimit: 2,
  completionTimeLimit: 24
});

const closeForm = reactive({
  closeReason: ''
});

const getRiskLevelType = (level: string) => getRiskLevelTagType(level);

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'danger';
    case 1:
      return 'warning';
    case 2:
      return 'primary';
    case 3:
      return 'info';
    case 4:
      return 'success';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '待派发';
    case 1:
      return '已派发';
    case 2:
      return '处理中';
    case 3:
      return '待验收';
    case 4:
      return '已关闭';
    default:
      return String(status);
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
    { key: 'deviceCode', label: '设备编码' },
    { key: 'riskLevel', label: (value) => `风险等级：${getRiskLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' }
  ],
  defaults: {
    deviceCode: '',
    riskLevel: '',
    status: ''
  }
});

const loadEventList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const params: { deviceCode?: string; riskLevel?: string; status?: number } = {};
    if (appliedFilters.deviceCode) params.deviceCode = appliedFilters.deviceCode;
    if (appliedFilters.riskLevel) params.riskLevel = appliedFilters.riskLevel;
    if (appliedFilters.status) params.status = parseInt(appliedFilters.status, 10);

    const res = await getEventList(params);
    if (requestId !== latestListRequestId) {
      return;
    }
    if (res.code === 200) {
      eventList.value = res.data || [];
      setTotal(eventList.value.length);
      stats.value.pendingEvents = eventList.value.filter((e) => e.status === 0).length;
      stats.value.dispatchedEvents = eventList.value.filter((e) => e.status === 1).length;
      stats.value.processingEvents = eventList.value.filter((e) => e.status === 2).length;
      stats.value.closedEvents = eventList.value.filter((e) => e.status === 4).length;
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    eventList.value = [];
    setTotal(0);
    stats.value.pendingEvents = 0;
    stats.value.dispatchedEvents = 0;
    stats.value.processingEvents = 0;
    stats.value.closedEvents = 0;
    console.error('查询事件列表失败', error);
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
  void loadEventList();
};

const handleReset = () => {
  filters.deviceCode = '';
  filters.riskLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadEventList();
};

const handleSelectionChange = (rows: EventRecord[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
};

const getEventRowActions = (row: EventRecord) => {
  const actions: Array<{ command: EventRowActionCommand; label: string }> = [{ command: 'detail', label: '详情' }];
  if (row.status === 0) {
    actions.push({ command: 'dispatch', label: '派发' });
  }
  if (row.status !== 4) {
    actions.push({ command: 'close', label: '关闭' });
  }
  return actions;
};

const handleEventRowAction = (command: EventRowActionCommand, row: EventRecord) => {
  switch (command) {
    case 'detail':
      void handleViewDetail(row);
      break;
    case 'dispatch':
      void handleDispatch(row);
      break;
    case 'close':
      void handleClose(row);
      break;
    default:
      break;
  }
};

const handleRefresh = () => {
  clearSelection();
  void loadEventList();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadEventList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true;
};

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys;
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys);
};

const getResolvedExportColumns = () => resolveCsvColumns(exportColumns, selectedExportColumnKeys.value);

const handleExportSelected = () => {
  downloadRowsAsCsv('事件协同台-选中项.csv', selectedRows.value, getResolvedExportColumns());
};

const handleExportCurrent = () => {
  downloadRowsAsCsv('事件协同台-当前结果.csv', eventList.value, getResolvedExportColumns());
};

const handleToolbarAction = (command: string | number | object) => {
  switch (command) {
    case 'export-config':
      openExportColumnSetting();
      break;
    case 'export-selected':
      handleExportSelected();
      break;
    case 'export-current':
      handleExportCurrent();
      break;
    case 'clear-selection':
      clearSelection();
      break;
    default:
      break;
  }
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
};

const handlePageChange = (page: number) => {
  setPageNum(page);
};

const handleViewDetail = async (row: EventRecord) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detailErrorMessage.value = '';
  detail.value = row;
  try {
    const res = await getEventDetail(row.id);
    if (res.code === 200) {
      detail.value = res.data || row;
    }
  } catch (error) {
    detailErrorMessage.value = error instanceof Error ? error.message : '查询事件详情失败';
    console.error('查询事件详情失败', error);
  } finally {
    detailLoading.value = false;
  }
};

const handleDispatch = (row: EventRecord) => {
  dispatchTarget.value = row;
  dispatchVisible.value = true;
};

const handleDispatchConfirm = async () => {
  if (!dispatchTarget.value) return;
  try {
    const res = await dispatchEvent(dispatchTarget.value.id, 1, dispatchForm.receiveUser);
    if (res.code === 200) {
      ElMessage.success('派发成功');
      closeDispatchDialog();
      void loadEventList();
    }
  } catch (error) {
    console.error('派发事件失败', error);
  }
};

const handleClose = (row: EventRecord) => {
  closeTarget.value = row;
  closeVisible.value = true;
};

const handleCloseConfirm = async () => {
  if (!closeTarget.value) return;
  if (!closeForm.closeReason) {
    ElMessage.warning('请输入关闭原因');
    return;
  }
  try {
    await confirmAction({
      title: '关闭事件',
      message: '确认关闭该事件吗？关闭后将结束当前处置流程。',
      type: 'warning',
      confirmButtonText: '确认关闭'
    });
    const res = await closeEvent(closeTarget.value.id, 1, closeForm.closeReason);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      closeCloseDialog();
      void loadEventList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('关闭事件失败', error);
  }
};

onMounted(() => {
  syncAppliedFilters();
  void loadRiskLevelOptions();
  void loadEventList();
});

async function loadRiskLevelOptions() {
  try {
    riskLevelOptions.value = await fetchRiskLevelOptions();
  } catch (error) {
    console.error('加载风险等级字典失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载风险等级字典失败');
  }
}

function closeDispatchDialog() {
  dispatchVisible.value = false;
  dispatchTarget.value = null;
}

function closeCloseDialog() {
  closeVisible.value = false;
  closeTarget.value = null;
  closeForm.closeReason = '';
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detail.value = null;
    detailLoading.value = false;
    detailErrorMessage.value = '';
  }
});
</script>

<style scoped>
.event-disposal-view {
  min-width: 0;
}

.event-drawer-form :deep(.el-select),
.event-drawer-form :deep(.el-input-number) {
  width: 100%;
}
</style>
