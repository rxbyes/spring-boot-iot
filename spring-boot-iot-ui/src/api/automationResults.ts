import { request } from './request';
import type { PageResult } from '../types/api';
import type {
  AcceptanceRegistryRunSummary,
  AutomationResultArchiveFacets,
  AutomationResultArchiveRefreshResult,
  AutomationResultEvidenceContent,
  AutomationResultEvidenceItem,
  AutomationResultRecentRun,
  AutomationResultRunPageQuery,
  AutomationResultRunSummary
} from '../types/automation';

function appendQueryParam(params: URLSearchParams, key: string, value?: string | number | null) {
  if (value === undefined || value === null) {
    return;
  }
  const normalized = String(value).trim();
  if (!normalized) {
    return;
  }
  params.append(key, normalized);
}

export function pageAutomationResults(params: AutomationResultRunPageQuery) {
  const query = new URLSearchParams();
  appendQueryParam(query, 'pageNum', params.pageNum);
  appendQueryParam(query, 'pageSize', params.pageSize);
  appendQueryParam(query, 'keyword', params.keyword);
  appendQueryParam(query, 'status', params.status);
  appendQueryParam(query, 'runnerType', params.runnerType);
  appendQueryParam(query, 'packageCode', params.packageCode);
  appendQueryParam(query, 'environmentCode', params.environmentCode);
  appendQueryParam(query, 'dateFrom', params.dateFrom);
  appendQueryParam(query, 'dateTo', params.dateTo);

  return request<PageResult<AutomationResultRunSummary>>(
    `/api/report/automation-results/page?${query.toString()}`,
    { method: 'GET' }
  );
}

export function listRecentAutomationResults(limit = 10) {
  const params = new URLSearchParams();
  params.append('limit', String(limit));
  return request<AutomationResultRecentRun[]>(
    `/api/report/automation-results/recent?${params.toString()}`,
    { method: 'GET' }
  );
}

export function listAutomationResultFacets() {
  return request<AutomationResultArchiveFacets>(
    '/api/report/automation-results/facets',
    { method: 'GET' }
  );
}

export function refreshAutomationResultIndex() {
  return request<AutomationResultArchiveRefreshResult>(
    '/api/report/automation-results/refresh-index',
    { method: 'POST' }
  );
}

export function getAutomationResultDetail(runId: string) {
  return request<AcceptanceRegistryRunSummary>(
    `/api/report/automation-results/${encodeURIComponent(runId)}`,
    { method: 'GET' }
  );
}

export function listAutomationResultEvidence(runId: string) {
  return request<AutomationResultEvidenceItem[]>(
    `/api/report/automation-results/${encodeURIComponent(runId)}/evidence`,
    { method: 'GET' }
  );
}

export function getAutomationResultEvidenceContent(runId: string, path: string) {
  const params = new URLSearchParams();
  params.append('path', path);
  return request<AutomationResultEvidenceContent>(
    `/api/report/automation-results/${encodeURIComponent(runId)}/evidence/content?${params.toString()}`,
    { method: 'GET' }
  );
}
