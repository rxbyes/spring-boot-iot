import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, PageResult, IdType } from '@/types/api';

export interface RiskGovernanceGapQuery {
  deviceCode?: string;
  riskPointId?: IdType;
  productId?: IdType;
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
  riskMetricId?: IdType | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  lastReportTime?: string | null;
}

export interface RiskMetricCatalogItem {
  id?: IdType | null;
  productId?: IdType | null;
  productModelId?: IdType | null;
  releaseBatchId?: IdType | null;
  contractIdentifier?: string | null;
  normativeIdentifier?: string | null;
  riskMetricCode?: string | null;
  riskMetricName?: string | null;
  riskCategory?: string | null;
  metricRole?: string | null;
  lifecycleStatus?: string | null;
  sourceScenarioCode?: string | null;
  metricUnit?: string | null;
  metricDimension?: string | null;
  thresholdType?: string | null;
  semanticDirection?: string | null;
  thresholdDirection?: string | null;
  trendEnabled?: number | null;
  gisEnabled?: number | null;
  insightEnabled?: number | null;
  analyticsEnabled?: number | null;
  enabled?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface RiskGovernanceCoverageOverview {
  productId?: IdType | null;
  contractPropertyCount?: number | null;
  publishedRiskMetricCount?: number | null;
  boundRiskMetricCount?: number | null;
  ruleCoveredRiskMetricCount?: number | null;
  linkageCoveredRiskMetricCount?: number | null;
  emergencyPlanCoveredRiskMetricCount?: number | null;
  linkagePlanCoveredRiskMetricCount?: number | null;
  contractMetricCoverageRate?: number | null;
  bindingCoverageRate?: number | null;
  ruleCoverageRate?: number | null;
  linkageCoverageRate?: number | null;
  emergencyPlanCoverageRate?: number | null;
  linkagePlanCoverageRate?: number | null;
}

export interface RiskGovernanceDashboardOverview {
  totalProductCount?: number | null;
  governedProductCount?: number | null;
  pendingProductGovernanceCount?: number | null;
  releasedProductCount?: number | null;
  pendingContractReleaseCount?: number | null;
  publishedRiskMetricCount?: number | null;
  boundRiskMetricCount?: number | null;
  ruleCoveredRiskMetricCount?: number | null;
  pendingRiskBindingCount?: number | null;
  pendingPolicyCount?: number | null;
  pendingThresholdPolicyCount?: number | null;
  pendingLinkageCount?: number | null;
  pendingEmergencyPlanCount?: number | null;
  pendingLinkagePlanCount?: number | null;
  pendingReplayCount?: number | null;
  rawStageProductCount?: number | null;
  rawStageVendorCount?: number | null;
  rawStageProductNames?: string[] | null;
  rawStageVendorNames?: string[] | null;
  governanceCompletionRate?: number | null;
  metricBindingCoverageRate?: number | null;
  policyCoverageRate?: number | null;
  thresholdPolicyCoverageRate?: number | null;
  linkageCoverageRate?: number | null;
  emergencyPlanCoverageRate?: number | null;
  linkagePlanCoverageRate?: number | null;
  averageOnboardingDurationHours?: number | null;
  bottleneckPendingProductGovernanceRate?: number | null;
  bottleneckPendingContractReleaseRate?: number | null;
  bottleneckPendingRiskBindingRate?: number | null;
  bottleneckPendingThresholdPolicyRate?: number | null;
  bottleneckPendingLinkagePlanRate?: number | null;
  bottleneckPendingReplayRate?: number | null;
}

export interface RiskGovernanceReplayGapSummary {
  missingBindingCount?: number | null;
  missingPolicyCount?: number | null;
  missingRiskMetricCount?: number | null;
}

export interface RiskGovernanceReplayBatchReconciliation {
  releaseBatchId?: IdType | null;
  approvalOrderId?: IdType | null;
  releaseStatus?: string | null;
  releaseReason?: string | null;
  rollbackTime?: string | null;
  snapshotAvailable?: boolean | null;
  consistent?: boolean | null;
  beforeApplyFieldCount?: number | null;
  afterApplyFieldCount?: number | null;
  currentFormalFieldCount?: number | null;
  batchCatalogMetricCount?: number | null;
  missingCurrentFieldCount?: number | null;
  extraCurrentFieldCount?: number | null;
  sampleMissingCurrentIdentifier?: string | null;
  sampleExtraCurrentIdentifier?: string | null;
}

export interface RiskGovernanceReplayChainStep {
  stepCode?: string | null;
  stepName?: string | null;
  status?: string | null;
  summary?: string | null;
  nextAction?: string | null;
}

export interface RiskGovernanceReplay {
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  releaseBatchId?: IdType | null;
  releaseScenarioCode?: string | null;
  matchedMessageCount?: number | null;
  matchedAccessErrorCount?: number | null;
  gapSummary?: RiskGovernanceReplayGapSummary | null;
  batchReconciliation?: RiskGovernanceReplayBatchReconciliation | null;
  replayChainSteps?: RiskGovernanceReplayChainStep[] | null;
}

export interface RiskGovernanceReplayQuery {
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  releaseBatchId?: IdType | null;
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

export function pageRiskMetricCatalogs(
  params: Pick<RiskGovernanceGapQuery, 'productId' | 'pageNum' | 'pageSize'> = {}
): Promise<ApiEnvelope<PageResult<RiskMetricCatalogItem>>> {
  const queryString = buildQueryString(params);
  const path = queryString
    ? `/api/risk-governance/metric-catalogs?${queryString}`
    : '/api/risk-governance/metric-catalogs';
  return request<PageResult<RiskMetricCatalogItem>>(path, { method: 'GET' });
}

export function getRiskGovernanceCoverageOverview(productId: IdType): Promise<ApiEnvelope<RiskGovernanceCoverageOverview>> {
  const queryString = buildQueryString({ productId });
  return request<RiskGovernanceCoverageOverview>(`/api/risk-governance/coverage-overview?${queryString}`, { method: 'GET' });
}

export function getRiskGovernanceDashboardOverview(): Promise<ApiEnvelope<RiskGovernanceDashboardOverview>> {
  return request<RiskGovernanceDashboardOverview>('/api/risk-governance/dashboard-overview', { method: 'GET' });
}

export function getRiskGovernanceReplay(
  params: RiskGovernanceReplayQuery
): Promise<ApiEnvelope<RiskGovernanceReplay>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/risk-governance/replay?${queryString}`
    : '/api/risk-governance/replay'
  return request<RiskGovernanceReplay>(path, { method: 'GET' })
}
