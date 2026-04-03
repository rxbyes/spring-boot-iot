import { computed } from 'vue';

import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';

export function useAutomationPlanComposer() {
  const planBuilder = useAutomationPlanBuilder();

  const warningSuggestions = computed(() =>
    planBuilder.suggestions.value.filter((item) => item.level === 'warning')
  );

  const planSummary = computed(() => ({
    scenarioCount: planBuilder.plan.value.scenarios.length,
    scenarioScopes: planBuilder.plan.value.target.scenarioScopes,
    failScopes: planBuilder.plan.value.target.failScopes,
    outputPrefix: planBuilder.plan.value.target.outputPrefix
  }));

  return {
    plan: planBuilder.plan,
    scopeOptions: planBuilder.scopeOptions,
    locatorTypeOptions: planBuilder.locatorTypeOptions,
    stepTypeOptions: planBuilder.stepTypeOptions,
    showImportDialog: planBuilder.showImportDialog,
    scenarioPreviews: planBuilder.scenarioPreviews,
    suggestions: planBuilder.suggestions,
    warningSuggestions,
    planSummary,
    copyScenario: planBuilder.copyScenario,
    removeScenario: planBuilder.removeScenario,
    moveScenario: planBuilder.moveScenario,
    addInitialApi: planBuilder.addInitialApi,
    addStep: planBuilder.addStep,
    addCapture: planBuilder.addCapture,
    handleStepTypeChange: planBuilder.handleStepTypeChange,
    handleScreenshotTargetChange: planBuilder.handleScreenshotTargetChange,
    moveStep: planBuilder.moveStep,
    downloadPlan: planBuilder.downloadPlan,
    resetPlan: planBuilder.resetPlan,
    applyImport: planBuilder.applyImport
  };
}
