import type { MessageFlowTimeline, ProtocolDecodeTimelineSummary } from '@/types/api';
import { prettyJson } from '@/utils/format';

const PROTOCOL_DECODE_STAGE = 'PROTOCOL_DECODE';

export interface MessageTracePayloadComparisonPanel {
  key: 'raw' | 'decrypted' | 'decoded';
  title: string;
  description: string;
  content: string;
  emptyText: string;
  available: boolean;
}

export interface MessageTracePayloadComparisonModel {
  panels: MessageTracePayloadComparisonPanel[];
}

export function resolveMessageTracePayloadComparison(input: {
  rawPayload?: string | null;
  timeline?: MessageFlowTimeline | null;
  timelineExpired: boolean;
}): MessageTracePayloadComparisonModel {
  const summary = resolveProtocolDecodeSummary(input.timeline);

  return {
    panels: [
      buildTextPanel('raw', '原始 Payload', '保留消息日志中的原始报文。', input.rawPayload, '当前无原始 Payload'),
      buildTextPanel(
        'decrypted',
        '解密后明文',
        '展示协议解码阶段拿到的明文快照。',
        summary?.decryptedPayloadPreview,
        input.timelineExpired ? '当前时间线已过期，无法恢复解密结果' : '当前无解密结果'
      ),
      buildJsonPanel(
        'decoded',
        '解析结果',
        '展示协议层归一化后的结构化上行结果。',
        summary?.decodedPayloadPreview,
        input.timelineExpired ? '当前时间线已过期，无法恢复解析结果' : '当前无解析结果'
      )
    ]
  };
}

function resolveProtocolDecodeSummary(timeline?: MessageFlowTimeline | null): ProtocolDecodeTimelineSummary | null {
  const decodeStep = timeline?.steps?.find((step) => step.stage === PROTOCOL_DECODE_STAGE);
  if (!decodeStep?.summary || typeof decodeStep.summary !== 'object') {
    return null;
  }
  return decodeStep.summary as ProtocolDecodeTimelineSummary;
}

function buildTextPanel(
  key: MessageTracePayloadComparisonPanel['key'],
  title: string,
  description: string,
  value: unknown,
  emptyText: string
): MessageTracePayloadComparisonPanel {
  const content = normalizeTextContent(value);

  return {
    key,
    title,
    description,
    content,
    emptyText,
    available: Boolean(content)
  };
}

function buildJsonPanel(
  key: MessageTracePayloadComparisonPanel['key'],
  title: string,
  description: string,
  value: unknown,
  emptyText: string
): MessageTracePayloadComparisonPanel {
  const content = normalizeJsonContent(value);

  return {
    key,
    title,
    description,
    content,
    emptyText,
    available: Boolean(content)
  };
}

function normalizeTextContent(value: unknown): string {
  if (typeof value !== 'string') {
    return '';
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return '';
  }
  return prettyJson(trimmed);
}

function normalizeJsonContent(value: unknown): string {
  if (value == null) {
    return '';
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    return trimmed ? prettyJson(trimmed) : '';
  }
  if (Array.isArray(value)) {
    return value.length ? prettyJson(value) : '';
  }
  if (typeof value === 'object') {
    return Object.keys(value as Record<string, unknown>).length ? prettyJson(value) : '';
  }
  return prettyJson(value);
}
