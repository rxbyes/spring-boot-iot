import type { IdType } from '../types/api';
import type { MenuTreeNode } from '../types/auth';

export interface MenuRelationState {
  ancestorIds: IdType[];
  childIds: IdType[];
  descendantIds: IdType[];
}

export interface MenuSelectionState {
  checked: boolean;
  indeterminate: boolean;
  selfSelected: boolean;
  selectedChildCount: number;
  totalChildCount: number;
}

export interface RoleAuthDetailItem {
  id: IdType;
  parentId?: IdType | null;
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

function isMenuId(value: unknown): value is IdType {
  return (
    (typeof value === 'string' && value.trim().length > 0) ||
    (typeof value === 'number' && Number.isFinite(value))
  );
}

function visitMenus(
  nodes: MenuTreeNode[],
  visitor: (node: MenuTreeNode, ancestorIds: IdType[]) => void,
  ancestorIds: IdType[] = []
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

function orderMenuIdsByTree(nodes: MenuTreeNode[], menuIds: Iterable<IdType>): IdType[] {
  const menuIdSet = new Set(menuIds);
  const orderedIds: IdType[] = [];
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

export function buildMenuNodeMap(nodes: MenuTreeNode[]): Map<IdType, MenuTreeNode> {
  const nodeMap = new Map<IdType, MenuTreeNode>();
  visitMenus(nodes, (node) => {
    nodeMap.set(node.id, node);
  });
  return nodeMap;
}

export function buildMenuRelationMap(nodes: MenuTreeNode[]): Map<IdType, MenuRelationState> {
  const relationMap = new Map<IdType, MenuRelationState>();

  const walk = (node: MenuTreeNode, ancestorIds: IdType[]): IdType[] => {
    const childIds = (node.children || []).map((child) => child.id);
    const descendantIds: IdType[] = [];

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
  grantedIds: Array<IdType | undefined | null>
): IdType[] {
  if (!grantedIds.length) {
    return [];
  }

  const nodeMap = buildMenuNodeMap(nodes);
  const relationMap = buildMenuRelationMap(nodes);
  const grantedIdSet = new Set<IdType>();

  grantedIds.forEach((menuId) => {
    if (!isMenuId(menuId) || !nodeMap.has(menuId)) {
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
  grantedIds: IdType[],
  targetId: IdType,
  checked: boolean
): IdType[] {
  const nodeMap = buildMenuNodeMap(nodes);
  if (!nodeMap.has(targetId)) {
    return resolveGrantedMenuIds(nodes, grantedIds);
  }

  const relationMap = buildMenuRelationMap(nodes);
  const relation = relationMap.get(targetId);
  if (!relation) {
    return resolveGrantedMenuIds(nodes, grantedIds);
  }

  const nextGrantedIdSet = new Set<IdType>(resolveGrantedMenuIds(nodes, grantedIds));

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
  grantedIds: Array<IdType | undefined | null>
): Map<IdType, MenuSelectionState> {
  const grantedIdSet = new Set(resolveGrantedMenuIds(nodes, grantedIds));
  const relationMap = buildMenuRelationMap(nodes);
  const stateMap = new Map<IdType, MenuSelectionState>();

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

export function resolveNodeAncestorIds(nodes: MenuTreeNode[], targetId: IdType | null): IdType[] {
  if (targetId === null) {
    return [];
  }
  return buildMenuRelationMap(nodes).get(targetId)?.ancestorIds || [];
}

export function resolveNodeDetailItems(
  nodes: MenuTreeNode[],
  targetId: IdType | null,
  keyword = '',
  selectionStateMap?: Map<IdType, MenuSelectionState>
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
  grantedIds: Array<IdType | undefined | null>
): IdType[] {
  if (!grantedIds.length) {
    return [];
  }

  const nodeMap = buildMenuNodeMap(nodes);
  const checkedIds: IdType[] = [];
  const seen = new Set<IdType>();

  grantedIds.forEach((menuId) => {
    if (!isMenuId(menuId) || seen.has(menuId)) {
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
  grantedIds: Array<IdType | undefined | null>
): IdType[] {
  if (!grantedIds.length) {
    return [];
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is IdType => isMenuId(menuId))
  );

  const selectedPageIds: IdType[] = [];
  visitMenus(nodes, (node) => {
    if (node.type === 1 && grantedSet.has(node.id)) {
      selectedPageIds.push(node.id);
    }
  });
  return selectedPageIds;
}

export function resolveRoleSelectedButtonIdsByPage(
  nodes: MenuTreeNode[],
  grantedIds: Array<IdType | undefined | null>
): Record<string, IdType[]> {
  if (!grantedIds.length) {
    return {};
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is IdType => isMenuId(menuId))
  );
  const selectedButtonIdsByPage: Record<string, IdType[]> = {};

  visitMenus(nodes, (node) => {
    if (node.type !== 2 || !isMenuId(node.parentId) || !grantedSet.has(node.id)) {
      return;
    }

    const parentKey = String(node.parentId);
    if (!selectedButtonIdsByPage[parentKey]) {
      selectedButtonIdsByPage[parentKey] = [];
    }
    selectedButtonIdsByPage[parentKey].push(node.id);
  });

  return selectedButtonIdsByPage;
}

export function composeRoleGrantedMenuIds(
  nodes: MenuTreeNode[],
  selectedPageIds: IdType[],
  selectedButtonIdsByPage: Record<string, IdType[]>
): IdType[] {
  if (!selectedPageIds.length) {
    return [];
  }

  const selectedPageIdSet = new Set(selectedPageIds.map((menuId) => String(menuId)));
  const nodeMap = buildMenuNodeMap(nodes);
  const composedIds = new Set<IdType>(selectedPageIds);

  Object.entries(selectedButtonIdsByPage).forEach(([pageIdText, buttonIds]) => {
    if (!selectedPageIdSet.has(pageIdText)) {
      return;
    }

    buttonIds.forEach((buttonId) => {
      const node = nodeMap.get(buttonId);
      if (node?.type === 2 && String(node.parentId) === pageIdText) {
        composedIds.add(buttonId);
      }
    });
  });

  return Array.from(composedIds);
}

export function resolveRoleMenuSummary(
  nodes: MenuTreeNode[],
  grantedIds: Array<IdType | undefined | null>,
  limit = 6
): string[] {
  if (!grantedIds.length || limit <= 0) {
    return [];
  }

  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is IdType => isMenuId(menuId))
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
