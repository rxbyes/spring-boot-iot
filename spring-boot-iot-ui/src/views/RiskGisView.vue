<template>
  <div class="risk-gis-view">
    <PanelCard
      eyebrow="GIS Situation"
      title="GIS 风险态势"
      description="GIS 点位态势列表，支持区域筛选并联动监测详情。"
    >
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="区域">
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option v-for="region in regionOptions" :key="region.value" :label="region.label" :value="region.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <div class="filter-actions">
              <el-button type="primary" @click="loadPoints">刷新点位</el-button>
              <el-button @click="handleReset">重置筛选</el-button>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Located Points"
      title="已定位风险点"
      :description="`已定位 ${locatedPoints.length} 个点位，未定位 ${unlocatedPoints.length} 个点位。`"
    >
      <div v-if="loading" class="chart-state">正在加载 GIS 点位...</div>
      <div v-else-if="locatedPoints.length === 0" class="chart-state">暂无可渲染的经纬度点位</div>
      <el-table v-else :data="locatedPoints" border>
        <el-table-column prop="riskPointName" label="风险点" min-width="160" />
        <el-table-column prop="regionName" label="区域" min-width="120" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskLevelTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="经纬度" min-width="220">
          <template #default="{ row }">
            {{ formatCoordinate(row.longitude, row.latitude) }}
          </template>
        </el-table-column>
        <el-table-column label="活跃告警" width="100">
          <template #default="{ row }">{{ row.activeAlarmCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="在线设备" width="100">
          <template #default="{ row }">{{ row.onlineDeviceCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetailByRiskPoint(row.riskPointId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </PanelCard>

    <PanelCard
      eyebrow="Fallback List"
      title="未定位风险点"
      description="无经纬度的风险点进入该列表。"
    >
      <div v-if="unlocatedPoints.length === 0" class="chart-state">当前没有未定位风险点</div>
      <div v-else class="unlocated-grid">
        <article v-for="point in unlocatedPoints" :key="point.riskPointId" class="unlocated-card">
          <div class="unlocated-card__header">
            <strong>{{ point.riskPointName || `风险点 ${point.riskPointId}` }}</strong>
            <el-tag :type="riskLevelTagType(point.riskLevel)">{{ riskLevelText(point.riskLevel) }}</el-tag>
          </div>
          <div class="unlocated-card__meta">
            <span>区域 {{ point.regionName || '--' }}</span>
            <span>设备数 {{ point.deviceCount ?? 0 }}</span>
            <span>在线设备 {{ point.onlineDeviceCount ?? 0 }}</span>
            <span>活跃告警 {{ point.activeAlarmCount ?? 0 }}</span>
          </div>
          <el-button type="primary" link @click="openDetailByRiskPoint(point.riskPointId)">查看详情</el-button>
        </article>
      </div>
    </PanelCard>

    <RiskMonitoringDetailDrawer v-model="detailVisible" :binding-id="activeBindingId" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';

import PanelCard from '../components/PanelCard.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import { getRiskMonitoringGisPoints, getRiskMonitoringList, type RiskMonitoringGisPoint } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';

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

const locatedPoints = computed(() =>
  points.value.filter((point) => point.longitude !== null && point.longitude !== undefined && point.latitude !== null && point.latitude !== undefined)
);

const unlocatedPoints = computed(() =>
  points.value.filter((point) => point.longitude === null || point.longitude === undefined || point.latitude === null || point.latitude === undefined)
);

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
  loading.value = true;
  try {
    const response = await getRiskMonitoringGisPoints(filters.regionId);
    points.value = response.data || [];
  } catch (error) {
    points.value = [];
    ElMessage.error(error instanceof Error ? error.message : 'GIS 点位加载失败');
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  filters.regionId = undefined;
  void loadPoints();
}

async function openDetailByRiskPoint(riskPointId: number) {
  try {
    const response = await getRiskMonitoringList({ riskPointId, pageNum: 1, pageSize: 1 });
    const bindingId = response.data.records?.[0]?.bindingId;
    if (!bindingId) {
      ElMessage.warning('当前风险点没有可用的监测绑定详情');
      return;
    }
    activeBindingId.value = bindingId;
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
  display: grid;
  gap: 1rem;
}

.filter-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.chart-state {
  color: #6b7a92;
}

.unlocated-grid {
  display: grid;
  gap: 0.75rem;
}

.unlocated-card {
  border: 1px solid #edf1f7;
  border-radius: 10px;
  padding: 0.8rem;
}

.unlocated-card__header {
  display: flex;
  justify-content: space-between;
}

.unlocated-card__meta {
  margin-top: 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #60708a;
  font-size: 12px;
}
</style>
