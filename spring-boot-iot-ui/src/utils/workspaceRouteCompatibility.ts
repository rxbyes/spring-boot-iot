import { buildAutomationGovernanceQuery, type AutomationGovernanceQueryState } from './automationGovernance';
import { normalizeOptionalRoutePath, normalizeRoutePath } from './routePath';

export interface LegacyQualityWorkspaceRouteConfig {
  sourcePath: string;
  targetPath: '/automation-governance';
  queryState: Partial<AutomationGovernanceQueryState>;
}

export const legacyQualityWorkspaceRoutes: readonly LegacyQualityWorkspaceRouteConfig[] = [
  {
    sourcePath: '/rd-workbench',
    targetPath: '/automation-governance',
    queryState: {}
  },
  {
    sourcePath: '/rd-automation-inventory',
    targetPath: '/automation-governance',
    queryState: {
      assetTab: 'inventory'
    }
  },
  {
    sourcePath: '/rd-automation-templates',
    targetPath: '/automation-governance',
    queryState: {
      assetTab: 'templates'
    }
  },
  {
    sourcePath: '/rd-automation-plans',
    targetPath: '/automation-governance',
    queryState: {
      assetTab: 'plans'
    }
  },
  {
    sourcePath: '/rd-automation-handoff',
    targetPath: '/automation-governance',
    queryState: {
      assetTab: 'handoff'
    }
  },
  {
    sourcePath: '/automation-assets',
    targetPath: '/automation-governance',
    queryState: {}
  },
  {
    sourcePath: '/automation-test',
    targetPath: '/automation-governance',
    queryState: {}
  },
  {
    sourcePath: '/automation-execution',
    targetPath: '/automation-governance',
    queryState: {
      tab: 'execution'
    }
  },
  {
    sourcePath: '/automation-results',
    targetPath: '/automation-governance',
    queryState: {
      tab: 'evidence'
    }
  }
] as const;

const legacyQualityWorkspaceRouteMap = new Map(
  legacyQualityWorkspaceRoutes.map((route) => [route.sourcePath, route] as const)
);

interface ParsedInternalRouteTarget {
  path: string;
  query: Record<string, string>;
  hash: string;
}

function parseInternalRouteTarget(rawPath?: string | null): ParsedInternalRouteTarget | null {
  const trimmed = (rawPath || '').trim();
  if (!trimmed || !trimmed.startsWith('/')) {
    return null;
  }

  const url = new URL(trimmed, 'https://codex.local');
  const query: Record<string, string> = {};
  url.searchParams.forEach((value, key) => {
    query[key] = value;
  });

  return {
    path: normalizeRoutePath(url.pathname),
    query,
    hash: url.hash || ''
  };
}

function appendQueryValue(searchParams: URLSearchParams, key: string, value: unknown): void {
  if (Array.isArray(value)) {
    value.forEach((item) => appendQueryValue(searchParams, key, item));
    return;
  }
  if (value === undefined || value === null || value === '') {
    return;
  }
  searchParams.append(key, String(value));
}

function buildInternalRouteTarget(path: string, query: Record<string, unknown>, hash = ''): string {
  const searchParams = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => appendQueryValue(searchParams, key, value));
  const search = searchParams.toString();
  return `${normalizeRoutePath(path)}${search ? `?${search}` : ''}${hash || ''}`;
}

export function normalizeWorkspaceRedirectTarget(rawPath?: string | null): string | null {
  const parsedTarget = parseInternalRouteTarget(rawPath);
  if (!parsedTarget) {
    return null;
  }

  if (parsedTarget.path === '/automation-governance') {
    return buildInternalRouteTarget(
      parsedTarget.path,
      buildAutomationGovernanceQuery({}, parsedTarget.query),
      parsedTarget.hash
    );
  }

  const legacyRoute = legacyQualityWorkspaceRouteMap.get(parsedTarget.path);
  if (!legacyRoute) {
    return buildInternalRouteTarget(parsedTarget.path, parsedTarget.query, parsedTarget.hash);
  }

  return buildInternalRouteTarget(
    legacyRoute.targetPath,
    buildAutomationGovernanceQuery(legacyRoute.queryState, parsedTarget.query),
    parsedTarget.hash
  );
}

export function resolveWorkspacePermissionPath(rawPath?: string | null): string | null {
  const normalizedTarget = normalizeWorkspaceRedirectTarget(rawPath);
  const parsedTarget = parseInternalRouteTarget(normalizedTarget);
  return normalizeOptionalRoutePath(parsedTarget?.path);
}
