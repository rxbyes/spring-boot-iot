<template>
  <div class="standard-trace-timeline">
    <div v-if="loading" class="standard-trace-timeline__empty">
      <strong>正在加载处理时间线...</strong>
      <p>请稍候，正在获取最新的阶段状态与处理摘要。</p>
    </div>

    <div v-else-if="!timeline" class="standard-trace-timeline__empty">
      <strong>{{ emptyTitle }}</strong>
      <p>{{ emptyDescription }}</p>
    </div>

    <div v-else class="standard-trace-timeline__body">
      <header class="standard-trace-timeline__summary">
        <article class="standard-trace-timeline__summary-card">
          <span class="standard-trace-timeline__summary-label">链路状态</span>
          <strong class="standard-trace-timeline__summary-value">{{ timeline.status || '--' }}</strong>
          <p class="standard-trace-timeline__summary-hint">TraceId：{{ timeline.traceId || '--' }}</p>
        </article>
        <article class="standard-trace-timeline__summary-card">
          <span class="standard-trace-timeline__summary-label">总耗时</span>
          <strong class="standard-trace-timeline__summary-value">{{ formatCost(timeline.totalCostMs) }}</strong>
          <p class="standard-trace-timeline__summary-hint">Session：{{ timeline.sessionId || '--' }}</p>
        </article>
        <article class="standard-trace-timeline__summary-card">
          <span class="standard-trace-timeline__summary-label">目标设备</span>
          <strong class="standard-trace-timeline__summary-value">{{ timeline.deviceCode || '--' }}</strong>
          <p class="standard-trace-timeline__summary-hint">协议：{{ timeline.protocolCode || '--' }}</p>
        </article>
      </header>

      <ol class="standard-trace-timeline__list">
        <li
          v-for="(step, index) in timeline.steps"
          :key="`${step.stage}-${step.startedAt || index}-${step.handlerClass || 'handler'}`"
          class="standard-trace-timeline__item"
        >
          <div class="standard-trace-timeline__rail">
            <span class="standard-trace-timeline__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="standard-trace-timeline__line" />
          </div>
          <article class="standard-trace-timeline__card" :data-status="normalizeStatus(step.status)">
            <header class="standard-trace-timeline__card-header">
              <div>
                <p class="standard-trace-timeline__stage">{{ step.stage || '--' }}</p>
                <h4 class="standard-trace-timeline__handler">
                  {{ formatHandler(step.handlerClass, step.handlerMethod) }}
                </h4>
              </div>
              <div class="standard-trace-timeline__meta">
                <span class="standard-trace-timeline__status" :data-status="normalizeStatus(step.status)">
                  {{ step.status || '--' }}
                </span>
                <span v-if="step.branch" class="standard-trace-timeline__branch">{{ step.branch }}</span>
                <span class="standard-trace-timeline__cost">{{ formatCost(step.costMs) }}</span>
              </div>
            </header>

            <div class="standard-trace-timeline__timing">
              <span>开始：{{ formatDateTime(step.startedAt) }}</span>
              <span>结束：{{ formatDateTime(step.finishedAt) }}</span>
            </div>

            <dl v-if="summaryEntries(step.summary).length" class="standard-trace-timeline__summary-grid">
              <template v-for="entry in summaryEntries(step.summary)" :key="`${step.stage}-${entry.key}`">
                <dt>{{ entry.label }}</dt>
                <dd>{{ entry.value }}</dd>
              </template>
            </dl>

            <div v-if="step.errorClass || step.errorMessage" class="standard-trace-timeline__error">
              <strong>{{ step.errorClass || '处理失败' }}</strong>
              <p>{{ step.errorMessage || '未返回错误消息。' }}</p>
            </div>
          </article>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MessageFlowTimeline } from '@/types/api';
import { formatDateTime } from '@/utils/format';

const props = withDefaults(
  defineProps<{
    timeline?: MessageFlowTimeline | null;
    loading?: boolean;
    emptyTitle?: string;
    emptyDescription?: string;
  }>(),
  {
    timeline: null,
    loading: false,
    emptyTitle: '暂无处理时间线',
    emptyDescription: '当前会话还没有可展示的处理阶段记录。'
  }
);

function formatHandler(handlerClass?: string | null, handlerMethod?: string | null) {
  const className = handlerClass?.trim() || 'UnknownHandler';
  const methodName = handlerMethod?.trim() || 'unknown';
  return `${className}#${methodName}`;
}

function normalizeStatus(status?: string | null) {
  return status?.trim().toUpperCase() || 'UNKNOWN';
}

function formatCost(costMs?: number | null) {
  if (costMs === undefined || costMs === null) {
    return '--';
  }
  return `${costMs} ms`;
}

function summaryEntries(summary?: Record<string, unknown> | null) {
  if (!summary) {
    return [];
  }
  return Object.entries(summary)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => ({
      key,
      label: prettifyKey(key),
      value: stringifyValue(value)
    }));
}

function prettifyKey(key: string) {
  return key.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').trim();
}

function stringifyValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => stringifyValue(item)).join(' / ');
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value);
    } catch {
      return '[object]';
    }
  }
  return String(value);
}
</script>

<style scoped>
.standard-trace-timeline {
  min-width: 0;
}

.standard-trace-timeline__empty {
  display: grid;
  gap: 0.42rem;
  padding: 1rem 1.05rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.standard-trace-timeline__empty strong {
  color: var(--text-heading);
  font-size: 0.92rem;
}

.standard-trace-timeline__empty p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.65;
  font-size: 0.84rem;
}

.standard-trace-timeline__body {
  display: grid;
  gap: 1rem;
}

.standard-trace-timeline__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.standard-trace-timeline__summary-card {
  display: grid;
  gap: 0.32rem;
  padding: 0.92rem 1rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, var(--surface-subtle), rgba(255, 255, 255, 0.98));
}

.standard-trace-timeline__summary-label {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.standard-trace-timeline__summary-value {
  color: var(--text-heading);
  font-size: 1.02rem;
}

.standard-trace-timeline__summary-hint {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.8rem;
}

.standard-trace-timeline__list {
  display: grid;
  gap: 0.9rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.standard-trace-timeline__item {
  display: grid;
  grid-template-columns: 3rem minmax(0, 1fr);
  gap: 0.82rem;
}

.standard-trace-timeline__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.55rem;
}

.standard-trace-timeline__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: var(--brand-deep);
  font-weight: 700;
  font-size: 0.8rem;
}

.standard-trace-timeline__line {
  flex: 1;
  width: 1px;
  min-height: 100%;
  background: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
}

.standard-trace-timeline__item:last-child .standard-trace-timeline__line {
  opacity: 0;
}

.standard-trace-timeline__card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem 1.05rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: var(--shadow-xs);
}

.standard-trace-timeline__card[data-status='FAILED'] {
  border-color: color-mix(in srgb, var(--danger) 28%, var(--panel-border));
  background: linear-gradient(180deg, color-mix(in srgb, var(--danger) 5%, white), rgba(255, 255, 255, 0.98));
}

.standard-trace-timeline__card[data-status='SKIPPED'] {
  border-color: color-mix(in srgb, var(--accent) 24%, var(--panel-border));
  background: linear-gradient(180deg, color-mix(in srgb, var(--accent) 5%, white), rgba(255, 255, 255, 0.98));
}

.standard-trace-timeline__card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.standard-trace-timeline__stage {
  margin: 0 0 0.2rem;
  color: var(--brand);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.standard-trace-timeline__handler {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
}

.standard-trace-timeline__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.48rem;
}

.standard-trace-timeline__status,
.standard-trace-timeline__branch,
.standard-trace-timeline__cost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.75rem;
  padding: 0 0.68rem;
  border-radius: var(--radius-pill);
  font-size: 0.76rem;
  font-weight: 700;
}

.standard-trace-timeline__status {
  border: 1px solid color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: var(--brand-deep);
}

.standard-trace-timeline__status[data-status='FAILED'] {
  border-color: color-mix(in srgb, var(--danger) 24%, var(--panel-border));
  background: color-mix(in srgb, var(--danger) 10%, white);
  color: color-mix(in srgb, var(--danger) 80%, var(--text-primary));
}

.standard-trace-timeline__status[data-status='SKIPPED'] {
  border-color: color-mix(in srgb, var(--accent) 24%, var(--panel-border));
  background: color-mix(in srgb, var(--accent) 10%, white);
  color: var(--accent-deep);
}

.standard-trace-timeline__branch,
.standard-trace-timeline__cost {
  border: 1px solid var(--line-soft);
  background: var(--surface-subtle);
  color: var(--text-secondary);
}

.standard-trace-timeline__timing {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  color: var(--text-caption);
  font-size: 0.8rem;
}

.standard-trace-timeline__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem 0.9rem;
  margin: 0;
}

.standard-trace-timeline__summary-grid dt {
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.standard-trace-timeline__summary-grid dd {
  margin: 0.12rem 0 0;
  color: var(--text-primary);
  font-size: 0.84rem;
  line-height: 1.55;
  word-break: break-word;
}

.standard-trace-timeline__error {
  display: grid;
  gap: 0.28rem;
  padding: 0.82rem 0.9rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--danger) 8%, white);
}

.standard-trace-timeline__error strong {
  color: color-mix(in srgb, var(--danger) 80%, var(--text-primary));
  font-size: 0.84rem;
}

.standard-trace-timeline__error p {
  margin: 0;
  color: color-mix(in srgb, var(--danger) 72%, var(--text-secondary));
  font-size: 0.82rem;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .standard-trace-timeline__summary {
    grid-template-columns: 1fr;
  }

  .standard-trace-timeline__summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
