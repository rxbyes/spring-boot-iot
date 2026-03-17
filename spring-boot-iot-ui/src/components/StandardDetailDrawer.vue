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

    <template v-if="$slots.footer" #footer>
      <div class="detail-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
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
</script>

<style scoped>
.standard-detail-drawer :deep(.el-drawer) {
  background:
    radial-gradient(circle at top right, rgba(232, 240, 255, 0.78), transparent 36%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(244, 248, 253, 0.98));
}

.standard-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 24px 28px 20px;
  border-bottom: 1px solid rgba(42, 63, 95, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(244, 248, 255, 0.86));
}

.standard-detail-drawer :deep(.el-drawer__body) {
  padding: 22px 28px 28px;
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
  gap: 1rem;
}

.detail-drawer__heading {
  min-width: 0;
}

.detail-drawer__eyebrow {
  margin: 0;
  color: #70809a;
  font-size: 13px;
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.detail-drawer__heading h2 {
  margin: 0.35rem 0 0;
  color: #243448;
  font-size: 2rem;
  line-height: 1.2;
  font-weight: 700;
}

.detail-drawer__subtitle {
  margin: 0.65rem 0 0;
  color: #70809a;
  font-size: 14px;
  line-height: 1.5;
}

.detail-drawer__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.detail-drawer__body {
  display: grid;
  gap: 1rem;
}

.detail-state {
  padding: 1rem 1.2rem;
  border: 1px solid rgba(42, 63, 95, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  color: #70809a;
}

.detail-state--error {
  color: #d94848;
  border-color: rgba(217, 72, 72, 0.18);
  background: rgba(255, 246, 246, 0.96);
}

.detail-drawer__footer {
  padding: 0 28px 24px;
}

.standard-detail-drawer :deep(.detail-panel) {
  padding: 1.25rem 1.35rem;
  border: 1px solid rgba(42, 63, 95, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 14px 32px rgba(32, 55, 90, 0.06);
}

.standard-detail-drawer :deep(.detail-panel > h3) {
  margin: 0 0 1rem;
  color: #243448;
  font-size: 15px;
  font-weight: 700;
}

.standard-detail-drawer :deep(.detail-grid) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.5rem;
}

.standard-detail-drawer :deep(.detail-field) {
  min-width: 0;
}

.standard-detail-drawer :deep(.detail-field--full) {
  grid-column: 1 / -1;
}

.standard-detail-drawer :deep(.detail-field__label) {
  display: block;
  margin-bottom: 0.35rem;
  color: #70809a;
  font-size: 13px;
  line-height: 1.4;
}

.standard-detail-drawer :deep(.detail-field__value) {
  display: block;
  color: #243448;
  font-size: 16px;
  line-height: 1.6;
  font-weight: 600;
  word-break: break-word;
}

.standard-detail-drawer :deep(.detail-field__value--plain) {
  font-weight: 500;
}

.standard-detail-drawer :deep(.detail-field__value--pre) {
  padding: 0.85rem 1rem;
  border: 1px solid rgba(42, 63, 95, 0.08);
  border-radius: 14px;
  background: #f7f9fc;
  color: #243448;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.standard-detail-drawer :deep(.detail-card-list) {
  display: grid;
  gap: 0.85rem;
}

.standard-detail-drawer :deep(.detail-card) {
  padding: 1rem 1.1rem;
  border: 1px solid rgba(42, 63, 95, 0.08);
  border-radius: 16px;
  background: rgba(252, 253, 255, 0.95);
}

.standard-detail-drawer :deep(.detail-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.standard-detail-drawer :deep(.detail-card__header strong) {
  color: #243448;
  font-size: 15px;
  line-height: 1.5;
}

.standard-detail-drawer :deep(.detail-card__meta) {
  margin-top: 0.65rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  color: #5f6f88;
  font-size: 13px;
  line-height: 1.5;
}

.standard-detail-drawer :deep(.detail-empty) {
  color: #70809a;
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
}
</style>
