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
