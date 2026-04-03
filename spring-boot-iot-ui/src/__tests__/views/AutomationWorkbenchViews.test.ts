import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

function readView(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8');
}

function readRouter() {
  return readFileSync(resolve(import.meta.dirname, '../../router/index.ts'), 'utf8');
}

describe('automation rd workbench route splits', () => {
  it('keeps quality workbench focused on rd and shared-center navigation', () => {
    const source = readView('QualityWorkbenchLandingView.vue');

    expect(source).toContain('/rd-workbench');
    expect(source).toContain('/automation-execution');
    expect(source).toContain('/automation-results');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the rd-workbench landing page focused on four rd authoring modules', () => {
    const source = readView('RdWorkbenchLandingView.vue');

    expect(source).toContain('/rd-automation-inventory');
    expect(source).toContain('/rd-automation-templates');
    expect(source).toContain('/rd-automation-plans');
    expect(source).toContain('/rd-automation-handoff');
    expect(source).not.toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the inventory page focused on page discovery and manual page curation', () => {
    const source = readView('AutomationInventoryView.vue');

    expect(source).toContain('<AutomationPageDiscoveryPanel');
    expect(source).toContain('<AutomationManualPageDrawer');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationRegistryPanel');
  });

  it('keeps the template page focused on template launch actions', () => {
    const source = readView('AutomationTemplatesView.vue');

    expect(source).toContain("addScenario('pageSmoke')");
    expect(source).toContain("addScenario('formSubmit')");
    expect(source).toContain("addScenario('listDetail')");
    expect(source).not.toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the plans page focused on scenario editing and plan import/export', () => {
    const source = readView('AutomationPlansView.vue');

    expect(source).toContain('<AutomationScenarioEditor');
    expect(source).toContain('<AutomationPlanImportDrawer');
    expect(source).toContain('<ResponsePanel');
    expect(source).not.toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the handoff page focused on summary and delivery guidance', () => {
    const source = readView('AutomationHandoffView.vue');

    expect(source).toContain('执行建议');
    expect(source).toContain('交付备注');
    expect(source).toContain('<ResponsePanel');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the legacy automation-assets and automation-test views as rd-workbench wrappers', () => {
    const assetSource = readView('AutomationAssetsView.vue');
    const legacySource = readView('AutomationTestCenterView.vue');

    expect(assetSource).toContain('<RdWorkbenchLandingView');
    expect(legacySource).toContain('<RdWorkbenchLandingView');
    expect(assetSource).not.toContain('<AutomationScenarioEditor');
    expect(legacySource).not.toContain('<AutomationRegistryPanel');
  });

  it('keeps the execution page focused on run configuration and registry visibility', () => {
    const source = readView('AutomationExecutionView.vue');

    expect(source).toContain('<AutomationExecutionConfigPanel');
    expect(source).toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the results page focused on imported run summaries and quality guidance', () => {
    const source = readView('AutomationResultsView.vue');

    expect(source).toContain('<AutomationRecentRunsPanel');
    expect(source).toContain('<AutomationResultImportPanel');
    expect(source).toContain('<AutomationSuggestionPanel');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationExecutionConfigPanel');
  });

  it('registers rd-workbench routes and keeps compatibility routes alive', () => {
    const source = readRouter();

    expect(source).toContain("path: '/quality-workbench'");
    expect(source).toContain("path: '/rd-workbench'");
    expect(source).toContain("path: '/rd-automation-inventory'");
    expect(source).toContain("path: '/rd-automation-templates'");
    expect(source).toContain("path: '/rd-automation-plans'");
    expect(source).toContain("path: '/rd-automation-handoff'");
    expect(source).toContain("path: '/automation-assets'");
    expect(source).toContain("path: '/automation-execution'");
    expect(source).toContain("path: '/automation-results'");
    expect(source).toContain("path: '/automation-test'");
  });
});
