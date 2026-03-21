<template>
  <el-drawer
    :model-value="modelValue"
    :size="size"
    direction="rtl"
    :destroy-on-close="destroyOnClose"
    class="standard-form-drawer"
    @close="handleClose"
  >
    <template #header>
      <div class="form-drawer__header">
        <div class="form-drawer__heading">
          <p v-if="eyebrow" class="form-drawer__eyebrow">{{ eyebrow }}</p>
          <h2>{{ title }}</h2>
          <p v-if="subtitle" class="form-drawer__subtitle">{{ subtitle }}</p>
        </div>
      </div>
    </template>

    <div class="form-drawer__body">
      <slot />
    </div>

    <template #footer>
      <div v-if="hasFooterSlot" class="form-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    eyebrow?: string;
    subtitle?: string;
    size?: string;
    destroyOnClose?: boolean;
  }>(),
  {
    eyebrow: '',
    subtitle: '',
    size: '42rem',
    destroyOnClose: false
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'close'): void;
}>();

const slots = useSlots()
const hasFooterSlot = computed(() => Boolean(slots.footer))

function handleClose() {
  emit('update:modelValue', false);
  emit('close');
}
</script>

<style scoped>
.standard-form-drawer :deep(.el-drawer) {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 36%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--accent) 7%, transparent), transparent 28%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.99), rgba(244, 248, 253, 0.99));
}

.standard-form-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 22px 26px 18px;
  border-bottom: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 248, 255, 0.88)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 30%);
}

.standard-form-drawer :deep(.el-drawer__body) {
  padding: 22px 26px 20px;
  background: transparent;
}

.standard-form-drawer :deep(.el-drawer__footer) {
  padding: 0;
  background: transparent;
}

.form-drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.form-drawer__heading {
  min-width: 0;
}

.form-drawer__eyebrow {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.form-drawer__heading h2 {
  margin: 0.35rem 0 0;
  color: var(--text-heading);
  font-size: 1.8rem;
  line-height: 1.2;
  font-weight: 700;
}

.form-drawer__subtitle {
  margin: 0.65rem 0 0;
  color: var(--text-caption);
  font-size: 14px;
  line-height: 1.5;
}

.form-drawer__body {
  display: grid;
  gap: 1rem;
}

.form-drawer__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 14px 26px 22px;
  border-top: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.92), rgba(246, 249, 255, 0.96));
}

.standard-form-drawer :deep(.el-form) {
  padding: 1.15rem 1.2rem;
  border: 1px solid var(--panel-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 14px 32px rgba(32, 55, 90, 0.05);
}

.standard-form-drawer :deep(.el-form.ops-drawer-form) {
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.standard-form-drawer :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.standard-form-drawer :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-weight: 600;
}

.standard-form-drawer :deep(.el-alert) {
  margin-bottom: 0.85rem;
}

@media (max-width: 900px) {
  .standard-form-drawer :deep(.el-drawer__header) {
    padding: 20px 20px 16px;
  }

  .standard-form-drawer :deep(.el-drawer__body) {
    padding: 18px 20px 18px;
  }

  .form-drawer__footer {
    padding: 12px 20px 20px;
  }
}
</style>
