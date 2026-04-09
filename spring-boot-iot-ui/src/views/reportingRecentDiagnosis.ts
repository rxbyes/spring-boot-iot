import type { MessageFlowRecentSession } from '@/types/api';
import type { ReportingVerdict } from './reportingDiagnosis';

export type ReportingRecentActionTarget = 'message-trace' | 'system-log' | 'file-debug' | 'reporting';
export type ReportingRecentFilter = 'all' | 'failed' | 'pending' | 'validated';

export interface ReportingRecentDiagnosisAction {
  label: string;
  target: ReportingRecentActionTarget;
}

export interface ReportingRecentDiagnosisItem {
  key: string;
  verdict: ReportingVerdict;
  statusLabel: string;
  summary: string;
  blocker: string;
  sessionId: string;
  traceId: string;
  deviceCode: string;
  transportMode: string;
  topic: string;
  submittedAt: string;
  primaryAction: ReportingRecentDiagnosisAction;
}

export function mapRecentSessionToDiagnosis(session: MessageFlowRecentSession): ReportingRecentDiagnosisItem {
  const verdict = resolveVerdict(session);
  const primaryAction = resolvePrimaryAction(session, verdict);

  return {
    key: normalizeText(session.sessionId) || `${normalizeText(session.deviceCode) || '--'}-${normalizeText(session.submittedAt) || '--'}`,
    verdict,
    statusLabel: resolveStatusLabel(verdict, session.timelineStatus),
    summary: normalizeText(session.diagnosticReason) || resolveSummary(verdict, session.timelineStatus),
    blocker: resolveBlocker(session),
    sessionId: normalizeText(session.sessionId),
    traceId: normalizeText(session.traceId),
    deviceCode: normalizeText(session.deviceCode),
    transportMode: normalizeText(session.transportMode),
    topic: normalizeText(session.topic),
    submittedAt: normalizeText(session.submittedAt),
    primaryAction
  };
}

export function mapRecentSessionToDiagnosisItem(session: MessageFlowRecentSession): ReportingRecentDiagnosisItem {
  return mapRecentSessionToDiagnosis(session);
}

export function filterRecentDiagnosisItems(
  items: ReportingRecentDiagnosisItem[],
  filter: ReportingRecentFilter
): ReportingRecentDiagnosisItem[] {
  if (filter === 'all') {
    return items;
  }
  if (filter === 'failed') {
    return items.filter((item) => item.verdict === 'failed' || item.verdict === 'degraded');
  }
  if (filter === 'pending') {
    return items.filter((item) => item.verdict === 'pending');
  }
  return items.filter((item) => item.verdict === 'validated');
}

function resolveVerdict(session: MessageFlowRecentSession): ReportingVerdict {
  const verdict = normalizeText(session.verdict);
  if (
    verdict === 'validated'
    || verdict === 'pending'
    || verdict === 'failed'
    || verdict === 'degraded'
  ) {
    return verdict;
  }
  if (Boolean(session.correlationPending) && !normalizeText(session.traceId)) {
    return 'pending';
  }
  if (normalizeText(session.status) === 'FAILED') {
    return 'failed';
  }
  if (Boolean(session.timelineAvailable)) {
    return 'validated';
  }
  return 'degraded';
}

function resolveStatusLabel(
  verdict: ReportingVerdict,
  timelineStatus?: MessageFlowRecentSession['timelineStatus']
): string {
  if (verdict === 'validated') {
    return '成功';
  }
  if (verdict === 'pending') {
    return '等待回流';
  }
  if (verdict === 'failed') {
    return '失败';
  }
  if (timelineStatus === 'expired') {
    return '已过期';
  }
  return '时间线缺失';
}

function resolveSummary(
  verdict: ReportingVerdict,
  timelineStatus?: MessageFlowRecentSession['timelineStatus']
): string {
  if (verdict === 'pending') {
    return '等待 trace 绑定';
  }
  if (verdict === 'failed') {
    return '本次验证失败';
  }
  if (verdict === 'validated') {
    return '已完成复盘';
  }
  if (timelineStatus === 'expired') {
    return '时间线已过期';
  }
  return '时间线暂不可用';
}

function resolveBlocker(session: MessageFlowRecentSession): string {
  const failureStage = normalizeText(session.failureStage);
  if (failureStage) {
    return `${failureStage} 阶段失败`;
  }
  if (session.timelineStatus === 'pending') {
    return '等待 trace 绑定';
  }
  if (session.timelineStatus === 'expired') {
    return '最近 trace 已超出保留窗口';
  }
  if (session.timelineStatus === 'missing') {
    return '时间线暂未写入';
  }
  if (session.timelineStatus === 'available') {
    return '已拿到完整复盘';
  }
  return '暂无明确卡点';
}

function resolvePrimaryAction(
  session: MessageFlowRecentSession,
  verdict: ReportingVerdict
): ReportingRecentDiagnosisAction {
  const target = resolveActionTarget(session, verdict);
  return {
    target,
    label: normalizeText(session.recommendedActionLabel) || resolveActionLabel(target)
  };
}

function resolveActionTarget(
  session: MessageFlowRecentSession,
  verdict: ReportingVerdict
): ReportingRecentActionTarget {
  const action = normalizeText(session.recommendedAction);
  if (action === 'message-trace' || action === 'system-log' || action === 'file-debug' || action === 'reporting') {
    return action;
  }
  if (verdict === 'failed') {
    return 'message-trace';
  }
  if (verdict === 'pending') {
    return 'reporting';
  }
  if (verdict === 'validated') {
    return 'file-debug';
  }
  return 'system-log';
}

function resolveActionLabel(target: ReportingRecentActionTarget): string {
  if (target === 'message-trace') {
    return '打开链路追踪';
  }
  if (target === 'system-log') {
    return '查看异常观测';
  }
  if (target === 'file-debug') {
    return '打开数据校验';
  }
  return '恢复当前复盘';
}

function normalizeText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}
