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

export function routePathMatchesPattern(patternPath?: string | null, actualPath?: string | null): boolean {
  const normalizedPattern = normalizeOptionalRoutePath(patternPath);
  const normalizedActual = normalizeOptionalRoutePath(actualPath);
  if (!normalizedPattern || !normalizedActual) {
    return false;
  }
  if (normalizedPattern === normalizedActual) {
    return true;
  }

  const patternSegments = normalizedPattern.split('/').filter(Boolean);
  const actualSegments = normalizedActual.split('/').filter(Boolean);
  if (patternSegments.length !== actualSegments.length) {
    return false;
  }

  return patternSegments.every((segment, index) => segment.startsWith(':') || segment === actualSegments[index]);
}
