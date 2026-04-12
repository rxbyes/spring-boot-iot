<template>
  <div class="role-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="角色权限"
      description="在角色页统一维护基础信息与目录、页面、按钮全层级权限，导航结构以导航编排页维护结果为准。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton
          v-permission="'system:role:add'"
          action="add"
          @click="handleAdd"
          :icon="Plus"
          >新增角色</StandardButton
        >
      </template>

      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="searchForm.roleName"
                placeholder="角色名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.roleCode"
                placeholder="角色编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="searchForm.status"
                placeholder="状态"
                clearable
              >
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="reset" @click="handleReset"
              >重置</StandardButton
            >
            <StandardButton action="query" @click="handleSearch"
              >查询</StandardButton
            >
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`]"
        >
          <template #right>
            <StandardButton
              action="refresh"
              link
              @click="openExportColumnSetting"
              >导出列设置</StandardButton
            >
            <StandardButton
              action="batch"
              link
              :disabled="selectedRows.length === 0"
              @click="handleExportSelected"
              >导出选中</StandardButton
            >
            <StandardButton
              action="refresh"
              link
              :disabled="tableData.length === 0"
              @click="handleExportCurrent"
              >导出当前结果</StandardButton
            >
            <StandardButton
              action="reset"
              link
              :disabled="selectedRows.length === 0"
              @click="clearSelection"
              >清空选中</StandardButton
            >
            <StandardButton action="refresh" link @click="handleRefresh"
              >刷新列表</StandardButton
            >
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新角色列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`role-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`role-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="tableData"
            border
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn
              prop="roleName"
              label="角色名称"
              :width="160"
            />
            <StandardTableTextColumn
              prop="roleCode"
              label="角色编码"
              :width="170"
            />
            <StandardTableTextColumn
              prop="description"
              label="角色描述"
              :min-width="220"
            />
            <StandardTableTextColumn prop="dataScopeType" label="数据范围" :width="150">
              <template #default="{ row }">
                {{ resolveDataScopeLabel(row.dataScopeType) }}
              </template>
            </StandardTableTextColumn>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? "启用" : "禁用" }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn
              prop="createTime"
              label="创建时间"
              :width="180"
            />
            <StandardTableTextColumn
              prop="updateTime"
              label="更新时间"
              :width="180"
            />
            <el-table-column
              label="操作"
              :width="roleActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRoleRowActions()"
                  @command="(command) => handleRoleRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="refresh" @click="handleRefresh">刷新列表</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            class="pagination"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="dialogVisible"
      :title="dialogTitle"
      subtitle="通过右侧抽屉维护角色基础信息，并同步配置目录、页面、按钮权限。"
      size="68rem"
      @close="handleDialogClose"
    >
      <div class="role-form-layout">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
          class="role-form-layout__basic"
        >
          <el-form-item label="角色名称" prop="roleName">
            <el-input
              v-model="formData.roleName"
              placeholder="请输入角色名称"
            />
          </el-form-item>
          <el-form-item label="角色编码" prop="roleCode">
            <el-input
              v-model="formData.roleCode"
              placeholder="请输入角色编码"
            />
          </el-form-item>
          <el-form-item label="角色描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="请输入角色描述"
            />
          </el-form-item>
          <el-form-item label="数据范围" prop="dataScopeType">
            <el-select
              v-model="formData.dataScopeType"
              placeholder="请选择数据范围"
            >
              <el-option
                v-for="item in dataScopeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <section class="role-form-layout__auth">
          <div class="role-auth-panel">
            <div class="role-auth-summary-grid">
              <article class="role-auth-summary-grid__item">
                <span class="role-auth-summary-grid__label">目录</span>
                <strong>{{ grantedDirectoryCount }}</strong>
                <p>已授权目录节点</p>
              </article>
              <article class="role-auth-summary-grid__item">
                <span class="role-auth-summary-grid__label">页面</span>
                <strong>{{ grantedPageCount }}</strong>
                <p>已授权页面节点</p>
              </article>
              <article class="role-auth-summary-grid__item">
                <span class="role-auth-summary-grid__label">按钮</span>
                <strong>{{ grantedButtonCount }}</strong>
                <p>已授权按钮节点</p>
              </article>
              <article class="role-auth-summary-grid__item">
                <span class="role-auth-summary-grid__label">当前节点</span>
                <strong>{{ currentNode ? currentNode.menuName : "未定位" }}</strong>
                <p>
                  {{ currentNodeStatusLabel }}
                  <template v-if="currentNodeState">
                    ，直属子级 {{ currentNodeState.selectedChildCount }}/{{
                      currentNodeState.totalChildCount
                    }}
                  </template>
                </p>
              </article>
            </div>
            <div class="role-auth-note">
              左侧统一处理目录、页面和按钮授权；右侧只平铺当前节点直属子级。保存仍沿用既有
              menuIds 合同，共 {{ checkedMenuCount }} 项授权。
            </div>
            <div class="role-auth-workspace">
              <section aria-label="左侧权限树">
                <RoleAuthPermissionTreePanel
                  :tree-data="displayTreeData"
                  :current-node-id="currentNodeId"
                  :expanded-keys="treePanelExpandedKeys"
                  :selection-state-map="menuSelectionStateMap"
                  :keyword="treeKeyword"
                  :loading="menuTreeLoading"
                  @update:keyword="treeKeyword = $event"
                  @toggle="handleToggleMenu"
                  @select-node="handleSelectCurrentNode"
                  @expand="handleExpandNode"
                  @collapse="handleCollapseNode"
                  @refresh="refreshMenuTree"
                />
              </section>
              <section aria-label="当前节点详情">
                <RoleAuthNodeDetailPanel
                  :current-node="currentNode"
                  :current-node-state="currentNodeState"
                  :parent-label="currentNodeParentLabel"
                  :items="currentNodeDetailItems"
                  :keyword="detailKeyword"
                  :loading="menuTreeLoading"
                  @update:keyword="detailKeyword = $event"
                  @toggle="handleToggleMenu"
                  @focus-child="handleSelectCurrentNode"
                />
              </section>
            </div>
          </div>
        </section>
      </div>

      <template #footer>
        <StandardDrawerFooter @cancel="dialogVisible = false">
          <StandardButton
            action="cancel"
            class="standard-drawer-footer__button standard-drawer-footer__button--ghost"
            @click="dialogVisible = false"
          >
            取消
          </StandardButton>
          <StandardButton
            v-permission="
              formData.id ? 'system:role:update' : 'system:role:add'
            "
            action="confirm"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            @click="handleSubmit"
            :loading="submitLoading"
          >
            确定
          </StandardButton>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="角色权限导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import CsvColumnSettingDialog from "@/components/CsvColumnSettingDialog.vue";
import EmptyState from "@/components/EmptyState.vue";
import RoleAuthNodeDetailPanel from "@/components/role/RoleAuthNodeDetailPanel.vue";
import RoleAuthPermissionTreePanel from "@/components/role/RoleAuthPermissionTreePanel.vue";
import StandardAppliedFiltersBar from "@/components/StandardAppliedFiltersBar.vue";
import StandardDrawerFooter from "@/components/StandardDrawerFooter.vue";
import StandardFormDrawer from "@/components/StandardFormDrawer.vue";
import StandardListFilterHeader from "@/components/StandardListFilterHeader.vue";
import StandardPagination from "@/components/StandardPagination.vue";
import StandardTableTextColumn from "@/components/StandardTableTextColumn.vue";
import StandardTableToolbar from "@/components/StandardTableToolbar.vue";
import StandardWorkbenchPanel from "@/components/StandardWorkbenchPanel.vue";
import StandardWorkbenchRowActions from "@/components/StandardWorkbenchRowActions.vue";
import { useListAppliedFilters } from "@/composables/useListAppliedFilters";
import { useServerPagination } from "@/composables/useServerPagination";
import { listMenuTree } from "@/api/menu";
import { usePermissionStore } from "@/stores/permission";
import {
  addRole,
  deleteRole,
  getRole,
  pageRoles,
  updateRole,
  type Role,
} from "@/api/role";
import type { MenuTreeNode } from "@/types/auth";
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions,
} from "@/utils/csvColumns";
import { downloadRowsAsCsv, type CsvColumn } from "@/utils/csv";
import { confirmDelete, isConfirmCancelled } from "@/utils/confirm";
import {
  buildMenuNodeMap,
  buildMenuSelectionStateMap,
  filterPermissionTreeByKeyword,
  resolveGrantedMenuIds,
  resolveNodeAncestorIds,
  resolveNodeDetailItems,
  toggleMenuGrant,
} from "@/utils/menuAuth";
import { resolveWorkbenchActionColumnWidth } from "@/utils/adaptiveActionColumn";

interface SearchFormState {
  roleName: string;
  roleCode: string;
  status: number | undefined;
}

interface RoleFormData {
  id: number | undefined;
  roleName: string;
  roleCode: string;
  description: string;
  dataScopeType: string;
  status: number;
  menuIds: number[];
}

type RoleRowActionCommand = "edit" | "delete";

const formRef = ref();
const tableRef = ref();
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } =
  useServerPagination();
let latestListRequestId = 0;

const searchForm = reactive<SearchFormState>({
  roleName: "",
  roleCode: "",
  status: undefined,
});
const appliedFilters = reactive<SearchFormState>({
  roleName: "",
  roleCode: "",
  status: undefined,
});

const tableData = ref<Role[]>([]);
const selectedRows = ref<Role[]>([]);
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref("新增角色");

const exportColumns: CsvColumn<Role>[] = [
  { key: "roleName", label: "角色名称" },
  { key: "roleCode", label: "角色编码" },
  { key: "description", label: "角色描述" },
  {
    key: "dataScopeType",
    label: "数据范围",
    formatter: (value) => resolveDataScopeLabel(String(value || "")),
  },
  {
    key: "status",
    label: "状态",
    formatter: (value) => (Number(value) === 1 ? "启用" : "禁用"),
  },
  { key: "createTime", label: "创建时间" },
  { key: "updateTime", label: "更新时间" },
];
const exportColumnStorageKey = "role-view";
const exportColumnOptions = toCsvColumnOptions(exportColumns);
const exportPresets = [
  {
    label: "默认模板",
    keys: exportColumns.map((column) => String(column.key)),
  },
  { label: "运维模板", keys: ["roleName", "roleCode", "dataScopeType", "status", "updateTime"] },
  {
    label: "管理模板",
    keys: ["roleName", "roleCode", "description", "dataScopeType", "status", "createTime"],
  },
];
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key)),
  ),
);
const exportColumnDialogVisible = ref(false);
const permissionStore = usePermissionStore();
const roleActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: "edit", label: "编辑/授权" },
    { command: "delete", label: "删除" },
  ],
});

const menuTreeLoading = ref(false);
const rawMenuTree = ref<MenuTreeNode[]>([]);
const grantedMenuIds = ref<number[]>([]);
const currentNodeId = ref<number | null>(null);
const treeKeyword = ref("");
const detailKeyword = ref("");
const expandedNodeKeys = ref<number[]>([]);
const dataScopeOptions = [
  { label: "全局", value: "ALL" },
  { label: "租户内全部", value: "TENANT" },
  { label: "本机构及下级", value: "ORG_AND_CHILDREN" },
  { label: "仅本机构", value: "ORG" },
  { label: "仅本人", value: "SELF" },
];

const formData = ref<RoleFormData>(createEmptyRoleForm());
const menuNodeMap = computed(() => buildMenuNodeMap(rawMenuTree.value));
const menuSelectionStateMap = computed(() =>
  buildMenuSelectionStateMap(rawMenuTree.value, grantedMenuIds.value),
);
const displayTreeData = computed(() =>
  filterPermissionTreeByKeyword(rawMenuTree.value, treeKeyword.value),
);
const treePanelExpandedKeys = computed(() => {
  if (treeKeyword.value.trim()) {
    const expandedKeySet = new Set<number>();
    const visitVisibleNodes = (nodes: MenuTreeNode[]) => {
      nodes.forEach((node) => {
        if (node.children?.length) {
          expandedKeySet.add(node.id);
          visitVisibleNodes(node.children);
        }
      });
    };
    visitVisibleNodes(displayTreeData.value);
    return Array.from(expandedKeySet);
  }

  const expandedKeySet = new Set(expandedNodeKeys.value);
  resolveNodeAncestorIds(rawMenuTree.value, currentNodeId.value).forEach((menuId) => {
    expandedKeySet.add(menuId);
  });
  return Array.from(expandedKeySet);
});
const checkedMenuCount = computed(() => grantedMenuIds.value.length);
const grantedDirectoryCount = computed(() =>
  grantedMenuIds.value.reduce((count, menuId) => {
    return menuNodeMap.value.get(menuId)?.type === 0 ? count + 1 : count;
  }, 0),
);
const grantedPageCount = computed(() =>
  grantedMenuIds.value.reduce((count, menuId) => {
    return menuNodeMap.value.get(menuId)?.type === 1 ? count + 1 : count;
  }, 0),
);
const grantedButtonCount = computed(() =>
  grantedMenuIds.value.reduce((count, menuId) => {
    return menuNodeMap.value.get(menuId)?.type === 2 ? count + 1 : count;
  }, 0),
);
const currentNode = computed(() => {
  if (currentNodeId.value === null) {
    return null;
  }
  return menuNodeMap.value.get(currentNodeId.value) || null;
});
const currentNodeState = computed(() => {
  if (currentNodeId.value === null) {
    return null;
  }
  return menuSelectionStateMap.value.get(currentNodeId.value) || null;
});
const currentNodeParentLabel = computed(() => {
  if (!currentNode.value || typeof currentNode.value.parentId !== "number") {
    return "";
  }
  return menuNodeMap.value.get(currentNode.value.parentId)?.menuName || "";
});
const currentNodeDetailItems = computed(() =>
  resolveNodeDetailItems(
    rawMenuTree.value,
    currentNodeId.value,
    detailKeyword.value,
    menuSelectionStateMap.value,
  ),
);
const currentNodeStatusLabel = computed(() => {
  if (!currentNodeState.value) {
    return "未授权";
  }
  if (currentNodeState.value.checked) {
    return "全量授权";
  }
  if (currentNodeState.value.indeterminate) {
    return currentNodeState.value.selfSelected ? "部分授权" : "子级部分授权";
  }
  if (currentNodeState.value.selfSelected) {
    return "已授权";
  }
  return "未授权";
});
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter,
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: "roleName", label: "角色名称" },
    { key: "roleCode", label: "角色编码" },
    {
      key: "status",
      label: (value) => `状态：${Number(value) === 1 ? "启用" : "禁用"}`,
      clearValue: undefined,
      isActive: (value) => value !== undefined,
    },
  ],
  defaults: {
    roleName: "",
    roleCode: "",
    status: undefined,
  },
});
const hasRecords = computed(() => tableData.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() =>
  hasAppliedFilters.value ? "没有符合条件的角色记录" : "当前还没有角色数据",
);
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? "已生效筛选暂时没有匹配结果，可以调整筛选条件，或者直接清空当前筛选。"
    : "当前还没有可展示的角色记录，建议稍后刷新，或先新增角色。",
);

const formRules = {
  roleName: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  roleCode: [{ required: true, message: "请输入角色编码", trigger: "blur" }],
  dataScopeType: [{ required: true, message: "请选择数据范围", trigger: "change" }],
};

function createEmptyRoleForm(): RoleFormData {
  return {
    id: undefined,
    roleName: "",
    roleCode: "",
    description: "",
    dataScopeType: "TENANT",
    status: 1,
    menuIds: [],
  };
}

function syncGrantedMenuIdsToForm() {
  formData.value.menuIds = [...grantedMenuIds.value];
}

function ensureExpandedNode(menuId: number | null) {
  if (menuId === null) {
    return;
  }
  const ancestorIds = resolveNodeAncestorIds(rawMenuTree.value, menuId);
  expandedNodeKeys.value = Array.from(
    new Set([...expandedNodeKeys.value, ...ancestorIds]),
  );
}

function resolveDefaultCurrentNodeId(preferredNodeId: number | null = null) {
  if (
    preferredNodeId !== null &&
    menuNodeMap.value.has(preferredNodeId)
  ) {
    return preferredNodeId;
  }
  if (
    currentNodeId.value !== null &&
    menuNodeMap.value.has(currentNodeId.value)
  ) {
    return currentNodeId.value;
  }
  return grantedMenuIds.value[0] ?? rawMenuTree.value[0]?.id ?? null;
}

function applyRoleGrantedMenuIds(
  menuIds: Array<number | undefined | null>,
  preferredNodeId: number | null = null,
) {
  grantedMenuIds.value = resolveGrantedMenuIds(rawMenuTree.value, menuIds);
  syncGrantedMenuIdsToForm();
  currentNodeId.value = resolveDefaultCurrentNodeId(preferredNodeId);
  ensureExpandedNode(currentNodeId.value);
}

function handleToggleMenu(menuId: number, checked: boolean) {
  grantedMenuIds.value = toggleMenuGrant(
    rawMenuTree.value,
    grantedMenuIds.value,
    menuId,
    checked,
  );
  syncGrantedMenuIdsToForm();
  if (currentNodeId.value === null || !menuNodeMap.value.has(currentNodeId.value)) {
    currentNodeId.value = resolveDefaultCurrentNodeId(menuId);
  }
  ensureExpandedNode(menuId);
}

function handleSelectCurrentNode(menuId: number) {
  if (!menuNodeMap.value.has(menuId)) {
    return;
  }
  currentNodeId.value = menuId;
  detailKeyword.value = "";
  ensureExpandedNode(menuId);
}

function handleExpandNode(menuId: number) {
  if (expandedNodeKeys.value.includes(menuId)) {
    return;
  }
  expandedNodeKeys.value = [...expandedNodeKeys.value, menuId];
}

function handleCollapseNode(menuId: number) {
  expandedNodeKeys.value = expandedNodeKeys.value.filter(
    (expandedMenuId) => expandedMenuId !== menuId,
  );
}

async function getRoles() {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const res = await pageRoles({
      roleName: appliedFilters.roleName || undefined,
      roleCode: appliedFilters.roleCode || undefined,
      status:
        typeof appliedFilters.status === "number"
          ? appliedFilters.status
          : undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    });
    if (requestId !== latestListRequestId) {
      return;
    }
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data);
      return;
    }
    tableData.value = [];
    resetTotal();
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    tableData.value = [];
    resetTotal();
    console.error("获取角色列表失败", error);
    ElMessage.error((error as Error).message || "获取角色列表失败");
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
  }
}

async function loadMenuAuthTree() {
  menuTreeLoading.value = true;
  try {
    const res = await listMenuTree();
    if (res.code === 200) {
      rawMenuTree.value = res.data || [];
      return true;
    }
    rawMenuTree.value = [];
    return false;
  } catch (error) {
    console.error("获取菜单树失败", error);
    ElMessage.error((error as Error).message || "获取菜单树失败");
    return false;
  } finally {
    menuTreeLoading.value = false;
  }
}

async function openRoleDialog(title: string, role: RoleFormData) {
  dialogTitle.value = title;
  formData.value = {
    ...role,
    menuIds: [],
  };
  treeKeyword.value = "";
  detailKeyword.value = "";
  currentNodeId.value = null;
  grantedMenuIds.value = [];
  expandedNodeKeys.value = [];
  dialogVisible.value = true;
  await loadMenuAuthTree();
  applyRoleGrantedMenuIds(role.menuIds);
}

function handleSearch() {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  getRoles();
}

function handleReset() {
  searchForm.roleName = "";
  searchForm.roleCode = "";
  searchForm.status = undefined;
  syncAppliedFilters();
  resetPage();
  clearSelection();
  getRoles();
}

function handleSelectionChange(rows: Role[]) {
  selectedRows.value = rows;
}

function clearSelection() {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
}

function handleRefresh() {
  clearSelection();
  getRoles();
}

function getRoleRowActions() {
  const actions: Array<{ command: RoleRowActionCommand; label: string }> = [];
  if (permissionStore.hasPermission("system:role:update")) {
    actions.push({ command: "edit", label: "编辑/授权" });
  }
  if (permissionStore.hasPermission("system:role:delete")) {
    actions.push({ command: "delete", label: "删除" });
  }
  return actions;
}

function handleRoleRowAction(command: RoleRowActionCommand, row: Role) {
  if (command === "edit") {
    void handleEdit(row);
    return;
  }
  void handleDelete(row);
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  getRoles();
}

function handleClearAppliedFilters() {
  handleReset();
}

function openExportColumnSetting() {
  exportColumnDialogVisible.value = true;
}

function handleExportColumnConfirm(selectedKeys: string[]) {
  selectedExportColumnKeys.value = selectedKeys;
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys);
}

function getResolvedExportColumns() {
  return resolveCsvColumns(exportColumns, selectedExportColumnKeys.value);
}

function handleExportSelected() {
  downloadRowsAsCsv(
    "角色权限-选中项.csv",
    selectedRows.value,
    getResolvedExportColumns(),
  );
}

function handleExportCurrent() {
  downloadRowsAsCsv(
    "角色权限-当前结果.csv",
    tableData.value,
    getResolvedExportColumns(),
  );
}

async function handleAdd() {
  await openRoleDialog("新增角色", createEmptyRoleForm());
}

async function handleEdit(row: Role) {
  try {
    const res = await getRole(row.id as number);
    if (res.code === 200 && res.data) {
      await openRoleDialog("编辑角色 / 菜单授权", {
        id: Number(res.data.id),
        roleName: res.data.roleName,
        roleCode: res.data.roleCode,
        description: res.data.description || "",
        dataScopeType: res.data.dataScopeType || "TENANT",
        status: Number(res.data.status ?? 1),
        menuIds: Array.isArray(res.data.menuIds) ? res.data.menuIds : [],
      });
    }
  } catch (error) {
    console.error("获取角色详情失败", error);
    ElMessage.error((error as Error).message || "获取角色详情失败");
  }
}

async function handleDelete(row: Role) {
  try {
    await confirmDelete("角色", row.roleName);
    const res = await deleteRole(row.id as number);
    if (res.code === 200) {
      ElMessage.success("删除成功");
      getRoles();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error("删除失败", error);
    ElMessage.error((error as Error).message || "删除失败");
  }
}

async function refreshMenuTree() {
  const currentGrantedMenuIds = [...grantedMenuIds.value];
  const success = await loadMenuAuthTree();
  if (!success) {
    return;
  }
  applyRoleGrantedMenuIds(currentGrantedMenuIds, currentNodeId.value);
  ElMessage.success("菜单树已刷新");
}

function resolveDataScopeLabel(dataScopeType?: string) {
  return (
    dataScopeOptions.find((item) => item.value === dataScopeType)?.label ||
    "租户内全部"
  );
}

async function handleSubmit() {
  if (!formRef.value) {
    return;
  }

  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  formData.value.menuIds = [...grantedMenuIds.value];
  submitLoading.value = true;
  try {
    const payload = {
      id: formData.value.id,
      roleName: formData.value.roleName,
      roleCode: formData.value.roleCode,
      description: formData.value.description,
      dataScopeType: formData.value.dataScopeType,
      status: formData.value.status,
      menuIds: [...grantedMenuIds.value],
    };
    const res = payload.id ? await updateRole(payload) : await addRole(payload);
    if (res.code === 200) {
      ElMessage.success(
        payload.id ? "更新成功，相关用户重新登录后将刷新菜单权限" : "新增成功",
      );
      dialogVisible.value = false;
      getRoles();
    }
  } catch (error) {
    console.error("提交失败", error);
    ElMessage.error((error as Error).message || "提交失败");
  } finally {
    submitLoading.value = false;
  }
}

function handleDialogClose() {
  formRef.value?.resetFields();
  treeKeyword.value = "";
  detailKeyword.value = "";
  currentNodeId.value = null;
  grantedMenuIds.value = [];
  expandedNodeKeys.value = [];
  formData.value = createEmptyRoleForm();
}

function handleSizeChange(size: number) {
  setPageSize(size);
  getRoles();
}

function handlePageChange(page: number) {
  setPageNum(page);
  getRoles();
}

onMounted(async () => {
  await Promise.all([getRoles(), loadMenuAuthTree()]);
});
</script>

<style scoped>
.role-form-layout {
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 20px;
}

.role-form-layout__basic,
.role-form-layout__auth {
  min-width: 0;
}

.role-auth-panel {
  display: grid;
  gap: 12px;
}

.role-auth-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.role-auth-summary-grid__item {
  display: grid;
  gap: 0.35rem;
  padding: 0.9rem 0.95rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.96);
}

.role-auth-summary-grid__label {
  color: var(--text-tertiary);
  font-size: 0.75rem;
}

.role-auth-summary-grid__item strong {
  color: var(--text-primary);
  font-size: 1.02rem;
}

.role-auth-summary-grid__item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.825rem;
  line-height: 1.5;
}

.role-auth-note {
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: var(--surface-subtle);
  color: var(--text-secondary);
  font-size: 0.875rem;
  line-height: 1.6;
}

.role-auth-workspace {
  display: grid;
  grid-template-columns: minmax(18rem, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  align-items: start;
}

.role-auth-workspace > section {
  min-width: 0;
  min-height: 0;
}

.role-auth-workspace > section :deep(.role-auth-card) {
  height: 100%;
}

.role-auth-workspace > section :deep(.role-auth-card > .el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.role-auth-workspace > section :deep(.panel-card__content) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

@media (max-width: 960px) {
  .role-form-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1180px) {
  .role-auth-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .role-auth-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .role-auth-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
