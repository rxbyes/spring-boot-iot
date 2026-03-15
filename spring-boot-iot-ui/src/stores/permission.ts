import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

// 用户角色类型
export type UserRole = 'field' | 'ops' | 'manager';

// 权限配置
export interface PermissionConfig {
      role: UserRole;
      name: string;
      description: string;
      menus: string[];
      actions: string[];
}

// 权限配置列表
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

// 权限 store
export const usePermissionStore = defineStore('permission', () => {
      // 当前用户角色
      const currentRole = ref<UserRole>('field');

      // 用户登录状态
      const isLoggedIn = ref(false);

      // 用户信息
      const userInfo = ref<{
            id: number;
            username: string;
            nickname: string;
            role: UserRole;
            avatar?: string;
            permissions?: string[];
      } | null>(null);

      // 获取当前角色配置
      const currentRoleConfig = computed(() => {
            return PERMISSION_CONFIGS[currentRole.value];
      });

      // 检查是否有指定权限
      const hasPermission = (action: string): boolean => {
            if (!isLoggedIn.value) return false;

            const config = currentRoleConfig.value;
            return config.actions.includes(action);
      };

      // 检查是否有指定菜单访问权限
      const hasMenuPermission = (menu: string): boolean => {
            if (!isLoggedIn.value) return false;

            const config = currentRoleConfig.value;
            return config.menus.includes(menu);
      };

      // 切换角色
      const switchRole = (role: UserRole): void => {
            currentRole.value = role;
      };

      // 登录
      const login = (user: {
            id: number;
            username: string;
            nickname: string;
            role: UserRole;
            avatar?: string;
            permissions?: string[];
      }): void => {
            isLoggedIn.value = true;
            userInfo.value = user;
            currentRole.value = user.role;
      };

      // 登出
      const logout = (): void => {
            isLoggedIn.value = false;
            userInfo.value = null;
            currentRole.value = 'field';
      };

      // 更新用户信息
      const updateUserInfo = (user: {
            id: number;
            username: string;
            nickname: string;
            role: UserRole;
            avatar?: string;
            permissions?: string[];
      }): void => {
            if (userInfo.value) {
                  userInfo.value = { ...userInfo.value, ...user };
                  currentRole.value = user.role;
            }
      };

      return {
            currentRole,
            currentRoleConfig,
            isLoggedIn,
            userInfo,
            hasPermission,
            hasMenuPermission,
            switchRole,
            login,
            logout,
            updateUserInfo
      };
});
