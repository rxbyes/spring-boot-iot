<template>
  <div class="role-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="角色权限"
      description="在角色页统一维护基础信息、页面菜单与按钮权限，导航结构以导航编排页维护结果为准。"
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
      subtitle="通过右侧抽屉维护角色基础信息，并同步配置菜单与按钮权限。"
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
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="授权摘要">
            <div class="role-auth-summary">
              <span class="role-auth-summary__count"
                >已选 {{ checkedMenuCount }} 项菜单/按钮</span
              >
              <div
                v-if="checkedMenuSummary.length"
                class="role-auth-summary__tags"
              >
                <el-tag
                  v-for="label in checkedMenuSummary"
                  :key="label"
                  size="small"
                  effect="plain"
                >
                  {{ label }}
                </el-tag>
                <span
                  v-if="checkedMenuCount > checkedMenuSummary.length"
                  class="role-auth-summary__more"
                >
                  +{{ checkedMenuCount - checkedMenuSummary.length }}
                </span>
              </div>
              <span v-else class="role-auth-summary__empty"
                >未配置菜单权限时，登录后将看不到业务导航。</span
              >
            </div>
          </el-form-item>
        </el-form>

        <section class="role-form-layout__auth">
          <div class="role-auth-panel">
            <div class="role-auth-panel__header">
              <div>
                <h3>菜单与按钮授权</h3>
                <p>
                  目录节点仅用于展示层级；勾选页面或按钮时，后端会自动补齐所需父级菜单。
                </p>
              </div>
              <StandardButton
                v-permission="
                  formData.id ? 'system:role:update' : 'system:role:add'
                "
                action="refresh"
                link
                @click="refreshMenuTree"
                >刷新菜单树</StandardButton
              >
            </div>

            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="导航编排负责维护菜单树与路由元数据；角色权限负责为角色分配可访问页面和按钮权限。"
            />

            <div class="role-auth-toolbar">
              <el-input
                v-model="menuKeyword"
                clearable
                placeholder="筛选菜单名称 / 编码 / 路由"
                class="role-auth-toolbar__search"
              />
              <StandardButton
                v-permission="
                  formData.id ? 'system:role:update' : 'system:role:add'
                "
                action="batch"
                @click="handleCheckAllMenus"
                :disabled="menuSelectableIds.length === 0"
                >全选</StandardButton
              >
              <StandardButton
                v-permission="
                  formData.id ? 'system:role:update' : 'system:role:add'
                "
                action="reset"
                @click="handleClearMenus"
                :disabled="checkedMenuCount === 0"
                >清空</StandardButton
              >
            </div>

            <div v-loading="menuTreeLoading" class="role-auth-tree">
              <el-tree
                ref="menuTreeRef"
                node-key="id"
                show-checkbox
                default-expand-all
                check-strictly
                highlight-current
                :data="menuTreeData"
                :props="menuTreeProps"
                :filter-node-method="filterMenuTreeNode"
                empty-text="暂无可授权菜单"
                @check="handleMenuCheck"
              >
                <template #default="{ data }">
                  <div class="role-tree-node">
                    <div class="role-tree-node__main">
                      <span class="role-tree-node__name">{{
                        data.menuName
                      }}</span>
                      <el-tag
                        size="small"
                        effect="plain"
                        :type="menuTypeTagType(data.type)"
                      >
                        {{ menuTypeLabel(data.type) }}
                      </el-tag>
                      <span
                        v-if="data.disabled"
                        class="role-tree-node__disabled-tip"
                        >目录节点自动补齐</span
                      >
                    </div>
                    <div class="role-tree-node__meta">
                      <code v-if="data.menuCode">{{ data.menuCode }}</code>
                      <code v-if="data.path">{{ data.path }}</code>
                    </div>
                  </div>
                </template>
              </el-tree>
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
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import CsvColumnSettingDialog from "@/components/CsvColumnSettingDialog.vue";
import EmptyState from "@/components/EmptyState.vue";
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
  resolveRoleCheckedMenuIds,
  resolveRoleMenuSummary,
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
  status: number;
  menuIds: number[];
}

interface RoleMenuTreeNode extends MenuTreeNode {
  disabled?: boolean;
  children: RoleMenuTreeNode[];
}

type RoleRowActionCommand = "edit" | "delete";

const formRef = ref();
const tableRef = ref();
const menuTreeRef = ref();
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
  { label: "运维模板", keys: ["roleName", "roleCode", "status", "updateTime"] },
  {
    label: "管理模板",
    keys: ["roleName", "roleCode", "description", "status", "createTime"],
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
const menuTreeData = ref<RoleMenuTreeNode[]>([]);
const menuKeyword = ref("");

const formData = ref<RoleFormData>(createEmptyRoleForm());
const menuTreeProps = {
  label: "menuName",
  children: "children",
  disabled: "disabled",
};

const checkedMenuCount = computed(() => formData.value.menuIds.length);
const checkedMenuSummary = computed(() =>
  resolveRoleMenuSummary(rawMenuTree.value, formData.value.menuIds, 8),
);
const menuSelectableIds = computed(() =>
  resolveRoleCheckedMenuIds(
    rawMenuTree.value,
    flattenMenuIds(rawMenuTree.value),
  ),
);
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
};

watch(menuKeyword, (keyword) => {
  menuTreeRef.value?.filter(keyword);
});

function createEmptyRoleForm(): RoleFormData {
  return {
    id: undefined,
    roleName: "",
    roleCode: "",
    description: "",
    status: 1,
    menuIds: [],
  };
}

function flattenMenuIds(nodes: MenuTreeNode[]): number[] {
  const ids: number[] = [];
  const visit = (items: MenuTreeNode[]) => {
    items.forEach((item) => {
      ids.push(item.id);
      if (item.children?.length) {
        visit(item.children);
      }
    });
  };
  visit(nodes);
  return ids;
}

function buildRoleMenuTree(nodes: MenuTreeNode[]): RoleMenuTreeNode[] {
  return nodes.map((node) => ({
    ...node,
    disabled: node.type === 0,
    children: buildRoleMenuTree(node.children || []),
  }));
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
      menuTreeData.value = buildRoleMenuTree(rawMenuTree.value);
      return true;
    }
    rawMenuTree.value = [];
    menuTreeData.value = [];
    return false;
  } catch (error) {
    console.error("获取菜单树失败", error);
    ElMessage.error((error as Error).message || "获取菜单树失败");
    return false;
  } finally {
    menuTreeLoading.value = false;
  }
}

function applyCheckedMenuIds(menuIds: number[]) {
  formData.value.menuIds = resolveRoleCheckedMenuIds(
    rawMenuTree.value,
    menuIds,
  );
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys(formData.value.menuIds);
    if (menuKeyword.value) {
      menuTreeRef.value?.filter(menuKeyword.value);
    }
  });
}

async function openRoleDialog(title: string, role: RoleFormData) {
  dialogTitle.value = title;
  formData.value = role;
  dialogVisible.value = true;
  await loadMenuAuthTree();
  await nextTick();
  applyCheckedMenuIds(role.menuIds);
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
  menuKeyword.value = "";
  await openRoleDialog("新增角色", createEmptyRoleForm());
}

async function handleEdit(row: Role) {
  menuKeyword.value = "";
  try {
    const res = await getRole(row.id as number);
    if (res.code === 200 && res.data) {
      await openRoleDialog("编辑角色 / 菜单授权", {
        id: Number(res.data.id),
        roleName: res.data.roleName,
        roleCode: res.data.roleCode,
        description: res.data.description || "",
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

function collectCheckedMenuIds(): number[] {
  const checkedKeys = (menuTreeRef.value?.getCheckedKeys(false) ||
    []) as number[];
  return checkedKeys.filter((menuId) => typeof menuId === "number");
}

function handleMenuCheck() {
  formData.value.menuIds = collectCheckedMenuIds();
}

function handleCheckAllMenus() {
  formData.value.menuIds = [...menuSelectableIds.value];
  menuTreeRef.value?.setCheckedKeys(formData.value.menuIds);
}

function handleClearMenus() {
  formData.value.menuIds = [];
  menuTreeRef.value?.setCheckedKeys([]);
}

async function refreshMenuTree() {
  const currentCheckedIds = [...formData.value.menuIds];
  const success = await loadMenuAuthTree();
  if (!success) {
    return;
  }
  await nextTick();
  applyCheckedMenuIds(currentCheckedIds);
  ElMessage.success("菜单树已刷新");
}

function filterMenuTreeNode(keyword: string, data: RoleMenuTreeNode) {
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

function menuTypeLabel(type?: number) {
  if (type === 0) {
    return "目录";
  }
  if (type === 1) {
    return "页面";
  }
  if (type === 2) {
    return "按钮";
  }
  return "未定义";
}

function menuTypeTagType(type?: number) {
  if (type === 1) {
    return "success";
  }
  if (type === 2) {
    return "warning";
  }
  return "info";
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

  formData.value.menuIds = collectCheckedMenuIds();
  submitLoading.value = true;
  try {
    const payload = {
      ...formData.value,
      menuIds: [...formData.value.menuIds],
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
  menuTreeRef.value?.setCheckedKeys([]);
  menuKeyword.value = "";
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

.role-auth-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.role-auth-panel__header h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.role-auth-panel__header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.role-auth-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.role-auth-toolbar__search {
  width: min(320px, 100%);
}

.role-auth-tree {
  min-height: 420px;
  max-height: 520px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--el-fill-color-blank);
}

.role-tree-node {
  display: grid;
  gap: 4px;
  padding: 2px 0;
}

.role-tree-node__main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tree-node__name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.role-tree-node__disabled-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.role-tree-node__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tree-node__meta code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: calc(var(--radius-2xs) + 2px);
  padding: 2px 6px;
}

.role-auth-summary {
  display: grid;
  gap: 8px;
}

.role-auth-summary__count {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.role-auth-summary__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-auth-summary__more,
.role-auth-summary__empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 960px) {
  .role-form-layout {
    grid-template-columns: 1fr;
  }
}
</style>
