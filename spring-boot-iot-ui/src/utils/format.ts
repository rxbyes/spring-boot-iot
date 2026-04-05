const DISPLAY_TIME_ZONE = 'Asia/Shanghai';
const EXPLICIT_ZONE_PATTERN = /(z|[+-]\d{2}:?\d{2})$/i;
const NAIVE_DATE_TIME_PATTERN =
  /^(\d{4})[-/](\d{2})[-/](\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$/;
const MESSAGE_TRACE_SHIFT_MS = 8 * 60 * 60 * 1000;
const MESSAGE_TRACE_SHIFT_TOLERANCE_MS = 30 * 60 * 1000;

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--';
  }

  const normalized = String(value).trim();
  if (!normalized) {
    return '--';
  }

  const naiveMatch = normalized.match(NAIVE_DATE_TIME_PATTERN);
  if (naiveMatch && !EXPLICIT_ZONE_PATTERN.test(normalized)) {
    const [, year, month, day, hour, minute, second = '00'] = naiveMatch;
    return `${year}/${month}/${day} ${hour}:${minute}:${second}`;
  }

  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: DISPLAY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date);
}

export function formatMessageTraceReportTime(reportTime?: string | null, createTime?: string | null): string {
  const reportDate = parseDateTimeValue(reportTime);
  const createDate = parseDateTimeValue(createTime);

  if (reportDate && createDate) {
    const diffMs = reportDate.getTime() - createDate.getTime();
    if (Math.abs(diffMs - MESSAGE_TRACE_SHIFT_MS) <= MESSAGE_TRACE_SHIFT_TOLERANCE_MS) {
      return formatLocalDateTime(new Date(reportDate.getTime() - MESSAGE_TRACE_SHIFT_MS));
    }
  }

  return formatDateTime(reportTime || createTime);
}

export function prettyJson(value: unknown): string {
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }

  const json = JSON.stringify(value, null, 2);
  return json ?? '';
}

export function prettyXml(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return '';
  }
  if (!looksLikeXml(trimmed) || !isValidXml(trimmed)) {
    return value;
  }

  const normalized = trimmed.replace(/>\s*</g, '>\n<').split('\n');
  let depth = 0;
  const lines = normalized.map((line) => {
    const current = line.trim();
    if (!current) {
      return '';
    }

    if (isClosingXmlTag(current)) {
      depth = Math.max(depth - 1, 0);
    }

    const formatted = `${'  '.repeat(depth)}${current}`;
    if (isOpeningXmlTag(current)) {
      depth += 1;
    }
    return formatted;
  }).filter(Boolean);

  return lines.join('\n');
}

export function looksLikeJson(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.startsWith('{') || trimmed.startsWith('[');
}

export function looksLikeXml(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.startsWith('<') && trimmed.endsWith('>');
}

export function parseJsonSafely<T>(value: string): T | null {
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export function statusLabel(onlineStatus?: number | null): string {
  return onlineStatus === 1 ? '在线' : '离线';
}

export function statusTone(onlineStatus?: number | null): 'success' | 'muted' {
  return onlineStatus === 1 ? 'success' : 'muted';
}

export function truncateText(value: string, maxLength = 64): string {
  if (value.length <= maxLength) {
    return value;
  }

  return `${value.slice(0, maxLength)}...`;
}

function parseDateTimeValue(value?: string | null): Date | null {
  if (!value) {
    return null;
  }

  const normalized = String(value).trim();
  if (!normalized) {
    return null;
  }

  const naiveMatch = normalized.match(NAIVE_DATE_TIME_PATTERN);
  if (naiveMatch && !EXPLICIT_ZONE_PATTERN.test(normalized)) {
    const [, year, month, day, hour, minute, second = '00'] = naiveMatch;
    return new Date(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second)
    );
  }

  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatLocalDateTime(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  const second = String(date.getSeconds()).padStart(2, '0');

  return `${year}/${month}/${day} ${hour}:${minute}:${second}`;
}

function isValidXml(value: string): boolean {
  if (typeof DOMParser === 'undefined') {
    return true;
  }

  const parser = new DOMParser();
  const document = parser.parseFromString(value, 'application/xml');
  return !document.querySelector('parsererror');
}

function isClosingXmlTag(value: string): boolean {
  return /^<\//.test(value);
}

function isOpeningXmlTag(value: string): boolean {
  return (
    /^<[^!?/][^>]*>$/.test(value) &&
    !/\/>$/.test(value) &&
    !/<[^>]+>.*<\/[^>]+>$/.test(value)
  );
}
