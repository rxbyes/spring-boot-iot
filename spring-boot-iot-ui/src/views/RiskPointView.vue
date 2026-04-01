<template>
  <StandardPageShell class="risk-point-view">
    <StandardWorkbenchPanel
      title="风险对象中心"
      :description="`当前 ${pagination.total} 条风险点记录，支持档案维护和设备绑定。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" @click="handleAdd">新增风险点</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.riskPointCode" placeholder="风险点编号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
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
          :title="riskPointAdvice"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `严重 ${criticalCount} 项`, `停用 ${disabledCount} 项`]"
        >
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新风险点列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`risk-point-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`risk-point-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="riskPointList"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="riskPointCode" label="风险点编号" :width="150" />
            <StandardTableTextColumn prop="riskPointName" label="风险点名称" :min-width="180" />
            <el-table-column prop="orgName" label="所属组织" :min-width="160">
              <template #default="{ row }">
                <span>{{ row.orgName || '未配置组织' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getRiskLevelType(row.riskLevel)" round>{{ getRiskLevelText(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="responsiblePhone" label="负责人电话" :width="140" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
            <el-table-column
              label="操作"
              :width="riskPointActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRiskPointRowActions()"
                  :max-direct-items="3"
                  @command="(command) => handleRiskPointRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="add" @click="handleAdd">新增风险点</StandardButton>
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
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护风险点基础信息。"
      size="42rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>配置提示</strong>
          <span>风险点编号在保存后自动生成；请先确认所属组织和风险等级，再继续补齐责任电话与治理说明。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础信息</h3>
                <p>维护风险点主档、所属组织与风险等级，为后续监测、处置与组织范围治理提供统一标识。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="风险点编号">
                <el-input :model-value="form.riskPointCode || '保存后自动生成'" readonly />
              </el-form-item>
              <el-form-item label="风险点名称" prop="riskPointName">
                <el-input v-model="form.riskPointName" placeholder="请输入风险点名称" />
              </el-form-item>
              <el-form-item label="所属组织" prop="orgId">
                <el-tree-select
                  v-model="form.orgId"
                  :data="organizationOptions"
                  node-key="id"
                  check-strictly
                  default-expand-all
                  clearable
                  :props="{ label: 'orgName', children: 'children', value: 'id' }"
                  placeholder="请选择所属组织"
                />
              </el-form-item>
              <el-form-item label="风险等级" prop="riskLevel">
                <el-select v-model="form.riskLevel" placeholder="请选择风险等级">
                  <el-option label="严重" value="critical" />
                  <el-option label="警告" value="warning" />
                  <el-option label="提醒" value="info" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>治理信息</h3>
                <p>补齐责任电话、启停状态和风险说明，便于值班与治理人员快速确认风险点责任归属。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="负责人电话" prop="responsiblePhone">
                <el-input v-model="form.responsiblePhone" placeholder="请输入负责人电话" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入风险点描述、场景说明或治理备注" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        />
      </template>
    </StandardFormDrawer>

    <StandardFormDrawer
      v-model="bindDeviceVisible"
      title="绑定设备"
      subtitle="统一通过右侧抽屉为风险点绑定设备与测点。"
      size="42rem"
      @close="handleBindDrawerClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>绑定提示</strong>
          <span>绑定完成后，风险对象会直接联动实时监测台、阈值策略和告警运营台，请确认设备与测点归属关系准确。</span>
        </div>
        <el-form :model="bindForm" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>绑定对象</h3>
                <p>确认当前风险点并选择要关联的设备、测点，形成后续监测链路。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="风险点" class="ops-drawer-grid__full ops-drawer-readonly">
                <el-input v-model="bindForm.riskPointName" disabled />
              </el-form-item>
              <el-form-item label="设备">
                <el-select v-model="bindForm.deviceId" placeholder="请选择设备">
                  <el-option v-for="device in deviceList" :key="device.id" :label="device.deviceName" :value="device.id">
                    {{ device.deviceCode }} - {{ device.deviceName }}
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="测点">
                <el-select v-model="bindForm.metricIdentifier" placeholder="请选择测点">
                  <el-option v-for="metric in metricList" :key="metric.identifier" :label="metric.name" :value="metric.identifier">
                    {{ metric.name }}
                  </el-option>
                </el-select>
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="bindDeviceVisible = false"
          @confirm="handleBindSubmit"
        />
      </template>
    </StandardFormDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import EmptyState from '@/components/EmptyState.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { listOrganizationTree } from '@/api/organization';
import type { Organization } from '@/api/organization';
import { listDeviceOptions, getDeviceMetricOptions } from '@/api/iot';
import type { DeviceMetricOption, DeviceOption } from '@/types/api';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { pageRiskPointList, addRiskPoint, updateRiskPoint, deleteRiskPoint, bindDevice } from '../api/riskPoint';
import type { RiskPoint } from '../api/riskPoint';

type RiskPointRowActionCommand = 'edit' | 'bind-device' | 'delete';

const loading = ref(false);
const formVisible = ref(false);
const bindDeviceVisible = ref(false);
const riskPointList = ref<RiskPoint[]>([]);
const organizationOptions = ref<Organization[]>([]);
const deviceList = ref<DeviceOption[]>([]);
const metricList = ref<DeviceMetricOption[]>([]);
const tableRef = ref();
const selectedRows = ref<RiskPoint[]>([]);
const riskPointActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'edit', label: '编辑' },
    { command: 'bind-device', label: '绑定设备' },
    { command: 'delete', label: '删除' }
  ],
});

const filters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑风险点' : '新增风险点'));
const form = reactive({
  id: undefined as number | undefined,
  riskPointCode: '',
  riskPointName: '',
  orgId: '' as '' | number,
  orgName: '',
  regionId: 0,
  regionName: '',
  responsibleUser: 0,
  responsiblePhone: '',
  riskLevel: 'info',
  description: '',
  status: 0
});

const rules = {
  riskPointName: [{ required: true, message: '请输入风险点名称', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }]
};

const bindForm = reactive({
  riskPointId: 0,
  riskPointName: '',
  deviceId: 0,
  deviceCode: '',
  deviceName: '',
  metricIdentifier: '',
  metricName: ''
});
const submitLoading = ref(false);
const riskPointAdvice = '优先核查高风险且已启用的风险点';
let latestListRequestId = 0;

const enabledCount = computed(() => riskPointList.value.filter((item) => item.status === 0).length);
const criticalCount = computed(() => riskPointList.value.filter((item) => item.riskLevel === 'critical').length);
const disabledCount = computed(() => riskPointList.value.filter((item) => item.status === 1).length);
const hasRecords = computed(() => riskPointList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的风险点' : '还没有风险对象'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有风险对象记录，先新增风险点，再继续设备绑定和策略治理。'
);

const loadOrganizationOptions = async () => {
  try {
    const res = await listOrganizationTree();
    if (res.code === 200) {
      organizationOptions.value = (res.data || []).filter((item) => item.status === 1);
    }
  } catch (error) {
    console.error('加载组织树失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载组织树失败');
  }
};

const findOrganizationById = (nodes: Organization[], targetId: number): Organization | null => {
  for (const node of nodes) {
    if (Number(node.id) === targetId) {
      return node;
    }
    const childMatch = node.children?.length ? findOrganizationById(node.children, targetId) : null;
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
};

const loadDeviceOptions = async () => {
  try {
    const res = await listDeviceOptions();
    if (res.code === 200) {
      deviceList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载设备选项失败', error);
    ElMessage.error('加载设备列表失败');
  }
};

const loadMetricOptions = async (deviceId: string | number) => {
  try {
    const res = await getDeviceMetricOptions(deviceId);
    if (res.code === 200) {
      metricList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载测点选项失败', error);
    ElMessage.error('加载测点列表失败');
  }
};

const getRiskLevelType = (level: string) => {
  switch (level) {
    case 'critical':
      return 'danger';
    case 'warning':
      return 'warning';
    case 'info':
      return 'info';
    default:
      return 'info';
  }
};

const getRiskLevelText = (level: string) => {
  switch (level) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return level;
  }
};

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'info';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '启用';
    case 1:
      return '停用';
    default:
      return status.toString();
  }
};

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'riskPointCode', label: '风险点编号' },
    { key: 'riskLevel', label: (value) => `风险等级：${getRiskLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    riskPointCode: '',
    riskLevel: '',
    status: '' as '' | number
  }
});

const loadRiskPointList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const res = await pageRiskPointList({
      riskPointCode: appliedFilters.riskPointCode || undefined,
      riskLevel: appliedFilters.riskLevel || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (requestId !== latestListRequestId) {
      return;
    }
    if (res.code === 200) {
      riskPointList.value = applyPageResult(res.data);
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    console.error('查询风险点列表失败', error);
    ElMessage.error(error instanceof Error ? error.message : '查询风险点列表失败');
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
  }
};

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleReset = () => {
  filters.riskPointCode = '';
  filters.riskLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRiskPointList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRiskPointList();
};

const handleSelectionChange = (rows: RiskPoint[]) => {
  selectedRows.value = rows;
};

const getRiskPointRowActions = () => [
  { command: 'edit' as const, label: '编辑' },
  { command: 'bind-device' as const, label: '绑定设备' },
  { command: 'delete' as const, label: '删除' }
];

const handleRiskPointRowAction = (command: RiskPointRowActionCommand, row: RiskPoint) => {
  if (command === 'edit') {
    handleEdit(row);
    return;
  }
  if (command === 'bind-device') {
    handleBindDevice(row);
    return;
  }
  handleDelete(row);
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRiskPointList();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const resetRiskPointForm = () => {
  form.id = undefined;
  form.riskPointCode = '';
  form.riskPointName = '';
  form.orgId = '';
  form.orgName = '';
  form.regionId = 0;
  form.regionName = '';
  form.responsibleUser = 0;
  form.responsiblePhone = '';
  form.riskLevel = 'info';
  form.description = '';
  form.status = 0;
};

const resetBindForm = () => {
  bindForm.riskPointId = 0;
  bindForm.riskPointName = '';
  bindForm.deviceId = 0;
  bindForm.deviceCode = '';
  bindForm.deviceName = '';
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  metricList.value = [];
};

const handleAdd = () => {
  resetRiskPointForm();
  formVisible.value = true;
};

const handleEdit = (row: RiskPoint) => {
  form.id = row.id;
  form.riskPointCode = row.riskPointCode;
  form.riskPointName = row.riskPointName;
  form.orgId = row.orgId ? Number(row.orgId) : '';
  form.orgName = row.orgName || '';
  form.regionId = row.regionId;
  form.regionName = row.regionName;
  form.responsibleUser = row.responsibleUser;
  form.responsiblePhone = row.responsiblePhone;
  form.riskLevel = row.riskLevel;
  form.description = row.description || '';
  form.status = row.status;
  formVisible.value = true;
};

const handleDelete = async (row: RiskPoint) => {
  try {
    await confirmDelete('风险点', row.riskPointName);
    const res = await deleteRiskPoint(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRiskPointList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除风险点失败', error);
    ElMessage.error(error instanceof Error ? error.message : '删除风险点失败');
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const selectedOrganization = findOrganizationById(organizationOptions.value, Number(form.orgId));
    form.orgName = selectedOrganization?.orgName || '';
    const res = form.id ? await updateRiskPoint(form) : await addRiskPoint(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
    ElMessage.error(error instanceof Error ? error.message : '提交风险点失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleBindDevice = async (row: RiskPoint) => {
  resetBindForm();
  bindForm.riskPointId = Number(row.id);
  bindForm.riskPointName = row.riskPointName;
  await loadDeviceOptions();
  bindDeviceVisible.value = true;
};

const handleBindSubmit = async () => {
  if (!bindForm.deviceId || !bindForm.metricIdentifier) {
    ElMessage.warning('请选择设备和测点');
    return;
  }
  try {
    submitLoading.value = true;
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(bindForm.deviceId));
    const selectedMetric = metricList.value.find((metric) => metric.identifier === bindForm.metricIdentifier);
    if (!selectedDevice || !selectedMetric) {
      ElMessage.warning('请选择有效的设备和测点');
      return;
    }
    const res = await bindDevice({
      riskPointId: bindForm.riskPointId,
      deviceId: bindForm.deviceId,
      deviceCode: selectedDevice.deviceCode,
      deviceName: selectedDevice.deviceName,
      metricIdentifier: selectedMetric.identifier,
      metricName: selectedMetric.name
    });
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      bindDeviceVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('绑定设备失败', error);
    ElMessage.error(error instanceof Error ? error.message : '绑定设备失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetRiskPointForm();
};

const handleBindDrawerClose = () => {
  resetBindForm();
};

watch(
  () => bindForm.deviceId,
  async (deviceId) => {
    bindForm.deviceCode = '';
    bindForm.deviceName = '';
    bindForm.metricIdentifier = '';
    bindForm.metricName = '';
    metricList.value = [];
    if (!deviceId) {
      return;
    }
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(deviceId));
    if (selectedDevice) {
      bindForm.deviceCode = selectedDevice.deviceCode;
      bindForm.deviceName = selectedDevice.deviceName;
    }
    await loadMetricOptions(deviceId);
  }
);

watch(
  () => bindForm.metricIdentifier,
  (metricIdentifier) => {
    const selectedMetric = metricList.value.find((metric) => metric.identifier === metricIdentifier);
    bindForm.metricName = selectedMetric?.name || '';
  }
);

onMounted(() => {
  syncAppliedFilters();
  void loadOrganizationOptions();
  void loadRiskPointList();
});
</script>

<style scoped>
.risk-point-view {
  min-width: 0;
}
</style>
