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
