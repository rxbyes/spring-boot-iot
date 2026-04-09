<template>
  <PanelCard
    title="属性趋势预览"
    :description="panelDescription"
  >
    <div v-if="activeGroups.length" class="trend-groups">
      <section v-for="group in activeGroups" :key="group.key" class="trend-group">
        <header class="trend-group__header">
          <strong>{{ group.title }}</strong>
        </header>

        <div class="trend-group__legend">
          <span v-for="series in group.series" :key="series.displayName" class="trend-group__legend-item">
            {{ series.displayName }}
          </span>
        </div>

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

const props = withDefaults(defineProps<{
  rangeCode?: InsightRangeCode;
  groups?: TrendGroup[];
  emptyMessage?: string;
}>(), {
  rangeCode: '7d',
  groups: () => [],
  emptyMessage: '暂无趋势数据'
});

const chartRefs = new Map<string, HTMLDivElement>();
const chartInstances = new Map<string, ReturnType<typeof echarts.init>>();
let resizeObserver: ResizeObserver | null = null;

const activeGroups = computed(() =>
  (props.groups ?? []).filter((group) => Array.isArray(group.series) && group.series.length > 0)
);

const panelDescription = computed(() => `支持按${getRangeLabel(props.rangeCode)}查看设备监测数据趋势。`);

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

function getRangeLabel(rangeCode?: InsightRangeCode) {
  switch (rangeCode) {
    case '1d':
      return '近一天';
    case '30d':
      return '近一月';
    case '365d':
      return '近一年';
    case '7d':
    default:
      return '近一周';
  }
}
</script>

<style scoped>
.trend-group {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.trend-group__legend-item {
  color: var(--text-tertiary);
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

.trend-group__legend {
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

@media (max-width: 1024px) {
  .trend-groups {
    grid-template-columns: 1fr;
  }
}
</style>
