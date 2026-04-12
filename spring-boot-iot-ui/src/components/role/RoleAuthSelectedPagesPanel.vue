<template>
  <PanelCard
    class="role-auth-card"
    title="步骤 2：已选页面"
    description="从已选页面里切换当前页面，再细化它的按钮权限。"
  >
    <div v-if="!items.length" class="role-auth-empty">
      <p>暂未选择页面。请先在步骤 1 勾选该角色可访问的页面。</p>
    </div>
    <div v-else class="role-auth-selected-pages">
      <article
        v-for="item in items"
        :key="item.id"
        class="role-auth-selected-pages__item"
        :class="{ 'role-auth-selected-pages__item--active': item.active }"
      >
        <button
          type="button"
          class="role-auth-selected-pages__main"
          @click="$emit('select', item.id)"
        >
          <div class="role-auth-selected-pages__head">
            <strong>{{ item.menuName }}</strong>
            <span v-if="item.active" class="role-auth-selected-pages__badge">当前页</span>
          </div>
          <p v-if="item.path" class="role-auth-selected-pages__path">{{ item.path }}</p>
          <p class="role-auth-selected-pages__summary">{{ item.buttonSummary }}</p>
        </button>
        <button
          type="button"
          class="role-auth-selected-pages__remove"
          @click="$emit('remove', item.id)"
        >
          移除
        </button>
      </article>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import PanelCard from "@/components/PanelCard.vue";

withDefaults(
  defineProps<{
    items?: Array<{
      id: number;
      menuName: string;
      path?: string;
      buttonSummary: string;
      active?: boolean;
    }>;
  }>(),
  {
    items: () => [],
  },
);

defineEmits<{
  (event: "select", pageId: number): void;
  (event: "remove", pageId: number): void;
}>();
</script>

<style scoped>
.role-auth-empty {
  min-height: 8rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-secondary);
}

.role-auth-selected-pages {
  display: grid;
  gap: 0.75rem;
}

.role-auth-selected-pages__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: stretch;
  padding: 0.75rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.92);
}

.role-auth-selected-pages__item--active {
  border-color: rgba(21, 101, 192, 0.34);
  box-shadow: inset 0 0 0 1px rgba(21, 101, 192, 0.16);
}

.role-auth-selected-pages__main {
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.role-auth-selected-pages__head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.role-auth-selected-pages__badge {
  display: inline-flex;
  align-items: center;
  min-height: 1.35rem;
  padding: 0 0.45rem;
  border-radius: 999px;
  background: rgba(21, 101, 192, 0.08);
  color: #1565c0;
  font-size: 0.75rem;
  font-weight: 600;
}

.role-auth-selected-pages__path,
.role-auth-selected-pages__summary {
  margin: 0.4rem 0 0;
  color: var(--text-secondary);
  font-size: 0.875rem;
  line-height: 1.5;
}

.role-auth-selected-pages__remove {
  align-self: center;
  border: none;
  background: transparent;
  color: #c62828;
  font-size: 0.875rem;
  cursor: pointer;
}
</style>
