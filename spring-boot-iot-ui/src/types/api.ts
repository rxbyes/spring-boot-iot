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
}

export interface ProductModelCandidateSummary {
  extractionMode?: string | null;
  sampleType?: string | null;
  sampleDeviceCode?: string | null;
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
}

export interface ProductModelGovernanceCompareRow {
  modelType: ProductModelType;
  identifier: string;
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

export interface ProductModelGovernanceManualExtractPayload {
  sampleType: ProductModelManualSampleType;
  samplePayload: string;
}

export interface ProductModelGovernanceManualDraftItem {
  modelType: ProductModelType;
  identifier: string;
  modelName: string;
  dataType?: string | null;
  specsJson?: string | null;
  eventType?: string | null;
  serviceInputJson?: string | null;
  serviceOutputJson?: string | null;
  description?: string | null;
}

export interface ProductModelGovernanceComparePayload {
  governanceMode?: 'normative' | 'generic' | null;
  normativePresetCode?: string | null;
  selectedNormativeIdentifiers?: string[] | null;
  manualExtract?: ProductModelGovernanceManualExtractPayload;
  manualDraftItems?: ProductModelGovernanceManualDraftItem[];
  includeRuntimeCandidates?: boolean;
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
  createdCount?: number | null;
  updatedCount?: number | null;
  skippedCount?: number | null;
  conflictCount?: number | null;
  lastAppliedAt?: string | null;
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

export type ProductModelManualSampleType = 'business' | 'status' | 'other';

export interface ProductModelManualExtractPayload {
  sampleType: ProductModelManualSampleType;
  samplePayload: string;
}

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
}

export interface DeviceMetricOption {
  identifier: string;
  name: string;
  dataType?: string | null;
}

export interface DeviceProperty {
  id: IdType;
  identifier: string;
  propertyName?: string | null;
  propertyValue?: string | null;
  valueType?: string | null;
  reportTime?: string | null;
  updateTime?: string | null;
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
  status?: number;
}

export interface ProductModelUpsertPayload {
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
