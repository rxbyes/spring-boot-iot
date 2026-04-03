import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  successMessageMock,
  errorMessageMock,
  listRecentAutomationResultsMock,
  getAutomationResultDetailMock,
  listAutomationResultEvidenceMock,
  getAutomationResultEvidenceContentMock
} = vi.hoisted(() => ({
  successMessageMock: vi.fn(),
  errorMessageMock: vi.fn(),
  listRecentAutomationResultsMock: vi.fn(),
  getAutomationResultDetailMock: vi.fn(),
  listAutomationResultEvidenceMock: vi.fn(),
  getAutomationResultEvidenceContentMock: vi.fn()
}));

vi.mock('@/api/automationResults', () => ({
  listRecentAutomationResults: listRecentAutomationResultsMock,
  getAutomationResultDetail: getAutomationResultDetailMock,
  listAutomationResultEvidence: listAutomationResultEvidenceMock,
  getAutomationResultEvidenceContent: getAutomationResultEvidenceContentMock
}));

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: successMessageMock,
    error: errorMessageMock
  }
}));

import { useAutomationRegistryWorkbench } from '@/composables/useAutomationRegistryWorkbench';

describe('useAutomationRegistryWorkbench', () => {
  beforeEach(() => {
    successMessageMock.mockReset();
    errorMessageMock.mockReset();
    listRecentAutomationResultsMock.mockReset();
    getAutomationResultDetailMock.mockReset();
    listAutomationResultEvidenceMock.mockReset();
    getAutomationResultEvidenceContentMock.mockReset();
  });

  it('loads recent runs and maps the selected run into imported results state', async () => {
    listRecentAutomationResultsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          runId: '20260402155432',
          reportPath: 'logs/acceptance/registry-run-20260402155432.json',
          updatedAt: '2026-04-02T15:54:32',
          summary: {
            total: 1,
            passed: 0,
            failed: 1
          },
          failedScenarioIds: ['risk.full-drill.red-chain'],
          relatedEvidenceFiles: ['logs/acceptance/risk-drill-1775116282733.json']
        }
      ]
    });
    getAutomationResultDetailMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        runId: '20260402155432',
        reportPath: 'logs/acceptance/registry-run-20260402155432.json',
        summary: {
          total: 1,
          passed: 0,
          failed: 1
        },
        results: [
          {
            scenarioId: 'risk.full-drill.red-chain',
            runnerType: 'riskDrill',
            status: 'failed',
            blocking: 'blocker',
            summary: 'simulated failure',
            evidenceFiles: ['logs/acceptance/risk-drill-1775116282733.json']
          }
        ]
      }
    });
    listAutomationResultEvidenceMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          path: 'logs/acceptance/registry-run-20260402155432.json',
          fileName: 'registry-run-20260402155432.json',
          category: 'run-summary',
          source: 'report'
        },
        {
          path: 'logs/acceptance/risk-drill-1775116282733.json',
          fileName: 'risk-drill-1775116282733.json',
          category: 'json',
          source: 'scenario'
        }
      ]
    });
    getAutomationResultEvidenceContentMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        path: 'logs/acceptance/registry-run-20260402155432.json',
        fileName: 'registry-run-20260402155432.json',
        category: 'run-summary',
        content: '{\"runId\":\"20260402155432\"}',
        truncated: false
      }
    });

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRecentRuns();

    expect(listRecentAutomationResultsMock).toHaveBeenCalledTimes(1);
    expect(workbench.recentRuns.value).toHaveLength(1);

    await workbench.selectRecentRun('20260402155432');

    expect(getAutomationResultDetailMock).toHaveBeenCalledWith('20260402155432');
    expect(listAutomationResultEvidenceMock).toHaveBeenCalledWith('20260402155432');
    expect(getAutomationResultEvidenceContentMock).toHaveBeenCalledWith(
      '20260402155432',
      'logs/acceptance/registry-run-20260402155432.json'
    );
    expect(workbench.selectedRecentRunId.value).toBe('20260402155432');
    expect(workbench.importedRun.value?.failedScenarioIds).toEqual(['risk.full-drill.red-chain']);
    expect(workbench.evidenceItems.value).toHaveLength(2);
    expect(workbench.selectedEvidencePath.value).toBe('logs/acceptance/registry-run-20260402155432.json');
    expect(workbench.evidencePreview.value?.content).toContain('20260402155432');
    expect(successMessageMock).toHaveBeenCalledWith('已载入最近运行结果');
  });
});
