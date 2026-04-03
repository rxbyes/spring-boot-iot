<template>
  <StandardPageShell class="automation-results-view">
    <StandardWorkbenchPanel
      title="结果与基线中心"
      description="集中读取历史运行台账或兼容导入统一运行汇总，查看失败场景，并结合质量建议维护基线证据。"
      show-notices
    >
      <template #notices>
        <div class="automation-chip-list">
          <span>历史台账</span>
          <span>统一汇总</span>
          <span>证据预览</span>
          <span>失败场景</span>
          <span>质量建议</span>
          <span>基线证据</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="结果概况" description="先看导入结果，再判断当前基线是否稳定。">
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

        <PanelCard title="当前判断" description="把统一运行汇总与计划建议并列判断，优先处理 blocker。">
          <StandardInlineState :tone="resultTone" :message="resultMessage" />
          <ul class="phase-ideas">
            <li>结果中心只负责导入和复盘，不再编辑场景计划或执行配置。</li>
            <li>建议先处理失败场景，再按测试建议补齐断言、截图或接口校验。</li>
            <li>当导入结果全部通过后，再做基线归档与交付复核。</li>
          </ul>
        </PanelCard>

        <ResponsePanel
          title="统一汇总快照"
          description="保留当前导入的运行摘要，方便做结果复盘和证据引用。"
          :body="summaryBody"
        />
      </section>

      <section>
        <AutomationRecentRunsPanel
          :ledger-runs="ledgerRuns"
          :filters="ledgerFilters"
          :pagination="pagination"
          :loading="ledgerLoading"
          :error-message="ledgerErrorMessage"
          :selected-run-id="selectedLedgerRunId"
          :last-reloaded-at="lastLedgerReloadedAt"
          @refresh="fetchRunLedger"
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
        <PanelCard title="失败场景明细" description="将导入失败与注册表口径并列，便于优先定位 blocker。">
          <StandardInlineState v-if="currentRunErrorMessage" tone="error" :message="currentRunErrorMessage" />
          <div v-else-if="failedScenarioDetails.length === 0" class="empty-block">
            当前没有失败场景；如果尚未选择运行，这里会在选中后展示失败明细。
          </div>
          <el-table v-else :data="failedScenarioDetails" size="small" border>
            <StandardTableTextColumn prop="title" label="场景" :min-width="180" />
            <StandardTableTextColumn prop="scenarioId" label="编码" :min-width="180" />
            <StandardTableTextColumn prop="runnerType" label="执行器" :width="110" />
            <StandardTableTextColumn prop="blocking" label="阻断级别" :width="110" />
            <StandardTableTextColumn prop="docRef" label="文档映射" :min-width="180" />
            <el-table-column label="摘要" :min-width="220">
              <template #default="{ row }">
                <span class="failure-summary">{{ row.summary }}</span>
              </template>
            </el-table-column>
          </el-table>
        </PanelCard>
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import AutomationRecentRunsPanel from '../components/AutomationRecentRunsPanel.vue';
import AutomationResultEvidencePanel from '../components/AutomationResultEvidencePanel.vue';
import AutomationResultImportPanel from '../components/AutomationResultImportPanel.vue';
import AutomationSuggestionPanel from '../components/AutomationSuggestionPanel.vue';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useAutomationResultsWorkbench } from '../composables/useAutomationResultsWorkbench';

const {
  suggestions,
  pagination,
  ledgerFilters,
  ledgerRuns,
  ledgerLoading,
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
  failedScenarioDetails,
  summaryBody,
  fetchRunLedger,
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
.automation-results-view {
  min-width: 0;
}

.automation-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.automation-chip-list span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 5%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
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

@media (max-width: 1024px) {
  .results-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
