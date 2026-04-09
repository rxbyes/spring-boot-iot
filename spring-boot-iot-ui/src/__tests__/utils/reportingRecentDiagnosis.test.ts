import { describe, expect, it } from 'vitest';

import {
  filterRecentDiagnosisItems,
  mapRecentSessionToDiagnosis
} from '@/views/reportingRecentDiagnosis';

describe('reportingRecentDiagnosis', () => {
  it('maps failed recent sessions into actionable diagnosis items', () => {
    const item = mapRecentSessionToDiagnosis({
      sessionId: 'session-failed-001',
      transportMode: 'MQTT',
      status: 'FAILED',
      submittedAt: '2026-03-29T11:00:00.000Z',
      traceId: 'trace-failed-001',
      deviceCode: 'demo-device-01',
      topic: '$dp',
      correlationPending: false,
      timelineAvailable: true,
      verdict: 'failed',
      timelineStatus: 'available',
      failureStage: 'PROTOCOL_DECODE',
      diagnosticReason: '协议解析失败',
      recommendedAction: 'message-trace',
      recommendedActionLabel: '打开链路追踪'
    });

    expect(item.verdict).toBe('failed');
    expect(item.statusLabel).toBe('失败');
    expect(item.blocker).toBe('PROTOCOL_DECODE 阶段失败');
    expect(item.summary).toBe('协议解析失败');
    expect(item.primaryAction).toEqual({
      target: 'message-trace',
      label: '打开链路追踪'
    });
  });

  it('derives pending diagnostics even when the backend has not filled verdict fields', () => {
    const item = mapRecentSessionToDiagnosis({
      sessionId: 'session-pending-001',
      transportMode: 'MQTT',
      status: 'PUBLISHED',
      submittedAt: new Date().toISOString(),
      traceId: '',
      deviceCode: 'demo-device-02',
      topic: '$dp',
      correlationPending: true,
      timelineAvailable: false
    });

    expect(item.verdict).toBe('pending');
    expect(item.statusLabel).toBe('等待回流');
    expect(item.summary).toBe('等待 trace 绑定');
    expect(item.primaryAction.target).toBe('reporting');
  });

  it('shows expired replay evidence as degraded and routes it to system log', () => {
    const item = mapRecentSessionToDiagnosis({
      sessionId: 'session-expired-001',
      transportMode: 'HTTP',
      status: 'COMPLETED',
      submittedAt: '2026-03-27T10:00:00.000Z',
      traceId: 'trace-expired-001',
      deviceCode: 'demo-device-03',
      topic: '/message/http/report',
      correlationPending: false,
      timelineAvailable: false,
      verdict: 'degraded',
      timelineStatus: 'expired',
      diagnosticReason: '时间线已过期'
    });

    expect(item.verdict).toBe('degraded');
    expect(item.statusLabel).toBe('已过期');
    expect(item.blocker).toBe('最近 trace 已超出保留窗口');
    expect(item.primaryAction.target).toBe('system-log');
  });

  it('groups degraded items into the failure review filter', () => {
    const failed = mapRecentSessionToDiagnosis({
      sessionId: 'session-failed-002',
      transportMode: 'MQTT',
      status: 'FAILED',
      submittedAt: '2026-03-29T11:00:00.000Z',
      traceId: 'trace-failed-002',
      deviceCode: 'demo-device-04',
      topic: '$dp',
      correlationPending: false,
      timelineAvailable: true,
      verdict: 'failed',
      timelineStatus: 'available'
    });
    const degraded = mapRecentSessionToDiagnosis({
      sessionId: 'session-missing-001',
      transportMode: 'HTTP',
      status: 'COMPLETED',
      submittedAt: '2026-03-29T11:00:00.000Z',
      traceId: 'trace-missing-001',
      deviceCode: 'demo-device-05',
      topic: '/message/http/report',
      correlationPending: false,
      timelineAvailable: false,
      verdict: 'degraded',
      timelineStatus: 'missing'
    });
    const pending = mapRecentSessionToDiagnosis({
      sessionId: 'session-pending-002',
      transportMode: 'MQTT',
      status: 'PUBLISHED',
      submittedAt: new Date().toISOString(),
      traceId: '',
      deviceCode: 'demo-device-06',
      topic: '$dp',
      correlationPending: true,
      timelineAvailable: false
    });

    const result = filterRecentDiagnosisItems([failed, degraded, pending], 'failed');

    expect(result.map((item) => item.sessionId)).toEqual(['session-failed-002', 'session-missing-001']);
  });
});
