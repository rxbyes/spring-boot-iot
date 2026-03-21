import { describe, expect, it } from 'vitest';

import type { HttpReportPayload } from '@/types/api';
import {
  evaluateReportWorkbenchInput,
  filterTemplatesByMode
} from '@/views/reportWorkbenchState';

function createPayload(overrides: Partial<HttpReportPayload> = {}): HttpReportPayload {
  return {
    protocolCode: 'mqtt-json',
    productKey: 'demo-product',
    deviceCode: 'demo-device-01',
    payload: JSON.stringify({
      messageType: 'property',
      properties: {
        temperature: 26.5
      }
    }),
    topic: '',
    clientId: 'demo-device-01',
    tenantId: '1',
    ...overrides
  };
}

describe('reportWorkbenchState', () => {
  it('blocks sending when required fields are missing', () => {
    const result = evaluateReportWorkbenchInput({
      mode: 'plaintext',
      report: createPayload({
        protocolCode: '',
        productKey: '',
        deviceCode: '',
        payload: ''
      })
    });

    expect(result.canSend).toBe(false);
    expect(result.validationIssues.map((item) => item.field)).toEqual(
      expect.arrayContaining(['protocolCode', 'productKey', 'deviceCode', 'payload'])
    );
  });

  it('blocks plaintext mode when payload json is invalid', () => {
    const result = evaluateReportWorkbenchInput({
      mode: 'plaintext',
      report: createPayload({
        payload: '{bad-json}'
      })
    });

    expect(result.canSend).toBe(false);
    expect(result.plaintextFrame).toBeNull();
    expect(result.validationIssues.some((item) => item.field === 'payload')).toBe(true);
  });

  it('supports valid plaintext type2 and recommends $dp', () => {
    const result = evaluateReportWorkbenchInput({
      mode: 'plaintext',
      report: createPayload({
        payload: JSON.stringify({
          SK00FB0D1310195: {
            L1_SW_1: {
              '2026-03-20T08:07:22.000Z': {
                dispsX: -0.0257
              }
            }
          }
        })
      })
    });

    expect(result.canSend).toBe(true);
    expect(result.plaintextFrame?.type).toBe(2);
    expect(result.recommendedTopic).toBe('$dp');
  });

  it('validates encrypted envelope structure', () => {
    const invalid = evaluateReportWorkbenchInput({
      mode: 'encrypted',
      report: createPayload({
        payload: JSON.stringify({
          header: {},
          bodies: {}
        })
      })
    });

    expect(invalid.canSend).toBe(false);
    expect(invalid.validationIssues.some((item) => item.field === 'encryptedEnvelope')).toBe(true);

    const valid = evaluateReportWorkbenchInput({
      mode: 'encrypted',
      report: createPayload({
        payload: JSON.stringify({
          header: { appId: '62000001' },
          bodies: { body: 'PTOLy04o/stDufUYFo5s3g==' }
        })
      })
    });

    expect(valid.canSend).toBe(true);
    expect(valid.recommendedTopic).toBe('$dp');
  });

  it('filters templates by mode', () => {
    const templates = [
      { name: '明文模板', mode: 'plaintext' as const },
      { name: '密文模板', mode: 'encrypted' as const }
    ];

    expect(filterTemplatesByMode(templates, 'plaintext').map((item) => item.name)).toEqual(['明文模板']);
    expect(filterTemplatesByMode(templates, 'encrypted').map((item) => item.name)).toEqual(['密文模板']);
  });
});
