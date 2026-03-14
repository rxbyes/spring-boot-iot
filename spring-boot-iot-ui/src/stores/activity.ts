import { computed, reactive } from 'vue';

import type { ActivityEntry } from '../types/api';

const MAX_ACTIVITY = 16;

const state = reactive({
  entries: [] as ActivityEntry[]
});

export const activityEntries = computed(() => state.entries);

export function recordActivity(entry: Omit<ActivityEntry, 'id' | 'createdAt'>): void {
  state.entries.unshift({
    ...entry,
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    createdAt: new Date().toISOString()
  });

  if (state.entries.length > MAX_ACTIVITY) {
    state.entries.length = MAX_ACTIVITY;
  }
}
