<template>
  <PanelCard
    eyebrow="Telemetry Preview"
    title="属性趋势预览"
    description="基于最近消息日志里的数值属性生成轻量趋势图，为后续接入 ECharts 或时序库打基础。"
  >
    <div v-if="series.length" class="trend-grid">
      <article v-for="item in series" :key="item.name" class="trend-card">
        <div class="trend-card__head">
          <div>
            <p class="trend-card__eyebrow">{{ item.name }}</p>
            <h3>{{ item.latest }}</h3>
          </div>
          <span class="trend-card__delta">{{ item.min }} ~ {{ item.max }}</span>
        </div>

        <svg
          class="trend-card__chart"
          viewBox="0 0 240 88"
          preserveAspectRatio="none"
          role="img"
          :aria-label="`${item.name} 趋势图`"
        >
          <defs>
            <linearGradient :id="`${item.name}-gradient`" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#39f1ff" />
              <stop offset="100%" stop-color="#f5b440" />
            </linearGradient>
          </defs>
          <path
            :d="item.areaPath"
            :fill="`url(#${item.name}-gradient)`"
            fill-opacity="0.12"
          />
          <path
            :d="item.linePath"
            :stroke="`url(#${item.name}-gradient)`"
            stroke-width="3"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <circle
            v-if="item.lastPoint"
            :cx="item.lastPoint.x"
            :cy="item.lastPoint.y"
            r="4"
            fill="#39f1ff"
          />
        </svg>

        <div class="trend-card__meta">
          <span>样本 {{ item.points.length }}</span>
          <span>{{ item.timestamps[item.timestamps.length - 1] || '--' }}</span>
        </div>
      </article>
    </div>
    <div v-else class="empty-state">
      最近日志里还没有足够的数值属性样本。连续发送几条属性上报后，这里会出现趋势图。
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { DeviceMessageLog } from '../types/api';
import { formatDateTime, parseJsonSafely } from '../utils/format';
import PanelCard from './PanelCard.vue';

interface TrendSeries {
  name: string;
  latest: string;
  min: string;
  max: string;
  timestamps: string[];
  points: Array<{ x: number; y: number; value: number }>;
  linePath: string;
  areaPath: string;
  lastPoint?: { x: number; y: number };
}

const props = defineProps<{
  logs: DeviceMessageLog[];
}>();

const series = computed<TrendSeries[]>(() => {
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

  return [...buckets.entries()]
    .filter(([, values]) => values.length >= 2)
    .slice(0, 4)
    .map(([name, values]) => {
      const numericValues = values.map((item) => item.value);
      const min = Math.min(...numericValues);
      const max = Math.max(...numericValues);
      const range = max - min || 1;
      const width = 240;
      const height = 88;
      const stepX = values.length === 1 ? 0 : width / (values.length - 1);

      const points = values.map((item, index) => {
        const x = index * stepX;
        const y = height - ((item.value - min) / range) * (height - 16) - 8;
        return { x, y, value: item.value };
      });

      const linePath = points
        .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
        .join(' ');
      const lastPoint = points[points.length - 1];
      const areaPath = `${linePath} L ${(lastPoint?.x ?? width).toFixed(2)} ${height} L ${(points[0]?.x ?? 0).toFixed(2)} ${height} Z`;

      return {
        name,
        latest: String(values[values.length - 1]?.value ?? '--'),
        min: String(min),
        max: String(max),
        timestamps: values.map((item) => item.time),
        points,
        linePath,
        areaPath,
        lastPoint
      };
    });
});
</script>

<style scoped>
.trend-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.trend-card {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: rgba(5, 9, 18, 0.9);
}

.trend-card__head,
.trend-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.trend-card__eyebrow {
  margin: 0;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
}

.trend-card h3 {
  margin: 0.35rem 0 0;
  font-size: 1.8rem;
}

.trend-card__delta,
.trend-card__meta {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.trend-card__chart {
  width: 100%;
  height: 5.5rem;
  margin: 0.9rem 0 0.7rem;
}

@media (max-width: 1200px) {
  .trend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
