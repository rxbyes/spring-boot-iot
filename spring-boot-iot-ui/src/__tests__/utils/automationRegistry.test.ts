import { describe, expect, it } from 'vitest';

import {
  buildRegistrySummary,
  parseRegistryRunSummary
} from '../../utils/automationRegistry';

describe('automationRegistry utils', () => {
  it('builds summary counts from registry scenarios', () => {
    const summary = buildRegistrySummary([
      {
        id: 'auth.browser-smoke',
        runnerType: 'browserPlan',
        blocking: 'blocker',
        scope: 'delivery'
      },
      {
        id: 'risk.full-drill.red-chain',
        runnerType: 'riskDrill',
        blocking: 'blocker',
        scope: 'delivery'
      }
    ] as any);

    expect(summary.total).toBe(2);
    expect(summary.blockerCount).toBe(2);
    expect(summary.byRunner.riskDrill).toBe(1);
  });

  it('parses imported registry run results and exposes failed scenario ids', () => {
    const parsed = parseRegistryRunSummary({
      summary: { total: 2, passed: 1, failed: 1 },
      results: [
        { scenarioId: 'auth.browser-smoke', status: 'passed', blocking: 'blocker' },
        { scenarioId: 'risk.full-drill.red-chain', status: 'failed', blocking: 'blocker' }
      ]
    } as any);

    expect(parsed.failedScenarioIds).toEqual(['risk.full-drill.red-chain']);
  });
});
