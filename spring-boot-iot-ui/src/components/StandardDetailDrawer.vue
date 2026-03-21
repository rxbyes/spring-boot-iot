<template>
  <el-drawer
    :model-value="modelValue"
    :size="size"
    direction="rtl"
    :destroy-on-close="destroyOnClose"
    class="standard-detail-drawer"
    @close="emit('update:modelValue', false)"
  >
    <template #header>
      <div class="detail-drawer__header">
        <div class="detail-drawer__heading">
          <p v-if="eyebrow" class="detail-drawer__eyebrow">{{ eyebrow }}</p>
          <h2>{{ title }}</h2>
          <p v-if="subtitle" class="detail-drawer__subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="tags.length" class="detail-drawer__tags">
          <el-tag
            v-for="tag in tags"
            :key="`${tag.label}-${tag.type || 'info'}-${tag.effect || 'light'}`"
            :type="tag.type || 'info'"
            :effect="tag.effect || 'light'"
            round
          >
            {{ tag.label }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="detail-drawer__body">
      <div v-if="loading" class="detail-state">{{ loadingText }}</div>
      <div v-else-if="errorMessage" class="detail-state detail-state--error">{{ errorMessage }}</div>
      <div v-else-if="empty" class="detail-state">{{ emptyText }}</div>
      <slot v-else />
    </div>

    <template #footer>
      <div v-if="hasFooterSlot" class="detail-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    eyebrow?: string;
    subtitle?: string;
    size?: string;
    destroyOnClose?: boolean;
    loading?: boolean;
    loadingText?: string;
    errorMessage?: string;
    empty?: boolean;
    emptyText?: string;
    tags?: Array<{
      label: string;
      type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
      effect?: 'dark' | 'light' | 'plain';
    }>;
  }>(),
  {
    eyebrow: '',
    subtitle: '',
    size: '48rem',
    destroyOnClose: true,
    loading: false,
    loadingText: '正在加载详情...',
    errorMessage: '',
    empty: false,
    emptyText: '暂无详情数据',
    tags: () => []
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const slots = useSlots()
const hasFooterSlot = computed(() => Boolean(slots.footer))
</script>

<style scoped>
.standard-detail-drawer :deep(.el-drawer) {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 30%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--accent) 8%, transparent), transparent 22%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.99), rgba(244, 248, 253, 0.99));
  box-shadow: var(--shadow-drawer);
}

.standard-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 26px 28px 22px;
  border-bottom: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 28%);
}

.standard-detail-drawer :deep(.el-drawer__body) {
  padding: 24px 28px 28px;
  background: transparent;
}

.standard-detail-drawer :deep(.el-drawer__footer) {
  padding: 0;
  background: transparent;
}

.detail-drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.25rem;
}

.detail-drawer__heading {
  min-width: 0;
}

.detail-drawer__eyebrow {
  margin: 0;
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detail-drawer__heading h2 {
  margin: 0.45rem 0 0;
  color: var(--text-heading);
  font-size: clamp(1.65rem, 2.2vw, 2.05rem);
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.detail-drawer__subtitle {
  margin: 0.7rem 0 0;
  max-width: 40rem;
  color: var(--text-caption);
  font-size: 14px;
  line-height: 1.6;
}

.detail-drawer__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.detail-drawer__tags :deep(.el-tag) {
  min-height: 1.8rem;
  border-radius: var(--radius-pill);
  padding-inline: 0.7rem;
  border-color: color-mix(in srgb, var(--accent) 10%, transparent);
}

.detail-drawer__body {
  display: grid;
  gap: 1.1rem;
}

.detail-state {
  padding: 1.1rem 1.25rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.92));
  color: var(--text-caption);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.detail-state--error {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 22%, transparent);
  background: linear-gradient(180deg, rgba(255, 246, 246, 0.98), rgba(255, 241, 241, 0.96));
}

.detail-drawer__footer {
  padding: 0 28px 24px;
}

.standard-detail-drawer :deep(.detail-panel) {
  position: relative;
  overflow: hidden;
  padding: 1.25rem 1.35rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(246, 250, 255, 0.91));
  box-shadow:
    0 6px 18px rgba(32, 55, 90, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.standard-detail-drawer :deep(.detail-panel::before) {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--brand) 82%, white),
    color-mix(in srgb, var(--accent) 62%, white),
    color-mix(in srgb, var(--brand-bright) 58%, white)
  );
}

.standard-detail-drawer :deep(.detail-panel--hero) {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 34%),
    linear-gradient(180deg, rgba(242, 247, 255, 0.95), rgba(255, 255, 255, 0.94));
}

.standard-detail-drawer :deep(.detail-section-header) {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.standard-detail-drawer :deep(.detail-section-header h3) {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.standard-detail-drawer :deep(.detail-panel > h3) {
  margin: 0 0 1rem;
  color: var(--text-heading);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.standard-detail-drawer :deep(.detail-section-header p) {
  margin: 0.38rem 0 0;
  color: var(--text-caption-2);
  font-size: 13px;
  line-height: 1.6;
}

.standard-detail-drawer :deep(.detail-grid) {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.9rem;
}

.standard-detail-drawer :deep(.detail-summary-grid) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.9rem;
}

.standard-detail-drawer :deep(.detail-summary-card) {
  display: grid;
  gap: 0.38rem;
  min-width: 0;
  padding: 1rem 1.05rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.standard-detail-drawer :deep(.detail-summary-card__label) {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0.02em;
}

.standard-detail-drawer :deep(.detail-summary-card__value) {
  color: var(--text-heading);
  font-size: 1.05rem;
  line-height: 1.4;
  font-weight: 700;
  word-break: break-word;
}

.standard-detail-drawer :deep(.detail-summary-card__hint) {
  margin: 0;
  color: var(--text-caption-2);
  font-size: 12px;
  line-height: 1.55;
}

.standard-detail-drawer :deep(.detail-field) {
  min-width: 0;
  grid-column: span 6;
  display: flex;
  flex-direction: column;
  gap: 0.38rem;
  padding: 0.95rem 1rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.standard-detail-drawer :deep(.detail-field--full) {
  grid-column: 1 / -1;
}

.standard-detail-drawer :deep(.detail-field__label) {
  display: block;
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.standard-detail-drawer :deep(.detail-field__value) {
  display: block;
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.65;
  font-weight: 700;
  word-break: break-word;
}

.standard-detail-drawer :deep(.detail-field__value--plain) {
  font-weight: 600;
  white-space: pre-wrap;
}

.standard-detail-drawer :deep(.detail-field__value--pre) {
  margin-top: 0.1rem;
  padding: 1rem 1.05rem;
  border: 1px solid rgba(15, 23, 42, 0.16);
  border-radius: var(--radius-2xl);
  background: linear-gradient(180deg, #0f172a, #162033);
  color: #e7eefb;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.72;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 12px 26px rgba(15, 23, 42, 0.18);
}

.standard-detail-drawer :deep(.detail-card-list) {
  display: grid;
  gap: 0.9rem;
}

.standard-detail-drawer :deep(.detail-card) {
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.standard-detail-drawer :deep(.detail-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.standard-detail-drawer :deep(.detail-card__header strong) {
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
}

.standard-detail-drawer :deep(.detail-card__meta) {
  margin-top: 0.65rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.5;
}

.standard-detail-drawer :deep(.detail-card__meta span) {
  display: inline-flex;
  align-items: center;
  min-height: 1.7rem;
  padding: 0.26rem 0.62rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--accent) 10%, transparent);
  background: rgba(247, 250, 255, 0.96);
}

.standard-detail-drawer :deep(.detail-notice) {
  display: grid;
  gap: 0.4rem;
  padding: 1rem 1.05rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(247, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.standard-detail-drawer :deep(.detail-notice--danger) {
  border-color: color-mix(in srgb, var(--danger) 24%, transparent);
  background: linear-gradient(180deg, rgba(255, 245, 245, 0.98), rgba(255, 239, 239, 0.96));
}

.standard-detail-drawer :deep(.detail-notice__label) {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.standard-detail-drawer :deep(.detail-notice__value) {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.65;
  word-break: break-word;
}

.standard-detail-drawer :deep(.detail-grid + .detail-notice),
.standard-detail-drawer :deep(.detail-summary-grid + .detail-notice) {
  margin-top: 1rem;
}

.standard-detail-drawer :deep(.detail-empty) {
  padding: 0.95rem 1rem;
  border: 1px dashed color-mix(in srgb, var(--accent) 20%, transparent);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.92));
  color: var(--text-caption);
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .standard-detail-drawer :deep(.el-drawer__header) {
    padding: 20px 20px 16px;
  }

  .standard-detail-drawer :deep(.el-drawer__body) {
    padding: 18px 20px 20px;
  }

  .detail-drawer__header {
    flex-direction: column;
  }

  .detail-drawer__tags {
    justify-content: flex-start;
  }

  .standard-detail-drawer :deep(.detail-grid) {
    grid-template-columns: minmax(0, 1fr);
  }

  .standard-detail-drawer :deep(.detail-field) {
    grid-column: 1 / -1;
  }
}
</style>
