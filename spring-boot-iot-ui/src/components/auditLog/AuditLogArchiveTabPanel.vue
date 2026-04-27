<template>
  <div data-testid="system-log-archive-panel" class="audit-log-system-panel audit-log-system-panel--archives">
    <section v-loading="loading" class="audit-log-archive-batch-ledger standard-list-surface">
      <header class="audit-log-archive-batch-ledger__header">
        <div class="audit-log-archive-batch-ledger__header-main">
          <div>
            <h3>归档批次台账</h3>
          </div>
          <div class="audit-log-archive-batch-ledger__filters">
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>批次号</span>
              <input
                :value="filters.batchNo"
                data-testid="archive-batch-filter-batch-no"
                type="text"
                placeholder="按批次号筛选"
                @input="handleFilterInput('batchNo', $event)"
              >
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>状态</span>
              <select
                :value="filters.status"
                data-testid="archive-batch-filter-status"
                @change="handleFilterSelect('status', $event)"
              >
                <option value="">全部状态</option>
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>对比结论</span>
              <select
                :value="filters.compareStatus"
                data-testid="archive-batch-filter-compare-status"
                @change="handleFilterSelect('compareStatus', $event)"
              >
                <option value="">全部结论</option>
                <option v-for="option in compareStatusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>开始日期</span>
              <input
                :value="filters.dateFrom"
                data-testid="archive-batch-filter-date-from"
                type="date"
                @input="handleFilterInput('dateFrom', $event)"
              >
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>结束日期</span>
              <input
                :value="filters.dateTo"
                data-testid="archive-batch-filter-date-to"
                type="date"
                @input="handleFilterInput('dateTo', $event)"
              >
            </label>
            <label
              class="audit-log-archive-batch-ledger__filter-field audit-log-archive-batch-ledger__filter-field--checkbox"
            >
              <span>仅看异常</span>
              <input
                :checked="filters.onlyAbnormal"
                data-testid="archive-batch-filter-only-abnormal"
                type="checkbox"
                @change="handleFilterToggle('onlyAbnormal', $event)"
              >
            </label>
          </div>
          <div class="audit-log-archive-batch-ledger__filter-actions">
            <StandardButton
              data-testid="archive-batch-search-button"
              action="query"
              @click="emit('search')"
            >
              查询
            </StandardButton>
            <StandardButton
              data-testid="archive-batch-reset-button"
              action="reset"
              @click="emit('reset')"
            >
              重置
            </StandardButton>
          </div>
        </div>
        <span>{{ rows.length }} / {{ total }}</span>
      </header>

      <div class="audit-log-archive-batch-ledger__overview">
        <article
          v-for="item in overviewCards"
          :key="item.key"
          :class="[
            'audit-log-archive-batch-ledger__overview-card',
            { 'is-clickable': item.clickable, 'is-active': item.active }
          ]"
          :data-testid="item.testId"
          :role="item.clickable ? 'button' : undefined"
          :tabindex="item.clickable ? 0 : undefined"
          @click="item.clickable && emit('select-overview-card', item.key)"
          @keydown.enter.prevent="item.clickable && emit('select-overview-card', item.key)"
          @keydown.space.prevent="item.clickable && emit('select-overview-card', item.key)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.meta }}</p>
        </article>
      </div>

      <button
        v-if="latestAbnormalFocus"
        data-testid="archive-batch-latest-focus"
        type="button"
        :class="[
          'audit-log-archive-batch-ledger__latest-focus',
          { 'is-active': latestAbnormalFocus.active }
        ]"
        @click="emit('select-latest-abnormal')"
      >
        <span>最近异常批次</span>
        <strong>{{ latestAbnormalFocus.batchNo }}</strong>
        <small>{{ latestAbnormalFocus.occurredAt }}</small>
      </button>
      <div v-if="overviewLoading" class="audit-log-slow-summary__empty">
        正在汇总异常摘要
      </div>
      <div v-else-if="overviewErrorMessage" class="audit-log-slow-summary__empty">
        {{ overviewErrorMessage }}
      </div>
      <div v-if="errorMessage" class="audit-log-slow-summary__empty">
        {{ errorMessage }}
      </div>
      <div v-else-if="rows.length === 0" class="audit-log-slow-summary__empty">
        暂无归档批次记录
      </div>
      <section v-else data-testid="archive-batch-master-table" class="audit-log-archive-master-table">
        <header data-testid="archive-batch-master-header" class="audit-log-archive-master-table__header">
          <span>归档批次</span>
          <span>执行状态</span>
          <span>对比结论</span>
          <span>风险信号</span>
          <span>最近时间</span>
          <span>操作</span>
        </header>
        <article
          v-for="row in rows"
          :key="buildArchiveBatchKey(row)"
          data-testid="archive-batch-master-row"
          :class="[
            'audit-log-archive-master-table__row',
            resolveArchiveBatchCompareStatusClass(row),
            { 'is-selected': buildArchiveBatchKey(row) === selectedBatchKey }
          ]"
          @click="emit('select-row', row)"
        >
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--identity">
            <strong>{{ formatArchiveBatchName(row) }}</strong>
            <span>{{ formatValue(row.sourceTable) }} / {{ formatRetentionDays(row.retentionDays) }}</span>
            <small>
              确认 {{ formatCount(row.confirmedExpiredRows) }} / 归档 {{ formatCount(row.archivedRows) }} / 删除
              {{ formatCount(row.deletedRows) }}
            </small>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--status">
            <span :class="['audit-log-archive-master-table__state-chip', resolveArchiveStatusTone(row.status)]">
              {{ formatValue(row.status) }}
            </span>
            <small>{{ resolveArchiveStatusMeta(row) }}</small>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--status">
            <span
              :class="[
                'audit-log-archive-master-table__state-chip',
                resolveArchiveCompareTone(row.compareStatus)
              ]"
            >
              {{ formatValue(row.compareStatusLabel || formatArchiveBatchCompareStatus(row.compareStatus)) }}
            </span>
            <small>{{ resolveArchiveCompareMeta(row.compareStatus) }}</small>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--risk">
            <strong>偏差 {{ formatSignedCount(row.deltaDryRunVsDeleted) }}</strong>
            <span>剩余 {{ formatOptionalCount(row.remainingExpiredRows) }}</span>
            <span>报告 {{ formatArchiveBatchPreviewAvailability(row) }}</span>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--recent">
            <strong>{{ formatValue(row.createTime || row.updateTime) }}</strong>
            <small>截止 {{ formatValue(row.cutoffAt) }}</small>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--actions">
            <StandardButton
              data-testid="archive-batch-open-detail"
              action="view"
              link
              @click="emit('open-detail', row)"
            >
              详情
            </StandardButton>
          </div>
        </article>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">

interface ArchiveBatchFilters {
  batchNo: string;
  status: string;
  compareStatus: string;
  dateFrom: string;
  dateTo: string;
  onlyAbnormal: boolean;
}

type ArchiveFilterField =
  | 'batchNo'
  | 'status'
  | 'compareStatus'
  | 'dateFrom'
  | 'dateTo'
  | 'onlyAbnormal';

interface ArchiveBatchOption {
  label: string;
  value: string;
}

interface ArchiveOverviewCard {
  key: string;
  label: string;
  value: string;
  meta: string;
  clickable: boolean;
  active: boolean;
  testId: string;
}

interface ArchiveLatestAbnormalFocus {
  batchNo: string;
  occurredAt: string;
  active: boolean;
}

interface ArchiveBatchRow {
  id?: number | string | null;
  batchNo?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  status?: string | null;
  compareStatus?: string | null;
  compareStatusLabel?: string | null;
  sourceTable?: string | null;
  retentionDays?: number | null;
  cutoffAt?: string | null;
  confirmedExpiredRows?: number | null;
  candidateRows?: number | null;
  archivedRows?: number | null;
  deletedRows?: number | null;
  deltaConfirmedVsDeleted?: number | null;
  deltaDryRunVsDeleted?: number | null;
  remainingExpiredRows?: number | null;
}

type ValueFormatter = (value: string | number | null | undefined) => string;
type CountFormatter = (value: number | null | undefined) => string;
type ArchiveBatchFormatter = (row: ArchiveBatchRow) => string;
type CompareStatusResolver = (row: ArchiveBatchRow) => string;
type AbnormalStatusResolver = (status: string | null | undefined) => boolean;

const props = defineProps<{
  loading: boolean;
  total: number;
  rows: ArchiveBatchRow[];
  errorMessage: string;
  overviewLoading: boolean;
  overviewErrorMessage: string;
  focusHint?: string;
  filters: ArchiveBatchFilters;
  statusOptions: ArchiveBatchOption[];
  compareStatusOptions: ArchiveBatchOption[];
  overviewCards: ArchiveOverviewCard[];
  latestAbnormalFocus?: ArchiveLatestAbnormalFocus | null;
  activeRow: ArchiveBatchRow | null;
  selectedBatchKey: string;
  formatValue: ValueFormatter;
  formatCount: CountFormatter;
  formatOptionalCount: CountFormatter;
  formatSignedCount: CountFormatter;
  formatRetentionDays: CountFormatter;
  formatArchiveBatchName: ArchiveBatchFormatter;
  formatArchiveBatchCompareStatus: ValueFormatter;
  formatArchiveBatchPreviewAvailability: ArchiveBatchFormatter;
  formatArchiveBatchFooter: ArchiveBatchFormatter;
  resolveArchiveBatchCompareStatusClass: CompareStatusResolver;
  isArchiveBatchAbnormalStatus: AbnormalStatusResolver;
}>();

const emit = defineEmits<{
  (event: 'update-filter', payload: { field: ArchiveFilterField; value: string | boolean }): void;
  (event: 'search'): void;
  (event: 'reset'): void;
  (event: 'select-overview-card', key: string): void;
  (event: 'select-latest-abnormal'): void;
  (event: 'select-row', row: ArchiveBatchRow): void;
  (event: 'open-detail', row: ArchiveBatchRow): void;
}>();

const buildArchiveBatchKey = (row?: Partial<ArchiveBatchRow> | null) =>
  String(row?.batchNo || row?.id || row?.createTime || '');

const resolveArchiveStatusTone = (status?: string | null) => {
  if (status === 'FAILED' || status === 'FAILURE') {
    return 'is-danger';
  }
  if (status === 'PARTIAL' || status === 'RUNNING') {
    return 'is-warning';
  }
  if (status === 'SUCCEEDED' || status === 'SUCCESS') {
    return 'is-success';
  }
  return 'is-neutral';
};

const resolveArchiveCompareTone = (status?: string | null) => {
  if (status === 'DRIFTED') {
    return 'is-danger';
  }
  if (status === 'PARTIAL' || status === 'UNAVAILABLE') {
    return 'is-warning';
  }
  if (status === 'MATCHED') {
    return 'is-success';
  }
  return 'is-neutral';
};

const resolveArchiveStatusMeta = (row: ArchiveBatchRow) =>
  row.failedReason ? props.formatValue(row.failedReason) : `更新 ${props.formatValue(row.updateTime)}`;

const resolveArchiveCompareMeta = (status?: string | null) => {
  if (status === 'DRIFTED') {
    return '需要复核';
  }
  if (status === 'PARTIAL') {
    return '部分可比';
  }
  if (status === 'UNAVAILABLE') {
    return '报告缺口';
  }
  if (status === 'MATCHED') {
    return '结果稳定';
  }
  return '待判断';
};

function handleFilterInput(field: Extract<ArchiveFilterField, 'batchNo' | 'dateFrom' | 'dateTo'>, event: Event) {
  emit('update-filter', {
    field,
    value: (event.target as HTMLInputElement).value
  });
}

function handleFilterSelect(field: Extract<ArchiveFilterField, 'status' | 'compareStatus'>, event: Event) {
  emit('update-filter', {
    field,
    value: (event.target as HTMLSelectElement).value
  });
}

function handleFilterToggle(field: Extract<ArchiveFilterField, 'onlyAbnormal'>, event: Event) {
  emit('update-filter', {
    field,
    value: (event.target as HTMLInputElement).checked
  });
}
</script>

<style scoped>
.audit-log-archive-batch-ledger {
  display: grid;
  gap: 0.95rem;
}

.audit-log-archive-batch-ledger__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.audit-log-archive-batch-ledger__header-main {
  flex: 1;
  min-width: 18rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.audit-log-archive-batch-ledger__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-archive-batch-ledger__header > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-archive-batch-ledger__filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
  align-items: end;
}

.audit-log-archive-batch-ledger__filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.audit-log-archive-batch-ledger__filter-field span {
  color: var(--el-text-color-secondary);
  font-size: 0.82rem;
}

.audit-log-archive-batch-ledger__filter-field input,
.audit-log-archive-batch-ledger__filter-field select {
  width: 100%;
  min-height: 2.35rem;
  border: 1px solid var(--el-border-color);
  border-radius: 0.56rem;
  padding: 0.5rem 0.75rem;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

.audit-log-archive-batch-ledger__filter-field--checkbox {
  flex-direction: row;
  align-items: center;
  gap: 0.6rem;
  min-height: 2.35rem;
}

.audit-log-archive-batch-ledger__filter-field--checkbox input {
  width: 1rem;
  height: 1rem;
}

.audit-log-archive-batch-ledger__filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
}

.audit-log-archive-batch-ledger__overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-archive-batch-ledger__overview-card {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
  padding: 0.85rem 0.95rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 88%, transparent);
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.audit-log-archive-batch-ledger__overview-card.is-clickable {
  cursor: pointer;
}

.audit-log-archive-batch-ledger__overview-card.is-clickable:hover,
.audit-log-archive-batch-ledger__overview-card.is-clickable:focus-visible {
  border-color: color-mix(in srgb, var(--el-color-primary) 42%, var(--panel-border));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 80%, var(--panel-bg));
}

.audit-log-archive-batch-ledger__overview-card.is-clickable:focus-visible {
  outline: none;
}

.audit-log-archive-batch-ledger__overview-card.is-active {
  border-color: color-mix(in srgb, var(--el-color-primary) 52%, var(--panel-border));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 88%, var(--panel-bg));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
}

.audit-log-archive-batch-ledger__overview-card span,
.audit-log-archive-batch-ledger__overview-card p {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-archive-batch-ledger__overview-card strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 1rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-archive-batch-ledger__latest-focus {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-bg-color) 92%, white);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

.audit-log-archive-batch-ledger__latest-focus.is-active {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.audit-log-archive-batch-ledger__latest-focus strong {
  color: inherit;
  font-size: 13px;
}

.audit-log-archive-batch-ledger__latest-focus small,
.audit-log-archive-master-table__cell span,
.audit-log-archive-master-table__cell small,
.audit-log-archive-master-table__header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.audit-log-archive-master-table__cell strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
  line-height: 1.5;
}

.audit-log-archive-master-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-log-archive-master-table__header,
.audit-log-archive-master-table__row {
  display: grid;
  grid-template-columns:
    minmax(220px, 1.7fr)
    minmax(96px, 0.78fr)
    minmax(96px, 0.8fr)
    minmax(148px, 0.95fr)
    minmax(132px, 0.88fr)
    minmax(74px, 0.58fr);
  gap: 14px;
  align-items: start;
}

.audit-log-archive-master-table__header {
  padding: 0 10px;
}

.audit-log-archive-master-table__row {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.audit-log-archive-master-table__row:hover {
  border-color: var(--el-color-primary-light-5);
}

.audit-log-archive-master-table__row.is-selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.12);
  background: rgba(236, 245, 255, 0.55);
}

.audit-log-archive-master-table__cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.audit-log-archive-master-table__cell--identity strong,
.audit-log-archive-master-table__cell--risk strong,
.audit-log-archive-master-table__cell--recent strong {
  font-size: 14px;
}

.audit-log-archive-master-table__state-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-bg-color) 92%, white);
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 600;
}

.audit-log-archive-master-table__state-chip.is-danger {
  border-color: color-mix(in srgb, var(--el-color-danger) 40%, white);
  background: color-mix(in srgb, var(--el-color-danger-light-9) 88%, white);
  color: var(--el-color-danger-dark-2);
}

.audit-log-archive-master-table__state-chip.is-warning {
  border-color: color-mix(in srgb, var(--el-color-warning) 44%, white);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 88%, white);
  color: color-mix(in srgb, var(--el-color-warning-dark-2) 82%, black);
}

.audit-log-archive-master-table__state-chip.is-success {
  border-color: color-mix(in srgb, var(--el-color-success) 40%, white);
  background: color-mix(in srgb, var(--el-color-success-light-9) 88%, white);
  color: var(--el-color-success-dark-2);
}

.audit-log-archive-master-table__state-chip.is-neutral {
  border-color: var(--el-border-color-lighter);
  background: color-mix(in srgb, var(--el-bg-color) 92%, white);
  color: var(--el-text-color-secondary);
}

.audit-log-archive-master-table__cell--actions {
  align-items: flex-end;
  justify-content: center;
}

@media (max-width: 1280px) {
  .audit-log-archive-batch-ledger__filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .audit-log-archive-batch-ledger__overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .audit-log-archive-batch-ledger__header {
    flex-direction: column;
    align-items: stretch;
  }

  .audit-log-archive-batch-ledger__header > span {
    align-self: flex-start;
  }

  .audit-log-archive-batch-ledger__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-log-archive-batch-ledger__overview {
    grid-template-columns: 1fr;
  }

  .audit-log-archive-master-table__header {
    display: none;
  }

  .audit-log-archive-master-table__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-log-archive-master-table__cell--identity,
  .audit-log-archive-master-table__cell--risk,
  .audit-log-archive-master-table__cell--actions {
    grid-column: 1 / -1;
  }

  .audit-log-archive-master-table__cell--actions {
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .audit-log-archive-batch-ledger__filters {
    grid-template-columns: 1fr;
  }

  .audit-log-archive-master-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
