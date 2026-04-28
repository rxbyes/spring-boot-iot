<template>
  <PanelCard title="属性趋势预览">
    <div class="trend-toolbar">
      <div class="trend-toolbar__meta">
        <span class="trend-toolbar__label">????</span>
        <div class="trend-toolbar__stats">
          <span class="trend-toolbar__pill">{{ rangeLabel }}</span>
          <span v-if="activeGroups.length" class="trend-toolbar__pill trend-toolbar__pill--muted">
            {{ activeGroups.length }} 个分组
          </span>
        </div>
      </div>
      <el-segmented
        class="trend-toolbar__segmented"
        :model-value="rangeCode"
        :options="rangeOptions"
        @change="handleRangeChange"
      />
    </div><div v-if="activeGroups.length" class="trend-groups">
      <section v-for="group in activeGroups" :key="group.key" class="trend-group">
        <header class="trend-group__header">
          <div class="trend-group__heading">
            <strong>{{ group.title }}</strong>
            <p>{{ resolveGroupSummary(group) }}</p>
          </div>
          <span class="trend-group__series-pill">{{ group.series.length }} 条序列</span>
        </header>

        <div
          :ref="(element) => registerChartRef(group.key, element)"
          class="trend-group__chart"
          :class="{ 'trend-group__chart--compact': shouldCompactGroup(group) }"
        />
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
  [-4]: '未上报',
  0: '正常',
  [-1]: '供电异常',
  [-2]: '传感器数据异常',
  [-3]: '采样间隔内未采集到数据'
};
const STATUS_EVENT_MISSING_SENTINEL = -4;

const props = withDefaults(defineProps<{
  rangeCode?: InsightRangeCode;
  groups?: TrendGroup[];
  emptyMessage?: string;
}>(), {
  rangeCode: '1d',
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

const rangeLabel = computed(() => {
  const current = INSIGHT_RANGE_OPTIONS.find((item) => item.value === props.rangeCode);
  return current?.label || '近一天';
});

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

function resolveGroupSummary(group: TrendGroup) {
  if (group.description?.trim()) {
    return group.description.trim();
  }
  if (isStatusEventGroup(group)) {
    return '用于回放异常状态与变化时点。';
  }
  if (isRuntimeStatusGroup(group)) {
    return '用于查看运行参数和状态辅助信号。';
  }
  return '用于查看监测数据在当前时间范围内的变化趋势。';
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
      type: 'scroll',
      top: 4,
      left: 20,
      right: 20,
      itemGap: 16,
      data: group.series.map((series) => series.displayName),
      textStyle: {
        color: '#5a6d85'
      }
    },
    grid: {
      top: 64,
      right: 24,
      bottom: 52,
      left: 28,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: axisLabels,
      axisLabel: {
        color: '#6c7e97',
        hideOverlap: true,
        margin: 16
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
          const rawValue = resolveTrendPointValue(series, group, bucket);
          return {
            value: rawValue,
            filled: bucket?.filled ?? true,
            statusText: useStepLine ? resolveStatusText(series, group, rawValue, bucket?.filled ?? true) : undefined
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
    const binaryStatusText = data?.filled ? '' : resolveBinaryStatusTextFromSemanticSource(seriesName, Number(value));
    const labelValue = data?.statusText || binaryStatusText || value;
    return `${marker}${seriesName}：${labelValue}`;
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

  if (isStatusEventGroup(group, series)) {
    return actualValues.every((value) => Number.isInteger(value));
  }

  const isStatusSeries = series.seriesType === 'status' || isRuntimeStatusGroup(group);
  if (!isStatusSeries) {
    return false;
  }
  return actualValues.every((value) => value === 0 || value === 1);
}

function resolveStatusText(series: TrendSeries, group: TrendGroup, value: number, filled: boolean) {
  const numericValue = Number(value);
  if (isStatusEventGroup(group, series)) {
    const eventText = resolveStatusEventText(numericValue, filled);
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

function resolveStatusEventText(numericValue: number, filled: boolean) {
  if (filled || numericValue === STATUS_EVENT_MISSING_SENTINEL) {
    return STATUS_EVENT_TEXT_MAP[STATUS_EVENT_MISSING_SENTINEL];
  }
  if (!Number.isFinite(numericValue)) {
    return '';
  }
  if (STATUS_EVENT_TEXT_MAP[numericValue]) {
    return STATUS_EVENT_TEXT_MAP[numericValue];
  }
  if (numericValue === 0) {
    return '正常';
  }
  if (numericValue === 1) {
    return '异常';
  }
  return `异常(${numericValue})`;
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
  return '';
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

  if (isStatusEventGroup(group)) {
    const visualValues = group.series
      .flatMap((series) =>
        series.buckets.map((bucket) => bucket.filled ? STATUS_EVENT_MISSING_SENTINEL : Number(bucket.value))
      )
      .filter((value) => Number.isFinite(value));
    const visibleStatusValues = new Set(
      visualValues
        .map((value) => Math.round(value))
        .filter((value) => Number.isFinite(value))
    );
    if (!visualValues.length) {
      return baseConfig;
    }

    const minValue = Math.floor(Math.min(...visualValues)) - 1;
    const maxValue = Math.ceil(Math.max(...visualValues)) + 1;

    return {
      ...baseConfig,
      min: minValue,
      max: maxValue,
      interval: 1,
      splitNumber: Math.min(5, Math.max(2, maxValue - minValue)),
      axisLabel: {
        ...baseConfig.axisLabel,
        formatter: (value: number) => {
          const roundedValue = Math.round(Number(value));
          if (!visibleStatusValues.has(roundedValue)) {
            return '';
          }
          return resolveStatusEventText(roundedValue, roundedValue === STATUS_EVENT_MISSING_SENTINEL);
        }
      }
    };
  }

  if (!values.length) {
    return baseConfig;
  }

  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const dataSpan = maxValue - minValue;
  const midpoint = (maxValue + minValue) / 2;
  const padding = dataSpan === 0
    ? Math.max(Math.abs(midpoint) * 0.002, midpoint === 0 ? 1 : 0.2)
    : Math.max(dataSpan * 0.4, Math.abs(midpoint) * 0.0005, 0.02);

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

function resolveTrendPointValue(series: TrendSeries, group: TrendGroup, bucket?: TrendBucketPoint) {
  if (isStatusEventGroup(group, series) && bucket?.filled) {
    return STATUS_EVENT_MISSING_SENTINEL;
  }
  return bucket?.value ?? 0;
}

function isStatusEventGroup(group: TrendGroup, series?: TrendSeries) {
  return group.key === 'status-event'
    || group.key === 'statusEvent'
    || series?.seriesType === 'event';
}

function isRuntimeStatusGroup(group: TrendGroup) {
  return group.key === 'status'
    || group.key === 'status-runtime'
    || group.key === 'runtime';
}

function shouldCompactGroup(group: TrendGroup) {
  if (!isStatusEventGroup(group)) {
    return false;
  }
  const uniqueValues = new Set(
    group.series
      .flatMap((series) =>
        series.buckets
          .filter((bucket) => bucket.filled === false)
          .map((bucket) => Number(bucket.value))
      )
      .filter((value) => Number.isFinite(value))
  );
  return uniqueValues.size <= 2;
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

.trend-toolbar__meta {
  display: grid;
  gap: 0.38rem;
}

.trend-toolbar__label {
  color: var(--text-secondary);
  font-size: var(--type-label-size);
  font-weight: 600;
}

.trend-toolbar__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.trend-toolbar__pill,
.trend-group__series-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.58rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}

.trend-toolbar__pill--muted,
.trend-group__series-pill {
  background: rgba(91, 109, 133, 0.08);
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

.trend-groups {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

.trend-group {
  display: grid;
  gap: 0.9rem;
}

.trend-group__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.trend-group__heading {
  display: grid;
  gap: 0.28rem;
}

.trend-group__header strong {
  color: var(--text-primary);
}

.trend-group__heading p {
  margin: 0;
  color: var(--text-tertiary);
  line-height: 1.6;
}

.trend-group__chart {
  width: 100%;
  height: 20rem;
}

.trend-group__chart--compact {
  height: 15rem;
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

  .trend-group__header {
    flex-direction: column;
  }
}
</style>
