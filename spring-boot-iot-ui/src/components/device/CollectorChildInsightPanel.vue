<template>
  <PanelCard
    title="子设备总览"
    description="采集器读侧汇总子设备最近监测值、正式状态与链路可达性，不回写采集器契约字段。"
  >
    <div class="collector-child-insight-panel__summary">
      <article class="collector-child-insight-panel__summary-card">
        <span>子设备总数</span>
        <strong>{{ overview.childCount ?? 0 }}</strong>
      </article>
      <article class="collector-child-insight-panel__summary-card">
        <span>链路可达</span>
        <strong>{{ overview.reachableChildCount ?? 0 }}</strong>
      </article>
      <article class="collector-child-insight-panel__summary-card">
        <span>已上报状态</span>
        <strong>{{ overview.sensorStateReportedCount ?? 0 }}</strong>
      </article>
      <article class="collector-child-insight-panel__summary-card collector-child-insight-panel__summary-card--warn">
        <span>状态缺失</span>
        <strong>{{ overview.missingChildCount ?? 0 }}</strong>
      </article>
      <article class="collector-child-insight-panel__summary-card collector-child-insight-panel__summary-card--warn">
        <span>状态过期</span>
        <strong>{{ overview.staleChildCount ?? 0 }}</strong>
      </article>
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

        <div class="collector-child-insight-panel__meta">
          <span>子产品：{{ child.childProductKey || '--' }}</span>
          <span>最近上报：{{ formatDateTime(child.lastReportTime) }}</span>
        </div>

        <div v-if="child.metrics?.length" class="collector-child-insight-panel__metrics">
          <article
            v-for="metric in child.metrics"
            :key="`${child.childDeviceCode}-${metric.identifier}`"
            class="collector-child-insight-panel__metric"
          >
            <span>{{ metric.displayName || metric.identifier }}</span>
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
import PanelCard from '@/components/PanelCard.vue';
import type { CollectorChildInsightMetric, CollectorChildInsightOverview } from '@/types/api';
import { formatDateTime } from '@/utils/format';

defineProps<{
  overview: CollectorChildInsightOverview;
}>();

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
</script>

<style scoped>
.collector-child-insight-panel__summary,
.collector-child-insight-panel__list,
.collector-child-insight-panel__metrics {
  display: grid;
  gap: 0.9rem;
}

.collector-child-insight-panel__summary {
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  margin-bottom: 0.95rem;
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

.collector-child-insight-panel__summary-card--warn strong {
  color: #c0392b;
}

.collector-child-insight-panel__summary-card span,
.collector-child-insight-panel__metric span,
.collector-child-insight-panel__meta,
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

.collector-child-insight-panel__child {
  display: grid;
  gap: 0.82rem;
  padding: 1rem 1.05rem;
}

.collector-child-insight-panel__child-head,
.collector-child-insight-panel__meta {
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

.collector-child-insight-panel__meta {
  flex-wrap: wrap;
  font-size: 0.86rem;
  line-height: 1.56;
}

.collector-child-insight-panel__metrics {
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
}

.collector-child-insight-panel__metric {
  display: grid;
  gap: 0.28rem;
  padding: 0.84rem 0.9rem;
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
  .collector-child-insight-panel__meta {
    flex-direction: column;
  }

  .collector-child-insight-panel__badges {
    justify-content: flex-start;
  }
}
</style>
