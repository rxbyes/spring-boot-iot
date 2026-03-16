<template>
  <div class="risk-gis-view">
    <PanelCard
      eyebrow="GIS Situation"
      title="GIS 风险态势"
      description="本轮 GIS 交付限定?ECharts 经纬度点位态势图，不包含第三方地图 SDK 或底图能力?
    >
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
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
          <el-col :span="16">
            <div class="filter-actions">
              <el-button type="primary" @click="loadPoints">刷新点位</el-button>
              <el-button @click="handleReset">重置筛?/el-button>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Scatter Situation"
      title="风险点位态势?
      :description="`已定?${locatedPoints.length} 个点位，未定?${unlocatedPoints.length} 个点位。颜色映射风险等级，大小映射活跃告警数。`"
    >
      <div v-if="loading" class="chart-state">正在加载 GIS 点位...</div>
      <div v-else-if="locatedPoints.length === 0" class="chart-state">暂无可渲染的经纬度点?/div>
      <div v-else ref="chartRef" class="gis-chart" />
    </PanelCard>

    <PanelCard
      eyebrow="Fallback List"
      title="未定位风险点"
      description="无经纬度的风险点进入该列表，避免页面静默丢失监测对象?
    >
      <div v-if="unlocatedPoints.length === 0" class="chart-state">当前没有未定位风险点</div>
      <div v-else class="unlocated-grid">
        <article
          v-for="point in unlocatedPoints"
          :key="point.riskPointId"
          class="unlocated-card"
        >
          <div class="unlocated-card__header">
            <strong>{{ point.riskPointName || `风险?${point.riskPointId}` }}</strong>
            <el-tag :type="riskLevelTagType(point.riskLevel)">{{ riskLevelText(point.riskLevel) }}</el-tag>
          </div>
          <div class="unlocated-card__meta">
            <span>区域 {{ point.regionName || '--' }}</span>
            <span>设备?{{ point.deviceCount ?? 0 }}</span>
            <span>在线设备 {{ point.onlineDeviceCount ?? 0 }}</span>
            <span>活跃告警 {{ point.activeAlarmCount ?? 0 }}</span>
          </div>
          <el-button type="primary" link @click="openDetailByRiskPoint(point.riskPointId)">
            查看详情
          </el-button>
        </article>
      </div>
    </PanelCard>

    <RiskMonitoringDetailDrawer
      v-model="detailVisible"
      :binding-id="activeBindingId"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import * as echarts from 'echarts/core';
import type { ECharts, SetOptionOpts } from 'echarts/core';
import { ScatterChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, VisualMapComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { ElMessage } from '@/utils/message';

import PanelCard from '../components/PanelCard.vue';
import RiskMonitoringDetailDrawer from '../components/RiskMonitoringDetailDrawer.vue';
import {
  getRiskMonitoringGisPoints,
  getRiskMonitoringList,
  type RiskMonitoringGisPoint
} from '../api/riskMonitoring';
import { getRiskPointList, type RiskPoint } from '../api/riskPoint';

echarts.use([ScatterChart, GridComponent, TooltipComponent, VisualMapComponent, CanvasRenderer]);

interface SelectOption {
  value: number;
  label: string;
}

const filters = reactive<{ regionId?: number }>({
  regionId: undefined
});

const loading = ref(false);
const points = ref<RiskMonitoringGisPoint[]>([]);
const riskPoints = ref<RiskPoint[]>([]);
const regionOptions = ref<SelectOption[]>([]);
const detailVisible = ref(false);
const activeBindingId = ref<number | null>(null);
const chartRef = ref<HTMLDivElement | null>(null);

let chartInstance: ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const locatedPoints = computed(() =>
  points.value.filter((point) => point.longitude !== null && point.longitude !== undefined && point.latitude !== null && point.latitude !== undefined)
);
const unlocatedPoints = computed(() =>
  points.value.filter((point) => point.longitude === null || point.longitude === undefined || point.latitude === null || point.latitude === undefined)
);

onMounted(async () => {
  await Promise.all([loadRegionOptions(), loadPoints()]);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chartInstance?.dispose();
  chartInstance = null;
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
    await nextTick();
    setupResizeObserver();
    renderChart();
  } catch (error) {
    points.value = [];
    ElMessage.error(error instanceof Error ? error.message : 'GIS 点位加载失败');
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  filters.regionId = undefined;
  loadPoints();
}

function setupResizeObserver() {
  if (!chartRef.value || typeof ResizeObserver === 'undefined') {
    return;
  }
  resizeObserver?.disconnect();
  resizeObserver = new ResizeObserver(() => {
    chartInstance?.resize();
  });
  resizeObserver.observe(chartRef.value);
}

function renderChart() {
  if (!chartRef.value || locatedPoints.value.length === 0) {
    chartInstance?.dispose();
    chartInstance = null;
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
    chartInstance.on('click', async (params) => {
      const riskPointId = (params.data as { riskPointId?: number } | undefined)?.riskPointId;
      if (!riskPointId) {
        return;
      }
      await openDetailByRiskPoint(riskPointId);
    });
  }

  const data = locatedPoints.value.map((point) => ({
    name: point.riskPointName || `风险?${point.riskPointId}`,
    value: [point.longitude, point.latitude, point.activeAlarmCount ?? 0],
    riskPointId: point.riskPointId,
    riskLevel: point.riskLevel,
    regionName: point.regionName,
    deviceCount: point.deviceCount ?? 0,
    onlineDeviceCount: point.onlineDeviceCount ?? 0,
    activeAlarmCount: point.activeAlarmCount ?? 0,
    symbolSize: calculatePointSize(point.activeAlarmCount)
  }));

  const option = {
    animationDuration: 420,
    grid: {
      top: 20,
      right: 18,
      bottom: 32,
      left: 48
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.2)',
      textStyle: {
        color: '#1f2a3d'
      },
      formatter: (params: { data: { name: string; value: number[]; riskLevel?: string; regionName?: string; deviceCount: number; onlineDeviceCount: number; activeAlarmCount: number } }) => {
        const point = params.data;
        return [
          `<strong>${point.name}</strong>`,
          `区域: ${point.regionName || '--'}`,
          `风险等级: ${riskLevelText(point.riskLevel)}`,
          `设备? ${point.deviceCount}`,
          `在线设备: ${point.onlineDeviceCount}`,
          `活跃告警: ${point.activeAlarmCount}`,
          `经度: ${point.value[0]}`,
          `纬度: ${point.value[1]}`
        ].join('<br/>');
      }
    },
    xAxis: {
      type: 'value',
      name: '经度',
      axisLabel: {
        color: '#6c7e97'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(67, 98, 148, 0.14)'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '纬度',
      axisLabel: {
        color: '#6c7e97'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(67, 98, 148, 0.14)'
        }
      }
    },
    series: [
      {
        type: 'scatter',
        data,
        symbolSize: (value: number[], params: { data: { symbolSize: number } }) => params.data.symbolSize,
        itemStyle: {
          color: (params: { data: { riskLevel?: string } }) => riskLevelColor(params.data.riskLevel),
          shadowBlur: 12,
          shadowColor: 'rgba(255, 106, 0, 0.22)'
        }
      }
    ]
  };

  const options: SetOptionOpts = { notMerge: true };
  chartInstance.setOption(option, options);
}

async function openDetailByRiskPoint(riskPointId: number) {
  try {
    const response = await getRiskMonitoringList({
      riskPointId,
      pageNum: 1,
      pageSize: 1
    });
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

function calculatePointSize(activeAlarmCount?: number | null) {
  const count = activeAlarmCount ?? 0;
  return Math.max(12, Math.min(34, 12 + count * 4));
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

function riskLevelColor(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '#ff5f5f';
    case 'WARNING':
    case 'MEDIUM':
      return '#f5b440';
    case 'INFO':
    case 'LOW':
      return '#13b38b';
    default:
      return '#1e80ff';
  }
}
</script>

<style scoped>
.risk-gis-view {
  display: grid;
  gap: 1rem;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.95rem;
}

.gis-chart {
  width: 100%;
  height: 28rem;
}

.chart-state {
  padding: 2rem 1rem;
  text-align: center;
  border-radius: var(--radius-md);
  border: 1px dashed var(--panel-border);
  color: var(--text-secondary);
}

.unlocated-grid {
  display: grid;
  gap: 0.85rem;
}

.unlocated-card {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.unlocated-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.unlocated-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem 1rem;
  margin: 0.75rem 0;
  color: var(--text-secondary);
}

@media (max-width: 960px) {
  .filter-actions {
    justify-content: flex-start;
    margin-top: 0;
  }
}
</style>

