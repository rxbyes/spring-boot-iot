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
            <label class="audit-log-archive-batch-ledger__filter-field audit-log-archive-batch-ledger__filter-field--checkbox">
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
              筛选
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
      <div v-else class="audit-log-archive-batch-ledger__list">
        <article
          v-for="row in rows"
          :key="`message-archive-batch-${row.id || row.batchNo || row.createTime}`"
          :class="[
            'audit-log-archive-batch-ledger__item',
            resolveArchiveBatchCompareStatusClass(row),
            { 'is-abnormal': isArchiveBatchAbnormalStatus(row.compareStatus) }
          ]"
        >
          <div class="audit-log-archive-batch-ledger__title">
            <strong>{{ formatArchiveBatchName(row) }}</strong>
            <span>{{ formatValue(row.createTime || row.updateTime) }}</span>
          </div>
          <div class="audit-log-archive-batch-ledger__meta">
            <span>{{ formatValue(row.status) }}</span>
            <span>{{ formatValue(row.compareStatusLabel || formatArchiveBatchCompareStatus(row.compareStatus)) }}</span>
            <span>{{ formatValue(row.sourceTable) }}</span>
            <span>{{ formatRetentionDays(row.retentionDays) }}</span>
            <span>截止 {{ formatValue(row.cutoffAt) }}</span>
          </div>
          <div class="audit-log-archive-batch-ledger__metrics">
            <span>确认 {{ formatCount(row.confirmedExpiredRows) }}</span>
            <span>候选 {{ formatCount(row.candidateRows) }}</span>
            <span>归档 {{ formatCount(row.archivedRows) }}</span>
            <span>删除 {{ formatCount(row.deletedRows) }}</span>
          </div>
          <div class="audit-log-archive-batch-ledger__insights">
            <span>确认差值 {{ formatSignedCount(row.deltaConfirmedVsDeleted) }}</span>
            <span>dry-run 差值 {{ formatSignedCount(row.deltaDryRunVsDeleted) }}</span>
            <span>剩余过期 {{ formatOptionalCount(row.remainingExpiredRows) }}</span>
            <span>报告 {{ formatArchiveBatchPreviewAvailability(row) }}</span>
          </div>
          <div class="audit-log-archive-batch-ledger__footer">
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
      </div>
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

defineProps<{
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
  (event: 'open-detail', row: ArchiveBatchRow): void;
}>();

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
