<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <el-icon :size="64" color="#7284a5">
        <DataAnalysis />
      </el-icon>
    </div>
    <p class="empty-state__title">{{ title }}</p>
    <p class="empty-state__description">{{ description }}</p>
    <div v-if="action" class="empty-state__action">
      <el-button @click="action.callback">
        {{ action.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataAnalysis } from '@element-plus/icons-vue';

interface EmptyStateProps {
  title?: string;
  description?: string;
  action?: {
    label: string;
    callback: () => void;
  };
}

defineProps<EmptyStateProps>();
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
  color: var(--text-secondary);
}

.empty-state__icon {
  margin-bottom: 1rem;
}

.empty-state__title {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-primary);
}

.empty-state__description {
  margin: 0 0 1.5rem;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-secondary);
}

.empty-state__action {
  margin-top: 1rem;
}
</style>
