import { request } from './request';
import type {
  BusinessAcceptanceAccountTemplate,
  BusinessAcceptancePackage,
  BusinessAcceptanceResult,
  BusinessAcceptanceRunLaunch,
  BusinessAcceptanceRunRequest,
  BusinessAcceptanceRunStatus
} from '../types/businessAcceptance';

export function listBusinessAcceptancePackages() {
  return request<BusinessAcceptancePackage[]>('/api/report/business-acceptance/packages', {
    method: 'GET'
  });
}

export function listBusinessAcceptanceAccountTemplates() {
  return request<BusinessAcceptanceAccountTemplate[]>(
    '/api/report/business-acceptance/account-templates',
    {
      method: 'GET'
    }
  );
}

export function launchBusinessAcceptanceRun(body: BusinessAcceptanceRunRequest) {
  return request<BusinessAcceptanceRunLaunch>('/api/report/business-acceptance/runs', {
    method: 'POST',
    body
  });
}

export function getBusinessAcceptanceRunStatus(jobId: string) {
  return request<BusinessAcceptanceRunStatus>(
    `/api/report/business-acceptance/runs/${encodeURIComponent(jobId)}`,
    {
      method: 'GET'
    }
  );
}

export function getBusinessAcceptanceResult(runId: string, packageCode: string) {
  const query = new URLSearchParams();
  query.append('packageCode', packageCode);
  return request<BusinessAcceptanceResult>(
    `/api/report/business-acceptance/results/${encodeURIComponent(runId)}?${query.toString()}`,
    {
      method: 'GET'
    }
  );
}
