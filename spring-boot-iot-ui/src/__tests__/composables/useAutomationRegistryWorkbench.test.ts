import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  successMessageMock,
  errorMessageMock,
  pageAutomationResultsMock,
  listRecentAutomationResultsMock,
  getAutomationResultDetailMock,
  listAutomationResultEvidenceMock,
  getAutomationResultEvidenceContentMock
} = vi.hoisted(() => ({
  successMessageMock: vi.fn(),
  errorMessageMock: vi.fn(),
  pageAutomationResultsMock: vi.fn(),
  listRecentAutomationResultsMock: vi.fn(),
  getAutomationResultDetailMock: vi.fn(),
  listAutomationResultEvidenceMock: vi.fn(),
  getAutomationResultEvidenceContentMock: vi.fn()
}));

vi.mock('@/api/automationResults', () => ({
  pageAutomationResults: pageAutomationResultsMock,
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

function createRunSummary(
  runId: string,
  overrides: Partial<Record<string, unknown>> = {}
) {
  return {
    runId,
    reportPath: `logs/acceptance/registry-run-${runId}.json`,
    updatedAt: '2026-04-03T09:30:00',
    summary: {
      total: 1,
      passed: 1,
      failed: 0
    },
    failedScenarioIds: [] as string[],
    relatedEvidenceFiles: [`logs/acceptance/business-browser-${runId}.md`],
    status: 'passed',
    runnerTypes: ['browserPlan'],
    ...overrides
  };
}

function createRunDetail(
  runId: string,
  overrides: Partial<Record<string, unknown>> = {}
) {
  return {
    runId,
    reportPath: `logs/acceptance/registry-run-${runId}.json`,
    summary: {
      total: 1,
      passed: 1,
      failed: 0
    },
    results: [
      {
        scenarioId: `scenario.${runId}`,
        runnerType: 'browserPlan',
        status: 'passed',
        blocking: 'warning',
        summary: `${runId} ok`,
        evidenceFiles: [`logs/acceptance/business-browser-${runId}.md`]
      }
    ],
    relatedEvidenceFiles: [`logs/acceptance/business-browser-${runId}.md`],
    ...overrides
  };
}

function createEvidenceList(runId: string) {
  return [
    {
      path: `logs/acceptance/registry-run-${runId}.json`,
      fileName: `registry-run-${runId}.json`,
      category: 'run-summary',
      source: 'report'
    },
    {
      path: `logs/acceptance/business-browser-${runId}.md`,
      fileName: `business-browser-${runId}.md`,
      category: 'markdown',
      source: 'scenario'
    }
  ];
}

function createPageResult(records: unknown[], pageNum = 1, pageSize = 10, total = records.length) {
  return {
    total,
    pageNum,
    pageSize,
    records
  };
}

describe('useAutomationRegistryWorkbench', () => {
  beforeEach(() => {
    successMessageMock.mockReset();
    errorMessageMock.mockReset();
    pageAutomationResultsMock.mockReset();
    listRecentAutomationResultsMock.mockReset();
    getAutomationResultDetailMock.mockReset();
    listAutomationResultEvidenceMock.mockReset();
    getAutomationResultEvidenceContentMock.mockReset();
  });

  it('loads the first history page and auto-selects the first run detail', async () => {
    const firstRun = createRunSummary('20260403093000', {
      summary: { total: 2, passed: 1, failed: 1 },
      failedScenarioIds: ['scenario.20260403093000'],
      status: 'failed',
      runnerTypes: ['browserPlan', 'messageFlow']
    });
    const secondRun = createRunSummary('20260403091500');

    pageAutomationResultsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createPageResult([firstRun, secondRun])
    });
    getAutomationResultDetailMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRunDetail('20260403093000', {
        summary: { total: 2, passed: 1, failed: 1 },
        results: [
          {
            scenarioId: 'scenario.20260403093000',
            runnerType: 'messageFlow',
            status: 'failed',
            blocking: 'blocker',
            summary: 'message flow failed',
            evidenceFiles: ['logs/acceptance/message-flow-20260403093000.json']
          }
        ],
        relatedEvidenceFiles: ['logs/acceptance/message-flow-20260403093000.json']
      })
    });
    listAutomationResultEvidenceMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createEvidenceList('20260403093000')
    });
    getAutomationResultEvidenceContentMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        path: 'logs/acceptance/registry-run-20260403093000.json',
        fileName: 'registry-run-20260403093000.json',
        category: 'run-summary',
        content: '{"runId":"20260403093000"}',
        truncated: false
      }
    });

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRunLedger();

    expect(pageAutomationResultsMock).toHaveBeenCalledWith(
      expect.objectContaining({
        pageNum: 1,
        pageSize: 10
      })
    );
    expect(workbench.ledgerRuns.value).toHaveLength(2);
    expect(workbench.selectedLedgerRunId.value).toBe('20260403093000');
    expect(getAutomationResultDetailMock).toHaveBeenCalledWith('20260403093000');
    expect(listAutomationResultEvidenceMock).toHaveBeenCalledWith('20260403093000');
    expect(getAutomationResultEvidenceContentMock).toHaveBeenCalledWith(
      '20260403093000',
      'logs/acceptance/registry-run-20260403093000.json'
    );
    expect(workbench.currentRun.value?.runId).toBe('20260403093000');
    expect(workbench.currentRun.value?.failedScenarioIds).toEqual(['scenario.20260403093000']);
    expect(workbench.activeEvidenceRunId.value).toBe('20260403093000');
    expect(workbench.visibleEvidenceItems.value).toHaveLength(2);
    expect(workbench.selectedEvidencePath.value).toBe('logs/acceptance/registry-run-20260403093000.json');
  });

  it('resets to page one when applying filters and reloads page changes', async () => {
    pageAutomationResultsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createPageResult([])
    });

    const workbench = useAutomationRegistryWorkbench();
    workbench.pagination.pageNum = 3;
    workbench.ledgerFilters.keyword = 'message-flow';
    workbench.ledgerFilters.status = 'failed';
    workbench.ledgerFilters.runnerType = 'messageFlow';
    workbench.ledgerFilters.dateRange = ['2026-04-01', '2026-04-03'];

    await workbench.applyLedgerFilters();

    expect(pageAutomationResultsMock).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      keyword: 'message-flow',
      status: 'failed',
      runnerType: 'messageFlow',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-03'
    });
    expect(workbench.pagination.pageNum).toBe(1);

    await workbench.handleLedgerPageChange(2);
    expect(pageAutomationResultsMock).toHaveBeenLastCalledWith({
      pageNum: 2,
      pageSize: 10,
      keyword: 'message-flow',
      status: 'failed',
      runnerType: 'messageFlow',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-03'
    });

    await workbench.handleLedgerPageSizeChange(20);
    expect(pageAutomationResultsMock).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 20,
      keyword: 'message-flow',
      status: 'failed',
      runnerType: 'messageFlow',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-03'
    });
  });

  it('switches to the new first row when the current selection disappears, then clears when empty', async () => {
    pageAutomationResultsMock
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createPageResult([createRunSummary('run-a'), createRunSummary('run-b')])
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createPageResult([createRunSummary('run-c')])
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createPageResult([])
      });
    getAutomationResultDetailMock.mockImplementation(async (runId: string) => ({
      code: 200,
      msg: 'success',
      data: createRunDetail(runId)
    }));
    listAutomationResultEvidenceMock.mockImplementation(async (runId: string) => ({
      code: 200,
      msg: 'success',
      data: createEvidenceList(runId)
    }));
    getAutomationResultEvidenceContentMock.mockImplementation(async (runId: string) => ({
      code: 200,
      msg: 'success',
      data: {
        path: `logs/acceptance/registry-run-${runId}.json`,
        fileName: `registry-run-${runId}.json`,
        category: 'run-summary',
        content: `{"runId":"${runId}"}`,
        truncated: false
      }
    }));

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRunLedger();
    await workbench.selectLedgerRun('run-b');

    expect(workbench.selectedLedgerRunId.value).toBe('run-b');
    expect(workbench.currentRun.value?.runId).toBe('run-b');

    await workbench.fetchRunLedger();

    expect(workbench.selectedLedgerRunId.value).toBe('run-c');
    expect(workbench.currentRun.value?.runId).toBe('run-c');

    await workbench.fetchRunLedger();

    expect(workbench.ledgerRuns.value).toHaveLength(0);
    expect(workbench.selectedLedgerRunId.value).toBe('');
    expect(workbench.currentRun.value).toBeNull();
    expect(workbench.activeEvidenceRunId.value).toBe('');
    expect(workbench.visibleEvidenceItems.value).toEqual([]);
  });

  it('keeps manual import as a temporary detail overlay without changing ledger rows', async () => {
    pageAutomationResultsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createPageResult([createRunSummary('20260403093000')])
    });
    getAutomationResultDetailMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRunDetail('20260403093000')
    });
    listAutomationResultEvidenceMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createEvidenceList('20260403093000')
    });
    getAutomationResultEvidenceContentMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        path: 'logs/acceptance/registry-run-20260403093000.json',
        fileName: 'registry-run-20260403093000.json',
        category: 'run-summary',
        content: '{"runId":"20260403093000"}',
        truncated: false
      }
    });

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRunLedger();
    workbench.importRegistryRunSummary(
      JSON.stringify({
        runId: 'manual-run',
        summary: {
          total: 2,
          passed: 1,
          failed: 1
        },
        results: [
          {
            scenarioId: 'manual.failed',
            runnerType: 'browserPlan',
            status: 'failed',
            blocking: 'blocker',
            summary: 'manual import failed'
          }
        ]
      })
    );

    expect(workbench.ledgerRuns.value).toHaveLength(1);
    expect(workbench.importedRun.value?.runId).toBe('manual-run');
    expect(workbench.currentRun.value?.runId).toBe('manual-run');
    expect(workbench.activeEvidenceRunId.value).toBe('');
    expect(workbench.visibleEvidenceItems.value).toEqual([]);
    expect(successMessageMock).toHaveBeenCalledWith('统一运行汇总已导入');

    workbench.clearImportedRun();

    expect(workbench.currentRun.value?.runId).toBe('20260403093000');
    expect(workbench.activeEvidenceRunId.value).toBe('20260403093000');
    expect(workbench.visibleEvidenceItems.value).toHaveLength(2);
    expect(successMessageMock).toHaveBeenCalledWith('已清空导入结果');
  });
});
