export interface ApiEnvelope<T> {
  code: number;
  msg: string;
  data: T;
}

export type IdType = string | number;

export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  records: T[];
}

export type DeviceOnboardingCaseCurrentStep =
  | 'PROTOCOL_GOVERNANCE'
  | 'PRODUCT_GOVERNANCE'
  | 'CONTRACT_RELEASE'
  | 'ACCEPTANCE';

export type DeviceOnboardingCaseStatus = 'BLOCKED' | 'IN_PROGRESS' | 'READY';

export type OnboardingTemplatePackStatus = 'ACTIVE' | 'INACTIVE';

export interface DeviceOnboardingAcceptanceSummary {
  jobId?: string | null;
  runId?: string | null;
  status?: 'RUNNING' | 'PASSED' | 'FAILED' | 'BLOCKED' | string | null;
  summary?: string | null;
  failedLayers?: string[] | null;
  jumpPath?: string | null;
}

export interface DeviceOnboardingCase {
  id: IdType;
  tenantId?: IdType | null;
  caseCode: string;
  caseName: string;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  protocolFamilyCode?: string | null;
  decryptProfileCode?: string | null;
  protocolTemplateCode?: string | null;
  templatePackId?: IdType | null;
  productId?: IdType | null;
  releaseBatchId?: IdType | null;
  deviceCode?: string | null;
  currentStep: DeviceOnboardingCaseCurrentStep;
  status: DeviceOnboardingCaseStatus;
  blockers: string[];
  acceptance?: DeviceOnboardingAcceptanceSummary | null;
  remark?: string | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
}

export interface DeviceOnboardingCasePageQuery {
  tenantId?: IdType | null;
  keyword?: string;
  status?: DeviceOnboardingCaseStatus | '';
  currentStep?: DeviceOnboardingCaseCurrentStep | '';
  pageNum?: number;
  pageSize?: number;
}

export interface DeviceOnboardingCaseUpsertPayload {
  tenantId?: IdType | null;
  caseCode: string;
  caseName: string;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  protocolFamilyCode?: string | null;
  decryptProfileCode?: string | null;
  protocolTemplateCode?: string | null;
  templatePackId?: IdType | null;
  productId?: IdType | null;
  releaseBatchId?: IdType | null;
  deviceCode?: string | null;
  remark?: string | null;
}

export type DeviceOnboardingCaseCreatePayload = DeviceOnboardingCaseUpsertPayload;

export type DeviceOnboardingCaseUpdatePayload = DeviceOnboardingCaseUpsertPayload;

export interface DeviceOnboardingCaseBatchCreatePayload {
  items: DeviceOnboardingCaseCreatePayload[];
}

export interface DeviceOnboardingCaseBatchTemplateApplyPayload {
  caseIds: IdType[];
  templatePackId: IdType;
}

export interface DeviceOnboardingCaseBatchStartAcceptancePayload {
  caseIds: IdType[];
}

export interface DeviceOnboardingCaseBatchSuccessItem {
  caseId?: IdType | null;
  caseCode?: string | null;
  caseName?: string | null;
  currentStep?: DeviceOnboardingCaseCurrentStep | string | null;
  status?: DeviceOnboardingCaseStatus | string | null;
  deviceCode?: string | null;
  acceptanceStatus?: string | null;
  acceptanceRunId?: string | null;
}

export interface DeviceOnboardingCaseBatchFailureItem {
  caseId?: IdType | null;
  caseCode?: string | null;
  caseName?: string | null;
  failureKey?: string | null;
  message: string;
}

export interface DeviceOnboardingCaseBatchFailureGroup {
  failureKey?: string | null;
  summary: string;
  count: number;
  caseCodes: string[];
}

export interface DeviceOnboardingCaseBatchResult {
  action?: 'BATCH_CREATE' | 'BATCH_APPLY_TEMPLATE' | 'BATCH_START_ACCEPTANCE' | string | null;
  requestedCount: number;
  successCount: number;
  failedCount: number;
  successItems: DeviceOnboardingCaseBatchSuccessItem[];
  failureItems: DeviceOnboardingCaseBatchFailureItem[];
  failureGroups: DeviceOnboardingCaseBatchFailureGroup[];
}

export interface OnboardingTemplatePack {
  id: IdType;
  tenantId?: IdType | null;
  packCode: string;
  packName: string;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  status: OnboardingTemplatePackStatus | string;
  versionNo?: number | null;
  protocolFamilyCode?: string | null;
  decryptProfileCode?: string | null;
  protocolTemplateCode?: string | null;
  defaultGovernanceConfigJson?: string | null;
  defaultInsightConfigJson?: string | null;
  defaultAcceptanceProfileJson?: string | null;
  description?: string | null;
  remark?: string | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
}

export interface OnboardingTemplatePackPageQuery {
  tenantId?: IdType | null;
  keyword?: string;
  status?: OnboardingTemplatePackStatus | '';
  scenarioCode?: string;
  deviceFamily?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface OnboardingTemplatePackUpsertPayload {
  tenantId?: IdType | null;
  packCode: string;
  packName: string;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  status?: OnboardingTemplatePackStatus | string | null;
  protocolFamilyCode?: string | null;
  decryptProfileCode?: string | null;
  protocolTemplateCode?: string | null;
  defaultGovernanceConfigJson?: string | null;
  defaultInsightConfigJson?: string | null;
  defaultAcceptanceProfileJson?: string | null;
  description?: string | null;
  remark?: string | null;
}

export type OnboardingTemplatePackCreatePayload = OnboardingTemplatePackUpsertPayload;

export type OnboardingTemplatePackUpdatePayload = OnboardingTemplatePackUpsertPayload;

export interface StatsBucket {
  label: string;
  value: string;
  count: number;
}

export interface SystemErrorStats {
  total: number;
  todayCount: number;
  mqttCount: number;
  systemCount: number;
  distinctTraceCount: number;
  distinctDeviceCount: number;
  topModules: StatsBucket[];
  topExceptionClasses: StatsBucket[];
  topErrorCodes: StatsBucket[];
}

export interface BusinessAuditStats {
  total: number;
  todayCount: number;
  successCount: number;
  failureCount: number;
  distinctUserCount: number;
  topModules: StatsBucket[];
  topUsers: StatsBucket[];
  topOperationTypes: StatsBucket[];
}

export interface MessageTraceStats {
  total: number;
  recentHourCount: number;
  recent24HourCount: number;
  distinctTraceCount: number;
  distinctDeviceCount: number;
  dispatchFailureCount: number;
  topMessageTypes: StatsBucket[];
  topProductKeys: StatsBucket[];
  topDeviceCodes: StatsBucket[];
  topTopics: StatsBucket[];
}

export interface MessageFlowStep {
  stage: string;
  handlerClass?: string | null;
  handlerMethod?: string | null;
  status?: string | null;
  costMs?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  summary?: Record<string, unknown> | null;
  errorClass?: string | null;
  errorMessage?: string | null;
  branch?: string | null;
}

export interface MessageFlowTimeline {
  traceId?: string | null;
  sessionId?: string | null;
  flowType?: string | null;
  status?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  topic?: string | null;
  protocolCode?: string | null;
  messageType?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  totalCostMs?: number | null;
  steps: MessageFlowStep[];
}

export interface ProtocolDecodeTimelineSummary {
  decryptedPayloadPreview?: string | null;
  decodedPayloadPreview?: Record<string, unknown> | null;
}

export interface ProtocolTemplateExecutionEvidence {
  templateCode?: string | null;
  logicalChannelCode?: string | null;
  childDeviceCode?: string | null;
  canonicalizationStrategy?: string | null;
  statusMirrorApplied?: boolean | null;
  parentRemovalKeys?: string[] | null;
}

export interface ProtocolTemplateEvidence {
  templateCodes?: string[] | null;
  executions?: ProtocolTemplateExecutionEvidence[] | null;
}

export interface DeviceUpProtocolMetadata {
  appId?: string | null;
  familyCodes?: string[] | null;
  normalizationStrategy?: string | null;
  timestampSource?: string | null;
  childSplitApplied?: boolean | null;
  routeType?: string | null;
  decryptedPayloadPreview?: string | null;
  decodedPayloadPreview?: Record<string, unknown> | null;
  templateEvidence?: ProtocolTemplateEvidence | null;
}

export interface MessageTraceDetail {
  id: IdType;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  messageType?: string | null;
  topic?: string | null;
  rawPayload?: string | null;
  decryptedPayload?: string | null;
  decodedPayload?: Record<string, unknown> | null;
  reportTime?: string | null;
  createTime?: string | null;
  protocolMetadata?: DeviceUpProtocolMetadata | null;
  timeline?: MessageFlowTimeline | null;
  timelineLookupError?: boolean | null;
}

export interface MessageFlowSession {
  sessionId?: string | null;
  transportMode?: string | null;
  status?: string | null;
  submittedAt?: string | null;
  traceId?: string | null;
  deviceCode?: string | null;
  topic?: string | null;
  correlationPending?: boolean | null;
  timeline?: MessageFlowTimeline | null;
}

export interface MessageFlowSessionCount {
  transportMode?: string | null;
  status?: string | null;
  count?: number | null;
}

export interface MessageFlowCorrelationCount {
  result?: string | null;
  count?: number | null;
}

export interface MessageFlowLookupCount {
  target?: string | null;
  result?: string | null;
  count?: number | null;
}

export interface MessageFlowStageMetric {
  stage: string;
  count?: number | null;
  failureCount?: number | null;
  skippedCount?: number | null;
  avgCostMs?: number | null;
  p95CostMs?: number | null;
  maxCostMs?: number | null;
}

export interface MessageFlowOpsOverview {
  runtimeStartedAt?: string | null;
  sessionCounts: MessageFlowSessionCount[];
  correlationCounts: MessageFlowCorrelationCount[];
  lookupCounts: MessageFlowLookupCount[];
  stageMetrics: MessageFlowStageMetric[];
}

export interface MessageFlowRecentSession {
  sessionId?: string | null;
  traceId?: string | null;
  transportMode?: string | null;
  status?: string | null;
  submittedAt?: string | null;
  deviceCode?: string | null;
  topic?: string | null;
  correlationPending?: boolean | null;
  timelineAvailable?: boolean | null;
}

export interface MessageFlowSubmitResult {
  sessionId?: string | null;
  traceId?: string | null;
  status?: string | null;
  timelineAvailable?: boolean | null;
  correlationPending?: boolean | null;
}

export interface DeviceAccessErrorStats {
  total: number;
  recentHourCount: number;
  recent24HourCount: number;
  distinctTraceCount: number;
  distinctDeviceCount: number;
  topFailureStages: StatsBucket[];
  topErrorCodes: StatsBucket[];
  topExceptionClasses: StatsBucket[];
  topProtocolCodes: StatsBucket[];
  topTopics: StatsBucket[];
}

export interface Product {
  id: IdType;
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat?: string | null;
  manufacturer?: string | null;
  description?: string | null;
  metadataJson?: string | null;
  status?: number | null;
  deviceCount?: number | null;
  onlineDeviceCount?: number | null;
  lastReportTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  // 活跃度统计（详情页增强数据）
  todayActiveCount?: number | null; // 今日活跃设备数
  sevenDaysActiveCount?: number | null; // 7日活跃设备数
  thirtyDaysActiveCount?: number | null; // 30日活跃设备数
  avgOnlineDuration?: number | null; // 在线时长（分钟，有会话明细时返回）
  maxOnlineDuration?: number | null; // 在线时长（分钟，有会话明细时返回）
}

export interface ProductOverviewSummary {
  productId: IdType;
  productKey: string;
  productName?: string | null;
  protocolCode?: string | null;
  nodeType?: number | null;
  dataFormat?: string | null;
  manufacturer?: string | null;
  description?: string | null;
  status?: number | null;
  deviceCount?: number | null;
  onlineDeviceCount?: number | null;
  lastReportTime?: string | null;
  formalFieldCount?: number | null;
  latestReleaseBatchId?: IdType | null;
  latestReleasedFieldCount?: number | null;
  latestReleaseStatus?: string | null;
  latestReleaseCreateTime?: string | null;
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

export interface DeviceCapabilityParamSchemaField {
  type?: 'string' | 'integer' | string | null;
  label?: string | null;
  required?: boolean | null;
  min?: number | string | null;
  max?: number | string | null;
}

export type DeviceCapabilityType = 'COLLECTING' | 'MONITORING' | 'WARNING' | 'VIDEO' | 'UNKNOWN';

export interface DeviceCapability {
  code: string;
  name: string;
  group?: string | null;
  enabled?: boolean | null;
  requiresOnline?: boolean | null;
  disabledReason?: string | null;
  paramsSchema?: Record<string, DeviceCapabilityParamSchemaField> | null;
}

export interface DeviceCapabilityOverview {
  deviceCode: string;
  productId?: IdType | null;
  productKey?: string | null;
  productCapabilityType?: DeviceCapabilityType | string | null;
  subType?: string | null;
  onlineExecutable?: boolean | null;
  disabledReason?: string | null;
  capabilities: DeviceCapability[];
}

export interface DeviceCapabilityExecutePayload {
  params: Record<string, unknown>;
}

export interface DeviceCapabilityExecuteResult {
  commandId: string;
  deviceCode: string;
  capabilityCode: string;
  status: string;
  topic?: string | null;
  sentAt?: string | null;
}

export interface CommandRecordPageItem {
  id: IdType;
  commandId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  topic?: string | null;
  commandType?: string | null;
  serviceIdentifier?: string | null;
  status?: string | null;
  sendTime?: string | null;
  ackTime?: string | null;
  timeoutTime?: string | null;
  errorMessage?: string | null;
  replyPayload?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProductMetadata {
  objectInsight?: ProductObjectInsightConfig | null;
  governance?: ProductGovernanceConfig | null;
}

export type ProductModelType = 'property' | 'event' | 'service';

export interface ProductModel {
  id: IdType;
  productId: IdType;
  modelType: ProductModelType;
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

export interface ProductModelCandidate {
  modelType: ProductModelType;
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
  groupKey?: string | null;
  confidence?: number | null;
  needsReview?: boolean | null;
  candidateStatus?: string | null;
  reviewReason?: string | null;
  evidenceCount?: number | null;
  messageEvidenceCount?: number | null;
  lastReportTime?: string | null;
  sourceTables?: string[] | null;
  protocolTemplateEvidence?: ProductModelProtocolTemplateEvidence | null;
}

export interface ProductModelProtocolTemplateEvidence {
  templateCodes?: string[] | null;
  logicalChannelCodes?: string[] | null;
  childDeviceCodes?: string[] | null;
  canonicalizationStrategies?: string[] | null;
  statusMirrorApplied?: boolean | null;
  parentRemovalKeys?: string[] | null;
  templateExecutionCount?: number | null;
  decodeFailureCount?: number | null;
}

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

export interface ProductModelCandidateResult {
  productId: IdType;
  summary: ProductModelCandidateSummary;
  propertyCandidates: ProductModelCandidate[];
  eventCandidates: ProductModelCandidate[];
  serviceCandidates: ProductModelCandidate[];
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
  modelId?: IdType | null;
  modelType: ProductModelType;
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
  protocolTemplateEvidence?: ProductModelProtocolTemplateEvidence | null;
}

export interface ProductModelGovernanceCompareRow {
  modelType: ProductModelType;
  identifier: string;
  normativeIdentifier?: string | null;
  normativeName?: string | null;
  normativeMatchStatus?: string | null;
  normativeMatchSource?: string | null;
  normativeMatchReason?: string | null;
  normativeCandidates?: string[] | null;
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

export interface ProductModelGovernanceCompareResult {
  productId: IdType;
  summary: ProductModelGovernanceSummary;
  manualSummary?: ProductModelCandidateSummary | null;
  runtimeSummary?: ProductModelCandidateSummary | null;
  formalSummary?: ProductModelGovernanceSummary | null;
  compareRows: ProductModelGovernanceCompareRow[];
}

export type ProductModelContractIdentifierMode = 'DIRECT' | 'FULL_PATH';

export interface ProductModelGovernanceManualExtractPayload {
  sampleType: ProductModelManualSampleType;
  deviceStructure: ProductModelGovernanceDeviceStructure;
  contractIdentifierMode?: ProductModelContractIdentifierMode | null;
  samplePayload: string;
  parentDeviceCode?: string | null;
  relationMappings?: ProductModelGovernanceRelationMappingPayload[] | null;
}

export type ProductModelGovernanceDeviceStructure = 'single' | 'composite';

export interface ProductModelGovernanceRelationMappingPayload {
  logicalChannelCode: string;
  childDeviceCode: string;
  canonicalizationStrategy?: string | null;
  statusMirrorStrategy?: string | null;
}

export interface ProductModelGovernanceComparePayload {
  manualExtract?: ProductModelGovernanceManualExtractPayload;
}

export interface ProductModelGovernanceApplyItem {
  decision: ProductModelGovernanceDecision;
  targetModelId?: IdType;
  modelType: ProductModelType;
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
  submittedItemCount?: number | null;
  createdCount?: number | null;
  updatedCount?: number | null;
  skippedCount?: number | null;
  conflictCount?: number | null;
  lastAppliedAt?: string | null;
  releaseBatchId?: IdType | null;
  approvalOrderId?: IdType | null;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | null;
  executionPending?: boolean | null;
  appliedItems?: ProductModelGovernanceAppliedItem[] | null;
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

export type VendorMetricMappingRulePublishedStatus =
  | 'PUBLISHED'
  | 'ROLLED_BACK'
  | string;

export interface VendorMetricMappingRuleSuggestion {
  id?: IdType | null;
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
  existingRuleId?: IdType | null;
  existingTargetNormativeIdentifier?: string | null;
}

export interface VendorMetricMappingRuleSuggestionQuery {
  includeCovered?: boolean;
  includeIgnored?: boolean;
  minEvidenceCount?: number;
}

export interface VendorMetricMappingRule {
  id?: IdType | null;
  productId?: IdType | null;
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
  approvalOrderId?: IdType | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
  coveredByFormalField?: boolean | null;
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

export interface VendorMetricMappingRuleLedgerRow {
  ruleId?: IdType | null;
  productId?: IdType | null;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier?: string | null;
  targetNormativeIdentifier?: string | null;
  scopeType?: VendorMetricMappingRuleScopeType | null;
  draftStatus?: VendorMetricMappingRuleLifecycleStatus | null;
  draftVersionNo?: number | null;
  publishedStatus?: VendorMetricMappingRulePublishedStatus | null;
  publishedVersionNo?: number | null;
  latestApprovalOrderId?: IdType | null;
  publishedSource?: string | null;
  logicalChannelCode?: string | null;
  coveredByFormalField?: boolean | null;
}

export interface VendorMetricMappingRuleHitPreview {
  matched?: boolean | null;
  hitSource?: string | null;
  ruleId?: IdType | null;
  rawIdentifier?: string | null;
  logicalChannelCode?: string | null;
  targetNormativeIdentifier?: string | null;
  publishedVersionNo?: number | null;
  approvalOrderId?: IdType | null;
}

export interface VendorMetricMappingRuleBatchStatusPayload {
  ruleIds: IdType[];
  targetStatus: VendorMetricMappingRuleLifecycleStatus | string;
}

export interface VendorMetricMappingRuleBatchStatusResult {
  requestedCount?: number | null;
  matchedCount?: number | null;
  changedCount?: number | null;
  targetStatus?: string | null;
}

export interface VendorMetricMappingRuleReplayPayload {
  rawIdentifier: string;
  logicalChannelCode?: string | null;
  sampleValue?: string | null;
}

export interface VendorMetricMappingRuleReplay {
  matched?: boolean | null;
  hitSource?: string | null;
  matchedScopeType?: VendorMetricMappingRuleScopeType | string | null;
  ruleId?: IdType | null;
  rawIdentifier?: string | null;
  logicalChannelCode?: string | null;
  targetNormativeIdentifier?: string | null;
  canonicalIdentifier?: string | null;
  sampleValue?: string | null;
}

export type RuntimeMetricDisplayRuleScopeType = VendorMetricMappingRuleScopeType;

export type RuntimeMetricDisplayRuleStatus = 'ACTIVE' | 'DISABLED' | string;

export interface RuntimeMetricDisplayRule {
  id?: IdType | null;
  productId?: IdType | null;
  scopeType?: RuntimeMetricDisplayRuleScopeType | null;
  protocolCode?: string | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  rawIdentifier?: string | null;
  displayName?: string | null;
  unit?: string | null;
  status?: RuntimeMetricDisplayRuleStatus | null;
  versionNo?: number | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
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

export interface NormativeMetricDefinitionImportItem {
  id?: IdType | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  identifier?: string | null;
  displayName?: string | null;
  unit?: string | null;
  precisionDigits?: number | null;
  monitorContentCode?: string | null;
  monitorTypeCode?: string | null;
  riskEnabled?: number | null;
  trendEnabled?: number | null;
  metricDimension?: string | null;
  thresholdType?: string | null;
  semanticDirection?: string | null;
  gisEnabled?: number | null;
  insightEnabled?: number | null;
  analyticsEnabled?: number | null;
  status?: string | null;
  versionNo?: number | null;
  metadataJson?: unknown;
}

export interface NormativeMetricDefinitionImportPayload {
  items: NormativeMetricDefinitionImportItem[];
}

export interface NormativeMetricDefinitionImportRow {
  rowIndex?: number | null;
  id?: IdType | null;
  scenarioCode?: string | null;
  deviceFamily?: string | null;
  identifier?: string | null;
  displayName?: string | null;
  monitorContentCode?: string | null;
  monitorTypeCode?: string | null;
  fallbackKey?: string | null;
  action?: string | null;
  status?: string | null;
  message?: string | null;
}

export interface NormativeMetricDefinitionImportResult {
  totalCount?: number | null;
  readyCount?: number | null;
  conflictCount?: number | null;
  appliedCount?: number | null;
  rows?: NormativeMetricDefinitionImportRow[] | null;
}

export interface ProtocolGovernancePageQuery {
  keyword?: string | null;
  status?: string | null;
  pageNum?: number;
  pageSize?: number;
}

export interface ProtocolGovernanceBatchSubmitPayload {
  recordIds: IdType[];
  submitReason?: string | null;
}

export interface ProtocolGovernanceBatchSubmitResultItem {
  recordId?: IdType | null;
  success?: boolean | null;
  approvalOrderId?: IdType | null;
  errorMessage?: string | null;
}

export interface ProtocolGovernanceBatchSubmitResult {
  totalCount?: number | null;
  submittedCount?: number | null;
  failedCount?: number | null;
  items?: ProtocolGovernanceBatchSubmitResultItem[] | null;
}

export interface ProtocolFamilyDefinitionUpsertPayload {
  familyCode: string;
  protocolCode: string;
  displayName: string;
  decryptProfileCode?: string | null;
  signAlgorithm?: string | null;
  normalizationStrategy?: string | null;
}

export interface ProtocolFamilyDefinition {
  id?: IdType | null;
  familyCode?: string | null;
  protocolCode?: string | null;
  displayName?: string | null;
  decryptProfileCode?: string | null;
  signAlgorithm?: string | null;
  normalizationStrategy?: string | null;
  status?: string | null;
  versionNo?: number | null;
  publishedStatus?: string | null;
  publishedVersionNo?: number | null;
  approvalOrderId?: IdType | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
}

export interface ProtocolDecryptProfileUpsertPayload {
  profileCode: string;
  algorithm: string;
  merchantSource: string;
  merchantKey: string;
  transformation?: string | null;
  signatureSecret?: string | null;
}

export interface ProtocolDecryptProfile {
  id?: IdType | null;
  profileCode?: string | null;
  algorithm?: string | null;
  merchantSource?: string | null;
  merchantKey?: string | null;
  transformation?: string | null;
  signatureSecret?: string | null;
  status?: string | null;
  versionNo?: number | null;
  publishedStatus?: string | null;
  publishedVersionNo?: number | null;
  approvalOrderId?: IdType | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
}

export interface ProtocolDecryptPreviewPayload {
  familyCode?: string | null;
  protocolCode?: string | null;
  appId?: string | null;
}

export interface ProtocolDecryptPreview {
  matched?: boolean | null;
  hitSource?: string | null;
  familyCode?: string | null;
  resolvedProfileCode?: string | null;
  algorithm?: string | null;
  merchantSource?: string | null;
  merchantKey?: string | null;
  transformation?: string | null;
}

export interface ProtocolGovernanceReplayPayload {
  familyCode?: string | null;
  protocolCode?: string | null;
  appId?: string | null;
}

export interface ProtocolGovernanceReplay {
  matched?: boolean | null;
  hitSource?: string | null;
  familyCode?: string | null;
  protocolCode?: string | null;
  appId?: string | null;
  resolvedProfileCode?: string | null;
  algorithm?: string | null;
  merchantSource?: string | null;
  merchantKey?: string | null;
  transformation?: string | null;
}

export interface ProtocolTemplateDefinitionUpsertPayload {
  templateCode: string;
  familyCode: string;
  protocolCode: string;
  displayName: string;
  expressionJson: string;
  outputMappingJson?: string | null;
}

export interface ProtocolTemplateDefinition {
  id?: IdType | null;
  templateCode?: string | null;
  familyCode?: string | null;
  protocolCode?: string | null;
  displayName?: string | null;
  expressionJson?: string | null;
  outputMappingJson?: string | null;
  status?: string | null;
  versionNo?: number | null;
  publishedStatus?: string | null;
  publishedVersionNo?: number | null;
  approvalOrderId?: IdType | null;
  createBy?: IdType | null;
  createTime?: string | null;
  updateBy?: IdType | null;
  updateTime?: string | null;
}

export interface ProtocolTemplateSubmitPayload {
  submitReason?: string | null;
}

export interface ProtocolTemplateReplayPayload {
  templateCode: string;
  payloadJson: string;
}

export interface ProtocolTemplateReplayChild {
  logicalChannelCode?: string | null;
  childProperties?: Record<string, unknown> | null;
  canonicalizationStrategy?: string | null;
  statusMirrorApplied?: boolean | null;
  rawPayload?: string | null;
}

export interface ProtocolTemplateReplay {
  templateCode?: string | null;
  resolvedTemplateCode?: string | null;
  matched?: boolean | null;
  summary?: string | null;
  extractedChildren?: ProtocolTemplateReplayChild[] | null;
}

export interface ProductModelGovernanceAppliedItem {
  modelType?: ProductModelType | null;
  identifier?: string | null;
  decision?: ProductModelGovernanceDecision | null;
  templateCodes?: string[] | null;
  canonicalizationStrategies?: string[] | null;
  childDeviceCodes?: string[] | null;
}

export type GovernanceApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface GovernanceSubmissionResult {
  workItemId?: IdType | null;
  approvalOrderId?: IdType | null;
  approvalStatus?: GovernanceApprovalStatus | null;
  executionStatus?: 'DIRECT_APPLIED' | 'PENDING_APPROVAL' | 'REJECTED' | 'CANCELLED' | null;
}

export interface GovernanceApprovalOrder {
  id: IdType;
  actionCode?: string | null;
  actionName?: string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  workItemId?: IdType | null;
  status?: GovernanceApprovalStatus | null;
  operatorUserId?: IdType | null;
  approverUserId?: IdType | null;
  payloadJson?: string | null;
  approvalComment?: string | null;
  approvedTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface GovernanceApprovalTransition {
  id?: IdType | null;
  fromStatus?: GovernanceApprovalStatus | null;
  toStatus?: GovernanceApprovalStatus | null;
  actorUserId?: IdType | null;
  transitionComment?: string | null;
  createTime?: string | null;
}

export interface GovernanceApprovalOrderDetail {
  order?: GovernanceApprovalOrder | null;
  transitions?: GovernanceApprovalTransition[] | null;
}

export interface GovernanceSimulationResult {
  orderId?: IdType | null;
  workItemId?: IdType | null;
  actionCode?: string | null;
  executable?: boolean | null;
  affectedCount?: number | null;
  affectedTypes?: string[] | null;
  rollbackable?: boolean | null;
  rollbackPlanSummary?: string | null;
  recommendation?: GovernanceRecommendationSnapshot | null;
  impact?: GovernanceImpactSnapshot | null;
  rollback?: GovernanceRollbackSnapshot | null;
  autoDraftEligible?: boolean | null;
  autoDraftComment?: string | null;
}

export interface GovernanceApprovalPageQuery {
  actionCode?: string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  status?: GovernanceApprovalStatus | null;
  operatorUserId?: IdType | null;
  approverUserId?: IdType | null;
  pageNum?: number;
  pageSize?: number;
}

export interface GovernanceApprovalDecisionPayload {
  comment?: string | null;
}

export interface GovernanceApprovalResubmitPayload {
  approverUserId: IdType;
  comment?: string | null;
}

export type GovernanceWorkItemStatus = 'OPEN' | 'ACKED' | 'BLOCKED' | 'RESOLVED' | 'CLOSED';
export type GovernanceWorkItemExecutionStatus =
  | 'PENDING_APPROVAL'
  | 'EXECUTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'IN_PROGRESS'
  | 'REPLAY_REQUIRED'
  | 'RESOLVED'
  | 'CLOSED';

export interface GovernanceEvidenceItem {
  evidenceType?: string | null;
  title?: string | null;
  summary?: string | null;
  sourceType?: string | null;
  sourceId?: string | null;
}

export interface GovernanceRecommendationSnapshot {
  recommendationType?: 'PROMOTE' | 'PUBLISH' | 'CREATE_POLICY' | 'REPLAY' | 'IGNORE' | string | null;
  confidence?: number | null;
  reasonCodes?: string[] | null;
  suggestedAction?: string | null;
  evidenceItems?: GovernanceEvidenceItem[] | null;
}

export interface GovernanceImpactSnapshot {
  affectedCount?: number | null;
  affectedTypes?: string[] | null;
  rollbackable?: boolean | null;
  rollbackPlanSummary?: string | null;
}

export interface GovernanceRollbackSnapshot {
  rollbackable?: boolean | null;
  rollbackPlanSummary?: string | null;
}

export interface GovernanceWorkItem {
  id: IdType;
  workItemCode?: string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  productId?: IdType | null;
  riskMetricId?: IdType | null;
  releaseBatchId?: IdType | null;
  approvalOrderId?: IdType | null;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  workStatus?: GovernanceWorkItemStatus | null;
  priorityLevel?: string | null;
  assigneeUserId?: IdType | null;
  sourceStage?: string | null;
  blockingReason?: string | null;
  snapshotJson?: string | null;
  taskCategory?: string | null;
  domainCode?: string | null;
  actionCode?: string | null;
  executionStatus?: GovernanceWorkItemExecutionStatus | string | null;
  recommendationSnapshotJson?: string | null;
  evidenceSnapshotJson?: string | null;
  impactSnapshotJson?: string | null;
  rollbackSnapshotJson?: string | null;
  recommendation?: GovernanceRecommendationSnapshot | null;
  impact?: GovernanceImpactSnapshot | null;
  rollback?: GovernanceRollbackSnapshot | null;
  dueTime?: string | null;
  resolvedTime?: string | null;
  closedTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface GovernanceWorkItemPageQuery {
  workItemCode?: string | null;
  workStatus?: GovernanceWorkItemStatus | string | null;
  executionStatus?: GovernanceWorkItemExecutionStatus | string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  productId?: IdType | null;
  riskMetricId?: IdType | null;
  assigneeUserId?: IdType | null;
  keyword?: string | null;
  pageNum?: number;
  pageSize?: number;
}

export interface GovernanceWorkItemTransitionPayload {
  comment?: string | null;
}

export interface GovernanceReplayFeedbackPayload {
  workItemId?: IdType | null;
  approvalOrderId?: IdType | null;
  releaseBatchId?: IdType | null;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  recommendedDecision?: string | null;
  adoptedDecision: string;
  executionOutcome: string;
  rootCauseCode: string;
  operatorSummary?: string | null;
}

export interface GovernanceDecisionContext {
  workItemId?: IdType | null;
  priorityLevel?: string | null;
  priorityScore?: number | null;
  problemSummary?: string | null;
  reasonCodes?: string[] | null;
  affectedModules?: string[] | null;
  affectedCount?: number | null;
  recommendedAction?: string | null;
  rollbackable?: boolean | null;
  rollbackPlanSummary?: string | null;
}

export type GovernanceOpsAlertStatus = 'OPEN' | 'ACKED' | 'SUPPRESSED' | 'RESOLVED' | 'CLOSED';

export interface GovernanceOpsAlert {
  id: IdType;
  alertType?: string | null;
  alertCode?: string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  productId?: IdType | null;
  riskMetricId?: IdType | null;
  releaseBatchId?: IdType | null;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  alertStatus?: GovernanceOpsAlertStatus | null;
  severityLevel?: string | null;
  affectedCount?: number | null;
  alertTitle?: string | null;
  alertMessage?: string | null;
  dimensionKey?: string | null;
  dimensionLabel?: string | null;
  sourceStage?: string | null;
  snapshotJson?: string | null;
  assigneeUserId?: IdType | null;
  recommendation?: GovernanceRecommendationSnapshot | null;
  impact?: GovernanceImpactSnapshot | null;
  rollback?: GovernanceRollbackSnapshot | null;
  firstSeenTime?: string | null;
  lastSeenTime?: string | null;
  resolvedTime?: string | null;
  closedTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface GovernanceOpsAlertPageQuery {
  alertType?: string | null;
  alertStatus?: GovernanceOpsAlertStatus | string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  productId?: IdType | null;
  riskMetricId?: IdType | null;
  severityLevel?: string | null;
  assigneeUserId?: IdType | null;
  pageNum?: number;
  pageSize?: number;
}

export interface GovernanceOpsAlertTransitionPayload {
  comment?: string | null;
}

export interface ProductModelCandidateConfirmItem {
  modelType: ProductModelType;
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
}

export interface ProductModelCandidateConfirmPayload {
  items: ProductModelCandidateConfirmItem[];
}

export type ProductModelManualSampleType = 'business' | 'status';

export interface Device {
  id?: IdType | null;
  productId?: IdType | null;
  gatewayId?: IdType | null;
  parentDeviceId?: IdType | null;
  sourceRecordId?: IdType | null;
  orgId?: IdType | null;
  orgName?: string | null;
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

export interface DeviceThresholdRule {
  ruleId?: IdType | null;
  ruleName?: string | null;
  ruleScope?: string | null;
  ruleScopeText?: string | null;
  expression?: string | null;
  alarmLevel?: string | null;
  sourceLabel?: string | null;
  targetLabel?: string | null;
  riskPointDeviceId?: IdType | null;
}

export interface DeviceThresholdMetricItem {
  riskMetricId?: IdType | null;
  metricIdentifier: string;
  metricName?: string | null;
  effectiveRules: DeviceThresholdRule[];
  bindingRules: DeviceThresholdRule[];
  deviceRules: DeviceThresholdRule[];
  productRules: DeviceThresholdRule[];
  fallbackRules: DeviceThresholdRule[];
}

export interface DeviceThresholdOverview {
  deviceId: IdType;
  deviceCode?: string | null;
  deviceName?: string | null;
  productId?: IdType | null;
  productName?: string | null;
  matchedMetricCount: number;
  missingMetricCount: number;
  items: DeviceThresholdMetricItem[];
}

export interface DeviceOnboardingSuggestion {
  traceId: string;
  deviceCode?: string | null;
  deviceName?: string | null;
  assetSourceType?: string | null;
  productKey?: string | null;
  protocolCode?: string | null;
  lastFailureStage?: string | null;
  lastErrorMessage?: string | null;
  lastReportTopic?: string | null;
  lastPayload?: string | null;
  recommendedProductId?: IdType | null;
  recommendedProductKey?: string | null;
  recommendedProductName?: string | null;
  recommendedFamilyCode?: string | null;
  recommendedFamilyName?: string | null;
  recommendedDecryptProfileCode?: string | null;
  recommendedTemplateCode?: string | null;
  recommendedTemplateName?: string | null;
  suggestionStatus?: string | null;
  ruleGaps: string[];
}

export interface DeviceOnboardingBatchActivatePayload {
  traceIds: string[];
  confirmed: boolean;
}

export interface DeviceOnboardingBatchError {
  traceId?: string | null;
  deviceCode?: string | null;
  message: string;
}

export interface DeviceOnboardingBatchResult {
  requestedCount: number;
  activatedCount: number;
  rejectedCount: number;
  activatedTraceIds: string[];
  activatedDeviceCodes: string[];
  errors: DeviceOnboardingBatchError[];
}

export interface DeviceOption {
  id: IdType;
  productId?: IdType | null;
  gatewayId?: IdType | null;
  parentDeviceId?: IdType | null;
  orgId?: IdType | null;
  orgName?: string | null;
  productKey?: string | null;
  productName?: string | null;
  deviceCode: string;
  deviceName: string;
  nodeType?: number | null;
  onlineStatus?: number | null;
  deviceStatus?: number | null;
  deviceCapabilityType?: string | null;
  supportsMetricBinding?: boolean | null;
  aiEventExpandable?: boolean | null;
}

export interface DeviceMetricOption {
  identifier: string;
  name: string;
  dataType?: string | null;
  riskMetricId?: IdType | null;
}

export interface DeviceProperty {
  id: IdType;
  identifier: string;
  propertyName?: string | null;
  propertyValue?: string | null;
  valueType?: string | null;
  unit?: string | null;
  reportTime?: string | null;
  updateTime?: string | null;
}

export type DeviceTopologyRole = 'COLLECTOR_PARENT' | 'COLLECTOR_CHILD' | 'STANDALONE';

export interface DevicePropertyInsight {
  topologyRole?: DeviceTopologyRole | null;
  properties: DeviceProperty[];
}

export interface CollectorChildInsightMetric {
  identifier: string;
  displayName?: string | null;
  propertyValue?: string | null;
  unit?: string | null;
  recommended?: boolean | null;
  reportTime?: string | null;
}

export interface CollectorChildInsightChild {
  logicalChannelCode: string;
  childDeviceCode: string;
  childDeviceName?: string | null;
  childProductKey?: string | null;
  collectorLinkState: string;
  sensorStateValue?: string | null;
  sensorStateHealth?: 'REPORTED_NORMAL' | 'REPORTED_ABNORMAL' | 'MISSING' | 'STALE' | null;
  lastReportTime?: string | null;
  recommendedMetricIdentifiers?: string[] | null;
  metrics: CollectorChildInsightMetric[];
}

export interface CollectorChildInsightOverview {
  parentDeviceCode: string;
  parentOnlineStatus?: number | null;
  childCount: number;
  reachableChildCount: number;
  sensorStateReportedCount: number;
  missingChildCount?: number | null;
  staleChildCount?: number | null;
  recommendedMetricCount?: number | null;
  children: CollectorChildInsightChild[];
}

export interface DeviceMessageLog {
  id: IdType;
  deviceId?: number | null;
  productId?: number | null;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  messageType?: string | null;
  topic?: string | null;
  payload?: string | null;
  reportTime?: string | null;
  createTime?: string | null;
}

export interface DeviceAccessErrorLog {
  id: IdType;
  tenantId?: number | null;
  traceId?: string | null;
  protocolCode?: string | null;
  requestMethod?: string | null;
  failureStage?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  gatewayDeviceCode?: string | null;
  subDeviceCode?: string | null;
  topicRouteType?: string | null;
  messageType?: string | null;
  topic?: string | null;
  clientId?: string | null;
  payloadSize?: number | null;
  payloadEncoding?: string | null;
  payloadTruncated?: number | null;
  rawPayload?: string | null;
  errorCode?: string | null;
  exceptionClass?: string | null;
  errorMessage?: string | null;
  contractSnapshot?: string | null;
  createTime?: string | null;
}

export interface DeviceFileSnapshot {
  transferId: string;
  deviceCode: string;
  productId?: number | null;
  messageType?: string | null;
  dataSetId?: string | null;
  fileType?: string | null;
  description?: string | null;
  timestamp?: string | null;
  binaryLength?: number | null;
  binaryBase64?: string | null;
  descriptor?: Record<string, unknown> | null;
  completed?: boolean | null;
  updatedTime?: string | null;
}

export interface DeviceFirmwareAggregate {
  transferId: string;
  deviceCode: string;
  productId?: number | null;
  messageType?: string | null;
  dataSetId?: string | null;
  fileType?: string | null;
  description?: string | null;
  timestamp?: string | null;
  binaryLength?: number | null;
  totalPackets?: number | null;
  receivedPacketCount?: number | null;
  receivedPacketIndexes?: number[] | null;
  firmwareMd5?: string | null;
  calculatedMd5?: string | null;
  md5Matched?: boolean | null;
  completed?: boolean | null;
  assembledBase64?: string | null;
  assembledLength?: number | null;
  descriptor?: Record<string, unknown> | null;
  updatedTime?: string | null;
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

export interface ProductModelUpsertPayload {
  modelType: ProductModelType;
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

export interface DeviceAddPayload {
  productKey: string;
  deviceName: string;
  deviceCode: string;
  parentDeviceId?: IdType | null;
  parentDeviceCode?: string | null;
  deviceSecret?: string;
  clientId?: string;
  username?: string;
  password?: string;
  activateStatus?: number;
  deviceStatus?: number;
  firmwareVersion?: string;
  ipAddress?: string;
  address?: string;
  metadataJson?: string;
}

export interface DeviceBatchAddPayload {
  items: DeviceAddPayload[];
}

export interface DeviceBatchAddError {
  rowNo: number;
  deviceCode?: string | null;
  message: string;
}

export interface DeviceBatchAddResult {
  totalCount: number;
  successCount: number;
  failureCount: number;
  createdDeviceCodes?: string[] | null;
  errors: DeviceBatchAddError[];
}

export interface DeviceReplacePayload {
  productKey?: string;
  deviceName: string;
  deviceCode: string;
  parentDeviceId?: IdType | null;
  parentDeviceCode?: string | null;
  deviceSecret?: string;
  clientId?: string;
  username?: string;
  password?: string;
  activateStatus?: number;
  deviceStatus?: number;
  firmwareVersion?: string;
  ipAddress?: string;
  address?: string;
  metadataJson?: string;
}

export interface DeviceReplaceResult {
  sourceDeviceId: IdType;
  sourceDeviceCode: string;
  sourceDeviceName: string;
  targetDeviceId: IdType;
  targetDeviceCode: string;
  targetDeviceName: string;
}

export interface DeviceRelation {
  id?: IdType | null;
  parentDeviceCode: string;
  logicalChannelCode: string;
  childDeviceCode: string;
  childProductId?: IdType | null;
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

export interface HttpReportPayload {
  protocolCode: string;
  productKey: string;
  deviceCode: string;
  payload: string;
  payloadEncoding?: string;
  topic?: string;
  clientId?: string;
  tenantId?: string;
}

export interface MqttReportPublishPayload {
  protocolCode: string;
  productKey: string;
  deviceCode: string;
  topic: string;
  payload: string;
  payloadEncoding?: string;
  qos?: number;
  retained?: boolean;
}

export interface ActivityDraft {
  title?: string;
  detail: string;
  tag?: string;
  module?: string;
  action?: string;
  request?: unknown;
  response?: unknown;
  ok?: boolean;
  path?: string;
}

export interface ActivityEntry {
  id: string;
  title: string;
  detail: string;
  createdAt: string;
  tag?: string;
  module?: string;
  action?: string;
  request?: unknown;
  response?: unknown;
  ok: boolean;
  path?: string;
}
