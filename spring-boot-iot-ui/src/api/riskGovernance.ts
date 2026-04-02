import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, PageResult, IdType } from '@/types/api';

export interface RiskGovernanceGapQuery {
  deviceCode?: string;
  riskPointId?: IdType;
  pageNum?: number;
  pageSize?: number;
}

export interface RiskGovernanceGapItem {
  issueType?: string | null;
  issueLabel?: string | null;
  deviceId?: IdType | null;
  deviceCode?: string | null;
  deviceName?: string | null;
  riskPointId?: IdType | null;
  riskPointName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  lastReportTime?: string | null;
}

export function listMissingBindings(
  params: RiskGovernanceGapQuery = {}
): Promise<ApiEnvelope<PageResult<RiskGovernanceGapItem>>> {
  const queryString = buildQueryString(params);
  const path = queryString
    ? `/api/risk-governance/missing-bindings?${queryString}`
    : '/api/risk-governance/missing-bindings';
  return request<PageResult<RiskGovernanceGapItem>>(path, { method: 'GET' });
}

export function listMissingPolicies(
  params: RiskGovernanceGapQuery = {}
): Promise<ApiEnvelope<PageResult<RiskGovernanceGapItem>>> {
  const queryString = buildQueryString(params);
  const path = queryString
    ? `/api/risk-governance/missing-policies?${queryString}`
    : '/api/risk-governance/missing-policies';
  return request<PageResult<RiskGovernanceGapItem>>(path, { method: 'GET' });
}
