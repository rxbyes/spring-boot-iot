<template>
  <div class="standard-drawer-footer">
    <slot name="prefix" />

    <div class="standard-drawer-footer__actions">
      <slot>
        <el-button
          v-if="showCancel"
          class="standard-drawer-footer__button standard-drawer-footer__button--ghost"
          @click="emit('cancel')"
        >
          {{ cancelText }}
        </el-button>
        <el-button
          :type="confirmType"
          class="standard-drawer-footer__button"
          :class="{
            'standard-drawer-footer__button--primary': confirmType === 'primary' && !danger,
            'standard-drawer-footer__button--danger': danger
          }"
          :loading="confirmLoading"
          :disabled="confirmDisabled"
          @click="emit('confirm')"
        >
          {{ confirmText }}
        </el-button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    cancelText?: string
    confirmText?: string
    confirmType?: '' | 'primary' | 'success' | 'warning' | 'info' | 'danger'
    confirmLoading?: boolean
    confirmDisabled?: boolean
    showCancel?: boolean
    danger?: boolean
  }>(),
  {
    cancelText: '取消',
    confirmText: '确定',
    confirmType: 'primary',
    confirmLoading: false,
    confirmDisabled: false,
    showCancel: true,
    danger: false
  }
)

const emit = defineEmits<{
  (event: 'cancel'): void
  (event: 'confirm'): void
}>()
</script>
