import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

function readView(fileName: string) {
  return decodeEscapedUnicode(
    readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
  );
}

function readComponent(relativePath: string) {
  return decodeEscapedUnicode(
    readFileSync(resolve(import.meta.dirname, `../../components/${relativePath}`), 'utf8')
  );
}

function readRouter() {
  return readFileSync(resolve(import.meta.dirname, '../../router/index.ts'), 'utf8');
}

function decodeEscapedUnicode(source: string) {
  return source.replace(/\\u([0-9a-fA-F]{4})/g, (_, code: string) =>
    String.fromCharCode(Number.parseInt(code, 16))
  );
}

describe('automation rd workbench route splits', () => {
  it('keeps quality workbench focused on business acceptance and governance navigation', () => {
    const source = readView('QualityWorkbenchLandingView.vue');

    expect(source).toContain('/business-acceptance');
    expect(source).toContain('/automation-governance');
    expect(source).toContain('quality-workbench-landing__hero');
    expect(source).toContain('quality-workbench-landing__summary-grid');
    expect(source).toContain('quality-workbench-landing__entry-grid');
    expect(source).not.toContain('/rd-workbench');
    expect(source).not.toContain('/automation-execution');
    expect(source).not.toContain('/automation-results');
  });

  it('keeps the business acceptance view free of rd authoring widgets', () => {
    const source = readView('BusinessAcceptanceWorkbenchView.vue');

    expect(source).toContain('<BusinessAcceptancePackagePanel');
    expect(source).toContain('<BusinessAcceptanceRunConfigPanel');
    expect(source).toContain('launchSelectedPackage');
    expect(source).toContain('useBusinessAcceptanceWorkbench');
    expect(source).toContain('哪些模块没过');
    expect(source).toContain('business-acceptance-workbench__hero');
    expect(source).toContain('business-acceptance-workbench__balanced-grid');
    expect(source).toContain('business-acceptance-workbench__aside');
    expect(source).not.toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationExecutionConfigPanel');
  });

  it('keeps the business acceptance result page focused on pass or fail conclusion and module drill-down', () => {
    const source = readView('BusinessAcceptanceResultView.vue');

    expect(source).toContain('<BusinessAcceptanceResultSummaryPanel');
    expect(source).toContain('<BusinessAcceptanceModuleResultPanel');
    expect(source).toContain('goToAutomationEvidence');
    expect(source).toContain(':show-evidence-action="canOpenAutomationEvidence"');
    expect(source).toContain("hasRoutePermission('/automation-governance')");
    expect(source).not.toContain('<AutomationExecutionConfigPanel');
    expect(source).not.toContain('<AutomationScenarioEditor');
  });

  it('keeps the business acceptance module panel showing lightweight failure diagnosis', () => {
    const source = readComponent('BusinessAcceptanceModuleResultPanel.vue');

    expect(source).toContain('主分类');
    expect(source).toContain('判断理由');
    expect(source).toContain('证据摘要');
  });

  it('keeps the automation governance view focused on assets, execution, and evidence tabs', () => {
    const source = readView('AutomationGovernanceWorkbenchView.vue');

    expect(source).toContain('自动化治理台');
    expect(source).toContain('资产编排');
    expect(source).toContain('执行配置');
    expect(source).toContain('结果证据');
    expect(source).toContain('automation-governance-workbench__hero');
    expect(source).toContain('automation-governance-workbench__tabs');
    expect(source).toContain('<AutomationAssetsWorkspaceSection');
    expect(source).toContain('<AutomationExecutionWorkspaceSection');
    expect(source).toContain('<AutomationEvidenceWorkspaceSection');
  });

  it('keeps asset authoring content in a reusable governance workspace section', () => {
    const source = readComponent('automationGovernance/AutomationAssetsWorkspaceSection.vue');

    expect(source).toContain('<AutomationInventoryWorkspaceSection');
    expect(source).toContain('<AutomationTemplatesWorkspaceSection');
    expect(source).toContain('<AutomationPlansWorkspaceSection');
    expect(source).toContain('<AutomationHandoffWorkspaceSection');
  });

  it('keeps execution workspace content in a reusable governance section', () => {
    const source = readComponent('automationGovernance/AutomationExecutionWorkspaceSection.vue');

    expect(source).toContain('<AutomationExecutionConfigPanel');
    expect(source).toContain('<AutomationRegistryPanel');
    expect(source).toContain('执行概况');
  });

  it('keeps evidence workspace content in a reusable governance section', () => {
    const source = readComponent('automationGovernance/AutomationEvidenceWorkspaceSection.vue');

    expect(source).toContain('<AutomationRecentRunsPanel');
    expect(source).toContain('<AutomationResultEvidencePanel');
    expect(source).toContain('失败分类分布');
    expect(source).toContain('主分类');
    expect(source).toContain('判断理由');
    expect(source).toContain('证据摘要');
    expect(source).toContain('失败场景明细');
  });

  it('keeps the recent runs panel aligned with archive index filters', () => {
    const source = readComponent('AutomationRecentRunsPanel.vue');

    expect(source).toContain('placeholder="验收包"');
    expect(source).toContain('placeholder="环境"');
    expect(source).toContain('刷新索引');
  });

  it('registers automation-governance route and retires the old rd/execution/result routes', () => {
    const source = readRouter();

    expect(source).toContain("path: '/quality-workbench'");
    expect(source).toContain("path: '/business-acceptance'");
    expect(source).toContain("path: '/business-acceptance/results/:runId'");
    expect(source).toContain("path: '/automation-governance'");
    expect(source).not.toContain("path: '/rd-workbench'");
    expect(source).not.toContain("path: '/rd-automation-inventory'");
    expect(source).not.toContain("path: '/rd-automation-templates'");
    expect(source).not.toContain("path: '/rd-automation-plans'");
    expect(source).not.toContain("path: '/rd-automation-handoff'");
    expect(source).not.toContain("path: '/automation-assets'");
    expect(source).not.toContain("path: '/automation-execution'");
    expect(source).not.toContain("path: '/automation-results'");
    expect(source).not.toContain("path: '/automation-test'");
    expect(source).not.toContain("component: () => import('../views/RdWorkbenchLandingView.vue')");
    expect(source).not.toContain("component: () => import('../views/AutomationExecutionView.vue')");
    expect(source).not.toContain("component: () => import('../views/AutomationResultsView.vue')");
  });
});
