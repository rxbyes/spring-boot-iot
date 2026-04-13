<template>
  <PanelCard
    class="role-auth-card"
    title="权限树"
    description="目录、页面、按钮统一授权；勾选父节点会默认覆盖全部后代。"
  >
    <div class="role-auth-tree-panel__toolbar">
      <el-input
        :model-value="keyword"
        clearable
        placeholder="筛选权限名称 / 编码 / 路由"
        @update:model-value="handleKeywordChange"
      />
      <button type="button" class="role-auth-link" @click="$emit('refresh')">
        刷新树
      </button>
    </div>

    <div
      v-loading="loading"
      class="role-auth-tree-panel__tree"
      element-loading-text="正在加载权限树"
    >
      <el-tree
        :key="treeRenderKey"
        node-key="id"
        highlight-current
        :data="treeData"
        :props="treeProps"
        :current-node-key="currentNodeId ?? undefined"
        :default-expanded-keys="expandedKeys"
        :expand-on-click-node="false"
        empty-text="暂无可授权权限"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        @node-collapse="handleNodeCollapse"
      >
        <template #default="{ data }">
          <div
            class="role-auth-tree-panel__node"
            :class="{ 'role-auth-tree-panel__node--active': data.id === currentNodeId }"
          >
            <el-checkbox
              :model-value="resolveCheckboxChecked(resolveSelectionState(data.id))"
              :indeterminate="resolveCheckboxIndeterminate(resolveSelectionState(data.id))"
              @click.stop
              @change="handleToggle(data.id, $event)"
            />
            <div class="role-auth-tree-panel__node-main">
              <span class="role-auth-tree-panel__node-name">{{ data.menuName }}</span>
              <span class="role-auth-tree-panel__node-type">
                {{ resolveTypeLabel(data.type) }}
              </span>
              <span v-if="(data.children || []).length > 0" class="role-auth-tree-panel__node-count">
                {{ (data.children || []).length }} 项
              </span>
              <span
                v-if="shouldShowPartialBadge(resolveSelectionState(data.id))"
                class="role-auth-tree-panel__node-partial"
              >
                子级部分
              </span>
            </div>
          </div>
        </template>
      </el-tree>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue';

import PanelCard from '@/components/PanelCard.vue';
import type { IdType } from '@/types/api';
import type { MenuTreeNode } from '@/types/auth';
import type { MenuSelectionState } from '@/utils/menuAuth';

const props = defineProps({
  treeData: {
    type: Array as PropType<MenuTreeNode[]>,
    default: () => []
  },
  currentNodeId: {
    type: [String, Number],
    default: null
  },
  expandedKeys: {
    type: Array as PropType<IdType[]>,
    default: () => []
  },
  selectionStateMap: {
    type: Object as PropType<Map<IdType, MenuSelectionState>>,
    default: () => new Map<IdType, MenuSelectionState>()
  },
  keyword: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits<{
  (event: 'update:keyword', value: string): void;
  (event: 'toggle', menuId: IdType, checked: boolean): void;
  (event: 'select-node', menuId: IdType): void;
  (event: 'expand', menuId: IdType): void;
  (event: 'collapse', menuId: IdType): void;
  (event: 'refresh'): void;
}>();

const treeProps = {
  label: 'menuName',
  children: 'children'
};

const treeRenderKey = computed(() => {
  return `${props.currentNodeId ?? 'none'}:${props.keyword}:${props.expandedKeys.join(',')}`;
});

function resolveTypeLabel(type?: number) {
  if (type === 2) {
    return '按钮';
  }
  if (type === 1) {
    return '页面';
  }
  return '目录';
}

function resolveSelectionState(menuId: IdType): MenuSelectionState {
  return (
    props.selectionStateMap.get(menuId) || {
      checked: false,
      indeterminate: false,
      selfSelected: false,
      selectedChildCount: 0,
      totalChildCount: 0
    }
  );
}

function resolveCheckboxChecked(state: MenuSelectionState): boolean {
  return state.selfSelected || state.checked;
}

function resolveCheckboxIndeterminate(state: MenuSelectionState): boolean {
  return state.indeterminate && !state.selfSelected;
}

function shouldShowPartialBadge(state: MenuSelectionState): boolean {
  return state.indeterminate && state.selfSelected && state.totalChildCount > 0;
}

function handleKeywordChange(value: string | number) {
  emit('update:keyword', String(value ?? ''));
}

function handleToggle(menuId: IdType, value: string | number | boolean) {
  emit('toggle', menuId, Boolean(value));
}

function handleNodeClick(data: MenuTreeNode) {
  emit('select-node', data.id);
}

function handleNodeExpand(data: MenuTreeNode) {
  emit('expand', data.id);
}

function handleNodeCollapse(data: MenuTreeNode) {
  emit('collapse', data.id);
}
</script>

<style scoped>
.role-auth-tree-panel__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.role-auth-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--brand);
  font-size: 0.875rem;
  cursor: pointer;
}

.role-auth-tree-panel__tree {
  min-height: 20rem;
  margin-top: 0.95rem;
  overflow: auto;
  padding-right: 0.25rem;
}

.role-auth-tree-panel__node {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 0.55rem;
  padding: 0.2rem 0;
}

.role-auth-tree-panel__node-main {
  display: flex;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  flex-wrap: nowrap;
  gap: 0.45rem;
  align-items: center;
}

.role-auth-tree-panel__node--active .role-auth-tree-panel__node-name {
  color: var(--brand);
}

.role-auth-tree-panel__node-name {
  color: var(--text-primary);
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-auth-tree-panel__node-type,
.role-auth-tree-panel__node-count {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  min-height: 1.35rem;
  padding: 0 0.45rem;
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
}

.role-auth-tree-panel__node-type {
  background: var(--info-bg);
  color: var(--info);
}

.role-auth-tree-panel__node-count {
  background: var(--surface-subtle);
  color: var(--text-secondary);
}

.role-auth-tree-panel__node-partial {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  min-height: 1.35rem;
  padding: 0 0.45rem;
  border-radius: var(--radius-pill);
  background: var(--surface-subtle);
  color: var(--text-secondary);
  font-size: 0.75rem;
}
</style>
