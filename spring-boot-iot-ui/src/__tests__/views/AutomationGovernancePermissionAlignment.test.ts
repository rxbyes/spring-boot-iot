import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

function readComponent(relativePath: string) {
  return readRepoFile(`spring-boot-iot-ui/src/components/${relativePath}`);
}

function readRepoFile(relativePath: string) {
  return decodeEscapedUnicode(
    readFileSync(resolve(import.meta.dirname, '../../../../', relativePath), 'utf8')
  );
}

function decodeEscapedUnicode(source: string) {
  return source.replace(/\\u([0-9a-fA-F]{4})/g, (_, code: string) =>
    String.fromCharCode(Number.parseInt(code, 16))
  );
}

describe('automation governance permission alignment', () => {
  it('uses the consolidated automation-governance namespace across current workspace actions', () => {
    const sources = [
      readComponent('AutomationPageDiscoveryPanel.vue'),
      readComponent('AutomationCaptureEditor.vue'),
      readComponent('AutomationStepEditor.vue'),
      readComponent('AutomationScenarioEditor.vue'),
      readComponent('AutomationRecentRunsPanel.vue'),
      readComponent('AutomationResultImportPanel.vue'),
      readComponent('AutomationResultEvidencePanel.vue'),
      readComponent('automationGovernance/AutomationInventoryWorkspaceSection.vue'),
      readComponent('automationGovernance/AutomationTemplatesWorkspaceSection.vue'),
      readComponent('automationGovernance/AutomationPlansWorkspaceSection.vue'),
      readComponent('automationGovernance/AutomationExecutionWorkspaceSection.vue'),
      readComponent('automationGovernance/AutomationHandoffWorkspaceSection.vue')
    ].join('\n');

    expect(sources).toContain('system:automation-governance:assets:inventory-refresh');
    expect(sources).toContain('system:automation-governance:assets:plans-edit');
    expect(sources).toContain('system:automation-governance:execution:copy-command');
    expect(sources).toContain('system:automation-governance:evidence:refresh');
    expect(sources).not.toContain('system:rd-automation-inventory:refresh');
    expect(sources).not.toContain('system:rd-automation-templates:add-page-smoke');
    expect(sources).not.toContain('system:rd-automation-plans:edit');
    expect(sources).not.toContain('system:rd-automation-handoff:copy-command');
    expect(sources).not.toContain('system:automation-execution:copy-command');
    expect(sources).not.toContain('system:automation-results:refresh');
  });

  it('re-seeds quality workbench action permissions under automation-governance and soft-deletes legacy codes', () => {
    const sql = readRepoFile('sql/init-data.sql');

    expect(sql).toContain("'system:automation-governance:assets:inventory-refresh'");
    expect(sql).toContain("'system:automation-governance:assets:plans-edit'");
    expect(sql).toContain("'system:automation-governance:execution:copy-command'");
    expect(sql).toContain("'system:automation-governance:evidence:refresh'");
    expect(sql).toContain("'system:automation-governance:evidence:clear-import'");
    expect(sql).toContain('UPDATE sys_menu');
    expect(sql).toContain("'system:rd-automation-inventory:refresh'");
    expect(sql).toContain("'system:rd-automation-handoff:export-plan'");
    expect(sql).toContain("'system:automation-execution:copy-command'");
    expect(sql).toContain("'system:automation-results:clear-import'");
  });

  it('documents automation evidence deep links and governance wording with the new architecture only', () => {
    const source = readRepoFile('docs/03-接口规范与接口清单.md');

    expect(source).toContain('/automation-governance?tab=evidence&runId=...');
    expect(source).toContain('自动化治理台读取历史运行台账');
    expect(source).not.toContain('/automation-results?runId=...');
    expect(source).not.toContain('结果与基线中心读取历史运行台账');
  });
});
