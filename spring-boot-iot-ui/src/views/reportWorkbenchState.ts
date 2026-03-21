import type { HttpReportPayload } from '../types/api';
import { looksLikeJson, looksLikeXml, prettyJson, prettyXml } from '../utils/format';
import type { PlaintextFrameBuildResult } from './reportPayloadFrame';
import { buildPlaintextFrame, inferPlaintextDataFormatType } from './reportPayloadFrame';

export type ReportMode = 'plaintext' | 'encrypted';
export type TransportMode = 'http' | 'mqtt';
export type PayloadFormat = 'json' | 'xml' | 'text';

export interface ReportWorkbenchInput {
  report: HttpReportPayload;
  mode: ReportMode;
  transportMode?: TransportMode;
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
  payloadFormat: PayloadFormat;
  actualPayloadFormat: PayloadFormat;
  actualPayload: string;
  actualPayloadPreview: string;
  actualPayloadEncoding: string;
  diagnosticNotes: string[];
  autoInjectedDeviceCode: boolean;
}

const EMPTY_MESSAGE_TYPE = 'property';
const MQTT_DP_TOPIC = '$dp';
const DEVICE_IDENTITY_KEYS = new Set([
  'deviceCode',
  'device_code',
  'deviceId',
  'device_id',
  'devId',
  'dev_id',
  'imei',
  'sn',
  'did',
  'gatewayDeviceCode',
  'subDeviceCode'
]);

export function evaluateReportWorkbenchInput(input: ReportWorkbenchInput): ReportWorkbenchEvaluation {
  const validationIssues: ValidationIssue[] = [];
  const diagnosticNotes: string[] = [];
  const transportMode = input.transportMode ?? 'http';
  const payloadText = typeof input.report.payload === 'string' ? input.report.payload : '';
  const payloadFormat = resolvePayloadFormat(payloadText);

  if (!hasText(input.report.protocolCode)) {
    validationIssues.push({ field: 'protocolCode', message: '协议编码不能为空。' });
  }
  if (!hasText(input.report.productKey)) {
    validationIssues.push({ field: 'productKey', message: '产品 Key 不能为空。' });
  }
  if (!hasText(input.report.deviceCode)) {
    validationIssues.push({ field: 'deviceCode', message: '设备编码不能为空。' });
  }
  if (!hasText(payloadText)) {
    validationIssues.push({ field: 'payload', message: 'Payload 不能为空。' });
  }

  let parsedPayload: Record<string, unknown> | null = null;
  let payloadMessageType = EMPTY_MESSAGE_TYPE;
  let plaintextFrame: PlaintextFrameBuildResult | null = null;
  let plaintextFrameError = '';
  let actualPayload = payloadText;
  let actualPayloadPreview = buildPayloadPreview(payloadText, payloadFormat);
  let actualPayloadFormat = payloadFormat;
  let actualPayloadEncoding = '';
  let autoInjectedDeviceCode = false;

  if (input.mode === 'plaintext') {
    if (looksLikeJson(payloadText)) {
      try {
        const preparedPayload = preparePlaintextPayload(input, transportMode);
        parsedPayload = preparedPayload.payload;
        payloadMessageType = resolvePayloadMessageType(parsedPayload);
        autoInjectedDeviceCode = preparedPayload.autoInjectedDeviceCode;
        if (autoInjectedDeviceCode) {
          diagnosticNotes.push('当前为 MQTT + $dp + 普通属性 JSON，已自动补入 deviceCode。');
        }

        plaintextFrame = buildPlaintextFrame(preparedPayload.payloadText, {
          type3BinaryBase64: input.type3BinaryBase64
        });
        actualPayload = plaintextFrame.framedPayload;
        actualPayloadPreview = prettyJson(parsedPayload);
        actualPayloadFormat = 'json';
        actualPayloadEncoding = 'ISO-8859-1';
        diagnosticNotes.push(
          `当前明文 JSON 已识别为 ${plaintextFrame.label}，发送时会按 ISO-8859-1 单字节编码透传。`
        );
      } catch (error) {
        plaintextFrameError = (error as Error).message;
        validationIssues.push({
          field: 'payload',
          message: plaintextFrameError
        });
      }
    } else if (looksLikeXml(payloadText)) {
      diagnosticNotes.push('当前 XML 仅按原始文本发送，不参与 C.1 / C.2 / C.3 帧识别。');
    } else if (hasText(payloadText)) {
      diagnosticNotes.push('当前文本不会构造明文帧，将按原始内容发送。');
    }
  } else if (hasText(payloadText)) {
    parsedPayload = parseJsonObject(payloadText);
    actualPayloadPreview = prettyJson(payloadText);
    actualPayloadFormat = parsedPayload ? 'json' : payloadFormat;
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
    validationIssues,
    payloadFormat,
    actualPayloadFormat,
    actualPayload,
    actualPayloadPreview,
    actualPayloadEncoding,
    diagnosticNotes,
    autoInjectedDeviceCode
  };
}

export function filterTemplatesByMode<T extends { mode: ReportMode }>(templates: T[], mode: ReportMode): T[] {
  return templates.filter((item) => item.mode === mode);
}

function preparePlaintextPayload(
  input: ReportWorkbenchInput,
  transportMode: TransportMode
): { payload: Record<string, unknown>; payloadText: string; autoInjectedDeviceCode: boolean } {
  const parsedPayload = parsePayloadAsObject(input.report.payload);
  const shouldInjectDeviceCode = shouldInjectDeviceCodeForMqttDp({
    payload: parsedPayload,
    topic: input.report.topic,
    deviceCode: input.report.deviceCode,
    transportMode
  });

  if (!shouldInjectDeviceCode) {
    return {
      payload: parsedPayload,
      payloadText: JSON.stringify(parsedPayload),
      autoInjectedDeviceCode: false
    };
  }

  const payloadWithDeviceCode = {
    ...parsedPayload,
    deviceCode: normalizeText(input.report.deviceCode)
  };

  return {
    payload: payloadWithDeviceCode,
    payloadText: JSON.stringify(payloadWithDeviceCode),
    autoInjectedDeviceCode: true
  };
}

function shouldInjectDeviceCodeForMqttDp(params: {
  payload: Record<string, unknown>;
  topic?: string;
  deviceCode: string;
  transportMode: TransportMode;
}): boolean {
  if (params.transportMode !== 'mqtt') {
    return false;
  }
  if (normalizeText(params.topic) !== MQTT_DP_TOPIC) {
    return false;
  }
  if (!normalizeText(params.deviceCode)) {
    return false;
  }
  if (hasAnyDeviceIdentityField(params.payload)) {
    return false;
  }

  const messageType = resolvePayloadMessageType(params.payload);
  if (messageType !== EMPTY_MESSAGE_TYPE) {
    return false;
  }

  return inferPlaintextDataFormatType(params.payload).type === 1;
}

function hasAnyDeviceIdentityField(payload: Record<string, unknown>): boolean {
  return Object.keys(payload).some((key) => DEVICE_IDENTITY_KEYS.has(key));
}

function resolveRecommendedTopic(params: {
  mode: ReportMode;
  productKey: string;
  deviceCode: string;
  payloadMessageType: string;
  plaintextFrameType?: number;
}): string {
  if (params.mode === 'encrypted') {
    return MQTT_DP_TOPIC;
  }
  if (params.plaintextFrameType && params.plaintextFrameType !== 1) {
    return MQTT_DP_TOPIC;
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

function resolvePayloadFormat(payloadText: string): PayloadFormat {
  if (looksLikeJson(payloadText)) {
    try {
      JSON.parse(payloadText);
      return 'json';
    } catch {
      return 'text';
    }
  }
  if (looksLikeXml(payloadText)) {
    return 'xml';
  }
  return 'text';
}

function buildPayloadPreview(payloadText: string, payloadFormat: PayloadFormat): string {
  if (payloadFormat === 'json') {
    return prettyJson(payloadText);
  }
  if (payloadFormat === 'xml') {
    return prettyXml(payloadText);
  }
  return payloadText;
}

function parseJsonObject(payloadText: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(payloadText);
    return asPlainObject(parsed);
  } catch {
    return null;
  }
}

function parsePayloadAsObject(payloadText: string): Record<string, unknown> {
  if (!payloadText || !payloadText.trim()) {
    throw new Error('Payload 不能为空。');
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(payloadText);
  } catch {
    throw new Error('Payload 不是有效 JSON，无法构造明文帧。');
  }

  const payload = asPlainObject(parsed);
  if (!payload) {
    throw new Error('Payload 顶层必须是 JSON 对象。');
  }

  return payload;
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
