<template>
  <PanelCard
    class="role-auth-card"
    title="当前页面按钮权限"
    description="按钮权限只针对当前页面生效，新勾选页面默认不自动附带按钮。"
  >
    <div v-if="!activePage" class="role-auth-empty">
      <p>请先勾选页面，或从已选页面列表选择一个页面</p>
    </div>
    <div v-else-if="!buttonRows.length" class="role-auth-empty">
      <p>{{ activePage.menuName }} 暂无可单独控制的按钮权限。</p>
    </div>
    <div v-else class="role-auth-button-panel">
      <div class="role-auth-button-panel__toolbar">
        <div class="role-auth-button-panel__current">
          <strong>{{ activePage.menuName }}</strong>
          <span v-if="activePage.path">{{ activePage.path }}</span>
        </div>
        <el-input
          :model-value="keyword"
          clearable
          placeholder="筛选当前页面按钮"
          @update:model-value="handleKeywordChange"
        />
        <div class="role-auth-button-panel__actions">
          <button
            type="button"
            class="role-auth-link"
            :disabled="filteredButtonRows.length === 0"
            @click="handleCheckAll"
          >
            全选按钮
          </button>
          <button
            type="button"
            class="role-auth-link"
            :disabled="selectedButtonIds.length === 0"
            @click="$emit('update:selectedButtonIds', [])"
          >
            清空按钮
          </button>
        </div>
      </div>

      <div class="role-auth-button-panel__summary">
        当前页面共 {{ filteredButtonRows.length }} 个可配置按钮，已选 {{ selectedButtonIds.length }} 个。
      </div>

      <el-checkbox-group
        class="role-auth-button-panel__list"
        :model-value="selectedButtonIds"
        @update:model-value="handleSelectionChange"
      >
        <label
          v-for="button in filteredButtonRows"
          :key="button.id"
          class="role-auth-button-panel__item"
        >
          <el-checkbox :value="button.id">{{ button.menuName }}</el-checkbox>
          <span v-if="button.description" class="role-auth-button-panel__hint">
            {{ button.description }}
          </span>
        </label>
      </el-checkbox-group>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from "vue";

import PanelCard from "@/components/PanelCard.vue";

const props = withDefaults(
  defineProps<{
    activePage?: {
      id: number;
      menuName: string;
      path?: string;
    } | null;
    buttonRows?: Array<{
      id: number;
      menuName: string;
      menuCode?: string;
      description?: string;
    }>;
    selectedButtonIds?: number[];
    keyword?: string;
    loading?: boolean;
  }>(),
  {
    activePage: null,
    buttonRows: () => [],
    selectedButtonIds: () => [],
    keyword: "",
    loading: false,
  },
);

const emit = defineEmits<{
  (event: "update:keyword", value: string): void;
  (event: "update:selectedButtonIds", value: number[]): void;
}>();

const filteredButtonRows = computed(() => {
  const normalizedKeyword = props.keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return props.buttonRows;
  }
  return props.buttonRows.filter((button) =>
    [button.menuName, button.menuCode, button.description]
      .filter((item): item is string => Boolean(item))
      .some((item) => item.toLowerCase().includes(normalizedKeyword)),
  );
});

function handleKeywordChange(value: string | number) {
  emit("update:keyword", String(value ?? ""));
}

function handleCheckAll() {
  emit(
    "update:selectedButtonIds",
    filteredButtonRows.value.map((button) => button.id),
  );
}

function handleSelectionChange(value: Array<number | string>) {
  emit(
    "update:selectedButtonIds",
    value
      .map((item) => Number(item))
      .filter((item) => Number.isInteger(item)),
  );
}
</script>

<style scoped>
.role-auth-empty {
  min-height: 10rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-secondary);
}

.role-auth-button-panel {
  display: grid;
  gap: 0.9rem;
}

.role-auth-button-panel__toolbar {
  display: grid;
  gap: 0.75rem;
}

.role-auth-button-panel__current {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.role-auth-button-panel__current strong {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.role-auth-button-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.role-auth-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--brand);
  font-size: 0.875rem;
  cursor: pointer;
}

.role-auth-link:disabled {
  color: var(--text-tertiary);
  cursor: not-allowed;
}

.role-auth-button-panel__summary {
  color: var(--text-secondary);
  font-size: 0.875rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: var(--surface-subtle);
}

.role-auth-button-panel__list {
  display: grid;
  gap: 0.75rem;
  min-height: 0;
  overflow: auto;
  padding-right: 0.2rem;
}

.role-auth-button-panel__item {
  display: grid;
  gap: 0.25rem;
  padding: 0.75rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.94);
}

.role-auth-button-panel__hint {
  color: var(--text-tertiary);
  font-size: 0.8125rem;
  line-height: 1.5;
}
</style>
