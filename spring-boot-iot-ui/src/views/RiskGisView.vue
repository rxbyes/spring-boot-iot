<template>
  <div class="ops-workbench risk-gis-view">
    <PanelCard
      eyebrow="GIS Situation"
      title="GIS态势图"
      description="统一呈现风险点空间分布、定位质量与监测入口，帮助值班人员快速掌握 GIS 风险态势。"
      class="ops-hero-card"
    >
      <div class="ops-kpi-grid">
        <MetricCard label="风险点总数" :value="String(totalPoints)" :badge="{ label: 'GIS 总览', tone: 'brand' }" />
        <MetricCard label="已定位点位" :value="String(locatedPoints.length)" :badge="{ label: '可联动', tone: 'success' }" />
        <MetricCard label="未定位点位" :value="String(unlocatedPoints.length)" :badge="{ label: '待补齐', tone: 'warning' }" />
        <MetricCard label="活跃告警" :value="String(activeAlarmTotal)" :badge="{ label: '重点关注', tone: 'danger' }" />
      </div>
      <div class="ops-inline-note">
        当前覆盖 {{ onlineDeviceTotal }} 台在线设备，可从点位列表直接进入统一监测详情抽屉，持续保持监测预警平台的工作台风格。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="GIS Filters"
      title="筛选条件"
      description="按区域聚焦 GIS 点位分布，优先补齐未定位风险点并联动查看监测详情。"
      class="ops-filter-card"
    >
      <el-form :model="filters" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="区域">
              <el-select v-model="filters.regionId" clearable placeholder="全部区域">
                <el-option v-for="region in regionOptions" :key="region.value" :label="region.label" :value="region.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检建议">
              <el-input :model-value="gisAdvice" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="ops-filter-actions">
          <StandardButton action="refresh" @click="loadPoints">刷新点位</StandardButton>
          <StandardButton action="reset" @click="handleReset">重置筛选</StandardButton>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Located Points"
      title="已定位风险点"
      :description="`已定位 ${locatedPoints.length} 个点位，可直接联动查看监测详情。`"
      class="ops-table-card"
    >
      <StandardTableToolbar
        :meta-items="[`已定位 ${locatedPoints.length} 个`, `活跃告警 ${activeAlarmTotal} 条`, `在线设备 ${onlineDeviceTotal} 台`]"
      >
        <template #right>
          <StandardButton action="refresh" link @click="loadPoints">刷新点位</StandardButton>
        </template>
      </StandardTableToolbar>

      <div v-if="loading" class="ops-state">正在加载 GIS 点位...</div>
      <div v-else-if="locatedPoints.length === 0" class="ops-state">暂无可渲染的经纬度点位</div>
      <el-table v-else :data="locatedPoints" border stripe>
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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide">
              <StandardActionLink @click="openDetailByRiskPoint(row.riskPointId)">详情</StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>
    </PanelCard>

    <PanelCard
      eyebrow="Fallback List"
      title="未定位风险点"
      description="无经纬度点位的风险点统一收拢为资源卡，方便补齐坐标与继续查看详情。"
      class="ops-table-card"
    >
      <div v-if="loading" class="ops-state">正在同步未定位风险点...</div>
      <div v-else-if="unlocatedPoints.length === 0" class="ops-state">当前没有未定位风险点</div>
      <div v-else class="ops-resource-grid">
        <article v-for="point in unlocatedPoints" :key="point.riskPointId" class="ops-resource-card">
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
            <StandardRowActions variant="card" gap="wide">
              <StandardActionLink @click="openDetailByRiskPoint(point.riskPointId)">详情</StandardActionLink>
            </StandardRowActions>
          </div>
        </article>
      </div>
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
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import { getRiskMonitoringGisPoints, getRiskMonitoringList, type RiskMonitoringGisPoint } from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';
import type { IdType } from '../types/api';

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
  padding: 18px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(245, 249, 253, 0.58));
  border: 1px solid rgba(41, 60, 92, 0.08);
  box-shadow: var(--shadow-inset-highlight-72);
}
</style>
