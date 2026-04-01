import type { RiskMonitoringDetail, RiskMonitoringListItem } from '@/api/riskMonitoring';
import type { DeviceMessageLog, DeviceProperty } from '@/types/api';

export type InsightObjectType = 'detect' | 'warning' | 'collect' | 'generic';

interface InsightReason {
  title: string;
  tag: string;
  description: string;
}

function riskLevelWeight(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return 4;
    case 'WARNING':
    case 'MEDIUM':
      return 3;
    case 'INFO':
    case 'LOW':
      return 2;
    default:
      return 1;
  }
}

function resolveTimestamp(value?: string | null) {
  if (!value) {
    return 0;
  }

  const timestamp = new Date(value).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

export function pickPrimaryBinding(items: RiskMonitoringListItem[]) {
  if (!items.length) {
    return null;
  }

  return [...items].sort((left, right) => {
    const reportDiff = resolveTimestamp(right.latestReportTime) - resolveTimestamp(left.latestReportTime);
    if (reportDiff !== 0) {
      return reportDiff;
    }

    const onlineDiff = Number(right.onlineStatus ?? 0) - Number(left.onlineStatus ?? 0);
    if (onlineDiff !== 0) {
      return onlineDiff;
    }

    const levelDiff = riskLevelWeight(right.riskLevel) - riskLevelWeight(left.riskLevel);
    if (levelDiff !== 0) {
      return levelDiff;
    }

    return Number(right.bindingId) - Number(left.bindingId);
  })[0];
}

export function resolveInsightObjectType(source: Partial<RiskMonitoringDetail>): InsightObjectType {
  const keywordSource = [
    source.metricIdentifier,
    source.metricName,
    source.productName,
    source.riskPointName
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();

  if (/(warning|warn|预警|告警|广播|声光|闪灯|喊话)/.test(keywordSource)) {
    return 'warning';
  }

  if (/(rain|water|collect|采集|雨量|水位|墒情|流量|采样)/.test(keywordSource)) {
    return 'collect';
  }

  if (/(gnss|angle|tilt|displacement|detect|检测|位移|倾角|裂缝|沉降)/.test(keywordSource)) {
    return 'detect';
  }

  return 'generic';
}

export function getInsightObjectTypeLabel(type: InsightObjectType) {
  switch (type) {
    case 'detect':
      return '检测型';
    case 'warning':
      return '预警型';
    case 'collect':
      return '采集型';
    default:
      return '通用型';
  }
}

export function getRiskLevelLabel(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '严重风险';
    case 'WARNING':
    case 'MEDIUM':
      return '警告风险';
    case 'INFO':
    case 'LOW':
      return '提醒风险';
    default:
      return value || '未标注';
  }
}

export function buildInsightReasons(
  detail: RiskMonitoringDetail | null,
  properties: DeviceProperty[],
  logs: DeviceMessageLog[]
) {
  const reasons: InsightReason[] = [];

  if (!detail) {
    return reasons;
  }

  if ((detail.monitorStatus || '').toUpperCase() === 'ALARM') {
    reasons.push({
      title: '当前监测对象处于告警中',
      tag: '监测状态',
      description: '建议优先核查最近告警与事件闭环，并确认现场是否需要立即响应。'
    });
  }

  if ((detail.monitorStatus || '').toUpperCase() === 'NO_DATA') {
    reasons.push({
      title: '监测对象暂无有效数据',
      tag: '数据时效',
      description: '当前趋势和当前值可信度不足，建议先核查采集链路与测点配置。'
    });
  }

  if (detail.onlineStatus !== 1) {
    reasons.push({
      title: '设备当前离线',
      tag: '在线状态',
      description: '设备离线会削弱风险趋势连续性，建议优先恢复链路或确认网关状态。'
    });
  }

  if ((detail.activeAlarmCount ?? 0) > 0) {
    reasons.push({
      title: '近期存在活跃告警',
      tag: '风险闭环',
      description: `当前对象关联 ${detail.activeAlarmCount ?? 0} 条活跃告警，建议结合事件处置情况统一研判。`
    });
  }

  if (!properties.length) {
    reasons.push({
      title: '缺少属性快照',
      tag: '数据完整性',
      description: '当前没有可直接核查的设备属性快照，不利于快速校验现场与设备状态。'
    });
  }

  if (!logs.length) {
    reasons.push({
      title: '缺少消息日志',
      tag: '审计链路',
      description: '当前没有用于回看 topic 和 payload 的消息日志，链路审计信息不足。'
    });
  }

  return reasons.slice(0, 6);
}

export function buildInsightDraft(detail: RiskMonitoringDetail | null, reasons: InsightReason[]) {
  if (!detail) {
    return {
      summary: '当前尚未加载风险监测详情。',
      actions: '请先输入设备编码或传入绑定对象后刷新洞察。',
      followUp: '完成绑定解析后再查看趋势与研判依据。'
    };
  }

  const summary = `${getRiskLevelLabel(detail.riskLevel)}，${detail.riskPointName || detail.deviceName || '当前对象'}当前监测状态为${detail.monitorStatus || '未标注'}。`;
  const actions = reasons.length
    ? reasons.map((item) => item.title).join('；')
    : '当前未发现需要立即处置的补充依据，建议继续保持日常观察。';
  const followUp = (detail.activeAlarmCount ?? 0) > 0 || detail.onlineStatus !== 1
    ? '建议跟踪告警闭环和链路恢复情况，并复核趋势是否继续恶化。'
    : '建议继续观察趋势变化，并定期刷新属性与日志快照。';

  return {
    summary,
    actions,
    followUp
  };
}
