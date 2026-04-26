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
              <input :value="filters.batchNo" data-testid="archive-batch-filter-batch-no" type="text" placeholder="按批次号筛选">
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>状态</span>
              <select :value="filters.status" data-testid="archive-batch-filter-status">
                <option value="">全部状态</option>
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="audit-log-archive-batch-ledger__filter-field">
              <span>对比结论</span>
              <select :value="filters.compareStatus" data-testid="archive-batch-filter-compare-status">
                <option value="">全部结论</option>
                <option v-for="option in compareStatusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
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
type Formatter<T = unknown, R = string> = (value: T) => R;

defineProps<{
  loading: boolean;
  total: number;
  rows: Record<string, any>[];
  errorMessage: string;
  overviewLoading: boolean;
  overviewErrorMessage: string;
  focusHint: string;
  filters: {
    batchNo: string;
    status: string;
    compareStatus: string;
    dateFrom: string;
    dateTo: string;
    onlyAbnormal: boolean;
  };
  statusOptions: Array<{ label: string; value: string }>;
  compareStatusOptions: Array<{ label: string; value: string }>;
  overviewCards: Array<{
    key: string;
    label: string;
    value: string;
    meta: string;
    clickable: boolean;
    active: boolean;
    testId: string;
  }>;
  formatValue: Formatter;
  formatCount: Formatter<number | null | undefined>;
  formatOptionalCount: Formatter<number | null | undefined>;
  formatSignedCount: Formatter<number | null | undefined>;
  formatRetentionDays: Formatter<number | null | undefined>;
  formatArchiveBatchName: Formatter;
  formatArchiveBatchCompareStatus: Formatter;
  formatArchiveBatchPreviewAvailability: Formatter;
  formatArchiveBatchFooter: Formatter;
  resolveArchiveBatchCompareStatusClass: (row: Record<string, any>) => string;
  isArchiveBatchAbnormalStatus: (status: string) => boolean;
}>();

const emit = defineEmits<{
  (event: 'open-detail', row: Record<string, any>): void;
}>();
</script>
