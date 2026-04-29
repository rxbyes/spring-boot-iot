import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  getAutomationResultDetail,
  getAutomationResultEvidenceContent,
  listAutomationResultFacets,
  listAutomationResultEvidence,
  pageAutomationResults,
  refreshAutomationResultIndex
} from '@/api/automationResults';
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request';
import { useServerPagination } from '@/composables/useServerPagination';
import { ElMessage } from '@/utils/message';
import type {
  AutomationResultEvidenceContent,
  AutomationResultEvidenceItem,
  AutomationResultArchiveFacets,
  AutomationResultLedgerFilters,
  AutomationResultRunSummary,
  ParsedAcceptanceRegistryRunSummary
} from '../types/automation';
import {
  buildRegistrySummary,
  loadAcceptanceRegistryDocument,
  parseRegistryRunSummary,
  parseRegistryRunSummaryText
} from '../utils/automationRegistry';

type RegistryRunDisplaySource = 'backend' | 'imported';

function formatLedgerReloadedAt() {
  return new Date().toLocaleString('zh-CN', { hour12: false });
}

function normalizeFilterValue(value?: string | null) {
  if (value === undefined || value === null) {
    return undefined;
  }
  const normalized = String(value).trim();
  return normalized || undefined;
}

function createLedgerFilters(): AutomationResultLedgerFilters {
  return {
    keyword: '',
    status: '',
    runnerType: '',
    packageCode: '',
    environmentCode: '',
    dateRange: []
  };
}

function createEmptyLedgerFacetOptions(): AutomationResultArchiveFacets {
  return {
    statuses: [],
    runnerTypes: [],
    packageCodes: [],
    environmentCodes: []
  };
}

export function useAutomationRegistryWorkbench() {
  const route = useRoute();
  const router = useRouter();
  const registryDocument = loadAcceptanceRegistryDocument();
  const importedRun = ref<ParsedAcceptanceRegistryRunSummary | null>(null);
  const displaySource = ref<RegistryRunDisplaySource>('backend');

  const ledgerFilters = reactive(createLedgerFilters());
  const { pagination, applyPageResult, resetPage, setPageNum, setPageSize, resetTotal } =
    useServerPagination();
  const ledgerRuns = ref<AutomationResultRunSummary[]>([]);
  const ledgerLoading = ref(false);
  const ledgerErrorMessage = ref('');
  const lastLedgerReloadedAt = ref('');
  const ledgerFacetOptions = ref<AutomationResultArchiveFacets>(createEmptyLedgerFacetOptions());
  const ledgerFacetLoaded = ref(false);
  const refreshArchiveIndexLoading = ref(false);

  const selectedLedgerRunId = ref('');
  const selectedLedgerRunDetail = ref<ParsedAcceptanceRegistryRunSummary | null>(null);
  const selectedLedgerRunErrorMessage = ref('');

  const evidenceItems = ref<AutomationResultEvidenceItem[]>([]);
  const evidenceLoading = ref(false);
  const evidenceErrorMessage = ref('');
  const selectedEvidencePath = ref('');
  const evidencePreview = ref<AutomationResultEvidenceContent | null>(null);
  const evidencePreviewLoading = ref(false);
  const evidencePreviewErrorMessage = ref('');

  const registryScenarios = computed(() => registryDocument.scenarios);
  const registrySummary = computed(() => buildRegistrySummary(registryScenarios.value));
  const currentRun = computed(() =>
    displaySource.value === 'imported' && importedRun.value ? importedRun.value : selectedLedgerRunDetail.value
  );
  const currentRunErrorMessage = computed(() =>
    displaySource.value === 'backend' ? selectedLedgerRunErrorMessage.value : ''
  );
  const activeEvidenceRunId = computed(() =>
    displaySource.value === 'backend' ? selectedLedgerRunId.value : ''
  );
  const visibleEvidenceItems = computed(() =>
    activeEvidenceRunId.value ? evidenceItems.value : []
  );
  const visibleSelectedEvidencePath = computed(() =>
    activeEvidenceRunId.value ? selectedEvidencePath.value : ''
  );
  const visibleEvidencePreview = computed(() =>
    activeEvidenceRunId.value ? evidencePreview.value : null
  );
  const visibleEvidenceLoading = computed(() =>
    activeEvidenceRunId.value ? evidenceLoading.value : false
  );
  const visibleEvidenceErrorMessage = computed(() =>
    activeEvidenceRunId.value ? evidenceErrorMessage.value : ''
  );
  const visibleEvidencePreviewLoading = computed(() =>
    activeEvidenceRunId.value ? evidencePreviewLoading.value : false
  );
  const visibleEvidencePreviewErrorMessage = computed(() =>
    activeEvidenceRunId.value ? evidencePreviewErrorMessage.value : ''
  );

  function resetLedgerFilters() {
    ledgerFilters.keyword = '';
    ledgerFilters.status = '';
    ledgerFilters.runnerType = '';
    ledgerFilters.packageCode = '';
    ledgerFilters.environmentCode = '';
    ledgerFilters.dateRange = [];
  }

  function clearEvidencePreview() {
    selectedEvidencePath.value = '';
    evidencePreview.value = null;
    evidencePreviewLoading.value = false;
    evidencePreviewErrorMessage.value = '';
  }

  function clearEvidenceState() {
    evidenceItems.value = [];
    evidenceLoading.value = false;
    evidenceErrorMessage.value = '';
    clearEvidencePreview();
  }

  function clearBackendSelection() {
    selectedLedgerRunId.value = '';
    selectedLedgerRunDetail.value = null;
    selectedLedgerRunErrorMessage.value = '';
    clearEvidenceState();
  }

  function switchToBackendDisplay() {
    displaySource.value = 'backend';
  }

  function buildLedgerQuery() {
    const [dateFrom, dateTo] = Array.isArray(ledgerFilters.dateRange) ? ledgerFilters.dateRange : [];

    return {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: normalizeFilterValue(ledgerFilters.keyword),
      status: normalizeFilterValue(ledgerFilters.status),
      runnerType: normalizeFilterValue(ledgerFilters.runnerType),
      packageCode: normalizeFilterValue(ledgerFilters.packageCode),
      environmentCode: normalizeFilterValue(ledgerFilters.environmentCode),
      dateFrom: normalizeFilterValue(dateFrom),
      dateTo: normalizeFilterValue(dateTo)
    };
  }

  async function loadLedgerFacets(force = false) {
    if (ledgerFacetLoaded.value && !force) {
      return;
    }
    try {
      const response = await listAutomationResultFacets();
      ledgerFacetOptions.value = response.data || createEmptyLedgerFacetOptions();
      ledgerFacetLoaded.value = true;
    } catch {
      ledgerFacetOptions.value = createEmptyLedgerFacetOptions();
      ledgerFacetLoaded.value = false;
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

  async function syncRunIdQuery(runId: string) {
    const currentRunId = normalizeFilterValue(route.query.runId);
    const nextRunId = normalizeFilterValue(runId);
    if (currentRunId === nextRunId) {
      return;
    }
    const nextQuery = { ...(route.query || {}) } as Record<string, unknown>;
    if (nextRunId) {
      nextQuery.runId = nextRunId;
    } else {
      delete nextQuery.runId;
    }
    await router.replace({
      query: nextQuery
    });
  }

  async function selectLedgerRun(runId: string, options: { silent?: boolean; syncQuery?: boolean } = {}) {
    if (!runId) {
      clearBackendSelection();
      return;
    }

    switchToBackendDisplay();
    selectedLedgerRunId.value = runId;
    selectedLedgerRunErrorMessage.value = '';
    if (options.syncQuery !== false) {
      await syncRunIdQuery(runId);
    }

    try {
      const response = await getAutomationResultDetail(runId);
      selectedLedgerRunDetail.value = parseRegistryRunSummary(response.data);
      await fetchEvidenceItems(runId, response.data?.reportPath || '');
      if (!options.silent) {
        ElMessage.success('已载入历史运行结果');
      }
    } catch (error) {
      selectedLedgerRunDetail.value = null;
      clearEvidenceState();
      selectedLedgerRunErrorMessage.value = '历史运行详情加载失败，请检查后台结果接口或日志目录。';
      if (!options.silent && !isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '载入历史运行结果失败'));
      }
    }
  }

  async function syncSelectedLedgerRun(records: Array<{ runId: string }>) {
    if (!records.length) {
      clearBackendSelection();
      return;
    }

    const hasSelectedRun =
      !!selectedLedgerRunId.value && records.some((item) => item.runId === selectedLedgerRunId.value);

    if (hasSelectedRun) {
      await selectLedgerRun(selectedLedgerRunId.value, { silent: true, syncQuery: false });
      return;
    }

    await selectLedgerRun(records[0].runId, { silent: true, syncQuery: false });
  }

  async function fetchRunLedger() {
    ledgerLoading.value = true;
    ledgerErrorMessage.value = '';
    try {
      await loadLedgerFacets();
      const response = await pageAutomationResults(buildLedgerQuery());
      const pageResult = response.data || undefined;
      ledgerRuns.value = applyPageResult(pageResult);
      lastLedgerReloadedAt.value = formatLedgerReloadedAt();
      await syncSelectedLedgerRun(ledgerRuns.value);
    } catch {
      ledgerRuns.value = [];
      resetTotal();
      ledgerErrorMessage.value = '历史运行台账加载失败，请检查后台结果接口或日志目录。';
    } finally {
      ledgerLoading.value = false;
    }
  }

  async function refreshResultArchiveIndex() {
    refreshArchiveIndexLoading.value = true;
    try {
      await refreshAutomationResultIndex();
      await loadLedgerFacets(true);
      await fetchRunLedger();
      ElMessage.success('结果归档索引已刷新');
    } catch (error) {
      if (!isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '刷新结果归档索引失败'));
      }
    } finally {
      refreshArchiveIndexLoading.value = false;
    }
  }

  async function applyLedgerFilters() {
    resetPage();
    await fetchRunLedger();
  }

  async function resetLedgerAndReload() {
    resetLedgerFilters();
    resetPage();
    await fetchRunLedger();
  }

  async function handleLedgerPageChange(page: number) {
    setPageNum(page);
    await fetchRunLedger();
  }

  async function handleLedgerPageSizeChange(size: number) {
    setPageSize(size);
    await fetchRunLedger();
  }

  function importRegistryRunSummary(rawText: string) {
    try {
      importedRun.value = parseRegistryRunSummaryText(rawText);
      displaySource.value = 'imported';
      ElMessage.success('统一运行汇总已导入');
    } catch {
      ElMessage.error('导入失败，请检查统一运行汇总 JSON');
    }
  }

  function clearImportedRun() {
    importedRun.value = null;
    displaySource.value = 'backend';
    ElMessage.success('已清空导入结果');
  }

  watch(
    () => route.query.runId,
    (runId) => {
      const normalizedRunId = typeof runId === 'string' ? runId.trim() : '';
      if (!normalizedRunId || normalizedRunId === selectedLedgerRunId.value) {
        return;
      }
      void selectLedgerRun(normalizedRunId, { silent: true, syncQuery: false });
    },
    { immediate: true }
  );

  return {
    registryDocument,
    registryScenarios,
    registrySummary,
    pagination,
    ledgerFilters,
    ledgerRuns,
    ledgerLoading,
    ledgerErrorMessage,
    lastLedgerReloadedAt,
    ledgerFacetOptions,
    refreshArchiveIndexLoading,
    selectedLedgerRunId,
    selectedLedgerRunDetail,
    selectedLedgerRunErrorMessage,
    importedRun,
    displaySource,
    currentRun,
    currentRunErrorMessage,
    evidenceItems,
    evidenceLoading,
    evidenceErrorMessage,
    selectedEvidencePath,
    evidencePreview,
    evidencePreviewLoading,
    evidencePreviewErrorMessage,
    activeEvidenceRunId,
    visibleEvidenceItems,
    visibleSelectedEvidencePath,
    visibleEvidencePreview,
    visibleEvidenceLoading,
    visibleEvidenceErrorMessage,
    visibleEvidencePreviewLoading,
    visibleEvidencePreviewErrorMessage,
    fetchRunLedger,
    refreshResultArchiveIndex,
    applyLedgerFilters,
    resetLedgerAndReload,
    handleLedgerPageChange,
    handleLedgerPageSizeChange,
    fetchEvidenceItems,
    selectLedgerRun,
    selectEvidence,
    importRegistryRunSummary,
    clearImportedRun
  };
}
