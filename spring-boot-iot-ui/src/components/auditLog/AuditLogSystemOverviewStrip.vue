<template>
  <section class="audit-log-system-overview-strip" aria-label="异常观测总览">
    <button
      v-for="item in items"
      :key="item.key"
      :data-testid="`system-log-overview-${item.key}`"
      type="button"
      class="audit-log-system-overview-strip__item"
      :class="{ 'is-active': resolveActiveState(item) }"
      @click="emit('change-tab', item.targetTab)"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </button>
  </section>
</template>

<script setup lang="ts">
interface AuditLogSystemOverviewItem {
  key: string;
  label: string;
  value: string;
  targetTab: string;
}

const props = defineProps<{
  activeTab: string;
  activeItemKey?: string;
  items: AuditLogSystemOverviewItem[];
}>();

const emit = defineEmits<{
  (event: 'change-tab', tabKey: string): void;
}>();

function resolveActiveState(item: AuditLogSystemOverviewItem) {
  return item.key === (props.activeItemKey || props.activeTab)
}
</script>
