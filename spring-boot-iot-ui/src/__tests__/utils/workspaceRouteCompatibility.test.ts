import { describe, expect, it } from 'vitest';

import {
  normalizeWorkspaceRedirectTarget,
  resolveWorkspacePermissionPath
} from '@/utils/workspaceRouteCompatibility';

describe('workspace route compatibility', () => {
  it('normalizes legacy quality workbench paths to automation governance targets', () => {
    expect(normalizeWorkspaceRedirectTarget('/rd-workbench')).toBe('/automation-governance');
    expect(normalizeWorkspaceRedirectTarget('/rd-automation-plans')).toBe('/automation-governance?assetTab=plans');
    expect(normalizeWorkspaceRedirectTarget('/automation-execution')).toBe('/automation-governance?tab=execution');
    const resultPath = normalizeWorkspaceRedirectTarget('/automation-results?runId=20260426193000');

    expect(resultPath?.startsWith('/automation-governance?')).toBe(true);
    const params = new URL(`https://codex.local${resultPath || ''}`).searchParams;
    expect(params.get('tab')).toBe('evidence');
    expect(params.get('runId')).toBe('20260426193000');
  });

  it('resolves permission checks against canonical governance paths even when redirect carries query params', () => {
    expect(resolveWorkspacePermissionPath('/automation-governance?tab=evidence&runId=20260426193000')).toBe(
      '/automation-governance'
    );
    expect(resolveWorkspacePermissionPath('/automation-results?runId=20260426193000')).toBe('/automation-governance');
    expect(resolveWorkspacePermissionPath('/business-acceptance/results/20260426193000')).toBe(
      '/business-acceptance/results/20260426193000'
    );
  });
});
