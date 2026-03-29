<template>
  <StandardFormDrawer
    :model-value="showAccountDialog"
    title="账号中心"
    subtitle="统一通过右侧抽屉查看当前账号、角色、联系方式与认证状态。"
    size="32rem"
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

      <dl class="account-dialog__list">
        <div><dt>账号类型</dt><dd>{{ summary.type }}</dd></div>
        <div><dt>当前角色</dt><dd>{{ summary.roleName }}</dd></div>
        <div><dt>真实姓名</dt><dd>{{ summary.realName }}</dd></div>
        <div><dt>显示名称</dt><dd>{{ summary.displayName }}</dd></div>
        <div><dt>手机号</dt><dd>{{ summary.phone }}</dd></div>
        <div><dt>邮箱</dt><dd>{{ summary.email }}</dd></div>
        <div><dt>认证状态</dt><dd>{{ summary.authStatus }}</dd></div>
        <div><dt>登录方式</dt><dd>{{ summary.loginMethods }}</dd></div>
      </dl>
    </div>

    <template #footer>
      <StandardButton action="reset" @click="$emit('openRealNameAuth')">实名认证</StandardButton>
      <StandardButton action="reset" @click="$emit('openLoginMethods')">登录方式</StandardButton>
      <StandardButton action="reset" @click="$emit('openChangePasswordDialog')">修改密码</StandardButton>
      <StandardButton action="confirm" @click="updateAccountDialog(false)">关闭</StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    :model-value="showRealNameAuthDialog"
    title="实名认证"
    subtitle="当前版本先展示账号实名信息与接入状态，后续再补独立认证流程。"
    size="30rem"
    @update:modelValue="updateRealNameAuthDialog"
  >
    <div class="account-dialog">
      <dl class="account-dialog__list">
        <div><dt>实名状态</dt><dd>{{ summary.authStatus }}</dd></div>
        <div><dt>实名姓名</dt><dd>{{ summary.realName }}</dd></div>
        <div><dt>账号类型</dt><dd>{{ summary.type }}</dd></div>
        <div><dt>当前说明</dt><dd>当前共享环境仅展示实名信息状态，不提供外部实名认证提交流程。</dd></div>
      </dl>
    </div>

    <template #footer>
      <StandardButton action="reset" @click="$emit('openAccountCenter')">返回账号中心</StandardButton>
      <StandardButton action="confirm" @click="updateRealNameAuthDialog(false)">我知道了</StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    :model-value="showLoginMethodsDialog"
    title="登录方式"
    subtitle="展示当前账号可用的登录方式与联系信息，后续再补独立绑定流程。"
    size="30rem"
    @update:modelValue="updateLoginMethodsDialog"
  >
    <div class="account-dialog">
      <dl class="account-dialog__list">
        <div><dt>可用登录方式</dt><dd>{{ summary.loginMethods }}</dd></div>
        <div><dt>账号登录</dt><dd>{{ summary.code }}</dd></div>
        <div><dt>手机号登录</dt><dd>{{ summary.phone }}</dd></div>
        <div><dt>邮箱通知</dt><dd>{{ summary.email }}</dd></div>
      </dl>
    </div>

    <template #footer>
      <StandardButton action="reset" @click="$emit('openAccountCenter')">返回账号中心</StandardButton>
      <StandardButton action="confirm" @click="updateLoginMethodsDialog(false)">关闭</StandardButton>
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

import type { ShellAccountDrawersProps, ShellPasswordPayload } from '../types/shell';
import StandardFormDrawer from './StandardFormDrawer.vue';

const props = defineProps<ShellAccountDrawersProps>();

const emit = defineEmits<{
  (event: 'update:showAccountDialog', value: boolean): void;
  (event: 'update:showRealNameAuthDialog', value: boolean): void;
  (event: 'update:showLoginMethodsDialog', value: boolean): void;
  (event: 'update:showChangePasswordDialog', value: boolean): void;
  (event: 'openRealNameAuth'): void;
  (event: 'openLoginMethods'): void;
  (event: 'openAccountCenter'): void;
  (event: 'openChangePasswordDialog'): void;
  (event: 'submitChangePassword', payload: ShellPasswordPayload): void;
}>();

const passwordForm = reactive<ShellPasswordPayload>({ oldPassword: '', newPassword: '', confirmPassword: '' });

watch(() => props.showChangePasswordDialog, () => resetPasswordForm(), { immediate: true });

function resetPasswordForm() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

function updatePasswordField(field: keyof ShellPasswordPayload, value: string) {
  passwordForm[field] = value;
}

function updateAccountDialog(value: boolean) { emit('update:showAccountDialog', value); }
function updateRealNameAuthDialog(value: boolean) { emit('update:showRealNameAuthDialog', value); }
function updateLoginMethodsDialog(value: boolean) { emit('update:showLoginMethodsDialog', value); }
function updateChangePasswordDialog(value: boolean) { emit('update:showChangePasswordDialog', value); }
function submitChangePassword() { emit('submitChangePassword', { ...passwordForm }); }
</script>

<style scoped>
.account-dialog { display: grid; gap: 1rem; }
.account-dialog__hero { display: flex; align-items: center; gap: 0.85rem; padding: 1rem; border-radius: 0.85rem; background: linear-gradient(180deg, #f8fbff 0%, var(--bg-card) 100%); border: 1px solid #e2eaf5; }
.account-dialog__avatar { width: 3rem; height: 3rem; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 1rem; font-weight: 700; color: var(--bg-card); background: linear-gradient(160deg, var(--accent), var(--accent-deep)); }
.account-dialog__hero-content strong { display: block; color: var(--text-heading); font-size: 1rem; }
.account-dialog__hero-content p { margin: 0.22rem 0 0; color: #5e769e; font-size: 0.8rem; }
.account-dialog__list { margin: 0; display: grid; gap: 0.7rem; }
.account-dialog__list div { display: grid; gap: 0.22rem; padding: 0.9rem 1rem; border-radius: 0.8rem; background: var(--surface-muted); border: 1px solid #e8edf5; }
.account-dialog__list dt { color: #7b8ca6; font-size: 0.75rem; }
.account-dialog__list dd { margin: 0; color: var(--text-heading); font-size: 0.86rem; font-weight: 500; }
.account-password-form { padding-top: 0.25rem; }
</style>
