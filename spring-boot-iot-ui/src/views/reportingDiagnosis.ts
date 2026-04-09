import type { MessageFlowSession, MessageFlowStep, MessageFlowTimeline } from '@/types/api';
import type { DiagnosticReportStatus } from '@/utils/iotAccessDiagnostics';

export type ReportingVerdict = 'idle' | 'validated' | 'pending' | 'failed' | 'degraded';
export type ReportingActionTarget = 'simulate' | 'message-trace' | 'system-log' | 'file-debug';

export interface ReportingDiagnosisAction {
  label: string;
  target: ReportingActionTarget;
}

export interface ReportingDiagnosisResult {
  verdict: ReportingVerdict;
  reportStatus: DiagnosticReportStatus;
  title: string;
  summary: string;
  blockerLabel: string;
  blocker: string;
  actions: ReportingDiagnosisAction[];
  primaryAction: ReportingDiagnosisAction | null;
}

export interface ResolveReportingDiagnosisParams {
  sessionId?: string;
  traceId?: string;
  transportMode?: 'http' | 'mqtt' | null;
  lookupError?: string;
  lookupMissing?: boolean;
  expectedTimeline?: boolean;
  messageFlowSession?: MessageFlowSession | null;
  session?: MessageFlowSession | null;
}

export function resolveReportingDiagnosis(
  params: ResolveReportingDiagnosisParams = {}
): ReportingDiagnosisResult {
  const session = params.messageFlowSession || params.session || null;
  const traceId = normalizeText(params.traceId) || normalizeText(session?.traceId);
  const sessionId = normalizeText(params.sessionId) || normalizeText(session?.sessionId);
  const transportMode = normalizeTransportMode(params.transportMode) || normalizeTransportMode(session?.transportMode);
  const timeline = normalizeTimeline(session?.timeline);
  const failedStep = resolveFailedStep(timeline);
  const hasReplayContext = Boolean(sessionId || traceId);

  if (!hasReplayContext) {
    return buildDiagnosis({
      verdict: 'idle',
      title: '尚未验证',
      summary: '先发起一次模拟验证，再进入结果复盘和最近记录。',
      blockerLabel: '下一步',
      blocker: '先发送模拟验证',
      actions: [{ target: 'simulate', label: '前往模拟验证' }]
    });
  }

  if (isFailed(session, timeline, failedStep)) {
    return buildDiagnosis({
      verdict: 'failed',
      title: '验证失败',
      summary: normalizeText(failedStep?.errorMessage) || '主链路存在失败阶段，请继续追踪并排查异常日志。',
      blockerLabel: '当前卡点',
      blocker: normalizeText(failedStep?.stage) || '主链路存在失败阶段',
      actions: [
        { target: 'message-trace', label: '继续链路追踪' },
        { target: 'system-log', label: '查看异常观测' }
      ]
    });
  }

  if (transportMode === 'mqtt' && Boolean(session?.correlationPending) && !traceId) {
    return buildDiagnosis({
      verdict: 'pending',
      title: '等待回流',
      summary: 'MQTT 模拟已发出，正在等待消费链路完成 trace 绑定。',
      blockerLabel: '当前卡点',
      blocker: '等待 trace 绑定',
      actions: [
        { target: 'system-log', label: '查看异常观测' },
        { target: 'simulate', label: '返回模拟验证' }
      ]
    });
  }

  if (hasText(params.lookupError) || (Boolean(params.lookupMissing) && Boolean(params.expectedTimeline))) {
    return buildDiagnosis({
      verdict: 'degraded',
      title: '复盘受限',
      summary: normalizeText(params.lookupError) || '当前时间线暂不可用，但仍可带着上下文继续排查。',
      blockerLabel: '当前卡点',
      blocker: normalizeText(params.lookupError) || '时间线暂不可用',
      actions: [
        { target: 'system-log', label: '查看异常观测' },
        { target: 'simulate', label: '返回模拟验证' }
      ]
    });
  }

  return buildDiagnosis({
    verdict: 'validated',
    title: '验证成功',
    summary: '已拿到完整复盘上下文，可以继续查看固定 Pipeline 和下游证据。',
    blockerLabel: '当前卡点',
    blocker: '已拿到完整复盘结果',
    actions: [
      { target: 'message-trace', label: '继续链路追踪' },
      { target: 'file-debug', label: '打开数据校验' }
    ]
  });
}

export function deriveReportingDiagnosis(
  params: ResolveReportingDiagnosisParams = {}
): ReportingDiagnosisResult {
  return resolveReportingDiagnosis(params);
}

export function mapReportingVerdictToReportStatus(verdict: ReportingVerdict): DiagnosticReportStatus {
  if (verdict === 'validated') {
    return 'validated';
  }
  if (verdict === 'pending') {
    return 'pending';
  }
  if (verdict === 'failed') {
    return 'failed';
  }
  if (verdict === 'degraded') {
    return 'timeline-missing';
  }
  return 'ready';
}

function buildDiagnosis(params: {
  verdict: ReportingVerdict;
  title: string;
  summary: string;
  blockerLabel: string;
  blocker: string;
  actions: ReportingDiagnosisAction[];
}): ReportingDiagnosisResult {
  return {
    verdict: params.verdict,
    reportStatus: mapReportingVerdictToReportStatus(params.verdict),
    title: params.title,
    summary: params.summary,
    blockerLabel: params.blockerLabel,
    blocker: params.blocker,
    actions: params.actions,
    primaryAction: params.actions[0] || null
  };
}

function isFailed(
  session: MessageFlowSession | null,
  timeline: MessageFlowTimeline | null,
  failedStep: MessageFlowStep | null
): boolean {
  return normalizeText(session?.status) === 'FAILED'
    || normalizeText(timeline?.status) === 'FAILED'
    || Boolean(failedStep);
}

function resolveFailedStep(timeline: MessageFlowTimeline | null): MessageFlowStep | null {
  const steps = Array.isArray(timeline?.steps) ? timeline?.steps : [];
  for (let index = steps.length - 1; index >= 0; index -= 1) {
    const step = steps[index];
    if (normalizeText(step?.status) === 'FAILED') {
      return step;
    }
  }
  return null;
}

function normalizeTimeline(timeline: MessageFlowTimeline | null | undefined): MessageFlowTimeline | null {
  if (!timeline) {
    return null;
  }
  return {
    ...timeline,
    steps: Array.isArray(timeline.steps) ? timeline.steps : []
  };
}

function normalizeTransportMode(value: unknown): 'http' | 'mqtt' | null {
  const normalized = normalizeText(value).toLowerCase();
  if (normalized === 'http' || normalized === 'mqtt') {
    return normalized;
  }
  return null;
}

function normalizeText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function hasText(value: unknown): boolean {
  return normalizeText(value).length > 0;
}
