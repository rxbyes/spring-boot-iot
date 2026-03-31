<template>
  <StandardPageShell class="alarm-center-view">
    <StandardWorkbenchPanel
      title="告警列表"
      :description="`当前 ${pagination.total} 条告警记录，支持确认、抑制、关闭和导出复核。`"
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
              <el-select v-model="filters.alarmLevel" placeholder="告警等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="未确认" :value="0" />
                <el-option label="已确认" :value="1" />
                <el-option label="已抑制" :value="2" />
                <el-option label="已关闭" :value="3" />
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
          title="优先定位待确认和高等级告警，快速聚焦需要立即响应的风险项。"
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
            `今日告警 ${stats.todayAlarms} 项`,
            `未确认 ${stats.unconfirmedAlarms} 项`,
            `已确认 ${stats.confirmedAlarms} 项`,
            `已关闭 ${stats.closedAlarms} 项`,
            `已选 ${selectedRows.length} 项`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            <StandardActionMenu
              label="更多操作"
              :items="alarmToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>

      <div v-if="loading" class="ops-state">正在加载告警列表...</div>
      <div v-else-if="alarmList.length === 0" class="ops-state">暂无符合条件的告警记录</div>
      <template v-else>
        <el-table ref="tableRef" :data="pagedAlarmList" border stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="alarmCode" label="告警编号" :width="180" />
          <StandardTableTextColumn prop="alarmTitle" label="告警标题" :min-width="220" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)" round>{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="regionName" label="区域" :width="120" />
          <StandardTableTextColumn prop="riskPointName" label="风险点" :width="150" />
          <StandardTableTextColumn prop="deviceName" label="设备名称" :width="150" />
          <StandardTableTextColumn prop="metricName" label="测点名称" :width="150" />
          <StandardTableTextColumn prop="currentValue" label="当前值" :width="120" />
          <StandardTableTextColumn prop="thresholdValue" label="阈值" :width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="triggerTime" label="触发时间" :width="180" />
          <el-table-column
            label="操作"
            :width="alarmActionColumnWidth"
            fixed="right"
            class-name="standard-row-actions-column"
            :show-overflow-tooltip="false"
          >
            <template #default="{ row }">
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="getAlarmDirectActions(row)"
                @command="(command) => handleAlarmRowAction(command, row)"
              />
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

    <AlarmDetailDrawer
      v-model="detailVisible"
      :detail="detail"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
    />

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="告警运营台导出列设置"
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
import AlarmDetailDrawer from '@/components/AlarmDetailDrawer.vue';
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardActionMenu from '@/components/StandardActionMenu.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { resolveAdaptiveActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv';
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns';
import { confirmAction, isConfirmCancelled } from '@/utils/confirm';

import { closeAlarm, confirmAlarm, getAlarmDetail, getAlarmList, suppressAlarm } from '../api/alarm';
import type { AlarmRecord } from '../api/alarm';

type AlarmRowActionCommand = 'detail' | 'confirm' | 'suppress' | 'close';

const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailErrorMessage = ref('');
const alarmList = ref<AlarmRecord[]>([]);
const detail = ref<AlarmRecord | null>(null);
const tableRef = ref();
const selectedRows = ref<AlarmRecord[]>([]);
const exportColumns: CsvColumn<AlarmRecord>[] = [
  { key: 'alarmCode', label: '告警编号' },
  { key: 'alarmTitle', label: '告警标题' },
  { key: 'alarmLevel', label: '告警等级', formatter: (value) => getAlarmLevelText(String(value || '')) },
  { key: 'regionName', label: '区域' },
  { key: 'riskPointName', label: '风险点' },
  { key: 'deviceName', label: '设备名称' },
  { key: 'metricName', label: '测点名称' },
  { key: 'currentValue', label: '当前值' },
  { key: 'thresholdValue', label: '阈值' },
  { key: 'status', label: '状态', formatter: (value) => getStatusText(Number(value)) },
  { key: 'triggerTime', label: '触发时间' }
];
const exportColumnStorageKey = 'alarm-center-view';
const exportColumnOptions = toCsvColumnOptions(exportColumns);
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  {
    label: '运维模板',
    keys: ['alarmCode', 'alarmTitle', 'alarmLevel', 'deviceName', 'metricName', 'currentValue', 'thresholdValue', 'status', 'triggerTime']
  },
  { label: '管理模板', keys: ['alarmCode', 'alarmTitle', 'alarmLevel', 'regionName', 'riskPointName', 'status', 'triggerTime'] }
];
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
);
const exportColumnDialogVisible = ref(false);
const alarmActionColumnWidth = resolveAdaptiveActionColumnWidth({
  directLabels: ['详情', '确认', '抑制', '关闭'],
  gap: 'compact'
});
const alarmToolbarActions = computed(() => [
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
    disabled: alarmList.value.length === 0
  },
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  }
]);

const stats = ref({
  todayAlarms: 0,
  unconfirmedAlarms: 0,
  confirmedAlarms: 0,
  closedAlarms: 0
});

const filters = reactive({
  deviceCode: '',
  alarmLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  deviceCode: '',
  alarmLevel: '',
  status: '' as '' | number
});

const { pagination, applyLocalRecords, resetPage, setPageSize, setPageNum, setTotal } = useServerPagination();
const pagedAlarmList = computed(() => applyLocalRecords(alarmList.value));

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
      return 'danger';
    case 1:
      return 'success';
    case 2:
    case 3:
      return 'info';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '未确认';
    case 1:
      return '已确认';
    case 2:
      return '已抑制';
    case 3:
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
    { key: 'alarmLevel', label: (value) => `告警等级：${getAlarmLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    deviceCode: '',
    alarmLevel: '',
    status: '' as '' | number
  }
});

const loadAlarmList = async () => {
  loading.value = true;
  try {
    const statusValue = appliedFilters.status === '' ? undefined : Number(appliedFilters.status);
    const normalizedStatus = typeof statusValue === 'number' && Number.isFinite(statusValue) ? statusValue : undefined;
    const res = await getAlarmList({
      deviceCode: appliedFilters.deviceCode || undefined,
      alarmLevel: appliedFilters.alarmLevel || undefined,
      status: normalizedStatus
    });

    if (res.code === 200) {
      alarmList.value = res.data || [];
      setTotal(alarmList.value.length);
      stats.value.todayAlarms = alarmList.value.length;
      stats.value.unconfirmedAlarms = alarmList.value.filter((a) => a.status === 0).length;
      stats.value.confirmedAlarms = alarmList.value.filter((a) => a.status === 1).length;
      stats.value.closedAlarms = alarmList.value.filter((a) => a.status === 3).length;
    }
  } catch (error) {
    console.error('查询告警列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadAlarmList();
};

const handleReset = () => {
  filters.deviceCode = '';
  filters.alarmLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadAlarmList();
};

const handleSelectionChange = (rows: AlarmRecord[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
};

const getAlarmDirectActions = (row: AlarmRecord) => {
  const actions: Array<{ key: AlarmRowActionCommand; command: AlarmRowActionCommand; label: string }> = [
    {
      key: 'detail',
      command: 'detail',
      label: '详情'
    }
  ];

  if (row.status === 0) {
    actions.push(
      {
        key: 'confirm',
        command: 'confirm',
        label: '确认'
      },
      {
        key: 'suppress',
        command: 'suppress',
        label: '抑制'
      }
    );
  }

  if (row.status !== 3) {
    actions.push({
      key: 'close',
      command: 'close',
      label: '关闭'
    });
  }

  return actions;
};

const handleAlarmRowAction = (command: AlarmRowActionCommand, row: AlarmRecord) => {
  switch (command) {
    case 'detail':
      void handleViewDetail(row);
      break;
    case 'confirm':
      void handleConfirm(row);
      break;
    case 'suppress':
      void handleSuppress(row);
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
  void loadAlarmList();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadAlarmList();
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
  downloadRowsAsCsv('告警运营台-选中项.csv', selectedRows.value, getResolvedExportColumns());
};

const handleExportCurrent = () => {
  downloadRowsAsCsv('告警运营台-当前结果.csv', alarmList.value, getResolvedExportColumns());
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

const handleViewDetail = async (row: AlarmRecord) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detailErrorMessage.value = '';
  detail.value = row;
  try {
    const res = await getAlarmDetail(row.id);
    if (res.code === 200) {
      detail.value = res.data || row;
    }
  } catch (error) {
    detailErrorMessage.value = error instanceof Error ? error.message : '查询告警详情失败';
    console.error('查询告警详情失败', error);
  } finally {
    detailLoading.value = false;
  }
};

const handleConfirm = async (row: AlarmRecord) => {
  try {
    await confirmAction({
      title: '确认告警',
      message: '确认该告警后，将进入已确认跟踪状态。',
      type: 'warning',
      confirmButtonText: '确认告警'
    });
    const res = await confirmAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('确认成功');
      void loadAlarmList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('确认告警失败', error);
  }
};

const handleSuppress = async (row: AlarmRecord) => {
  try {
    await confirmAction({
      title: '抑制告警',
      message: '确认抑制该告警吗？抑制后将暂停当前告警继续触发。',
      type: 'warning',
      confirmButtonText: '确认抑制'
    });
    const res = await suppressAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('抑制成功');
      void loadAlarmList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('抑制告警失败', error);
  }
};

const handleClose = async (row: AlarmRecord) => {
  try {
    await confirmAction({
      title: '关闭告警',
      message: '确认关闭该告警吗？关闭后将结束当前告警处置流程。',
      type: 'warning',
      confirmButtonText: '确认关闭'
    });
    const res = await closeAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      void loadAlarmList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('关闭告警失败', error);
  }
};

onMounted(() => {
  syncAppliedFilters();
  void loadAlarmList();
});

watch(detailVisible, (visible) => {
  if (!visible) {
    detail.value = null;
    detailLoading.value = false;
    detailErrorMessage.value = '';
  }
});
</script>

<style scoped>
.alarm-center-view {
  min-width: 0;
}
</style>
