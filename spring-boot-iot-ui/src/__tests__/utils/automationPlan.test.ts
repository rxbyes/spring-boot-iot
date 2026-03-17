import { describe, expect, it } from 'vitest';

import {
  buildPlanSuggestions,
  buildScenarioPreviews,
  createDefaultAutomationPlan,
  createFormSubmitScenario,
  duplicateScenario,
  normalizeAutomationPlan
} from '../../utils/automationPlan';

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
    expect(plan.scenarios[0].steps.length).toBeGreaterThan(0);
    expect(plan.scenarios[0].scope).toBe('baseline');
  });

  it('builds scenario previews for metrics panel', () => {
    const previews = buildScenarioPreviews(createDefaultAutomationPlan());
    expect(previews[0].stepCount).toBeGreaterThan(0);
    expect(previews.some((item: { hasAssertion: boolean }) => item.hasAssertion)).toBe(true);
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
});
