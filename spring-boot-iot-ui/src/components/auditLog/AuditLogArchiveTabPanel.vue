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

      <section data-testid="archive-governance-focus-strip" class="audit-log-archive-focus-strip">
        <div class="audit-log-archive-focus-strip__copy">
          <span>{{ focusEyebrow }}</span>
          <strong>{{ focusTitle }}</strong>
          <p>{{ focusSubtitle }}</p>
        </div>
        <div class="audit-log-archive-focus-strip__meta">
          <span>{{ focusMetaPrimary }}</span>
          <span>{{ focusMetaSecondary }}</span>
        </div>
      </section>

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

      <div v-if="focusHint" class="audit-log-archive-batch-ledger__focus-hint">
        {{ focusHint }}
      </div>
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
        <header class="audit-log-archive-master-table__header">
          <span>批次</span>
          <span>执行状态</span>
          <span>确认与执行</span>
          <span>治理信号</span>
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
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--title">
            <strong>{{ formatArchiveBatchName(row) }}</strong>
            <span>{{ formatValue(row.createTime || row.updateTime) }}</span>
          </div>
          <div class="audit-log-archive-master-table__cell">
            <span>{{ formatValue(row.status) }}</span>
            <span>{{ formatValue(row.compareStatusLabel || formatArchiveBatchCompareStatus(row.compareStatus)) }}</span>
            <span>{{ formatValue(row.sourceTable) }}</span>
            <span>{{ formatRetentionDays(row.retentionDays) }}</span>
            <span>截止 {{ formatValue(row.cutoffAt) }}</span>
          </div>
          <div class="audit-log-archive-master-table__cell">
            <span>确认 {{ formatCount(row.confirmedExpiredRows) }}</span>
            <span>候选 {{ formatCount(row.candidateRows) }}</span>
            <span>归档 {{ formatCount(row.archivedRows) }}</span>
            <span>删除 {{ formatCount(row.deletedRows) }}</span>
          </div>
          <div class="audit-log-archive-master-table__cell">
            <span>确认差值 {{ formatSignedCount(row.deltaConfirmedVsDeleted) }}</span>
            <span>dry-run 差值 {{ formatSignedCount(row.deltaDryRunVsDeleted) }}</span>
            <span>剩余过期 {{ formatOptionalCount(row.remainingExpiredRows) }}</span>
            <span>报告 {{ formatArchiveBatchPreviewAvailability(row) }}</span>
          </div>
          <div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--actions">
            <span>{{ formatArchiveBatchFooter(row) }}</span>
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
import { computed } from 'vue';

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
  focusHint: string;
  filters: ArchiveBatchFilters;
  statusOptions: ArchiveBatchOption[];
  compareStatusOptions: ArchiveBatchOption[];
  overviewCards: ArchiveOverviewCard[];
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
  (event: 'select-row', row: ArchiveBatchRow): void;
  (event: 'open-detail', row: ArchiveBatchRow): void;
}>();

const activeOverviewCard = computed(() => props.overviewCards.find((item) => item.active) ?? null);

const focusEyebrow = computed(() => {
  if (props.activeRow) {
    return '当前治理对象';
  }
  if (activeOverviewCard.value) {
    return '当前治理焦点';
  }
  return '归档治理';
});

const focusTitle = computed(() => {
  if (props.activeRow) {
    return props.formatArchiveBatchName(props.activeRow);
  }
  if (activeOverviewCard.value) {
    return activeOverviewCard.value.label;
  }
  return '归档批次台账';
});

const focusSubtitle = computed(() => {
  if (props.activeRow) {
    return [
      props.formatValue(props.activeRow.status),
      props.formatValue(
        props.activeRow.compareStatusLabel || props.formatArchiveBatchCompareStatus(props.activeRow.compareStatus)
      ),
      props.formatValue(props.activeRow.sourceTable)
    ].join(' · ');
  }
  if (activeOverviewCard.value) {
    return `${activeOverviewCard.value.value} · ${activeOverviewCard.value.meta}`;
  }
  return '按批次、状态和对比结论排查归档治理执行情况';
});

const focusMetaPrimary = computed(() => {
  if (props.activeRow) {
    return `确认 ${props.formatCount(props.activeRow.confirmedExpiredRows)}`;
  }
  return `当前结果 ${props.rows.length} / ${props.total}`;
});

const focusMetaSecondary = computed(() => {
  if (props.activeRow) {
    return `剩余过期 ${props.formatOptionalCount(props.activeRow.remainingExpiredRows)}`;
  }
  return props.focusHint || '详情抽屉继续承接批次对比和确认报告';
});

const buildArchiveBatchKey = (row?: Partial<ArchiveBatchRow> | null) =>
  String(row?.batchNo || row?.id || row?.createTime || '');

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
.audit-log-archive-focus-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(255, 248, 240, 0.9), rgba(255, 255, 255, 0.98));
}

.audit-log-archive-focus-strip__copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.audit-log-archive-focus-strip__copy > span,
.audit-log-archive-focus-strip__meta > span,
.audit-log-archive-master-table__cell span,
.audit-log-archive-master-table__header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.audit-log-archive-focus-strip__copy strong,
.audit-log-archive-master-table__cell strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
  line-height: 1.5;
}

.audit-log-archive-focus-strip__copy p {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.audit-log-archive-focus-strip__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 40%;
}

.audit-log-archive-focus-strip__meta > span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
}

.audit-log-archive-master-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-log-archive-master-table__header,
.audit-log-archive-master-table__row {
  display: grid;
  grid-template-columns: minmax(210px, 1.4fr) minmax(220px, 1.15fr) minmax(220px, 1fr) minmax(220px, 1fr) minmax(140px, 0.85fr);
  gap: 14px;
  align-items: start;
}

.audit-log-archive-master-table__header {
  padding: 0 10px;
}

.audit-log-archive-master-table__row {
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

.audit-log-archive-master-table__cell--actions {
  align-items: flex-start;
}

@media (max-width: 1200px) {
  .audit-log-archive-focus-strip {
    flex-direction: column;
  }

  .audit-log-archive-focus-strip__meta {
    max-width: none;
    justify-content: flex-start;
  }

  .audit-log-archive-master-table__header {
    display: none;
  }

  .audit-log-archive-master-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
