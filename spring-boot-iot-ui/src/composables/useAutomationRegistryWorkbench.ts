import { computed, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import type { ParsedAcceptanceRegistryRunSummary } from '../types/automation';
import {
  buildRegistrySummary,
  loadAcceptanceRegistryDocument,
  parseRegistryRunSummaryText
} from '../utils/automationRegistry';

export function useAutomationRegistryWorkbench() {
  const registryDocument = loadAcceptanceRegistryDocument();
  const importedRun = ref<ParsedAcceptanceRegistryRunSummary | null>(null);

  const registryScenarios = computed(() => registryDocument.scenarios);
  const registrySummary = computed(() => buildRegistrySummary(registryScenarios.value));

  function importRegistryRunSummary(rawText: string) {
    try {
      importedRun.value = parseRegistryRunSummaryText(rawText);
      ElMessage.success('统一运行汇总已导入');
    } catch {
      ElMessage.error('导入失败，请检查统一运行汇总 JSON');
    }
  }

  function clearImportedRun() {
    importedRun.value = null;
    ElMessage.success('已清空导入结果');
  }

  return {
    registryDocument,
    registryScenarios,
    registrySummary,
    importedRun,
    importRegistryRunSummary,
    clearImportedRun
  };
}
