import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  getBusinessAcceptanceResult,
  getBusinessAcceptanceRunStatus,
  launchBusinessAcceptanceRun,
  listBusinessAcceptanceAccountTemplates,
  listBusinessAcceptancePackages
} from '@/api/businessAcceptance';
import { buildAutomationGovernanceEvidencePath } from '@/utils/automationGovernance';
import type {
  BusinessAcceptanceAccountTemplate,
  BusinessAcceptancePackage,
  BusinessAcceptanceResult,
  BusinessAcceptanceRunStatus
} from '@/types/businessAcceptance';

type PollHandle = unknown;

interface UseBusinessAcceptanceWorkbenchOptions {
  pollDelayMs?: number;
  schedulePoll?: (callback: () => void | Promise<void>, delay: number) => PollHandle;
  clearScheduledPoll?: (handle: PollHandle) => void;
}

function normalizeText(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function pickRouteQueryString(value: unknown) {
  if (Array.isArray(value)) {
    return normalizeText(value[0]);
  }
  return normalizeText(value);
}

function defaultSchedulePoll(callback: () => void | Promise<void>, delay: number) {
  return window.setTimeout(() => {
    void callback();
  }, delay);
}

function defaultClearScheduledPoll(handle: PollHandle) {
  window.clearTimeout(handle as number);
}

export function useBusinessAcceptanceWorkbench(options: UseBusinessAcceptanceWorkbenchOptions = {}) {
  const route = useRoute();
  const router = useRouter();
  const pollDelayMs = options.pollDelayMs ?? 1200;
  const schedulePoll = options.schedulePoll ?? defaultSchedulePoll;
  const clearScheduledPoll = options.clearScheduledPoll ?? defaultClearScheduledPoll;

  const packages = ref<BusinessAcceptancePackage[]>([]);
  const accountTemplates = ref<BusinessAcceptanceAccountTemplate[]>([]);
  const loadingInitial = ref(false);
  const initialErrorMessage = ref('');

  const selectedPackageCode = ref('');
  const selectedEnvironment = ref('');
  const selectedAccountTemplate = ref('');
  const selectedModuleCodes = ref<string[]>([]);

  const launching = ref(false);
  const launchErrorMessage = ref('');
  const runStatus = ref<BusinessAcceptanceRunStatus | null>(null);
  const pollHandle = ref<PollHandle | null>(null);

  const result = ref<BusinessAcceptanceResult | null>(null);
  const resultLoading = ref(false);
  const resultErrorMessage = ref('');
  const activeModuleCode = ref('');

  const selectedPackage = computed(
    () => packages.value.find((item) => item.packageCode === selectedPackageCode.value) || null
  );
  const selectedPackageModules = computed(() => selectedPackage.value?.modules || []);
  const selectedLatestResult = computed(() => selectedPackage.value?.latestResult || null);
  const environmentOptions = computed(() => selectedPackage.value?.supportedEnvironments || []);
  const availableAccountTemplates = computed(() =>
    accountTemplates.value.filter((item) => {
      if (!selectedEnvironment.value) {
        return true;
      }
      return item.supportedEnvironments.includes(selectedEnvironment.value);
    })
  );
  const activeModule = computed(
    () => result.value?.modules.find((item) => item.moduleCode === activeModuleCode.value) || null
  );

  function clearPendingPoll() {
    if (pollHandle.value == null) {
      return;
    }
    clearScheduledPoll(pollHandle.value);
    pollHandle.value = null;
  }

  function syncSelectionsForPackage(pkg: BusinessAcceptancePackage | null) {
    if (!pkg) {
      selectedEnvironment.value = '';
      selectedAccountTemplate.value = '';
      selectedModuleCodes.value = [];
      return;
    }

    if (!pkg.supportedEnvironments.includes(selectedEnvironment.value)) {
      selectedEnvironment.value = pkg.supportedEnvironments[0] || '';
    }

    const availableTemplateCodes = availableAccountTemplates.value.map((item) => item.templateCode);
    if (!availableTemplateCodes.includes(selectedAccountTemplate.value)) {
      selectedAccountTemplate.value =
        availableTemplateCodes.find((code) => code === pkg.defaultAccountTemplate) ||
        availableTemplateCodes[0] ||
        '';
    }

    const packageModuleCodes = pkg.modules.map((item) => item.moduleCode);
    const stillSelected = selectedModuleCodes.value.filter((code) => packageModuleCodes.includes(code));
    selectedModuleCodes.value = stillSelected.length > 0 ? stillSelected : packageModuleCodes;
  }

  watch(
    [selectedPackageCode, packages, accountTemplates],
    () => {
      syncSelectionsForPackage(selectedPackage.value);
    },
    { immediate: true }
  );

  watch(selectedEnvironment, () => {
    syncSelectionsForPackage(selectedPackage.value);
  });

  async function loadInitialData() {
    loadingInitial.value = true;
    initialErrorMessage.value = '';
    try {
      const [packageResponse, accountTemplateResponse] = await Promise.all([
        listBusinessAcceptancePackages(),
        listBusinessAcceptanceAccountTemplates()
      ]);
      packages.value = Array.isArray(packageResponse.data) ? packageResponse.data : [];
      accountTemplates.value = Array.isArray(accountTemplateResponse.data)
        ? accountTemplateResponse.data
        : [];

      if (!selectedPackageCode.value && packages.value.length > 0) {
        selectedPackageCode.value = packages.value[0].packageCode;
      } else if (selectedPackageCode.value && !selectedPackage.value && packages.value.length > 0) {
        selectedPackageCode.value = packages.value[0].packageCode;
      } else {
        syncSelectionsForPackage(selectedPackage.value);
      }
    } catch {
      packages.value = [];
      accountTemplates.value = [];
      initialErrorMessage.value = '业务验收台初始化失败，请检查业务验收接口。';
    } finally {
      loadingInitial.value = false;
    }
  }

  async function handleTerminalStatus(status: BusinessAcceptanceRunStatus | null) {
    if (!status?.runId) {
      return;
    }
    await router.push({
      path: `/business-acceptance/results/${status.runId}`,
      query: {
        packageCode: selectedPackageCode.value
      }
    });
  }

  async function pollRunStatus(jobId: string) {
    try {
      const response = await getBusinessAcceptanceRunStatus(jobId);
      runStatus.value = response.data || null;
      if (runStatus.value?.status === 'running') {
        pollHandle.value = schedulePoll(() => pollRunStatus(jobId), pollDelayMs);
        return;
      }
      clearPendingPoll();
      await handleTerminalStatus(runStatus.value);
    } catch {
      clearPendingPoll();
      launchErrorMessage.value = '业务验收执行状态获取失败，请稍后重试。';
    }
  }

  async function launchSelectedPackage() {
    if (!selectedPackage.value) {
      launchErrorMessage.value = '请选择验收包。';
      return;
    }
    launching.value = true;
    launchErrorMessage.value = '';
    clearPendingPoll();
    try {
      const response = await launchBusinessAcceptanceRun({
        packageCode: selectedPackage.value.packageCode,
        environmentCode: selectedEnvironment.value,
        accountTemplateCode: selectedAccountTemplate.value,
        moduleCodes: selectedModuleCodes.value
      });
      runStatus.value = response.data || null;
      if (runStatus.value?.status === 'running' && runStatus.value.jobId) {
        pollHandle.value = schedulePoll(() => pollRunStatus(runStatus.value!.jobId), pollDelayMs);
      } else {
        await handleTerminalStatus(runStatus.value);
      }
    } catch {
      runStatus.value = null;
      launchErrorMessage.value = '业务验收启动失败，请检查参数或后端日志。';
    } finally {
      launching.value = false;
    }
  }

  async function loadResult(runId: string, packageCode: string) {
    resultLoading.value = true;
    resultErrorMessage.value = '';
    try {
      const response = await getBusinessAcceptanceResult(runId, packageCode);
      result.value = response.data || null;
      activeModuleCode.value =
        result.value?.modules.find((item) => item.status !== 'passed')?.moduleCode ||
        result.value?.modules[0]?.moduleCode ||
        '';
    } catch {
      result.value = null;
      activeModuleCode.value = '';
      resultErrorMessage.value = '业务验收结果加载失败，请检查 runId 和 packageCode。';
    } finally {
      resultLoading.value = false;
    }
  }

  async function loadResultFromRoute() {
    const runId = normalizeText(route.params.runId);
    const packageCode = pickRouteQueryString(route.query.packageCode);
    if (!runId || !packageCode) {
      result.value = null;
      activeModuleCode.value = '';
      resultErrorMessage.value = '缺少业务验收结果上下文。';
      return;
    }
    await loadResult(runId, packageCode);
  }

  async function goToAutomationEvidence(runId: string) {
    await router.push(buildAutomationGovernanceEvidencePath(runId));
  }

  if (getCurrentInstance()) {
    onBeforeUnmount(() => {
      clearPendingPoll();
    });
  }

  return {
    packages,
    accountTemplates,
    loadingInitial,
    initialErrorMessage,
    selectedPackageCode,
    selectedPackage,
    selectedEnvironment,
    selectedAccountTemplate,
    selectedModuleCodes,
    selectedPackageModules,
    selectedLatestResult,
    environmentOptions,
    availableAccountTemplates,
    launching,
    launchErrorMessage,
    runStatus,
    result,
    resultLoading,
    resultErrorMessage,
    activeModuleCode,
    activeModule,
    loadInitialData,
    launchSelectedPackage,
    loadResult,
    loadResultFromRoute,
    goToAutomationEvidence
  };
}
