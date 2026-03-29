<template>
  <div class="reporting-recent-diagnosis-list">
    <div class="reporting-recent-diagnosis-list__toolbar">
      <div class="reporting-recent-diagnosis-list__filters">
        <StandardChoiceGroup
          :model-value="modelValue"
          :options="filterOptions"
          responsive
          @update:model-value="handleFilterChange"
        />
      </div>

      <StandardActionGroup gap="sm">
        <StandardButton action="refresh" plain @click="$emit('refresh')">刷新最近记录</StandardButton>
      </StandardActionGroup>
    </div>

    <div v-if="loading && !items.length" class="reporting-recent-diagnosis-list__empty">
      正在同步最近记录...
    </div>
    <div v-else-if="!items.length" class="reporting-recent-diagnosis-list__empty">
      {{ emptyText }}
    </div>

    <div v-else class="reporting-recent-diagnosis-list__items">
      <article
        v-for="item in items"
        :key="item.key"
        class="reporting-recent-diagnosis-list__item"
        :data-active="item.sessionId === activeSessionId"
      >
        <div class="reporting-recent-diagnosis-list__item-top">
          <span class="reporting-recent-diagnosis-list__tag">{{ item.statusLabel }}</span>
          <strong>{{ item.summary }}</strong>
        </div>

        <div class="reporting-recent-diagnosis-list__meta">
          <span>Session {{ item.sessionId || '--' }}</span>
          <span>设备 {{ item.deviceCode || '--' }}</span>
          <span>{{ item.transportMode || '--' }}</span>
          <span>{{ item.topic || '--' }}</span>
          <span>{{ item.submittedAt || '--' }}</span>
        </div>

        <p class="reporting-recent-diagnosis-list__blocker">{{ item.blocker }}</p>

        <div class="reporting-recent-diagnosis-list__actions">
          <StandardButton
            action="refresh"
            plain
            @click="$emit('restore', item.key)"
          >
            {{ item.sessionId || '恢复当前复盘' }}
          </StandardButton>
          <StandardButton
            action="refresh"
            plain
            @click="$emit('action', { key: item.key, target: item.primaryAction.target })"
          >
            {{ item.primaryAction.label }}
          </StandardButton>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import StandardActionGroup from '@/components/StandardActionGroup.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardChoiceGroup from '@/components/StandardChoiceGroup.vue';
import type { ReportingRecentFilter, ReportingRecentDiagnosisItem, ReportingRecentActionTarget } from '@/views/reportingRecentDiagnosis';

const filterOptions: Array<{ label: string; value: ReportingRecentFilter }> = [
  { label: '全部', value: 'all' },
  { label: '失败', value: 'failed' },
  { label: '等待中', value: 'pending' },
  { label: '成功', value: 'validated' }
];

withDefaults(
  defineProps<{
    items: ReportingRecentDiagnosisItem[];
    modelValue?: ReportingRecentFilter;
    loading?: boolean;
    emptyText?: string;
    activeSessionId?: string;
  }>(),
  {
    modelValue: 'all',
    loading: false,
    emptyText: '最近还没有可恢复的 message-flow session。',
    activeSessionId: ''
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: ReportingRecentFilter): void;
  (event: 'refresh'): void;
  (event: 'restore', key: string): void;
  (event: 'action', payload: { key: string; target: ReportingRecentActionTarget }): void;
}>();

function handleFilterChange(value: string | number | boolean) {
  if (typeof value === 'string') {
    const filter = filterOptions.find((item) => item.value === value)?.value;
    if (filter) {
      emit('update:modelValue', filter);
    }
  }
}
</script>

<style scoped>
.reporting-recent-diagnosis-list {
  display: grid;
  gap: 0.9rem;
}

.reporting-recent-diagnosis-list__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  flex-wrap: wrap;
}

.reporting-recent-diagnosis-list__filters {
  display: inline-flex;
  min-width: 0;
}

.reporting-recent-diagnosis-list__empty {
  padding: 1.2rem 1rem;
  border-radius: calc(var(--radius-card) - 4px);
  background: rgba(15, 23, 42, 0.03);
  color: var(--text-secondary);
  text-align: center;
}

.reporting-recent-diagnosis-list__items {
  display: grid;
  gap: 0.72rem;
}

.reporting-recent-diagnosis-list__item {
  display: grid;
  gap: 0.62rem;
  padding: 0.92rem 1rem;
  border: 1px solid var(--line-panel);
  border-radius: calc(var(--radius-card) - 2px);
  background: rgba(255, 255, 255, 0.82);
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.reporting-recent-diagnosis-list__item:hover,
.reporting-recent-diagnosis-list__item[data-active='true'] {
  border-color: rgba(14, 116, 144, 0.22);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  transform: none;
}

.reporting-recent-diagnosis-list__filters :deep(.standard-choice-group) {
  gap: 0.42rem;
}

.reporting-recent-diagnosis-list__filters :deep(.standard-choice-group__item) {
  min-width: 4.4rem;
  min-height: 2rem;
  padding: 0 0.78rem;
  font-size: 0.82rem;
}

.reporting-recent-diagnosis-list__item-top {
  display: flex;
  align-items: center;
  gap: 0.62rem;
  flex-wrap: wrap;
}

.reporting-recent-diagnosis-list__item-top strong {
  color: var(--text-primary);
}

.reporting-recent-diagnosis-list__tag {
  display: inline-flex;
  align-items: center;
  min-height: 1.65rem;
  padding: 0 0.58rem;
  border-radius: 999px;
  background: rgba(14, 116, 144, 0.08);
  color: var(--brand);
  font-size: 0.76rem;
  font-weight: 700;
}

.reporting-recent-diagnosis-list__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.72rem 1rem;
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.reporting-recent-diagnosis-list__blocker {
  margin: 0;
  color: var(--text-primary);
  font-weight: 600;
}

.reporting-recent-diagnosis-list__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.62rem;
}
</style>
