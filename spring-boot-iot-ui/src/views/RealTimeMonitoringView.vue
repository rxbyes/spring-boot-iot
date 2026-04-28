<template>
  <StandardPageShell class="risk-monitoring-view">
    <StandardWorkbenchPanel
      title="实时监测台"
      :description="`当前 ${pagination.total} 条监测记录，支持按区域、风险点、设备编码和在线状态快速定位。`"
      show-filters
      show-notices
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option v-for="region in regionOptions" :key="region.value" :label="region.label" :value="region.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskPointId" clearable placeholder="全部风险点">
                <el-option v-for="riskPoint in riskPointOptions" :key="riskPoint.value" :label="riskPoint.label" :value="riskPoint.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.deviceCode" clearable placeholder="请输入设备编码" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskLevel" clearable placeholder="全部等级">
                <el-option
                  v-for="option in riskLevelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.onlineStatus" clearable placeholder="全部状态">
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #notices>
        <el-alert
          :title="monitoringAdvice"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="monitoringMetaItems"
        >
          <template #right>
            <StandardButton action="reset" link @click="handleReset">重置筛选</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新实时监测数据"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`monitor-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`monitor-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table :data="rows" border stripe>
            <StandardTableTextColumn
              prop="deviceName"
              label="设备"
              secondary-prop="deviceCode"
              :min-width="180"
            />
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
            <el-table-column
              label="操作"
              :width="monitoringActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="monitoringRowActions"
                  @command="() => openDetail(row.bindingId)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleReset">清空筛选条件</StandardButton>
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
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <RiskMonitoringDetailDrawer v-model="detailVisible" :binding-id="activeBindingId" />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';

import EmptyState from '../components/EmptyState.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import StandardPagination from '../components/StandardPagination.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '../components/StandardWorkbenchRowActions.vue';
import { useServerPagination } from '../composables/useServerPagination';
import { getRiskMonitoringList, type RiskMonitoringListItem } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';
import type { IdType } from '../types/api';
import { resolveWorkbenchActionColumnWidth } from '../utils/adaptiveActionColumn';
import { formatDateTime } from '../utils/format';
import { normalizeOptionalId } from '../utils/id';
import { fetchRiskLevelOptions, getRiskLevelTagType, getRiskLevelText, getRiskLevelWeight, type RiskLevelOption } from '../utils/riskLevel';

interface SelectOption {
  value: IdType;
  label: string;
}

const loading = ref(false);
const rows = ref<RiskMonitoringListItem[]>([]);
const riskPoints = ref<RiskPoint[]>([]);
const riskLevelOptions = ref<RiskLevelOption[]>([]);
const detailVisible = ref(false);
const activeBindingId = ref<IdType | null>(null);
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination();
const monitoringRowActions = [{ command: 'detail' as const, label: '详情' }];
const monitoringActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: monitoringRowActions
});
let latestListRequestId = 0;

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
const redCount = computed(() => rows.value.filter((row) => getRiskLevelWeight(row.riskLevel) === 4).length);
const monitoringAdvice = '优先关注告警中、无数据和高风险监测项，详情统一从右侧抽屉展开。';
const hasAppliedFilters = computed(() =>
  Boolean(filters.regionId || filters.riskPointId || filters.deviceCode || filters.riskLevel || filters.onlineStatus !== undefined)
);
const hasRecords = computed(() => rows.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的监测记录' : '当前还没有实时监测数据'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整筛选条件，或者直接清空当前筛选。'
    : '当前还没有可展示的实时监测记录，建议稍后刷新，或先检查监测绑定和设备上报链路。'
);
const monitoringMetaItems = computed(() => [
  `当前页 ${displayedCount.value} 项`,
  `在线 ${onlineCount.value} 项`,
  `告警 ${alarmCount.value} 项`,
  `无数据 ${noDataCount.value} 项`,
  `红色 ${redCount.value} 项`
]);
const riskLevelText = getRiskLevelText;
const riskLevelTagType = getRiskLevelTagType;

onMounted(async () => {
  await Promise.all([loadFilterOptions(), loadList()]);
});

async function loadFilterOptions() {
  try {
    riskLevelOptions.value = await fetchRiskLevelOptions();
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
  const requestId = ++latestListRequestId;
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
    if (requestId !== latestListRequestId) {
      return;
    }
    rows.value = applyPageResult(response.data);
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    rows.value = [];
    resetTotal();
    ElMessage.error(error instanceof Error ? error.message : '实时监测列表加载失败');
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
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
  resetPage();
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
  const normalizedBindingId = normalizeOptionalId(bindingId);
  if (normalizedBindingId === undefined) {
    ElMessage.warning('监测详情标识无效');
    return;
  }
  activeBindingId.value = normalizedBindingId;
  detailVisible.value = true;
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
  min-width: 0;
}
</style>
