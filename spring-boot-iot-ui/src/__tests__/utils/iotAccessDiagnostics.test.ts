import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  loadDiagnosticContext,
  persistDiagnosticContext,
  resolveDiagnosticContext
} from '@/utils/iotAccessDiagnostics';

describe('iotAccessDiagnostics', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.sessionStorage.clear();
  });

  it('keeps only query-safe keys in route query', () => {
    const query = buildDiagnosticRouteQuery({
      sourcePage: 'reporting',
      deviceCode: 'device-001',
      traceId: 'trace-001',
      productKey: 'product-001',
      topic: '$dp',
      sessionId: 'session-001',
      transportMode: 'mqtt',
      reportStatus: 'sent',
      capturedAt: '2026-03-28T10:00:00Z'
    });

    expect(query).toEqual({
      deviceCode: 'device-001',
      traceId: 'trace-001',
      productKey: 'product-001',
      topic: '$dp'
    });
  });

  it('persists and restores context, while route query overrides stored values', () => {
    persistDiagnosticContext({
      sourcePage: 'message-trace',
      deviceCode: 'stored-device',
      traceId: 'stored-trace',
      productKey: 'stored-product',
      topic: 'stored-topic',
      sessionId: 'stored-session',
      transportMode: 'mqtt',
      reportStatus: 'pending',
      capturedAt: '2026-03-28T10:00:00Z'
    });

    expect(loadDiagnosticContext()).toMatchObject({
      sourcePage: 'message-trace',
      deviceCode: 'stored-device',
      traceId: 'stored-trace',
      productKey: 'stored-product',
      topic: 'stored-topic',
      sessionId: 'stored-session',
      transportMode: 'mqtt',
      reportStatus: 'pending',
      capturedAt: '2026-03-28T10:00:00Z'
    });

    const merged = resolveDiagnosticContext({
      deviceCode: 'query-device',
      topic: 'query-topic'
    });

    expect(merged).toMatchObject({
      sourcePage: 'message-trace',
      deviceCode: 'query-device',
      traceId: 'stored-trace',
      productKey: 'stored-product',
      topic: 'query-topic',
      sessionId: 'stored-session',
      transportMode: 'mqtt',
      reportStatus: 'pending',
      capturedAt: '2026-03-28T10:00:00Z'
    });
  });

  it('returns null when resolving without stored context', () => {
    const resolved = resolveDiagnosticContext({
      deviceCode: 'query-device'
    });
    expect(resolved).toBeNull();
  });

  it('returns null when resolving without diagnostic query keys even if stored context exists', () => {
    persistDiagnosticContext({
      sourcePage: 'reporting',
      deviceCode: 'stored-device',
      sessionId: 'stored-session',
      transportMode: 'http',
      reportStatus: 'ready',
      capturedAt: '2026-03-28T10:00:00Z'
    });

    const resolved = resolveDiagnosticContext({});
    expect(resolved).toBeNull();
  });

  it('cleans up stale session context after ttl', () => {
    const nowSpy = vi.spyOn(Date, 'now');
    nowSpy.mockReturnValue(1_000_000);

    persistDiagnosticContext({
      sourcePage: 'reporting',
      deviceCode: 'device-ttl',
      capturedAt: '2026-03-28T10:00:00Z'
    });

    nowSpy.mockReturnValue(1_000_000 + 30 * 60 * 1000 + 1);
    expect(loadDiagnosticContext()).toBeNull();
    expect(window.sessionStorage.getItem('iot-access:diagnostic-context')).toBeNull();
  });

  it('cleans malformed storage json without throwing', () => {
    window.sessionStorage.setItem('iot-access:diagnostic-context', '{bad-json');

    expect(loadDiagnosticContext()).toBeNull();
    expect(window.sessionStorage.getItem('iot-access:diagnostic-context')).toBeNull();
  });

  it('isolates sessionStorage exceptions for persist and load', () => {
    const storageProto = Object.getPrototypeOf(window.sessionStorage) as Storage;
    const setItemSpy = vi.spyOn(storageProto, 'setItem').mockImplementation(() => {
      throw new Error('setItem blocked');
    });

    expect(() =>
      persistDiagnosticContext({
        sourcePage: 'reporting',
        deviceCode: 'device-001',
        transportMode: 'http',
        reportStatus: 'ready',
        capturedAt: '2026-03-28T10:00:00Z'
      })
    ).not.toThrow();

    setItemSpy.mockRestore();

    vi.spyOn(storageProto, 'getItem').mockImplementation(() => {
      throw new Error('getItem blocked');
    });

    expect(loadDiagnosticContext()).toBeNull();
  });

  it('degrades gracefully when sessionStorage getter throws', () => {
    vi.spyOn(window, 'sessionStorage', 'get').mockImplementation(() => {
      throw new Error('sessionStorage blocked');
    });

    expect(() =>
      persistDiagnosticContext({
        sourcePage: 'reporting',
        deviceCode: 'device-001',
        transportMode: 'http',
        reportStatus: 'ready',
        capturedAt: '2026-03-28T10:00:00Z'
      })
    ).not.toThrow();

    expect(loadDiagnosticContext()).toBeNull();
  });

  it('maps source labels for compact strip copy', () => {
    expect(describeDiagnosticSource('reporting')).toBe('链路验证中心');
    expect(describeDiagnosticSource('access-error')).toBe('失败归档');
    expect(describeDiagnosticSource('system-log')).toBe('异常观测台');
  });
});
