/**
 * API 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

/**
 * 分页响应格式
 */
export interface PageResponse<T = any> {
  list: T[];
  total: number;
  pageSize: number;
  currentPage: number;
}

/**
 * 产品相关类型
 */
export interface Product {
  id: number;
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat: string;
  manufacturer: string;
  description: string;
  metadataJson?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type ProductObjectInsightMetricGroup = 'measure' | 'statusEvent' | 'runtime';

export interface ProductObjectInsightCustomMetricConfig {
  identifier: string;
  displayName: string;
  group: ProductObjectInsightMetricGroup;
  unit?: string | null;
  includeInTrend?: boolean | null;
  includeInExtension?: boolean | null;
  analysisTitle?: string | null;
  analysisTag?: string | null;
  analysisTemplate?: string | null;
  enabled?: boolean | null;
  sortNo?: number | null;
}

export interface ProductObjectInsightConfig {
  customMetrics?: ProductObjectInsightCustomMetricConfig[] | null;
}

export type ProductGovernanceCapabilityType = 'MONITORING' | 'COLLECTING' | 'WARNING' | 'VIDEO' | 'UNKNOWN';

export interface ProductGovernanceConfig {
  productCapabilityType?: ProductGovernanceCapabilityType | string | null;
}

export interface ProductMetadata {
  objectInsight?: ProductObjectInsightConfig | null;
  governance?: ProductGovernanceConfig | null;
}

export interface ProductAddPayload {
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat?: string;
  manufacturer?: string;
  description?: string;
  metadataJson?: string;
  status?: number;
}

export interface ProductModel {
  id: string | number;
  productId: string | number;
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  dataType?: string | null;
  specsJson?: string | null;
  eventType?: string | null;
  serviceInputJson?: string | null;
  serviceOutputJson?: string | null;
  sortNo?: number | null;
  requiredFlag?: number | null;
  description?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProductModelUpsertPayload {
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  dataType?: string;
  specsJson?: string | null;
  eventType?: string;
  serviceInputJson?: string;
  serviceOutputJson?: string;
  sortNo?: number;
  requiredFlag?: number;
  description?: string;
}

export type ProductModelGovernanceCompareStatus =
  | 'double_aligned'
  | 'manual_only'
  | 'runtime_only'
  | 'formal_exists'
  | 'suspected_conflict'
  | 'evidence_insufficient';

export type ProductModelGovernanceDecision = 'create' | 'update' | 'skip';

export type ProductModelGovernanceEvidenceOrigin =
  | 'normative'
  | 'sample_json'
  | 'manual_draft'
  | 'runtime'
  | 'formal';

export interface ProductModelGovernanceEvidence {
  modelId?: string | number | null;
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  evidenceOrigin?: ProductModelGovernanceEvidenceOrigin | null;
  dataType?: string | null;
  specsJson?: string | null;
  eventType?: string | null;
  serviceInputJson?: string | null;
  serviceOutputJson?: string | null;
  sortNo?: number | null;
  requiredFlag?: number | null;
  description?: string | null;
  groupKey?: string | null;
  unit?: string | null;
  normativeSource?: string | null;
  rawIdentifiers?: string[] | null;
  monitorContentCode?: string | null;
  monitorTypeCode?: string | null;
  sensorCode?: string | null;
  confidence?: number | null;
  needsReview?: boolean | null;
  candidateStatus?: string | null;
  reviewReason?: string | null;
  evidenceCount?: number | null;
  messageEvidenceCount?: number | null;
  lastReportTime?: string | null;
  sourceTables?: string[] | null;
}

export interface ProductModelGovernanceCompareRow {
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  normativeIdentifier?: string | null;
  normativeName?: string | null;
  riskReady?: boolean | null;
  rawIdentifiers?: string[] | null;
  manualCandidate?: ProductModelGovernanceEvidence | null;
  runtimeCandidate?: ProductModelGovernanceEvidence | null;
  formalModel?: ProductModelGovernanceEvidence | null;
  compareStatus: ProductModelGovernanceCompareStatus;
  riskFlags?: string[] | null;
  suggestedAction?: string | null;
  suspectedMatches?: string[] | null;
}

export interface ProductModelGovernanceSummary {
  manualCount?: number | null;
  runtimeCount?: number | null;
  formalCount?: number | null;
  propertyCount?: number | null;
  eventCount?: number | null;
  serviceCount?: number | null;
  doubleAlignedCount?: number | null;
  manualOnlyCount?: number | null;
  runtimeOnlyCount?: number | null;
  formalExistsCount?: number | null;
  suspectedConflictCount?: number | null;
  evidenceInsufficientCount?: number | null;
  lastComparedAt?: string | null;
}

export type ProductModelContractIdentifierMode = 'DIRECT' | 'FULL_PATH';

export interface ProductModelCandidateSummary {
  extractionMode?: string | null;
  sampleType?: string | null;
  sampleDeviceCode?: string | null;
  resolvedContractIdentifierMode?: ProductModelContractIdentifierMode | null;
  propertyEvidenceCount?: number | null;
  propertyCandidateCount?: number | null;
  eventEvidenceCount?: number | null;
  eventCandidateCount?: number | null;
  serviceEvidenceCount?: number | null;
  serviceCandidateCount?: number | null;
  needsReviewCount?: number | null;
  existingModelCount?: number | null;
  createdCount?: number | null;
  skippedCount?: number | null;
  conflictCount?: number | null;
  eventHint?: string | null;
  serviceHint?: string | null;
  ignoredFieldCount?: number | null;
  lastExtractedAt?: string | null;
}

export interface ProductModelGovernanceComparePayload {
  manualExtract?: {
    sampleType: 'business' | 'status';
    deviceStructure: 'single' | 'composite';
    contractIdentifierMode?: ProductModelContractIdentifierMode | null;
    samplePayload: string;
    parentDeviceCode?: string | null;
    relationMappings?: Array<{
      logicalChannelCode: string;
      childDeviceCode: string;
    }> | null;
  };
}

export interface ProductModelGovernanceCompareResult {
  productId: string | number;
  summary: ProductModelGovernanceSummary;
  manualSummary?: ProductModelCandidateSummary | null;
  runtimeSummary?: ProductModelCandidateSummary | null;
  formalSummary?: ProductModelGovernanceSummary | null;
  compareRows: ProductModelGovernanceCompareRow[];
}

export interface ProductModelGovernanceApplyItem {
  decision: ProductModelGovernanceDecision;
  targetModelId?: string | number;
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  dataType?: string;
  specsJson?: string;
  eventType?: string;
  serviceInputJson?: string;
  serviceOutputJson?: string;
  sortNo?: number;
  requiredFlag?: number;
  description?: string;
  compareStatus?: ProductModelGovernanceCompareStatus;
}

export interface ProductModelGovernanceApplyPayload {
  items: ProductModelGovernanceApplyItem[];
}

export interface ProductModelGovernanceApplyResult {
  createdCount?: number | null;
  updatedCount?: number | null;
  skippedCount?: number | null;
  conflictCount?: number | null;
  lastAppliedAt?: string | null;
  releaseBatchId?: string | number | null;
  approvalOrderId?: string | number | null;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | null;
  executionPending?: boolean | null;
}

export type VendorMetricMappingSuggestionStatus =
  | 'READY_TO_CREATE'
  | 'ALREADY_COVERED'
  | 'LOW_CONFIDENCE'
  | 'CONFLICTS_WITH_EXISTING'
  | `IGNORED_${string}`
  | string;

export type VendorMetricMappingRuleScopeType =
  | 'PRODUCT'
  | 'DEVICE_FAMILY'
  | 'SCENARIO'
  | 'PROTOCOL'
  | 'TENANT_DEFAULT';

export type VendorMetricMappingRuleLifecycleStatus =
  | 'DRAFT'
  | 'ACTIVE'
  | 'DISABLED'
  | string;

export interface VendorMetricMappingRuleSuggestion {
  id?: string | number | null;
  rawIdentifier: string;
  logicalChannelCode?: string | null;
  targetNormativeIdentifier: string;
  recommendedScopeType?: VendorMetricMappingRuleScopeType | null;
  status: VendorMetricMappingSuggestionStatus | string;
  confidence?: string | null;
  evidenceCount?: number | null;
  sampleValue?: string | null;
  valueType?: string | null;
  evidenceOrigin?: string | null;
  lastSeenTime?: string | null;
  reason?: string | null;
  existingRuleId?: string | number | null;
  existingTargetNormativeIdentifier?: string | null;
}

export interface VendorMetricMappingRuleSuggestionQuery {
  includeCovered?: boolean;
  includeIgnored?: boolean;
  minEvidenceCount?: number;
}

export interface VendorMetricMappingRule {
  id?: string | number | null;
  productId?: string | number | null;
  scopeType?: VendorMetricMappingRuleScopeType | null;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier?: string | null;
  logicalChannelCode?: string | null;
  relationConditionJson?: string | null;
  normalizationRuleJson?: string | null;
  targetNormativeIdentifier?: string | null;
  status?: VendorMetricMappingRuleLifecycleStatus | null;
  versionNo?: number | null;
  approvalOrderId?: string | number | null;
  createBy?: string | number | null;
  createTime?: string | null;
  updateBy?: string | number | null;
  updateTime?: string | null;
}

export interface VendorMetricMappingRuleCreatePayload {
  scopeType: VendorMetricMappingRuleScopeType;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier: string;
  logicalChannelCode?: string | null;
  relationConditionJson?: string | null;
  normalizationRuleJson?: string | null;
  targetNormativeIdentifier: string;
  status: VendorMetricMappingRuleLifecycleStatus;
}

export type RuntimeMetricDisplayRuleScopeType = VendorMetricMappingRuleScopeType;

export type RuntimeMetricDisplayRuleStatus = 'ACTIVE' | 'DISABLED' | string;

export interface RuntimeMetricDisplayRule {
  id?: string | number | null;
  productId?: string | number | null;
  scopeType?: RuntimeMetricDisplayRuleScopeType | null;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier?: string | null;
  displayName?: string | null;
  unit?: string | null;
  status?: RuntimeMetricDisplayRuleStatus | null;
  versionNo?: number | null;
  createBy?: string | number | null;
  createTime?: string | null;
  updateBy?: string | number | null;
  updateTime?: string | null;
}

export interface RuntimeMetricDisplayRuleUpsertPayload {
  scopeType: RuntimeMetricDisplayRuleScopeType;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier: string;
  displayName: string;
  unit?: string | null;
  status?: RuntimeMetricDisplayRuleStatus | null;
}

export type GovernanceApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface GovernanceApprovalOrder {
  id: string | number;
  actionCode?: string | null;
  actionName?: string | null;
  subjectType?: string | null;
  subjectId?: string | number | null;
  status?: GovernanceApprovalStatus | null;
  operatorUserId?: string | number | null;
  approverUserId?: string | number | null;
  payloadJson?: string | null;
  approvalComment?: string | null;
  approvedTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface GovernanceApprovalTransition {
  id?: string | number | null;
  fromStatus?: GovernanceApprovalStatus | null;
  toStatus?: GovernanceApprovalStatus | null;
  actorUserId?: string | number | null;
  transitionComment?: string | null;
  createTime?: string | null;
}

export interface GovernanceApprovalOrderDetail {
  order?: GovernanceApprovalOrder | null;
  transitions?: GovernanceApprovalTransition[] | null;
}

export interface GovernanceApprovalPageQuery {
  actionCode?: string | null;
  subjectType?: string | null;
  subjectId?: string | number | null;
  status?: GovernanceApprovalStatus | null;
  operatorUserId?: string | number | null;
  approverUserId?: string | number | null;
  pageNum?: number;
  pageSize?: number;
}

export interface GovernanceApprovalDecisionPayload {
  comment?: string | null;
}

export interface GovernanceApprovalResubmitPayload {
  approverUserId: string | number;
  comment?: string | null;
}

export interface DeviceRelation {
  id?: number | string | null;
  parentDeviceCode: string;
  logicalChannelCode: string;
  childDeviceCode: string;
  childProductId?: number | string | null;
  childProductKey?: string | null;
  relationType: string;
  canonicalizationStrategy: string;
  statusMirrorStrategy?: string | null;
  enabled?: number | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DeviceRelationUpsertPayload {
  parentDeviceCode: string;
  logicalChannelCode: string;
  childDeviceCode: string;
  relationType: string;
  canonicalizationStrategy: string;
  statusMirrorStrategy?: string | null;
  enabled?: number | null;
  remark?: string | null;
}

/**
 * 设备相关类型
 */
export interface Device {
  id?: number | string | null;
  productId?: number | string | null;
  gatewayId?: number | string | null;
  parentDeviceId?: number | string | null;
  sourceRecordId?: number | string | null;
  productKey?: string | null;
  productName?: string | null;
  gatewayDeviceCode?: string | null;
  gatewayDeviceName?: string | null;
  parentDeviceCode?: string | null;
  parentDeviceName?: string | null;
  deviceName: string;
  deviceCode: string;
  deviceSecret?: string | null;
  clientId?: string | null;
  username?: string | null;
  password?: string | null;
  protocolCode?: string | null;
  nodeType?: number | null;
  onlineStatus?: number | null;
  activateStatus?: number | null;
  deviceStatus?: number | null;
  registrationStatus?: number | null;
  assetSourceType?: string | null;
  firmwareVersion?: string | null;
  ipAddress?: string | null;
  address?: string | null;
  metadataJson?: string | null;
  lastFailureStage?: string | null;
  lastErrorMessage?: string | null;
  lastReportTopic?: string | null;
  lastTraceId?: string | null;
  lastPayload?: string | null;
  lastOnlineTime?: string | null;
  lastOfflineTime?: string | null;
  lastReportTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/**
 * 属性相关类型
 */
export interface DeviceProperty {
  identifier: string;
  propertyName: string;
  propertyValue: string;
  valueType: string;
  unit: string | null;
  timestamp: string | null;
}

/**
 * 消息日志相关类型
 */
export interface MessageLog {
  id: number;
  deviceCode: string;
  topic: string;
  messageType: string;
  payload: string;
  timestamp: string;
  createdAt: string;
}

/**
 * HTTP 请求配置
 */
export interface RequestConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  params?: Record<string, any>;
  data?: Record<string, any>;
  headers?: Record<string, string>;
}

/**
 * HTTP 响应
 */
export interface HttpResponse<T = any> {
  data: T;
  status: number;
  statusText: string;
  headers: Record<string, string>;
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  currentPage?: number;
  pageSize?: number;
  keyword?: string;
}

/**
 * 设备上报消息类型
 */
export interface DeviceUpMessage {
  messageType: 'property' | 'event' | 'status';
  properties?: Record<string, any>;
  events?: Record<string, any>;
  status?: {
    timestamp: string;
    value: string;
  };
}

/**
 * 设备状态
 */
export interface DeviceStatus {
  deviceCode: string;
  onlineStatus: number;
  lastOnlineTime: string | null;
  lastReportTime: string | null;
  lastSeenTime: string | null;
}
