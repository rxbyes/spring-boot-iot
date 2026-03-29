export type DiagnosticSourcePage =
  | 'reporting'
  | 'message-trace'
  | 'system-log'
  | 'file-debug'
  | 'access-error'
  | 'products'
  | 'devices';

export type DiagnosticReportStatus =
  | 'ready'
  | 'sent'
  | 'pending'
  | 'timeline-missing'
  | 'validated'
  | 'failed';

export interface DiagnosticContext {
  sourcePage: DiagnosticSourcePage;
  deviceCode?: string;
  traceId?: string;
  productKey?: string;
  topic?: string;
  sessionId?: string;
  transportMode?: 'http' | 'mqtt' | null;
  reportStatus?: DiagnosticReportStatus;
  capturedAt: string;
}

export interface DiagnosticFinding {
  kind: 'identity' | 'contract' | 'correlation' | 'timeline' | 'runtime' | 'validation';
  level: 'info' | 'success' | 'warning' | 'danger';
  title: string;
  summary: string;
  reason: string;
  nextActionLabel?: string;
  nextActionTarget?: string;
}

const DIAGNOSTIC_CONTEXT_STORAGE_KEY = 'iot-access:diagnostic-context';
const DIAGNOSTIC_CONTEXT_TTL_MS = 30 * 60 * 1000;

type RouteQuerySafeKey = 'deviceCode' | 'traceId' | 'productKey' | 'topic';

const ROUTE_QUERY_SAFE_KEYS: RouteQuerySafeKey[] = ['deviceCode', 'traceId', 'productKey', 'topic'];

interface StoredDiagnosticContext {
  storedAt: number;
  context: DiagnosticContext;
}

type PartialDiagnosticContext = Partial<DiagnosticContext>;

function isDiagnosticSourcePage(value: unknown): value is DiagnosticSourcePage {
  return (
    value === 'reporting'
    || value === 'message-trace'
    || value === 'system-log'
    || value === 'file-debug'
    || value === 'access-error'
    || value === 'products'
    || value === 'devices'
  );
}

function isDiagnosticReportStatus(value: unknown): value is DiagnosticReportStatus {
  return (
    value === 'ready'
    || value === 'sent'
    || value === 'pending'
    || value === 'timeline-missing'
    || value === 'validated'
    || value === 'failed'
  );
}

function normalizeTextValue(value: unknown): string | undefined {
  if (value == null) {
    return undefined;
  }
  const firstValue = Array.isArray(value) ? value[0] : value;
  if (typeof firstValue !== 'string') {
    return undefined;
  }
  const normalized = firstValue.trim();
  return normalized || undefined;
}

function getSafeSessionStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function safeGetSessionItem(storage: Storage, key: string): string | null {
  try {
    return storage.getItem(key);
  } catch {
    return null;
  }
}

function safeSetSessionItem(storage: Storage, key: string, value: string): boolean {
  try {
    storage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

function safeRemoveSessionItem(storage: Storage, key: string): void {
  try {
    storage.removeItem(key);
  } catch {
    // ignore storage cleanup failure
  }
}

function normalizeDiagnosticContext(context: PartialDiagnosticContext): DiagnosticContext | null {
  const sourcePage = normalizeTextValue(context.sourcePage);
  const capturedAt = normalizeTextValue(context.capturedAt);
  if (!sourcePage || !capturedAt || !isDiagnosticSourcePage(sourcePage)) {
    return null;
  }

  const normalized: DiagnosticContext = {
    sourcePage,
    capturedAt
  };

  const deviceCode = normalizeTextValue(context.deviceCode);
  if (deviceCode) {
    normalized.deviceCode = deviceCode;
  }
  const traceId = normalizeTextValue(context.traceId);
  if (traceId) {
    normalized.traceId = traceId;
  }
  const productKey = normalizeTextValue(context.productKey);
  if (productKey) {
    normalized.productKey = productKey;
  }
  const topic = normalizeTextValue(context.topic);
  if (topic) {
    normalized.topic = topic;
  }
  const sessionId = normalizeTextValue(context.sessionId);
  if (sessionId) {
    normalized.sessionId = sessionId;
  }
  if (context.transportMode === 'http' || context.transportMode === 'mqtt' || context.transportMode === null) {
    normalized.transportMode = context.transportMode;
  }
  if (isDiagnosticReportStatus(context.reportStatus)) {
    normalized.reportStatus = context.reportStatus;
  }
  return normalized;
}

export function buildDiagnosticRouteQuery(context: DiagnosticContext): Partial<Pick<DiagnosticContext, RouteQuerySafeKey>> {
  const query: Partial<Pick<DiagnosticContext, RouteQuerySafeKey>> = {};
  ROUTE_QUERY_SAFE_KEYS.forEach((key) => {
    const value = normalizeTextValue(context[key]);
    if (value) {
      query[key] = value;
    }
  });
  return query;
}

export function persistDiagnosticContext(context: DiagnosticContext): void {
  const storage = getSafeSessionStorage();
  if (!storage) {
    return;
  }
  const normalizedContext = normalizeDiagnosticContext(context);
  if (!normalizedContext) {
    return;
  }
  const payload: StoredDiagnosticContext = {
    storedAt: Date.now(),
    context: normalizedContext
  };
  safeSetSessionItem(storage, DIAGNOSTIC_CONTEXT_STORAGE_KEY, JSON.stringify(payload));
}

export function loadDiagnosticContext(): DiagnosticContext | null {
  const storage = getSafeSessionStorage();
  if (!storage) {
    return null;
  }
  const raw = safeGetSessionItem(storage, DIAGNOSTIC_CONTEXT_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as StoredDiagnosticContext;
    const storedAt = Number(parsed?.storedAt || 0);
    if (!storedAt || Date.now() - storedAt > DIAGNOSTIC_CONTEXT_TTL_MS) {
      safeRemoveSessionItem(storage, DIAGNOSTIC_CONTEXT_STORAGE_KEY);
      return null;
    }
    const normalized = normalizeDiagnosticContext(parsed?.context || {});
    if (!normalized) {
      safeRemoveSessionItem(storage, DIAGNOSTIC_CONTEXT_STORAGE_KEY);
      return null;
    }
    return normalized;
  } catch {
    safeRemoveSessionItem(storage, DIAGNOSTIC_CONTEXT_STORAGE_KEY);
    return null;
  }
}

export function resolveDiagnosticContext(query: Record<string, unknown>): DiagnosticContext | null {
  const hasDiagnosticQueryKey = ROUTE_QUERY_SAFE_KEYS.some((key) => normalizeTextValue(query[key]));
  if (!hasDiagnosticQueryKey) {
    return null;
  }

  const stored = loadDiagnosticContext();
  if (!stored) {
    return null;
  }
  const routeOverrides: Partial<Pick<DiagnosticContext, RouteQuerySafeKey>> = {};
  ROUTE_QUERY_SAFE_KEYS.forEach((key) => {
    const value = normalizeTextValue(query[key]);
    if (value) {
      routeOverrides[key] = value;
    }
  });
  return {
    ...stored,
    ...routeOverrides
  };
}

export function describeDiagnosticSource(sourcePage: DiagnosticSourcePage): string {
  switch (sourcePage) {
    case 'reporting':
      return '链路验证中心';
    case 'message-trace':
      return '链路追踪台';
    case 'system-log':
      return '异常观测台';
    case 'file-debug':
      return '数据校验台';
    case 'access-error':
      return '失败归档';
    case 'products':
      return '产品定义中心';
    case 'devices':
      return '设备资产中心';
    default:
      return sourcePage;
  }
}
