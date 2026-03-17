<template>
  <div class="event-disposal-view">
    <div class="event-header">
      <h1>事件处置</h1>
      <div class="event-stats">
        <el-statistic title="待派发事件" :value="stats.pendingEvents" />
        <el-statistic title="已派发事件" :value="stats.dispatchedEvents" />
        <el-statistic title="处理中事件" :value="stats.processingEvents" />
        <el-statistic title="已关闭事件" :value="stats.closedEvents" />
      </div>
    </div>

    <div class="event-filters">
      <el-form :model="filters" label-position="left" class="event-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" placeholder="请输入设备编码" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="风险等级">
              <el-select v-model="filters.riskLevel" placeholder="请选择风险等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="待派发" :value="0" />
                <el-option label="已派发" :value="1" />
                <el-option label="处理中" :value="2" />
                <el-option label="待验收" :value="3" />
                <el-option label="已关闭" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="" class="event-filter-actions">
              <el-button type="primary" class="event-btn event-btn--primary" @click="handleSearch">查询</el-button>
              <el-button class="event-btn event-btn--ghost" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="event-list">
      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">已选 {{ selectedRows.length }} 项</span>
        </div>
        <div class="table-action-bar__right">
          <el-button link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button link :disabled="eventList.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>
      <el-table ref="tableRef" :data="eventList" v-loading="loading" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="eventCode" label="事件编号" width="180" />
        <el-table-column prop="eventTitle" label="事件标题" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)">{{ getRiskLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="区域" width="120" />
        <el-table-column prop="riskPointName" label="风险点" width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="metricName" label="测点名称" width="150" />
        <el-table-column prop="currentValue" label="当前值" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="primary" link @click="handleDispatch(row)">派发</el-button>
            <el-button v-if="row.status !== 4" type="primary" link @click="handleClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="event-pagination">
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

    <EventDetailDrawer
      v-model="detailVisible"
      :detail="detail"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
    />

    <StandardFormDrawer
      v-model="dispatchVisible"
      eyebrow="Event Workflow"
      title="工单派发"
      subtitle="统一通过右侧抽屉配置派发对象与处理时限。"
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
        <el-button class="event-btn event-btn--ghost" @click="closeDispatchDialog">取消</el-button>
        <el-button type="primary" class="event-btn event-btn--primary" @click="handleDispatchConfirm">确定</el-button>
      </template>
    </StandardFormDrawer>

    <StandardFormDrawer
      v-model="closeVisible"
      eyebrow="Event Workflow"
      title="事件关闭"
      subtitle="统一通过右侧抽屉填写关闭原因并完成事件收口。"
      size="34rem"
      @close="closeCloseDialog"
    >
      <el-form :model="closeForm" label-position="left" class="event-drawer-form">
        <el-form-item label="关闭原因">
          <el-input v-model="closeForm.closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="event-btn event-btn--ghost" @click="closeCloseDialog">取消</el-button>
        <el-button type="primary" class="event-btn event-btn--primary" @click="handleCloseConfirm">确定</el-button>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="事件处置导出列设置"
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
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue';
import EventDetailDrawer from '@/components/EventDetailDrawer.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv';
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns';

import { ElMessageBox } from '@/utils/messageBox';
import { closeEvent, dispatchEvent, getEventDetail, getEventList } from '../api/alarm';
import type { EventRecord } from '../api/alarm';

const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailErrorMessage = ref('');
const dispatchVisible = ref(false);
const closeVisible = ref(false);
const eventList = ref<EventRecord[]>([]);
const detail = ref<EventRecord | null>(null);
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

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

const dispatchForm = reactive({
  dispatchUserName: '系统管理员',
  receiveUser: 1,
  arrivalTimeLimit: 2,
  completionTimeLimit: 24
});

const closeForm = reactive({
  closeReason: ''
});

const getRiskLevelType = (level: string) => {
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

const getRiskLevelText = (level: string) => {
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

const loadEventList = async () => {
  loading.value = true;
  try {
    const params: { deviceCode?: string; riskLevel?: string; status?: number } = {};
    if (filters.deviceCode) params.deviceCode = filters.deviceCode;
    if (filters.riskLevel) params.riskLevel = filters.riskLevel;
    if (filters.status) params.status = parseInt(filters.status, 10);

    const res = await getEventList(params);
    if (res.code === 200) {
      eventList.value = res.data || [];
      pagination.total = eventList.value.length;
      stats.value.pendingEvents = eventList.value.filter((e) => e.status === 0).length;
      stats.value.dispatchedEvents = eventList.value.filter((e) => e.status === 1).length;
      stats.value.processingEvents = eventList.value.filter((e) => e.status === 2).length;
      stats.value.closedEvents = eventList.value.filter((e) => e.status === 4).length;
    }
  } catch (error) {
    console.error('查询事件列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadEventList();
};

const handleReset = () => {
  filters.deviceCode = '';
  filters.riskLevel = '';
  filters.status = '';
  pagination.page = 1;
  void loadEventList();
};

const handleSelectionChange = (rows: EventRecord[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadEventList();
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
  downloadRowsAsCsv('事件处置-选中项.csv', selectedRows.value, getResolvedExportColumns());
};

const handleExportCurrent = () => {
  downloadRowsAsCsv('事件处置-当前结果.csv', eventList.value, getResolvedExportColumns());
};

const handleSizeChange = () => {
  void loadEventList();
};

const handlePageChange = () => {
  void loadEventList();
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
    await ElMessageBox.confirm('确定要关闭该事件吗？', '关闭事件', { type: 'warning' });
    const res = await closeEvent(closeTarget.value.id, 1, closeForm.closeReason);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      closeCloseDialog();
      void loadEventList();
    }
  } catch (error) {
    console.error('关闭事件失败', error);
  }
};

onMounted(() => {
  void loadEventList();
});

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
  padding: 20px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}

.event-header {
  margin-bottom: 20px;
}

.event-header h1 {
  font-size: 24px;
  margin-bottom: 16px;
}

.event-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.event-stats :deep(.el-statistic) {
  min-width: 170px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94));
  box-shadow: var(--shadow-sm);
}

.event-filters {
  margin-bottom: 12px;
  padding: 12px 12px 4px;
  background: #fafbfd;
  border-radius: 4px;
  border: 1px solid #e6eaf0;
  box-shadow: none;
}

.event-filter-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
}

.event-filter-actions {
  display: flex;
  justify-content: flex-end;
}

.event-btn {
  border-radius: 4px;
  padding-inline: 12px;
}

.event-btn--primary {
  box-shadow: none;
}

.event-btn--ghost {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #4f5969;
}

.event-list {
  margin-bottom: 12px;
  border: 1px solid #e6eaf0;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.event-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 0;
}

.event-drawer-form :deep(.el-select),
.event-drawer-form :deep(.el-input-number) {
  width: 100%;
}
</style>
