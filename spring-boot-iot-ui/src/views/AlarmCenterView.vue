<template>
  <div class="alarm-center-view">
    <div class="alarm-header">
      <h1>告警中心</h1>
      <div class="alarm-stats">
        <el-statistic title="今日告警" :value="stats.todayAlarms" />
        <el-statistic title="未确认告警" :value="stats.unconfirmedAlarms" />
        <el-statistic title="已确认告警" :value="stats.confirmedAlarms" />
        <el-statistic title="已关闭告警" :value="stats.closedAlarms" />
      </div>
    </div>

    <div class="alarm-filters">
      <el-form :model="filters" label-position="left" class="alarm-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" placeholder="请输入设备编码" clearable />
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
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="未确认" :value="0" />
                <el-option label="已确认" :value="1" />
                <el-option label="已抑制" :value="2" />
                <el-option label="已关闭" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="" class="alarm-filter-actions">
              <el-button type="primary" class="alarm-btn alarm-btn--primary" @click="handleSearch">查询</el-button>
              <el-button class="alarm-btn alarm-btn--ghost" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="alarm-list">
      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">已选 {{ selectedRows.length }} 项</span>
        </div>
        <div class="table-action-bar__right">
          <el-button link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button link :disabled="alarmList.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>
      <el-table ref="tableRef" :data="alarmList" v-loading="loading" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="alarmCode" label="告警编号" width="180" />
        <el-table-column prop="alarmTitle" label="告警标题" />
        <el-table-column prop="alarmLevel" label="告警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="区域" width="120" />
        <el-table-column prop="riskPointName" label="风险点" width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="metricName" label="测点名称" width="150" />
        <el-table-column prop="currentValue" label="当前值" width="120" />
        <el-table-column prop="thresholdValue" label="阈值" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="primary" link @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 0" type="primary" link @click="handleSuppress(row)">抑制</el-button>
            <el-button v-if="row.status !== 3" type="primary" link @click="handleClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="alarm-pagination">
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

    <AlarmDetailDrawer
      v-model="detailVisible"
      :detail="detail"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
    />

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="告警中心导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import AlarmDetailDrawer from '@/components/AlarmDetailDrawer.vue';
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue';
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv';
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns';

import { closeAlarm, confirmAlarm, getAlarmDetail, getAlarmList, suppressAlarm } from '../api/alarm';
import type { AlarmRecord } from '../api/alarm';

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

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

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

const loadAlarmList = async () => {
  loading.value = true;
  try {
    const statusValue = filters.status === '' ? undefined : Number(filters.status);
    const normalizedStatus = typeof statusValue === 'number' && Number.isFinite(statusValue) ? statusValue : undefined;
    const res = await getAlarmList({
      deviceCode: filters.deviceCode || undefined,
      alarmLevel: filters.alarmLevel || undefined,
      status: normalizedStatus
    });

    if (res.code === 200) {
      alarmList.value = res.data || [];
      pagination.total = alarmList.value.length;
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
  pagination.page = 1;
  void loadAlarmList();
};

const handleReset = () => {
  filters.deviceCode = '';
  filters.alarmLevel = '';
  filters.status = '';
  pagination.page = 1;
  void loadAlarmList();
};

const handleSelectionChange = (rows: AlarmRecord[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadAlarmList();
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
  downloadRowsAsCsv('告警中心-选中项.csv', selectedRows.value, getResolvedExportColumns());
};

const handleExportCurrent = () => {
  downloadRowsAsCsv('告警中心-当前结果.csv', alarmList.value, getResolvedExportColumns());
};

const handleSizeChange = () => {
  void loadAlarmList();
};

const handlePageChange = () => {
  void loadAlarmList();
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
    await ElMessageBox.confirm('确定要确认该告警吗？', '确认告警', { type: 'warning' });
    const res = await confirmAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('确认成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('确认告警失败', error);
  }
};

const handleSuppress = async (row: AlarmRecord) => {
  try {
    await ElMessageBox.confirm('确定要抑制该告警吗？', '抑制告警', { type: 'warning' });
    const res = await suppressAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('抑制成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('抑制告警失败', error);
  }
};

const handleClose = async (row: AlarmRecord) => {
  try {
    await ElMessageBox.confirm('确定要关闭该告警吗？', '关闭告警', { type: 'warning' });
    const res = await closeAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('关闭告警失败', error);
  }
};

onMounted(() => {
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
  padding: 20px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}

.alarm-header {
  margin-bottom: 20px;
}

.alarm-header h1 {
  font-size: 20px;
  margin-bottom: 12px;
}

.alarm-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.alarm-stats :deep(.el-statistic) {
  min-width: 170px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94));
  box-shadow: var(--shadow-sm);
}

.alarm-filters {
  margin-bottom: 12px;
  padding: 12px 12px 4px;
  background: #fafbfd;
  border-radius: 4px;
  border: 1px solid #e6eaf0;
  box-shadow: none;
}

.alarm-filter-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
}

.alarm-filter-actions {
  display: flex;
  justify-content: flex-end;
}

.alarm-btn {
  border-radius: 4px;
  padding-inline: 12px;
}

.alarm-btn--primary {
  box-shadow: none;
}

.alarm-btn--ghost {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #4f5969;
}

.alarm-list {
  margin-bottom: 12px;
  border: 1px solid #e6eaf0;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.alarm-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 0;
}

</style>
