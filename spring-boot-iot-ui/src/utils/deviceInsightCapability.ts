import type { InsightRangeCode, TelemetryHistoryBatchRequest } from '@/api/telemetry';
import type { DeviceProperty, ProductObjectInsightMetricGroup } from '@/types/api';
import { resolveInsightObjectType, type InsightObjectType } from '@/utils/deviceInsight';
import {
  getInsightMetricPriorityBoost,
  hasPrioritizedSnapshotMetrics,
  resolveInsightMetricDisplayName
} from '@/utils/deviceInsightNaming';
import {
  inferObjectInsightStatusGroup,
  normalizeObjectInsightMetricGroup
} from '@/utils/objectInsightMetricGroup';

export interface InsightMetricDefinition {
  identifier: string;
  displayName: string;
  group: ProductObjectInsightMetricGroup;
}

export interface InsightTrendGroupDefinition {
  key: ProductObjectInsightMetricGroup;
  title: string;
  identifiers: string[];
}

export interface InsightExtensionParameterDefinition {
  parameterKey: string;
  identifier: string;
  displayName: string;
}

export interface InsightCustomMetricDefinition {
  parameterKey: string;
  identifier: string;
  displayName: string;
  group: ProductObjectInsightMetricGroup;
  unit?: string;
  includeInTrend?: boolean;
  includeInExtension?: boolean;
  analysisTitle?: string;
  analysisTag?: string;
  analysisTemplate?: string;
  enabled?: boolean;
  sortNo?: number;
}

export interface InsightCapabilityProfile {
  key: string;
  objectType: 'detect' | 'warning' | 'collect' | 'generic';
  heroMetrics: InsightMetricDefinition[];
  trendGroups: InsightTrendGroupDefinition[];
  extensionParameters: InsightExtensionParameterDefinition[];
  historyIdentifiers: string[];
  customMetrics: InsightCustomMetricDefinition[];
}

type RawInsightCustomMetricConfig = Omit<Partial<InsightCustomMetricDefinition>, 'group'> & {
  group?: string;
};

export const INSIGHT_RANGE_OPTIONS = [
  { label: '近一天', value: '1d' },
  { label: '近一周', value: '7d' },
  { label: '近一月', value: '30d' },
  { label: '近一年', value: '365d' }
] as const;

export const DEFAULT_INSIGHT_RANGE: InsightRangeCode = '1d';

interface InsightCapabilitySource {
  deviceCode?: string | null;
  productName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  riskPointName?: string | null;
  properties?: DeviceProperty[] | null;
  deviceMetadataJson?: string | null;
  productMetadataJson?: string | null;
  metadataJson?: string | null;
}

interface RuntimeMetricCandidate {
  identifier: string;
  displayName: string;
  text: string;
  valueType: string;
}

interface RuntimeTemplateConfig {
  key: string;
  measureKeywords: string[];
  statusPriorityKeywords: string[];
}

const MAX_EXTENSION_PARAMETERS = 4;
const STATUS_METRIC_PATTERN =
  /(sensor_state|status|online|battery|signal|humidity|temperature|temp|voltage|current|network|energy|power|4g|rssi|snr|dbm|strength|state|zt|soc|dump_energy|remaining)/;
const NUMERIC_VALUE_TYPES = new Set(['bool', 'boolean', 'byte', 'short', 'int', 'integer', 'long', 'float', 'double', 'decimal', 'number']);
const DISPLAY_NAME_LABELS: Array<{ pattern: RegExp; label: string }> = [
  { pattern: /(battery_dump_energy|battery|电量|电池)/i, label: '剩余电量' },
  { pattern: /(signal_4g|4g|rssi|snr|dbm|信号)/i, label: '4G 信号强度' },
  { pattern: /(humidity|湿度)/i, label: '相对湿度' },
  { pattern: /(temperature|temp|温度)/i, label: '温度' }
];

const BASE_TREND_GROUPS: InsightTrendGroupDefinition[] = [
  { key: 'measure', title: '监测数据', identifiers: [] },
  { key: 'statusEvent', title: '状态事件', identifiers: [] },
  { key: 'runtime', title: '运行参数', identifiers: [] }
];

const BUILTIN_CUSTOM_METRIC_REGISTRY: Record<string, Omit<InsightCustomMetricDefinition, 'parameterKey'>> = {
  's1_zt_1.humidity': {
    identifier: 'S1_ZT_1.humidity',
    displayName: '相对湿度',
    group: 'runtime',
    analysisTitle: '现场环境补充',
    analysisTag: '系统自定义参数',
    analysisTemplate: '{{label}}当前为{{value}}，可辅助判断现场环境湿润程度。'
  },
  's1_zt_1.signal_4g': {
    identifier: 'S1_ZT_1.signal_4g',
    displayName: '4G 信号强度',
    group: 'runtime',
    analysisTitle: '通信状态补充',
    analysisTag: '系统自定义参数',
    analysisTemplate: '{{label}}当前为{{value}}，可辅助判断设备回传链路稳定性。'
  }
};

const RUNTIME_TEMPLATE_CONFIG: Record<InsightObjectType, RuntimeTemplateConfig> = {
  detect: {
    key: 'monitoring-dynamic',
    measureKeywords: ['gnss', 'value', 'displacement', 'tilt', 'angle', 'crack', 'settlement', '水位', '泥水位', '位移', '倾角', '裂缝', '沉降', '监测'],
    statusPriorityKeywords: ['sensor_state', 'online', '状态', '在线', 'battery', '电量', 'signal', '4g', 'humidity', '湿度']
  },
  collect: {
    key: 'collect-dynamic',
    measureKeywords: ['rain', 'water', 'flow', 'collect', '采集', '雨量', '水位', '流量', '泥水位', '墒情'],
    statusPriorityKeywords: ['sensor_state', 'online', '状态', '在线', 'signal', '4g', 'battery', '电量', 'humidity', '湿度']
  },
  warning: {
    key: 'warning-dynamic',
    measureKeywords: ['warning', 'alarm', 'warn', 'light', 'horn', 'speaker', '广播', '告警', '预警', '喊话', '声光'],
    statusPriorityKeywords: ['sensor_state', 'online', '状态', '在线', 'battery', '电量', 'signal', '4g', 'humidity', '湿度']
  },
  generic: {
    key: 'generic-dynamic',
    measureKeywords: ['value', 'measure', '监测', '数值'],
    statusPriorityKeywords: ['sensor_state', 'online', '状态', '在线', 'battery', '电量', 'signal', '4g', 'humidity', '湿度']
  }
};

const MUDDY_WATER_PROFILE: InsightCapabilityProfile = {
  key: 'muddy-water-level',
  objectType: 'detect',
  heroMetrics: [
    { identifier: 'L4_NW_1', displayName: '泥水位高程', group: 'measure' },
    { identifier: 'S1_ZT_1.sensor_state.L4_NW_1', displayName: '传感器在线状态', group: 'statusEvent' },
    { identifier: 'S1_ZT_1.battery_dump_energy', displayName: '剩余电量', group: 'runtime' }
  ],
  trendGroups: BASE_TREND_GROUPS,
  extensionParameters: [
    { parameterKey: 'humidity', identifier: 'S1_ZT_1.humidity', displayName: '相对湿度' },
    { parameterKey: 'signal_4g', identifier: 'S1_ZT_1.signal_4g', displayName: '4G 信号强度' }
  ],
  historyIdentifiers: [],
  customMetrics: []
};

const GENERIC_MONITORING_PROFILE: InsightCapabilityProfile = {
  key: 'generic-monitoring',
  objectType: 'generic',
  heroMetrics: [],
  trendGroups: BASE_TREND_GROUPS,
  extensionParameters: [],
  historyIdentifiers: [],
  customMetrics: []
};

export function getInsightCapabilityProfile(source: {
  deviceCode?: string | null;
  productName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  riskPointName?: string | null;
  properties?: DeviceProperty[] | null;
  deviceMetadataJson?: string | null;
  productMetadataJson?: string | null;
  metadataJson?: string | null;
}): InsightCapabilityProfile {
  const deviceCode = source.deviceCode?.trim();
  const productName = source.productName?.trim() ?? '';
  const baseProfile = deviceCode === 'SK00EB0D1308313' || /泥水位/.test(productName)
    ? MUDDY_WATER_PROFILE
    : buildRuntimeProfile(source);
  return applyCustomMetrics(baseProfile, source);
}

export function buildInsightHistoryRequest(
  deviceId: number | string,
  profile: InsightCapabilityProfile,
  rangeCode: InsightRangeCode = DEFAULT_INSIGHT_RANGE
): TelemetryHistoryBatchRequest {
  return {
    deviceId,
    identifiers: profile.historyIdentifiers,
    rangeCode,
    fillPolicy: 'ZERO'
  };
}

function buildRuntimeProfile(source: InsightCapabilitySource): InsightCapabilityProfile {
  const objectType = resolveInsightObjectType({
    metricIdentifier: source.metricIdentifier,
    metricName: source.metricName,
    productName: source.productName,
    riskPointName: source.riskPointName
  });
  const config = RUNTIME_TEMPLATE_CONFIG[objectType];
  const candidates = (source.properties ?? []).flatMap((item) => toRuntimeCandidate(item));
  if (!candidates.length) {
    return {
      ...GENERIC_MONITORING_PROFILE,
      key: config.key,
      objectType
    };
  }

  const measureCandidates = sortCandidates(
    candidates.filter((item) => !isStatusMetric(item)),
    config.measureKeywords
  );
  const statusCandidates = sortCandidates(
    candidates.filter((item) => isStatusMetric(item)),
    config.statusPriorityKeywords
  );

  const primaryMeasure = measureCandidates[0];
  const primaryStatus = statusCandidates[0];
  const secondaryStatus = statusCandidates.find((item) => item.identifier !== primaryStatus?.identifier);
  const heroSourceCandidates = hasPrioritizedSnapshotMetrics(source.properties ?? [])
    ? measureCandidates.slice(0, Math.min(3, measureCandidates.length))
    : [primaryMeasure, primaryStatus, secondaryStatus];
  const heroMetrics = heroSourceCandidates
    .filter((item): item is RuntimeMetricCandidate => Boolean(item))
    .map((item) => ({
      identifier: item.identifier,
      displayName: item.displayName,
      group: isStatusMetric(item) ? inferObjectInsightStatusGroup(item.identifier, item.displayName) : 'measure'
    }));

  const extensionParameters = statusCandidates
    .filter((item) => inferObjectInsightStatusGroup(item.identifier, item.displayName) === 'runtime')
    .filter((item) => heroMetrics.every((metric) => metric.identifier !== item.identifier))
    .slice(0, MAX_EXTENSION_PARAMETERS)
    .map((item) => ({
      parameterKey: item.identifier,
      identifier: item.identifier,
      displayName: item.displayName
    }));

  return {
    key: config.key,
    objectType,
    heroMetrics,
    trendGroups: BASE_TREND_GROUPS,
    extensionParameters,
    historyIdentifiers: [],
    customMetrics: []
  };
}

function applyCustomMetrics(
  profile: InsightCapabilityProfile,
  source: InsightCapabilitySource
): InsightCapabilityProfile {
  const customMetrics = uniqueCustomMetrics([
    ...resolveBuiltInCustomMetrics(profile, source),
    ...resolveMetadataCustomMetrics(profile, source.productMetadataJson, source),
    ...resolveMetadataCustomMetrics(profile, source.deviceMetadataJson ?? source.metadataJson, source)
  ]);
  if (!customMetrics.length) {
    return profile;
  }

  const heroMetrics = profile.heroMetrics.map((item) => ({ ...item }));
  const extensionParameters = profile.extensionParameters.map((item) => ({ ...item }));
  const trendGroups = profile.trendGroups.map((group) => ({
    ...group,
    identifiers: [...group.identifiers]
  }));
  const excludedIdentifiers = new Set(
    customMetrics
      .filter((item) => item.enabled === false || item.includeInTrend === false)
      .map((item) => item.identifier)
  );
  const filteredExtensionParameters = extensionParameters.filter(
    (item) => !excludedIdentifiers.has(item.identifier)
  );
  trendGroups.forEach((group) => {
    group.identifiers = group.identifiers.filter((identifier) => !excludedIdentifiers.has(identifier));
  });

  customMetrics.forEach((metric) => {
    heroMetrics
      .filter((item) => item.identifier === metric.identifier)
      .forEach((item) => {
        item.displayName = metric.displayName;
      });

    const existingExtensionIndex = filteredExtensionParameters.findIndex((item) => item.identifier === metric.identifier);
    if (metric.enabled === false || metric.includeInExtension === false) {
      if (existingExtensionIndex >= 0) {
        filteredExtensionParameters.splice(existingExtensionIndex, 1);
      }
    } else if (existingExtensionIndex >= 0) {
      filteredExtensionParameters[existingExtensionIndex] = {
        ...filteredExtensionParameters[existingExtensionIndex],
        displayName: metric.displayName
      };
    } else {
      filteredExtensionParameters.push({
        parameterKey: metric.parameterKey,
        identifier: metric.identifier,
        displayName: metric.displayName
      });
    }

    const trendGroup = trendGroups.find((group) => group.key === metric.group);
    if (!trendGroup) {
      return;
    }

    trendGroup.identifiers = trendGroup.identifiers.filter((identifier) => identifier !== metric.identifier);
    if (metric.enabled === false || metric.includeInTrend === false) {
      return;
    }
    trendGroup.identifiers = uniqueIdentifiers([...trendGroup.identifiers, metric.identifier]);
  });

  const prioritizedTrendGroups = trendGroups.map((group) => ({
    ...group,
    identifiers: prioritizeTrendGroupIdentifiers(group, customMetrics)
  }));

  return {
    ...profile,
    heroMetrics,
    trendGroups: prioritizedTrendGroups,
    extensionParameters: filteredExtensionParameters,
    historyIdentifiers: uniqueIdentifiers(prioritizedTrendGroups.flatMap((group) => group.identifiers)),
    customMetrics
  };
}

function resolveBuiltInCustomMetrics(
  profile: InsightCapabilityProfile,
  source: InsightCapabilitySource
): InsightCustomMetricDefinition[] {
  const candidateIdentifiers = uniqueIdentifiers([
    ...profile.heroMetrics.map((item) => item.identifier),
    ...profile.extensionParameters.map((item) => item.identifier),
    ...(source.properties ?? []).map((item) => item.identifier)
  ]);
  return candidateIdentifiers
    .filter((identifier) => Boolean(BUILTIN_CUSTOM_METRIC_REGISTRY[identifier.toLowerCase()]))
    .map((identifier) => buildConfiguredMetricDefinition(identifier, {}, profile, source))
    .filter((item): item is InsightCustomMetricDefinition => Boolean(item));
}

function resolveMetadataCustomMetrics(
  profile: InsightCapabilityProfile,
  metadataJson: string | null | undefined,
  source: InsightCapabilitySource
): InsightCustomMetricDefinition[] {
  const parsed = parseObjectInsightMetadata(metadataJson);
  return parsed
    .map((item) => buildConfiguredMetricDefinition(item.identifier, item, profile, source))
    .filter((item): item is InsightCustomMetricDefinition => Boolean(item));
}

function buildConfiguredMetricDefinition(
  identifier: string | undefined,
  config: RawInsightCustomMetricConfig,
  profile: InsightCapabilityProfile,
  source: InsightCapabilitySource
): InsightCustomMetricDefinition | null {
  const normalizedIdentifier = identifier?.trim();
  if (!normalizedIdentifier) {
    return null;
  }
  const resolvedIdentifier = resolveRuntimeMetricIdentifier(normalizedIdentifier, source.properties ?? []);
  const builtIn = BUILTIN_CUSTOM_METRIC_REGISTRY[normalizedIdentifier.toLowerCase()];
  const hasExplicitConfig = Object.keys(config).length > 0;
  const displayName = normalizeOptionalText(config.displayName)
    || builtIn?.displayName
    || resolveConfiguredMetricDisplayName(resolvedIdentifier, source.properties ?? [])
    || resolvedIdentifier;
  const profileGroup = resolveMetricGroup(profile, resolvedIdentifier);
  const fallbackGroup = builtIn?.group
    || profileGroup
    || inferObjectInsightStatusGroup(resolvedIdentifier, displayName);
  const group = config.group === undefined || config.group === null || config.group === ''
    ? fallbackGroup
    : normalizeObjectInsightMetricGroup(config.group, resolvedIdentifier, displayName);
  const existsInHero = profile.heroMetrics.some((item) => item.identifier === resolvedIdentifier);
  return {
    parameterKey: normalizeOptionalText(config.parameterKey) || resolvedIdentifier,
    identifier: resolvedIdentifier,
    displayName,
    group,
    unit: normalizeOptionalText(config.unit) || builtIn?.unit,
    includeInTrend: typeof config.includeInTrend === 'boolean' ? config.includeInTrend : hasExplicitConfig ? true : false,
    includeInExtension: typeof config.includeInExtension === 'boolean' ? config.includeInExtension : !existsInHero,
    analysisTitle: normalizeOptionalText(config.analysisTitle) || builtIn?.analysisTitle,
    analysisTag: normalizeOptionalText(config.analysisTag) || builtIn?.analysisTag,
    analysisTemplate: normalizeOptionalText(config.analysisTemplate) || builtIn?.analysisTemplate,
    enabled: typeof config.enabled === 'boolean' ? config.enabled : true,
    sortNo: normalizeOptionalNumber(config.sortNo)
  };
}

function parseObjectInsightMetadata(metadataJson?: string | null): Array<RawInsightCustomMetricConfig & { identifier: string }> {
  const parsedMetadata = safeParseJson(metadataJson);
  const insightConfig = safeReadObject(parsedMetadata?.objectInsight);
  const customMetrics = Array.isArray(insightConfig?.customMetrics) ? insightConfig.customMetrics : [];
  return customMetrics.flatMap((item) => {
    const config = safeReadObject(item);
    const identifier = normalizeOptionalText(config?.identifier);
    if (!identifier) {
      return [];
    }
    return [{
      identifier,
        parameterKey: normalizeOptionalText(config?.parameterKey) || identifier,
        displayName: normalizeOptionalText(config?.displayName) || undefined,
        group: typeof config?.group === 'string' ? config.group : undefined,
        unit: normalizeOptionalText(config?.unit) || undefined,
        includeInTrend: typeof config?.includeInTrend === 'boolean' ? config.includeInTrend : undefined,
        includeInExtension: typeof config?.includeInExtension === 'boolean' ? config.includeInExtension : undefined,
        analysisTitle: normalizeOptionalText(config?.analysisTitle) || undefined,
      analysisTag: normalizeOptionalText(config?.analysisTag) || undefined,
      analysisTemplate: normalizeOptionalText(config?.analysisTemplate) || undefined,
      enabled: typeof config?.enabled === 'boolean' ? config.enabled : undefined,
      sortNo: normalizeOptionalNumber(config?.sortNo)
    }];
  });
}

function resolveConfiguredMetricDisplayName(identifier: string, properties: DeviceProperty[]) {
  const property = properties.find((item) => item.identifier === identifier);
  if (property) {
    return resolveDisplayName(property);
  }
  const matched = DISPLAY_NAME_LABELS.find((item) => item.pattern.test(identifier));
  return matched?.label || null;
}

function resolveMetricGroup(profile: InsightCapabilityProfile, identifier: string): ProductObjectInsightMetricGroup | null {
  const heroMetric = profile.heroMetrics.find((item) => item.identifier === identifier);
  if (heroMetric) {
    return heroMetric.group;
  }
  const trendGroup = profile.trendGroups.find((item) => item.identifiers.includes(identifier));
  return trendGroup?.key || null;
}

function uniqueCustomMetrics(values: InsightCustomMetricDefinition[]) {
  const map = new Map<string, InsightCustomMetricDefinition>();
  values.forEach((item) => {
    map.set(item.identifier, mergeCustomMetricDefinition(map.get(item.identifier), item));
  });
  return Array.from(map.values());
}

function safeParseJson(value?: string | null): Record<string, unknown> | null {
  const text = value?.trim();
  if (!text) {
    return null;
  }
  try {
    const parsed = JSON.parse(text);
    return safeReadObject(parsed);
  } catch {
    return null;
  }
}

function safeReadObject(value: unknown): Record<string, any> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, any>;
}

function normalizeOptionalText(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}

function normalizeOptionalNumber(value: unknown) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : undefined;
}

function resolveRuntimeMetricIdentifier(identifier: string, properties: DeviceProperty[]) {
  const exactMatch = properties.find((item) => item.identifier === identifier);
  if (exactMatch?.identifier) {
    return exactMatch.identifier;
  }
  const normalizedIdentifier = identifier.trim().toLowerCase();
  const caseInsensitiveMatch = properties.find((item) => item.identifier?.trim().toLowerCase() === normalizedIdentifier);
  if (caseInsensitiveMatch?.identifier) {
    return caseInsensitiveMatch.identifier;
  }
  const suffixMatches = properties.filter((item) => {
    const runtimeIdentifier = item.identifier?.trim();
    if (!runtimeIdentifier) {
      return false;
    }
    return runtimeIdentifier.toLowerCase().endsWith(`.${normalizedIdentifier}`);
  });
  if (suffixMatches.length === 1 && suffixMatches[0]?.identifier) {
    return suffixMatches[0].identifier;
  }
  return identifier;
}

function toRuntimeCandidate(property: DeviceProperty): RuntimeMetricCandidate[] {
  if (!property.identifier || !isNumericProperty(property)) {
    return [];
  }
  const displayName = resolveDisplayName(property);
  return [{
    identifier: property.identifier,
    displayName,
    text: `${property.identifier} ${displayName}`.toLowerCase(),
    valueType: (property.valueType || '').toLowerCase()
  }];
}

function isNumericProperty(property: DeviceProperty) {
  const valueType = (property.valueType || '').trim().toLowerCase();
  if (valueType && NUMERIC_VALUE_TYPES.has(valueType)) {
    return true;
  }
  const value = property.propertyValue?.trim();
  if (!value) {
    return false;
  }
  return !Number.isNaN(Number(value));
}

function resolveDisplayName(property: DeviceProperty) {
  const resolvedName = resolveInsightMetricDisplayName(property.identifier, property.propertyName);
  if (resolvedName !== property.identifier) {
    return resolvedName;
  }
  const matched = DISPLAY_NAME_LABELS.find((item) => item.pattern.test(property.identifier));
  return matched?.label || property.identifier;
}

function isStatusMetric(candidate: RuntimeMetricCandidate) {
  return STATUS_METRIC_PATTERN.test(candidate.text);
}

function sortCandidates(candidates: RuntimeMetricCandidate[], priorityKeywords: string[]) {
  return [...candidates].sort((left, right) => {
    const scoreDiff = scoreCandidate(right, priorityKeywords) - scoreCandidate(left, priorityKeywords);
    if (scoreDiff !== 0) {
      return scoreDiff;
    }
    const lengthDiff = left.identifier.length - right.identifier.length;
    if (lengthDiff !== 0) {
      return lengthDiff;
    }
    return left.identifier.localeCompare(right.identifier);
  });
}

function scoreCandidate(candidate: RuntimeMetricCandidate, priorityKeywords: string[]) {
  const keywordScore = priorityKeywords.reduce((score, keyword, index) => {
    if (candidate.text.includes(keyword.toLowerCase())) {
      return score + (priorityKeywords.length - index) * 10;
    }
    return score;
  }, 0);
  return keywordScore + getInsightMetricPriorityBoost(candidate.identifier);
}

function uniqueIdentifiers(values: string[]) {
  return values.filter((value, index) => Boolean(value) && values.indexOf(value) === index);
}

function prioritizeTrendGroupIdentifiers(
  group: InsightTrendGroupDefinition,
  customMetrics: InsightCustomMetricDefinition[]
) {
  const prioritizedIdentifiers = customMetrics
    .filter((item) =>
      item.group === group.key
      && item.enabled !== false
      && item.includeInTrend !== false
      && item.includeInExtension === false
    )
    .sort((left, right) => {
      const sortDiff = (left.sortNo ?? Number.MAX_SAFE_INTEGER) - (right.sortNo ?? Number.MAX_SAFE_INTEGER);
      if (sortDiff !== 0) {
        return sortDiff;
      }
      return left.identifier.localeCompare(right.identifier);
    })
    .map((item) => item.identifier);

  return uniqueIdentifiers([
    ...prioritizedIdentifiers,
    ...group.identifiers.filter((identifier) => !prioritizedIdentifiers.includes(identifier))
  ]);
}

function mergeCustomMetricDefinition(
  current: InsightCustomMetricDefinition | undefined,
  incoming: InsightCustomMetricDefinition
): InsightCustomMetricDefinition {
  if (!current) {
    return incoming;
  }

  const merged: InsightCustomMetricDefinition = { ...current };
  const normalizedParameterKey = normalizeOptionalText(incoming.parameterKey);
  const normalizedIdentifier = normalizeOptionalText(incoming.identifier);
  const normalizedDisplayName = normalizeOptionalText(incoming.displayName);
  const normalizedUnit = normalizeOptionalText(incoming.unit);
  const normalizedAnalysisTitle = normalizeOptionalText(incoming.analysisTitle);
  const normalizedAnalysisTag = normalizeOptionalText(incoming.analysisTag);
  const normalizedAnalysisTemplate = normalizeOptionalText(incoming.analysisTemplate);

  if (normalizedParameterKey) {
    merged.parameterKey = normalizedParameterKey;
  }
  if (normalizedIdentifier) {
    merged.identifier = normalizedIdentifier;
  }
  if (normalizedDisplayName) {
    merged.displayName = normalizedDisplayName;
  }
  if (normalizedUnit) {
    merged.unit = normalizedUnit;
  }
  merged.group = normalizeObjectInsightMetricGroup(incoming.group, merged.identifier, merged.displayName);
  if (typeof incoming.includeInTrend === 'boolean') {
    merged.includeInTrend = incoming.includeInTrend;
  }
  if (typeof incoming.includeInExtension === 'boolean') {
    merged.includeInExtension = incoming.includeInExtension;
  }
  if (normalizedAnalysisTitle) {
    merged.analysisTitle = normalizedAnalysisTitle;
  }
  if (normalizedAnalysisTag) {
    merged.analysisTag = normalizedAnalysisTag;
  }
  if (normalizedAnalysisTemplate) {
    merged.analysisTemplate = normalizedAnalysisTemplate;
  }
  if (typeof incoming.enabled === 'boolean') {
    merged.enabled = incoming.enabled;
  }
  if (typeof incoming.sortNo === 'number') {
    merged.sortNo = incoming.sortNo;
  }

  return merged;
}
