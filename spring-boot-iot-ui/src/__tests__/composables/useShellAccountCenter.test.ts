import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

const {
  pushMock,
  changePasswordMock,
  updateCurrentUserProfileMock,
  successMock,
  errorMock,
  warningMock
} = vi.hoisted(() => ({
  pushMock: vi.fn(),
  changePasswordMock: vi.fn(),
  updateCurrentUserProfileMock: vi.fn(),
  successMock: vi.fn(),
  errorMock: vi.fn(),
  warningMock: vi.fn()
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}));

vi.mock('@/api/user', () => ({
  changePassword: changePasswordMock,
  updateCurrentUserProfile: updateCurrentUserProfileMock
}));

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: successMock,
    error: errorMock,
    warning: warningMock
  }
}));

import { useShellAccountCenter } from '@/composables/useShellAccountCenter';
import { usePermissionStore } from '@/stores/permission';
import type { UserAuthContext } from '@/types/auth';

function createAuthContext(overrides?: Partial<UserAuthContext>): UserAuthContext {
  return {
    userId: 1,
    username: 'admin',
    nickname: '',
    realName: '',
    displayName: '',
    phone: '',
    email: '',
    accountType: '主账号',
    authStatus: '未填写实名信息',
    loginMethods: ['账号登录'],
    tenantName: '默认租户',
    orgName: '平台治理中心',
    dataScopeSummary: '全局',
    superAdmin: true,
    homePath: '/',
    roleCodes: ['SUPER_ADMIN'],
    permissions: [],
    roles: [{ id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' }],
    menus: [],
    ...overrides
  };
}

describe('useShellAccountCenter', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    window.localStorage.clear();
    pushMock.mockReset();
    changePasswordMock.mockReset();
    updateCurrentUserProfileMock.mockReset();
    successMock.mockReset();
    errorMock.mockReset();
    warningMock.mockReset();
  });

  it('keeps editable profile fields empty when display name falls back to username', () => {
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(createAuthContext());

    const { accountSummary } = useShellAccountCenter();

    expect(accountSummary.value.displayName).toBe('admin');
    expect(accountSummary.value.nickname).toBe('');
    expect(accountSummary.value.realName).toBe('');
  });
});
