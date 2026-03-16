import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

// 用户角色类型
export type UserRole = 'field' | 'ops' | 'manager';

export interface PermissionConfig {
  role: UserRole;
  name: string;
  description: string;
  menus: string[];
  actions: string[];
}

const ACCESS_TOKEN_KEY = 'spring-boot-iot.access-token';
const USER_INFO_KEY = 'spring-boot-iot.user-info';

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

export function getStoredAccessToken(): string {
  return readStorage(ACCESS_TOKEN_KEY);
}

export function clearStoredAuth(): void {
  removeStorage(ACCESS_TOKEN_KEY);
  removeStorage(USER_INFO_KEY);
}

interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  role: UserRole;
  avatar?: string;
  permissions?: string[];
}

function resolveInitialUserInfo(): UserInfo | null {
  const raw = readStorage(USER_INFO_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserInfo;
  } catch {
    return null;
  }
}

export const PERMISSION_CONFIGS: Record<UserRole, PermissionConfig> = {
  field: {
    role: 'field',
    name: '一线人员',
    description: '负责风险监测、研判和处置',
    menus: ['dashboard', 'insight', 'reporting'],
    actions: ['view', 'report', 'generate_report']
  },
  ops: {
    role: 'ops',
    name: '运维人员',
    description: '负责设备运维、远程控制和参数配置',
    menus: ['dashboard', 'devices', 'config', 'debug'],
    actions: ['view', 'control', 'configure', 'debug']
  },
  manager: {
    role: 'manager',
    name: '管理人员',
    description: '负责整体态势监控、报告生成和数据分析',
    menus: ['dashboard', 'reporting', 'analytics', 'settings'],
    actions: ['view', 'analyze', 'generate', 'configure']
  }
};

export const usePermissionStore = defineStore('permission', () => {
  const token = ref<string>(getStoredAccessToken());
  const currentRole = ref<UserRole>('manager');
  const userInfo = ref<UserInfo | null>(resolveInitialUserInfo());

  if (userInfo.value?.role) {
    currentRole.value = userInfo.value.role;
  }

  const isLoggedIn = computed(() => Boolean(token.value));

  const currentRoleConfig = computed(() => {
    return PERMISSION_CONFIGS[currentRole.value];
  });

  const hasPermission = (action: string): boolean => {
    if (!isLoggedIn.value) {
      return false;
    }
    return currentRoleConfig.value.actions.includes(action);
  };

  const hasMenuPermission = (menu: string): boolean => {
    if (!isLoggedIn.value) {
      return false;
    }
    return currentRoleConfig.value.menus.includes(menu);
  };

  const switchRole = (role: UserRole): void => {
    currentRole.value = role;
  };

  // 登录成功后统一更新 token 和用户信息，保证刷新后仍能恢复会话。
  const login = (user: UserInfo, accessToken?: string): void => {
    userInfo.value = user;
    currentRole.value = user.role;

    if (accessToken) {
      token.value = accessToken;
      writeStorage(ACCESS_TOKEN_KEY, accessToken);
    }
    writeStorage(USER_INFO_KEY, JSON.stringify(user));
  };

  const setAccessToken = (accessToken: string): void => {
    token.value = accessToken;
    if (accessToken) {
      writeStorage(ACCESS_TOKEN_KEY, accessToken);
      return;
    }
    removeStorage(ACCESS_TOKEN_KEY);
  };

  const logout = (): void => {
    token.value = '';
    userInfo.value = null;
    currentRole.value = 'manager';
    clearStoredAuth();
  };

  const updateUserInfo = (user: Partial<UserInfo>): void => {
    if (!userInfo.value) {
      return;
    }
    userInfo.value = { ...userInfo.value, ...user };
    if (userInfo.value.role) {
      currentRole.value = userInfo.value.role;
    }
    writeStorage(USER_INFO_KEY, JSON.stringify(userInfo.value));
  };

  return {
    token,
    currentRole,
    currentRoleConfig,
    isLoggedIn,
    userInfo,
    hasPermission,
    hasMenuPermission,
    switchRole,
    login,
    setAccessToken,
    logout,
    updateUserInfo
  };
});
