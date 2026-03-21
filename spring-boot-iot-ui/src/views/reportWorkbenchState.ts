import type { HttpReportPayload } from '../types/api';
import type { PlaintextFrameBuildResult } from './reportPayloadFrame';
import { buildPlaintextFrame } from './reportPayloadFrame';

export type ReportMode = 'plaintext' | 'encrypted';

export interface ReportWorkbenchInput {
  report: HttpReportPayload;
  mode: ReportMode;
  type3BinaryBase64?: string;
}

export interface ValidationIssue {
  field: 'protocolCode' | 'productKey' | 'deviceCode' | 'payload' | 'encryptedEnvelope';
  message: string;
}

export interface ReportWorkbenchEvaluation {
  canSend: boolean;
  recommendedTopic: string;
  parsedPayload: Record<string, unknown> | null;
  payloadMessageType: string;
  plaintextFrame: PlaintextFrameBuildResult | null;
  plaintextFrameError: string;
  validationIssues: ValidationIssue[];
}

const EMPTY_MESSAGE_TYPE = 'property';

export function evaluateReportWorkbenchInput(input: ReportWorkbenchInput): ReportWorkbenchEvaluation {
  const validationIssues: ValidationIssue[] = [];

  if (!hasText(input.report.protocolCode)) {
    validationIssues.push({ field: 'protocolCode', message: '协议编码不能为空。' });
  }
  if (!hasText(input.report.productKey)) {
    validationIssues.push({ field: 'productKey', message: '产品 Key 不能为空。' });
  }
  if (!hasText(input.report.deviceCode)) {
    validationIssues.push({ field: 'deviceCode', message: '设备编码不能为空。' });
  }
  if (!hasText(input.report.payload)) {
    validationIssues.push({ field: 'payload', message: 'Payload 不能为空。' });
  }

  let parsedPayload: Record<string, unknown> | null = null;
  let payloadMessageType = EMPTY_MESSAGE_TYPE;
  let plaintextFrame: PlaintextFrameBuildResult | null = null;
  let plaintextFrameError = '';

  if (input.mode === 'plaintext') {
    try {
      plaintextFrame = buildPlaintextFrame(input.report.payload, {
        type3BinaryBase64: input.type3BinaryBase64
      });
      parsedPayload = JSON.parse(plaintextFrame.normalizedJson) as Record<string, unknown>;
      payloadMessageType = resolvePayloadMessageType(parsedPayload);
    } catch (error) {
      plaintextFrameError = (error as Error).message;
      validationIssues.push({
        field: 'payload',
        message: plaintextFrameError
      });
    }
  } else if (hasText(input.report.payload)) {
    parsedPayload = parseJsonObject(input.report.payload);
    if (!parsedPayload) {
      validationIssues.push({
        field: 'payload',
        message: '密文封包不是有效 JSON。'
      });
    } else {
      validateEncryptedEnvelope(parsedPayload, validationIssues);
      payloadMessageType = resolvePayloadMessageType(parsedPayload);
    }
  }

  const recommendedTopic = resolveRecommendedTopic({
    mode: input.mode,
    productKey: input.report.productKey,
    deviceCode: input.report.deviceCode,
    payloadMessageType,
    plaintextFrameType: plaintextFrame?.type
  });

  return {
    canSend: validationIssues.length === 0,
    recommendedTopic,
    parsedPayload,
    payloadMessageType,
    plaintextFrame,
    plaintextFrameError,
    validationIssues
  };
}

export function filterTemplatesByMode<T extends { mode: ReportMode }>(templates: T[], mode: ReportMode): T[] {
  return templates.filter((item) => item.mode === mode);
}

function resolveRecommendedTopic(params: {
  mode: ReportMode;
  productKey: string;
  deviceCode: string;
  payloadMessageType: string;
  plaintextFrameType?: number;
}): string {
  if (params.mode === 'encrypted') {
    return '$dp';
  }
  if (params.plaintextFrameType && params.plaintextFrameType !== 1) {
    return '$dp';
  }
  const suffix =
    params.payloadMessageType === 'status'
      ? 'thing/status/post'
      : params.payloadMessageType === 'event'
        ? 'thing/event/post'
        : 'thing/property/post';
  return `/sys/${params.productKey}/${params.deviceCode}/${suffix}`;
}

function validateEncryptedEnvelope(payload: Record<string, unknown>, issues: ValidationIssue[]) {
  const header = asPlainObject(payload.header);
  const bodies = asPlainObject(payload.bodies);
  const appId = header ? normalizeText(header.appId) : '';
  const body = bodies ? normalizeText(bodies.body) : '';

  if (!appId) {
    issues.push({
      field: 'encryptedEnvelope',
      message: '密文封包缺少 header.appId。'
    });
  }
  if (!body) {
    issues.push({
      field: 'encryptedEnvelope',
      message: '密文封包缺少 bodies.body。'
    });
  }
}

function resolvePayloadMessageType(payload: Record<string, unknown> | null): string {
  if (!payload) {
    return EMPTY_MESSAGE_TYPE;
  }
  const messageType = normalizeText(payload.messageType);
  return messageType || EMPTY_MESSAGE_TYPE;
}

function parseJsonObject(payloadText: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(payloadText);
    return asPlainObject(parsed);
  } catch {
    return null;
  }
}

function hasText(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

function normalizeText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function asPlainObject(value: unknown): Record<string, unknown> | null {
  if (Object.prototype.toString.call(value) !== '[object Object]') {
    return null;
  }
  return value as Record<string, unknown>;
}
