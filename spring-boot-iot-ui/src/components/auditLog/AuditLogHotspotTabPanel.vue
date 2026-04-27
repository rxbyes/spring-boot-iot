<template>
  <div data-testid="system-log-hotspot-panel" class="audit-log-system-panel audit-log-system-panel--hotspots">
    <section v-loading="slowSummaryLoading" class="audit-log-hotspot-workbench audit-log-slow-summary standard-list-surface">
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
      <template v-else>
        <header
          v-if="activeHotspot"
          data-testid="hotspot-focus-strip"
          class="audit-log-hotspot-workbench__focus"
        >
          <div class="audit-log-hotspot-workbench__focus-copy">
            <span class="audit-log-hotspot-workbench__eyebrow">当前热点</span>
            <strong>{{ formatSlowSummaryTitle(activeHotspot) }}</strong>
            <p>{{ formatSlowSummaryTarget(activeHotspot) }}</p>
          </div>
          <div class="audit-log-hotspot-workbench__focus-meta">
            <span>{{ formatValue(activeHotspot.latestTraceId) }}</span>
            <span>{{ formatValue(activeHotspot.latestStartedAt) }}</span>
            <span>{{ slowTrendWindowLabel }}</span>
          </div>
        </header>

        <section data-testid="hotspot-master-table" class="audit-log-hotspot-master-table">
          <header class="audit-log-hotspot-master-table__header">
            <span>热点</span>
            <span>目标</span>
            <span>最近时间</span>
            <span>最大耗时</span>
            <span>平均耗时</span>
            <span>总次数</span>
            <span>操作</span>
          </header>

          <article
            v-for="row in slowSummaryRows"
            :key="buildSlowSummaryKey(row)"
            data-testid="hotspot-master-row"
            :class="[
              'audit-log-hotspot-master-table__row',
              { 'is-selected': isSelectedSlowSummary(row) }
            ]"
            @click="emit('select-slow-summary', row)"
          >
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--title">
              <strong>{{ formatSlowSummaryTitle(row) }}</strong>
            </div>
            <div class="audit-log-hotspot-master-table__cell">
              <span>{{ formatSlowSummaryTarget(row) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell">
              <span>{{ formatValue(row.latestStartedAt) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell">
              <span>{{ formatDuration(row.maxDurationMs) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell">
              <span>{{ formatDuration(row.avgDurationMs) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell">
              <span>{{ formatCount(row.totalCount) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--actions">
              <StandardButton
                action="view"
                link
                :disabled="!row.latestTraceId"
                @click="
                  emit('select-slow-summary', row);
                  row.latestTraceId && emit('open-trace-evidence', row.latestTraceId)
                "
              >
                证据
              </StandardButton>
              <StandardButton
                action="view"
                link
                @click="
                  emit('select-slow-summary', row);
                  emit('open-slow-span-detail', row)
                "
              >
                明细
              </StandardButton>
              <StandardButton
                action="view"
                link
                @click="
                  emit('select-slow-summary', row);
                  emit('open-slow-trend', row, defaultSlowTrendWindow)
                "
              >
                趋势
              </StandardButton>
            </div>
          </article>
        </section>

        <section
          v-if="activeHotspot"
          data-testid="hotspot-detail-section"
          class="audit-log-hotspot-detail standard-list-surface"
        >
          <div class="audit-log-hotspot-detail__grid">
            <section v-loading="slowSpanLoading" class="audit-log-slow-span-drilldown">
              <header class="audit-log-slow-span-drilldown__header">
                <div>
                  <h3>最近样本</h3>
                  <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
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

            <section v-loading="slowTrendLoading" class="audit-log-slow-trend-drilldown">
              <header class="audit-log-slow-trend-drilldown__header">
                <div>
                  <h3>趋势观察</h3>
                  <p>{{ formatSlowSummaryTitle(activeTrendHotspot) }} / {{ formatSlowSummaryTarget(activeTrendHotspot) }}</p>
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
          </div>
        </section>
      </template>
    </section>

    <section
      v-loading="scheduledTaskLoading"
      class="audit-log-scheduled-task-ledger audit-log-scheduled-task-ledger--subdued standard-list-surface"
    >
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
import { computed } from 'vue'

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

const props = defineProps<{
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
  selectedSlowSummaryKey: string;
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
}>()

const emit = defineEmits<{
  (event: 'select-slow-summary', row: SlowSummaryRow): void;
  (event: 'open-trace-evidence', traceId: string): void;
  (event: 'open-slow-span-detail', row: SlowSummaryRow): void;
  (event: 'open-slow-trend', row: SlowSummaryRow, window: string): void;
  (event: 'change-slow-trend-window', value: string): void;
}>()

const buildSlowSummaryKey = (row: SlowSummaryRow) =>
  [
    row.spanType || 'span',
    row.domainCode || 'domain',
    row.eventCode || 'event',
    row.objectType || 'object',
    row.objectId || 'id'
  ].join('-')

const isSelectedSlowSummary = (row: SlowSummaryRow) =>
  buildSlowSummaryKey(row) === props.selectedSlowSummaryKey

const activeHotspot = computed<SlowSummaryRow | null>(() =>
  props.activeSlowSummary || props.activeSlowTrendSummary || props.slowSummaryRows[0] || null
)

const activeTrendHotspot = computed<SlowSummaryRow | null>(() =>
  props.activeSlowTrendSummary || activeHotspot.value
)

const slowTrendWindowLabel = computed(() => {
  const matched = props.slowTrendWindowOptions.find((item) => item.value === props.slowTrendWindow)
  return matched?.label || props.slowTrendWindow
})
</script>

<style scoped>
.audit-log-hotspot-workbench {
  display: grid;
  gap: 0.82rem;
}

.audit-log-hotspot-workbench__focus {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.95rem 1rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 74%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, white);
}

.audit-log-hotspot-workbench__focus-copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.audit-log-hotspot-workbench__eyebrow,
.audit-log-hotspot-workbench__focus-meta span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-hotspot-workbench__focus-copy strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 0.96rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-hotspot-workbench__focus-copy p {
  margin: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-hotspot-workbench__focus-meta {
  display: grid;
  gap: 0.35rem;
  justify-items: end;
  text-align: right;
}

.audit-log-hotspot-master-table {
  display: grid;
  gap: 0.62rem;
}

.audit-log-hotspot-master-table__header,
.audit-log-hotspot-master-table__row {
  display: grid;
  grid-template-columns: minmax(11rem, 1.3fr) minmax(12rem, 1.4fr) minmax(8rem, 0.9fr) repeat(3, minmax(5.5rem, 0.6fr)) minmax(9rem, 0.9fr);
  gap: 0.75rem;
  align-items: center;
}

.audit-log-hotspot-master-table__header {
  padding: 0 0.9rem;
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-hotspot-master-table__row {
  padding: 0.82rem 0.9rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 68%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 88%, transparent);
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.audit-log-hotspot-master-table__row:hover {
  border-color: color-mix(in srgb, var(--el-color-primary) 28%, var(--panel-border));
}

.audit-log-hotspot-master-table__row.is-selected {
  border-color: color-mix(in srgb, var(--el-color-primary) 42%, var(--panel-border));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 76%, white);
}

.audit-log-hotspot-master-table__cell {
  min-width: 0;
}

.audit-log-hotspot-master-table__cell strong,
.audit-log-hotspot-master-table__cell span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-hotspot-master-table__cell strong {
  color: var(--text-heading);
  font-size: 0.9rem;
}

.audit-log-hotspot-master-table__cell span {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.audit-log-hotspot-master-table__cell--actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.audit-log-hotspot-detail {
  padding: 1rem;
}

.audit-log-hotspot-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.82rem;
}

.audit-log-scheduled-task-ledger--subdued {
  opacity: 0.96;
}

@media (max-width: 960px) {
  .audit-log-hotspot-workbench__focus,
  .audit-log-hotspot-detail__grid {
    grid-template-columns: 1fr;
  }

  .audit-log-hotspot-workbench__focus {
    flex-direction: column;
  }

  .audit-log-hotspot-workbench__focus-meta {
    justify-items: start;
    text-align: left;
  }

  .audit-log-hotspot-master-table__header {
    display: none;
  }

  .audit-log-hotspot-master-table__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-log-hotspot-master-table__cell--title,
  .audit-log-hotspot-master-table__cell--actions {
    grid-column: 1 / -1;
  }

  .audit-log-hotspot-master-table__cell--actions {
    justify-content: flex-start;
  }
}
</style>
