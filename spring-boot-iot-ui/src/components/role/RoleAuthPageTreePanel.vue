<template>
  <PanelCard
    class="role-auth-card"
    title="步骤 1：页面授权"
    description="菜单树仅展示目录与页面，先批量勾选角色可访问页面。"
  >
    <div class="role-auth-card__toolbar">
      <el-input
        :model-value="keyword"
        clearable
        placeholder="筛选页面名称 / 编码 / 路由"
        @update:model-value="handleKeywordChange"
      />
      <div class="role-auth-card__actions">
        <button type="button" class="role-auth-link" @click="$emit('refresh')">
          刷新页面树
        </button>
        <button
          type="button"
          class="role-auth-link"
          :disabled="selectablePageIds.length === 0"
          @click="handleCheckAll"
        >
          全选页面
        </button>
        <button
          type="button"
          class="role-auth-link"
          :disabled="checkedPageIds.length === 0"
          @click="$emit('clear')"
        >
          清空
        </button>
      </div>
    </div>

    <div v-loading="loading" class="role-auth-tree">
      <el-tree
        ref="treeRef"
        node-key="id"
        show-checkbox
        default-expand-all
        check-strictly
        :data="displayTreeData"
        :props="treeProps"
        :filter-node-method="filterNode"
        empty-text="暂无可授权页面"
        @check="handleCheck"
      >
        <template #default="{ data }">
          <div class="role-auth-node">
            <div class="role-auth-node__main">
              <span class="role-auth-node__name">{{ data.menuName }}</span>
              <span class="role-auth-node__type">
                {{ data.type === 0 ? "目录" : "页面" }}
              </span>
            </div>
            <div class="role-auth-node__meta">
              <span v-if="data.type === 0" class="role-auth-node__hint">目录节点自动补齐</span>
              <code v-if="data.path">{{ data.path }}</code>
            </div>
          </div>
        </template>
      </el-tree>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";

import PanelCard from "@/components/PanelCard.vue";
import type { MenuTreeNode } from "@/types/auth";

const props = withDefaults(
  defineProps<{
    treeData?: MenuTreeNode[];
    checkedPageIds?: number[];
    keyword?: string;
    loading?: boolean;
  }>(),
  {
    treeData: () => [],
    checkedPageIds: () => [],
    keyword: "",
    loading: false,
  },
);

const emit = defineEmits<{
  (event: "update:keyword", value: string): void;
  (event: "update:checkedPageIds", value: number[]): void;
  (event: "refresh"): void;
  (event: "clear"): void;
}>();

const treeRef = ref();
const treeProps = {
  label: "menuName",
  children: "children",
  disabled: "disabled",
};

const displayTreeData = computed(() =>
  props.treeData.map((node) => toDisplayNode(node)),
);

const selectablePageIds = computed(() => {
  const ids: number[] = [];
  const visit = (nodes: MenuTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.type === 1) {
        ids.push(node.id);
      }
      if (node.children?.length) {
        visit(node.children);
      }
    });
  };
  visit(props.treeData);
  return ids;
});

watch(
  () => props.keyword,
  (keyword) => {
    treeRef.value?.filter(keyword || "");
  },
);

watch(
  () => props.checkedPageIds,
  (checkedPageIds) => {
    nextTick(() => {
      treeRef.value?.setCheckedKeys(checkedPageIds || []);
    });
  },
  {
    immediate: true,
    deep: true,
  },
);

watch(
  () => props.treeData,
  () => {
    nextTick(() => {
      treeRef.value?.setCheckedKeys(props.checkedPageIds || []);
      treeRef.value?.filter(props.keyword || "");
    });
  },
  {
    deep: true,
  },
);

function handleKeywordChange(value: string | number) {
  emit("update:keyword", String(value ?? ""));
}

function handleCheck() {
  const checkedKeys = (treeRef.value?.getCheckedKeys(false) || []) as number[];
  emit(
    "update:checkedPageIds",
    checkedKeys.filter((menuId) => typeof menuId === "number"),
  );
}

function handleCheckAll() {
  emit("update:checkedPageIds", [...selectablePageIds.value]);
}

function toDisplayNode(node: MenuTreeNode): MenuTreeNode & { disabled: boolean } {
  return {
    ...node,
    disabled: node.type === 0,
    children: (node.children || []).map((child) => toDisplayNode(child)),
  };
}

function filterNode(keyword: string, data: MenuTreeNode) {
  if (!keyword) {
    return true;
  }
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }
  return [data.menuName, data.menuCode, data.path]
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(normalizedKeyword));
}
</script>

<style scoped>
.role-auth-card__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.role-auth-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.role-auth-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--el-color-primary);
  font-size: 0.875rem;
  cursor: pointer;
}

.role-auth-link:disabled {
  color: var(--text-tertiary);
  cursor: not-allowed;
}

.role-auth-tree {
  margin-top: 0.9rem;
  min-height: 18rem;
  max-height: 28rem;
  overflow: auto;
  padding-right: 0.25rem;
}

.role-auth-node {
  width: 100%;
  padding: 0.1rem 0;
}

.role-auth-node__main,
.role-auth-node__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  align-items: center;
}

.role-auth-node__meta {
  margin-top: 0.15rem;
  color: var(--text-secondary);
  font-size: 0.75rem;
}

.role-auth-node__name {
  color: var(--text-primary);
  font-weight: 600;
}

.role-auth-node__type {
  display: inline-flex;
  align-items: center;
  min-height: 1.4rem;
  padding: 0 0.45rem;
  border-radius: 999px;
  background: rgba(21, 101, 192, 0.08);
  color: #1565c0;
  font-size: 0.75rem;
}

.role-auth-node__hint,
.role-auth-node__meta code {
  color: var(--text-tertiary);
}
</style>
