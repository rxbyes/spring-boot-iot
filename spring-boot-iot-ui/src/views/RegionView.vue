<template>
  <div class="region-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="区域版图"
      description="统一维护区域层级、类型和坐标信息，默认按需懒加载树结构。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" :icon="Plus" v-permission="'system:region:add'" @click="handleAdd"
          >新增</StandardButton
        >
      </template>

      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="searchForm.regionName"
                placeholder="区域名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.regionCode"
                placeholder="区域编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="searchForm.regionType"
                placeholder="区域类型"
                clearable
              >
                <el-option label="省份" value="province" />
                <el-option label="城市" value="city" />
                <el-option label="区县" value="district" />
                <el-option label="街道" value="street" />
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

      <template #notices>
        <el-alert
          :title="regionModeNotice"
          type="info"
          :closable="false"
          show-icon
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
              v-permission="'system:region:export'"
              @click="handleExportSelected"
              >导出选中</StandardButton
            >
            <StandardButton
              action="refresh"
              link
              :disabled="tableData.length === 0"
              v-permission="'system:region:export'"
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
        element-loading-text="正在刷新区域列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`region-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`region-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
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
            row-key="id"
            :lazy="!isFilterMode"
            :load="loadChildren"
            :tree-props="treeProps"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn
              prop="regionCode"
              label="区域编码"
              :width="150"
            />
            <StandardTableTextColumn
              prop="regionName"
              label="区域名称"
              :width="200"
            />
            <el-table-column prop="regionType" label="区域类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getRegionTypeTag(row.regionType)">
                  {{ getRegionTypeName(row.regionType) }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="longitude" label="经度" :width="120" />
            <StandardTableTextColumn prop="latitude" label="纬度" :width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? "启用" : "禁用" }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="sortNo" label="排序" :width="80" />
            <StandardTableTextColumn prop="remark" label="备注" :min-width="180" />
            <el-table-column
              label="操作"
              :width="regionActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRegionRowActions()"
                  @command="(command) => handleRegionRowAction(command, row)"
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
            class="pagination"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="dialogVisible"
      :title="dialogTitle"
      subtitle="通过右侧抽屉维护区域层级、类型与坐标信息。"
      size="42rem"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="区域名称" prop="regionName">
          <el-input
            v-model="formData.regionName"
            placeholder="请输入区域名称"
          />
        </el-form-item>
        <el-form-item label="区域编码" prop="regionCode">
          <el-input
            v-model="formData.regionCode"
            placeholder="请输入区域编码"
          />
        </el-form-item>
        <el-form-item label="区域类型" prop="regionType">
          <el-select v-model="formData.regionType" placeholder="请选择区域类型">
            <el-option label="省份" value="province" />
            <el-option label="城市" value="city" />
            <el-option label="区县" value="district" />
            <el-option label="街道" value="street" />
          </el-select>
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input-number
            v-model="formData.longitude"
            :min="-180"
            :max="180"
            :step="0.000001"
          />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input-number
            v-model="formData.latitude"
            :min="-90"
            :max="90"
            :step="0.000001"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="formData.sortNo" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="dialogVisible = false"
          @confirm="handleSubmit"
        />
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="区域版图导出列设置"
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
import { downloadRowsAsCsv, type CsvColumn } from "@/utils/csv";
import { resolveWorkbenchActionColumnWidth } from "@/utils/adaptiveActionColumn";
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions,
} from "@/utils/csvColumns";
import { confirmDelete, isConfirmCancelled } from "@/utils/confirm";
import { useServerPagination } from "@/composables/useServerPagination";
import {
  addRegion,
  deleteRegion,
  getRegion,
  listRegions,
  pageRegions,
  updateRegion,
  type Region,
} from "@/api/region";

type RegionRowActionCommand = "edit" | "add-sub" | "delete";

const formRef = ref();
const tableRef = ref();
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref("新增区域");
const tableData = ref<Region[]>([]);
const selectedRows = ref<Region[]>([]);
const regionActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: "edit", label: "编辑" },
    { command: "add-sub", label: "新增子级" },
    { command: "delete", label: "删除" },
  ],
});
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } =
  useServerPagination();
let latestListRequestId = 0;

const searchForm = reactive({
  regionName: "",
  regionCode: "",
  regionType: undefined as string | undefined,
});
const appliedFilters = reactive({
  regionName: "",
  regionCode: "",
  regionType: undefined as string | undefined,
});

const formData = ref<Partial<Region>>({
  id: undefined,
  parentId: 0,
  regionName: "",
  regionCode: "",
  regionType: "province",
  longitude: undefined,
  latitude: undefined,
  status: 1,
  sortNo: 0,
  remark: "",
});

const formRules = {
  regionName: [{ required: true, message: "请输入区域名称", trigger: "blur" }],
  regionCode: [{ required: true, message: "请输入区域编码", trigger: "blur" }],
  regionType: [
    { required: true, message: "请选择区域类型", trigger: "change" },
  ],
};

const exportColumns: CsvColumn<Region>[] = [
  { key: "regionCode", label: "区域编码" },
  { key: "regionName", label: "区域名称" },
  {
    key: "regionType",
    label: "区域类型",
    formatter: (value) => getRegionTypeName(String(value || "")),
  },
  { key: "longitude", label: "经度" },
  { key: "latitude", label: "纬度" },
  {
    key: "status",
    label: "状态",
    formatter: (value) => (Number(value) === 1 ? "启用" : "禁用"),
  },
  { key: "sortNo", label: "排序" },
  { key: "remark", label: "备注" },
];
const exportColumnStorageKey = "region-view";
const exportColumnOptions = toCsvColumnOptions(exportColumns);
const exportPresets = [
  {
    label: "默认模板",
    keys: exportColumns.map((column) => String(column.key)),
  },
  {
    label: "运维模板",
    keys: [
      "regionCode",
      "regionName",
      "regionType",
      "longitude",
      "latitude",
      "status",
    ],
  },
  {
    label: "管理模板",
    keys: [
      "regionCode",
      "regionName",
      "regionType",
      "status",
      "sortNo",
      "remark",
    ],
  },
];
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key)),
  ),
);
const exportColumnDialogVisible = ref(false);

const isFilterMode = computed(() =>
  Boolean(
    appliedFilters.regionName.trim() ||
    appliedFilters.regionCode.trim() ||
    appliedFilters.regionType,
  ),
);
const regionModeNotice = computed(() =>
  isFilterMode.value
    ? "搜索模式返回扁平分页结果，不再加载整棵区域树。"
    : "默认仅分页加载根区域，展开行时按需加载子区域。",
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
    { key: "regionName", label: "区域名称" },
    { key: "regionCode", label: "区域编码" },
    {
      key: "regionType",
      label: (value) => `区域类型：${getRegionTypeName(String(value || ""))}`,
      clearValue: undefined,
      isActive: (value) => Boolean(value),
    },
  ],
  defaults: {
    regionName: "",
    regionCode: "",
    regionType: undefined,
  },
});
const hasRecords = computed(() => tableData.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() =>
  hasAppliedFilters.value ? "没有符合条件的区域记录" : "当前还没有区域数据",
);
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? "已生效筛选暂时没有匹配结果，可以调整筛选条件，或者直接清空当前筛选。"
    : "当前还没有可展示的区域记录，建议稍后刷新，或先新增区域。",
);

const treeProps = {
  children: "children",
  hasChildren: "hasChildren",
};

const loadRegionPage = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const res = await pageRegions({
      regionName: appliedFilters.regionName || undefined,
      regionCode: appliedFilters.regionCode || undefined,
      regionType: appliedFilters.regionType || undefined,
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
    console.error("获取区域分页失败", error);
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
  }
};

const loadChildren = async (
  row: Region,
  _treeNode: unknown,
  resolve: (data: Region[]) => void,
) => {
  try {
    const res = await listRegions(row.id);
    const children = res.data || [];
    row.children = children;
    row.hasChildren = children.length > 0;
    resolve(children);
  } catch (error) {
    console.error("加载区域子节点失败", error);
    resolve([]);
  }
};

onMounted(() => {
  loadRegionPage();
});

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  loadRegionPage();
};

const handleReset = () => {
  searchForm.regionName = "";
  searchForm.regionCode = "";
  searchForm.regionType = undefined;
  syncAppliedFilters();
  resetPage();
  clearSelection();
  loadRegionPage();
};

const handleSelectionChange = (rows: Region[]) => {
  selectedRows.value = rows;
};

const getRegionRowActions = () => [
  { command: "edit" as const, label: "编辑" },
  { command: "add-sub" as const, label: "新增子级" },
  { command: "delete" as const, label: "删除" },
];

const handleRegionRowAction = (command: RegionRowActionCommand, row: Region) => {
  if (command === "edit") {
    handleEdit(row);
    return;
  }
  if (command === "add-sub") {
    handleAddSub(row);
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
  loadRegionPage();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  loadRegionPage();
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
    "区域版图-选中项.csv",
    selectedRows.value,
    getResolvedExportColumns(),
  );
};

const flattenTreeRows = (rows: Region[]): Region[] =>
  rows.flatMap((row) => [
    row,
    ...(Array.isArray(row.children) ? flattenTreeRows(row.children) : []),
  ]);

const handleExportCurrent = () => {
  const rows = isFilterMode.value
    ? tableData.value
    : flattenTreeRows(tableData.value);
  downloadRowsAsCsv("区域版图-当前结果.csv", rows, getResolvedExportColumns());
};

const resetFormData = (region?: Partial<Region>) => {
  formData.value = {
    id: region?.id,
    parentId: region?.parentId ?? 0,
    regionName: region?.regionName || "",
    regionCode: region?.regionCode || "",
    regionType: region?.regionType || "province",
    longitude: region?.longitude,
    latitude: region?.latitude,
    status: region?.status ?? 1,
    sortNo: region?.sortNo ?? 0,
    remark: region?.remark || "",
  };
};

const handleAdd = () => {
  dialogTitle.value = "新增区域";
  resetFormData();
  dialogVisible.value = true;
};

const handleAddSub = (row: Region) => {
  dialogTitle.value = "新增子级";
  resetFormData({ parentId: row.id });
  dialogVisible.value = true;
};

const handleEdit = async (row: Region) => {
  dialogTitle.value = "编辑区域";
  const res = await getRegion(row.id);
  if (res.code === 200 && res.data) {
    resetFormData(res.data);
    dialogVisible.value = true;
  }
};

const handleDelete = async (row: Region) => {
  try {
    await confirmDelete("区域", row.regionName);
    await deleteRegion(row.id);
    ElMessage.success("删除成功");
    loadRegionPage();
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error("删除区域失败", error);
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
      await updateRegion(formData.value);
      ElMessage.success("更新成功");
    } else {
      await addRegion(formData.value);
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    loadRegionPage();
  } catch (error) {
    console.error("提交区域失败", error);
  } finally {
    submitLoading.value = false;
  }
};

const handleDialogClose = () => {
  formRef.value?.resetFields();
};

const getRegionTypeName = (type: string) => {
  const map: Record<string, string> = {
    province: "省份",
    city: "城市",
    district: "区县",
    street: "街道",
  };
  return map[type] || type;
};

const getRegionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    province: "primary",
    city: "warning",
    district: "info",
    street: "success",
  };
  return map[type] || "info";
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  loadRegionPage();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  loadRegionPage();
};
</script>
