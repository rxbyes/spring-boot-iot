import { computed, ref } from 'vue';
import { getAutomationResultDetail, listRecentAutomationResults } from '@/api/automationResults';
import { ElMessage } from '@/utils/message';
import type {
  AutomationResultRecentRun,
  ParsedAcceptanceRegistryRunSummary
} from '../types/automation';
import {
  buildRegistrySummary,
  loadAcceptanceRegistryDocument,
  parseRegistryRunSummary,
  parseRegistryRunSummaryText
} from '../utils/automationRegistry';

export function useAutomationRegistryWorkbench() {
  const registryDocument = loadAcceptanceRegistryDocument();
  const importedRun = ref<ParsedAcceptanceRegistryRunSummary | null>(null);
  const recentRuns = ref<AutomationResultRecentRun[]>([]);
  const recentRunsLoading = ref(false);
  const recentRunsErrorMessage = ref('');
  const selectedRecentRunId = ref('');

  const registryScenarios = computed(() => registryDocument.scenarios);
  const registrySummary = computed(() => buildRegistrySummary(registryScenarios.value));

  async function fetchRecentRuns(limit = 10) {
    recentRunsLoading.value = true;
    recentRunsErrorMessage.value = '';
    try {
      const response = await listRecentAutomationResults(limit);
      recentRuns.value = Array.isArray(response.data) ? response.data : [];
    } catch {
      recentRuns.value = [];
      recentRunsErrorMessage.value = '最近运行结果加载失败，请检查后台结果接口或日志目录。';
    } finally {
      recentRunsLoading.value = false;
    }
  }

  async function selectRecentRun(runId: string) {
    try {
      const response = await getAutomationResultDetail(runId);
      importedRun.value = parseRegistryRunSummary(response.data);
      selectedRecentRunId.value = runId;
      ElMessage.success('已载入最近运行结果');
    } catch {
      ElMessage.error('载入最近运行结果失败');
    }
  }

  function importRegistryRunSummary(rawText: string) {
    try {
      importedRun.value = parseRegistryRunSummaryText(rawText);
      selectedRecentRunId.value = '';
      ElMessage.success('统一运行汇总已导入');
    } catch {
      ElMessage.error('导入失败，请检查统一运行汇总 JSON');
    }
  }

  function clearImportedRun() {
    importedRun.value = null;
    selectedRecentRunId.value = '';
    ElMessage.success('已清空导入结果');
  }

  return {
    registryDocument,
    registryScenarios,
    registrySummary,
    importedRun,
    recentRuns,
    recentRunsLoading,
    recentRunsErrorMessage,
    selectedRecentRunId,
    fetchRecentRuns,
    selectRecentRun,
    importRegistryRunSummary,
    clearImportedRun
  };
}
