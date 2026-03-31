<template>
  <div class="user-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="账号中心"
      description="统一维护平台账号、联系方式和登录状态，分页与刷新只基于已提交筛选。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton
          v-permission="'system:user:add'"
          action="add"
          :icon="Plus"
          @click="handleAdd"
          >新增</StandardButton
        >
      </template>

      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="searchForm.username"
                placeholder="用户名"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.phone"
                placeholder="手机号"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.email"
                placeholder="邮箱"
                clearable
                @keyup.enter="handleSearch"
              />
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

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="username" label="用户名" :width="150" />
        <StandardTableTextColumn
          prop="realName"
          label="真实姓名"
          :width="120"
        />
        <StandardTableTextColumn prop="phone" label="手机号" :width="150" />
        <StandardTableTextColumn prop="email" label="邮箱" :width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn
          prop="lastLoginTime"
          label="最后登录时间"
          :width="180"
        />
        <StandardTableTextColumn
          prop="lastLoginIp"
          label="最后登录 IP"
          :width="150"
        />
        <StandardTableTextColumn
          prop="createTime"
          label="创建时间"
          :width="180"
        />
        <el-table-column
          label="操作"
          :width="userActionColumnWidth"
          fixed="right"
          class-name="standard-row-actions-column"
          :show-overflow-tooltip="false"
        >
          <template #default="{ row }">
            <StandardWorkbenchRowActions
              variant="table"
              gap="compact"
              :direct-items="getUserRowActions()"
              @command="(command) => handleUserRowAction(command, row)"
            />
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="dialogVisible"
      :title="dialogTitle"
      subtitle="通过右侧抽屉维护账号基础信息、联系方式与启停状态。"
      size="42rem"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
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
              formData.id ? 'system:user:update' : 'system:user:add'
            "
            action="confirm"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            确定
          </StandardButton>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="账号中心导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import CsvColumnSettingDialog from "@/components/CsvColumnSettingDialog.vue";
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
import { downloadRowsAsCsv, type CsvColumn } from "@/utils/csv";
import { resolveWorkbenchActionColumnWidth } from "@/utils/adaptiveActionColumn";
import { usePermissionStore } from "@/stores/permission";
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions,
} from "@/utils/csvColumns";
import {
  confirmAction,
  confirmDelete,
  isConfirmCancelled,
} from "@/utils/confirm";
import {
  addUser,
  deleteUser,
  getUser,
  pageUsers,
  resetPassword,
  updateUser,
  type User,
} from "@/api/user";

type UserRowActionCommand = "edit" | "reset-password" | "delete";

const formRef = ref();
const tableRef = ref();
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref("新增用户");
const tableData = ref<User[]>([]);
const selectedRows = ref<User[]>([]);
const permissionStore = usePermissionStore();
const userActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: "edit", label: "编辑" },
    { command: "reset-password", label: "重置密码" },
    { command: "delete", label: "删除" },
  ],
  gap: "compact",
});
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } =
  useServerPagination();

const searchForm = reactive({
  username: "",
  phone: "",
  email: "",
});
const appliedFilters = reactive({
  username: "",
  phone: "",
  email: "",
});

const formData = ref<Partial<User>>({
  id: undefined,
  username: "",
  realName: "",
  phone: "",
  email: "",
  password: "",
  status: 1,
});

const formRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  phone: [{ required: true, message: "请输入手机号", trigger: "blur" }],
  email: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const exportColumns: CsvColumn<User>[] = [
  { key: "username", label: "用户名" },
  { key: "realName", label: "真实姓名" },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  {
    key: "status",
    label: "状态",
    formatter: (value) => (Number(value) === 1 ? "启用" : "禁用"),
  },
  { key: "lastLoginTime", label: "最后登录时间" },
  { key: "lastLoginIp", label: "最后登录 IP" },
  { key: "createTime", label: "创建时间" },
];
const exportColumnStorageKey = "user-view";
const exportColumnOptions = toCsvColumnOptions(exportColumns);
const exportPresets = [
  {
    label: "默认模板",
    keys: exportColumns.map((column) => String(column.key)),
  },
  {
    label: "运维模板",
    keys: ["username", "realName", "status", "lastLoginTime", "lastLoginIp"],
  },
  {
    label: "管理模板",
    keys: ["username", "realName", "phone", "email", "status", "createTime"],
  },
];
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key)),
  ),
);
const exportColumnDialogVisible = ref(false);

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter,
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: "username", label: "用户名" },
    { key: "phone", label: "手机号" },
    { key: "email", label: "邮箱" },
  ],
  defaults: {
    username: "",
    phone: "",
    email: "",
  },
});

const loadUserPage = async () => {
  loading.value = true;
  try {
    const res = await pageUsers({
      username: appliedFilters.username || undefined,
      phone: appliedFilters.phone || undefined,
      email: appliedFilters.email || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    });
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data);
    }
  } catch (error) {
    console.error("获取用户分页失败", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadUserPage();
});

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  loadUserPage();
};

const handleReset = () => {
  searchForm.username = "";
  searchForm.phone = "";
  searchForm.email = "";
  syncAppliedFilters();
  resetPage();
  clearSelection();
  loadUserPage();
};

const handleSelectionChange = (rows: User[]) => {
  selectedRows.value = rows;
};

const getUserRowActions = () => {
  const actions: Array<{ command: UserRowActionCommand; label: string }> = [];
  if (permissionStore.hasPermission("system:user:update")) {
    actions.push({ command: "edit", label: "编辑" });
  }
  if (permissionStore.hasPermission("system:user:reset-password")) {
    actions.push({ command: "reset-password", label: "重置密码" });
  }
  if (permissionStore.hasPermission("system:user:delete")) {
    actions.push({ command: "delete", label: "删除" });
  }
  return actions;
};

const handleUserRowAction = (command: UserRowActionCommand, row: User) => {
  if (command === "edit") {
    handleEdit(row);
    return;
  }
  if (command === "reset-password") {
    handleResetPassword(row);
    return;
  }
  handleDelete(row);
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  loadUserPage();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  loadUserPage();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true;
};

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys;
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys);
};

const getResolvedExportColumns = () =>
  resolveCsvColumns(exportColumns, selectedExportColumnKeys.value);

const handleExportSelected = () => {
  downloadRowsAsCsv(
    "账号中心-选中项.csv",
    selectedRows.value,
    getResolvedExportColumns(),
  );
};

const handleExportCurrent = () => {
  downloadRowsAsCsv(
    "账号中心-当前结果.csv",
    tableData.value,
    getResolvedExportColumns(),
  );
};

const resetFormData = (parent?: Partial<User>) => {
  formData.value = {
    id: parent?.id,
    username: parent?.username || "",
    realName: parent?.realName || "",
    phone: parent?.phone || "",
    email: parent?.email || "",
    password: "",
    status: parent?.status ?? 1,
    roleIds: parent?.roleIds,
  };
};

const handleAdd = () => {
  dialogTitle.value = "新增用户";
  resetFormData();
  dialogVisible.value = true;
};

const handleEdit = async (row: User) => {
  dialogTitle.value = "编辑用户";
  const res = await getUser(row.id as string | number);
  if (res.code === 200 && res.data) {
    resetFormData(res.data);
    dialogVisible.value = true;
  }
};

const handleDelete = async (row: User) => {
  try {
    await confirmDelete("用户", row.username);
    await deleteUser(row.id as string | number);
    ElMessage.success("删除成功");
    loadUserPage();
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error("删除用户失败", error);
  }
};

const handleResetPassword = async (row: User) => {
  try {
    await confirmAction({
      title: "重置密码",
      message: `确认重置用户“${row.username}”的密码吗？重置后默认密码为 123456。`,
      type: "warning",
      confirmButtonText: "确认重置",
    });
    await resetPassword(row.id as string | number);
    ElMessage.success("密码已重置为 123456");
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error("重置用户密码失败", error);
  }
};

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitLoading.value = true;
  try {
    if (formData.value.id) {
      await updateUser(formData.value);
      ElMessage.success("更新成功");
    } else {
      await addUser(formData.value);
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    loadUserPage();
  } catch (error) {
    console.error("提交用户失败", error);
  } finally {
    submitLoading.value = false;
  }
};

const handleDialogClose = () => {
  formRef.value?.resetFields();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  loadUserPage();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  loadUserPage();
};
</script>
