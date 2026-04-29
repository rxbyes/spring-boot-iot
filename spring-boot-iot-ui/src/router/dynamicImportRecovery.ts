export const DYNAMIC_IMPORT_RELOAD_FLAG = 'spring-boot-iot.dynamic-import-reload-path';

const DYNAMIC_IMPORT_ERROR_PATTERNS = [
  /Failed to fetch dynamically imported module/i,
  /Importing a module script failed/i,
  /Loading chunk .+ failed/i,
  /Unable to preload CSS/i
];

export interface DynamicImportRecoveryLocation {
  pathname: string;
  search: string;
  hash: string;
  replace(url: string): void;
  reload(): void;
}

export interface DynamicImportRecoveryStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export function isDynamicImportLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error || '');
  return DYNAMIC_IMPORT_ERROR_PATTERNS.some((pattern) => pattern.test(message));
}

export function handleDynamicImportRouteError(
  error: unknown,
  location: DynamicImportRecoveryLocation = window.location,
  storage: DynamicImportRecoveryStorage = window.sessionStorage
): boolean {
  if (!isDynamicImportLoadError(error)) {
    return false;
  }

  const currentPath = `${location.pathname}${location.search}${location.hash}`;
  if (storage.getItem(DYNAMIC_IMPORT_RELOAD_FLAG) === currentPath) {
    storage.removeItem(DYNAMIC_IMPORT_RELOAD_FLAG);
    location.reload();
    return false;
  }

  storage.setItem(DYNAMIC_IMPORT_RELOAD_FLAG, currentPath);
  location.replace(currentPath);
  return true;
}
