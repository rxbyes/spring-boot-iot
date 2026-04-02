<template>
  <StandardFormDrawer
    :model-value="showAccountDialog"
    title="账号中心"
    subtitle="统一通过右侧抽屉维护基础资料、机构角色、安全信息与实名资料。"
    size="36rem"
    @update:modelValue="updateAccountDialog"
  >
    <div class="account-dialog">
      <div class="account-dialog__hero">
        <span class="account-dialog__avatar">{{ summary.initial }}</span>
        <div class="account-dialog__hero-content">
          <strong>{{ summary.name }}</strong>
          <p>登录账号：{{ summary.code }}</p>
        </div>
      </div>

      <section class="account-section">
        <header class="account-section__header">
          <h3>基础资料</h3>
          <p>可直接维护当前账号昵称、实名、联系方式。</p>
        </header>
        <el-form label-position="top" class="account-profile-form">
          <div class="account-profile-form__grid">
            <el-form-item label="昵称">
              <el-input :model-value="profileForm.nickname" placeholder="请输入昵称" @update:modelValue="updateProfileField('nickname', $event)" />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input :model-value="profileForm.realName" placeholder="请输入真实姓名" @update:modelValue="updateProfileField('realName', $event)" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :model-value="profileForm.phone" placeholder="请输入手机号" @update:modelValue="updateProfileField('phone', $event)" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input :model-value="profileForm.email" placeholder="请输入邮箱" @update:modelValue="updateProfileField('email', $event)" />
            </el-form-item>
          </div>
        </el-form>
      </section>

      <section class="account-section">
        <header class="account-section__header">
          <h3>机构与角色</h3>
          <p>展示当前登录账号的租户、主机构、角色和数据范围。</p>
        </header>
        <dl class="account-section__list">
          <div><dt>所属租户</dt><dd>{{ summary.tenantName }}</dd></div>
          <div><dt>主机构</dt><dd>{{ summary.orgName }}</dd></div>
          <div><dt>当前角色</dt><dd>{{ summary.roleName }}</dd></div>
          <div><dt>数据范围</dt><dd>{{ summary.dataScopeSummary }}</dd></div>
        </dl>
      </section>

      <section class="account-section">
        <header class="account-section__header">
          <h3>安全信息</h3>
          <p>统一查看账号类型、可用登录方式和最近一次登录记录。</p>
        </header>
        <dl class="account-section__list">
          <div><dt>账号类型</dt><dd>{{ summary.type }}</dd></div>
          <div><dt>登录方式</dt><dd>{{ summary.loginMethods }}</dd></div>
          <div><dt>最后登录时间</dt><dd>{{ summary.lastLoginTime }}</dd></div>
          <div><dt>最后登录 IP</dt><dd>{{ summary.lastLoginIp }}</dd></div>
        </dl>
      </section>

      <section class="account-section">
        <header class="account-section__header">
          <h3>实名资料</h3>
          <p>实名信息统一在账号中心展示，不再拆分独立静态页面。</p>
        </header>
        <dl class="account-section__list">
          <div><dt>认证状态</dt><dd>{{ summary.authStatus }}</dd></div>
          <div><dt>实名姓名</dt><dd>{{ profileForm.realName || summary.realName || '未填写' }}</dd></div>
          <div><dt>显示名称</dt><dd>{{ summary.displayName }}</dd></div>
          <div><dt>主要联系</dt><dd>{{ summary.primaryContact || '未填写' }}</dd></div>
        </dl>
      </section>
    </div>

    <template #footer>
      <StandardButton action="cancel" @click="updateAccountDialog(false)">关闭</StandardButton>
      <StandardButton action="reset" @click="$emit('openChangePasswordDialog')">修改密码</StandardButton>
      <StandardButton action="confirm" :loading="profileSubmitting" @click="submitProfileUpdate">保存资料</StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    :model-value="showChangePasswordDialog"
    title="修改密码"
    subtitle="统一通过右侧抽屉完成密码修改，提交成功后需要重新登录。"
    size="30rem"
    @update:modelValue="updateChangePasswordDialog"
  >
    <el-form label-position="top" class="account-password-form">
      <el-form-item label="原密码">
        <el-input :model-value="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" @update:modelValue="updatePasswordField('oldPassword', $event)" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input :model-value="passwordForm.newPassword" type="password" show-password autocomplete="new-password" @update:modelValue="updatePasswordField('newPassword', $event)" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input :model-value="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" @update:modelValue="updatePasswordField('confirmPassword', $event)" />
      </el-form-item>
    </el-form>

    <template #footer>
      <StandardButton action="cancel" @click="updateChangePasswordDialog(false)">取消</StandardButton>
      <StandardButton action="confirm" :loading="passwordSubmitting" @click="submitChangePassword">确认修改</StandardButton>
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue';

import type {
  ShellAccountDrawersProps,
  ShellPasswordPayload,
  ShellProfilePayload
} from '../types/shell';
import StandardFormDrawer from './StandardFormDrawer.vue';

const props = defineProps<ShellAccountDrawersProps>();

const emit = defineEmits<{
  (event: 'update:showAccountDialog', value: boolean): void;
  (event: 'update:showChangePasswordDialog', value: boolean): void;
  (event: 'openChangePasswordDialog'): void;
  (event: 'submitChangePassword', payload: ShellPasswordPayload): void;
  (event: 'submitProfileUpdate', payload: ShellProfilePayload): void;
}>();

const passwordForm = reactive<ShellPasswordPayload>({ oldPassword: '', newPassword: '', confirmPassword: '' });
const profileForm = reactive<ShellProfilePayload>({ nickname: '', realName: '', phone: '', email: '', avatar: '' });

watch(() => props.showChangePasswordDialog, () => resetPasswordForm(), { immediate: true });
watch(
  () => [props.showAccountDialog, props.summary.nickname, props.summary.realName, props.summary.phone, props.summary.email] as const,
  () => {
    profileForm.nickname = props.summary.nickname || '';
    profileForm.realName = props.summary.realName || '';
    profileForm.phone = props.summary.phone || '';
    profileForm.email = props.summary.email || '';
    profileForm.avatar = '';
  },
  { immediate: true }
);

function resetPasswordForm() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

function updatePasswordField(field: keyof ShellPasswordPayload, value: string) {
  passwordForm[field] = value;
}

function updateProfileField(field: keyof ShellProfilePayload, value: string) {
  profileForm[field] = value;
}

function updateAccountDialog(value: boolean) { emit('update:showAccountDialog', value); }
function updateChangePasswordDialog(value: boolean) { emit('update:showChangePasswordDialog', value); }
function submitChangePassword() { emit('submitChangePassword', { ...passwordForm }); }
function submitProfileUpdate() { emit('submitProfileUpdate', { ...profileForm }); }
</script>

<style scoped>
.account-dialog { display: grid; gap: 1rem; }
.account-dialog__hero { display: flex; align-items: center; gap: 0.85rem; padding: 1rem; border-radius: 0.85rem; background: linear-gradient(180deg, #f8fbff 0%, var(--bg-card) 100%); border: 1px solid #e2eaf5; }
.account-dialog__avatar { width: 3rem; height: 3rem; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 1rem; font-weight: 700; color: var(--bg-card); background: linear-gradient(160deg, var(--accent), var(--accent-deep)); }
.account-dialog__hero-content strong { display: block; color: var(--text-heading); font-size: 1rem; }
.account-dialog__hero-content p { margin: 0.22rem 0 0; color: #5e769e; font-size: 0.8rem; }
.account-section { display: grid; gap: 0.75rem; padding: 1rem; border-radius: 0.85rem; background: var(--surface-muted); border: 1px solid #e8edf5; }
.account-section__header { display: grid; gap: 0.25rem; }
.account-section__header h3 { margin: 0; color: var(--text-heading); font-size: 0.92rem; }
.account-section__header p { margin: 0; color: #6f829d; font-size: 0.76rem; line-height: 1.6; }
.account-profile-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.85rem 0.9rem; }
.account-section__list { margin: 0; display: grid; gap: 0.7rem; }
.account-section__list div { display: grid; gap: 0.22rem; }
.account-section__list dt { color: #7b8ca6; font-size: 0.75rem; }
.account-section__list dd { margin: 0; color: var(--text-heading); font-size: 0.86rem; font-weight: 500; }
.account-password-form { padding-top: 0.25rem; }
@media (max-width: 640px) { .account-profile-form__grid { grid-template-columns: minmax(0, 1fr); } }
</style>
