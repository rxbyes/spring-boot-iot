import type { DeviceProperty } from '@/types/api';

const FIXED_DISPLAY_NAME_RULES: Array<{ pattern: RegExp; label: string }> = [
  { pattern: /^L\d+_LF_\d+\.value$/i, label: '裂缝量' },
  { pattern: /^L\d+_QJ_\d+\.angle$/i, label: '水平面夹角' },
  { pattern: /^L\d+_QJ_\d+\.AZI$/i, label: '方位角' },
  { pattern: /^L\d+_QJ_\d+\.X$/i, label: 'X轴倾角' },
  { pattern: /^L\d+_QJ_\d+\.Y$/i, label: 'Y轴倾角' },
  { pattern: /^L\d+_QJ_\d+\.Z$/i, label: 'Z轴倾角' },
  { pattern: /^L\d+_JS_\d+\.gX$/i, label: 'X轴加速度' },
  { pattern: /^L\d+_JS_\d+\.gY$/i, label: 'Y轴加速度' },
  { pattern: /^L\d+_JS_\d+\.gZ$/i, label: 'Z轴加速度' }
];

const RAW_PROPERTY_NAME_RULES: RegExp[] = [
  /^\d+号倾角测点(angle|AZI|X|Y|Z)$/i,
  /^\d+号加速度测点g[XYZ]$/i,
  /^\d+号裂缝测点value$/i
];

const PRIORITY_BOOST_RULES: Array<{ pattern: RegExp; priority: number }> = [
  { pattern: /^L\d+_LF_\d+\.value$/i, priority: 320 },
  { pattern: /^L\d+_QJ_\d+\.angle$/i, priority: 260 },
  { pattern: /^L\d+_JS_\d+\.gX$/i, priority: 260 },
  { pattern: /^L\d+_JS_\d+\.gY$/i, priority: 250 },
  { pattern: /^L\d+_JS_\d+\.gZ$/i, priority: 240 },
  { pattern: /^L\d+_QJ_\d+\.X$/i, priority: 190 },
  { pattern: /^L\d+_QJ_\d+\.Y$/i, priority: 180 },
  { pattern: /^L\d+_QJ_\d+\.Z$/i, priority: 170 },
  { pattern: /^L\d+_QJ_\d+\.AZI$/i, priority: 160 }
];

export function resolveInsightMetricDisplayName(identifier: string, propertyName?: string | null) {
  const normalizedPropertyName = propertyName?.trim();
  if (normalizedPropertyName && !/^value$/i.test(normalizedPropertyName) && !isRawPropertyName(normalizedPropertyName)) {
    return normalizedPropertyName;
  }

  const fixedLabel = FIXED_DISPLAY_NAME_RULES.find((item) => item.pattern.test(identifier))?.label;
  if (fixedLabel) {
    return fixedLabel;
  }
  if (normalizedPropertyName) {
    return normalizedPropertyName;
  }
  return identifier;
}

function isRawPropertyName(propertyName: string) {
  return RAW_PROPERTY_NAME_RULES.some((pattern) => pattern.test(propertyName));
}

export function getInsightMetricPriorityBoost(identifier: string) {
  return PRIORITY_BOOST_RULES.find((item) => item.pattern.test(identifier))?.priority ?? 0;
}

export function hasPrioritizedSnapshotMetrics(properties: DeviceProperty[]) {
  return properties.filter((item) => getInsightMetricPriorityBoost(item.identifier) > 0).length >= 2;
}
