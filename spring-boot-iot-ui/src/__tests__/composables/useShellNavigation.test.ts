import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

const { pushMock, routeState } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  routeState: {
    path: '/',
    meta: {} as Record<string, unknown>
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    push: pushMock
  })
}));

import { useShellNavigation } from '@/composables/useShellNavigation';
import { usePermissionStore } from '@/stores/permission';
import type { MenuTreeNode, UserAuthContext } from '@/types/auth';

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

function createDynamicMenus(): MenuTreeNode[] {
  return [
    {
      id: 100,
      menuName: '接入智维',
      menuCode: 'iot-access',
      path: '',
      type: 0,
      meta: {
        description: '资产、链路与异常观测',
        menuTitle: '接入智维',
        menuHint: '覆盖产品定义、设备资产、链路校验与异常观测。'
      },
      children: [
        {
          id: 101,
          parentId: 100,
          menuName: '产品定义中心',
          menuCode: 'device:product',
          path: '/products',
          type: 1,
          meta: {
            caption: '产品台账与接入契约'
          },
          children: []
        },
        {
          id: 102,
          parentId: 100,
          menuName: '设备资产中心',
          menuCode: 'device:device',
          path: '/devices',
          type: 1,
          meta: {
            caption: '设备建档与运维资产'
          },
          children: []
        }
      ]
    }
  ];
}

function createLegacyQualityMenus(): MenuTreeNode[] {
  return [
    {
      id: 93000005,
      menuName: '质量工场',
      menuCode: 'quality-workbench',
      path: '',
      type: 0,
      meta: {
        description: '研发工场、执行组织与结果基线',
        menuTitle: '质量工场',
        menuHint: '覆盖研发资产编排、执行组织与结果基线治理。'
      },
      children: [
        {
          id: 93003009,
          parentId: 93000005,
          menuName: '自动化工场（兼容入口）',
          menuCode: 'system:automation-test',
          path: '/automation-test',
          type: 1,
          meta: {
            caption: '兼容旧入口'
          },
          children: []
        }
      ]
    }
  ];
}

describe('useShellNavigation', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    window.localStorage.clear();
    pushMock.mockReset();
    routeState.path = '/';
    routeState.meta = {};
  });

  it('uses the guest overview group before login', () => {
    routeState.path = '/';
    routeState.meta = { title: '平台首页' };

    const navigation = useShellNavigation();

    expect(navigation.navigationGroups.value).toHaveLength(1);
    expect(navigation.navigationGroups.value[0]?.key).toBe('guest-overview');
    expect(navigation.activeGroup.value.key).toBe('guest-overview');
    expect(navigation.showSidebarContext.value).toBe(true);
    expect(navigation.activeTitle.value).toBe('平台首页');
  });

  it('builds dynamic groups from backend menus and routes group switches to the section landing page', async () => {
    routeState.path = '/products';
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['OPS_STAFF'],
        roles: [{ id: 11, roleCode: 'OPS_STAFF', roleName: '运维人员' }],
        homePath: '/products',
        menus: createDynamicMenus()
      })
    );

    const navigation = useShellNavigation();
    const accessGroup = navigation.navigationGroups.value.find((group) => group.key === 'iot-access');

    expect(accessGroup?.items.map((item) => item.to)).toEqual(['/device-access', '/products', '/devices']);
    expect(navigation.activeGroup.value.key).toBe('iot-access');
    expect(navigation.showSidebarContext.value).toBe(false);
    expect(navigation.activeTitle.value).toBe('产品定义中心');

    navigation.switchGroup('iot-access');

    expect(pushMock).toHaveBeenCalledWith('/device-access');
  });

  it('falls back to the shared static schema when roles exist but the menu tree is empty', () => {
    routeState.path = '/user';
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['SYSTEM_MANAGER'],
        roles: [{ id: 21, roleCode: 'SYSTEM_MANAGER', roleName: '系统管理员' }],
        menus: []
      })
    );

    const navigation = useShellNavigation();
    const governanceGroup = navigation.navigationGroups.value.find((group) => group.key === 'system-governance');

    expect(governanceGroup).toBeTruthy();
    expect(governanceGroup?.items[0]?.to).toBe('/system-management');
    expect(governanceGroup?.items.some((item) => item.to === '/user')).toBe(true);
    expect(navigation.activeGroup.value.key).toBe('system-governance');

    navigation.switchGroup('system-governance');

    expect(pushMock).toHaveBeenCalledWith('/system-management');
  });

  it('upgrades the legacy quality-workbench menu tree to the split rd/execution/results navigation', () => {
    routeState.path = '/rd-automation-plans';
    routeState.meta = { title: '计划编排台' };
    const permissionStore = usePermissionStore();
    permissionStore.setAccessToken('token');
    permissionStore.setAuthContext(
      createAuthContext({
        roleCodes: ['DEVELOPER_STAFF'],
        roles: [{ id: 31, roleCode: 'DEVELOPER_STAFF', roleName: '开发人员' }],
        homePath: '/quality-workbench',
        menus: createLegacyQualityMenus()
      })
    );

    const navigation = useShellNavigation();
    const qualityGroup = navigation.navigationGroups.value.find((group) => group.key === 'quality-workbench');

    expect(qualityGroup?.items.map((item) => item.to)).toEqual([
      '/quality-workbench',
      '/rd-workbench',
      '/rd-automation-inventory',
      '/rd-automation-templates',
      '/rd-automation-plans',
      '/rd-automation-handoff',
      '/automation-execution',
      '/automation-results'
    ]);
    expect(qualityGroup?.items.some((item) => item.to === '/automation-test')).toBe(false);
    expect(navigation.activeGroup.value.key).toBe('quality-workbench');
    expect(navigation.activeTitle.value).toBe('计划编排台');
  });
});
