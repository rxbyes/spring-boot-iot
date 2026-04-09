<template>
  <PanelCard title="属性趋势预览">
    <div class="trend-toolbar">
      <span class="trend-toolbar__label">时间范围</span>
      <el-segmented
        class="trend-toolbar__segmented"
        :model-value="rangeCode"
        :options="rangeOptions"
        @change="handleRangeChange"
      />
    </div>
    <div v-if="activeGroups.length" class="trend-groups">
      <section v-for="group in activeGroups" :key="group.key" class="trend-group">
        <header class="trend-group__header">
          <strong>{{ group.title }}</strong>
        </header>

        <div :ref="(element) => registerChartRef(group.key, element)" class="trend-group__chart" />
      </section>
    </div>
    <div v-else class="empty-state">{{ emptyMessage }}</div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

import type { InsightRangeCode } from '@/api/telemetry';
import { INSIGHT_RANGE_OPTIONS } from '@/utils/deviceInsightCapability';
import PanelCard from './PanelCard.vue';

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

interface TrendBucketPoint {
  time: string;
  value: number;
  filled?: boolean;
}

interface TrendSeries {
  identifier?: string;
  displayName: string;
  seriesType?: 'measure' | 'status' | 'event' | string;
  buckets: TrendBucketPoint[];
}

interface TrendGroup {
  key: string;
  title: string;
  description?: string;
  series: TrendSeries[];
}

const TREND_SERIES_COLORS = [
  '#1f6feb',
  '#13b38b',
  '#ff7a1a',
  '#8f5cff',
  '#e55381',
  '#12a6c8',
  '#c28a00',
  '#2f855a',
  '#d9485f',
  '#5b8def',
  '#fb8c00',
  '#7c4dff'
];

const STATUS_EVENT_TEXT_MAP: Record<number, string> = {
  0: '正常',
  [-1]: '供电异常',
  [-2]: '传感器数据异常',
  [-3]: '采样间隔内未采集到数据'
};

const props = withDefaults(defineProps<{
  rangeCode?: InsightRangeCode;
  groups?: TrendGroup[];
  emptyMessage?: string;
}>(), {
  rangeCode: '7d',
  groups: () => [],
  emptyMessage: '暂无趋势数据'
});

const emit = defineEmits<{
  (e: 'change-range', value: InsightRangeCode): void;
}>();

const chartRefs = new Map<string, HTMLDivElement>();
const chartInstances = new Map<string, ReturnType<typeof echarts.init>>();
let resizeObserver: ResizeObserver | null = null;

const activeGroups = computed(() =>
  (props.groups ?? []).filter((group) => Array.isArray(group.series) && group.series.length > 0)
);

const rangeOptions = INSIGHT_RANGE_OPTIONS.map((item) => ({
  label: item.label,
  value: item.value
}));

watch(activeGroups, async () => {
  await nextTick();
  renderAllCharts();
}, { deep: true });

onMounted(async () => {
  await nextTick();
  renderAllCharts();
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstances.forEach((instance) => instance.resize());
    });
    observeCharts();
  }
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chartInstances.forEach((instance) => instance.dispose());
  chartInstances.clear();
  chartRefs.clear();
});

function registerChartRef(key: string, element: Element | null) {
  if (element instanceof HTMLDivElement) {
    chartRefs.set(key, element);
    if (resizeObserver) {
      resizeObserver.observe(element);
    }
    return;
  }
  const previous = chartRefs.get(key);
  if (previous && resizeObserver) {
    resizeObserver.unobserve(previous);
  }
  chartRefs.delete(key);
}

function observeCharts() {
  if (!resizeObserver) {
    return;
  }
  chartRefs.forEach((element) => resizeObserver?.observe(element));
}

function handleRangeChange(value: string | number | boolean) {
  if (typeof value !== 'string') {
    return;
  }
  emit('change-range', value as InsightRangeCode);
}

function renderAllCharts() {
  const activeKeys = new Set(activeGroups.value.map((group) => group.key));
  chartInstances.forEach((instance, key) => {
    if (!activeKeys.has(key)) {
      instance.dispose();
      chartInstances.delete(key);
    }
  });
  let colorOffset = 0;
  activeGroups.value.forEach((group) => {
    renderGroupChart(group, colorOffset);
    colorOffset += group.series.length;
  });
  observeCharts();
}

function renderGroupChart(group: TrendGroup, colorOffset: number) {
  const container = chartRefs.get(group.key);
  if (!container) {
    return;
  }

  const axisLabels = collectAxisLabels(group, props.rangeCode);
  if (!axisLabels.length) {
    return;
  }

  let chart = chartInstances.get(group.key);
  if (!chart) {
    chart = echarts.init(container);
    chartInstances.set(group.key, chart);
  }

  const yAxisConfig = buildYAxisConfig(group);

  chart.setOption({
    animationDuration: 300,
    color: group.series.map((_, index) => resolveTrendSeriesColor(colorOffset + index)),
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(49, 72, 104, 0.18)',
      textStyle: {
        color: '#1f2a3d'
      },
      formatter: (params: Array<Record<string, unknown>>) => formatTooltip(params)
    },
    legend: {
      top: 0,
      data: group.series.map((series) => series.displayName),
      textStyle: {
        color: '#5a6d85'
      }
    },
    grid: {
      top: 42,
      right: 18,
      bottom: 24,
      left: 16,
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
    yAxis: yAxisConfig,
    series: group.series.map((series) => {
      const useStepLine = shouldUseStepLine(series, group);
      return {
        name: series.displayName,
        type: 'line',
        smooth: false,
        step: useStepLine ? 'middle' : false,
        showSymbol: useStepLine ? false : props.rangeCode === '1d',
        symbolSize: props.rangeCode === '1d' ? 7 : 5,
        lineStyle: {
          width: 3
        },
        data: axisLabels.map((time) => {
          const bucket = series.buckets.find((item) => item.time === time);
          const rawValue = bucket?.value ?? 0;
          return {
            value: rawValue,
            filled: bucket?.filled ?? true,
            statusText: useStepLine ? resolveStatusText(series, group, rawValue) : undefined
          };
        })
      };
    })
  }, {
    notMerge: true
  });
}

function collectAxisLabels(group: TrendGroup, rangeCode?: InsightRangeCode) {
  const labels = Array.from(new Set(group.series.flatMap((series) => series.buckets.map((bucket) => bucket.time))))
    .sort((left, right) => {
      const leftTime = new Date(left).getTime();
      const rightTime = new Date(right).getTime();
      if (Number.isNaN(leftTime) || Number.isNaN(rightTime)) {
        return left.localeCompare(right);
      }
      return leftTime - rightTime;
    });
  if (rangeCode !== '1d') {
    return labels;
  }
  const firstActualIndex = labels.findIndex((time) => hasActualBucket(group, time));
  if (firstActualIndex < 0) {
    return labels;
  }
  let lastActualIndex = firstActualIndex;
  for (let index = labels.length - 1; index >= firstActualIndex; index -= 1) {
    if (hasActualBucket(group, labels[index])) {
      lastActualIndex = index;
      break;
    }
  }
  return labels.slice(firstActualIndex, lastActualIndex + 1);
}

function hasActualBucket(group: TrendGroup, time: string) {
  return group.series.some((series) =>
    series.buckets.some((bucket) => bucket.time === time && bucket.filled === false)
  );
}

function formatTooltip(params: Array<Record<string, unknown>>) {
  if (!Array.isArray(params) || !params.length) {
    return '';
  }
  const axisValue = String(params[0]?.axisValue ?? '');
  const lines = params.map((item) => {
    const marker = String(item.marker ?? '');
    const seriesName = String(item.seriesName ?? '');
    const data = item.data as { value?: number; filled?: boolean; statusText?: string } | undefined;
    const value = data?.value ?? 0;
    const labelValue = data?.statusText || resolveBinaryStatusTextFromSemanticSource(seriesName, Number(value)) || value;
    const suffix = data?.filled ? '（补零补齐）' : '';
    return `${marker}${seriesName}：${labelValue}${suffix}`;
  });
  return [axisValue, ...lines].join('<br/>');
}

function resolveTrendSeriesColor(index: number) {
  if (index < TREND_SERIES_COLORS.length) {
    return TREND_SERIES_COLORS[index];
  }
  const hue = (index * 47) % 360;
  return `hsl(${hue}, 68%, 48%)`;
}

function shouldUseStepLine(series: TrendSeries, group: TrendGroup) {
  const actualValues = extractActualValues(series);
  if (!actualValues.length) {
    return false;
  }

  if (series.seriesType === 'event' || group.key === 'status-event') {
    return actualValues.every((value) => Number.isInteger(value) && value >= -3 && value <= 1);
  }

  const isStatusSeries = series.seriesType === 'status' || group.key === 'status';
  if (!isStatusSeries) {
    return false;
  }
  return actualValues.every((value) => value === 0 || value === 1);
}

function resolveStatusText(series: TrendSeries, group: TrendGroup, value: number) {
  const numericValue = Number(value);
  if (series.seriesType === 'event' || group.key === 'status-event') {
    const eventText = resolveStatusEventText(series, numericValue);
    if (eventText) {
      return eventText;
    }
  }
  if (numericValue !== 0 && numericValue !== 1) {
    return String(value);
  }

  return resolveBinaryStatusTextFromSemanticSource(
    `${series.identifier ?? ''} ${series.displayName}`,
    numericValue
  );
}

function resolveStatusEventText(series: TrendSeries, numericValue: number) {
  if (!Number.isFinite(numericValue)) {
    return '';
  }
  if (isMappedStatusCodeSeries(series)) {
    return STATUS_EVENT_TEXT_MAP[numericValue] ?? (numericValue === 0 ? '正常' : '异常');
  }
  if (numericValue === 0 || numericValue === 1) {
    return resolveBinaryStatusTextFromSemanticSource(
      `${series.identifier ?? ''} ${series.displayName}`,
      numericValue
    );
  }
  return STATUS_EVENT_TEXT_MAP[numericValue] ?? (numericValue === 0 ? '正常' : '异常');
}

function resolveBinaryStatusTextFromSemanticSource(semanticSource: string, numericValue: number) {
  if (numericValue !== 0 && numericValue !== 1) {
    return '';
  }

  const normalizedSource = semanticSource.toLowerCase();
  if (/(sensor_state|online|在线)/.test(normalizedSource)) {
    return numericValue === 1 ? '在线' : '离线';
  }
  if (/(alarm|warn|告警|预警|报警)/.test(normalizedSource)) {
    return numericValue === 1 ? '告警' : '正常';
  }
  if (/(switch|enable|open|close|relay|valve|pump|door|light|horn|开关|启停|开启|关闭|阀|泵|门|声光)/.test(normalizedSource)) {
    return numericValue === 1 ? '开启' : '关闭';
  }
  return numericValue === 1 ? '是' : '否';
}

function buildYAxisConfig(group: TrendGroup) {
  const values = group.series
    .flatMap((series) => series.buckets.map((bucket) => Number(bucket.value)))
    .filter((value) => Number.isFinite(value));

  const baseConfig = {
    type: 'value' as const,
    splitNumber: 4,
    scale: true,
    axisLabel: {
      color: '#6c7e97'
    },
    splitLine: {
      lineStyle: {
        color: 'rgba(67, 98, 148, 0.12)'
      }
    }
  };

  if (group.key === 'status-event') {
    const actualValues = group.series.flatMap((series) => extractActualValues(series));
    if (!actualValues.length) {
      return baseConfig;
    }

    if (actualValues.every((value) => value === 0 || value === -1 || value === -2 || value === -3)) {
      return {
        ...baseConfig,
        min: -3,
        max: 0,
        interval: 1,
        splitNumber: 3,
        axisLabel: {
          ...baseConfig.axisLabel,
          formatter: (value: number) => STATUS_EVENT_TEXT_MAP[Number(value)] ?? ''
        }
      };
    }

    if (actualValues.every((value) => value === 0 || value === 1) && group.series.length === 1) {
      const semanticSource = `${group.series[0]?.identifier ?? ''} ${group.series[0]?.displayName ?? ''}`;
      return {
        ...baseConfig,
        min: 0,
        max: 1,
        interval: 1,
        splitNumber: 1,
        axisLabel: {
          ...baseConfig.axisLabel,
          formatter: (value: number) =>
            resolveBinaryStatusTextFromSemanticSource(semanticSource, Number(value)) || String(value)
        }
      };
    }

    return {
      ...baseConfig,
      min: Math.floor(Math.min(...actualValues)),
      max: Math.ceil(Math.max(...actualValues)),
      interval: 1
    };
  }

  if (!values.length) {
    return baseConfig;
  }

  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const dataSpan = maxValue - minValue;
  const midpoint = (maxValue + minValue) / 2;
  const targetSpan = dataSpan === 0
    ? Math.max(Math.abs(midpoint) * 0.12, midpoint === 0 ? 1 : Math.abs(midpoint) * 0.04)
    : Math.max(dataSpan * 1.8, Math.abs(midpoint) * 0.01);
  const padding = Math.max((targetSpan - dataSpan) / 2, 0);

  return {
    ...baseConfig,
    min: normalizeAxisNumber(minValue - padding),
    max: normalizeAxisNumber(maxValue + padding)
  };
}

function normalizeAxisNumber(value: number) {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Number(value.toFixed(6));
}

function extractActualValues(series: TrendSeries) {
  return series.buckets
    .filter((bucket) => bucket.filled === false)
    .map((bucket) => Number(bucket.value))
    .filter((value) => Number.isFinite(value));
}

function isMappedStatusCodeSeries(series: TrendSeries) {
  const actualValues = extractActualValues(series);
  return actualValues.length > 0
    && actualValues.every((value) => value === 0 || value === -1 || value === -2 || value === -3)
    && actualValues.some((value) => value < 0);
}

</script>

<style scoped>
.trend-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.trend-toolbar__label {
  color: var(--text-secondary);
  font-size: var(--type-label-size);
  font-weight: 600;
}

.trend-toolbar__segmented {
  flex: 0 0 auto;
}
.trend-group {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.trend-group__header strong {
  color: var(--text-primary);
}

.trend-groups {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

.trend-group {
  display: grid;
  gap: 0.9rem;
}

.trend-group__chart {
  width: 100%;
  height: 20rem;
}

@media (max-width: 1024px) {
  .trend-groups {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .trend-toolbar {
    align-items: stretch;
  }
}
</style>
