<template>
  <PanelCard
    title="子设备总览"
    description="采集器读侧汇总子设备最近监测值、正式状态与链路可达性，不回写采集器契约字段。"
  >
    <div class="collector-child-insight-panel__summary">
      <article class="collector-child-insight-panel__summary-card collector-child-insight-panel__summary-card--primary">
        <span>采集器视角</span>
        <strong>{{ overview.parentDeviceCode || '--' }}</strong>
        <p class="collector-child-insight-panel__summary-copy">{{ overviewSummaryCopy }}</p>
        <div class="collector-child-insight-panel__summary-meta">
          <small class="collector-child-insight-panel__summary-meta-item">在线状态：{{ parentOnlineStatusText }}</small>
          <small class="collector-child-insight-panel__summary-meta-item">子设备总数：{{ overview.childCount ?? 0 }}</small>
          <small class="collector-child-insight-panel__summary-meta-item">状态缺失：{{ overview.missingChildCount ?? 0 }}</small>
          <small class="collector-child-insight-panel__summary-meta-item">状态过期：{{ overview.staleChildCount ?? 0 }}</small>
        </div>
      </article>

      <div class="collector-child-insight-panel__summary-stats">
        <article
          v-for="stat in summaryStats"
          :key="stat.key"
          class="collector-child-insight-panel__summary-card collector-child-insight-panel__summary-card--compact"
          :class="{
            'collector-child-insight-panel__summary-card--attention': stat.tone === 'warning',
            'collector-child-insight-panel__summary-card--brand': stat.tone === 'brand'
          }"
        >
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </article>
      </div>
    </div>

    <div class="collector-child-insight-panel__list">
      <article
        v-for="child in overview.children"
        :key="`${child.logicalChannelCode}-${child.childDeviceCode}`"
        class="collector-child-insight-panel__child"
      >
        <div class="collector-child-insight-panel__child-head">
          <div class="collector-child-insight-panel__child-title">
            <strong>{{ child.childDeviceName || child.logicalChannelCode }}</strong>
            <p>{{ child.logicalChannelCode }} · {{ child.childDeviceCode }}</p>
          </div>
          <div class="collector-child-insight-panel__badges">
            <span
              class="collector-child-insight-panel__badge"
              :class="collectorLinkStateClass(child.collectorLinkState)"
            >
              {{ collectorLinkStateLabel(child.collectorLinkState) }}
            </span>
            <span
              class="collector-child-insight-panel__badge"
              :class="sensorStateHealthClass(child.sensorStateHealth)"
            >
              {{ sensorStateHealthLabel(child.sensorStateHealth, child.sensorStateValue) }}
            </span>
          </div>
        </div>

        <div class="collector-child-insight-panel__child-meta">
          <span class="collector-child-insight-panel__child-meta-pill">子产品：{{ child.childProductKey || '--' }}</span>
          <span class="collector-child-insight-panel__child-meta-pill">最近上报：{{ formatDateTime(child.lastReportTime) }}</span>
          <span class="collector-child-insight-panel__child-meta-pill">{{ childMetricCountText(child) }}</span>
        </div>

        <div v-if="child.metrics?.length" class="collector-child-insight-panel__metrics">
          <article
            v-for="metric in child.metrics"
            :key="`${child.childDeviceCode}-${metric.identifier}`"
            class="collector-child-insight-panel__metric"
            :class="{ 'collector-child-insight-panel__metric--recommended': childMetricRecommended(child, metric) }"
          >
            <div class="collector-child-insight-panel__metric-head">
              <span>{{ metric.displayName || metric.identifier }}</span>
              <small v-if="childMetricRecommended(child, metric)" class="collector-child-insight-panel__metric-badge">建议</small>
            </div>
            <strong>{{ metricValueText(metric) }}</strong>
            <small>{{ formatDateTime(metric.reportTime) }}</small>
          </article>
        </div>

        <div v-else class="collector-child-insight-panel__empty">
          当前子设备暂无监测指标快照。
        </div>
      </article>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/PanelCard.vue';
import type { CollectorChildInsightChild, CollectorChildInsightMetric, CollectorChildInsightOverview } from '@/types/api';
import { formatDateTime } from '@/utils/format';

const props = defineProps<{
  overview: CollectorChildInsightOverview;
}>();

const stateAttentionCount = computed(() => (props.overview.missingChildCount ?? 0) + (props.overview.staleChildCount ?? 0));
const overviewSummaryCopy = computed(
  () => `已挂接 ${props.overview.childCount ?? 0} 个子设备，其中 ${props.overview.reachableChildCount ?? 0} 个链路可达，${props.overview.sensorStateReportedCount ?? 0} 个已回传正式状态。`
);
const summaryStats = computed(() => {
  const stats = [
    {
      key: 'reachable',
      label: '链路可达',
      value: props.overview.reachableChildCount ?? 0,
      tone: 'default'
    },
    {
      key: 'reported',
      label: '已上报状态',
      value: props.overview.sensorStateReportedCount ?? 0,
      tone: 'default'
    },
    {
      key: 'attention',
      label: '待关注',
      value: stateAttentionCount.value,
      tone: stateAttentionCount.value > 0 ? 'warning' : 'default'
    }
  ];
  if ((props.overview.recommendedMetricCount ?? 0) > 0) {
    stats.push({
      key: 'recommended',
      label: '建议指标',
      value: props.overview.recommendedMetricCount ?? 0,
      tone: 'brand'
    });
  }
  return stats;
});

const parentOnlineStatusText = computed(() => {
  if (props.overview.parentOnlineStatus === 1) {
    return '在线';
  }
  if (props.overview.parentOnlineStatus === 0) {
    return '离线';
  }
  return '待确认';
});

function collectorLinkStateLabel(state?: string | null) {
  if (state === 'reachable') {
    return '链路可达';
  }
  if (state === 'unreachable') {
    return '链路不可达';
  }
  return '链路待确认';
}

function collectorLinkStateClass(state?: string | null) {
  if (state === 'reachable') {
    return 'collector-child-insight-panel__badge--reachable';
  }
  if (state === 'unreachable') {
    return 'collector-child-insight-panel__badge--unreachable';
  }
  return 'collector-child-insight-panel__badge--unknown';
}

function sensorStateHealthLabel(
  health?: 'REPORTED_NORMAL' | 'REPORTED_ABNORMAL' | 'MISSING' | 'STALE' | null,
  sensorStateValue?: string | null
) {
  if (health === 'REPORTED_NORMAL') {
    return `状态正常 (${sensorStateValue?.trim() || '--'})`;
  }
  if (health === 'REPORTED_ABNORMAL') {
    return `状态异常 (${sensorStateValue?.trim() || '--'})`;
  }
  if (health === 'MISSING') {
    return '状态缺失';
  }
  if (health === 'STALE') {
    return '状态过期';
  }
  return sensorStateValue?.trim() ? `传感器状态 ${sensorStateValue.trim()}` : '传感器状态 --';
}

function sensorStateHealthClass(health?: 'REPORTED_NORMAL' | 'REPORTED_ABNORMAL' | 'MISSING' | 'STALE' | null) {
  if (health === 'REPORTED_NORMAL') {
    return 'collector-child-insight-panel__badge--reachable';
  }
  if (health === 'REPORTED_ABNORMAL' || health === 'MISSING' || health === 'STALE') {
    return 'collector-child-insight-panel__badge--unreachable';
  }
  return 'collector-child-insight-panel__badge--state';
}

function metricValueText(metric: CollectorChildInsightMetric) {
  const propertyValue = metric.propertyValue?.trim();
  if (!propertyValue) {
    return '--';
  }
  const unit = metric.unit?.trim();
  return unit ? `${propertyValue} ${unit}` : propertyValue;
}

function childMetricCountText(child: CollectorChildInsightChild) {
  const metricCount = child.metrics?.length ?? 0;
  return metricCount > 0 ? `最近指标 ${metricCount} 项` : '暂无指标';
}

function childMetricRecommended(child: CollectorChildInsightChild, metric: CollectorChildInsightMetric) {
  if (metric.recommended) {
    return true;
  }
  return Boolean(child.recommendedMetricIdentifiers?.includes(metric.identifier));
}
</script>

<style scoped>
.collector-child-insight-panel__summary,
.collector-child-insight-panel__list,
.collector-child-insight-panel__metrics {
  display: grid;
  gap: 0.9rem;
}

.collector-child-insight-panel__summary {
  gap: 0.95rem;
  margin-bottom: 0.95rem;
}

.collector-child-insight-panel__summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(8.5rem, 1fr));
  gap: 0.75rem;
}

.collector-child-insight-panel__summary-card,
.collector-child-insight-panel__child,
.collector-child-insight-panel__metric {
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 5%, white));
}

.collector-child-insight-panel__summary-card {
  display: grid;
  gap: 0.28rem;
  padding: 0.9rem 1rem;
}

.collector-child-insight-panel__summary-card--primary {
  gap: 0.55rem;
  padding: 1rem 1.05rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(247, 250, 255, 0.96) 100%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 45%);
}

.collector-child-insight-panel__summary-card--compact {
  gap: 0.22rem;
}

.collector-child-insight-panel__summary-card--attention strong {
  color: #c0392b;
}

.collector-child-insight-panel__summary-card--brand strong {
  color: color-mix(in srgb, var(--brand) 72%, var(--text-primary));
}

.collector-child-insight-panel__summary-card span,
.collector-child-insight-panel__metric span,
.collector-child-insight-panel__child-meta,
.collector-child-insight-panel__metric small,
.collector-child-insight-panel__child-title p,
.collector-child-insight-panel__empty {
  color: var(--text-secondary);
}

.collector-child-insight-panel__summary-card strong,
.collector-child-insight-panel__child-title strong,
.collector-child-insight-panel__metric strong {
  color: var(--text-primary);
}

.collector-child-insight-panel__summary-card strong {
  font-size: 1.35rem;
}

.collector-child-insight-panel__summary-copy {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.85rem;
  line-height: 1.65;
}

.collector-child-insight-panel__summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem 0.75rem;
}

.collector-child-insight-panel__summary-meta-item {
  color: var(--text-caption);
  font-size: 0.78rem;
  line-height: 1.45;
}

.collector-child-insight-panel__child {
  display: grid;
  gap: 0.82rem;
  padding: 1rem 1.05rem;
}

.collector-child-insight-panel__child-head,
.collector-child-insight-panel__child-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.9rem;
  align-items: flex-start;
}

.collector-child-insight-panel__child-title {
  display: grid;
  gap: 0.22rem;
}

.collector-child-insight-panel__child-title p {
  margin: 0;
  line-height: 1.5;
}

.collector-child-insight-panel__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-end;
}

.collector-child-insight-panel__child-meta {
  flex-wrap: wrap;
}

.collector-child-insight-panel__child-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.32rem 0.62rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.1);
  color: var(--text-secondary);
  font-size: 0.79rem;
  line-height: 1.4;
}

.collector-child-insight-panel__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.38rem 0.72rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 600;
}

.collector-child-insight-panel__badge--reachable {
  background: rgba(19, 179, 139, 0.12);
  color: #0e8d6d;
}

.collector-child-insight-panel__badge--unreachable {
  background: rgba(239, 68, 68, 0.12);
  color: #c0392b;
}

.collector-child-insight-panel__badge--unknown,
.collector-child-insight-panel__badge--state {
  background: rgba(17, 24, 39, 0.08);
  color: var(--text-primary);
}

.collector-child-insight-panel__metrics {
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
}

.collector-child-insight-panel__metric {
  display: grid;
  gap: 0.28rem;
  padding: 0.84rem 0.9rem;
}

.collector-child-insight-panel__metric--recommended {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(255, 249, 242, 0.94) 100%);
}

.collector-child-insight-panel__metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.collector-child-insight-panel__metric-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.16rem 0.44rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 14%, transparent);
  color: color-mix(in srgb, var(--brand) 78%, var(--text-primary));
  font-size: 0.72rem;
  line-height: 1.2;
  white-space: nowrap;
}

.collector-child-insight-panel__metric strong {
  font-size: 1.05rem;
}

.collector-child-insight-panel__metric small {
  font-size: 0.78rem;
}

.collector-child-insight-panel__empty {
  line-height: 1.6;
}

@media (max-width: 768px) {
  .collector-child-insight-panel__child-head,
  .collector-child-insight-panel__child-meta {
    flex-direction: column;
  }

  .collector-child-insight-panel__badges {
    justify-content: flex-start;
  }

  .collector-child-insight-panel__metric-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
