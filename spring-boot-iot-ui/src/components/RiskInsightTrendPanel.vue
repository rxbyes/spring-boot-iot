<template>
  <PanelCard
    title="属性趋势预览"
    :description="panelDescription"
  >
    <div v-if="summaryCards.length" class="trend-summary">
      <article v-for="item in summaryCards" :key="item.label" class="trend-summary__item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div v-if="activeSeries.length" ref="chartRef" class="trend-chart" aria-label="风险监测趋势图" />
    <div v-else class="empty-state">{{ emptyMessage }}</div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

import type { RiskMonitoringDetail } from '@/api/riskMonitoring';
import type { DeviceMessageLog } from '@/types/api';
import type { InsightObjectType } from '@/utils/deviceInsight';
import { getInsightObjectTypeLabel } from '@/utils/deviceInsight';
import { formatDateTime, parseJsonSafely } from '@/utils/format';
import PanelCard from './PanelCard.vue';

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

interface TrendSeriesPoint {
  time: string;
  value: number;
  rawValue: string;
}

interface TrendSeries {
  name: string;
  points: TrendSeriesPoint[];
}

interface TrendSummaryCard {
  label: string;
  value: string;
  hint: string;
}

const props = defineProps<{
  detail: RiskMonitoringDetail | null;
  logs: DeviceMessageLog[];
  objectType: InsightObjectType;
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: ReturnType<typeof echarts.init> | null = null;
let resizeObserver: ResizeObserver | null = null;

const riskPoints = computed(() =>
  props.detail?.trendPoints?.filter(
    (item) => typeof item.numericValue === 'number' && Number.isFinite(item.numericValue)
  ) ?? []
);

const fallbackSeries = computed<TrendSeries[]>(() => {
  const buckets = new Map<string, TrendSeriesPoint[]>();
  const orderedLogs = [...props.logs].sort((left, right) => {
    const leftTime = new Date(left.reportTime || left.createTime || 0).getTime();
    const rightTime = new Date(right.reportTime || right.createTime || 0).getTime();
    return leftTime - rightTime;
  });

  for (const log of orderedLogs) {
    const parsed = parseJsonSafely<Record<string, unknown>>(log.payload || '');
    if (!parsed || typeof parsed !== 'object') {
      continue;
    }

    const numericFields = extractNumericFields(parsed);
    if (!Object.keys(numericFields).length) {
      continue;
    }

    const time = formatDateTime(log.reportTime || log.createTime);
    for (const [key, value] of Object.entries(numericFields)) {
      const current = buckets.get(key) ?? [];
      current.push({
        time,
        value,
        rawValue: String(value)
      });
      buckets.set(key, current);
    }
  }

  return [...buckets.entries()]
    .filter(([, points]) => points.length >= 2)
    .slice(0, 4)
    .map(([name, points]) => ({
      name,
      points
    }));
});

const activeSeries = computed<TrendSeries[]>(() => {
  if (riskPoints.value.length) {
    return [{
      name: props.detail?.metricName || props.detail?.metricIdentifier || '监测趋势',
      points: riskPoints.value.map((item) => ({
        time: formatDateTime(item.reportTime),
        value: item.numericValue as number,
        rawValue: item.value || String(item.numericValue ?? '--')
      }))
    }];
  }

  return fallbackSeries.value;
});

const objectTypeLabel = computed(() => getInsightObjectTypeLabel(props.objectType));

const summaryCards = computed<TrendSummaryCard[]>(() => {
  if (props.detail) {
    const values = riskPoints.value.map((item) => item.numericValue as number);
    const latestValue = riskPoints.value[riskPoints.value.length - 1]?.value || props.detail.currentValue || '--';
    const rangeText = values.length ? `${Math.min(...values)} / ${Math.max(...values)}` : '--';
    return [
      {
        label: '对象类型',
        value: objectTypeLabel.value,
        hint: '当前绑定测点'
      },
      {
        label: '测点名称',
        value: props.detail.metricName || props.detail.metricIdentifier || '--',
        hint: props.detail.metricIdentifier || '--'
      },
      {
        label: '最新值',
        value: latestValue,
        hint: `最小 / 最大 ${rangeText}`
      },
      {
        label: '近 24h 点数',
        value: String(riskPoints.value.length),
        hint: props.detail.latestReportTime ? `最新上报 ${formatDateTime(props.detail.latestReportTime)}` : '暂无最近上报时间'
      }
    ];
  }

  return activeSeries.value.map((item, index) => {
    const values = item.points.map((point) => point.value);
    return {
      label: index === 0 ? '设备上报趋势' : item.name,
      value: item.points[item.points.length - 1]?.rawValue || '--',
      hint: index === 0 ? `${item.name} · ${Math.min(...values)} ~ ${Math.max(...values)}` : `${Math.min(...values)} ~ ${Math.max(...values)}`
    };
  });
});

const panelDescription = computed(
  () => props.detail
    ? `折线预览按${objectTypeLabel.value}对象口径展示当前绑定测点最近 24 小时的趋势。`
    : '设备上报趋势按最近消息中的数值属性展示，便于在未绑定风险监测时继续做综合分析。'
);

const emptyMessage = computed(() => {
  if (!props.detail) {
    return '当前设备未纳入风险监测绑定，且最近消息中未提取到可绘制的数值属性趋势。';
  }

  return riskPoints.value.length ? '趋势点不足 2 个，当前仅展示最近测点值。' : '暂无趋势点。';
});

watch(activeSeries, async () => {
  await nextTick();
  renderChart();
}, { deep: true });

onMounted(async () => {
  await nextTick();
  renderChart();

  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartRef.value);
  }
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chartInstance?.dispose();
  chartInstance = null;
});

function renderChart() {
  if (!chartRef.value || !activeSeries.value.length) {
    chartInstance?.dispose();
    chartInstance = null;
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const axisLabels = Array.from(new Set(
    activeSeries.value.flatMap((series) => series.points.map((point) => point.time))
  ));

  chartInstance.setOption({
    animationDuration: 360,
    color: ['#ff7a1a', '#1e80ff', '#f5b440', '#13b38b'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.2)',
      textStyle: {
        color: '#1f2a3d'
      }
    },
    legend: {
      top: 0,
      data: activeSeries.value.map((series) => series.name),
      textStyle: {
        color: '#5a6d85'
      }
    },
    grid: {
      top: 40,
      right: 12,
      bottom: 24,
      left: 18,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: axisLabels,
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
    series: activeSeries.value.map((series) => ({
      name: series.name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: {
        width: 3
      },
      areaStyle: {
        opacity: 0.08
      },
      data: axisLabels.map((time) => {
        const matchedPoint = series.points.find((point) => point.time === time);
        return matchedPoint ? matchedPoint.value : null;
      })
    }))
  }, {
    notMerge: true
  });
}

function extractNumericFields(source: Record<string, unknown>) {
  const candidates = [
    source.properties,
    source.telemetry,
    source.data,
    source
  ];
  const numericFields: Record<string, number> = {};

  for (const candidate of candidates) {
    if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) {
      continue;
    }

    for (const [key, value] of Object.entries(candidate)) {
      if (typeof value === 'number' && Number.isFinite(value) && !(key in numericFields)) {
        numericFields[key] = value;
      }
    }
  }

  return numericFields;
}
</script>

<style scoped>
.trend-summary {
  display: grid;
  gap: 0.9rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 1rem;
}

.trend-summary__item {
  display: grid;
  gap: 0.3rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.trend-summary__item span,
.trend-summary__item small {
  color: var(--text-tertiary);
}

.trend-summary__item strong {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.trend-chart {
  height: 24rem;
  width: 100%;
}

@media (max-width: 1024px) {
  .trend-summary {
    grid-template-columns: 1fr;
  }
}
</style>
