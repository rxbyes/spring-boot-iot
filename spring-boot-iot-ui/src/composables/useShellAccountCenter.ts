import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';

import { changePassword } from '../api/user';
import { usePermissionStore } from '../stores/permission';
import type { ShellAccountCenterState, ShellAccountSummary, ShellPasswordPayload } from '../types/shell';

export function useShellAccountCenter(): ShellAccountCenterState {
  const router = useRouter();
  const permissionStore = usePermissionStore();
  const showAccountDialog = ref(false);
  const showRealNameAuthDialog = ref(false);
  const showLoginMethodsDialog = ref(false);
  const showChangePasswordDialog = ref(false);
  const passwordSubmitting = ref(false);

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
    return permissionStore.displayName || permissionStore.userInfo?.username || '系统管理员';
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
    realName: permissionStore.authContext?.realName || '未填写',
    displayName: permissionStore.displayName || headerAccountCode.value,
    phone: maskedPhone.value,
    email: maskedEmail.value,
    authStatus: headerAuthStatus.value,
    loginMethods: headerLoginMethods.value,
    primaryContact: headerPrimaryContact.value
  }));

  function closeAccountOverlays() {
    showAccountDialog.value = false;
    showRealNameAuthDialog.value = false;
    showLoginMethodsDialog.value = false;
    showChangePasswordDialog.value = false;
  }

  function openAccountCenter() {
    showRealNameAuthDialog.value = false;
    showLoginMethodsDialog.value = false;
    showAccountDialog.value = true;
  }

  function openRealNameAuth() {
    showAccountDialog.value = false;
    showLoginMethodsDialog.value = false;
    showRealNameAuthDialog.value = true;
  }

  function openLoginMethods() {
    showAccountDialog.value = false;
    showRealNameAuthDialog.value = false;
    showLoginMethodsDialog.value = true;
  }

  function openChangePasswordDialog() {
    if (!permissionStore.isLoggedIn) {
      router.push('/login');
      return;
    }
    showAccountDialog.value = false;
    showRealNameAuthDialog.value = false;
    showLoginMethodsDialog.value = false;
    showChangePasswordDialog.value = true;
    passwordSubmitting.value = false;
  }

  function closeChangePasswordDialog() {
    showChangePasswordDialog.value = false;
    passwordSubmitting.value = false;
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
    router.push('/login');
    ElMessage.success('已退出登录');
  }

  return {
    showAccountDialog,
    showRealNameAuthDialog,
    showLoginMethodsDialog,
    showChangePasswordDialog,
    passwordSubmitting,
    headerIdentity,
    accountSummary,
    openAccountCenter,
    openRealNameAuth,
    openLoginMethods,
    openChangePasswordDialog,
    closeAccountOverlays,
    closeChangePasswordDialog,
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
