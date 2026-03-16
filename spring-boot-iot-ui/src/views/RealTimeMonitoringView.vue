<template>
  <div class="risk-monitoring-view">
    <PanelCard
      eyebrow="Real-Time Monitoring"
      title="实时监测"
      description="按区域、风险点、设备和风险等级筛选当前监测项，并通过统一详情抽屉查看趋势、告警和事件摘要?
    >
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="区域">
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option
                  v-for="region in regionOptions"
                  :key="region.value"
                  :label="region.label"
                  :value="region.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="风险?>
              <el-select v-model="filters.riskPointId" clearable placeholder="全部风险?>
                <el-option
                  v-for="riskPoint in riskPointOptions"
                  :key="riskPoint.value"
                  :label="riskPoint.label"
                  :value="riskPoint.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" clearable placeholder="请输入设备编? />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="风险等级">
              <el-select v-model="filters.riskLevel" clearable placeholder="全部等级">
                <el-option label="严重" value="CRITICAL" />
                <el-option label="? value="WARNING" />
                <el-option label="? value="INFO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="在线状?>
              <el-select v-model="filters.onlineStatus" clearable placeholder="全部状?>
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Live List"
      title="监测列表"
      :description="`当前?${pagination.total} 条监测记录，首屏按后端分页结果直接消费。`"
    >
      <div v-if="loading" class="table-state">正在加载实时监测数据...</div>
      <div v-else-if="rows.length === 0" class="table-state">暂无符合条件的监测记?/div>
      <template v-else>
        <el-table :data="rows" border>
          <el-table-column prop="deviceCode" label="设备编码" min-width="140" />
          <el-table-column prop="deviceName" label="设备名称" min-width="150" />
          <el-table-column prop="productName" label="产品名称" min-width="150" />
          <el-table-column prop="riskPointName" label="风险? min-width="140" />
          <el-table-column label="测点" min-width="150">
            <template #default="{ row }">
              {{ row.metricName || row.metricIdentifier || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="当前? min-width="120">
            <template #default="{ row }">
              {{ formatCurrentValue(row.currentValue, row.unit) }}
            </template>
          </el-table-column>
          <el-table-column label="状? width="110">
            <template #default="{ row }">
              <el-tag :type="monitorStatusTagType(row.monitorStatus)">
                {{ monitorStatusText(row.monitorStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最新上报时? min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.latestReportTime) }}
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="riskLevelTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="告警标记" width="100">
            <template #default="{ row }">
              <el-tag :type="row.alarmFlag ? 'danger' : 'info'">
                {{ row.alarmFlag ? '有告? : '无告? }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openDetail(row.bindingId)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </PanelCard>

    <RiskMonitoringDetailDrawer
      v-model="detailVisible"
      :binding-id="activeBindingId"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';

import PanelCard from '../components/PanelCard.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import { getRiskMonitoringList, type RiskMonitoringListItem } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';
import { formatDateTime } from '../utils/format';

interface SelectOption {
  value: number;
  label: string;
}

const loading = ref(false);
const rows = ref<RiskMonitoringListItem[]>([]);
const riskPoints = ref<RiskPoint[]>([]);
const detailVisible = ref(false);
const activeBindingId = ref<number | null>(null);

const filters = reactive<{
  regionId?: number;
  riskPointId?: number;
  deviceCode: string;
  riskLevel: string;
  onlineStatus?: number;
}>({
  regionId: undefined,
  riskPointId: undefined,
  deviceCode: '',
  riskLevel: '',
  onlineStatus: undefined
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const regionOptions = ref<SelectOption[]>([]);
const riskPointOptions = ref<SelectOption[]>([]);

onMounted(async () => {
  await Promise.all([loadFilterOptions(), loadList()]);
});

async function loadFilterOptions() {
  try {
    const response = await getRiskPointList();
    riskPoints.value = response.data || [];

    const regionMap = new Map<number, string>();
    for (const item of riskPoints.value) {
      if (item.regionId && item.regionName) {
        regionMap.set(item.regionId, item.regionName);
      }
    }

    regionOptions.value = [...regionMap.entries()].map(([value, label]) => ({ value, label }));
    riskPointOptions.value = riskPoints.value.map((item) => ({
      value: item.id,
      label: `${item.riskPointName} (${item.riskPointCode})`
    }));
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '风险点筛选项加载失败');
  }
}

async function loadList() {
  loading.value = true;
  try {
    const response = await getRiskMonitoringList({
      regionId: filters.regionId,
      riskPointId: filters.riskPointId,
      deviceCode: filters.deviceCode || undefined,
      riskLevel: filters.riskLevel || undefined,
      onlineStatus: filters.onlineStatus,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    rows.value = response.data.records || [];
    pagination.total = response.data.total || 0;
    pagination.pageNum = response.data.pageNum || pagination.pageNum;
    pagination.pageSize = response.data.pageSize || pagination.pageSize;
  } catch (error) {
    rows.value = [];
    pagination.total = 0;
    ElMessage.error(error instanceof Error ? error.message : '实时监测列表加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.pageNum = 1;
  loadList();
}

function handleReset() {
  filters.regionId = undefined;
  filters.riskPointId = undefined;
  filters.deviceCode = '';
  filters.riskLevel = '';
  filters.onlineStatus = undefined;
  pagination.pageNum = 1;
  pagination.pageSize = 10;
  loadList();
}

function handlePageChange() {
  loadList();
}

function handlePageSizeChange() {
  pagination.pageNum = 1;
  loadList();
}

function openDetail(bindingId: number) {
  activeBindingId.value = bindingId;
  detailVisible.value = true;
}

function riskLevelText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '严重';
    case 'WARNING':
    case 'MEDIUM':
      return '?;
    case 'INFO':
    case 'LOW':
      return '?;
    default:
      return value || '未标?;
  }
}

function riskLevelTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return 'danger';
    case 'WARNING':
    case 'MEDIUM':
      return 'warning';
    case 'INFO':
    case 'LOW':
      return 'success';
    default:
      return 'info';
  }
}

function monitorStatusText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'ALARM':
      return '告警?;
    case 'OFFLINE':
      return '离线';
    case 'NO_DATA':
      return '无数?;
    case 'NORMAL':
      return '正常';
    default:
      return value || '未识?;
  }
}

function monitorStatusTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch ((value || '').toUpperCase()) {
    case 'ALARM':
      return 'danger';
    case 'OFFLINE':
    case 'NO_DATA':
      return 'warning';
    case 'NORMAL':
      return 'success';
    default:
      return 'info';
  }
}

function formatCurrentValue(value?: string | null, unit?: string | null) {
  if (!value) {
    return '--';
  }
  return unit ? `${value} ${unit}` : value;
}
</script>

<style scoped>
.risk-monitoring-view {
  display: grid;
  gap: 1rem;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.table-state {
  padding: 2rem 1rem;
  text-align: center;
  border-radius: var(--radius-md);
  border: 1px dashed var(--panel-border);
  color: var(--text-secondary);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 960px) {
  .filter-actions {
    justify-content: flex-start;
  }
}
</style>

