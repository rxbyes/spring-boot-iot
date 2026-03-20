export function normalizeRoutePath(path?: string | null): string {
  const rawPath = (path || '').trim();
  if (!rawPath || rawPath === '/') {
    return '/';
  }

  let normalized = rawPath.replace(/\/+$/, '');
  normalized = normalized.replace(/^\/+/, '/');
  if (!normalized.startsWith('/')) {
    normalized = `/${normalized}`;
  }
  return normalized || '/';
}

export function normalizeOptionalRoutePath(path?: string | null): string | null {
  const rawPath = (path || '').trim();
  if (!rawPath) {
    return null;
  }
  return normalizeRoutePath(rawPath);
}
