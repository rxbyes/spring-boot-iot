import { computed } from 'vue';
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';
import { useAutomationRegistryWorkbench } from './useAutomationRegistryWorkbench';

export function useAutomationResultsWorkbench() {
  const { suggestions } = useAutomationPlanBuilder();
  const {
    registryScenarios,
    pagination,
    ledgerFilters,
    ledgerRuns,
    ledgerLoading,
    ledgerErrorMessage,
    lastLedgerReloadedAt,
    selectedLedgerRunId,
    importedRun,
    displaySource,
    currentRun,
    currentRunErrorMessage,
    activeEvidenceRunId,
    visibleEvidenceItems,
    visibleSelectedEvidencePath,
    visibleEvidencePreview,
    visibleEvidenceLoading,
    visibleEvidenceErrorMessage,
    visibleEvidencePreviewLoading,
    visibleEvidencePreviewErrorMessage,
    fetchRunLedger,
    applyLedgerFilters,
    resetLedgerAndReload,
    handleLedgerPageChange,
    handleLedgerPageSizeChange,
    selectLedgerRun,
    selectEvidence,
    importRegistryRunSummary,
    clearImportedRun
  } = useAutomationRegistryWorkbench();

  const warningSuggestionCount = computed(
    () => suggestions.value.filter((item) => item.level === 'warning').length
  );
  const successSuggestionCount = computed(
    () => suggestions.value.filter((item) => item.level === 'success').length
  );

  const resultsMetrics = computed(() => [
    {
      label: '当前结果',
      value: currentRun.value ? String(currentRun.value.summary.total) : '0',
      badge: { label: displaySource.value === 'imported' ? 'Import' : 'Run', tone: 'brand' as const }
    },
    {
      label: '失败场景',
      value: currentRun.value ? String(currentRun.value.summary.failed) : '0',
      badge: {
        label: 'Fail',
        tone:
          currentRun.value && currentRun.value.summary.failed > 0
            ? ('danger' as const)
            : ('success' as const)
      }
    },
    {
      label: '待补齐建议',
      value: String(warningSuggestionCount.value),
      badge: { label: 'Gap', tone: 'warning' as const }
    },
    {
      label: '正向信号',
      value: String(successSuggestionCount.value),
      badge: { label: 'Good', tone: 'success' as const }
    }
  ]);

  const resultTone = computed<'info' | 'error'>(() => {
    if (currentRunErrorMessage.value) {
      return 'error';
    }
    return currentRun.value && currentRun.value.summary.failed > 0 ? 'error' : 'info';
  });

  const resultMessage = computed(() => {
    if (currentRunErrorMessage.value) {
      return currentRunErrorMessage.value;
    }

    if (!currentRun.value) {
      return '当前未选中运行，建议先从历史台账选择一次运行，或继续粘贴 registry-run JSON 做兼容导入。';
    }

    if (displaySource.value === 'imported') {
      if (currentRun.value.summary.failed > 0) {
        return `当前展示兼容导入结果，共 ${currentRun.value.summary.total} 个场景，失败 ${currentRun.value.summary.failed} 个，请优先处理 blocker。`;
      }
      return `当前展示兼容导入结果，共 ${currentRun.value.summary.total} 个场景，全部通过，可继续做基线回归与证据归档。`;
    }

    if (currentRun.value.summary.failed > 0) {
      return `当前已选运行 ${selectedLedgerRunId.value || currentRun.value.runId || '--'} 共 ${currentRun.value.summary.total} 个场景，失败 ${currentRun.value.summary.failed} 个，请优先处理 blocker。`;
    }

    return `当前已选运行 ${selectedLedgerRunId.value || currentRun.value.runId || '--'} 共 ${currentRun.value.summary.total} 个场景，全部通过，可继续做基线回归与证据归档。`;
  });

  const failedScenarioDetails = computed(() =>
    currentRun.value
      ? currentRun.value.failedResults.map((result) => {
          const registryScenario = registryScenarios.value.find((item) => item.id === result.scenarioId);

          return {
            scenarioId: result.scenarioId,
            title: registryScenario?.title || result.scenarioId,
            runnerType: registryScenario?.runnerType || result.runnerType || 'unknown',
            blocking: registryScenario?.blocking || result.blocking,
            docRef: registryScenario?.docRef || '未映射文档章节',
            summary: result.summary || '无摘要'
          };
        })
      : []
  );

  const summaryBody = computed(() => {
    if (currentRunErrorMessage.value) {
      return {
        status: 'error',
        message: currentRunErrorMessage.value
      };
    }

    return currentRun.value
      ? currentRun.value
      : {
          status: 'waiting',
          message: '尚未选中运行结果'
        };
  });

  return {
    suggestions,
    pagination,
    ledgerFilters,
    ledgerRuns,
    ledgerLoading,
    ledgerErrorMessage,
    lastLedgerReloadedAt,
    selectedLedgerRunId,
    importedRun,
    displaySource,
    currentRun,
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
  };
}
