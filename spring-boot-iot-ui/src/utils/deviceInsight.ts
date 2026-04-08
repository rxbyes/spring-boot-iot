import type { RiskMonitoringDetail, RiskMonitoringListItem } from '@/api/riskMonitoring';
import type { Device, DeviceMessageLog, DeviceProperty } from '@/types/api';
import { getRiskLevelText, getRiskLevelWeight } from '@/utils/riskLevel';

export type InsightObjectType = 'detect' | 'warning' | 'collect' | 'generic';

interface InsightReason {
  title: string;
  tag: string;
  description: string;
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
      return '监测型';
    case 'warning':
      return '预警型';
    case 'collect':
      return '采集型';
    default:
      return '通用型';
  }
}

export function getRiskLevelLabel(value?: string | null) {
  return `${getRiskLevelText(value)}风险`;
}

export function buildInsightReasons(
  detail: RiskMonitoringDetail | null,
  properties: DeviceProperty[],
  logs: DeviceMessageLog[],
  device?: Device | null
) {
  const reasons: InsightReason[] = [];

  if (!detail) {
    reasons.push({
      title: '当前设备未纳入风险监测绑定',
      tag: '风险口径',
      description: '当前页已退化为设备上报分析视图，风险等级和趋势预览需在完成风险监测绑定后才会出现。'
    });

    if (device?.onlineStatus !== 1) {
      reasons.push({
        title: '设备当前离线',
        tag: '在线状态',
        description: '设备离线会影响上报连续性，建议先核查供电、网络或网关链路。'
      });
    }

    if (!properties.length) {
      reasons.push({
        title: '缺少属性快照',
        tag: '数据完整性',
        description: '当前没有可直接核查的设备属性快照，建议先确认最近上报是否已正常入库。'
      });
    }

    if (!logs.length) {
      reasons.push({
        title: '缺少消息日志',
        tag: '审计链路',
        description: '当前没有可回看 topic 和 payload 的消息日志，无法直接复盘最近一次上报。'
      });
    }

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

export function buildInsightDraft(detail: RiskMonitoringDetail | null, reasons: InsightReason[], device?: Device | null) {
  if (!detail) {
    return {
      summary: `${device?.deviceName || device?.deviceCode || '当前设备'}尚未纳入风险监测绑定，当前草稿基于设备上报数据生成。`,
      actions: reasons.length
        ? reasons.map((item) => item.title).join('；')
        : '建议先核查最近上报、属性快照和消息日志是否完整。',
      followUp: '完成风险监测绑定后，可继续查看风险等级、绑定测点趋势和监测对象档案。'
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
