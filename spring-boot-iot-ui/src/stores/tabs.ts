import { computed, reactive } from 'vue';

export interface VisitedTab {
  path: string;
  title: string;
}

const state = reactive({
  tabs: [] as VisitedTab[]
});

export const visitedTabs = computed(() => state.tabs);

export function pushVisitedTab(tab: VisitedTab): void {
  const exists = state.tabs.find((item) => item.path === tab.path);
  if (exists) {
    exists.title = tab.title;
    return;
  }

  state.tabs.push(tab);
}

export function closeVisitedTab(path: string): void {
  const index = state.tabs.findIndex((item) => item.path === path);
  if (index >= 0) {
    state.tabs.splice(index, 1);
  }
}
