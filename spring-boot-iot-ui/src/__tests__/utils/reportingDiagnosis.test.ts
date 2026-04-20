import { describe, expect, it } from 'vitest';

import { resolveReportingDiagnosis } from '@/views/reportingDiagnosis';

describe('reportingDiagnosis', () => {
  it('keeps the page in idle mode before any verification context exists', () => {
    const result = resolveReportingDiagnosis({});

    expect(result.verdict).toBe('idle');
    expect(result.reportStatus).toBe('ready');
    expect(result.title).toBe('排障起点');
    expect(result.blockerLabel).toBe('当前节点');
    expect(result.blocker).toBe('链路验证');
    expect(result.actions).toEqual([
      {
        target: 'simulate',
        label: '前往模拟验证'
      }
    ]);
  });

  it('marks mqtt correlation-pending sessions as pending', () => {
    const result = resolveReportingDiagnosis({
      sessionId: 'session-pending-001',
      transportMode: 'mqtt',
      messageFlowSession: {
        sessionId: 'session-pending-001',
        transportMode: 'MQTT',
        status: 'PUBLISHED',
        submittedAt: new Date().toISOString(),
        traceId: '',
        correlationPending: true,
        timeline: null,
        steps: []
      }
    });

    expect(result.verdict).toBe('pending');
    expect(result.reportStatus).toBe('pending');
    expect(result.title).toBe('当前节点：链路验证');
    expect(result.blocker).toBe('等待回流 / 异常观测');
    expect(result.actions.map((item) => item.target)).toEqual(['system-log', 'simulate']);
  });

  it('surfaces failed sessions as failed verdicts', () => {
    const result = resolveReportingDiagnosis({
      sessionId: 'session-failed-001',
      messageFlowSession: {
        sessionId: 'session-failed-001',
        transportMode: 'MQTT',
        status: 'FAILED',
        traceId: 'trace-failed-001',
        correlationPending: false,
        timeline: {
          traceId: 'trace-failed-001',
          sessionId: 'session-failed-001',
          status: 'FAILED',
          steps: [
            {
              stage: 'PROTOCOL_DECODE',
              status: 'FAILED',
              errorMessage: '协议解析失败'
            }
          ]
        }
      }
    });

    expect(result.verdict).toBe('failed');
    expect(result.reportStatus).toBe('failed');
    expect(result.title).toBe('当前节点：链路验证');
    expect(result.blocker).toBe('链路追踪 / 异常观测（先核对 PROTOCOL_DECODE）');
    expect(result.summary).toContain('协议解析失败');
    expect(result.actions.map((item) => item.target)).toEqual(['message-trace', 'system-log']);
  });

  it('maps timeline-missing replay states to degraded while preserving legacy report status', () => {
    const result = resolveReportingDiagnosis({
      sessionId: 'session-degraded-001',
      expectedTimeline: true,
      lookupMissing: true
    });

    expect(result.verdict).toBe('degraded');
    expect(result.reportStatus).toBe('timeline-missing');
    expect(result.title).toBe('当前节点：链路验证');
    expect(result.blocker).toBe('异常观测（时间线暂不可用）');
    expect(result.summary).toContain('时间线');
    expect(result.actions.map((item) => item.target)).toEqual(['system-log', 'simulate']);
  });

  it('treats trace-ready sessions as validated', () => {
    const result = resolveReportingDiagnosis({
      sessionId: 'session-validated-001',
      traceId: 'trace-validated-001',
      messageFlowSession: {
        sessionId: 'session-validated-001',
        transportMode: 'HTTP',
        status: 'COMPLETED',
        traceId: 'trace-validated-001',
        correlationPending: false,
        timeline: {
          traceId: 'trace-validated-001',
          sessionId: 'session-validated-001',
          status: 'COMPLETED',
          steps: [
            {
              stage: 'COMPLETE',
              status: 'SUCCESS'
            }
          ]
        }
      }
    });

    expect(result.verdict).toBe('validated');
    expect(result.reportStatus).toBe('validated');
    expect(result.title).toBe('当前节点：链路验证');
    expect(result.blocker).toBe('链路追踪台 / 数据校验');
    expect(result.actions.map((item) => item.target)).toEqual(['message-trace', 'file-debug']);
  });
});
