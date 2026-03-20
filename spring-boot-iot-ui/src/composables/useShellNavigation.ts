import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { usePermissionStore } from '../stores/permission';
import type { ShellNavigationState } from '../types/shell';
import type { MenuTreeNode } from '../types/auth';
import { normalizeOptionalRoutePath, normalizeRoutePath } from '../utils/routePath';
import {
  createSectionHomeNavItem,
  listStaticNavigationGroups,
  type WorkspaceNavGroup,
  type WorkspaceNavItem
} from '../utils/sectionWorkspaces';

const guestGroup: WorkspaceNavGroup = {
  key: 'guest-overview',
  label: '平台首页',
  description: '游客预览',
  menuTitle: '监测预警平台首页',
  menuHint: '未登录时仅开放首页预览与登录入口。',
  items: [{ to: '/', label: '首页总览', caption: '平台定位、能力边界与登录入口说明', short: '首' }]
};

function cloneGroups(groups: WorkspaceNavGroup[]): WorkspaceNavGroup[] {
  return groups.map((group) => ({
    ...group,
    items: group.items.map((item) => ({ ...item }))
  }));
}

function prependSectionHomeItem(groupKey: string, groupLabel: string, items: WorkspaceNavItem[]): WorkspaceNavItem[] {
  const overviewItem = createSectionHomeNavItem(groupKey, groupLabel);
  if (!overviewItem) {
    return items;
  }
  if (items.some((item) => item.to === overviewItem.to)) {
    return items;
  }
  return [overviewItem, ...items];
}

function buildShortLabel(label: string, fallback?: string): string {
  const short = (fallback || '').trim();
  if (short) {
    return short;
  }
  const text = (label || '').trim();
  return text ? text.slice(0, 1) : '-';
}

function appendNavItem(items: WorkspaceNavItem[], node: MenuTreeNode, pathSet: Set<string>): void {
  if (node.type === 2) {
    return;
  }
  const path = normalizeOptionalRoutePath(node.path);
  if (path && !pathSet.has(path)) {
    pathSet.add(path);
    items.push({
      to: path,
      label: node.menuName || path,
      caption: node.meta?.caption || node.meta?.description || `${node.menuName || path}功能`,
      short: buildShortLabel(node.menuName || path, node.meta?.shortLabel)
    });
  }

  (node.children || []).forEach((child) => appendNavItem(items, child, pathSet));
}

function buildDynamicGroups(menus: MenuTreeNode[]): WorkspaceNavGroup[] {
  return menus
    .filter((root) => root.type !== 2)
    .map((root) => {
      const items: WorkspaceNavItem[] = [];
      const pathSet = new Set<string>();
      appendNavItem(items, root, pathSet);
      return {
        key: root.menuCode || `menu-${root.id}`,
        label: root.menuName || '未命名分组',
        description: root.meta?.description || '权限分组',
        menuTitle: root.meta?.menuTitle || root.menuName || '菜单分组',
        menuHint: root.meta?.menuHint || root.meta?.description || '由后端菜单权限动态驱动。',
        items: prependSectionHomeItem(root.menuCode || `menu-${root.id}`, root.menuName || '', items)
      } as WorkspaceNavGroup;
    })
    .filter((group) => group.items.length > 0);
}

export function useShellNavigation(): ShellNavigationState {
  const route = useRoute();
  const router = useRouter();
  const permissionStore = usePermissionStore();
  const staticNavigationGroups = cloneGroups(listStaticNavigationGroups());

  function resolveGroupLandingPath(group: WorkspaceNavGroup): string {
    const sectionHomeItem = createSectionHomeNavItem(group.key, group.label);
    if (sectionHomeItem && permissionStore.hasRoutePermission(sectionHomeItem.to)) {
      return normalizeRoutePath(sectionHomeItem.to);
    }
    return normalizeRoutePath(group.items[0]?.to || '/');
  }

  const navigationGroups = computed<WorkspaceNavGroup[]>(() => {
    if (!permissionStore.isLoggedIn) {
      return [guestGroup];
    }
    const dynamicGroups = buildDynamicGroups(permissionStore.menus || []);
    if (dynamicGroups.length > 0) {
      return dynamicGroups;
    }

    const filteredFallbackGroups = staticNavigationGroups
      .map((group) => {
        const mergedItems = prependSectionHomeItem(group.key, group.label, group.items);
        const allowedItems = mergedItems.filter((item) => permissionStore.hasRoutePermission(item.to));
        return {
          ...group,
          items: allowedItems
        };
      })
      .filter((group) => group.items.length > 0);

    if (filteredFallbackGroups.length > 0) {
      return filteredFallbackGroups;
    }
    return [guestGroup];
  });

  const flattenedItems = computed(() => navigationGroups.value.flatMap((group) => group.items));
  const currentRoutePath = computed(() => normalizeRoutePath(route.path));
  const activeGroup = computed(() => {
    const matchedGroup = navigationGroups.value.find((group) => group.items.some((item) => item.to === currentRoutePath.value));
    return matchedGroup || navigationGroups.value[0] || guestGroup;
  });
  const activeMenuItem = computed(() => flattenedItems.value.find((item) => item.to === currentRoutePath.value) || null);
  const activeGroupHomePath = computed(() => resolveGroupLandingPath(activeGroup.value));
  const showSidebarContext = computed(() => currentRoutePath.value === activeGroupHomePath.value);
  const activeTitle = computed(() => {
    if (showSidebarContext.value) {
      return String(route.meta.title || activeGroup.value.label || '平台首页');
    }
    return activeMenuItem.value?.label || String(route.meta.title || '平台首页');
  });

  function switchGroup(groupKey: string) {
    const group = navigationGroups.value.find((item) => item.key === groupKey);
    if (!group || group.items.length === 0) {
      return;
    }

    const targetPath = resolveGroupLandingPath(group);
    if (targetPath !== currentRoutePath.value) {
      router.push(targetPath);
    }
  }

  return {
    navigationGroups,
    flattenedItems,
    currentRoutePath,
    activeGroup,
    activeMenuItem,
    activeGroupHomePath,
    showSidebarContext,
    activeTitle,
    switchGroup
  };
}
