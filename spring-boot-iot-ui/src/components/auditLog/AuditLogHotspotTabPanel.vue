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
            <span>Trace {{ formatValue(activeHotspot.latestTraceId) }}</span>
            <span>最近 {{ formatValue(activeHotspot.latestStartedAt) }}</span>
            <span>窗口 {{ slowTrendWindowLabel }}</span>
          </div>
        </header>

        <section data-testid="hotspot-master-table" class="audit-log-hotspot-master-table">
          <header data-testid="hotspot-master-header" class="audit-log-hotspot-master-table__header">
            <span>热点对象</span>
            <span>风险状态</span>
            <span>性能信号</span>
            <span>最近情况</span>
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
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--identity">
              <strong>{{ formatSlowSummaryTitle(row) }}</strong>
              <span>{{ formatSlowSummaryTarget(row) }}</span>
            </div>
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--status">
              <span :class="['audit-log-hotspot-master-table__risk-chip', resolveHotspotRiskTone(row)]">
                {{ resolveHotspotRiskLabel(row) }}
              </span>
              <small>{{ resolveHotspotRiskMeta(row) }}</small>
            </div>
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--signals">
              <strong>P峰 {{ formatDuration(row.maxDurationMs) }}</strong>
              <span>均值 {{ formatDuration(row.avgDurationMs) }}</span>
              <small>{{ formatCount(row.totalCount) }} 次</small>
            </div>
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--recent">
              <strong>{{ formatValue(row.latestStartedAt) }}</strong>
              <small>{{ formatValue(row.latestTraceId) }}</small>
            </div>
            <div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--actions">
              <div class="audit-log-hotspot-master-table__action-rail">
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
                    emit('change-hotspot-drilldown-view', 'samples');
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
                    emit('change-hotspot-drilldown-view', 'trends');
                    emit('open-slow-trend', row, defaultSlowTrendWindow)
                  "
                >
                  趋势
                </StandardButton>
              </div>
            </div>
          </article>
        </section>

        <section
          v-if="activeHotspot"
          data-testid="hotspot-detail-section"
          class="audit-log-hotspot-detail standard-list-surface"
        >
          <header class="audit-log-hotspot-detail__header">
            <div>
              <h3>热点下钻</h3>
              <p>{{ formatSlowSummaryTitle(activeHotspot) }} / {{ formatSlowSummaryTarget(activeHotspot) }}</p>
            </div>
            <div data-testid="hotspot-drilldown-switch" class="audit-log-hotspot-detail__switch">
              <StandardChoiceGroup
                :model-value="hotspotDrilldownView"
                :options="hotspotDrilldownOptions"
                @update:model-value="emit('change-hotspot-drilldown-view', $event)"
              />
            </div>
          </header>

          <section
            v-if="hotspotDrilldownView === 'samples'"
            v-loading="slowSpanLoading"
            class="audit-log-slow-span-drilldown"
          >
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
                    <span>Trace {{ formatValue(span.traceId) }}</span>
                    <span>状态 {{ formatValue(span.status) }}</span>
                    <span>开始 {{ formatValue(span.startedAt) }}</span>
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
            v-else-if="hotspotDrilldownView === 'trends'"
            v-loading="slowTrendLoading"
            class="audit-log-slow-trend-drilldown"
          >
              <header class="audit-log-slow-trend-drilldown__header">
                <div>
                  <h3>趋势</h3>
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
                  <div
                    v-if="hasSlowTrendSamples(item)"
                    class="audit-log-slow-trend-drilldown__metrics"
                  >
                    <span class="is-primary">P95 {{ formatDuration(item.p95DurationMs) }}</span>
                    <span>错误率 {{ formatPercentage(item.errorRate) }}</span>
                    <span>最大 {{ formatDuration(item.maxDurationMs) }}</span>
                  </div>
                  <p
                    v-if="hasSlowTrendSamples(item)"
                    class="audit-log-slow-trend-drilldown__summary"
                  >
                    {{ formatSlowTrendSummary(item) }}
                  </p>
                  <p v-else class="audit-log-slow-trend-drilldown__summary is-empty">本桶暂无请求样本</p>
                </article>
              </div>
          </section>

          <section
            v-else
            v-loading="scheduledTaskLoading"
            class="audit-log-scheduled-task-ledger audit-log-scheduled-task-ledger--embedded"
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
                    <span>触发 {{ formatValue(row.triggerType) }}</span>
                    <span>{{ formatScheduledTaskTrigger(row) }}</span>
                    <span>状态 {{ formatValue(row.status) }}</span>
                    <span>开始 {{ formatValue(row.startedAt) }}</span>
                  </div>
                  <div class="audit-log-scheduled-task-ledger__footer">
                    <span>Trace {{ formatValue(row.traceId) }}</span>
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
        </section>
      </template>
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

type HotspotDrilldownView = 'samples' | 'trends' | 'tasks';

interface HotspotDrilldownOption {
  label: string;
  value: HotspotDrilldownView;
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
  hotspotDrilldownView: HotspotDrilldownView;
  hotspotDrilldownOptions: HotspotDrilldownOption[];
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
  (event: 'change-hotspot-drilldown-view', view: HotspotDrilldownView): void;
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

const resolveHotspotRiskLabel = (row: SlowSummaryRow) => {
  const maxDuration = Number(row.maxDurationMs || 0)
  const avgDuration = Number(row.avgDurationMs || 0)
  const totalCount = Number(row.totalCount || 0)

  if (maxDuration >= 2000) {
    return '高耗时'
  }
  if (totalCount >= 8) {
    return '高频次'
  }
  if (avgDuration >= 1200) {
    return '持续偏慢'
  }
  return '持续关注'
}

const resolveHotspotRiskTone = (row: SlowSummaryRow) => {
  const maxDuration = Number(row.maxDurationMs || 0)
  const avgDuration = Number(row.avgDurationMs || 0)
  const totalCount = Number(row.totalCount || 0)

  if (maxDuration >= 2000) {
    return 'is-danger'
  }
  if (totalCount >= 8 || avgDuration >= 1200) {
    return 'is-warning'
  }
  return 'is-neutral'
}

const resolveHotspotRiskMeta = (row: SlowSummaryRow) =>
  `峰值 ${props.formatDuration(row.maxDurationMs)} / 共 ${props.formatCount(row.totalCount)} 次`

const hasSlowTrendSamples = (row: SlowTrendRow) => Number(row.totalCount || 0) > 0

const formatSlowTrendSummary = (row: SlowTrendRow) =>
  `P99 ${props.formatDuration(row.p99DurationMs)} · 平均 ${props.formatDuration(row.avgDurationMs)}`
</script>

<style scoped>
.audit-log-hotspot-workbench {
  display: grid;
  gap: 0.82rem;
}

.audit-log-hotspot-workbench__focus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.82rem 0.96rem;
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
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.audit-log-hotspot-workbench__focus-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0 0.62rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 92%, white);
}

.audit-log-hotspot-master-table {
  display: grid;
  gap: 0.62rem;
}

.audit-log-hotspot-master-table__header,
.audit-log-hotspot-master-table__row {
  display: grid;
  grid-template-columns:
    minmax(15rem, 1.65fr)
    minmax(8rem, 0.8fr)
    minmax(10rem, 0.92fr)
    minmax(9rem, 0.88fr)
    minmax(7.5rem, 0.74fr);
  gap: 0.62rem;
  align-items: center;
}

.audit-log-hotspot-master-table__header {
  padding: 0 0.9rem;
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-hotspot-master-table__row {
  min-height: 74px;
  padding: 0.86rem 0.9rem;
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
  display: grid;
  gap: 0.22rem;
}

.audit-log-hotspot-master-table__cell strong,
.audit-log-hotspot-master-table__cell span,
.audit-log-hotspot-master-table__cell small {
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

.audit-log-hotspot-master-table__cell small {
  color: var(--text-caption);
  font-size: 0.77rem;
}

.audit-log-hotspot-master-table__cell--identity strong,
.audit-log-hotspot-master-table__cell--signals strong,
.audit-log-hotspot-master-table__cell--recent strong {
  font-size: 0.88rem;
}

.audit-log-hotspot-master-table__cell--status {
  align-content: start;
}

.audit-log-hotspot-master-table__risk-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 28px;
  padding: 0 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 78%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 92%, white);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}

.audit-log-hotspot-master-table__risk-chip.is-danger {
  border-color: color-mix(in srgb, var(--el-color-danger) 40%, white);
  background: color-mix(in srgb, var(--el-color-danger-light-9) 88%, white);
  color: var(--el-color-danger-dark-2);
}

.audit-log-hotspot-master-table__risk-chip.is-warning {
  border-color: color-mix(in srgb, var(--el-color-warning) 44%, white);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 88%, white);
  color: color-mix(in srgb, var(--el-color-warning-dark-2) 82%, black);
}

.audit-log-hotspot-master-table__risk-chip.is-neutral {
  border-color: color-mix(in srgb, var(--panel-border) 78%, transparent);
  background: color-mix(in srgb, var(--panel-bg) 92%, white);
  color: var(--text-secondary);
}

.audit-log-hotspot-master-table__cell--actions {
  display: flex;
  justify-content: flex-end;
}

.audit-log-hotspot-master-table__action-rail {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem 0.45rem;
  justify-content: flex-end;
}

.audit-log-hotspot-detail {
  display: grid;
  gap: 0.82rem;
  padding: 1rem;
}

.audit-log-hotspot-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.audit-log-hotspot-detail__header h3,
.audit-log-hotspot-detail__header p {
  margin: 0;
}

.audit-log-hotspot-detail__header h3 {
  color: var(--text-heading);
  font-size: 0.92rem;
}

.audit-log-hotspot-detail__header p {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.5;
}

.audit-log-hotspot-detail__switch {
  flex-shrink: 0;
}

.audit-log-slow-span-drilldown,
.audit-log-slow-trend-drilldown,
.audit-log-scheduled-task-ledger {
  display: grid;
  gap: 0.78rem;
  min-width: 0;
  padding-top: 0.82rem;
  border-top: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
}

.audit-log-slow-span-drilldown__header,
.audit-log-slow-span-drilldown__title,
.audit-log-slow-span-drilldown__footer,
.audit-log-slow-trend-drilldown__header,
.audit-log-slow-trend-drilldown__title,
.audit-log-slow-trend-drilldown__actions,
.audit-log-scheduled-task-ledger__header,
.audit-log-scheduled-task-ledger__title,
.audit-log-scheduled-task-ledger__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-span-drilldown__header,
.audit-log-slow-trend-drilldown__header,
.audit-log-scheduled-task-ledger__header {
  align-items: flex-start;
}

.audit-log-slow-span-drilldown__header h3,
.audit-log-slow-trend-drilldown__header h3,
.audit-log-scheduled-task-ledger__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
  line-height: 1.3;
}

.audit-log-slow-span-drilldown__header p,
.audit-log-slow-trend-drilldown__header p {
  margin: 0.25rem 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__header > span,
.audit-log-slow-trend-drilldown__actions > span,
.audit-log-scheduled-task-ledger__header > span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0 0.62rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 92%, white);
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-span-drilldown__list,
.audit-log-slow-trend-drilldown__list,
.audit-log-scheduled-task-ledger__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-slow-span-drilldown__item,
.audit-log-slow-trend-drilldown__item,
.audit-log-scheduled-task-ledger__item {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.78rem 0.84rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-slow-span-drilldown__title strong,
.audit-log-slow-span-drilldown__footer > span,
.audit-log-slow-trend-drilldown__title strong,
.audit-log-scheduled-task-ledger__title strong,
.audit-log-scheduled-task-ledger__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__title span,
.audit-log-scheduled-task-ledger__title span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0 0.62rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 78%, white);
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-slow-span-drilldown__meta,
.audit-log-slow-trend-drilldown__metrics,
.audit-log-scheduled-task-ledger__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-slow-span-drilldown__meta span,
.audit-log-slow-trend-drilldown__metrics span,
.audit-log-scheduled-task-ledger__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0 0.56rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 70%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 92%, white);
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-trend-drilldown__actions {
  flex: 0 0 auto;
  flex-direction: column;
  align-items: flex-end;
}

.audit-log-slow-trend-drilldown__title span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0 0.62rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 78%, white);
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-slow-trend-drilldown__metrics span.is-primary {
  border-color: color-mix(in srgb, var(--el-color-primary) 32%, white);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 78%, white);
  color: var(--text-secondary);
}

.audit-log-slow-trend-drilldown__summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.55;
}

.audit-log-slow-trend-drilldown__summary.is-empty {
  color: var(--text-caption);
}

.audit-log-scheduled-task-ledger--embedded {
  margin-top: 0;
  border: 1px solid color-mix(in srgb, var(--panel-border) 68%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 88%, transparent);
  padding: 0.9rem;
}

@media (max-width: 1120px) {
  .audit-log-hotspot-master-table__header,
  .audit-log-hotspot-master-table__row {
    grid-template-columns:
      minmax(13.5rem, 1.5fr)
      minmax(7rem, 0.74fr)
      minmax(9rem, 0.85fr)
      minmax(8rem, 0.82fr)
      minmax(6.5rem, 0.64fr);
    gap: 0.56rem;
  }
}

@media (max-width: 960px) {
  .audit-log-hotspot-workbench__focus {
    flex-direction: column;
    align-items: flex-start;
  }

  .audit-log-hotspot-workbench__focus-meta {
    justify-content: flex-start;
  }

  .audit-log-hotspot-detail__header {
    flex-direction: column;
  }

  .audit-log-slow-span-drilldown__header,
  .audit-log-slow-span-drilldown__title,
  .audit-log-slow-span-drilldown__footer,
  .audit-log-slow-trend-drilldown__header,
  .audit-log-slow-trend-drilldown__title,
  .audit-log-scheduled-task-ledger__header,
  .audit-log-scheduled-task-ledger__title,
  .audit-log-scheduled-task-ledger__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .audit-log-slow-trend-drilldown__actions {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .audit-log-hotspot-master-table__header {
    display: none;
  }

  .audit-log-hotspot-master-table__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-log-hotspot-master-table__cell--identity,
  .audit-log-hotspot-master-table__cell--actions {
    grid-column: 1 / -1;
  }

  .audit-log-hotspot-master-table__cell--actions {
    justify-content: flex-start;
  }

  .audit-log-hotspot-master-table__action-rail {
    justify-content: flex-start;
  }
}
</style>
