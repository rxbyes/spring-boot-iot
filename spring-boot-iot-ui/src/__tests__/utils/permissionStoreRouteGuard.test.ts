import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import { usePermissionStore } from '@/stores/permission';
import type { UserAuthContext } from '@/types/auth';

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

  it('keeps no-role users blocked even when menu tree is empty', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(createAuthContext({ menus: [] }));

    expect(permissionStore.hasRoutePermission('/user')).toBe(false);
  });
});
