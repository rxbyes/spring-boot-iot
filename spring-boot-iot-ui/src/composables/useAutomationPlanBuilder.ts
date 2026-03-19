import { computed, nextTick, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import { usePermissionStore } from '../stores/permission';
import type {
  AutomationPageInventoryItem,
  AutomationPlanDocument,
  AutomationScenarioConfig,
  AutomationScenarioTemplateType,
  AutomationStep
} from '../types/automation';
import {
  buildAutomationPageInventory,
  buildAutomationCommand,
  buildPageCoverageSummary,
  buildPlanSuggestions,
  buildScenarioPreviews,
  collectScenarioRoutes,
  createScenarioFromInventory,
  createDefaultAutomationPlan,
  createFormSubmitScenario,
  createListDetailScenario,
  createPageSmokeScenario,
  createAutomationId,
  duplicateScenario,
  loadSavedAutomationInventory,
  loadSavedAutomationPlan,
  normalizeAutomationPlan,
  saveAutomationInventory,
  saveAutomationPlan
} from '../utils/automationPlan';

type ScenarioTemplateType = 'pageSmoke' | 'formSubmit' | 'listDetail';
type InventoryTableBridge = {
  clearSelection?: () => void;
  toggleRowSelection?: (row: AutomationPageInventoryItem, selected?: boolean) => void;
};

export function useAutomationPlanBuilder() {
  const scopeOptions = ['delivery', 'baseline', 'regression', 'smoke'];
  const locatorTypeOptions = ['css', 'placeholder', 'role', 'text', 'label', 'testId'];
  const stepTypeOptions = [
    'fill',
    'click',
    'press',
    'setChecked',
    'selectOption',
    'uploadFile',
    'waitVisible',
    'triggerApi',
    'tableRowAction',
    'dialogAction',
    'assertText',
    'assertUrlIncludes',
    'assertScreenshot',
    'sleep'
  ];
  const inventoryTemplateOptions: AutomationScenarioTemplateType[] = [
    'pageSmoke',
    'formSubmit',
    'listDetail',
    'login'
  ];

  const permissionStore = usePermissionStore();

  const plan = ref<AutomationPlanDocument>(normalizeAutomationPlan(loadSavedAutomationPlan()));
  const inventoryTableRef = ref<InventoryTableBridge | null>(null);
  const manualPages = ref<AutomationPageInventoryItem[]>(loadSavedAutomationInventory());
  const selectedInventoryRows = ref<AutomationPageInventoryItem[]>([]);
  const showImportDialog = ref(false);
  const showManualPageDialog = ref(false);

  const scenarioPreviews = computed(() => buildScenarioPreviews(plan.value));
  const suggestions = computed(() => buildPlanSuggestions(plan.value));
  const coveredRouteSet = computed(() => new Set(collectScenarioRoutes(plan.value)));
  const pageInventory = computed(() =>
    buildAutomationPageInventory({
      menus: permissionStore.menus || [],
      manualPages: manualPages.value,
      includeStaticFallback: !permissionStore.isLoggedIn || (permissionStore.menus || []).length === 0
    })
  );
  const coverageSummary = computed(() => buildPageCoverageSummary(plan.value, pageInventory.value));
  const uncoveredInventoryRows = computed(() =>
    pageInventory.value.filter((item) => !coveredRouteSet.value.has(item.route))
  );
  const inventorySourceText = computed(() =>
    permissionStore.isLoggedIn && (permissionStore.menus || []).length > 0
      ? '已授权菜单 + 自定义页面'
      : '静态路由种子 + 自定义页面'
  );
  const totalSteps = computed(() =>
    plan.value.scenarios.reduce((sum, scenario) => sum + scenario.steps.length, 0)
  );
  const totalApiChecks = computed(() =>
    scenarioPreviews.value.reduce((sum, scenario) => sum + scenario.apiCount, 0)
  );
  const assertedScenarios = computed(() =>
    scenarioPreviews.value.filter((scenario) => scenario.hasAssertion).length
  );
  const planMetrics = computed(() => [
    {
      label: '场景数',
      value: String(scenarioPreviews.value.length),
      badge: { label: 'Scenario', tone: 'brand' as const }
    },
    {
      label: '步骤数',
      value: String(totalSteps.value),
      badge: { label: 'Step', tone: 'success' as const }
    },
    {
      label: '接口检查数',
      value: String(totalApiChecks.value),
      badge: { label: 'API', tone: 'warning' as const }
    },
    {
      label: '断言场景数',
      value: String(assertedScenarios.value),
      badge: { label: 'Assert', tone: 'danger' as const }
    }
  ]);
  const inventoryMetrics = computed(() => [
    {
      label: '盘点页面数',
      value: String(coverageSummary.value.totalPages),
      badge: { label: 'Page', tone: 'brand' as const }
    },
    {
      label: '已覆盖页面',
      value: String(coverageSummary.value.coveredPages),
      badge: { label: 'Covered', tone: 'success' as const }
    },
    {
      label: '待补齐页面',
      value: String(coverageSummary.value.uncoveredPages),
      badge: { label: 'Gap', tone: 'warning' as const }
    },
    {
      label: '当前勾选数',
      value: String(selectedInventoryRows.value.length),
      badge: { label: 'Selected', tone: 'danger' as const }
    }
  ]);
  const commandPreview = computed(() => buildAutomationCommand('config/automation/sample-web-smoke-plan.json'));

  function ensureStepShape(step: AutomationStep): void {
    if (!step.locator && stepUsesLocator(step)) {
      step.locator = {
        type: 'css',
        value: ''
      };
    }
    if ((step.type === 'triggerApi' || step.type === 'tableRowAction') && !step.action) {
      step.action = {
        type: 'click',
        locator: {
          type: 'css',
          value: ''
        }
      };
    }
    if (step.type === 'dialogAction') {
      step.dialogAction = step.dialogAction || 'waitVisible';
      if (step.dialogAction === 'custom' && !step.action) {
        step.action = {
          type: 'click',
          locator: {
            type: 'css',
            value: ''
          }
        };
      }
    }
    if (step.type === 'assertScreenshot') {
      step.screenshotTarget = step.screenshotTarget || 'page';
      step.threshold = step.threshold ?? 0;
      step.fullPage = step.fullPage ?? true;
      if (step.screenshotTarget === 'locator' && !step.locator) {
        step.locator = {
          type: 'css',
          value: ''
        };
      }
    }
    if (!stepSupportsCaptures(step.type)) {
      step.captures = [];
    }
  }

  plan.value.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));

  watch(
    plan,
    (value) => {
      const normalized = normalizeAutomationPlan(value);
      normalized.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
      saveAutomationPlan(normalized);
    },
    {
      deep: true
    }
  );

  watch(
    manualPages,
    (value) => {
      saveAutomationInventory(value);
    },
    {
      deep: true
    }
  );

  function stepUsesLocator(step: AutomationStep | string): boolean {
    const stepType = typeof step === 'string' ? step : step.type;
    if (stepType === 'assertScreenshot') {
      return typeof step === 'string' ? true : step.screenshotTarget !== 'page';
    }
    return !['sleep', 'assertUrlIncludes', 'dialogAction'].includes(stepType);
  }

  function stepSupportsCaptures(stepType: string): boolean {
    return ['triggerApi', 'tableRowAction', 'dialogAction'].includes(stepType);
  }

  function buildTemplateLabel(template: AutomationScenarioTemplateType): string {
    switch (template) {
      case 'formSubmit':
        return '表单提交';
      case 'listDetail':
        return '列表详情';
      case 'login':
        return '登录前置';
      default:
        return '页面冒烟';
    }
  }

  function buildInventorySourceLabel(source: AutomationPageInventoryItem['source']): string {
    switch (source) {
      case 'menu':
        return '授权菜单';
      case 'manual':
        return '手工补充';
      default:
        return '静态种子';
    }
  }

  function isRouteCovered(route: string): boolean {
    return coveredRouteSet.value.has(route);
  }

  function handleInventorySelectionChange(rows: AutomationPageInventoryItem[]) {
    selectedInventoryRows.value = rows;
  }

  async function refreshPageInventory() {
    if (permissionStore.isLoggedIn) {
      try {
        await permissionStore.ensureInitialized(true);
      } catch {
        ElMessage.warning('菜单权限刷新失败，已保留当前本地页面盘点结果');
      }
    }
    await nextTick();
    ElMessage.success(`页面盘点已刷新，共识别 ${pageInventory.value.length} 个页面`);
  }

  async function selectUncoveredPages() {
    await nextTick();
    inventoryTableRef.value?.clearSelection?.();
    uncoveredInventoryRows.value.forEach((item) => inventoryTableRef.value?.toggleRowSelection?.(item, true));

    if (uncoveredInventoryRows.value.length === 0) {
      ElMessage.success('当前页面盘点已全部覆盖');
      return;
    }
    ElMessage.success(`已勾选 ${uncoveredInventoryRows.value.length} 个待补齐页面`);
  }

  function appendInventoryScenarios(items: AutomationPageInventoryItem[]) {
    if (items.length === 0) {
      ElMessage.warning('请先选择要生成脚手架的页面');
      return;
    }

    const routeSet = new Set(coveredRouteSet.value);
    let appendedCount = 0;
    let skippedCount = 0;

    items.forEach((item) => {
      if (routeSet.has(item.route)) {
        skippedCount += 1;
        return;
      }

      const scenario = createScenarioFromInventory(item);
      scenario.steps.forEach(ensureStepShape);
      plan.value.scenarios.push(scenario);
      routeSet.add(item.route);
      appendedCount += 1;
    });

    if (appendedCount === 0) {
      ElMessage.success('所选页面已全部在当前计划中覆盖');
      return;
    }

    if (skippedCount > 0) {
      ElMessage.success(`已新增 ${appendedCount} 个页面脚手架，跳过 ${skippedCount} 个已覆盖页面`);
      return;
    }

    ElMessage.success(`已新增 ${appendedCount} 个页面脚手架`);
  }

  function generateSelectedInventoryScenarios() {
    appendInventoryScenarios(selectedInventoryRows.value);
  }

  function generateUncoveredInventoryScenarios() {
    appendInventoryScenarios(uncoveredInventoryRows.value);
  }

  function openManualPageDialog() {
    showManualPageDialog.value = true;
  }

  function saveManualPage(nextItem: AutomationPageInventoryItem) {
    const existingIndex = manualPages.value.findIndex((item) => item.route === nextItem.route);

    if (existingIndex >= 0) {
      manualPages.value.splice(existingIndex, 1, nextItem);
      ElMessage.success('已更新自定义页面盘点项');
    } else {
      manualPages.value.push(nextItem);
      ElMessage.success('已新增自定义页面盘点项');
    }

    showManualPageDialog.value = false;
  }

  function removeManualPage(id: string) {
    const index = manualPages.value.findIndex((item) => item.id === id);
    if (index < 0) {
      return;
    }
    manualPages.value.splice(index, 1);
    ElMessage.success('已移除自定义页面盘点项');
  }

  function addScenario(type: ScenarioTemplateType) {
    const scenario =
      type === 'formSubmit'
        ? createFormSubmitScenario()
        : type === 'listDetail'
          ? createListDetailScenario()
          : createPageSmokeScenario();
    scenario.steps.forEach(ensureStepShape);
    plan.value.scenarios.push(scenario);
  }

  function copyScenario(index: number) {
    const scenario = plan.value.scenarios[index];
    if (!scenario) {
      return;
    }
    const duplicated = duplicateScenario(scenario);
    duplicated.steps.forEach(ensureStepShape);
    plan.value.scenarios.splice(index + 1, 0, duplicated);
  }

  function removeScenario(index: number) {
    plan.value.scenarios.splice(index, 1);
  }

  function moveScenario(index: number, offset: number) {
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= plan.value.scenarios.length) {
      return;
    }
    const [item] = plan.value.scenarios.splice(index, 1);
    plan.value.scenarios.splice(targetIndex, 0, item);
  }

  function addInitialApi(scenario: AutomationScenarioConfig) {
    scenario.initialApis.push({
      label: '页面接口',
      matcher: '/api/example/list',
      optional: true,
      timeout: 15000
    });
  }

  function addStep(scenario: AutomationScenarioConfig) {
    const step: AutomationStep = {
      id: createAutomationId('step'),
      label: '新增步骤',
      type: 'waitVisible',
      locator: {
        type: 'css',
        value: 'body'
      },
      optional: false,
      timeout: 15000
    };
    ensureStepShape(step);
    scenario.steps.push(step);
  }

  function addCapture(step: AutomationStep) {
    if (!step.captures) {
      step.captures = [];
    }
    step.captures.push({
      variable: '',
      path: ''
    });
  }

  function handleStepTypeChange(step: AutomationStep) {
    ensureStepShape(step);
  }

  function handleScreenshotTargetChange(step: AutomationStep) {
    ensureStepShape(step);
  }

  function moveStep(scenario: AutomationScenarioConfig, index: number, offset: number) {
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= scenario.steps.length) {
      return;
    }
    const [item] = scenario.steps.splice(index, 1);
    scenario.steps.splice(targetIndex, 0, item);
  }

  async function copyCommand() {
    try {
      await navigator.clipboard.writeText(commandPreview.value);
      ElMessage.success('执行命令已复制');
    } catch {
      ElMessage.warning('当前环境不支持剪贴板复制，请手动复制命令');
    }
  }

  function downloadTextFile(fileName: string, content: string) {
    const blob = new Blob([content], { type: 'application/json;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  function downloadPlan() {
    const normalized = normalizeAutomationPlan(plan.value);
    downloadTextFile('automation-plan.json', JSON.stringify(normalized, null, 2));
    ElMessage.success('自动化计划已导出');
  }

  function resetPlan() {
    plan.value = createDefaultAutomationPlan();
    plan.value.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
    ElMessage.success('已恢复默认计划模板');
  }

  function applyImport(importText: string) {
    try {
      const nextPlan = normalizeAutomationPlan(JSON.parse(importText) as AutomationPlanDocument);
      nextPlan.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
      plan.value = nextPlan;
      showImportDialog.value = false;
      ElMessage.success('自动化计划已导入');
    } catch {
      ElMessage.error('导入失败，请检查 JSON 格式是否正确');
    }
  }

  return {
    scopeOptions,
    locatorTypeOptions,
    stepTypeOptions,
    inventoryTemplateOptions,
    plan,
    inventoryTableRef,
    showImportDialog,
    showManualPageDialog,
    scenarioPreviews,
    suggestions,
    pageInventory,
    inventorySourceText,
    planMetrics,
    inventoryMetrics,
    commandPreview,
    buildTemplateLabel,
    buildInventorySourceLabel,
    isRouteCovered,
    handleInventorySelectionChange,
    refreshPageInventory,
    selectUncoveredPages,
    generateSelectedInventoryScenarios,
    generateUncoveredInventoryScenarios,
    openManualPageDialog,
    saveManualPage,
    removeManualPage,
    addScenario,
    copyScenario,
    removeScenario,
    moveScenario,
    addInitialApi,
    addStep,
    addCapture,
    handleStepTypeChange,
    handleScreenshotTargetChange,
    moveStep,
    copyCommand,
    downloadPlan,
    resetPlan,
    applyImport
  };
}
