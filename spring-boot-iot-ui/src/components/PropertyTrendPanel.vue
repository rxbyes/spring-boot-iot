<template>
  <PanelCard
    eyebrow="Telemetry Preview"
    title="属性趋势预览"
  >
    <div v-if="seriesSummaries.length" class="trend-summary">
      <div v-for="item in seriesSummaries" :key="item.name" class="trend-summary__item">
        <span>{{ item.name }}</span>
        <strong>{{ item.latest }}</strong>
        <small>{{ item.min }} ~ {{ item.max }}</small>
      </div>
    </div>

    <div v-if="seriesSummaries.length" ref="chartRef" class="trend-chart" aria-label="设备属性趋势图" />
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import type { ECharts, SetOptionOpts } from 'echarts/core';
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

import type { DeviceMessageLog } from '../types/api';
import { formatDateTime, parseJsonSafely } from '../utils/format';
import PanelCard from './PanelCard.vue';

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

interface TrendSeriesSummary {
  name: string;
  latest: string;
  min: string;
  max: string;
  values: number[];
}

const props = defineProps<{
  logs: DeviceMessageLog[];
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const chartPayload = computed(() => {
  const buckets = new Map<string, Array<{ value: number; time: string }>>();
  const orderedLogs = [...props.logs].reverse();

  for (const log of orderedLogs) {
    const parsed = parseJsonSafely<{ properties?: Record<string, unknown> }>(log.payload || '');
    const properties = parsed?.properties;
    if (!properties) {
      continue;
    }

    for (const [key, rawValue] of Object.entries(properties)) {
      if (typeof rawValue !== 'number' || Number.isNaN(rawValue)) {
        continue;
      }

      const list = buckets.get(key) || [];
      list.push({
        value: rawValue,
        time: formatDateTime(log.reportTime || log.createTime)
      });
      buckets.set(key, list);
    }
  }

  const series = [...buckets.entries()]
    .filter(([, values]) => values.length >= 2)
    .slice(0, 4)
    .map(([name, values]) => ({
      name,
      values
    }));

  const xAxis = series[0]?.values.map((item) => item.time) || [];

  return {
    xAxis,
    series
  };
});

const seriesSummaries = computed<TrendSeriesSummary[]>(() =>
  chartPayload.value.series.map((item) => {
    const numericValues = item.values.map((entry) => entry.value);
    return {
      name: item.name,
      latest: String(item.values[item.values.length - 1]?.value ?? '--'),
      min: String(Math.min(...numericValues)),
      max: String(Math.max(...numericValues)),
      values: numericValues
    };
  })
);

watch(chartPayload, async () => {
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
  if (!chartRef.value || !seriesSummaries.value.length) {
    chartInstance?.dispose();
    chartInstance = null;
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const option = {
    animationDuration: 480,
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
      data: chartPayload.value.xAxis,
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
    series: chartPayload.value.series.map((item) => ({
      name: item.name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: {
        width: 3
      },
      areaStyle: {
        opacity: 0.08
      },
      data: item.values.map((entry) => entry.value)
    }))
  };

  const options: SetOptionOpts = {
    notMerge: true
  };
  chartInstance.setOption(option, options);
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
  gap: 0.25rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.trend-summary__item span,
.trend-summary__item small {
  color: var(--text-tertiary);
}

.trend-summary__item strong {
  font-family: var(--font-display);
  font-size: 1.35rem;
}

.trend-chart {
  height: 24rem;
  width: 100%;
}

@media (max-width: 1200px) {
  .trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .trend-summary {
    grid-template-columns: 1fr;
  }
}
</style>
