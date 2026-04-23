import type { IdType } from './api';

export interface RoleSummary {
  id: IdType;
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
  id: IdType;
  parentId?: IdType | null;
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
  userId: IdType;
  tenantId?: IdType;
  tenantName?: string;
  orgId?: IdType;
  orgName?: string;
  username: string;
  nickname?: string;
  realName?: string;
  displayName?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  accountType?: string;
  authStatus?: string;
  loginMethods?: string[];
  lastLoginTime?: string;
  lastLoginIp?: string;
  dataScopeType?: string;
  dataScopeSummary?: string;
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
  userId?: IdType;
  username?: string;
  realName?: string;
  authContext: UserAuthContext;
}
