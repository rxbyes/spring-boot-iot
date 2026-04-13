<template>
  <PanelCard
    class="role-auth-card"
    title="当前节点详情"
    description="右侧只承接当前节点的直属子级或当前按钮信息，不再在同一区域堆叠整棵树。"
  >
    <div v-if="!currentNode" class="role-auth-detail__empty">
      <p>请先从左侧权限树选择一个节点。</p>
    </div>

    <div v-else class="role-auth-detail">
      <div class="role-auth-detail__summary">
        <div class="role-auth-detail__current">
          <strong>{{ currentNode.menuName }}</strong>
          <span class="role-auth-detail__type">{{ resolveTypeLabel(currentNode.type) }}</span>
        </div>
        <div class="role-auth-detail__meta">
          <span>当前状态：{{ currentNodeStatusLabel }}</span>
          <span v-if="currentNodeState">
            直属子级已授权 {{ currentNodeState.selectedChildCount }}/{{ currentNodeState.totalChildCount }}
          </span>
        </div>
      </div>

      <div v-if="isButtonNode" class="role-auth-detail__button-info">
        <div class="role-auth-detail__info-row">
          <span class="role-auth-detail__info-label">权限码</span>
          <span>{{ currentNode.menuCode || '--' }}</span>
        </div>
        <div class="role-auth-detail__info-row">
          <span class="role-auth-detail__info-label">所属页面</span>
          <span>{{ parentLabel || '--' }}</span>
        </div>
        <div class="role-auth-detail__info-row">
          <span class="role-auth-detail__info-label">说明</span>
          <span>{{ currentNode.meta?.menuHint || currentNode.meta?.description || '--' }}</span>
        </div>
      </div>

      <template v-else>
        <div class="role-auth-detail__toolbar">
          <el-input
            :model-value="keyword"
            clearable
            :placeholder="detailSearchPlaceholder"
            @update:model-value="handleKeywordChange"
          />
        </div>

        <div v-if="!items.length" class="role-auth-detail__empty role-auth-detail__empty--compact">
          <p>{{ emptyStateText }}</p>
        </div>

        <div v-else v-loading="loading" class="role-auth-detail__list">
          <article
            v-for="item in items"
            :key="item.id"
            class="role-auth-detail__item"
          >
            <div class="role-auth-detail__item-main">
              <el-checkbox
                :model-value="resolveCheckboxChecked(item)"
                :indeterminate="resolveCheckboxIndeterminate(item)"
                @click.stop
                @change="handleToggle(item.id, $event)"
              />
              <button
                type="button"
                class="role-auth-detail__focus"
                @click="$emit('focus-child', item.id)"
              >
                <div class="role-auth-detail__focus-head">
                  <strong>{{ item.menuName }}</strong>
                  <span class="role-auth-detail__type">{{ resolveTypeLabel(item.type) }}</span>
                  <span v-if="item.childCount > 0" class="role-auth-detail__count">
                    {{ item.childCount }} 项
                  </span>
                  <span
                    v-if="shouldShowPartialBadge(item)"
                    class="role-auth-detail__partial"
                  >
                    子级部分
                  </span>
                </div>
                <p v-if="item.menuCode || item.path" class="role-auth-detail__line">
                  {{ item.menuCode || item.path }}
                </p>
                <p v-if="item.description" class="role-auth-detail__line">
                  {{ item.description }}
                </p>
              </button>
            </div>
          </article>
        </div>
      </template>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/PanelCard.vue';
import type { IdType } from '@/types/api';
import type { MenuTreeNode } from '@/types/auth';
import type { MenuSelectionState, RoleAuthDetailItem } from '@/utils/menuAuth';

const props = withDefaults(
  defineProps<{
    currentNode?: MenuTreeNode | null;
    currentNodeState?: MenuSelectionState | null;
    parentLabel?: string;
    items?: RoleAuthDetailItem[];
    keyword?: string;
    loading?: boolean;
  }>(),
  {
    currentNode: null,
    currentNodeState: null,
    parentLabel: '',
    items: () => [],
    keyword: '',
    loading: false
  }
);

const emit = defineEmits<{
  (event: 'update:keyword', value: string): void;
  (event: 'toggle', menuId: IdType, checked: boolean): void;
  (event: 'focus-child', menuId: IdType): void;
}>();

const isButtonNode = computed(() => props.currentNode?.type === 2);
const detailSearchPlaceholder = computed(() => {
  return props.currentNode?.type === 1 ? '筛选当前页面按钮' : '筛选当前节点直属子级';
});
const emptyStateText = computed(() => {
  if (props.currentNode?.type === 1) {
    return '当前页面暂无按钮权限项。';
  }
  return '当前节点暂无直属子级。';
});
const currentNodeStatusLabel = computed(() => {
  if (!props.currentNodeState) {
    return '未授权';
  }
  if (props.currentNodeState.checked) {
    return '全量授权';
  }
  if (props.currentNodeState.indeterminate) {
    return props.currentNodeState.selfSelected ? '部分授权' : '子级部分授权';
  }
  if (props.currentNodeState.selfSelected) {
    return '已授权';
  }
  return '未授权';
});

type MenuSelectableState = Pick<MenuSelectionState, 'checked' | 'indeterminate' | 'selfSelected'> & {
  totalChildCount?: number;
  childCount?: number;
};

function resolveTypeLabel(type?: number) {
  if (type === 2) {
    return '按钮';
  }
  if (type === 1) {
    return '页面';
  }
  return '目录';
}

function handleKeywordChange(value: string | number) {
  emit('update:keyword', String(value ?? ''));
}

function resolveCheckboxChecked(state: MenuSelectableState): boolean {
  return state.selfSelected || state.checked;
}

function resolveCheckboxIndeterminate(state: MenuSelectableState): boolean {
  return state.indeterminate && !state.selfSelected;
}

function shouldShowPartialBadge(state: MenuSelectableState): boolean {
  return state.indeterminate && state.selfSelected && (state.childCount ?? state.totalChildCount ?? 0) > 0;
}

function handleToggle(menuId: IdType, value: string | number | boolean) {
  emit('toggle', menuId, Boolean(value));
}
</script>

<style scoped>
.role-auth-detail {
  display: grid;
  gap: 0.95rem;
}

.role-auth-detail__summary {
  display: grid;
  gap: 0.45rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: var(--surface-subtle);
}

.role-auth-detail__current,
.role-auth-detail__meta,
.role-auth-detail__focus-head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.role-auth-detail__meta,
.role-auth-detail__line,
.role-auth-detail__info-row {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.role-auth-detail__type,
.role-auth-detail__count,
.role-auth-detail__partial {
  display: inline-flex;
  align-items: center;
  min-height: 1.35rem;
  padding: 0 0.45rem;
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
}

.role-auth-detail__type {
  background: var(--info-bg);
  color: var(--info);
}

.role-auth-detail__count {
  background: var(--surface-subtle);
  color: var(--text-secondary);
}

.role-auth-detail__partial {
  background: var(--surface-subtle);
  color: var(--text-secondary);
}

.role-auth-detail__toolbar {
  display: block;
}

.role-auth-detail__list {
  display: grid;
  gap: 0.75rem;
  min-height: 0;
  overflow: auto;
  padding-right: 0.2rem;
}

.role-auth-detail__item {
  padding: 0.8rem 0.9rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.94);
}

.role-auth-detail__item-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.75rem;
  align-items: flex-start;
}

.role-auth-detail__focus {
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.role-auth-detail__line {
  margin: 0.35rem 0 0;
  line-height: 1.55;
}

.role-auth-detail__button-info {
  display: grid;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.94);
}

.role-auth-detail__info-row {
  display: grid;
  gap: 0.3rem;
}

.role-auth-detail__info-label {
  color: var(--text-tertiary);
  font-size: 0.75rem;
}

.role-auth-detail__empty {
  min-height: 12rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-secondary);
}

.role-auth-detail__empty--compact {
  min-height: 10rem;
}
</style>
