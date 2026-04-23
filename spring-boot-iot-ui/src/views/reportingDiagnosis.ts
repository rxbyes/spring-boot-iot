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
      title: '排障起点',
      summary: '先发起一次模拟验证，再决定进入哪一条诊断分支。',
      blockerLabel: '当前节点',
      blocker: '链路验证',
      actions: [{ target: 'simulate', label: '前往模拟验证' }]
    });
  }

  if (isFailed(session, timeline, failedStep)) {
    const failedStage = normalizeText(failedStep?.stage);
    return buildDiagnosis({
      verdict: 'failed',
      title: '当前节点：链路验证',
      summary: normalizeText(failedStep?.errorMessage) || '当前验证失败，下一步进入链路追踪或异常观测定位证据。',
      blockerLabel: '下一步',
      blocker: failedStage ? `链路追踪 / 异常观测（先核对 ${failedStage}）` : '链路追踪 / 异常观测',
      actions: [
        { target: 'message-trace', label: '继续链路追踪' },
        { target: 'system-log', label: '查看异常观测' }
      ]
    });
  }

  if (transportMode === 'mqtt' && Boolean(session?.correlationPending) && !traceId) {
    return buildDiagnosis({
      verdict: 'pending',
      title: '当前节点：链路验证',
      summary: 'MQTT 模拟已发出，下一步等待消费回流完成 trace 绑定，超时后转异常观测继续排查。',
      blockerLabel: '下一步',
      blocker: '等待回流 / 异常观测',
      actions: [
        { target: 'system-log', label: '查看异常观测' },
        { target: 'simulate', label: '返回模拟验证' }
      ]
    });
  }

  if (hasText(params.lookupError) || (Boolean(params.lookupMissing) && Boolean(params.expectedTimeline))) {
    const degradedReason = normalizeText(params.lookupError) || '时间线暂不可用';
    return buildDiagnosis({
      verdict: 'degraded',
      title: '当前节点：链路验证',
      summary: normalizeText(params.lookupError) || '当前验证已完成，但时间线暂不可用；下一步先去异常观测，再决定是否继续链路追踪。',
      blockerLabel: '下一步',
      blocker: `异常观测（${degradedReason}）`,
      actions: [
        { target: 'system-log', label: '查看异常观测' },
        { target: 'simulate', label: '返回模拟验证' }
      ]
    });
  }

  return buildDiagnosis({
    verdict: 'validated',
    title: '当前节点：链路验证',
    summary: '本轮链路验证已完成，下一步进入链路追踪台复盘固定 Pipeline。',
    blockerLabel: '下一步',
    blocker: '链路追踪台 / 数据校验',
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
