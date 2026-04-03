import { request } from './request';
import type {
  AcceptanceRegistryRunSummary,
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
