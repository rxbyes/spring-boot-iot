export type PlaintextDataFormatType = 1 | 2 | 3;

export interface DataFormatInference {
  type: PlaintextDataFormatType;
  label: 'C.1' | 'C.2' | 'C.3';
  reason: string;
}

export interface PlaintextFrameBuildOptions {
  type3BinaryBase64?: string;
}

export interface PlaintextFrameBuildResult extends DataFormatInference {
  normalizedJson: string;
  jsonLength: number;
  lengthHighByte: number;
  lengthLowByte: number;
  fileStreamLength: number;
  frameBytes: Uint8Array;
  framedPayload: string;
}

const textEncoder = new TextEncoder();
const TYPE_3_DESCRIPTOR_KEYS = ['did', 'ds_id', 'at', 'desc'] as const;

export function inferPlaintextDataFormatType(payload: unknown): DataFormatInference {
  if (isType3Descriptor(payload)) {
    return {
      type: 3,
      label: 'C.3',
      reason: '识别到 did/ds_id/at/desc 文件描述字段，按类型 3（C.3）处理。'
    };
  }

  if (containsTimestampSeries(payload)) {
    return {
      type: 2,
      label: 'C.2',
      reason: '识别到“时间戳 -> 值/对象”结构，按类型 2（C.2）处理。'
    };
  }

  return {
    type: 1,
    label: 'C.1',
    reason: '未命中 C.2/C.3 特征，按普通属性 JSON（C.1）处理。'
  };
}

export function buildPlaintextFrame(payloadText: string, options: PlaintextFrameBuildOptions = {}): PlaintextFrameBuildResult {
  const parsedPayload = parsePayloadAsObject(payloadText);
  const dataFormat = inferPlaintextDataFormatType(parsedPayload);
  const normalizedJson = JSON.stringify(parsedPayload);
  const jsonBytes = textEncoder.encode(normalizedJson);
  const jsonLength = jsonBytes.length;
  const lengthHighByte = (jsonLength >> 8) & 0xff;
  const lengthLowByte = jsonLength & 0xff;

  let frameBytes: Uint8Array;
  let fileStreamLength = 0;

  if (dataFormat.type === 3) {
    const fileStreamBytes = decodeBase64Bytes(options.type3BinaryBase64 ?? '');
    fileStreamLength = fileStreamBytes.length;
    frameBytes = new Uint8Array(3 + jsonLength + 2 + fileStreamLength);
    frameBytes[0] = dataFormat.type;
    frameBytes[1] = lengthHighByte;
    frameBytes[2] = lengthLowByte;
    frameBytes.set(jsonBytes, 3);
    const fileLengthIndex = 3 + jsonLength;
    frameBytes[fileLengthIndex] = (fileStreamLength >> 8) & 0xff;
    frameBytes[fileLengthIndex + 1] = fileStreamLength & 0xff;
    frameBytes.set(fileStreamBytes, fileLengthIndex + 2);
  } else {
    frameBytes = new Uint8Array(3 + jsonLength);
    frameBytes[0] = dataFormat.type;
    frameBytes[1] = lengthHighByte;
    frameBytes[2] = lengthLowByte;
    frameBytes.set(jsonBytes, 3);
  }

  return {
    ...dataFormat,
    normalizedJson,
    jsonLength,
    lengthHighByte,
    lengthLowByte,
    fileStreamLength,
    frameBytes,
    framedPayload: bytesToLatin1String(frameBytes)
  };
}

export function formatFrameDecimalPreview(frameBytes: Uint8Array, limit = 160): string {
  if (!frameBytes.length) {
    return '[]';
  }
  const values = Array.from(frameBytes.slice(0, limit));
  if (frameBytes.length <= limit) {
    return `[${values.join(', ')}]`;
  }
  return `[${values.join(', ')}, +${frameBytes.length - limit} more]`;
}

export function formatFrameHexPreview(frameBytes: Uint8Array, limit = 96): string {
  if (!frameBytes.length) {
    return '--';
  }
  const values = Array.from(frameBytes.slice(0, limit))
    .map((value) => value.toString(16).toUpperCase().padStart(2, '0'))
    .join(' ');
  if (frameBytes.length <= limit) {
    return values;
  }
  return `${values} ... (+${frameBytes.length - limit} bytes)`;
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

  if (!isPlainObject(parsed)) {
    throw new Error('Payload 顶层必须是 JSON 对象。');
  }

  return parsed;
}

function isType3Descriptor(payload: unknown): payload is Record<string, unknown> {
  if (!isPlainObject(payload)) {
    return false;
  }
  return TYPE_3_DESCRIPTOR_KEYS.every((key) => Object.prototype.hasOwnProperty.call(payload, key));
}

function containsTimestampSeries(payload: unknown, depth = 0): boolean {
  if (!isPlainObject(payload) || depth > 8) {
    return false;
  }

  const entries = Object.entries(payload);
  if (!entries.length) {
    return false;
  }

  const hasTimestampKey = entries.some(([key, value]) => isTimestampKey(key) && isTimestampValue(value));
  if (hasTimestampKey) {
    return true;
  }

  return entries.some(([, value]) => containsTimestampSeries(value, depth + 1));
}

function isTimestampKey(value: string): boolean {
  const key = value.trim();
  if (!key) {
    return false;
  }

  if (/^\d{10,13}$/.test(key)) {
    return true;
  }

  if (!key.includes('T')) {
    return false;
  }

  return !Number.isNaN(Date.parse(key));
}

function isTimestampValue(value: unknown): boolean {
  return (
    isPlainObject(value) ||
    typeof value === 'number' ||
    typeof value === 'string' ||
    typeof value === 'boolean'
  );
}

function decodeBase64Bytes(base64Text: string): Uint8Array {
  const normalized = base64Text.replace(/\s+/g, '');
  if (!normalized) {
    return new Uint8Array();
  }

  try {
    if (typeof globalThis.atob === 'function') {
      const binaryText = globalThis.atob(normalized);
      const bytes = new Uint8Array(binaryText.length);
      for (let index = 0; index < binaryText.length; index += 1) {
        bytes[index] = binaryText.charCodeAt(index);
      }
      return bytes;
    }

    const maybeBuffer = (globalThis as unknown as { Buffer?: { from(input: string, encoding: string): Uint8Array } }).Buffer;
    if (maybeBuffer?.from) {
      return Uint8Array.from(maybeBuffer.from(normalized, 'base64'));
    }
  } catch {
    throw new Error('类型 3 文件流 Base64 无效，请检查输入。');
  }

  throw new Error('当前环境不支持 Base64 解码。');
}

function bytesToLatin1String(bytes: Uint8Array): string {
  if (!bytes.length) {
    return '';
  }

  const chunkSize = 8192;
  let result = '';
  for (let start = 0; start < bytes.length; start += chunkSize) {
    const chunk = bytes.subarray(start, Math.min(start + chunkSize, bytes.length));
    result += String.fromCharCode(...chunk);
  }
  return result;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Object.prototype.toString.call(value) === '[object Object]';
}
