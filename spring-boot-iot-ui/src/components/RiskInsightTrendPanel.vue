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

    <div v-if="activeGroups.length" class="trend-groups">
      <section v-for="group in activeGroups" :key="group.key" class="trend-group">
        <header class="trend-group__header">
          <div>
            <strong>{{ group.title }}</strong>
            <p>{{ group.description || getGroupDescription(group.key) }}</p>
          </div>
        </header>

        <div class="trend-group__legend">
          <span v-for="series in group.series" :key="series.displayName" class="trend-group__legend-item">
            {{ series.displayName }}
          </span>
        </div>

        <div :ref="(element) => registerChartRef(group.key, element)" class="trend-group__chart" />

        <div class="trend-group__notes">
          <article v-for="series in group.series" :key="`${group.key}-${series.displayName}`" class="trend-group__note">
            <strong>{{ series.displayName }}</strong>
            <span>{{ buildSeriesLatestText(series) }}</span>
          </article>
        </div>
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
import PanelCard from './PanelCard.vue';

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

interface TrendSummaryCard {
  label: string;
  value: string;
  hint: string;
}

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

const props = withDefaults(defineProps<{
  rangeCode?: InsightRangeCode;
  groups?: TrendGroup[];
  summary?: TrendSummaryCard[];
  emptyMessage?: string;
}>(), {
  rangeCode: '7d',
  groups: () => [],
  summary: () => [],
  emptyMessage: '暂无趋势数据'
});

const chartRefs = new Map<string, HTMLDivElement>();
const chartInstances = new Map<string, ReturnType<typeof echarts.init>>();
let resizeObserver: ResizeObserver | null = null;

const activeGroups = computed(() =>
  (props.groups ?? []).filter((group) => Array.isArray(group.series) && group.series.length > 0)
);

const summaryCards = computed(() => props.summary ?? []);

const panelDescription = computed(() => `按${getRangeLabel(props.rangeCode)}展示设备监测数据与状态数据折线趋势，缺失桶已按 0 补齐。`);

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

function renderAllCharts() {
  const activeKeys = new Set(activeGroups.value.map((group) => group.key));
  chartInstances.forEach((instance, key) => {
    if (!activeKeys.has(key)) {
      instance.dispose();
      chartInstances.delete(key);
    }
  });
  activeGroups.value.forEach((group) => renderGroupChart(group));
  observeCharts();
}

function renderGroupChart(group: TrendGroup) {
  const container = chartRefs.get(group.key);
  if (!container) {
    return;
  }

  const axisLabels = collectAxisLabels(group);
  if (!axisLabels.length) {
    return;
  }

  let chart = chartInstances.get(group.key);
  if (!chart) {
    chart = echarts.init(container);
    chartInstances.set(group.key, chart);
  }

  chart.setOption({
    animationDuration: 300,
    color: group.key === 'measure'
      ? ['#1f6feb', '#4da3ff', '#13b38b']
      : ['#ff7a1a', '#f5b440', '#6a8dff'],
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
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#6c7e97'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(67, 98, 148, 0.12)'
        }
      }
    },
    series: group.series.map((series) => ({
      name: series.displayName,
      type: 'line',
      smooth: false,
      showSymbol: false,
      lineStyle: {
        width: 3
      },
      data: axisLabels.map((time) => {
        const bucket = series.buckets.find((item) => item.time === time);
        return {
          value: bucket?.value ?? 0,
          filled: bucket?.filled ?? true
        };
      })
    }))
  }, {
    notMerge: true
  });
}

function collectAxisLabels(group: TrendGroup) {
  return Array.from(new Set(group.series.flatMap((series) => series.buckets.map((bucket) => bucket.time))));
}

function formatTooltip(params: Array<Record<string, unknown>>) {
  if (!Array.isArray(params) || !params.length) {
    return '';
  }
  const axisValue = String(params[0]?.axisValue ?? '');
  const lines = params.map((item) => {
    const marker = String(item.marker ?? '');
    const seriesName = String(item.seriesName ?? '');
    const data = item.data as { value?: number; filled?: boolean } | undefined;
    const value = data?.value ?? 0;
    const suffix = data?.filled ? '（补零补齐）' : '';
    return `${marker}${seriesName}：${value}${suffix}`;
  });
  return [axisValue, ...lines].join('<br/>');
}

function buildSeriesLatestText(series: TrendSeries) {
  const latestBucket = [...series.buckets].reverse().find((bucket) => bucket && bucket.time);
  if (!latestBucket) {
    return '当前范围暂无有效数据';
  }
  return latestBucket.filled
    ? `最近桶值 ${latestBucket.value}（补零补齐）`
    : `最近桶值 ${latestBucket.value}`;
}

function getGroupDescription(groupKey: string) {
  return groupKey === 'measure'
    ? '展示设备本体的监测值折线变化。'
    : '展示设备在线状态、电量等状态值折线变化。';
}

function getRangeLabel(rangeCode?: InsightRangeCode) {
  switch (rangeCode) {
    case '1d':
      return '近一天';
    case '30d':
      return '近一月';
    case '90d':
      return '近一季度';
    case '365d':
      return '近一年';
    case '7d':
    default:
      return '近一周';
  }
}
</script>

<style scoped>
.trend-summary {
  display: grid;
  gap: 0.9rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 1rem;
}

.trend-summary__item,
.trend-group,
.trend-group__note {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.trend-summary__item {
  display: grid;
  gap: 0.3rem;
}

.trend-summary__item span,
.trend-summary__item small,
.trend-group__header p,
.trend-group__legend-item,
.trend-group__note span {
  color: var(--text-tertiary);
}

.trend-summary__item strong,
.trend-group__header strong,
.trend-group__note strong {
  color: var(--text-primary);
}

.trend-summary__item strong {
  font-size: 1.2rem;
  font-family: var(--font-display);
}

.trend-groups {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.trend-group {
  display: grid;
  gap: 0.9rem;
}

.trend-group__header p {
  margin: 0.35rem 0 0;
  line-height: 1.6;
}

.trend-group__legend,
.trend-group__notes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.trend-group__legend-item {
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.trend-group__chart {
  width: 100%;
  height: 20rem;
}

.trend-group__note {
  display: grid;
  gap: 0.25rem;
  min-width: 10rem;
}

@media (max-width: 1024px) {
  .trend-summary,
  .trend-groups {
    grid-template-columns: 1fr;
  }
}
</style>
