import type { DeviceProperty } from '@/types/api';

const FIXED_DISPLAY_NAME_RULES: Array<{ pattern: RegExp; label: string }> = [
  { pattern: /^L\d+_LF_\d+\.value$/i, label: '裂缝量' },
  { pattern: /^L\d+_QJ_\d+\.angle$/i, label: '水平面夹角' },
  { pattern: /^L\d+_QJ_\d+\.AZI$/i, label: '方位角' },
  { pattern: /^L\d+_QJ_\d+\.X$/i, label: 'X轴倾角' },
  { pattern: /^L\d+_QJ_\d+\.Y$/i, label: 'Y轴倾角' },
  { pattern: /^L\d+_QJ_\d+\.Z$/i, label: 'Z轴倾角' },
  { pattern: /^L\d+_JS_\d+\.gX$/i, label: 'X向加速度' },
  { pattern: /^L\d+_JS_\d+\.gY$/i, label: 'Y向加速度' },
  { pattern: /^L\d+_JS_\d+\.gZ$/i, label: 'Z向加速度' }
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
  const fixedLabel = FIXED_DISPLAY_NAME_RULES.find((item) => item.pattern.test(identifier))?.label;
  if (fixedLabel) {
    return fixedLabel;
  }

  const normalizedPropertyName = propertyName?.trim();
  if (!normalizedPropertyName || /^value$/i.test(normalizedPropertyName)) {
    return identifier;
  }
  return normalizedPropertyName;
}

export function getInsightMetricPriorityBoost(identifier: string) {
  return PRIORITY_BOOST_RULES.find((item) => item.pattern.test(identifier))?.priority ?? 0;
}

export function hasPrioritizedSnapshotMetrics(properties: DeviceProperty[]) {
  return properties.filter((item) => getInsightMetricPriorityBoost(item.identifier) > 0).length >= 2;
}
