<template>
  <div class="automation-evidence-workspace-section">
    <section class="tri-grid">
      <PanelCard title="结果概况" description="先看当前结果，再判断当前基线是否稳定。">
        <div class="quad-grid results-metrics">
          <MetricCard
            v-for="metric in resultsMetrics"
            :key="metric.label"
            class="results-metrics__card"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
          />
        </div>
      </PanelCard>

      <PanelCard title="当前判断" description="把当前运行结果与计划建议并列判断，优先处理 blocker。">
        <StandardInlineState :tone="resultTone" :message="resultMessage" />
        <ul class="phase-ideas">
          <li>结果证据只负责历史台账查看、兼容导入和复盘，不再编辑场景计划或执行配置。</li>
          <li>建议先处理失败场景，再按测试建议补齐断言、截图或接口校验。</li>
          <li>当当前结果全部通过后，再做基线归档与交付复核。</li>
        </ul>
      </PanelCard>

      <ResponsePanel
        title="统一汇总快照"
        description="保留当前选中或导入的运行摘要，方便做结果复盘和证据引用。"
        :body="summaryBody"
      />
    </section>

    <section>
      <AutomationRecentRunsPanel
        :ledger-runs="ledgerRuns"
        :filters="ledgerFilters"
        :facet-options="ledgerFacetOptions"
        :pagination="pagination"
        :loading="ledgerLoading"
        :refresh-index-loading="refreshArchiveIndexLoading"
        :error-message="ledgerErrorMessage"
        :selected-run-id="selectedLedgerRunId"
        :last-reloaded-at="lastLedgerReloadedAt"
        @refresh="fetchRunLedger"
        @refresh-index="refreshResultArchiveIndex"
        @search="applyLedgerFilters"
        @reset="resetLedgerAndReload"
        @select-run="selectLedgerRun"
        @page-change="handleLedgerPageChange"
        @page-size-change="handleLedgerPageSizeChange"
      />
    </section>

    <section>
      <AutomationResultEvidencePanel
        :run-id="activeEvidenceRunId"
        :evidence-items="visibleEvidenceItems"
        :loading="visibleEvidenceLoading"
        :error-message="visibleEvidenceErrorMessage"
        :selected-path="visibleSelectedEvidencePath"
        :preview="visibleEvidencePreview"
        :preview-loading="visibleEvidencePreviewLoading"
        :preview-error-message="visibleEvidencePreviewErrorMessage"
        @select-evidence="selectEvidence(activeEvidenceRunId, $event)"
      />
    </section>

    <section class="two-column-grid">
      <AutomationResultImportPanel
        :imported-run="importedRun"
        @import-json="importRegistryRunSummary"
        @clear="clearImportedRun"
      />
      <AutomationSuggestionPanel :suggestions="suggestions" />
    </section>

    <section>
      <PanelCard title="失败场景明细" description="将当前运行失败与注册表口径并列，便于优先定位 blocker。">
        <StandardInlineState v-if="currentRunErrorMessage" tone="error" :message="currentRunErrorMessage" />
        <div v-else-if="failedScenarioDetails.length === 0" class="empty-block">
          当前没有失败场景；如果尚未选择运行，这里会在选中后展示失败明细。
        </div>
        <template v-else>
          <div v-if="failureCategorySummaryRows.length > 0" class="failure-diagnosis-overview">
            <div class="failure-diagnosis-overview__header">
              <div>
                <h3>失败分类分布</h3>
                <p>当前主分类：{{ failurePrimaryCategory || '其他' }}</p>
              </div>
              <span class="failure-diagnosis-overview__note">按失败场景的规则归因汇总</span>
            </div>
            <div class="failure-diagnosis-overview__grid">
              <article
                v-for="item in failureCategorySummaryRows"
                :key="item.category"
                :class="[
                  'failure-diagnosis-overview__item',
                  { 'failure-diagnosis-overview__item--primary': item.primary }
                ]"
              >
                <span>{{ item.category }}</span>
                <strong>{{ item.count }}</strong>
                <small>失败场景</small>
              </article>
            </div>
          </div>

          <el-table :data="failedScenarioDetails" size="small" border>
            <StandardTableTextColumn prop="title" label="场景" :min-width="180" />
            <StandardTableTextColumn prop="scenarioId" label="编码" :min-width="180" />
            <StandardTableTextColumn prop="runnerType" label="执行器" :width="110" />
            <StandardTableTextColumn prop="blocking" label="阻断级别" :width="110" />
            <StandardTableTextColumn prop="diagnosisCategory" label="主分类" :width="100" />
            <StandardTableTextColumn prop="docRef" label="文档映射" :min-width="180" />
            <el-table-column label="摘要" :min-width="220">
              <template #default="{ row }">
                <span class="failure-summary">{{ row.summary }}</span>
              </template>
            </el-table-column>
            <el-table-column label="判断理由" :min-width="220">
              <template #default="{ row }">
                <span class="failure-summary">{{ row.diagnosisReason }}</span>
              </template>
            </el-table-column>
            <el-table-column label="证据摘要" :min-width="260">
              <template #default="{ row }">
                <span class="failure-summary">{{ row.evidenceSummary }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import AutomationRecentRunsPanel from '@/components/AutomationRecentRunsPanel.vue';
import AutomationResultEvidencePanel from '@/components/AutomationResultEvidencePanel.vue';
import AutomationResultImportPanel from '@/components/AutomationResultImportPanel.vue';
import AutomationSuggestionPanel from '@/components/AutomationSuggestionPanel.vue';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import ResponsePanel from '@/components/ResponsePanel.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import { useAutomationResultsWorkbench } from '@/composables/useAutomationResultsWorkbench';

const {
  suggestions,
  pagination,
  ledgerFilters,
  ledgerRuns,
  ledgerLoading,
  ledgerFacetOptions,
  refreshArchiveIndexLoading,
  ledgerErrorMessage,
  lastLedgerReloadedAt,
  selectedLedgerRunId,
  importedRun,
  currentRunErrorMessage,
  activeEvidenceRunId,
  visibleEvidenceItems,
  visibleSelectedEvidencePath,
  visibleEvidencePreview,
  visibleEvidenceLoading,
  visibleEvidenceErrorMessage,
  visibleEvidencePreviewLoading,
  visibleEvidencePreviewErrorMessage,
  resultsMetrics,
  resultTone,
  resultMessage,
  failureCategorySummaryRows,
  failurePrimaryCategory,
  failedScenarioDetails,
  summaryBody,
  fetchRunLedger,
  refreshResultArchiveIndex,
  applyLedgerFilters,
  resetLedgerAndReload,
  handleLedgerPageChange,
  handleLedgerPageSizeChange,
  selectLedgerRun,
  selectEvidence,
  importRegistryRunSummary,
  clearImportedRun
} = useAutomationResultsWorkbench();

onMounted(() => {
  void fetchRunLedger();
});
</script>

<style scoped>
.automation-evidence-workspace-section {
  display: grid;
  gap: 1rem;
}

.results-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.results-metrics__card {
  min-height: 7.75rem;
}

.phase-ideas {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.failure-summary {
  color: var(--text-secondary);
  line-height: 1.7;
}

.failure-diagnosis-overview {
  display: grid;
  gap: 0.85rem;
  margin-bottom: 0.95rem;
}

.failure-diagnosis-overview__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.failure-diagnosis-overview__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
}

.failure-diagnosis-overview__header p {
  margin: 0.28rem 0 0;
  color: var(--text-secondary);
}

.failure-diagnosis-overview__note {
  color: var(--text-tertiary);
  font-size: 0.84rem;
}

.failure-diagnosis-overview__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.failure-diagnosis-overview__item {
  display: grid;
  gap: 0.24rem;
  padding: 0.9rem 0.95rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.92);
}

.failure-diagnosis-overview__item span,
.failure-diagnosis-overview__item small {
  color: var(--text-secondary);
}

.failure-diagnosis-overview__item strong {
  color: var(--text-heading);
  font-size: 1.4rem;
  line-height: 1;
}

.failure-diagnosis-overview__item--primary {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 24%, var(--panel-border));
  background: color-mix(in srgb, var(--danger, #d84f45) 6%, white);
}

@media (max-width: 1024px) {
  .results-metrics {
    grid-template-columns: 1fr;
  }

  .failure-diagnosis-overview__grid {
    grid-template-columns: 1fr;
  }

  .failure-diagnosis-overview__header {
    flex-direction: column;
  }
}
</style>
