<template>
  <el-drawer
    :model-value="modelValue"
    size="48rem"
    direction="rtl"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <template #header>
      <div class="drawer-header">
        <div>
          <p class="drawer-eyebrow">Risk Monitoring Detail</p>
          <h2>{{ detail?.riskPointName || detail?.deviceName || '监测详情' }}</h2>
        </div>
        <div class="drawer-statuses">
          <el-tag :type="riskLevelTagType(detail?.riskLevel)">{{ riskLevelText(detail?.riskLevel) }}</el-tag>
          <el-tag :type="monitorStatusTagType(detail?.monitorStatus)">{{ monitorStatusText(detail?.monitorStatus) }}</el-tag>
          <el-tag :type="detail?.onlineStatus === 1 ? 'success' : 'info'">
            {{ detail?.onlineStatus === 1 ? '在线' : '离线' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="drawer-body">
      <div v-if="loading" class="drawer-state">正在加载监测详情...</div>
      <div v-else-if="errorMessage" class="drawer-state drawer-state--error">{{ errorMessage }}</div>
      <div v-else-if="!detail" class="drawer-state">暂无详情数据</div>
      <template v-else>
        <PanelCard
          eyebrow="Current Snapshot"
          title="当前监测信息"
          description="统一承接实时监测列表�?GIS 风险态势的详情入口�?
        >
          <div class="snapshot-grid">
            <div class="snapshot-item">
              <span>设备编码</span>
              <strong>{{ detail.deviceCode || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>设备名称</span>
              <strong>{{ detail.deviceName || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>产品名称</span>
              <strong>{{ detail.productName || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>区域</span>
              <strong>{{ detail.regionName || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>风险�?/span>
              <strong>{{ detail.riskPointName || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>测点</span>
              <strong>{{ detail.metricName || detail.metricIdentifier || '--' }}</strong>
            </div>
            <div class="snapshot-item">
              <span>当前�?/span>
              <strong>{{ formatCurrentValue(detail.currentValue, detail.unit) }}</strong>
            </div>
            <div class="snapshot-item">
              <span>最新上�?/span>
              <strong>{{ formatDateTime(detail.latestReportTime) }}</strong>
            </div>
            <div class="snapshot-item">
              <span>活跃告警�?/span>
              <strong>{{ detail.activeAlarmCount ?? 0 }}</strong>
            </div>
            <div class="snapshot-item">
              <span>近期事件�?/span>
              <strong>{{ detail.recentEventCount ?? 0 }}</strong>
            </div>
            <div class="snapshot-item">
              <span>经纬�?/span>
              <strong>{{ formatCoordinate(detail.longitude, detail.latitude) }}</strong>
            </div>
            <div class="snapshot-item">
              <span>位置描述</span>
              <strong>{{ detail.address || '--' }}</strong>
            </div>
          </div>
        </PanelCard>

        <PanelCard
          eyebrow="24h Trend"
          title="最�?24 小时趋势"
          description="趋势图为空时显示兜底提示，便于真实环境验收判断空态�?
        >
          <div v-if="trendPoints.length" ref="trendChartRef" class="trend-chart" />
          <div v-else class="empty-block">最�?24 小时暂无趋势数据</div>
        </PanelCard>

        <div class="summary-grid">
          <PanelCard
            eyebrow="Recent Alarms"
            title="最近告�?
            description="展示最近触发的告警摘要�?
          >
            <div v-if="recentAlarms.length" class="summary-list">
              <article
                v-for="alarm in recentAlarms"
                :key="alarm.id"
                class="summary-card"
              >
                <div class="summary-card__header">
                  <strong>{{ alarm.alarmTitle || alarm.alarmCode || `告警 ${alarm.id}` }}</strong>
                  <el-tag :type="riskLevelTagType(alarm.alarmLevel)">{{ riskLevelText(alarm.alarmLevel) }}</el-tag>
                </div>
                <div class="summary-card__meta">
                  <span>当前�?{{ alarm.currentValue || '--' }}</span>
                  <span>阈�?{{ alarm.thresholdValue || '--' }}</span>
                  <span>{{ formatDateTime(alarm.triggerTime) }}</span>
                </div>
              </article>
            </div>
            <div v-else class="empty-block">暂无最近告�?/div>
          </PanelCard>

          <PanelCard
            eyebrow="Recent Events"
            title="最近事�?
            description="展示与当前监测对象相关的事件摘要�?
          >
            <div v-if="recentEvents.length" class="summary-list">
              <article
                v-for="event in recentEvents"
                :key="event.id"
                class="summary-card"
              >
                <div class="summary-card__header">
                  <strong>{{ event.eventTitle || event.eventCode || `事件 ${event.id}` }}</strong>
                  <el-tag :type="riskLevelTagType(event.riskLevel)">{{ riskLevelText(event.riskLevel) }}</el-tag>
                </div>
                <div class="summary-card__meta">
                  <span>当前�?{{ event.currentValue || '--' }}</span>
                  <span>状�?{{ eventStatusText(event.status) }}</span>
                  <span>{{ formatDateTime(event.triggerTime) }}</span>
                </div>
              </article>
            </div>
            <div v-else class="empty-block">暂无最近事�?/div>
          </PanelCard>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import type { ECharts, SetOptionOpts } from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { ElMessage } from '@/utils/message';

import PanelCard from './PanelCard.vue';
import {
  getRiskMonitoringDetail,
  type RiskMonitoringAlarmSummary,
  type RiskMonitoringDetail,
  type RiskMonitoringEventSummary,
  type RiskMonitoringTrendPoint
} from '../api/riskMonitoring';
import { formatDateTime } from '../utils/format';

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{
  modelValue: boolean;
  bindingId: number | null;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const loading = ref(false);
const errorMessage = ref('');
const detail = ref<RiskMonitoringDetail | null>(null);
const trendChartRef = ref<HTMLDivElement | null>(null);

let trendChart: ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const trendPoints = computed<RiskMonitoringTrendPoint[]>(() => detail.value?.trendPoints ?? []);
const recentAlarms = computed<RiskMonitoringAlarmSummary[]>(() => detail.value?.recentAlarms ?? []);
const recentEvents = computed<RiskMonitoringEventSummary[]>(() => detail.value?.recentEvents ?? []);

watch(
  () => [props.modelValue, props.bindingId] as const,
  async ([visible, bindingId]) => {
    if (!visible || !bindingId) {
      return;
    }
    await loadDetail(bindingId);
  },
  { immediate: true }
);

watch(trendPoints, async () => {
  await nextTick();
  renderTrendChart();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  trendChart?.dispose();
  trendChart = null;
});

async function loadDetail(bindingId: number) {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await getRiskMonitoringDetail(bindingId);
    detail.value = response.data;
    await nextTick();
    setupResizeObserver();
    renderTrendChart();
  } catch (error) {
    detail.value = null;
    errorMessage.value = error instanceof Error ? error.message : '加载详情失败';
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
}

function setupResizeObserver() {
  if (!trendChartRef.value || typeof ResizeObserver === 'undefined') {
    return;
  }
  resizeObserver?.disconnect();
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize();
  });
  resizeObserver.observe(trendChartRef.value);
}

function renderTrendChart() {
  if (!trendChartRef.value || !trendPoints.value.length) {
    trendChart?.dispose();
    trendChart = null;
    return;
  }

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  }

  const option = {
    animationDuration: 420,
    color: ['#ff8f1f'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(255, 143, 31, 0.24)',
      textStyle: {
        color: '#1f2a3d'
      }
    },
    grid: {
      top: 18,
      right: 12,
      bottom: 26,
      left: 18,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendPoints.value.map((point) => formatDateTime(point.reportTime)),
      axisLabel: {
        color: '#6c7e97',
        hideOverlap: true
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(67, 98, 148, 0.24)'
        }
      }
    },
    yAxis: {
      type: 'value',
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
        name: '当前�?,
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3
        },
        areaStyle: {
          opacity: 0.12
        },
        data: trendPoints.value.map((point) => {
          if (typeof point.numericValue === 'number') {
            return point.numericValue;
          }
          const numeric = Number(point.value);
          return Number.isFinite(numeric) ? numeric : null;
        })
      }
    ]
  };

  const options: SetOptionOpts = { notMerge: true };
  trendChart.setOption(option, options);
}

function riskLevelText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '严重';
    case 'HIGH':
      return '�?;
    case 'WARNING':
    case 'MEDIUM':
      return '�?;
    case 'INFO':
    case 'LOW':
      return '�?;
    default:
      return value || '未标�?;
  }
}

function riskLevelTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
    case 'HIGH':
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
      return '告警�?;
    case 'OFFLINE':
      return '离线';
    case 'NO_DATA':
      return '无数�?;
    case 'NORMAL':
      return '正常';
    default:
      return value || '未识�?;
  }
}

function monitorStatusTagType(value?: string | null): 'danger' | 'warning' | 'info' | 'success' {
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

function eventStatusText(status?: number | null) {
  switch (status) {
    case 0:
      return '待处�?;
    case 1:
      return '处理�?;
    case 2:
      return '已完�?;
    case 3:
      return '已关�?;
    default:
      return status === null || status === undefined ? '--' : String(status);
  }
}

function formatCurrentValue(value?: string | null, unit?: string | null) {
  if (!value) {
    return '--';
  }
  return unit ? `${value} ${unit}` : value;
}

function formatCoordinate(longitude?: number | null, latitude?: number | null) {
  if (longitude === null || longitude === undefined || latitude === null || latitude === undefined) {
    return '--';
  }
  return `${longitude.toFixed(6)}, ${latitude.toFixed(6)}`;
}
</script>

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.drawer-eyebrow {
  margin: 0 0 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.drawer-header h2 {
  margin: 0;
  font-size: 1.35rem;
}

.drawer-statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-end;
}

.drawer-body {
  display: grid;
  gap: 1rem;
}

.drawer-state {
  padding: 2rem 1rem;
  text-align: center;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
  color: var(--text-secondary);
}

.drawer-state--error {
  color: #ff9d86;
}

.snapshot-grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.snapshot-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.snapshot-item span {
  color: var(--text-tertiary);
  font-size: 0.82rem;
}

.snapshot-item strong {
  font-size: 1rem;
  word-break: break-word;
}

.trend-chart {
  width: 100%;
  height: 18rem;
}

.summary-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-list {
  display: grid;
  gap: 0.85rem;
}

.summary-card {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.summary-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.summary-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem 1rem;
  margin-top: 0.7rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.empty-block {
  padding: 1.5rem 1rem;
  border-radius: var(--radius-md);
  border: 1px dashed var(--panel-border);
  color: var(--text-secondary);
  text-align: center;
}

@media (max-width: 960px) {
  .snapshot-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .drawer-header {
    flex-direction: column;
  }

  .drawer-statuses {
    justify-content: flex-start;
  }
}
</style>

