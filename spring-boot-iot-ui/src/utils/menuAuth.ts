import type { MenuTreeNode } from '../types/auth';

export interface MenuRelationState {
  ancestorIds: number[];
  childIds: number[];
  descendantIds: number[];
}

export interface MenuSelectionState {
  checked: boolean;
  indeterminate: boolean;
  selfSelected: boolean;
  selectedChildCount: number;
  totalChildCount: number;
}

export interface RoleAuthDetailItem {
  id: number;
  parentId?: number | null;
  menuName: string;
  menuCode?: string;
  path?: string;
  type: number;
  description?: string;
  checked: boolean;
  indeterminate: boolean;
  selfSelected: boolean;
  childCount: number;
}

function visitMenus(
  nodes: MenuTreeNode[],
  visitor: (node: MenuTreeNode, ancestorIds: number[]) => void,
  ancestorIds: number[] = []
): void {
  nodes.forEach((node) => {
    visitor(node, ancestorIds);
    if (node.children?.length) {
      visitMenus(node.children, visitor, [...ancestorIds, node.id]);
    }
  });
}

function matchesMenuKeyword(node: MenuTreeNode, keyword: string): boolean {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }

  return [node.menuName, node.menuCode, node.path]
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(normalizedKeyword));
}

function orderMenuIdsByTree(nodes: MenuTreeNode[], menuIds: Iterable<number>): number[] {
  const menuIdSet = new Set(menuIds);
  const orderedIds: number[] = [];
  visitMenus(nodes, (node) => {
    if (menuIdSet.has(node.id)) {
      orderedIds.push(node.id);
    }
  });
  return orderedIds;
}

function resolveNodeDescription(node: MenuTreeNode): string {
  return (
    node.meta?.menuHint ||
    node.meta?.description ||
    node.meta?.caption ||
    node.meta?.shortLabel ||
    ''
  );
}

export function buildMenuNodeMap(nodes: MenuTreeNode[]): Map<number, MenuTreeNode> {
  const nodeMap = new Map<number, MenuTreeNode>();
  visitMenus(nodes, (node) => {
    nodeMap.set(node.id, node);
  });
  return nodeMap;
}

export function buildMenuRelationMap(nodes: MenuTreeNode[]): Map<number, MenuRelationState> {
  const relationMap = new Map<number, MenuRelationState>();

  const walk = (node: MenuTreeNode, ancestorIds: number[]): number[] => {
    const childIds = (node.children || []).map((child) => child.id);
    const descendantIds: number[] = [];

    (node.children || []).forEach((child) => {
      descendantIds.push(child.id);
      descendantIds.push(...walk(child, [...ancestorIds, node.id]));
    });

    relationMap.set(node.id, {
      ancestorIds: [...ancestorIds],
      childIds,
      descendantIds
    });

    return descendantIds;
  };

  nodes.forEach((node) => {
    walk(node, []);
  });

  return relationMap;
}

export function resolveGrantedMenuIds(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): number[] {
  if (!grantedIds.length) {
    return [];
  }

  const nodeMap = buildMenuNodeMap(nodes);
  const relationMap = buildMenuRelationMap(nodes);
  const grantedIdSet = new Set<number>();

  grantedIds.forEach((menuId) => {
    if (typeof menuId !== 'number' || !nodeMap.has(menuId)) {
      return;
    }
    grantedIdSet.add(menuId);
    (relationMap.get(menuId)?.ancestorIds || []).forEach((ancestorId) => {
      grantedIdSet.add(ancestorId);
    });
  });

  return orderMenuIdsByTree(nodes, grantedIdSet);
}

export function toggleMenuGrant(
  nodes: MenuTreeNode[],
  grantedIds: number[],
  targetId: number,
  checked: boolean
): number[] {
  const nodeMap = buildMenuNodeMap(nodes);
  if (!nodeMap.has(targetId)) {
    return resolveGrantedMenuIds(nodes, grantedIds);
  }

  const relationMap = buildMenuRelationMap(nodes);
  const relation = relationMap.get(targetId);
  if (!relation) {
    return resolveGrantedMenuIds(nodes, grantedIds);
  }

  const nextGrantedIdSet = new Set(resolveGrantedMenuIds(nodes, grantedIds));

  if (checked) {
    [targetId, ...relation.ancestorIds, ...relation.descendantIds].forEach((menuId) => {
      nextGrantedIdSet.add(menuId);
    });
  } else {
    [targetId, ...relation.descendantIds].forEach((menuId) => {
      nextGrantedIdSet.delete(menuId);
    });
  }

  return orderMenuIdsByTree(nodes, nextGrantedIdSet);
}

export function buildMenuSelectionStateMap(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): Map<number, MenuSelectionState> {
  const grantedIdSet = new Set(resolveGrantedMenuIds(nodes, grantedIds));
  const relationMap = buildMenuRelationMap(nodes);
  const stateMap = new Map<number, MenuSelectionState>();

  relationMap.forEach((relation, nodeId) => {
    const subtreeIds = [nodeId, ...relation.descendantIds];
    const selectedCount = subtreeIds.reduce((count, currentId) => {
      return grantedIdSet.has(currentId) ? count + 1 : count;
    }, 0);
    const selectedChildCount = relation.childIds.reduce((count, childId) => {
      const childRelation = relationMap.get(childId);
      const childSubtreeIds = [childId, ...(childRelation?.descendantIds || [])];
      return childSubtreeIds.some((currentId) => grantedIdSet.has(currentId)) ? count + 1 : count;
    }, 0);

    stateMap.set(nodeId, {
      checked: subtreeIds.length > 0 && selectedCount === subtreeIds.length,
      indeterminate: selectedCount > 0 && selectedCount < subtreeIds.length,
      selfSelected: grantedIdSet.has(nodeId),
      selectedChildCount,
      totalChildCount: relation.childIds.length
    });
  });

  return stateMap;
}

export function filterPermissionTreeByKeyword(
  nodes: MenuTreeNode[],
  keyword: string
): MenuTreeNode[] {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return nodes;
  }

  return nodes.reduce<MenuTreeNode[]>((result, node) => {
    const filteredChildren = filterPermissionTreeByKeyword(node.children || [], keyword);
    if (matchesMenuKeyword(node, normalizedKeyword) || filteredChildren.length > 0) {
      result.push({
        ...node,
        children: filteredChildren
      });
    }
    return result;
  }, []);
}

export function resolveNodeAncestorIds(nodes: MenuTreeNode[], targetId: number | null): number[] {
  if (targetId === null) {
    return [];
  }
  return buildMenuRelationMap(nodes).get(targetId)?.ancestorIds || [];
}

export function resolveNodeDetailItems(
  nodes: MenuTreeNode[],
  targetId: number | null,
  keyword = '',
  selectionStateMap?: Map<number, MenuSelectionState>
): RoleAuthDetailItem[] {
  if (targetId === null) {
    return [];
  }

  const node = buildMenuNodeMap(nodes).get(targetId);
  if (!node) {
    return [];
  }

  const normalizedKeyword = keyword.trim().toLowerCase();

  return (node.children || [])
    .filter((child) => {
      if (!normalizedKeyword) {
        return true;
      }
      return [child.menuName, child.menuCode, child.path, resolveNodeDescription(child)]
        .filter((item): item is string => Boolean(item))
        .some((item) => item.toLowerCase().includes(normalizedKeyword));
    })
    .map((child) => {
      const selectionState = selectionStateMap?.get(child.id);
      return {
        id: child.id,
        parentId: child.parentId,
        menuName: child.menuName,
        menuCode: child.menuCode,
        path: child.path,
        type: child.type ?? 0,
        description: resolveNodeDescription(child),
        checked: selectionState?.checked ?? false,
        indeterminate: selectionState?.indeterminate ?? false,
        selfSelected: selectionState?.selfSelected ?? false,
        childCount: child.children?.length || 0
      };
    });
}

export function buildRolePageTree(nodes: MenuTreeNode[]): MenuTreeNode[] {
  return nodes
    .filter((node) => node.type !== 2)
    .map((node) => ({
      ...node,
      children: buildRolePageTree(node.children || [])
    }));
}

export function resolveRoleCheckedMenuIds(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): number[] {
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

export function resolveRoleMenuSummary(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>,
  limit = 6
): string[] {
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
