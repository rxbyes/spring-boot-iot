import { describe, expect, it } from 'vitest';

import { normalizeUnsafeIdJson, parseApiEnvelope } from '@/api/request';

describe('request id precision guard', () => {
  it('converts unsafe id-like numeric fields to strings before JSON parsing', () => {
    const raw =
      '{"code":200,"msg":"success","data":{"records":[{"id":2033720817103306754,"userId":2033720817103306755,"tenantId":1,"requestParams":"{\\"deviceId\\":2033720817103306756}"}]}}';

    const normalized = normalizeUnsafeIdJson(raw);

    expect(normalized).toContain('"id":"2033720817103306754"');
    expect(normalized).toContain('"userId":"2033720817103306755"');
    expect(normalized).toContain('"{\\"deviceId\\":2033720817103306756}"');

    const payload = parseApiEnvelope<{
      records: Array<{
        id: string;
        userId: string;
        tenantId: number;
        requestParams: string;
      }>;
    }>(raw);

    expect(payload?.data.records[0].id).toBe('2033720817103306754');
    expect(payload?.data.records[0].userId).toBe('2033720817103306755');
    expect(payload?.data.records[0].tenantId).toBe(1);
    expect(payload?.data.records[0].requestParams).toBe('{"deviceId":2033720817103306756}');
  });

  it('keeps already-safe or non-id numeric fields unchanged', () => {
    const raw =
      '{"code":200,"msg":"success","data":{"id":"2033720817103306754","parentId":900719925474099,"traceNo":2033720817103306754}}';

    const normalized = normalizeUnsafeIdJson(raw);
    const payload = parseApiEnvelope<{
      id: string;
      parentId: number;
      traceNo: number;
    }>(raw);

    expect(normalized).toContain('"id":"2033720817103306754"');
    expect(normalized).toContain('"parentId":900719925474099');
    expect(normalized).toContain('"traceNo":2033720817103306754');
    expect(payload?.data.id).toBe('2033720817103306754');
    expect(payload?.data.parentId).toBe(900719925474099);
  });
});
