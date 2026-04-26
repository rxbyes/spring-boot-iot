<template>
  <div data-testid="system-log-hotspot-panel" class="audit-log-system-panel audit-log-system-panel--hotspots">
    <section v-loading="slowSummaryLoading" class="audit-log-slow-summary standard-list-surface">
      <header class="audit-log-slow-summary__header">
        <div>
          <h3>性能慢点 Top</h3>
        </div>
        <span>{{ slowSummaryRows.length }} 项</span>
      </header>
      <div v-if="slowSummaryErrorMessage" class="audit-log-slow-summary__empty">
        {{ slowSummaryErrorMessage }}
      </div>
      <div v-else-if="slowSummaryRows.length === 0" class="audit-log-slow-summary__empty">
        暂无慢点热点
      </div>
      <div v-else class="audit-log-slow-summary__grid">
        <article
          v-for="row in slowSummaryRows"
          :key="`${row.spanType || 'span'}-${row.domainCode || 'domain'}-${row.eventCode || 'event'}-${row.objectType || 'object'}-${row.objectId || 'id'}`"
          class="audit-log-slow-summary__item"
        >
          <div class="audit-log-slow-summary__title">
            <strong>{{ formatSlowSummaryTitle(row) }}</strong>
            <span>{{ formatValue(row.latestStartedAt) }}</span>
          </div>
          <p>{{ formatSlowSummaryTarget(row) }}</p>
          <div class="audit-log-slow-summary__metrics">
            <span>最大 {{ formatDuration(row.maxDurationMs) }}</span>
            <span>平均 {{ formatDuration(row.avgDurationMs) }}</span>
            <span>{{ formatCount(row.totalCount) }} 次</span>
          </div>
          <div class="audit-log-slow-summary__footer">
            <span>{{ formatValue(row.latestTraceId) }}</span>
            <StandardButton
              action="view"
              link
              :disabled="!row.latestTraceId"
              @click="row.latestTraceId && emit('open-trace-evidence', row.latestTraceId)"
            >
              证据
            </StandardButton>
            <StandardButton action="view" link @click="emit('open-slow-span-detail', row)">
              明细
            </StandardButton>
            <StandardButton action="view" link @click="emit('open-slow-trend', row, defaultSlowTrendWindow)">
              趋势
            </StandardButton>
          </div>
        </article>
      </div>
      <section
        v-if="activeSlowSummary"
        v-loading="slowSpanLoading"
        class="audit-log-slow-span-drilldown"
      >
        <header class="audit-log-slow-span-drilldown__header">
          <div>
            <h3>慢点明细</h3>
            <p>{{ formatSlowSummaryTitle(activeSlowSummary) }} · {{ formatSlowSummaryTarget(activeSlowSummary) }}</p>
          </div>
          <span>{{ slowSpanTotal }} 条</span>
        </header>
        <div v-if="slowSpanErrorMessage" class="audit-log-slow-summary__empty">
          {{ slowSpanErrorMessage }}
        </div>
        <div v-else-if="slowSpanRows.length === 0" class="audit-log-slow-summary__empty">
          暂无慢点明细
        </div>
        <div v-else class="audit-log-slow-span-drilldown__list">
          <article
            v-for="span in slowSpanRows"
            :key="`slow-span-${span.id || span.traceId || span.startedAt}`"
            class="audit-log-slow-span-drilldown__item"
          >
            <div class="audit-log-slow-span-drilldown__title">
              <strong>{{ formatValue(span.spanName || span.spanType) }}</strong>
              <span>{{ formatDuration(span.durationMs) }}</span>
            </div>
            <div class="audit-log-slow-span-drilldown__meta">
              <span>{{ formatValue(span.traceId) }}</span>
              <span>{{ formatValue(span.status) }}</span>
              <span>{{ formatValue(span.startedAt) }}</span>
            </div>
            <div class="audit-log-slow-span-drilldown__footer">
              <span>{{ formatValue(span.eventCode) }} / {{ formatValue(span.objectId) }}</span>
              <StandardButton
                action="view"
                link
                :disabled="!span.traceId"
                @click="span.traceId && emit('open-trace-evidence', span.traceId)"
              >
                证据
              </StandardButton>
            </div>
          </article>
        </div>
      </section>
      <section
        v-if="activeSlowTrendSummary"
        v-loading="slowTrendLoading"
        class="audit-log-slow-trend-drilldown"
      >
        <header class="audit-log-slow-trend-drilldown__header">
          <div>
            <h3>慢点趋势</h3>
            <p>{{ formatSlowSummaryTitle(activeSlowTrendSummary) }} · {{ formatSlowSummaryTarget(activeSlowTrendSummary) }}</p>
          </div>
          <div class="audit-log-slow-trend-drilldown__actions">
            <span>{{ slowTrendRows.length }} 桶</span>
            <StandardChoiceGroup
              :model-value="slowTrendWindow"
              :options="slowTrendWindowOptions"
              @update:model-value="emit('change-slow-trend-window', $event)"
            />
          </div>
        </header>
        <div v-if="slowTrendErrorMessage" class="audit-log-slow-summary__empty">
          {{ slowTrendErrorMessage }}
        </div>
        <div v-else-if="slowTrendRows.length === 0" class="audit-log-slow-summary__empty">
          暂无慢点趋势
        </div>
        <div v-else class="audit-log-slow-trend-drilldown__list">
          <article
            v-for="item in slowTrendRows"
            :key="`slow-trend-${item.bucket || 'bucket'}-${item.bucketStart || item.bucketEnd}`"
            class="audit-log-slow-trend-drilldown__item"
          >
            <div class="audit-log-slow-trend-drilldown__title">
              <strong>{{ formatSlowTrendBucketLabel(item) }}</strong>
              <span>{{ formatCount(item.totalCount) }} 次</span>
            </div>
            <div class="audit-log-slow-trend-drilldown__metrics">
              <span>P95 {{ formatDuration(item.p95DurationMs) }}</span>
              <span>P99 {{ formatDuration(item.p99DurationMs) }}</span>
              <span>平均 {{ formatDuration(item.avgDurationMs) }}</span>
              <span>最大 {{ formatDuration(item.maxDurationMs) }}</span>
              <span>错误率 {{ formatPercentage(item.errorRate) }}</span>
            </div>
          </article>
        </div>
      </section>
    </section>

    <section v-loading="scheduledTaskLoading" class="audit-log-scheduled-task-ledger standard-list-surface">
      <header class="audit-log-scheduled-task-ledger__header">
        <div>
          <h3>调度任务台账</h3>
        </div>
        <span>{{ scheduledTaskRows.length }} / {{ scheduledTaskTotal }}</span>
      </header>
      <div v-if="scheduledTaskErrorMessage" class="audit-log-slow-summary__empty">
        {{ scheduledTaskErrorMessage }}
      </div>
      <div v-else-if="scheduledTaskRows.length === 0" class="audit-log-slow-summary__empty">
        暂无调度任务记录
      </div>
      <div v-else class="audit-log-scheduled-task-ledger__list">
        <article
          v-for="row in scheduledTaskRows"
          :key="`scheduled-task-${row.id || row.traceId || row.taskCode}`"
          class="audit-log-scheduled-task-ledger__item"
        >
          <div class="audit-log-scheduled-task-ledger__title">
            <strong>{{ formatScheduledTaskName(row) }}</strong>
            <span>{{ formatDuration(row.durationMs) }}</span>
          </div>
          <div class="audit-log-scheduled-task-ledger__meta">
            <span>{{ formatValue(row.triggerType) }}</span>
            <span>{{ formatScheduledTaskTrigger(row) }}</span>
            <span>{{ formatValue(row.status) }}</span>
            <span>{{ formatValue(row.startedAt) }}</span>
          </div>
          <div class="audit-log-scheduled-task-ledger__footer">
            <span>{{ formatValue(row.traceId) }}</span>
            <span v-if="row.errorMessage">{{ formatValue(row.errorMessage) }}</span>
            <StandardButton
              action="view"
              link
              :disabled="!row.traceId"
              @click="row.traceId && emit('open-trace-evidence', row.traceId)"
            >
              证据
            </StandardButton>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
interface SlowSummaryRow {
  spanType?: string | null;
  domainCode?: string | null;
  eventCode?: string | null;
  objectType?: string | null;
  objectId?: string | null;
  latestStartedAt?: string | null;
  latestTraceId?: string | null;
  maxDurationMs?: number | null;
  avgDurationMs?: number | null;
  totalCount?: number | null;
}

interface SlowSpanRow {
  id?: number | string | null;
  traceId?: string | null;
  spanName?: string | null;
  spanType?: string | null;
  status?: string | null;
  startedAt?: string | null;
  durationMs?: number | null;
  eventCode?: string | null;
  objectId?: string | null;
}

interface SlowTrendRow {
  bucket?: string | null;
  bucketStart?: string | null;
  bucketEnd?: string | null;
  totalCount?: number | null;
  p95DurationMs?: number | null;
  p99DurationMs?: number | null;
  avgDurationMs?: number | null;
  maxDurationMs?: number | null;
  errorRate?: number | null;
  successCount?: number | null;
  errorCount?: number | null;
}

interface ScheduledTaskRow {
  id?: number | string | null;
  taskCode?: string | null;
  traceId?: string | null;
  triggerType?: string | null;
  triggerExpression?: string | null;
  status?: string | null;
  startedAt?: string | null;
  durationMs?: number | null;
  errorMessage?: string | null;
}

interface SlowTrendWindowOption {
  label: string;
  value: string;
}

type ValueFormatter = (value: string | number | null | undefined) => string;
type CountFormatter = (value: number | null | undefined) => string;
type SlowSummaryFormatter = (row: SlowSummaryRow) => string;
type SlowTrendFormatter = (row: SlowTrendRow) => string;
type ScheduledTaskFormatter = (row: ScheduledTaskRow) => string;

defineProps<{
  slowSummaryLoading: boolean;
  slowSummaryRows: SlowSummaryRow[];
  slowSummaryErrorMessage: string;
  formatSlowSummaryTitle: SlowSummaryFormatter;
  formatSlowSummaryTarget: SlowSummaryFormatter;
  formatValue: ValueFormatter;
  formatDuration: CountFormatter;
  formatCount: CountFormatter;
  activeSlowSummary: SlowSummaryRow | null;
  slowSpanLoading: boolean;
  slowSpanTotal: number;
  slowSpanRows: SlowSpanRow[];
  slowSpanErrorMessage: string;
  activeSlowTrendSummary: SlowSummaryRow | null;
  slowTrendLoading: boolean;
  slowTrendRows: SlowTrendRow[];
  slowTrendErrorMessage: string;
  slowTrendWindow: string;
  slowTrendWindowOptions: SlowTrendWindowOption[];
  defaultSlowTrendWindow: string;
  formatSlowTrendBucketLabel: SlowTrendFormatter;
  formatPercentage: CountFormatter;
  scheduledTaskLoading: boolean;
  scheduledTaskRows: ScheduledTaskRow[];
  scheduledTaskTotal: number;
  scheduledTaskErrorMessage: string;
  formatScheduledTaskName: ScheduledTaskFormatter;
  formatScheduledTaskTrigger: ScheduledTaskFormatter;
}>();

const emit = defineEmits<{
  (event: 'open-trace-evidence', traceId: string): void;
  (event: 'open-slow-span-detail', row: SlowSummaryRow): void;
  (event: 'open-slow-trend', row: SlowSummaryRow, window: string): void;
  (event: 'change-slow-trend-window', value: string): void;
}>();
</script>
