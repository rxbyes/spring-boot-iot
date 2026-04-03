import { computed } from 'vue';
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';
import { useAutomationRegistryWorkbench } from './useAutomationRegistryWorkbench';

export function useAutomationResultsWorkbench() {
  const { suggestions } = useAutomationPlanBuilder();
  const {
    registryScenarios,
    importedRun,
    recentRuns,
    recentRunsLoading,
    recentRunsErrorMessage,
    selectedRecentRunId,
    evidenceItems,
    evidenceLoading,
    evidenceErrorMessage,
    selectedEvidencePath,
    evidencePreview,
    evidencePreviewLoading,
    evidencePreviewErrorMessage,
    fetchRecentRuns,
    selectRecentRun,
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
      label: '已导入结果',
      value: importedRun.value ? String(importedRun.value.summary.total) : '0',
      badge: { label: 'Run', tone: 'brand' as const }
    },
    {
      label: '失败场景',
      value: importedRun.value ? String(importedRun.value.summary.failed) : '0',
      badge: {
        label: 'Fail',
        tone: importedRun.value && importedRun.value.summary.failed > 0 ? ('danger' as const) : ('success' as const)
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

  const resultTone = computed<'info' | 'error'>(() =>
    importedRun.value && importedRun.value.summary.failed > 0 ? 'error' : 'info'
  );

  const resultMessage = computed(() => {
    if (!importedRun.value) {
      return '尚未载入运行结果，建议先选择最近一次运行，或继续粘贴 registry-run JSON 做兼容导入。';
    }

    if (importedRun.value.summary.failed > 0) {
      return `本次导入共 ${importedRun.value.summary.total} 个场景，失败 ${importedRun.value.summary.failed} 个，请优先处理 blocker。`;
    }

    return `本次导入共 ${importedRun.value.summary.total} 个场景，全部通过，可继续做基线回归与证据归档。`;
  });

  const failedScenarioDetails = computed(() =>
    importedRun.value
      ? importedRun.value.failedResults.map((result) => {
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

  const summaryBody = computed(() =>
    importedRun.value
      ? importedRun.value
      : {
          status: 'waiting',
          message: '尚未导入统一运行汇总'
        }
  );

  return {
    suggestions,
    importedRun,
    recentRuns,
    recentRunsLoading,
    recentRunsErrorMessage,
    selectedRecentRunId,
    evidenceItems,
    evidenceLoading,
    evidenceErrorMessage,
    selectedEvidencePath,
    evidencePreview,
    evidencePreviewLoading,
    evidencePreviewErrorMessage,
    resultsMetrics,
    resultTone,
    resultMessage,
    failedScenarioDetails,
    summaryBody,
    fetchRecentRuns,
    selectRecentRun,
    selectEvidence,
    importRegistryRunSummary,
    clearImportedRun
  };
}
