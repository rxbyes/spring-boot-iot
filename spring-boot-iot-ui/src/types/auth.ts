export interface RoleSummary {
  id: number;
  roleCode: string;
  roleName: string;
}

export interface MenuMeta {
  description?: string;
  menuTitle?: string;
  menuHint?: string;
  caption?: string;
  shortLabel?: string;
}

export interface MenuTreeNode {
  id: number;
  parentId?: number | null;
  menuName: string;
  menuCode?: string;
  path?: string;
  component?: string;
  icon?: string;
  sort?: number;
  type?: number;
  meta?: MenuMeta;
  children: MenuTreeNode[];
}

export interface UserAuthContext {
  userId: number;
  username: string;
  realName?: string;
  displayName?: string;
  superAdmin: boolean;
  homePath?: string;
  roleCodes: string[];
  permissions: string[];
  roles: RoleSummary[];
  menus: MenuTreeNode[];
}

export interface LoginResult {
  token: string;
  tokenType?: string;
  expiresIn?: number;
  tokenHeader?: string;
  userId?: number;
  username?: string;
  realName?: string;
  authContext: UserAuthContext;
}
