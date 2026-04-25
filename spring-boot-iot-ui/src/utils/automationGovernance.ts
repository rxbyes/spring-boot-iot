import type { LocationQuery, LocationQueryRaw } from 'vue-router';

export const automationGovernanceTabs = ['assets', 'execution', 'evidence'] as const;
export const automationGovernanceAssetTabs = ['inventory', 'templates', 'plans', 'handoff'] as const;

export type AutomationGovernanceTab = (typeof automationGovernanceTabs)[number];
export type AutomationGovernanceAssetTab = (typeof automationGovernanceAssetTabs)[number];

export interface AutomationGovernanceQueryState {
  tab: AutomationGovernanceTab;
  assetTab: AutomationGovernanceAssetTab;
  runId: string;
}

function normalizeQueryText(value: unknown) {
  if (Array.isArray(value)) {
    return normalizeQueryText(value[0]);
  }
  if (value === undefined || value === null) {
    return '';
  }
  return String(value).trim();
}

function isGovernanceTab(value: string): value is AutomationGovernanceTab {
  return (automationGovernanceTabs as readonly string[]).includes(value);
}

function isGovernanceAssetTab(value: string): value is AutomationGovernanceAssetTab {
  return (automationGovernanceAssetTabs as readonly string[]).includes(value);
}

export function normalizeAutomationGovernanceQuery(
  query: LocationQuery | Record<string, unknown>
): AutomationGovernanceQueryState {
  const requestedTab = normalizeQueryText(query.tab);
  const requestedAssetTab = normalizeQueryText(query.assetTab);
  const runId = normalizeQueryText(query.runId);

  return {
    tab: runId ? 'evidence' : isGovernanceTab(requestedTab) ? requestedTab : 'assets',
    assetTab: isGovernanceAssetTab(requestedAssetTab) ? requestedAssetTab : 'inventory',
    runId
  };
}

export function buildAutomationGovernanceQuery(
  partial: Partial<AutomationGovernanceQueryState>,
  currentQuery: LocationQuery | Record<string, unknown> = {}
): LocationQueryRaw {
  const next = normalizeAutomationGovernanceQuery({
    ...currentQuery,
    ...partial
  });
  const query: LocationQueryRaw = { ...(currentQuery as Record<string, unknown>) };

  if (next.tab === 'assets') {
    delete query.tab;
  } else {
    query.tab = next.tab;
  }

  if (next.assetTab === 'inventory') {
    delete query.assetTab;
  } else {
    query.assetTab = next.assetTab;
  }

  if (next.runId) {
    query.runId = next.runId;
  } else {
    delete query.runId;
  }

  return query;
}

export function buildAutomationGovernanceEvidencePath(runId?: string | null) {
  const normalizedRunId = normalizeQueryText(runId);
  if (!normalizedRunId) {
    return '/automation-governance?tab=evidence';
  }
  return `/automation-governance?tab=evidence&runId=${encodeURIComponent(normalizedRunId)}`;
}
