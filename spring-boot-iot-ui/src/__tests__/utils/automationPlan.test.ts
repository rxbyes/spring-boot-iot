import { describe, expect, it } from 'vitest';

import {
  buildAutomationPageInventory,
  buildPageCoverageSummary,
  buildPlanSuggestions,
  buildScenarioPreviews,
  createManualInventoryItem,
  createScenarioFromInventory,
  createDefaultAutomationPlan,
  createFormSubmitScenario,
  duplicateScenario,
  normalizeAutomationPlan
} from '../../utils/automationPlan';
import type { MenuTreeNode } from '../../types/auth';

describe('automationPlan utils', () => {
  it('creates a default plan with runnable baseline scenarios', () => {
    const plan = createDefaultAutomationPlan();
    expect(plan.target.planName).toContain('自动化');
    expect(plan.scenarios.map((item: { key: string }) => item.key)).toContain('login');
    expect(plan.scenarios.length).toBeGreaterThanOrEqual(3);
  });

  it('normalizes imported plan and fills default fields', () => {
    const plan = normalizeAutomationPlan({
      target: {
        planName: '外部系统计划',
        frontendBaseUrl: 'http://127.0.0.1:3000'
      } as any,
      scenarios: [
        {
          key: 'external-page',
          name: '外部页面',
          route: '/dashboard',
          steps: []
        } as any
      ]
    });

    expect(plan.target.backendBaseUrl).toBe('http://127.0.0.1:9999');
    expect(plan.target.baselineDir).toBe('config/automation/baselines');
    expect(plan.scenarios[0].steps.length).toBeGreaterThan(0);
    expect(plan.scenarios[0].scope).toBe('baseline');
  });

  it('builds scenario previews for metrics panel', () => {
    const previews = buildScenarioPreviews(createDefaultAutomationPlan());
    expect(previews[0].stepCount).toBeGreaterThan(0);
    expect(previews.some((item: { hasAssertion: boolean }) => item.hasAssertion)).toBe(true);
  });

  it('treats screenshot assertions as assertion coverage', () => {
    const plan = createDefaultAutomationPlan();
    plan.scenarios[0].steps = [
      {
        id: 'visual-check',
        label: '页面截图断言',
        type: 'assertScreenshot',
        screenshotTarget: 'page',
        baselineName: 'login-page',
        threshold: 0
      } as any
    ];

    const previews = buildScenarioPreviews(plan);

    expect(previews[0].hasAssertion).toBe(true);
  });

  it('warns when a plan misses auth and assertions', () => {
    const base = createDefaultAutomationPlan();
    const scenario = createFormSubmitScenario();
    scenario.route = '/external-form';
    scenario.steps = scenario.steps.filter((item: { type: string }) => item.type !== 'assertText');

    const suggestions = buildPlanSuggestions(
      normalizeAutomationPlan({
        scenarios: [scenario],
        target: {
          ...base.target,
          planName: '弱约束计划',
          frontendBaseUrl: 'http://127.0.0.1:3000'
        }
      })
    );

    expect(suggestions.some((item: { title: string }) => item.title.includes('登录前置'))).toBe(true);
    expect(suggestions.some((item: { title: string }) => item.title.includes('断言覆盖'))).toBe(true);
  });

  it('duplicates a scenario and renews step ids', () => {
    const plan = createDefaultAutomationPlan();
    const duplicated = duplicateScenario(plan.scenarios[1]);

    expect(duplicated.key).not.toBe(plan.scenarios[1].key);
    expect(duplicated.steps[0].id).not.toBe(plan.scenarios[1].steps[0].id);
  });

  it('builds page inventory from menus and manual items', () => {
    const menus: MenuTreeNode[] = [
      {
        id: 1,
        menuName: '平台治理',
        path: '/group',
        type: 0,
        children: [
          {
            id: 2,
            menuName: '账号中心',
            path: '/user',
            menuCode: 'system:user',
            type: 1,
            meta: {
              caption: '用户档案、状态与重置密码管理。'
            },
            children: []
          }
        ]
      }
    ];

    const manualPage = createManualInventoryItem({
      route: '/external-dashboard',
      title: '外部大屏',
      caption: '外部系统首页'
    });

    const inventory = buildAutomationPageInventory({
      menus,
      manualPages: [manualPage],
      includeStaticFallback: false
    });

    expect(inventory.some((item: { route: string; source: string }) => item.route === '/user' && item.source === 'menu')).toBe(true);
    expect(inventory.some((item: { route: string; source: string }) => item.route === '/external-dashboard' && item.source === 'manual')).toBe(true);
  });

  it('uses automation governance as the static quality fallback route', () => {
    const inventory = buildAutomationPageInventory({
      menus: [],
      manualPages: []
    });

    expect(inventory.some((item: { route: string; title: string }) => item.route === '/automation-governance' && item.title === '自动化治理台')).toBe(true);
    expect(inventory.some((item: { route: string }) => item.route === '/automation-test')).toBe(false);
  });

  it('creates safe smoke scaffolds from discovered pages', () => {
    const scenario = createScenarioFromInventory(
      createManualInventoryItem({
        route: '/report-analysis',
        title: '分析报表',
        matcher: '/api/report/'
      })
    );

    expect(scenario.route).toBe('/report-analysis');
    expect(scenario.initialApis[0].matcher).toBe('/api/report/');
    expect(scenario.steps.some((item: { type: string }) => item.type === 'assertText')).toBe(true);
  });

  it('calculates page coverage against current plan', () => {
    const plan = createDefaultAutomationPlan();
    const inventory = [
      createManualInventoryItem({
        route: '/products',
        title: '产品模板中心'
      }),
      createManualInventoryItem({
        route: '/external-dashboard',
        title: '外部大屏'
      })
    ];

    const summary = buildPageCoverageSummary(plan, inventory);

    expect(summary.coveredPages).toBe(1);
    expect(summary.uncoveredPages).toBe(1);
    expect(summary.uncoveredRoutes).toContain('/external-dashboard');
  });
});
