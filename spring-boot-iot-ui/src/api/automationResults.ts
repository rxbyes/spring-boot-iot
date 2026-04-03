import { request } from './request';
import type {
  AcceptanceRegistryRunSummary,
  AutomationResultEvidenceContent,
  AutomationResultEvidenceItem,
  AutomationResultRecentRun
} from '../types/automation';

export function listRecentAutomationResults(limit = 10) {
  const params = new URLSearchParams();
  params.append('limit', String(limit));
  return request<AutomationResultRecentRun[]>(
    `/api/report/automation-results/recent?${params.toString()}`,
    { method: 'GET' }
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
