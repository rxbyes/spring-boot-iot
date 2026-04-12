import type { MenuTreeNode } from '../types/auth';

function visitMenus(nodes: MenuTreeNode[], visitor: (node: MenuTreeNode) => void): void {
  nodes.forEach((node) => {
    visitor(node);
    if (node.children?.length) {
      visitMenus(node.children, visitor);
    }
  });
}

export function buildMenuNodeMap(nodes: MenuTreeNode[]): Map<number, MenuTreeNode> {
  const nodeMap = new Map<number, MenuTreeNode>();
  visitMenus(nodes, (node) => {
    nodeMap.set(node.id, node);
  });
  return nodeMap;
}

export function buildRolePageTree(nodes: MenuTreeNode[]): MenuTreeNode[] {
  return nodes
    .filter((node) => node.type !== 2)
    .map((node) => ({
      ...node,
      children: buildRolePageTree(node.children || [])
    }));
}

export function resolveRoleCheckedMenuIds(nodes: MenuTreeNode[], grantedIds: Array<number | undefined | null>): number[] {
  if (!grantedIds.length) {
    return [];
  }

  const nodeMap = buildMenuNodeMap(nodes);
  const checkedIds: number[] = [];
  const seen = new Set<number>();

  grantedIds.forEach((menuId) => {
    if (typeof menuId !== 'number' || seen.has(menuId)) {
      return;
    }
    const node = nodeMap.get(menuId);
    if (!node || node.type === 0) {
      return;
    }
    seen.add(menuId);
    checkedIds.push(menuId);
  });

  return checkedIds;
}

export function resolveRoleSelectedPageIds(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): number[] {
  if (!grantedIds.length) {
    return [];
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is number => typeof menuId === 'number')
  );

  const selectedPageIds: number[] = [];
  visitMenus(nodes, (node) => {
    if (node.type === 1 && grantedSet.has(node.id)) {
      selectedPageIds.push(node.id);
    }
  });
  return selectedPageIds;
}

export function resolveRoleSelectedButtonIdsByPage(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): Record<number, number[]> {
  if (!grantedIds.length) {
    return {};
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is number => typeof menuId === 'number')
  );
  const selectedButtonIdsByPage: Record<number, number[]> = {};

  visitMenus(nodes, (node) => {
    if (node.type !== 2 || typeof node.parentId !== 'number' || !grantedSet.has(node.id)) {
      return;
    }

    if (!selectedButtonIdsByPage[node.parentId]) {
      selectedButtonIdsByPage[node.parentId] = [];
    }
    selectedButtonIdsByPage[node.parentId].push(node.id);
  });

  return selectedButtonIdsByPage;
}

export function composeRoleGrantedMenuIds(
  nodes: MenuTreeNode[],
  selectedPageIds: number[],
  selectedButtonIdsByPage: Record<number, number[]>
): number[] {
  if (!selectedPageIds.length) {
    return [];
  }

  const selectedPageIdSet = new Set(selectedPageIds);
  const nodeMap = buildMenuNodeMap(nodes);
  const composedIds = new Set<number>(selectedPageIds);

  Object.entries(selectedButtonIdsByPage).forEach(([pageIdText, buttonIds]) => {
    const pageId = Number(pageIdText);
    if (!selectedPageIdSet.has(pageId)) {
      return;
    }

    buttonIds.forEach((buttonId) => {
      const node = nodeMap.get(buttonId);
      if (node?.type === 2 && node.parentId === pageId) {
        composedIds.add(buttonId);
      }
    });
  });

  return Array.from(composedIds);
}

export function resolveRoleMenuSummary(nodes: MenuTreeNode[], grantedIds: Array<number | undefined | null>, limit = 6): string[] {
  if (!grantedIds.length || limit <= 0) {
    return [];
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is number => typeof menuId === 'number')
  );

  const labels: string[] = [];
  const seen = new Set<string>();
  visitMenus(nodes, (node) => {
    if (!grantedSet.has(node.id) || node.type === 0) {
      return;
    }
    const label = (node.menuName || '').trim();
    if (!label || seen.has(label) || labels.length >= limit) {
      return;
    }
    seen.add(label);
    labels.push(label);
  });

  return labels;
}
