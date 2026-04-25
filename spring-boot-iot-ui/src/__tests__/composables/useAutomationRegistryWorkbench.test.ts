import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockRoute,
  replaceMock,
  successMessageMock,
  errorMessageMock,
  pageAutomationResultsMock,
  listAutomationResultFacetsMock,
  refreshAutomationResultIndexMock,
  listRecentAutomationResultsMock,
  getAutomationResultDetailMock,
  listAutomationResultEvidenceMock,
  getAutomationResultEvidenceContentMock
} = vi.hoisted(() => ({
  mockRoute: {
    query: {}
  },
  replaceMock: vi.fn(),
  successMessageMock: vi.fn(),
  errorMessageMock: vi.fn(),
  pageAutomationResultsMock: vi.fn(),
  listAutomationResultFacetsMock: vi.fn(),
  refreshAutomationResultIndexMock: vi.fn(),
  listRecentAutomationResultsMock: vi.fn(),
  getAutomationResultDetailMock: vi.fn(),
  listAutomationResultEvidenceMock: vi.fn(),
  getAutomationResultEvidenceContentMock: vi.fn()
}));

vi.mock('@/api/automationResults', () => ({
  pageAutomationResults: pageAutomationResultsMock,
  listAutomationResultFacets: listAutomationResultFacetsMock,
  refreshAutomationResultIndex: refreshAutomationResultIndexMock,
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

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    replace: replaceMock
  })
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
    mockRoute.query = {};
    replaceMock.mockReset();
    successMessageMock.mockReset();
    errorMessageMock.mockReset();
    pageAutomationResultsMock.mockReset();
    listAutomationResultFacetsMock.mockReset();
    refreshAutomationResultIndexMock.mockReset();
    listRecentAutomationResultsMock.mockReset();
    getAutomationResultDetailMock.mockReset();
    listAutomationResultEvidenceMock.mockReset();
    getAutomationResultEvidenceContentMock.mockReset();
    listAutomationResultFacetsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        statuses: ['failed', 'passed'],
        runnerTypes: ['browserPlan', 'riskDrill'],
        packageCodes: ['product-governance-p1', 'quality-factory-p0'],
        environmentCodes: ['dev', 'sit']
      }
    });
    refreshAutomationResultIndexMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        generatedAt: '2026-04-25T23:30:00Z',
        latestIndexPath: 'logs/acceptance/automation-result-index.latest.json',
        indexedRuns: 2,
        skippedFiles: 0
      }
    });
  });

  it('preselects a run in automation results when runId query exists', async () => {
    mockRoute.query = {
      runId: '20260404153000'
    };
    getAutomationResultDetailMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRunDetail('20260404153000')
    });
    listAutomationResultEvidenceMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createEvidenceList('20260404153000')
    });
    getAutomationResultEvidenceContentMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        path: 'logs/acceptance/registry-run-20260404153000.json',
        fileName: 'registry-run-20260404153000.json',
        category: 'run-summary',
        content: '{"runId":"20260404153000"}',
        truncated: false
      }
    });

    const workbench = useAutomationRegistryWorkbench();

    await Promise.resolve();
    await Promise.resolve();

    expect(workbench.selectedLedgerRunId.value).toBe('20260404153000');
    expect(getAutomationResultDetailMock).toHaveBeenCalledWith('20260404153000');
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
    workbench.ledgerFilters.packageCode = 'quality-factory-p0';
    workbench.ledgerFilters.environmentCode = 'dev';
    workbench.ledgerFilters.dateRange = ['2026-04-01', '2026-04-03'];

    await workbench.applyLedgerFilters();

    expect(pageAutomationResultsMock).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      keyword: 'message-flow',
      status: 'failed',
      runnerType: 'messageFlow',
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev',
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
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev',
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
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-03'
    });
  });

  it('loads archive facets and exposes package and environment filter options', async () => {
    pageAutomationResultsMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createPageResult([])
    });

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRunLedger();

    expect(listAutomationResultFacetsMock).toHaveBeenCalledTimes(1);
    expect(workbench.ledgerFacetOptions.value.packageCodes).toEqual([
      'product-governance-p1',
      'quality-factory-p0'
    ]);
    expect(workbench.ledgerFacetOptions.value.environmentCodes).toEqual(['dev', 'sit']);
  });

  it('refreshes the archive index and reloads the ledger', async () => {
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
    pageAutomationResultsMock.mockClear();

    await workbench.refreshResultArchiveIndex();

    expect(refreshAutomationResultIndexMock).toHaveBeenCalledTimes(1);
    expect(pageAutomationResultsMock).toHaveBeenCalledTimes(1);
    expect(successMessageMock).toHaveBeenCalledWith('结果归档索引已刷新');
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
    expect(replaceMock).toHaveBeenCalledWith({
      query: {
        runId: 'run-b'
      }
    });

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

  it('refreshes the currently selected run detail when the ledger reload keeps the same run', async () => {
    pageAutomationResultsMock
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createPageResult([createRunSummary('run-a')])
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createPageResult([createRunSummary('run-a')])
      });
    getAutomationResultDetailMock
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createRunDetail('run-a', {
          summary: { total: 1, passed: 1, failed: 0 },
          results: [
            {
              scenarioId: 'scenario.run-a',
              runnerType: 'browserPlan',
              status: 'passed',
              blocking: 'warning',
              summary: 'first snapshot'
            }
          ]
        })
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createRunDetail('run-a', {
          summary: { total: 1, passed: 0, failed: 1 },
          results: [
            {
              scenarioId: 'scenario.run-a',
              runnerType: 'messageFlow',
              status: 'failed',
              blocking: 'blocker',
              summary: 'second snapshot'
            }
          ]
        })
      });
    listAutomationResultEvidenceMock
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: createEvidenceList('run-a')
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: [
          {
            path: 'logs/acceptance/registry-run-run-a.json',
            fileName: 'registry-run-run-a.json',
            category: 'run-summary',
            source: 'report'
          },
          {
            path: 'logs/acceptance/message-flow-run-a.json',
            fileName: 'message-flow-run-a.json',
            category: 'json',
            source: 'scenario'
          }
        ]
      });
    getAutomationResultEvidenceContentMock
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          path: 'logs/acceptance/registry-run-run-a.json',
          fileName: 'registry-run-run-a.json',
          category: 'run-summary',
          content: '{"stage":"first"}',
          truncated: false
        }
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          path: 'logs/acceptance/registry-run-run-a.json',
          fileName: 'registry-run-run-a.json',
          category: 'run-summary',
          content: '{"stage":"second"}',
          truncated: false
        }
      });

    const workbench = useAutomationRegistryWorkbench();

    await workbench.fetchRunLedger();
    expect(workbench.currentRun.value?.summary.failed).toBe(0);
    expect(workbench.visibleEvidenceItems.value).toHaveLength(2);
    expect(workbench.visibleEvidencePreview.value?.content).toContain('first');

    await workbench.fetchRunLedger();

    expect(getAutomationResultDetailMock).toHaveBeenCalledTimes(2);
    expect(listAutomationResultEvidenceMock).toHaveBeenCalledTimes(2);
    expect(workbench.currentRun.value?.summary.failed).toBe(1);
    expect(workbench.currentRun.value?.failedResults[0]?.summary).toBe('second snapshot');
    expect(workbench.visibleEvidenceItems.value[1]?.path).toBe('logs/acceptance/message-flow-run-a.json');
    expect(workbench.visibleEvidencePreview.value?.content).toContain('second');
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
