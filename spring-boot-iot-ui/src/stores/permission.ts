import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import { getCurrentUser } from '../api/auth';
import { canAccessSectionHome, listSectionHomeConfigs } from '../utils/sectionWorkspaces';
import type { LoginResult, MenuTreeNode, UserAuthContext } from '../types/auth';
import { normalizeOptionalRoutePath, normalizeRoutePath } from '../utils/routePath';

const ACCESS_TOKEN_KEY = 'spring-boot-iot.access-token';
const AUTH_CONTEXT_KEY = 'spring-boot-iot.auth-context';

function readStorage(key: string): string {
  if (typeof window === 'undefined') {
    return '';
  }
  return window.localStorage.getItem(key) || '';
}

function writeStorage(key: string, value: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(key, value);
}

function removeStorage(key: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(key);
}

function parseStoredAuthContext(): UserAuthContext | null {
  const raw = readStorage(AUTH_CONTEXT_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as UserAuthContext;
  } catch {
    removeStorage(AUTH_CONTEXT_KEY);
    return null;
  }
}

function collectMenuPaths(menus: MenuTreeNode[]): string[] {
  const pathSet = new Set<string>();

  const visit = (nodes: MenuTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.type !== 2) {
        const normalizedPath = normalizeOptionalRoutePath(node.path);
        if (normalizedPath) {
          pathSet.add(normalizedPath);
        }
      }
      if (node.children?.length) {
        visit(node.children);
      }
    });
  };

  visit(menus);
  return Array.from(pathSet);
}

function collectStaticFallbackPaths(): string[] {
  const pathSet = new Set<string>();
  listSectionHomeConfigs().forEach((config) => {
    pathSet.add(normalizeRoutePath(config.path));
    config.cards.forEach((card) => {
      pathSet.add(normalizeRoutePath(card.path));
    });
  });
  return Array.from(pathSet);
}

export function getStoredAccessToken(): string {
  return readStorage(ACCESS_TOKEN_KEY);
}

export function clearStoredAuth(): void {
  removeStorage(ACCESS_TOKEN_KEY);
  removeStorage(AUTH_CONTEXT_KEY);
}

export const usePermissionStore = defineStore('permission', () => {
  const token = ref<string>(getStoredAccessToken());
  const authContext = ref<UserAuthContext | null>(parseStoredAuthContext());
  const initialized = ref<boolean>(!token.value);
  let initPromise: Promise<UserAuthContext | null> | null = null;

  const isLoggedIn = computed(() => Boolean(token.value));
  const menus = computed(() => authContext.value?.menus || []);
  const permissions = computed(() => authContext.value?.permissions || []);
  const roleCodes = computed(() => authContext.value?.roleCodes || []);
  const roleNames = computed(() => authContext.value?.roles.map((item) => item.roleName) || []);
  const displayName = computed(() => {
    if (!authContext.value) {
      return '';
    }
    return authContext.value.displayName || authContext.value.realName || authContext.value.username;
  });
  const primaryRoleName = computed(() => roleNames.value[0] || '');
  const homePath = computed(() => normalizeRoutePath(authContext.value?.homePath));
  const allowedPaths = computed(() => collectMenuPaths(menus.value));
  const staticFallbackPaths = computed(() => collectStaticFallbackPaths());
  const hasBoundRoles = computed(() => roleCodes.value.length > 0 || roleNames.value.length > 0);
  const userInfo = computed(() => {
    if (!authContext.value) {
      return null;
    }
    return {
      id: authContext.value.userId,
      username: authContext.value.username,
      realName: authContext.value.realName,
      displayName: displayName.value,
      phone: authContext.value.phone,
      email: authContext.value.email,
      accountType: authContext.value.accountType,
      authStatus: authContext.value.authStatus,
      loginMethods: authContext.value.loginMethods || [],
      roleNames: roleNames.value,
      roleCodes: roleCodes.value
    };
  });

  function setAccessToken(accessToken: string): void {
    token.value = accessToken;
    if (accessToken) {
      writeStorage(ACCESS_TOKEN_KEY, accessToken);
      initialized.value = false;
      return;
    }
    removeStorage(ACCESS_TOKEN_KEY);
  }

  function setAuthContext(context: UserAuthContext | null): void {
    authContext.value = context;
    initialized.value = true;

    if (context) {
      writeStorage(AUTH_CONTEXT_KEY, JSON.stringify(context));
      return;
    }

    removeStorage(AUTH_CONTEXT_KEY);
  }

  function login(result: LoginResult): void {
    setAccessToken(result.token);
    setAuthContext(result.authContext);
  }

  function logout(): void {
    token.value = '';
    authContext.value = null;
    initialized.value = true;
    clearStoredAuth();
  }

  async function fetchCurrentUser(): Promise<UserAuthContext | null> {
    if (!token.value) {
      setAuthContext(null);
      return null;
    }

    const response = await getCurrentUser();
    setAuthContext(response.data);
    return response.data;
  }

  async function ensureInitialized(force = false): Promise<UserAuthContext | null> {
    if (!token.value) {
      initialized.value = true;
      return null;
    }

    if (!force && initialized.value && authContext.value) {
      return authContext.value;
    }

    if (!force && initPromise) {
      return initPromise;
    }

    initPromise = fetchCurrentUser()
      .catch((error) => {
        logout();
        throw error;
      })
      .finally(() => {
        initPromise = null;
      });

    return initPromise;
  }

  function hasPermission(permissionCode?: string): boolean {
    if (!permissionCode) {
      return true;
    }
    if (!isLoggedIn.value || !authContext.value) {
      return false;
    }
    return authContext.value.superAdmin || permissions.value.includes(permissionCode);
  }

  function hasRoutePermission(path: string): boolean {
    const normalizedPath = normalizeRoutePath(path);
    if (normalizedPath === '/') {
      return true;
    }
    if (!isLoggedIn.value || !authContext.value) {
      return false;
    }
    if (authContext.value.superAdmin) {
      return true;
    }
    if (allowedPaths.value.includes(normalizedPath) || canAccessSectionHome(normalizedPath, allowedPaths.value)) {
      return true;
    }
    // 共享环境中菜单树偶发为空时，允许访问标准分组内的静态页面，避免“菜单可见但无法跳转”。
    if (allowedPaths.value.length === 0 && hasBoundRoles.value) {
      return staticFallbackPaths.value.includes(normalizedPath);
    }
    return false;
  }

  return {
    token,
    authContext,
    initialized,
    isLoggedIn,
    menus,
    permissions,
    roleCodes,
    roleNames,
    displayName,
    primaryRoleName,
    homePath,
    allowedPaths,
    userInfo,
    setAccessToken,
    setAuthContext,
    login,
    logout,
    fetchCurrentUser,
    ensureInitialized,
    hasPermission,
    hasRoutePermission
  };
});
