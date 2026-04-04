import { describe, expect, it } from 'vitest';

import { resolveMessageTracePayloadComparison } from '@/utils/messageTracePayloadComparison';

describe('resolveMessageTracePayloadComparison', () => {
  it('builds three panels from raw payload and protocol decode previews', () => {
    const model = resolveMessageTracePayloadComparison({
      rawPayload: '{"cipher":true}',
      timelineExpired: false,
      timeline: {
        traceId: 'trace-001',
        steps: [
          {
            stage: 'PROTOCOL_DECODE',
            summary: {
              decryptedPayloadPreview: '{"temperature":26.5}',
              decodedPayloadPreview: {
                messageType: 'property',
                deviceCode: 'demo-device-01',
                properties: { temperature: 26.5 }
              }
            }
          }
        ]
      } as never
    });

    expect(model.panels.map((item) => item.title)).toEqual(['原始 Payload', '解密后明文', '解析结果']);
    expect(model.panels[0]?.content).toContain('"cipher": true');
    expect(model.panels[1]?.content).toContain('"temperature": 26.5');
    expect(model.panels[2]?.content).toContain('"deviceCode": "demo-device-01"');
  });

  it('keeps the decrypted panel explicit when only the decoded preview is available', () => {
    const model = resolveMessageTracePayloadComparison({
      rawPayload: '{"temperature":26.5}',
      timelineExpired: false,
      timeline: {
        traceId: 'trace-002',
        steps: [
          {
            stage: 'PROTOCOL_DECODE',
            summary: {
              decodedPayloadPreview: {
                messageType: 'property',
                deviceCode: 'demo-device-01',
                properties: { temperature: 26.5 }
              }
            }
          }
        ]
      } as never
    });

    expect(model.panels[1]?.available).toBe(false);
    expect(model.panels[1]?.emptyText).toBe('当前无解密结果');
    expect(model.panels[2]?.content).toContain('"temperature": 26.5');
  });

  it('keeps explicit placeholders when the timeline is expired', () => {
    const model = resolveMessageTracePayloadComparison({
      rawPayload: '{"temperature":26.5}',
      timelineExpired: true,
      timeline: null
    });

    expect(model.panels[0]?.content).toContain('"temperature": 26.5');
    expect(model.panels[1]?.emptyText).toBe('当前时间线已过期，无法恢复解密结果');
    expect(model.panels[2]?.emptyText).toBe('当前时间线已过期，无法恢复解析结果');
  });
});
