import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';

export function useAutomationInventoryWorkbench() {
  const planBuilder = useAutomationPlanBuilder();

  return {
    inventoryTableRef: planBuilder.inventoryTableRef,
    inventoryMetrics: planBuilder.inventoryMetrics,
    inventorySourceText: planBuilder.inventorySourceText,
    pageInventory: planBuilder.pageInventory,
    buildInventorySourceLabel: planBuilder.buildInventorySourceLabel,
    buildTemplateLabel: planBuilder.buildTemplateLabel,
    isRouteCovered: planBuilder.isRouteCovered,
    handleInventorySelectionChange: planBuilder.handleInventorySelectionChange,
    refreshPageInventory: planBuilder.refreshPageInventory,
    selectUncoveredPages: planBuilder.selectUncoveredPages,
    generateSelectedInventoryScenarios: planBuilder.generateSelectedInventoryScenarios,
    generateUncoveredInventoryScenarios: planBuilder.generateUncoveredInventoryScenarios,
    openManualPageDialog: planBuilder.openManualPageDialog,
    removeManualPage: planBuilder.removeManualPage,
    showManualPageDialog: planBuilder.showManualPageDialog,
    scopeOptions: planBuilder.scopeOptions,
    inventoryTemplateOptions: planBuilder.inventoryTemplateOptions,
    saveManualPage: planBuilder.saveManualPage
  };
}
