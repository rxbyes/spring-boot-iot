import { reactive } from 'vue';

const STORAGE_KEY = 'spring-boot-iot-ui-runtime';

interface RuntimeState {
  apiBaseUrl: string;
}

function normalizeBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, '');
}

function readStorage(): RuntimeState {
  if (typeof window === 'undefined') {
    return { apiBaseUrl: '' };
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return {
      apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL || '')
    };
  }

  try {
    const parsed = JSON.parse(raw) as RuntimeState;
    return {
      apiBaseUrl: normalizeBaseUrl(parsed.apiBaseUrl || '')
    };
  } catch {
    return { apiBaseUrl: '' };
  }
}

export const runtimeState = reactive<RuntimeState>(readStorage());

export function setApiBaseUrl(value: string): void {
  runtimeState.apiBaseUrl = normalizeBaseUrl(value);
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(runtimeState));
  }
}

export function isProxyMode(): boolean {
  return runtimeState.apiBaseUrl.length === 0;
}
