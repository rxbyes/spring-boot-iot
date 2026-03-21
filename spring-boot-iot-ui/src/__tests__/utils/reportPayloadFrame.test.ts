import { describe, expect, it } from 'vitest';

import {
  buildPlaintextFrame,
  formatFrameDecimalPreview,
  formatFrameHexPreview,
  inferPlaintextDataFormatType
} from '@/views/reportPayloadFrame';

describe('reportPayloadFrame', () => {
  it('infers type 1 for ordinary property payload', () => {
    const result = inferPlaintextDataFormatType({
      messageType: 'property',
      properties: {
        temperature: 26.5
      }
    });

    expect(result.type).toBe(1);
    expect(result.label).toBe('C.1');
  });

  it('infers type 2 for timestamp-series payload', () => {
    const result = inferPlaintextDataFormatType({
      SK00FB0D1310195: {
        L1_SW_1: {
          '2026-03-20T08:07:22.000Z': {
            dispsX: -0.0257,
            dispsY: -0.0605
          }
        }
      }
    });

    expect(result.type).toBe(2);
    expect(result.label).toBe('C.2');
  });

  it('infers type 3 for file descriptor payload', () => {
    const result = inferPlaintextDataFormatType({
      did: 'device-file-1',
      ds_id: 'camera-image',
      file_type: 'jpg',
      at: '2018-08-02T10:52:32.449Z',
      desc: '0-1-256'
    });

    expect(result.type).toBe(3);
    expect(result.label).toBe('C.3');
  });

  it('builds big-endian frame header for type 2 payload', () => {
    const frame = buildPlaintextFrame(
      JSON.stringify({
        SK00FB0D1310195: {
          L1_SW_1: {
            '2026-03-20T08:07:22.000Z': {
              dispsX: -0.0257,
              dispsY: -0.0605
            }
          }
        }
      })
    );

    expect(frame.type).toBe(2);
    expect(frame.frameBytes[0]).toBe(2);
    expect(frame.frameBytes[1]).toBe((frame.jsonLength >> 8) & 0xff);
    expect(frame.frameBytes[2]).toBe(frame.jsonLength & 0xff);
    expect(frame.framedPayload.length).toBe(frame.frameBytes.length);
  });

  it('builds type 3 frame with file-stream length bytes', () => {
    const frame = buildPlaintextFrame(
      JSON.stringify({
        did: 'device-file-1',
        ds_id: 'camera-image',
        file_type: 'jpg',
        at: '2018-08-02T10:52:32.449Z',
        desc: '0-1-256'
      }),
      {
        type3BinaryBase64: 'AQIDBA=='
      }
    );

    const fileLengthIndex = 3 + frame.jsonLength;
    expect(frame.type).toBe(3);
    expect(frame.fileStreamLength).toBe(4);
    expect(frame.frameBytes[fileLengthIndex]).toBe(0);
    expect(frame.frameBytes[fileLengthIndex + 1]).toBe(4);
    expect(Array.from(frame.frameBytes.slice(fileLengthIndex + 2))).toEqual([1, 2, 3, 4]);
  });

  it('formats decimal and hex previews', () => {
    const frame = buildPlaintextFrame(
      JSON.stringify({
        messageType: 'property',
        properties: {
          temperature: 26.5
        }
      })
    );

    expect(formatFrameDecimalPreview(frame.frameBytes)).toContain('[');
    expect(formatFrameHexPreview(frame.frameBytes)).toContain('01');
  });

  it('throws when payload is invalid json', () => {
    expect(() => buildPlaintextFrame('not-json')).toThrow('Payload 不是有效 JSON');
  });
});
