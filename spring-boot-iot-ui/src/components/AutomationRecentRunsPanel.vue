<template>
  <PanelCard
    title="历史运行台账"
    description="基于 logs/acceptance 下的 registry-run 结果建立只读历史台账，主流程从这里选择某次运行进入详情。"
  >
    <StandardListFilterHeader :model="filters" primary-columns="repeat(4, minmax(220px, 1fr))">
      <template #primary>
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="关键字（运行编号 / 场景 / 执行器）"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.status" clearable placeholder="聚合状态">
            <el-option label="全部状态" value="" />
            <el-option label="通过" value="passed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.runnerType" clearable placeholder="执行器">
            <el-option label="全部执行器" value="" />
            <el-option label="browserPlan" value="browserPlan" />
            <el-option label="apiSmoke" value="apiSmoke" />
            <el-option label="messageFlow" value="messageFlow" />
            <el-option label="riskDrill" value="riskDrill" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            unlink-panels
            clearable
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
      </template>
      <template #actions>
        <StandardButton action="query" @click="handleSearch">查询</StandardButton>
        <StandardButton action="reset" @click="handleReset">重置</StandardButton>
        <StandardButton action="refresh" :loading="loading" @click="handleRefresh">刷新台账</StandardButton>
      </template>
    </StandardListFilterHeader>

    <StandardInlineState v-if="errorMessage" tone="error" :message="errorMessage" />
    <StandardInlineState
      v-else-if="loading && ledgerRuns.length === 0"
      message="正在读取历史运行台账..."
    />
    <div v-else-if="ledgerRuns.length === 0" class="empty-block">
      当前筛选范围内没有命中的历史运行结果；可调整条件后重试，或继续使用兼容导入查看外部 JSON。
    </div>
    <template v-else>
      <StandardTableToolbar
        compact
        :meta-items="[
          `命中 ${pagination.total} 条`,
          selectedRunId ? `当前 ${selectedRunId}` : '当前未选择运行',
          lastReloadedAt ? `最近刷新 ${lastReloadedAt}` : '尚未刷新'
        ]"
      />
      <div class="table-wrap">
        <el-table
          :data="displayRows"
          size="small"
          border
          highlight-current-row
          @row-click="handleRowSelect"
        >
          <StandardTableTextColumn prop="runId" label="运行编号" :min-width="170" />
          <StandardTableTextColumn prop="updatedAt" label="更新时间" :min-width="170" />
          <StandardTableTextColumn prop="statusText" label="状态" :width="110" />
          <StandardTableTextColumn prop="runnerTypeText" label="执行器" :min-width="180" />
          <StandardTableTextColumn prop="summaryText" label="汇总" :min-width="200" />
          <StandardTableTextColumn prop="evidenceText" label="证据" :width="110" />
          <el-table-column label="操作" :width="150">
            <template #default="{ row }">
              <div class="action-cell">
                <span v-if="row.runId === selectedRunId" class="selected-label">当前已载入</span>
                <StandardButton
                  v-else
                  action="confirm"
                  :link="true"
                  @click.stop="$emit('select-run', row.runId)"
                >
                  查看详情
                </StandardButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="pagination.total > 0" class="ledger-pagination">
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="$emit('page-change', $event)"
          @size-change="$emit('page-size-change', $event)"
        />
      </div>
    </template>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ServerPaginationState } from '@/composables/useServerPagination';
import type { AutomationResultLedgerFilters, AutomationResultRunSummary } from '../types/automation';
import PanelCard from './PanelCard.vue';
import StandardButton from './StandardButton.vue';
import StandardInlineState from './StandardInlineState.vue';
import StandardListFilterHeader from './StandardListFilterHeader.vue';
import StandardPagination from './StandardPagination.vue';
import StandardTableTextColumn from './StandardTableTextColumn.vue';
import StandardTableToolbar from './StandardTableToolbar.vue';

const statusLabelMap: Record<string, string> = {
  passed: '通过',
  failed: '失败'
};

const props = withDefaults(
  defineProps<{
    ledgerRuns: AutomationResultRunSummary[];
    filters: AutomationResultLedgerFilters;
    pagination: ServerPaginationState;
    loading?: boolean;
    errorMessage?: string;
    selectedRunId?: string;
    lastReloadedAt?: string;
  }>(),
  {
    loading: false,
    errorMessage: '',
    selectedRunId: '',
    lastReloadedAt: ''
  }
);

const displayRows = computed(() =>
  props.ledgerRuns.map((item) => ({
    ...item,
    statusText: statusLabelMap[item.status] || item.status,
    runnerTypeText: item.runnerTypes.length > 0 ? item.runnerTypes.join(' / ') : '--',
    summaryText: `总 ${item.summary.total} / 通过 ${item.summary.passed} / 失败 ${item.summary.failed}`,
    evidenceText: `${item.relatedEvidenceFiles.length} 份`
  }))
);

function handleRowSelect(row: { runId: string }) {
  if (!row.runId || row.runId === props.selectedRunId) {
    return;
  }
  emit('select-run', row.runId);
}

const emit = defineEmits<{
  refresh: [];
  search: [];
  reset: [];
  'select-run': [runId: string];
  'page-change': [page: number];
  'page-size-change': [pageSize: number];
}>();

function handleSearch() {
  emit('search');
}

function handleReset() {
  emit('reset');
}

function handleRefresh() {
  emit('refresh');
}
</script>

<style scoped>
.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.table-wrap {
  min-width: 0;
}

.ledger-pagination {
  margin-top: 0.9rem;
}

.action-cell {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.selected-label {
  color: var(--success);
  font-size: 0.88rem;
  font-weight: 600;
}
</style>
