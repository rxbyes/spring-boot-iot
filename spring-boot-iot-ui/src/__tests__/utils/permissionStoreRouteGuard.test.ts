import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

const { getCurrentUserMock } = vi.hoisted(() => ({
  getCurrentUserMock: vi.fn()
}));

vi.mock('@/api/auth', () => ({
  getCurrentUser: getCurrentUserMock
}));

import type { RequestError } from '@/api/request';
import { usePermissionStore } from '@/stores/permission';
import type { UserAuthContext } from '@/types/auth';

const ACCESS_TOKEN_KEY = 'spring-boot-iot.access-token';
const AUTH_CONTEXT_KEY = 'spring-boot-iot.auth-context';

function createAuthContext(overrides?: Partial<UserAuthContext>): UserAuthContext {
  return {
    userId: 1,
    username: 'tester',
    realName: 'Tester',
    displayName: 'Tester',
    phone: '',
    email: '',
    accountType: 'sub-account',
    authStatus: 'verified',
    loginMethods: ['account'],
    superAdmin: false,
    homePath: '/',
    roleCodes: [],
    permissions: [],
    roles: [],
    menus: [],
    ...overrides
  };
}

describe('permission store route guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    window.localStorage.clear();
    getCurrentUserMock.mockReset();
  });

  it('normalizes malformed menu path and grants route permission', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        menus: [
          {
            id: 1001,
            menuName: 'User',
            menuCode: 'system:user',
            path: 'user',
            type: 1,
            children: []
          }
        ]
      })
    );

    expect(permissionStore.allowedPaths).toContain('/user');
    expect(permissionStore.hasRoutePermission('/user')).toBe(true);
  });

  it('allows static fallback pages when role exists but menu tree is empty', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['system-manager'],
        roles: [{ id: 11, roleCode: 'system-manager', roleName: 'System Manager' }],
        menus: []
      })
    );

    expect(permissionStore.hasRoutePermission('/user')).toBe(true);
    expect(permissionStore.hasRoutePermission('/not-exists')).toBe(false);
  });

  it('expands the legacy automation-test menu into the split quality workbench routes', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['DEVELOPER_STAFF'],
        roles: [{ id: 31, roleCode: 'DEVELOPER_STAFF', roleName: '开发人员' }],
        menus: [
          {
            id: 93000005,
            menuName: '质量工场',
            menuCode: 'quality-workbench',
            path: '',
            type: 0,
            children: [
              {
                id: 93003009,
                parentId: 93000005,
                menuName: '自动化工场（兼容入口）',
                menuCode: 'system:automation-test',
                path: '/automation-test',
                type: 1,
                children: []
              }
            ]
          }
        ]
      })
    );

    expect(permissionStore.allowedPaths).toContain('/automation-test');
    expect(permissionStore.allowedPaths).toContain('/rd-workbench');
    expect(permissionStore.allowedPaths).toContain('/rd-automation-plans');
    expect(permissionStore.allowedPaths).toContain('/automation-results');
    expect(permissionStore.hasRoutePermission('/quality-workbench')).toBe(true);
    expect(permissionStore.hasRoutePermission('/rd-workbench')).toBe(true);
    expect(permissionStore.hasRoutePermission('/rd-automation-handoff')).toBe(true);
    expect(permissionStore.hasRoutePermission('/automation-execution')).toBe(true);
    expect(permissionStore.hasRoutePermission('/automation-results')).toBe(true);
  });

  it('expands the legacy automation-assets menu into the rd authoring routes only', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['DEVELOPER_STAFF'],
        roles: [{ id: 32, roleCode: 'DEVELOPER_STAFF', roleName: '开发人员' }],
        menus: [
          {
            id: 93000005,
            menuName: '质量工场',
            menuCode: 'quality-workbench',
            path: '',
            type: 0,
            children: [
              {
                id: 93003012,
                parentId: 93000005,
                menuName: '自动化资产中心（兼容入口）',
                menuCode: 'system:automation-assets',
                path: '/automation-assets',
                type: 1,
                children: []
              }
            ]
          }
        ]
      })
    );

    expect(permissionStore.allowedPaths).toContain('/automation-assets');
    expect(permissionStore.allowedPaths).toContain('/rd-workbench');
    expect(permissionStore.allowedPaths).toContain('/rd-automation-inventory');
    expect(permissionStore.hasRoutePermission('/rd-automation-templates')).toBe(true);
    expect(permissionStore.hasRoutePermission('/automation-results')).toBe(false);
  });

  it('keeps no-role users blocked even when menu tree is empty', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(createAuthContext({ menus: [] }));

    expect(permissionStore.hasRoutePermission('/user')).toBe(false);
  });

  it('reuses cached auth context on first refresh without forcing /api/auth/me', async () => {
    const cachedContext = createAuthContext({
      roleCodes: ['OPS_STAFF'],
      roles: [{ id: 11, roleCode: 'OPS_STAFF', roleName: 'Ops' }],
      homePath: '/products',
      menus: [
        {
          id: 1001,
          menuName: 'Products',
          menuCode: 'device:product',
          path: '/products',
          type: 1,
          children: []
        }
      ]
    });

    window.localStorage.setItem(ACCESS_TOKEN_KEY, 'cached-token');
    window.localStorage.setItem(AUTH_CONTEXT_KEY, JSON.stringify(cachedContext));

    const permissionStore = usePermissionStore();
    const result = await permissionStore.ensureInitialized();

    expect(permissionStore.initialized).toBe(true);
    expect(permissionStore.isLoggedIn).toBe(true);
    expect(result).toEqual(cachedContext);
    expect(getCurrentUserMock).not.toHaveBeenCalled();
  });

  it('keeps cached auth context when forced refresh validation is not a 401', async () => {
    const cachedContext = createAuthContext({
      roleCodes: ['OPS_STAFF'],
      roles: [{ id: 11, roleCode: 'OPS_STAFF', roleName: 'Ops' }],
      menus: [
        {
          id: 1001,
          menuName: 'Products',
          menuCode: 'device:product',
          path: '/products',
          type: 1,
          children: []
        }
      ]
    });

    window.localStorage.setItem(ACCESS_TOKEN_KEY, 'cached-token');
    window.localStorage.setItem(AUTH_CONTEXT_KEY, JSON.stringify(cachedContext));
    getCurrentUserMock.mockRejectedValueOnce(new Error('network down'));

    const permissionStore = usePermissionStore();

    await expect(permissionStore.ensureInitialized(true)).rejects.toThrow('network down');
    expect(permissionStore.isLoggedIn).toBe(true);
    expect(permissionStore.authContext).toEqual(cachedContext);
  });

  it('clears cached auth context when forced refresh validation returns 401', async () => {
    const cachedContext = createAuthContext({
      roleCodes: ['OPS_STAFF'],
      roles: [{ id: 11, roleCode: 'OPS_STAFF', roleName: 'Ops' }],
      menus: [
        {
          id: 1001,
          menuName: 'Products',
          menuCode: 'device:product',
          path: '/products',
          type: 1,
          children: []
        }
      ]
    });
    const unauthorizedError = new Error('expired') as RequestError;
    unauthorizedError.status = 401;

    window.localStorage.setItem(ACCESS_TOKEN_KEY, 'cached-token');
    window.localStorage.setItem(AUTH_CONTEXT_KEY, JSON.stringify(cachedContext));
    getCurrentUserMock.mockRejectedValueOnce(unauthorizedError);

    const permissionStore = usePermissionStore();

    await expect(permissionStore.ensureInitialized(true)).rejects.toThrow('expired');
    expect(permissionStore.isLoggedIn).toBe(false);
    expect(permissionStore.authContext).toBeNull();
    expect(window.localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(window.localStorage.getItem(AUTH_CONTEXT_KEY)).toBeNull();
  });
});
