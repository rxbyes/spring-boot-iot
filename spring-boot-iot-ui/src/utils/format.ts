export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date);
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
