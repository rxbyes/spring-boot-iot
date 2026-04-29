<template>
  <PanelCard title="属性趋势预览">
    <div class="trend-toolbar" :class="{ 'trend-toolbar--compact': activeSeriesMeta }">
      <div class="trend-toolbar__meta">
        <span class="trend-toolbar__label">{{ activeSeriesMeta ? '当前聚焦' : '当前范围' }}</span>
        <div class="trend-toolbar__stats">
          <template v-if="activeSeriesMeta">
            <span class="trend-toolbar__pill trend-toolbar__pill--brand">{{ activeSeriesMeta.groupTitle }}</span>
            <span class="trend-toolbar__pill">{{ activeSeriesMeta.displayName }}</span>
            <span class="trend-toolbar__pill trend-toolbar__pill--muted">{{ rangeLabel }}</span>
          </template>
          <template v-else>
            <span class="trend-toolbar__pill">{{ rangeLabel }}</span>
            <span v-if="activeGroups.length" class="trend-toolbar__pill trend-toolbar__pill--muted">
              {{ activeGroups.length }} 个分组
            </span>
          </template>
        </div>
      </div>
      <el-segmented
        class="trend-toolbar__segmented"
        :model-value="rangeCode"
        :options="rangeOptions"
        @change="handleRangeChange"
      />
    </div>
    <div
      v-if="activeGroups.length"
      class="trend-groups"
      :class="{ 'trend-groups--continuous': activeGroups.length > 1 }"
    >
      <section
        v-for="group in activeGroups"
        :key="group.key"
        class="trend-group"
        :class="{
          'trend-group--integrated': activeGroups.length > 1,
          'trend-group--active': isGroupActive(group),
          'trend-group--muted': isGroupMuted(group)
        }"
      >
        <header
          class="trend-group__header"
          :class="{ 'trend-group__header--compact': activeGroups.length > 1 }"
        >
          <div class="trend-group__heading">
            <div class="trend-group__title-row">
              <strong>{{ group.title }}</strong>
              <span class="trend-group__summary-meta">{{ group.series.length }} 条序列</span>
            </div>
            <p>{{ resolveGroupSummary(group) }}</p>
          </div>
        </header>

        <div
          class="trend-group__focus-strip"
          :class="{ 'trend-group__focus-strip--compact': activeGroups.length > 1 }"
        >
          <button
            v-for="series in group.series"
            :key="`${group.key}-${series.identifier || series.displayName}`"
            type="button"
            class="trend-group__focus-button"
            :class="{
              'trend-group__focus-button--integrated': activeGroups.length > 1,
              'trend-group__focus-button--active': isSeriesActive(series),
              'trend-group__focus-button--muted': hasActiveIdentifier && !isSeriesActive(series)
            }"
            :data-testid="buildTrendSeriesFocusTestId(series.identifier || series.displayName)"
            @click="handleSeriesSelect(group, series)"
          >
            <strong>{{ series.displayName }}</strong>
            <small>{{ resolveSeriesPreview(series, group) }}</small>
          </button>
        </div>

        <div
          v-if="hasRenderableSamples(group)"
          :ref="(element) => registerChartRef(group.key, element)"
          class="trend-group__chart"
          :class="{
            'trend-group__chart--compact': shouldCompactGroup(group),
            'trend-group__chart--integrated': activeGroups.length > 1
          }"
        />
        <div v-else class="trend-group__empty">
          <strong class="trend-group__empty-title">{{ resolveGroupEmptyTitle(group) }}</strong>
          <p class="trend-group__empty-message">{{ resolveGroupEmptyMessage(group) }}</p>
        </div>
      </section>
    </div>
    <div v-else class="trend-empty-state">
      <span class="trend-empty-state__eyebrow">趋势样本尚未就绪</span>
      <strong class="trend-empty-state__title">{{ emptyStateTitle }}</strong>
      <p class="trend-empty-state__message">{{ emptyMessage }}</p>
      <div class="trend-empty-state__meta">
        <span class="trend-empty-state__range">{{ rangeLabel }}</span>
        <span class="trend-empty-state__hint">趋势变化和样本信号会在这里继续展开</span>
      </div>
    </div>
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

interface TrendSeriesSelectionPayload {
  groupKey: string;
  groupTitle: string;
  identifier: string;
  displayName: string;
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
  activeIdentifier?: string;
  emptyMessage?: string;
}>(), {
  rangeCode: '1d',
  groups: () => [],
  activeIdentifier: '',
  emptyMessage: '暂无趋势数据'
});

const emit = defineEmits<{
  (e: 'change-range', value: InsightRangeCode): void;
  (e: 'select-series', value: TrendSeriesSelectionPayload): void;
}>();

const chartRefs = new Map<string, HTMLDivElement>();
const chartInstances = new Map<string, ReturnType<typeof echarts.init>>();
let resizeObserver: ResizeObserver | null = null;

const activeGroups = computed(() =>
  (props.groups ?? []).filter((group) => Array.isArray(group.series) && group.series.length > 0)
);

const activeSeriesIdentifier = computed(() => normalizeSeriesSelectionIdentifier(props.activeIdentifier));
const hasActiveIdentifier = computed(() => Boolean(activeSeriesIdentifier.value));
const activeSeriesMeta = computed(() => {
  if (!activeSeriesIdentifier.value) {
    return null;
  }
  for (const group of activeGroups.value) {
    for (const series of group.series) {
      if (normalizeSeriesSelectionIdentifier(series.identifier) === activeSeriesIdentifier.value) {
        return {
          groupTitle: group.title,
          displayName: series.displayName
        };
      }
    }
  }
  return null;
});
const emptyStateTitle = computed(() =>
  props.emptyMessage.includes('请输入设备编码')
    ? '当前还没有趋势样本'
    : '当前范围内还没有趋势样本'
);

const activeGroupKey = computed(() => activeSeriesMeta.value?.groupTitle ?? '');

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

function handleSeriesSelect(group: TrendGroup, series: TrendSeries) {
  const identifier = series.identifier?.trim();
  if (!identifier) {
    return;
  }
  emit('select-series', {
    groupKey: group.key,
    groupTitle: group.title,
    identifier,
    displayName: series.displayName
  });
}

function normalizeSeriesSelectionIdentifier(value?: string) {
  return (value ?? '').trim().toLowerCase();
}

function isSeriesActive(series: TrendSeries) {
  if (!activeSeriesIdentifier.value) {
    return false;
  }
  return normalizeSeriesSelectionIdentifier(series.identifier) === activeSeriesIdentifier.value;
}

function isGroupActive(group: TrendGroup) {
  return Boolean(activeGroupKey.value) && group.title === activeGroupKey.value;
}

function isGroupMuted(group: TrendGroup) {
  return Boolean(activeGroupKey.value) && group.title !== activeGroupKey.value;
}

function buildTrendSeriesFocusTestId(value: string) {
  return `trend-series-focus-${value.trim().replace(/[^0-9A-Za-z]+/g, '_')}`;
}

function resolveSeriesPreview(series: TrendSeries, group: TrendGroup) {
  const latestBucket = [...series.buckets]
    .reverse()
    .find((bucket) => bucket.filled !== true && bucket.value !== null && bucket.value !== undefined);
  if (!latestBucket) {
    return `当前暂无${resolveGroupSampleLabel(group)}样本`;
  }
  if (shouldUseStepLine(series, group)) {
    return resolveStatusText(series, group, latestBucket.value, latestBucket.filled ?? false);
  }
  return `最近值 ${latestBucket.value}`;
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

function resolveGroupSampleLabel(group: TrendGroup) {
  if (isStatusEventGroup(group)) {
    return '状态';
  }
  if (isRuntimeStatusGroup(group)) {
    return '运行';
  }
  return '监测';
}

function resolveGroupEmptyTitle(group: TrendGroup) {
  if (isGroupActive(group) && activeSeriesMeta.value) {
    return `当前聚焦字段 ${activeSeriesMeta.value.displayName} 在${rangeLabel.value}内暂无${resolveGroupSampleLabel(group)}样本`;
  }
  return `当前范围内暂无${resolveGroupSampleLabel(group)}样本`;
}

function resolveGroupEmptyMessage(group: TrendGroup) {
  if (isGroupActive(group) && activeSeriesMeta.value) {
    return '可以切换时间范围后再回看当前聚焦字段。';
  }
  if (isStatusEventGroup(group)) {
    return '可以切换时间范围后再回看状态变化时点。';
  }
  if (isRuntimeStatusGroup(group)) {
    return '可以切换时间范围后再看这组运行信号。';
  }
  return '可以切换时间范围后再看这组变化。';
}

function hasRenderableSamples(group: TrendGroup) {
  return collectAxisLabels(group, props.rangeCode).length > 0;
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
    disposeChart(group.key);
    return;
  }

  const axisLabels = collectAxisLabels(group, props.rangeCode);
  if (!axisLabels.length) {
    disposeChart(group.key);
    return;
  }

  let chart = chartInstances.get(group.key);
  if (!chart) {
    chart = echarts.init(container);
    chartInstances.set(group.key, chart);
  }

  const useIntegratedChartLayout = activeGroups.value.length > 1;
  const yAxisConfig = buildYAxisConfig(group, useIntegratedChartLayout);

  chart.setOption({
    animationDuration: 300,
    color: group.series.map((_, index) => resolveTrendSeriesColor(colorOffset + index)),
    tooltip: {
      trigger: 'axis',
      padding: 0,
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(49, 72, 104, 0.18)',
      extraCssText: 'border-radius: 12px; overflow: hidden; box-shadow: 0 14px 36px rgba(31, 42, 61, 0.12);',
      textStyle: {
        color: '#1f2a3d'
      },
      formatter: (params: Array<Record<string, unknown>>) => formatTooltip(params)
    },
    legend: {
      type: 'scroll',
      top: useIntegratedChartLayout ? 2 : 4,
      left: useIntegratedChartLayout ? 16 : 20,
      right: useIntegratedChartLayout ? 16 : 20,
      itemGap: useIntegratedChartLayout ? 12 : 16,
      data: group.series.map((series) => series.displayName),
      textStyle: {
        color: '#5a6d85',
        fontSize: useIntegratedChartLayout ? 11 : 12
      }
    },
    grid: {
      top: useIntegratedChartLayout ? 58 : 64,
      right: useIntegratedChartLayout ? 20 : 24,
      bottom: useIntegratedChartLayout ? 48 : 52,
      left: useIntegratedChartLayout ? 24 : 28,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: axisLabels,
      axisLabel: {
        color: '#6c7e97',
        hideOverlap: true,
        margin: useIntegratedChartLayout ? 14 : 16,
        fontSize: useIntegratedChartLayout ? 11 : 12
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
      const isActive = !hasActiveIdentifier.value || isSeriesActive(series);
      return {
        name: series.displayName,
        type: 'line',
        smooth: false,
        step: useStepLine ? 'middle' : false,
        showSymbol: useStepLine ? false : props.rangeCode === '1d',
        symbolSize: isActive ? (props.rangeCode === '1d' ? 7 : 5) : 4,
        lineStyle: {
          width: isActive ? 3.2 : 2.2,
          opacity: isActive ? 1 : 0.35
        },
        itemStyle: {
          opacity: isActive ? 1 : 0.45
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

function disposeChart(key: string) {
  const chart = chartInstances.get(key);
  if (!chart) {
    return;
  }
  chart.dispose();
  chartInstances.delete(key);
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
  const axisValue = escapeTooltipHtml(String(params[0]?.axisValue ?? ''));
  const lines = params.map((item) => {
    const marker = String(item.marker ?? '');
    const seriesName = String(item.seriesName ?? '');
    const data = item.data as { value?: number; filled?: boolean; statusText?: string } | undefined;
    const value = data?.value ?? 0;
    const binaryStatusText = data?.filled ? '' : resolveBinaryStatusTextFromSemanticSource(seriesName, Number(value));
    const labelValue = data?.statusText || binaryStatusText || value;
    const plainText = escapeTooltipHtml(`${seriesName}：${String(labelValue)}`);
    const itemClassName = data?.statusText || binaryStatusText
      ? 'trend-tooltip__item-label trend-tooltip__value--status'
      : 'trend-tooltip__item-label';
    const itemStyle = data?.statusText || binaryStatusText
      ? 'color: #d55a12; font-weight: 600;'
      : 'color: #1f2a3d; font-weight: 500;';
    return `<div class="trend-tooltip__item" style="display:flex; align-items:flex-start; gap:6px; line-height:1.45;"><span class="${itemClassName}" style="${itemStyle}">${marker}${plainText}</span></div>`;
  });
  return `<div class="trend-tooltip" style="min-width: 168px; background: rgba(255,255,255,0.98);">
    <div class="trend-tooltip__header" style="padding: 10px 12px 8px; border-bottom: 1px solid rgba(67, 98, 148, 0.12); color: #5a6d85; font-size: 12px; font-weight: 600;">${axisValue}</div>
    <div class="trend-tooltip__body" style="display:grid; gap:6px; padding: 10px 12px 12px;">${lines.join('')}</div>
  </div>`;
}

function escapeTooltipHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
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

function buildYAxisConfig(group: TrendGroup, compactText = false) {
  const values = group.series
    .flatMap((series) => series.buckets.map((bucket) => Number(bucket.value)))
    .filter((value) => Number.isFinite(value));

  const baseConfig = {
    type: 'value' as const,
    splitNumber: 4,
    scale: true,
    axisLabel: {
      color: '#6c7e97',
      fontSize: compactText ? 11 : 12
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

.trend-toolbar--compact {
  gap: 0.6rem;
  margin-bottom: 0.8rem;
}

.trend-toolbar__meta {
  display: grid;
  gap: 0.38rem;
}

.trend-toolbar--compact .trend-toolbar__meta {
  gap: 0.24rem;
}

.trend-toolbar__label {
  color: var(--text-secondary);
  font-size: var(--type-label-size);
  font-weight: 600;
}

.trend-toolbar--compact .trend-toolbar__label {
  font-size: 0.74rem;
}

.trend-toolbar__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.trend-toolbar__pill {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.58rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}

.trend-toolbar__pill--brand {
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: color-mix(in srgb, var(--brand) 72%, var(--text-primary));
}

.trend-toolbar--compact .trend-toolbar__pill {
  padding: 0.24rem 0.54rem;
  font-size: 0.74rem;
}

.trend-toolbar__pill--muted {
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

.trend-groups--continuous {
  gap: 0.64rem;
}

.trend-group {
  display: grid;
  gap: 0.9rem;
}

.trend-group--integrated {
  gap: 0.82rem;
  padding: 0.86rem 0.92rem;
  border-color: color-mix(in srgb, var(--panel-border) 84%, white);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(252, 252, 254, 0.96));
  box-shadow: none;
}

.trend-group--active {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), color-mix(in srgb, var(--brand) 4%, white));
}

.trend-group--muted {
  border-color: color-mix(in srgb, var(--panel-border) 74%, white);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.96), rgba(255, 255, 255, 0.98));
}

.trend-group__header {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 0.75rem;
}

.trend-group__header--compact {
  gap: 0.52rem;
  margin-bottom: 0.12rem;
}

.trend-group__heading {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.trend-group__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
}

.trend-group__header strong {
  color: var(--text-primary);
}

.trend-group__summary-meta {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  font-weight: 600;
  white-space: nowrap;
}

.trend-group__heading p {
  margin: 0;
  color: var(--text-tertiary);
  line-height: 1.6;
}

.trend-group__focus-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.trend-group__focus-strip--compact {
  gap: 0.46rem;
  margin-top: -0.06rem;
  margin-bottom: 0.14rem;
}

.trend-group__focus-button {
  display: grid;
  gap: 0.18rem;
  min-width: 10rem;
  padding: 0.62rem 0.8rem;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(91, 109, 133, 0.14);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-secondary);
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.trend-group__focus-button--integrated {
  min-width: 9.4rem;
  padding: 0.56rem 0.72rem;
  border-color: color-mix(in srgb, var(--panel-border) 78%, white);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(249, 251, 255, 0.9));
  box-shadow: none;
}

.trend-group__focus-button strong {
  color: var(--text-primary);
  font-size: 0.83rem;
  font-weight: 600;
}

.trend-group__focus-button small {
  color: var(--text-tertiary);
  font-size: 0.74rem;
}

.trend-group__focus-button:hover {
  border-color: color-mix(in srgb, var(--brand) 24%, rgba(91, 109, 133, 0.18));
  background: color-mix(in srgb, var(--brand) 4%, white);
  transform: translateY(-1px);
}

.trend-group__focus-button--active {
  border-color: color-mix(in srgb, var(--brand) 46%, rgba(91, 109, 133, 0.2));
  background: color-mix(in srgb, var(--brand) 8%, white);
  box-shadow: 0 8px 18px rgba(255, 122, 26, 0.09);
}

.trend-group__focus-button--active small {
  color: var(--brand-strong);
}

.trend-group__focus-button--muted {
  opacity: 0.56;
}

.trend-group__chart {
  width: 100%;
  height: 20rem;
}

.trend-group__chart--integrated {
  height: 19rem;
  margin-top: -0.12rem;
}

.trend-group__chart--compact {
  height: 15rem;
}

.trend-empty-state {
  display: grid;
  gap: 0.5rem;
  padding: 1rem 1.05rem;
  border-radius: var(--radius-md);
  border: 1px dashed color-mix(in srgb, var(--brand) 24%, rgba(91, 109, 133, 0.18));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), color-mix(in srgb, var(--brand) 4%, white));
}

.trend-empty-state__eyebrow {
  color: var(--brand-strong);
  font-size: 0.73rem;
  font-weight: 700;
}

.trend-empty-state__title {
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 700;
}

.trend-empty-state__message {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.6;
  margin: 0;
}

.trend-empty-state__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.1rem;
}

.trend-empty-state__range,
.trend-empty-state__hint {
  display: inline-flex;
  align-items: center;
  padding: 0.24rem 0.56rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 600;
}

.trend-empty-state__range {
  color: var(--brand-strong);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.trend-empty-state__hint {
  color: var(--text-secondary);
  background: rgba(91, 109, 133, 0.08);
}

.trend-group__empty {
  display: grid;
  gap: 0.34rem;
  padding: 0.9rem 0.92rem;
  border-radius: var(--radius-sm);
  border: 1px dashed rgba(91, 109, 133, 0.18);
  background: linear-gradient(180deg, rgba(248, 250, 253, 0.92), rgba(255, 255, 255, 0.96));
}

.trend-group__empty-title {
  color: var(--text-primary);
  font-size: 0.83rem;
  font-weight: 700;
}

.trend-group__empty-message {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.76rem;
  line-height: 1.6;
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
