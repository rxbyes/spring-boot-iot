import { computed, ref } from 'vue';
import {
  getAutomationResultDetail,
  getAutomationResultEvidenceContent,
  listAutomationResultEvidence,
  listRecentAutomationResults
} from '@/api/automationResults';
import { ElMessage } from '@/utils/message';
import type {
  AutomationResultEvidenceContent,
  AutomationResultEvidenceItem,
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
  const evidenceItems = ref<AutomationResultEvidenceItem[]>([]);
  const evidenceLoading = ref(false);
  const evidenceErrorMessage = ref('');
  const selectedEvidencePath = ref('');
  const evidencePreview = ref<AutomationResultEvidenceContent | null>(null);
  const evidencePreviewLoading = ref(false);
  const evidencePreviewErrorMessage = ref('');

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

  async function fetchEvidenceItems(runId: string, defaultPath = '') {
    evidenceLoading.value = true;
    evidenceErrorMessage.value = '';
    try {
      const response = await listAutomationResultEvidence(runId);
      evidenceItems.value = Array.isArray(response.data) ? response.data : [];

      const autoSelectPath =
        (defaultPath && evidenceItems.value.find((item) => item.path === defaultPath)?.path) ||
        evidenceItems.value[0]?.path ||
        '';

      if (autoSelectPath) {
        await selectEvidence(runId, autoSelectPath);
      } else {
        clearEvidencePreview();
      }
    } catch {
      evidenceItems.value = [];
      clearEvidencePreview();
      evidenceErrorMessage.value = '证据清单加载失败，请检查后台结果接口或日志目录。';
    } finally {
      evidenceLoading.value = false;
    }
  }

  async function selectEvidence(runId: string, path: string) {
    if (!runId || !path) {
      clearEvidencePreview();
      return;
    }

    selectedEvidencePath.value = path;
    evidencePreviewLoading.value = true;
    evidencePreviewErrorMessage.value = '';
    try {
      const response = await getAutomationResultEvidenceContent(runId, path);
      evidencePreview.value = response.data || null;
    } catch {
      evidencePreview.value = null;
      evidencePreviewErrorMessage.value = '证据原文加载失败，请检查日志目录中的原始文件。';
    } finally {
      evidencePreviewLoading.value = false;
    }
  }

  async function selectRecentRun(runId: string) {
    try {
      const response = await getAutomationResultDetail(runId);
      importedRun.value = parseRegistryRunSummary(response.data);
      selectedRecentRunId.value = runId;
      await fetchEvidenceItems(runId, response.data?.reportPath || '');
      ElMessage.success('已载入最近运行结果');
    } catch {
      ElMessage.error('载入最近运行结果失败');
    }
  }

  function importRegistryRunSummary(rawText: string) {
    try {
      importedRun.value = parseRegistryRunSummaryText(rawText);
      selectedRecentRunId.value = '';
      clearEvidenceState();
      ElMessage.success('统一运行汇总已导入');
    } catch {
      ElMessage.error('导入失败，请检查统一运行汇总 JSON');
    }
  }

  function clearImportedRun() {
    importedRun.value = null;
    selectedRecentRunId.value = '';
    clearEvidenceState();
    ElMessage.success('已清空导入结果');
  }

  function clearEvidenceState() {
    evidenceItems.value = [];
    evidenceLoading.value = false;
    evidenceErrorMessage.value = '';
    clearEvidencePreview();
  }

  function clearEvidencePreview() {
    selectedEvidencePath.value = '';
    evidencePreview.value = null;
    evidencePreviewLoading.value = false;
    evidencePreviewErrorMessage.value = '';
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
    evidenceItems,
    evidenceLoading,
    evidenceErrorMessage,
    selectedEvidencePath,
    evidencePreview,
    evidencePreviewLoading,
    evidencePreviewErrorMessage,
    fetchRecentRuns,
    fetchEvidenceItems,
    selectRecentRun,
    selectEvidence,
    importRegistryRunSummary,
    clearImportedRun
  };
}
