<template>
  <div class="ops-workbench risk-monitoring-view">
    <PanelCard
      eyebrow="Real-Time Monitoring"
      title="实时监测台"
      description="统一汇总当前监测项的在线状态、告警风险与详情入口，帮助值班人员快速完成筛选与研判。"
      class="ops-hero-card"
    >
      <div class="ops-kpi-grid">
        <MetricCard label="筛选结果" :value="String(pagination.total)" :badge="{ label: '当前条件', tone: 'brand' }" />
        <MetricCard label="当前页在线" :value="String(onlineCount)" :badge="{ label: '稳定', tone: 'success' }" />
        <MetricCard label="当前页告警" :value="String(alarmCount)" :badge="{ label: '优先', tone: 'danger' }" />
        <MetricCard label="高风险项" :value="String(criticalCount)" :badge="{ label: '严重', tone: 'warning' }" />
      </div>
      <div class="ops-inline-note">
        支持按区域、风险点、设备编码、风险等级和在线状态组合筛选，列表与右侧详情抽屉保持统一的监测预警平台视觉风格。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Monitoring Filters"
      title="筛选条件"
      description="优先关注当前页告警项、无数据项和高风险项，快速定位需要立即跟进的监测对象。"
      class="ops-filter-card"
    >
      <el-form :model="filters" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="区域">
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option v-for="region in regionOptions" :key="region.value" :label="region.label" :value="region.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="风险点">
              <el-select v-model="filters.riskPointId" clearable placeholder="全部风险点">
                <el-option v-for="riskPoint in riskPointOptions" :key="riskPoint.value" :label="riskPoint.label" :value="riskPoint.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" clearable placeholder="请输入设备编码" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="风险等级">
              <el-select v-model="filters.riskLevel" clearable placeholder="全部等级">
                <el-option label="严重" value="CRITICAL" />
                <el-option label="警告" value="WARNING" />
                <el-option label="提醒" value="INFO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="在线状态">
              <el-select v-model="filters.onlineStatus" clearable placeholder="全部状态">
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="ops-filter-actions">
          <StandardButton action="query" @click="handleSearch">查询</StandardButton>
          <StandardButton action="reset" @click="handleReset">重置</StandardButton>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Monitoring List"
      title="监测列表"
      :description="`当前 ${pagination.total} 条监测记录，详情统一从右侧抽屉展开。`"
      class="ops-table-card"
    >
      <StandardTableToolbar
        :meta-items="[`当前页 ${displayedCount} 项`, `告警 ${alarmCount} 项`, `无数据 ${noDataCount} 项`]"
      >
        <template #right>
          <StandardButton action="reset" link @click="handleReset">重置筛选</StandardButton>
          <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
        </template>
      </StandardTableToolbar>

      <div v-if="loading" class="ops-state">正在加载实时监测数据...</div>
      <div v-else-if="rows.length === 0" class="ops-state">暂无符合条件的监测记录</div>
      <template v-else>
        <el-table :data="rows" border stripe>
          <StandardTableTextColumn prop="deviceCode" label="设备编码" :min-width="140" />
          <StandardTableTextColumn prop="deviceName" label="设备名称" :min-width="150" />
          <StandardTableTextColumn prop="productName" label="产品名称" :min-width="150" />
          <StandardTableTextColumn prop="riskPointName" label="风险点" :min-width="140" />
          <StandardTableTextColumn label="测点" :min-width="150">
            <template #default="{ row }">
              {{ row.metricName || row.metricIdentifier || '--' }}
            </template>
          </StandardTableTextColumn>
          <StandardTableTextColumn prop="currentValue" label="当前值" :min-width="120">
            <template #default="{ row }">
              {{ formatCurrentValue(row.currentValue, row.unit) }}
            </template>
          </StandardTableTextColumn>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="monitorStatusTagType(row.monitorStatus)" round>{{ monitorStatusText(row.monitorStatus) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="latestReportTime" label="最新上报时间" :min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.latestReportTime) }}</template>
          </StandardTableTextColumn>
          <el-table-column label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="riskLevelTagType(row.riskLevel)" round>{{ riskLevelText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="告警标记" width="110">
            <template #default="{ row }">
              <el-tag :type="row.alarmFlag ? 'danger' : 'info'" round>
                {{ row.alarmFlag ? '有告警' : '无告警' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <StandardRowActions variant="table" gap="wide">
                <StandardActionLink @click="openDetail(row.bindingId)">详情</StandardActionLink>
              </StandardRowActions>
            </template>
          </el-table-column>
        </el-table>

        <div class="ops-pagination">
          <StandardPagination
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

    <RiskMonitoringDetailDrawer v-model="detailVisible" :binding-id="activeBindingId" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';

import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import StandardPagination from '../components/StandardPagination.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import { useServerPagination } from '../composables/useServerPagination';
import { getRiskMonitoringList, type RiskMonitoringListItem } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';
import type { IdType } from '../types/api';
import { formatDateTime } from '../utils/format';

interface SelectOption {
  value: IdType;
  label: string;
}

const loading = ref(false);
const rows = ref<RiskMonitoringListItem[]>([]);
const riskPoints = ref<RiskPoint[]>([]);
const detailVisible = ref(false);
const activeBindingId = ref<number | null>(null);
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination();

const filters = reactive<{
  regionId?: number;
  riskPointId?: IdType;
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

const regionOptions = ref<SelectOption[]>([]);
const riskPointOptions = ref<SelectOption[]>([]);

const displayedCount = computed(() => rows.value.length);
const onlineCount = computed(() => rows.value.filter((row) => Number(row.onlineStatus) === 1).length);
const alarmCount = computed(() => rows.value.filter((row) => Boolean(row.alarmFlag)).length);
const noDataCount = computed(() => rows.value.filter((row) => (row.monitorStatus || '').toUpperCase() === 'NO_DATA').length);
const criticalCount = computed(() => rows.value.filter((row) => (row.riskLevel || '').toUpperCase() === 'CRITICAL').length);

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
    rows.value = applyPageResult(response.data);
  } catch (error) {
    rows.value = [];
    resetTotal();
    ElMessage.error(error instanceof Error ? error.message : '实时监测列表加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  resetPage();
  void loadList();
}

function handleReset() {
  filters.regionId = undefined;
  filters.riskPointId = undefined;
  filters.deviceCode = '';
  filters.riskLevel = '';
  filters.onlineStatus = undefined;
  setPageSize(10);
  void loadList();
}

function handleRefresh() {
  void loadList();
}

function handlePageChange(page: number) {
  setPageNum(page);
  void loadList();
}

function handlePageSizeChange(size: number) {
  setPageSize(size);
  void loadList();
}

function openDetail(bindingId: IdType) {
  const normalizedBindingId = Number(bindingId);
  if (Number.isNaN(normalizedBindingId)) {
    ElMessage.warning('监测详情标识无效');
    return;
  }
  activeBindingId.value = normalizedBindingId;
  detailVisible.value = true;
}

function riskLevelText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '严重';
    case 'WARNING':
    case 'MEDIUM':
      return '警告';
    case 'INFO':
    case 'LOW':
      return '提醒';
    default:
      return value || '未标注';
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
      return '告警中';
    case 'OFFLINE':
      return '离线';
    case 'NO_DATA':
      return '无数据';
    case 'NORMAL':
      return '正常';
    default:
      return value || '未识别';
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
  if (!value) return '--';
  return unit ? `${value} ${unit}` : value;
}
</script>

<style scoped>
.risk-monitoring-view {
  padding: 18px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(245, 249, 253, 0.58));
  border: 1px solid rgba(41, 60, 92, 0.08);
  box-shadow: var(--shadow-inset-highlight-72);
}
</style>
