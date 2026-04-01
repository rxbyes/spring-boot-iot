<template>
  <StandardPageShell class="risk-gis-view">
    <StandardWorkbenchPanel
      title="GIS态势图"
      :description="`当前 ${totalPoints} 个风险点，支持按区域聚焦空间分布并联动查看监测详情。`"
      show-filters
      show-notices
      show-toolbar
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option v-for="region in regionOptions" :key="region.value" :label="region.label" :value="region.value" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">刷新点位</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置筛选</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #notices>
        <el-alert
          :title="gisNotice"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="gisMetaItems"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新点位</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <section class="ops-section">
        <div class="ops-section__header">
          <h3 class="ops-section__title">已定位风险点</h3>
          <p class="ops-section__description">已定位 {{ locatedTotal }} 个点位，可直接联动查看监测详情。</p>
        </div>

        <div
          v-loading="loading && locatedHasRecords"
          class="ops-list-result-panel standard-list-surface"
          element-loading-text="正在刷新 GIS 点位"
          element-loading-background="var(--loading-mask-bg)"
        >
          <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
            <div class="ops-list-loading-state__summary">
              <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
            </div>
            <div class="ops-list-loading-table ops-list-loading-table--header">
              <span v-for="item in 6" :key="`gis-located-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
            </div>
            <div v-for="row in 5" :key="`gis-located-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
              <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            </div>
          </div>

          <template v-else-if="locatedHasRecords">
            <el-table :data="pagedLocatedPoints" border stripe>
              <StandardTableTextColumn prop="riskPointName" label="风险点" :min-width="160" />
              <StandardTableTextColumn prop="regionName" label="区域" :min-width="120" />
              <el-table-column label="风险等级" width="100">
                <template #default="{ row }">
                  <el-tag :type="riskLevelTagType(row.riskLevel)" round>{{ riskLevelText(row.riskLevel) }}</el-tag>
                </template>
              </el-table-column>
              <StandardTableTextColumn label="经纬度" :min-width="220">
                <template #default="{ row }">
                  {{ formatCoordinate(row.longitude, row.latitude) }}
                </template>
              </StandardTableTextColumn>
              <StandardTableTextColumn prop="activeAlarmCount" label="活跃告警" :width="100">
                <template #default="{ row }">{{ row.activeAlarmCount ?? 0 }}</template>
              </StandardTableTextColumn>
              <StandardTableTextColumn prop="onlineDeviceCount" label="在线设备" :width="100">
                <template #default="{ row }">{{ row.onlineDeviceCount ?? 0 }}</template>
              </StandardTableTextColumn>
              <el-table-column
                label="操作"
                :width="gisActionColumnWidth"
                fixed="right"
                class-name="standard-row-actions-column"
                :show-overflow-tooltip="false"
              >
                <template #default="{ row }">
                  <StandardWorkbenchRowActions
                    variant="table"
                    :direct-items="gisRowActions"
                    @command="() => openDetailByRiskPoint(row.riskPointId)"
                  />
                </template>
              </el-table-column>
            </el-table>
          </template>

          <div v-else-if="!loading" class="standard-list-empty-state">
            <EmptyState :title="locatedEmptyStateTitle" :description="locatedEmptyStateDescription" />
            <div class="standard-list-empty-state__actions">
              <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleReset">清空筛选条件</StandardButton>
              <StandardButton v-else action="refresh" @click="handleRefresh">刷新点位</StandardButton>
            </div>
          </div>
        </div>

        <div v-if="locatedTotal > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="locatedPagination.pageNum"
            v-model:page-size="locatedPagination.pageSize"
            :total="locatedTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleLocatedPageSizeChange"
            @current-change="handleLocatedPageChange"
          />
        </div>
      </section>

      <section class="ops-section">
        <div class="ops-section__header">
          <h3 class="ops-section__title">未定位风险点</h3>
          <p class="ops-section__description">无经纬度点位的风险点统一收拢为资源卡，当前共 {{ unlocatedTotal }} 个，方便补齐坐标与继续查看详情。</p>
        </div>

        <div
          v-loading="loading && unlocatedHasRecords"
          class="ops-list-result-panel standard-list-surface"
          element-loading-text="正在同步未定位风险点"
          element-loading-background="var(--loading-mask-bg)"
        >
          <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
            <div class="ops-list-loading-state__summary">
              <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
            </div>
            <div class="ops-list-loading-table ops-list-loading-table--header">
              <span v-for="item in 4" :key="`gis-unlocated-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
            </div>
            <div v-for="row in 4" :key="`gis-unlocated-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
              <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
              <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            </div>
          </div>

          <div v-else-if="unlocatedHasRecords" class="ops-resource-grid">
            <article v-for="point in pagedUnlocatedPoints" :key="point.riskPointId" class="ops-resource-card">
              <div class="ops-resource-card__header">
                <strong>{{ point.riskPointName || `风险点 ${point.riskPointId}` }}</strong>
                <el-tag :type="riskLevelTagType(point.riskLevel)" round>{{ riskLevelText(point.riskLevel) }}</el-tag>
              </div>
              <div class="ops-resource-card__meta">
                <span>区域 {{ point.regionName || '--' }}</span>
                <span>设备数 {{ point.deviceCount ?? 0 }}</span>
                <span>在线设备 {{ point.onlineDeviceCount ?? 0 }}</span>
                <span>活跃告警 {{ point.activeAlarmCount ?? 0 }}</span>
              </div>
              <div class="ops-resource-card__footer">
                <StandardWorkbenchRowActions
                  variant="card"
                  :direct-items="gisRowActions"
                  @command="() => openDetailByRiskPoint(point.riskPointId)"
                />
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="standard-list-empty-state">
            <EmptyState :title="unlocatedEmptyStateTitle" :description="unlocatedEmptyStateDescription" />
            <div class="standard-list-empty-state__actions">
              <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleReset">清空筛选条件</StandardButton>
              <StandardButton v-else action="refresh" @click="handleRefresh">刷新点位</StandardButton>
            </div>
          </div>
        </div>

        <div v-if="unlocatedTotal > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="unlocatedPagination.pageNum"
            v-model:page-size="unlocatedPagination.pageSize"
            :total="unlocatedTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleUnlocatedPageSizeChange"
            @current-change="handleUnlocatedPageChange"
          />
        </div>
      </section>
    </StandardWorkbenchPanel>

    <RiskMonitoringDetailDrawer v-model="detailVisible" :binding-id="activeBindingId" />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';

import EmptyState from '../components/EmptyState.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardPagination from '../components/StandardPagination.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '../components/StandardWorkbenchRowActions.vue';
import { useServerPagination } from '../composables/useServerPagination';
import { getRiskMonitoringGisPoints, getRiskMonitoringList, type RiskMonitoringGisPoint } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';
import type { IdType } from '../types/api';
import { resolveWorkbenchActionColumnWidth } from '../utils/adaptiveActionColumn';

interface SelectOption {
  value: number;
  label: string;
}

const filters = reactive<{ regionId?: number }>({ regionId: undefined });
const loading = ref(false);
const points = ref<RiskMonitoringGisPoint[]>([]);
const riskPoints = ref<RiskPoint[]>([]);
const regionOptions = ref<SelectOption[]>([]);
const detailVisible = ref(false);
const activeBindingId = ref<number | null>(null);
const gisAdvice = '优先补齐未定位风险点的经纬度信息';
const gisRowActions = [{ command: 'detail' as const, label: '详情' }];
const { pagination: locatedPagination, applyLocalRecords: applyLocatedRecords, resetPage: resetLocatedPage, setPageNum: setLocatedPageNum, setPageSize: setLocatedPageSize } = useServerPagination();
const { pagination: unlocatedPagination, applyLocalRecords: applyUnlocatedRecords, resetPage: resetUnlocatedPage, setPageNum: setUnlocatedPageNum, setPageSize: setUnlocatedPageSize } = useServerPagination();
const gisActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: gisRowActions
});
let latestListRequestId = 0;

const totalPoints = computed(() => points.value.length);
const locatedPoints = computed(() =>
  points.value.filter((point) => point.longitude !== null && point.longitude !== undefined && point.latitude !== null && point.latitude !== undefined)
);

const unlocatedPoints = computed(() =>
  points.value.filter((point) => point.longitude === null || point.longitude === undefined || point.latitude === null || point.latitude === undefined)
);

const activeAlarmTotal = computed(() =>
  points.value.reduce((sum, point) => sum + Number(point.activeAlarmCount ?? 0), 0)
);

const onlineDeviceTotal = computed(() =>
  points.value.reduce((sum, point) => sum + Number(point.onlineDeviceCount ?? 0), 0)
);
const hasAppliedFilters = computed(() => filters.regionId !== undefined);
const locatedHasRecords = computed(() => locatedPoints.value.length > 0);
const unlocatedHasRecords = computed(() => unlocatedPoints.value.length > 0);
const showListSkeleton = computed(() => loading.value && totalPoints.value === 0);
const locatedTotal = computed(() => locatedPoints.value.length);
const unlocatedTotal = computed(() => unlocatedPoints.value.length);
const pagedLocatedPoints = computed(() => applyLocatedRecords(locatedPoints.value));
const pagedUnlocatedPoints = computed(() => applyUnlocatedRecords(unlocatedPoints.value));
const locatedEmptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的已定位风险点' : '当前还没有已定位风险点'));
const locatedEmptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配到可渲染经纬度的点位，可以调整筛选条件，或直接清空当前筛选。'
    : '当前还没有可展示的已定位风险点，建议先补齐经纬度数据，或稍后刷新点位列表。'
);
const unlocatedEmptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的未定位风险点' : '当前没有未定位风险点'));
const unlocatedEmptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '当前筛选下没有需要补齐坐标的风险点，可以调整筛选条件后继续查看。'
    : '当前所有风险点都已经具备经纬度坐标，可以直接在已定位列表中查看空间分布。'
);

const gisNotice = computed(
  () => `当前覆盖 ${onlineDeviceTotal.value} 台在线设备，${gisAdvice}，并可直接从点位列表联动查看统一监测详情。`
);

const gisMetaItems = computed(() => [
  `风险点 ${totalPoints.value} 个`,
  `已定位 ${locatedPoints.value.length} 个`,
  `未定位 ${unlocatedPoints.value.length} 个`,
  `活跃告警 ${activeAlarmTotal.value} 条`,
  `在线设备 ${onlineDeviceTotal.value} 台`
]);

onMounted(async () => {
  await Promise.all([loadRegionOptions(), loadPoints()]);
});

async function loadRegionOptions() {
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
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '区域筛选项加载失败');
  }
}

async function loadPoints() {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const response = await getRiskMonitoringGisPoints(filters.regionId);
    if (requestId !== latestListRequestId) {
      return;
    }
    points.value = response.data || [];
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    points.value = [];
    ElMessage.error(error instanceof Error ? error.message : 'GIS 点位加载失败');
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
  }
}

function handleSearch() {
  resetLocatedPage();
  resetUnlocatedPage();
  void loadPoints();
}

function handleReset() {
  filters.regionId = undefined;
  resetLocatedPage();
  resetUnlocatedPage();
  void loadPoints();
}

function handleRefresh() {
  void loadPoints();
}

function handleLocatedPageChange(page: number) {
  setLocatedPageNum(page);
}

function handleLocatedPageSizeChange(size: number) {
  setLocatedPageSize(size);
}

function handleUnlocatedPageChange(page: number) {
  setUnlocatedPageNum(page);
}

function handleUnlocatedPageSizeChange(size: number) {
  setUnlocatedPageSize(size);
}

async function openDetailByRiskPoint(riskPointId: IdType) {
  try {
    const response = await getRiskMonitoringList({ riskPointId, pageNum: 1, pageSize: 1 });
    const bindingId = response.data.records?.[0]?.bindingId;
    if (!bindingId) {
      ElMessage.warning('当前风险点没有可用的监测绑定详情');
      return;
    }
    const normalizedBindingId = Number(bindingId);
    if (Number.isNaN(normalizedBindingId)) {
      ElMessage.warning('监测详情标识无效');
      return;
    }
    activeBindingId.value = normalizedBindingId;
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '监测详情入口解析失败');
  }
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

function formatCoordinate(longitude?: number | null, latitude?: number | null) {
  if (longitude === null || longitude === undefined || latitude === null || latitude === undefined) {
    return '--';
  }
  return `${longitude.toFixed(6)}, ${latitude.toFixed(6)}`;
}
</script>

<style scoped>
.risk-gis-view {
  min-width: 0;
}

.ops-section {
  display: grid;
  gap: 0.88rem;
}

.ops-section + .ops-section {
  margin-top: 1.1rem;
}

.ops-section__header {
  display: grid;
  gap: 0.22rem;
}

.ops-section__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
  font-weight: 600;
}

.ops-section__description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.6;
}
</style>
