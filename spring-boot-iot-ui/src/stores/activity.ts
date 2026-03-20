import { computed, reactive } from 'vue';

import type { ActivityDraft, ActivityEntry } from '../types/api';

const MAX_ACTIVITY = 16;

const state = reactive({
  entries: [] as ActivityEntry[]
});

export const activityEntries = computed(() => state.entries);

function resolveActivityTitle(entry: ActivityDraft): string {
  const explicitTitle = entry.title?.trim();
  if (explicitTitle) {
    return explicitTitle;
  }

  const segments = [entry.module?.trim(), entry.action?.trim()].filter(Boolean);
  if (segments.length > 0) {
    return segments.join(' · ');
  }

  return '最近操作';
}

function resolveActivityTag(entry: ActivityDraft): string | undefined {
  return entry.tag?.trim() || entry.module?.trim() || undefined;
}

function resolveActivityPath(entry: ActivityDraft): string | undefined {
  const explicitPath = entry.path?.trim();
  if (explicitPath) {
    return explicitPath;
  }
  if (typeof window === 'undefined') {
    return undefined;
  }
  return window.location.pathname || undefined;
}

export function normalizeActivityEntry(entry: ActivityDraft): Omit<ActivityEntry, 'id' | 'createdAt'> {
  return {
    title: resolveActivityTitle(entry),
    detail: entry.detail,
    tag: resolveActivityTag(entry),
    module: entry.module?.trim() || undefined,
    action: entry.action?.trim() || undefined,
    request: entry.request,
    response: entry.response,
    ok: entry.ok ?? true,
    path: resolveActivityPath(entry)
  };
}

export function recordActivity(entry: ActivityDraft): void {
  state.entries.unshift({
    ...normalizeActivityEntry(entry),
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    createdAt: new Date().toISOString()
  });

  if (state.entries.length > MAX_ACTIVITY) {
    state.entries.length = MAX_ACTIVITY;
  }
}
