import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';

import { changePassword, updateCurrentUserProfile } from '../api/user';
import { usePermissionStore } from '../stores/permission';
import type {
  ShellAccountCenterState,
  ShellAccountSummary,
  ShellPasswordPayload,
  ShellProfilePayload
} from '../types/shell';

export function useShellAccountCenter(): ShellAccountCenterState {
  const router = useRouter();
  const permissionStore = usePermissionStore();
  const showAccountDialog = ref(false);
  const showChangePasswordDialog = ref(false);
  const passwordSubmitting = ref(false);
  const profileSubmitting = ref(false);

  const headerIdentity = computed(() => {
    if (!permissionStore.isLoggedIn) {
      return '访客模式';
    }
    const roleText = permissionStore.roleNames.join(' / ') || '未分配角色';
    return `当前角色：${roleText}`;
  });

  const headerAccountName = computed(() => {
    if (!permissionStore.isLoggedIn) {
      return '访客账号';
    }
    return permissionStore.displayName || permissionStore.userInfo?.nickname || permissionStore.userInfo?.username || '系统管理员';
  });

  const headerAccountCode = computed(() => permissionStore.userInfo?.username || 'guest');
  const headerAccountType = computed(() => permissionStore.userInfo?.accountType || '子账号');

  const headerRoleName = computed(() => {
    if (!permissionStore.isLoggedIn) {
      return '未登录';
    }
    return permissionStore.roleNames.join(' / ') || permissionStore.primaryRoleName || '未分配角色';
  });

  const headerAuthStatus = computed(() => {
    if (!permissionStore.isLoggedIn) {
      return '未登录';
    }
    return permissionStore.userInfo?.authStatus || '未填写实名信息';
  });

  const headerLoginMethods = computed(() => {
    if (!permissionStore.isLoggedIn) {
      return '账号登录';
    }
    const methods = permissionStore.userInfo?.loginMethods || [];
    return methods.length > 0 ? methods.join(' / ') : '账号登录';
  });

  const maskedPhone = computed(() => maskPhone(permissionStore.userInfo?.phone));
  const maskedEmail = computed(() => maskEmail(permissionStore.userInfo?.email));

  const headerPrimaryContact = computed(() => {
    const orgName = permissionStore.userInfo?.orgName?.trim();
    if (orgName) {
      return `机构：${orgName}`;
    }
    if (maskedPhone.value !== '未绑定手机号') {
      return `手机号：${maskedPhone.value}`;
    }
    if (maskedEmail.value !== '未绑定邮箱') {
      return `邮箱：${maskedEmail.value}`;
    }
    return '';
  });

  const accountInitial = computed(() => {
    const source = headerAccountName.value.trim();
    return source ? source.slice(0, 1).toUpperCase() : '管';
  });

  const accountSummary = computed<ShellAccountSummary>(() => ({
    initial: accountInitial.value,
    name: headerAccountName.value,
    code: headerAccountCode.value,
    type: headerAccountType.value,
    roleName: headerRoleName.value,
    tenantName: permissionStore.userInfo?.tenantName || '默认租户',
    orgName: permissionStore.userInfo?.orgName || '未关联机构',
    nickname: permissionStore.userInfo?.nickname || '',
    realName: permissionStore.authContext?.realName || '',
    displayName: permissionStore.displayName || headerAccountCode.value,
    phone: permissionStore.userInfo?.phone || '',
    email: permissionStore.userInfo?.email || '',
    authStatus: headerAuthStatus.value,
    loginMethods: headerLoginMethods.value,
    dataScopeSummary: permissionStore.userInfo?.dataScopeSummary || '未配置',
    lastLoginTime: permissionStore.userInfo?.lastLoginTime || '暂无记录',
    lastLoginIp: permissionStore.userInfo?.lastLoginIp || '暂无记录',
    primaryContact: headerPrimaryContact.value
  }));

  function closeAccountOverlays() {
    showAccountDialog.value = false;
    showChangePasswordDialog.value = false;
  }

  function openAccountCenter() {
    if (!permissionStore.isLoggedIn) {
      void router.push('/login');
      return;
    }
    showAccountDialog.value = true;
    showChangePasswordDialog.value = false;
  }

  function openChangePasswordDialog() {
    if (!permissionStore.isLoggedIn) {
      void router.push('/login');
      return;
    }
    showAccountDialog.value = false;
    showChangePasswordDialog.value = true;
    passwordSubmitting.value = false;
  }

  function closeChangePasswordDialog() {
    showChangePasswordDialog.value = false;
    passwordSubmitting.value = false;
  }

  async function submitProfileUpdate(payload: ShellProfilePayload): Promise<void> {
    if (!permissionStore.userInfo?.id) {
      ElMessage.error('当前登录信息缺失，请重新登录后再试');
      return;
    }

    profileSubmitting.value = true;
    try {
      await updateCurrentUserProfile({
        nickname: payload.nickname?.trim() || '',
        realName: payload.realName?.trim() || '',
        phone: payload.phone?.trim() || '',
        email: payload.email?.trim() || '',
        avatar: payload.avatar?.trim() || ''
      });
      await permissionStore.fetchCurrentUser();
      ElMessage.success('账号资料已更新');
    } catch {
      profileSubmitting.value = false;
      return;
    }
    profileSubmitting.value = false;
  }

  async function submitChangePassword(payload: ShellPasswordPayload): Promise<void> {
    const currentUserId = permissionStore.userInfo?.id;
    if (!currentUserId) {
      ElMessage.error('当前登录信息缺失，请重新登录后再试');
      return;
    }
    if (!payload.oldPassword || !payload.newPassword || !payload.confirmPassword) {
      ElMessage.warning('请完整填写密码信息');
      return;
    }
    if (payload.newPassword.length < 6) {
      ElMessage.warning('新密码长度不能少于 6 位');
      return;
    }
    if (payload.newPassword !== payload.confirmPassword) {
      ElMessage.warning('两次输入的新密码不一致');
      return;
    }

    passwordSubmitting.value = true;
    try {
      await changePassword({
        id: currentUserId,
        oldPassword: payload.oldPassword,
        newPassword: payload.newPassword
      });
      closeChangePasswordDialog();
      permissionStore.logout();
      await router.push('/login');
      ElMessage.success('密码已修改，请重新登录');
    } catch {
      passwordSubmitting.value = false;
    }
  }

  function handleLogout() {
    closeAccountOverlays();
    permissionStore.logout();
    void router.push('/login');
    ElMessage.success('已退出登录');
  }

  return {
    showAccountDialog,
    showChangePasswordDialog,
    passwordSubmitting,
    profileSubmitting,
    headerIdentity,
    accountSummary,
    openAccountCenter,
    openChangePasswordDialog,
    closeAccountOverlays,
    closeChangePasswordDialog,
    submitProfileUpdate,
    submitChangePassword,
    handleLogout
  };
}

function maskPhone(phone?: string | null): string {
  const value = (phone || '').trim();
  if (!value) {
    return '未绑定手机号';
  }
  if (value.length < 7) {
    return value;
  }
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}

function maskEmail(email?: string | null): string {
  const value = (email || '').trim();
  if (!value) {
    return '未绑定邮箱';
  }
  const [name, domain] = value.split('@');
  if (!name || !domain) {
    return value;
  }
  if (name.length <= 2) {
    return `${name.slice(0, 1)}***@${domain}`;
  }
  return `${name.slice(0, 2)}***@${domain}`;
}
