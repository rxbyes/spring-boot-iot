import { describe, expect, it } from 'vitest';

import { normalizeAutomationGovernanceQuery } from '@/utils/automationGovernance';

describe('automationGovernance query model', () => {
  it('defaults to assets and inventory when query is empty', () => {
    expect(normalizeAutomationGovernanceQuery({})).toEqual({
      tab: 'assets',
      assetTab: 'inventory',
      runId: ''
    });
  });

  it('forces evidence tab when runId is present', () => {
    expect(normalizeAutomationGovernanceQuery({
      tab: 'execution',
      runId: '20260425123456'
    })).toEqual({
      tab: 'evidence',
      assetTab: 'inventory',
      runId: '20260425123456'
    });
  });

  it('drops invalid tab names back to the governance defaults', () => {
    expect(normalizeAutomationGovernanceQuery({
      tab: 'legacy-results',
      assetTab: 'legacy-plans',
      runId: ''
    })).toEqual({
      tab: 'assets',
      assetTab: 'inventory',
      runId: ''
    });
  });
});
