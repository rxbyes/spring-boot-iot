import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockRoute,
  mockRouter,
  listBusinessAcceptancePackagesMock,
  listBusinessAcceptanceAccountTemplatesMock,
  launchBusinessAcceptanceRunMock,
  getBusinessAcceptanceRunStatusMock,
  getBusinessAcceptanceResultMock
} = vi.hoisted(() => ({
  mockRoute: {
    params: {},
    query: {}
  },
  mockRouter: {
    push: vi.fn().mockResolvedValue(undefined)
  },
  listBusinessAcceptancePackagesMock: vi.fn(),
  listBusinessAcceptanceAccountTemplatesMock: vi.fn(),
  launchBusinessAcceptanceRunMock: vi.fn(),
  getBusinessAcceptanceRunStatusMock: vi.fn(),
  getBusinessAcceptanceResultMock: vi.fn()
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/businessAcceptance', () => ({
  listBusinessAcceptancePackages: listBusinessAcceptancePackagesMock,
  listBusinessAcceptanceAccountTemplates: listBusinessAcceptanceAccountTemplatesMock,
  launchBusinessAcceptanceRun: launchBusinessAcceptanceRunMock,
  getBusinessAcceptanceRunStatus: getBusinessAcceptanceRunStatusMock,
  getBusinessAcceptanceResult: getBusinessAcceptanceResultMock
}));

import { useBusinessAcceptanceWorkbench } from '@/composables/useBusinessAcceptanceWorkbench';

function createPackage() {
  return {
    packageCode: 'product-device',
    packageName: '产品与设备',
    description: '覆盖产品与设备交付验收。',
    targetRoles: ['acceptance', 'product', 'manager'],
    supportedEnvironments: ['dev', 'test'],
    defaultAccountTemplate: 'acceptance-default',
    latestResult: {
      runId: '20260403101010',
      status: 'failed',
      updatedAt: '2026-04-03T10:10:10+08:00',
      passedModuleCount: 1,
      failedModuleCount: 1,
      failedModuleNames: ['产品新增']
    },
    modules: [
      {
        moduleCode: 'product-create',
        moduleName: '产品新增',
        suggestedDirection: 'needsReview',
        scenarioRefs: ['auth.browser-smoke']
      },
      {
        moduleCode: 'product-query',
        moduleName: '产品查询',
        suggestedDirection: 'needsReview',
        scenarioRefs: ['system.api-smoke']
      }
    ]
  };
}

function createAccountTemplates() {
  return [
    {
      templateCode: 'acceptance-default',
      templateName: '验收账号模板',
      username: 'biz_demo',
      roleHint: '业务验收',
      supportedEnvironments: ['dev', 'test']
    },
    {
      templateCode: 'manager-default',
      templateName: '项目经理账号模板',
      username: 'manager_demo',
      roleHint: '项目经理',
      supportedEnvironments: ['dev', 'test']
    }
  ];
}

describe('useBusinessAcceptanceWorkbench', () => {
  beforeEach(() => {
    mockRoute.params = {};
    mockRoute.query = {};
    mockRouter.push.mockReset();
    listBusinessAcceptancePackagesMock.mockReset();
    listBusinessAcceptanceAccountTemplatesMock.mockReset();
    launchBusinessAcceptanceRunMock.mockReset();
    getBusinessAcceptanceRunStatusMock.mockReset();
    getBusinessAcceptanceResultMock.mockReset();
  });

  it('loads package cards and account templates, then launches a run', async () => {
    let scheduledPoll: (() => Promise<void> | void) | null = null;
    listBusinessAcceptancePackagesMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [createPackage()]
    });
    listBusinessAcceptanceAccountTemplatesMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createAccountTemplates()
    });
    launchBusinessAcceptanceRunMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        jobId: 'job-001',
        status: 'running',
        startedAt: '2026-04-04T11:00:00+08:00'
      }
    });
    getBusinessAcceptanceRunStatusMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        jobId: 'job-001',
        status: 'completed',
        runId: '20260404153000',
        startedAt: '2026-04-04T11:00:00+08:00',
        finishedAt: '2026-04-04T11:02:00+08:00'
      }
    });

    const workbench = useBusinessAcceptanceWorkbench({
      schedulePoll: (callback) => {
        scheduledPoll = callback;
        return 1;
      },
      clearScheduledPoll: vi.fn()
    });

    await workbench.loadInitialData();

    expect(workbench.packages.value[0]?.packageCode).toBe('product-device');
    expect(workbench.selectedEnvironment.value).toBe('dev');
    expect(workbench.selectedAccountTemplate.value).toBe('acceptance-default');
    expect(workbench.selectedModuleCodes.value).toEqual(['product-create', 'product-query']);

    await workbench.launchSelectedPackage();

    expect(workbench.runStatus.value?.status).toBe('running');
    expect(launchBusinessAcceptanceRunMock).toHaveBeenCalledWith({
      packageCode: 'product-device',
      environmentCode: 'dev',
      accountTemplateCode: 'acceptance-default',
      moduleCodes: ['product-create', 'product-query']
    });

    await scheduledPoll?.();

    expect(getBusinessAcceptanceRunStatusMock).toHaveBeenCalledWith('job-001');
    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/business-acceptance/results/20260404153000',
      query: {
        packageCode: 'product-device'
      }
    });
  });

  it('selects the platform P0 package defaults and preserves blocked result deep links', async () => {
    listBusinessAcceptancePackagesMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          packageCode: 'platform-p0-full-flow',
          packageName: 'P0 全流程业务验收',
          description: '覆盖 P0 主业务链路。',
          targetRoles: ['acceptance', 'product', 'manager'],
          supportedEnvironments: ['dev', 'test'],
          defaultAccountTemplate: 'manager-default',
          latestResult: {
            runId: '20260425101010',
            status: 'blocked',
            updatedAt: '2026-04-25T10:10:10+08:00',
            passedModuleCount: 5,
            failedModuleCount: 1,
            failedModuleNames: ['质量工场自验']
          },
          modules: [
            {
              moduleCode: 'login-auth',
              moduleName: '登录与权限上下文',
              suggestedDirection: 'needsReview',
              scenarioRefs: ['auth.browser-smoke']
            },
            {
              moduleCode: 'quality-factory-self-check',
              moduleName: '质量工场自验',
              suggestedDirection: 'environment',
              scenarioRefs: ['quality-factory.business-acceptance.browser-smoke']
            }
          ]
        }
      ]
    });
    listBusinessAcceptanceAccountTemplatesMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createAccountTemplates()
    });

    const workbench = useBusinessAcceptanceWorkbench();

    await workbench.loadInitialData();

    expect(workbench.selectedPackageCode.value).toBe('platform-p0-full-flow');
    expect(workbench.selectedPackage.value?.packageCode).toBe('platform-p0-full-flow');
    expect(workbench.selectedEnvironment.value).toBe('dev');
    expect(workbench.selectedAccountTemplate.value).toBe('manager-default');
    expect(workbench.selectedModuleCodes.value).toEqual(['login-auth', 'quality-factory-self-check']);
    expect(workbench.selectedLatestResult.value?.status).toBe('blocked');
    expect(workbench.selectedLatestResult.value?.failedModuleNames).toEqual(['质量工场自验']);

    await workbench.goToAutomationResults('20260425101010');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/automation-results',
      query: {
        runId: '20260425101010'
      }
    });
  });

  it('loads result details from route context and keeps automation results deep link', async () => {
    mockRoute.params = {
      runId: '20260404153000'
    };
    mockRoute.query = {
      packageCode: 'product-device'
    };
    getBusinessAcceptanceResultMock.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        runId: '20260404153000',
        status: 'failed',
        passedModuleCount: 1,
        failedModuleCount: 1,
        failedModuleNames: ['产品新增'],
        durationText: '2m 0s',
        jumpToAutomationResultsPath: '/automation-results?runId=20260404153000',
        modules: [
          {
            moduleCode: 'product-create',
            moduleName: '产品新增',
            status: 'failed',
            failedScenarioCount: 1,
            failedScenarioTitles: ['登录与产品设备浏览器冒烟'],
            suggestedDirection: 'needsReview',
            failureDetails: [
              {
                scenarioId: 'auth.browser-smoke',
                scenarioTitle: '登录与产品设备浏览器冒烟',
                stepLabel: '提交产品新增表单',
                apiRef: 'POST /device/product/add',
                pageAction: '点击新增产品并提交',
                summary: '产品新增链路需要复核。'
              }
            ]
          },
          {
            moduleCode: 'product-query',
            moduleName: '产品查询',
            status: 'passed',
            failedScenarioCount: 0,
            failedScenarioTitles: [],
            suggestedDirection: 'needsReview',
            failureDetails: []
          }
        ]
      }
    });

    const workbench = useBusinessAcceptanceWorkbench();

    await workbench.loadResultFromRoute();

    expect(getBusinessAcceptanceResultMock).toHaveBeenCalledWith('20260404153000', 'product-device');
    expect(workbench.result.value?.status).toBe('failed');
    expect(workbench.activeModuleCode.value).toBe('product-create');

    await workbench.goToAutomationResults('20260404153000');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/automation-results',
      query: {
        runId: '20260404153000'
      }
    });
  });
});
